package com.kevinandrey.qrscanner.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scans", indices = [Index(value = ["raw"], unique = true)])
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val raw: String,
    @ColumnInfo(name = "scanned_at") val scannedAt: Long,
    val favorite: Boolean = false,
)

@Dao
abstract class ScanDao {
    @Query("SELECT * FROM scans ORDER BY scanned_at DESC, id DESC")
    abstract fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE id = :id")
    abstract fun observe(id: Long): Flow<ScanEntity?>

    @Query("SELECT id FROM scans WHERE raw = :raw")
    protected abstract suspend fun findIdByRaw(raw: String): Long?

    @Insert
    protected abstract suspend fun insert(entity: ScanEntity): Long

    @Query("UPDATE scans SET scanned_at = :scannedAt WHERE id = :id")
    protected abstract suspend fun touch(
        id: Long,
        scannedAt: Long,
    )

    /** Scanning the same code again moves it to the top instead of duplicating it. */
    @Transaction
    open suspend fun upsertByRaw(
        raw: String,
        scannedAt: Long,
    ): Long {
        val existing = findIdByRaw(raw) ?: return insert(ScanEntity(raw = raw, scannedAt = scannedAt))
        touch(existing, scannedAt)

        return existing
    }

    @Query("UPDATE scans SET favorite = :favorite WHERE id = :id")
    abstract suspend fun setFavorite(
        id: Long,
        favorite: Boolean,
    )

    @Query("DELETE FROM scans WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM scans WHERE favorite = 0")
    abstract suspend fun deleteNonFavorites()
}

@Database(entities = [ScanEntity::class], version = 1, exportSchema = true)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao

    companion object {
        private const val NAME = "scans.db"

        fun create(context: Context): ScanDatabase = Room.databaseBuilder(context, ScanDatabase::class.java, NAME).build()
    }
}
