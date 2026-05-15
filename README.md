# 微分享社区 (MicroShare)

> 基于 Android 的博客论坛类社交应用的设计与实现

微分享社区是一款融合社交互动与健康管理的移动应用，灵感源自小红书的设计理念。用户可以在平台上发布图文帖子、关注好友、点赞评论，并通过加速度传感器记录步数数据，参与运动排行榜。

---

## 🛠 技术栈

| 层 | 技术 | 用途 |
|-----|------|------|
| 客户端 | Android (Kotlin) + Jetpack Compose | 响应式 UI，14 个 Screen |
| 网络 | OkHttp + 原生 Socket | HTTP RESTful API + TCP 长连接推送 |
| 后端 | Python 3 + Flask | 24 个 RESTful API（Gunicorn 部署） |
| 数据库 | MySQL 8.0 (InnoDB) | 7 张业务表，utf8mb4 编码 |
| 传感器 | Android Sensor Framework | 加速度峰值检测计步算法 |
| 协程 | Kotlin Coroutines | 异步网络请求，避免主线程阻塞 |
| 部署 | systemd + Nginx | 阿里云 ECS CentOS/Ubuntu |

## 📱 功能模块

| 模块 | 功能 |
|------|------|
| 用户 | 注册、登录、资料编辑、关注/粉丝管理 |
| 内容 | 图文帖子发布（最多 9 图）、话题标签、分页帖子流 |
| 社交 | 点赞/取消、评论、收藏/取消、关注/取消 |
| 运动 | 加速度传感器计步、步数排行榜、历史统计 |
| 消息 | Socket 实时推送（点赞/评论/关注通知） |
| 搜索 | 帖子内容搜索、用户昵称搜索 |

## 🗄️ 数据库（7 张表）

- `users` — 用户账号、密码(SHA256)、昵称、头像
- `posts` — 帖子内容、图片 URL、话题标签、计数
- `comments` — 评论、关联父评论(支持回复)
- `follows` — 关注关系（唯一约束防重复）
- `likes` — 点赞记录（唯一约束防重复）
- `favorites` — 收藏记录
- `sport_data` — 步数/距离/卡路里（每日唯一）

## 📦 项目结构

```
F:\MicroShare\
├── android/                  # Android 客户端
│   └── app/src/main/
│       ├── java/com/microshare/
│       │   ├── config/       # 应用配置
│       │   ├── model/        # 数据模型
│       │   ├── network/      # OkHttp + Socket
│       │   ├── sensor/       # 加速度计步
│       │   ├── service/      # 前台计步服务
│       │   ├── ui/
│       │   │   ├── navigation/  # 路由
│       │   │   ├── screens/     # 14 个 Compose Screen
│       │   │   ├── theme/       # 颜色/主题
│       │   │   └── utils/       # Token 管理
│       │   └── res/          # 资源文件
│       ├── build.gradle
│       └── AndroidManifest.xml
│
├── backend/                  # Python 后端
│   ├── app.py                # Flask 主应用（24 API）
│   ├── socket_server.py      # Socket 推送服务器
│   ├── config.py             # 数据库配置
│   ├── db.py                 # 数据库操作封装
│   ├── init_db.py            # 建表脚本
│   ├── utils.py              # 工具函数
│   └── requirements.txt      # 依赖清单
│
├── deploy/                   # 部署配置
│   ├── microshare.service    # systemd 服务
│   ├── nginx.conf            # 反向代理
│   └── setup.sh              # 部署脚本
│
├── docs/                     # 项目文档
├── 部署指南.md               # 部署说明
└── README.md                 # 本文件
```

## 🚀 部署

见 [部署指南.md](部署指南.md)

### 快速启动

```bash
# 后端
cd backend
pip install -r requirements.txt
python init_db.py    # 首次建表
python app.py        # Flask API → 端口 5000
python socket_server.py  # Socket → 端口 9000

# Android
# 用 Android Studio 打开 android/ 目录
# 修改 AppConfig.kt 中的 BASE_URL 为你的服务器 IP
# Build → Build APK(s)
```

## 📄 考核技术覆盖

- ✅ **网络通信** — HTTP (OkHttp) + Socket 长连接
- ✅ **数据库服务器** — MySQL 8.0
- ✅ **传感器（物联网）** — 加速度传感器计步
- ✅ **多协程** — Kotlin Coroutine

## 📃 文档清单

| 文件 | 说明 |
|------|------|
| `docs/期末考查项目任务书.docx` | 任务书（含分工/进度/技术路线） |
| `docs/设计规格文档.docx` | 设计规格 |
| `docs/项目设计文档.docx` | 项目设计详细文档 |
| `docs/系统设计项目书.docx` | 系统设计说明书 |
| `docs/成绩评定单_*.docx` | 成员评定表（3 份） |
| `docs/项目交付文档.md` | 交付文档 |
| `部署指南.md` | 部署构建指南 |
