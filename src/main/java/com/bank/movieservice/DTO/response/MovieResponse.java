package com.bank.movieservice.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private Long id;
    private Long filmId;
    private String filmName;
    private Integer year;
    private BigDecimal rating;
    private String description;
    private List<String> genres;

    public static MovieResponse fromEntity(com.bank.movieservice.entity.Movie movie) {
        if (movie == null) {
            return null;
        }

        return MovieResponse.builder()
                .id(movie.getId())
                .filmId(movie.getFilmId())
                .filmName(movie.getFilmName())
                .year(movie.getYear())
                .rating(movie.getRating())
                .description(movie.getDescription())
                .genres(movie.getGenres() != null ?
                        movie.getGenres().stream()
                                .map(genre -> genre.getName())
                                .collect(Collectors.toList()) : null)
                .build();
    }
}
