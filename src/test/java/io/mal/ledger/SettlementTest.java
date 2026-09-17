package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.mal.ledger.Outcome.Status.APPROVED;
import static io.mal.ledger.Outcome.Status.DECLINED;
import static io.mal.ledger.Outcome.Status.REJECTED;
import static io.mal.ledger.Outcome.Status.SETTLED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementTest {

    private static final Account ACC_001 = new Account("ACC-001", Money.of("AED", "0.00"));

    private static final Event E1 = new Event.Credit("E1", 1, ACC_001, aed("1200.00"), 1);
    private static final Event E2 = new Event.Debit("E2", 1, ACC_001, aed("950.00"), 1);
    private static final Event E3 = new Event.Authorisation("E3", 2, ACC_001, "Auth-A", aed("200.00"), 2);
    private static final Event E4 = new Event.Credit("E4", 3, ACC_001, aed("400.00"), 3);
    private static final Event E5 = new Event.Settlement("E5", 4, ACC_001, "Auth-A", aed("185.00"), 4);
    private static final Event E6 = new Event.Settlement("E6", 4, ACC_001, "Auth-Z", aed("180.00"), 4);
    private static final Event E7 = new Event.Debit("E7", 5, ACC_001, aed("620.00"), 2);
    private static final Event E8 = new Event.Authorisation("E8", 5, ACC_001, "Auth-B", aed("90.00"), 5);

    private static Money aed(String amount) {
        return Money.of("AED", amount);
    }

    private static Replay replay(int lastDay, Event... events) {
        Replay replay = new Replay(lastDay);
        replay.run(List.of(events));
        return replay;
    }

    @Test
    void e5SettlesAuthAForTheSettledAmountAndReleasesTheWholeHold() {
        Replay beforeE5 = replay(3, E1, E2, E3, E4);
        Replay afterE5 = replay(4, E1, E2, E3, E4, E5);

        assertEquals(aed("450.00"), beforeE5.availableBalance(ACC_001, 3));
        assertEquals(new Outcome(E5, SETTLED), afterE5.outcomes().getLast());
        assertEquals(new Entry(ACC_001, 4, 4, aed("-185.00")), afterE5.ledger().entries().getLast());
        assertEquals(aed("465.00"), afterE5.ledger().closingBalance(ACC_001, 4));
        assertEquals(aed("465.00"), afterE5.availableBalance(ACC_001, 4));
    }

    @Test
    void e6IsRejectedAndNoFundsMove() {
        Replay replay = replay(4, E1, E2, E3, E4, E5, E6);

        assertEquals(new Outcome(E6, REJECTED, "no active authorisation Auth-Z"), replay.outcomes().getLast());
        assertEquals(4, replay.ledger().entries().size());
        assertEquals(aed("465.00"), replay.ledger().closingBalance(ACC_001, 4));
    }

    @Test
    void aSecondSettlementOfTheSameAuthorisationIsRejected() {
        Event duplicate = new Event.Settlement("X1", 4, ACC_001, "Auth-A", aed("185.00"), 4);

        Replay replay = replay(4, E1, E2, E3, E4, E5, duplicate);

        assertEquals(new Outcome(duplicate, REJECTED, "no active authorisation Auth-A"), replay.outcomes().getLast());
        assertEquals(aed("465.00"), replay.ledger().closingBalance(ACC_001, 4));
    }

    @Test
    void aSettlementAgainstADeclinedAuthorisationIsRejected() {
        Event declined = new Event.Authorisation("X1", 2, ACC_001, "Auth-X", aed("250.01"), 2);
        Event settlement = new Event.Settlement("X2", 3, ACC_001, "Auth-X", aed("10.00"), 3);

        Replay replay = replay(3, E1, E2, declined, settlement);

        assertEquals(new Outcome(declined, DECLINED), replay.outcomes().get(2));
        assertEquals(new Outcome(settlement, REJECTED, "no active authorisation Auth-X"), replay.outcomes().getLast());
        assertEquals(aed("250.00"), replay.ledger().closingBalance(ACC_001, 3));
    }

    @Test
    void aSettlementAboveItsHoldIsDebitedInFull() {
        Event aboveTheHold = new Event.Settlement("X1", 4, ACC_001, "Auth-A", aed("210.00"), 4);

        Replay replay = replay(4, E1, E2, E3, E4, aboveTheHold);

        assertEquals(new Outcome(aboveTheHold, SETTLED), replay.outcomes().getLast());
        assertEquals(aed("440.00"), replay.ledger().closingBalance(ACC_001, 4));
        assertEquals(aed("440.00"), replay.availableBalance(ACC_001, 4));
    }

    @Test
    void e8IsDeclinedBecauseE7ComesFirst() {
        Replay replay = replay(5, E1, E2, E3, E4, E5, E6, E7, E8);

        assertEquals(new Outcome(E8, DECLINED), replay.outcomes().getLast());
    }

    @Test
    void authBWouldBeApprovedIfE8CameBeforeE7() {
        Replay replay = replay(5, E1, E2, E3, E4, E5, E6, E8, E7);

        assertEquals(new Outcome(E8, APPROVED), replay.outcomes().get(6));
    }

    @Test
    void withoutE7AuthBIsApprovedAndItsHoldReducesAvailableButNotLedger() {
        Replay replay = replay(5, E1, E2, E3, E4, E5, E6, E8);

        assertEquals(new Outcome(E8, APPROVED), replay.outcomes().getLast());
        assertEquals(aed("375.00"), replay.availableBalance(ACC_001, 5));
        assertEquals(aed("465.00"), replay.ledger().closingBalance(ACC_001, 5));
    }
}
