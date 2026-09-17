# Working agreement

- Never commit or stage. Make the change, run the tests, stop. Ahmed reviews and commits.
- No comments in code. The single exception is the intentionally failing test, which the brief requires to be inline-annotated.
- Java 21 API only (`--release 21`). Plain Java, JUnit 5, no frameworks.
- One slice per request: the smallest change that delivers it, its test, and a one-line WORKLOG entry. Then stop.
- Do not add classes, fields, or methods the current slice does not use.
- Decisions live in AMBIGUITIES.md and REJECTED.md. If a slice needs a decision not recorded there, ask before choosing.
- Amounts are `Money`, never raw `BigDecimal` or `double`, once `Money` exists.
