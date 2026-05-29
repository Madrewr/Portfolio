package sk.tuke.gamestudio.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import sk.tuke.gamestudio.service.*;

@SpringBootApplication(scanBasePackages = "sk.tuke.gamestudio")
@EntityScan(basePackages = "sk.tuke.gamestudio.entity")
public class GameStudioServer {

    public static void main(String[] args) {
        // Toto spusti Spring Boot server na porte z application.properties.
        SpringApplication.run(GameStudioServer.class, args);
    }

    @Bean
    @Primary
    public ScoreService scoreService() {
        // Server pouziva JPA implementaciu score sluzby.
        return new ScoreServiceJPA();
    }

    @Bean
    @Primary
    public CommentService commentService() {
        // Server pouziva JPA implementaciu komentarov.
        return new CommentServiceJPA();
    }

    @Bean
    @Primary
    public RatingService ratingService() {
        // Server pouziva JPA implementaciu ratingu.
        return new RatingServiceJPA();
    }

    @Bean
    @Primary
    public AuthService authService() {
        // Server pouziva JPA implementaciu prihlasenia a registracie.
        return new AuthServiceJPA();
    }

    @Bean
    public CommandLineRunner timestampColumnFix(JdbcTemplate jdbcTemplate) {
        // Pri starte opravim DB stlpce na timestamp, aby sa nestracal cas a neostalo iba 00:00.
        return args -> {
            alterColumnToTimestamp(jdbcTemplate, "\"comment\"", "commentedon");
            alterColumnToTimestamp(jdbcTemplate, "score", "playedon");
            alterColumnToTimestamp(jdbcTemplate, "rating", "ratedon");
            alterColumnToTimestamp(jdbcTemplate, "player_account", "registeredon");
        };
    }

    private static void alterColumnToTimestamp(JdbcTemplate jdbcTemplate, String table, String column) {
        try {
            // PostgreSQL prikaz pretypuje date/datetime stlpec na timestamp.
            jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN " + column
                    + " TYPE timestamp USING " + column + "::timestamp");
        } catch (Exception ignored) {
            // Ak tabulka este neexistuje alebo je stlpec uz spravny, server nema kvoli tomu spadnut.
        }
    }
}
