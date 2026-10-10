package com.beeftech.tagscanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.beeftech.database.util.TagColour
import com.beeftech.database.util.TagNamingUtils
import com.beeftech.tagscanner.model.EarTagScanResult
import com.beeftech.tagscanner.scan.EarTagAnalyzer
import java.util.concurrent.Executors

private val ScannerPrimaryDeep = Color(0xFF4F6256)
private val ScannerText = Color(0xFF2F3632)
private val ScannerMuted = Color(0xFF6F756F)
private val ScannerBorder = Color(0xFFD6D9D1)
private val ScannerSurface = Color(0xFFFAF9F2)
private val ScannerWhite = Color(0xFFFFFFFF)

// Guide: centre 80% width x 35% height of the frame.
private val GuideFraction = RectF(0.10f, 0.325f, 0.90f, 0.675f)

@Composable
fun EarTagScannerScreen(
    onTagScanned: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    var permissionAsked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionAsked = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        ScannerCameraContent(onTagScanned = onTagScanned, onCancel = onCancel)
    } else if (permissionAsked) {
        PermissionDenied(
            onEnterManually = onCancel,
            onTryAgain = { launcher.launch(Manifest.permission.CAMERA) }
        )
    }
}

@Composable
private fun PermissionDenied(onEnterManually: () -> Unit, onTryAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(ScannerSurface).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera access needed", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ScannerText)
        Spacer(Modifier.height(8.dp))
        Text(
            "BeefTech needs the camera to read the number and colour on an ear tag. " +
                "You can still type the tag number by hand.",
            color = ScannerMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onTryAgain,
            colors = ButtonDefaults.buttonColors(containerColor = ScannerPrimaryDeep)
        ) { Text("Try again") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onEnterManually) { Text("Enter manually", color = ScannerText) }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScannerCameraContent(
    onTagScanned: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var stableResult by remember { mutableStateOf<EarTagScanResult?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        EarTagAnalyzer(GuideFraction) { result -> stableResult = result }
    }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            provider = cameraProvider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            @Suppress("DEPRECATION")
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
            analyzer.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        GuideOverlay()

        Text(
            text = "Fill the frame with the tag number",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp, start = 24.dp, end = 24.dp)
        )

        IconButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) { Icon(Icons.Filled.Close, contentDescription = "Close scanner", tint = Color.White) }

        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            IconButton(
                onClick = {
                    torchOn = !torchOn
                    camera?.cameraControl?.enableTorch(torchOn)
                },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Toggle torch",
                    tint = Color.White
                )
            }
        }

        stableResult?.let { result ->
            ConfirmationCard(
                result = result,
                modifier = Modifier.align(Alignment.BottomCenter),
                onUse = onTagScanned,
                onRescan = {
                    stableResult = null
                    analyzer.resume()
                }
            )
        }
    }

    // Pause analysis while the confirmation card is showing.
    LaunchedEffect(stableResult) {
        if (stableResult != null) analyzer.pause()
    }
}

@Composable
private fun GuideOverlay() {
    Canvas(
        modifier = Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawRect(Color.Black.copy(alpha = 0.55f))
        val topLeft = Offset(size.width * GuideFraction.left, size.height * GuideFraction.top)
        val guideSize = Size(
            size.width * (GuideFraction.right - GuideFraction.left),
            size.height * (GuideFraction.bottom - GuideFraction.top)
        )
        val corner = CornerRadius(16.dp.toPx())
        drawRoundRect(Color.Transparent, topLeft, guideSize, corner, blendMode = BlendMode.Clear)
        drawRoundRect(Color.White, topLeft, guideSize, corner, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun ConfirmationCard(
    result: EarTagScanResult,
    onUse: (String) -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var colour by remember(result) { mutableStateOf(result.colour) }
    var sequence by remember(result) { mutableStateOf(result.sequence) }

    val tagId = colour?.let { c ->
        sequence.toLongOrNull()?.takeIf { sequence.length in 1..7 }?.let { TagNamingUtils.formatTag(c, it) }
    }
    val canUse = tagId != null && TagNamingUtils.validateTag(tagId)

    Card(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ScannerSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "TAG COLOUR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = ScannerPrimaryDeep
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagColour.entries.forEach { option ->
                    ColourChip(
                        colour = option,
                        selected = colour == option,
                        onClick = { colour = option },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (colour == null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Couldn't detect colour. Tap the tag colour.",
                    fontSize = 12.sp,
                    color = Color(0xFFB71C1C)
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = sequence,
                onValueChange = { input -> sequence = input.filter { it.isDigit() }.take(7) },
                label = { Text("Tag number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = tagId ?: "—",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = ScannerText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) {
                    Text("Rescan", color = ScannerText)
                }
                Button(
                    onClick = { tagId?.let(onUse) },
                    enabled = canUse,
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerPrimaryDeep),
                    modifier = Modifier.weight(1f)
                ) { Text("Use this tag") }
            }
        }
    }
}

@Composable
private fun ColourChip(
    colour: TagColour,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipBgColor = when (colour) {
        TagColour.BLUE -> Color(0xFFE3F2FD)
        TagColour.RED -> Color(0xFFFFEBEE)
        TagColour.GREEN -> Color(0xFFE8F5E9)
        TagColour.YELLOW -> Color(0xFFFFFDE7)
    }
    val chipTextColor = when (colour) {
        TagColour.BLUE -> Color(0xFF0D47A1)
        TagColour.RED -> Color(0xFFB71C1C)
        TagColour.GREEN -> Color(0xFF1B5E20)
        TagColour.YELLOW -> Color(0xFFF57F17)
    }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) chipBgColor else ScannerWhite,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) chipTextColor else ScannerBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).background(chipTextColor, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(
                text = colour.prefix,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) chipTextColor else ScannerText
            )
        }
    }
}
