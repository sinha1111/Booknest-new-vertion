package com.tricosoft.booknest.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tricosoft.booknest.data.Book
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.BnButton
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.bnCard
import com.tricosoft.booknest.ui.components.topSafe
import com.tricosoft.booknest.ui.timeAgo

@Composable
fun WriteScreen(nav: NavController) {
    val g = LocalGraph.current
    val c = bn()
    val session by g.api.session.collectAsState()
    val uid = session?.userId
    var drafts by remember { mutableStateOf<List<Book>>(emptyList()) }
    var published by remember { mutableStateOf<List<Book>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid == null) {
            nav.navigate("auth") { launchSingleTop = true }
            return@LaunchedEffect
        }
        loading = true
        try {
            val all = g.repo.myBooksByUpdated(uid)
            drafts = all.filter { !it.isPublished }
            published = all.filter { it.isPublished }
        } catch (e: Exception) {
            g.toaster.error(e.message ?: "Failed to load")
        }
        loading = false
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = topSafe(), bottom = 16.dp)) {
            Txt("My Writing", size = 20, weight = FontWeight.Bold, display = true)
            Txt("Your drafts and published works", size = 14, color = c.mutedForeground, modifier = Modifier.padding(top = 4.dp))
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            BnButton(
                "Start a New Book", onClick = { nav.navigate("editor/new") },
                icon = Icons.Outlined.Add, modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
            if (loading) {
                Txt("Loading...", color = c.mutedForeground, align = TextAlign.Center, modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp))
            } else {
                SectionTitle(Icons.Outlined.Description, "Drafts")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (drafts.isNotEmpty()) {
                        drafts.forEach { d -> BookRow(d, c.primary, onClick = { nav.navigate("editor/${d.id}") }) }
                    } else {
                        Txt("No drafts yet", size = 14, color = c.mutedForeground, align = TextAlign.Center, modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                SectionTitle(Icons.Outlined.MenuBook, "Published")
                if (published.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        published.forEach { b -> BookRow(b, c.accent, onClick = { nav.navigate("editor/${b.id}") }) }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, c.border, RoundedCornerShape(12.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Txt("Your published books will appear here", size = 14, color = c.mutedForeground, align = TextAlign.Center)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val c = bn()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Txt(text, size = 16, weight = FontWeight.SemiBold, display = true)
    }
}

@Composable
private fun BookRow(b: Book, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val c = bn()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bnCard(12.dp, 1.dp)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Txt(
                b.title.ifBlank { "Untitled" }, size = 14, weight = FontWeight.SemiBold, display = true,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Txt(b.genre?.takeIf { it.isNotBlank() } ?: "No genre", size = 12, color = c.mutedForeground)
        }
        Txt(timeAgo(b.updatedAt), size = 10, color = c.mutedForeground, maxLines = 1)
    }
}
