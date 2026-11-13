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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validEcuMap(uid) {
  const pastNow = new Date(Date.now() - 2000);
  return {
    userId: uid,
    mapName: "Vario 160 Drag 201m",
    ecuModelId: "vario160-k2s",
    ecuName: "Honda Vario 160 eSP+",
    partNumber: "30400-K2S-N01",
    strategyName: "Drag Race",
    revLimiterRpm: 11800,
    launchControlRpm: 5200,
    targetAfr: 12.8,
    fuelVeValues: Array(256).fill(100.0),
    ignitionValues: Array(256).fill(18.5),
    checksumHex: "A4F90C12",
    offImmobilizer: false,
    offSideStand: true,
    offO2Sensor: true,
    offEctCut: false,
    notes: "Bore up 63mm klep standar",
    createdAt: pastNow,
    updatedAt: pastNow,
  };
}

function validDynoRun(uid) {
  return {
    userId: uid,
    ecuModelId: "vario160-k2s",
    ecuName: "Honda Vario 160 eSP+",
    mapName: "Vario 160 Drag 201m",
    peakHp: 18.4,
    peakHpRpm: 8900,
    peakTorqueNm: 15.2,
    peakTorqueRpm: 6800,
    zeroToHundredSec: 8.9,
    drag201mSec: 10.6,
    drag402mSec: 17.1,
    createdAt: new Date(Date.now() - 2000),
  };
}

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

test("Unauthenticated user cannot read or list ecu_maps", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ecu_maps").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("ecu_maps").doc("map_1").get());
});

test("Authenticated owner can create, read, update, and delete ecu_map", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const mapRef = aliceDb.collection("users").doc(ALICE_UID).collection("ecu_maps").doc("map_1");
  await assertSucceeds(mapRef.set(validEcuMap(ALICE_UID)));
  await assertSucceeds(mapRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("ecu_maps").get());
  await assertSucceeds(
    mapRef.update({
      revLimiterRpm: 12200,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(mapRef.delete());
});

test("Cross-user access to ecu_maps and dyno_runs is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("ecu_maps").doc("map_alice").set(validEcuMap(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ecu_maps").doc("map_alice").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("ecu_maps").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("ecu_maps").doc("map_hack").set(validEcuMap(BOB_UID))
  );
});

test("Shadow update with extra ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = { ...validEcuMap(ALICE_UID), isAdmin: true };
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("ecu_maps").doc("map_ghost").set(payload)
  );
});

test("Authenticated owner can create and delete dyno_run, but cannot update immutable dyno_run", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const runRef = aliceDb.collection("users").doc(ALICE_UID).collection("dyno_runs").doc("run_1");
  await assertSucceeds(runRef.set(validDynoRun(ALICE_UID)));
  await assertSucceeds(runRef.get());
  await assertFails(runRef.update({ peakHp: 99.9 }));
  await assertSucceeds(runRef.delete());
});
