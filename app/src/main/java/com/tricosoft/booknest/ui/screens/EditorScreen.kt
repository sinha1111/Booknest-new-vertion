package com.tricosoft.booknest.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tricosoft.booknest.crypto.VaultCrypto
import com.tricosoft.booknest.data.ChapterPayload
import com.tricosoft.booknest.data.GENRES
import com.tricosoft.booknest.state.LocalGraph
import com.tricosoft.booknest.ui.bn
import com.tricosoft.booknest.ui.components.BnButton
import com.tricosoft.booknest.ui.components.BnInput
import com.tricosoft.booknest.ui.components.BtnVariant
import com.tricosoft.booknest.ui.components.CenterText
import com.tricosoft.booknest.ui.components.IconBtn
import com.tricosoft.booknest.ui.components.Txt
import com.tricosoft.booknest.ui.components.topSafe
import kotlinx.coroutines.launch
import javax.crypto.SecretKey

private data class EChapter(val id: String, val title: String, val content: String)

@Composable
fun EditorScreen(nav: NavController, routeId: String) {
    val g = LocalGraph.current
    val c = bn()
    val scope = rememberCoroutineScope()
    val session by g.api.session.collectAsState()
    val uid = session?.userId
    val isNew = routeId == "new"

    var bookId by remember(routeId) { mutableStateOf<String?>(if (isNew) null else routeId) }
    var title by remember(routeId) { mutableStateOf("") }
    var description by remember(routeId) { mutableStateOf("") }
    var genre by remember(routeId) { mutableStateOf("") }
    val chapters = remember(routeId) { mutableStateListOf(EChapter("temp-1", "Chapter 1", "")) }
    var active by remember(routeId) { mutableIntStateOf(0) }
    var saving by remember(routeId) { mutableStateOf(false) }
    var loading by remember(routeId) { mutableStateOf(!isNew) }
    var isPublished by remember(routeId) { mutableStateOf(false) }
    var genreMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uid, routeId) {
        if (uid == null) {
            nav.navigate("auth") { launchSingleTop = true }
            return@LaunchedEffect
        }
        if (isNew) return@LaunchedEffect
        loading = true
        try {
            val book = g.repo.editorBook(routeId)
            if (book == null || book.userId != uid) {
                g.toaster.error("Book not found")
                nav.popBackStack()
                return@LaunchedEffect
            }
            title = book.title
            description = book.description ?: ""
            genre = book.genre ?: ""
            isPublished = book.isPublished

            val raw = g.repo.rawChapters(routeId)
            if (raw.isNotEmpty()) {
                val key: SecretKey? = if (raw.any { it.isEncrypted }) g.vault.ensureUnlocked() else null
                if (raw.any { it.isEncrypted } && key == null) {
                    g.toaster.error("Unlock draft encryption to open this draft")
                    nav.popBackStack()
                    return@LaunchedEffect
                }
                val list = raw.map { ch ->
                    if (ch.isEncrypted && key != null) {
                        val d = VaultCrypto.decryptChapter(key, uid, routeId, ch.content)
                        EChapter(ch.id, d.title, d.content)
                    } else {
                        EChapter(ch.id, ch.title, ch.content)
                    }
                }
                chapters.clear()
                chapters.addAll(list)
                active = 0
            }
        } catch (e: Exception) {
            g.toaster.error("Could not open this draft (wrong key or corrupted data)")
            nav.popBackStack()
            return@LaunchedEffect
        }
        loading = false
    }

    /**
     * plain=false: unpublished => chapter encrypt hoye jay; published => plain (karon public).
     * plain=true: publish er age, chapter plain e save.
     * Saved book id ferot dey, fail/cancel hole null.
     */
    suspend fun persist(plain: Boolean): String? {
        val u = uid ?: return null
        val dek: SecretKey? = if (!plain && !isPublished) g.vault.ensureUnlocked() else null
        if (!plain && !isPublished && dek == null) {
            g.toaster.error("Unlock draft encryption to save")
            return null
        }
        saving = true
        try {
            val existing = bookId
            val t = title.ifBlank { "Untitled" }
            val cur: String = if (existing == null) {
                g.repo.createBook(u, t, description, genre).also { bookId = it }
            } else {
                g.repo.updateBook(existing, t, description, genre)
                existing
            }
            val payload = chapters.map { ch ->
                if (dek != null) {
                    ChapterPayload("", VaultCrypto.encryptChapter(dek, u, cur, ch.title, ch.content), true)
                } else {
                    ChapterPayload(ch.title, ch.content, false)
                }
            }
            g.repo.replaceChapters(cur, payload)
            return cur
        } catch (e: Exception) {
            g.toaster.error(e.message ?: "Failed to save")
            return null
        } finally {
            saving = false
        }
    }

    fun handleSave() {
        scope.launch {
            val id = persist(false)
            if (id != null) g.toaster.show(if (isPublished) "Changes saved" else "Encrypted draft saved \uD83D\uDD12")
        }
    }

    var confirmPublish by remember { mutableStateOf(false) }

    fun doPublish() {
        scope.launch {
            val id = persist(true) ?: return@launch
            try {
                g.repo.setPublished(id, true)
                isPublished = true
                g.toaster.show("Book published! \uD83C\uDF89")
            } catch (e: Exception) {
                g.toaster.error("Failed to publish")
                persist(false) // publish na hole draft abar encrypted kore rakhi
            }
        }
    }

    fun handlePublish() {
        if (title.isBlank()) { g.toaster.error("Please add a title first"); return }
        if (chapters.none { it.content.isNotBlank() }) { g.toaster.error("Write at least one chapter first"); return }
        confirmPublish = true
    }

    if (loading) {
        CenterText("Loading...")
        return
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .imePadding()) {
        // sticky header
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(c.background)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = topSafe(), bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, onClick = { nav.popBackStack() }, contentDescription = "Back")
                Txt(
                    title.ifBlank { "Untitled Book" }, size = 14, weight = FontWeight.SemiBold, display = true,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                BnButton(
                    if (saving) "..." else "Save", onClick = { handleSave() },
                    variant = BtnVariant.Outline, small = true, enabled = !saving, icon = Icons.Outlined.Save,
                )
            }
            HorizontalDivider(color = c.border, thickness = 1.dp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(
                    if (isPublished) Icons.Outlined.Public else Icons.Outlined.Lock,
                    contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(6.dp))
                Txt(
                    if (isPublished) "Published \u2014 public, stored unencrypted" else "Draft \u2014 end-to-end encrypted when you save",
                    size = 12, color = c.mutedForeground,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                BnInput(
                    title, { title = it }, placeholder = "Book Title", bordered = false,
                    textSize = 20, display = true, weight = FontWeight.Bold,
                    placeholderColor = c.mutedForeground.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth(),
                )
                BnInput(
                    description, { description = it }, placeholder = "Write a short description...",
                    bordered = false, singleLine = false, minLines = 2,
                    placeholderColor = c.mutedForeground.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth(),
                )
                Box {
                    val selShape = RoundedCornerShape(12.dp)
                    Row(
                        modifier = Modifier
                            .clip(selShape)
                            .background(c.card)
                            .border(1.dp, c.border, selShape)
                            .clickable { genreMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Txt(genre.ifBlank { "Select Genre" }, size = 14, color = if (genre.isBlank()) c.mutedForeground else c.foreground)
                    }
                    DropdownMenu(expanded = genreMenu, onDismissRequest = { genreMenu = false }, containerColor = c.card) {
                        DropdownMenuItem(
                            text = { Txt("Select Genre", color = c.mutedForeground) },
                            onClick = { genre = ""; genreMenu = false },
                        )
                        GENRES.forEach { gname ->
                            DropdownMenuItem(
                                text = { Txt(gname) },
                                onClick = { genre = gname; genreMenu = false },
                            )
                        }
                    }
                }
            }

            // chapter tabs
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chapters.forEachIndexed { i, _ ->
                    val selected = active == i
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) c.primary else c.secondary)
                            .clickable { active = i }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Txt(
                            "Ch ${i + 1}", size = 12, weight = FontWeight.Medium,
                            color = if (selected) c.primaryForeground else c.secondaryForeground,
                        )
                        if (selected && chapters.size > 1) {
                            Icon(
                                Icons.Outlined.Delete, contentDescription = "Delete chapter", tint = c.primaryForeground,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        chapters.removeAt(i)
                                        if (active >= chapters.size) active = chapters.size - 1
                                    },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.secondary.copy(alpha = 0.5f))
                        .clickable {
                            chapters.add(EChapter("temp-${System.currentTimeMillis()}", "Chapter ${chapters.size + 1}", ""))
                            active = chapters.size - 1
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = c.mutedForeground, modifier = Modifier.size(12.dp))
                    Txt("Add", size = 12, weight = FontWeight.Medium, color = c.mutedForeground)
                }
            }

            val current = chapters.getOrNull(active)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BnInput(
                    current?.title ?: "",
                    { v -> if (current != null) chapters[active] = current.copy(title = v) },
                    placeholder = "Chapter Title", bordered = false, textSize = 16, display = true,
                    weight = FontWeight.SemiBold, placeholderColor = c.mutedForeground.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                )
                BnInput(
                    current?.content ?: "",
                    { v -> if (current != null) chapters[active] = current.copy(content = v) },
                    placeholder = "Start writing your chapter...", singleLine = false, minLines = 20,
                    fill = c.card, radius = 12.dp, innerPadding = 16.dp,
                    lineHeight = 23.sp, placeholderColor = c.mutedForeground.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BnButton(
                    if (isPublished) "Save changes" else "Save Draft", onClick = { handleSave() },
                    variant = BtnVariant.Outline, enabled = !saving, modifier = Modifier.weight(1f),
                )
                if (!isPublished) {
                    BnButton("Publish", onClick = { handlePublish() }, enabled = !saving, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmPublish) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmPublish = false },
            containerColor = c.card,
            title = { Txt("Publish this book?", size = 18, weight = FontWeight.SemiBold, display = true) },
            text = {
                Txt(
                    "It becomes public, and its chapters will be stored unencrypted so readers can open them.",
                    size = 14, color = c.mutedForeground,
                )
            },
            confirmButton = {
                BnButton("Publish", onClick = { confirmPublish = false; doPublish() }, small = true)
            },
            dismissButton = {
                BnButton("Cancel", onClick = { confirmPublish = false }, variant = BtnVariant.Ghost, small = true)
            },
        )
    }
}
