package com.bank.movieservice.DTO.response;

import com.bank.movieservice.entity.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreDTO {

    private Long id;
    private String name;

    public static GenreDTO fromEntity(Genre genre) {
        if (genre == null) {
            return null;
        }

        return GenreDTO.builder()
                .id(genre.getId())
                .name(genre.getName())
                .build();
    }
}
