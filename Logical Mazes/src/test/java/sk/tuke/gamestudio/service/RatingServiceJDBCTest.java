package sk.tuke.gamestudio.service;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.entity.Rating;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class RatingServiceJDBCTest extends JdbcTestBase {

    private final RatingService ratingService = new RatingServiceJDBC();

    @Test
    void testSetAndGetRating() {
        ratingService.setRating(new Rating("a", "logicalmazes_level_1", 4, new Date()));

        int r = ratingService.getRating("logicalmazes_level_1", "a");
        assertEquals(4, r);
    }

    @Test
    void testUpdateRatingForSamePlayerAndGame() {
        // prvy rating
        ratingService.setRating(new Rating("a", "logicalmazes_level_2", 2, new Date()));
        assertEquals(2, ratingService.getRating("logicalmazes_level_2", "a"));

        // druhy rating sa ma prepisat (ON CONFLICT)
        ratingService.setRating(new Rating("a", "logicalmazes_level_2", 5, new Date()));
        assertEquals(5, ratingService.getRating("logicalmazes_level_2", "a"));
    }

    @Test
    void testGetRatingMissingReturns0() {
        int r = ratingService.getRating("logicalmazes_level_1", "neexistuje");
        assertEquals(0, r);
    }

    @Test
    void testAverageRatingRounding() {
        // 4 a 5 -> priemer 4.5 -> round = 5
        ratingService.setRating(new Rating("a", "logicalmazes_level_3", 4, new Date()));
        ratingService.setRating(new Rating("b", "logicalmazes_level_3", 5, new Date()));

        int avg = ratingService.getAverageRating("logicalmazes_level_3");
        assertEquals(5, avg);
    }

    @Test
    void testRatingRangeCheck() {
        // mimo rozsah 1-5 ma hodit exception
        assertThrows(RatingException.class, () -> {
            ratingService.setRating(new Rating("a", "logicalmazes_level_1", 0, new Date()));
        });
    }

    @Test
    void testResetClearsRatingTable() {
        ratingService.setRating(new Rating("a", "logicalmazes_level_1", 3, new Date()));
        assertEquals(3, ratingService.getRating("logicalmazes_level_1", "a"));

        ratingService.reset();

        assertEquals(0, ratingService.getRating("logicalmazes_level_1", "a"));
    }
}