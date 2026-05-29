package sk.tuke.gamestudio.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTest {

    @Test
    void testScoreGetLevelFromGameKey() {
        Score score = new Score("marek", "logicalmazes_level_9", 12, new Date());

        // getLevel vytiahne cislo za poslednym podciarknutim
        assertEquals(9, score.getLevel());
    }

    @Test
    void testScoreGetLevelReturnsZeroForBadGameKey() {
        Score score = new Score("marek", "logicalmazes", 12, new Date());

        // ked v nazve nie je cislo levelu, metoda ma vratit 0 a nie spadnut
        assertEquals(0, score.getLevel());
    }

    @Test
    void testCommentDefaultDateIsFilledBeforePersist() {
        Comment comment = new Comment("marek", "logicalmazes_level_1", "text", null);

        // simulujem JPA callback pred ulozenim
        comment.setDefaultCommentedOn();

        assertNotNull(comment.getCommentedOn());
    }

    @Test
    void testRatingDefaultDateIsFilledBeforePersist() {
        Rating rating = new Rating("marek", "logicalmazes_level_1", 5, null);

        // simulujem JPA callback pred ulozenim
        rating.setDefaultRatedOn();

        assertNotNull(rating.getRatedOn());
    }

    @Test
    void testPlayerAccountDefaultDateIsFilledBeforePersist() {
        PlayerAccount account = new PlayerAccount("marek", "hash", null);

        // simulujem JPA callback pred ulozenim uctu
        account.setDefaultRegisteredOn();

        assertNotNull(account.getRegisteredOn());
    }

    @Test
    void testRatingIdEqualsAndHashCode() {
        Rating.RatingId a = new Rating.RatingId("marek", "logicalmazes_level_1");
        Rating.RatingId b = new Rating.RatingId("marek", "logicalmazes_level_1");
        Rating.RatingId c = new Rating.RatingId("ina", "logicalmazes_level_1");

        // rovnaky hrac a hra znamenaju rovnaky kluc
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // iny hrac znamena iny kluc
        assertNotEquals(a, c);
    }
}
