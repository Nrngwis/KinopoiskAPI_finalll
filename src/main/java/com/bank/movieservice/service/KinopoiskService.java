package com.bank.movieservice.service;

import com.bank.movieservice.DTO.response.KinopoiskResponse;
import com.bank.movieservice.entity.Genre;
import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.repository.GenreRepository;
import com.bank.movieservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KinopoiskService {

    private final RestTemplate restTemplate;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    @Value("${kinopoisk.api.key:45d26e72-1903-4a28-8482-d59a02b9b36a}")
    private String apiKey;

    @Value("${kinopoisk.api.url:https://kinopoiskapiunofficial.tech/api/v2.2/films}")
    private String apiUrl;

    // Получить детали фильма по ID (включая жанры)
    private KinopoiskResponse.Film getFilmDetails(Long filmId) {
        String detailsUrl = "https://kinopoiskapiunofficial.tech/api/v2.2/films/" + filmId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<KinopoiskResponse.Film> response = restTemplate.exchange(
                    detailsUrl,
                    HttpMethod.GET,
                    entity,
                    KinopoiskResponse.Film.class
            );

            return response.getBody();
        } catch (Exception e) {
            System.err.println("❌ Ошибка получения деталей фильма " + filmId + ": " + e.getMessage());
            return null;
        }
    }

    public List<Movie> searchAndSaveFilms(String keyword, Integer yearFrom, Integer yearTo,
                                          Double ratingFrom, Double ratingTo) {

        // Строим URL для запроса к Кинопоиску
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl);

        // Если keyword передан, используем его для поиска (может быть жанр или название)
        if (keyword != null) {
            builder.queryParam("keyword", keyword);
        }
        if (yearFrom != null) builder.queryParam("yearFrom", yearFrom);
        if (yearTo != null) builder.queryParam("yearTo", yearTo);
        if (ratingFrom != null) builder.queryParam("ratingFrom", ratingFrom);
        if (ratingTo != null) builder.queryParam("ratingTo", ratingTo);

        // Добавляем сортировку по рейтингу для получения лучших фильмов
        builder.queryParam("order", "RATING");
        builder.queryParam("type", "ALL");

        // Настройка заголовков
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Запрос к Кинопоиску
            ResponseEntity<KinopoiskResponse> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    KinopoiskResponse.class
            );

            KinopoiskResponse kinopoiskResponse = response.getBody();
            if (kinopoiskResponse == null || kinopoiskResponse.getItems() == null) {
                return Collections.emptyList();
            }

            // Фильтруем фильмы, которых нет в базе
            List<KinopoiskResponse.Film> newFilms = kinopoiskResponse.getItems().stream()
                    .filter(film -> !movieRepository.existsByFilmId(film.getKinopoiskId()))
                    .collect(Collectors.toList());

            System.out.println("📊 Найдено новых фильмов: " + newFilms.size());

            // Получаем детали для каждого фильма (чтобы получить жанры)
            List<KinopoiskResponse.Film> filmsWithDetails = newFilms.stream()
                    .map(film -> {
                        KinopoiskResponse.Film details = getFilmDetails(film.getKinopoiskId());
                        if (details != null) {
                            System.out.println("✅ Получены детали для: " + details.getNameRu() +
                                    " (жанры: " + (details.getGenres() != null ? details.getGenres().size() : 0) + ")");
                            return details;
                        }
                        System.out.println("⚠️ Не удалось получить детали для фильма ID: " + film.getKinopoiskId());
                        return film; // Возвращаем оригинал без жанров
                    })
                    .collect(Collectors.toList());

            // Сохраняем новые фильмы
            List<Movie> savedMovies = newFilms.stream()
                    .map(film -> {
                        Movie movie = new Movie(
                                film.getKinopoiskId(),
                                film.getNameRu(),
                                film.getYear(),
                                film.getRatingKinopoisk() != null ?
                                        BigDecimal.valueOf(film.getRatingKinopoisk()) : null,
                                film.getDescription()
                        );

                        // Обрабатываем жанры
                        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
                            Set<Genre> genres = film.getGenres().stream()
                                    .map(genreDto -> {
                                        String genreName = genreDto.getGenre();
                                        // Ищем существующий жанр или создаем новый
                                        return genreRepository.findByName(genreName)
                                                .orElseGet(() -> {
                                                    Genre newGenre = new Genre(genreName);
                                                    return genreRepository.save(newGenre);
                                                });
                                    })
                                    .collect(Collectors.toSet());
                            movie.setGenres(genres);
                        }

                        return movie;
                    })
                    .map(movieRepository::save)
                    .collect(Collectors.toList());

            return savedMovies;

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}