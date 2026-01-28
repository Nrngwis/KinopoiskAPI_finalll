# ========== ЭТАП 1: СБОРКА ==========
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
# Скачиваем зависимости для кэширования
RUN mvn dependency:go-offline -B
COPY src ./src
# Сборка с пропуском тестов
RUN mvn clean package -DskipTests

# ========== ЭТАП 2: ЗАПУСК ==========
FROM eclipse-temurin:17-jre-alpine

# Создаем непривилегированного пользователя
RUN addgroup -S springgroup && adduser -S springuser -G springgroup

WORKDIR /app

# Копируем JAR файл с правильными правами
COPY --from=builder --chown=springuser:springgroup /app/target/KinopoiskAPI.jar app.jar

# Переключаемся на непривилегированного пользователя
USER springuser:springgroup

# ========== ПЕРЕМЕННЫЕ ОКРУЖЕНИЯ ==========
# Обязательные переменные (без значений по умолчанию)
ENV DB_URL=jdbc:mysql://mysql:3306/kinopoiskdb
ENV DB_USERNAME=root
# Пароль БД - ДОЛЖЕН БЫТЬ ПЕРЕДАН ПРИ ЗАПУСКЕ
ENV DB_PASSWORD=asd123

# API ключ для внешнего сервиса - ДОЛЖЕН БЫТЬ ПЕРЕДАН ПРИ ЗАПУСКЕ
ENV API_KEY=kinopoisk.api.key:45d26e72-1903-4a28-8482-d59a02b9b36a

# Настройки Kafka
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV KAFKA_TOPIC_MOVIES=movies
ENV KAFKA_TOPIC_RATINGS=ratings

# Оптимизированные настройки JVM
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Открываем порт приложения
EXPOSE 8080

# Команда запуска приложения
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]

# Активация профиля Docker
ENV SPRING_PROFILES_ACTIVE=docker