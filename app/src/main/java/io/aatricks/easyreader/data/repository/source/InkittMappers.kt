package io.aatricks.easyreader.data.repository.source

import io.aatricks.easyreader.data.model.ExploreItem
import io.aatricks.easyreader.data.remote.inkitt.InkittStory

internal fun InkittStory.toExploreItem(): ExploreItem {
    return ExploreItem(
        id = id.toString(),
        title = title,
        url = "https://www.inkitt.com/story/$id",
        coverUrl = cover,
        author = author?.displayName ?: author?.username ?: "Unknown",
        description = description ?: "",
        status = if (completed) "Completed" else "Ongoing",
        genres = tags ?: emptyList(),
        rating = null,
        totalChapters = chapterCount,
        sourceName = name,
    )
}
