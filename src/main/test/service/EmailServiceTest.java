package service;

import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.repository.MovieRepository;
import com.bank.movieservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private MovieRepository movieRepository;

    private EmailService emailService;
    private Movie movie1;
    private Movie movie2;
    private DecimalFormat decimalFormat;

    @BeforeEach
    void setUp() {
        // Создаем DecimalFormat для тестов (такой же как в Spring конфигурации)
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#0.0", symbols);
        decimalFormat.setRoundingMode(java.math.RoundingMode.HALF_UP);

        // Создаем EmailService для тестов - теперь передаем 3 параметра
        emailService = new EmailService(null, movieRepository, decimalFormat);

        // Инициализируем тестовые данные
        movie1 = new Movie();
        movie1.setFilmId(12345L);
        movie1.setFilmName("Матрица");
        movie1.setYear(1999);
        movie1.setRating(new BigDecimal("8.7"));
        movie1.setDescription("Фильм о \"Матрице\" & виртуальной реальности");

        movie2 = new Movie();
        movie2.setFilmId(67890L);
        movie2.setFilmName("Властелин колец");
        movie2.setYear(2001);
        movie2.setRating(new BigDecimal("8.8"));
        movie2.setDescription("Эпическая фэнтези-сага");
    }

    @Test
    void generateCsvReport_ShouldGenerateValidCsv_WhenAllParametersAreNull() {
        // Arrange
        List<Movie> movies = Arrays.asList(movie1, movie2);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String csv = emailService.generateCsvReport(null, null, null, null, null);

        // Assert
        assertThat(csv).isNotNull();
        assertThat(csv).contains("filmId,filmName,year,rating,description");
        assertThat(csv).contains("12345");
        assertThat(csv).contains("Матрица");
        assertThat(csv).contains("1999");
        // Теперь используем точку как десятичный разделитель
        assertThat(csv).contains("8.7");
        assertThat(csv).contains("8.8");
        assertThat(csv).contains("\"Фильм о \"\"Матрице\"\" & виртуальной реальности\"");
    }

    @Test
    void generateCsvReport_ShouldGenerateValidCsv_WithNonNullRatingTo() {
        // Arrange
        List<Movie> movies = Arrays.asList(movie1);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String csv = emailService.generateCsvReport(null, null, null, null, 9.0);

        // Assert
        assertThat(csv).isNotNull();
        assertThat(csv).contains("Матрица");
        assertThat(csv).contains("8.7");
    }

    @Test
    void generateCsvReport_ShouldHandleEmptyMovieList() {
        // Arrange
        List<Movie> movies = Collections.emptyList();
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String csv = emailService.generateCsvReport(null, null, null, null, null);

        // Assert
        assertThat(csv).isNotNull();
        assertThat(csv).isEqualTo("filmId,filmName,year,rating,description\n");
    }

    @Test
    void generateCsvReport_ShouldEscapeSpecialCharacters() {
        // Arrange
        Movie movieWithQuotes = new Movie();
        movieWithQuotes.setFilmId(99999L);
        movieWithQuotes.setFilmName("Test \"Movie\"");
        movieWithQuotes.setYear(2020);
        movieWithQuotes.setRating(new BigDecimal("7.5"));
        movieWithQuotes.setDescription("Description with \"quotes\" & ampersand");

        List<Movie> movies = Arrays.asList(movieWithQuotes);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String csv = emailService.generateCsvReport(null, null, null, null, null);

        // Assert
        assertThat(csv).contains("\"Test \"\"Movie\"\"\"");
        assertThat(csv).contains("\"Description with \"\"quotes\"\" & ampersand\"");
        assertThat(csv).contains("7.5");
    }

    @Test
    void generateCsvReport_ShouldHandleNullRating() {
        // Arrange
        Movie movieWithNullRating = new Movie();
        movieWithNullRating.setFilmId(11111L);
        movieWithNullRating.setFilmName("Test Movie");
        movieWithNullRating.setYear(2022);
        movieWithNullRating.setRating(null);
        movieWithNullRating.setDescription("Test description");

        List<Movie> movies = Arrays.asList(movieWithNullRating);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String csv = emailService.generateCsvReport(null, null, null, null, null);

        // Assert
        assertThat(csv).contains("11111,\"Test Movie\",2022,0.0,\"Test description\"");
    }

    @Test
    void generateXmlReport_ShouldGenerateValidXml_WhenAllParametersAreNull() {
        // Arrange
        List<Movie> movies = Arrays.asList(movie1);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String xml = emailService.generateXmlReport(null, null, null, null, null);

        // Assert
        assertThat(xml).isNotNull();
        assertThat(xml).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<movies>");
        assertThat(xml).contains("<filmName>Матрица</filmName>");
        assertThat(xml).contains("<year>1999</year>");
        assertThat(xml).contains("<rating>8.7</rating>");
        assertThat(xml).contains("Фильм о &quot;Матрице&quot; &amp; виртуальной реальности");
    }

    @Test
    void generateXmlReport_ShouldGenerateValidXml_WithNonNullRatingTo() {
        // Arrange
        List<Movie> movies = Arrays.asList(movie1);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String xml = emailService.generateXmlReport(null, null, null, null, 9.0);

        // Assert
        assertThat(xml).isNotNull();
        assertThat(xml).contains("Матрица");
        assertThat(xml).contains("8.7");
    }

    @Test
    void generateXmlReport_ShouldHandleEmptyMovieList() {
        // Arrange
        List<Movie> movies = Collections.emptyList();
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String xml = emailService.generateXmlReport(null, null, null, null, null);

        // Assert
        assertThat(xml).isNotNull();
        assertThat(xml).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<movies>\n</movies>");
    }

    @Test
    void generateXmlReport_ShouldEscapeSpecialCharacters() {
        // Arrange
        Movie movieWithSpecialChars = new Movie();
        movieWithSpecialChars.setFilmId(88888L);
        movieWithSpecialChars.setFilmName("Test <Movie>");
        movieWithSpecialChars.setYear(2021);
        movieWithSpecialChars.setRating(new BigDecimal("6.5"));
        movieWithSpecialChars.setDescription("Description with <tag> & \"quotes\" 'apos'");

        List<Movie> movies = Arrays.asList(movieWithSpecialChars);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String xml = emailService.generateXmlReport(null, null, null, null, null);

        // Assert
        assertThat(xml).contains("<filmName>Test &lt;Movie&gt;</filmName>");
        assertThat(xml).contains("Description with &lt;tag&gt; &amp; &quot;quotes&quot; &apos;apos&apos;");
        assertThat(xml).contains("<rating>6.5</rating>");
    }

    @Test
    void sendReportByEmail_ShouldNotThrow_WhenMailSenderIsNull() {
        // Arrange
        String testReport = "test,report\n1,Матрица";

        // Act & Assert
        // Should not throw exception when mailSender is null
        emailService.sendReportByEmail(
                "test@example.com",
                "Test Report",
                testReport,
                "csv"
        );
    }

    @Test
    void generateXmlReport_ShouldHandleNullDescription() {
        // Arrange
        Movie movieWithNullDesc = new Movie();
        movieWithNullDesc.setFilmId(22222L);
        movieWithNullDesc.setFilmName("Test Movie");
        movieWithNullDesc.setYear(2023);
        movieWithNullDesc.setRating(new BigDecimal("6.0"));
        movieWithNullDesc.setDescription(null);

        List<Movie> movies = Arrays.asList(movieWithNullDesc);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String xml = emailService.generateXmlReport(null, null, null, null, null);

        // Assert
        assertThat(xml).contains("<description></description>");
    }

    @Test
    void generateCsvReport_ShouldFormatDecimalNumbersCorrectly() {
        // Arrange
        Movie movieWithVariousRatings = new Movie();
        movieWithVariousRatings.setFilmId(33333L);
        movieWithVariousRatings.setFilmName("Rating Test");
        movieWithVariousRatings.setYear(2024);
        movieWithVariousRatings.setRating(new BigDecimal("9.75"));
        movieWithVariousRatings.setDescription("Test");

        List<Movie> movies = Arrays.asList(movieWithVariousRatings);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String csv = emailService.generateCsvReport(null, null, null, null, null);

        // Assert
        // Проверяем, что форматирование работает правильно с одним десятичным знаком
        // 9.75 должно округлиться до 9.8
        assertThat(csv).contains("9.8");
    }

    @Test
    void generateCsvReport_ShouldFormatVariousRatings() {
        // Arrange
        Movie movie1 = new Movie();
        movie1.setFilmId(1L);
        movie1.setFilmName("Movie 1");
        movie1.setYear(2020);
        movie1.setRating(new BigDecimal("7.0"));
        movie1.setDescription("Test 1");

        Movie movie2 = new Movie();
        movie2.setFilmId(2L);
        movie2.setFilmName("Movie 2");
        movie2.setYear(2021);
        movie2.setRating(new BigDecimal("8.5"));
        movie2.setDescription("Test 2");

        Movie movie3 = new Movie();
        movie3.setFilmId(3L);
        movie3.setFilmName("Movie 3");
        movie3.setYear(2022);
        movie3.setRating(new BigDecimal("9.99"));
        movie3.setDescription("Test 3");

        List<Movie> movies = Arrays.asList(movie1, movie2, movie3);
        Page<Movie> moviePage = new PageImpl<>(movies);

        when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(moviePage);

        // Act
        String csv = emailService.generateCsvReport(null, null, null, null, null);

        // Assert
        assertThat(csv).contains("7.0");
        assertThat(csv).contains("8.5");
        assertThat(csv).contains("10.0"); // 9.99 округляется до 10.0
    }
}