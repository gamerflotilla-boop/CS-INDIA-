package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
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

// Drawing stroke models
data class StrokePath(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isHighlight: Boolean = false
)

data class TextBoxItem(
    val text: String,
    val position: Offset,
    val color: Color
)

data class SignatureItem(
    val points: List<Offset>,
    val position: Offset,
    val color: Color,
    val width: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkupEditorScreen(
    viewModel: DocScannerViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val mockTarget by viewModel.activeMarkupTarget.collectAsStateWithLifecycle()

    if (mockTarget == null) {
        LaunchedEffect(Unit) { onCancel() }
        return
    }

    // Load active image
    val imagePath = remember(mockTarget) {
        when (val target = mockTarget) {
            is DocScannerViewModel.MarkupTarget.MainDoc -> target.document.processedImagePath
            is DocScannerViewModel.MarkupTarget.PageDoc -> target.page.processedImagePath
            else -> ""
        }
    }

    val imageFile = File(imagePath)
    val bitmap = remember(imagePath) {
        if (imageFile.exists()) {
            BitmapFactory.decodeFile(imageFile.absolutePath)
        } else {
            null
        }
    }

    if (bitmap == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Unable to load document image for editing.")
            Button(onClick = onCancel) { Text("Back") }
        }
        return
    }

    // Annotation Toolbar modes
    // "FREEHAND", "HIGHLIGHT", "TEXTBOX", "SIGNATURE"
    var activeTool by remember { mutableStateOf("FREEHAND") }
    var penColor by remember { mutableStateOf(Color.Red) }
    var penWidth by remember { mutableStateOf(8f) }

    // Path accumulator
    val freehandPaths = remember { mutableStateListOf<StrokePath>() }
    val textBoxes = remember { mutableStateListOf<TextBoxItem>() }
    val signatures = remember { mutableStateListOf<SignatureItem>() }

    // Drawing trace state
    var currentPoints = remember { mutableStateListOf<Offset>() }

    // Add TextBox interactive input
    var showTextBoxDialog by remember { mutableStateOf(false) }
    var pendingTextString by remember { mutableStateOf("") }

    // Draw Signature Pad modal
    var showSignaturePadDialog by remember { mutableStateOf(false) }
    val signaturePadPoints = remember { mutableStateListOf<Offset>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signature & Annotation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            // Render all drawings into the bitmap
                            val alteredBitmap = renderOverlayOnBitmap(
                                srcBitmap = bitmap,
                                paths = freehandPaths,
                                texes = textBoxes,
                                signs = signatures
                            )
                            when (val target = mockTarget) {
                                is DocScannerViewModel.MarkupTarget.MainDoc -> {
                                    viewModel.updateDocumentImage(context, target.document, alteredBitmap)
                                }
                                is DocScannerViewModel.MarkupTarget.PageDoc -> {
                                    viewModel.updatePageImage(context, target.page, alteredBitmap)
                                }
                                else -> {}
                            }
                            onSave()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("submit_markup_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Markup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tool settings (Color selector and status)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current state description icon and name
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tool: $activeTool",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Colors
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(Color.Red, Color.Black, Color.Blue, Color(0xFFE9C400) /* Yellow highlight */).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (penColor == color) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            penColor = color
                                            if (color == Color(0xFFE9C400)) {
                                                activeTool = "HIGHLIGHT"
                                            } else if (activeTool == "HIGHLIGHT") {
                                                activeTool = "FREEHAND"
                                            }
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Freehand Draw mode
                        FilledTonalIconButton(
                            onClick = {
                                activeTool = "FREEHAND"
                                if (penColor == Color(0xFFE9C400)) penColor = Color.Red
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (activeTool == "FREEHAND") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = "Freehand pen", tint = if (activeTool == "FREEHAND") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Textbox dialog activator
                        FilledTonalIconButton(
                            onClick = {
                                pendingTextString = ""
                                showTextBoxDialog = true
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (activeTool == "TEXTBOX") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = "Add Text box", tint = if (activeTool == "TEXTBOX") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Signature tablet activator
                        FilledTonalIconButton(
                            onClick = {
                                signaturePadPoints.clear()
                                showSignaturePadDialog = true
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (activeTool == "SIGNATURE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Draw, contentDescription = "Add Signature Stamper", tint = if (activeTool == "SIGNATURE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Undo last scribble
                        IconButton(
                            onClick = {
                                if (freehandPaths.isNotEmpty()) {
                                    freehandPaths.removeLast()
                                } else if (textBoxes.isNotEmpty()) {
                                    textBoxes.removeLast()
                                } else if (signatures.isNotEmpty()) {
                                    signatures.removeLast()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                        }

                        // Reset all
                        IconButton(
                            onClick = {
                                freehandPaths.clear()
                                textBoxes.clear()
                                signatures.clear()
                            }
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Clear all markups")
                        }
                    }
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            val visualAspect = 1.375f // ~ 1:1.375 page scaling
            val containerW = maxWidth.value
            val containerH = maxHeight.value

            var boxWidth: Float
            var boxHeight: Float

            if (containerW * visualAspect <= containerH) {
                boxWidth = containerW
                boxHeight = containerW * visualAspect
            } else {
                boxHeight = containerH
                boxWidth = containerH / visualAspect
            }

            Box(
                modifier = Modifier
                    .size(width = boxWidth.dp, height = boxHeight.dp)
                    .background(Color.White)
                    .clip(RoundedCornerShape(4.dp))
                    .pointerInput(activeTool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (activeTool == "FREEHAND" || activeTool == "HIGHLIGHT") {
                                    currentPoints.clear()
                                    currentPoints.add(offset)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (activeTool == "FREEHAND" || activeTool == "HIGHLIGHT") {
                                    currentPoints.add(change.position)
                                }
                            },
                            onDragEnd = {
                                if ((activeTool == "FREEHAND" || activeTool == "HIGHLIGHT") && currentPoints.isNotEmpty()) {
                                    freehandPaths.add(
                                        StrokePath(
                                            points = currentPoints.toList(),
                                            color = if (activeTool == "HIGHLIGHT") Color(0xFFE9C400).copy(alpha = 0.5f) else penColor,
                                            width = if (activeTool == "HIGHLIGHT") 24f else penWidth,
                                            isHighlight = (activeTool == "HIGHLIGHT")
                                        )
                                    )
                                    currentPoints.clear()
                                }
                            }
                        )
                    }
                    .pointerInput(activeTool) {
                        // Check for taps to place textbox or signatures
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (activeTool == "TEXTBOX" && pendingTextString.isNotEmpty()) {
                                    textBoxes.add(
                                        TextBoxItem(
                                            text = pendingTextString,
                                            position = offset,
                                            color = penColor
                                        )
                                    )
                                    pendingTextString = ""
                                    activeTool = "FREEHAND"
                                } else if (activeTool == "SIGNATURE" && signaturePadPoints.isNotEmpty()) {
                                    signatures.add(
                                        SignatureItem(
                                            points = signaturePadPoints.toList(),
                                            position = offset,
                                            color = Color.Black,
                                            width = 6f
                                        )
                                    )
                                    activeTool = "FREEHAND"
                                }
                            },
                            onDrag = { _, _ -> },
                            onDragEnd = {}
                        )
                    }
            ) {
                // The underlying document image
                AsyncImage(
                    model = imageFile,
                    contentDescription = "Document Canvas",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                // Overlapping Interactive Drawing Canvas
                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width
                    val scaleY = size.height

                    // 1. Draw existing finalized freehand lines & highlights
                    freehandPaths.forEach { sp ->
                        if (sp.points.size > 1) {
                            val path = Path()
                            path.moveTo(sp.points[0].x, sp.points[0].y)
                            for (i in 1 until sp.points.size) {
                                path.lineTo(sp.points[i].x, sp.points[i].y)
                            }
                            drawPath(
                                path = path,
                                color = sp.color,
                                style = Stroke(
                                    width = sp.width,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }

                    // 2. Draw current transient path in-progress
                    if (currentPoints.size > 1) {
                        val path = Path()
                        path.moveTo(currentPoints[0].x, currentPoints[0].y)
                        for (i in 1 until currentPoints.size) {
                            path.lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                        drawPath(
                            path = path,
                            color = if (activeTool == "HIGHLIGHT") Color(0xFFE9C400).copy(alpha = 0.5f) else penColor,
                            style = Stroke(
                                width = if (activeTool == "HIGHLIGHT") 24f else penWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    // 3. Draw Signatures stamped
                    signatures.forEach { sig ->
                        if (sig.points.isNotEmpty()) {
                            val path = Path()
                            // Center signature stamp over its position
                            // Map coordinates
                            val minX = sig.points.minOf { it.x }
                            val maxX = sig.points.maxOf { it.x }
                            val minY = sig.points.minOf { it.y }
                            val maxY = sig.points.maxOf { it.y }
                            val widthSig = maxX - minX
                            val heightSig = maxY - minY
                            
                            val centerX = sig.position.x - widthSig/2f
                            val centerY = sig.position.y - heightSig/2f

                            sig.points.forEachIndexed { idx, pt ->
                                val relativeX = pt.x - minX + centerX
                                val relativeY = pt.y - minY + centerY
                                if (idx == 0) {
                                    path.moveTo(relativeX, relativeY)
                                } else {
                                    path.lineTo(relativeX, relativeY)
                                }
                            }
                            drawPath(
                                path = path,
                                color = sig.color,
                                style = Stroke(width = sig.width, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // 4. Render Text boxes (We can place them as overlays using simple Box & Text inside)
                textBoxes.forEach { textItem ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (textItem.position.x / boxWidth).dp * boxWidth,
                                y = (textItem.position.y / boxHeight).dp * boxHeight
                            )
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .border(1.dp, textItem.color, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = textItem.text,
                            color = textItem.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Instructions label
                if (activeTool == "TEXTBOX" && pendingTextString.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            "Tap to place textbox: \"$pendingTextString\"",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                } else if (activeTool == "SIGNATURE" && signaturePadPoints.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            "Tap on document to Stamp Signature",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // --- Dialogs ---

        // Text box dialogue
        if (showTextBoxDialog) {
            AlertDialog(
                onDismissRequest = { showTextBoxDialog = false },
                title = { Text("Add Text Annotation") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pendingTextString,
                            onValueChange = { pendingTextString = it },
                            placeholder = { Text("Type annotations (e.g. APPROVED, PAID)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("textbox_input_markup"),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pendingTextString.trim().isNotEmpty()) {
                                activeTool = "TEXTBOX"
                            }
                            showTextBoxDialog = false
                        }
                    ) {
                        Text("Ready to Place")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextBoxDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Signature Scribble Pad dialogue
        if (showSignaturePadDialog) {
            AlertDialog(
                onDismissRequest = { showSignaturePadDialog = false },
                title = { Text("Draw Signature Pad") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Scribble your signature inside the box below:", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        
                        // Scribble pad
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color.White)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            signaturePadPoints.add(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            signaturePadPoints.add(change.position)
                                        },
                                        onDragEnd = {}
                                    )
                                }
                        ) {
                            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                                if (signaturePadPoints.size > 1) {
                                    val path = Path()
                                    path.moveTo(signaturePadPoints[0].x, signaturePadPoints[0].y)
                                    for (i in 1 until signaturePadPoints.size) {
                                        path.lineTo(signaturePadPoints[i].x, signaturePadPoints[i].y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.Black,
                                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { signaturePadPoints.clear() }) {
                                Text("Clear Pad")
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (signaturePadPoints.isNotEmpty()) {
                                activeTool = "SIGNATURE"
                            }
                            showSignaturePadDialog = false
                        },
                        enabled = signaturePadPoints.isNotEmpty()
                    ) {
                        Text("Use Signature")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSignaturePadDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Bakes annotations, signature shapes, pathways, highlights, and text strings directly onto the Bitmap.
 * Coordinate matching is safely scaled using normalized values.
 */
fun renderOverlayOnBitmap(
    srcBitmap: Bitmap,
    paths: List<StrokePath>,
    texes: List<TextBoxItem>,
    signs: List<SignatureItem>
): Bitmap {
    val altered = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(altered)
    
    val imgW = altered.width.toFloat()
    val imgH = altered.height.toFloat()

    // Assuming layout space had standard visual aspect ~ 1.375
    // Let's compute box dims corresponding to actual aspect ratios to accurately scale
    val layoutAspect = 1.375f
    var renderW: Float
    var renderH: Float

    // Align rendering space bounding boxes with the visual editor's scale ratios
    if (layoutAspect >= (imgH / imgW)) {
        renderW = imgW
        renderH = imgW * layoutAspect
    } else {
        renderH = imgH
        renderW = imgH / layoutAspect
    }

    val offsetLeft = (imgW - renderW) / 2f
    val offsetTop = (imgH - renderH) / 2f

    // Scale helpers
    fun scalePoint(pt: Offset): android.graphics.PointF {
        val normalizedX = pt.x / renderW
        val normalizedY = pt.y / renderH
        return android.graphics.PointF(
            normalizedX * imgW,
            normalizedY * imgH
        )
    }

    // 1. Draw drawings paths
    paths.forEach { sp ->
        if (sp.points.size > 1) {
            val paint = AndroidPaint().apply {
                color = sp.color.toArgb()
                style = AndroidPaint.Style.STROKE
                strokeWidth = (sp.width / renderW) * imgW
                strokeCap = AndroidPaint.Cap.ROUND
                isAntiAlias = true
            }
            val path = AndroidPath()
            val start = scalePoint(sp.points[0])
            path.moveTo(start.x, start.y)
            for (i in 1 until sp.points.size) {
                val next = scalePoint(sp.points[i])
                path.lineTo(next.x, next.y)
            }
            canvas.drawPath(path, paint)
        }
    }

    // 2. Draw signatures
    signs.forEach { sig ->
        if (sig.points.isNotEmpty()) {
            val paint = AndroidPaint().apply {
                color = android.graphics.Color.BLACK
                style = AndroidPaint.Style.STROKE
                strokeWidth = (sig.width / renderW) * imgW
                strokeCap = AndroidPaint.Cap.ROUND
                isAntiAlias = true
            }

            val minX = sig.points.minOf { it.x }
            val maxX = sig.points.maxOf { it.x }
            val minY = sig.points.minOf { it.y }
            val maxY = sig.points.maxOf { it.y }
            val widthSig = maxX - minX
            val heightSig = maxY - minY
            
            val centerX = sig.position.x - widthSig/2f
            val centerY = sig.position.y - heightSig/2f

            val path = AndroidPath()
            sig.points.forEachIndexed { idx, pt ->
                val relativeX = pt.x - minX + centerX
                val relativeY = pt.y - minY + centerY
                val scaled = scalePoint(Offset(relativeX, relativeY))
                if (idx == 0) {
                    path.moveTo(scaled.x, scaled.y)
                } else {
                    path.lineTo(scaled.x, scaled.y)
                }
            }
            canvas.drawPath(path, paint)
        }
    }

    // 3. Draw text boxes
    texes.forEach { textItem ->
        val paintText = AndroidPaint().apply {
            color = textItem.color.toArgb()
            textSize = (16f / renderW) * imgW
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
        
        val paintBg = AndroidPaint().apply {
            color = android.graphics.Color.WHITE
            style = AndroidPaint.Style.FILL
            alpha = 230
        }

        val paintBorder = AndroidPaint().apply {
            color = textItem.color.toArgb()
            style = AndroidPaint.Style.STROKE
            strokeWidth = (2f / renderW) * imgW
            isAntiAlias = true
        }

        val scaled = scalePoint(textItem.position)
        
        // Measure text size to draw neat background boxes
        val bounds = android.graphics.Rect()
        paintText.getTextBounds(textItem.text, 0, textItem.text.length, bounds)
        
        val pad = (6f / renderW) * imgW
        val rectText = android.graphics.RectF(
            scaled.x - pad,
            scaled.y - bounds.height() - pad,
            scaled.x + bounds.width() + pad,
            scaled.y + pad
        )
        
        canvas.drawRoundRect(rectText, 4f, 4f, paintBg)
        canvas.drawRoundRect(rectText, 4f, 4f, paintBorder)
        canvas.drawText(textItem.text, scaled.x, scaled.y, paintText)
    }

    return altered
}
