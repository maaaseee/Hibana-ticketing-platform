# Hibana.tk Ticketing Platform - Unified Dev Runner with Tilt Web UI
# Runs containerized microservices (Gateway, Catalog, Inventory, Orders, Frontend, Postgres, Redis, Mailpit, Floci-AZ)

Write-Host "Iniciando plataforma Hibana.tk con Tilt Web UI..." -ForegroundColor Cyan
Write-Host "Web Dashboard de Tilt: http://localhost:10350" -ForegroundColor Green
Write-Host "B2C Frontend:          http://localhost:5173" -ForegroundColor Green
Write-Host "API Gateway:           http://localhost:8080" -ForegroundColor Green
Write-Host "Mailpit Web:           http://localhost:8025" -ForegroundColor Green

tilt up
