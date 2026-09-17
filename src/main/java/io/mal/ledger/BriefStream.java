package io.mal.ledger;

import java.util.List;

public final class BriefStream {

    public static final int LAST_DAY = 6;

    public static final Account ACC_001 = new Account("ACC-001", Money.of("AED", "0.00"));
    public static final Account ACC_002 = new Account("ACC-002", Money.of("BHD", "0.000"));

    public static final List<Account> ACCOUNTS = List.of(ACC_001, ACC_002);

    public static final Event E1 = new Event.Credit("E1", 1, ACC_001, Money.of("AED", "1200.00"), 1);
    public static final Event E2 = new Event.Debit("E2", 1, ACC_001, Money.of("AED", "950.00"), 1);
    public static final Event E3 = new Event.Authorisation("E3", 2, ACC_001, "Auth-A", Money.of("AED", "200.00"), 2);
    public static final Event E4 = new Event.Credit("E4", 3, ACC_001, Money.of("AED", "400.00"), 3);
    public static final Event E5 = new Event.Settlement("E5", 4, ACC_001, "Auth-A", Money.of("AED", "185.00"), 4);
    public static final Event E6 = new Event.Settlement("E6", 4, ACC_001, "Auth-Z", Money.of("AED", "180.00"), 4);
    public static final Event E7 = new Event.Debit("E7", 5, ACC_001, Money.of("AED", "620.00"), 2);
    public static final Event E8 = new Event.Authorisation("E8", 5, ACC_001, "Auth-B", Money.of("AED", "90.00"), 5);
    public static final Event E9 = new Event.Reversal("E9", 6, ACC_001, "E7", 2);
    public static final Event E10 = new Event.InstalmentCredit("E10", 5, ACC_002, Money.of("BHD", "10.000"), 3, 5);

    public static final List<Event> EVENTS = List.of(E1, E2, E3, E4, E5, E6, E7, E8, E9, E10);

    private BriefStream() {
    }
}
