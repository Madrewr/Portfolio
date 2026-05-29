package sk.tuke.gamestudio.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogicalMazesControllerTest {

    @Test
    void testShowClampsLevelToExistingMaxLevel() {
        LogicalMazesController controller = new LogicalMazesController(
                new FakeScoreService(),
                new FakeCommentService(),
                new FakeRatingService()
        );
        Model model = new ExtendedModelMap();

        String view = controller.show(15, model);

        // V resources je 9 levelov, preto request na 15 nesmie zhodit aplikaciu.
        assertEquals("logicalmazes", view);
        assertEquals(9, model.asMap().get("level"));
        assertEquals(9, model.asMap().get("maxLevel"));
    }

    @Test
    void testShowLoadsBoardAndServiceData() {
        FakeScoreService scoreService = new FakeScoreService();
        FakeCommentService commentService = new FakeCommentService();
        FakeRatingService ratingService = new FakeRatingService();
        LogicalMazesController controller = new LogicalMazesController(scoreService, commentService, ratingService);
        Model model = new ExtendedModelMap();

        controller.show(1, model);

        // Model musi obsahovat veci, ktore pouziva Thymeleaf sablona.
        assertNotNull(model.asMap().get("board"));
        assertNotNull(model.asMap().get("topScores"));
        assertNotNull(model.asMap().get("comments"));
        assertEquals(0, model.asMap().get("myRating"));
    }

    @Test
    void testBestOverallReturnsBestTemplate() {
        LogicalMazesController controller = new LogicalMazesController(
                new FakeScoreService(),
                new FakeCommentService(),
                new FakeRatingService()
        );
        Model model = new ExtendedModelMap();

        String view = controller.bestOverall(model);

        assertEquals("logicalmazes_best", view);
        assertNotNull(model.asMap().get("bestScores"));
    }

    @Test
    void testCommentRequiresPlayerName() {
        LogicalMazesController controller = new LogicalMazesController(
                new FakeScoreService(),
                new FakeCommentService(),
                new FakeRatingService()
        );

        String redirect = controller.addComment("ahoj");

        // Bez mena sa komentar nema ulozit.
        assertEquals("redirect:/logicalmazes?level=1", redirect);
    }

    private static class FakeScoreService implements ScoreService {
        private final List<Score> scores = new ArrayList<>();

        @Override
        public void addScore(Score score) {
            scores.add(score);
        }

        @Override
        public List<Score> getTopScores(String game) {
            return scores.stream().filter(s -> s.getGame().equals(game)).toList();
        }

        @Override
        public void reset() {
            scores.clear();
        }

        @Override
        public List<Score> getBestOverallScores(String gamePrefix) {
            return scores;
        }
    }

    private static class FakeCommentService implements CommentService {
        private final List<Comment> comments = new ArrayList<>();

        @Override
        public void addComment(Comment comment) {
            comments.add(comment);
        }

        @Override
        public List<Comment> getComments(String game) {
            return comments.stream().filter(c -> c.getGame().equals(game)).toList();
        }

        @Override
        public void reset() {
            comments.clear();
        }
    }

    private static class FakeRatingService implements RatingService {
        @Override
        public void setRating(Rating rating) {
            // V tomto teste rating neukladam, staci fake implementacia.
        }

        @Override
        public int getAverageRating(String game) {
            return 0;
        }

        @Override
        public int getRating(String game, String player) {
            return 0;
        }

        @Override
        public void reset() {
            // Nie je co mazat.
        }
    }
}
