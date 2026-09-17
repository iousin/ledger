package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.mal.ledger.BriefStream.ACCOUNTS;
import static io.mal.ledger.BriefStream.ACC_001;
import static io.mal.ledger.BriefStream.ACC_002;
import static io.mal.ledger.BriefStream.E1;
import static io.mal.ledger.BriefStream.E10;
import static io.mal.ledger.BriefStream.E2;
import static io.mal.ledger.BriefStream.E3;
import static io.mal.ledger.BriefStream.E4;
import static io.mal.ledger.BriefStream.E5;
import static io.mal.ledger.BriefStream.E6;
import static io.mal.ledger.BriefStream.E7;
import static io.mal.ledger.BriefStream.E8;
import static io.mal.ledger.BriefStream.E9;
import static io.mal.ledger.BriefStream.EVENTS;
import static io.mal.ledger.BriefStream.LAST_DAY;
import static io.mal.ledger.Entry.Kind.CREDIT;
import static io.mal.ledger.Entry.Kind.DEBIT;
import static io.mal.ledger.Entry.Kind.FEE;
import static io.mal.ledger.Entry.Kind.INTEREST;
import static io.mal.ledger.Entry.Kind.REVERSAL;
import static io.mal.ledger.Entry.Kind.SETTLEMENT;
import static io.mal.ledger.Outcome.Status.APPROVED;
import static io.mal.ledger.Outcome.Status.DECLINED;
import static io.mal.ledger.Outcome.Status.POSTED;
import static io.mal.ledger.Outcome.Status.REJECTED;
import static io.mal.ledger.Outcome.Status.REVERSED;
import static io.mal.ledger.Outcome.Status.SETTLED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GoldenTest {

    private static Replay replayThrough(int day) {
        Replay replay = new Replay(day, ACCOUNTS);
        replay.run(EVENTS.stream().filter(event -> event.postingDay() <= day).toList());
        return replay;
    }

    private static Money aed(String amount) {
        return Money.of("AED", amount);
    }

    private static Money bhd(String amount) {
        return Money.of("BHD", amount);
    }

    private static List<Money> aedEach(String... amounts) {
        List<Money> result = new ArrayList<>();
        for (String amount : amounts) {
            result.add(aed(amount));
        }
        return result;
    }

    private static List<Money> bhdEach(String... amounts) {
        List<Money> result = new ArrayList<>();
        for (String amount : amounts) {
            result.add(bhd(amount));
        }
        return result;
    }

    private static List<Money> closings(Ledger ledger, Account account, int throughDay) {
        List<Money> result = new ArrayList<>();
        for (int day = 1; day <= throughDay; day++) {
            result.add(ledger.closingBalance(account, day));
        }
        return result;
    }

    private static List<Integer> feeDaysPostedOn(Replay replay, int postingDay) {
        return replay.ledger().entries().stream()
                .filter(entry -> entry.kind() == FEE && entry.postingDay() == postingDay)
                .map(Entry::valueDate)
                .toList();
    }

    private static Money creditedInterest(Replay replay, Account account) {
        return replay.ledger().entries().stream()
                .filter(entry -> entry.kind() == INTEREST && entry.account().equals(account))
                .map(Entry::amount)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void theWholeLedgerAfterDaySix() {
        assertEquals(
                List.of(
                        new Entry(ACC_001, 1, 1, aed("1200.00"), CREDIT),
                        new Entry(ACC_001, 1, 1, aed("-950.00"), DEBIT),
                        new Entry(ACC_001, 3, 3, aed("400.00"), CREDIT),
                        new Entry(ACC_001, 4, 4, aed("-185.00"), SETTLEMENT),
                        new Entry(ACC_001, 5, 2, aed("-620.00"), DEBIT),
                        new Entry(ACC_002, 5, 5, bhd("3.334"), CREDIT),
                        new Entry(ACC_002, 5, 5, bhd("3.333"), CREDIT),
                        new Entry(ACC_002, 5, 5, bhd("3.333"), CREDIT),
                        new Entry(ACC_001, 5, 2, aed("-25.00"), FEE),
                        new Entry(ACC_001, 5, 4, aed("-25.00"), FEE),
                        new Entry(ACC_001, 5, 5, aed("-25.00"), FEE),
                        new Entry(ACC_001, 6, 2, aed("620.00"), REVERSAL),
                        new Entry(ACC_001, 6, 6, aed("0.93"), INTEREST),
                        new Entry(ACC_002, 6, 6, bhd("0.008"), INTEREST)),
                replayThrough(LAST_DAY).ledger().entries());
    }

    @Test
    void theWholeOutcomeLog() {
        assertEquals(
                List.of(
                        new Outcome(E1, POSTED),
                        new Outcome(E2, POSTED),
                        new Outcome(E3, APPROVED),
                        new Outcome(E4, POSTED),
                        new Outcome(E5, SETTLED),
                        new Outcome(E6, REJECTED, "no active authorisation Auth-Z"),
                        new Outcome(E7, POSTED),
                        new Outcome(E8, DECLINED),
                        new Outcome(E10, POSTED),
                        new Outcome(E9, REVERSED)),
                replayThrough(LAST_DAY).outcomes());
    }

    @Test
    void dayOneAtItsClose() {
        Replay replay = replayThrough(1);

        assertEquals(aedEach("250.00"), closings(replay.ledger(), ACC_001, 1));
        assertEquals(aed("250.00"), replay.availableBalance(ACC_001, 1));
        assertEquals(aed("0.10"), replay.interestAccruals(ACC_001, 1).getLast());
        assertEquals(List.of(), feeDaysPostedOn(replay, 1));
        assertEquals(bhd("0.000"), replay.ledger().closingBalance(ACC_002, 1));
    }

    @Test
    void dayTwoAtItsClose() {
        Replay replay = replayThrough(2);

        assertEquals(aedEach("250.00", "250.00"), closings(replay.ledger(), ACC_001, 2));
        assertEquals(aed("50.00"), replay.availableBalance(ACC_001, 2));
        assertEquals(aed("0.10"), replay.interestAccruals(ACC_001, 2).getLast());
        assertEquals(List.of(), feeDaysPostedOn(replay, 2));
        assertEquals(bhd("0.000"), replay.ledger().closingBalance(ACC_002, 2));
    }

    @Test
    void dayThreeAtItsClose() {
        Replay replay = replayThrough(3);

        assertEquals(aedEach("250.00", "250.00", "650.00"), closings(replay.ledger(), ACC_001, 3));
        assertEquals(aed("450.00"), replay.availableBalance(ACC_001, 3));
        assertEquals(aed("0.26"), replay.interestAccruals(ACC_001, 3).getLast());
        assertEquals(List.of(), feeDaysPostedOn(replay, 3));
        assertEquals(bhd("0.000"), replay.ledger().closingBalance(ACC_002, 3));
    }

    @Test
    void dayFourAtItsClose() {
        Replay replay = replayThrough(4);

        assertEquals(aedEach("250.00", "250.00", "650.00", "465.00"), closings(replay.ledger(), ACC_001, 4));
        assertEquals(aed("465.00"), replay.availableBalance(ACC_001, 4));
        assertEquals(aed("0.19"), replay.interestAccruals(ACC_001, 4).getLast());
        assertEquals(List.of(), feeDaysPostedOn(replay, 4));
        assertEquals(bhd("0.000"), replay.ledger().closingBalance(ACC_002, 4));
    }

    @Test
    void dayFiveAtItsClose() {
        Replay replay = replayThrough(5);

        assertEquals(
                aedEach("250.00", "-395.00", "5.00", "-205.00", "-230.00"),
                closings(replay.ledger(), ACC_001, 5));
        assertEquals(aed("-230.00"), replay.availableBalance(ACC_001, 5));
        assertEquals(aed("0.00"), replay.interestAccruals(ACC_001, 5).getLast());
        assertEquals(List.of(2, 4, 5), feeDaysPostedOn(replay, 5));
        assertEquals(bhd("10.000"), replay.ledger().closingBalance(ACC_002, 5));
        assertEquals(bhd("0.004"), replay.interestAccruals(ACC_002, 5).getLast());
    }

    @Test
    void daySixAtItsClose() {
        Replay replay = replayThrough(6);

        assertEquals(
                aedEach("250.00", "225.00", "625.00", "415.00", "390.00", "390.93"),
                closings(replay.ledger(), ACC_001, 6));
        assertEquals(aed("390.93"), replay.availableBalance(ACC_001, 6));
        assertEquals(aed("0.16"), replay.interestAccruals(ACC_001, 6).getLast());
        assertEquals(List.of(), feeDaysPostedOn(replay, 6));
        assertEquals(bhd("10.008"), replay.ledger().closingBalance(ACC_002, 6));
        assertEquals(bhd("0.004"), replay.interestAccruals(ACC_002, 6).getLast());
    }

    @Test
    void theDaySixInterestPicture() {
        Replay replay = replayThrough(6);

        assertEquals(aedEach("0.10", "0.09", "0.25", "0.17", "0.16", "0.16"), replay.interestAccruals(ACC_001, 6));
        assertEquals(aed("0.93"), creditedInterest(replay, ACC_001));
        assertEquals(aed("390.00"),
                replay.ledger().closingBalance(ACC_001, 6).subtract(creditedInterest(replay, ACC_001)));

        assertEquals(
                bhdEach("0.000", "0.000", "0.000", "0.000", "0.004", "0.004"),
                replay.interestAccruals(ACC_002, 6));
        assertEquals(bhd("0.008"), creditedInterest(replay, ACC_002));
        assertEquals(bhd("10.000"),
                replay.ledger().closingBalance(ACC_002, 6).subtract(creditedInterest(replay, ACC_002)));
    }

    @Test
    void theLedgerAtTheEndOfDayFiveBeforeAnyFee() {
        Replay replay = replayThrough(5);
        Ledger beforeFees = new Ledger();
        for (Entry entry : replay.ledger().entries()) {
            if (entry.kind() != FEE) {
                beforeFees.post(entry);
            }
        }

        assertEquals(
                aedEach("250.00", "-370.00", "30.00", "-155.00", "-155.00"),
                closings(beforeFees, ACC_001, 5));
        assertEquals(replay.ledger().closingBalance(ACC_001, 5), replay.availableBalance(ACC_001, 5));
    }
}
