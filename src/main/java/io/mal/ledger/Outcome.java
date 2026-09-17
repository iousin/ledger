package io.mal.ledger;

public record Outcome(Event event, Status status) {

    public enum Status {
        POSTED,
        APPROVED,
        DECLINED
    }
}
