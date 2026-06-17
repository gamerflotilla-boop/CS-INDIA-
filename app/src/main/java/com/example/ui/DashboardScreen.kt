package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Document
import com.example.data.Folder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DocScannerViewModel,
    onNavigateToCamera: () -> Unit,
    onViewDocument: (Document) -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedPriority.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }
    
    val categories = listOf("All", "Invoice", "Receipt", "Financial", "Legal", "Academic", "Personal", "Other")
    val priorities = listOf("All", "High", "Medium", "Normal")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner, 
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "CsIndia",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCamera,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("fab_capture_document"),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan Image")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "New Scan", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val userName by viewModel.userName.collectAsStateWithLifecycle()
            val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
            val userPhone by viewModel.userPhone.collectAsStateWithLifecycle()
            val userAvatarIdx by viewModel.userAvatarIndex.collectAsStateWithLifecycle()
            val isPinLockEnabled by viewModel.isPinLockEnabled.collectAsStateWithLifecycle()

            val avatarColors = remember {
                listOf(
                    Color(0xFFE67E22), // Orange
                    Color(0xFF2ECC71), // Green
                    Color(0xFF3498DB), // Blue
                    Color(0xFF9B59B6), // Purple
                    Color(0xFFF1C40F), // Yellow
                    Color(0xFF1ABC9C)  // Turquoise
                )
            }
            val avatarIcons = remember {
                listOf(
                    Icons.Default.Person,
                    Icons.Default.Business,
                    Icons.Default.School,
                    Icons.Default.WorkspacePremium,
                    Icons.Default.Engineering,
                    Icons.Default.AdminPanelSettings
                )
            }

            // User Profile Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("user_profile_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(avatarColors.getOrElse(userAvatarIdx) { Color(0xFF3498DB) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcons.getOrElse(userAvatarIdx) { Icons.Default.Person },
                            contentDescription = "User Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userName.isNotEmpty()) "Namaste, $userName!" else "Namaste, CsIndia Member!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (userEmail.isNotEmpty()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        if (userPhone.isNotEmpty()) {
                            Text(
                                text = "Phone: +91 $userPhone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        if (isPinLockEnabled) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Lock Shield",
                                    tint = Color(0xFF27AE60),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PIN Secured Docs Vault",
                                    fontSize = 10.sp,
                                    color = Color(0xFF27AE60),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { viewModel.logoutUser() },
                        modifier = Modifier.testTag("dashboard_sign_out_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar"),
                placeholder = { Text("Search title, scanned OCR text...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )
            )

            // Cloud Backup and Sync Expandable Control Center
            var showSyncSettings by remember { mutableStateOf(false) }
            val syncProvider by viewModel.backupProvider.collectAsStateWithLifecycle()
            val autoBackup by viewModel.automaticBackupEnabled.collectAsStateWithLifecycle()
            val sStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
            val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
            val lastSyncedTime by viewModel.lastSyncedTime.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSyncSettings = !showSyncSettings },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (sStatus == "Syncing") Icons.Default.Sync 
                                              else if (sStatus == "Synced") Icons.Default.CloudQueue
                                              else Icons.Default.CloudOff,
                                contentDescription = "Cloud Icon",
                                tint = if (sStatus == "Synced") Color(0xFF2ECC71) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "Cloud Backup & Sync Pro",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isLoggedIn) "Linked to $syncProvider • $sStatus" else "Backup scans onto Cloud Storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(onClick = { showSyncSettings = !showSyncSettings }) {
                            Icon(
                                imageVector = if (showSyncSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand Cloud Options"
                            )
                        }
                    }

                    if (showSyncSettings) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        if (isLoggedIn) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Storage Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(userEmail, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { viewModel.logoutUser() }) {
                                    Text("Disconnect Account", color = Color.Red, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Backup Cloud Provider", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Choose active target drive", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilterChip(
                                        selected = syncProvider == "Google Drive",
                                        onClick = { viewModel.setBackupProvider("Google Drive") },
                                        label = { Text("Google Drive", fontSize = 10.sp) }
                                    )
                                    FilterChip(
                                        selected = syncProvider == "Dropbox",
                                        onClick = { viewModel.setBackupProvider("Dropbox") },
                                        label = { Text("Dropbox", fontSize = 10.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Real-time Auto Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Upload copies instantly on capture", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = autoBackup,
                                    onCheckedChange = { viewModel.toggleAutomaticBackup() }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Last Upload Session Status", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(lastSyncedTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.performManualSync() },
                                    enabled = sStatus != "Syncing"
                                ) {
                                    if (sStatus == "Syncing") {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Uploading...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Upload, contentDescription = "Manual sync", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync Now", fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            var inputEmail by remember { mutableStateOf("") }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Log in to sync your document library across all devices securely.", fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = inputEmail,
                                    onValueChange = { inputEmail = it },
                                    placeholder = { Text("scanner.backup@gmail.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        if (inputEmail.trim().isNotEmpty()) {
                                            viewModel.loginUser(inputEmail.trim())
                                        } else {
                                            viewModel.loginUser("scanner.backup@gmail.com")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Link Account & Enable Backups", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Folders Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Folders & Library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        folderNameInput = ""
                        showCreateFolderDialog = true
                    },
                    modifier = Modifier.testTag("create_folder_button")
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Folder")
                }
            }

            // Horizontal Folders List
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Docs" virtual folder
                item {
                    val isSelected = selectedFolderId == null
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                             else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .clickable { viewModel.selectedFolderId.value = null }
                            .testTag("folder_all"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "All Folders")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Scans", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Database Folders
                items(folders) { folder ->
                    val isSelected = selectedFolderId == folder.id
                    var showFolderMenu by remember { mutableStateOf(false) }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                             else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { viewModel.selectedFolderId.value = folder.id },
                                onLongClick = { showFolderMenu = true }
                            )
                            .testTag("folder_item_${folder.id}"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder, 
                                contentDescription = "Folder Icon",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(folder.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            DropdownMenu(
                                expanded = showFolderMenu,
                                onDismissRequest = { showFolderMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete Folder") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                                    onClick = {
                                        viewModel.deleteFolder(folder.id)
                                        showFolderMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Filters Divider Row (Categories & Priorities)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Category Chips
                Text("Category filter:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSel = (cat == "All" && selectedCategory == null) || (selectedCategory == cat)
                        FilterChip(
                            selected = isSel,
                            onClick = { 
                                viewModel.selectedCategory.value = if (cat == "All") null else cat
                            },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }

                // Priority Filter Row
                Text("Priority filter:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(priorities) { pr ->
                        val isSel = (pr == "All" && selectedPriority == null) || (selectedPriority == pr)
                        FilterChip(
                            selected = isSel,
                            onClick = { 
                                viewModel.selectedPriority.value = if (pr == "All") null else pr
                            },
                            label = { Text("$pr Priority", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Document Lists
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Empty docs",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Scanned Documents",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Snap or select a document to start cropping, enhancing, or OCR scanning text extraction right away!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("document_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            onClick = { onViewDocument(doc) },
                            onDelete = { viewModel.deleteDocument(doc.id) },
                            onSharePdf = { viewModel.shareDocumentAsPdf(context, doc) },
                            onShareTxt = { viewModel.shareDocumentAsTxt(context, doc) },
                            onCopyText = {
                                clipboardManager.setText(AnnotatedString(doc.extractedText))
                                val toastMsg = "Text Copied to clipboard!"
                                android.widget.Toast.makeText(context, toastMsg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Add Folder Dialog Box
        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text("Create Folder") },
                text = {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        placeholder = { Text("Enter folder name (e.g. Receipts)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("folder_input_dialog"),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createFolder(folderNameInput)
                            showCreateFolderDialog = false
                        },
                        modifier = Modifier.testTag("folder_confirm_btn")
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSharePdf: () -> Unit,
    onShareTxt: () -> Unit,
    onCopyText: () -> Unit
) {
    val context = LocalContext.current
    var expandedMenu by remember { mutableStateOf(false) }
    val formattedDate = remember(document.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(document.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("doc_card_${document.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Thumbnail with Quick Export direct Share PDF trigger via Long Press
            val pathFile = File(document.processedImagePath)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            android.widget.Toast.makeText(context, "Quick Exporting PDF: ${document.title}", android.widget.Toast.LENGTH_SHORT).show()
                            onSharePdf()
                        }
                    )
                    .testTag("doc_thumbnail_${document.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (pathFile.exists()) {
                    AsyncImage(
                        model = pathFile,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Core Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = document.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // Priority Indicator Pill
                    val badgeColor = when (document.priorityLabel.lowercase()) {
                        "high" -> MaterialTheme.colorScheme.errorContainer
                        "medium" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val badgeTextPaint = when (document.priorityLabel.lowercase()) {
                        "high" -> MaterialTheme.colorScheme.onErrorContainer
                        "medium" -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = badgeColor),
                        shape = RoundedCornerShape(6.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Text(
                            text = document.priorityLabel.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = badgeTextPaint
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Sub-labels (Category & OCR characters status)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(document.category, fontSize = 10.sp) },
                        modifier = Modifier.height(20.dp),
                        enabled = false
                    )

                    if (document.extractedText.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TextFields, contentDescription = "OCR active", modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("OCR Text Done", fontSize = 10.sp)
                                }
                            },
                            modifier = Modifier.height(20.dp),
                            enabled = false
                        )
                    }
                }
            }

            // Quick Operations Options Menu
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share as A4 PDF") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF") },
                        onClick = {
                            onSharePdf()
                            expandedMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share as Text File") },
                        leadingIcon = { Icon(Icons.Default.Article, contentDescription = "Text") },
                        onClick = {
                            onShareTxt()
                            expandedMenu = false
                        }
                    )
                    if (document.extractedText.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Copy OCR Content") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") },
                            onClick = {
                                onCopyText()
                                expandedMenu = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete Document") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            expandedMenu = false
                        }
                    )
                }
            }
        }
    }
}
