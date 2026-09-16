package com.beeftech.backend.api.feedcrib

class FeedCribService {

    private val records =
        mutableListOf<FeedCribResponse>()

    private var nextId = 1L

    fun saveReading(
        request: FeedCribRequest
    ): FeedCribResponse {

        val record =
            FeedCribResponse(
                id = nextId++,
                penName = request.penName,
                adiValue = request.adiValue,
                morning = request.morning,
                midDay = request.midDay,
                evening = request.evening,
                timestamp = request.timestamp
            )

        records.add(record)

        return record
    }

    fun getAll(): List<FeedCribResponse> {
        return records
    }

    fun getByPenName(
        penName: String
    ): List<FeedCribResponse> {

        return records.filter {
            it.penName.equals(
                penName,
                ignoreCase = true
            )
        }
    }
}