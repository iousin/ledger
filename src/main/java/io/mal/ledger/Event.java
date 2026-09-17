package io.mal.ledger;

public sealed interface Event {

    String id();

    int postingDay();

    record Credit(String id, int postingDay, Account account, Money amount, int valueDate) implements Event {

        public Credit {
            requirePositive(id, amount);
        }
    }

    record Debit(String id, int postingDay, Account account, Money amount, int valueDate) implements Event {

        public Debit {
            requirePositive(id, amount);
        }
    }

    record Authorisation(String id, int postingDay, Account account, String authId, Money amount, int valueDate)
            implements Event {

        public Authorisation {
            requirePositive(id, amount);
        }
    }

    record Settlement(String id, int postingDay, Account account, String authId, Money amount, int valueDate)
            implements Event {

        public Settlement {
            requirePositive(id, amount);
        }
    }

    private static void requirePositive(String id, Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException(id + " must carry an amount above zero, not " + amount.amount());
        }
    }
}
