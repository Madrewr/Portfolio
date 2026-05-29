package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Score;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;

@Transactional
public class ScoreServiceJPA implements ScoreService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void addScore(Score score) throws ScoreException {
        // persist vlozi nove skore do databazy
        entityManager.persist(score);
    }

    @Override
    public List<Score> getTopScores(String game) throws ScoreException {
        // Vyberam len score pre jeden konkretny level.
        // ORDER BY points ASC znamena, ze menej tahov je lepsie.
        return entityManager.createQuery(
                        "SELECT s FROM Score s WHERE s.game = :game ORDER BY s.points ASC, s.playedOn DESC",
                        Score.class)
                .setParameter("game", game)
                // V tabulke na stranke nechcem zobrazovat nekonecne vela zaznamov.
                .setMaxResults(10)
                .getResultList();
    }

    @Override
    public List<Score> getBestOverallScores(String gamePrefix) {
        String normalizedPrefix = normalizeGamePrefix(gamePrefix);

        // Celkovy rebricek berie vsetky levely, preto hladam podla prefixu.
        List<Score> scores = entityManager.createQuery(
                        "SELECT s FROM Score s WHERE s.game LIKE :gp", Score.class)
                .setParameter("gp", normalizedPrefix + "%")
                .getResultList();

        scores.sort((a, b) -> {
            // Najprv porovnam level, aby boli tazsie/vyssie levely vyssie v rebricku.
            int la = extractLevel(a.getGame(), normalizedPrefix);
            int lb = extractLevel(b.getGame(), normalizedPrefix);

            if (la != lb) return Integer.compare(lb, la); // level desc

            // Pri rovnakom leveli vyhrava mensi pocet tahov.
            int pa = a.getPoints();
            int pb = b.getPoints();
            if (pa != pb) return Integer.compare(pa, pb); // tahy asc

            // Ak je remiza, novsie skore bude vyssie.
            if (a.getPlayedOn() == null && b.getPlayedOn() == null) return 0;
            if (a.getPlayedOn() == null) return 1;
            if (b.getPlayedOn() == null) return -1;
            return b.getPlayedOn().compareTo(a.getPlayedOn());
        });

        // Na stranke zobrazim maximalne top 10.
        return scores.size() > 10 ? scores.subList(0, 10) : scores;
    }

    private String normalizeGamePrefix(String gamePrefix) {
        // Web kontroler pouziva logicalmazes_level_, konzola historicky pouziva logicalmazes.
        if (gamePrefix == null || gamePrefix.isBlank()) {
            return "logicalmazes_level_";
        }

        String prefix = gamePrefix.trim();

        // Ak prefix uz konci na _level_, je pripraveny pre LIKE.
        if (prefix.endsWith("_level_")) {
            return prefix;
        }

        // Inak doplnim cast _level_.
        return prefix + "_level_";
    }

    private int extractLevel(String game, String prefix) {
        // Ak je nazov hry prazdny alebo z ineho prefixu, level neviem urcit.
        if (game == null) return 0;
        if (!game.startsWith(prefix)) return 0;

        // Pri prefixe logicalmazes_level_ ostane len cislo levelu.
        String s = game.substring(prefix.length());
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            // Chybny format nema zhodit celu stranku s rebrickom.
            return 0;
        }
    }

    @Override
    public void reset() throws ScoreException {
        // vymazem vsetky score z DB (pomocne na testovanie)
        try {
            entityManager.createQuery("DELETE FROM Score").executeUpdate();
        } catch (Exception e) {
            throw new ScoreException("DB reset error: " + e.getMessage());
        }
    }
}
