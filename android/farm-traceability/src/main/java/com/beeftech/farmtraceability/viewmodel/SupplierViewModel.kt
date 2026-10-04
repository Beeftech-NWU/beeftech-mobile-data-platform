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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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


    /*
     * Loads purchase history and automatically repairs exact
     * duplicate rows that may have been produced by an older
     * build.
     */
    fun loadSuppliers(
        animalId: String
    ) {

        if (
            animalId.isBlank()
        ) {

            _purchases.value =
                emptyList()

            return
        }


        viewModelScope.launch {

            try {

                animalPurchaseDao
                    .getPurchasesForAnimal(
                        animalId
                    )
                    .collect {
                            list ->

                        val unique =
                            mutableListOf<
                                AnimalPurchaseEntity
                                >()

                        val duplicateIds =
                            mutableListOf<String>()

                        val seen =
                            mutableSetOf<String>()


                        list.forEach {
                                purchase ->

                            val key =
                                duplicateIdentity(
                                    purchase
                                )

                            if (
                                seen.add(key)
                            ) {

                                unique +=
                                    purchase

                            } else {

                                duplicateIds +=
                                    purchase.purchaseId
                            }
                        }


                        duplicateIds.forEach {
                                purchaseId ->

                            animalPurchaseDao
                                .deletePurchaseById(
                                    purchaseId
                                )
                        }


                        _purchases.value =
                            unique
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

        if (
            animalId.isBlank()
        ) {

            onResult(
                false,
                "Please select an animal first."
            )

            return
        }


        if (
            supplierName.isBlank()
        ) {

            onResult(
                false,
                "Please enter the seller/supplier name."
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
                 * IMPORTANT:
                 *
                 * Supplier does NOT create another Farmer.
                 *
                 * It searches Farmer Registration and references the
                 * farmer_id of the existing supplier farm.
                 */
                val linkedFarmer =
                    database
                        .farmerDao()
                        .findSupplierFarmerByDisplayName(
                            supplierName.trim()
                        )


                val resolvedSupplierName =
                    linkedFarmer
                        ?.organisation_name
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?: supplierName
                            .trim()


                val resolvedGln =
                    linkedFarmer
                        ?.gln_number
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?: glnNumber
                            .trim()
                            .takeIf {
                                it.isNotEmpty()
                            }


                val resolvedBatch =
                    purchaseBatchNumber
                        .trim()
                        .takeIf {
                            it.isNotEmpty()
                        }


                val resolvedPurchaseDate =
                    parsePurchaseDate(
                        purchaseDate
                    )


                /*
                 * DUPLICATE GUARD
                 *
                 * Check before creating a UUID/new row.
                 */
                val existingPurchase =
                    animalPurchaseDao
                        .findEquivalentPurchase(
                            animalId =
                                animalId.trim(),

                            supplierFarmerId =
                                linkedFarmer
                                    ?.farmer_id,

                            sellerName =
                                resolvedSupplierName,

                            glnNumber =
                                resolvedGln,

                            purchaseDate =
                                resolvedPurchaseDate,

                            purchaseBatchNumber =
                                resolvedBatch
                        )


                if (
                    existingPurchase != null
                ) {

                    /*
                     * Use the existing transaction instead of
                     * inserting another AnimalPurchase row.
                     */
                    onResult(
                        true,
                        "This supplier purchase is already saved. " +
                            "The existing record was reused."
                    )

                    return@launch
                }


                /*
                 * Only a genuinely new purchase transaction reaches
                 * this point.
                 */
                val purchase =
                    AnimalPurchaseEntity(
                        animalId =
                            animalId.trim(),

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
                        purchase
                    )


                /*
                 * Only queue the newly-created transaction.
                 *
                 * Re-selecting an existing farmer/calf therefore
                 * cannot produce another queue entry through this
                 * save operation.
                 */
                PendingSyncRepository(
                    database.pendingSyncDao()
                ).queueOperation(
                    entityType =
                        TraceabilityOutboxWorker
                            .ENTITY_ANIMAL_PURCHASE,

                    entityId =
                        purchase.recordGuid,

                    operation =
                        "UPSERT",

                    payload =
                        purchase.recordGuid
                )


                TraceabilitySyncScheduler
                    .kick()


                onResult(
                    true,
                    if (
                        linkedFarmer ==
                        null
                    ) {

                        "Supplier purchase saved offline and queued for sync."

                    } else {

                        "Existing registered farm linked to this animal. " +
                            "No duplicate farmer was created."
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


    /*
     * Identity used to repair duplicate rows from builds that
     * existed before duplicate protection was added.
     *
     * Registered suppliers use supplierFarmerId as their stable
     * identity.
     *
     * External suppliers fall back to seller name + GLN.
     */
    private fun duplicateIdentity(
        purchase:
            AnimalPurchaseEntity
    ): String {

        val supplierIdentity =
            purchase
                .supplierFarmerId
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?.lowercase(
                    Locale.ROOT
                )
                ?: (
                    purchase
                        .sellerName
                        .trim()
                        .lowercase(
                            Locale.ROOT
                        ) +
                        "|" +
                        purchase
                            .glnNumber
                            .orEmpty()
                            .trim()
                )


        return listOf(
            purchase
                .animalId
                .trim()
                .lowercase(
                    Locale.ROOT
                ),

            supplierIdentity,

            purchase
                .purchaseDate
                .toString(),

            purchase
                .purchaseBatchNumber
                .orEmpty()
                .trim()
                .lowercase(
                    Locale.ROOT
                )
        ).joinToString(
            separator = "|"
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
        ).forEach {
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
                    .parse(value)
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
