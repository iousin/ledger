package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterestTest {

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
    private static final Event E9_STAND_IN = new Event.Credit("E9-STAND-IN", 6, ACC_001, aed("620.00"), 2);
    private static final Event E10_STAND_IN = new Event.Credit("E10-STAND-IN", 5, ACC_002, bhd("10.000"), 5);

    private static final List<Event> STREAM =
            List.of(E1, E2, E3, E4, E5, E6, E7, E8, E9_STAND_IN, E10_STAND_IN);

    private static Money aed(String amount) {
        return Money.of("AED", amount);
    }

    private static Money bhd(String amount) {
        return Money.of("BHD", amount);
    }

    private static Replay replayThrough(int day) {
        return replay(day, STREAM.stream().filter(event -> event.postingDay() <= day).toList());
    }

    private static Replay replay(int lastDay, List<Event> events) {
        Replay replay = new Replay(lastDay, List.of(ACC_001, ACC_002));
        replay.run(events);
        return replay;
    }

    private static List<Entry> interestEntries(Replay replay) {
        return replay.ledger().entries().stream().filter(entry -> entry.kind() == Entry.Kind.INTEREST).toList();
    }

    private static Money sum(List<Money> amounts) {
        Money total = Money.zero(amounts.getFirst().currency());
        for (Money amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    @Test
    void eachDaysAccrualAsKnownAtItsOwnClose() {
        assertEquals(aed("0.10"), replayThrough(1).interestAccruals(ACC_001, 1).getLast());
        assertEquals(aed("0.10"), replayThrough(2).interestAccruals(ACC_001, 2).getLast());
        assertEquals(aed("0.26"), replayThrough(3).interestAccruals(ACC_001, 3).getLast());
        assertEquals(aed("0.19"), replayThrough(4).interestAccruals(ACC_001, 4).getLast());
        assertEquals(aed("0.00"), replayThrough(5).interestAccruals(ACC_001, 5).getLast());
    }

    @Test
    void theScheduleFollowsTheLedgerAsItStands() {
        assertEquals(
                List.of(aed("0.10"), aed("0.00"), aed("0.00"), aed("0.00"), aed("0.00")),
                replayThrough(5).interestAccruals(ACC_001, 5));
    }

    @Test
    void theFinalScheduleIsCreditedOnceOnDaySix() {
        Replay replay = replayThrough(6);

        assertEquals(
                List.of(aed("0.10"), aed("0.09"), aed("0.25"), aed("0.17"), aed("0.16"), aed("0.16")),
                replay.interestAccruals(ACC_001, 6));
        assertEquals(new Entry(ACC_001, 6, 6, aed("0.93"), Entry.Kind.INTEREST), interestEntries(replay).getFirst());
        assertEquals(aed("390.93"), replay.ledger().closingBalance(ACC_001, 6));
    }

    @Test
    void theRoundedAccrualsSumExactlyToTheCreditOnBothAccounts() {
        Replay replay = replayThrough(6);

        assertEquals(
                List.of(sum(replay.interestAccruals(ACC_001, 6)), sum(replay.interestAccruals(ACC_002, 6))),
                interestEntries(replay).stream().map(Entry::amount).toList());
    }

    @Test
    void bhdAccruesAtThreeDecimals() {
        Replay replay = replayThrough(6);

        assertEquals(
                List.of(bhd("0.000"), bhd("0.000"), bhd("0.000"), bhd("0.000"), bhd("0.004"), bhd("0.004")),
                replay.interestAccruals(ACC_002, 6));
        assertEquals(new Entry(ACC_002, 6, 6, bhd("0.008"), Entry.Kind.INTEREST), interestEntries(replay).getLast());
        assertEquals(bhd("10.008"), replay.ledger().closingBalance(ACC_002, 6));
    }

    @Test
    void noInterestIsPostedBeforeDaySix() {
        assertEquals(List.of(), interestEntries(replayThrough(5)));
    }

    @Test
    void onDaySixFeesComeBeforeInterestAndAnAccountThatEarnsNothingGetsNoCredit() {
        Replay replay = replay(6, List.of(E1, E2, E3, E4, E5, E6, E7, E8));

        assertEquals(
                List.of(new Entry(ACC_001, 6, 6, aed("-25.00"), Entry.Kind.FEE),
                        new Entry(ACC_001, 6, 6, aed("0.10"), Entry.Kind.INTEREST)),
                replay.ledger().entries().subList(replay.ledger().entries().size() - 2,
                        replay.ledger().entries().size()));
        assertEquals(aed("-254.90"), replay.ledger().closingBalance(ACC_001, 6));
        assertEquals(1, interestEntries(replay).size());
    }
}
