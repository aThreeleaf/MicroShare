package com.microshare.ui.navigation

/** 所有页面路由 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN = "main"
    const val POST_DETAIL = "post_detail/{postId}"
    const val POST_EDIT = "post_edit"
    const val USER_PROFILE = "user_profile/{userId}"
    const val FOLLOW_LIST = "follow_list/{userId}/{tab}"
    const val MESSAGE = "message"
    const val SEARCH = "search"
    const val SPORT = "sport"

    fun postDetail(postId: Int) = "post_detail/$postId"
    fun userProfile(userId: Int) = "user_profile/$userId"
    fun followList(userId: Int, tab: String) = "follow_list/$userId/$tab"
}
