package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.mal.ledger.Outcome.Status.APPROVED;
import static io.mal.ledger.Outcome.Status.DECLINED;
import static io.mal.ledger.Outcome.Status.POSTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorisationTest {

    private static final Account ACC_001 = new Account("ACC-001", Money.of("AED", "0.00"));
    private static final Account ACC_002 = new Account("ACC-002", Money.of("BHD", "0.000"));

    private static final Event E1 = new Event.Credit("E1", 1, ACC_001, aed("1200.00"), 1);
    private static final Event E2 = new Event.Debit("E2", 1, ACC_001, aed("950.00"), 1);
    private static final Event E3 = authorisation("E3", 2, "Auth-A", "200.00");
    private static final Event E7 = new Event.Debit("E7", 5, ACC_001, aed("620.00"), 2);

    private static Money aed(String amount) {
        return Money.of("AED", amount);
    }

    private static Event authorisation(String id, int day, String authId, String amount) {
        return new Event.Authorisation(id, day, ACC_001, authId, aed(amount), day);
    }

    private static Replay replay(int lastDay, Event... events) {
        Replay replay = new Replay(lastDay);
        replay.run(List.of(events));
        return replay;
    }

    @Test
    void authAIsApprovedAndLeavesFiftyAvailableWithoutTouchingTheLedger() {
        Replay replay = replay(2, E1, E2, E3);

        assertEquals(
                List.of(new Outcome(E1, POSTED), new Outcome(E2, POSTED), new Outcome(E3, APPROVED)),
                replay.outcomes());
        assertEquals(aed("50.00"), replay.availableBalance(ACC_001, 2));
        assertEquals(aed("250.00"), replay.ledger().closingBalance(ACC_001, 2));
        assertEquals(2, replay.ledger().entries().size());
    }

    @Test
    void aHoldThatLeavesExactlyZeroIsApproved() {
        Event exact = authorisation("X1", 2, "Auth-X", "250.00");

        Replay replay = replay(2, E1, E2, exact);

        assertEquals(new Outcome(exact, APPROVED), replay.outcomes().getLast());
        assertEquals(aed("0.00"), replay.availableBalance(ACC_001, 2));
    }

    @Test
    void aHoldThatWouldTakeAvailableBelowZeroIsDeclinedAndDoesNotCount() {
        Event tooMuch = authorisation("X1", 2, "Auth-X", "250.01");

        Replay replay = replay(2, E1, E2, tooMuch);

        assertEquals(new Outcome(tooMuch, DECLINED), replay.outcomes().getLast());
        assertEquals(aed("250.00"), replay.availableBalance(ACC_001, 2));
    }

    @Test
    void activeHoldsStack() {
        Event tooMuch = authorisation("X1", 2, "Auth-X", "50.01");
        Event justFits = authorisation("X2", 2, "Auth-Y", "50.00");

        Replay replay = replay(2, E1, E2, E3, tooMuch, justFits);

        assertEquals(
                List.of(new Outcome(E3, APPROVED), new Outcome(tooMuch, DECLINED), new Outcome(justFits, APPROVED)),
                replay.outcomes().subList(2, 5));
        assertEquals(aed("0.00"), replay.availableBalance(ACC_001, 2));
    }

    @Test
    void anyHoldIsDeclinedWhenTheBalanceIsAlreadyNegative() {
        Event smallest = authorisation("X1", 5, "Auth-X", "0.01");

        Replay replay = replay(5, E1, E2, E7, smallest);

        assertEquals(new Outcome(smallest, DECLINED), replay.outcomes().getLast());
        assertEquals(aed("-370.00"), replay.availableBalance(ACC_001, 5));
    }

    @Test
    void holdsBelongToOneAccount() {
        Replay replay = replay(2, E1, E2, E3);

        assertEquals(Money.of("BHD", "0.000"), replay.availableBalance(ACC_002, 2));
    }
}
