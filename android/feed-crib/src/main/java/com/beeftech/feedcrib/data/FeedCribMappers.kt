package com.beeftech.feedcrib.data

import com.beeftech.database.entity.CribReadingCodeEntity
import com.beeftech.database.entity.FeedCribEntity
import com.beeftech.database.entity.FeedCribEntryEntity
import com.beeftech.database.entity.FeedEntryOrigin

object FeedCribMappers {

    fun toDto(entry: FeedCribEntryEntity) = FeedCribEntryDto(
        recordguid = entry.recordGuid,
        cribNumber = entry.cribNumber,
        readingDate = entry.readingDate,
        slot = entry.slot,
        code = entry.code,
        adi = entry.adi,
        capturedAt = entry.capturedAt,
        deviceId = entry.deviceId,
        gpsLat = entry.gpsLat,
        gpsLng = entry.gpsLng
    )

    fun toEntity(dto: FeedCribNetworkDto, downloadedAt: Long) = FeedCribEntity(
        cribNumber = dto.cribNumber,
        siteId = dto.siteId,
        penDescription = dto.penDescription,
        ration = dto.ration,
        method = dto.method,
        description = dto.description,
        requiredKg = dto.requiredKg,
        animalsBegin = dto.animalsBegin,
        animalsIn = dto.animalsIn,
        animalsOut = dto.animalsOut,
        animalsClose = dto.animalsClose,
        currentAdi = dto.currentAdi,
        active = dto.active,
        updatedAt = dto.updatedAt,
        lastDownloadedAt = downloadedAt
    )

    fun toEntity(dto: CribReadingCodeNetworkDto) = CribReadingCodeEntity(
        code = dto.code,
        label = dto.label,
        description = dto.description,
        active = dto.active
    )

    /** A reading some phone on the site already synced: kept for the history, never uploaded again. */
    fun toEntity(dto: FeedCribEntryDownloadDto, siteId: String) = FeedCribEntryEntity(
        recordGuid = dto.recordguid,
        cribNumber = dto.cribNumber,
        siteId = siteId,
        readingDate = dto.readingDate,
        slot = dto.slot,
        code = dto.code,
        adi = dto.adi,
        capturedAt = dto.capturedAt,
        deviceId = dto.deviceId,
        userId = dto.submittedByUserId.orEmpty(),
        origin = FeedEntryOrigin.SERVER,
        syncStatus = "SYNCED",
        syncedAt = dto.syncedAt
    )
}
