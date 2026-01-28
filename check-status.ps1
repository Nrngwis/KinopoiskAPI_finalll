# Скрипт для проверки статуса всех сервисов

Write-Host "`n📊 Статус Docker контейнеров:" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
docker-compose ps

Write-Host "`n🔍 Проверка доступности сервисов:" -ForegroundColor Yellow
Write-Host "================================" -ForegroundColor Yellow

# Проверка Spring Boot приложения
Write-Host "`n1. Spring Boot API (http://localhost:8080):" -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✅ Приложение работает!" -ForegroundColor Green
    }
} catch {
    Write-Host "   ⚠️ Приложение не отвечает (возможно, нет /actuator/health endpoint)" -ForegroundColor Yellow
    Write-Host "   Попробуйте: http://localhost:8080" -ForegroundColor Cyan
}

# Проверка Kafka UI
Write-Host "`n2. Kafka UI (http://localhost:8081):" -ForegroundColor Green
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081" -TimeoutSec 5 -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✅ Kafka UI работает!" -ForegroundColor Green
    }
} catch {
    Write-Host "   ❌ Kafka UI не доступен" -ForegroundColor Red
}

# Проверка MySQL
Write-Host "`n3. MySQL (localhost:3307):" -ForegroundColor Green
$mysqlStatus = docker exec kinopoisk-mysql mysqladmin ping -h localhost 2>$null
if ($mysqlStatus -like "*alive*") {
    Write-Host "   ✅ MySQL работает!" -ForegroundColor Green
} else {
    Write-Host "   ❌ MySQL не доступен" -ForegroundColor Red
}

# Проверка Kafka
Write-Host "`n4. Kafka (localhost:9092):" -ForegroundColor Green
$kafkaTopics = docker exec kinopoisk-kafka kafka-topics --list --bootstrap-server localhost:9092 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "   ✅ Kafka работает!" -ForegroundColor Green
    if ($kafkaTopics) {
        Write-Host "   📋 Топики: $kafkaTopics" -ForegroundColor Cyan
    }
} else {
    Write-Host "   ❌ Kafka не доступен" -ForegroundColor Red
}

Write-Host "`n📝 Логи Spring Boot приложения (последние 20 строк):" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
docker logs kinopoisk-api --tail 20

Write-Host "`n💡 Полезные команды:" -ForegroundColor Yellow
Write-Host "   docker-compose logs -f kinopoisk-api  # Логи приложения" -ForegroundColor White
Write-Host "   docker-compose logs -f kafka          # Логи Kafka" -ForegroundColor White
Write-Host "   docker-compose restart kinopoisk-api  # Перезапуск приложения" -ForegroundColor White
Write-Host "   docker-compose down                   # Остановить все" -ForegroundColor White
