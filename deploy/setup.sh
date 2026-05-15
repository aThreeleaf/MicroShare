#!/bin/bash
# ============================================
# 微分享社区后端 - 阿里云服务器部署脚本 (Ubuntu)
# ============================================

set -e

echo "🚀 开始部署微分享社区后端..."

# 1. 更新系统
echo "📦 更新系统包..."
sudo apt update && sudo apt upgrade -y

# 2. 安装 Python 3.10 和 pip
echo "🐍 安装 Python..."
sudo apt install -y python3.10 python3.10-venv python3-pip

# 3. 安装 MySQL
echo "🗄️ 安装 MySQL..."
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# 4. 安装 Nginx
echo "🌐 安装 Nginx..."
sudo apt install -y nginx

# 5. 创建应用目录
echo "📁 创建应用目录..."
sudo mkdir -p /var/www/microshare
sudo chown -R $USER:$USER /var/www/microshare

# 6. 配置 MySQL
echo "⚙️ 配置 MySQL..."
sudo mysql -e "CREATE DATABASE IF NOT EXISTS microshare_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'microshare'@'localhost' IDENTIFIED BY 'MicroShare@2026';"
sudo mysql -e "GRANT ALL PRIVILEGES ON microshare_db.* TO 'microshare'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "✅ 环境准备完成！"
echo ""
echo "下一步：上传后端代码到 /var/www/microshare"
echo "MySQL 配置："
echo "  - 数据库: microshare_db"
echo "  - 用户名: microshare"
echo "  - 密码: MicroShare@2026"
