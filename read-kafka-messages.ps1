# Скрипт для чтения сообщений из Kafka
param(
    [Parameter(Mandatory=$false)]
    [string]$Topic = "movie-daily-topic",

    [string]$BootstrapServer = "localhost:9092",

    [switch]$FromBeginning,

    [switch]$WithMetadata
)

Write-Host "`n📥 Чтение сообщений из Kafka" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
Write-Host "   Топик: $Topic" -ForegroundColor White
Write-Host "   Сервер: $BootstrapServer" -ForegroundColor White

if ($FromBeginning) {
    Write-Host "   Режим: С начала топика" -ForegroundColor Yellow
} else {
    Write-Host "   Режим: Только новые сообщения" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "💡 Для выхода нажмите Ctrl+C" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor DarkGray
Write-Host ""

# Формирование команды
$command = "docker exec -it kinopoisk-kafka kafka-console-consumer --bootstrap-server $BootstrapServer --topic $Topic"

if ($FromBeginning) {
    $command += " --from-beginning"
}

if ($WithMetadata) {
    $command += " --property print.timestamp=true --property print.partition=true --property print.offset=true --property print.key=true"
}

# Выполнение
Invoke-Expression $command
