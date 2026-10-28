package com.tricosoft.booknest.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tricosoft.booknest.BuildConfig
import com.tricosoft.booknest.R
import com.tricosoft.booknest.data.GENRES
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.ImageUtils
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.BnButton
import com.tricosoft.booknest.ui.components.BnInput
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.topSafe
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun AuthScreen(nav: NavController) {
    val g = LocalGraph.current
    val c = bn()
    val ctx = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val session by g.api.session.collectAsState()

    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var avatarBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }

    // Login hoye gele home e
    LaunchedEffect(session) {
        if (session != null) {
            nav.navigate("home") {
                popUpTo("auth") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = ImageUtils.prepareAvatar(ctx, uri)
            if (bytes == null) {
                g.toaster.error("Could not read this image")
            } else {
                avatarBytes = bytes
                avatarBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
    }

    fun submit() {
        if (loading) return
        if (email.isBlank() || password.isBlank()) { g.toaster.error("Please enter email and password"); return }
        if (!isLogin && name.isBlank()) { g.toaster.error("Please enter your name"); return }
        scope.launch {
            loading = true
            try {
                if (isLogin) {
                    g.api.signIn(email.trim(), password)
                    g.toaster.show("Welcome back!")
                } else {
                    val meta = buildJsonObject {
                        put("full_name", name.trim())
                        put("booknest_bio", bio.trim())
                        put("booknest_category", category)
                    }
                    val s = g.api.signUp(email.trim(), password, meta)
                    if (s != null) {
                        g.repo.ensureProfile()
                        val bytes = avatarBytes
                        if (bytes != null) {
                            try {
                                val url = g.repo.uploadAvatar(s.userId, bytes)
                                g.repo.updateProfile(s.userId, name.trim(), bio.trim(), category.ifBlank { null }, url)
                            } catch (e: Exception) { /* avatar pore Edit Profile theke dewa jabe */ }
                        }
                        g.toaster.show("Account created successfully!")
                    } else {
                        g.toaster.show("Account created! Check your email to confirm, then sign in.")
                        isLogin = true
                    }
                }
            } catch (e: Exception) {
                g.toaster.error(e.message ?: "Something went wrong")
            }
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(topSafe()))
        Spacer(Modifier.height(24.dp))
        // logo
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "BookNest",
            modifier = Modifier
                .size(80.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
        )
        Txt("BookNest", size = 24, weight = FontWeight.Bold, display = true, modifier = Modifier.padding(top = 16.dp))
        Txt(
            if (isLogin) "Welcome back, storyteller" else "Join the story community",
            size = 14, color = c.mutedForeground, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!isLogin) {
                // avatar picker
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(c.secondary)
                            .border(2.dp, c.border, CircleShape)
                            .clickable {
                                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val bmp = avatarBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp, contentDescription = "Avatar", contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = "Add photo", tint = c.mutedForeground, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                BnInput(name, { name = it }, placeholder = "Your name", leading = Icons.Outlined.Person, modifier = Modifier.fillMaxWidth(), maxLength = 80)
                BnInput(
                    bio, { bio = it }, placeholder = "Short bio (optional)", singleLine = false, minLines = 2,
                    maxLength = 500, modifier = Modifier.fillMaxWidth(),
                )
                Box {
                    val shape = RoundedCornerShape(10.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(shape)
                            .background(c.background)
                            .border(1.dp, c.input, shape)
                            .clickable { categoryMenu = true }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Category, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Txt(
                            category.ifBlank { "Writing category" }, size = 14,
                            color = if (category.isBlank()) c.mutedForeground else c.foreground,
                        )
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }, containerColor = c.card) {
                        GENRES.forEach { gname ->
                            DropdownMenuItem(text = { Txt(gname) }, onClick = { category = gname; categoryMenu = false })
                        }
                    }
                }
            }
            BnInput(
                email, { email = it }, placeholder = "Email", leading = Icons.Outlined.Mail,
                keyboardType = KeyboardType.Email, modifier = Modifier.fillMaxWidth(),
            )
            BnInput(
                password, { password = it }, placeholder = "Password", leading = Icons.Outlined.Lock,
                password = true, modifier = Modifier.fillMaxWidth(),
            )
            BnButton(
                if (loading) "Please wait..." else if (isLogin) "Sign In" else "Create Account",
                onClick = { submit() }, enabled = !loading, modifier = Modifier.fillMaxWidth(),
            )
            // Privacy policy: login ar sign up duto page-i
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Txt(
                    if (isLogin) "By signing in, you agree to our" else "By creating an account, you agree to our",
                    size = 12, color = c.mutedForeground, align = TextAlign.Center,
                )
                Txt(
                    "Privacy Policy", size = 12, weight = FontWeight.Medium, color = c.primary, underline = true,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { uriHandler.openUri(BuildConfig.PRIVACY_URL) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Row(modifier = Modifier.padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt(if (isLogin) "Don't have an account? " else "Already have an account? ", size = 14, color = c.mutedForeground)
            Txt(
                if (isLogin) "Sign Up" else "Sign In", size = 14, weight = FontWeight.Medium, color = c.primary,
                modifier = Modifier.clickable { isLogin = !isLogin },
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
