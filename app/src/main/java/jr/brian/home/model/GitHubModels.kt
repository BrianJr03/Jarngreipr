package jr.brian.home.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepoResult(
    @SerialName("full_name") val fullName: String,
    @SerialName("description") val description: String? = null,
    @SerialName("stargazers_count") val stars: Int = 0
)

@Serializable
data class GitHubSearchResponse(
    @SerialName("items") val items: List<GitHubRepoResult> = emptyList()
)

@Serializable
data class GitHubContentEntry(
    val name: String,
    val type: String,
    val size: Long = 0
)
