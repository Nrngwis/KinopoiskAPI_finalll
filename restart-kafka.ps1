# Скрипт для перезапуска Kafka с очисткой

Write-Host "🛑 Остановка всех контейнеров..." -ForegroundColor Yellow
docker-compose down

Write-Host "`n🗑️ Удаление старого volume Kafka..." -ForegroundColor Yellow
docker volume rm kinopoiskapi_kafka-data -ErrorAction SilentlyContinue

Write-Host "`n🚀 Запуск всех сервисов..." -ForegroundColor Green
docker-compose up -d

Write-Host "`n⏳ Ожидание запуска сервисов (30 секунд)..." -ForegroundColor Cyan
Start-Sleep -Seconds 30

Write-Host "`n📊 Статус контейнеров:" -ForegroundColor Green
docker-compose ps

Write-Host "`n✅ Готово! Проверьте Kafka UI: http://localhost:8081" -ForegroundColor Green
Write-Host "📝 Для просмотра логов используйте: docker-compose logs -f kafka" -ForegroundColor Cyan
