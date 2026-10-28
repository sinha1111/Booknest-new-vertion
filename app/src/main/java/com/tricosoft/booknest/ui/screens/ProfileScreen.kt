package com.tricosoft.booknest.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tricosoft.booknest.data.Book
import com.tricosoft.booknest.data.CommentWithBook
import com.tricosoft.booknest.data.GENRES
import com.tricosoft.booknest.data.PersonItem
import com.tricosoft.booknest.data.Profile
import com.tricosoft.booknest.data.ReadItem
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.ImageUtils
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.Avatar
import com.tricosoft.booknest.ui.components.Badge
import com.tricosoft.booknest.ui.components.BnButton
import com.tricosoft.booknest.ui.components.BnInput
import com.tricosoft.booknest.ui.components.BtnVariant
import com.tricosoft.booknest.ui.components.Chip
import com.tricosoft.booknest.ui.components.ConfirmDialog
import com.tricosoft.booknest.ui.components.IconBtn
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.bnCard
import com.tricosoft.booknest.ui.components.topSafe
import com.tricosoft.booknest.ui.timeAgo
import kotlinx.coroutines.launch
import java.util.Locale

private val PROFILE_TABS = listOf("My Books", "Liked", "Comments", "Views", "Followers", "Following")

@Composable
fun ProfileScreen(nav: NavController) {
    val g = LocalGraph.current
    val c = bn()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by g.api.session.collectAsState()
    val uid = session?.userId

    var activeTab by remember { mutableStateOf("My Books") }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var profileError by remember { mutableStateOf("") }
    var myBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var likedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var comments by remember { mutableStateOf<List<CommentWithBook>>(emptyList()) }
    var history by remember { mutableStateOf<List<ReadItem>>(emptyList()) }
    var followers by remember { mutableStateOf<List<PersonItem>>(emptyList()) }
    var following by remember { mutableStateOf<List<PersonItem>>(emptyList()) }

    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var categoryMenu by remember { mutableStateOf(false) }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var avatarPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var saving by remember { mutableStateOf(false) }

    var deletingBookId by remember { mutableStateOf<String?>(null) }
    var confirmBook by remember { mutableStateOf<Book?>(null) }
    var confirmAccount by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }

    suspend fun loadProfile(u: String) {
        profileError = ""
        try {
            val p = g.repo.myProfile(u)
            profile = p
            editName = p.name
            editBio = p.bio ?: ""
            editCategory = p.category ?: ""
        } catch (e: Exception) {
            profileError = e.message ?: "Could not load your profile"
        }
    }

    suspend fun loadLists(u: String) {
        try { myBooks = g.repo.myBooks(u) } catch (e: Exception) { /* ignore */ }
        try { likedBooks = g.repo.likedBooks(u) } catch (e: Exception) { /* ignore */ }
        try { comments = g.repo.myComments(u) } catch (e: Exception) { /* ignore */ }
        try { history = g.repo.readHistory(u) } catch (e: Exception) { /* ignore */ }
        try { following = g.repo.following(u) } catch (e: Exception) { /* ignore */ }
        try { followers = g.repo.followers(u) } catch (e: Exception) { /* ignore */ }
    }

    LaunchedEffect(uid) {
        if (uid == null) {
            nav.navigate("auth") { launchSingleTop = true }
            return@LaunchedEffect
        }
        loadProfile(uid)
        loadLists(uid)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = ImageUtils.prepareAvatar(ctx, uri)
            if (bytes == null) {
                g.toaster.error("Could not read this image")
            } else {
                avatarBytes = bytes
                avatarPreview = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
    }

    val p = profile
    if (uid == null || p == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (profileError.isNotEmpty()) {
                Txt("Could not load your profile", size = 14, weight = FontWeight.Medium, color = c.destructive)
                Txt(profileError, size = 12, color = c.mutedForeground, align = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                BnButton("Retry", onClick = { uid?.let { u -> scope.launch { loadProfile(u) } } })
            } else {
                Txt("Loading...", color = c.mutedForeground)
            }
        }
        return
    }

    fun cancelEditing() {
        editing = false
        editName = p.name
        editBio = p.bio ?: ""
        editCategory = p.category ?: ""
        avatarBytes = null
        avatarPreview = null
    }

    fun saveProfile() {
        scope.launch {
            saving = true
            try {
                var url: String? = null
                val bytes = avatarBytes
                if (bytes != null) {
                    try {
                        url = g.repo.uploadAvatar(uid, bytes)
                    } catch (e: Exception) {
                        g.toaster.error("Avatar upload failed (JPG/PNG/WebP, max 2MB)")
                    }
                }
                g.repo.updateProfile(uid, editName, editBio, editCategory, url)
                g.toaster.show("Profile updated!")
                editing = false
                avatarBytes = null
                avatarPreview = null
                loadProfile(uid)
            } catch (e: Exception) {
                g.toaster.error("Failed to save profile")
            }
            saving = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = topSafe(), bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt("Profile", size = 20, weight = FontWeight.Bold, display = true)
            IconBtn(Icons.Outlined.Settings, onClick = { nav.navigate("settings") }, tint = c.mutedForeground, contentDescription = "Settings")
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // profile card
            Row(modifier = Modifier.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box {
                    val preview = avatarPreview
                    if (editing && preview != null) {
                        Image(
                            bitmap = preview, contentDescription = p.name, contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .border(2.dp, c.primary.copy(alpha = 0.2f), CircleShape)
                                .clip(CircleShape),
                        )
                    } else {
                        Avatar(p.avatarUrl, p.name, 64.dp, ring = true, textSize = 18)
                    }
                    if (editing) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(c.primary)
                                .clickable {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = "Change photo", tint = c.primaryForeground, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (editing) {
                        BnInput(
                            editName, { editName = it }, placeholder = "Your name", display = true, textSize = 18,
                            weight = FontWeight.Bold, maxLength = 80, modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Txt(p.name, size = 18, weight = FontWeight.Bold, display = true)
                        if (!p.category.isNullOrBlank()) {
                            Badge(p.category, modifier = Modifier.padding(top = 4.dp))
                        }
                        Txt(
                            p.bio?.takeIf { it.isNotBlank() } ?: "No bio yet", size = 14, color = c.mutedForeground,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            if (editing) {
                Column(modifier = Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Txt("Bio", size = 12, weight = FontWeight.Medium, color = c.mutedForeground, modifier = Modifier.padding(bottom = 4.dp))
                        BnInput(
                            editBio, { editBio = it }, placeholder = "Tell about yourself...", singleLine = false, minLines = 3,
                            maxLength = 500, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Column {
                        Txt("Category", size = 12, weight = FontWeight.Medium, color = c.mutedForeground, modifier = Modifier.padding(bottom = 4.dp))
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
                                Txt(editCategory.ifBlank { "Select category" }, size = 14, color = if (editCategory.isBlank()) c.mutedForeground else c.foreground)
                            }
                            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }, containerColor = c.card) {
                                DropdownMenuItem(text = { Txt("Select category", color = c.mutedForeground) }, onClick = { editCategory = ""; categoryMenu = false })
                                GENRES.forEach { gname ->
                                    DropdownMenuItem(text = { Txt(gname) }, onClick = { editCategory = gname; categoryMenu = false })
                                }
                            }
                        }
                    }
                }
            }

            // stats
            Row(modifier = Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBox(Icons.Outlined.MenuBook, "Books", myBooks.size.toString(), Modifier.weight(1f))
                StatBox(Icons.Outlined.People, "Followers", String.format(Locale.getDefault(), "%,d", p.followers), Modifier.weight(1f))
                StatBox(Icons.Outlined.FavoriteBorder, "Following", following.size.toString(), Modifier.weight(1f))
            }

            if (editing) {
                Row(modifier = Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BnButton(if (saving) "Saving..." else "Save", onClick = { saveProfile() }, enabled = !saving, icon = Icons.Outlined.Check, modifier = Modifier.weight(1f))
                    BnButton("Cancel", onClick = { cancelEditing() }, variant = BtnVariant.Outline, icon = Icons.Outlined.Close, modifier = Modifier.weight(1f))
                }
            } else {
                BnButton(
                    "Edit Profile", onClick = { editing = true }, variant = BtnVariant.Outline, icon = Icons.Outlined.Edit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
                BnButton(
                    "Sign Out", variant = BtnVariant.Ghost, contentColor = c.destructive, icon = Icons.AutoMirrored.Outlined.Logout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    onClick = {
                        scope.launch {
                            g.api.signOut()
                            nav.navigate("auth") { popUpTo(0) }
                        }
                    },
                )
                BnButton(
                    "Delete Account", variant = BtnVariant.Destructive, icon = Icons.Outlined.Delete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    onClick = { confirmAccount = true },
                )
            }

            // tabs
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PROFILE_TABS.forEach { tab ->
                    Chip(
                        tab, selected = activeTab == tab, onClick = { activeTab = tab },
                        shape = RoundedCornerShape(12.dp), horizontal = 12.dp, vertical = 8.dp, size = 12,
                        unselectedTransparent = true,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (activeTab) {
                    "My Books" -> if (myBooks.isEmpty()) {
                        EmptyTab(Icons.Outlined.MenuBook, "No books written yet", "Start Writing") { nav.navigate("write") }
                    } else myBooks.forEach { book ->
                        BookItem(book, showDraft = !book.isPublished, showReads = true, fallbackTitle = "Untitled", fallbackDesc = "No description") {
                            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BnButton("Edit", onClick = { nav.navigate("editor/${book.id}") }, variant = BtnVariant.Outline, small = true, icon = Icons.Outlined.Edit, modifier = Modifier.weight(1f))
                                BnButton(
                                    "Delete", onClick = { confirmBook = book }, variant = BtnVariant.Outline, small = true,
                                    icon = Icons.Outlined.Delete, contentColor = c.destructive, modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    "Liked" -> if (likedBooks.isEmpty()) {
                        EmptyTab(Icons.Outlined.FavoriteBorder, "No liked books yet", "Explore Books") { nav.navigate("explore") }
                    } else likedBooks.forEach { book ->
                        BookItem(book, showDraft = false, showReads = false, fallbackTitle = "", fallbackDesc = "", onClick = { nav.navigate("book/${book.id}") }, trailing = {
                            IconBtn(
                                Icons.Filled.Favorite, tint = c.destructive, size = 16.dp,
                                onClick = {
                                    scope.launch {
                                        try {
                                            g.repo.unlike(uid, book.id)
                                            likedBooks = likedBooks.filter { it.id != book.id }
                                            g.toaster.show("Removed from liked!")
                                        } catch (e: Exception) {
                                            g.toaster.error("Could not remove like")
                                        }
                                    }
                                },
                            )
                        })
                    }
                    "Comments" -> if (comments.isEmpty()) {
                        EmptyTab(Icons.Outlined.ChatBubbleOutline, "No comments yet", "Explore Books") { nav.navigate("explore") }
                    } else comments.forEach { cm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bnCard(12.dp, 1.dp)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = c.mutedForeground, modifier = Modifier
                                .padding(top = 4.dp)
                                .size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Txt(
                                    cm.bookTitle, size = 12, weight = FontWeight.SemiBold, color = c.primary,
                                    modifier = Modifier.clickable { nav.navigate("book/${cm.comment.bookId}") },
                                )
                                Txt(cm.comment.content, size = 14, modifier = Modifier.padding(top = 4.dp))
                                Txt(timeAgo(cm.comment.createdAt), size = 10, color = c.mutedForeground)
                            }
                            IconBtn(
                                Icons.Outlined.Close, tint = c.mutedForeground, size = 14.dp,
                                onClick = {
                                    scope.launch {
                                        try {
                                            g.repo.deleteComment(cm.comment.id)
                                            comments = comments.filter { it.comment.id != cm.comment.id }
                                            g.toaster.show("Comment deleted")
                                        } catch (e: Exception) {
                                            g.toaster.error("Could not delete comment")
                                        }
                                    }
                                },
                            )
                        }
                    }
                    "Views" -> if (history.isEmpty()) {
                        EmptyTab(Icons.Outlined.Visibility, "No reading history yet", "Start Reading") { nav.navigate("explore") }
                    } else history.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bnCard(12.dp, 1.dp)
                                .clickable { nav.navigate("book/${item.row.bookId}") }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SmallCover(item.cover, Icons.Outlined.Visibility)
                            Column(modifier = Modifier.weight(1f)) {
                                Txt(item.title, size = 16, weight = FontWeight.Bold, display = true, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (item.genre.isNotBlank()) Badge(item.genre, size = 10, modifier = Modifier.padding(top = 4.dp))
                                Txt(timeAgo(item.row.createdAt), size = 10, color = c.mutedForeground, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    "Followers" -> if (followers.isEmpty()) {
                        EmptyTab(Icons.Outlined.People, "No followers yet", "Share Your Work") { nav.navigate("explore") }
                    } else followers.forEach { person -> PersonRow(person, null) }
                    "Following" -> if (following.isEmpty()) {
                        EmptyTab(Icons.Outlined.People, "Not following anyone yet", "Discover Authors") { nav.navigate("explore") }
                    } else following.forEach { person ->
                        PersonRow(person) {
                            scope.launch {
                                try {
                                    g.repo.unfollowById(person.followId)
                                    following = following.filter { it.followId != person.followId }
                                    g.toaster.show("Unfollowed!")
                                } catch (e: Exception) {
                                    g.toaster.error("Could not unfollow")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    val bookToDelete = confirmBook
    if (bookToDelete != null) {
        ConfirmDialog(
            title = "Delete this book?",
            text = "\"${bookToDelete.title.ifBlank { "Untitled" }}\" and its chapters will be permanently deleted. This cannot be undone.",
            confirmLabel = if (deletingBookId == bookToDelete.id) "Deleting..." else "Delete",
            busy = deletingBookId == bookToDelete.id,
            onDismiss = { confirmBook = null },
            onConfirm = {
                scope.launch {
                    deletingBookId = bookToDelete.id
                    try {
                        g.repo.deleteBook(bookToDelete.id)
                        myBooks = myBooks.filter { it.id != bookToDelete.id }
                        g.toaster.show("Book deleted")
                    } catch (e: Exception) {
                        g.toaster.error("Failed to delete book")
                    }
                    deletingBookId = null
                    confirmBook = null
                }
            },
        )
    }

    if (confirmAccount) {
        ConfirmDialog(
            title = "Delete your account?",
            text = "This permanently deletes your BookNest profile, books, comments and encryption keys. This action cannot be undone.",
            confirmLabel = if (deletingAccount) "Deleting..." else "Delete Forever",
            busy = deletingAccount,
            onDismiss = { confirmAccount = false },
            onConfirm = {
                scope.launch {
                    deletingAccount = true
                    try {
                        g.repo.deleteAccountData()
                        g.toaster.show("Your account has been deleted")
                        confirmAccount = false
                        g.api.signOut()
                        nav.navigate("auth") { popUpTo(0) }
                    } catch (e: Exception) {
                        g.toaster.error(e.message ?: "Failed to delete account")
                    }
                    deletingAccount = false
                }
            },
        )
    }
}

@Composable
private fun StatBox(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    val c = bn()
    Column(
        modifier = modifier
            .bnCard(12.dp, 1.dp)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(16.dp))
        Txt(value, size = 18, weight = FontWeight.Bold, display = true, modifier = Modifier.padding(top = 4.dp))
        Txt(label, size = 10, color = c.mutedForeground)
    }
}

@Composable
private fun EmptyTab(icon: ImageVector, text: String, action: String, onAction: () -> Unit) {
    val c = bn()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(32.dp))
        Txt(text, size = 14, color = c.mutedForeground, modifier = Modifier.padding(top = 8.dp))
        BnButton(action, onClick = onAction, variant = BtnVariant.Outline, small = true, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SmallCover(url: String?, fallbackIcon: ImageVector) {
    val c = bn()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(64.dp)
            .clip(shape)
            .background(c.secondary),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(fallbackIcon, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BookItem(
    book: Book,
    showDraft: Boolean,
    showReads: Boolean,
    fallbackTitle: String,
    fallbackDesc: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    val c = bn()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bnCard(12.dp, 1.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallCover(book.coverUrl, Icons.Outlined.MenuBook)
            Column(modifier = Modifier.weight(1f)) {
                Txt(
                    book.title.ifBlank { fallbackTitle }, size = 16, weight = FontWeight.Bold, display = true,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Txt(
                    (book.description?.takeIf { it.isNotBlank() } ?: fallbackDesc), size = 12, color = c.mutedForeground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!book.genre.isNullOrBlank()) Badge(book.genre, size = 10)
                    if (showDraft) Badge("Draft", outline = true, size = 10)
                    Txt("\u2764 ${book.likesCount}", size = 10, color = c.mutedForeground)
                    if (showReads) Txt("\uD83D\uDC41 ${book.readsCount}", size = 10, color = c.mutedForeground)
                }
            }
            trailing?.invoke()
        }
        actions()
    }
}

@Composable
private fun PersonRow(person: PersonItem, onUnfollow: (() -> Unit)?) {
    val c = bn()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bnCard(12.dp, 1.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(person.avatarUrl, person.name, 40.dp, textSize = 14)
        Column(modifier = Modifier.weight(1f)) {
            Txt(person.name.ifBlank { "Unknown" }, size = 14, weight = FontWeight.Bold, display = true)
            if (person.category.isNotBlank()) Badge(person.category, size = 10, modifier = Modifier.padding(top = 2.dp))
            Txt(person.bio, size = 12, color = c.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
        if (onUnfollow != null) {
            BnButton("Unfollow", onClick = onUnfollow, variant = BtnVariant.Outline, small = true, icon = Icons.Outlined.PersonRemove)
        }
    }
}
