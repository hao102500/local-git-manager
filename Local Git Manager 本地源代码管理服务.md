# Local Git Manager 本地源代码管理服务

## 1. 项目概述

### 1.1 项目定位

开发一套支持 Windows 和 macOS 的本地 Git 源代码管理工具。

用户可以在自己的电脑上管理源代码版本，无需依赖 GitHub、Gitee 或远程服务器。

系统通过图形化界面提供代码提交、历史记录、差异对比、分支管理、版本恢复和发布台账等功能。

### 1.2 建设目标

- 支持 Windows 10/11 和 macOS 本地运行。
- 支持管理电脑上的多个本地 Git 仓库。
- 支持代码提交、历史记录和版本差异对比。
- 支持分支、标签和历史版本恢复。
- 支持发布台账，并关联对应的 Git 提交。
- 支持离线运行，代码和历史版本保存在本地。
- 支持 Windows 和 macOS 独立安装、启动和升级。
- 为未来内网部署和多人协作预留扩展空间。

### 1.3 首期功能范围

| 功能                     | 首期实现 |
| ------------------------ | -------- |
| Windows / macOS 本地运行 | 支持     |
| 本地仓库管理             | 支持     |
| 代码提交与历史           | 支持     |
| 代码差异对比             | 支持     |
| 分支与标签管理           | 支持     |
| 历史版本恢复             | 支持     |
| 发布台账                 | 支持     |
| 远程仓库同步             | 暂不实现 |
| 多人协作与权限           | 暂不实现 |
| 自动构建和部署           | 暂不实现 |

## 2. 技术架构

### 2.1 技术选型

| 层级       | 技术                   | 用途           |
| ---------- | ---------------------- | -------------- |
| 前端       | React 19 + Vite 8      | 跨平台管理界面 |
| UI 组件    | Ant Design 6           | 管理后台界面   |
| 路由       | React Router 7         | 页面路由管理   |
| 代码对比   | Monaco Editor          | 代码差异展示   |
| 后端       | Java 17+ + Spring Boot | 业务接口和服务 |
| Git 引擎   | JGit                   | Git 仓库管理   |
| 数据访问   | MyBatis-Plus           | 数据库访问     |
| 数据库     | SQLite                 | 本地业务数据   |
| 数据库迁移 | Flyway                 | 数据库结构管理 |
| 构建工具   | Maven + Node.js        | 前后端构建     |

### 2.2 系统架构

```
┌────────────────────────────────────────┐
│            Windows / macOS             │
│                                        │
│  ┌──────────────────────────────────┐  │
│  │          React 前端              │  │
│  │                                  │  │
│  │ 仓库管理 / 代码对比 / 提交历史   │  │
│  │ 分支管理 / 版本恢复 / 发布台账   │  │
│  └────────────────┬─────────────────┘  │
│                   │ HTTP API           │
│  ┌────────────────▼─────────────────┐  │
│  │        Spring Boot 后端          │  │
│  │                                  │  │
│  │ Controller → Service             │  │
│  │                    ↓             │  │
│  │              GitService          │  │
│  │              Repository          │  │
│  └─────────┬────────────────┬───────┘  │
│            │                │          │
│  ┌─────────▼──────┐ ┌───────▼────────┐ │
│  │ Git 本地仓库   │ │ SQLite 数据库  │ │
│  │                │ │                │ │
│  │ 源代码         │ │ 仓库元数据     │ │
│  │ 提交历史       │ │ 发布台账       │ │
│  │ 分支与标签     │ │ 用户配置       │ │
│  └────────────────┘ └────────────────┘ │
└────────────────────────────────────────┘
```

### 2.3 跨平台运行方式

采用前后端分离架构，开发阶段分别启动前端和后端。

- 前端开发地址：`http://localhost:5173`
- 后端服务地址：`http://localhost:8080`
- SQLite 数据库：存储于当前操作系统的用户数据目录。
- Git 仓库：使用用户已有项目目录，或由用户指定独立仓库目录。

正式运行时，将 React 前端构建为静态资源，由 Spring Boot 提供访问。

应用启动后自动打开本地浏览器，用户无需手动打开终端执行 Git 命令。

后端作为本地服务运行，不要求用户安装 Java、Node.js 或 Maven。上述环境仅用于开发和构建，正式安装包内提供运行所需的 Java Runtime。

## 3. 跨平台设计原则

