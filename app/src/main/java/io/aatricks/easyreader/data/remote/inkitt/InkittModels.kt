package io.aatricks.easyreader.data.remote.inkitt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InkittStory(
    val id: Long,
    val title: String,
    val description: String? = null,
    val cover: String? = null,
    val author: InkittAuthor? = null,
    val chapters: List<InkittChapter>? = null,
    val tags: List<String>? = null,
    val language: String? = null,
    val mature: Boolean = false,
    val completed: Boolean = false,
    val chapterCount: Int = 0,
    val reads: Long = 0,
    val likes: Long = 0,
    val comments: Long = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class InkittAuthor(
    val id: Long,
    val username: String,
    @SerialName("display_name") val displayName: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
)

@Serializable
data class InkittChapter(
    val id: Long,
    val title: String? = null,
    val number: Int = 0,
    val url: String? = null,
    @SerialName("content_url") val contentUrl: String? = null,
    val reads: Long = 0,
    val likes: Long = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class InkittChapterContent(
    val id: Long,
    val title: String? = null,
    val content: String,
    val pages: List<InkittPage>? = null,
)

@Serializable
data class InkittPage(
    val number: Int,
    val content: String,
)

@Serializable
data class InkittSearchResponse(
    val stories: List<InkittStory>? = null,
    val total: Int = 0,
    val page: Int = 1,
)

@Serializable
data class InkittAuthRequest(
    val email: String,
    val password: String,
)

@Serializable
data class InkittAuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: InkittAuthor? = null,
)
