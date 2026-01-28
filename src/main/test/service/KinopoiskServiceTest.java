package service;

import com.bank.movieservice.DTO.response.KinopoiskResponse;
import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.repository.MovieRepository;
import com.bank.movieservice.service.KinopoiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KinopoiskServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private KinopoiskService kinopoiskService;

    @BeforeEach
    void setUp() {
        String apiUrl = "https://kinopolskapiunofficial.tech/api/v2.2/";
        String apiKey = "45026472-1903-4828-8482-dS?a0200b5da";

        ReflectionTestUtils.setField(kinopoiskService, "apiUrl", apiUrl);
        ReflectionTestUtils.setField(kinopoiskService, "apiKey", apiKey);
    }

    @Test
    void searchAndSaveFilms_ShouldSaveNewFilms() {
        // Подготовка
        KinopoiskResponse.Film film = new KinopoiskResponse.Film();
        film.setKinopoiskId(12345L);
        film.setNameRu("Матрица");
        film.setYear(1999);
        film.setRatingKinopoisk(8.7);
        film.setDescription("Описание");

        KinopoiskResponse response = new KinopoiskResponse();
        response.setItems(Collections.singletonList(film));

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KinopoiskResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        when(movieRepository.existsByFilmId(12345L)).thenReturn(false);
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie movie = invocation.getArgument(0);
            movie.setId(1L);
            return movie;
        });

        // Действие
        List<Movie> result = kinopoiskService.searchAndSaveFilms("матрица", null, null, null, null);

        // Проверка
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFilmName()).isEqualTo("Матрица");
        verify(movieRepository, times(1)).existsByFilmId(12345L);
        verify(movieRepository, times(1)).save(any(Movie.class));
    }

    @Test
    void searchAndSaveFilms_ShouldSkipExistingFilms() {
        // Подготовка
        KinopoiskResponse.Film film = new KinopoiskResponse.Film();
        film.setKinopoiskId(12345L);
        film.setNameRu("Матрица");

        KinopoiskResponse response = new KinopoiskResponse();
        response.setItems(Collections.singletonList(film));

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KinopoiskResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        when(movieRepository.existsByFilmId(12345L)).thenReturn(true); // Фильм уже существует

        // Действие
        List<Movie> result = kinopoiskService.searchAndSaveFilms("матрица", null, null, null, null);

        // Проверка
        assertThat(result).isEmpty(); // Не должен сохранять существующий фильм
        verify(movieRepository, times(1)).existsByFilmId(12345L);
        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void searchAndSaveFilms_ShouldHandleApiError() {
        // Подготовка
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(KinopoiskResponse.class)
        )).thenThrow(new RuntimeException("API error"));

        // Действие
        List<Movie> result = kinopoiskService.searchAndSaveFilms("матрица", null, null, null, null);

        // Проверка
        assertThat(result).isEmpty(); // Должен возвращать пустой список при ошибке
        verify(movieRepository, never()).existsByFilmId(anyLong());
        verify(movieRepository, never()).save(any(Movie.class));
    }
}
