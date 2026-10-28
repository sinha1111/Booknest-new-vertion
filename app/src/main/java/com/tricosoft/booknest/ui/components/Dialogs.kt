package com.tricosoft.booknest.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.tricosoft.booknest.ui.bn

/** shadcn AlertDialog er moto confirm dialog. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String = "Cancel",
    destructive: Boolean = true,
    busy: Boolean = false,
) {
    val c = bn()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = c.card,
        title = { Txt(title, size = 18, weight = FontWeight.SemiBold, display = true) },
        text = { Txt(text, size = 14, color = c.mutedForeground) },
        confirmButton = {
            BnButton(
                confirmLabel, onClick = onConfirm, enabled = !busy, small = true,
                variant = if (destructive) BtnVariant.Destructive else BtnVariant.Default,
            )
        },
        dismissButton = {
            BnButton(cancelLabel, onClick = onDismiss, enabled = !busy, small = true, variant = BtnVariant.Outline)
        },
    )
}
