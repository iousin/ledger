package io.mal.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Ledger {

    private final List<Entry> entries = new ArrayList<>();

    public void post(Entry entry) {
        if (!entries.isEmpty() && entry.postingDay() < entries.getLast().postingDay()) {
            throw new IllegalArgumentException("Cannot post on day " + entry.postingDay()
                    + " after an entry posted on day " + entries.getLast().postingDay());
        }
        entries.add(entry);
    }

    public Money closingBalance(Account account, int day) {
        Money balance = account.openingBalance();
        for (Entry entry : entries) {
            if (entry.account().equals(account) && entry.valueDate() <= day) {
                balance = balance.add(entry.amount());
            }
        }
        return balance;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }
}
