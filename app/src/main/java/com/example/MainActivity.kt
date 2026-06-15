package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Document
import com.example.ui.CameraViewScreen
import com.example.ui.CropOverlayScreen
import com.example.ui.DashboardScreen
import com.example.ui.DocScannerViewModel
import com.example.ui.DocScannerViewModelFactory
import com.example.ui.DocumentDetailedViewScreen
import com.example.ui.FilterOcrScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    DASHBOARD,
    CAMERA,
    CROP,
    FILTER_OCR,
    VIEW_DETAILS
}

class MainActivity : ComponentActivity() {

    private val viewModel: DocScannerViewModel by viewModels {
        DocScannerViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
                var activeDetailedDoc by remember { mutableStateOf<Document?>(null) }

                // System Back button interceptors for seamless UX
                BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
                    currentScreen = when (currentScreen) {
                        Screen.CAMERA -> Screen.DASHBOARD
                        Screen.CROP -> Screen.CAMERA
                        Screen.FILTER_OCR -> Screen.CROP
                        Screen.VIEW_DETAILS -> Screen.DASHBOARD
                        else -> Screen.DASHBOARD
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    when (currentScreen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToCamera = {
                                    currentScreen = Screen.CAMERA
                                },
                                onViewDocument = { doc ->
                                    activeDetailedDoc = doc
                                    currentScreen = Screen.VIEW_DETAILS
                                }
                            )
                        }
                        
                        Screen.CAMERA -> {
                            CameraViewScreen(
                                onImageCaptured = { file ->
                                    viewModel.startScanFlow(file)
                                    currentScreen = Screen.CROP
                                },
                                onNavigateBack = {
                                    currentScreen = Screen.DASHBOARD
                                }
                            )
                        }

                        Screen.CROP -> {
                            CropOverlayScreen(
                                viewModel = viewModel,
                                onConfirmed = {
                                    currentScreen = Screen.FILTER_OCR
                                },
                                onCancel = {
                                    currentScreen = Screen.CAMERA
                                }
                            )
                        }

                        Screen.FILTER_OCR -> {
                            FilterOcrScreen(
                                viewModel = viewModel,
                                onSaved = {
                                    currentScreen = Screen.DASHBOARD
                                },
                                onCancel = {
                                    currentScreen = Screen.CROP
                                }
                            )
                        }

                        Screen.VIEW_DETAILS -> {
                            val doc = activeDetailedDoc
                            if (doc != null) {
                                DocumentDetailedViewScreen(
                                    document = doc,
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = Screen.DASHBOARD
                                        activeDetailedDoc = null
                                    }
                                )
                            } else {
                                currentScreen = Screen.DASHBOARD
                            }
                        }
                    }
                }
            }
        }
    }
}
