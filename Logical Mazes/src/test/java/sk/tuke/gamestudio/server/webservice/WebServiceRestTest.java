package sk.tuke.gamestudio.server.webservice;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WebServiceRestTest {

    @Test
    void testScoreRestDelegatesToService() {
        FakeScoreService service = new FakeScoreService();
        ScoreServiceRest rest = new ScoreServiceRest(service);
        Score score = new Score("marek", "logicalmazes_level_1", 8, new Date());

        // REST kontroler ma iba posunut objekt do service vrstvy
        rest.addScore(score);

        assertEquals(1, service.saved.size());
        assertSame(score, service.saved.get(0));
        assertEquals(1, rest.getTopScores("logicalmazes_level_1").size());
    }

    @Test
    void testScoreRestBestOverallDelegatesToService() {
        FakeScoreService service = new FakeScoreService();
        service.saved.add(new Score("marek", "logicalmazes_level_2", 3, new Date()));
        ScoreServiceRest rest = new ScoreServiceRest(service);

        List<Score> best = rest.getBestOverallScores("logicalmazes");

        assertEquals(1, best.size());
        assertEquals("logicalmazes", service.lastBestPrefix);
    }

    @Test
    void testCommentRestDelegatesToService() {
        FakeCommentService service = new FakeCommentService();
        CommentServiceRest rest = new CommentServiceRest(service);
        Comment comment = new Comment("marek", "logicalmazes_level_1", "ok", new Date());

        rest.addComment(comment);

        assertEquals(1, service.saved.size());
        assertSame(comment, service.saved.get(0));
        assertEquals(1, rest.getComments("logicalmazes_level_1").size());
    }

    @Test
    void testRatingRestDelegatesToService() {
        FakeRatingService service = new FakeRatingService();
        RatingServiceRest rest = new RatingServiceRest(service);
        Rating rating = new Rating("marek", "logicalmazes_level_1", 4, new Date());

        rest.setRating(rating);

        assertSame(rating, service.saved);
        assertEquals(4, rest.getRating("logicalmazes_level_1", "marek"));
        assertEquals(4, rest.getAverageRating("logicalmazes_level_1"));
    }

    private static class FakeScoreService implements ScoreService {
        private final List<Score> saved = new ArrayList<>();
        private String lastBestPrefix;

        @Override
        public void addScore(Score score) {
            saved.add(score);
        }

        @Override
        public List<Score> getTopScores(String game) {
            return saved.stream().filter(s -> s.getGame().equals(game)).toList();
        }

        @Override
        public void reset() {
            saved.clear();
        }

        @Override
        public List<Score> getBestOverallScores(String gamePrefix) {
            lastBestPrefix = gamePrefix;
            return saved;
        }
    }

    private static class FakeCommentService implements CommentService {
        private final List<Comment> saved = new ArrayList<>();

        @Override
        public void addComment(Comment comment) {
            saved.add(comment);
        }

        @Override
        public List<Comment> getComments(String game) {
            return saved.stream().filter(c -> c.getGame().equals(game)).toList();
        }

        @Override
        public void reset() {
            saved.clear();
        }
    }

    private static class FakeRatingService implements RatingService {
        private Rating saved;

        @Override
        public void setRating(Rating rating) {
            saved = rating;
        }

        @Override
        public int getAverageRating(String game) {
            return saved == null ? 0 : saved.getRating();
        }

        @Override
        public int getRating(String game, String player) {
            if (saved == null) return 0;
            if (!saved.getGame().equals(game)) return 0;
            if (!saved.getPlayer().equals(player)) return 0;
            return saved.getRating();
        }

        @Override
        public void reset() {
            saved = null;
        }
    }
}
