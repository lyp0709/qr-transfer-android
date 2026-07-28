#!/usr/bin/env python3
"""
离线二维码生成器
将文件转换为二维码图片序列
"""

import os
import sys
import base64
import json
import qrcode
import zipfile

# 配置
CHUNK_SIZE = 2000  # 每个二维码的数据块大小（字符数）
OUTPUT_DIR = "qr_output"

def ensure_output_dir():
    """确保输出目录存在"""
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

def file_to_base64(file_path):
    """将文件转换为Base64编码"""
    with open(file_path, 'rb') as f:
        return base64.b64encode(f.read()).decode('utf-8')

def create_zip_from_folder(folder_path):
    """将文件夹压缩为zip"""
    zip_path = os.path.join(OUTPUT_DIR, 'temp.zip')
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

def generate_qr_code(data, index, total, output_dir):
    """生成二维码"""
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )
    
    # 添加元数据
    metadata = {
        'index': index,
        'total': total,
        'data': data
    }
    qr.add_data(json.dumps(metadata))
    qr.make(fit=True)
    
    img = qr.make_image(fill_color="black", back_color="white")
    
    # 保存二维码
    qr_path = os.path.join(output_dir, f'qr_{index:03d}.png')
    img.save(qr_path)
    
    return qr_path

def process_file(file_path, is_folder=False):
    """处理文件或文件夹"""
    ensure_output_dir()
    
    print(f"正在处理: {file_path}")
    
    if is_folder:
        print("压缩文件夹...")
        file_path = create_zip_from_folder(file_path)
    
    # 转换为Base64
    print("转换为Base64编码...")
    base64_data = file_to_base64(file_path)
    
    # 分割数据
    print(f"分割数据（总大小: {len(base64_data)} 字符）...")
    chunks = split_data_into_chunks(base64_data, CHUNK_SIZE)
    
    # 生成二维码
    print(f"生成 {len(chunks)} 个二维码...")
    for i, chunk in enumerate(chunks):
        generate_qr_code(chunk, i, len(chunks), OUTPUT_DIR)
        print(f"进度: {i+1}/{len(chunks)}", end='\r')
    
    print(f"\n完成！二维码已保存到 {OUTPUT_DIR} 目录")
    print(f"总共生成 {len(chunks)} 个二维码")
    
    # 清理临时文件
    if is_folder and os.path.exists(file_path):
        os.remove(file_path)
        print("已清理临时文件")

def main():
    print("=" * 50)
    print("离线二维码生成器")
    print("=" * 50)
    
    if len(sys.argv) < 2:
        print("使用方法:")
        print("  单个文件: python generate_qr.py <文件路径>")
        print("  文件夹:   python generate_qr.py <文件夹路径> --folder")
        print("\n示例:")
        print("  python generate_qr.py document.pdf")
        print("  python generate_qr.py my_folder --folder")
        sys.exit(1)
    
    file_path = sys.argv[1]
    is_folder = '--folder' in sys.argv
    
    if not os.path.exists(file_path):
        print(f"错误: 文件不存在 - {file_path}")
        sys.exit(1)
    
    try:
        process_file(file_path, is_folder)
    except Exception as e:
        print(f"错误: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