### 3.1 操作系统兼容

前端采用 React 和 Vite，后端采用 Java 和 Spring Boot，均支持 Windows 和 macOS。

应用业务逻辑不得依赖特定操作系统的路径格式、命令行语法或文件系统行为。

### 3.2 路径处理

所有仓库路径统一使用 Java 的 `Path` 和 `Files` API 处理，不使用字符串拼接构造文件路径。

Windows 示例：

```
D:\Code\release-manager-web
```

macOS 示例：

```
/Users/hao/code/release-manager-web
```

前端仅展示和传递仓库路径，不自行拼接路径。后端负责路径规范化、目录校验和仓库访问。

### 3.3 文件系统兼容

- 支持 Windows 盘符、UNC 网络路径和 macOS 绝对路径。
- 兼容 Windows 不区分大小写的文件系统。
- 正确处理空格、中文和特殊字符目录。
- 不假设文件系统大小写敏感。
- 不依赖 macOS 专有目录结构。
- 不依赖 Windows 注册表或特定 Shell。

### 3.4 Git 环境检测

JGit 作为主要 Git 操作引擎，不要求用户必须安装 Git 命令行。

导入已有仓库时，检测目标目录是否存在有效的 `.git` 仓库结构。

如果用户已有 Git for Windows 或 macOS Git，也不影响系统使用。

首期不执行任意 Shell 命令，避免 Windows 和 macOS 命令差异带来的兼容问题。

### 3.5 文件权限与安全

- 仅访问用户明确授权的本地目录。
- 禁止通过接口执行任意命令。
- 删除分支、恢复文件等危险操作必须进行校验。
- 恢复操作前检查未提交修改。
- 不允许应用自动删除用户原始代码。
- 不在代码仓库中存储应用数据库和用户配置。

## 4. 功能模块设计

### 4.1 仓库管理

- 新建本地 Git 仓库。
- 导入已有 Git 仓库。
- 查看仓库名称、路径、当前分支和修改状态。
- 查看仓库文件及工作区变更。
- 管理仓库基本信息。
- 支持从文件选择器选择仓库目录。

### 4.2 代码版本管理

- 查看当前工作区修改。
- 查看已暂存和未暂存的代码变更。
- 将文件加入暂存区。
- 提交代码并填写提交说明。
- 查询提交历史和提交详情。
- 查看指定提交包含的文件变更。

### 4.3 代码差异对比

- 工作区与最近一次提交的差异。
- 暂存区与最近一次提交的差异。
- 任意两个历史提交之间的差异。
- 文件级差异和行级差异。
- 使用 Monaco DiffEditor 展示左右代码对比。

### 4.4 分支与标签管理

- 查看本地分支。
- 创建、切换和删除分支。
- 创建和查看版本标签。
- 查看分支对应的提交记录。

### 4.5 版本恢复

- 恢复单个文件到指定版本。
- 恢复整个工作区到指定版本。
- 查看恢复前后的差异。
- 恢复前检查未提交修改。
- 对覆盖工作区的操作提供明确的风险提示。

### 4.6 发布台账

- 新增发布记录。
- 记录项目名称、版本号、发布时间和变更内容。
- 关联 Git 提交或版本标签。
- 查询发布历史。
- 查看指定发布版本对应的代码变更。

## 5. 前端目录结构

项目名称：`local-git-manager-web`

```
local-git-manager-web/
├── public/
├── src/
│   ├── api/
│   │   ├── request.js
│   │   ├── repository.js
│   │   ├── git.js
│   │   └── release.js
│   ├── assets/
│   ├── components/
│   │   ├── AppLayout/
│   │   ├── PageHeader/
│   │   ├── GitStatus/
│   │   ├── CommitList/
│   │   └── DiffViewer/
│   ├── pages/
│   │   ├── Dashboard/
│   │   ├── Repository/
│   │   ├── Changes/
│   │   ├── History/
│   │   ├── Branches/
│   │   ├── Releases/
│   │   └── Settings/
│   ├── hooks/
│   ├── router/
│   ├── stores/
│   ├── utils/
│   ├── styles/
│   ├── App.jsx
│   └── main.jsx
├── .env.development
├── .env.production
├── package.json
└── vite.config.js
```

前端通过统一 API 调用后端服务，不直接访问 Git 仓库文件。

仓库目录选择由后端提供跨平台文件选择接口，避免前端自行处理操作系统差异。

