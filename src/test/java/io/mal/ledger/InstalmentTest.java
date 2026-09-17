package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.mal.ledger.Outcome.Status.POSTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InstalmentTest {

    private static final Account ACC_002 = new Account("ACC-002", Money.of("BHD", "0.000"));

    private static final Event E10 = new Event.InstalmentCredit("E10", 5, ACC_002, bhd("10.000"), 3, 5);

    private static Money bhd(String amount) {
        return Money.of("BHD", amount);
    }

    private static Entry credit(String amount) {
        return new Entry(ACC_002, 5, 5, bhd(amount), Entry.Kind.CREDIT);
    }

    @Test
    void e10IsPostedAsThreeCreditsThatSumToTheWhole() {
        Replay replay = new Replay(5, List.of(ACC_002));

        replay.run(List.of(E10));

        assertEquals(List.of(credit("3.334"), credit("3.333"), credit("3.333")), replay.ledger().entries());
        assertEquals(bhd("10.000"), replay.ledger().closingBalance(ACC_002, 5));
        assertEquals(bhd("0.000"), replay.ledger().closingBalance(ACC_002, 4));
        assertEquals(List.of(new Outcome(E10, POSTED)), replay.outcomes());
    }
}
