package com.beeftech.farmtraceability.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.dao.AnimalPurchaseDao
import com.beeftech.database.entity.AnimalPurchaseEntity
import com.beeftech.database.repository.PendingSyncRepository
import com.beeftech.farmtraceability.worker.TraceabilityOutboxWorker
import com.beeftech.farmtraceability.worker.TraceabilitySyncScheduler
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SupplierViewModel(
    private val animalPurchaseDao:
        AnimalPurchaseDao
) : ViewModel() {

    private val _purchases =
        MutableStateFlow<
            List<AnimalPurchaseEntity>
        >(
            emptyList()
        )

    val purchases:
            StateFlow<
                List<AnimalPurchaseEntity>
            > =
        _purchases.asStateFlow()

    val suppliers:
            StateFlow<
                List<AnimalPurchaseEntity>
            >
        get() = purchases

    private var loadJob:
            Job? =
        null


    fun loadSuppliers(
        animalId: String
    ) {

        loadJob?.cancel()

        val normalizedAnimalId =
            animalId.trim()

        if (
            normalizedAnimalId.isBlank()
        ) {

            _purchases.value =
                emptyList()

            return
        }

        loadJob =
            viewModelScope.launch {

                try {

                    animalPurchaseDao
                        .getPurchasesForAnimal(
                            normalizedAnimalId
                        )
                        .collect {
                                rows ->

                            /*
                             * Historical builds may contain duplicate
                             * entries for the same supplier.
                             *
                             * Supplier History now shows only the newest
                             * row for each supplier.
                             */
                            _purchases.value =
                                collapseHistory(
                                    rows
                                )
                        }

                } catch (
                    _: Exception
                ) {

                    _purchases.value =
                        emptyList()
                }
            }
    }


    fun saveSupplier(
        animalId: String,
        supplierName: String,
        glnNumber: String,
        purchaseDate: String,
        purchaseBatchNumber: String,
        onResult: (
            Boolean,
            String
        ) -> Unit = { _, _ -> }
    ) {

        val normalizedAnimalId =
            animalId.trim()

        val normalizedSupplier =
            supplierName.trim()

        if (
            normalizedAnimalId.isBlank()
        ) {

            onResult(
                false,
                "Please select an animal first."
            )

            return
        }

        if (
            normalizedSupplier.isBlank()
        ) {

            onResult(
                false,
                "Please select or enter a supplier."
            )

            return
        }

        viewModelScope.launch {

            try {

                val database =
                    DatabaseProvider
                        .getDatabase()
                        ?: throw IllegalStateException(
                            "The encrypted database is not available."
                        )

                /*
                 * Look up the Farmer Registration supplier.
                 *
                 * Supplier never creates a second Farmer.
                 */
                val linkedFarmer =
                    database
                        .farmerDao()
                        .findSupplierFarmerByDisplayName(
                            normalizedSupplier
                        )

                val resolvedSupplierName =
                    linkedFarmer
                        ?.organisation_name
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: normalizedSupplier

                val resolvedGln =
                    linkedFarmer
                        ?.gln_number
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: glnNumber
                            .trim()
                            .takeIf {
                                it.isNotBlank()
                            }

                val resolvedBatch =
                    purchaseBatchNumber
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }

                val resolvedPurchaseDate =
                    parsePurchaseDate(
                        purchaseDate
                    )

                val currentRows =
                    animalPurchaseDao
                        .getPurchasesForAnimal(
                            normalizedAnimalId
                        )
                        .first()

                /*
                 * IMPORTANT:
                 *
                 * History represents suppliers linked to the animal.
                 *
                 * Selecting/saving Khanyisa Livestock Farm again
                 * therefore updates the existing Khanyisa row rather
                 * than appending another row.
                 */
                val existing =
                    currentRows
                        .sortedByDescending {
                            it.purchaseDate
                        }
                        .firstOrNull {
                                row ->

                            sameSupplier(
                                existingName =
                                    row.sellerName,

                                newName =
                                    resolvedSupplierName
                            )
                        }

                val savedPurchase =
                    if (
                        existing != null
                    ) {

                        val updated =
                            existing.copy(
                                purchaseDate =
                                    resolvedPurchaseDate,

                                sellerName =
                                    resolvedSupplierName,

                                supplierFarmerId =
                                    linkedFarmer
                                        ?.farmer_id,

                                glnNumber =
                                    resolvedGln,

                                purchaseBatchNumber =
                                    resolvedBatch,

                                notes =
                                    if (
                                        linkedFarmer ==
                                        null
                                    ) {

                                        "External supplier"

                                    } else {

                                        "Linked to registered BeefTech farm"
                                    }
                            )

                        animalPurchaseDao
                            .insertPurchase(
                                updated
                            )

                        updated

                    } else {

                        val created =
                            AnimalPurchaseEntity(
                                animalId =
                                    normalizedAnimalId,

                                purchasePrice =
                                    0.0,

                                purchaseDate =
                                    resolvedPurchaseDate,

                                sellerName =
                                    resolvedSupplierName,

                                supplierFarmerId =
                                    linkedFarmer
                                        ?.farmer_id,

                                glnNumber =
                                    resolvedGln,

                                purchaseBatchNumber =
                                    resolvedBatch,

                                notes =
                                    if (
                                        linkedFarmer ==
                                        null
                                    ) {

                                        "External supplier"

                                    } else {

                                        "Linked to registered BeefTech farm"
                                    }
                            )

                        animalPurchaseDao
                            .insertPurchase(
                                created
                            )

                        created
                    }

                PendingSyncRepository(
                    database.pendingSyncDao()
                )
                    .queueOperation(
                        entityType =
                            TraceabilityOutboxWorker
                                .ENTITY_ANIMAL_PURCHASE,

                        entityId =
                            savedPurchase.recordGuid,

                        operation =
                            "UPSERT",

                        payload =
                            savedPurchase.recordGuid
                    )

                TraceabilitySyncScheduler
                    .kick()

                onResult(
                    true,
                    if (
                        existing != null
                    ) {

                        "Supplier record updated successfully."

                    } else {

                        "Supplier saved successfully."
                    }
                )

            } catch (
                exception: Exception
            ) {

                onResult(
                    false,
                    exception.message
                        ?: "Unable to save supplier record."
                )
            }
        }
    }


    private fun collapseHistory(
        rows:
            List<AnimalPurchaseEntity>
    ): List<AnimalPurchaseEntity> {

        val seen =
            mutableSetOf<String>()

        return rows
            .sortedByDescending {
                it.purchaseDate
            }
            .filter {
                    row ->

                seen.add(
                    supplierHistoryKey(
                        row.sellerName
                    )
                )
            }
    }


    private fun sameSupplier(
        existingName: String,
        newName: String
    ): Boolean {

        return supplierHistoryKey(
            existingName
        ) ==
            supplierHistoryKey(
                newName
            )
    }


    private fun supplierHistoryKey(
        supplierName: String
    ): String {

        return supplierName
            .trim()
            .lowercase(
                Locale.ROOT
            )
    }


    private fun parsePurchaseDate(
        rawValue: String
    ): Long {

        val value =
            rawValue.trim()

        value
            .toLongOrNull()
            ?.let {
                return it
            }

        listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd/MM/yyyy"
        )
            .forEach {
                    pattern ->

                try {

                    val formatter =
                        SimpleDateFormat(
                            pattern,
                            Locale.US
                        )

                    formatter.isLenient =
                        false

                    formatter
                        .parse(
                            value
                        )
                        ?.let {
                            return it.time
                        }

                } catch (
                    _: Exception
                ) {
                    // Try next supported format.
                }
            }

        return System.currentTimeMillis()
    }
}
