package io.mal.ledger;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.out.print(new DailyReport(BriefStream.ACCOUNTS, BriefStream.EVENTS, BriefStream.LAST_DAY).render());
    }
}
