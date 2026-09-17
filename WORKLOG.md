# WORKLOG

Entry format: `YYYY-MM-DD HH:MM GST` followed by what happened, what I was thinking, and which tools I used. Written at the time, not reconstructed.

## 2026-09-17

00:55 Session 1 start. Slice 0, build skeleton. Design was done on 17 Sep in a chat with Claude, walked through each rule, built the expected six-day table by hand, identified four acceptance criteria to reject and a first list of ambiguities. Working agreement for the build: Claude drafts each slice, I review and commit, no comments in code except the annotated failing test the brief asks for.

15:35 Slice 1, REJECTED.md first draft. Went through all eight criteria with Claude Code against a throwaway Python replay of the stream: four refused (C2, C6, C7, C8), four accepted. Decisions taken on the way: fees stand after E9 and an automatic refund is deferred, the interest schedule is computed from the ledger as it stands at capitalisation, and "the day assessed" is read as the day whose balance is tested. Abandoned-approaches section left as a stub until the build produces a real one.

16:12 Slice 2, AMBIGUITIES.md first draft. Claude's first version listed eighteen entries with tables. I cut it to four plain sections: overdraft fee, interest and rounding, authorisation and settlement, declines and rejections. Dropped the alternative fee-date readings, the fee pass timing and the "once per day" entry as more than I need to carry, kept Auth-B being declined after first cutting it, and reworded the authorisation value date entry because "ignored" was the wrong word. One sentence in REJECTED.md C2 that pointed at the dropped readings was shortened to match.

16:30 Cross-checked REJECTED.md against AMBIGUITIES.md with Claude. Figures and quotations all agreed. Six fixes to the reasoning and wording: C3 no longer implies the hold is a cap, "once per day" now says how I read it, C8 says its interest figures use the balances after E9, "booked" became "posted" in two places, the Auth-B test reads the balance when the event arrives rather than at close, and two labels were tightened.
