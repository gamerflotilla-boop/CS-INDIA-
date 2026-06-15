package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import coil.compose.AsyncImage
import com.example.data.Document
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailedViewScreen(
    document: Document,
    viewModel: DocScannerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val formattedDate = remember(document.timestamp) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(document.timestamp))
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
            val pathFile = File(document.processedImagePath)
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

            Spacer(modifier = Modifier.height(16.dp))

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

                    if (document.extractedText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(document.extractedText))
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
                    if (document.extractedText.isNotEmpty()) {
                        Text(
                            text = document.extractedText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "No extracted text saved for this study/document. You can re-run scanning processes when creating new documents.",
                            fontSize = 12.sp,
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
