# 水印相机

水印相机是一款原生 Android 拍照与图片标注应用，适用于外勤签到、工程记录、巡检取证和日常照片整理。当前发布版本为 **1.0**，包名为 `com.watermarkcamera.studio`，最低支持 Android 8.0（API 26）。

详细功能、权限和发布说明见 [APP_INTRODUCTION.md](APP_INTRODUCTION.md)。最终图标的设计源文件、各尺寸导出物和来源记录位于 [branding/app-icon](branding/app-icon)。

## 功能概览

- CameraX 拍照、前后摄像头、闪光灯、点击对焦、倒计时和焦距调节。
- 从系统相册导入照片，支持平移、缩放、旋转和自由/定比裁剪。
- 添加文字、动态时间、位置、图片、Logo 和内置图标水印。
- 图层移动、缩放、旋转、锁定、隐藏、复制、删除、排序、撤销和重做。
- 命名模板的创建、更新、重命名、删除以及 JSON 导入/导出。
- JPEG 保存到系统相册和系统分享。
- 高德定位、逆地理编码和地点搜索，也支持手动输入位置。

## 开发环境

- Android Studio，JDK 17
- Android SDK 36
- Windows PowerShell（以下命令以 Windows 为例）

首次构建前，在项目根目录的 `local.properties` 中配置 Android SDK 路径。高德 Android Key 可以在应用设置中填写，也可以在本机 `gradle.properties` 中配置备用值：

```properties
AMAP_API_KEY=你的高德AndroidKey
```

应用内保存的 Key 优先于工程备用 Key。Key 应在高德开放平台绑定包名 `com.watermarkcamera.studio` 和实际安装包的签名 SHA-1。不要将真实 Key 提交到版本控制或放入公开备份。

## 构建与测试

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## GitHub Actions

仓库内的 `Build APK` 工作流会在推送到 `main`、提交 Pull Request 或手动触发时运行单元测试并构建 Debug APK。构建完成后，可在对应的 Actions 运行页面下载 `WatermarkCameraStudio-debug` 构件。

CI 仅使用 `gradle.properties.example` 中的非敏感配置，不包含高德 Key、发布签名文件或密码。

Release 构建需要本机 `keystore.properties` 和对应的密钥文件。配置文件格式如下：

```properties
storeFile=signing/你的密钥文件.jks
storePassword=你的密码
keyAlias=你的别名
keyPassword=你的密码
```

然后执行：

```powershell
.\gradlew.bat :app:assembleRelease
```

`keystore.properties`、`signing/`、`local.properties` 和真实 API Key 均属于本机敏感配置，不应进入源码备份。

## 图标

版本 1.0 使用 B2 Coordinate Cut / R2 Signal Red 方案：信号红 `#E43D36`、白色 `#FFFFFF`、琥珀色 `#FFC247`。Android 自适应图标的白色主体在启动器实测约占图标宽度 60%。图标源文件不得在清理构建产物时删除。
