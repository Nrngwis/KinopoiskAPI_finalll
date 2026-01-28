package com.bank.movieservice.service;

import com.bank.movieservice.entity.Movie;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MovieSchedulerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KinopoiskService kinopoiskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kafka.topics.movie-daily:movie-topic}")
    private String movieTopic;

    @Value("${daily-genre.schedule.MONDAY:драма}")
    private String mondayGenre;

    @Value("${daily-genre.schedule.TUESDAY:комедия}")
    private String tuesdayGenre;

    @Value("${daily-genre.schedule.WEDNESDAY:боевик}")
    private String wednesdayGenre;

    @Value("${daily-genre.schedule.THURSDAY:фантастика}")
    private String thursdayGenre;

    @Value("${daily-genre.schedule.FRIDAY:триллер}")
    private String fridayGenre;

    @Value("${daily-genre.schedule.SATURDAY:приключения}")
    private String saturdayGenre;

    @Value("${daily-genre.schedule.SUNDAY:мультфильм}")
    private String sundayGenre;

    @Scheduled(cron = "${scheduler.cron:0 0 7 * * *}") // Каждый день в 7 утра
    public void sendToKafka() {
        try {
            // 1. Определяем жанр по дню недели
            String genre = getGenreForToday();
            System.out.println("📅 Запуск ежедневного планировщика. День: " + LocalDate.now().getDayOfWeek() + ", Жанр: " + genre);

            // 2. Запрашиваем фильмы из Kinopoisk API
            List<Movie> movies = kinopoiskService.searchAndSaveFilms(genre, null, null, 7.0, null);

            if (movies.isEmpty()) {
                System.out.println("⚠️ Не найдено новых фильмов для жанра: " + genre);
                return;
            }

            // 3. Отправляем каждый фильм в Kafka
            int sentCount = 0;
            for (Movie movie : movies) {
                try {
                    String movieJson = convertMovieToJson(movie);
                    kafkaTemplate.send(movieTopic, movieJson);
                    sentCount++;
                } catch (JsonProcessingException e) {
                    System.err.println("❌ Ошибка сериализации фильма ID=" + movie.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("✅ Отправлено " + sentCount + " фильмов в Kafka топик: " + movieTopic);

        } catch (Exception e) {
            System.err.println("❌ Ошибка в планировщике: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getGenreForToday() {
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        Map<DayOfWeek, String> genreMap = new HashMap<>();
        genreMap.put(DayOfWeek.MONDAY, mondayGenre);
        genreMap.put(DayOfWeek.TUESDAY, tuesdayGenre);
        genreMap.put(DayOfWeek.WEDNESDAY, wednesdayGenre);
        genreMap.put(DayOfWeek.THURSDAY, thursdayGenre);
        genreMap.put(DayOfWeek.FRIDAY, fridayGenre);
        genreMap.put(DayOfWeek.SATURDAY, saturdayGenre);
        genreMap.put(DayOfWeek.SUNDAY, sundayGenre);

        return genreMap.getOrDefault(today, "драма");
    }

    private String convertMovieToJson(Movie movie) throws JsonProcessingException {
        Map<String, Object> movieData = new HashMap<>();
        movieData.put("id", movie.getId());
        movieData.put("filmId", movie.getFilmId());
        movieData.put("filmName", movie.getFilmName());
        movieData.put("year", movie.getYear());
        movieData.put("rating", movie.getRating());
        movieData.put("description", movie.getDescription());

        // Добавляем жанры
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            List<String> genreNames = movie.getGenres().stream()
                    .map(genre -> genre.getName())
                    .collect(java.util.stream.Collectors.toList());
            movieData.put("genres", genreNames);
        } else {
            movieData.put("genres", new java.util.ArrayList<>());
        }

        return objectMapper.writeValueAsString(movieData);
    }
}
