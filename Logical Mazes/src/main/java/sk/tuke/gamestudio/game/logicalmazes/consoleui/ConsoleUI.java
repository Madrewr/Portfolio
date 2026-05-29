package sk.tuke.gamestudio.game.logicalmazes.consoleui;

import sk.tuke.gamestudio.entity.Comment;
import sk.tuke.gamestudio.entity.Rating;
import sk.tuke.gamestudio.entity.Score;
import sk.tuke.gamestudio.game.logicalmazes.core.Direction;
import sk.tuke.gamestudio.game.logicalmazes.core.GameState;
import sk.tuke.gamestudio.game.logicalmazes.core.LevelLoader;
import sk.tuke.gamestudio.game.logicalmazes.core.Maze;
import sk.tuke.gamestudio.game.logicalmazes.core.TileState;
import sk.tuke.gamestudio.service.CommentService;
import sk.tuke.gamestudio.service.CommentServiceJDBC;
import sk.tuke.gamestudio.service.RatingService;
import sk.tuke.gamestudio.service.RatingServiceJDBC;
import sk.tuke.gamestudio.service.ScoreService;
import sk.tuke.gamestudio.service.ScoreServiceJDBC;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    // scanner na vstup z konzoly
    private final Scanner scanner = new Scanner(System.in);

    // nazov hry do DB
    private static final String GAME_NAME = "logicalmazes";

    // sluzby (mozu byt JDBC alebo REST klient)
    private ScoreService scoreService;
    private CommentService commentService;
    private RatingService ratingService;

    // meno hraca si ulozim, aby som sa nepytal furt dokola
    private String playerName = null;

    // default konstruktor (ked spustam cez tvoj Main a chcem JDBC)
    public ConsoleUI() {
        this.scoreService = new ScoreServiceJDBC();
        this.commentService = new CommentServiceJDBC();
        this.ratingService = new RatingServiceJDBC();
    }

    // konstruktor ked chcem dat sluzby zvonku (napr. REST klient)
    public ConsoleUI(ScoreService scoreService, CommentService commentService, RatingService ratingService) {
        this.scoreService = scoreService;
        this.commentService = commentService;
        this.ratingService = ratingService;
    }

    // farby do konzoly - keby nechcem farby tak false
    private static final boolean USE_COLORS = true;

    // zakladne ansi farby
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_GRAY = "\u001B[90m";

    // menu
    public int showMenuAndGetStartLevel() {
        // budem sa pytat, kym hrac neda normalnu moznost
        while (true) {
            System.out.println();
            System.out.println("===== MENU =====");
            System.out.println("1 - start od levelu 1");
            System.out.println("2 - vybrat level");
            System.out.println("3 - vypisat najlepsie celkovo");
            System.out.println("4 - napoveda");
            System.out.println("5 - koniec");
            System.out.print("> ");

            String s = scanner.nextLine().trim();

            // start od levelu 1
            if (s.equals("1")) {
                return 1;
            }

            // vyber levelu
            if (s.equals("2")) {
                System.out.print("Zadaj cislo levelu: ");
                String x = scanner.nextLine().trim();

                // snazim sa prekonvertovat na cislo
                try {
                    int lvl = Integer.parseInt(x);

                    // level musi byt aspon 1
                    if (lvl >= 1) {
                        return lvl;
                    }
                } catch (Exception ignored) {
                    // nechcem tu riesit nic, len vypisem ze je to zle
                }

                System.out.println("Zle cislo levelu.");
                continue;
            }

            // vypis najlepsich celkovo
            if (s.equals("3")) {
                System.out.println();
                System.out.println("NAJLEPSIE CELKOVO: ");
                showBestOverall();
                continue;
            }

            // napoveda
            if (s.equals("4")) {
                printHelp();
                continue;
            }

            // koniec
            if (s.equals("5")) {
                return 0;
            }

            // ked nic nesedi, tak vypisem chybu
            System.out.println("Zly vyber.");
        }
    }

    // napoveda pre hraca
    private void printHelp() {
        System.out.println();
        System.out.println("NAPOVEDA:");
        System.out.println("- pohybujes sa w/a/s/d");
        System.out.println("- postava sa klze az po prekazku alebo okraj");
        System.out.println("- C su veci na zber (musis pozbierat vsetky)");
        System.out.println("- r = restart levelu, q = koniec");
        System.out.println("- h = napoveda");
        System.out.println("- po vyhre mozes dat p = replay poslednej vyhry");
        System.out.println();
    }

    // pomocna funkcia na farby
    private String color(String s, String ansi) {
        // ked farby nechcem, vratim normalne
        if (!USE_COLORS) return s;

        // inak obalim do ANSI kodov
        return ansi + s + ANSI_RESET;
    }

    // hlavna metoda pre 1 level
    public PlayResult play(Maze maze, int levelNumber) {
        // ulozenie krokov pre replay
        List<Character> recordedMoves = new ArrayList<>();

        // herna slucka pre level
        while (maze.getState() == GameState.PLAYING) {
            // hud + vykreslenie pola
            show(maze, levelNumber);

            // spracovanie vstupu od hraca
            PlayResult res = handleInput(maze, recordedMoves);

            // ked chce restart alebo quit, vratim to do Main
            if (res == PlayResult.RESTART || res == PlayResult.QUIT) {
                return res;
            }
        }

        // ak sme vypadli z while, tak uz nie sme PLAYING (to znamena SOLVED)
        show(maze, levelNumber);
        System.out.println("Vyhral si level " + levelNumber + "!");

        // score ratam ako pocet tahov (moveCount), menej je lepsie

        // uloz score + vypisy z DB
        try {
            // pytam meno len raz
            String player = getPlayerName();

            // do db si ulozim aj level, aby level bol hlavny
            String gameKey = GAME_NAME + "_level_" + levelNumber;

            // body = tahy za level (menej = lepsie)
            int points = maze.getMoveCount();

            // zapis score do DB
            scoreService.addScore(new Score(player, gameKey, points, new Date()));

            // vypisem co som dosiahol
            System.out.println();
            System.out.println("Tvoje tahy: " + points);

            // vypis top pre aktualny level (z DB)
            showTopScores(gameKey);

            // vypis najlepsie celkovo (z DB)
            System.out.println();
            System.out.println("NAJLEPSIE CELKOVO (level je hlavny):");
            showBestOverall();

            // rating + comment pre aktualny level (z DB)
            askRatingAndComment(gameKey);

            // replay poslednej vyhry
            askReplay(levelNumber, recordedMoves);

        } catch (Exception e) {
            // ked DB nejde, nech hra stale bezi
            System.out.println("Nepodarilo sa zapisat/vypisat DB: " + e.getMessage());
        }

        return PlayResult.WON;
    }

    // meno hraca si pytame len raz
    private String getPlayerName() {
        // ak uz mam meno, vratim ho
        if (playerName != null) {
            return playerName;
        }

        // pytam sa kym nedostanem neprazdny string
        while (true) {
            System.out.print("Zadaj meno hraca: ");
            String s = scanner.nextLine().trim();

            // ked je neprazdne, ulozim a vratim
            if (!s.isEmpty()) {
                playerName = s;
                return playerName;
            }

            // ked je prazdne, vypisem chybu
            System.out.println("Meno nemoze byt prazdne.");
        }
    }

    // hud + farby + vykreslenie pola
    private void show(Maze maze, int levelNumber) {
        System.out.println();

        // nazov a level
        System.out.println("Logical Mazes | level " + levelNumber);

        // hud info
        System.out.println("Hrac: " + (playerName == null ? "-" : playerName));
        System.out.println("Tahov: " + maze.getMoveCount());
        System.out.println("Zostava C: " + maze.getRemainingCollectibles());

        // ovladanie
        System.out.println("Ovladas: w/a/s/d, h = napoveda, r = restart, q = koniec");
        System.out.println();

        // vykreslenie pola po riadkoch
        for (int r = 0; r < maze.getRows(); r++) {
            StringBuilder sb = new StringBuilder();

            // vykreslenie jedneho riadku
            for (int c = 0; c < maze.getCols(); c++) {

                // hrac (P)
                if (r == maze.getPlayerRow() && c == maze.getPlayerCol()) {
                    // P - zelene
                    sb.append(color("P", ANSI_GREEN));
                    continue;
                }

                // stena
                if (maze.getCell(r, c).isBlocked()) {
                    // # - siva
                    sb.append(color("#", ANSI_GRAY));
                    continue;
                }

                // collectible (C)
                if (maze.getCell(r, c).getState() == TileState.COLLECTIBLE) {
                    // C - cervena
                    sb.append(color("C", ANSI_RED));
                    continue;
                }

                // prazdne policko
                sb.append(".");
            }

            // vypisem riadok
            System.out.println(sb);
        }
    }

    // vstupy a pohyb
    private PlayResult handleInput(Maze maze, List<Character> recordedMoves) {
        // nacitam vstup
        System.out.print("> ");
        String line = scanner.nextLine().trim().toLowerCase();

        // ak je prazdne, nic nerobim
        if (line.isEmpty()) {
            return null;
        }

        // beriem len prvy znak
        char ch = line.charAt(0);

        // ovladanie cez switch
        switch (ch) {
            case 'w' -> {
                // pohyb hore
                maze.move(Direction.UP);

                // ulozim do recordu na replay
                recordedMoves.add('w');
            }
            case 's' -> {
                // pohyb dole
                maze.move(Direction.DOWN);
                recordedMoves.add('s');
            }
            case 'a' -> {
                // pohyb dolava
                maze.move(Direction.LEFT);
                recordedMoves.add('a');
            }
            case 'd' -> {
                // pohyb doprava
                maze.move(Direction.RIGHT);
                recordedMoves.add('d');
            }
            case 'h' -> {
                // napoveda
                printHelp();
            }
            case 'r' -> {
                // restart levelu - toto si Main potom nacita znovu
                return PlayResult.RESTART;
            }
            case 'q' -> {
                // koniec hry
                return PlayResult.QUIT;
            }
            default -> {
                // neznamy prikaz
                System.out.println("Neznamy vstup, daj w/a/s/d alebo r alebo q alebo h.");
            }
        }

        return null;
    }

    // otazka po vyhre, ci chce hrac dalsi level
    public boolean askNextLevel() {
        // pytam sa dokola kym nedostanem y/n
        while (true) {
            System.out.print("Chces ist na dalsi level? (y/n): ");
            String s = scanner.nextLine().trim().toLowerCase();

            if (s.equals("y")) return true;
            if (s.equals("n")) return false;

            // zly vstup
            System.out.println("Zadaj y alebo n.");
        }
    }

    // top pre aktualny level
    private void showTopScores(String gameKey) {
        // zoberiem top 10 score pre level
        List<Score> list = scoreService.getTopScores(gameKey);

        System.out.println("TOP PRE: " + gameKey);

        // ked nic nie je v DB, vypisem info
        if (list.isEmpty()) {
            System.out.println("- zatial nic v databaze");
            return;
        }

        // vypisem poradie
        int i = 1;
        for (Score s : list) {
            System.out.println(i + ". " + s.getPlayer() + " | tahy " + s.getPoints() + " | " + s.getPlayedOn());
            i++;
        }

        // priemer ratingu pre tento level
        int avg = ratingService.getAverageRating(gameKey);
        System.out.println("Priemerne hodnotenie levelu: " + avg + "/5");
    }

    // najlepsie celkovo (level je hlavny)
    private void showBestOverall() {
        // toto zoberie z DB top 10 a zoradi podla levelu desc a tahy asc
        List<Score> list = scoreService.getBestOverallScores(GAME_NAME);

        // ak nic nie je v DB, vypisem to
        if (list.isEmpty()) {
            System.out.println("- zatial nic v databaze");
            return;
        }

        // vypisem poradie
        int i = 1;
        for (Score s : list) {
            // z game vytiahnem len level cislo na vypis
            String levelText = s.getGame().replace(GAME_NAME + "_level_", "");

            // vypis
            System.out.println(i + ". " + s.getPlayer() + " | level " + levelText + " | tahy " + s.getPoints());
            i++;
        }
    }

    // rating + comment pre konkretny level (gameKey)
    private void askRatingAndComment(String gameKey) {
        // hrac
        String player = getPlayerName();

        // rating (moze sa preskocit)
        while (true) {
            System.out.print("Daj hodnotenie 1-5 (alebo enter preskoc): ");
            String s = scanner.nextLine().trim();

            // enter = preskoc
            if (s.isEmpty()) {
                break;
            }

            // skusim prekonvertovat na int
            try {
                int r = Integer.parseInt(s);

                // ulozim rating do DB pre konkretne gameKey
                ratingService.setRating(new Rating(player, gameKey, r, new Date()));
                System.out.println("Ok, rating ulozeny.");
                break;
            } catch (Exception e) {
                // zle cislo
                System.out.println("Zle cislo, daj 1-5.");
            }
        }

        // komentar (moze sa preskocit)
        System.out.print("Napis komentar (alebo enter preskoc): ");
        String text = scanner.nextLine().trim();

        // ked nie je prazdny, ulozim
        if (!text.isEmpty()) {
            commentService.addComment(new Comment(player, gameKey, text, new Date()));
            System.out.println("Ok, komentar ulozeny.");
        }

        // vypis posledne komentare pre tento level
        List<Comment> list = commentService.getComments(gameKey);

        System.out.println("Komentare pre " + gameKey + ":");
        if (list.isEmpty()) {
            System.out.println("- zatial nic");
        } else {
            // vypisem kazdy komentar
            for (Comment c : list) {
                System.out.println("- " + c.getPlayer() + ": " + c.getComment());
            }
        }
    }

    // replay poslednej vyhry
    private void askReplay(int levelNumber, List<Character> recordedMoves) {
        // replay ma zmysel len ked mam nejake kroky
        if (recordedMoves == null || recordedMoves.isEmpty()) {
            return;
        }

        // spytam sa ci chce replay
        System.out.print("Chces replay poslednej vyhry? (p = ano, enter = nie): ");
        String s = scanner.nextLine().trim().toLowerCase();

        // iba p = ano
        if (!s.equals("p")) {
            return;
        }

        try {
            // nacitam level znova zo suboru
            String path = "levels/level" + levelNumber + ".txt";
            Maze replayMaze = LevelLoader.loadFromResource(path);

            // vypisem ze idem replay
            System.out.println();
            System.out.println("REPLAY:");

            // ukazem zaciatok
            show(replayMaze, levelNumber);

            // prejdem vsetky kroky
            for (char ch : recordedMoves) {
                // vykonam pohyb
                if (ch == 'w') replayMaze.move(Direction.UP);
                else if (ch == 's') replayMaze.move(Direction.DOWN);
                else if (ch == 'a') replayMaze.move(Direction.LEFT);
                else if (ch == 'd') replayMaze.move(Direction.RIGHT);

                // ukazem stav po kroku
                show(replayMaze, levelNumber);

                // mala pauza, aby to bolo vidno
                try {
                    Thread.sleep(180);
                } catch (InterruptedException ignored) {
                }
            }

        } catch (Exception e) {
            // ked sa nieco pokazi
            System.out.println("Replay sa nepodaril: " + e.getMessage());
        }
    }
}