# 离线二维码转译工具

完全离线的文件传输解决方案，通过二维码在电脑和手机之间传输文件。

## 功能特性

- **完全离线**：无需网络连接
- **简单易用**：一条命令生成二维码
- **支持大文件**：自动分割为多个二维码
- **支持文件夹**：自动压缩为zip后传输

## 安装

1. 确保已安装Python 3.6+
2. 安装依赖：
```bash
pip install -r requirements.txt
```

## 使用方法

### 传输单个文件
```bash
python generate_qr.py 文件路径
```

示例：
```bash
python generate_qr.py document.pdf
python generate_qr.py photo.jpg
```

### 传输文件夹
```bash
python generate_qr.py 文件夹路径 --folder
```

示例：
```bash
python generate_qr.py my_documents --folder
python generate_qr.py project_files --folder
```

## 使用流程

1. **电脑端**：
   - 运行Python脚本生成二维码
   - 二维码保存在 `qr_output` 目录

2. **手机端**：
   - 打开Android应用
   - 依次扫描所有二维码
   - 应用自动重建文件

3. **完成**：
   - 文件保存在手机的 `QRTransfer` 目录

## Android应用

### 安装
1. 使用Android Studio打开 `android` 目录
2. 构建并安装到手机

### 使用
1. 打开应用，授予相机权限
2. 点击"开始扫描"
3. 按顺序扫描所有二维码
4. 扫描完成后点击"保存文件"

## 注意事项

- 大文件会生成多个二维码，需要按顺序扫描
- 确保手机有足够存储空间
- 二维码图片可以打印或通过其他方式传输
- 扫描时保持二维码清晰可见

## 技术原理

1. **编码**：文件 → Base64编码 → JSON格式
2. **分片**：大数据分割为小块，每块一个二维码
3. **传输**：通过二维码传输数据块
4. **解码**：手机端重组数据块 → Base64解码 → 还原文件

## 故障排除

### Python依赖问题
```bash
pip install --upgrade pip
pip install qrcode pillow
```

### 二维码无法扫描
- 确保二维码清晰完整
- 调整扫描距离和角度
- 检查光线条件

### 文件重建失败
- 确保所有二维码都已扫描
- 检查扫描顺序是否正确
- 确认手机存储空间充足
