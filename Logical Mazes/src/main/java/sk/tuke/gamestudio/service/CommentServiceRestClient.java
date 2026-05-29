package sk.tuke.gamestudio.service;

import org.springframework.web.client.RestTemplate;
import sk.tuke.gamestudio.entity.Comment;

import java.util.List;

public class CommentServiceRestClient implements CommentService {
    // Zakladna URL REST API servera.
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api";

    // RestTemplate robi HTTP requesty na server.
    private final RestTemplate restTemplate = new RestTemplate();

    // baseUrl je bez koncoveho lomitka, aby sa endpointy skladali jednoducho.
    private final String baseUrl;

    public CommentServiceRestClient() {
        // Default url na lokalny server.
        this(DEFAULT_BASE_URL);
    }

    public CommentServiceRestClient(String baseUrl) {
        // Odstranim koncovy '/', aby sa dobre skladali endpointy.
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public void addComment(Comment comment) throws CommentException {
        try {
            // POST /api/comment ulozi komentar na serveri.
            restTemplate.postForEntity(baseUrl + "/comment", comment, Comment.class);
        } catch (Exception e) {
            throw new CommentException("Nepodarilo sa zapisat komentar cez REST", e);
        }
    }

    @Override
    public List<Comment> getComments(String game) throws CommentException {
        try {
            // GET /api/comment/{game} vrati komentare pre konkretny level.
            Comment[] comments = restTemplate.getForObject(baseUrl + "/comment/" + game, Comment[].class);
            return comments == null ? List.of() : List.of(comments);
        } catch (Exception e) {
            throw new CommentException("Nepodarilo sa nacitat komentare cez REST", e);
        }
    }

    @Override
    public void reset() throws CommentException {
        // V REST API nemame reset endpoint, aby sa cez web nahodou nevymazali data.
        throw new CommentException("Not supported via web service");
    }

    private String normalizeBaseUrl(String url) {
        // Ked pride prazdna URL, pouzijem default localhost server.
        if (url == null || url.isBlank()) return DEFAULT_BASE_URL;

        // Odstranim medzery a koncove lomitka.
        String u = url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }
}
