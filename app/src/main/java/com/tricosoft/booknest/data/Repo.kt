package com.tricosoft.booknest.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

const val LOCKED_NOTE =
    "\uD83D\uDD12 This draft chapter is end-to-end encrypted. Open it in the editor and unlock to read it."

const val AVATAR_BUCKET = "booknest-avatars"

val GENRES = listOf(
    "Fiction", "Romance", "Fantasy", "Sci-Fi", "Mystery",
    "Thriller", "Horror", "Poetry", "Non-Fiction", "Biography",
    "Self-Help", "Adventure",
)

private const val BOOK_COLS = "id, title, description, genre, cover_url, is_published, likes_count, reads_count, comments_count, created_at, updated_at, user_id"

/** Web er shob supabase query hubohu eikhane (hooks + page gulor query). */
class Repo(private val api: Api) {

    private fun inList(ids: List<String>) = "in.(${ids.joinToString(",")})"

    private fun authorName(p: Profile?, fallback: String) =
        p?.name?.takeIf { it.isNotBlank() } ?: fallback

    // ------------------------------------------------------------ home / explore
    suspend fun publishedBooks(): List<BookUi> {
        val books = api.decodeList<Book>(
            api.get("books", listOf("select" to "*", "is_published" to "eq.true", "order" to "created_at.desc")),
        )
        if (books.isEmpty()) return emptyList()
        val ids = books.map { it.userId }.distinct()
        val profiles = api.decodeList<Profile>(
            api.get("profiles", listOf("select" to "user_id,name,avatar_url", "user_id" to inList(ids))),
        ).associateBy { it.userId }
        return books.map { b ->
            val p = profiles[b.userId]
            BookUi(b, authorName(p, "Unknown Author"), p?.avatarUrl ?: "")
        }
    }

    suspend fun followingIds(userId: String): List<String> =
        api.decodeList<FollowRow>(
            api.get("follows", listOf("select" to "id,following_id", "follower_id" to "eq.$userId")),
        ).map { it.followingId }

    suspend fun likedGenres(userId: String): List<String> {
        val likes = api.decodeList<LikeRow>(
            api.get("likes", listOf("select" to "book_id", "user_id" to "eq.$userId")),
        )
        if (likes.isEmpty()) return emptyList()
        val books = api.decodeList<Book>(
            api.get("books", listOf("select" to "id,genre", "id" to inList(likes.map { it.bookId }))),
        )
        return books.mapNotNull { it.genre }.filter { it.isNotEmpty() }
    }

    // ------------------------------------------------------------ book detail / reader
    suspend fun bookWithChapters(id: String): BookDetailData? {
        val book = api.decodeList<Book>(api.get("books", listOf("select" to "*", "id" to "eq.$id"))).firstOrNull()
            ?: return null
        val profile = api.decodeList<Profile>(
            api.get("profiles", listOf("select" to "user_id,name,avatar_url", "user_id" to "eq.${book.userId}")),
        ).firstOrNull()
        val chapters = api.decodeList<Chapter>(
            api.get("chapters", listOf("select" to "*", "book_id" to "eq.$id", "order" to "order.asc")),
        ).map { c ->
            if (c.isEncrypted) c.copy(title = c.title.ifBlank { "Encrypted chapter" }, content = LOCKED_NOTE) else c
        }
        return BookDetailData(BookUi(book, authorName(profile, "Unknown"), profile?.avatarUrl ?: ""), chapters)
    }

    suspend fun trackRead(bookId: String) {
        api.rpc("track_book_read", buildJsonObject { put("p_book_id", bookId) }.toString())
    }

    suspend fun isLiked(userId: String, bookId: String): Boolean =
        api.decodeList<IdRow>(
            api.get("likes", listOf("select" to "id", "user_id" to "eq.$userId", "book_id" to "eq.$bookId")),
        ).isNotEmpty()

    suspend fun isFollowing(followerId: String, followingId: String): Boolean =
        api.decodeList<IdRow>(
            api.get("follows", listOf("select" to "id", "follower_id" to "eq.$followerId", "following_id" to "eq.$followingId")),
        ).isNotEmpty()

    suspend fun like(userId: String, bookId: String) {
        api.insert("likes", buildJsonObject { put("user_id", userId); put("book_id", bookId) }.toString())
    }

    suspend fun unlike(userId: String, bookId: String) {
        api.delete("likes", listOf("user_id" to "eq.$userId", "book_id" to "eq.$bookId"))
    }

    suspend fun follow(followerId: String, followingId: String) {
        api.insert("follows", buildJsonObject { put("follower_id", followerId); put("following_id", followingId) }.toString())
    }

    suspend fun unfollow(followerId: String, followingId: String) {
        api.delete("follows", listOf("follower_id" to "eq.$followerId", "following_id" to "eq.$followingId"))
    }

