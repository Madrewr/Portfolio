package sk.tuke.gamestudio.service;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.entity.Score;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScoreServiceJDBCTest extends JdbcTestBase {

    private final ScoreService scoreService = new ScoreServiceJDBC();

    @Test
    void testAddAndGetTopScoresSortedByPointsAsc() {
        // pridam 3 score do rovnakeho levelu
        scoreService.addScore(new Score("a", "logicalmazes_level_1", 10, new Date()));
        scoreService.addScore(new Score("b", "logicalmazes_level_1", 3, new Date()));
        scoreService.addScore(new Score("c", "logicalmazes_level_1", 7, new Date()));

        List<Score> list = scoreService.getTopScores("logicalmazes_level_1");

        // musi byt 3 zaznamy
        assertEquals(3, list.size());

        // najmenej tahov musi byt prvy
        assertEquals(3, list.get(0).getPoints());
        assertEquals(7, list.get(1).getPoints());
        assertEquals(10, list.get(2).getPoints());
    }

    @Test
    void testTopScoresOnlyForGivenGameKey() {
        // pridam score pre level1 aj level2
        scoreService.addScore(new Score("a", "logicalmazes_level_1", 5, new Date()));
        scoreService.addScore(new Score("b", "logicalmazes_level_2", 1, new Date()));

        List<Score> list1 = scoreService.getTopScores("logicalmazes_level_1");
        List<Score> list2 = scoreService.getTopScores("logicalmazes_level_2");

        assertEquals(1, list1.size());
        assertEquals(1, list2.size());

        assertEquals("logicalmazes_level_1", list1.get(0).getGame());
        assertEquals("logicalmazes_level_2", list2.get(0).getGame());
    }

    @Test
    void testTopScoresLimit10() {
        // pridam 12 zaznamov, ale chcem max 10 vo vysledku
        for (int i = 0; i < 12; i++) {
            // points su i, aby sa to dalo pekne zoradit
            scoreService.addScore(new Score("p" + i, "logicalmazes_level_3", i, new Date()));
        }

        List<Score> list = scoreService.getTopScores("logicalmazes_level_3");

        assertEquals(10, list.size());

        // prvy musi mat points 0 (najmenej)
        assertEquals(0, list.get(0).getPoints());
    }

    @Test
    void testResetClearsScoreTable() {
        scoreService.addScore(new Score("a", "logicalmazes_level_1", 5, new Date()));
        assertEquals(1, scoreService.getTopScores("logicalmazes_level_1").size());

        scoreService.reset();

        assertEquals(0, scoreService.getTopScores("logicalmazes_level_1").size());
    }

    @Test
    void testBestOverallPrefersHigherLevelEvenWithWorsePoints() {
        // level 7 ma menej tahov, ale level 8 ma byt celkovo vyssie
        scoreService.addScore(new Score("a", "logicalmazes_level_7", 1, new Date()));
        scoreService.addScore(new Score("b", "logicalmazes_level_8", 50, new Date()));

        List<Score> list = scoreService.getBestOverallScores("logicalmazes");

        assertFalse(list.isEmpty());
        assertTrue(list.get(0).getGame().contains("_level_8"));
    }

    @Test
    void testBestOverallAlsoAcceptsFullLevelPrefix() {
        scoreService.addScore(new Score("a", "logicalmazes_level_1", 1, new Date()));
        scoreService.addScore(new Score("b", "logicalmazes_level_2", 1, new Date()));

        List<Score> list = scoreService.getBestOverallScores("logicalmazes_level_");

        // Ked prefix uz obsahuje _level_, sluzba ho nesmie pokazit.
        assertEquals(2, list.size());
        assertEquals("logicalmazes_level_2", list.get(0).getGame());
    }

    @Test
    void testBestOverallTieBreakByPointsAsc() {
        // rovnaky level 9, ale iny pocet tahov
        scoreService.addScore(new Score("a", "logicalmazes_level_9", 20, new Date()));
        scoreService.addScore(new Score("b", "logicalmazes_level_9", 5, new Date()));

        List<Score> list = scoreService.getBestOverallScores("logicalmazes");

        // prve musi byt level 9 s 5 tahmi
        assertTrue(list.get(0).getGame().contains("_level_9"));
        assertEquals(5, list.get(0).getPoints());
    }

    @Test
    void testScoreKeepsTimeNotOnlyDate() {
        Date playedOn = new Date(1767274200000L); // 01.01.2026 12:30:00 UTC približne
        scoreService.addScore(new Score("time", "logicalmazes_level_4", 4, playedOn));

        List<Score> list = scoreService.getTopScores("logicalmazes_level_4");

        assertEquals(1, list.size());
        assertEquals(playedOn.getTime(), list.get(0).getPlayedOn().getTime());
    }
}
