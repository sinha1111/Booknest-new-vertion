package com.tricosoft.booknest.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tricosoft.booknest.data.BookUi
import com.tricosoft.booknest.data.GENRES
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.BnInput
import com.tricosoft.booknest.ui.components.BookCard
import com.tricosoft.booknest.ui.components.Chip
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.topSafe

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(nav: NavController) {
    val g = LocalGraph.current
    val c = bn()
    var books by remember { mutableStateOf<List<BookUi>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf<String?>(null) }
    var showAll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            books = g.repo.publishedBooks()
        } catch (e: Exception) {
            g.toaster.error(e.message ?: "Failed to load books")
        }
        loading = false
    }

    val filtered = books.filter { b ->
        val q = search.lowercase()
        val matchesSearch = q.isEmpty() || b.book.title.lowercase().contains(q) || b.authorName.lowercase().contains(q)
        val matchesGenre = genre == null || b.book.genre == genre
        matchesSearch && matchesGenre
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(c.background)) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = topSafe(), bottom = 12.dp)) {
                Txt("Explore", size = 20, weight = FontWeight.Bold, display = true, modifier = Modifier.padding(bottom = 12.dp))
                BnInput(
                    value = search, onValueChange = { search = it },
                    placeholder = "Search books or authors...",
                    leading = Icons.Outlined.Search,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                // collapsed: shudhu prothom line (max-h-8)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!showAll) Modifier.heightIn(max = 32.dp).clipToBounds() else Modifier),
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top, unbounded = true),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Chip("All", selected = genre == null, onClick = { genre = null }, horizontal = 12.dp, vertical = 4.dp, size = 12)
                        GENRES.forEach { gname ->
                            Chip(
                                gname, selected = genre == gname,
                                onClick = { genre = if (genre == gname) null else gname },
                                horizontal = 12.dp, vertical = 4.dp, size = 12,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { showAll = !showAll },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Txt(if (showAll) "Less" else "More", size = 12, color = c.mutedForeground)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (showAll) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
                Txt("Loading...", color = c.mutedForeground)
            }
        } else if (filtered.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Txt("No books found", size = 18, color = c.mutedForeground, display = true, align = TextAlign.Center)
                Txt(
                    "Try a different search or genre", size = 14,
                    color = c.mutedForeground.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filtered, key = { it.book.id }) { b ->
                    BookCard(b, onClick = { nav.navigate("book/${b.book.id}") })
                }
            }
        }
    }
}
