# 直销系统通用框架 (Direct Selling Platform)

基于 **Spring Boot 3.5 + NuxtJS 3** 的全栈直销系统，支持后台一键切换奖金制度，内置多种行业标准模式，适用于十万级会员规模。

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.5.x |
| JDK | Java | 17 LTS |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 消息队列 | RabbitMQ | 3.x |
| 权限认证 | Sa-Token | 1.40+ |
| API 文档 | Knife4j | 4.x |
| 前端框架 | NuxtJS 3 + Vue 3 | 3.12+ |
| UI 组件 | Element Plus | 2.7+ |
| 图表 | ECharts 5 | 5.5+ |
| 语言 | TypeScript | 5.x |

---

## 核心优势

- **奖金制度可切换**：后台一键切换级差制/双轨制/矩阵制/太阳线，策略模式实现，无需改代码或重启
- **十万级会员支撑**：路径枚举 + Redis 缓存 + 异步批量计算，专为大团队设计
- **前后端分离**：NuxtJS 3 SSR 前台商城 + Vue 3 SPA 后台管理，一套代码多端复用
- **模块化架构**：后端 8 个业务模块独立解耦，后期可平滑拆分为微服务
- **开箱即用**：Docker Compose 一键启动中间件，SQL 脚本自动初始化 17 张表 + 种子数据
- **完整业务闭环**：会员注册 → 商品购买 → 订单分润 → 奖金结算 → 钱包提现，全链路覆盖

---

## 功能模块

```
┌──────────────────────────────────────────────────────┐
│                    直销系统通用框架                     │
├────────────┬────────────┬────────────┬───────────────┤
│  会员管理   │  商品管理   │  订单管理   │   奖金结算 ⭐  │
│  财务管理   │  报表统计   │  系统管理   │   前台商城    │
└────────────┴────────────┴────────────┴───────────────┘
```

| 模块 | 功能 |
|------|------|
| 会员管理 | 注册/登录、实名认证、等级体系、推荐关系、团队树 |
| 商品管理 | 分类管理、SKU 管理、零售价/会员价、库存管理 |
| 订单管理 | 购物车、下单支付、发货签收、售后退换、业绩归属 |
| 奖金结算 | 策略模式 + 4 种制度可切换、异步批量计算、可追溯可回滚 |
| 财务管理 | 电子钱包、提现审核、收支明细 |
| 报表统计 | 经营仪表盘、销售/会员/奖金/团队报表、数据大屏 |
| 系统管理 | 用户/角色/权限、参数配置、操作日志 |

---

## 项目结构

```
直销/
├── 直销系统需求分析与技术方案.md      # 需求文档
├── README.md                          # 本文件
├── docker-compose.yml                 # 中间件编排
├── sql/
│   └── init.sql                       # 17 张表 + 种子数据
│
├── ds-backend/                        # Spring Boot 后端
│   ├── pom.xml                        # 父 POM
│   ├── ds-common/                     # 公共模块
│   ├── ds-system/                     # 系统管理
│   ├── ds-member/                     # 会员管理
│   ├── ds-product/                    # 商品管理
│   ├── ds-order/                      # 订单管理
│   ├── ds-bonus/                      # 奖金结算引擎 ⭐
│   ├── ds-finance/                    # 财务管理
│   ├── ds-report/                     # 报表统计
│   └── ds-app/                        # 启动入口
│
└── ds-frontend/                       # NuxtJS 3 前端
    ├── pages/
    │   ├── admin/                     # 后台管理（8 个页面）
    │   ├── member/                    # 会员中心（3 个页面）
    │   └── shop/                      # 前台商城（3 个页面）
    ├── stores/                        # Pinia 状态管理
    ├── composables/                   # 组合式函数
    └── middleware/                    # 路由中间件
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- Docker & Docker Compose（或手动安装 MySQL 8.0 + Redis 7 + RabbitMQ 3）

### 1. 启动中间件

```bash
# 在项目根目录执行
docker-compose up -d
```

启动后：
- MySQL 端口 `3306`，数据库 `direct_selling`，账号 `root` / `root123456`
- Redis 端口 `6379`，密码 `redis123456`
- RabbitMQ 端口 `5672`，管理面板 `http://localhost:15672`，账号 `admin` / `admin123456`

数据库初始化脚本 `sql/init.sql` 会在容器首次启动时自动执行。

### 2. 启动后端

```bash
cd ds-backend

# 编译安装所有模块
mvn clean install -DskipTests

# 启动应用
cd ds-app
mvn spring-boot:run
```

后端启动后访问：
- API 服务：`http://localhost:8080`
- API 文档（Knife4j）：`http://localhost:8080/doc.html`

### 3. 启动前端

```bash
cd ds-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后访问：
- 前台商城：`http://localhost:3000/shop`
- 会员中心：`http://localhost:3000/member`
- 后台管理：`http://localhost:3000/admin`

### 4. 默认账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 系统管理员 | `admin` | `123456` |

> 注意：`123456` 是初始化 SQL 中的 BCrypt 加密值，如需修改密码请通过系统管理界面操作。

---

## 生产部署

### 后端部署

```bash
# 打包
cd ds-backend
mvn clean package -DskipTests

# 运行
java -jar ds-app/target/ds-app-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 前端部署

```bash
cd ds-frontend
npm run build
# 将 .output/public/ 部署到 Nginx 或 CDN
```

### 环境变量

前端通过 `.env` 文件配置 API 地址：

```bash
# .env.production
NUXT_PUBLIC_API_BASE=https://your-api-domain.com
```

后端通过 `application-prod.yml` 配置生产环境参数。

---

## 奖金制度切换

1. 登录后台管理 → 奖金管理
2. 查看当前激活的制度
3. 点击"切换制度"按钮
4. 选择目标制度（级差制/双轨制/矩阵制/太阳线）
5. 确认切换，新产生的订单将按新制度计算

> 切换制度不影响历史数据，已结算的奖金不会回溯。

---

## 开发指南

### 新增奖金制度

1. 在 `ds-bonus/strategy/` 下创建新的 Calculator 类，实现 `BonusCalculator` 接口
2. 添加 `@Component` 注解，Spring 会自动注册到策略配置中
3. 在 `ds_bonus_plan` 表中添加新制度配置记录

### 新增业务模块

1. 在 `ds-backend/` 下创建新模块目录
2. 复制参考模块的 `pom.xml`，修改 artifactId
3. 在父 POM 的 `<modules>` 中添加
4. 在 `ds-app/pom.xml` 中添加依赖

---

## 许可证

MIT License