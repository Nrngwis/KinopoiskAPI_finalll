package com.bank.movieservice.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "film_id", unique = true, nullable = false)
    private Long filmId;

    @Column(name = "film_name", nullable = false, length = 500)
    private String filmName;

    @Column(name = "release_year")
    private Integer year;

    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    // Жанры хранятся в фильме - просто связь
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    // Конструкторы
    public Movie() {}

    public Movie(Long filmId, String filmName, Integer year,
                 BigDecimal rating, String description) {
        this.filmId = filmId;
        this.filmName = filmName;
        this.year = year;
        this.rating = rating;
        this.description = description;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFilmId() { return filmId; }
    public void setFilmId(Long filmId) { this.filmId = filmId; }

    public String getFilmName() { return filmName; }
    public void setFilmName(String filmName) { this.filmName = filmName; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // ТОЛЬКО ГЕТТЕР для жанров - получаем существующие
    public Set<Genre> getGenres() {
        return genres;
    }
}
