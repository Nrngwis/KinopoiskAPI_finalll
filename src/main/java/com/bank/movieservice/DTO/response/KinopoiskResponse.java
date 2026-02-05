package com.bank.movieservice.DTO.response;

import java.util.List;

public class KinopoiskResponse {
    private Integer total;
    private Integer totalPages;
    private List<Film> items;

    public static class Film {
        private Long kinopoiskId;

        public Long getKinopoiskId() {
            return kinopoiskId;
        }

        public void setKinopoiskId(Long kinopoiskId) {
            this.kinopoiskId = kinopoiskId;
        }

        private String nameRu;

        public String getNameRu() {
            return nameRu != null ? nameRu : "Unknown";
        }

        public void setNameRu(String nameRu) {
            this.nameRu = nameRu;
        }

        private String nameEn;

        public String getNameEn() {
            return nameEn != null ? nameEn : "";
        }

        public void setNameEn(String nameEn) {
            this.nameEn = nameEn;
        }

        private Integer year;

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        private Double ratingKinopoisk;
        public Double getRatingKinopoisk() {
            return ratingKinopoisk != null ? ratingKinopoisk : 0.0;
        }

        public void setRatingKinopoisk(Double ratingKinopoisk) {
            this.ratingKinopoisk = ratingKinopoisk;
        }

        private String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        private List<GenreDto> genres;

        public List<GenreDto> getGenres() {
            return genres;
        }

        public void setGenres(List<GenreDto> genres) {
            this.genres = genres;
        }

        @Override
        public String toString() {
            return "Film{" +
                    "kinopoiskId=" + kinopoiskId +
                    ", nameRu='" + nameRu + '\'' +
                    ", year=" + year +
                    ", ratingKinopoisk=" + ratingKinopoisk +
                    ", description='" + description + '\'' +
                    ", genres=" + genres +
                    '}';
        }
    }

    public static class GenreDto {
        private String genre;

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        @Override
        public String toString() {
            return "GenreDto{" +
                    "genre='" + genre + '\'' +
                    '}';
        }
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<Film> getItems() {
        return items;
    }

    public void setItems(List<Film> items) {
        this.items = items;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    @Override
    public String toString() {
        return "KinopoiskResponse{" +
                "total=" + total +
                ", totalPages=" + totalPages +
                ", items=" + items +
                '}';
    }
}
