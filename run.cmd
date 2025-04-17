@echo off
@chcp -q -- "%~dp0" "%~dpnx0" >nul 2>&1
@chcp 65001 >nul 2>&1
if not exist "ollama-fat.jar" (
    echo "ollama-fat.jar bulunamadı, compile.cmd çalıştırılıyor..."
    call compile.cmd
) else (
    echo "ollama-fat.jar bulundu, çalıştırılıyor..."
)
java -jar ollama-fat.jar