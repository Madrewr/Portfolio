package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Score;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScoreServiceJDBC implements ScoreService {

    // db nastavenia su v jednej triede, aby som to nemusel vsade kopirovat
    private static final String URL = DBSettings.URL;
    private static final String USER = DBSettings.USER;
    private static final String PASS = DBSettings.PASS;

    @Override
    public void addScore(Score score) throws ScoreException {
        // sql na vlozenie score do tabulky score
        String sql = "INSERT INTO score (player, game, points, playedon) VALUES (?, ?, ?, ?)";

        // otvorim spojenie a pripravim insert
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // doplnim parametre (musi sediet poradie s VALUES)
            ps.setString(1, score.getPlayer());  // meno hraca
            ps.setString(2, score.getGame());    // kluc hry/levelu (napr. logicalmazes_level_3)
            ps.setInt(3, score.getPoints());     // body (u teba tahy, menej je lepsie)

            // Date -> Timestamp, aby sa nestratil cas a neostalo iba 00:00.
            ps.setTimestamp(4, new java.sql.Timestamp(score.getPlayedOn().getTime()));

            // vykonam insert
            ps.executeUpdate();

        } catch (SQLException e) {
            // ked DB zlyha, hodim vlastnu vynimku
            throw new ScoreException("Chyba pri addScore", e);
        }
    }

    @Override
    public List<Score> getTopScores(String game) throws ScoreException {
        // top 10 pre konkretny level/hru
        // points = pocet tahov, preto ORDER BY points ASC (najmenej tahov je najlepsie)
        String sql = "SELECT player, game, points, playedon FROM score WHERE game = ? " +
                "ORDER BY points ASC, playedon DESC LIMIT 10";

        // sem budem ukladat vysledok
        List<Score> list = new ArrayList<>();

        // otvorim spojenie a pripravim select
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // nastavim parameter game (napr. logicalmazes_level_1)
            ps.setString(1, game);

            // vykonam select
            try (ResultSet rs = ps.executeQuery()) {

                // prechadzam riadky z DB
                while (rs.next()) {
                    // nacitam stlpce podla poradia v SELECT
                    String player = rs.getString(1);
                    String g = rs.getString(2);
                    int points = rs.getInt(3);
                    // getTimestamp zachova aj hodiny a minuty.
                    Date playedOn = rs.getTimestamp(4);

                    // vytvorim Score objekt a pridam do listu
                    list.add(new Score(player, g, points, playedOn));
                }
            }

        } catch (SQLException e) {
            throw new ScoreException("Chyba pri getTopScores", e);
        }

        // vratim top list
        return list;
    }

    @Override
    public void reset() throws ScoreException {
        // vymaze vsetky score (pozor: realne to vymaze data v DB)
        String sql = "DELETE FROM score";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Statement st = con.createStatement()) {

            // vykonam delete
            st.executeUpdate(sql);

        } catch (SQLException e) {
            throw new ScoreException("Chyba pri reset score", e);
        }
    }

    @Override
    public List<Score> getBestOverallScores(String gamePrefix) throws ScoreException {
        // toto je globalny rebricek
        // game je napr. logicalmazes_level_1
        // 1) vyssi level je lepsi (DESC)
        // 2) pri rovnakom leveli menej tahov je lepsie (ASC)
        // 3) pri rovnakych tahoch preferujem novsi zaznam (DESC)
        String normalizedPrefix = normalizeGamePrefix(gamePrefix);

        String sql =
                "SELECT player, game, points, playedon " +
                        "FROM score " +
                        "WHERE game LIKE ? " +
                        "ORDER BY " +
                        "CAST(regexp_replace(game, '.*_level_', '') AS INTEGER) DESC, " +
                        "points ASC, playedon DESC " +
                        "LIMIT 10";

        // sem si ukladam vysledok
        List<Score> list = new ArrayList<>();

        // otvorim spojenie a pripravim select
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Vyberiem vsetky levely pre danu hru.
            // Funguje pre vstup logicalmazes aj logicalmazes_level_.
            ps.setString(1, normalizedPrefix + "%");

            // vykonam select
            try (ResultSet rs = ps.executeQuery()) {

                // prechadzam riadky z DB
                while (rs.next()) {
                    String player = rs.getString(1);
                    String g = rs.getString(2);
                    int points = rs.getInt(3);
                    // getTimestamp zachova aj hodiny a minuty.
                    Date playedOn = rs.getTimestamp(4);

                    // pridam do listu
                    list.add(new Score(player, g, points, playedOn));
                }
            }

        } catch (SQLException e) {
            throw new ScoreException("Chyba pri getBestOverallScores", e);
        }

        return list;
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
}
