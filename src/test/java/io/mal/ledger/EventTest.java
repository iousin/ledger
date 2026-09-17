package io.mal.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EventTest {

    private static final Account ACC_001 = new Account("ACC-001", Money.of("AED", "0.00"));

    @Test
    void refusesADebitThatIsNotAboveZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event.Debit("E7", 5, ACC_001, Money.of("AED", "-620.00"), 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Event.Debit("E7", 5, ACC_001, Money.of("AED", "0.00"), 2));
    }

    @Test
    void refusesAnAuthorisationThatIsNotAboveZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event.Authorisation("E3", 2, ACC_001, "Auth-A", Money.of("AED", "-200.00"), 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Event.Authorisation("E3", 2, ACC_001, "Auth-A", Money.of("AED", "0.00"), 2));
    }

    @Test
    void refusesACreditThatIsNotAboveZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new Event.Credit("E1", 1, ACC_001, Money.of("AED", "-1200.00"), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Event.Credit("E1", 1, ACC_001, Money.of("AED", "0.00"), 1));
    }
}
