package io.mal.ledger;

public record Entry(Account account, int postingDay, int valueDate, Money amount, Kind kind) {

    public enum Kind {
        CREDIT,
        DEBIT,
        SETTLEMENT,
        FEE,
        INTEREST,
        REVERSAL
    }

    public Entry {
        if (!amount.currency().equals(account.openingBalance().currency())) {
            throw new IllegalArgumentException("Cannot post " + amount.currency() + " to " + account.id()
                    + ", which is held in " + account.openingBalance().currency());
        }
    }
}
