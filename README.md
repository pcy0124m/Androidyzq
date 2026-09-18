# TOTP 验证器

一款兼容微软 / 谷歌验证器的开源 TOTP 工具，支持 **Windows 桌面版** 与 **Android 手机版**，纯本地存储，不上传任何数据。

## 功能

- 标准 **HMAC-SHA1 TOTP** 算法，每 30 秒生成 6 位动态验证码
- **扫码添加**：Android 版支持相机扫描二维码（CameraX + ZXing），离线可用
- **手动添加**：输入账号名和 Base32 密钥
- 多账号管理，独立倒计时与一键复制
- 账号数据仅保存在本地，不上传服务器

## 下载

| 平台 | 下载方式 | 说明 |
|------|----------|------|
| Android | [Releases](https://github.com/pcy0124m/Androidyzq/releases) 下载 `app-debug.apk` | 安装即用，需允许相机权限以扫码 |
| Windows | 见本仓库 Releases 或 [TOTPValidator.exe](https://github.com/pcy0124m/Androidyzq/releases) | 单文件 exe，双击运行，无需安装 |

## Android 使用步骤

1. 安装 APK 后打开应用。
2. 点击右下角 **+** 添加账号：
   - 选择 **扫码添加**：对准网站给出的二维码。
   - 或选择 **手动输入**：填写账号名和 Base32 密钥。
3. 列表中会显示账号名与当前 6 位验证码，倒计时 30 秒自动刷新。
4. 点击验证码即可复制到剪贴板。

## Windows 使用步骤

1. 下载 `TOTPValidator.exe`。
2. 双击运行，点击 **添加账号**。
3. 输入账号名和 Base32 密钥，保存。
4. 选中条目后点击 **复制** 即可粘贴到网站验证码框。

## 技术栈

- Android：Kotlin + AndroidX + CameraX + ZXing
- Windows：Python + Tkinter + PyInstaller
- CI：GitHub Actions 自动编译 APK

## 隐私说明

所有账号信息（账号名、密钥、生成的验证码）均只保存在用户设备本地，不会上传到任何服务器。

## 开源许可

MIT License
