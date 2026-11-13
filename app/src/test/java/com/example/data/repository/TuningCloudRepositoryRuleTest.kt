package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DynoRunRecord
import com.example.data.model.EcuTuningMap
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleEcuMap(uid: String) = EcuTuningMap(
        userId = uid,
        mapName = "Vario 160 Drag 201m",
        ecuModelId = "vario160-k2s",
        ecuName = "Honda Vario 160 eSP+ (4V)",
        partNumber = "30400-K2S-N01",
        strategyName = "Drag Race",
        revLimiterRpm = 11500L,
        launchControlRpm = 5600L,
        targetAfr = 12.6,
        fuelVeValues = List(256) { 102.5 },
        ignitionValues = List(256) { 21.0 },
        checksumHex = "A1B2C3D4",
        offImmobilizer = false,
        offSideStand = true,
        offO2Sensor = true,
        offEctCut = false,
        notes = "Test spec"
    )

    @Test
    fun saveAndObserveEcuMap_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = TuningCloudRepository(firestore)

        val saveResult = withTimeout(TIMEOUT_MS) { repo.saveNewEcuMap(sampleEcuMap(uid)) }
        assertTrue(saveResult.isSuccess)
        val mapId = saveResult.getOrThrow()
        assertTrue(mapId.isNotEmpty())

        val maps = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSavedMaps(uid).first { list -> list.any { it.id == mapId } }
        }
        assertTrue(maps.any { it.id == mapId && it.mapName == "Vario 160 Drag 201m" })

        val deleteResult = withTimeout(TIMEOUT_MS) { repo.deleteEcuMap(mapId) }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun saveDynoRun_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = TuningCloudRepository(firestore)

        val run = DynoRunRecord(
            userId = uid,
            ecuModelId = "vario160-k2s",
            ecuName = "Honda Vario 160 eSP+ (4V)",
            mapName = "Drag Race",
            peakHp = 18.2,
            peakHpRpm = 8900L,
            peakTorqueNm = 15.1,
            peakTorqueRpm = 6700L,
            zeroToHundredSec = 8.4,
            drag201mSec = 10.1,
            drag402mSec = 16.2
        )

        val saveResult = withTimeout(TIMEOUT_MS) { repo.saveDynoRun(run) }
        assertTrue(saveResult.isSuccess)
        val runId = saveResult.getOrThrow()

        val runs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeDynoRuns(uid).first { list -> list.any { it.id == runId } }
        }
        assertTrue(runs.any { it.id == runId })
    }

    @Test
    fun crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = TuningCloudRepository(firestore)
        val mapId = withTimeout(TIMEOUT_MS) { aliceRepo.saveNewEcuMap(sampleEcuMap(aliceUid)).getOrThrow() }

        signInTestUser(BOB_EMAIL)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("ecu_maps").document(mapId)
                    .get()
                    .await()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    @Test
    fun unauthenticatedObserve_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = TuningCloudRepository(firestore)

        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeSavedMaps(aliceUid).first()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.tuner@test.com"
        const val BOB_EMAIL = "bob.tuner@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
