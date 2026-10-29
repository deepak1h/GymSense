# Security Specification — Kinesis BioLab

## 1. Data Invariants
1. **Strict Multi-Tenant Ownership**: Every document under `/users/{userId}`, `/users/{userId}/workout_logs/{logId}`, and `/users/{userId}/vital_logs/{vitalId}` is strictly owned by `userId`. Only an authenticated user where `request.auth.uid == userId` and `incoming().userId == request.auth.uid` can read, create, update, or delete their documents.
2. **Path Variable Hardening (`isValidId`)**: All single-document operations (`get`, `create`, `update`, `delete`) validate document IDs against `^[a-zA-Z0-9_\-]+$` with max length 128.
3. **Immutable Audit Fields**: `userId` and `createdAt` cannot be modified on `update`.
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be valid Firestore `timestamp` objects `<= request.time`.
5. **Strict Schema & Bounds Validation**: Every `create` and `update` invokes `isValidUserProfile`, `isValidWorkoutLog`, or `isValidVitalLog` with explicit `.keys().hasAll(...)` and `.keys().hasOnly(...)` / `.affectedKeys().hasOnly(...)`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Profile Read**: `unauthDb.doc("users/alice").get()` -> Reject (`request.auth != null`).
2. **Cross-User Profile Read (PII/Vitals Leak)**: `bobDb.doc("users/alice").get()` -> Reject (`request.auth.uid == userId`).
3. **Identity Spoofing on Create**: `aliceDb.doc("users/alice").set({ userId: "bob", ... })` -> Reject (`incoming().userId == request.auth.uid`).
4. **Shadow Update / Ghost Field Injection**: `aliceDb.doc("users/alice").update({ isAdmin: true })` -> Reject (`affectedKeys().hasOnly(...)`).
5. **Immutable `createdAt` Tampering**: `aliceDb.doc("users/alice").update({ createdAt: newTimestamp })` -> Reject (`incoming().createdAt == existing().createdAt`).
6. **Immutable `userId` Reassignment**: `aliceDb.doc("users/alice").update({ userId: "bob" })` -> Reject (`incoming().userId == existing().userId`).
7. **Out-of-Bounds Vitals Poisoning**: `aliceDb.doc("users/alice").set({ heightCm: 9999, weightKg: -5, ... })` -> Reject (bounds check in `isValidUserProfile`).
8. **String Overflow (Denial of Wallet)**: `aliceDb.doc("users/alice").set({ displayName: "A".repeat(5000), ... })` -> Reject (`size() <= 80`).
9. **Invalid Enum Value**: `aliceDb.doc("users/alice").set({ fitnessLevel: "GodMode", ... })` -> Reject (enum check).
10. **Cross-User Workout Log List Scraping**: `bobDb.collection("users/alice/workout_logs").get()` -> Reject (`request.auth.uid == userId`).
11. **Workout Log Negative Reps/Sets**: `aliceDb.doc("users/alice/workout_logs/log1").set({ completedSets: 0, totalReps: -10, ... })` -> Reject (`isValidWorkoutLog`).
12. **Vital Log Future Timestamp Forgery**: `aliceDb.doc("users/alice/vital_logs/v1").set({ createdAt: futureTimestamp, ... })` -> Reject (`createdAt <= request.time`).
