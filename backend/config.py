"""
微分享社区 - 配置文件（MySQL专用）
"""
import os

# ========== MySQL 数据库配置 ==========
MYSQL_CONFIG = {
    "host": os.environ.get("DB_HOST", "127.0.0.1"),
    "port": int(os.environ.get("DB_PORT", 3306)),
    "user": os.environ.get("DB_USER", "root"),
    "password": os.environ.get("DB_PASSWORD", "123456"),
    "database": os.environ.get("DB_NAME", "microshare"),
    "charset": "utf8mb4",
}

# ========== Flask 配置 ==========
FLASK_CONFIG = {
    "host": "0.0.0.0",
    "port": 5000,
    "debug": True,
}

# ========== Socket 服务器配置 ==========
SOCKET_CONFIG = {
    "host": "0.0.0.0",
    "port": 9000,
}

# ========== 上传文件配置 ==========
SERVER_HOST = os.environ.get("SERVER_HOST", "http://8.137.186.43")
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), "uploads")
MAX_CONTENT_LENGTH = 10 * 1024 * 1024  # 10MB
ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "webp"}

# ========== JWT密钥 ==========
SECRET_KEY = "microshare_secret_key_2026"
