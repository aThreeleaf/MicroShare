# 微分享社区后端 - 阿里云部署指南

## 📋 部署前准备

1. 阿里云服务器（Ubuntu 20.04/22.04）
2. 服务器公网 IP 地址
3. 阿里云 Workbench 或 SSH 客户端

## 🚀 部署步骤

### 第一步：连接服务器并执行环境安装

1. 登录阿里云控制台 → 进入你的 ECS 实例
2. 点击「远程连接」→ 选择「Workbench 远程连接」
3. 连接成功后，执行以下命令：

```bash
# 下载并执行安装脚本
curl -fsSL https://raw.githubusercontent.com/your-repo/setup.sh | bash

# 或者手动执行以下命令：
sudo apt update && sudo apt upgrade -y
sudo apt install -y python3.10 python3.10-venv python3-pip mysql-server nginx
sudo systemctl start mysql && sudo systemctl enable mysql

# 创建数据库
sudo mysql -e "CREATE DATABASE IF NOT EXISTS microshare_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'microshare'@'localhost' IDENTIFIED BY 'MicroShare@2026';"
sudo mysql -e "GRANT ALL PRIVILEGES ON microshare_db.* TO 'microshare'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"

# 创建应用目录
sudo mkdir -p /var/www/microshare
sudo chown -R $USER:$USER /var/www/microshare
```

### 第二步：上传后端代码

**方法一：使用 scp 命令（本地终端）**

```bash
# 在本地电脑执行，将 backend 目录上传到服务器
scp -r F:/MicroShare/backend/* root@你的服务器IP:/var/www/microshare/
```

**方法二：使用阿里云 Workbench 的文件传输**

1. 在 Workbench 界面找到「文件」或「文件传输」功能
2. 将本地 `backend` 文件夹内的所有文件上传到 `/var/www/microshare/`

**方法三：使用 Git（如果代码已推送到 GitHub）**

```bash
cd /var/www/microshare
git clone https://github.com/your-username/microshare.git .
```

### 第三步：配置生产环境

```bash
cd /var/www/microshare

# 1. 替换配置文件
cp deploy/config.py config.py

# 2. 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 3. 安装依赖
pip install -r deploy/requirements.txt

# 4. 创建上传目录
mkdir -p uploads
chmod 755 uploads

# 5. 初始化数据库
python3 -c "from init_db import init_database; init_database()"
```

### 第四步：配置 Systemd 服务

```bash
# 复制服务配置文件
sudo cp deploy/microshare.service /etc/systemd/system/

# 重新加载 systemd
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start microshare
sudo systemctl enable microshare

# 查看状态
sudo systemctl status microshare
```

### 第五步：配置 Nginx

```bash
# 复制 Nginx 配置
sudo cp deploy/nginx.conf /etc/nginx/sites-available/microshare

# 启用站点
sudo ln -sf /etc/nginx/sites-available/microshare /etc/nginx/sites-enabled/

# 删除默认站点（可选）
sudo rm -f /etc/nginx/sites-enabled/default

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

### 第六步：开放阿里云安全组端口

1. 登录阿里云控制台 → 云服务器 ECS → 安全组
2. 找到你的实例所属安全组，点击「配置规则」
3. 添加以下入方向规则：

| 协议 | 端口范围 | 授权对象 | 说明 |
|------|----------|----------|------|
| TCP | 80 | 0.0.0.0/0 | HTTP 访问 |
| TCP | 443 | 0.0.0.0/0 | HTTPS（可选） |
| TCP | 9000 | 0.0.0.0/0 | Socket 服务器（可选） |

## ✅ 验证部署

```bash
# 测试 API 是否正常运行
curl http://你的服务器IP/

# 预期返回：
# {"status": "ok", "message": "微分享社区后端服务运行中"}
```

## 🔧 常用命令

```bash
# 查看服务日志
sudo journalctl -u microshare -f

# 重启服务
sudo systemctl restart microshare

# 停止服务
sudo systemctl stop microshare

# 查看 Nginx 日志
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

## 📱 更新 Android 配置

部署成功后，修改 Android 项目的 `AppConfig.kt`：

```kotlin
object AppConfig {
    const val BASE_URL = "http://你的服务器公网IP"
    const val SOCKET_HOST = "你的服务器公网IP"
    const val SOCKET_PORT = 9000
    // ...
}
```

## 🔒 安全建议（可选）

1. **配置 HTTPS**：使用阿里云 SSL 证书或 Let's Encrypt
2. **修改默认密码**：更改 MySQL 密码和 SECRET_KEY
3. **配置防火墙**：只开放必要端口
4. **定期备份**：设置数据库自动备份

## ❓ 常见问题

**Q: 服务启动失败？**
```bash
# 检查日志
sudo journalctl -u microshare -n 50

# 检查端口占用
sudo netstat -tlnp | grep 5000
```

**Q: 数据库连接失败？**
```bash
# 测试 MySQL 连接
mysql -u microshare -p -e "SHOW DATABASES;"
```

**Q: Nginx 502 错误？**
```bash
# 检查 Gunicorn 是否运行
sudo systemctl status microshare

# 检查端口监听
sudo netstat -tlnp | grep 5000
```
