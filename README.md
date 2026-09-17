# Ledger

In-memory account ledger core: value-dated entries, authorisation holds, overdraft fees, daily interest. Plain Java 21 and JUnit 5, no frameworks.

## Run

Print the six-day report for the brief's event stream:

```
./gradlew run -q
```

Run the test suite:

```
./gradlew test
```

On Windows use `gradlew.bat`. The Gradle wrapper is committed, so no Gradle install is needed.

## What to expect from the tests

87 tests. 86 pass and 1 fails on purpose:

```
KnownGapTest > afterE9TheAccountIsBackWhereItStoodBeforeE7() FAILED
    expected: <AED 465.00> but was: <AED 390.00>
```

That is the annotated failing test the brief asks for. The comment in `KnownGapTest` says what it reveals. Because of it, `./gradlew test` and `./gradlew build` end with a failure. `./gradlew run` is not affected. The full results are in `build/reports/tests/test/index.html`.

`GoldenTest` is the one place to check my answer. It asserts the whole ledger after Day 6, the whole outcome log, each day as it was known at its own close, the Day 6 interest, and the ledger at the end of Day 5 before any fee.

## How to read the report

The report replays the stream once per day, through that day. Each block shows the ledger as it was known that evening. This is Day 5:

```
=== Day 5 ===
Events    E7 POSTED, E8 DECLINED, E10 POSTED
ACC-001   closing AED -230.00, available AED -230.00
          restated: Day 2 AED 250.00 -> AED -395.00, Day 3 AED 650.00 -> AED 5.00, Day 4 AED 465.00 -> AED -205.00
          fees posted: AED 25.00 for Day 2, AED 25.00 for Day 4, AED 25.00 for Day 5
          interest today: AED 0.00 (provisional)
ACC-002   closing BHD 10.000, available BHD 10.000
          fees posted: none
          interest today: BHD 0.004 (provisional)
Auths     Auth-A SETTLED, Auth-B DECLINED
Errors    none
```

- **Events** lists every event posted that day with its outcome.
- **closing** is that day's closing ledger balance: every entry with a value date on or before the day. **available** is the closing balance less active holds.
- **restated** lists earlier days whose closing balance changed today, old value then new. It appears only when an entry arrives late with an early value date. Here E7, value-dated Day 2, and the fees it causes change Days 2 to 4.
- **fees posted** lists the fees posted this evening and the day each one is for, which is its value date.
- **interest today** is that day's accrual as known this evening. It is provisional because a late entry can still change it. On Day 6 it is replaced by the final schedule, the credit, and the closing balance before the credit.
- **Auths** gives the state of every authorisation so far: ACTIVE with its hold, SETTLED, or DECLINED. A decline is a state, not an error.
- **Errors** lists events rejected that day with the reason. Day 4 shows `E6 REJECTED: no active authorisation Auth-Z`.

## The other documents

- `REJECTED.md`: the acceptance criteria I refused, with figures.
- `AMBIGUITIES.md`: where the brief can be read more than one way, and what I chose.
- `NUMBERS.md`: every constant, and what changes if it is halved.
- `WORKLOG.md`: the timestamped log of the work.

## Code map

All in `io.mal.ledger`.

- `Money`: an amount at its currency's precision. The only place anything is rounded.
- `Account`, `Entry`, `Ledger`: the append-only ledger. A closing balance is a sum over value dates.
- `Event`, `Outcome`: the six kinds of event the replay handles, and what happened to each.
- `Replay`: walks the days, applies events, then closes each day with fees and, on Day 6, interest.
- `BriefStream`: the brief's two accounts and ten events.
- `DailyReport`, `Main`: the printed report.

## Toolchain

Built on JDK 25, compiled with `--release 21`. Runs on any JDK 21 or later.
