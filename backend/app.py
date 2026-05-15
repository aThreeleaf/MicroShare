"""
微分享社区 - Flask 主应用入口
"""
from flask import Flask
from flask_cors import CORS
from config import FLASK_CONFIG, UPLOAD_FOLDER
import os

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 10 * 1024 * 1024  # 10MB
CORS(app)

# 确保上传目录存在
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# 自动初始化数据库
from init_db import init_database
init_database()

# ==================== 路由定义 ====================

@app.route("/")
def index():
    return {"status": "ok", "message": "微分享社区后端服务运行中"}


# ==================== 用户模块 ====================

@app.route("/api/register", methods=["POST"])
def register():
    from flask import request
    from db import query, execute_insert
    from utils import hash_password, generate_token
    data = request.get_json()
    username = data.get("username", "").strip()
    password = data.get("password", "").strip()
    nickname = data.get("nickname", username)

    if not username or not password:
        return {"code": 400, "msg": "用户名和密码不能为空"}, 400

    exist = query("SELECT id FROM users WHERE username=%s", (username,), fetch_one=True)
    if exist:
        return {"code": 400, "msg": "用户名已存在"}, 400

    hashed = hash_password(password)
    uid = execute_insert(
        "INSERT INTO users (username, password, nickname) VALUES (%s, %s, %s)",
        (username, hashed, nickname)
    )
    token = generate_token(uid)
    return {"code": 200, "msg": "注册成功", "data": {"user_id": uid, "token": token, "nickname": nickname}}


@app.route("/api/login", methods=["POST"])
def login():
    from flask import request
    from db import query
    from utils import hash_password, generate_token
    data = request.get_json()
    username = data.get("username", "").strip()
    password = data.get("password", "").strip()

    user = query("SELECT id, nickname, avatar, bio FROM users WHERE username=%s AND password=%s",
                 (username, hash_password(password)), fetch_one=True)
    if not user:
        return {"code": 401, "msg": "用户名或密码错误"}, 401

    token = generate_token(user["id"])
    return {"code": 200, "msg": "登录成功", "data": {
        "user_id": user["id"], "token": token,
        "nickname": user["nickname"], "avatar": user["avatar"], "bio": user["bio"]
    }}


@app.route("/api/user/<int:uid>", methods=["GET"])
def get_user(uid):
    from flask import request
    from db import query
    user = query("SELECT id, username, nickname, avatar, bio, phone, created_at FROM users WHERE id=%s",
                 (uid,), fetch_one=True)
    if not user:
        return {"code": 404, "msg": "用户不存在"}, 404

    # 统计关注、粉丝数和帖子数
    stats = query("""
        SELECT
            (SELECT COUNT(*) FROM follows WHERE follower_id=%s) AS following_count,
            (SELECT COUNT(*) FROM follows WHERE followed_id=%s) AS follower_count,
            (SELECT COUNT(*) FROM posts WHERE user_id=%s) AS post_count
    """, (uid, uid, uid), fetch_one=True)
    user["following_count"] = stats["following_count"]
    user["follower_count"] = stats["follower_count"]
    user["post_count"] = stats["post_count"]

    # 判断当前登录用户是否关注了该用户
    viewer_id = request.args.get("viewer_id", type=int)
    if viewer_id and viewer_id != uid:
        rel = query("SELECT id FROM follows WHERE follower_id=%s AND followed_id=%s",
                    (viewer_id, uid), fetch_one=True)
        user["is_following"] = rel is not None
    else:
        user["is_following"] = False
    return {"code": 200, "data": user}


@app.route("/api/user/<int:uid>", methods=["PUT"])
def update_user(uid):
    from flask import request
    from db import execute
    data = request.get_json()
    nickname = data.get("nickname")
    bio = data.get("bio")
    avatar = data.get("avatar")

    execute("UPDATE users SET nickname=%s, bio=%s, avatar=%s WHERE id=%s",
            (nickname, bio, avatar, uid))
    return {"code": 200, "msg": "更新成功"}


