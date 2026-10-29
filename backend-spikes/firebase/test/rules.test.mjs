// Firestore Security Rules + offline/conflict behaviour, against the LOCAL EMULATOR only.
// Project "demo-brainrot": the demo- prefix means no real Firebase project or Google account is involved.
import { after, before, beforeEach, describe, test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  disableNetwork,
  doc,
  enableNetwork,
  getDoc,
  getDocFromCache,
  setDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
} from "firebase/firestore";

const ALICE = "alice-uid";
const BOB = "bob-uid";
let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-brainrot",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});
after(async () => env?.cleanup());
beforeEach(async () => env.clearFirestore());

// auth_time is in seconds, like a real Firebase ID token.
const as = (uid, authTime = 1_000) => env.authenticatedContext(uid, { auth_time: authTime }).firestore();

function event(id, overrides = {}) {
  return {
    id,
    habitId: "water",
    type: "QUANTITY",
    value: 1,
    eventTimeUtcMs: 1_761_620_400_000,
    zone: "Asia/Kolkata",
    createdAtUtcMs: 1_761_620_405_000,
    clientMutationId: `m-${id}`,
    deleted: false,
    ...overrides,
  };
}

async function seed(path, data) {
  await env.withSecurityRulesDisabled(async (ctx) => setDoc(doc(ctx.firestore(), path), data));
}

describe("user isolation", () => {
  test("owner can create and read own event", async () => {
    const db = as(ALICE);
    await assertSucceeds(setDoc(doc(db, `users/${ALICE}/events/e1`), event("e1")));
    await assertSucceeds(getDoc(doc(db, `users/${ALICE}/events/e1`)));
  });

  test("another signed-in user cannot read or write it", async () => {
    await seed(`users/${ALICE}/events/e1`, event("e1"));
    await seed(`users/${ALICE}/habits/water`, { id: "water", version: 1, target: 3 });
    const bob = as(BOB);
    await assertFails(getDoc(doc(bob, `users/${ALICE}/events/e1`)));
    await assertFails(getDoc(doc(bob, `users/${ALICE}/habits/water`)));
    await assertFails(setDoc(doc(bob, `users/${ALICE}/events/e2`), event("e2")));
  });

  test("signed-out access is refused", async () => {
    await seed(`users/${ALICE}/events/e1`, event("e1"));
    const anon = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(anon, `users/${ALICE}/events/e1`)));
    await assertFails(setDoc(doc(anon, `users/${ALICE}/events/e2`), event("e2")));
  });

  test("paths outside /users/{uid} are closed", async () => {
    await assertFails(setDoc(doc(as(ALICE), "shared/anything"), { a: 1 }));
  });
});

describe("append-only events", () => {
  test("malformed events are rejected", async () => {
    const db = as(ALICE);
    await assertFails(setDoc(doc(db, `users/${ALICE}/events/e1`), event("e1", { type: "MAGIC" })));
    await assertFails(setDoc(doc(db, `users/${ALICE}/events/e1`), event("other-id")));
    await assertFails(setDoc(doc(db, `users/${ALICE}/events/e1`), event("e1", { value: "3 L" })));
    const { zone, ...noZone } = event("e1");
    await assertFails(setDoc(doc(db, `users/${ALICE}/events/e1`), noZone));
  });

  test("a negative correction event is allowed (decrement)", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), `users/${ALICE}/events/e1`), event("e1", { value: -0.5 })));
  });

  test("events cannot be edited, only tombstoned; hard delete is refused", async () => {
    await seed(`users/${ALICE}/events/e1`, event("e1"));
    const db = as(ALICE);
    const ref = doc(db, `users/${ALICE}/events/e1`);
    await assertFails(updateDoc(ref, { value: 99 }));
    await assertFails(deleteDoc(ref));
    await assertSucceeds(updateDoc(ref, { deleted: true, deletedAtUtcMs: 1_761_700_000_000, updatedAtUtcMs: 1_761_700_000_000 }));
    await assertFails(updateDoc(ref, { deleted: false }));
  });
});

describe("habit settings: stale edits are conflicts, not silent overwrites", () => {
  test("version must advance by exactly one", async () => {
    await seed(`users/${ALICE}/habits/water`, { id: "water", version: 3, target: 3 });
    const ref = doc(as(ALICE), `users/${ALICE}/habits/water`);
    await assertSucceeds(setDoc(ref, { id: "water", version: 4, target: 2.5 }));
    await assertFails(setDoc(ref, { id: "water", version: 4, target: 2 }), "edit based on v3 after v4 exists");
  });
});

