# Быстрый тест Kafka - отправка и чтение сообщения
param(
    [string]$Topic = "movie-daily-topic"
)

Write-Host "`n🧪 Быстрый тест Kafka" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray

# 1. Проверка Kafka
Write-Host "`n1️⃣  Проверка подключения к Kafka..." -ForegroundColor Yellow
$kafkaCheck = docker exec kinopoisk-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 2>$null
if (-not $kafkaCheck) {
    Write-Host "   ❌ Kafka не доступен!" -ForegroundColor Red
    Write-Host "   💡 Запустите: docker-compose up -d kafka" -ForegroundColor Yellow
    exit 1
}
Write-Host "   ✅ Kafka работает" -ForegroundColor Green

# 2. Проверка топика
Write-Host "`n2️⃣  Проверка топика '$Topic'..." -ForegroundColor Yellow
$topics = docker exec kinopoisk-kafka kafka-topics --list --bootstrap-server localhost:9092 2>$null
if ($topics -match $Topic) {
    Write-Host "   ✅ Топик существует" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Топик не найден, будет создан автоматически" -ForegroundColor Yellow
}

# 3. Отправка тестового сообщения
Write-Host "`n3️⃣  Отправка тестового сообщения..." -ForegroundColor Yellow
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$testMessage = @{
    id = 999
    filmId = 99999
    filmName = "Тестовый фильм (Quick Test)"
    year = 2024
    rating = "9.0"
    description = "Автоматическое тестовое сообщение, отправлено в $timestamp"
    timestamp = $timestamp
} | ConvertTo-Json -Compress

try {
    # Экранируем кавычки для bash
    $escapedMessage = $testMessage -replace '"', '\"'

    # Отправляем через echo внутри контейнера
    docker exec kinopoisk-kafka bash -c "echo '$escapedMessage' | kafka-console-producer --bootstrap-server localhost:9092 --topic $Topic" 2>&1 | Out-Null

    if ($LASTEXITCODE -eq 0) {
        Write-Host "   ✅ Сообщение отправлено" -ForegroundColor Green
    } else {
        Write-Host "   ❌ Ошибка отправки" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "   ❌ Исключение: $_" -ForegroundColor Red
    exit 1
}

# 4. Чтение последнего сообщения
Write-Host "`n4️⃣  Чтение последнего сообщения..." -ForegroundColor Yellow
Write-Host "   (ожидание 2 секунды для обработки...)" -ForegroundColor DarkGray
Start-Sleep -Seconds 2

Write-Host ""
Write-Host "   📥 Последние сообщения из топика:" -ForegroundColor Cyan
Write-Host "   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray

# Читаем последние 5 сообщений
$job = Start-Job -ScriptBlock {
    param($Topic)
    docker exec kinopoisk-kafka kafka-console-consumer `
        --bootstrap-server localhost:9092 `
        --topic $Topic `
        --from-beginning `
        --max-messages 5 `
        --timeout-ms 3000 2>$null
} -ArgumentList $Topic

Wait-Job $job -Timeout 5 | Out-Null
$messages = Receive-Job $job
Remove-Job $job -Force

if ($messages) {
    $messages | ForEach-Object {
        Write-Host "   $_" -ForegroundColor White
    }
} else {
    Write-Host "   ⚠️  Сообщения не получены (возможно, consumer group уже прочитал их)" -ForegroundColor Yellow
}

# 5. Статистика топика
Write-Host "`n5️⃣  Статистика топика..." -ForegroundColor Yellow
$topicInfo = docker exec kinopoisk-kafka kafka-topics `
    --bootstrap-server localhost:9092 `
    --describe `
    --topic $Topic 2>$null

if ($topicInfo) {
    Write-Host "   $topicInfo" -ForegroundColor White
}

# 6. Consumer Groups
Write-Host "`n6️⃣  Consumer Groups..." -ForegroundColor Yellow
$consumerGroups = docker exec kinopoisk-kafka kafka-consumer-groups `
    --bootstrap-server localhost:9092 `
    --list 2>$null

if ($consumerGroups) {
    Write-Host "   Активные группы:" -ForegroundColor Cyan
    $consumerGroups | ForEach-Object {
        Write-Host "   - $_" -ForegroundColor White
    }
} else {
    Write-Host "   ⚠️  Consumer groups не найдены" -ForegroundColor Yellow
}

# Итоги
Write-Host "`n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
Write-Host "✅ Тест завершен успешно!" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Полезные команды:" -ForegroundColor Cyan
Write-Host "   .\send-kafka-message.ps1              # Отправить сообщение" -ForegroundColor White
Write-Host "   .\read-kafka-messages.ps1             # Читать сообщения" -ForegroundColor White
Write-Host "   .\generate-test-movies.ps1 -Count 50  # Сгенерировать 50 фильмов" -ForegroundColor White
Write-Host "   http://localhost:8081                 # Kafka UI" -ForegroundColor White
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
Write-Host ""
