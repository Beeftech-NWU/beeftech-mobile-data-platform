package com.beeftech.farmerregistration

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.beeftech.database.DatabaseProvider
import com.beeftech.database.entity.FarmerAddressEntity
import com.beeftech.database.entity.FarmerEntity
import com.beeftech.database.repository.FarmerRepository
import com.beeftech.farmerregistration.ui.theme.BeeftechTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

data class CoordinatesSaveData(
    val latitude: String = "",
    val longitude: String = "",
    val organisationName: String = ""
)

class CoordinatesAndSaveScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BeeftechTheme {

                var formData by remember {
                    mutableStateOf(
                        CoordinatesSaveData(
                            organisationName =
                                FarmerRegistrationSession.clientDetails
                                    ?.organisationName
                                    ?: "Farmer"
                        )
                    )
                }

                var isLoadingLocation by remember {
                    mutableStateOf(false)
                }

                var isSaving by remember {
                    mutableStateOf(false)
                }

                val locationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->

                        val fineGranted =
                            permissions[
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ] == true

                        val coarseGranted =
                            permissions[
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ] == true

                        if (fineGranted || coarseGranted) {

                            isLoadingLocation = true

                            fetchCurrentLocation(
                                onLocation = { location ->

                                    formData = formData.copy(
                                        latitude = String.format(
                                            Locale.US,
                                            "%.6f",
                                            location.latitude
                                        ),
                                        longitude = String.format(
                                            Locale.US,
                                            "%.6f",
                                            location.longitude
                                        )
                                    )

                                    isLoadingLocation = false

                                    Toast.makeText(
                                        this,
                                        "Current location captured",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { message ->

                                    isLoadingLocation = false

                                    Toast.makeText(
                                        this,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )

                        } else {

                            Toast.makeText(
                                this,
                                "Location permission is required",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                CoordinatesAndSaveContent(
                    formData = formData,
                    isLoadingLocation = isLoadingLocation,
                    isSaving = isSaving,

                    onFormDataChange = {
                        formData = it
                    },

                    onBackClick = {
                        finish()
                    },

                    onUseCurrentLocationClick = {

                        val fineGranted =
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                        val coarseGranted =
                            ContextCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                        if (fineGranted || coarseGranted) {

                            isLoadingLocation = true

                            fetchCurrentLocation(
                                onLocation = { location ->

                                    formData = formData.copy(
                                        latitude = String.format(
                                            Locale.US,
                                            "%.6f",
                                            location.latitude
                                        ),
                                        longitude = String.format(
                                            Locale.US,
                                            "%.6f",
                                            location.longitude
                                        )
                                    )

                                    isLoadingLocation = false

                                    Toast.makeText(
                                        this,
                                        "Current location captured",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { message ->

                                    isLoadingLocation = false

                                    Toast.makeText(
                                        this,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )

                        } else {

                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },

                    onViewOnMapClick = {

                        openLocationOnMap(
                            latitude = formData.latitude,
                            longitude = formData.longitude
                        )
                    },

                    onSaveClick = {

                        if (isSaving) {
                            return@CoordinatesAndSaveContent
                        }

                        val latitude =
                            formData.latitude.toDoubleOrNull()

                        val longitude =
                            formData.longitude.toDoubleOrNull()

                        if (latitude == null || longitude == null) {

                            Toast.makeText(
                                this,
                                "Please enter valid latitude and longitude values",
                                Toast.LENGTH_LONG
                            ).show()

                            return@CoordinatesAndSaveContent
                        }

                        if (latitude !in -90.0..90.0) {

                            Toast.makeText(
                                this,
                                "Latitude must be between -90 and 90",
                                Toast.LENGTH_LONG
                            ).show()

                            return@CoordinatesAndSaveContent
                        }

                        if (longitude !in -180.0..180.0) {

                            Toast.makeText(
                                this,
                                "Longitude must be between -180 and 180",
                                Toast.LENGTH_LONG
                            ).show()

                            return@CoordinatesAndSaveContent
                        }

                        val clientData =
                            FarmerRegistrationSession.clientDetails

                        val addressData =
                            FarmerRegistrationSession.addressDetails

                        if (clientData == null) {

                            Toast.makeText(
                                this,
                                "Client details are missing. Please go back and complete Client Details.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@CoordinatesAndSaveContent
                        }

                        if (addressData == null) {

                            Toast.makeText(
                                this,
                                "Address details are missing. Please go back and complete the address.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@CoordinatesAndSaveContent
                        }

                        isSaving = true

                        lifecycleScope.launch {

                            try {

                                val result =
                                    withContext(Dispatchers.IO) {

                                        val database =
                                            DatabaseProvider.getDatabase()
                                                ?: throw IllegalStateException(
                                                    "Database is not available."
                                                )

                                        val repository =
                                            FarmerRepository(
                                                database.farmerDao()
                                            )

                                        val existingFarmers =
                                            repository.getAllFarmers()

                                        val duplicate =
                                            existingFarmers.any {
                                                it.client_code
                                                    ?.trim()
                                                    ?.equals(
                                                        clientData.clientCode.trim(),
                                                        ignoreCase = true
                                                    ) == true
                                            }

                                        if (duplicate) {

                                            throw IllegalStateException(
                                                "A farmer with client code ${clientData.clientCode} already exists."
                                            )
                                        }

                                        val farmerId =
                                            UUID.randomUUID().toString()

                                        val addressId =
                                            UUID.randomUUID().toString()

                                        val farmer =
                                            FarmerEntity(
                                                farmer_id = farmerId,

                                                client_code =
                                                    clientData.clientCode.trim(),

                                                organisation_name =
                                                    clientData.organisationName.trim(),

                                                vat_number =
                                                    clientData.vatNumber
                                                        .trim()
                                                        .ifBlank { null },

                                                email_address =
                                                    clientData.emailAddress
                                                        .trim()
                                                        .ifBlank { null },

                                                gps_latitude = latitude,

                                                gps_longitude = longitude,

                                                sync_status = "PENDING"
                                            )

                                        val address =
                                            FarmerAddressEntity(
                                                address_id = addressId,

                                                farmer_id = farmerId,

                                                address_type = "PRIMARY",

                                                address_line_1 =
                                                    addressData.streetAddress.trim(),

                                                province =
                                                    addressData.province
                                                        .trim()
                                                        .ifBlank { null },

                                                postal_code =
                                                    addressData.postalCode
                                                        .trim()
                                                        .ifBlank { null },

                                                gps_latitude = latitude,

                                                gps_longitude = longitude
                                            )

                                        repository.addFarmer(farmer)
                                        repository.addAddress(address)

                                        true
                                    }

                                if (result) {

                                    // Clear all temporary registration data
                                    // so the next farmer starts with empty fields.
                                    FarmerRegistrationSession.clear()

                                    Toast.makeText(
                                        this@CoordinatesAndSaveScreen,
                                        "Farmer registration saved successfully",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    /*
                                     * Return to the existing MainActivity.
                                     *
                                     * We use setClassName() instead of importing
                                     * MainActivity because this registration
                                     * module should not depend directly on the
                                     * application module.
                                     *
                                     * CLEAR_TOP removes:
                                     * CoordinatesAndSaveScreen
                                     * AddressAndLocationScreen
                                     * ClientDetailsScreen
                                     *
                                     * and brings the existing MainActivity
                                     * back to the front.
                                     */
                                    val intent = Intent().apply {

                                        setClassName(
                                            this@CoordinatesAndSaveScreen,
                                            "com.beeftech.demoapp.MainActivity"
                                        )

                                        flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }

                                    startActivity(intent)

                                    // Finish this Activity so it cannot remain
                                    // in the back stack.
                                    finish()
                                }

                            } catch (ex: Exception) {

                                Toast.makeText(
                                    this@CoordinatesAndSaveScreen,
                                    "Could not save farmer: ${ex.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                            } finally {

                                isSaving = false
                            }
                        }
                    }
                )
            }
        }
    }

    private fun fetchCurrentLocation(
        onLocation: (Location) -> Unit,
        onError: (String) -> Unit
    ) {

        val locationManager =
            getSystemService(LOCATION_SERVICE) as LocationManager

        val gpsEnabled =
            try {
                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            } catch (_: Exception) {
                false
            }

        val networkEnabled =
            try {
                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            } catch (_: Exception) {
                false
            }

        if (!gpsEnabled && !networkEnabled) {

            onError(
                "Location Services are disabled. Please turn on Location/GPS and try again."
            )

            return
        }

        val fineGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {

            onError(
                "Location permission has not been granted."
            )

            return
        }

        try {

            val provider =
                when {
                    gpsEnabled -> LocationManager.GPS_PROVIDER
                    networkEnabled -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }

            if (provider == null) {

                onError(
                    "No location provider is available."
                )

                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                locationManager.getCurrentLocation(
                    provider,
                    null,
                    mainExecutor
                ) { location ->

                    if (location != null) {

                        onLocation(location)

                    } else {

                        val fallback =
                            getLastKnownLocation(
                                locationManager,
                                gpsEnabled,
                                networkEnabled
                            )

                        if (fallback != null) {

                            onLocation(fallback)

                        } else {

                            onError(
                                "The device could not determine its current location. If you are using an emulator, set an emulator location first."
                            )
                        }
                    }
                }

            } else {

                val fallback =
                    getLastKnownLocation(
                        locationManager,
                        gpsEnabled,
                        networkEnabled
                    )

                if (fallback != null) {

                    onLocation(fallback)

                } else {

                    onError(
                        "No recent device location is available."
                    )
                }
            }

        } catch (securityException: SecurityException) {

            onError(
                "Location permission was denied by Android."
            )

        } catch (exception: Exception) {

            onError(
                "Unable to obtain location: ${exception.message}"
            )
        }
    }

    private fun getLastKnownLocation(
        locationManager: LocationManager,
        gpsEnabled: Boolean,
        networkEnabled: Boolean
    ): Location? {

        val providers =
            buildList {

                if (gpsEnabled) {
                    add(LocationManager.GPS_PROVIDER)
                }

                if (networkEnabled) {
                    add(LocationManager.NETWORK_PROVIDER)
                }
            }

        var bestLocation: Location? = null

        for (provider in providers) {

            try {

                val location =
                    locationManager.getLastKnownLocation(provider)

                if (location != null) {

                    if (
                        bestLocation == null ||
                        location.accuracy < bestLocation!!.accuracy
                    ) {
                        bestLocation = location
                    }
                }

            } catch (_: SecurityException) {

                return bestLocation

            } catch (_: Exception) {
                // Ignore this provider and continue.
            }
        }

        return bestLocation
    }

    private fun openLocationOnMap(
        latitude: String,
        longitude: String
    ) {

        val lat =
            latitude.toDoubleOrNull()

        val lng =
            longitude.toDoubleOrNull()

        if (lat == null || lng == null) {

            Toast.makeText(
                this,
                "Enter valid coordinates before opening the map",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            lat !in -90.0..90.0 ||
            lng !in -180.0..180.0
        ) {

            Toast.makeText(
                this,
                "The coordinates are outside the valid range",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val googleMapsUrl =
            "https://www.google.com/maps/search/?api=1&query=$lat,$lng"

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(googleMapsUrl)
            )

        try {

            startActivity(intent)

        } catch (_: ActivityNotFoundException) {

            val geoIntent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                )

            try {

                startActivity(geoIntent)

            } catch (_: ActivityNotFoundException) {

                Toast.makeText(
                    this,
                    "No map application or browser is available",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
fun CoordinatesAndSaveContent(
    formData: CoordinatesSaveData,
    isLoadingLocation: Boolean,
    isSaving: Boolean,
    onFormDataChange: (CoordinatesSaveData) -> Unit,
    onBackClick: () -> Unit,
    onUseCurrentLocationClick: () -> Unit,
    onViewOnMapClick: () -> Unit,
    onSaveClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeeftechBackground)
            .verticalScroll(rememberScrollState())
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BeeftechPrimaryDeep)
                .padding(
                    start = 14.dp,
                    end = 22.dp,
                    top = 44.dp,
                    bottom = 20.dp
                )
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(42.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = BeeftechWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(4.dp)
                )

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            BeeftechPrimary.copy(alpha = 0.18f),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = BeeftechPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "FARM LOCATION TRACKING",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BeeftechPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "Coordinates & Save",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = BeeftechWhite
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(9.dp)
            )

            Text(
                text = "Capture precise geographic coordinates for the farmer location and save the registration securely.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = BeeftechWhite.copy(alpha = 0.7f)
            )

            Spacer(
                modifier = Modifier.height(17.dp)
            )

            HorizontalDivider(
                thickness = 2.dp,
                color = BeeftechPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            SafeFarmerSectionTitle(
                title = "Where I Am"
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            SafeFarmerCard {

                SafeFarmerTextField(
                    label = "Latitude",
                    value = formData.latitude,
                    onValueChange = {
                        onFormDataChange(
                            formData.copy(
                                latitude = it
                            )
                        )
                    },
                    placeholder = "e.g. -26.713700"
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                SafeFarmerTextField(
                    label = "Longitude",
                    value = formData.longitude,
                    onValueChange = {
                        onFormDataChange(
                            formData.copy(
                                longitude = it
                            )
                        )
                    },
                    placeholder = "e.g. 27.097900"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "Use the current location button to automatically capture the device location.",
                    fontSize = 12.sp,
                    color = BeeftechMutedText,
                    lineHeight = 16.sp
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                SafeFarmerSecondaryButton(
                    text =
                        if (isLoadingLocation) {
                            "Getting current location..."
                        } else {
                            "Use current location"
                        },
                    onClick = onUseCurrentLocationClick,
                    enabled = !isLoadingLocation
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                SafeFarmerSecondaryButton(
                    text = "View on map",
                    onClick = onViewOnMapClick,
                    enabled =
                        formData.latitude.isNotBlank() &&
                                formData.longitude.isNotBlank()
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            SafeFarmerCard {

                Text(
                    text =
                        "Record 1 of 1 • ${
                            formData.organisationName.ifBlank {
                                "Farmer"
                            }
                        }",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BeeftechPrimaryDeep
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "The registration will be saved locally with a PENDING sync status.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = BeeftechText
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Button(
                onClick = onSaveClick,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BeeftechPrimaryDeep
                )
            ) {

                Text(
                    text =
                        if (isSaving) {
                            "Saving..."
                        } else {
                            "Save Location Data"
                        },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BeeftechWhite
                )
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

@Composable
private fun SafeFarmerSectionTitle(
    title: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(
                    width = 4.dp,
                    height = 18.dp
                )
                .background(
                    BeeftechPrimaryDark,
                    RoundedCornerShape(3.dp)
                )
        )

        Spacer(
            modifier = Modifier.width(9.dp)
        )

        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = BeeftechPrimaryDark
        )
    }
}

@Composable
private fun SafeFarmerCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BeeftechSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp),
            content = content
        )
    }
}

@Composable
private fun SafeFarmerTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "—",
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.EditNote
) {

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = BeeftechPrimaryDark
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {

                Text(
                    text = placeholder,
                    color = BeeftechMutedText,
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            leadingIcon = {

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            BeeftechSoftAccent,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BeeftechPrimaryDark,
                        modifier = Modifier.size(19.dp)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(11.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BeeftechPrimaryDark,
                unfocusedBorderColor = BeeftechBorder,
                cursorColor = BeeftechPrimaryDark,
                focusedContainerColor = BeeftechWhite,
                unfocusedContainerColor = BeeftechWhite
            )
        )
    }
}

@Composable
private fun SafeFarmerSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BeeftechPrimaryDeep
        )
    ) {

        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
fun CoordinatesAndSaveScreenPreview() {

    BeeftechTheme {

        CoordinatesAndSaveContent(
            formData = CoordinatesSaveData(
                latitude = "-26.713700",
                longitude = "27.097900",
                organisationName = "Green Valley Cattle Farm"
            ),
            isLoadingLocation = false,
            isSaving = false,
            onFormDataChange = {},
            onBackClick = {},
            onUseCurrentLocationClick = {},
            onViewOnMapClick = {},
            onSaveClick = {}
        )
    }
}



