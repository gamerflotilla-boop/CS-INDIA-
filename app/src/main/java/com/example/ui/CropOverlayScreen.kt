package com.example.ui

import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropOverlayScreen(
    viewModel: DocScannerViewModel,
    onConfirmed: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val srcFile by viewModel.capturedImageFile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Observe corner point states in ViewModel
    val topLeftState = viewModel.cropTopLeft.collectAsStateWithLifecycle()
    val topRightState = viewModel.cropTopRight.collectAsStateWithLifecycle()
    val bottomRightState = viewModel.cropBottomRight.collectAsStateWithLifecycle()
    val bottomLeftState = viewModel.cropBottomLeft.collectAsStateWithLifecycle()

    var isProcessingWarp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Automatically run real-time AI auto-cropping Fitting on screen enter to reduce manual adjustments!
        viewModel.runAiAutoCrop()
        android.widget.Toast.makeText(context, "✨ AI auto-detected document edges!", android.widget.Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refine Scanning Area", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.runAiAutoCrop()
                            android.widget.Toast.makeText(context, "✨ Edge coordinates aligned!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("ai_auto_fit_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Fit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Auto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Retake", color = MaterialTheme.colorScheme.error)
                    }

                    if (isProcessingWarp) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick = {
                                isProcessingWarp = true
                                // Trigger background Perspective Correction & Filter processing
                                viewModel.applyPerspectiveFilterAndSave(context, "Original") {
                                    isProcessingWarp = false
                                    onConfirmed()
                                }
                            },
                            modifier = Modifier.testTag("confirm_crop_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Confirm")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Crop")
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121212)) // Deep dark canvas backdrop
                .testTag("crop_workspace"),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight

            if (srcFile != null && srcFile!!.exists()) {
                // Background Original Image
                AsyncImage(
                    model = srcFile,
                    contentDescription = "Original scan target",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Drag and Drop Corners overlay
                val tl = topLeftState.value
                val tr = topRightState.value
                val br = bottomRightState.value
                val bl = bottomLeftState.value

                // Translate normalized [0, 1] coordinates to display pixels
                val containerWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { containerWidth.toPx() }
                val containerHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { containerHeight.toPx() }

                val offsetTLX = tl.x * containerWidthPx
                val offsetTLY = tl.y * containerHeightPx
                val offsetTRX = tr.x * containerWidthPx
                val offsetTRY = tr.y * containerHeightPx
                val offsetBRX = br.x * containerWidthPx
                val offsetBRY = br.y * containerHeightPx
                val offsetBLX = bl.x * containerWidthPx
                val offsetBLY = bl.y * containerHeightPx

                // Drawing lines between the 4 draggable handle points
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(offsetTLX, offsetTLY)
                        lineTo(offsetTRX, offsetTRY)
                        lineTo(offsetBRX, offsetBRY)
                        lineTo(offsetBLX, offsetBLY)
                        close()
                    }

                    // Green alignment perimeter
                    drawPath(
                        path = path,
                        color = Color.Green,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    
                    // Translucent highlight overlay fill
                    drawPath(
                        path = path,
                        color = Color.Green.copy(alpha = 0.15f)
                    )
                }

                // Render Draggable Corners Handles as custom touch points
                DragCornerHandle(
                    offsetX = offsetTLX,
                    offsetY = offsetTLY,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    onPositionChanged = { proposed ->
                        viewModel.cropTopLeft.value = proposed
                    },
                    color = Color.Green,
                    testTagLabel = "handle_top_left"
                )

                DragCornerHandle(
                    offsetX = offsetTRX,
                    offsetY = offsetTRY,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    onPositionChanged = { proposed ->
                        viewModel.cropTopRight.value = proposed
                    },
                    color = Color.Green,
                    testTagLabel = "handle_top_right"
                )

                DragCornerHandle(
                    offsetX = offsetBRX,
                    offsetY = offsetBRY,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    onPositionChanged = { proposed ->
                        viewModel.cropBottomRight.value = proposed
                    },
                    color = Color.Green,
                    testTagLabel = "handle_bottom_right"
                )

                DragCornerHandle(
                    offsetX = offsetBLX,
                    offsetY = offsetBLY,
                    containerWidthPx = containerWidthPx,
                    containerHeightPx = containerHeightPx,
                    onPositionChanged = { proposed ->
                        viewModel.cropBottomLeft.value = proposed
                    },
                    color = Color.Green,
                    testTagLabel = "handle_bottom_left"
                )

                // Drag Info Hint Label
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = "Drag handles to align with the paper edges",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DragCornerHandle(
    offsetX: Float,
    offsetY: Float,
    containerWidthPx: Float,
    containerHeightPx: Float,
    onPositionChanged: (PointF) -> Unit,
    color: Color,
    testTagLabel: String
) {
    val handleRadius = 24.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    // Convert current position in pixels back to Dp offset so we can place the handle
    val offsetXInDp = with(density) { offsetX.toDp() }
    val offsetYInDp = with(density) { offsetY.toDp() }

    Box(
        modifier = Modifier
            .size(handleRadius * 2)
            .offset(
                x = offsetXInDp - handleRadius,
                y = offsetYInDp - handleRadius
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Calculate proposed coordinates bounded inside container width and height
                    val targetX = (offsetX + dragAmount.x).coerceIn(0f, containerWidthPx)
                    val targetY = (offsetY + dragAmount.y).coerceIn(0f, containerHeightPx)
                    
                    val normalizedPoint = PointF(
                        targetX / containerWidthPx,
                        targetY / containerHeightPx
                    )
                    onPositionChanged(normalizedPoint)
                }
            }
            .testTag(testTagLabel)
    ) {
        // Double concentric visual circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(color.copy(alpha = 0.35f), CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, color, CircleShape)
            )
        }
    }
}
