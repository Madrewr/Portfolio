package sk.tuke.gamestudio.service;

import org.springframework.web.client.RestTemplate;
import sk.tuke.gamestudio.entity.Score;

import java.util.List;

public class ScoreServiceRestClient implements ScoreService {
    // Zakladna URL REST API servera.
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api";

    // RestTemplate robi HTTP requesty na server.
    private final RestTemplate restTemplate = new RestTemplate();

    // baseUrl je bez koncoveho lomitka, aby sa endpointy skladali jednoducho.
    private final String baseUrl;

    public ScoreServiceRestClient() {
        // Default url na lokalny server.
        this(DEFAULT_BASE_URL);
    }

    public ScoreServiceRestClient(String baseUrl) {
        // Ulozim normalizovanu URL bez koncoveho lomitka.
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public void addScore(Score score) throws ScoreException {
        try {
            // POST /api/score ulozi skore na serveri.
            restTemplate.postForEntity(baseUrl + "/score", score, Score.class);
        } catch (Exception e) {
            throw new ScoreException("Nepodarilo sa ulozit score cez REST", e);
        }
    }

    @Override
    public List<Score> getTopScores(String game) throws ScoreException {
        try {
            // GET /api/score/{game} vrati top score pre jeden level.
            Score[] scores = restTemplate.getForObject(baseUrl + "/score/" + game, Score[].class);
            return scores == null ? List.of() : List.of(scores);
        } catch (Exception e) {
            throw new ScoreException("Nepodarilo sa nacitat top scores cez REST", e);
        }
    }

    @Override
    public List<Score> getBestOverallScores(String game) throws ScoreException {
        // Tu sa pouziva prefix hry, napr. "logicalmazes".
        try {
            // GET /api/score/best/{game} vrati celkovy rebricek.
            Score[] scores = restTemplate.getForObject(baseUrl + "/score/best/" + game, Score[].class);
            return scores == null ? List.of() : List.of(scores);
        } catch (Exception e) {
            throw new ScoreException("Nepodarilo sa nacitat best overall cez REST", e);
        }
    }

    @Override
    public void reset() throws ScoreException {
        // V REST API nemame reset endpoint, aby sa cez web nahodou nevymazali data.
        throw new ScoreException("Not supported via web service");
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
