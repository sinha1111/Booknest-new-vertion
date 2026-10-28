package com.tricosoft.booknest.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tricosoft.booknest.data.BookDetailData
import com.tricosoft.booknest.data.CommentWithProfile
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.Avatar
import com.tricosoft.booknest.ui.components.Badge
import com.tricosoft.booknest.ui.components.BnButton
import com.tricosoft.booknest.ui.components.BnInput
import com.tricosoft.booknest.ui.components.BookCover
import com.tricosoft.booknest.ui.components.CenterText
import com.tricosoft.booknest.ui.components.FreePill
import com.tricosoft.booknest.ui.components.IconBtn
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.bnCard
import com.tricosoft.booknest.ui.components.topSafe
import com.tricosoft.booknest.ui.formatCount
import com.tricosoft.booknest.ui.timeAgo
import kotlinx.coroutines.launch

@Composable
fun BookDetailScreen(nav: NavController, id: String) {
    val g = LocalGraph.current
    val c = bn()
    val scope = rememberCoroutineScope()
    val session by g.api.session.collectAsState()
    val uid = session?.userId

    var data by remember(id) { mutableStateOf<BookDetailData?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var liked by remember(id) { mutableStateOf(false) }
    var following by remember(id) { mutableStateOf(false) }
    var comments by remember(id) { mutableStateOf<List<CommentWithProfile>>(emptyList()) }
    var newComment by remember(id) { mutableStateOf("") }
    var submitting by remember(id) { mutableStateOf(false) }
    var likesCount by remember(id) { mutableIntStateOf(0) }
    var commentsCount by remember(id) { mutableIntStateOf(0) }

    suspend fun loadComments() {
        try {
            comments = g.repo.comments(id)
        } catch (e: Exception) {
            // ignore
        }
    }

    LaunchedEffect(id) {
        loading = true
        try {
            data = g.repo.bookWithChapters(id)
        } catch (e: Exception) {
            g.toaster.error(e.message ?: "Failed to load book")
        }
        loading = false
        data?.let {
            likesCount = it.book.book.likesCount
            commentsCount = it.book.book.commentsCount
            loadComments()
        }
    }

    LaunchedEffect(uid, data?.book?.book?.id) {
        val d = data
        if (uid != null && d != null) {
            try { liked = g.repo.isLiked(uid, d.book.book.id) } catch (e: Exception) { /* ignore */ }
            try { following = g.repo.isFollowing(uid, d.book.book.userId) } catch (e: Exception) { /* ignore */ }
        } else {
            liked = false
            following = false
        }
    }

    val d = data
    if (loading) {
        CenterText("Loading...")
        return
    }
    if (d == null) {
        CenterText("Book not found")
        return
    }
    val book = d.book.book

    fun toggleLike() {
        if (uid == null) { nav.navigate("auth"); return }
        scope.launch {
            try {
                if (liked) {
                    g.repo.unlike(uid, book.id)
                    liked = false
                    likesCount = (likesCount - 1).coerceAtLeast(0)
                    g.toaster.show("Removed like")
                } else {
                    g.repo.like(uid, book.id)
                    liked = true
                    likesCount += 1
                    g.toaster.show("Liked!")
                }
            } catch (e: Exception) {
                g.toaster.error(if (liked) "Could not remove like" else "Could not like this book")
            }
        }
    }

    fun toggleFollow() {
        if (uid == null) { nav.navigate("auth"); return }
        if (book.userId == uid) return
        scope.launch {
            try {
                if (following) {
                    g.repo.unfollow(uid, book.userId)
                    following = false
                    g.toaster.show("Unfollowed")
                } else {
                    g.repo.follow(uid, book.userId)
                    following = true
                    g.toaster.show("Following!")
                }
            } catch (e: Exception) {
                g.toaster.error(if (following) "Could not unfollow" else "Could not follow")
            }
        }
    }

    fun submitComment() {
        if (uid == null) { nav.navigate("auth"); return }
        val text = newComment.trim()
        if (text.isEmpty()) return
        scope.launch {
            submitting = true
            try {
                g.repo.addComment(uid, book.id, text)
                newComment = ""
                commentsCount += 1
                loadComments()
                g.toaster.show("Comment added!")
            } catch (e: Exception) {
                g.toaster.error(e.message ?: "Could not add comment")
            }
            submitting = false
        }
    }

    fun removeComment(commentId: String) {
        scope.launch {
            try {
                g.repo.deleteComment(commentId)
                commentsCount = (commentsCount - 1).coerceAtLeast(0)
                loadComments()
                g.toaster.show("Comment deleted")
            } catch (e: Exception) {
                g.toaster.error("Could not delete comment")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // sticky top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.background)
                .padding(start = 16.dp, end = 16.dp, top = topSafe(), bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, onClick = { nav.popBackStack() }, contentDescription = "Back")
            Spacer(Modifier.weight(1f))
            IconBtn(Icons.Outlined.Share, onClick = { })
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BookCover(
                url = book.coverUrl, title = book.title, author = d.book.authorName,
                modifier = Modifier
                    .width(192.dp)
                    .aspectRatio(3f / 4f)
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp), titleSize = 18, padding = 24.dp,
            )

            Txt(
                book.title, size = 24, weight = FontWeight.Bold, display = true, align = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { toggleFollow() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Avatar(d.book.authorAvatar, d.book.authorName, 24.dp, textSize = 10)
                Txt(d.book.authorName, size = 14, color = c.mutedForeground)
                if (uid != null && book.userId != uid) {
                    Icon(
                        if (following) Icons.Outlined.HowToReg else Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = if (following) c.primary else c.mutedForeground,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatInline(if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, likesCount, if (liked) c.destructive else c.mutedForeground)
                StatInline(Icons.Outlined.ChatBubbleOutline, commentsCount, c.mutedForeground)
                StatInline(Icons.Outlined.Visibility, book.readsCount, c.mutedForeground)
            }

            if (!book.genre.isNullOrBlank()) {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Badge(book.genre)
                    Spacer(Modifier.width(0.dp))
                    FreePill(size = 12, horizontal = 12.dp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BnButton(
                    "Start Reading", onClick = { nav.navigate("read/${book.id}?chapter=0") },
                    icon = Icons.Outlined.MenuBook, modifier = Modifier.weight(1f),
                )
                val heartShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(heartShape)
                        .background(c.background)
                        .border(1.dp, if (liked) c.destructive.copy(alpha = 0.3f) else c.input, heartShape)
                        .clickable { toggleLike() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (liked) c.destructive else c.foreground,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (!book.description.isNullOrBlank()) {
                    Txt("About This Book", size = 16, weight = FontWeight.SemiBold, display = true, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                    Txt(book.description, size = 14, color = c.mutedForeground, lineHeight = 23.sp)
                }

                if (d.chapters.isNotEmpty()) {
                    Txt(
                        "Chapters (${d.chapters.size})", size = 16, weight = FontWeight.SemiBold, display = true,
                        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        d.chapters.forEachIndexed { i, ch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { nav.navigate("read/${book.id}?chapter=$i") }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c.secondary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Txt(ch.order.toString(), size = 12, weight = FontWeight.SemiBold, color = c.secondaryForeground)
                                }
                                Txt(ch.title, size = 14, weight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // comments
                Txt(
                    "Comments ($commentsCount)", size = 16, weight = FontWeight.SemiBold, display = true,
                    modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    BnInput(
                        value = newComment, onValueChange = { newComment = it },
                        placeholder = if (uid != null) "Write a comment..." else "Login to comment",
                        singleLine = false, minLines = 2, enabled = uid != null,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (uid != null) 1f else 0.5f),
                    )
                    val canSend = newComment.isNotBlank() && !submitting
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(if (canSend) 1f else 0.5f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.primary)
                            .clickable(enabled = canSend) { submitComment() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send", tint = c.primaryForeground, modifier = Modifier.size(16.dp))
                    }
                }

                if (comments.isEmpty()) {
                    Txt(
                        "No comments yet. Be the first!", size = 14, color = c.mutedForeground, align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        comments.forEach { cm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bnCard(12.dp, 1.dp)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Avatar(cm.avatarUrl, cm.name, 32.dp, textSize = 12)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Txt(cm.name.ifBlank { "Unknown" }, size = 14, weight = FontWeight.SemiBold)
                                        Txt(timeAgo(cm.comment.createdAt), size = 10, color = c.mutedForeground)
                                    }
                                    Txt(cm.comment.content, size = 14, color = c.mutedForeground, modifier = Modifier.padding(top = 4.dp))
                                }
                                if (uid != null && cm.comment.userId == uid) {
                                    IconBtn(
                                        Icons.Outlined.Delete, onClick = { removeComment(cm.comment.id) },
                                        tint = c.mutedForeground, size = 14.dp,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatInline(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, tint: androidx.compose.ui.graphics.Color) {
    val c = bn()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Txt(formatCount(count), size = 14, color = c.mutedForeground)
    }
}
