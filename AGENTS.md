# Project Agent Instructions

## Model routing preference

Optimize model usage dynamically for each task instead of using the most expensive model by default.

- Use `gpt-5.6-luna` with low reasoning for simple checks, repetitive edits, Git inspection, and routine documentation.
- Use `gpt-5.6-terra` with medium reasoning by default for normal implementation, testing, and debugging.
- Use `gpt-5.6-sol` only for complex architecture, difficult debugging, high-risk changes, or final review where its additional capability is justified.
- Increase reasoning effort only when task complexity requires it.
- When a running primary session cannot change models directly, delegate a clearly bounded task to an appropriately selected model when delegation is available and useful.
- Prefer the lowest-cost model and reasoning effort that can complete the work reliably.

This preference should survive new sessions and context resets.
