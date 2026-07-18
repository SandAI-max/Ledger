# 苏苏记账

## 项目简介

苏苏记账是一款面向个人使用的Android原生应用，能够自动记录微信和支付宝的每一笔消费，帮助自己轻松管理个人财务。
- **目标用户**：开发者本人，单机使用
- **应用ID**：`com.sudeng.zhangben`
- **包名**：`com.sudeng.zhangben`

## 核心功能

### 1. 自动记录消费 ✅
- 通过无障碍服务自动捕获微信和支付宝支付信息
- 通过通知监听服务实时获取支付通知
- 自动提取金额、商户、时间等关键信息
- **智能分类**：150+ 关键词自动匹配商户 → 分类
> 代码已就绪，需真机安装后授权使用

### 2. 手动记账 ✅
- 手动输入金额、选择分类、设置日期
- 交易详情查看（时间、分类、金额、来源、备注）
- 交易删除（带确认对话框）

### 3. OCR 截图识别 ✅
- 批量选择最多 10 张支付截图
- ML Kit 中文离线 OCR，自动提取金额、商户、日期
- 智能分类 + 按时间排序 + 确认后批量保存

### 4. 智能分类管理 ✅
- 预设 8 个支出分类 + 4 个收入分类（含 emoji 图标）
- 商户名关键词自动匹配分类（12 类 × 150+ 关键词）
- 分类 CRUD：添加 / 编辑（名称+图标）/ 删除

### 5. 数据分析统计 ✅
- 日期区间折线图（每日支出/收入趋势，自定义起止日期）
- 分类消费占比饼图
- 商户消费排名（金/银/铜前三高亮）
- 月度收支汇总

### 6. 预算管理 ✅
- 月度总预算设置 + 三色进度条（绿 <80% / 橙 80~100% / 红 >100%）
- 分类预算设置（每个支出分类独立预算 + 进度条）
- 超支提醒 / 剩余预算显示

### 7. 数据管理 ✅
- 多格式导出（TXT / HTML / CSV），系统分享发送
- WorkManager 每周自动备份 CSV（保留最近 5 份）

### 8. 账单筛选 ✅
- 关键词搜索（分类名/备注）
- 类型筛选（全部 / 支出 / 收入）
- 按日期分组显示（今天/昨天/本周/本月/更早）

## 核心页面

| 页面 | 标题 | 功能 |
|------|------|------|
| **首页** | 苏苏记账 | 月份标签 + 消费概览 + 预算进度入口 + 最近5条交易 + 可展开 FAB（记一笔/截图识别） |
| **消费账单** | 消费账单 | 搜索 + 筛选 + 日期分组 + 点击详情 + 删除确认 + 多格式导出 |
| **数据分析** | 数据分析 | 日期区间 + 折线图 + 饼图 + 商户排名 |
| **预算管理** | 预算管理 | 总预算/分类预算设置 + 进度条 + 超支/剩余提醒 |
| **个人中心** | 个人中心 | APP 信息 + 记录总数 + 管理分类 + 服务状态 + 数据导出 |

## 技术栈

### 开发环境
- **开发语言**：Kotlin 2.0.21
- **构建工具**：Gradle Kotlin DSL + Version Catalog (libs.versions.toml)
- **AGP 版本**：8.13.2
- **最低支持**：Android 7.0 (API 24)
- **编译 SDK**：API 36

### 架构设计
- **架构模式**：MVVM (Model-View-ViewModel)
- **响应式编程**：Kotlin Coroutines + Flow
- **依赖注入**：Hilt (Dagger)
- **导航组件**：Jetpack Navigation（5 Tab 底部导航）

### UI 技术
- **UI 框架**：Jetpack Compose (Material 3)
- **设计系统**：蓝色主题 + 统一色彩常量（`ExpenseRed`/`IncomeGreen` 等）
- **图表**：Compose Canvas 自绘（折线图 + 饼图）
- **动画**：Spring 弹簧入场 + 淡出过渡 + 底部导航缩放反馈
- **图标**：Material Icons Extended + Emoji
- **图片加载**：Coil

### 数据存储
- **本地数据库**：Room (SQLite ORM)
- **偏好设置**：DataStore（预算数据持久化）
- **文件存储**：FileProvider（CSV/TXT/HTML 导出）
- **备份**：WorkManager 每周自动备份