describe("₹0 'sign out other devices' (revokedBefore)", () => {
  test("tokens issued before revokedBefore are refused; newer sign-ins work", async () => {
    await seed(`users/${ALICE}`, { revokedBefore: 2_000 });
    await seed(`users/${ALICE}/events/e1`, event("e1"));
    await assertFails(getDoc(doc(as(ALICE, 1_500), `users/${ALICE}/events/e1`)), "lost phone, signed in at t=1500");
    await assertSucceeds(getDoc(doc(as(ALICE, 2_500), `users/${ALICE}/events/e1`)), "trusted device, re-signed in at t=2500");
  });

  test("a device can revoke everything older than its own sign-in, but not lock itself out", async () => {
    await seed(`users/${ALICE}`, { createdAt: 1 });
    const trusted = as(ALICE, 3_000);
    await assertFails(setDoc(doc(trusted, `users/${ALICE}`), { createdAt: 1, revokedBefore: 9_999 }));
    await assertSucceeds(setDoc(doc(trusted, `users/${ALICE}`), { createdAt: 1, revokedBefore: 3_000 }));
    await assertSucceeds(getDoc(doc(trusted, `users/${ALICE}`)));
  });

  test("the revoked device cannot undo the revocation", async () => {
    await seed(`users/${ALICE}`, { revokedBefore: 2_000 });
    await assertFails(setDoc(doc(as(ALICE, 1_500), `users/${ALICE}`), { revokedBefore: 0 }));
  });
});

describe("Firestore SDK offline behaviour (web SDK, memory cache, against the emulator)", () => {
  test("a write made offline is visible locally at once and reaches the server after reconnect", async () => {
    const phone = as(ALICE);
    const ref = doc(phone, `users/${ALICE}/events/offline1`);
    await disableNetwork(phone);
    const pending = setDoc(ref, event("offline1")); // resolves only when the server confirms
    const local = await getDocFromCache(ref);
    assert.equal(local.exists(), true, "visible locally while offline");
    assert.equal(local.metadata.hasPendingWrites, true, "flagged as not yet synced");

    const web = as(ALICE);
    await env.withSecurityRulesDisabled(async (ctx) => {
      const serverCopy = await getDoc(doc(ctx.firestore(), `users/${ALICE}/events/offline1`));
      assert.equal(serverCopy.exists(), false, "server has not seen it yet");
    });

    await enableNetwork(phone);
    await pending;
    const fromWeb = await getDoc(doc(web, `users/${ALICE}/events/offline1`));
    assert.equal(fromWeb.exists(), true, "second client sees it after sync");
  });

  test("two devices edit the same habit offline: first wins, second is rejected and rolled back locally", async () => {
    await seed(`users/${ALICE}/habits/water`, { id: "water", version: 1, target: 3 });
    const phone = as(ALICE);
    const laptop = as(ALICE);
    const phoneRef = doc(phone, `users/${ALICE}/habits/water`);
    const laptopRef = doc(laptop, `users/${ALICE}/habits/water`);
    await getDoc(phoneRef);
    await getDoc(laptopRef);

    await disableNetwork(phone);
    await disableNetwork(laptop);
    const phoneWrite = setDoc(phoneRef, { id: "water", version: 2, target: 2.5 });
    const laptopWrite = setDoc(laptopRef, { id: "water", version: 2, target: 4 });

    await enableNetwork(phone);
    await phoneWrite;
    await enableNetwork(laptop);
    await assert.rejects(laptopWrite, /permission|PERMISSION_DENIED/i, "stale laptop edit must be refused");

    // The laptop's optimistic local value is rolled back to the server's.
    const settled = await new Promise((resolve) => {
      const stop = onSnapshot(laptopRef, (snap) => {
        if (!snap.metadata.hasPendingWrites && snap.data()?.target === 2.5) {
          stop();
          resolve(snap.data());
        }
      });
    });
    assert.deepEqual(settled, { id: "water", version: 2, target: 2.5 });
  });

  test("append-only events from two offline devices never conflict: both survive", async () => {
    const phone = as(ALICE);
    const laptop = as(ALICE);
    await disableNetwork(phone);
    await disableNetwork(laptop);
    const a = setDoc(doc(phone, `users/${ALICE}/events/p1`), event("p1", { value: 1 }));
    const b = setDoc(doc(laptop, `users/${ALICE}/events/l1`), event("l1", { value: 1 }));
    await enableNetwork(laptop);
    await enableNetwork(phone);
    await Promise.all([a, b]);
    const reader = as(ALICE);
    assert.equal((await getDoc(doc(reader, `users/${ALICE}/events/p1`))).exists(), true);
    assert.equal((await getDoc(doc(reader, `users/${ALICE}/events/l1`))).exists(), true);
  });
});
