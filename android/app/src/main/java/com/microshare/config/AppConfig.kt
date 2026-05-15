package com.microshare.config

/**
 * 应用全局配置常量
 */
object AppConfig {
    // 服务器地址（阿里云生产环境）
    const val BASE_URL = "http://8.137.186.43:5000"
    const val SOCKET_HOST = "8.137.186.43"
    const val SOCKET_PORT = 9000
    const val PAGE_SIZE = 10
    const val HEARTBEAT_INTERVAL = 30_000L // 30秒心跳间隔
}