### 网络通信
- 单机使用，无网络依赖

### 数据采集
- **无障碍服务**：PaymentAccessibilityService（微信/支付宝页面）
- **通知监听**：PaymentNotificationListener（支付通知解析）
- **支付识别**：正则提取金额、商户名 + 智能分类匹配
- **OCR 识别**：ML Kit 中文离线文字识别

## 项目结构

```
app/src/main/java/com/sudeng/zhangben/
├── ZhangbenApplication.kt              # Hilt 入口 + WorkManager 调度
├── MainActivity.kt                     # 唯一 Activity
├── di/
│   └── AppModule.kt                    # Hilt DI（Database, DAOs）
├── data/
│   └── local/
│       ├── AppDatabase.kt              # Room 数据库
│       ├── BudgetManager.kt            # DataStore 预算管理
│       ├── Converters.kt               # Room 类型转换器
│       ├── dao/
│       │   ├── TransactionDao.kt
│       │   ├── CategoryDao.kt
│       │   ├── DailySum.kt             # 日聚合查询
│       │   ├── CategorySum.kt          # 分类聚合查询
│       │   └── MerchantRank.kt         # 商户排名查询
│       ├── entity/
│       │   ├── TransactionEntity.kt
│       │   ├── CategoryEntity.kt
│       │   └── TransactionWithCategory.kt
│       └── repository/
│           ├── TransactionRepository.kt
│           └── CategoryRepository.kt
├── domain/model/
│   └── Models.kt                       # 领域模型
├── ui/
│   ├── navigation/
│   │   ├── AppNavigation.kt            # 导航图 + 弹簧动画过渡
│   │   └── Screen.kt                   # 路由定义
│   ├── theme/
│   │   ├── Color.kt                    # 色彩常量（ExpenseRed / IncomeGreen 等）
│   │   ├── Theme.kt                    # Material3 主题
│   │   └── Type.kt                     # 字体排版
│   └── screen/
│       ├── home/                       # 首页
│       ├── transaction/                # 账单（列表/详情/添加）
│       ├── statistics/                 # 统计（折线图/饼图/商户排名）
│       ├── budget/                     # 预算（总预算/分类预算）
│       ├── profile/                    # 个人中心（分类管理/导出/服务状态）
│       └── ocr/                        # OCR 截图识别
├── service/
│   ├── PaymentAccessibilityService.kt  # 无障碍支付抓取
│   └── PaymentNotificationListener.kt  # 通知解析
├── util/
│   ├── DateFormatUtil.kt               # 北京时间格式化
│   ├── ExportManager.kt                # 多格式导出（TXT/HTML/CSV）
│   ├── MerchantClassifier.kt           # 商户→分类智能匹配
│   └── OcrAnalyzer.kt                  # ML Kit OCR 引擎
└── worker/
    └── BackupWorker.kt                 # WorkManager 定时备份
```

## 开发计划

### 第一阶段：基础功能 ✅ 已完成
- [x] 项目架构搭建
- [x] 核心依赖引入（Hilt、Room、Navigation、DataStore、WorkManager、ML Kit、Coil）
- [x] 本地数据库设计
- [x] 手动记账功能
- [x] 分类管理（12 分类 + CRUD）
- [x] 预算管理（总预算 + 分类预算 + 进度条）
- [x] 数据统计（折线图 + 饼图 + 商户排名）
- [x] 多格式导出（TXT/HTML/CSV）
- [x] 智能分类算法（150+ 关键词商户匹配）
- [x] OCR 批量截图识别
- [x] WorkManager 每周自动备份
- [x] 全局色彩常量化
- [x] 弹簧动画入场 + 底部导航缩放反馈

### 第二阶段：真机验证
- [ ] 无障碍服务真机测试（微信/支付宝支付页面抓取）
- [ ] 通知监听服务真机测试
- [ ] 智能分类真机准确性验证
- [ ] OCR 识别真机准确性验证

### 第三阶段：待优化
- [ ] 数据导入恢复
- [ ] 深色模式验证

## 安全考虑

- 100% 本地存储，数据不上传云端
- 权限最小化（仅无障碍 + 通知监听）
- OCR 离线运行，截图不联网

## 许可证

MIT License
