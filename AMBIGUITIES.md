# AMBIGUITIES

Places where the brief can be read more than one way, what I chose, and why. Criteria are numbered C1 to C8 as in REJECTED.md. Figures are ACC-001 in AED unless stated.

One idea runs through most of these. An entry can arrive late with an early value date, so a day's closing balance can change after the day is over. My rule: anything already posted stands, and anything not yet posted follows the ledger.

## 1. Overdraft fee

**Which day does a late fee belong to?** The brief books a fee "with value_date equal to the day assessed". E7 arrives on Day 5 and makes Day 2 negative. The fee could be dated Day 2, the day that went negative, or Day 5, the day it was found.

I date it Day 2. C2 itself talks about a fee "on Day 2" caused by an event on Day 5. C1 checks Day 2's balance at the end of Day 5 "before any fee is assessed", so the brief expects earlier days to be re-checked. Dating all three fees Day 5 would also put three fees on one day, against "once per day", which I read as one fee for each day that closes negative.

**Does reversing E7 refund the fees?** C6 expects it. I decided no. I worked out the alternative: post a 25.00 refund for each day that has a fee and is no longer negative. That gives three refunds on Day 6, closings back at 250.00, 250.00, 650.00, 465.00, 465.00, 465.00, interest of 1.03 and Day 6 at 466.03 after the interest credit. I deferred it because whether to refund depends on why the debit was reversed. A bank error should be refunded, a merchant refund need not be, and the event does not say which. The risk is covered in the architecture document.

## 2. Interest and rounding

**Does a late entry change earlier days' interest?** The rule says interest is "on the closing ledger balance" but not when that balance is read.

- Read each evening and never again: 0.10, 0.10, 0.26, 0.19, 0.00, 0.16. Total 0.81.
- Read when the interest is actually posted, at the end of Day 6: 0.10, 0.09, 0.25, 0.17, 0.16, 0.16. Total 0.93.

I use the second. No interest is posted until the single credit on Day 6, and C1 shows the brief itself re-reads an earlier day's closing balance. So Day 5, which stood at -230.00 that evening, earns 0.16. Once E9 is posted with value date Day 2, Day 5's closing balance is 390.00. The Day 5 fee still stands because it was already posted. Once the interest credit is posted, it stands too.

**End of Day 6.** Fees first, then Day 6's interest on the balance before the credit, then the credit dated Day 6. Day 6 is 390.00 before and 390.93 after. The credit earns no interest on itself.

**"Positive balances only."** Zero and negative balances earn nothing, and no interest is charged on an overdraft.

**Rounding.** Each day's interest is rounded half up to the currency's decimals, then the rounded figures are added, giving 0.93. Adding first and rounding after gives 0.92, which is why C8 is refused. No figure in this stream falls exactly on a half, so the rounding mode changes nothing here.

**"Three equal instalments."** BHD 10.000 does not divide by three at three decimals. I post 3.334, 3.333 and 3.333. The spare 0.001 goes on the first.

## 3. Authorisation and settlement

**Is Auth-B approved?** No. Available balance is today's ledger balance as it stands when the event arrives, less active holds. At E8 that is -155.00 with no holds, so Auth-B is declined before its 90.00 is even applied. After the fee pass it would be -230.00 and still declined. The brief's remark that Auth-B "is never settled" and the wording of C5 suggest an open hold was expected. There is none. C5 is accepted as a true rule whose "if" never happens.

**Settlement below the hold.** E5 settles 185.00 against a 200.00 hold. I debit 185.00 and release the whole hold, so the spare 15.00 becomes available again. A settlement above its hold never happens in the stream. The same rule would debit it in full, since a hold reserves funds and does not cap the charge.

**Value date on an authorisation.** E3 and E8 each carry a value date equal to their posting day, so it never changes anything here. A hold is not a ledger entry. It takes effect when approved and ends when settled. The settlement carries its own value date, and that is what dates the debit.

## 4. Declines and rejections

Neither moves money or posts a ledger entry. Both stay in the event log with their outcome, and the log is never edited. In the report Auth-B shows under authorisation states as DECLINED. E6 shows under errors, because there is no authorisation for it to be a state of. A reversal that names an unknown event would be rejected the same way as E6.
