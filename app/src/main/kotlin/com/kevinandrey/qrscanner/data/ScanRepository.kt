package com.kevinandrey.qrscanner.data

import com.kevinandrey.qrscanner.core.Scan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

interface ScanRepository {
    /** History ordered from the most recent scan. */
    fun observeScans(): Flow<List<Scan>>

    fun observeScan(id: Long): Flow<Scan?>

    /** Stores a scan, or moves an identical earlier scan to the top, and returns its id. */
    suspend fun record(raw: String): Long

    suspend fun setFavorite(
        id: Long,
        favorite: Boolean,
    )

    suspend fun delete(id: Long)

    /** Removes every scan that is not a favorite. */
    suspend fun clearHistory()
}

class RoomScanRepository(
    private val dao: ScanDao,
) : ScanRepository {
    override fun observeScans(): Flow<List<Scan>> = dao.observeAll().map { entities -> entities.map(ScanEntity::toScan) }

    override fun observeScan(id: Long): Flow<Scan?> = dao.observe(id).map { it?.toScan() }

    override suspend fun record(raw: String): Long = dao.upsertByRaw(raw, Instant.now().toEpochMilli())

    override suspend fun setFavorite(
        id: Long,
        favorite: Boolean,
    ) = dao.setFavorite(id, favorite)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clearHistory() = dao.deleteNonFavorites()
}

private fun ScanEntity.toScan() = Scan(id = id, raw = raw, scannedAt = Instant.ofEpochMilli(scannedAt), favorite = favorite)
