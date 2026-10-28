package com.tricosoft.booknest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tricosoft.booknest.R
import com.tricosoft.booknest.data.BookUi
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.BookCard
import com.tricosoft.booknest.ui.components.Chip
import com.tricosoft.booknest.ui.components.EmptyState
import com.tricosoft.booknest.ui.components.FeaturedBookCard
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.topSafe

private val HOME_TABS = listOf("For You", "Trending", "Latest", "Following")

@Composable
fun HomeScreen(nav: NavController) {
    val g = LocalGraph.current
    val c = bn()
    val session by g.api.session.collectAsState()
    val uid = session?.userId

    var books by remember { mutableStateOf<List<BookUi>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf("For You") }
    var followingIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var likedGenres by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        loading = true
        try {
            books = g.repo.publishedBooks()
        } catch (e: Exception) {
            g.toaster.error(e.message ?: "Failed to load books")
        }
        loading = false
    }

    LaunchedEffect(uid) {
        if (uid == null) {
            followingIds = emptyList()
            likedGenres = emptyList()
        } else {
            try { followingIds = g.repo.followingIds(uid) } catch (e: Exception) { /* ignore */ }
            try { likedGenres = g.repo.likedGenres(uid) } catch (e: Exception) { /* ignore */ }
        }
    }

    val trending = remember(books) { books.sortedByDescending { it.book.likesCount + it.book.commentsCount } }
    val latest = remember(books) { books.sortedByDescending { it.book.createdAt } }
    val following = remember(books, followingIds) { books.filter { followingIds.contains(it.book.userId) } }
    val forYou = remember(books, likedGenres, uid) {
        if (uid == null || likedGenres.isEmpty()) {
            trending
        } else {
            val counts = HashMap<String, Int>()
            likedGenres.forEach { counts[it] = (counts[it] ?: 0) + 1 }
            books.sortedWith(
                compareByDescending<BookUi> { counts[it.book.genre ?: ""] ?: 0 }
                    .thenByDescending { it.book.likesCount + it.book.commentsCount },
            )
        }
    }
    val display = when (tab) {
        "Trending" -> trending
        "Latest" -> latest
        "Following" -> following
        else -> forYou
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // header
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(c.background)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = topSafe(), bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = "BookNest",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Txt("BookNest", size = 20, weight = FontWeight.Bold, display = true)
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                items(HOME_TABS) { t ->
                    Chip(t, selected = tab == t, onClick = { tab = t })
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
                Txt("Loading...", color = c.mutedForeground)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                if (tab == "For You" && trending.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Txt(
                                "Popular This Week", size = 18, weight = FontWeight.SemiBold, display = true,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(trending.take(4), key = { it.book.id }) { b ->
                                    FeaturedBookCard(b, onClick = { nav.navigate("book/${b.book.id}") })
                                }
                            }
                        }
                    }
                }
                if (tab == "For You" && display.isNotEmpty()) {
                    item {
                        Txt(
                            "All Books", size = 18, weight = FontWeight.SemiBold, display = true,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                        )
                    }
                }
                if (display.isEmpty()) {
                    item { EmptyState(Icons.Outlined.MenuBook, "No books found") }
                } else {
                    items(display, key = { it.book.id }) { b ->
                        Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                            BookCard(b, onClick = { nav.navigate("book/${b.book.id}") })
                        }
                    }
                }
            }
        }
    }
}
