package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Score;

import java.util.List;

public interface ScoreService {
    // ulozi nove skore
    void addScore(Score score) throws ScoreException;

    // vrati top skore pre konkretnu hru alebo level
    List<Score> getTopScores(String game) throws ScoreException;

    // vymaze vsetky skore, pouziva sa hlavne v testoch
    void reset() throws ScoreException;

    // top vysledky celkovo, level je hlavne kriterium
    List<Score> getBestOverallScores(String gamePrefix) throws ScoreException;
}
