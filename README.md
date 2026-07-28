# 二维码转译工具

一个完整的文件到二维码转换系统，支持通过手机扫描二维码传输文件。

## 功能特性

- **文件转二维码**：将任意文件或文件夹转换为二维码序列
- **智能分片**：大文件自动分割为多个二维码
- **Android应用**：通过相机扫描二维码并重建文件
- **Web界面**：简洁的文件上传和二维码展示界面

## 系统架构

### 后端服务 (Python Flask)
- 文件上传和处理
- Base64编码
- 数据分片
- 二维码生成
- REST API接口

### Web前端
- 文件拖拽上传
- 实时进度显示
- 二维码网格展示
- 批量下载功能

### Android应用
- 相机实时预览
- 二维码自动识别
- 数据解码和重组
- 文件保存功能

## 安装和运行

### 后端服务

1. 安装依赖：
```bash
cd backend
pip install -r ../requirements.txt
```

2. 启动服务：
```bash
python app.py
```

服务将在 `http://localhost:5000` 启动

### Web前端

直接在浏览器中打开 `frontend/index.html`，或使用本地服务器：
```bash
cd frontend
python -m http.server 8080
```

### Android应用

1. 使用Android Studio打开 `android` 目录
2. 同步Gradle依赖
3. 连接Android设备或启动模拟器
4. 点击运行按钮

## 使用方法

### 发送文件（PC端）

1. 打开Web界面
2. 选择文件类型（单个文件或文件夹）
3. 拖拽或点击上传文件
4. 等待二维码生成
5. 使用Android应用扫描所有二维码

### 接收文件（Android端）

1. 打开Android应用
2. 授予相机权限
3. 点击"开始扫描"
4. 依次扫描所有生成的二维码
5. 扫描完成后点击"保存文件"
6. 文件将保存到 `QRTransfer` 目录

## 技术栈

- **后端**：Python Flask, qrcode, Pillow
- **前端**：HTML5, CSS3, JavaScript
- **Android**：Kotlin, CameraX, ML Kit
- **编码**：Base64, JSON

## API接口

### POST /api/upload
上传文件并生成二维码

参数：
- file: 文件数据
- is_folder: 是否为文件夹（true/false）

返回：
```json
{
  "success": true,
  "total_qr": 5,
  "file_name": "example.zip",
  "qr_paths": ["generated_qr/qr_0.png", ...]
}
```

### GET /api/qr/{index}
获取指定索引的二维码图片

### POST /api/clear
清理所有生成的二维码

## 注意事项

- 大文件会生成多个二维码，需要按顺序扫描
- 二维码数据包含元数据（索引和总数）
- Android应用需要API 24+ (Android 7.0+)
- 相机权限是必需的

## 开发计划

- [ ] 添加加密功能
- [ ] 支持更多文件格式
- [ ] 优化大文件处理
- [ ] 添加传输历史记录
- [ ] 支持断点续传
