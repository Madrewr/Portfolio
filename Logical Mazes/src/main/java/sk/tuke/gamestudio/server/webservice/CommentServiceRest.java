package sk.tuke.gamestudio.server.webservice;

import org.springframework.web.bind.annotation.*;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentServiceRest {

    // REST vrstva sama nepracuje s DB, iba vola CommentService.
    private final CommentService commentService;

    public CommentServiceRest(CommentService commentService) {
        // Spring sem injektuje implementaciu sluzby z GameStudioServer.
        this.commentService = commentService;
    }

    @PostMapping
    public void addComment(@RequestBody Comment comment) {
        // Telo requestu je JSON, Spring ho prevedie na objekt Comment.
        commentService.addComment(comment);
    }

    @GetMapping("/{game}")
    public List<Comment> getComments(@PathVariable String game) {
        // Vrati posledne komentare pre konkretny level.
        return commentService.getComments(game);
    }
}
