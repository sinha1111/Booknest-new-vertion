package com.tricosoft.booknest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tricosoft.booknest.BuildConfig
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.state.Strings
import com.tricosoft.booknest.state.VaultStatus
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.BnButton
import com.tricosoft.booknest.ui.components.BnInput
import com.tricosoft.booknest.ui.components.BtnVariant
import com.tricosoft.booknest.ui.components.ConfirmDialog
import com.tricosoft.booknest.ui.components.IconBtn
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.bnCard
import com.tricosoft.booknest.ui.components.topSafe
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavController) {
    val g = LocalGraph.current
    val c = bn()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val session by g.api.session.collectAsState()
    val uid = session?.userId
    val t = Strings.labels(g.prefs.language)
    val vault = g.vault

    var editingProfile by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var showLang by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                val p = g.repo.myProfile(uid)
                name = p.name
                bio = p.bio ?: ""
            } catch (e: Exception) { /* ignore */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(top = topSafe(), bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, onClick = { nav.popBackStack() }, contentDescription = t.back)
            Txt(t.settings, size = 20, weight = FontWeight.Bold, display = true)
        }

        // Appearance
        Section(t.appearance) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        if (g.prefs.dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(20.dp),
                    )
                    Txt(t.darkMode, size = 14, weight = FontWeight.Medium)
                }
                Switch(
                    checked = g.prefs.dark, onCheckedChange = { g.prefs.toggleDark() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = c.primary,
                        uncheckedThumbColor = Color.White, uncheckedTrackColor = c.input,
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }

        // Language
        Section(t.language) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLang = !showLang }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Language, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(20.dp))
                        Txt(Strings.languageNames[g.prefs.language] ?: "English", size = 14, weight = FontWeight.Medium)
                    }
                    Icon(
                        Icons.Outlined.ChevronRight, contentDescription = null, tint = c.mutedForeground,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showLang) 90f else 0f),
                    )
                }
                if (showLang) {
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    Strings.languageNames.forEach { (code, label) ->
                        val selected = g.prefs.language == code
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (selected) Modifier.androidBg(c.primary.copy(alpha = 0.10f)) else Modifier)
                                .clickable { g.prefs.changeLanguage(code); showLang = false }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Txt(label, size = 14, weight = if (selected) FontWeight.Medium else FontWeight.Normal, color = if (selected) c.primary else c.foreground)
                        }
                    }
                }
            }
        }

        // Account
        Section(t.account) {
            if (!editingProfile) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingProfile = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(20.dp))
                        Txt(t.editProfile, size = 14, weight = FontWeight.Medium)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(16.dp))
                }
            } else {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Txt(t.name, size = 12, weight = FontWeight.Medium, color = c.mutedForeground, modifier = Modifier.padding(bottom = 4.dp))
                        BnInput(name, { name = it }, modifier = Modifier.fillMaxWidth(), maxLength = 80)
                    }
                    Column {
                        Txt(t.bio, size = 12, weight = FontWeight.Medium, color = c.mutedForeground, modifier = Modifier.padding(bottom = 4.dp))
                        BnInput(bio, { bio = it }, modifier = Modifier.fillMaxWidth(), maxLength = 500)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BnButton(
                            if (saving) "..." else t.save, small = true, enabled = !saving, modifier = Modifier.weight(1f),
                            onClick = {
                                val u = uid ?: return@BnButton
                                scope.launch {
                                    saving = true
                                    try {
                                        g.repo.updateProfile(u, name, bio, null, null)
                                        g.toaster.show("Profile updated!")
                                        editingProfile = false
                                    } catch (e: Exception) {
                                        g.toaster.error("Failed to save")
                                    }
                                    saving = false
                                }
                            },
                        )
                        BnButton(t.cancel, onClick = { editingProfile = false }, variant = BtnVariant.Outline, small = true, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Draft encryption
        Section("Draft encryption") {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        if (vault.status == VaultStatus.Unlocked) Icons.Outlined.VerifiedUser else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (vault.status == VaultStatus.Unlocked) c.primary else c.mutedForeground,
                        modifier = Modifier.size(20.dp),
                    )
                    Txt(
                        when (vault.status) {
                            VaultStatus.Unlocked -> "Unlocked on this device"
                            VaultStatus.Locked -> "Locked"
                            VaultStatus.None -> "Not set up"
                            VaultStatus.Loading -> "Checking..."
                        },
                        size = 14, weight = FontWeight.Medium,
                    )
                }
                Txt(
                    "Unpublished chapters are encrypted on your device before upload. Published books are public by design and are not encrypted.",
                    size = 12, color = c.mutedForeground,
                )
                when (vault.status) {
                    VaultStatus.Unlocked -> BnButton("Lock now", onClick = { vault.lock() }, variant = BtnVariant.Outline, modifier = Modifier.fillMaxWidth())
                    VaultStatus.Locked, VaultStatus.None -> BnButton(
                        if (vault.status == VaultStatus.None) "Set up encryption" else "Unlock",
                        onClick = { scope.launch { vault.ensureUnlocked() } },
                        variant = BtnVariant.Outline, modifier = Modifier.fillMaxWidth(),
                    )
                    VaultStatus.Loading -> {}
                }
            }
        }

        // Legal
        Section("Legal") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(BuildConfig.PRIVACY_URL) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(20.dp))
                    Txt("Privacy Policy", size = 14, weight = FontWeight.Medium)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(16.dp))
            }
        }

        // Sign out
        BnButton(
            t.signOut, variant = BtnVariant.Ghost, contentColor = c.destructive, icon = Icons.AutoMirrored.Outlined.Logout,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    g.api.signOut()
                    nav.navigate("auth") { popUpTo(0) }
                }
            },
        )

        // Danger zone
        Section("Danger Zone", labelColor = c.destructive) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Txt(
                    "Delete all your BookNest data (profile, books, comments, drafts) permanently. This cannot be undone.",
                    size = 14, color = c.mutedForeground,
                )
                BnButton(
                    "Delete Account", variant = BtnVariant.Destructive, enabled = uid != null, icon = Icons.Outlined.Delete,
                    modifier = Modifier.fillMaxWidth(), onClick = { confirmDelete = true },
                )
            }
        }

        Txt(
            "Privacy Policy", size = 12, color = c.mutedForeground, align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { uriHandler.openUri(BuildConfig.PRIVACY_URL) },
        )
        Txt(
            "BookNest v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) \u00B7 native", size = 10,
            color = c.mutedForeground.copy(alpha = 0.6f), align = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete your account?",
            text = "This permanently deletes your BookNest profile, books, comments and encryption keys. This action cannot be undone.",
            confirmLabel = if (deleting) "Deleting..." else "Delete Forever",
            cancelLabel = t.cancel, busy = deleting,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                scope.launch {
                    deleting = true
                    try {
                        g.repo.deleteAccountData()
                        g.toaster.show("Your account has been deleted")
                        confirmDelete = false
                        g.api.signOut()
                        nav.navigate("auth") { popUpTo(0) }
                    } catch (e: Exception) {
                        g.toaster.error(e.message ?: "Failed to delete account")
                    }
                    deleting = false
                }
            },
        )
    }
}

private fun Modifier.androidBg(color: Color): Modifier = this.then(Modifier.background(color))

@Composable
private fun Section(
    label: String,
    labelColor: Color = bn().mutedForeground,
    content: @Composable () -> Unit,
) {
    Column {
        Txt(
            label.uppercase(), size = 12, weight = FontWeight.SemiBold, color = labelColor,
            letterSpacing = 0.6.sp, modifier = Modifier.padding(bottom = 12.dp),
        )
        Box(modifier = Modifier
            .fillMaxWidth()
            .bnCard(12.dp, 1.dp)) {
            content()
        }
    }
}
