package sk.tuke.gamestudio.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.game.logicalmazes.core.Direction;
import sk.tuke.gamestudio.game.logicalmazes.core.GameState;
import sk.tuke.gamestudio.game.logicalmazes.core.LevelLoader;
import sk.tuke.gamestudio.game.logicalmazes.core.Maze;
import sk.tuke.gamestudio.game.logicalmazes.core.TileState;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.ScoreService;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@Scope(WebApplicationContext.SCOPE_SESSION)
@RequestMapping("/logicalmazes")
public class LogicalMazesController {

    // Prefix pre nazov hry v databaze. Kazdy level ma vlastny zaznam, napr. logicalmazes_level_1.
    private static final String GAME_PREFIX = "logicalmazes_level_";

    // Pri celkovom rebricku hladame vsetky score, ktore patria levelom Logical Mazes.
    private static final String BEST_PREFIX = GAME_PREFIX;

    // Sluzby sa pouzivaju na ukladanie score, komentarov a ratingu do databazy.
    private final ScoreService scoreService;
    private final CommentService commentService;
    private final RatingService ratingService;

    // Pocet levelov sa zisti podla suborov v src/main/resources/levels.
    private final int maxLevel;

    // Aktualne zvoleny level v tejto session.
    private int level = 1;

    // Objekt Maze drzi realny stav hry: pozicia hraca, pocet tahov, zostavajuce collectible.
    private Maze maze;

    // Sem si ukladam tahy aktualnej hry, aby sa dali po vyhre prehrat ako replay.
    private List<Character> recordedMoves = new ArrayList<>();

    // Posledny uspesny replay ostava ulozeny aj po ulozeni skore.
    private List<Character> lastReplayMoves = new ArrayList<>();

    // Level, ku ktoremu patri posledny replay.
    private int lastReplayLevel = 1;

    // Ked je true, stranka prehrava replay a bezne tahy sa nezapisuju do zaznamu.
    private boolean replayMode = false;

    // Kolko krokov replayu je uz prehratych.
    private int replayStep = 0;

    // Meno hraca sa synchronizuje z HTTP session po prihlaseni.
    private String player = "";

    // Kratka sprava, ktora sa ukaze hore na stranke po akcii hraca.
    private String message = "";

    @Autowired
    public LogicalMazesController(ScoreService scoreService, CommentService commentService, RatingService ratingService) {
        this.scoreService = scoreService;
        this.commentService = commentService;
        this.ratingService = ratingService;

        // Ak by sa levely nahodou nenasli, nech aplikacia stale skusi aspon level 1.
        this.maxLevel = Math.max(1, findMaxLevel());

        // Po vytvoreni session kontrolera sa automaticky nacita prvy level.
        loadLevel(1);
    }

    @GetMapping
    public String show(@RequestParam(name = "level", required = false) Integer newLevel, Model model, HttpSession session) {
        // Pred zobrazenim stranky si nacitam prihlaseneho hraca zo session.
        syncPlayerFromSession(session);

        // Flash sprava pride napriklad po prihlaseni alebo registracii.
        Object flashMessage = model.asMap().get("message");

        // Level moze prist aj rucne cez URL, preto ho vzdy kontrolujem aj na serveri.
        if (newLevel != null) {
            int safeLevel = normalizeLevel(newLevel);

            // Ak pouzivatel zada level mimo rozsah, nenecham aplikaciu spadnut.
            if (safeLevel != newLevel) {
                message = "Level " + newLevel + " neexistuje. Vyber level 1-" + maxLevel + ".";
            }

            // Level nacitam iba vtedy, ked sa naozaj zmenil.
            if (safeLevel != level) {
                loadLevel(safeLevel);
            }
        }

        prepareModel(model);

        if (flashMessage instanceof String && !((String) flashMessage).isEmpty()) {
            model.addAttribute("message", flashMessage);
        }

        return "logicalmazes";
    }

