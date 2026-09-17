package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerTest {

    private static final Account ACC_001 = new Account("ACC-001", Money.of("AED", "0.00"));
    private static final Account ACC_002 = new Account("ACC-002", Money.of("BHD", "0.000"));

    private static final Entry E1 = aed(1, 1, "1200.00");
    private static final Entry E2 = aed(1, 1, "-950.00");
    private static final Entry E4 = aed(3, 3, "400.00");
    private static final Entry E5 = aed(4, 4, "-185.00");
    private static final Entry E7 = aed(5, 2, "-620.00");
    private static final Entry E9 = aed(6, 2, "620.00");
    private static final Entry E10_FIRST_INSTALMENT = new Entry(ACC_002, 5, 5, Money.of("BHD", "3.334"), Entry.Kind.CREDIT);

    private static Entry aed(int postingDay, int valueDate, String amount) {
        Money money = Money.of("AED", amount);
        return new Entry(ACC_001, postingDay, valueDate, money,
                money.isNegative() ? Entry.Kind.DEBIT : Entry.Kind.CREDIT);
    }

    private static Ledger ledgerWith(Entry... entries) {
        Ledger ledger = new Ledger();
        for (Entry entry : entries) {
            ledger.post(entry);
        }
        return ledger;
    }

    @Test
    void e1AndE2CloseDayOneAt250() {
        Ledger ledger = ledgerWith(E1, E2);

        assertEquals(Money.of("AED", "250.00"), ledger.closingBalance(ACC_001, 1));
        assertEquals(Money.of("AED", "250.00"), ledger.closingBalance(ACC_001, 6));
    }

    @Test
    void anAccountWithNoEntriesClosesAtItsOpeningBalance() {
        Ledger ledger = ledgerWith(E1, E2);

        assertEquals(Money.of("BHD", "0.000"), ledger.closingBalance(ACC_002, 1));
    }

    @Test
    void anEntryCountsFromItsValueDate() {
        Ledger ledger = ledgerWith(E1, E2, E4);

        assertEquals(Money.of("AED", "250.00"), ledger.closingBalance(ACC_001, 2));
        assertEquals(Money.of("AED", "650.00"), ledger.closingBalance(ACC_001, 3));
    }

    @Test
    void aLateEntryWithAnEarlyValueDateRewritesEarlierDays() {
        Ledger ledger = ledgerWith(E1, E2, E4, E5);
        assertEquals(Money.of("AED", "250.00"), ledger.closingBalance(ACC_001, 2));

        ledger.post(E7);

        assertEquals(Money.of("AED", "250.00"), ledger.closingBalance(ACC_001, 1));
        assertEquals(Money.of("AED", "-370.00"), ledger.closingBalance(ACC_001, 2));
        assertEquals(Money.of("AED", "30.00"), ledger.closingBalance(ACC_001, 3));
        assertEquals(Money.of("AED", "-155.00"), ledger.closingBalance(ACC_001, 4));
        assertEquals(Money.of("AED", "-155.00"), ledger.closingBalance(ACC_001, 5));
    }

    @Test
    void accountsAreKeptApart() {
        Ledger ledger = ledgerWith(E1, E10_FIRST_INSTALMENT);

        assertEquals(Money.of("AED", "1200.00"), ledger.closingBalance(ACC_001, 5));
        assertEquals(Money.of("BHD", "3.334"), ledger.closingBalance(ACC_002, 5));
    }

    @Test
    void refusesAnEntryInAnotherCurrencyThanItsAccount() {
        assertThrows(IllegalArgumentException.class, () -> new Entry(ACC_002, 5, 5, Money.of("AED", "-25.00"), Entry.Kind.FEE));
    }

    @Test
    void refusesAPostingDayEarlierThanTheLastOne() {
        Ledger ledger = ledgerWith(E1, E2, E9);

        assertThrows(IllegalArgumentException.class, () -> ledger.post(E10_FIRST_INSTALMENT));
        assertEquals(List.of(E1, E2, E9), ledger.entries());
    }

    @Test
    void theEntriesViewCannotBeModified() {
        Ledger ledger = ledgerWith(E1, E2);

        assertEquals(List.of(E1, E2), ledger.entries());
        assertThrows(UnsupportedOperationException.class, () -> ledger.entries().add(E4));
        assertThrows(UnsupportedOperationException.class, () -> ledger.entries().remove(0));
        assertThrows(UnsupportedOperationException.class, () -> ledger.entries().clear());
    }
}
