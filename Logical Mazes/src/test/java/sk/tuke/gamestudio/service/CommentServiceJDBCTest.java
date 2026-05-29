package sk.tuke.gamestudio.service;

import org.junit.jupiter.api.Test;
import sk.tuke.gamestudio.entity.Comment;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommentServiceJDBCTest extends JdbcTestBase {

    private final CommentService commentService = new CommentServiceJDBC();

    @Test
    void testAddAndGetComments() {
        commentService.addComment(new Comment("a", "logicalmazes_level_1", "ok", new Date()));

        List<Comment> list = commentService.getComments("logicalmazes_level_1");
        assertEquals(1, list.size());
        assertEquals("ok", list.get(0).getComment());
    }

    @Test
    void testCommentsSeparatedByGameKey() {
        commentService.addComment(new Comment("a", "logicalmazes_level_1", "c1", new Date()));
        commentService.addComment(new Comment("a", "logicalmazes_level_2", "c2", new Date()));

        List<Comment> list1 = commentService.getComments("logicalmazes_level_1");
        List<Comment> list2 = commentService.getComments("logicalmazes_level_2");

        assertEquals(1, list1.size());
        assertEquals(1, list2.size());

        assertEquals("c1", list1.get(0).getComment());
        assertEquals("c2", list2.get(0).getComment());
    }

    @Test
    void testCommentsLimit10AndOrderByDateDesc() {
        // spravim 12 komentarov s rozdnymi datumami
        long day = 24L * 60 * 60 * 1000;

        for (int i = 0; i < 12; i++) {
            // starsie -> mensi time
            Date d = new Date(System.currentTimeMillis() - (11 - i) * day);
            commentService.addComment(new Comment("p" + i, "logicalmazes_level_3", "t" + i, d));
        }

        List<Comment> list = commentService.getComments("logicalmazes_level_3");

        // limit 10
        assertEquals(10, list.size());

        // prvy musi byt najnovsi (najvacsi datum)
        // kedze sme davali postupne datumy, posledny vkladany je najnovsi (t11)
        assertEquals("t11", list.get(0).getComment());
    }

    @Test
    void testResetClearsCommentTable() {
        commentService.addComment(new Comment("a", "logicalmazes_level_1", "ok", new Date()));
        assertEquals(1, commentService.getComments("logicalmazes_level_1").size());

        commentService.reset();

        assertEquals(0, commentService.getComments("logicalmazes_level_1").size());
    }

    @Test
    void testCommentKeepsTimeNotOnlyDate() {
        Date commentedOn = new Date(1767274200000L); // 01.01.2026 12:30:00 UTC približne
        commentService.addComment(new Comment("time", "logicalmazes_level_4", "cas", commentedOn));

        List<Comment> list = commentService.getComments("logicalmazes_level_4");

        assertEquals(1, list.size());
        assertEquals(commentedOn.getTime(), list.get(0).getCommentedOn().getTime());
    }
}
