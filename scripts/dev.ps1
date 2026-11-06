# 一键启动本地开发环境（Windows PowerShell）
# 前置：MySQL 已启动并创建 recipe_menu 库；Redis 可选。
# 用法：powershell -ExecutionPolicy Bypass -File scripts/dev.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

$javaHome = $env:JAVA_HOME
if (-not $javaHome -or -not (Test-Path "$javaHome\bin\java.exe")) {
    if (Test-Path 'D:\jdk-21.0.12.1\bin\java.exe') {
        $javaHome = 'D:\jdk-21.0.12.1'
    } else {
        throw '未找到 JDK 21，请设置 JAVA_HOME'
    }
}
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

Write-Host "启动后端 http://localhost:8080 ..." -ForegroundColor Cyan
Start-Process -FilePath 'mvn' -ArgumentList 'spring-boot:run' -WorkingDirectory "$root\backend" -WindowStyle Hidden

Write-Host "启动前端 http://localhost:5173 ..." -ForegroundColor Cyan
Start-Process -FilePath 'npm.cmd' -ArgumentList 'run','dev' -WorkingDirectory "$root\frontend" -WindowStyle Hidden

Start-Sleep -Seconds 8
Write-Host '完成。打开 http://localhost:5173 ，演示账号 demo / 123456' -ForegroundColor Green
Write-Host '后端 Swagger: http://localhost:8080/swagger-ui.html'
