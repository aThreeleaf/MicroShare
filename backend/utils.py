"""
微分享社区 - 工具函数
"""
import hashlib
from config import SECRET_KEY


def hash_password(password):
    """SHA256密码加密"""
    return hashlib.sha256((password + SECRET_KEY).encode()).hexdigest()


def generate_token(user_id):
    """生成登录令牌（简化版）"""
    raw = f"{user_id}_{SECRET_KEY}"
    return hashlib.md5(raw.encode()).hexdigest()


def verify_token(token, user_id):
    """验证令牌"""
    return token == generate_token(user_id)
