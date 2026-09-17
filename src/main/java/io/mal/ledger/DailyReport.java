package io.mal.ledger;

import java.util.ArrayList;
import java.util.List;

public final class DailyReport {

    private static final String INDENT = "          ";

    private final List<Account> accounts;
    private final List<Event> events;
    private final int lastDay;

    public DailyReport(List<Account> accounts, List<Event> events, int lastDay) {
        this.accounts = List.copyOf(accounts);
        this.events = List.copyOf(events);
        this.lastDay = lastDay;
    }

    public String render() {
        StringBuilder out = new StringBuilder();
        Replay yesterday = replayThrough(0);
        for (int day = 1; day <= lastDay; day++) {
            Replay today = replayThrough(day);
            renderDay(out, day, today, yesterday);
            yesterday = today;
        }
        return out.toString();
    }

    private Replay replayThrough(int day) {
        List<Event> postedByThen = day == lastDay
                ? events
                : events.stream().filter(event -> event.postingDay() <= day).toList();
        Replay replay = new Replay(day, accounts);
        replay.run(postedByThen);
        return replay;
    }

    private void renderDay(StringBuilder out, int day, Replay today, Replay yesterday) {
        out.append("=== Day ").append(day).append(" ===\n");
        out.append(label("Events")).append(orNone(eventsOf(day, today))).append('\n');
        for (Account account : accounts) {
            renderAccount(out, day, account, today, yesterday);
        }
        out.append(label("Auths")).append(orNone(authorisationStates(today))).append('\n');
        out.append(label("Errors")).append(orNone(errorsOf(day, today))).append('\n');
        out.append('\n');
    }

    private void renderAccount(StringBuilder out, int day, Account account, Replay today, Replay yesterday) {
        Money closing = today.ledger().closingBalance(account, day);
        out.append(label(account.id())).append("closing ").append(closing)
                .append(", available ").append(today.availableBalance(account, day)).append('\n');

        List<String> restated = new ArrayList<>();
        for (int earlier = 1; earlier < day; earlier++) {
            Money before = yesterday.ledger().closingBalance(account, earlier);
            Money now = today.ledger().closingBalance(account, earlier);
            if (!before.equals(now)) {
                restated.add("Day " + earlier + " " + before + " -> " + now);
            }
        }
        if (!restated.isEmpty()) {
            out.append(INDENT).append("restated: ").append(String.join(", ", restated)).append('\n');
        }

        List<String> fees = new ArrayList<>();
        for (Entry entry : postedOn(day, today, account, Entry.Kind.FEE)) {
            fees.add(entry.amount().negate() + " for Day " + entry.valueDate());
        }
        out.append(INDENT).append("fees posted: ").append(orNone(fees)).append('\n');

        List<Money> accruals = today.interestAccruals(account, day);
        List<Entry> credited = postedOn(day, today, account, Entry.Kind.INTEREST);
        if (credited.isEmpty()) {
            out.append(INDENT).append("interest today: ").append(accruals.getLast()).append(" (provisional)\n");
        } else {
            List<String> schedule = new ArrayList<>();
            for (Money accrual : accruals) {
                schedule.add(accrual.amount().toPlainString());
            }
            Money credit = credited.getFirst().amount();
            out.append(INDENT).append("interest schedule: ").append(String.join(", ", schedule)).append('\n');
            out.append(INDENT).append("interest credited: ").append(credit)
                    .append(", closing before credit ").append(closing.subtract(credit)).append('\n');
        }
    }

    private List<Entry> postedOn(int day, Replay replay, Account account, Entry.Kind kind) {
        return replay.ledger().entries().stream()
                .filter(entry -> entry.kind() == kind && entry.postingDay() == day
                        && entry.account().equals(account))
                .toList();
    }

    private List<String> eventsOf(int day, Replay replay) {
        List<String> result = new ArrayList<>();
        for (Outcome outcome : replay.outcomes()) {
            if (outcome.event().postingDay() == day) {
                result.add(outcome.event().id() + " " + outcome.status());
            }
        }
        return result;
    }

    private List<String> errorsOf(int day, Replay replay) {
        List<String> result = new ArrayList<>();
        for (Outcome outcome : replay.outcomes()) {
            if (outcome.event().postingDay() == day && outcome.status() == Outcome.Status.REJECTED) {
                result.add(outcome.event().id() + " REJECTED: " + outcome.reason());
            }
        }
        return result;
    }

    private List<String> authorisationStates(Replay replay) {
        List<String> result = new ArrayList<>();
        for (Outcome outcome : replay.outcomes()) {
            if (!(outcome.event() instanceof Event.Authorisation authorisation)) {
                continue;
            }
            if (outcome.status() == Outcome.Status.DECLINED) {
                result.add(authorisation.authId() + " DECLINED");
            } else if (isSettled(authorisation, replay)) {
                result.add(authorisation.authId() + " SETTLED");
            } else {
                result.add(authorisation.authId() + " ACTIVE (hold " + authorisation.amount() + ")");
            }
        }
        return result;
    }

    private boolean isSettled(Event.Authorisation authorisation, Replay replay) {
        for (Outcome outcome : replay.outcomes()) {
            if (outcome.status() == Outcome.Status.SETTLED
                    && outcome.event() instanceof Event.Settlement settlement
                    && settlement.authId().equals(authorisation.authId())
                    && settlement.account().equals(authorisation.account())) {
                return true;
            }
        }
        return false;
    }

    private static String label(String text) {
        return String.format("%-10s", text);
    }

    private static String orNone(List<String> items) {
        return items.isEmpty() ? "none" : String.join(", ", items);
    }
}
