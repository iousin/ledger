package io.mal.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnownGapTest {

    // THIS TEST FAILS ON PURPOSE. It is the one failing test the brief asks for.
    //
    // What it asserts: once E9 has reversed E7, ACC-001 is back where it stood before E7
    // arrived, so Day 5 closes at AED 465.00. This is criterion C6 written as a test.
    //
    // What happens: Day 5 closes at AED 390.00, which is 75.00 short. The shortfall is the three
    // overdraft fees posted on Day 5 for Days 2, 4 and 5. E7 caused them, E9 took E7 back, and
    // the fees are still there.
    //
    // Why my design does this: a posted entry stands until a business event reverses it. Whether
    // a reversal should also refund the fees it caused depends on why the debit was reversed. A
    // bank error should be refunded. A merchant refund need not be. E9 carries no reason, so the
    // ledger has no basis to choose, and I chose not to guess.
    //
    // What this reveals about the design:
    //   1. Reversal has no reason field, so the ledger cannot tell a bank error from a refund.
    //   2. There is no fee refund at all. Even if a person decided to refund the 75.00, this
    //      ledger has no entry kind to record it.
    //   3. If E7 was the bank's own error, the customer stays 75.00 out of pocket until someone
    //      notices. For a UAE-licensed bank that is a consumer protection problem, not only a
    //      gap in the code.
    //
    // What would make it pass: a reason on the reversal and, for a bank error, a compensating
    // refund entry for each charged day that no longer closes negative. I worked that design out
    // and deferred it. The figures are in AMBIGUITIES.md section 1, the refusal is C6 in
    // REJECTED.md, and the production risk and the control are in the architecture document.
    @Test
    void afterE9TheAccountIsBackWhereItStoodBeforeE7() {
        Replay replay = new Replay(BriefStream.LAST_DAY, BriefStream.ACCOUNTS);
        replay.run(BriefStream.EVENTS);

        assertEquals(Money.of("AED", "465.00"), replay.ledger().closingBalance(BriefStream.ACC_001, 5));
    }
}
