package com.tricosoft.booknest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tricosoft.booknest.state.Toaster
import com.tricosoft.booknest.ui.BnFonts
import com.tricosoft.booknest.ui.bn
import kotlinx.coroutines.delay

// ---------------------------------------------------------------- text
private fun defaultLineHeight(size: Int): TextUnit = when (size) {
    10 -> 15.sp
    11 -> 16.sp
    12 -> 16.sp
    13 -> 18.sp
    14 -> 20.sp
    16 -> 24.sp
    18 -> 28.sp
    20 -> 28.sp
    24 -> 32.sp
    else -> (size * 1.5f).sp
}

/** Tailwind text style: size = px, display = Playfair (font-display). */
@Composable
fun Txt(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 14,
    weight: FontWeight = FontWeight.Normal,
    color: Color = bn().foreground,
    display: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    underline: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        fontFamily = if (display) BnFonts.display else BnFonts.body,
        lineHeight = if (lineHeight == TextUnit.Unspecified) defaultLineHeight(size) else lineHeight,
        letterSpacing = letterSpacing,
        textDecoration = if (underline) TextDecoration.Underline else null,
        textAlign = align,
        maxLines = maxLines,
        overflow = overflow,
    )
}

// ---------------------------------------------------------------- safe area
/** Web er `pt-safe`: status bar er niche theke header shuru. */
@Composable
fun topSafe(): Dp {
    val top = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    return top + 40.dp
}

// ---------------------------------------------------------------- cards
@Composable
fun Modifier.bnCard(radius: Dp = 12.dp, elevation: Dp = 1.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.shadow(elevation, shape).clip(shape).background(bn().card)
}

// ---------------------------------------------------------------- buttons
enum class BtnVariant { Default, Outline, Ghost, Destructive }

@Composable
fun BnButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BtnVariant = BtnVariant.Default,
    small: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color? = null,
) {
    val c = bn()
    val shape = RoundedCornerShape(10.dp)
    val bg = when (variant) {
        BtnVariant.Default -> c.primary
        BtnVariant.Destructive -> c.destructive
        BtnVariant.Outline -> c.background
        BtnVariant.Ghost -> Color.Transparent
    }
    val fg = contentColor ?: when (variant) {
        BtnVariant.Default -> c.primaryForeground
        BtnVariant.Destructive -> c.destructiveForeground
        else -> c.foreground
    }
    Box(
        modifier = modifier
            .height(if (small) 36.dp else 40.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(bg)
            .then(if (variant == BtnVariant.Outline) Modifier.border(1.dp, c.input, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(if (small) 14.dp else 16.dp))
                Spacer(Modifier.width(if (small) 6.dp else 8.dp))
            }
            Txt(text, size = 14, weight = FontWeight.Medium, color = fg, maxLines = 1)
        }
    }
}

@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = bn().foreground,
    size: Dp = 20.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size))
    }
}

// ---------------------------------------------------------------- input
@Composable
fun BnInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLength: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    bordered: Boolean = true,
    textSize: Int = 14,
    display: Boolean = false,
    weight: FontWeight = FontWeight.Normal,
    fill: Color? = null,
    radius: Dp = 10.dp,
    innerPadding: Dp = 12.dp,
    placeholderColor: Color? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    val c = bn()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var showPassword by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(radius)
    val family = if (display) BnFonts.display else BnFonts.body

    val frame = if (bordered) {
        Modifier
            .clip(shape)
            .background(fill ?: c.background)
            .border(1.dp, if (focused) c.primary else c.input, shape)
    } else {
        Modifier
    }

    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        modifier = modifier.then(frame),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = TextStyle(
            color = c.foreground,
            fontSize = textSize.sp,
            fontFamily = family,
            fontWeight = weight,
            lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else if (textSize == 14) 20.sp else (textSize * 1.4f).sp,
        ),
        cursorBrush = SolidColor(c.primary),
        visualTransformation = if (password && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
        interactionSource = interaction,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .then(if (singleLine && bordered) Modifier.height(40.dp) else Modifier.heightIn(min = if (bordered) 40.dp else 0.dp))
                    .padding(horizontal = if (bordered) innerPadding else 0.dp, vertical = if (singleLine || !bordered) 0.dp else if (innerPadding > 12.dp) innerPadding else 10.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (leading != null) {
                    Icon(leading, null, tint = c.mutedForeground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Txt(placeholder, size = textSize, color = placeholderColor ?: c.mutedForeground, display = display, weight = weight)
                    }
                    inner()
                }
                if (password) {
                    Icon(
                        if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = c.mutedForeground,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showPassword = !showPassword },
                    )
                }
            }
        },
    )
}

