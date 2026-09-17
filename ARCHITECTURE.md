# Architecture and trade-offs

This document is about the ledger in this repository: what the build does today, where that stops working, and what I left out to stay in scope.

## 1. Append-only at scale

**What breaks first.** The evening fee check. Every evening, for every account it can charge, it goes back to Day 1 and tests each day in turn. For a day with no fee yet, which is nearly every day, that means reading the whole ledger twice: once to see whether the day already carries a fee, and once to add up its balance. The ledger is a single list shared by all accounts, so an account with ten entries still pays for everyone else's.

A hundred times the brief's events would not break this. A thousand events still replay in an instant. What breaks it is that the loop multiplies accounts by days by the size of the ledger, and all three grow together. Take 1,000 accounts, a year of history and ten entries per account per day. That is 3.65 million entries. By the end of the year each evening's check makes 730,000 reads of all of them, around 2.7 trillion entry visits. That is hours of work every evening, and more each day. I wrote the simplest loop that gives the right answer for six days, and the same habit shows elsewhere: finding active holds, finding the event a reversal names, and the daily report, which replays the stream once per day, all re-read everything.

**First move: split by account and work in parallel.** This is cheap because of something the model happens to guarantee: no event touches two accounts. There are no transfers. Balances, holds, fees and interest are all worked out one account at a time. So accounts can be dealt into groups, each with its own worker, and each account can keep its own entry list and outcome log. The groups never need to talk to each other. The only thing they share is the clock. Every group finishes a day's events before any group closes that day. My ledger's rule that posting days never go backwards would then hold within a group, and the shared clock keeps its purpose: a closed day takes no new postings.

With entries held per account, each read shrinks from 3.65 million entries to the 3,650 that belong to one account. The evening's work drops a thousandfold, to about 2.7 billion visits, spread across as many workers as there are groups.

**What splitting leaves behind.** It divides the work. It does not stop it growing. Three things in my design grow without limit.

- The entry list. This one is deliberate. An append-only ledger keeps everything, and a bank must retain it for years anyway.
- The outcome log, which keeps every declined and settled authorisation in the pile the next one searches.
- The set of open days. This is the one that hurts. No day is ever closed, so every evening re-tests every day since Day 1. Doubling an account's age quadruples its cost, however many workers there are.

**The cheapest structural change: a balance brought forward.** Nobody adds up every transaction since an account opened to find today's balance. A statement starts from the balance brought forward. My code already works this way in miniature: every balance starts from the account's opening balance and adds entries to it. A date on that opening balance is the hook. Sums start from the brought-forward figure, the evening check starts from its date, and older entries move to cheaper storage without being lost. With a figure brought forward 30 days ago, the same evening check costs about 18 million visits, and it stays there however old the account gets.

I would introduce it in two steps, because they cost different things.

- First as a convenience. If a late entry reaches back past the brought-forward date, the figure is discarded and rebuilt from the full log. No rule changes and no answer changes. The common case is fast and the rare case is slow.
- Then as a closed period. Once the bank agrees that nothing may be dated further back than, say, 30 days, the rare case disappears too. The price is that a genuinely old correction can no longer carry its true date. It has to be posted today as an explicit adjustment. That is a business decision, not a technical one, and section 2 returns to it.