    suspend fun unfollowById(followId: String) {
        api.delete("follows", listOf("id" to "eq.$followId"))
    }

    suspend fun comments(bookId: String): List<CommentWithProfile> {
        val rows = api.decodeList<CommentRow>(
            api.get(
                "comments",
                listOf("select" to "id,user_id,content,created_at", "book_id" to "eq.$bookId", "order" to "created_at.desc"),
            ),
        )
        if (rows.isEmpty()) return emptyList()
        val profiles = api.decodeList<Profile>(
            api.get("profiles", listOf("select" to "user_id,name,avatar_url", "user_id" to inList(rows.map { it.userId }.distinct()))),
        ).associateBy { it.userId }
        return rows.map { r ->
            val p = profiles[r.userId]
            CommentWithProfile(r, p?.name ?: "Unknown", p?.avatarUrl ?: "")
        }
    }

    suspend fun addComment(userId: String, bookId: String, content: String) {
        api.insert(
            "comments",
            buildJsonObject { put("user_id", userId); put("book_id", bookId); put("content", content) }.toString(),
        )
    }

    suspend fun deleteComment(id: String) {
        api.delete("comments", listOf("id" to "eq.$id"))
    }

    // ------------------------------------------------------------ profile
    suspend fun ensureProfile() {
        api.rpc("ensure_profile")
    }

    suspend fun myProfile(userId: String): Profile {
        ensureProfile()
        val p = api.decodeList<Profile>(
            api.get("profiles", listOf("select" to "user_id,name,bio,avatar_url,category,followers,following", "user_id" to "eq.$userId")),
        ).firstOrNull() ?: throw ApiException("Profile not found")
        val followers = api.count("follows", listOf("following_id" to "eq.$userId"))
        val following = api.count("follows", listOf("follower_id" to "eq.$userId"))
        return p.copy(followers = followers, following = following)
    }

    suspend fun updateProfile(userId: String, name: String, bio: String, category: String?, avatarUrl: String?) {
        val body = buildJsonObject {
            put("name", name)
            put("bio", bio)
            if (category != null) put("category", category)
            if (avatarUrl != null) put("avatar_url", avatarUrl)
        }.toString()
        api.update("profiles", listOf("user_id" to "eq.$userId"), body)
    }

    suspend fun uploadAvatar(userId: String, jpeg: ByteArray): String =
        api.uploadPublicObject(AVATAR_BUCKET, "$userId/avatar.jpg", jpeg, "image/jpeg")

    suspend fun myBooks(userId: String): List<Book> =
        api.decodeList(
            api.get("books", listOf("select" to BOOK_COLS, "user_id" to "eq.$userId", "order" to "created_at.desc")),
        )

    suspend fun myBooksByUpdated(userId: String): List<Book> =
        api.decodeList(
            api.get("books", listOf("select" to "id,title,genre,is_published,created_at,updated_at", "user_id" to "eq.$userId", "order" to "updated_at.desc")),
        )

    suspend fun deleteBook(id: String) {
        api.delete("books", listOf("id" to "eq.$id"))
    }

    suspend fun likedBooks(userId: String): List<Book> {
        val likes = api.decodeList<LikeRow>(
            api.get("likes", listOf("select" to "book_id", "user_id" to "eq.$userId")),
        )
        if (likes.isEmpty()) return emptyList()
        return api.decodeList(
            api.get("books", listOf("select" to BOOK_COLS, "id" to inList(likes.map { it.bookId }))),
        )
    }

    private suspend fun people(follows: List<FollowRow>, pick: (FollowRow) -> String): List<PersonItem> {
        if (follows.isEmpty()) return emptyList()
        val ids = follows.map(pick).distinct()
        val profiles = api.decodeList<Profile>(
            api.get("profiles", listOf("select" to "user_id,name,avatar_url,bio,category", "user_id" to inList(ids))),
        ).associateBy { it.userId }
        return follows.map { f ->
            val uid = pick(f)
            val p = profiles[uid]
            PersonItem(f.id, uid, p?.name ?: "Unknown", p?.avatarUrl ?: "", p?.bio ?: "", p?.category ?: "")
        }
    }

    suspend fun following(userId: String): List<PersonItem> {
        val rows = api.decodeList<FollowRow>(
            api.get("follows", listOf("select" to "id,following_id", "follower_id" to "eq.$userId")),
        )
        return people(rows) { it.followingId }
    }

    suspend fun followers(userId: String): List<PersonItem> {
        val rows = api.decodeList<FollowRow>(
            api.get("follows", listOf("select" to "id,follower_id", "following_id" to "eq.$userId")),
        )
        return people(rows) { it.followerId }
    }

