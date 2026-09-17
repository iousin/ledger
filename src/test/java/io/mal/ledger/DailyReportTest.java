package io.mal.ledger;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyReportTest {

    private static final String REPORT =
            new DailyReport(BriefStream.ACCOUNTS, BriefStream.EVENTS, BriefStream.LAST_DAY).render();

    private static List<String> linesOfDay(int day) {
        List<String> lines = new ArrayList<>();
        boolean inDay = false;
        for (String line : REPORT.split("\n")) {
            if (line.startsWith("=== Day ")) {
                inDay = line.equals("=== Day " + day + " ===");
            } else if (inDay && !line.isBlank()) {
                lines.add(line.strip().replaceAll("\\s+", " "));
            }
        }
        return lines;
    }

    private static void assertDayHas(int day, String... expectedLines) {
        List<String> lines = linesOfDay(day);
        for (String expected : expectedLines) {
            assertTrue(lines.contains(expected), "Day " + day + " lacks [" + expected + "] in " + lines);
        }
    }

    @Test
    void dayOne() {
        assertDayHas(1,
                "Events E1 POSTED, E2 POSTED",
                "ACC-001 closing AED 250.00, available AED 250.00",
                "fees posted: none",
                "interest today: AED 0.10 (provisional)",
                "ACC-002 closing BHD 0.000, available BHD 0.000",
                "interest today: BHD 0.000 (provisional)",
                "Auths none",
                "Errors none");
    }

    @Test
    void dayTwoShowsTheHold() {
        assertDayHas(2,
                "Events E3 APPROVED",
                "ACC-001 closing AED 250.00, available AED 50.00",
                "Auths Auth-A ACTIVE (hold AED 200.00)");
    }

    @Test
    void dayThree() {
        assertDayHas(3,
                "Events E4 POSTED",
                "ACC-001 closing AED 650.00, available AED 450.00",
                "interest today: AED 0.26 (provisional)",
                "Auths Auth-A ACTIVE (hold AED 200.00)");
    }

    @Test
    void dayFourShowsTheSettlementAndTheError() {
        assertDayHas(4,
                "Events E5 SETTLED, E6 REJECTED",
                "ACC-001 closing AED 465.00, available AED 465.00",
                "interest today: AED 0.19 (provisional)",
                "Auths Auth-A SETTLED",
                "Errors E6 REJECTED: no active authorisation Auth-Z");
    }

    @Test
    void dayFiveShowsTheFeesTheRestatedDaysAndTheDecline() {
        assertDayHas(5,
                "Events E7 POSTED, E8 DECLINED, E10 POSTED",
                "ACC-001 closing AED -230.00, available AED -230.00",
                "restated: Day 2 AED 250.00 -> AED -395.00, Day 3 AED 650.00 -> AED 5.00, "
                        + "Day 4 AED 465.00 -> AED -205.00",
                "fees posted: AED 25.00 for Day 2, AED 25.00 for Day 4, AED 25.00 for Day 5",
                "interest today: AED 0.00 (provisional)",
                "ACC-002 closing BHD 10.000, available BHD 10.000",
                "interest today: BHD 0.004 (provisional)",
                "Auths Auth-A SETTLED, Auth-B DECLINED",
                "Errors none");
    }

    @Test
    void daySixShowsTheReversalAndTheInterestCredit() {
        assertDayHas(6,
                "Events E9 REVERSED",
                "ACC-001 closing AED 390.93, available AED 390.93",
                "restated: Day 2 AED -395.00 -> AED 225.00, Day 3 AED 5.00 -> AED 625.00, "
                        + "Day 4 AED -205.00 -> AED 415.00, Day 5 AED -230.00 -> AED 390.00",
                "fees posted: none",
                "interest schedule: 0.10, 0.09, 0.25, 0.17, 0.16, 0.16",
                "interest credited: AED 0.93, closing before credit AED 390.00",
                "ACC-002 closing BHD 10.008, available BHD 10.008",
                "interest schedule: 0.000, 0.000, 0.000, 0.000, 0.004, 0.004",
                "interest credited: BHD 0.008, closing before credit BHD 10.000",
                "Auths Auth-A SETTLED, Auth-B DECLINED",
                "Errors none");
    }

    @Test
    void refusesAStreamWithAnEventPostedAfterTheLastDay() {
        List<Event> events = new ArrayList<>(BriefStream.EVENTS);
        events.add(new Event.Credit("E11", 7, BriefStream.ACC_001, Money.of("AED", "1.00"), 7));

        assertThrows(IllegalArgumentException.class,
                () -> new DailyReport(BriefStream.ACCOUNTS, events, BriefStream.LAST_DAY).render());
    }
}
