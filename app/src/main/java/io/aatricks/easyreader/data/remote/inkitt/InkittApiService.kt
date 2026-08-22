package io.aatricks.easyreader.data.remote.inkitt

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Inkitt reverse-engineered mobile API.
 * Endpoints observed from ik-mini Rust crate and mobile app traffic.
 *
 * Base URL: https://www.inkitt.com/api/v1 (observed)
 * Auth: Bearer token via /auth/login
 */
interface InkittApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: InkittAuthRequest,
    ): Response<InkittAuthResponse>

    @GET("api/v1/stories/{story_id}")
    suspend fun getStory(
        @Path("story_id") storyId: Long,
    ): Response<InkittStory>

    @GET("api/v1/stories/{story_id}/chapters")
    suspend fun getChapters(
        @Path("story_id") storyId: Long,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50,
    ): Response<List<InkittChapter>>

    @GET("api/v1/chapters/{chapter_id}")
    suspend fun getChapter(
        @Path("chapter_id") chapterId: Long,
    ): Response<InkittChapterContent>

    @GET("api/v1/stories/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): Response<InkittSearchResponse>

    @GET("api/v1/stories/popular")
    suspend fun getPopular(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): Response<InkittSearchResponse>

    @GET("api/v1/stories/latest")
    suspend fun getLatest(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): Response<InkittSearchResponse>

    @GET("api/v1/users/{user_id}/stories")
    suspend fun getUserStories(
        @Path("user_id") userId: Long,
        @Query("page") page: Int = 1,
    ): Response<InkittSearchResponse>
}
