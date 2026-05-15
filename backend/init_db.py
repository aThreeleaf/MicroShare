"""
微分享社区 - 数据库初始化模块（MySQL）
"""
import pymysql
from config import MYSQL_CONFIG


def init_database():
    """初始化 MySQL 数据库表结构（7张表）"""
    conn = pymysql.connect(
        host=MYSQL_CONFIG["host"],
        port=MYSQL_CONFIG["port"],
        user=MYSQL_CONFIG["user"],
        password=MYSQL_CONFIG["password"],
        database=MYSQL_CONFIG["database"],
        charset=MYSQL_CONFIG["charset"],
    )

    sql_list = [
        # 1. 用户表
        """CREATE TABLE IF NOT EXISTS users (
            id INT PRIMARY KEY AUTO_INCREMENT,
            username VARCHAR(50) UNIQUE NOT NULL,
            password VARCHAR(255) NOT NULL,
            nickname VARCHAR(50),
            avatar VARCHAR(255),
            bio VARCHAR(200),
            phone VARCHAR(20),
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",

        # 2. 帖子表
        """CREATE TABLE IF NOT EXISTS posts (
            id INT PRIMARY KEY AUTO_INCREMENT,
            user_id INT NOT NULL,
            content TEXT NOT NULL,
            images TEXT,
            topic VARCHAR(50),
            like_count INT DEFAULT 0,
            comment_count INT DEFAULT 0,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",

        # 3. 评论表
        """CREATE TABLE IF NOT EXISTS comments (
            id INT PRIMARY KEY AUTO_INCREMENT,
            post_id INT NOT NULL,
            user_id INT NOT NULL,
            parent_id INT DEFAULT 0,
            content VARCHAR(500) NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES users(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",

        # 4. 关注表
        """CREATE TABLE IF NOT EXISTS follows (
            id INT PRIMARY KEY AUTO_INCREMENT,
            follower_id INT NOT NULL,
            followed_id INT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uq_follow (follower_id, followed_id),
            FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (followed_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",

        # 5. 点赞表
        """CREATE TABLE IF NOT EXISTS likes (
            id INT PRIMARY KEY AUTO_INCREMENT,
            user_id INT NOT NULL,
            post_id INT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uq_like (user_id, post_id),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",

        # 6. 收藏表
        """CREATE TABLE IF NOT EXISTS favorites (
            id INT PRIMARY KEY AUTO_INCREMENT,
            user_id INT NOT NULL,
            post_id INT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uq_favorite (user_id, post_id),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",

        # 7. 运动数据表
        """CREATE TABLE IF NOT EXISTS sport_data (
            id INT PRIMARY KEY AUTO_INCREMENT,
            user_id INT NOT NULL,
            steps INT DEFAULT 0,
            distance FLOAT DEFAULT 0,
            calories FLOAT DEFAULT 0,
            record_date DATE NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY uq_sport (user_id, record_date),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""",
    ]

    try:
        cursor = conn.cursor()
        for sql in sql_list:
            cursor.execute(sql)
        conn.commit()
        cursor.close()
        print(f"[DB] MySQL 初始化成功，共创建 {len(sql_list)} 张表")
        # 追加 favorite_count 列（兼容旧表）
        try:
            cursor = conn.cursor()
            cursor.execute("ALTER TABLE posts ADD COLUMN favorite_count INT DEFAULT 0")
            conn.commit()
            cursor.close()
        except Exception:
            pass  # 列已存在
    except Exception as e:
        conn.rollback()
        print(f"[DB] 数据库初始化失败: {e}")
        raise e
    finally:
        conn.close()


if __name__ == "__main__":
    init_database()
