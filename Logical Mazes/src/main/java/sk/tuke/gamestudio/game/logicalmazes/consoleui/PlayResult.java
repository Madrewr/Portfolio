package sk.tuke.gamestudio.game.logicalmazes.consoleui;

// vysledok hrania jedneho levelu, aby Main vedel co spravit dalej
public enum PlayResult {
    // hrac vyhral level
    WON,

    // hrac chce restartovat aktualny level
    RESTART,

    // hrac chce ukoncit hru
    QUIT
}
