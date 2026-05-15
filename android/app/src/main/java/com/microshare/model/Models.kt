package com.microshare.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val bio: String = "",
    val phone: String = "",
    val following_count: Int = 0,
    val follower_count: Int = 0,
    val post_count: Int = 0,
    val created_at: String = ""
)

data class Post(
    val id: Int = 0,
    val user_id: Int = 0,
    val content: String = "",
    @SerializedName("images")
    private val _images: Any? = null,
    val topic: String = "",
    val like_count: Int = 0,
    val comment_count: Int = 0,
    val favorite_count: Int = 0,
    val nickname: String = "",
    val avatar: String = "",
    val created_at: String = ""
) {
    val images: List<String>
        get() = when (_images) {
            is List<*> -> _images.filterIsInstance<String>()
            is String -> if ((_images as String).isNotEmpty()) listOf(_images as String) else emptyList()
            else -> emptyList()
        }
}

data class Comment(
    val id: Int = 0,
    val post_id: Int = 0,
    val user_id: Int = 0,
    val parent_id: Int = 0,
    val content: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val created_at: String = ""
)

data class SportData(
    val id: Int = 0,
    val user_id: Int = 0,
    val steps: Int = 0,
    val distance: Float = 0f,
    val calories: Float = 0f,
    val record_date: String = "",
    val nickname: String = "",
    val avatar: String = ""
)

data class Notification(
    val type: String = "",
    val msg: String = "",
    val user_id: Int = 0,
    val post_id: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApiResponse<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null
)
