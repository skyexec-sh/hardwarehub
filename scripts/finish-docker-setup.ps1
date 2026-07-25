# HardwareHub — finish Docker setup after reboot
# Run in an elevated PowerShell if needed:
#   powershell -ExecutionPolicy Bypass -File .\scripts\finish-docker-setup.ps1

$ErrorActionPreference = "Continue"
Write-Host "== HardwareHub Docker post-reboot setup ==" -ForegroundColor Cyan

Write-Host "1) Ensuring WSL is installed..."
wsl --install --no-distribution --web-download
wsl --set-default-version 2
wsl --update

Write-Host "2) Installing Ubuntu distro (if missing)..."
$distros = wsl -l -q 2>$null
if (-not ($distros | Where-Object { $_ -match "Ubuntu" })) {
    wsl --install -d Ubuntu --no-launch
}

Write-Host "3) Starting Docker Desktop..."
$dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
if (Test-Path $dockerDesktop) {
    Start-Process $dockerDesktop
} else {
    Write-Error "Docker Desktop not found at $dockerDesktop"
    exit 1
}

Write-Host "4) Waiting for Docker engine..."
$deadline = (Get-Date).AddMinutes(5)
do {
    Start-Sleep -Seconds 5
    docker info 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) { break }
    Write-Host "  still starting..."
} while ((Get-Date) -lt $deadline)

if ($LASTEXITCODE -ne 0) {
    Write-Error "Docker engine did not become ready. Open Docker Desktop and check Settings > General > Use WSL 2 based engine."
    exit 1
}

Write-Host "5) Docker is ready. Bringing up HardwareHub (Postgres first)..."
Set-Location (Split-Path $PSScriptRoot -Parent)
docker compose up -d postgres
docker compose ps

Write-Host ""
Write-Host "Done. Next:" -ForegroundColor Green
Write-Host "  docker compose up --build"
Write-Host "  Web UI: http://localhost:8088"
Write-Host "  API:    http://localhost:8080/api/v1"
Write-Host "  Login:  owner / Owner@123"
