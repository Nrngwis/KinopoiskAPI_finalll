# Скрипт для отправки сообщения в Kafka
param(
    [Parameter(Mandatory=$false)]
    [string]$Topic = "movie-daily-topic",

    [Parameter(Mandatory=$false)]
    [string]$Message,

    [string]$BootstrapServer = "localhost:9092"
)

Write-Host "`n📨 Отправка сообщения в Kafka" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray

# Если сообщение не указано, используем тестовое
if (-not $Message) {
    $Message = @{
        id = 1
        filmId = 12345
        filmName = "Тестовый фильм"
        year = 2024
        rating = "8.5"
        description = "Автоматически сгенерированное тестовое сообщение"
        genre = "боевик"
    } | ConvertTo-Json -Compress

    Write-Host "⚠️  Сообщение не указано, используется тестовое" -ForegroundColor Yellow
}

Write-Host "   Топик: $Topic" -ForegroundColor White
Write-Host "   Сервер: $BootstrapServer" -ForegroundColor White
Write-Host "   Сообщение: $Message" -ForegroundColor White
Write-Host ""

# Проверка, что Kafka запущен
Write-Host "🔍 Проверка подключения к Kafka..." -ForegroundColor Cyan
$kafkaCheck = docker exec kinopoisk-kafka kafka-broker-api-versions --bootstrap-server $BootstrapServer 2>$null
if (-not $kafkaCheck) {
    Write-Host "❌ Kafka не доступен! Убедитесь, что контейнер запущен:" -ForegroundColor Red
    Write-Host "   docker-compose up -d kafka" -ForegroundColor Yellow
    exit 1
}
Write-Host "✅ Kafka доступен" -ForegroundColor Green
Write-Host ""

# Отправка сообщения
Write-Host "📤 Отправка..." -ForegroundColor Cyan
try {
    # Экранируем кавычки для bash
    $escapedMessage = $Message -replace '"', '\"'

    # Отправляем через echo внутри контейнера
    docker exec kinopoisk-kafka bash -c "echo '$escapedMessage' | kafka-console-producer --bootstrap-server $BootstrapServer --topic $Topic" 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Сообщение успешно отправлено в топик '$Topic'!" -ForegroundColor Green
        Write-Host ""
        Write-Host "💡 Для проверки используйте:" -ForegroundColor Cyan
        Write-Host "   .\read-kafka-messages.ps1 -Topic $Topic" -ForegroundColor White
        Write-Host "   или откройте Kafka UI: http://localhost:8081" -ForegroundColor White
    } else {
        Write-Host "❌ Ошибка отправки сообщения" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Ошибка: $_" -ForegroundColor Red
    exit 1
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
