package com.alessiomartini.dispensa.network

import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    val tag_name: String = "",
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    val browser_download_url: String = ""
)
