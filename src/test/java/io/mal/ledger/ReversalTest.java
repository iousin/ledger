package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.mal.ledger.Outcome.Status.REJECTED;
import static io.mal.ledger.Outcome.Status.REVERSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReversalTest {

    private static final Account ACC_001 = new Account("ACC-001", Money.of("AED", "0.00"));
    private static final Account ACC_002 = new Account("ACC-002", Money.of("BHD", "0.000"));

    private static final Event E1 = new Event.Credit("E1", 1, ACC_001, aed("1200.00"), 1);
    private static final Event E2 = new Event.Debit("E2", 1, ACC_001, aed("950.00"), 1);
    private static final Event E3 = new Event.Authorisation("E3", 2, ACC_001, "Auth-A", aed("200.00"), 2);
    private static final Event E4 = new Event.Credit("E4", 3, ACC_001, aed("400.00"), 3);
    private static final Event E5 = new Event.Settlement("E5", 4, ACC_001, "Auth-A", aed("185.00"), 4);
    private static final Event E6 = new Event.Settlement("E6", 4, ACC_001, "Auth-Z", aed("180.00"), 4);
    private static final Event E7 = new Event.Debit("E7", 5, ACC_001, aed("620.00"), 2);
    private static final Event E8 = new Event.Authorisation("E8", 5, ACC_001, "Auth-B", aed("90.00"), 5);
    private static final Event E9 = new Event.Reversal("E9", 6, ACC_001, "E7", 2);
    private static final Event E10 = new Event.InstalmentCredit("E10", 5, ACC_002, Money.of("BHD", "10.000"), 3, 5);

    private static Money aed(String amount) {
        return Money.of("AED", amount);
    }

    private static Replay replay(int lastDay, Event... events) {
        Replay replay = new Replay(lastDay, List.of(ACC_001, ACC_002));
        replay.run(List.of(events));
        return replay;
    }

    private static List<Entry> entriesOfKind(Replay replay, Entry.Kind kind) {
        return replay.ledger().entries().stream().filter(entry -> entry.kind() == kind).toList();
    }

    @Test
    void e9PostsACompensatingEntryAndLeavesE7Untouched() {
        Replay replay = replay(6, E1, E2, E3, E4, E5, E6, E7, E8, E9, E10);

        assertTrue(replay.ledger().entries().contains(new Entry(ACC_001, 5, 2, aed("-620.00"), Entry.Kind.DEBIT)));
        assertEquals(
                List.of(new Entry(ACC_001, 6, 2, aed("620.00"), Entry.Kind.REVERSAL)),
                entriesOfKind(replay, Entry.Kind.REVERSAL));
        assertEquals(new Outcome(E9, REVERSED), replay.outcomes().getLast());
    }

    @Test
    void theFeesStandAfterTheReversal() {
        Replay replay = replay(6, E1, E2, E3, E4, E5, E6, E7, E8, E9, E10);

        assertEquals(
                List.of(new Entry(ACC_001, 5, 2, aed("-25.00"), Entry.Kind.FEE),
                        new Entry(ACC_001, 5, 4, aed("-25.00"), Entry.Kind.FEE),
                        new Entry(ACC_001, 5, 5, aed("-25.00"), Entry.Kind.FEE)),
                entriesOfKind(replay, Entry.Kind.FEE));
        assertEquals(aed("250.00"), replay.ledger().closingBalance(ACC_001, 1));
        assertEquals(aed("225.00"), replay.ledger().closingBalance(ACC_001, 2));
        assertEquals(aed("625.00"), replay.ledger().closingBalance(ACC_001, 3));
        assertEquals(aed("415.00"), replay.ledger().closingBalance(ACC_001, 4));
        assertEquals(aed("390.00"), replay.ledger().closingBalance(ACC_001, 5));
    }

    @Test
    void aReversalOfAnUnknownEventIsRejectedAndNothingMoves() {
        Event unknown = new Event.Reversal("X1", 6, ACC_001, "E99", 2);

        Replay replay = replay(6, E1, E2, unknown);

        assertEquals(new Outcome(unknown, REJECTED, "no reversible event E99"), replay.outcomes().getLast());
        assertEquals(List.of(), entriesOfKind(replay, Entry.Kind.REVERSAL));
    }

    @Test
    void theSameEventCannotBeReversedTwice() {
        Event again = new Event.Reversal("X1", 6, ACC_001, "E7", 2);

        Replay replay = replay(6, E1, E2, E3, E4, E5, E6, E7, E8, E9, E10, again);

        assertEquals(new Outcome(again, REJECTED, "no reversible event E7"), replay.outcomes().getLast());
        assertEquals(aed("225.00"), replay.ledger().closingBalance(ACC_001, 2));
    }

    @Test
    void anAuthorisationCannotBeReversed() {
        Event ofAnAuthorisation = new Event.Reversal("X1", 6, ACC_001, "E3", 2);

        Replay replay = replay(6, E1, E2, E3, ofAnAuthorisation);

        assertEquals(new Outcome(ofAnAuthorisation, REJECTED, "no reversible event E3"), replay.outcomes().getLast());
        assertEquals(aed("50.00"), replay.availableBalance(ACC_001, 5));
    }

    @Test
    void aReversalOnAnotherAccountThanItsTargetIsRejected() {
        Event wrongAccount = new Event.Reversal("X1", 6, ACC_002, "E7", 2);

        Replay replay = replay(6, E1, E2, E7, wrongAccount);

        assertEquals(new Outcome(wrongAccount, REJECTED, "no reversible event E7"), replay.outcomes().getLast());
        assertEquals(List.of(), entriesOfKind(replay, Entry.Kind.REVERSAL));
    }

    @Test
    void aCreditCanBeReversedToo() {
        Event ofE1 = new Event.Reversal("X1", 2, ACC_001, "E1", 1);

        Replay replay = replay(2, E1, ofE1);

        assertEquals(new Outcome(ofE1, REVERSED), replay.outcomes().getLast());
        assertEquals(
                List.of(new Entry(ACC_001, 2, 1, aed("-1200.00"), Entry.Kind.REVERSAL)),
                entriesOfKind(replay, Entry.Kind.REVERSAL));
        assertEquals(aed("0.00"), replay.ledger().closingBalance(ACC_001, 1));
    }
}