@app.route("/api/user/<int:uid>/followers", methods=["GET"])
def get_followers(uid):
    from db import query
    rows = query("""
        SELECT u.id, u.nickname, u.avatar FROM follows f
        JOIN users u ON f.follower_id = u.id
        WHERE f.followed_id = %s ORDER BY f.created_at DESC
    """, (uid,))
    return {"code": 200, "data": rows}


@app.route("/api/user/<int:uid>/following", methods=["GET"])
def get_following(uid):
    from db import query
    rows = query("""
        SELECT u.id, u.nickname, u.avatar FROM follows f
        JOIN users u ON f.followed_id = u.id
        WHERE f.follower_id = %s ORDER BY f.created_at DESC
    """, (uid,))
    return {"code": 200, "data": rows}


# ==================== 内容模块 ====================

@app.route("/api/post", methods=["POST"])
def create_post():
    from flask import request
    from db import execute_insert
    import json
    data = request.get_json()
    uid = data.get("user_id")
    content = data.get("content", "").strip()
    images = json.dumps(data.get("images", []))
    topic = data.get("topic", "").strip()

    if not content:
        return {"code": 400, "msg": "内容不能为空"}, 400

    pid = execute_insert(
        "INSERT INTO posts (user_id, content, images, topic) VALUES (%s, %s, %s, %s)",
        (uid, content, images, topic)
    )
    return {"code": 200, "msg": "发布成功", "data": {"post_id": pid}}


@app.route("/api/post/<int:pid>", methods=["GET"])
def get_post(pid):
    from flask import request
    from db import query, execute
    import json
    post = query("""
        SELECT p.*, u.nickname, u.avatar FROM posts p
        JOIN users u ON p.user_id = u.id WHERE p.id=%s
    """, (pid,), fetch_one=True)
    if not post:
        return {"code": 404, "msg": "帖子不存在"}, 404
    if post.get("images"):
        post["images"] = json.loads(post["images"]) if isinstance(post["images"], str) else post["images"]

    # 判断当前用户是否点赞/收藏
    viewer_id = request.args.get("viewer_id", type=int)
    if viewer_id:
        liked = query("SELECT id FROM likes WHERE user_id=%s AND post_id=%s", (viewer_id, pid), fetch_one=True)
        fav = query("SELECT id FROM favorites WHERE user_id=%s AND post_id=%s", (viewer_id, pid), fetch_one=True)
        post["is_liked"] = liked is not None
        post["is_favorited"] = fav is not None
    else:
        post["is_liked"] = False
        post["is_favorited"] = False
    return {"code": 200, "data": post}


@app.route("/api/post/<int:pid>", methods=["PUT"])
def update_post(pid):
    from flask import request
    from db import execute
    import json
    data = request.get_json()
    content = data.get("content", "")
    images = json.dumps(data.get("images", []))
    topic = data.get("topic", "")
    execute("UPDATE posts SET content=%s, images=%s, topic=%s WHERE id=%s",
            (content, images, topic, pid))
    return {"code": 200, "msg": "更新成功"}


@app.route("/api/post/<int:pid>", methods=["DELETE"])
def delete_post(pid):
    from db import execute
    execute("DELETE FROM comments WHERE post_id=%s", (pid,))
    execute("DELETE FROM likes WHERE post_id=%s", (pid,))
    execute("DELETE FROM favorites WHERE post_id=%s", (pid,))
    execute("DELETE FROM posts WHERE id=%s", (pid,))
    return {"code": 200, "msg": "删除成功"}


