package com.bank.movieservice.service;

import com.bank.movieservice.entity.Movie;
import com.bank.movieservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:#{null}}")
    private String yandexUsername;

    private final MovieRepository movieRepository;

    private final DecimalFormat decimalFormat;  // Внедряем через Spring

    public void sendReportByEmail(String toEmail, String subject, String reportContent, String reportType) {
        // Если mailSender не настроен - логируем и выходим
        if (mailSender == null) {
            System.out.println("=== 📧 EMAIL SIMULATION ===");
            System.out.println("SMTP not configured. Would send to: " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Report type: " + reportType);
            System.out.println("Report size: " + reportContent.length() + " characters");
            System.out.println("=== END SIMULATION ===");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Для Яндекс: формируем правильный адрес отправителя
            String fromAddress = getFromAddress();
            helper.setFrom(fromAddress, "Kinopoisk API Service");

            helper.setTo(toEmail);
            helper.setSubject(subject);

            // Текст письма
            String emailText = getEmailText(reportType);
            helper.setText(emailText);

            // Создаем вложение
            String mimeType = "csv".equalsIgnoreCase(reportType) ? "text/csv" : "application/xml";
            String fileName = "movies_report." + reportType.toLowerCase();
            byte[] reportBytes = reportContent.getBytes(StandardCharsets.UTF_8);

            // Используем ByteArrayResource вместо ByteArrayDataSource
            helper.addAttachment(fileName,
                    () -> new java.io.ByteArrayInputStream(reportBytes),
                    mimeType
            );

            // Отправляем письмо
            mailSender.send(message);

            // Логируем успех
            logEmailSuccess(toEmail, subject, reportType, reportBytes.length, fromAddress);

        } catch (Exception e) {
            System.err.println("❌ Ошибка отправки email через Яндекс: " + e.getMessage());
            e.printStackTrace();

            // Fallback: логируем детали для отладки
            System.out.println("=== DEBUG INFO ===");
            System.out.println("Yandex Username: " + yandexUsername);
            System.out.println("To: " + toEmail);
            System.out.println("Report size: " + reportContent.length());
            System.out.println("Error: " + e.getClass().getName() + ": " + e.getMessage());

            throw new RuntimeException("Failed to send email via Yandex: " + e.getMessage(), e);
        }
    }

    public String generateCsvReport(String keyword, Integer yearFrom, Integer yearTo,
                                    BigDecimal ratingFrom, Double ratingTo) {

        // Преобразуем Double в BigDecimal с проверкой на null
        BigDecimal ratingToBigDecimal = null;
        if (ratingTo != null) {
            ratingToBigDecimal = BigDecimal.valueOf(ratingTo);
        }

        List<Movie> movies = findMovies(keyword, yearFrom, yearTo, ratingFrom, ratingToBigDecimal, Pageable.unpaged())
                .getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("filmId,filmName,year,rating,description\n");

        for (Movie movie : movies) {
            // Форматируем рейтинг с помощью DecimalFormat для гарантированного использования точки
            String formattedRating = "0.0";
            if (movie.getRating() != null) {
                formattedRating = decimalFormat.format(movie.getRating());
            }

            csv.append(String.format("%d,\"%s\",%d,%s,\"%s\"\n",
                    movie.getFilmId(),
                    escapeCsv(movie.getFilmName()),
                    movie.getYear(),
                    formattedRating,  // Используем форматированное значение
                    escapeCsv(movie.getDescription())
            ));
        }

        return csv.toString();
    }

    public String generateXmlReport(String keyword, Integer yearFrom, Integer yearTo,
                                    BigDecimal ratingFrom, Double ratingTo) {

        // Преобразуем Double в BigDecimal с проверкой на null
        BigDecimal ratingToBigDecimal = null;
        if (ratingTo != null) {
            ratingToBigDecimal = BigDecimal.valueOf(ratingTo);
        }

        List<Movie> movies = findMovies(keyword, yearFrom, yearTo, ratingFrom, ratingToBigDecimal, Pageable.unpaged())
                .getContent();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<movies>\n");

        for (Movie movie : movies) {
            xml.append("  <movie>\n");
            xml.append("    <filmId>").append(movie.getFilmId()).append("</filmId>\n");
            xml.append("    <filmName>").append(escapeXml(movie.getFilmName())).append("</filmName>\n");
            xml.append("    <year>").append(movie.getYear()).append("</year>\n");
            xml.append("    <rating>").append(movie.getRating()).append("</rating>\n");
            xml.append("    <description>").append(escapeXml(movie.getDescription())).append("</description>\n");
            xml.append("  </movie>\n");
        }

        xml.append("</movies>");
        return xml.toString();
    }

    private Page<Movie> findMovies(String keyword, Integer yearFrom, Integer yearTo,
                                   BigDecimal ratingFrom, BigDecimal ratingTo, Pageable pageable) {

        return movieRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("filmName")), "%" + keyword.toLowerCase() + "%"));
            }
            if (yearFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), yearFrom));
            }
            if (yearTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("year"), yearTo));
            }
            if (ratingFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), ratingFrom));
            }
            if (ratingTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rating"), ratingTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String getFromAddress() {
        if (yandexUsername == null || yandexUsername.isEmpty()) {
            return "noreply@kinopoisk-app.com";
        }

        // Для Яндекс: если username не содержит @, добавляем @yandex.ru
        if (!yandexUsername.contains("@")) {
            return yandexUsername + "@yandex.ru";
        }

        return yandexUsername;
    }

    private String getEmailText(String reportType) {
        return "Добрый день!\n\n" +
                "Во вложении находится запрошенный отчет о фильмах в формате " + reportType.toUpperCase() + ".\n\n" +
                "Отчет сформирован на основе данных Кинопоиска.\n\n" +
                "С уважением,\nСервис Kinopoisk API\n" +
                "https://kinopoisk-api.example.com";
    }

    private void logEmailSuccess(String toEmail, String subject, String reportType,
                                 int contentLength, String fromAddress) {
        System.out.println("=".repeat(50));
        System.out.println("✅ EMAIL SENT SUCCESSFULLY");
        System.out.println("📧 From: " + fromAddress);
        System.out.println("📧 To: " + toEmail);
        System.out.println("📌 Subject: " + subject);
        System.out.println("📎 Attachment: movies_report." + reportType.toLowerCase());
        System.out.println("📊 Size: " + contentLength + " bytes");
        System.out.println("⏰ Time: " + java.time.LocalDateTime.now());
        System.out.println("=".repeat(50));
    }

    public String checkMailConfiguration() {
        StringBuilder config = new StringBuilder();
        config.append("📧 Email Configuration Status:\n");
        config.append("SMTP Configured: ").append(mailSender != null ? "✅ YES" : "❌ NO").append("\n");

        if (yandexUsername != null) {
            config.append("Yandex Username: ").append(yandexUsername).append("\n");
            config.append("From Address: ").append(getFromAddress()).append("\n");
        } else {
            config.append("Yandex Username: ❌ NOT SET\n");
        }

        config.append("\n📝 Application.properties should contain:\n");
        config.append("spring.mail.host=smtp.yandex.ru\n");
        config.append("spring.mail.port=587\n");
        config.append("spring.mail.username=your_login (without @yandex.ru)\n");
        config.append("spring.mail.password=app_password (16 chars)\n");

        return config.toString();
    }
}