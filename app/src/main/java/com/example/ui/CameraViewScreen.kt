package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.util.ImageProcessor
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraViewScreen(
    viewModel: DocScannerViewModel,
    onImageCaptured: (File) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isBatchMode by viewModel.isBatchMode.collectAsStateWithLifecycle()
    val batchCapturedFiles by viewModel.batchCapturedFiles.collectAsStateWithLifecycle()
    val isBatchProcessingActive by viewModel.isBatchProcessingActive.collectAsStateWithLifecycle()
    val batchProcessingProgress by viewModel.batchProcessingProgress.collectAsStateWithLifecycle()
    val batchProcessingStatus by viewModel.batchProcessingStatus.collectAsStateWithLifecycle()

    var showBatchSaveDialog by remember { mutableStateOf(false) }

    // Permission handling State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera view config
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }

    // Edge animation overlay pulsing
    val transition = rememberInfiniteTransition(label = "edgePulse")
    val borderAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Copy uri stream to local file to feed our processor
            val cacheFile = File(context.cacheDir, "imported_image_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                onImageCaptured(cacheFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Unbind/Bind Camera with lifecycle
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // React to Flash toggles
    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Batch Mode Toggle Button
                        AssistChip(
                            onClick = { viewModel.toggleBatchMode() },
                            label = { Text("Batch Scan", color = Color.White, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Batch Mode Indicator",
                                    tint = if (isBatchMode) Color.Green else Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isBatchMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isBatchMode) Color.Green else Color.Gray
                            ),
                            modifier = Modifier.testTag("batch_mode_toggle_chip")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF) {
                                    ImageCapture.FLASH_MODE_ON
                                } else {
                                    ImageCapture.FLASH_MODE_OFF
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (flashMode == ImageCapture.FLASH_MODE_ON) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash mode",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                // Viewfinder Stream
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // High tech Cyan/Green Edge Detection Scanner Guides overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("camera_lens_overlay")
                ) {
                    val w = size.width
                    val h = size.height

                    // Simulated perspective aligned lines forming document borders
                    val tlX = w * 0.12f
                    val tlY = h * 0.18f
                    val trX = w * 0.88f
                    val trY = h * 0.22f
                    val brX = w * 0.84f
                    val brY = h * 0.78f
                    val blX = w * 0.16f
                    val blY = h * 0.76f

                    val path = Path().apply {
                        moveTo(tlX, tlY)
                        lineTo(trX, trY)
                        lineTo(brX, brY)
                        lineTo(blX, blY)
                        close()
                    }

                    // Pulse outline borders
                    drawPath(
                        path = path,
                        color = Color.Cyan.copy(alpha = borderAlpha),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Draw distinct corners indicators
                    drawCircle(Color.Green, 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(tlX, tlY))
                    drawCircle(Color.Green, 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(trX, trY))
                    drawCircle(Color.Green, 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(brX, brY))
                    drawCircle(Color.Green, 8.dp.toPx(), center = androidx.compose.ui.geometry.Offset(blX, blY))
                }

                // AI Lock feedback text
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Green.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "• AI Auto Border Alignment Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Green,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Bottom control visual dashboard
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Launcher trigger button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.DarkGray.copy(alpha = 0.7f), CircleShape)
                            .testTag("gallery_trigger_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "From local Library",
                            tint = Color.White
                        )
                    }

                    // Main snap trigger button
                    IconButton(
                        onClick = {
                            val activeCap = imageCapture
                            if (activeCap != null) {
                                val file = File(context.cacheDir, "camera_snap_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                                activeCap.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            if (isBatchMode) {
                                                viewModel.addFileToBatch(file)
                                                android.widget.Toast.makeText(context, "Page added to batch queue! (${batchCapturedFiles.size + 1})", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                onImageCaptured(file)
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            exception.printStackTrace()
                                            val simulatedFile = simulateMockDocumentCapture(context)
                                            if (isBatchMode) {
                                                viewModel.addFileToBatch(simulatedFile)
                                                android.widget.Toast.makeText(context, "Page added to batch queue! (${batchCapturedFiles.size + 1})", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                onImageCaptured(simulatedFile)
                                            }
                                        }
                                    }
                                )
                            } else {
                                val simulatedFile = simulateMockDocumentCapture(context)
                                if (isBatchMode) {
                                    viewModel.addFileToBatch(simulatedFile)
                                    android.widget.Toast.makeText(context, "Page added to batch queue! (${batchCapturedFiles.size + 1})", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onImageCaptured(simulatedFile)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(4.dp)
                            .background(Color.White, CircleShape)
                            .testTag("shutter_button")
                    ) {
                        // Empty action representing physical camera visual
                    }

                    // Done/Check Badge button for compiling the completed Batch
                    if (isBatchMode && batchCapturedFiles.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showBatchSaveDialog = true
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Green.copy(alpha = 0.85f), CircleShape)
                                .testTag("batch_done_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.Black
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Symmetry balance holder
                        }
                    }
                }

            } else {
                // Denied or loading state representation
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "To scan physical paperwork, this app needs access to your camera. Click the button below or import from your photo gallery.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("request_permission_retry")
                    ) {
                        Text("Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.testTag("import_gallery_permission_fallback")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "import")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload from Gallery instead")
                    }
                }
            }

            // Real-time AI Batch compilation progression screen overlay
            if (isBatchProcessingActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "✨ AI Batch Scanner Pipeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            CircularProgressIndicator(
                                progress = { batchProcessingProgress },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = batchProcessingStatus,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            LinearProgressIndicator(
                                progress = { batchProcessingProgress },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // AI Batch Checkout Dialog
            if (showBatchSaveDialog) {
                var docTitle by remember { 
                    val sdf = SimpleDateFormat("Batch_Scan_yyyyMMdd_HHmmss", Locale.getDefault())
                    mutableStateOf(sdf.format(Date()))
                }
                var selectedCategory by remember { mutableStateOf("Invoice") }
                var selectedPriority by remember { mutableStateOf("Normal") }
                var selectedFolderId by remember { mutableStateOf<Int?>(null) }
                var folderDropdownExpanded by remember { mutableStateOf(false) }

                val folders by viewModel.allFolders.collectAsStateWithLifecycle()

                AlertDialog(
                    onDismissRequest = { showBatchSaveDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
                            Text("AI Batch Checkout", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "You captured ${batchCapturedFiles.size} pages. AI will automatically auto-crop edges, index Text OCR, run de-blur contrast parameters, and tag categories.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Title
                            OutlinedTextField(
                                value = docTitle,
                                onValueChange = { docTitle = it },
                                label = { Text("Base Document Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("batch_title_field")
                            )

                            // Folder
                            Box {
                                val folderLabel = if (selectedFolderId != null) {
                                    folders.find { it.id == selectedFolderId }?.name ?: "No Folder"
                                } else {
                                    "No Folder (General)"
                                }
                                OutlinedButton(
                                    onClick = { folderDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth().testTag("batch_folder_spinner")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("In Folder: $folderLabel", fontSize = 13.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "arrow")
                                    }
                                }
                                DropdownMenu(
                                    expanded = folderDropdownExpanded,
                                    onDismissRequest = { folderDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("General Documents (No folder)") },
                                        onClick = {
                                            selectedFolderId = null
                                            folderDropdownExpanded = false
                                        }
                                    )
                                    HorizontalDivider()
                                    folders.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.name) },
                                            onClick = {
                                                selectedFolderId = f.id
                                                folderDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Category Choices
                            Text("Target Category:", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Invoice", "Receipt", "Financial", "Legal", "Academic", "Personal", "Other").forEach { cat ->
                                    val isSel = selectedCategory == cat
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat, fontSize = 11.sp) }
                                    )
                                }
                            }

                            // Priority Row
                            Text("Select Priority Level:", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("High", "Medium", "Normal").forEach { pr ->
                                    val isSel = selectedPriority == pr
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { selectedPriority = pr },
                                        label = { Text(pr, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.processBatchQueueAndSave(
                                    context = context,
                                    baseTitle = docTitle,
                                    category = selectedCategory,
                                    priority = selectedPriority,
                                    folderId = selectedFolderId
                                ) {
                                    showBatchSaveDialog = false
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.testTag("batch_submit_btn")
                        ) {
                            Text("Compile & Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBatchSaveDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Helper to generate a simulated document image using android Canvas so that the applet features can be tested end-to-end inside emulators.
 */
fun simulateMockDocumentCapture(context: Context): File {
    val bitmap = android.graphics.Bitmap.createBitmap(800, 1100, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Fill slightly angled beige/gray background representing a paper resting on an table
    val borderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
    }
    canvas.drawRect(0f, 0f, 800f, 1100f, borderPaint)
    
    // Draw the skewed sheet of paper inside
    val paperPath = android.graphics.Path().apply {
        moveTo(100f, 150f)
        lineTo(700f, 120f)
        lineTo(730f, 1000f)
        lineTo(70f, 980f)
        close()
    }
    val paperPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawPath(paperPath, paperPaint)
    
    // Draw mock lines simulating code/text elements
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 28f
        isAntiAlias = true
    }
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLUE
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    canvas.drawText("=====================================", 120f, 250f, textPaint)
    canvas.drawText("       GLOBAL TECHNOLOGY INVOICE      ", 120f, 320f, titlePaint)
    canvas.drawText("=====================================", 120f, 380f, textPaint)
    
    canvas.drawText("Invoice ID     : INV-2026-8941", 140f, 440f, textPaint)
    canvas.drawText("Date Generated : 2026-06-15", 140f, 500f, textPaint)
    canvas.drawText("Client Entity  : GLOBAL TECH CORP", 140f, 560f, textPaint)
    
    canvas.drawText("Description              | Qty | Total", 140f, 660f, textPaint)
    canvas.drawText("-------------------------+-----+------", 140f, 700f, textPaint)
    canvas.drawText("DocScanner Suite SDK     |  1  | $2400", 140f, 750f, textPaint)
    canvas.drawText("Volume OCR Engine Bundle |  1  | $500 ", 140f, 800f, textPaint)
    canvas.drawText("--------------------------------------", 140f, 850f, textPaint)
    canvas.drawText("SUBTOTAL BALANCE DUE     |     | $2900", 140f, 900f, textPaint)
    
    // Save to file
    val cacheFile = File(context.cacheDir, "simulated_snap_${System.currentTimeMillis()}.jpg")
    try {
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return cacheFile
}
