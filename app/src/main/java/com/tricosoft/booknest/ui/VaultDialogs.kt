package com.tricosoft.booknest.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.state.VaultMode
import com.tricosoft.booknest.ui.components.BnButton
import com.tricosoft.booknest.ui.components.BnInput
import com.tricosoft.booknest.ui.components.BtnVariant
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.bnCard
import kotlinx.coroutines.launch

/** Web VaultDialog er port: setup / recovery code dekhano / unlock / recovery diye reset. */
@Composable
fun VaultDialogHost() {
    val g = LocalGraph.current
    val v = g.vault
    val mode = v.dialog ?: return
    val c = bn()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var pass by remember(mode) { mutableStateOf("") }
    var pass2 by remember(mode) { mutableStateOf("") }
    var code by remember(mode) { mutableStateOf("") }
    var saved by remember(mode) { mutableStateOf(false) }
    var copied by remember(mode) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (mode != VaultMode.ShowCode) v.cancelDialog() },
        properties = DialogProperties(dismissOnBackPress = mode != VaultMode.ShowCode, dismissOnClickOutside = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bnCard(16.dp, 6.dp)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (mode) {
                VaultMode.Setup -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = c.foreground, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Txt("Encrypt your drafts", size = 18, weight = FontWeight.SemiBold, display = true)
                    }
                    Txt(
                        "Choose a vault passphrase. Your unpublished chapters are encrypted on this device before upload, so nobody \u2014 not even the server \u2014 can read them. This is separate from your login password.",
                        size = 14, color = c.mutedForeground,
                    )
                    BnInput(pass, { pass = it }, placeholder = "Vault passphrase (min 10 chars)", password = true, modifier = Modifier.fillMaxWidth())
                    BnInput(pass2, { pass2 = it }, placeholder = "Repeat passphrase", password = true, modifier = Modifier.fillMaxWidth())
                    BnButton(
                        if (v.busy) "Setting up..." else "Enable encryption", enabled = !v.busy, modifier = Modifier.fillMaxWidth(),
                        onClick = { scope.launch { v.setup(pass, pass2) } },
                    )
                }

                VaultMode.ShowCode -> {
                    Txt("Save your recovery code", size = 18, weight = FontWeight.SemiBold, display = true)
                    Txt(
                        "If you forget your passphrase, this code is the ONLY way to recover your drafts. It is shown once and is never stored on the server. Keep it somewhere safe (password manager or paper).",
                        size = 14, color = c.mutedForeground,
                    )
                    SelectionContainer {
                        Txt(
                            v.recoveryCode, size = 14, align = TextAlign.Center, weight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, c.border, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                        )
                    }
                    BnButton(
                        if (copied) "Copied" else "Copy code", variant = BtnVariant.Outline, modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("BookNest recovery code", v.recoveryCode))
                            copied = true
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = saved, onCheckedChange = { saved = it },
                            colors = CheckboxDefaults.colors(checkedColor = c.primary, checkmarkColor = c.primaryForeground, uncheckedColor = c.mutedForeground),
                        )
                        Txt("I have saved my recovery code", size = 14)
                    }
                    BnButton("Continue", enabled = saved, modifier = Modifier.fillMaxWidth(), onClick = { v.finishDialog() })
                }

                VaultMode.Unlock -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.foreground, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Txt("Unlock your drafts", size = 18, weight = FontWeight.SemiBold, display = true)
                    }
                    Txt("Enter your vault passphrase to read and save encrypted drafts.", size = 14, color = c.mutedForeground)
                    BnInput(pass, { pass = it }, placeholder = "Vault passphrase", password = true, modifier = Modifier.fillMaxWidth())
                    BnButton(
                        if (v.busy) "Unlocking..." else "Unlock", enabled = !v.busy && pass.isNotEmpty(), modifier = Modifier.fillMaxWidth(),
                        onClick = { scope.launch { v.unlock(pass) } },
                    )
                    Txt(
                        "Forgot passphrase? Use recovery code", size = 12, color = c.mutedForeground, align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .androidClick { v.openRecovery() },
                    )
                }

                VaultMode.Recovery -> {
                    Txt("Reset with recovery code", size = 18, weight = FontWeight.SemiBold, display = true)
                    Txt("Enter your recovery code and choose a new passphrase.", size = 14, color = c.mutedForeground)
                    BnInput(code, { code = it }, placeholder = "XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX", modifier = Modifier.fillMaxWidth())
                    BnInput(pass, { pass = it }, placeholder = "New passphrase (min 10 chars)", password = true, modifier = Modifier.fillMaxWidth())
                    BnInput(pass2, { pass2 = it }, placeholder = "Repeat new passphrase", password = true, modifier = Modifier.fillMaxWidth())
                    BnButton(
                        if (v.busy) "Resetting..." else "Reset passphrase", enabled = !v.busy && code.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                        onClick = { scope.launch { v.recover(code, pass, pass2) } },
                    )
                    Txt(
                        "Back", size = 12, color = c.mutedForeground, align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .androidClick { v.backToUnlock() },
                    )
                }
            }
        }
    }
}

private fun Modifier.androidClick(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