// ---------------------------------------------------------------- badge / avatar / cover
@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    outline: Boolean = false,
    size: Int = 12,
) {
    val c = bn()
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .then(if (outline) Modifier.border(1.dp, c.border, shape) else Modifier.background(c.secondary))
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        Txt(
            text, size = size, weight = FontWeight.SemiBold,
            color = if (outline) c.mutedForeground else c.secondaryForeground, maxLines = 1,
        )
    }
}

/** Free pill: bg-accent/10 text-accent */
@Composable
fun FreePill(size: Int = 12, horizontal: Dp = 10.dp) {
    val c = bn()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.accent.copy(alpha = 0.10f))
            .padding(horizontal = horizontal, vertical = 2.dp),
    ) {
        Txt("Free", size = size, weight = FontWeight.SemiBold, color = c.accent)
    }
}

@Composable
fun Avatar(
    url: String?,
    name: String,
    size: Dp,
    modifier: Modifier = Modifier,
    ring: Boolean = false,
    textSize: Int = 14,
) {
    val c = bn()
    Box(
        modifier = modifier
            .size(size)
            .then(if (ring) Modifier.border(2.dp, c.primary.copy(alpha = 0.2f), CircleShape) else Modifier)
            .clip(CircleShape)
            .background(c.secondary),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Txt(
                name.take(1).uppercase().ifEmpty { "?" },
                size = textSize, weight = FontWeight.Bold, color = c.mutedForeground, display = true,
            )
        }
    }
}

@Composable
fun coverBrush(): Brush {
    val c = bn()
    return Brush.linearGradient(
        listOf(c.primary.copy(alpha = 0.30f), c.primary.copy(alpha = 0.10f), c.accent.copy(alpha = 0.20f)),
    )
}

@Composable
fun BookCover(
    url: String?,
    title: String,
    author: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    titleSize: Int = 14,
    padding: Dp = 16.dp,
) {
    val c = bn()
    Box(modifier = modifier.clip(shape).background(coverBrush())) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Txt(
                    title, size = titleSize, weight = FontWeight.Bold, color = c.primary.copy(alpha = 0.8f),
                    display = true, align = TextAlign.Center, lineHeight = (titleSize * 1.25f).sp,
                )
                if (!author.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Txt(author, size = 10, color = c.mutedForeground, align = TextAlign.Center)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- misc
@Composable
fun CenterText(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Txt(text, color = bn().mutedForeground)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = bn()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Txt(text, size = 14, color = c.mutedForeground, align = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            BnButton(actionLabel, onAction, variant = BtnVariant.Outline, small = true)
        }
    }
}

@Composable
fun ToastHost(toaster: Toaster, modifier: Modifier = Modifier) {
    val c = bn()
    val msg = toaster.message
    var last by remember { mutableStateOf("") }
    var lastError by remember { mutableStateOf(false) }
    if (msg != null && (last != msg || lastError != toaster.isError)) {
        last = msg
        lastError = toaster.isError
    }
    LaunchedEffect(toaster.serial) {
        if (toaster.message != null) {
            delay(2600)
            toaster.clear()
        }
    }
    AnimatedVisibility(
        visible = msg != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier,
    ) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .shadow(6.dp, shape)
                .clip(shape)
                .background(c.card)
                .border(1.dp, c.border, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Txt(last, size = 14, weight = FontWeight.Medium, color = if (lastError) c.destructive else c.foreground)
        }
    }
}
