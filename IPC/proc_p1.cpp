// proc_p1.cpp
// proces P1
// cita slova zo suboru p1.txt a posiela ich cez ruru dalsiemu procesu

#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <cstring>

#include <unistd.h>
#include <signal.h>
#include <fcntl.h>

// globalne premenne
// fd rury a fd suboru si nechavame globalne, lebo sa pouzivaju v signal handleroch
static int g_pipe_r1 = -1;
static int g_file1  = -1;

// obsluha signalu SIGINT
// pri ukonceni procesu zatvorime subor a hned skoncime
static void exit_signal(int /*sig*/) {
    if (g_file1 != -1) {
        close(g_file1);
    }
    _exit(0);
}

// obsluha signalu SIGUSR1
// po prijati signalu posleme jeden riadok (slovo) do rury
static void write_word(int /*sig*/) {
    char znak = '\0';

    // citame zo suboru po znakoch az po znak noveho riadku
    while (true) {
        ssize_t r = read(g_file1, &znak, 1);

        if (r == 0) {
            // EOF - uz nic nie je, posleme aspon znak noveho riadku
            znak = '\n';
        } else if (r < 0) {
            // ak bolo citanie prerusene signalom, pokracujeme dalej
            if (errno == EINTR) continue;

            // ina chyba pri citani
            std::perror("proc_p1 read error");
            _exit(1);
        }

        // zapis jedneho znaku do rury
        while (true) {
            ssize_t w = write(g_pipe_r1, &znak, 1);

            if (w == 1) break;
            if (w < 0 && errno == EINTR) continue;

            // chyba pri zapise do rury
            std::perror("proc_p1 write error");
            _exit(1);
        }

        // ak sme dosli na koniec riadku, ukoncime posielanie slova
        if (znak == '\n') break;

        // fallback pri EOF
        if (r == 0) break;
    }
}

int main(int argc, char* argv[]) {
    // ocakava sa jeden argument - fd na zapis do rury
    if (argc < 2) {
        std::fprintf(stderr, "pouzitie: %s <pipe_write_fd>\n", argv[0]);
        return 1;
    }

    // otvorime subor p1.txt na citanie
    g_file1 = open("p1.txt", O_RDONLY);
    if (g_file1 == -1) {
        std::perror("p1 otvorenie");
        return 1;
    }

    // fd rury je poslane ako argument programu
    g_pipe_r1 = std::atoi(argv[1]);
    if (g_pipe_r1 < 0) {
        std::fprintf(stderr, "zly pipe fd\n");
        return 1;
    }

    // nastavime signal handlery
    // SIGUSR1 spusti odoslanie jedneho slova
    // SIGINT ukonci proces
    if (signal(SIGUSR1, write_word) == SIG_ERR) {
        std::perror("signal SIGUSR1 error");
        return 1;
    }
    if (signal(SIGINT, exit_signal) == SIG_ERR) {
        std::perror("signal SIGINT error");
        return 1;
    }

    // dame rodicovi vediet, ze proces P1 je pripraveny
    kill(getppid(), SIGUSR1);

    // hlavny cyklus procesu
    // proces nerobi nic, len caka na signaly
    while (true) {
        sleep(1);
    }

    return 0;
}
