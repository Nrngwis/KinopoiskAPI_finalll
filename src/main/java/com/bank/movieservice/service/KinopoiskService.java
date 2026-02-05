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
import org.springframework.transaction.annotation.Transactional;
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

    // Карта соответствия названий жанров и их ID в API Кинопоиска
    private static final Map<String, Integer> GENRE_MAP = Map.ofEntries(
            Map.entry("триллер", 1),
            Map.entry("драма", 2),
            Map.entry("криминал", 3),
            Map.entry("мелодрама", 4),
            Map.entry("детектив", 5),
            Map.entry("фантастика", 6),
            Map.entry("приключения", 7),
            Map.entry("боевик", 8),
            Map.entry("фэнтези", 9),
            Map.entry("комедия", 10),
            Map.entry("военный", 11),
            Map.entry("история", 12),
            Map.entry("музыка", 13),
            Map.entry("ужасы", 14),
            Map.entry("семейный", 15),
            Map.entry("мультфильм", 16),
            Map.entry("мюзикл", 17),
            Map.entry("спорт", 18),
            Map.entry("документальный", 19),
            Map.entry("короткометражка", 20),
            Map.entry("аниме", 21),
            Map.entry("биография", 22),
            Map.entry("вестерн", 23),
            Map.entry("фильм-нуар", 24),
            Map.entry("церемония", 25),
            Map.entry("реальное тв", 26),
            Map.entry("ток-шоу", 27),
            Map.entry("игра", 28),
            Map.entry("новости", 29),
            Map.entry("концерт", 30),
            Map.entry("для взрослых", 31),
            Map.entry("детский", 32)
    );

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

    /**
     * Основной метод поиска и сохранения фильмов
     * @param keyword - ключевое слово для поиска (название, актер и т.д.)
     * @param genre - название жанра для точного поиска
     * @param yearFrom - год выпуска от
     * @param yearTo - год выпуска до
     * @param ratingFrom - рейтинг от
     * @param ratingTo - рейтинг до
     * @return список сохраненных фильмов
     */
    @Transactional
    public List<Movie> searchAndSaveFilms(String keyword, String genre, Integer yearFrom, Integer yearTo,
                                          Double ratingFrom, Double ratingTo) {

        // Строим URL для запроса к Кинопоиску
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl);

        // Если передан жанр - используем точный поиск по ID жанра
        if (genre != null && !genre.trim().isEmpty()) {
            Integer genreId = getGenreIdByName(genre.trim());
            if (genreId != null) {
                builder.queryParam("genres[]", genreId);
            } else {
                // Если не нашли ID жанра, используем keyword
                System.out.println("⚠️ Жанр '" + genre + "' не найден в списке, использую keyword поиск");
                if (keyword == null) {
                    keyword = genre; // Используем название жанра как keyword
                }
            }
        }

        // Если keyword передан и не был использован для жанра
        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.queryParam("keyword", keyword.trim());
        }

        // Остальные параметры
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
                System.out.println("📭 API вернуло пустой результат");
                return Collections.emptyList();
            }

            System.out.println("📊 Найдено фильмов в API: " + kinopoiskResponse.getItems().size());

            // Фильтруем фильмы, которых нет в базе
            List<KinopoiskResponse.Film> newFilms = kinopoiskResponse.getItems().stream()
                    .filter(film -> !movieRepository.existsByFilmId(film.getKinopoiskId()))
                    .collect(Collectors.toList());

            System.out.println("🆕 Новых фильмов для сохранения: " + newFilms.size());

            if (newFilms.isEmpty()) {
                System.out.println("✅ Все фильмы уже есть в базе данных");
                return Collections.emptyList();
            }

            // Получаем детали для каждого фильма (чтобы получить жанры)
            List<KinopoiskResponse.Film> filmsWithDetails = new ArrayList<>();
            for (KinopoiskResponse.Film film : newFilms) {
                try {
                    Thread.sleep(100); // Небольшая задержка, чтобы не превысить лимиты API
                    KinopoiskResponse.Film details = getFilmDetails(film.getKinopoiskId());
                    if (details != null) {
                        filmsWithDetails.add(details);
                        System.out.println("✅ Получены детали для: " + details.getNameRu() +
                                " (жанры: " + (details.getGenres() != null ? details.getGenres().size() : 0) + ")");
                    } else {
                        filmsWithDetails.add(film); // Используем оригинал без деталей
                        System.out.println("⚠️ Использую базовую информацию для фильма ID: " + film.getKinopoiskId());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("⚠️ Задержка прервана для фильма ID: " + film.getKinopoiskId());
                    filmsWithDetails.add(film);
                }
            }

            // Сохраняем новые фильмы
            List<Movie> savedMovies = new ArrayList<>();
            for (KinopoiskResponse.Film film : filmsWithDetails) {
                try {
                    Movie movie = convertToMovieEntity(film);
                    Movie saved = movieRepository.save(movie);
                    savedMovies.add(saved);
                    System.out.println("💾 Сохранен фильм: " + saved.getFilmName() + " (ID: " + saved.getFilmId() + ")");
                } catch (Exception e) {
                    System.err.println("❌ Ошибка сохранения фильма ID " + film.getKinopoiskId() + ": " + e.getMessage());
                }
            }

            System.out.println("🎉 Всего сохранено фильмов: " + savedMovies.size());
            return savedMovies;

        } catch (Exception e) {
            System.err.println("❌ Ошибка при работе с API Кинопоиска: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Преобразование Film API в сущность Movie с получением существующих жанров
     */
    private Movie convertToMovieEntity(KinopoiskResponse.Film film) {
        // Создаем фильм
        Movie movie = new Movie(
                film.getKinopoiskId(),
                film.getNameRu() != null ? film.getNameRu() : film.getNameEn(),
                film.getYear(),
                film.getRatingKinopoisk() != null ?
                        BigDecimal.valueOf(film.getRatingKinopoisk()) : BigDecimal.ZERO,
                film.getDescription()
        );

        // Получаем существующие жанры из БД
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> existingGenres = new HashSet<>();

            for (KinopoiskResponse.GenreDto genreDto : film.getGenres()) {
                String genreName = genreDto.getGenre();
                if (genreName == null || genreName.trim().isEmpty()) {
                    continue;
                }

                // Ищем существующий жанр в БД
                genreRepository.findByName(genreName)
                        .ifPresent(existingGenres::add);
            }

            // Устанавливаем существующие жанры в фильм
            if (!existingGenres.isEmpty()) {
                movie.getGenres().addAll(existingGenres);
                System.out.println("🎭 Получены существующие жанры для фильма " + movie.getFilmName() + ": " +
                        existingGenres.stream()
                                .map(Genre::getName)
                                .collect(Collectors.joining(", ")));
            }
        }

        return movie;
    }

    /**
     * Получить список всех доступных жанров
     */
    public Map<String, Integer> getAvailableGenres() {
        return new HashMap<>(GENRE_MAP);
    }

    /**
     * Поиск фильмов по ID жанра (точный поиск)
     */
    public List<Movie> searchFilmsByGenreId(Integer genreId, Integer yearFrom, Integer yearTo,
                                            Double ratingFrom, Double ratingTo) {
        if (genreId == null) {
            throw new IllegalArgumentException("ID жанра не может быть null");
        }

        // Просто вызываем основной метод с параметром genre
        return searchAndSaveFilms(null, findGenreNameById(genreId), yearFrom, yearTo, ratingFrom, ratingTo);
    }

    /**
     * Получить ID жанра по названию
     */
    private Integer getGenreIdByName(String genreName) {
        return GENRE_MAP.get(genreName.toLowerCase().trim());
    }

    /**
     * Получить название жанра по ID
     */
    private String findGenreNameById(Integer genreId) {
        return GENRE_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().equals(genreId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Получить информацию о фильмах без сохранения в базу
     */
    public List<KinopoiskResponse.Film> searchFilmsOnly(String keyword, String genre,
                                                        Integer yearFrom, Integer yearTo,
                                                        Double ratingFrom, Double ratingTo) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl);

        if (genre != null && !genre.trim().isEmpty()) {
            Integer genreId = getGenreIdByName(genre.trim());
            if (genreId != null) {
                builder.queryParam("genres[]", genreId);
            } else if (keyword == null) {
                builder.queryParam("keyword", genre.trim());
            }
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            builder.queryParam("keyword", keyword.trim());
        }

        if (yearFrom != null) builder.queryParam("yearFrom", yearFrom);
        if (yearTo != null) builder.queryParam("yearTo", yearTo);
        if (ratingFrom != null) builder.queryParam("ratingFrom", ratingFrom);
        if (ratingTo != null) builder.queryParam("ratingTo", ratingTo);

        builder.queryParam("order", "RATING");
        builder.queryParam("type", "ALL");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
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

            return kinopoiskResponse.getItems();
        } catch (Exception e) {
            System.err.println("❌ Ошибка поиска фильмов: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}