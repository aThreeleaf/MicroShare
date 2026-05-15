"""
微分享社区 - Socket通知服务器
当点赞/收藏/评论/关注时向目标用户推送实时通知
"""
import socket
import threading
import json
import logging

logging.basicConfig(level=logging.INFO, format='[SocketServer] %(message)s')
logger = logging.getLogger(__name__)

# 在线用户连接池 {user_id: socket}
online_clients: dict = {}
clients_lock = threading.Lock()


def notify_user(user_id: int, notification_type: str, message: str, from_user_id: int = 0):
    """推送通知给指定在线用户"""
    data = json.dumps({
        "type": notification_type,
        "msg": message,
        "user_id": from_user_id
    })
    with clients_lock:
        sock = online_clients.get(user_id)
    if sock:
        try:
            sock.sendall((data + "\n").encode("utf-8"))
            logger.info(f"推送通知给用户{user_id}: {message}")
        except Exception as e:
            logger.error(f"推送失败: {e}")
            with clients_lock:
                online_clients.pop(user_id, None)
    else:
        logger.info(f"用户{user_id}不在线，跳过推送")


def handle_client(conn: socket.socket, addr):
    """处理单个客户端连接"""
    user_id = None
    conn.settimeout(120)
    try:
        while True:
            data = conn.recv(4096)
            if not data:
                break
            for line in data.decode("utf-8").split("\n"):
                line = line.strip()
                if not line:
                    continue
                try:
                    msg = json.loads(line)
                    msg_type = msg.get("type", "")
                    uid = msg.get("user_id", 0)
                    if msg_type == "register" and uid:
                        user_id = uid
                        with clients_lock:
                            online_clients[uid] = conn
                        logger.info(f"用户{uid}已注册在线 ({addr[0]})")
                    elif msg_type == "heartbeat":
                        pass  # 心跳维持连接
                    elif msg_type == "relay":
                        # 中继转发：Flask后端 → Socket服务器 → 目标用户
                        target_uid = msg.get("target_user_id", 0)
                        notif_type = msg.get("notif_type", "")
                        notif_msg = msg.get("notif_msg", "")
                        from_uid = msg.get("from_user_id", 0)
                        post_id = msg.get("post_id", 0)
                        data = json.dumps({
                            "type": notif_type,
                            "msg": notif_msg,
                            "user_id": from_uid,
                            "post_id": post_id
                        })
                        with clients_lock:
                            target_sock = online_clients.get(target_uid)
                        if target_sock:
                            try:
                                target_sock.sendall((data + "\n").encode("utf-8"))
                                logger.info(f"中继通知→用户{target_uid}: {notif_msg}")
                            except Exception as e:
                                logger.error(f"中继失败: {e}")
                                with clients_lock:
                                    online_clients.pop(target_uid, None)
                except json.JSONDecodeError:
                    pass
    except (socket.timeout, ConnectionError, OSError):
        pass
    finally:
        if user_id:
            with clients_lock:
                online_clients.pop(user_id, None)
            logger.info(f"用户{user_id}已离线")
        try:
            conn.close()
        except Exception:
            pass


def start_socket_server(host="0.0.0.0", port=9000):
    """启动Socket服务器（阻塞，在独立线程中运行）"""
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((host, port))
    server.listen(50)
    logger.info(f"Socket服务器启动在 {host}:{port}")

    try:
        while True:
            conn, addr = server.accept()
            t = threading.Thread(target=handle_client, args=(conn, addr), daemon=True)
            t.start()
    except KeyboardInterrupt:
        pass
    finally:
        server.close()


def run_socket_server():
    """在独立线程中启动Socket服务器"""
    t = threading.Thread(target=start_socket_server, daemon=True)
    t.start()
    return t


if __name__ == "__main__":
    # 直接启动 Socket 服务器（阻塞模式）
    start_socket_server(host="0.0.0.0", port=9000)


def relay_notification(target_user_id: int, notif_type: str, message: str, from_user_id: int = 0, post_id: int = 0):
    """
    中继通知（Flask后端调用此函数，通过TCP中继到Socket服务器）
    因为Socket服务器和Flask在不同进程中，需要跨进程通信
    """
    import logging
    relay_logger = logging.getLogger("RelayNotifier")
    try:
        relay_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        relay_sock.settimeout(3)
        relay_sock.connect(("127.0.0.1", 9000))
        data = json.dumps({
            "type": "relay",
            "target_user_id": target_user_id,
            "notif_type": notif_type,
            "notif_msg": message,
            "from_user_id": from_user_id,
            "post_id": post_id
        }) + "\n"
        relay_sock.sendall(data.encode("utf-8"))
        relay_sock.close()
    except Exception as e:
        relay_logger.error(f"中继通知失败: {e}")
