# Експорт ЛОКАЛЬНИХ даних (Windows) для перенесення на сервер:
#   powershell -ExecutionPolicy Bypass -File scripts\export-local.ps1
# Результат — папка usty-export\ з db.dump (уся база) і uploads.tar.gz (усі фото з backend\uploads).
# Далі: scp -r usty-export <user>@<сервер>:~/usty/  і на сервері bash scripts/restore.sh usty-export
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$out = Join-Path $root 'usty-export'
New-Item -ItemType Directory -Force $out | Out-Null

# дамп робимо всередині контейнера й копіюємо файлом (перенаправлення > у PowerShell псує двійкові дані)
docker exec planning-db pg_dump -U planning -d planning -Fc -f /tmp/usty.dump
docker cp planning-db:/tmp/usty.dump (Join-Path $out 'db.dump')
docker exec planning-db rm /tmp/usty.dump

tar -czf (Join-Path $out 'uploads.tar.gz') -C (Join-Path $root 'backend\uploads') .

Get-ChildItem $out | Format-Table Name, @{ n = 'MB'; e = { [math]::Round($_.Length / 1MB, 1) } }
Write-Host "Готово: $out"
