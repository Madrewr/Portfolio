package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Rating;

import java.sql.*;

public class RatingServiceJDBC implements RatingService {

    // db nastavenia su v jednej triede, aby som to nemusel kopirovat do kazdej triedy
    private static final String URL = DBSettings.URL;
    private static final String USER = DBSettings.USER;
    private static final String PASS = DBSettings.PASS;

    @Override
    public void setRating(Rating rating) throws RatingException {
        // rating ma primary key (player, game)
        // ked uz rating existuje, tak ho prepise (UPSERT)
        String sql = "INSERT INTO rating (player, game, rating, ratedon) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT (player, game) DO UPDATE SET rating = EXCLUDED.rating, ratedon = EXCLUDED.ratedon";

        // kontrola rozsahu 1-5, aby sa do DB nedalo ulozit blbost
        if (rating.getRating() < 1 || rating.getRating() > 5) {
            throw new RatingException("Rating musi byt 1-5");
        }

        // pripojenie do DB + prepared statement
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // doplnim parametre do sql
            ps.setString(1, rating.getPlayer()); // meno hraca
            ps.setString(2, rating.getGame());   // kluc hry / levelu
            ps.setInt(3, rating.getRating());    // cislo 1-5

            // Date -> Timestamp, aby sa nestratil cas a neostalo iba 00:00.
            ps.setTimestamp(4, new java.sql.Timestamp(rating.getRatedOn().getTime()));

            // vykonam insert/update
            ps.executeUpdate();

        } catch (SQLException e) {
            // ked DB zlyha, hodim vlastnu vynimku
            throw new RatingException("Chyba pri setRating", e);
        }
    }

    @Override
    public int getAverageRating(String game) throws RatingException {
        // vypocitam priemer ratingu pre konkretny level/hru
        // COALESCE je tam preto, aby ked nic nie je v DB, vratilo 0
        String sql = "SELECT COALESCE(AVG(rating), 0) FROM rating WHERE game = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // nastavim parameter game (napr. logicalmazes_level_2)
            ps.setString(1, game);

            // vykonam select
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // AVG v SQL vracia double
                    double avg = rs.getDouble(1);

                    // ja to chcem ako int, tak zaokruhlim
                    return (int) Math.round(avg);
                }
            }

        } catch (SQLException e) {
            throw new RatingException("Chyba pri getAverageRating", e);
        }

        // fallback, keby nic neprislo
        return 0;
    }

    @Override
    public int getRating(String game, String player) throws RatingException {
        // zistim rating konkretneho hraca pre konkretny level/hru
        String sql = "SELECT rating FROM rating WHERE game = ? AND player = ?";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // nastavim parametre do sql
            ps.setString(1, game);
            ps.setString(2, player);

            // vykonam select
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // ak existuje, vratim rating
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            throw new RatingException("Chyba pri getRating", e);
        }

        // ked rating neexistuje, vratim 0
        return 0;
    }

    @Override
    public void reset() throws RatingException {
        // vymaze vsetky ratingy v DB
        String sql = "DELETE FROM rating";

        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Statement st = con.createStatement()) {

            // vykonam delete
            st.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RatingException("Chyba pri reset rating", e);
        }
    }
}
