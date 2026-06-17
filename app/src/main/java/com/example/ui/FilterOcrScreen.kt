package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Folder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterOcrScreen(
    viewModel: DocScannerViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Observe state from ViewModel
    val processedBmp by viewModel.processedBitmap.collectAsStateWithLifecycle()
    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val extractedText by viewModel.extractedOcrText.collectAsStateWithLifecycle()
    val isOcrRunning by viewModel.isProcessingOcr.collectAsStateWithLifecycle()
    val aiInfo by viewModel.aiEnhancementInfo.collectAsStateWithLifecycle()

    // Observe Gemini background category suggestion states
    val aiSuggestedCategory by viewModel.aiSuggestedCategory.collectAsStateWithLifecycle()
    val isSuggestingCategory by viewModel.isSuggestingCategory.collectAsStateWithLifecycle()

    // Document Save parameters state
    var documentTitle by remember { 
        val defaultName = "Scan_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        mutableStateOf(defaultName) 
    }
    var selectedCategory by remember { mutableStateOf("Invoice") }
    var selectedPriority by remember { mutableStateOf("Normal") }
    var selectedFolderId by remember { mutableStateOf<Int?>(null) }
    var folderDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Invoice", "Receipt", "Financial", "Legal", "Academic", "Personal", "Other")
    val priorities = listOf("High", "Medium", "Normal")

    // Automatically trigger Gemini background suggestion service when OCR text updates or is loaded
    LaunchedEffect(extractedText) {
        if (extractedText.isNotBlank()) {
            viewModel.runCategorySuggestion(extractedText)
        }
    }

    // Auto-select when a high-probability category is suggested by background service
    LaunchedEffect(aiSuggestedCategory) {
        val suggestion = aiSuggestedCategory
        if (suggestion != null && categories.contains(suggestion)) {
            selectedCategory = suggestion
        }
    }

    val multiDocId by viewModel.activeMultiPageDocId.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enhance & Extract", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        viewModel.saveCompletedDocument(
                            context = context,
                            title = documentTitle,
                            category = selectedCategory,
                            priority = selectedPriority,
                            folderId = selectedFolderId,
                            onSaved = onSaved
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                        .testTag("save_doc_submit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(if (multiDocId != null) Icons.Default.AddCircleOutline else Icons.Default.Save, contentDescription = "Save Document")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (multiDocId != null) "Append Page to Document" else "Save To Docs Library",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
        ) {
            // Document Image preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (processedBmp != null) {
                    AsyncImage(
                        model = processedBmp,
                        contentDescription = "Deskewed Result",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Copilot Pipeline Hub
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("ai_pipeline_hub"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Scanner pipeline",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI CO-PILOT ANALYSIS HUB",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val detectedTypeLabel = remember(extractedText) {
                        val txt = extractedText.lowercase(Locale.getDefault())
                        when {
                            txt.contains("receipt") -> "Receipt"
                            txt.contains("invoice") -> "Invoice"
                            txt.contains("passport") || txt.contains("id card") || txt.contains("identity") || txt.contains("license") -> "Identity Card"
                            else -> "Book Page / Standard Document"
                        }
                    }

                    Text(
                        text = "• AI OCR Document Classifier: Auto-categorised as \"$selectedCategory\"\n" +
                               "• Auto-Detected Photo Characteristics: Style is \"$detectedTypeLabel\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (aiInfo != null) {
                        Text(
                            text = "✨ $aiInfo",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    var isRunningOptimization by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            isRunningOptimization = true
                            val pipelineTarget = when {
                                detectedTypeLabel.contains("Receipt") -> "Receipt"
                                detectedTypeLabel.contains("Identity") -> "ID Card"
                                else -> "Book Page"
                            }
                            viewModel.runAiEnhancementPipeline(context, pipelineTarget) {
                                isRunningOptimization = false
                                android.widget.Toast.makeText(context, "✨ AI pipeline optimized contrast, brightness & applied de-blurring filters!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .testTag("ai_pipeline_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isRunningOptimization) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Optimize", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run AI Image Enhancement Pipeline", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filters selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "IMAGE AUGMENTATION FILTERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Original", "Magic Color", "B&W / Grayscale", "AI Smart Clean").forEach { filter ->
                        val isSelected = activeFilter == filter
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                 else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .clickable {
                                    viewModel.applyPerspectiveFilterAndSave(context, filter) {}
                                }
                                .testTag("filter_opt_${filter.replace(" ", "_")}"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (filter == "AI Smart Clean") {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Icon",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFFF9933),
                                        modifier = Modifier.size(12.dp).padding(end = 4.dp)
                                    )
                                }
                                Text(
                                    text = filter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OCR Extraction Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "OPTICAL CHARACTER RECOGNITION (OCR)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Local OCR
                    Button(
                        onClick = { viewModel.extractTextFromActiveImage(context, useAiOcr = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ocr_local_btn")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = "Local scan OCR", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Local OCR", fontSize = 12.sp)
                    }

                    // Advanced Gemini AI OCR
                    Button(
                        onClick = { viewModel.extractTextFromActiveImage(context, useAiOcr = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("ocr_ai_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI OCR Scanner", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI OCR scan", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Texts area display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    if (isOcrRunning) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Decoding document text...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (extractedText.isNotEmpty()) "Extracted OCR Text:" else "No Extracted OCR Text yet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (extractedText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(extractedText))
                                            android.widget.Toast.makeText(context, "Copied text to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("copy_clipboard_btn")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Clipboard", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            OutlinedTextField(
                                value = extractedText,
                                onValueChange = { viewModel.extractedOcrText.value = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 220.dp)
                                    .testTag("ocr_editable_text_input"),
                                placeholder = { Text("Extract document text contents above, or manually type scan details here to keep them indexed.") },
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            if (multiDocId != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Multi-page Mode",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Multi-Page Scan Mode",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 14.sp
                            )
                            Text(
                                "This enhanced scan will be automatically appended as an additional page to your existing document.",
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                // Document Meta details Form Setup (Folders, Category, Priority, and Title)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "DOCUMENT METADATA & ORGANIZATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Title Input
                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it },
                        label = { Text("Document Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_title_field"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Select Folder dialog spinner
                    Box {
                        val folderNameLabel = if (selectedFolderId != null) {
                            folders.find { it.id == selectedFolderId }?.name ?: "No Folder"
                        } else {
                            "No Folder (General)"
                        }
                        
                        OutlinedButton(
                            onClick = { folderDropdownExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("select_folder_spinner"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Save In Folder: $folderNameLabel", fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = folderDropdownExpanded,
                            onDismissRequest = { folderDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text("General Documents (No folder)") },
                                onClick = {
                                    selectedFolderId = null
                                    folderDropdownExpanded = false
                                }
                            )
                            HorizontalDivider()
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        selectedFolderId = folder.id
                                        folderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Selection Cards
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Select Document Category:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (isSuggestingCategory) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "AI Classifying...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (aiSuggestedCategory != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clickable {
                                            aiSuggestedCategory?.let {
                                                if (categories.contains(it)) {
                                                    selectedCategory = it
                                                }
                                            }
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Suggested",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "AI suggests: $aiSuggestedCategory",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSel = selectedCategory == cat
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag("category_chip_$cat")
                                )
                            }
                        }
                    }

                    // Priority Indicator selections
                    Column {
                        Text("Select Priority Indicator Label:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            priorities.forEach { pr ->
                                val isSel = selectedPriority == pr
                                val tintColor = when (pr) {
                                    "High" -> if (isSel) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                                    "Medium" -> if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    else -> if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = tintColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPriority = pr }
                                        .testTag("priority_chip_$pr"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pr.uppercase(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp)) // End filler spacer
        }
    }
}
