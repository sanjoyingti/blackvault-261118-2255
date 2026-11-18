=== ABOUT: Fixed a missing database command so the "shred all logs" button can empty every stored transfer ===
package com.aibuilt.blackvault.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aibuilt.blackvault.model.Rail
import com.aibuilt.blackvault.model.RunRecord
import com.aibuilt.blackvault.model.Tranche
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_targets")
data class CustomTarget(
    @PrimaryKey val host: String,
    val name: String,
    val rail: String,
    val keyHint: String
) {
    fun railEnum(): Rail = runCatching { Rail.valueOf(rail) }.getOrDefault(Rail.INDIA_IMPS)
}

@Dao
interface VaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: RunRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRun(run: RunRecord)

    @Query("SELECT * FROM runs WHERE id = :id")
    suspend fun runById(id: String): RunRecord?

    @Query("SELECT * FROM runs WHERE id = :id")
    fun runFlow(id: String): Flow<RunRecord?>

    @Query("SELECT * FROM runs ORDER BY createdAt DESC")
    fun allRuns(): Flow<List<RunRecord>>

    @Query("DELETE FROM runs WHERE id = :id")
    suspend fun deleteRun(id: String)

    @Query("DELETE FROM runs")
    suspend fun clearRuns()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranche(tranche: Tranche)

    @Query("SELECT * FROM tranches WHERE runId = :runId ORDER BY indexNo ASC")
    fun tranches(runId: String): Flow<List<Tranche>>

    @Query("SELECT * FROM tranches WHERE runId = :runId ORDER BY indexNo ASC")
    suspend fun tranchesNow(runId: String): List<Tranche>

    @Query("SELECT * FROM tranches WHERE id = :id")
    suspend fun trancheById(id: String): Tranche?

    @Query("UPDATE tranches SET status = :status, reference = :reference, message = :message WHERE id = :id")
    suspend fun finishTranche(id: String, status: String, reference: String?, message: String?)

    @Query("DELETE FROM tranches WHERE runId = :runId")
    suspend fun clearTranches(runId: String)

    @Query("DELETE FROM tranches")
    suspend fun clearTranchesAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: CustomTarget)

    @Query("SELECT * FROM custom_targets ORDER BY name ASC")
    fun targets(): Flow<List<CustomTarget>>

    @Query("DELETE FROM custom_targets WHERE host = :host")
    suspend fun deleteTarget(host: String)

    @Query("DELETE FROM custom_targets")
    suspend fun clearTargets()
}

@Database(entities = [RunRecord::class, Tranche::class, CustomTarget::class], version = 1, exportSchema = false)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun dao(): VaultDao

    companion object {
        fun build(context: Context): VaultDatabase =
            Room.databaseBuilder(context, VaultDatabase::class.java, "blackvault.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
