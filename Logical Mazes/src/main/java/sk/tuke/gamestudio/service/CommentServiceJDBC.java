package sk.tuke.gamestudio.service;

import sk.tuke.gamestudio.entity.Comment;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentServiceJDBC implements CommentService {

    // db nastavenia su v jednej triede, aby som to nemusel vsade kopirovat
    private static final String URL = DBSettings.URL;
    private static final String USER = DBSettings.USER;
    private static final String PASS = DBSettings.PASS;

    @Override
    public void addComment(Comment comment) throws CommentException {
        // sql na vlozenie komentara do tabulky comment
        String sql = "INSERT INTO comment (player, game, comment, commentedon) VALUES (?, ?, ?, ?)";

        // otvorim spojenie do DB + pripravim statement
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // doplnim parametre do sql (poradie musi sediet s VALUES)
            ps.setString(1, comment.getPlayer()); // meno hraca
            ps.setString(2, comment.getGame());   // kluc hry / levelu
            ps.setString(3, comment.getComment()); // text komentara

            // Date -> Timestamp, aby sa nestratil cas a neostalo iba 00:00.
            ps.setTimestamp(4, new java.sql.Timestamp(comment.getCommentedOn().getTime()));

            // vykonam insert
            ps.executeUpdate();

        } catch (SQLException e) {
            // ked je problem s DB, prehodim to na vlastnu vynimku
            throw new CommentException("Chyba pri addComment", e);
        }
    }

    @Override
    public List<Comment> getComments(String game) throws CommentException {
        // sql na nacitanie poslednych 10 komentarov pre konkretnu hru/level
        // zoradim to podla datumu od najnovsieho
        String sql = "SELECT player, game, comment, commentedon FROM comment WHERE game = ? " +
                "ORDER BY commentedon DESC LIMIT 10";

        // sem si budem ukladat vysledok
        List<Comment> list = new ArrayList<>();

        // otvorim spojenie a pripravim dotaz
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement ps = con.prepareStatement(sql)) {

            // nastavim parameter game (napr. logicalmazes_level_3)
            ps.setString(1, game);

            // vykonam select
            try (ResultSet rs = ps.executeQuery()) {

                // prechadzam vsetky riadky z DB
                while (rs.next()) {
                    // nacitam stlpce podla poradia v SELECT
                    String player = rs.getString(1);
                    String g = rs.getString(2);
                    String text = rs.getString(3);
                    // getTimestamp zachova aj hodiny a minuty.
                    Date d = rs.getTimestamp(4);

                    // vytvorim objekt Comment a pridam do listu
                    list.add(new Comment(player, g, text, d));
                }
            }

        } catch (SQLException e) {
            // ked je problem s DB, hodim vlastnu vynimku
            throw new CommentException("Chyba pri getComments", e);
        }

        // vratim zoznam komentarov
        return list;
    }

    @Override
    public void reset() throws CommentException {
        // vymazem vsetky komentare
        String sql = "DELETE FROM comment";

        // otvorim spojenie a vykonam delete
        try (Connection con = DriverManager.getConnection(URL, USER, PASS);
             Statement st = con.createStatement()) {

            st.executeUpdate(sql);

        } catch (SQLException e) {
            // ked je problem s DB, hodim vlastnu vynimku
            throw new CommentException("Chyba pri reset comment", e);
        }
    }
}