@app.route("/api/posts", methods=["GET"])
def get_posts():
    from flask import request
    from db import query
    import json
    page = int(request.args.get("page", 1))
    topic = request.args.get("topic", "")
    page_size = 10
    offset = (page - 1) * page_size

    if topic:
        sql = "SELECT p.*, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id WHERE p.topic=%s ORDER BY p.created_at DESC LIMIT %s OFFSET %s"
        rows = query(sql, (topic, page_size, offset))
    else:
        sql = "SELECT p.*, u.nickname, u.avatar FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.created_at DESC LIMIT %s OFFSET %s"
        rows = query(sql, (page_size, offset))

    for post in rows:
        if post.get("images"):
            post["images"] = json.loads(post["images"]) if isinstance(post["images"], str) else post["images"]

    return {"code": 200, "data": rows, "page": page}


@app.route("/api/user/<int:uid>/posts", methods=["GET"])
def get_user_posts(uid):
    from db import query
    import json
    rows = query("""
        SELECT p.*, u.nickname, u.avatar FROM posts p
        JOIN users u ON p.user_id = u.id WHERE p.user_id=%s
        ORDER BY p.created_at DESC LIMIT 20
    """, (uid,))
    for post in rows:
        if post.get("images"):
            post["images"] = json.loads(post["images"]) if isinstance(post["images"], str) else post["images"]
    return {"code": 200, "data": rows}


@app.route("/api/upload", methods=["POST"])
def upload_image():
    from flask import request
    import uuid
    import os
    file = request.files.get("file")
    if not file:
        return {"code": 400, "msg": "请选择文件"}, 400

    # 兼容处理文件名：优先从 Content-Type 推断扩展名
    original_name = file.filename or ""
    ext = ""
    if "." in original_name:
        ext = original_name.rsplit(".", 1)[-1].lower()
    if not ext or ext not in {"png", "jpg", "jpeg", "gif", "webp", "bmp", "svg"}:
        # 从 Content-Type 推断
        ct = file.content_type or ""
        mime_map = {"image/png": "png", "image/jpeg": "jpg", "image/gif": "gif",
                     "image/webp": "webp", "image/bmp": "bmp", "image/svg+xml": "svg"}
        ext = mime_map.get(ct, "jpg")

    filename = f"{uuid.uuid4().hex}.{ext}"
    filepath = os.path.join(UPLOAD_FOLDER, filename)
    file.save(filepath)
    from config import SERVER_HOST
    url = f"{SERVER_HOST}/uploads/{filename}"
    return {"code": 200, "data": {"url": url}}


@app.route("/uploads/<filename>")
def serve_upload(filename):
    """提供上传文件访问"""
    from config import UPLOAD_FOLDER
    from flask import send_from_directory
    return send_from_directory(UPLOAD_FOLDER, filename)


# ==================== 社交互动模块 ====================

@app.route("/api/like", methods=["POST"])
def toggle_like():
    from flask import request
    from db import query, execute, execute_insert
    data = request.get_json()
    uid = data.get("user_id")
    pid = data.get("post_id")

    exist = query("SELECT id FROM likes WHERE user_id=%s AND post_id=%s", (uid, pid), fetch_one=True)
    if exist:
        execute("DELETE FROM likes WHERE id=%s", (exist["id"],))
        execute("UPDATE posts SET like_count = GREATEST(like_count - 1, 0) WHERE id=%s", (pid,))
        return {"code": 200, "msg": "已取消点赞", "data": {"liked": False}}
    else:
        execute_insert("INSERT INTO likes (user_id, post_id) VALUES (%s, %s)", (uid, pid))
        execute("UPDATE posts SET like_count = like_count + 1 WHERE id=%s", (pid,))
        # 通知帖子发布者
        post_owner = query("SELECT user_id, content FROM posts WHERE id=%s", (pid,), fetch_one=True)
        if post_owner and post_owner["user_id"] != uid:
            liker = query("SELECT nickname FROM users WHERE id=%s", (uid,), fetch_one=True)
            name = liker["nickname"] if liker else "有人"
            from socket_server import relay_notification
            relay_notification(post_owner["user_id"], "new_like", f"{name} 赞了你的帖子", uid, post_id=pid)
        return {"code": 200, "msg": "点赞成功", "data": {"liked": True}}


