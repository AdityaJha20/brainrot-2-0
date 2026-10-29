# Shared scoring fixtures (Phase 0 proposal: `scoring-v0-proposal`)

These JSON files are the **single contract** that both scoring implementations must pass:

- Kotlin: `core/` (used by the Android app), run with `./gradlew :core:test`
- TypeScript: `web/scoring/` (used by the web companion), run with `npm test` in that folder

If a fixture passes in one language and fails in the other, the implementations have drifted. CI runs both.

> **Status: proposal, not approved.** The handoff (section 4) says the scoring *principles* are confirmed
> but the exact weights and formula are still open. This folder implements the handoff's **proposed
> baseline**. Every open decision is a named **policy parameter** with a placeholder default, so nothing
> is locked in. Changing a default is a product decision and needs approval (see
> `docs/phase0/REPORT.md`, "Decisions that need your approval").

## What is calculated (kept separate, as the handoff requires)

| Metric | Meaning in v0 |
|---|---|
| `recorded` | Sum of non-deleted quantity/duration events for that habit on that local day. Never below 0. Never invented. |
| `progress` | 0..1. Binary: 1 if a completion event exists. Quantity/duration: `min(max(recorded / target, 0), 1)`, or 1 if an explicit completion event exists (the "one-tap complete" case; `recorded` is **not** changed, so no quantity is fabricated). |
| `status` | `NOT_SCHEDULED`, `REST`, `SKIPPED`, `COMPLETED`, `PARTIAL`, `MISSED`, `UNKNOWN`; see below. |
| `dailyCompletion` | `completed / eligible`, where eligible = scheduled and not REST/SKIPPED. `ratio` is `null` when nothing is eligible. Unweighted. |
| `dailyScore` | `100 × Σ(importance × progress) / Σ(importance)` over eligible habits. `null` when nothing is eligible. Capped at 100 by construction (progress ≤ 1). Never negative. |
| `scoreIsProvisional` | `true` when the day has not been reviewed/closed, so the score may still change. |
| `streak` | Consecutive **confirmed** successful scheduled occurrences ending on the evaluated date. |
| `streakPendingReview` | `true` when the streak count stopped at an unreviewed (`UNKNOWN`/`PARTIAL`) day rather than a confirmed `MISSED` day. |
| `heatmapLevel` | 0–4. 0 = nothing eligible, or no non-deleted events at all that day; 4 = perfect day only; 1–3 = score bins. |

### Status rules

1. Day outside the habit's `startDate..endDate`, or weekday not in schedule → `NOT_SCHEDULED`.
2. A `REST` or `SKIPPED` day mark → that status (not eligible).
3. `progress ≥ successThreshold` → `COMPLETED`.
4. Otherwise, if the day is **closed** (listed in `reviewedDates`) → `MISSED`. Only a reviewed day can be a miss.
5. Otherwise, if any event exists for that habit/day → `PARTIAL`.
6. Otherwise → `UNKNOWN` (missing data is not treated as a miss).

### Streak rules

Walk backwards from the evaluated date, one local day at a time, stopping at the habit's `startDate`:

- `COMPLETED` → +1, continue.
- `NOT_SCHEDULED`, `REST`, `SKIPPED` → ignore, continue (rest days never break a streak).
- The evaluated date itself, when not completed and not closed → ignore (today is still in progress).
- `MISSED` → stop.
- `UNKNOWN` or `PARTIAL` on an earlier, unreviewed day → stop, and set `streakPendingReview = true`
  (we neither count it as success nor call it a failure).

### Which day does an event belong to?

`eventTime` is stored in UTC with the `zone` the user was in when it happened. The local day is
`(eventTime converted to zone) − dayStartHour hours`, then take the date. `createdAt` (when it was typed)
never decides the day, so backdated entries land on the day they describe.

## Policy parameters (all placeholders; open decisions in the handoff)

| Parameter | Default | Open decision it stands in for |
|---|---|---|
| `successThreshold` | `1.0` | Streak/partial threshold (handoff open decision 23) |
| `dayStartHour` | `0` | After-midnight / day closure behaviour (open decision 26) |
| day assignment | event's own `zone` | Travel behaviour (open decision 26): v0 uses the zone recorded on each event, not a fixed home zone |
| `heatmapBins` | `[34, 67]` | Heatmap bin edges: score < 34 → level 1, < 67 → 2, otherwise 3 |
| `perfectDay` | `"ALL_ELIGIBLE_COMPLETED"` | Perfect-day definition (open decision 22) |

Not covered in v0 (deliberately left out until decided): weekly-target schedules ("3× per week"),
quality ratings, combined measurements, effort modifiers, grace days/streak freezes, automatic day closure.

## Fixture format

```jsonc
{
  "id": "quantity-partial",
  "description": "Human explanation",
  "policy": { "dayStartHour": 0 },            // optional overrides of the defaults above
  "habits": [{
    "id": "water", "measurement": "QUANTITY",  // BINARY | QUANTITY | DURATION
    "target": 3, "unit": "L", "importance": 1,
    "schedule": { "type": "DAILY" },           // or { "type": "DAYS_OF_WEEK", "days": [1,2,3,4,5] } (1 = Monday)
    "startDate": "2026-10-01", "endDate": null
  }],
  "events": [{
    "id": "e1", "habitId": "water",
    "type": "QUANTITY",                        // QUANTITY | DURATION | COMPLETION
    "value": 1.5,
    "eventTime": "2026-10-28T03:00:00Z", "zone": "Asia/Kolkata",
    "createdAt": "2026-10-28T03:00:05Z", "deleted": false
  }],
  "dayMarks": [{ "habitId": "water", "date": "2026-10-26", "mark": "REST" }],
  "reviewedDates": ["2026-10-27"],
  "evaluateDate": "2026-10-28",
  "expected": {
    "habits": { "water": { "status": "PARTIAL", "recorded": 1.5, "progress": 0.5,
                           "streak": 0, "streakPendingReview": false } },
    "dailyCompletion": { "completed": 0, "eligible": 1, "ratio": 0 },
    "dailyScore": 50, "scoreIsProvisional": true, "heatmapLevel": 2
  }
}
```

Numbers are compared with a tolerance of `1e-9`. Fields omitted from `expected.habits.<id>` are not checked.
