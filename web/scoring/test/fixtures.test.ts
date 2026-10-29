// Runs every shared fixture in shared-fixtures/scoring against the TypeScript engine.
import { test } from "node:test";
import assert from "node:assert/strict";
import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { ScoringEngine } from "../src/scoring.ts";

const FIXTURE_DIR = join(import.meta.dirname, "..", "..", "..", "shared-fixtures", "scoring");
const TOLERANCE = 1e-9;

function assertClose(actual: unknown, expected: unknown, path: string): void {
  if (typeof expected === "number" && typeof actual === "number") {
    assert.ok(Math.abs(actual - expected) <= TOLERANCE, `${path}: expected ${expected}, got ${actual}`);
  } else {
    assert.deepEqual(actual, expected, path);
  }
}

const files = readdirSync(FIXTURE_DIR).filter((f) => f.endsWith(".json")).sort();
assert.ok(files.length > 0, "no fixtures found");

for (const file of files) {
  const fixture = JSON.parse(readFileSync(join(FIXTURE_DIR, file), "utf8"));
  test(fixture.id, () => {
    const result = new ScoringEngine(fixture).evaluate(fixture.evaluateDate);
    const exp = fixture.expected;
    for (const [habitId, fields] of Object.entries<Record<string, unknown>>(exp.habits)) {
      const actual = result.habits[habitId];
      assert.ok(actual, `missing habit ${habitId}`);
      for (const [key, value] of Object.entries(fields)) {
        assertClose(actual[key as keyof typeof actual], value, `${habitId}.${key}`);
      }
    }
    assert.equal(result.dailyCompletion.completed, exp.dailyCompletion.completed, "dailyCompletion.completed");
    assert.equal(result.dailyCompletion.eligible, exp.dailyCompletion.eligible, "dailyCompletion.eligible");
    assertClose(result.dailyCompletion.ratio, exp.dailyCompletion.ratio, "dailyCompletion.ratio");
    assertClose(result.dailyScore, exp.dailyScore, "dailyScore");
    assert.equal(result.scoreIsProvisional, exp.scoreIsProvisional, "scoreIsProvisional");
    assert.equal(result.heatmapLevel, exp.heatmapLevel, "heatmapLevel");
  });
}
