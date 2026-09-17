package io.mal.ledger;

public record Outcome(Event event, Status status, String reason) {

    public Outcome(Event event, Status status) {
        this(event, status, "");
    }

    public enum Status {
        POSTED,
        APPROVED,
        DECLINED,
        SETTLED,
        REJECTED
    }
}
