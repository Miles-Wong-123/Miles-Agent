# Miles-Agent

这是一个基于 Spring Boot + LangChain4j 的 Agent 项目。

## 为什么推到 GitHub 后感觉“很多东西没了”

最常见的原因有 4 个：

1. 文件还没有被 Git 跟踪。
   你当前仓库里大量核心代码还是 `??` 状态，说明它们只是存在于本地，还没有 `git add` 进暂存区，自然不会被推到 GitHub。
2. 文件被 `.gitignore` 忽略了。
   例如 `target/`、`.idea/`、`HELP.md` 这类文件本来就不应该推上去。
3. GitHub 不显示空目录。
   如果某个目录里面没有被跟踪的文件，GitHub 页面上看起来就像“目录没了”。
4. 只提交了初始化模板。
   当前提交历史里只有一个 `first commit`，里面主要还是 Spring Initializr 生成的骨架文件。

## 这份仓库建议如何安全推送

### 1. 先检查哪些文件会被提交

```bash
git status
git status --ignored
```

如果看到 `?? src/main/resources/`、`?? src/main/java/...`，说明这些文件还没有被纳入版本控制。

### 2. 提交代码前先检查是否包含敏感信息

当前项目涉及下面这些敏感配置：

- 邮箱账号和 SMTP 授权码
- DashScope API Key
- BigModel API Key
- Redis 密码
- pgvector 数据库密码

这些信息不要直接写进 GitHub 仓库。推荐做法：

- 仓库里只保留带占位符的 `application.yml`、`application-dev.yml`、`application-prod.yml`
- 本地真实配置放在 `src/main/resources/application-local.yml`
- `application-local.yml` 已加入 `.gitignore`，不会被提交

### 3. 本地准备自己的私有配置

可以参考：

- [application-local.example.yml](/Users/wmy/Code/Miles-Agent/src/main/resources/application-local.example.yml)

把它复制成 `src/main/resources/application-local.yml` 后，再填入你自己的真实配置。

### 4. 只提交应该进仓库的内容

推荐先按目录加，而不是直接 `git add .`：

```bash
git add pom.xml
git add src/main/java
git add src/main/resources
git add src/test/java
git add README.md
git add .gitignore
```

然后检查暂存区内容：

```bash
git diff --cached --stat
git diff --cached
```

如果发现不该提交的文件，可以撤回暂存：

```bash
git restore --staged <文件路径>
```

### 5. 提交并推送

```bash
git commit -m "Add Miles Agent implementation"
git push origin main
```

## 很有用的排查命令

查看某个文件为什么没有上 Git：

```bash
git check-ignore -v <文件路径>
```

查看当前已经被 Git 跟踪的文件：

```bash
git ls-files
```

## 如果你曾经把密钥推上过 GitHub

即使后来删掉，历史记录里也可能还在。需要立刻做两件事：

1. 去对应平台把密钥吊销或重置
2. 再清理 Git 历史，或者直接新建安全提交后强推
