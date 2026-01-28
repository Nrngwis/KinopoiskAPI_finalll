# Скрипт для генерации и отправки тестовых фильмов в Kafka
param(
    [Parameter(Mandatory=$false)]
    [int]$Count = 10,

    [string]$Topic = "movie-daily-topic",

    [string]$BootstrapServer = "localhost:9092"
)

Write-Host "`n🎬 Генератор тестовых фильмов для Kafka" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray

# Проверка Kafka
Write-Host "🔍 Проверка подключения к Kafka..." -ForegroundColor Cyan
$kafkaCheck = docker exec kinopoisk-kafka kafka-broker-api-versions --bootstrap-server $BootstrapServer 2>$null
if (-not $kafkaCheck) {
    Write-Host "❌ Kafka не доступен! Запустите: docker-compose up -d kafka" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Kafka доступен" -ForegroundColor Green
Write-Host ""

# Данные для генерации
$movieNames = @(
    "Матрица", "Начало", "Интерстеллар", "Бойцовский клуб", "Форрест Гамп",
    "Криминальное чтиво", "Темный рыцарь", "Список Шиндлера", "Побег из Шоушенка",
    "Крестный отец", "Властелин колец", "Звездные войны", "Гладиатор", "Титаник",
    "Аватар", "Джокер", "Паразиты", "Зеленая миля", "Леон", "Престиж"
)

$genres = @("драма", "боевик", "фантастика", "триллер", "комедия", "детектив")
$descriptions = @(
    "Захватывающая история о",
    "Невероятное путешествие в мир",
    "Эпическая сага о",
    "Драматическая история о",
    "Увлекательный рассказ о"
)

Write-Host "📊 Параметры генерации:" -ForegroundColor Cyan
Write-Host "   Количество фильмов: $Count" -ForegroundColor White
Write-Host "   Топик: $Topic" -ForegroundColor White
Write-Host "   Сервер: $BootstrapServer" -ForegroundColor White
Write-Host ""

$successCount = 0
$errorCount = 0

Write-Host "🚀 Начинаем генерацию и отправку..." -ForegroundColor Cyan
Write-Host ""

for ($i = 1; $i -le $Count; $i++) {
    # Генерация случайных данных
    $randomMovie = $movieNames | Get-Random
    $randomGenre = $genres | Get-Random
    $randomYear = Get-Random -Minimum 1990 -Maximum 2025
    $randomRating = [math]::Round((Get-Random -Minimum 60 -Maximum 99) / 10, 1)
    $randomDesc = $descriptions | Get-Random

    # Создание JSON объекта
    $movie = @{
        id = $i
        filmId = 1000 + $i
        filmName = "$randomMovie (Тест #$i)"
        year = $randomYear
        rating = $randomRating.ToString()
        genre = $randomGenre
        description = "$randomDesc $randomGenre. Год выпуска: $randomYear"
    } | ConvertTo-Json -Compress

    # Отправка в Kafka
    try {
        # Экранируем кавычки для bash
        $escapedMovie = $movie -replace '"', '\"'

        # Отправляем через echo внутри контейнера
        docker exec kinopoisk-kafka bash -c "echo '$escapedMovie' | kafka-console-producer --bootstrap-server $BootstrapServer --topic $Topic" 2>&1 | Out-Null

        if ($LASTEXITCODE -eq 0) {
            Write-Host "   ✅ #$i : $randomMovie (рейтинг: $randomRating)" -ForegroundColor Green
            $successCount++
        } else {
            Write-Host "   ❌ #$i : Ошибка отправки" -ForegroundColor Red
            $errorCount++
        }
    } catch {
        Write-Host "   ❌ #$i : Исключение - $_" -ForegroundColor Red
        $errorCount++
    }

    # Небольшая задержка для имитации реальной нагрузки
    Start-Sleep -Milliseconds 50
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
Write-Host "📊 Статистика:" -ForegroundColor Cyan
Write-Host "   ✅ Успешно отправлено: $successCount" -ForegroundColor Green
if ($errorCount -gt 0) {
    Write-Host "   ❌ Ошибок: $errorCount" -ForegroundColor Red
}
Write-Host "   📈 Всего обработано: $Count" -ForegroundColor White
Write-Host ""

if ($successCount -gt 0) {
    Write-Host "💡 Для проверки сообщений используйте:" -ForegroundColor Cyan
    Write-Host "   .\read-kafka-messages.ps1 -Topic $Topic -FromBeginning" -ForegroundColor White
    Write-Host "   или откройте Kafka UI: http://localhost:8081" -ForegroundColor White
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
Write-Host ""
