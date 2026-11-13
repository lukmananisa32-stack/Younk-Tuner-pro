package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.DynoRunRecord
import com.example.data.model.EcuTuningMap
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TuningCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Silakan masuk dengan Google sebelum mengakses Cloud Maps.")
    }

    fun observeSavedMaps(userId: String): Flow<List<EcuTuningMap>> = flow {
        val path = "users/$userId/ecu_maps"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("ecu_maps")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        EcuTuningMap::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveNewEcuMap(mapData: EcuTuningMap): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users")
                .document(uid)
                .collection("ecu_maps")
                .document()

            val payload = hashMapOf(
                "userId" to uid,
                "mapName" to mapData.mapName.take(80),
                "ecuModelId" to mapData.ecuModelId,
                "ecuName" to mapData.ecuName,
                "partNumber" to mapData.partNumber,
                "strategyName" to mapData.strategyName,
                "revLimiterRpm" to mapData.revLimiterRpm,
                "launchControlRpm" to mapData.launchControlRpm,
                "targetAfr" to mapData.targetAfr,
                "fuelVeValues" to mapData.fuelVeValues,
                "ignitionValues" to mapData.ignitionValues,
                "checksumHex" to mapData.checksumHex,
                "offImmobilizer" to mapData.offImmobilizer,
                "offSideStand" to mapData.offSideStand,
                "offO2Sensor" to mapData.offO2Sensor,
                "offEctCut" to mapData.offEctCut,
                "notes" to mapData.notes.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/ecu_maps")
            Result.failure(e)
        }
    }

    suspend fun deleteEcuMap(mapId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users")
                .document(uid)
                .collection("ecu_maps")
                .document(mapId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/ecu_maps/$mapId")
            Result.failure(e)
        }
    }

    fun observeDynoRuns(userId: String): Flow<List<DynoRunRecord>> = flow {
        val path = "users/$userId/dyno_runs"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("dyno_runs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DynoRunRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveDynoRun(run: DynoRunRecord): Result<String> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users")
                .document(uid)
                .collection("dyno_runs")
                .document()

            val payload = hashMapOf(
                "userId" to uid,
                "ecuModelId" to run.ecuModelId,
                "ecuName" to run.ecuName,
                "mapName" to run.mapName.take(80),
                "peakHp" to run.peakHp,
                "peakHpRpm" to run.peakHpRpm,
                "peakTorqueNm" to run.peakTorqueNm,
                "peakTorqueRpm" to run.peakTorqueRpm,
                "zeroToHundredSec" to run.zeroToHundredSec,
                "drag201mSec" to run.drag201mSec,
                "drag402mSec" to run.drag402mSec,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.CREATE, "users/$uid/dyno_runs")
            Result.failure(e)
        }
    }

    suspend fun deleteDynoRun(runId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users")
                .document(uid)
                .collection("dyno_runs")
                .document(runId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "anonymous"
            handleFirestoreError(e, OperationType.DELETE, "users/$uid/dyno_runs/$runId")
            Result.failure(e)
        }
    }
}
