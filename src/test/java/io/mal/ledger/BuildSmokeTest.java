package io.mal.ledger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildSmokeTest {

    @Test
    void runsOnJava21OrLater() {
        assertTrue(Runtime.version().feature() >= 21);
    }
}
