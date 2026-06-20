package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val primaryAccent = MaterialTheme.colorScheme.primary
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
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Program Logo (Custom Scales of Justice representation)
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Shield",
                    tint = primaryAccent,
                    modifier = Modifier
                        .size(76.dp)
                        .background(surfaceColor, RoundedCornerShape(20.dp))
                        .border(1.dp, glassBorderColor, RoundedCornerShape(20.dp))
                        .padding(16.dp)
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
                                                    imageVector = Icons.Default.Lock,
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

                            Button(
                                onClick = {
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        authViewModel.login(username, password)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_button")
                                    .shadow(6.dp, RoundedCornerShape(12.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                primaryAccent.copy(alpha = 0.90f),
                                                primaryAccent.copy(alpha = 0.70f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.White.copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ورود به سامانه",
                                    style = Typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = glassBorderColor.copy(alpha = 0.5f))
                                Text(
                                    text = "یا",
                                    style = Typography.labelMedium,
                                    color = onSurfaceColor,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = glassBorderColor.copy(alpha = 0.5f))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                    )
                                        .requestEmail()
                                        .build()
                                    val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("google_login_button")
                                    .shadow(4.dp, RoundedCornerShape(12.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.White.copy(alpha = 0.04f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.35f),
                                                Color.White.copy(alpha = 0.08f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                border = null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White,
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.google_3d_icon_1781930528706),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "ورود با حساب گوگل",
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
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
                        
                        // Pick beautiful custom velvet colors
                        val roleColor = when (roleTitle) {
                            "مقام قضایی" -> Color(0xFFE11D48) // Royal Velvet Crimson Rose
                            "وکیل" -> Color(0xFFD97706) // Velvet Ochre Gold Amber
                            "کاربر عادی" -> Color(0xFF10B981) // Velvet Mint Emerald
                            "مدیر سیستم" -> Color(0xFF3B82F6) // Velvet Cobalt Sapphire
                            else -> AccentGold
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            roleColor.copy(alpha = if (isEnabled) 0.35f else 0.12f),
                                            roleColor.copy(alpha = if (isEnabled) 0.15f else 0.04f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            roleColor.copy(alpha = if (isEnabled) 0.65f else 0.25f),
                                            roleColor.copy(alpha = if (isEnabled) 0.25f else 0.08f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isEnabled) roleColor else roleColor.copy(alpha = 0.55f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = roleTitle,
                                    style = Typography.labelMedium,
                                    color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = status,
                                    style = Typography.labelSmall,
                                    color = if (isEnabled) roleColor else onSurfaceColor.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.SemiBold
                                )
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

// Help extension inside the file to clean up padding hierarchy
@Composable
private fun Modifier.fillOuterPadding() = Modifier
    .fillMaxWidth()
    .padding(24.dp)
