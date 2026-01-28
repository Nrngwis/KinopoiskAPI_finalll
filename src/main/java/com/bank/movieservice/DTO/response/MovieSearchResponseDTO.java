package com.bank.movieservice.DTO.response;

import com.bank.movieservice.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieSearchResponseDTO {

    private Long id;
    private String name;
    private Integer year;
    private List<GenreDTO> genres;

    public static MovieSearchResponseDTO fromEntity(Movie movie) {
        if (movie == null) {
            return null;
        }

        return MovieSearchResponseDTO.builder()
                .id(movie.getId())
                .name(movie.getFilmName())
                .year(movie.getYear())
                .genres(movie.getGenres() != null ?
                        movie.getGenres().stream()
                                .map(GenreDTO::fromEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}
