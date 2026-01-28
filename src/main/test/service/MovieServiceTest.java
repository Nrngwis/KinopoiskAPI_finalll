package service;  // ✅ Пакет должен совпадать с основным!

import com.bank.movieservice.DTO.response.MovieResponse;
import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.repository.MovieRepository;
import com.bank.movieservice.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для MovieService
 * Только Mockito, без Spring контекста
 */
@ExtendWith(MockitoExtension.class)  // ✅ Только эта аннотация для unit-тестов
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie avatarMovie;
    private Movie matrixMovie;

    @BeforeEach
    void setUp() {
        // Инициализация тестовых данных
        avatarMovie = createMovie(1L, 12345L, "Аватар", 2009, "7.9");
        matrixMovie = createMovie(2L, 67890L, "Матрица", 1999, "8.7");
    }

    private Movie createMovie(Long id, Long filmId, String name, Integer year, String rating) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setFilmId(filmId);
        movie.setFilmName(name);
        movie.setYear(year);
        movie.setRating(new BigDecimal(rating));
        movie.setDescription("Описание фильма " + name);
        return movie;
    }

    @Test
    @DisplayName("getMovieResponseById - возвращает MovieResponse если фильм существует")
    void getMovieResponseById_ShouldReturnMovieResponse_WhenMovieExists() {
        // Given (Подготовка)
        when(movieRepository.findById(1L)).thenReturn(Optional.of(avatarMovie));

        // When (Выполнение)
        Optional<MovieResponse> result = movieService.getMovieResponseById(1L);

        // Then (Проверка)
        assertThat(result).isPresent();
        MovieResponse response = result.get();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFilmName()).isEqualTo("Аватар");  // Исправлено: было "Матрица"
        verify(movieRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getMovieResponseById - возвращает empty если фильм не найден")
    void getMovieResponseById_ShouldReturnEmpty_WhenMovieNotFound() {
        // Given
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<MovieResponse> result = movieService.getMovieResponseById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(movieRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getMovieResponseByFilmId - возвращает MovieResponse по filmId")
    void getMovieResponseByFilmId_ShouldReturnMovieResponse_WhenMovieExists() {
        // Given
        when(movieRepository.findByFilmId(12345L)).thenReturn(Optional.of(avatarMovie));

        // When
        Optional<MovieResponse> result = movieService.getMovieResponseByFilmId(12345L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFilmName()).isEqualTo("Аватар");
        verify(movieRepository, times(1)).findByFilmId(12345L);
    }

    @Test
    @DisplayName("convertToResponseList - конвертирует список фильмов в DTO")
    void convertToResponseList_ShouldConvertMoviesToResponses() {
        // Given
        List<Movie> movies = Arrays.asList(avatarMovie, matrixMovie);

        // When
        List<MovieResponse> result = movieService.convertToResponseList(movies);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFilmName()).isEqualTo("Аватар");
        assertThat(result.get(1).getFilmName()).isEqualTo("Матрица");
        // Этот метод не должен взаимодействовать с репозиторием
        verifyNoInteractions(movieRepository);
    }

    @Test
    @DisplayName("getMovies - возвращает страницу с фильмами")
    void getMovies_ShouldReturnPageOfMovies_WhenSpecificationAndPageableProvided() {
        // Given
        Page<Movie> moviePage = new PageImpl<>(Arrays.asList(avatarMovie));
        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // When
        Page<Movie> result = movieService.getMovies(
                mock(Specification.class),
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFilmName()).isEqualTo("Аватар");  // Исправлено: было "Матрица"
        verify(movieRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getMovies - возвращает пустую страницу когда нет фильмов")
    void getMovies_ShouldReturnEmptyPage_WhenNoMoviesFound() {
        // Given
        Page<Movie> emptyPage = Page.empty();
        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        Page<Movie> result = movieService.getMovies(
                mock(Specification.class),
                PageRequest.of(0, 10)
        );

        // Then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(movieRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
    }
}
