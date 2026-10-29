# Claude Code project skills

Third-party [Agent Skills](https://code.claude.com/docs/en/skills) vendored for Brain Rot 2.0.
Claude Code loads every `.claude/skills/<name>/SKILL.md` in this repository as a project skill.

Only the skill folders listed below are included. No plugins, hooks, scripts on the
execution path, permission settings or application code come with them. Files are copied
unmodified from the pinned upstream commits; the only additions are the license files and
this README.

| Skill | Source repository | Pinned commit | Upstream path | License |
|---|---|---|---|---|
| `glance-appwidget-updates-and-memory` | [android/skills](https://github.com/android/skills) | `6daa53ef27fa2aa87827d183a85c06cedceca833` | `system/glance-appwidget-updates-and-memory/` | Apache-2.0 (`LICENSE.txt`) |
| `edge-to-edge` | [android/skills](https://github.com/android/skills) | `6daa53ef27fa2aa87827d183a85c06cedceca833` | `system/edge-to-edge/` | Apache-2.0 (`LICENSE.txt`) |
| `systematic-debugging` | [obra/superpowers](https://github.com/obra/superpowers) | `2ce8aa15d05bd57213e6f746f953aafb7ded7a8b` | `skills/systematic-debugging/` | MIT (`LICENSE`) |

## Skills

### `glance-appwidget-updates-and-memory`

Guidance for Jetpack Glance / AppWidget state updates, background refresh with WorkManager,
and RemoteViews bitmap memory limits on Android 17 (API 37). Relevant to the Brain Rot home
screen heatmap widget. `references/` holds upstream sample code and copies of Android
documentation pages; they are reference material for Claude, not part of the app build.

### `edge-to-edge`

Guidance for adaptive edge-to-edge layouts in Jetpack Compose: system bar and IME insets,
content obscured by the navigation or status bar, and system bar legibility.

### `systematic-debugging`

A four-phase root-cause debugging process (investigate, analyse patterns, test a hypothesis,
then fix) with supporting notes on root-cause tracing, defence in depth and condition-based
waiting.

Excluded from upstream, as agreed during review:

- `test-academic.md`, `test-pressure-1.md`, `test-pressure-2.md`, `test-pressure-3.md`:
  the author's internal test scenarios for the skill.
- `find-polluter.sh`: a shell script. `root-cause-tracing.md` still mentions it, unmodified
  from upstream; that reference points to a file that is intentionally not vendored here.

## Licenses

- The two `android/skills` folders each carry a copy of that repository's `LICENSE.txt`
  (Apache License 2.0), which their `SKILL.md` frontmatter refers to.
- `systematic-debugging/LICENSE` is the `obra/superpowers` MIT license (Copyright (c) 2025
  Jesse Vincent).

## Updating

To update a skill, copy its folder again from a newer upstream commit after reviewing the
diff, keep the exclusions above, and update the pinned commit in this table.
