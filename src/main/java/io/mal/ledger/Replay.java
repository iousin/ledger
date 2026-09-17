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

    private void apply(Event event) {
        Outcome.Status status = switch (event) {
            case Event.Credit credit -> post(new Entry(
                    credit.account(), credit.postingDay(), credit.valueDate(), credit.amount()));
            case Event.Debit debit -> post(new Entry(
                    debit.account(), debit.postingDay(), debit.valueDate(), debit.amount().negate()));
        };
        outcomes.add(new Outcome(event, status));
    }

    private Outcome.Status post(Entry entry) {
        ledger.post(entry);
        return Outcome.Status.POSTED;
    }
}
