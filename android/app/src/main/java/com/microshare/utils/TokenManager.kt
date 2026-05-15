package com.microshare.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 登录态管理工具
 */
object TokenManager {
    private const val PREF_NAME = "microshare_prefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_TOKEN = "token"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveLogin(userId: Int, token: String, nickname: String, avatar: String) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_TOKEN, token)
            .putString(KEY_NICKNAME, nickname)
            .putString(KEY_AVATAR, avatar)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .commit()
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getToken(): String = prefs.getString(KEY_TOKEN, "") ?: ""
    fun getNickname(): String = prefs.getString(KEY_NICKNAME, "") ?: ""
    fun getAvatar(): String = prefs.getString(KEY_AVATAR, "") ?: ""
}
