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
        for (Event.Authorisation hold : activeHolds(account)) {
            available = available.subtract(hold.amount());
        }
        return available;
    }

    private List<Event.Authorisation> activeHolds(Account account) {
        List<Event.Authorisation> holds = new ArrayList<>();
        for (Outcome outcome : outcomes) {
            if (outcome.status() == Outcome.Status.APPROVED
                    && outcome.event() instanceof Event.Authorisation authorisation
                    && authorisation.account().equals(account)) {
                holds.add(authorisation);
            }
        }
        for (Outcome outcome : outcomes) {
            if (outcome.status() == Outcome.Status.SETTLED
                    && outcome.event() instanceof Event.Settlement settlement
                    && settlement.account().equals(account)) {
                holds.removeIf(hold -> hold.authId().equals(settlement.authId()));
            }
        }
        return holds;
    }

    private void apply(Event event) {
        Outcome outcome = switch (event) {
            case Event.Credit credit -> post(credit, new Entry(
                    credit.account(), credit.postingDay(), credit.valueDate(), credit.amount()));
            case Event.Debit debit -> post(debit, new Entry(
                    debit.account(), debit.postingDay(), debit.valueDate(), debit.amount().negate()));
            case Event.Authorisation authorisation -> authorise(authorisation);
            case Event.Settlement settlement -> settle(settlement);
        };
        outcomes.add(outcome);
    }

    private Outcome post(Event event, Entry entry) {
        ledger.post(entry);
        return new Outcome(event, Outcome.Status.POSTED);
    }

    private Outcome authorise(Event.Authorisation authorisation) {
        Money afterHold = availableBalance(authorisation.account(), authorisation.postingDay())
                .subtract(authorisation.amount());
        return new Outcome(authorisation,
                afterHold.isNegative() ? Outcome.Status.DECLINED : Outcome.Status.APPROVED);
    }

    private Outcome settle(Event.Settlement settlement) {
        for (Event.Authorisation hold : activeHolds(settlement.account())) {
            if (hold.authId().equals(settlement.authId())) {
                ledger.post(new Entry(settlement.account(), settlement.postingDay(), settlement.valueDate(),
                        settlement.amount().negate()));
                return new Outcome(settlement, Outcome.Status.SETTLED);
            }
        }
        return new Outcome(settlement, Outcome.Status.REJECTED,
                "no active authorisation " + settlement.authId());
    }
}
