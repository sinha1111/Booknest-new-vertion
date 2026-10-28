package com.tricosoft.booknest.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("user_id") val userId: String = "",
    val name: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = "",
    val bio: String? = "",
    val category: String? = "",
    val followers: Int = 0,
    val following: Int = 0,
)

@Serializable
data class Book(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val genre: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("is_published") val isPublished: Boolean = false,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("reads_count") val readsCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("user_id") val userId: String = "",
)

/** Book + author info (web er DbBook er moto). */
data class BookUi(
    val book: Book,
    val authorName: String,
    val authorAvatar: String,
)

@Serializable
data class Chapter(
    val id: String,
    @SerialName("book_id") val bookId: String = "",
    val title: String = "",
    val content: String = "",
    val order: Int = 0,
    @SerialName("is_encrypted") val isEncrypted: Boolean = false,
)

@Serializable
data class IdRow(val id: String)

@Serializable
data class FollowRow(
    val id: String,
    @SerialName("follower_id") val followerId: String = "",
    @SerialName("following_id") val followingId: String = "",
)

@Serializable
data class LikeRow(@SerialName("book_id") val bookId: String)

@Serializable
data class CommentRow(
    val id: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("book_id") val bookId: String = "",
    val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class ReadRow(
    val id: String,
    @SerialName("book_id") val bookId: String,
    @SerialName("created_at") val createdAt: String,
)

/** Server e rakha encrypted key material (shudhu wrapped key; passphrase kokhono jay na). */
@Serializable
data class VaultRecord(
    @SerialName("kdf_salt") val kdfSalt: String,
    @SerialName("kdf_iterations") val kdfIterations: Int,
    @SerialName("wrapped_dek") val wrappedDek: String,
    @SerialName("wrap_iv") val wrapIv: String,
    @SerialName("recovery_salt") val recoverySalt: String,
    @SerialName("recovery_wrapped_dek") val recoveryWrappedDek: String,
    @SerialName("recovery_iv") val recoveryIv: String,
)

// ---- UI-level models (Profile page) ----
data class CommentWithBook(val comment: CommentRow, val bookTitle: String)
data class ReadItem(val row: ReadRow, val title: String, val cover: String, val genre: String)
data class PersonItem(
    val followId: String,
    val userId: String,
    val name: String,
    val avatarUrl: String,
    val bio: String,
    val category: String,
)
data class CommentWithProfile(val comment: CommentRow, val name: String, val avatarUrl: String)
data class BookDetailData(val book: BookUi, val chapters: List<Chapter>)
data class ChapterPayload(val title: String, val content: String, val isEncrypted: Boolean)
