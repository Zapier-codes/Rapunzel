package io.aatricks.easyreader.data.remote.wattpad

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WattpadStory(
    val id: Long,
    val title: String,
    val description: String? = null,
    val cover: String? = null,
    val author: WattpadUser? = null,
    val parts: List<WattpadPart>? = null,
    val tags: List<String>? = null,
    val language: WattpadLanguage? = null,
    val mature: Boolean = false,
    val completed: Boolean = false,
    val numParts: Int = 0,
    val readCount: Long = 0,
    val voteCount: Long = 0,
    val commentCount: Long = 0,
    val createDate: String? = null,
    val modifyDate: String? = null,
)

@Serializable
data class WattpadUser(
    val username: String,
    val name: String? = null,
    val avatar: String? = null,
    val description: String? = null,
)

@Serializable
data class WattpadPart(
    val id: Long,
    val title: String? = null,
    val url: String? = null,
    val text_url: String? = null,
    val commentCount: Long = 0,
    val voteCount: Long = 0,
    val readCount: Long = 0,
    val createDate: String? = null,
    val modifyDate: String? = null,
    val draft: Boolean = false,
)

@Serializable
data class WattpadPartText(
    val text: String,
    val pages: List<WattpadPage>? = null,
)

@Serializable
data class WattpadPage(
    val page: Int,
    val text: String,
)

@Serializable
data class WattpadLanguage(
    val id: String,
    val name: String,
)

@Serializable
data class WattpadSearchResult(
    val stories: List<WattpadStory>? = null,
    val total: Int = 0,
)

@Serializable
data class WattpadAuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
data class WattpadAuthRequest(
    val username: String,
    val password: String,
)
