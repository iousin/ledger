# NUMBERS

Every constant in the build: where it comes from, why that value, and what happens if it is halved. Figures are ACC-001 in AED unless stated.

## Given by the brief

I did not choose these, so I cannot justify the value. What I can show is what each one drives.

**Overdraft fee, AED 25.00.** Halved to 12.50, nothing structural changes: E7 still causes three fees, for Days 2, 4 and 5. The sensitive direction is upward. Day 3 sits at 30.00 before fees, so any fee above 30.00 tips Day 3 negative and a fourth fee appears. Halving does have one side effect: two interest accruals become exact halves, so my rounding mode would start to move money.

**Interest, 0.04% a day.** Held as the exact decimal 0.0004, never as 0.04 divided by 100 in floating point. It is about 14.6% a year simple, which is high for a deposit rate, but it is the brief's figure. Halved to 0.02%, two accruals land on exact halves, 0.045 and 0.125. Rounding half up gives a credit of 0.47. Rounding half even would give 0.45. At the given rate no accrual is a half and both modes give 0.93.

**Decimals, 2 for AED and 3 for BHD.** These are the ISO 4217 minor units, read from the JDK's currency data. They are facts about the currencies, not settings, so halving them means nothing.

**Six days, with the interest credit at the end of Day 6.** The code holds these as two separate constants that happen to be equal: the last day of the brief's stream, and the day interest is credited. A shorter replay never reaches the credit. Halved, the credit would post on Day 3 as 0.46, before E7 arrives, and nothing after Day 3 would earn interest inside the window.

**Three instalments.** Three is what makes E10 a test. Split in two, BHD 10.000 is 5.000 and 5.000 with nothing left over, and the question behind C7 never arises.

**Opening balances of zero.** An account's closing balance starts from its opening balance, so zero means the ledger entries are the whole story.

## Chosen by me

**Rounding half up.** It is what a customer expects on a statement. No figure in the brief's stream is an exact half, so the choice moves no money here. The two entries above show when it would.

**Round once, with no working precision.** Interest is multiplied exactly and rounded one time, to the currency's decimals. 465.00 at 0.0004 is exactly 0.186000, then 0.19. There is no intermediate precision constant to choose, and that absence is deliberate. Amounts given with too many decimals are refused, not rounded.

**The spare instalment unit goes on the first part.** 3.334, 3.333, 3.333. Putting it last would be just as valid. No balance depends on it, because all three share a value date.

**The fee check looks back to Day 1.** Every evening it tests every day so far. Halved to a three-day look-back, the Day 5 check would test only Days 3 to 5, charge Days 4 and 5, and never charge Day 2 at -370.00. In production this becomes a limit on how far back a value date may reach, which the architecture document takes up.

**Report label width, 10 characters.** Cosmetic. It fits "ACC-001" with room to spare.

## Chosen for tests

Each value is the smallest or simplest one that triggers the behaviour.

| Value | Test | Why that value | Halved |
|---|---|---|---|
| 112.50 at 0.04% | Money, an exact half rounds up | Gives exactly 0.045, the only test that pins the rounding mode. Half even would give 0.04 | 56.25 gives 0.0225, which rounds to 0.02 either way and pins nothing |
| AED 0.05 in 3 parts | Money, a spare of two | 0.02, 0.02, 0.01. The smallest amount that leaves two spare units without a zero part. The BHD case only leaves one | Not representable at two decimals |
| Holds of 250.00 and 250.01 | Authorisation boundary | Exactly the available balance is approved. One minor unit more is declined | Both would be approved, testing nothing |
| Holds of 50.01 and 50.00 | Holds stack | The same boundary with Auth-A's 200.00 already held | Both approved |
| Settlement of 210.00 | Settlement above its 200.00 hold | Above the hold, yet leaves the balance positive at 440.00, so no fee muddies the test | 105.00 is below the hold and tests nothing new |
| 100.00, then 110.00 back-valued to Day 3, then 30.00 | Fee cascade | Day 3 becomes -10.00 and Day 4 is +20.00 before Day 3's fee. The test only works while Day 4 is above zero and below one fee of 25.00 | A 15.00 credit leaves Day 4 at +5.00 and still works. A credit of 35.00 or more breaks it |
| 100.00 against 150.00, and against 100.00 | Charged once per negative day, and zero is not negative | -50.00 is charged. Exactly 0.00 is not | 75.00 against 100.00 never goes negative |
| BHD 10.000 against 15.000 | An account not held in AED is never charged | Any negative BHD balance would do | Still negative, same result |
| Posting days 7 and 0 | Replay and report refuse the stream | One day either side of the window | Day 3 is inside the window and is accepted |
