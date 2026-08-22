package io.aatricks.easyreader.data.remote.wattpad

import retrofit2.Response
import retrofit2.http.*

/**
 * Wattpad undocumented REST API (v3/apiv2).
 * Reference: https://github.com/Archive-WP/WattpadAPIDocumentation
 */
interface WattpadApiService {

    @GET("api/v3/stories/{story_id}")
    suspend fun getStory(
        @Path("story_id") storyId: Long,
        @Query("fields") fields: String = "id,title,description,cover,author,parts,tags,language,mature,completed,numParts,readCount,voteCount,commentCount,createDate,modifyDate"
    ): Response<WattpadStory>

    @GET("api/v3/parts/{part_id}")
    suspend fun getPart(
        @Path("part_id") partId: Long,
    ): Response<WattpadPart>

    @GET("v4/parts/{part_id}/text")
    suspend fun getPartText(
        @Path("part_id") partId: Long,
        @Query("page") page: Int? = null,
    ): Response<WattpadPartText>

    @GET("api/v3/stories")
    suspend fun searchStories(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = "id,title,description,cover,author,tags,language,mature,completed,numParts,readCount,voteCount,commentCount",
    ): Response<WattpadSearchResult>

    @GET("api/v3/users/{username}")
    suspend fun getUser(
        @Path("username") username: String,
    ): Response<WattpadUser>

    @GET("api/v3/users/{username}/stories")
    suspend fun getUserStories(
        @Path("username") username: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<WattpadSearchResult>

    @POST("v4/auth/login")
    suspend fun login(
        @Body request: WattpadAuthRequest,
    ): Response<WattpadAuthResponse>
}
