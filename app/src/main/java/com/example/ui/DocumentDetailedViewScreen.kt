package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.Document
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailedViewScreen(
    document: Document,
    viewModel: DocScannerViewModel,
    onBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToMarkup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val formattedDate = remember(document.timestamp) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(document.timestamp))
    }

    val pages by viewModel.getPagesForDocument(document.id).collectAsStateWithLifecycle(emptyList())
    var activePageIndex by remember { mutableStateOf(0) }

    // Clamp active state boundaries
    if (activePageIndex > pages.size) {
        activePageIndex = pages.size
    }

    val activeImagePath = remember(activePageIndex, pages, document) {
        if (activePageIndex == 0) {
            document.processedImagePath
        } else {
            pages.getOrNull(activePageIndex - 1)?.processedImagePath ?: document.processedImagePath
        }
    }

    val activeExtractedText = remember(activePageIndex, pages, document) {
        if (activePageIndex == 0) {
            document.extractedText
        } else {
            pages.getOrNull(activePageIndex - 1)?.extractedText ?: ""
        }
    }

    LaunchedEffect(activePageIndex) {
        viewModel.clearAiStates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.deleteDocument(document.id)
                            onBack()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Document", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export PDF Button
                    Button(
                        onClick = { viewModel.shareDocumentAsPdf(context, document) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_share_pdf_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Pdf Icon")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export A4 PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Export Plain Text Button
                    Button(
                        onClick = { viewModel.shareDocumentAsTxt(context, document) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("detail_share_txt_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Article, contentDescription = "Text Icon")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export TXT", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
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
            // Enhanced Scan image container banner
            val pathFile = File(activeImagePath)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (pathFile.exists()) {
                    AsyncImage(
                        model = pathFile,
                        contentDescription = "Document Scan Visual",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = "Not found",
                        tint = Color.LightGray,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Annotation or re-ordering toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Markup and Sign Button
                Button(
                    onClick = {
                        if (activePageIndex == 0) {
                            viewModel.activeMarkupTarget.value = DocScannerViewModel.MarkupTarget.MainDoc(document)
                        } else {
                            val activePage = pages.getOrNull(activePageIndex - 1)
                            if (activePage != null) {
                                viewModel.activeMarkupTarget.value = DocScannerViewModel.MarkupTarget.PageDoc(activePage)
                            }
                        }
                        onNavigateToMarkup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("annotate_page_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Annotate", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Markup / Sign", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Reorder controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            viewModel.movePageUp(document, pages, activePageIndex)
                            if (activePageIndex > 0) activePageIndex--
                        },
                        enabled = activePageIndex > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Move Left", modifier = Modifier.size(16.dp))
                    }
                    
                    Text(
                        text = "${activePageIndex + 1}/${pages.size + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            viewModel.movePageDown(document, pages, activePageIndex)
                            if (activePageIndex < pages.size) activePageIndex++
                        },
                        enabled = activePageIndex < pages.size,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Move Right", modifier = Modifier.size(16.dp))
                    }
                }

                // Delete Page if not main
                if (activePageIndex > 0) {
                    IconButton(
                        onClick = {
                            val pg = pages.getOrNull(activePageIndex - 1)
                            if (pg != null) {
                                viewModel.deletePage(pg)
                                activePageIndex = 0
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Spacer(modifier = Modifier.width(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal Document Pages & Management List
            Text(
                text = "DOCUMENT PAGES & MANAGEMENT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First Page Cover Thumb
                val isCoverSelected = activePageIndex == 0
                Card(
                    modifier = Modifier
                        .size(width = 64.dp, height = 88.dp)
                        .clickable { activePageIndex = 0 }
                        .border(
                            width = if (isCoverSelected) 2.dp else 1.dp,
                            color = if (isCoverSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = File(document.processedImagePath),
                            contentDescription = "Cover preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cover", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Pages List
                pages.forEachIndexed { index, page ->
                    val pageIdx = index + 1
                    val isSelected = activePageIndex == pageIdx
                    Card(
                        modifier = Modifier
                            .size(width = 64.dp, height = 88.dp)
                            .clickable { activePageIndex = pageIdx }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = File(page.processedImagePath),
                                contentDescription = "Page $pageIdx preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Page ${pageIdx + 1}", color = Color.White, fontSize = 8.sp)
                            }
                        }
                    }
                }

                // "+" Add Page Virtual Thumbnail
                Card(
                    modifier = Modifier
                        .size(width = 64.dp, height = 88.dp)
                        .clickable {
                            viewModel.startAddPageFlow(document.id)
                            onNavigateToCamera()
                        }
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add page icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add Page", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata panel Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SCAN METADATA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Created on", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(formattedDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category label", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(document.category) },
                            modifier = Modifier.height(24.dp),
                            enabled = false
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Priority status", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(document.priorityLabel.uppercase()) },
                            modifier = Modifier.height(24.dp),
                            enabled = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gemini AI Copilot Card
            val aiSummary by viewModel.aiSummaryResult.collectAsStateWithLifecycle()
            val aiTranslation by viewModel.aiTranslationResult.collectAsStateWithLifecycle()
            val aiActionItems by viewModel.aiActionItemsResult.collectAsStateWithLifecycle()
            val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
            val aiError by viewModel.aiErrorMsg.collectAsStateWithLifecycle()

            var showLanguageDropdown by remember { mutableStateOf(false) }
            val languages = listOf("Hindi", "Spanish", "French", "German")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI Copilot",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "GEMINI AI COPILOT",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v3.5 Flash",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Leverage advanced AI to instantaneously analyze, summarize, extract tasks, or translate this scanned page.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 15.sp
                    )

                    // Prompt trigger chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Summary Chip
                        FilterChip(
                            selected = aiSummary != null,
                            onClick = { viewModel.runGeminiSummary(activeExtractedText) },
                            label = { Text("Summarize", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(12.dp))
                            },
                            modifier = Modifier.height(32.dp)
                        )

                        // Action Items Chip
                        FilterChip(
                            selected = aiActionItems != null,
                            onClick = { viewModel.runGeminiActionItems(activeExtractedText) },
                            label = { Text("Tasks & Keys", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            modifier = Modifier.height(32.dp)
                        )

                        // Translate Chip with dropdown trigger
                        Box {
                            FilterChip(
                                selected = aiTranslation != null,
                                onClick = { showLanguageDropdown = true },
                                label = { Text("Translate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(12.dp))
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                },
                                modifier = Modifier.height(32.dp)
                            )

                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false }
                            ) {
                                languages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang, fontSize = 12.sp) },
                                        onClick = {
                                            showLanguageDropdown = false
                                            viewModel.runGeminiTranslation(activeExtractedText, lang)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Loader
                    if (isAiLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "AI is thinking via Gemini Gateway...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Error display
                    if (aiError != null) {
                        Text(
                            text = aiError ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Result Body
                    val activeResult = aiSummary ?: aiActionItems ?: aiTranslation
                    if (activeResult != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val headerTitle = when {
                                    aiSummary != null -> "Executive AI Summary"
                                    aiActionItems != null -> "AI Entity & Tasks"
                                    aiTranslation != null -> "AI Translation"
                                    else -> "AI Copilot Response"
                                }
                                Text(
                                    text = headerTitle.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(activeResult))
                                        android.widget.Toast.makeText(context, "Copied AI results!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy AI Results",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = activeResult,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OCR Extraction Result Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXTRACTED TEXT (OCR)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (activeExtractedText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(activeExtractedText))
                                android.widget.Toast.makeText(context, "Copied text to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    if (activeExtractedText.isNotEmpty()) {
                        Text(
                            text = activeExtractedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "No extracted text saved for this page scan. Use OCR engines when digitizing papers to extract dynamic searchable texts.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp)) // Padding spacer
        }
    }
}
