package io.mal.ledger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Replay {

    private static final Money OVERDRAFT_FEE = Money.of("AED", "25.00");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.0004");
    private static final int CAPITALISATION_DAY = 6;

    private final Ledger ledger = new Ledger();
    private final List<Outcome> outcomes = new ArrayList<>();
    private final int lastDay;
    private final List<Account> accounts;

    public Replay(int lastDay, List<Account> accounts) {
        this.lastDay = lastDay;
        this.accounts = List.copyOf(accounts);
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
            closeDay(day);
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

    public List<Money> interestAccruals(Account account, int throughDay) {
        List<Money> accruals = new ArrayList<>();
        for (int day = 1; day <= throughDay; day++) {
            Money base = ledger.closingBalance(account, day).subtract(interestCredited(account, day));
            accruals.add(base.isPositive() ? base.multiply(DAILY_INTEREST_RATE) : Money.zero(base.currency()));
        }
        return List.copyOf(accruals);
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
                    credit.account(), credit.postingDay(), credit.valueDate(), credit.amount(),
                    Entry.Kind.CREDIT));
            case Event.Debit debit -> post(debit, new Entry(
                    debit.account(), debit.postingDay(), debit.valueDate(), debit.amount().negate(),
                    Entry.Kind.DEBIT));
            case Event.Authorisation authorisation -> authorise(authorisation);
            case Event.Settlement settlement -> settle(settlement);
        };
        outcomes.add(outcome);
    }

    private void closeDay(int today) {
        for (Account account : accounts) {
            if (account.openingBalance().currency().equals(OVERDRAFT_FEE.currency())) {
                chargeOverdraftFees(account, today);
            }
        }
        if (today == CAPITALISATION_DAY) {
            for (Account account : accounts) {
                capitaliseInterest(account, today);
            }
        }
    }

    private void chargeOverdraftFees(Account account, int today) {
        for (int day = 1; day <= today; day++) {
            if (!hasFee(account, day) && ledger.closingBalance(account, day).isNegative()) {
                ledger.post(new Entry(account, today, day, OVERDRAFT_FEE.negate(), Entry.Kind.FEE));
            }
        }
    }

    private boolean hasFee(Account account, int day) {
        for (Entry entry : ledger.entries()) {
            if (entry.kind() == Entry.Kind.FEE && entry.account().equals(account) && entry.valueDate() == day) {
                return true;
            }
        }
        return false;
    }

    private void capitaliseInterest(Account account, int today) {
        Money total = Money.zero(account.openingBalance().currency());
        for (Money accrual : interestAccruals(account, today)) {
            total = total.add(accrual);
        }
        if (total.isPositive()) {
            ledger.post(new Entry(account, today, today, total, Entry.Kind.INTEREST));
        }
    }

    private Money interestCredited(Account account, int day) {
        Money credited = Money.zero(account.openingBalance().currency());
        for (Entry entry : ledger.entries()) {
            if (entry.kind() == Entry.Kind.INTEREST && entry.account().equals(account)
                    && entry.valueDate() <= day) {
                credited = credited.add(entry.amount());
            }
        }
        return credited;
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
                        settlement.amount().negate(), Entry.Kind.SETTLEMENT));
                return new Outcome(settlement, Outcome.Status.SETTLED);
            }
        }
        return new Outcome(settlement, Outcome.Status.REJECTED,
                "no active authorisation " + settlement.authId());
    }
}