@app.route("/api/favorite", methods=["POST"])
def toggle_favorite():
    from flask import request
    from db import query, execute, execute_insert
    data = request.get_json()
    uid = data.get("user_id")
    pid = data.get("post_id")

    exist = query("SELECT id FROM favorites WHERE user_id=%s AND post_id=%s", (uid, pid), fetch_one=True)
    if exist:
        execute("DELETE FROM favorites WHERE id=%s", (exist["id"],))
        execute("UPDATE posts SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE id=%s", (pid,))
        return {"code": 200, "msg": "已取消收藏", "data": {"favorited": False}}
    else:
        execute_insert("INSERT INTO favorites (user_id, post_id) VALUES (%s, %s)", (uid, pid))
        execute("UPDATE posts SET favorite_count = favorite_count + 1 WHERE id=%s", (pid,))
        return {"code": 200, "msg": "收藏成功", "data": {"favorited": True}}


@app.route("/api/user/<int:uid>/favorites", methods=["GET"])
def get_favorites(uid):
    from db import query
    import json
    rows = query("""
        SELECT p.*, u.nickname, u.avatar FROM favorites f
        JOIN posts p ON f.post_id = p.id
        JOIN users u ON p.user_id = u.id
        WHERE f.user_id = %s ORDER BY f.created_at DESC
    """, (uid,))
    for post in rows:
        if post.get("images"):
            post["images"] = json.loads(post["images"]) if isinstance(post["images"], str) else post["images"]
    return {"code": 200, "data": rows}


@app.route("/api/follow", methods=["POST"])
def toggle_follow():
    from flask import request
    from db import query, execute, execute_insert
    data = request.get_json()
    follower = data.get("follower_id")
    followed = data.get("followed_id")

    if follower == followed:
        return {"code": 400, "msg": "不能关注自己"}, 400

    exist = query("SELECT id FROM follows WHERE follower_id=%s AND followed_id=%s",
                  (follower, followed), fetch_one=True)
    if exist:
        execute("DELETE FROM follows WHERE id=%s", (exist["id"],))
        return {"code": 200, "msg": "已取消关注", "data": {"followed": False}}
    else:
        execute_insert("INSERT INTO follows (follower_id, followed_id) VALUES (%s, %s)",
                       (follower, followed))
        # 通知被关注用户
        follower_user = query("SELECT nickname FROM users WHERE id=%s", (follower,), fetch_one=True)
        name = follower_user["nickname"] if follower_user else "有人"
        from socket_server import relay_notification
        relay_notification(followed, "new_follower", f"{name} 关注了你", follower)
        return {"code": 200, "msg": "关注成功", "data": {"followed": True}}


@app.route("/api/comment", methods=["POST"])
def add_comment():
    from flask import request
    from db import execute_insert, execute, query
    data = request.get_json()
    uid = data.get("user_id")
    pid = data.get("post_id")
    content = data.get("content", "").strip()
    parent_id = data.get("parent_id", 0)

    if not content:
        return {"code": 400, "msg": "评论内容不能为空"}, 400

    cid = execute_insert(
        "INSERT INTO comments (post_id, user_id, parent_id, content) VALUES (%s, %s, %s, %s)",
        (pid, uid, parent_id, content)
    )
    execute("UPDATE posts SET comment_count = comment_count + 1 WHERE id=%s", (pid,))
    # 通知帖子发布者
    post_owner = query("SELECT user_id FROM posts WHERE id=%s", (pid,), fetch_one=True)
    if post_owner and post_owner["user_id"] != uid:
        commenter = query("SELECT nickname FROM users WHERE id=%s", (uid,), fetch_one=True)
        name = commenter["nickname"] if commenter else "有人"
        from socket_server import relay_notification
        relay_notification(post_owner["user_id"], "new_comment", f"{name} 评论了你的帖子", uid, post_id=pid)
    return {"code": 200, "msg": "评论成功", "data": {"comment_id": cid}}


