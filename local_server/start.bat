@echo off
echo ========================================
echo 本地二维码转译服务器
echo ========================================
echo.
echo 正在启动服务器...
echo.

cd /d "%~dp0"

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Python，请先安装Python 3.6+
    pause
    exit /b 1
)

REM 安装依赖
echo 正在检查依赖...
pip install flask qrcode -q

REM 启动服务器
echo.
echo 服务器启动成功！
echo 请在浏览器中打开: http://localhost:5000
echo.
echo 按 Ctrl+C 停止服务器
echo ========================================
echo.

python app.py

pause
