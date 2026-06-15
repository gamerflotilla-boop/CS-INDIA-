package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Document
import com.example.data.DocumentRepository
import com.example.data.Folder
import com.example.util.ExportManager
import com.example.util.ImageProcessor
import com.example.util.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = DocumentRepository(database)
    }

    // App Preferences / Theme Mode
    private val _isDarkMode = MutableStateFlow(true) // Professional dark mode by default
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Organizing Scan Folders
    val allFolders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter, Search, and Lists
    val searchQuery = MutableStateFlow("")
    val selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedCategory = MutableStateFlow<String?>(null) // "All" or a specific category
    val selectedPriority = MutableStateFlow<String?>(null) // "All" or high/medium/low

    // Combine flows to dynamically filter our list of scanned documents
    val documents: StateFlow<List<Document>> = combine(
        repository.allDocuments,
        searchQuery,
        selectedFolderId,
        selectedCategory,
        selectedPriority
    ) { docs, query, folderId, cat, priority ->
        docs.filter { doc ->
            val matchesQuery = query.isEmpty() || 
                    doc.title.contains(query, ignoreCase = true) || 
                    doc.extractedText.contains(query, ignoreCase = true)
            
            val matchesFolder = folderId == null || doc.folderId == folderId
            
            val matchesCategory = cat == null || cat == "All" || doc.category.equals(cat, ignoreCase = true)
            
            val matchesPriority = priority == null || priority == "All" || doc.priorityLabel.equals(priority, ignoreCase = true)

            matchesQuery && matchesFolder && matchesCategory && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State For Active Document Scanner Flow ---
    val capturedImageFile = MutableStateFlow<File?>(null)
    val processedBitmap = MutableStateFlow<Bitmap?>(null)
    
    // Default 4 drag handles for document perspective cropping
    val cropTopLeft = MutableStateFlow(PointF(0.05f, 0.05f))
    val cropTopRight = MutableStateFlow(PointF(0.95f, 0.05f))
    val cropBottomRight = MutableStateFlow(PointF(0.95f, 0.95f))
    val cropBottomLeft = MutableStateFlow(PointF(0.05f, 0.95f))

    // Active Processing filters
    val activeFilter = MutableStateFlow("Original") // "Original", "Magic Color", "B&W / Grayscale"
    val extractedOcrText = MutableStateFlow("")
    val isProcessingOcr = MutableStateFlow(false)

    // Setup active editing parameters
    fun startScanFlow(imageFile: File) {
        capturedImageFile.value = imageFile
        processedBitmap.value = BitmapFactory.decodeFile(imageFile.absolutePath)
        
        // Reset crop corners to comfortable defaults
        cropTopLeft.value = PointF(0.05f, 0.05f)
        cropTopRight.value = PointF(0.95f, 0.05f)
        cropBottomRight.value = PointF(0.95f, 0.95f)
        cropBottomLeft.value = PointF(0.05f, 0.95f)
        
        activeFilter.value = "Original"
        extractedOcrText.value = ""
        isProcessingOcr.value = false
    }

    fun applyPerspectiveFilterAndSave(context: Context, filterName: String, onDone: (Bitmap) -> Unit) {
        viewModelScope.launch {
            val srcFile = capturedImageFile.value ?: return@launch
            val srcBitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(srcFile.absolutePath)
            } ?: return@launch

            // 1. Perspective Crop & Deskew
            val deskewed = withContext(Dispatchers.Default) {
                ImageProcessor.cropAndDeskew(
                    srcBitmap,
                    cropTopLeft.value,
                    cropTopRight.value,
                    cropBottomRight.value,
                    cropBottomLeft.value
                )
            }

            // 2. Apply Custom Image Filter
            val filtered = withContext(Dispatchers.Default) {
                ImageProcessor.applyFilter(deskewed, filterName)
            }

            activeFilter.value = filterName
            processedBitmap.value = filtered
            onDone(filtered)
        }
    }

    fun extractTextFromActiveImage(context: Context, useAiOcr: Boolean) {
        viewModelScope.launch {
            val bitmap = processedBitmap.value ?: return@launch
            isProcessingOcr.value = true
            
            val ocrText = if (useAiOcr) {
                OcrEngine.performGeminiAnalysis(bitmap)
            } else {
                OcrEngine.extractTextFromBitmap(context, bitmap)
            }
            
            extractedOcrText.value = ocrText
            isProcessingOcr.value = false
        }
    }

    fun saveCompletedDocument(
        context: Context,
        title: String,
        category: String,
        priority: String,
        folderId: Int?,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val origFile = capturedImageFile.value ?: return@launch
            val procBitmap = processedBitmap.value ?: return@launch

            // Save processed bitmap to file system
            val processedPath = withContext(Dispatchers.IO) {
                ImageProcessor.saveBitmapToFile(context, procBitmap, "proc")
            }

            val document = Document(
                title = title,
                originalImagePath = origFile.absolutePath,
                processedImagePath = processedPath,
                extractedText = extractedOcrText.value,
                folderId = folderId,
                priorityLabel = priority,
                category = category,
                timestamp = System.currentTimeMillis()
            )

            repository.insertDocument(document)
            onSaved()
        }
    }

    // --- Folder / Organization management ---
    fun createFolder(name: String) {
        viewModelScope.launch {
            if (name.trim().isNotEmpty()) {
                repository.insertFolder(Folder(name = name.trim()))
            }
        }
    }

    fun deleteFolder(id: Int) {
        viewModelScope.launch {
            repository.deleteFolder(id)
            if (selectedFolderId.value == id) {
                selectedFolderId.value = null
            }
        }
    }

    fun deleteDocument(id: Int) {
        viewModelScope.launch {
            repository.deleteDocument(id)
        }
    }

    // --- Exporters ---
    fun shareDocumentAsPdf(context: Context, document: Document) {
        viewModelScope.launch {
            val file = File(document.processedImagePath)
            if (file.exists()) {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } ?: return@launch
                
                val pdfFile = withContext(Dispatchers.IO) {
                    ExportManager.generatePdf(context, document, bitmap)
                }
                
                ExportManager.shareFile(context, pdfFile, "application/pdf")
            }
        }
    }

    fun shareDocumentAsTxt(context: Context, document: Document) {
        viewModelScope.launch {
            val txtFile = withContext(Dispatchers.IO) {
                ExportManager.generateTxt(context, document)
            }
            ExportManager.shareFile(context, txtFile, "text/plain")
        }
    }
}

class DocScannerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocScannerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
