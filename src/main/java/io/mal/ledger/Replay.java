package io.mal.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Replay {

    private final Ledger ledger = new Ledger();
    private final List<Outcome> outcomes = new ArrayList<>();
    private final int lastDay;

    public Replay(int lastDay) {
        this.lastDay = lastDay;
    }

    public void run(List<Event> events) {
        for (Event event : events) {
            if (event.postingDay() < 1 || event.postingDay() > lastDay) {
                throw new IllegalArgumentException(event.id() + " is posted on day " + event.postingDay()
                        + ", outside days 1 to " + lastDay);
            }
        }
        for (int day = 1; day <= lastDay; day++) {
            for (Event event : events) {
                if (event.postingDay() == day) {
                    apply(event);
                }
            }
        }
    }

    public Ledger ledger() {
        return ledger;
    }

    public List<Outcome> outcomes() {
        return Collections.unmodifiableList(outcomes);
    }

    public Money availableBalance(Account account, int day) {
        Money available = ledger.closingBalance(account, day);
        for (Outcome outcome : outcomes) {
            if (outcome.status() == Outcome.Status.APPROVED
                    && outcome.event() instanceof Event.Authorisation authorisation
                    && authorisation.account().equals(account)) {
                available = available.subtract(authorisation.amount());
            }
        }
        return available;
    }

    private void apply(Event event) {
        Outcome.Status status = switch (event) {
            case Event.Credit credit -> post(new Entry(
                    credit.account(), credit.postingDay(), credit.valueDate(), credit.amount()));
            case Event.Debit debit -> post(new Entry(
                    debit.account(), debit.postingDay(), debit.valueDate(), debit.amount().negate()));
            case Event.Authorisation authorisation -> authorise(authorisation);
        };
        outcomes.add(new Outcome(event, status));
    }

    private Outcome.Status authorise(Event.Authorisation authorisation) {
        Money afterHold = availableBalance(authorisation.account(), authorisation.postingDay())
                .subtract(authorisation.amount());
        return afterHold.isNegative() ? Outcome.Status.DECLINED : Outcome.Status.APPROVED;
    }

    private Outcome.Status post(Entry entry) {
        ledger.post(entry);
        return Outcome.Status.POSTED;
    }
}