    public String show(Integer newLevel, Model model) {
        // Tato pretazena metoda je tu hlavne pre jednoduche unit testy bez HTTP session.
        return show(newLevel, model, null);
    }

    @PostMapping("/move")
    public String move(@RequestParam("cmd") String cmd) {
        // cmd je znak z tlacidiel alebo klavesnice: w, a, s, d.
        if (cmd == null || cmd.isEmpty()) {
            return "redirect:/logicalmazes?level=" + level;
        }

        char c = cmd.charAt(0);

        // Manualny pohyb pocas replayu by pomiesal prehravanie, preto replay najprv vypnem.
        if (replayMode) {
            replayMode = false;
            message = "Replay zastaveny.";
        }

        // Po vyrieseni levelu uz nema zmysel hybat hracom, aby sa nemenilo ulozene score.
        if (maze.getState() == GameState.SOLVED) {
            message = "Uz je vyriesene. Daj restart alebo dalsi level.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // Podla klavesy zavolam pohyb v jadre hry.
        if (c == 'w') moveAndRecord(Direction.UP, 'w');
        if (c == 'a') moveAndRecord(Direction.LEFT, 'a');
        if (c == 's') moveAndRecord(Direction.DOWN, 's');
        if (c == 'd') moveAndRecord(Direction.RIGHT, 'd');

        // Ak sa po pohybe vyriesil level, pripravim spravu pre hraca.
        if (maze.getState() == GameState.SOLVED) {
            lastReplayMoves = new ArrayList<>(recordedMoves);
            lastReplayLevel = level;
            message = "Vyhral si! Uloz skore.";
        } else {
            message = "";
        }

        return "redirect:/logicalmazes?level=" + level;
    }

    @PostMapping("/restart")
    public String restart() {
        // Restart iba znova nacita aktualny level zo suboru.
        loadLevel(level);
        message = "Restart.";
        return "redirect:/logicalmazes?level=" + level;
    }

    @PostMapping("/replay")
    public String startReplay() {
        // Replay ma zmysel len vtedy, ked uz existuje zaznam vyhratej hry.
        if (lastReplayMoves.isEmpty()) {
            message = "Najprv vyries level, potom mozes spustit replay.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // Nacitam level od zaciatku, ale nemazem posledny ulozeny replay.
        loadLevel(lastReplayLevel, false);
        replayMode = true;
        replayStep = 0;
        message = "Replay spusteny.";
        return "redirect:/logicalmazes?level=" + level;
    }

    @PostMapping("/replay/step")
    public String replayStep() {
        // Tento endpoint vola JavaScript automaticky, vzdy posunie replay o jeden krok.
        if (!replayMode) {
            return "redirect:/logicalmazes?level=" + level;
        }

        if (replayStep >= lastReplayMoves.size()) {
            replayMode = false;
            message = "Replay dokonceny.";
            return "redirect:/logicalmazes?level=" + level;
        }

        applyReplayMove(lastReplayMoves.get(replayStep));
        replayStep++;

        if (replayStep >= lastReplayMoves.size()) {
            replayMode = false;
            message = "Replay dokonceny.";
        } else {
            message = "Replay: krok " + replayStep + " / " + lastReplayMoves.size();
        }

        return "redirect:/logicalmazes?level=" + level;
    }

    @PostMapping("/replay/stop")
    public String stopReplay() {
        // Hrac moze replay kedykolvek zastavit.
        replayMode = false;
        message = "Replay zastaveny.";
        return "redirect:/logicalmazes?level=" + level;
    }

    @GetMapping("/move")
    public String moveGet(@RequestParam(name = "cmd", required = false) String cmd) {
        // Pomocna GET verzia, aby sa dal pohyb testovat aj cez URL.
        return move(cmd);
    }

    @GetMapping("/restart")
    public String restartGet() {
        // Pomocna GET verzia restartu.
        return restart();
    }

    @PostMapping("/score")
    public String saveScore(HttpSession session) {
        // Pri kazdej DB akcii si overim aktualne prihlasenie.
        syncPlayerFromSession(session);

        // Score ukladam len prihlasenemu hracovi.
        if (player.isEmpty()) {
            message = "Najprv sa prihlas alebo zaregistruj.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // Skore sa ma ulozit az po vyrieseni levelu.
        if (maze.getState() != GameState.SOLVED) {
            message = "Najprv vyries level.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // Body su pocet tahov, cim menej tahov, tym lepsie score.
        scoreService.addScore(new Score(player, gameName(level), maze.getMoveCount(), new Date()));
        message = "Skore ulozene.";
        return "redirect:/logicalmazes?level=" + level;
    }

    @PostMapping("/comment")
    public String addComment(@RequestParam("text") String text, HttpSession session) {
        // Komentar ma byt podpisany prihlasenym uctom.
        syncPlayerFromSession(session);

        // Komentar moze pridat iba hrac, ktory ma zadane meno.
        if (player.isEmpty()) {
            message = "Najprv sa prihlas alebo zaregistruj.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // Prazdny komentar nema zmysel ukladat do databazy.
        if (text == null || text.trim().isEmpty()) {
            message = "Komentar je prazdny.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // new Date() ulozi realny cas pridania komentara.
        commentService.addComment(new Comment(player, gameName(level), text.trim(), new Date()));
        message = "Komentar ulozeny.";
        return "redirect:/logicalmazes?level=" + level;
    }

    public String addComment(String text) {
        // Tato pretazena metoda je tu hlavne pre jednoduche unit testy bez HTTP session.
        return addComment(text, null);
    }

    @PostMapping("/rating")
    public String setRating(@RequestParam("value") int value, HttpSession session) {
        // Rating patri prihlasenemu hracovi.
        syncPlayerFromSession(session);

        // Rating moze pridat iba hrac, ktory ma zadane meno.
        if (player.isEmpty()) {
            message = "Najprv sa prihlas alebo zaregistruj.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // Rating je povoleny iba od 1 do 5.
        if (value < 1 || value > 5) {
            message = "Rating musi byt 1-5.";
            return "redirect:/logicalmazes?level=" + level;
        }

        // V JPA sluzbe sa pouziva merge, preto dalsi rating od toho isteho hraca hodnotu aktualizuje.
        ratingService.setRating(new Rating(player, gameName(level), value, new Date()));
        message = "Hodnotenie ulozene.";
        return "redirect:/logicalmazes?level=" + level;
    }

    @GetMapping("/best")
    public String bestOverall(Model model) {
        // Celkovy rebricek berie score zo vsetkych levelov tejto hry.
        model.addAttribute("bestScores", scoreService.getBestOverallScores(BEST_PREFIX));
        return "logicalmazes_best";
    }

    private void loadLevel(int lvl) {
        loadLevel(lvl, true);
    }

    private void loadLevel(int lvl, boolean resetRecord) {
        // Aj interne si level pre istotu orezem na povoleny rozsah.
        this.level = normalizeLevel(lvl);

        // Nacitam mapu z resources/levels/levelX.txt.
        this.maze = LevelLoader.loadFromResource("levels/level" + this.level + ".txt");

        // Pri novej hre sa zacina novy zaznam tahov.
        if (resetRecord) {
            recordedMoves = new ArrayList<>();
            replayMode = false;
            replayStep = 0;
        }
    }

    private void moveAndRecord(Direction direction, char command) {
        // Najprv ulozim znak pre replay a potom vykonam realny pohyb.
        recordedMoves.add(command);
        maze.move(direction);
    }

    private void applyReplayMove(char command) {
        // Replay pouziva rovnaku logiku pohybu ako realna hra, len sa uz znova nezaznamenava.
        if (command == 'w') maze.move(Direction.UP);
        if (command == 'a') maze.move(Direction.LEFT);
        if (command == 's') maze.move(Direction.DOWN);
        if (command == 'd') maze.move(Direction.RIGHT);
    }

    private int normalizeLevel(int requestedLevel) {
        // Mensie cislo ako 1 nema vyznam, preto ho zmenim na 1.
        if (requestedLevel < 1) {
            return 1;
        }

        // Vacsi level ako existuje by zhodil nacitanie suboru, preto ho orezem.
        if (requestedLevel > maxLevel) {
            return maxLevel;
        }

        return requestedLevel;
    }

    private int findMaxLevel() {
        int found = 0;

        // Levely su pomenovane level1.txt, level2.txt, ...
        // Cyklus ide postupne, kym existuje dalsi subor v resources.
        while (LevelLoader.class.getClassLoader().getResource("levels/level" + (found + 1) + ".txt") != null) {
            found++;
        }

        return found;
    }

    private void syncPlayerFromSession(HttpSession session) {
        if (session == null) {
            this.player = "";
            return;
        }

        // Prihlaseny hrac je ulozeny v session pod jednym spolocnym klucom.
        Object loggedPlayer = session.getAttribute(AuthController.SESSION_PLAYER);
        this.player = loggedPlayer == null ? "" : loggedPlayer.toString();
    }

    private void prepareModel(Model model) {
        // Zakladne udaje pre hlavicku stranky.
        model.addAttribute("level", level);
        model.addAttribute("maxLevel", maxLevel);
        model.addAttribute("player", player);
        model.addAttribute("loggedIn", !player.isEmpty());
        model.addAttribute("moves", maze.getMoveCount());
        model.addAttribute("remaining", maze.getRemainingCollectibles());
        model.addAttribute("solved", maze.getState() == GameState.SOLVED);
        model.addAttribute("message", message);
        model.addAttribute("replayAvailable", !lastReplayMoves.isEmpty());
        model.addAttribute("replayMode", replayMode);
        model.addAttribute("replayStep", replayStep);
        model.addAttribute("replayTotal", lastReplayMoves.size());

        // Board je jednoducha verzia mapy pripravena pre Thymeleaf.
        model.addAttribute("board", buildBoard());

        String g = gameName(level);

        // Sluzby sa nacitaju vzdy pre aktualny level.
        model.addAttribute("topScores", scoreService.getTopScores(g));
        model.addAttribute("comments", commentService.getComments(g));
        model.addAttribute("avgRating", ratingService.getAverageRating(g));

        // Ak hrac nema meno, nema ani vlastny rating.
        if (player.isEmpty()) {
            model.addAttribute("myRating", 0);
        } else {
            model.addAttribute("myRating", ratingService.getRating(g, player));
        }
    }

    private List<List<CellType>> buildBoard() {
        List<List<CellType>> board = new ArrayList<>();

        // Prejdem kazdy riadok a stlpec jadra hry.
        for (int r = 0; r < maze.getRows(); r++) {
            List<CellType> row = new ArrayList<>();
            for (int c = 0; c < maze.getCols(); c++) {
                // Poradie je dolezite: najprv stena, potom hrac, potom collectible, nakoniec prazdne pole.
                if (maze.getCell(r, c).isBlocked()) {
                    row.add(CellType.WALL);
                } else if (r == maze.getPlayerRow() && c == maze.getPlayerCol()) {
                    row.add(CellType.PLAYER);
                } else if (maze.getCell(r, c).getState() == TileState.COLLECTIBLE) {
                    row.add(CellType.COLLECTIBLE);
                } else {
                    row.add(CellType.EMPTY);
                }
            }
            board.add(row);
        }

        return board;
    }

    private String gameName(int level) {
        // Toto meno sa pouziva v DB, aby mal kazdy level vlastne score, komentare a rating.
        return GAME_PREFIX + level;
    }

    public enum CellType {
        // Typy buniek, ktore vie HTML sablona zobrazit cez CSS.
        WALL, PLAYER, COLLECTIBLE, EMPTY
    }
}
