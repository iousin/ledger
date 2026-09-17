package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.mal.ledger.Outcome.Status.POSTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplayTest {

    private static final Account ACC_001 = new Account("ACC-001", Money.of("AED", "0.00"));

    private static final Event E1 = new Event.Credit("E1", 1, ACC_001, aed("1200.00"), 1);
    private static final Event E2 = new Event.Debit("E2", 1, ACC_001, aed("950.00"), 1);
    private static final Event E4 = new Event.Credit("E4", 3, ACC_001, aed("400.00"), 3);
    private static final Event E7 = new Event.Debit("E7", 5, ACC_001, aed("620.00"), 2);

    private static Money aed(String amount) {
        return Money.of("AED", amount);
    }

    private static Replay replay(int lastDay, Event... events) {
        Replay replay = new Replay(lastDay, List.of(ACC_001));
        replay.run(List.of(events));
        return replay;
    }

    @Test
    void creditsAndDebitsArePostedAsSignedEntries() {
        Replay replay = replay(1, E1, E2);

        assertEquals(
                List.of(new Entry(ACC_001, 1, 1, aed("1200.00"), Entry.Kind.CREDIT),
                        new Entry(ACC_001, 1, 1, aed("-950.00"), Entry.Kind.DEBIT)),
                replay.ledger().entries());
        assertEquals(aed("250.00"), replay.ledger().closingBalance(ACC_001, 1));
        assertEquals(List.of(new Outcome(E1, POSTED), new Outcome(E2, POSTED)), replay.outcomes());
    }

    @Test
    void aBackdatedDebitRewritesAnEarlierDaysClosing() {
        Replay beforeE7 = replay(4, E1, E2, E4);
        Replay afterE7 = replay(5, E1, E2, E4, E7);

        assertEquals(aed("250.00"), beforeE7.ledger().closingBalance(ACC_001, 2));
        assertEquals(aed("-395.00"), afterE7.ledger().closingBalance(ACC_001, 2));
        assertEquals(aed("5.00"), afterE7.ledger().closingBalance(ACC_001, 3));
    }

    @Test
    void eventsAreTakenByPostingDayKeepingTheListedOrderWithinADay() {
        Event postedDaySix = new Event.Credit("LISTED-THIRD", 6, ACC_001, aed("620.00"), 2);
        Event postedDayFive = new Event.Credit("LISTED-FOURTH", 5, ACC_001, aed("10.00"), 5);

        Replay replay = replay(6, E1, E2, postedDaySix, postedDayFive);

        assertEquals(
                List.of(new Outcome(E1, POSTED), new Outcome(E2, POSTED),
                        new Outcome(postedDayFive, POSTED), new Outcome(postedDaySix, POSTED)),
                replay.outcomes());
    }

    @Test
    void refusesAStreamWithAnEventOutsideTheWindowBeforePostingAnything() {
        Event postedDaySeven = new Event.Credit("E11", 7, ACC_001, aed("1.00"), 7);
        Event postedDayZero = new Event.Credit("E0", 0, ACC_001, aed("1.00"), 1);
        Replay replay = new Replay(6, List.of(ACC_001));

        assertThrows(IllegalArgumentException.class, () -> replay.run(List.of(E1, postedDaySeven)));
        assertThrows(IllegalArgumentException.class, () -> replay.run(List.of(postedDayZero, E1)));
        assertEquals(List.of(), replay.ledger().entries());
        assertEquals(List.of(), replay.outcomes());
    }

    @Test
    void theOutcomeLogCannotBeModified() {
        Replay replay = replay(6, E1, E2);

        assertThrows(UnsupportedOperationException.class, () -> replay.outcomes().add(new Outcome(E4, POSTED)));
        assertThrows(UnsupportedOperationException.class, () -> replay.outcomes().remove(0));
        assertThrows(UnsupportedOperationException.class, () -> replay.outcomes().clear());
    }
}
