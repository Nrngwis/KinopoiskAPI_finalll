package com.bank.movieservice.controller;

import com.bank.movieservice.DTO.response.MovieResponse;
import com.bank.movieservice.DTO.response.MovieSearchResponseDTO;
import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.service.KinopoiskService;
import com.bank.movieservice.service.EmailService;
import com.bank.movieservice.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MovieController {

    private final KinopoiskService kinopoiskService;
    private final EmailService emailService;
    private final MovieService movieService; // Добавляем MovieService

    // Поиск фильмов в Кинопоиске и сохранение в БД
    @GetMapping("/v2/films")
    public ResponseEntity<?> searchAndSaveFilms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double ratingFrom,
            @RequestParam(required = false) Double ratingTo) {

        try {
            var savedMovies = kinopoiskService.searchAndSaveFilms(keyword, yearFrom, yearTo, ratingFrom, ratingTo);

            // Используем MovieService для конвертации
            List<MovieResponse> savedMoviesDto = movieService.convertToResponseList(savedMovies);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully saved " + savedMovies.size() + " new films");
            response.put("savedFilms", savedMoviesDto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Поиск фильмов из базы данных
    @GetMapping("/films")
    public ResponseEntity<Page<Movie>> getFilms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double ratingFrom,
            @RequestParam(required = false) Double ratingTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "filmId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Movie> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<javax.persistence.criteria.Predicate>();

            if (keyword != null) {
                predicates.add(cb.like(cb.lower(root.get("filmName")), "%" + keyword.toLowerCase() + "%"));
            }
            if (yearFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), yearFrom));
            }
            if (yearTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("year"), yearTo));
            }
            if (ratingFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), ratingFrom));
            }
            if (ratingTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), ratingTo));
            }

            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };

        Page<Movie> films = movieService.getMovies(spec, pageable);
        return ResponseEntity.ok(films);
    }

    // Новый endpoint для получения фильма по ID через MovieResponse
    @GetMapping("/films/{id}/response")
    public ResponseEntity<MovieResponse> getFilmResponseById(@PathVariable Long id) {
        return movieService.getMovieResponseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Новый endpoint для получения фильма по filmId через MovieResponse
    @GetMapping("/films/film-id/{filmId}/response")
    public ResponseEntity<MovieResponse> getFilmResponseByFilmId(@PathVariable Long filmId) {
        return movieService.getMovieResponseByFilmId(filmId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Поиск фильмов по названию (новая ручка)
    @GetMapping("/movies/search")
    public ResponseEntity<List<MovieSearchResponseDTO>> searchMovies(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<MovieSearchResponseDTO> results = movieService.searchMoviesByName(query);
        return ResponseEntity.ok(results);
    }

    // Генерация отчетов с обработкой null-safety
    @GetMapping("/reports/csv")
    public ResponseEntity<String> generateCsvReport(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double ratingFrom,
            @RequestParam(required = false) Double ratingTo) {

        try {
            BigDecimal ratingFromBigDecimal = ratingFrom != null ? BigDecimal.valueOf(ratingFrom) : null;
            String csv = emailService.generateCsvReport(keyword, yearFrom, yearTo, ratingFromBigDecimal, ratingTo);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv; charset=UTF-8")
                    .header("Content-Disposition", "attachment; filename=movies_report.csv")
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error generating CSV report: " + e.getMessage());
        }
    }

    @GetMapping("/reports/xml")
    public ResponseEntity<String> generateXmlReport(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double ratingFrom,
            @RequestParam(required = false) Double ratingTo) {

        try {
            BigDecimal ratingFromBigDecimal = ratingFrom != null ? BigDecimal.valueOf(ratingFrom) : null;
            String xml = emailService.generateXmlReport(keyword, yearFrom, yearTo, ratingFromBigDecimal, ratingTo);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/xml; charset=UTF-8")
                    .header("Content-Disposition", "attachment; filename=movies_report.xml")
                    .body(xml);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error generating XML report: " + e.getMessage());
        }
    }

    @PostMapping("/reports/send")
    public ResponseEntity<String> sendReportByEmail(
            @RequestParam String email,
            @RequestParam String reportType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Double ratingFrom,
            @RequestParam(required = false) Double ratingTo) {

        try {
            // Валидация email
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                return ResponseEntity.badRequest().body("Invalid email address");
            }

            // Валидация типа отчета
            if (reportType == null || reportType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Report type is required");
            }

            String reportContent;
            BigDecimal ratingFromBigDecimal = ratingFrom != null ? BigDecimal.valueOf(ratingFrom) : null;

            if ("csv".equalsIgnoreCase(reportType)) {
                reportContent = emailService.generateCsvReport(keyword, yearFrom, yearTo, ratingFromBigDecimal, ratingTo);
            } else if ("xml".equalsIgnoreCase(reportType)) {
                reportContent = emailService.generateXmlReport(keyword, yearFrom, yearTo, ratingFromBigDecimal, ratingTo);
            } else {
                return ResponseEntity.badRequest().body("Invalid report type. Use 'csv' or 'xml'");
            }

            emailService.sendReportByEmail(email, "Movies Report - " + reportType, reportContent, reportType);
            return ResponseEntity.ok("Report sent successfully to " + email);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error sending report: " + e.getMessage());
        }
    }
}