@app.route("/api/post/<int:pid>/comments", methods=["GET"])
def get_comments(pid):
    from db import query
    rows = query("""
        SELECT c.*, u.nickname, u.avatar FROM comments c
        JOIN users u ON c.user_id = u.id
        WHERE c.post_id = %s
        ORDER BY c.parent_id ASC, c.created_at ASC
    """, (pid,))
    return {"code": 200, "data": rows}


# ==================== 运动数据模块 ====================

@app.route("/api/sport/upload", methods=["POST"])
def upload_sport():
    from flask import request
    from db import query, execute, execute_insert
    import datetime
    data = request.get_json()
    uid = data.get("user_id")
    steps = data.get("steps", 0)
    distance = data.get("distance", 0.0)
    calories = data.get("calories", 0.0)
    today = datetime.date.today().isoformat()

    exist = query("SELECT id FROM sport_data WHERE user_id=%s AND record_date=%s",
                  (uid, today), fetch_one=True)
    if exist:
        execute("UPDATE sport_data SET steps=%s, distance=%s, calories=%s WHERE id=%s",
                (steps, distance, calories, exist["id"]))
        return {"code": 200, "msg": "运动数据已更新"}
    else:
        execute_insert(
            "INSERT INTO sport_data (user_id, steps, distance, calories, record_date) VALUES (%s, %s, %s, %s, %s)",
            (uid, steps, distance, calories, today)
        )
        return {"code": 200, "msg": "运动数据上传成功"}


@app.route("/api/sport/<int:uid>", methods=["GET"])
def get_sport(uid):
    from flask import request
    from db import query
    date = request.args.get("date", "")
    if date:
        row = query("SELECT * FROM sport_data WHERE user_id=%s AND record_date=%s",
                    (uid, date), fetch_one=True)
        return {"code": 200, "data": row}
    rows = query("SELECT * FROM sport_data WHERE user_id=%s ORDER BY record_date DESC LIMIT 7", (uid,))
    return {"code": 200, "data": rows}


@app.route("/api/sport/rank", methods=["GET"])
def sport_rank():
    from flask import request
    from db import query
    import datetime
    date = request.args.get("date", datetime.date.today().isoformat())
    rows = query("""
        SELECT s.*, u.nickname, u.avatar FROM sport_data s
        JOIN users u ON s.user_id = u.id
        WHERE s.record_date = %s ORDER BY s.steps DESC
    """, (date,))
    return {"code": 200, "data": rows}


# ==================== 搜索模块 ====================

@app.route("/api/search", methods=["GET"])
def search_posts():
    from flask import request
    from db import query
    import json
    keyword = request.args.get("keyword", "").strip()
    if not keyword:
        return {"code": 200, "data": []}

    rows = query("""
        SELECT p.*, u.nickname, u.avatar FROM posts p
        JOIN users u ON p.user_id = u.id
        WHERE p.content LIKE %s OR p.topic LIKE %s
        ORDER BY p.created_at DESC LIMIT 20
    """, (f"%{keyword}%", f"%{keyword}%"))
    for post in rows:
        if post.get("images"):
            post["images"] = json.loads(post["images"]) if isinstance(post["images"], str) else post["images"]
    return {"code": 200, "data": rows}


@app.route("/api/search/user", methods=["GET"])
def search_users():
    from flask import request
    from db import query
    keyword = request.args.get("keyword", "").strip()
    if not keyword:
        return {"code": 200, "data": []}
    rows = query("""
        SELECT id, nickname, avatar, bio FROM users
        WHERE nickname LIKE %s OR username LIKE %s LIMIT 20
    """, (f"%{keyword}%", f"%{keyword}%"))
    return {"code": 200, "data": rows}


# ==================== 启动入口 ====================

if __name__ == "__main__":
    app.run(
        host=FLASK_CONFIG["host"],
        port=FLASK_CONFIG["port"],
        debug=FLASK_CONFIG["debug"]
    )
