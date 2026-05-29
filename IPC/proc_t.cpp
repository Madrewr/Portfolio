// proc_t.cpp
// proces T
// cita slova z rury a zapisuje ich do zdielanej pamate
// pouziva semafor na synchronizaciu s dalsim procesom

#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <cstring>

#include <unistd.h>
#include <signal.h>

#include <sys/ipc.h>
#include <sys/sem.h>
#include <sys/shm.h>

// globalny identifikator semafora s1
static int g_sem_s1 = -1;

// obsluha signalu SIGINT
// proces sa hned ukonci
static void on_exit_sigint(int /*sig*/) {
    _exit(0);
}

// semaforova operacia P
// zamkne zapis do zdielanej pamate
static int sem_lock_writer() {
    sembuf op{};
    op.sem_num = 0;     // mutex pre zapis
    op.sem_op  = -1;    // P operacia
    op.sem_flg = SEM_UNDO;

    if (semop(g_sem_s1, &op, 1) == -1) {
        std::perror("semop P (proc_t)");
        return 0;
    }
    return 1;
}

// semaforova operacia V
// signalizuje, ze data v pamati su pripravene
static int sem_signal_data_ready() {
    sembuf op{};
    op.sem_num = 1;     // semafor pre signal
    op.sem_op  = +1;    // V operacia
    op.sem_flg = SEM_UNDO;

    if (semop(g_sem_s1, &op, 1) == -1) {
        std::perror("semop V (proc_t)");
        return 0;
    }
    return 1;
}

// pomocna funkcia na vypis chyby a ukoncenie programu
static void die_perror(const char* msg) {
    std::perror(msg);
    std::exit(EXIT_FAILURE);
}

int main(int argc, char* argv[]) {
    // ocakavane argumenty:
    // argv[1] - id semafora s1
    // argv[2] - id zdielanej pamate sm1
    // argv[3] - fd rury na citanie
    if (argc < 4) {
        std::fprintf(stderr, "pouzitie: %s <sem_s1_id> <shm_sm1_id> <pipe_r2_read_fd>\n", argv[0]);
        return 1;
    }

    g_sem_s1 = std::atoi(argv[1]);
    int shm_sm1 = std::atoi(argv[2]);
    int pipe_r2 = std::atoi(argv[3]);

    // zakladna kontrola argumentov
    if (g_sem_s1 < 0 || shm_sm1 < 0 || pipe_r2 < 0) {
        std::fprintf(stderr, "zle argumenty (sem/shm/pipe)\n");
        return 1;
    }

    // pripojenie na zdielanu pamat
    void* shMemory = shmat(shm_sm1, nullptr, 0);
    if (shMemory == reinterpret_cast<void*>(-1)) {
        die_perror("zdielana pamat sm1 shmat");
    }

    // nastavime signal na korektne ukoncenie procesu
    if (signal(SIGINT, on_exit_sigint) == SIG_ERR) {
        die_perror("signal SIGINT error");
    }

    // dame rodicovi vediet, ze proces T je pripraveny
    kill(getppid(), SIGUSR1);

    // buffer pre jedno slovo precitane z rury
    char buffer[150];
    std::memset(buffer, 0, sizeof(buffer));

    // hlavny cyklus procesu
    while (true) {
        // zamkneme zapis do zdielanej pamate
        if (!sem_lock_writer()) return 1;

        // vycistime pamet a buffer pre nove data
        std::memset(shMemory, 0, 150);
        std::memset(buffer, 0, sizeof(buffer));

        // citame z rury po znakoch az po znak noveho riadku
        int i = 0;
        while (i < 149) {
            char znak = '\0';
            ssize_t r = read(pipe_r2, &znak, 1);

            if (r < 0) {
                // ak bolo citanie prerusene signalom, skusime znova
                if (errno == EINTR) continue;

                std::perror("read pipe_r2 proc_t");
                // aj ked nastane chyba, posleme signal, aby sa system nezablokoval
                sem_signal_data_ready();
                return 1;
            }

            if (r == 0) {
                // EOF - uz nic nejde z rury
                break;
            }

            if (znak == '\n') {
                // koniec slova
                break;
            }

            buffer[i] = znak;
            i++;
        }
        buffer[i] = '\0';

        // zapiseme precitane slovo do zdielanej pamate
        std::strncpy(static_cast<char*>(shMemory), buffer, 149);
        static_cast<char*>(shMemory)[149] = '\0';

        // signalizujeme dalsiemu procesu, ze data su pripravene
        if (!sem_signal_data_ready()) return 1;
    }

    // tento kod sa nikdy nevykona
    return 0;
}
