package com.bank.movieservice.repository;

import com.bank.movieservice.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    Optional<Movie> findByFilmId(Long filmId);
    boolean existsByFilmId(Long filmId);
    List<Movie> findByFilmIdIn(List<Long> filmIds);

    // Поиск фильмов по названию (регистронезависимый, поиск по началу слов)
    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE " +
           "LOWER(m.filmName) LIKE LOWER(CONCAT(:query, '%')) OR " +
           "LOWER(m.filmName) LIKE LOWER(CONCAT('% ', :query, '%'))")
    List<Movie> searchByName(@Param("query") String query);
}