    suspend fun myComments(userId: String): List<CommentWithBook> {
        val rows = api.decodeList<CommentRow>(
            api.get("comments", listOf("select" to "id,content,created_at,book_id", "user_id" to "eq.$userId", "order" to "created_at.desc")),
        )
        if (rows.isEmpty()) return emptyList()
        val books = api.decodeList<Book>(
            api.get("books", listOf("select" to "id,title", "id" to inList(rows.map { it.bookId }.distinct()))),
        ).associateBy { it.id }
        return rows.map { CommentWithBook(it, books[it.bookId]?.title ?: "Unknown") }
    }

    suspend fun readHistory(userId: String): List<ReadItem> {
        val rows = api.decodeList<ReadRow>(
            api.get("read_history", listOf("select" to "id,book_id,created_at", "user_id" to "eq.$userId", "order" to "created_at.desc")),
        )
        if (rows.isEmpty()) return emptyList()
        val books = api.decodeList<Book>(
            api.get("books", listOf("select" to "id,title,cover_url,genre", "id" to inList(rows.map { it.bookId }.distinct()))),
        ).associateBy { it.id }
        return rows.map {
            val b = books[it.bookId]
            ReadItem(it, b?.title ?: "Unknown", b?.coverUrl ?: "", b?.genre ?: "")
        }
    }

    suspend fun deleteAccountData() {
        api.invokeFunction("booknest-delete-account")
    }

    // ------------------------------------------------------------ editor
    suspend fun editorBook(bookId: String): Book? =
        api.decodeList<Book>(api.get("books", listOf("select" to "*", "id" to "eq.$bookId"))).firstOrNull()

    suspend fun rawChapters(bookId: String): List<Chapter> =
        api.decodeList(api.get("chapters", listOf("select" to "*", "book_id" to "eq.$bookId", "order" to "order.asc")))

    suspend fun createBook(userId: String, title: String, description: String, genre: String): String {
        val text = api.insert(
            "books",
            buildJsonObject {
                put("title", title)
                put("description", description)
                put("genre", genre)
                put("user_id", userId)
            }.toString(),
            returning = true,
        )
        return api.decodeList<Book>(text).first().id
    }

    suspend fun updateBook(id: String, title: String, description: String, genre: String) {
        api.update(
            "books", listOf("id" to "eq.$id"),
            buildJsonObject { put("title", title); put("description", description); put("genre", genre) }.toString(),
        )
    }

    suspend fun setPublished(id: String, published: Boolean) {
        api.update("books", listOf("id" to "eq.$id"), buildJsonObject { put("is_published", published) }.toString())
    }

    /** Atomic replace (DB function replace_chapters). */
    suspend fun replaceChapters(bookId: String, chapters: List<ChapterPayload>) {
        val arr: JsonArray = buildJsonArray {
            chapters.forEach { c ->
                add(
                    buildJsonObject {
                        put("title", c.title)
                        put("content", c.content)
                        put("is_encrypted", c.isEncrypted)
                    },
                )
            }
        }
        val body: JsonObject = buildJsonObject {
            put("p_book_id", bookId)
            put("p_chapters", arr)
        }
        api.rpc("replace_chapters", body.toString())
    }

    // ------------------------------------------------------------ vault (E2EE keys)
    suspend fun vaultRecord(userId: String): VaultRecord? =
        api.decodeList<VaultRecord>(
            api.get(
                "user_keys",
                listOf(
                    "select" to "kdf_salt,kdf_iterations,wrapped_dek,wrap_iv,recovery_salt,recovery_wrapped_dek,recovery_iv",
                    "user_id" to "eq.$userId",
                ),
            ),
        ).firstOrNull()

    suspend fun insertVault(userId: String, r: VaultRecord) {
        api.insert("user_keys", vaultBody(userId, r, includeUser = true))
    }

    suspend fun updateVaultPassphrase(userId: String, r: VaultRecord) {
        api.update(
            "user_keys", listOf("user_id" to "eq.$userId"),
            buildJsonObject {
                put("kdf_salt", r.kdfSalt)
                put("kdf_iterations", r.kdfIterations)
                put("wrapped_dek", r.wrappedDek)
                put("wrap_iv", r.wrapIv)
            }.toString(),
        )
    }

    private fun vaultBody(userId: String, r: VaultRecord, includeUser: Boolean): String = buildJsonObject {
        if (includeUser) put("user_id", userId)
        put("kdf_salt", r.kdfSalt)
        put("kdf_iterations", r.kdfIterations)
        put("wrapped_dek", r.wrappedDek)
        put("wrap_iv", r.wrapIv)
        put("recovery_salt", r.recoverySalt)
        put("recovery_wrapped_dek", r.recoveryWrappedDek)
        put("recovery_iv", r.recoveryIv)
    }.toString()
}
