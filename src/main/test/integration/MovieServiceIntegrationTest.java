package integration;

import com.bank.movieservice.DTO.response.MovieResponse;
import com.bank.movieservice.KinopoiskApiApplication; // Импортируем главный класс
import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.repository.MovieRepository;
import com.bank.movieservice.service.EmailService;
import com.bank.movieservice.service.KinopoiskService;
import com.bank.movieservice.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = KinopoiskApiApplication.class) // Явно указываем главный класс
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class MovieServiceIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieService movieService;

    @Autowired
    private EmailService emailService;

    @MockBean
    private KinopoiskService kinopoiskService;

    @BeforeEach
    void setUp() {
        // Очищаем базу перед каждым тестом
        movieRepository.deleteAll();

        // Добавляем тестовые данные
        Movie matrix = new Movie();
        matrix.setFilmId(12345L);
        matrix.setFilmName("Матрица");
        matrix.setYear(1999);
        matrix.setRating(new BigDecimal("8.7"));
        matrix.setDescription("Хакер Нео узнаёт, что его мир — виртуальный.");
        movieRepository.save(matrix);

        Movie lotr = new Movie();
        lotr.setFilmId(67890L);
        lotr.setFilmName("Властелин колец");
        lotr.setYear(2001);
        lotr.setRating(new BigDecimal("8.8"));
        lotr.setDescription("Эпическая фэнтези-сага.");
        movieRepository.save(lotr);
    }

    @Test
    void contextLoads() {
        // Простой тест на загрузку контекста
        assertThat(movieRepository).isNotNull();
        assertThat(movieService).isNotNull();
        assertThat(emailService).isNotNull();
        assertThat(kinopoiskService).isNotNull();
    }

    @Test
    void testFullIntegration_MovieServiceAndRepository() {
        // 1. Проверяем сохранение в БД
        assertThat(movieRepository.count()).isEqualTo(2);

        // 2. Проверяем поиск через MovieService
        Optional<MovieResponse> matrixResponse = movieService.getMovieResponseByFilmId(12345L);
        assertThat(matrixResponse).isPresent();
        assertThat(matrixResponse.get().getFilmName()).isEqualTo("Матрица");

        // 3. Проверяем поиск всех фильмов
        Page<Movie> moviesPage = movieService.getMovies(
                (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("year"), 2000),
                PageRequest.of(0, 10)
        );
        assertThat(moviesPage.getTotalElements()).isEqualTo(1);
        assertThat(moviesPage.getContent().get(0).getFilmName()).isEqualTo("Властелин колец");

        // 4. Проверяем конвертацию в DTO
        List<Movie> allMovies = movieRepository.findAll();
        List<MovieResponse> responses = movieService.convertToResponseList(allMovies);
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(MovieResponse::getFilmName)
                .containsExactlyInAnyOrder("Матрица", "Властелин колец");
    }

    @Test
    void testEmailServiceIntegration_WithRealDatabase() {
        // Arrange - данные уже в БД из @BeforeEach

        // Act - генерируем отчет из реальных данных
        String csvReport = emailService.generateCsvReport("Матрица", null, null, null, null);
        String xmlReport = emailService.generateXmlReport(null, 2000, null, null, null);

        // Assert
        assertThat(csvReport).isNotNull();
        assertThat(csvReport).contains("Матрица");
        assertThat(csvReport).contains("12345");
        assertThat(csvReport).contains("8.7");

        assertThat(xmlReport).isNotNull();
        assertThat(xmlReport).contains("<movies>");
        assertThat(xmlReport).contains("Властелин колец");
        assertThat(xmlReport).doesNotContain("Матрица");
    }

    @Test
    void testSearchWithFilters_Integration() {
        // Arrange - данные уже в БД

        // Act - ищем фильмы с фильтрами
        String csv = emailService.generateCsvReport(
                null,        // keyword
                1999,        // yearFrom
                2000,        // yearTo
                new BigDecimal("8.0"), // ratingFrom
                null         // ratingTo
        );

        // Assert
        assertThat(csv).contains("Матрица");
        assertThat(csv).doesNotContain("Властелин колец");
    }

    @Test
    void testMovieCRUD_Integration() {
        // Create
        Movie newMovie = new Movie();
        newMovie.setFilmId(99999L);
        newMovie.setFilmName("Новый фильм");
        newMovie.setYear(2023);
        newMovie.setRating(new BigDecimal("7.5"));
        newMovie.setDescription("Тестовое описание");

        Movie saved = movieRepository.save(newMovie);
        assertThat(saved.getId()).isNotNull();
        assertThat(movieRepository.count()).isEqualTo(3);

        // Read
        Optional<Movie> found = movieRepository.findByFilmId(99999L);
        assertThat(found).isPresent();
        assertThat(found.get().getFilmName()).isEqualTo("Новый фильм");

        // Update
        found.get().setRating(new BigDecimal("8.0"));
        movieRepository.save(found.get());

        Optional<Movie> updated = movieRepository.findByFilmId(99999L);
        assertThat(updated).isPresent();
        assertThat(updated.get().getRating()).isEqualByComparingTo("8.0");

        // Delete
        movieRepository.deleteById(saved.getId());
        assertThat(movieRepository.count()).isEqualTo(2);
        assertThat(movieRepository.findByFilmId(99999L)).isEmpty();
    }

    @Test
    void testComplexScenario_Integration() {
        // 1. Получаем фильм через service (использует репозиторий)
        Optional<MovieResponse> movieOpt = movieService.getMovieResponseByFilmId(12345L);
        assertThat(movieOpt).isPresent();

        // 2. Генерируем CSV отчет с фильтром по названию
        String csv = emailService.generateCsvReport("Матрица", null, null, null, null);
        assertThat(csv).contains(movieOpt.get().getFilmName());
        assertThat(csv).contains(movieOpt.get().getRating().toString());

        // 3. Генерируем XML отчет
        String xml = emailService.generateXmlReport(null, null, null, null, null);
        assertThat(xml).contains("<movies>");
        assertThat(xml.split("<movie>").length - 1).isEqualTo(2);

        // 4. Проверяем, что оба сервиса работают с одними данными
        List<Movie> allMovies = movieRepository.findAll();
        List<MovieResponse> allResponses = movieService.convertToResponseList(allMovies);

        assertThat(allResponses).hasSize(2);
    }
}
