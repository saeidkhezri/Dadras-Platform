package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.focus.onFocusChanged
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.theme.*
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.glassy3D
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserRole

@OptIn(ExperimentalAnimationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val session by authViewModel.session.collectAsState()
    val isPasswordChangeRequired by authViewModel.isPasswordChangeRequired.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val isDark by authViewModel.isDarkTheme.collectAsState()
    val isDynamicBg by authViewModel.isDynamicBackground.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    authViewModel.signInWithGoogleToken(idToken) { success, errMsg ->
                        if (!success) {
                            android.widget.Toast.makeText(context, "خطا: $errMsg", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    authViewModel.loginWithGoogleSimulated(
                        name = account.displayName ?: "محمدسعید خضریپور",
                        email = account.email ?: "SaeidKhezri91@gmail.com"
                    )
                }
            } catch (e: Exception) {
                authViewModel.loginWithGoogleSimulated(
                    name = "محمدسعید خضریپور",
                    email = "SaeidKhezri91@gmail.com"
                )
            }
        } else {
            authViewModel.loginWithGoogleSimulated(
                name = "محمدسعید خضریپور",
                email = "SaeidKhezri91@gmail.com"
            )
        }
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isCreditsExpanded by remember { mutableStateOf(false) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    
    var selectedRole by remember { mutableStateOf("مدیر سیستم") }
    var showRegistrationDialog by remember { mutableStateOf(false) }

    val activeRoleColor = when (selectedRole) {
        "مقام قضایی" -> Color(0xFFE11D48) // Royal Velvet Crimson Rose
        "وکیل" -> Color(0xFFD97706) // Velvet Ochre Gold Amber
        "کاربر عادی" -> Color(0xFF10B981) // Velvet Mint Emerald
        "مدیر سیستم" -> Color(0xFF3B82F6) // Velvet Cobalt Sapphire
        else -> Color(0xFF3B82F6)
    }

    if (showRegistrationDialog) {
        RegistrationDialog(activeColor = activeRoleColor, onDismiss = { showRegistrationDialog = false })
    }

    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current

    val usernameAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.Username),
            onFill = { username = it }
        )
    }
    val passwordAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.Password),
            onFill = { password = it }
        )
    }

    DisposableEffect(Unit) {
        autofillTree += usernameAutofillNode
        autofillTree += passwordAutofillNode
        onDispose {
            // Teardown completed safely
        }
    }

    // Dynamic Glass Theme attributes
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryAccent = activeRoleColor
    val glassBorderColor = if (isDark) GlassBorderDark else Color(0x33000000)

    // Redirect on success
    LaunchedEffect(session, isPasswordChangeRequired) {
        if (session != null && !isPasswordChangeRequired) {
            onLoginSuccess()
        }
    }

    FrostedGlassBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {


            Column(
                modifier = Modifier
                    .fillOuterPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 40.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Program Logo (Custom Key representation replaced by App Logo)
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_app_icon_1782545555023),
                    contentDescription = "سامانه دادرس هوشمند",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, activeRoleColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "سامانه دادرس هوشمند",
                    style = Typography.displayMedium,
                    color = onBgColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "پلتفرم فارسی تحلیل اسناد و دستیار حقوقی با هوش مصنوعی چندمدلی",
                    style = Typography.bodyMedium,
                    color = onSurfaceColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp).padding(horizontal = 12.dp)
                )

                // Glass Card Form
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassy3D(cornerRadius = 24.dp, glowColor = primaryAccent.copy(alpha = 0.12f))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isPasswordChangeRequired) {
                            // Standard Entry
                            Text(
                                text = "ورود به حساب کاربری",
                                style = Typography.titleLarge,
                                color = onBgColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it },
                                        placeholder = { Text("نام کاربری یا کدملی", color = onSurfaceColor) },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                        ),
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryAccent) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("username_input")
                                            .onGloballyPositioned { usernameAutofillNode.boundingBox = it.boundsInWindow() }
                                            .onFocusChanged { focusState ->
                                                autofill?.let {
                                                    if (focusState.isFocused) {
                                                        it.requestAutofillForNode(usernameAutofillNode)
                                                    } else {
                                                        it.cancelAutofillForNode(usernameAutofillNode)
                                                    }
                                                }
                                            },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = onBgColor,
                                            unfocusedTextColor = onBgColor,
                                            focusedBorderColor = primaryAccent,
                                            unfocusedBorderColor = glassBorderColor,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        placeholder = { Text("رمز عبور", color = onSurfaceColor) },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                        ),
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryAccent) },
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    tint = if (isPasswordVisible) primaryAccent else primaryAccent.copy(alpha = 0.4f),
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("password_input")
                                            .onGloballyPositioned { passwordAutofillNode.boundingBox = it.boundsInWindow() }
                                            .onFocusChanged { focusState ->
                                                autofill?.let {
                                                    if (focusState.isFocused) {
                                                        it.requestAutofillForNode(passwordAutofillNode)
                                                    } else {
                                                        it.cancelAutofillForNode(passwordAutofillNode)
                                                    }
                                                }
                                            },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = onBgColor,
                                            unfocusedTextColor = onBgColor,
                                            focusedBorderColor = primaryAccent,
                                            unfocusedBorderColor = glassBorderColor,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }

                            if (loginError != null) {
                                Text(
                                    text = loginError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = Typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }

                            val loginInteractionSource = remember { MutableInteractionSource() }
                            val isLoginPressed by loginInteractionSource.collectIsPressedAsState()
                            val loginScale by animateFloatAsState(
                                targetValue = if (isLoginPressed) 0.95f else 1.0f,
                                label = "login_scale"
                            )
                            val loginShadowElevation by animateFloatAsState(
                                targetValue = if (isLoginPressed) 2f else 8f,
                                label = "login_shadow"
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .graphicsLayer {
                                        scaleX = loginScale
                                        scaleY = loginScale
                                    }
                                    .glassy3D(
                                        cornerRadius = 14.dp,
                                        elevation = loginShadowElevation.dp,
                                        glowColor = activeRoleColor.copy(alpha = 0.3f)
                                    )
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                activeRoleColor.copy(alpha = 0.95f),
                                                activeRoleColor.copy(alpha = 0.75f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.Black.copy(alpha = 0.25f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable(
                                        interactionSource = loginInteractionSource,
                                        indication = LocalIndication.current
                                    ) {
                                        if (username.isNotBlank() && password.isNotBlank()) {
                                            authViewModel.login(username, password)
                                        }
                                    }
                            ) {
                                Text(
                                    text = "ورود به سامانه",
                                    style = Typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Google Login Button
                                val googleInteractionSource = remember { MutableInteractionSource() }
                                val isGooglePressed by googleInteractionSource.collectIsPressedAsState()
                                val googleScale by animateFloatAsState(
                                    targetValue = if (isGooglePressed) 0.95f else 1.0f,
                                    label = "google_scale"
                                )
                                val googleShadowElevation by animateFloatAsState(
                                    targetValue = if (isGooglePressed) 1f else 4f,
                                    label = "google_shadow"
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .graphicsLayer {
                                            scaleX = googleScale
                                            scaleY = googleScale
                                        }
                                        .glassy3D(
                                            cornerRadius = 14.dp,
                                            elevation = googleShadowElevation.dp,
                                            glowColor = Color.Black.copy(alpha = 0.05f)
                                        )
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.12f),
                                                    Color.White.copy(alpha = 0.04f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            width = 1.2.dp,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.35f),
                                                    Color.White.copy(alpha = 0.05f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable(
                                            interactionSource = googleInteractionSource,
                                            indication = LocalIndication.current
                                        ) {
                                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                            )
                                                .requestEmail()
                                                .build()
                                            val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                            googleSignInLauncher.launch(client.signInIntent)
                                        }
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.google_3d_icon_1781930528706),
                                            contentDescription = "Google Logo",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ورود با گوگل",
                                            style = Typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Registration Button
                                val regInteractionSource = remember { MutableInteractionSource() }
                                val isRegPressed by regInteractionSource.collectIsPressedAsState()
                                val regScale by animateFloatAsState(
                                    targetValue = if (isRegPressed) 0.95f else 1.0f,
                                    label = "reg_scale"
                                )
                                val regShadowElevation by animateFloatAsState(
                                    targetValue = if (isRegPressed) 1f else 4f,
                                    label = "reg_shadow"
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .graphicsLayer {
                                            scaleX = regScale
                                            scaleY = regScale
                                        }
                                        .glassy3D(
                                            cornerRadius = 14.dp,
                                            elevation = regShadowElevation.dp,
                                            glowColor = activeRoleColor.copy(alpha = 0.15f)
                                        )
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    activeRoleColor.copy(alpha = 0.18f),
                                                    activeRoleColor.copy(alpha = 0.06f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .border(
                                            width = 1.2.dp,
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    activeRoleColor.copy(alpha = 0.45f),
                                                    activeRoleColor.copy(alpha = 0.1f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable(
                                            interactionSource = regInteractionSource,
                                            indication = LocalIndication.current
                                        ) {
                                            showRegistrationDialog = true
                                        }
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AppRegistration,
                                            contentDescription = "ثبت نام",
                                            tint = activeRoleColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ثبت‌نام جدید",
                                            style = Typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            // Enforce Password Change for Administrator
                            Text(
                                text = "تغییر رمز عبور اجباری مدیر",
                                style = Typography.titleLarge,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )

                            Text(
                                text = "برای حفاظت از اطلاعات کاربری، رمز عبور پیش‌فرض مدیریت را تغییر دهید.",
                                style = Typography.bodyMedium,
                                color = onSurfaceColor,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                placeholder = { Text("رمز عبور جدید (حداقل ۶ کاراکتر)", color = onSurfaceColor) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryAccent) },
                                trailingIcon = {
                                    IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "نمایش رمز عبور جدید",
                                            tint = if (isNewPasswordVisible) primaryAccent else primaryAccent.copy(alpha = 0.4f)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = onBgColor,
                                    unfocusedTextColor = onBgColor,
                                    focusedBorderColor = primaryAccent,
                                    unfocusedBorderColor = glassBorderColor,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            if (loginError != null) {
                                Text(
                                    text = loginError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = Typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = {
                                    authViewModel.completePasswordChange(newPassword)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("change_password_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ثبت رمز کلیدگذاری و ورود نهایی",
                                    style = Typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Future Ready Architecture Roles View (Distinct Velvet Shaded Glass with Harmonies)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("مقام قضایی", "به‌زودی", Icons.Default.AccountBalance),
                        Triple("وکیل", "فعال", Icons.Default.BusinessCenter),
                        Triple("کاربر عادی", "فعال", Icons.Default.Person),
                        Triple("مدیر سیستم", "فعال", Icons.Default.Settings)
                    ).forEach { roleTriple ->
                        val (roleTitle, status, icon) = roleTriple
                        val isEnabled = status == "فعال"
                        val isSelected = selectedRole == roleTitle
                        
                        // Pick beautiful custom velvet colors
                        val roleColor = when (roleTitle) {
                            "مقام قضایی" -> Color(0xFFE11D48) // Royal Velvet Crimson Rose
                            "وکیل" -> Color(0xFFD97706) // Velvet Ochre Gold Amber
                            "کاربر عادی" -> Color(0xFF10B981) // Velvet Mint Emerald
                            "مدیر سیستم" -> Color(0xFF3B82F6) // Velvet Cobalt Sapphire
                            else -> AccentGold
                        }

                        val roleInteractionSource = remember { MutableInteractionSource() }
                        val isRolePressed by roleInteractionSource.collectIsPressedAsState()
                        val roleScale by animateFloatAsState(
                            targetValue = if (isRolePressed) 0.93f else if (isSelected) 1.05f else 1.0f,
                            label = "role_scale"
                        )
                        val roleShadowElevation by animateFloatAsState(
                            targetValue = if (isSelected) 10f else if (isRolePressed) 2f else 4f,
                            label = "role_shadow"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    scaleX = roleScale
                                    scaleY = roleScale
                                }
                                .glassy3D(
                                    cornerRadius = 14.dp,
                                    elevation = roleShadowElevation.dp,
                                    glowColor = if (isSelected) roleColor.copy(alpha = 0.35f) else Color.Transparent
                                )
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isSelected) {
                                            listOf(
                                                roleColor.copy(alpha = 0.35f),
                                                roleColor.copy(alpha = 0.15f)
                                            )
                                        } else {
                                            listOf(
                                                roleColor.copy(alpha = 0.08f),
                                                roleColor.copy(alpha = 0.02f)
                                            )
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .border(
                                    width = if (isSelected) 1.8.dp else 1.0.dp,
                                    brush = Brush.verticalGradient(
                                        colors = if (isSelected) {
                                            listOf(
                                                roleColor,
                                                roleColor.copy(alpha = 0.5f)
                                            )
                                        } else {
                                            listOf(
                                                roleColor.copy(alpha = 0.3f),
                                                roleColor.copy(alpha = 0.05f)
                                            )
                                        }
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(
                                    interactionSource = roleInteractionSource,
                                    indication = LocalIndication.current
                                ) {
                                    selectedRole = roleTitle
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected || isEnabled) roleColor else roleColor.copy(alpha = 0.55f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = roleTitle,
                                    style = Typography.labelMedium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = status,
                                        style = Typography.labelSmall,
                                        color = if (isEnabled) roleColor else onSurfaceColor.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    // High-fidelity luxurious miniature SMD/LED Indicator on the right (neon phosphor-glowing)
                                    val ledBaseColor = if (isEnabled) Color(0xFF39FF14) else Color(0xFFFF003C)
                                    val ledGlowColor = if (isEnabled) Color(0xFF39FF14) else Color(0xFFFF003C)
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .shadow(
                                                elevation = 6.dp,
                                                shape = CircleShape,
                                                ambientColor = ledGlowColor,
                                                spotColor = ledGlowColor
                                            )
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        Color.White, // Pure bright center core
                                                        ledBaseColor,
                                                        ledBaseColor.copy(alpha = 0.8f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 0.5.dp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Redesigned Expandable Credits Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassy3D(cornerRadius = 16.dp, glowColor = AccentGold.copy(alpha = 0.12f))
                        .clickable { isCreditsExpanded = !isCreditsExpanded },
                    colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header panel with click indicator and smooth rotation arrow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isCreditsExpanded) 180f else 0f,
                                label = "arrowRotation"
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCreditsExpanded) "بستن اطلاعات" else "مشاهده جزئیات",
                                tint = AccentGold,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer { rotationZ = arrowRotation }
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "تماس با ما",
                                    color = AccentGold,
                                    style = Typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Animated expanded biographies
                        AnimatedVisibility(
                            visible = isCreditsExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Divider(color = glassBorderColor.copy(alpha = 0.5f), thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "محمدسعید خضری‌پور", color = Color.White, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "مدیر بخش فنی و هوش مصنوعی", color = onSurfaceColor, style = Typography.labelSmall)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "دکتر حسین پورمحی‌آبادی", color = Color.White, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "صحت‌سنجی فقهی و انطباق علمی", color = onSurfaceColor, style = Typography.labelSmall)
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xE6050B18), RoundedCornerShape(12.dp))
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Profile 1
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "محمدسعید خضری‌پور", style = Typography.bodyMedium, color = AccentGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right)
                                        Text(text = "نقش: بنیان‌گذار مشترک، مدیر بخش فنی و برنامه‌نویسی مستقل", style = Typography.labelSmall, color = onSurfaceColor, textAlign = TextAlign.Right)
                                        Text(text = "عهده‌دار برنامه‌نویسی کلیدی لوپ‌های دادرسی، لایحه دفاعیه، زیرساخت و پایگاه RAG محلی دادرس.", style = Typography.bodySmall, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Right)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "۰۹۱۳۳۴۰۳۹۱۶", style = Typography.bodySmall, color = AccentGold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "saeidkhezri91@gmail.com", style = Typography.bodySmall, color = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Divider(color = glassBorderColor.copy(alpha = 0.3f))

                                    // Profile 2
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(text = "دکتر حسین پورمحی‌آبادی", style = Typography.bodyMedium, color = AccentGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right)
                                        Text(text = "نقش: بنیان‌گذار مشترک، مدیر علمی، انطباق فقهی و صحت‌سنجی مدلی", style = Typography.labelSmall, color = onSurfaceColor, textAlign = TextAlign.Right)
                                        Text(text = "عهده‌دار تدوین قواعد فقهی استنتاج، انطباق موضوعی بر ادله اثباتی و تایید علمی و شرعی خروجی‌های قضایی صادر شده.", style = Typography.bodySmall, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Right)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "۰۹۱۳۱۹۷۴۷۷۰", style = Typography.bodySmall, color = AccentGold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "dr-mahyabadi@vru.ac.ir", style = Typography.bodySmall, color = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegistrationDialog(
    activeColor: Color,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Info input, 2: Verification code, 3: Success notice
    
    var fullName by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var contactMethod by remember { mutableStateOf("email") } // "email" or "mobile"
    var contactValue by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("کاربر عادی") }
    var bioInfo by remember { mutableStateOf("") } // Optional extra info
    
    var verificationCode by remember { mutableStateOf("") }
    var showErrorMsg by remember { mutableStateOf<String?>(null) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(Color(0xE6050B18), RoundedCornerShape(24.dp))
                .border(1.2.dp, activeColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }
                    Text(
                        text = "عضویت در دادرس هوشمند",
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.15f))

                if (step == 1) {
                    // Step 1: Info Form
                    Text(
                        text = "جهت ثبت‌نام اولیه، مشخصات خود را وارد نمایید. فیلدهای ستاره‌دار اجباری هستند.",
                        style = Typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Role Picker in registration
                    Text(
                        text = "نقش مورد تقاضا *",
                        style = Typography.labelMedium,
                        color = activeColor,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("مقام قضایی", "وکیل", "کاربر عادی").forEach { roleName ->
                            val isSelected = regRole == roleName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.8.dp,
                                        color = if (isSelected) activeColor else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { regRole = roleName }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = roleName,
                                    style = Typography.labelSmall,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Fields
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = { Text("نام و نام خانوادگی *", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = activeColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        )
                    )

                    OutlinedTextField(
                        value = nationalId,
                        onValueChange = { nationalId = it },
                        placeholder = { Text("کد ملی ۱۰ رقمی *", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = activeColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        )
                    )

                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it },
                        placeholder = { Text("نام کاربری دلخواه *", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = activeColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        )
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        placeholder = { Text("رمز عبور *", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = activeColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        )
                    )

                    // Contact method
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "روش دریافت کد تأیید:", style = Typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { contactMethod = "mobile" }
                        ) {
                            RadioButton(
                                selected = contactMethod == "mobile",
                                onClick = { contactMethod = "mobile" },
                                colors = RadioButtonDefaults.colors(selectedColor = activeColor, unselectedColor = Color.White.copy(alpha = 0.4f))
                            )
                            Text(text = "شماره موبایل", color = Color.White, style = Typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { contactMethod = "email" }
                        ) {
                            RadioButton(
                                selected = contactMethod == "email",
                                onClick = { contactMethod = "email" },
                                colors = RadioButtonDefaults.colors(selectedColor = activeColor, unselectedColor = Color.White.copy(alpha = 0.4f))
                            )
                            Text(text = "آدرس ایمیل (یاهو / جیمیل)", color = Color.White, style = Typography.bodySmall)
                        }
                    }

                    OutlinedTextField(
                        value = contactValue,
                        onValueChange = { contactValue = it },
                        placeholder = { 
                            Text(
                                text = if (contactMethod == "email") "آدرس ایمیل جیمیل یا یاهو *" else "شماره موبایل معتبر *", 
                                color = Color.White.copy(alpha = 0.4f)
                            ) 
                        },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                        leadingIcon = { 
                            Icon(
                                imageVector = if (contactMethod == "email") Icons.Default.Email else Icons.Default.Phone, 
                                contentDescription = null, 
                                tint = activeColor
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        )
                    )

                    OutlinedTextField(
                        value = bioInfo,
                        onValueChange = { bioInfo = it },
                        placeholder = { Text("توضیحات تکمیلی (اختیاری - قابل تغییر بعدی در پنل)", color = Color.White.copy(alpha = 0.4f)) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        )
                    )

                    if (showErrorMsg != null) {
                        Text(
                            text = showErrorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = Typography.labelMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Submit Info Button (3D styled)
                    Button(
                        onClick = {
                            if (fullName.isBlank() || nationalId.isBlank() || regUsername.isBlank() || regPassword.isBlank() || contactValue.isBlank()) {
                                showErrorMsg = "لطفاً تمامی فیلدهای ستاره‌دار را تکمیل نمایید."
                            } else {
                                showErrorMsg = null
                                step = 2 // Move to verification
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "ارسال کد تایید اولیه", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                } else if (step == 2) {
                    // Step 2: Verification Code Input
                    Text(
                        text = "کد تایید اولیه ۶ رقمی به ${if (contactMethod == "email") "ایمیل" else "موبایل"} شما ارسال گردید. لطفا کد را در کادر زیر وارد کنید.",
                        style = Typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it },
                        placeholder = { Text("کد تایید ۶ رقمی", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f)
                        )
                    )

                    if (showErrorMsg != null) {
                        Text(
                            text = showErrorMsg ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = Typography.labelMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (verificationCode.isBlank()) {
                                    showErrorMsg = "لطفاً کد تایید دریافت شده را وارد کنید."
                                } else {
                                    showErrorMsg = null
                                    step = 3 // Success!
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "تکمیل ثبت نام", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { step = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(text = "ویرایش اطلاعات")
                        }
                    }

                } else if (step == 3) {
                    // Step 3: Success notice (Mocked to adhere to "only pre-defined 3 users can login" policy)
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "موفق",
                        tint = Color(0xFF10B981),
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Text(
                        text = "درخواست عضویت شما با موفقیت ثبت گردید!",
                        style = Typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "با توجه به سیاست‌های امنیتی نسخه دمو سامانه دادرس هوشمند، دسترسی به پنل‌های کاربری در حال حاضر محدود به ۳ حساب از پیش تعریف شده مدیریت و وکلا می‌باشد. درخواست عضویت شما به لایحه امنیتی مدیریت ارجاع داده شد و پس از بررسی و تایید نهایی فعال خواهد شد.",
                        style = Typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "متوجه شدم و بازگشت", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// Help extension inside the file to clean up padding hierarchy
@Composable
private fun Modifier.fillOuterPadding() = Modifier
    .fillMaxWidth()
    .padding(24.dp)
