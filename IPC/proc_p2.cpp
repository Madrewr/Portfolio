// proc_p2.cpp
// proces P2
// cita slova zo suboru p2.txt a posiela ich cez ruru dalsiemu procesu

#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <cstring>

#include <unistd.h>
#include <signal.h>
#include <fcntl.h>

// globalne premenne
// fd rury a fd vstupneho suboru su globalne, lebo sa pouzivaju v signal handleroch
static int g_pipe_fd = -1;
static int g_in_fd   = -1;

// obsluha signalu SIGINT
// pri ukonceni procesu zavrieme subor a proces hned skonci
static void on_exit_sigint(int /*sig*/) {
    if (g_in_fd != -1) {
        close(g_in_fd);
    }
    _exit(0);
}

// obsluha signalu SIGUSR1
// po prijati signalu posleme jeden riadok (slovo) do rury
static void on_send_line(int /*sig*/) {
    char znak = '\0';

    // citame zo suboru po znakoch az po znak noveho riadku
    while (true) {
        ssize_t n = read(g_in_fd, &znak, 1);

        if (n < 0) {
            // ak bolo citanie prerusene signalom, skusime znova
            if (errno == EINTR) continue;

            // ina chyba pri citani
            std::perror("proc_p2 read error");
            _exit(2);
        }

        if (n == 0) {
            // EOF - uz nic nie je, posleme aspon znak noveho riadku
            znak = '\n';
        }

        // zapiseme jeden znak do rury
        while (true) {
            ssize_t w = write(g_pipe_fd, &znak, 1);

            if (w == 1) break;
            if (w < 0 && errno == EINTR) continue;

            // chyba pri zapise do rury
            std::perror("proc_p2 write error");
            _exit(3);
        }

        // ak sme dosli na koniec riadku, ukoncime posielanie slova
        if (znak == '\n') break;

        // fallback pri EOF
        if (n == 0) break;
    }
}

int main(int argc, char* argv[]) {
    // ocakava sa jeden argument - fd na zapis do rury
    if (argc < 2) {
        std::fprintf(stderr, "pouzitie: %s <pipe_write_fd>\n", argv[0]);
        return 1;
    }

    // otvorime subor p2.txt na citanie
    g_in_fd = open("p2.txt", O_RDONLY);
    if (g_in_fd == -1) {
        std::perror("p2 otvorenie");
        return 1;
    }

    // fd rury dostaneme ako argument programu
    g_pipe_fd = std::atoi(argv[1]);
    if (g_pipe_fd < 0) {
        std::fprintf(stderr, "zly pipe fd\n");
        return 1;
    }

    // nastavime signal handlery
    // SIGUSR1 spusti odoslanie jedneho slova
    // SIGINT ukonci proces
    if (signal(SIGUSR1, on_send_line) == SIG_ERR) {
        std::perror("signal SIGUSR1 error");
        return 1;
    }
    if (signal(SIGINT, on_exit_sigint) == SIG_ERR) {
        std::perror("signal SIGINT error");
        return 1;
    }

    // dame rodicovi vediet, ze proces P2 je pripraveny
    kill(getppid(), SIGUSR1);

    // hlavny cyklus procesu
    // proces nerobi nic, len caka na prichod signalov
    while (true) {
        sleep(1);
    }

    return 0;
}
