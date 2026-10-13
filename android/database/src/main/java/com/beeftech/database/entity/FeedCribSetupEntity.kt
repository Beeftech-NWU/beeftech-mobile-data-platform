package com.beeftech.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Snapshot is scoped to the provisioned user; cached setup permits offline creation. */
@Entity(tableName = "feed_crib_setup")
data class FeedCribSetupEntity(@PrimaryKey val userId: String, val json: String)
