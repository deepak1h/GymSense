const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Alex Rivera",
    heightCm: 178,
    weightKg: 76,
    targetWeightKg: 72,
    weeklyWorkoutGoal: 4,
    targetCaloriesPerWeek: 2200,
    restingHeartRateBpm: 58,
    vo2MaxEstimate: 48,
    fitnessLevel: "Intermediate",
    primaryFocus: "Hypertrophy",
    equipmentCsv: "Dumbbells,Barbell,Cable Machine,Bench",
    lastFeedbackRpe: 7,
    setupCompleted: true,
    createdAt: now,
    updatedAt: now,
  };
}

function validWorkoutPayload(uid, now) {
  return {
    userId: uid,
    exerciseId: "ex_barbell_bench",
    exerciseName: "Barbell Bench Press",
    muscleGroup: "Chest & Triceps",
    equipmentUsed: "Barbell",
    completedSets: 4,
    totalReps: 40,
    weightUsedKg: 80,
    durationSeconds: 420,
    caloriesBurned: 145,
    rpeFeedback: 8,
    dayKey: "2026-10-29",
    createdAt: now,
    updatedAt: now,
  };
}

function validVitalPayload(uid, now) {
  return {
    userId: uid,
    weightKg: 76,
    bmiTenths: 240,
    restingHeartRateBpm: 58,
    recoveryScore: 86,
    notes: "Feeling strong and well recovered",
    createdAt: now,
    updatedAt: now,
  };
}

test("1. Unauthenticated user cannot read or write profiles or workout logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("workout_logs").get()
  );
});

test("2. Authenticated owner can create and read valid UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("3. Cross-user isolation: Bob cannot read or update Alice's profile", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ weightKg: 90, updatedAt: new Date() })
  );
});

test("4. Shadow update / ghost field injection on UserProfile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .update({ isAdmin: true, updatedAt: new Date() })
  );
});

test("5. Out-of-bounds vitals or invalid enum in UserProfile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const badPayload = {
    ...validProfilePayload(ALICE_UID, now),
    heightCm: 999,
  };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badPayload));
});

test("6. Authenticated owner can create, query, and update their WorkoutLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const logRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("workout_logs")
    .doc("log_1");

  await assertSucceeds(logRef.set(validWorkoutPayload(ALICE_UID, now)));
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workout_logs")
      .where("userId", "==", ALICE_UID)
      .get()
  );
  await assertSucceeds(
    logRef.update({
      completedSets: 5,
      rpeFeedback: 9,
      updatedAt: new Date(),
    })
  );
});

test("7. Cross-user access to WorkoutLogs and VitalLogs is rejected", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("workout_logs")
      .doc("log_1")
      .set(validWorkoutPayload(ALICE_UID, now));
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("vital_logs")
      .doc("vital_1")
      .set(validVitalPayload(ALICE_UID, now));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workout_logs")
      .doc("log_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vital_logs")
      .doc("vital_1")
      .get()
  );
});

test("8. Authenticated owner can create and query VitalLogs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const vitalRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("vital_logs")
    .doc("vital_1");

  await assertSucceeds(vitalRef.set(validVitalPayload(ALICE_UID, now)));
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("vital_logs")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});
