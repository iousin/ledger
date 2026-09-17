package io.mal.ledger;

public record Entry(Account account, int postingDay, int valueDate, Money amount) {

    public Entry {
        if (!amount.currency().equals(account.openingBalance().currency())) {
            throw new IllegalArgumentException("Cannot post " + amount.currency() + " to " + account.id()
                    + ", which is held in " + account.openingBalance().currency());
        }
    }
}
