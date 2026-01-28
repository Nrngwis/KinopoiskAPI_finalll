DROP TABLE IF EXISTS movies;

CREATE TABLE movies (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        film_id BIGINT UNIQUE NOT NULL,
                        film_name VARCHAR(500) NOT NULL,
                        release_year INT,
                        rating DECIMAL(3,1),
                        description TEXT

);

