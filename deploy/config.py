"""
微分享社区 - 生产环境配置文件（阿里云服务器）
"""
import os

# ========== MySQL 数据库配置 ==========
MYSQL_CONFIG = {
    "host": os.environ.get("DB_HOST", "127.0.0.1"),
    "port": int(os.environ.get("DB_PORT", 3306)),
    "user": os.environ.get("DB_USER", "microshare"),
    "password": os.environ.get("DB_PASSWORD", "MicroShare@2026"),
    "database": os.environ.get("DB_NAME", "microshare_db"),
    "charset": "utf8mb4",
}

# ========== Flask 配置 ==========
FLASK_CONFIG = {
    "host": "127.0.0.1",  # Gunicorn 监听本地，Nginx 反向代理
    "port": 5000,
    "debug": False,  # 生产环境关闭调试
}

# ========== Socket 服务器配置 ==========
SOCKET_CONFIG = {
    "host": "0.0.0.0",
    "port": 9000,
}

# ========== 上传文件配置 ==========
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), "uploads")
MAX_CONTENT_LENGTH = 10 * 1024 * 1024  # 10MB
ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "webp"}

# ========== JWT密钥（生产环境请修改！） ==========
SECRET_KEY = os.environ.get("SECRET_KEY", "microshare_production_secret_key_2026")
