package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Simulated google accounts list
data class GoogleMockAccount(
    val email: String,
    val name: String,
    val initial: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSignupScreen(
    viewModel: DocScannerViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Screen-level state: Tab selection "LOGIN" or "SIGNUP"
    var activeTab by remember { mutableStateOf("LOGIN") }
    
    // Sub-login methods: "EMAIL", "PHONE", "GOOGLE"
    var loginMethod by remember { mutableStateOf("EMAIL") }

    // Forms fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Signup form
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupPinCode by remember { mutableStateOf("") }
    var selectedAvatarIdx by remember { mutableStateOf(0) }

    // Validation error states
    var fieldErrorMsg by remember { mutableStateOf<String?>(null) }

    // Simulated Loading States
    var showLoadingSpinner by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Verifying credentials...") }

    // Simulated Phone OTP flow
    var showOtpDialog by remember { mutableStateOf(false) }
    var enteredOtpCode by remember { mutableStateOf("") }
    var otpTimerSeconds by remember { mutableStateOf(30) }
    var phoneInputForOtp by remember { mutableStateOf("") }

    // Simulated Google Accounts Dialog
    var showGoogleChooserDialog by remember { mutableStateOf(false) }

    // Forgot Password Flow Dialog States
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordErrorMsg by remember { mutableStateOf<String?>(null) }

    // Biometric Authentication States
    var showBiometricDialog by remember { mutableStateOf(false) }
    var biometricFeedbackText by remember { mutableStateOf("Place your registered finger on the scanner or align your face.") }
    var isBiometricSuccess by remember { mutableStateOf(false) }

    // Simulated Facebook Account Dialog State
    var showFacebookChooserDialog by remember { mutableStateOf(false) }

    val mockFacebookAccounts = remember {
        listOf(
            GoogleMockAccount("gamerflotilla@facebook.com", "Flotilla Gamer (FB)", "F", Color(0xFF1877F2)),
            GoogleMockAccount("csindia.member@facebook.com", "CS India Facebook Profile", "C", Color(0xFF1877F2)),
            GoogleMockAccount("visitor.social@facebook.com", "Facebook Visitor", "V", Color(0xFF1877F2))
        )
    }

    val mockGoogleAccounts = remember {
        listOf(
            GoogleMockAccount("gamerflotilla@gmail.com", "Flotilla Gamer", "F", Color(0xFFE74C3C)),
            GoogleMockAccount("work.india@csindia.org", "CS India Admin", "C", Color(0xFF2980B9)),
            GoogleMockAccount("guest.user@gmail.com", "Guest Explorer", "G", Color(0xFF27AE60)),
            GoogleMockAccount("new.account@gmail.com", "Register New Google ID", "+", Color(0xFF7F8C8D))
        )
    }

    // List of predefined beautiful colors to act as avatars
    val avatarColors = listOf(
        Color(0xFFE67E22), // Orange
        Color(0xFF2ECC71), // Green
        Color(0xFF3498DB), // Blue
        Color(0xFF9B59B6), // Purple
        Color(0xFFF1C40F), // Yellow
        Color(0xFF1ABC9C)  // Turquoise
    )
    val avatarIcons = listOf(
        Icons.Default.Person,
        Icons.Default.Business,
        Icons.Default.School,
        Icons.Default.WorkspacePremium,
        Icons.Default.Engineering,
        Icons.Default.AdminPanelSettings
    )

    // Layout start
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isSystemInDarkTheme()) {
                        listOf(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF2C3E50))
                    } else {
                        listOf(Color(0xFFFFFAF0), Color(0xFFFFF3CD), Color(0xFFE8F5E9))
                    }
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Brand Logo placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF9933), Color(0xFFFFFFFF), Color(0xFF138808))
                        )
                    )
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "CsIndia Scanner Logo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Typography
            Text(
                text = "CsIndia",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "DIGITAL SCANNER & INDIAN DOCUMENTS PORTAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Dynamic card for login / signup forms
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_form_card"),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom Tab Bar (LOGIN vs SIGNUP)
                    TabRow(
                        selectedTabIndex = if (activeTab == "LOGIN") 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        indicator = { boxSource ->
                            // Custom clean line empty indicator because we shape inside the button tabs
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = activeTab == "LOGIN",
                            onClick = { 
                                activeTab = "LOGIN"
                                fieldErrorMsg = null
                            },
                            modifier = Modifier
                                .testTag("login_tab")
                                .background(if (activeTab == "LOGIN") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .height(44.dp)
                        ) {
                            Text(
                                "LOG IN",
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == "LOGIN") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }

                        Tab(
                            selected = activeTab == "SIGNUP",
                            onClick = { 
                                activeTab = "SIGNUP" 
                                fieldErrorMsg = null
                            },
                            modifier = Modifier
                                .testTag("signup_tab")
                                .background(if (activeTab == "SIGNUP") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .height(44.dp)
                        ) {
                            Text(
                                "SIGN UP",
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == "SIGNUP") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Firebase Secured",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Secured & Federated via Firebase Authentication",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (fieldErrorMsg != null) {
                        Text(
                            text = fieldErrorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("validation_error_message")
                        )
                    }

                    // Render LOGIN Forms vs SIGNUP Forms
                    if (activeTab == "LOGIN") {
                        // Toggle Login Method Toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Login via:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = loginMethod == "EMAIL",
                                    onClick = { loginMethod = "EMAIL" },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = "Email Login",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("method_email_chip")
                                )
                                FilterChip(
                                    selected = loginMethod == "PHONE",
                                    onClick = { loginMethod = "PHONE" },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Mobile Login",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier.testTag("method_phone_chip")
                                )
                                FilterChip(
                                    selected = loginMethod == "GOOGLE",
                                    onClick = { loginMethod = "GOOGLE" },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4285F4)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("G", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("method_google_chip")
                                )
                                FilterChip(
                                    selected = loginMethod == "FACEBOOK",
                                    onClick = { loginMethod = "FACEBOOK" },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1877F2)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("f", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("method_facebook_chip")
                                )
                            }
                        }

                        // Display Form matching loginMethod Selection
                        when (loginMethod) {
                            "EMAIL" -> {
                                OutlinedTextField(
                                    value = loginEmail,
                                    onValueChange = { loginEmail = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .testTag("email_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (isPasswordVisible) "Hide" else "Show"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .testTag("password_input"),
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (loginEmail.trim().isEmpty() || !loginEmail.contains("@")) {
                                            fieldErrorMsg = "Please enter a valid email address."
                                        } else if (loginPassword.length < 4) {
                                            fieldErrorMsg = "Password must be at least 4 characters."
                                        } else {
                                            fieldErrorMsg = null
                                            coroutineScope.launch {
                                                loadingText = "Authenticating with Firebase Auth..."
                                                showLoadingSpinner = true
                                                delay(1200)
                                                showLoadingSpinner = false
                                                viewModel.loginUser(loginEmail.trim())
                                                onAuthSuccess()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("submit_login"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = "Login")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In Securely", fontWeight = FontWeight.Bold)
                                }
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(vertical = 4.dp),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     // Fingerprint option
                                     TextButton(
                                         onClick = { 
                                             biometricFeedbackText = "Place your registered finger on the scanner or align your face."
                                             isBiometricSuccess = false
                                             showBiometricDialog = true 
                                         },
                                         modifier = Modifier.testTag("biometric_login_button")
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.Fingerprint,
                                             contentDescription = "Fingerprint sensor",
                                             tint = MaterialTheme.colorScheme.primary,
                                             modifier = Modifier.size(16.dp)
                                         )
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text("Biometric Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                     }

                                     // Forgot password text link
                                     TextButton(
                                         onClick = { 
                                             forgotPasswordEmail = loginEmail
                                             forgotPasswordErrorMsg = null
                                             showForgotPasswordDialog = true 
                                         },
                                         modifier = Modifier.testTag("forgot_password_button")
                                     ) {
                                         Text("Forgot Password?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                     }
                                 }

                                 Spacer(modifier = Modifier.height(12.dp))

                                 HorizontalDivider(
                                     color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                     thickness = 1.dp,
                                     modifier = Modifier.padding(horizontal = 8.dp)
                                 )

                                 Spacer(modifier = Modifier.height(12.dp))

                                 Text(
                                     text = "Quick Sign-In:",
                                     style = MaterialTheme.typography.labelSmall,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                     modifier = Modifier.align(Alignment.CenterHorizontally)
                                 )

                                 Spacer(modifier = Modifier.height(8.dp))

                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.Center,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     IconButton(
                                         onClick = { showGoogleChooserDialog = true },
                                         modifier = Modifier
                                             .size(42.dp)
                                             .clip(CircleShape)
                                             .background(Color(0xFF4285F4))
                                             .testTag("quick_google_icon")
                                     ) {
                                         Box(
                                              modifier = Modifier
                                                  .size(24.dp)
                                                  .clip(CircleShape)
                                                  .background(Color.White),
                                              contentAlignment = Alignment.Center
                                         ) {
                                             Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                         }
                                     }

                                     Spacer(modifier = Modifier.width(16.dp))

                                     IconButton(
                                         onClick = { showFacebookChooserDialog = true },
                                         modifier = Modifier
                                             .size(42.dp)
                                             .clip(CircleShape)
                                             .background(Color(0xFF1877F2))
                                             .testTag("quick_facebook_icon")
                                     ) {
                                         Box(
                                              modifier = Modifier
                                                  .size(24.dp)
                                                  .clip(CircleShape)
                                                  .background(Color.White),
                                              contentAlignment = Alignment.Center
                                         ) {
                                              Text("f", color = Color(0xFF1877F2), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                         }
                                     }
                                 }

                                 Spacer(modifier = Modifier.height(12.dp))
                             }
                            
                            "PHONE" -> {
                                OutlinedTextField(
                                    value = phoneInputForOtp,
                                    onValueChange = { phoneInputForOtp = it },
                                    label = { Text("Indian Mobile Number") },
                                    prefix = { Text("+91 ") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .testTag("phone_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val digits = phoneInputForOtp.filter { it.isDigit() }
                                        if (digits.length != 10) {
                                            fieldErrorMsg = "Please enter a 10-digit Indian mobile number."
                                        } else {
                                            fieldErrorMsg = null
                                            coroutineScope.launch {
                                                loadingText = "Requesting Firebase Phone OTP SMS..."
                                                showLoadingSpinner = true
                                                delay(1000)
                                                showLoadingSpinner = false
                                                
                                                // Trigger OTP keypad overlay
                                                enteredOtpCode = ""
                                                otpTimerSeconds = 30
                                                showOtpDialog = true
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("request_otp_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = "Request Code")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Request OTP via SMS", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Quick Sign-In:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { showGoogleChooserDialog = true },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4285F4))
                                            .testTag("quick_google_icon_phone")
                                    ) {
                                        Box(
                                             modifier = Modifier
                                                 .size(24.dp)
                                                 .clip(CircleShape)
                                                 .background(Color.White),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    IconButton(
                                        onClick = { showFacebookChooserDialog = true },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1877F2))
                                            .testTag("quick_facebook_icon_phone")
                                    ) {
                                        Box(
                                             modifier = Modifier
                                                 .size(24.dp)
                                                 .clip(CircleShape)
                                                 .background(Color.White),
                                             contentAlignment = Alignment.Center
                                         ) {
                                              Text("f", color = Color(0xFF1877F2), fontWeight = FontWeight.Black, fontSize = 14.sp)
                                         }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            "GOOGLE" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Authenticate utilizing Google One-Tap Sign-In federated securely with Firebase Authentication.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    Button(
                                        onClick = { showGoogleChooserDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("google_identity_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4285F4),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            // Mock Google logo G button
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "G",
                                                    color = Color(0xFF4285F4),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Continue with Google", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            "FACEBOOK" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Authenticate of Facebook Identity Services secured via Firebase OAuth redirection.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    Button(
                                        onClick = { showFacebookChooserDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("facebook_identity_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1877F2),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            // Mock Facebook logo "f" in a circle
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "f",
                                                    color = Color(0xFF1877F2),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Continue with Facebook", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- SIGN UP VIEW ---
                        OutlinedTextField(
                            value = signupName,
                            onValueChange = { signupName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = "Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("name_signup_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = signupEmail,
                            onValueChange = { signupEmail = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("email_signup_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = signupPhone,
                            onValueChange = { signupPhone = it },
                            label = { Text("Mobile Phone Number") },
                            leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = "Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("phone_signup_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = signupPassword,
                            onValueChange = { signupPassword = it },
                            label = { Text("Choose Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("password_signup_input"),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = signupPinCode,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() } && it.length <= 4) {
                                    signupPinCode = it
                                }
                            },
                            label = { Text("Document Security PIN (Optional)") },
                            placeholder = { Text("e.g. 4 Digit numeric PIN") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = "Security PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("pin_signup_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Avatar Selection Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                "Choose Portal Avatar Badge:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                (0..5).forEach { index ->
                                    val isSelected = selectedAvatarIdx == index
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(avatarColors[index])
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedAvatarIdx = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = avatarIcons[index],
                                            contentDescription = "Avatar $index",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (signupName.trim().isEmpty()) {
                                    fieldErrorMsg = "Please supply your name."
                                } else if (!signupEmail.contains("@")) {
                                    fieldErrorMsg = "A valid Email is required."
                                } else if (signupPassword.length < 4) {
                                    fieldErrorMsg = "Password must be at least 4 characters long."
                                } else {
                                    fieldErrorMsg = null
                                    coroutineScope.launch {
                                        loadingText = "Registering user credentials in Firebase..."
                                        showLoadingSpinner = true
                                        delay(1500)
                                        showLoadingSpinner = false
                                        
                                        // Save signup fields
                                        viewModel.signupUser(
                                            name = signupName.trim(),
                                            email = signupEmail.trim(),
                                            phone = signupPhone.trim(),
                                            avatar = selectedAvatarIdx,
                                            pin = signupPinCode
                                        )
                                        onAuthSuccess()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_signup"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AppRegistration, contentDescription = "Sign Up")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create CsIndia Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Guest Bypass
            TextButton(
                onClick = {
                    viewModel.userName.value = "Guest Reader"
                    viewModel.userEmail.value = "visitor.portal@csindia.gov"
                    viewModel.isLoggedIn.value = true
                    onAuthSuccess()
                }
            ) {
                Text(
                    text = "Bypass Login & Enter Portal (Guest Mode)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // --- DIALOGS FOR AUTHENTICATION FLOWS ---

    // 1. Loading blocker overlay
    if (showLoadingSpinner) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            dismissButton = {},
            title = null,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = loadingText, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                }
            }
        )
    }

    // 2. Mock Google Account Identity Chooser Dialog
    if (showGoogleChooserDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleChooserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                        Text(
                            "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your credentials and session will be securely federated and managed via Firebase Authentication to join CsIndia services portal.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    mockGoogleAccounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showGoogleChooserDialog = false
                                    coroutineScope.launch {
                                        loadingText = "Federating Google ID token with Firebase Authentication..."
                                        showLoadingSpinner = true
                                        delay(1300)
                                        showLoadingSpinner = false
                                        
                                        // login VM
                                        viewModel.signupUser(
                                            name = acc.name,
                                            email = acc.email,
                                            phone = "",
                                            avatar = (0..5).random(),
                                            pin = ""
                                        )
                                        onAuthSuccess()
                                    }
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(acc.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    acc.initial,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(acc.email, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGoogleChooserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Simulated SMS Gateway verification panel
    if (showOtpDialog) {
        // Run timer subtraction
        LaunchedEffect(showOtpDialog) {
            while (otpTimerSeconds > 0) {
                delay(1000)
                otpTimerSeconds--
            }
        }

        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = {
                Text("Confirm 4-Digit Verification Code", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "We sent a simulated verification OTP to +91 $phoneInputForOtp. Please type the mock passcode below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Box display with code hint
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "💡 Auto OTP Hint: type 5555 or any 4 digit code to auto verify",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    OutlinedTextField(
                        value = enteredOtpCode,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 4) {
                                enteredOtpCode = it
                            }
                        },
                        label = { Text("4-Digit OTP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 12.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .testTag("otp_input_field"),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    if (otpTimerSeconds > 0) {
                        Text(
                            "Resend available in ${otpTimerSeconds}s",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        TextButton(
                            onClick = {
                                otpTimerSeconds = 30
                            }
                        ) {
                            Text("Resend OTP SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredOtpCode.length != 4) {
                            // Non blocking simulation pop up
                        } else {
                            showOtpDialog = false
                            coroutineScope.launch {
                                loadingText = "Validating OTP Code..."
                                showLoadingSpinner = true
                                delay(1200)
                                showLoadingSpinner = false
                                
                                viewModel.loginUserWithPhone(phoneInputForOtp)
                                onAuthSuccess()
                            }
                        }
                    },
                    enabled = enteredOtpCode.length == 4
                ) {
                    Text("Verify & Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- FORGOT PASSWORD CONTROL ---
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = "Forgot Password",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Forgot Password?")
                }
            },
            text = {
                Column {
                    Text(
                        "Enter your registered email address below, and Firebase Identity Services will initiate a secure password reset link to your inbox.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { 
                            forgotPasswordEmail = it
                            forgotPasswordErrorMsg = null
                        },
                        label = { Text("Registered Email Address") },
                        placeholder = { Text("user@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        modifier = Modifier.fillMaxWidth().testTag("forgot_password_email_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (forgotPasswordErrorMsg != null) {
                        Text(
                            text = forgotPasswordErrorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordEmail.trim().isEmpty() || !forgotPasswordEmail.contains("@")) {
                            forgotPasswordErrorMsg = "Please enter a valid email address."
                        } else {
                            showForgotPasswordDialog = false
                            coroutineScope.launch {
                                loadingText = "Sending reset link to ${forgotPasswordEmail.trim()}..."
                                showLoadingSpinner = true
                                delay(1500)
                                showLoadingSpinner = false
                                // Successfully triggered email reset simulation
                                viewModel.loginUser(forgotPasswordEmail.trim()) // auto logins
                                onAuthSuccess()
                            }
                        }
                    },
                    modifier = Modifier.testTag("submit_reset_password_button")
                ) {
                    Text("Send Recovery Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Maybe later")
                }
            }
        )
    }

    // --- BIOMETRIC KEYSTORE CREDENTIAL UNLOCK ---
    if (showBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Sensor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Biometric Authentication")
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Verify identity using secure hardware backing (Face Unlock / Touch ID Keystore Key).",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Large interactive scanning asset
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBiometricSuccess) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )
                            .clickable {
                                if (!isBiometricSuccess) {
                                    coroutineScope.launch {
                                        biometricFeedbackText = "Reading biometric map..."
                                        delay(800)
                                        biometricFeedbackText = "Validating secure Android Vault key..."
                                        delay(850)
                                        isBiometricSuccess = true
                                        biometricFeedbackText = "Identity verified successfully!"
                                        delay(1000)
                                        showBiometricDialog = false
                                        
                                        // Auto sign in user
                                        loadingText = "Unlocking secure document bank..."
                                        showLoadingSpinner = true
                                        delay(1000)
                                        showLoadingSpinner = false
                                        viewModel.loginUser("gamerflotilla@gmail.com")
                                        onAuthSuccess()
                                    }
                                }
                            }
                            .testTag("biometric_sensor_pad"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBiometricSuccess) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Verified ID",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Scan target",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = biometricFeedbackText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (isBiometricSuccess) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            },
            confirmButton = {
                if (!isBiometricSuccess) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                biometricFeedbackText = "Authenticating..."
                                delay(600)
                                isBiometricSuccess = true
                                biometricFeedbackText = "Success!"
                                delay(600)
                                showBiometricDialog = false
                                viewModel.loginUser("gamerflotilla@gmail.com")
                                onAuthSuccess()
                            }
                        },
                        modifier = Modifier.testTag("biometric_sim_success")
                    ) {
                        Text("Simulate Unlock")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBiometricDialog = false }) {
                    Text("Switch to Password")
                }
            }
        )
    }

    // --- MOCK FACEBOOK ACCOUNTS CHOOSER DIALOG ---
    if (showFacebookChooserDialog) {
        AlertDialog(
            onDismissRequest = { showFacebookChooserDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1877F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "f",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("FB QuickConnect", fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "Configure direct account federation via Facebook OAuth. Your session maps securely through Firebase Authentication token verification.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    mockFacebookAccounts.forEach { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showFacebookChooserDialog = false
                                    coroutineScope.launch {
                                        loadingText = "Exchanging Facebook access token with Firebase Auth..."
                                        showLoadingSpinner = true
                                        delay(1500)
                                        showLoadingSpinner = false
                                        viewModel.loginUser(account.email)
                                        onAuthSuccess()
                                    }
                                }
                                .testTag("facebook_acc_${account.email.replace("@", "_").replace(".", "_")}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(account.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = account.initial,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(account.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(account.email, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFacebookChooserDialog = false }) {
                    Text("Cancel connection")
                }
            }
        )
    }
}
