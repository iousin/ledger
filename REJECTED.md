# REJECTED

The brief lists eight acceptance criteria and says some are wrong. I number them C1 to C8 in the order the brief gives them. I refuse four and accept four. Each refusal names the rule it breaks and shows the figures. Approaches abandoned during the build are recorded at the end.

All figures are for ACC-001 in AED unless stated.

## Criteria refused

### C2. "E7 causes exactly one overdraft fee to be assessed, on Day 2."

Refused. E7 causes three fees, for Day 2, Day 4 and Day 5.

E7 is posted on Day 5 with value date Day 2, so it is part of the closing ledger balance of every day from Day 2 onward. The fee rule applies to each of those days.

| Day | Closing after E7, before fees | Closing as the fee pass reaches it | Fee | Closing after fee |
|---|---|---|---|---|
| 2 | -370.00 | -370.00 | yes | -395.00 |
| 3 | 30.00 | 5.00 | no | 5.00 |
| 4 | -155.00 | -180.00 | yes | -205.00 |
| 5 | -155.00 | -205.00 | yes | -230.00 |

Day 4 and Day 5 are already negative at -155.00 before any new fee is counted, and Day 3 is positive either way. So the count of three does not depend on whether the fee pass lets the Day 2 fee flow into later days.

The brief books a fee "with value_date equal to the day assessed". I read "the day assessed" as the day whose closing balance is tested. The fee for Day 2 therefore carries value date Day 2, although it is posted on Day 5 when E7 arrives. The criterion uses the phrase the same way: a fee "on Day 2" caused by an event on Day 5. The reasoning is in AMBIGUITIES.md.

### C6. "After E9, all balances and fees return to their pre-E7 values."

Refused. The ledger is append-only. E9 is a new credit of 620.00 with value date Day 2. E7 stays in the ledger and so do the three fees.

The fees were posted on Day 5 against the ledger as it stood that evening. A posted entry stands until a business event reverses it, and the stream contains no fee refund.

| Day | Before E7 | After E9 | Difference |
|---|---|---|---|
| 1 | 250.00 | 250.00 | 0.00 |
| 2 | 250.00 | 225.00 | 25.00 |
| 3 | 650.00 | 625.00 | 25.00 |
| 4 | 465.00 | 415.00 | 50.00 |
| 5 | 465.00 | 390.00 | 75.00 |

Fees before E7: none. Fees after E9: three, 75.00 in total.

Read literally, the criterion needs records to disappear, which the append-only rule forbids. Read generously, it needs a compensating fee refund. I considered that and deferred it. Whether a reversal should refund the fees it caused depends on why the debit was reversed. A bank error should be refunded. A merchant refund need not be. The event carries no reason, so the ledger has no basis to choose. The worked alternative is in AMBIGUITIES.md.

### C7. "The three BHD instalments in E10 must each be BHD 3.334."

Refused. Three instalments of 3.334 sum to 10.002. That credits the customer 0.002 BHD that E10 never contained.

10.000 divided by 3 cannot be represented at three decimal places. I post 3.334, 3.333 and 3.333, which sum to exactly 10.000. The 0.001 remainder goes to the first instalment. "Equal" is honoured as far as the precision rule allows: no two instalments differ by more than 0.001.

### C8. "If the rounded daily interest accruals do not sum to the capitalized total, the remainder is discarded."

Refused. It contradicts the rule that the rounded daily accruals must sum exactly to the capitalised total.

The criterion assumes the total is computed some other way, then compared with the daily figures, with any gap thrown away. I define the capitalised total as the sum of the rounded daily accruals, so a gap cannot exist.

The daily figures use the closing balances after E9, shown in the C6 table. Why interest follows the final balances is in AMBIGUITIES.md.

| | D1 | D2 | D3 | D4 | D5 | D6 | Total |
|---|---|---|---|---|---|---|---|
| Unrounded | 0.100 | 0.090 | 0.250 | 0.166 | 0.156 | 0.156 | 0.918 |
| Rounded | 0.10 | 0.09 | 0.25 | 0.17 | 0.16 | 0.16 | 0.93 |

Rounding the unrounded total gives 0.92. The capitalised credit is 0.93. The 0.01 between them is what the criterion would discard. A ledger does not discard money. An amount is either posted or it is not.

## Criteria accepted

| | Criterion | Why it holds |
|---|---|---|
| C1 | Day 2 closing at end of Day 5, before fees, is -370.00 | 250.00 less 620.00 |
| C3 | The Day 4 settlement of Auth-A must be accepted | Auth-A is approved and not yet settled. Debit 185.00, hold released in full |
| C4 | A settlement for an unknown authorisation is rejected and no funds move | E6 posts no entry and is reported as an error |
| C5 | If Auth-B is approved, its hold reduces available but not ledger balance | True as a statement of the hold rule. In this stream Auth-B is declined: available is -155.00 before the hold is applied. See AMBIGUITIES.md |

## Approaches abandoned

None yet. Entries are added during the build as they happen.
