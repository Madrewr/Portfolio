package sk.tuke.gamestudio.game.logicalmazes.core;

public class Maze {
    // velkost mapy (riadky a stlpce)
    private final int rowCount;
    private final int columnCount;

    // pocet tahov (pokusov o pohyb)
    private int moveCount;

    // herna mriezka buniek
    private final Cell[][] cells;

    // aktualna pozicia hraca
    private int playerRow;
    private int playerCol;

    // kolko C bolo na zaciatku a kolko ich este chyba zobrat
    private int initialCollectibles;
    private int remainingCollectibles;

    // stav hry (PLAYING alebo SOLVED)
    private GameState state;

    public Maze(int rows, int cols) {
        // ulozim rozmery
        this.rowCount = rows;
        this.columnCount = cols;

        // vytvorim 2D pole buniek
        this.cells = new Cell[rows][cols];

        // kazdu bunku inicializujem
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }

        // default pozicia hraca (LevelLoader to potom prepise cez setPlayerPosition)
        this.playerRow = 0;
        this.playerCol = 0;

        // pocitadla C (collectibles)
        initialCollectibles = 0;
        this.remainingCollectibles = 0;

        // default stav hry
        this.state = GameState.PLAYING;

        // na zaciatku ma hrac 0 tahov
        moveCount = 0;

        // okraje mapy nastavime ako steny, aby sa nedalo ist mimo
        initBorderWalls();
    }

    private void initBorderWalls() {
        // hore a dole nastavim steny
        for (int c = 0; c < columnCount; c++) {
            cells[0][c].setWall(Direction.UP, true);
            cells[rowCount - 1][c].setWall(Direction.DOWN, true);
        }

        // lava a prava nastavim steny
        for (int r = 0; r < rowCount; r++) {
            cells[r][0].setWall(Direction.LEFT, true);
            cells[r][columnCount - 1].setWall(Direction.RIGHT, true);
        }
    }

    // vrati pocet tahov
    public int getMoveCount() {
        return moveCount;
    }

    // pocet riadkov
    public int getRows() {
        return rowCount;
    }

    // pocet stlpcov
    public int getCols() {
        return columnCount;
    }

    // pozicia hraca - riadok
    public int getPlayerRow() {
        return playerRow;
    }

    // pozicia hraca - stlpec
    public int getPlayerCol() {
        return playerCol;
    }

    // kolko C este ostava
    public int getRemainingCollectibles() {
        return remainingCollectibles;
    }

    // stav hry
    public GameState getState() {
        return state;
    }

    // vratim konkretnu bunku na pozicii (r,c)
    public Cell getCell(int r, int c) {
        return cells[r][c];
    }

    // nastavi start poziciu hraca
    public void setPlayerPosition(int r, int c) {
        playerRow = r;
        playerCol = c;

        // keby start stal na C, tak ho hned zoberiem
        collectIfPresent(r, c);

        // po zmene pozicie skontrolujem vyhru
        checkSolved();
    }

    // nastavi C na policku
    public void setCollectible(int r, int c) {
        // ked tam este nebolo collectible, tak ho pridam
        if (cells[r][c].getState() != TileState.COLLECTIBLE) {
            cells[r][c].setState(TileState.COLLECTIBLE);
            remainingCollectibles++;
            initialCollectibles++;
        }

        // po zmene skontrolujem vyhru
        checkSolved();
    }

    // nastavi blok (#) na policku
    public void setBlocked(int r, int c, boolean value) {
        cells[r][c].setBlocked(value);
    }

    // hlavna logika pohybu (klzanie)
    public void move(Direction dir) {
        // ak uz je vyriesene, nic nerobim
        if (state == GameState.SOLVED) {
            return;
        }

        // zvysim pocet tahov (aj ked sa realne nepohne)
        moveCount++;

        // posun smeru (dr/dc)
        int dr = 0;
        int dc = 0;

        // nastavim rozdiely podla smeru
        switch (dir) {
            case UP -> dr = -1;
            case DOWN -> dr = 1;
            case LEFT -> dc = -1;
            case RIGHT -> dc = 1;
        }

        // startujem z aktualnej pozicie hraca
        int r = playerRow;
        int c = playerCol;

        // klzanie: posuvam sa kym sa da
        while (true) {
            // dalsia pozicia kam by som isiel
            int nr = r + dr;
            int nc = c + dc;

            // ked je mimo mapy, stop
            if (!isInside(nr, nc)) {
                break;
            }

            // ak je na aktualnom policku stena v smere pohybu, stop
            if (cells[r][c].hasWall(dir)) {
                break;
            }

            // ak dalsie policko je blokovane (#), stop
            if (cells[nr][nc].isBlocked()) {
                break;
            }

            // posun na dalsie policko
            r = nr;
            c = nc;

            // zober C ak tam je
            collectIfPresent(r, c);
        }

        // ulozim novu poziciu hraca
        playerRow = r;
        playerCol = c;

        // po pohybe skontrolujem vyhru
        checkSolved();
    }

    // kontrola ci je pozicia v poli
    private boolean isInside(int r, int c) {
        return r >= 0 && r < rowCount && c >= 0 && c < columnCount;
    }

    // ak je na policku C, tak ho zoberiem
    private void collectIfPresent(int r, int c) {
        if (cells[r][c].getState() == TileState.COLLECTIBLE) {
            cells[r][c].setState(TileState.EMPTY);
            remainingCollectibles--;
        }
    }

    // kontrola vyhry (SOLVED)
    private void checkSolved() {
        // vyhram len vtedy, ked v leveli realne bolo nejake C
        // a zaroven uz nezostava ziadne C
        if (initialCollectibles > 0 && remainingCollectibles <= 0) {
            state = GameState.SOLVED;
        } else {
            state = GameState.PLAYING;
        }
    }
}