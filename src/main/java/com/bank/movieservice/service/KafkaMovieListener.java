package com.bank.movieservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            // 1. Парсим JSON
            MovieData movieData = parseMovieJson(movieJson);

            if (movieData == null) {
                System.err.println("⚠️ Не удалось распарсить JSON: " + movieJson);
                return;
            }

            // 2. Добавляем в список
            movies.add(movieData);
            System.out.println("📥 Получен фильм: " + movieData.filmName + " (всего в буфере: " + movies.size() + ")");

            // 3. Если набралось достаточно фильмов - отправляем email
            if (movies.size() >= batchSize) {
                sendToEmail();
            }
        } catch (Exception e) {
            System.err.println("❌ Ошибка обработки сообщения Kafka: " + e.getMessage());
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private MovieData parseMovieJson(String movieJson) {
        try {
            // Удаляем BOM (Byte Order Mark) если он есть
            if (movieJson != null && movieJson.startsWith("\uFEFF")) {
                movieJson = movieJson.substring(1);
            }

            // Также удаляем любые невидимые символы в начале и конце
            movieJson = movieJson.trim();

            JsonNode node = objectMapper.readTree(movieJson);

            MovieData data = new MovieData();
            data.id = node.has("id") ? node.get("id").asLong() : null;
            data.filmId = node.has("filmId") ? node.get("filmId").asLong() : null;
            data.filmName = node.has("filmName") ? node.get("filmName").asText() : "Unknown";
            data.year = node.has("year") ? node.get("year").asInt() : null;
            data.rating = node.has("rating") && !node.get("rating").isNull()
                         ? node.get("rating").asText() : "N/A";
            data.description = node.has("description") ? node.get("description").asText() : "";

            // Парсим жанры
            if (node.has("genres") && node.get("genres").isArray()) {
                StringBuilder genresStr = new StringBuilder();
                node.get("genres").forEach(genre -> {
                    if (genresStr.length() > 0) genresStr.append(", ");
                    genresStr.append(genre.asText());
                });
                data.genres = genresStr.toString();
            } else {
                data.genres = "";
            }

            return data;
        } catch (Exception e) {
            System.err.println("❌ Ошибка парсинга JSON: " + e.getMessage());
            return null;
        }
    }

    public void sendToEmail() {
        if (movies.isEmpty()) {
            System.out.println("⚠️ Нет фильмов для отправки");
            return;
        }

        try {
            // Формируем отчет и отправляем через EmailService
            String report = generateReport(movies);
            String subject = emailSubjectPrefix + " - " + java.time.LocalDate.now();

            emailService.sendReportByEmail(recipientEmail, subject, report, "csv");

            System.out.println("✅ Отправлен email с " + movies.size() + " фильмами на адрес: " + recipientEmail);

            // Очищаем список после отправки
            movies.clear();
        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateReport(List<MovieData> movieList) {
        StringBuilder csv = new StringBuilder();
        csv.append("filmId,filmName,year,rating,description,genres\n");

        for (MovieData movie : movieList) {
            csv.append(String.format("%s,\"%s\",%s,%s,\"%s\",\"%s\"\n",
                    movie.filmId != null ? movie.filmId : "",
                    escapeCsv(movie.filmName),
                    movie.year != null ? movie.year : "",
                    movie.rating,
                    escapeCsv(movie.description),
                    escapeCsv(movie.genres)
            ));
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    // Внутренний класс для хранения данных о фильме
    private static class MovieData {
        Long id;
        Long filmId;
        String filmName;
        Integer year;
        String rating;
        String description;
        String genres;
    }
}
