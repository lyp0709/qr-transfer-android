from flask import Flask, request, jsonify, send_file
import qrcode
import io
import base64
import zipfile
import os
import json

app = Flask(__name__)

# 配置
UPLOAD_FOLDER = 'uploads'
QR_FOLDER = 'generated_qr'
# QR Code version 40, Error Correction L 最大容量约4296字符
# 考虑格式开销和压缩优化，设置安全容量为4000字符
CHUNK_SIZE = 4000  # 每个二维码的数据块大小（字符数）
USE_COMPRESSION = True  # 启用gzip压缩

os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(QR_FOLDER, exist_ok=True)

def file_to_base64(file_path):
    """将文件转换为Base64编码（先压缩）"""
    with open(file_path, 'rb') as f:
        data = f.read()
    
    # 先用gzip压缩
    compressed_data = gzip.compress(data)
    
    # 转换为Base64
    return base64.b64encode(compressed_data).decode('utf-8')

def create_zip_from_folder(folder_path):
    """将文件夹压缩为zip"""
    zip_path = os.path.join(UPLOAD_FOLDER, 'temp.zip')
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(folder_path):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, folder_path)
                zipf.write(file_path, arcname)
    return zip_path

def split_data_into_chunks(data, chunk_size):
    """将数据分割成多个块"""
    chunks = []
    for i in range(0, len(data), chunk_size):
        chunks.append(data[i:i + chunk_size])
    return chunks

def generate_qr_code(data, index, total):
    """生成二维码（专用APP格式，包含压缩标记）"""
    qr = qrcode.QRCode(
        version=40,  # 使用最高版本，最大容量
        error_correction=qrcode.constants.ERROR_CORRECT_L,  # 低纠错，最大数据容量
        box_size=10,
        border=4,
    )
    
    # 专用APP格式：包含压缩标记
    metadata = {
        'index': index,
        'total': total,
        'compressed': USE_COMPRESSION,
        'data': data
    }
    qr.add_data(json.dumps(metadata))
    qr.make(fit=True)
    
    img = qr.make_image(fill_color="black", back_color="white")
    
    # 保存二维码
    qr_path = os.path.join(QR_FOLDER, f'qr_{index}.png')
    img.save(qr_path)
    
    return qr_path

@app.route('/')
def index():
    """返回主页面"""
    return send_file('index.html')

@app.route('/api/upload', methods=['POST'])
def upload_file():
    """上传文件或文件夹"""
    try:
        if 'file' not in request.files:
            return jsonify({'error': '没有文件上传'}), 400
        
        file = request.files['file']
        is_folder = request.form.get('is_folder', 'false').lower() == 'true'
        
        if file.filename == '':
            return jsonify({'error': '未选择文件'}), 400
        
        # 保存上传的文件
        temp_path = os.path.join(UPLOAD_FOLDER, file.filename)
        file.save(temp_path)
        
        if is_folder:
            # 如果是文件夹，需要先解压
            zip_path = temp_path
        else:
            zip_path = temp_path
        
        # 转换为Base64
        base64_data = file_to_base64(zip_path)
        
        # 分割数据
        chunks = split_data_into_chunks(base64_data, CHUNK_SIZE)
        
        # 生成二维码
        qr_paths = []
        for i, chunk in enumerate(chunks):
            qr_path = generate_qr_code(chunk, i, len(chunks))
            qr_paths.append(f'/api/qr/{i}')
        
        # 清理临时文件
        os.remove(zip_path)
        
        return jsonify({
            'success': True,
            'total_qr': len(chunks),
            'file_name': file.filename,
            'qr_paths': qr_paths
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/qr/<int:index>', methods=['GET'])
def get_qr_image(index):
    """获取二维码图片"""
    try:
        qr_path = os.path.join(QR_FOLDER, f'qr_{index}.png')
        if os.path.exists(qr_path):
            return send_file(qr_path, mimetype='image/png')
        else:
            return jsonify({'error': '二维码不存在'}), 404
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/clear', methods=['POST'])
def clear_qr():
    """清理生成的二维码"""
    try:
        for file in os.listdir(QR_FOLDER):
            file_path = os.path.join(QR_FOLDER, file)
            if os.path.isfile(file_path):
                os.remove(file_path)
        return jsonify({'success': True})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("=" * 50)
    print("本地二维码转译服务器")
    print("=" * 50)
    print("服务器启动中...")
    print("请在浏览器中打开: http://localhost:5000")
    print("=" * 50)
    app.run(debug=False, host='127.0.0.1', port=5000)
