package sk.tuke.gamestudio.game.logicalmazes.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CellTest {

    @Test
    void testNewCellIsEmptyAndNotBlocked() {
        Cell cell = new Cell();

        // nova bunka nema byt blokovana
        assertFalse(cell.isBlocked());

        // nova bunka je prazdna
        assertEquals(TileState.EMPTY, cell.getState());
    }

    @Test
    void testBlockedCanBeChanged() {
        Cell cell = new Cell();

        // nastavim blok
        cell.setBlocked(true);
        assertTrue(cell.isBlocked());

        // zrusim blok
        cell.setBlocked(false);
        assertFalse(cell.isBlocked());
    }

    @Test
    void testTileStateCanBeChanged() {
        Cell cell = new Cell();

        // nastavim collectible
        cell.setState(TileState.COLLECTIBLE);
        assertEquals(TileState.COLLECTIBLE, cell.getState());

        // vratim prazdny stav
        cell.setState(TileState.EMPTY);
        assertEquals(TileState.EMPTY, cell.getState());
    }

    @Test
    void testWallsAreIndependentForEveryDirection() {
        Cell cell = new Cell();

        // na zaciatku nema ziadnu stenu
        assertFalse(cell.hasWall(Direction.UP));
        assertFalse(cell.hasWall(Direction.DOWN));
        assertFalse(cell.hasWall(Direction.LEFT));
        assertFalse(cell.hasWall(Direction.RIGHT));

        // nastavim iba pravu stenu
        cell.setWall(Direction.RIGHT, true);

        // prava stena je true, ostatne ostanu false
        assertFalse(cell.hasWall(Direction.UP));
        assertFalse(cell.hasWall(Direction.DOWN));
        assertFalse(cell.hasWall(Direction.LEFT));
        assertTrue(cell.hasWall(Direction.RIGHT));
    }
}
