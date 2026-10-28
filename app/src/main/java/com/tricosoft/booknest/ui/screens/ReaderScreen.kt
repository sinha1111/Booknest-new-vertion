package com.tricosoft.booknest.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tricosoft.booknest.data.BookDetailData
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.CenterText
import com.tricosoft.booknest.ui.components.IconBtn
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.topSafe

@Composable
fun ReaderScreen(nav: NavController, id: String, initialChapter: Int) {
    val g = LocalGraph.current
    val c = bn()

    var data by remember(id) { mutableStateOf<BookDetailData?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var chapterIndex by remember(id) { mutableIntStateOf(initialChapter) }
    var fontSize by remember { mutableIntStateOf(16) }
    var showControls by remember { mutableStateOf(true) }
    val scroll = rememberScrollState()

    LaunchedEffect(id) {
        loading = true
        try {
            data = g.repo.bookWithChapters(id)
        } catch (e: Exception) {
            g.toaster.error(e.message ?: "Failed to load book")
        }
        loading = false
        // Server nije user ke auth.uid() theke chine; login user er jonno protita book e 1 bar count hoy
        if (data != null) {
            try { g.repo.trackRead(id) } catch (e: Exception) { /* ignore */ }
        }
    }

    LaunchedEffect(chapterIndex) { scroll.scrollTo(0) }

    val d = data
    if (loading) {
        CenterText("Loading...")
        return
    }
    if (d == null || d.chapters.isEmpty()) {
        CenterText("Book not found")
        return
    }

    val chapters = d.chapters
    val index = chapterIndex.coerceIn(0, chapters.size - 1)
    val chapter = chapters[index]
    val hasPrev = index > 0
    val hasNext = index < chapters.size - 1
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = noRipple, indication = null) { showControls = !showControls },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(start = 24.dp, end = 24.dp, top = topSafe() + 56.dp, bottom = 128.dp),
        ) {
            Txt(
                chapter.title, size = 20, weight = FontWeight.Bold, display = true,
                align = TextAlign.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
            Txt(
                chapter.content, size = fontSize, color = c.foreground.copy(alpha = 0.9f),
                lineHeight = (fontSize * 1.9f).sp,
            )
        }

        // top bar
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.background.copy(alpha = 0.95f))
                    .clickable(interactionSource = noRipple, indication = null) { }
                    .padding(start = 16.dp, end = 16.dp, top = topSafe(), bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, onClick = { nav.popBackStack() }, contentDescription = "Back")
                Column(modifier = Modifier.weight(1f)) {
                    Txt(d.book.book.title, size = 14, weight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Txt(chapter.title, size = 12, color = c.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconBtn(
                    Icons.Outlined.TextFields,
                    onClick = { fontSize = if (fontSize >= 22) 14 else fontSize + 2 },
                    contentDescription = "Font size",
                )
            }
        }

        // bottom bar
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.background.copy(alpha = 0.95f))
                    .clickable(interactionSource = noRipple, indication = null) { },
            ) {
                HorizontalDivider(color = c.border, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    NavTextButton("Previous", hasPrev, isNext = false) { chapterIndex = index - 1 }
                    Txt("${index + 1} / ${chapters.size}", size = 12, color = c.mutedForeground)
                    NavTextButton("Next", hasNext, isNext = true) { chapterIndex = index + 1 }
                }
            }
        }
    }
}

@Composable
private fun NavTextButton(label: String, enabled: Boolean, isNext: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isNext) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp), tint = bn().foreground)
            Spacer(Modifier.width(4.dp))
        }
        Txt(label, size = 14, weight = FontWeight.Medium)
        if (isNext) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = bn().foreground)
        }
    }
}
