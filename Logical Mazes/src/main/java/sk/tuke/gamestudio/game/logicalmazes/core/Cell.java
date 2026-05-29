package sk.tuke.gamestudio.game.logicalmazes.core;

public class Cell {
    // Tieto boolean hodnoty hovoria, ci ma bunka stenu v konkretnom smere.
    private boolean wallUp;
    private boolean wallDown;
    private boolean wallLeft;
    private boolean wallRight;

    // blocked znamena pevny blok v mape, cez ktory sa neda prejst.
    private boolean blocked;

    // state hovori, ci je policko prazdne alebo obsahuje collectible.
    private TileState state;

    public Cell() {
        // Nova bunka je defaultne prejazdna a prazdna.
        this.blocked = false;
        this.state = TileState.EMPTY;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public TileState getState() {
        return state;
    }

    public void setState(TileState state) {
        this.state = state;
    }

    public boolean hasWall(Direction dir) {
        // Podla smeru vratim konkretnu stenu danej bunky.
        switch (dir) {
            case UP:
                return wallUp;
            case DOWN:
                return wallDown;
            case LEFT:
                return wallLeft;
            case RIGHT:
                return wallRight;
        }
        return false;
    }

    public void setWall(Direction dir, boolean value) {
        // Podla smeru nastavim konkretnu stenu danej bunky.
        switch (dir) {
            case UP:
                wallUp = value;
                break;
            case DOWN:
                wallDown = value;
                break;
            case LEFT:
                wallLeft = value;
                break;
            case RIGHT:
                wallRight = value;
                break;
        }
    }
}
