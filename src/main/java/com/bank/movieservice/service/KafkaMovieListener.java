package com.bank.movieservice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class KafkaMovieListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaMovieListener.class);

    private final EmailService emailService;
    private final ObjectMapper objectMapper; // Внедряем через конструктор

    // Thread-safe список для хранения фильмов
    private final List<MovieData> movies = new ArrayList<>();
    private final Lock lock = new ReentrantLock();

    @Value("${email.daily-report.recipient:mym99527@gmail.com}")
    private String recipientEmail;

    @Value("${email.daily-report.subject-prefix:🎬 Ежедневные фильмы}")
    private String emailSubjectPrefix;

    @Value("${kafka.consumer.batch-size:50}")
    private int batchSize;

    @KafkaListener(topics = "${kafka.topics.movie-daily:movie-topic}",
            groupId = "${kafka.consumer.group-id:movie-email-consumer-group}")
    public void consumeMovie(String movieJson) {
        lock.lock();
        try {
            // 1. Парсим JSON с использованием ObjectMapper.readValue()
            MovieData movieData = parseMovieJson(movieJson);

            if (movieData == null) {
                log.warn("⚠️ Не удалось распарсить JSON, пропускаем сообщение");
                return;
            }

            // 2. Добавляем в список
            movies.add(movieData);
            log.info("📥 Получен фильм: {} (всего в буфере: {})",
                    movieData.getFilmName(), movies.size());

            // 3. Если набралось достаточно фильмов - отправляем email
            if (movies.size() >= batchSize) {
                sendToEmail();
            }
        } catch (Exception e) {
            log.error("❌ Ошибка обработки сообщения Kafka", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Парсинг JSON с использованием ObjectMapper.readValue()
     * Вместо ручного разбора через JsonNode
     */
    private MovieData parseMovieJson(String movieJson) {
        if (movieJson == null || movieJson.trim().isEmpty()) {
            log.warn("Получен пустой JSON");
            return null;
        }

        try {
            // Очистка JSON от BOM и лишних пробелов
            String cleanedJson = cleanJsonString(movieJson);

            // ОСНОВНОЕ ИЗМЕНЕНИЕ: используем readValue() вместо readTree()
            // ObjectMapper сам преобразует JSON в объект MovieData
            // Вся логика обработки null и преобразований теперь в геттерах/сеттерах MovieData
            return objectMapper.readValue(cleanedJson, MovieData.class);

        } catch (JsonProcessingException e) {
            log.error("❌ Ошибка парсинга JSON: {}", e.getMessage());
            log.debug("Проблемный JSON: {}",
                    getJsonPreview(movieJson));
            return null;
        } catch (Exception e) {
            log.error("❌ Неожиданная ошибка при парсинге JSON", e);
            return null;
        }
    }

    private String getJsonPreview(String json) {
        if (json == null) return "null";
        int previewLength = Math.min(json.length(), 500);
        return json.substring(0, previewLength) + (json.length() > 500 ? "..." : "");
    }

    /**
     * Очистка JSON строки от BOM и лишних символов
     */
    private String cleanJsonString(String json) {
        if (json == null) return "";

        // Удаляем BOM (Byte Order Mark) если он есть
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1);
        }

        // Удаляем невидимые символы и обрезаем пробелы
        return json.trim();
    }

    public void sendToEmail() {
        if (movies.isEmpty()) {
            log.info("⚠️ Нет фильмов для отправки");
            return;
        }

        try {
            // Формируем отчет и отправляем через EmailService
            String report = generateReport(movies);
            String subject = emailSubjectPrefix + " - " + java.time.LocalDate.now();

            emailService.sendReportByEmail(recipientEmail, subject, report, "csv");

            log.info("✅ Отправлен email с {} фильмами на адрес: {}",
                    movies.size(), recipientEmail);

            // Очищаем список после отправки
            movies.clear();
        } catch (Exception e) {
            log.error("❌ Ошибка отправки email", e);
        }
    }

    private String generateReport(List<MovieData> movieList) {
        StringBuilder csv = new StringBuilder();
        csv.append("filmId,filmName,year,rating,description,genres\n");

        for (MovieData movie : movieList) {
            csv.append(String.format("%s,\"%s\",%s,%s,\"%s\",\"%s\"\n",
                    movie.getFilmId() != null ? movie.getFilmId() : "",
                    escapeCsv(movie.getFilmName()),
                    movie.getYear() != null ? movie.getYear() : "",
                    movie.getRating(),
                    escapeCsv(movie.getDescription()),
                    escapeCsv(movie.getGenres())
            ));
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    /**
     * DTO класс для представления данных фильма
     * ВСЯ логика обработки null и преобразований теперь в геттерах
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true) // Игнорируем неизвестные поля в JSON
    public static class MovieData {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("filmId")
        private Long filmId;

        @JsonProperty("filmName")
        private String filmName;

        @JsonProperty("year")
        private Integer year;

        @JsonProperty("rating")
        private String rating;

        @JsonProperty("description")
        private String description;

        @JsonProperty("genres")
        private List<String> genres; // Храним как List<String>, а не как строку

        // ========== КАСТОМНЫЕ ГЕТТЕРЫ ДЛЯ ОБРАБОТКИ NULL ==========

        /**
         * Геттер для названия фильма с обработкой null
         */
        public String getFilmName() {
            return filmName != null && !filmName.trim().isEmpty()
                    ? filmName.trim()
                    : "Unknown";
        }

        /**
         * Геттер для рейтинга с обработкой null
         */
        public String getRating() {
            if (rating == null || rating.trim().isEmpty()) {
                return "N/A";
            }

            String trimmedRating = rating.trim();

            // Пытаемся проверить, что рейтинг - это число
            try {
                // Убираем возможные постфиксы типа "/10"
                String numericPart = trimmedRating;
                if (trimmedRating.contains("/")) {
                    numericPart = trimmedRating.substring(0, trimmedRating.indexOf('/')).trim();
                }

                Double.parseDouble(numericPart);
                return trimmedRating;
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }

        /**
         * Геттер для описания с обработкой null
         */
        public String getDescription() {
            return description != null ? description : "";
        }

        /**
         * Геттер для жанров в виде строки (через запятую)
         */
        public String getGenres() {
            if (genres == null || genres.isEmpty()) {
                return "";
            }

            // Фильтруем null и пустые строки, тримим каждую строку
            List<String> validGenres = new ArrayList<>();
            for (String genre : genres) {
                if (genre != null && !genre.trim().isEmpty()) {
                    validGenres.add(genre.trim());
                }
            }

            return String.join(", ", validGenres);
        }

        // ========== КАСТОМНЫЕ СЕТТЕРЫ ДЛЯ ОБРАБОТКИ ВХОДНЫХ ДАННЫХ ==========

        /**
         * Сеттер для жанров с обработкой разных форматов
         */
        public void setGenres(Object genres) {
            this.genres = new ArrayList<>();

            if (genres == null) {
                return;
            }

            // Если genres уже List<String>
            if (genres instanceof List) {
                for (Object item : (List<?>) genres) {
                    if (item != null) {
                        this.genres.add(item.toString().trim());
                    }
                }
            }
            // Если genres пришла как строка (например, "Action, Drama")
            else if (genres instanceof String) {
                String genresStr = ((String) genres).trim();
                if (!genresStr.isEmpty()) {
                    String[] genreArray = genresStr.split(",");
                    for (String genre : genreArray) {
                        String trimmedGenre = genre.trim();
                        if (!trimmedGenre.isEmpty()) {
                            this.genres.add(trimmedGenre);
                        }
                    }
                }
            }
        }

        /**
         * Сеттер для рейтинга с базовой очисткой
         */
        public void setRating(String rating) {
            if (rating != null) {
                this.rating = rating.trim();
            } else {
                this.rating = null;
            }
        }

        /**
         * Сеттер для названия с базовой очисткой
         */
        public void setFilmName(String filmName) {
            if (filmName != null) {
                this.filmName = filmName.trim();
            } else {
                this.filmName = null;
            }
        }

        /**
         * Сеттер для описания с базовой очисткой
         */
        public void setDescription(String description) {
            if (description != null) {
                this.description = description.trim();
            } else {
                this.description = null;
            }
        }

        // ========== ДОПОЛНИТЕЛЬНЫЕ УДОБНЫЕ МЕТОДЫ ==========

        /**
         * Проверка, что фильм валиден (имеет хотя бы название или ID)
         */
        public boolean isValid() {
            return (filmId != null) ||
                    (filmName != null && !filmName.trim().isEmpty() && !"Unknown".equals(getFilmName()));
        }

        /**
         * Получить жанры как List (оригинальный формат)
         */
        public List<String> getGenresList() {
            return genres != null ? new ArrayList<>(genres) : new ArrayList<>();
        }

        /**
         * Получить числовое значение рейтинга (если возможно)
         */
        public Double getRatingAsDouble() {
            try {
                String ratingStr = getRating();
                if ("N/A".equals(ratingStr)) {
                    return null;
                }

                // Убираем нечисловые символы (кроме точки)
                String numericRating = ratingStr.replaceAll("[^\\d.]", "");
                return Double.parseDouble(numericRating);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
