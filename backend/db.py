"""
微分享社区 - 数据库连接模块（MySQL专用）
"""
import pymysql
from config import MYSQL_CONFIG


def _get_connection():
    """获取MySQL数据库连接"""
    return pymysql.connect(
        host=MYSQL_CONFIG["host"],
        port=MYSQL_CONFIG["port"],
        user=MYSQL_CONFIG["user"],
        password=MYSQL_CONFIG["password"],
        database=MYSQL_CONFIG["database"],
        charset=MYSQL_CONFIG["charset"],
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=False,
    )


def query(sql, params=None, fetch_one=False):
    """执行查询SQL，返回字典或字典列表"""
    conn = _get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(sql, params)
        result = cursor.fetchone() if fetch_one else cursor.fetchall()
        cursor.close()
        return result
    finally:
        conn.close()


def execute(sql, params=None):
    """执行增删改SQL，返回受影响行数"""
    conn = _get_connection()
    try:
        cursor = conn.cursor()
        rows = cursor.execute(sql, params)
        conn.commit()
        cursor.close()
        return rows
    except Exception as e:
        conn.rollback()
        raise e
    finally:
        conn.close()


def execute_insert(sql, params=None):
    """执行插入SQL，返回自增ID"""
    conn = _get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(sql, params)
        conn.commit()
        last_id = cursor.lastrowid
        cursor.close()
        return last_id
    except Exception as e:
        conn.rollback()
        raise e
    finally:
        conn.close()