## 6. 后端目录结构

项目名称：`local-git-manager-server`

```
local-git-manager-server/
├── pom.xml
├── local-git-manager-common/
│   └── src/main/java/com/localgit/common/
│       ├── result/
│       ├── exception/
│       └── constant/
├── local-git-manager-core/
│   └── src/main/java/com/localgit/core/
│       ├── repository/
│       ├── commit/
│       ├── diff/
│       ├── branch/
│       ├── tag/
│       └── restore/
└── local-git-manager-server/
    └── src/
        ├── main/
        │   ├── java/com/localgit/server/
        │   │   ├── controller/
        │   │   ├── service/
        │   │   ├── mapper/
        │   │   ├── entity/
        │   │   ├── dto/
        │   │   ├── config/
        │   │   ├── platform/
        │   │   └── LocalGitManagerApplication.java
        │   └── resources/
        │       ├── application.yml
        │       └── db/migration/
        └── test/
```

### 模块职责

- `common`：统一响应、异常处理和公共常量。
- `core`：封装 Git 仓库操作、提交、差异计算、分支、标签和版本恢复。
- `server`：负责 HTTP 接口、业务逻辑、数据库访问和服务启动。
- `platform`：封装操作系统相关能力，如用户目录、应用数据目录、文件选择和服务启动。

## 7. 本地数据目录设计

### 7.1 Windows

应用数据目录：

```
%LOCALAPPDATA%\LocalGitManager\
├── data\
│   └── local-git-manager.db
├── logs\
├── config\
└── backup\
```

例如：

```
C:\Users\<用户名>\AppData\Local\LocalGitManager\
```

### 7.2 macOS

应用数据目录：

```
~/Library/Application Support/LocalGitManager/
├── data/
│   └── local-git-manager.db
├── logs/
├── config/
└── backup/
```

### 7.3 项目代码目录

用户的代码仍保留在原来的开发目录中。

Windows：

```
D:\Code\
├── release-manager-web\
├── release-manager-server\
└── salary-manager\
```

macOS：

```
~/code/
├── release-manager-web/
├── release-manager-server/
└── salary-manager/
```

应用通过仓库配置记录项目路径，不移动、不复制用户已有的源代码。

## 8. 安装与部署设计

### 8.1 Windows

- 提供 Windows x64 安装包。
- 安装包包含应用后端、前端资源和 Java Runtime。
- 支持开始菜单快捷方式。
- 支持应用启动和退出。
- 提供卸载程序。
- 应用数据和用户代码默认不随卸载删除。

### 8.2 macOS

- 提供 macOS Apple Silicon 和 Intel 对应的安装包。
- 支持独立启动应用。
- 应用数据存储于用户目录。
- 支持应用退出和卸载。
- 不依赖用户全局安装 Java 或 Node.js。

### 8.3 桌面应用实现方式

首期采用浏览器访问本地服务，减少桌面应用开发复杂度。

后续可以使用 Tauri 或 Electron 封装为独立桌面应用，提供菜单栏、系统托盘和原生文件选择等能力。

## 9. 关键设计原则

### 9.1 Git 是唯一的版本管理引擎

不自行实现代码版本存储、提交快照或差异计算。所有版本相关功能通过 Git / JGit 实现。

### 9.2 本地优先，逐步扩展

首期不引入 Redis、消息队列、复杂微服务架构或远程服务器依赖。

### 9.3 数据与代码分离

Git 仓库负责代码历史，SQLite 负责业务元数据。两者独立备份，互不覆盖。

### 9.4 跨平台兼容

所有操作系统相关能力集中封装，不在业务代码中直接判断操作系统或拼接平台路径。

### 9.5 保持可迁移性

后续可以将 SQLite 迁移到 MySQL，将本地服务部署到内网服务器，并扩展远程仓库和多人协作能力。

## 10. 后续开发顺序

第一阶段：完成 Windows 和 macOS 双平台项目初始化、仓库导入和 Git 状态查询。

第二阶段：完成代码提交、提交历史和代码差异对比。

第三阶段：完成分支、标签和历史版本恢复。

第四阶段：完成发布台账与 Git 提交关联。

第五阶段：完成 Windows 和 macOS 安装包、自动启动、数据备份和安全管理。

最终形成一套支持 Windows 和 macOS 本地运行、可持续扩展的 Git 源代码管理服务。