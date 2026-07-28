@echo off
cd frontend
echo Starting frontend server on http://localhost:8080
start http://localhost:8080
python -m http.server 8080
