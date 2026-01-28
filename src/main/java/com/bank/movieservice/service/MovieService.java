package com.bank.movieservice.service;

import com.bank.movieservice.DTO.response.MovieResponse;
import com.bank.movieservice.DTO.response.MovieSearchResponseDTO;
import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Page<Movie> getMovies(Specification<Movie> spec, Pageable pageable) {
        return movieRepository.findAll(spec, pageable);
    }

    // Получить MovieResponse по ID
    public Optional<MovieResponse> getMovieResponseById(Long id) {
        return movieRepository.findById(id)
                .map(MovieResponse::fromEntity);
    }

    // Получить MovieResponse по filmId
    public Optional<MovieResponse> getMovieResponseByFilmId(Long filmId) {
        return movieRepository.findByFilmId(filmId)
                .map(MovieResponse::fromEntity);
    }

    // Конвертировать Page<Movie> в List<MovieResponse>
    public List<MovieResponse> convertToResponseList(List<Movie> movies) {
        return movies.stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // Поиск фильмов по названию
    public List<MovieSearchResponseDTO> searchMoviesByName(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<Movie> movies = movieRepository.searchByName(query.trim());
        return movies.stream()
                .map(MovieSearchResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
