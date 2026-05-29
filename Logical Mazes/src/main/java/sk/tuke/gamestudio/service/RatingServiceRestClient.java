package sk.tuke.gamestudio.service;

import org.springframework.web.client.RestTemplate;
import sk.tuke.gamestudio.entity.Rating;

public class RatingServiceRestClient implements RatingService {
    // Zakladna URL REST API servera.
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api";

    // RestTemplate robi HTTP requesty na server.
    private final RestTemplate restTemplate = new RestTemplate();

    // baseUrl je bez koncoveho lomitka, aby sa endpointy skladali jednoducho.
    private final String baseUrl;

    public RatingServiceRestClient() {
        // Default url na lokalny server.
        this(DEFAULT_BASE_URL);
    }

    public RatingServiceRestClient(String baseUrl) {
        // Ulozim normalizovanu URL bez koncoveho lomitka.
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    @Override
    public void setRating(Rating rating) throws RatingException {
        try {
            // POST /api/rating ulozi alebo aktualizuje rating.
            restTemplate.postForEntity(baseUrl + "/rating", rating, Rating.class);
        } catch (Exception e) {
            throw new RatingException("Nepodarilo sa ulozit rating cez REST", e);
        }
    }

    @Override
    public int getAverageRating(String game) throws RatingException {
        try {
            // GET /api/rating/{game} vrati priemerny rating.
            Integer avg = restTemplate.getForObject(baseUrl + "/rating/" + game, Integer.class);
            return avg == null ? 0 : avg;
        } catch (Exception e) {
            throw new RatingException("Nepodarilo sa nacitat priemerny rating cez REST", e);
        }
    }

    @Override
    public int getRating(String game, String player) throws RatingException {
        try {
            // GET /api/rating/{game}/{player} vrati rating konkretneho hraca.
            Integer r = restTemplate.getForObject(baseUrl + "/rating/" + game + "/" + player, Integer.class);
            return r == null ? 0 : r;
        } catch (Exception e) {
            throw new RatingException("Nepodarilo sa nacitat rating cez REST", e);
        }
    }

    @Override
    public void reset() throws RatingException {
        // V REST API nemame reset endpoint, aby sa cez web nahodou nevymazali data.
        throw new RatingException("Not supported via web service");
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
