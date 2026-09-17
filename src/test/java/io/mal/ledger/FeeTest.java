package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.mal.ledger.Outcome.Status.DECLINED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeTest {

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

    private static Money aed(String amount) {
        return Money.of("AED", amount);
    }

    private static Entry fee(int postingDay, int valueDate) {
        return new Entry(ACC_001, postingDay, valueDate, aed("-25.00"), Entry.Kind.FEE);
    }

    private static Replay replay(int lastDay, Event... events) {
        Replay replay = new Replay(lastDay, List.of(ACC_001, ACC_002));
        replay.run(List.of(events));
        return replay;
    }

    private static List<Entry> fees(Replay replay) {
        return replay.ledger().entries().stream().filter(entry -> entry.kind() == Entry.Kind.FEE).toList();
    }

    @Test
    void noFeeWhileEveryDayClosesPositive() {
        Replay replay = replay(4, E1, E2, E3, E4, E5, E6);

        assertEquals(List.of(), fees(replay));
    }

    @Test
    void aDayThatClosesAtExactlyZeroIsNotCharged() {
        Replay replay = replay(2,
                new Event.Credit("X1", 1, ACC_001, aed("100.00"), 1),
                new Event.Debit("X2", 2, ACC_001, aed("100.00"), 2));

        assertEquals(aed("0.00"), replay.ledger().closingBalance(ACC_001, 2));
        assertEquals(List.of(), fees(replay));
    }

    @Test
    void anOverdrawnAccountIsChargedOncePerNegativeDay() {
        Replay replay = replay(3,
                new Event.Credit("X1", 1, ACC_001, aed("100.00"), 1),
                new Event.Debit("X2", 2, ACC_001, aed("150.00"), 2));

        assertEquals(List.of(fee(2, 2), fee(3, 3)), fees(replay));
        assertEquals(aed("-75.00"), replay.ledger().closingBalance(ACC_001, 2));
        assertEquals(aed("-100.00"), replay.ledger().closingBalance(ACC_001, 3));
    }

    @Test
    void aFeeForAnEarlierDayCountsWhenTheNextDayIsTested() {
        Replay replay = replay(4,
                new Event.Credit("X1", 1, ACC_001, aed("100.00"), 1),
                new Event.Credit("X2", 4, ACC_001, aed("30.00"), 4),
                new Event.Debit("X3", 4, ACC_001, aed("110.00"), 3));

        assertEquals(List.of(fee(4, 3), fee(4, 4)), fees(replay));
        assertEquals(aed("-35.00"), replay.ledger().closingBalance(ACC_001, 3));
        assertEquals(aed("-30.00"), replay.ledger().closingBalance(ACC_001, 4));
    }

    @Test
    void e7CausesThreeFeesForDaysTwoFourAndFive() {
        Replay replay = replay(5, E1, E2, E3, E4, E5, E6, E7, E8);

        assertEquals(List.of(fee(5, 2), fee(5, 4), fee(5, 5)), fees(replay));
        assertEquals(aed("250.00"), replay.ledger().closingBalance(ACC_001, 1));
        assertEquals(aed("-395.00"), replay.ledger().closingBalance(ACC_001, 2));
        assertEquals(aed("5.00"), replay.ledger().closingBalance(ACC_001, 3));
        assertEquals(aed("-205.00"), replay.ledger().closingBalance(ACC_001, 4));
        assertEquals(aed("-230.00"), replay.ledger().closingBalance(ACC_001, 5));
        assertEquals(aed("-230.00"), replay.availableBalance(ACC_001, 5));
        assertEquals(new Outcome(E8, DECLINED), replay.outcomes().getLast());
    }

    @Test
    void anAccountNotHeldInAedIsNeverChargedAndTheOthersCarryOn() {
        Replay replay = replay(2,
                new Event.Credit("X1", 1, ACC_002, Money.of("BHD", "10.000"), 1),
                new Event.Credit("X2", 1, ACC_001, aed("100.00"), 1),
                new Event.Debit("X3", 2, ACC_002, Money.of("BHD", "15.000"), 2),
                new Event.Debit("X4", 2, ACC_001, aed("150.00"), 2));

        assertEquals(Money.of("BHD", "-5.000"), replay.ledger().closingBalance(ACC_002, 2));
        assertEquals(List.of(fee(2, 2)), fees(replay));
    }
}
