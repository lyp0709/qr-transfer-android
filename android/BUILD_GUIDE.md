# Android APK 编译指南

## 方案对比

### 方案1：Android Studio编译（推荐）
**优点**：完全控制，可自定义
**缺点**：需要安装Android Studio（约2GB）

### 方案2：GitHub Actions自动构建
**优点**：无需本地安装，自动生成APK
**缺点**：需要GitHub账号，配置稍复杂

### 方案3：在线APK构建服务
**优点**：简单快速
**缺点**：可能有安全风险，限制较多

## 推荐方案：GitHub Actions自动构建

### 步骤1：准备GitHub仓库

1. **创建GitHub账号**（如果没有）
2. **创建新仓库**：命名为 `qr-transfer-android`
3. **上传Android项目**：
   ```bash
   cd d:/lypworktools/workFile/Devin/qr-transfer/android
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/你的用户名/qr-transfer-android.git
   git push -u origin main
   ```

### 步骤2：配置GitHub Actions

1. **创建工作流文件**：
   在GitHub仓库中创建 `.github/workflows/build.yml`：

```yaml
name: Build Android APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Build APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

### 步骤3：触发构建

1. **推送到GitHub**：代码会自动触发构建
2. **手动触发**：在GitHub Actions页面点击"Run workflow"
3. **下载APK**：构建完成后在Actions页面下载artifact

## 方案2：Android Studio本地编译

### 安装步骤

1. **下载Android Studio**：
   https://developer.android.com/studio

2. **安装Android Studio**：
   - 运行安装程序
   - 选择"Standard"安装
   - 等待下载SDK和组件（约2GB）

3. **打开项目**：
   - 启动Android Studio
   - 选择"Open an Existing Project"
   - 选择 `d:/lypworktools/workFile/Devin/qr-transfer/android`

4. **同步Gradle**：
   - 等待Gradle同步完成
   - 可能需要下载依赖

5. **构建APK**：
   - 菜单：Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 等待构建完成

6. **找到APK**：
   - 位置：`android/app/build/outputs/apk/debug/app-debug.apk`

## 性能优化说明

当前版本已优化：
- ✅ **gzip压缩**：减少数据量15-70%
- ✅ **QR Code v40**：最大容量（4296字符）
- ✅ **自动连续扫描**：约2秒/个
- ✅ **自动数据重组**：无需手动操作

### 2MB文件传输时间

- **已压缩7z文件**：约20分钟（592个二维码）
- **未压缩文件**：约7分钟（209个二维码）

## 使用说明

### 安装APK到手机

1. **启用未知来源**：
   - 设置 → 安全 → 允许安装未知来源应用

2. **传输APK**：
   - USB数据线
   - 或其他方式

3. **安装APK**：
   - 在手机文件管理器中找到APK
   - 点击安装

### 使用APP

1. **启动APP**，授予相机权限
2. **点击"开始扫描"**
3. **依次扫描所有二维码**
4. **扫描完成后点击"保存文件"**
5. **文件保存在**：`内部存储/Android/data/com.qrtransfer.app/files/QRTransfer/`

## 故障排除

### 构建失败

**Gradle同步失败**：
- 检查网络连接
- 尝试使用VPN
- 手动下载依赖

**编译错误**：
- 检查JDK版本（需要JDK 17）
- 清理项目：Build → Clean Project
- 重新同步：File → Sync Project with Gradle Files

### 安装失败

**签名问题**：
- Debug APK可以直接安装
- 如需Release版本，需要配置签名

**兼容性问题**：
- 确保Android版本 ≥ 7.0 (API 24)
- 检查手机架构支持

## 下一步

选择一个方案开始编译APK：

1. **快速方案**：使用GitHub Actions（推荐）
2. **完整方案**：安装Android Studio
3. **其他方案**：寻找在线APK构建服务

编译完成后，就可以开始使用二维码传输文件了！
