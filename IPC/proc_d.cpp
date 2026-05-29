// proc_d.cpp
// proces D
// cita data zo zdielanej pamate a posiela ich cez TCP socket dalej

#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <cstring>

#include <unistd.h>
#include <signal.h>

#include <sys/ipc.h>
#include <sys/sem.h>
#include <sys/shm.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

// globalny identifikator semafora s2
// pouziva sa na synchronizaciu so zapisom do zdielanej pamate
static int g_sem_s2 = -1;

// ukoncenie procesu po SIGINT
// pouzivam _exit aby sa proces ukoncil hned
static void on_exit_sigint(int /*sig*/) {
    _exit(0);
}

// semaforova operacia P
// caka sa, kym iny proces zapise data do shm
static int sem_wait_word_ready() {
    sembuf op{};
    op.sem_num = 1;     // semafor pre signal, ze data su pripravene
    op.sem_op  = -1;    // P operacia
    op.sem_flg = SEM_UNDO;

    if (semop(g_sem_s2, &op, 1) == -1) {
        std::perror("semop P (proc_d)");
        return 0;
    }
    return 1;
}

// semaforova operacia V
// odomkne zapis do zdielanej pamate pre dalsi cyklus
static int sem_unlock_writer() {
    sembuf op{};
    op.sem_num = 0;     // mutex pre zapis do shm
    op.sem_op  = +1;    // V operacia
    op.sem_flg = SEM_UNDO;

    if (semop(g_sem_s2, &op, 1) == -1) {
        std::perror("semop V (proc_d)");
        return 0;
    }
    return 1;
}

// pomocna funkcia na ukoncenie programu s vypisom chyby
static void die_perror(const char* msg) {
    std::perror(msg);
    std::exit(EXIT_FAILURE);
}

// zapise presne len bajtov do socketu
// write moze zapisat menej, tak to robime v cykle
static bool write_all(int fd, const void* data, size_t len) {
    const char* p = static_cast<const char*>(data);
    size_t left = len;

    while (left > 0) {
        ssize_t w = write(fd, p, left);
        if (w < 0) {
            if (errno == EINTR) continue; // prerusenie signalom
            return false;
        }
        if (w == 0) return false;

        p += static_cast<size_t>(w);
        left -= static_cast<size_t>(w);
    }
    return true;
}

int main(int argc, char* argv[]) {
    // ocakavane argumenty:
    // argv[1] - id semafora s2
    // argv[2] - id zdielanej pamate sm2
    // argv[3] - port TCP servera
    if (argc < 4) {
        std::fprintf(stderr, "pouzitie: %s <sem_s2_id> <shm_sm2_id> <port>\n", argv[0]);
        return 1;
    }

    g_sem_s2 = std::atoi(argv[1]);
    int shm_sm2 = std::atoi(argv[2]);
    int portno  = std::atoi(argv[3]);

    // zakladna kontrola argumentov
    if (g_sem_s2 < 0 || shm_sm2 < 0 || portno <= 0 || portno > 65535) {
        std::fprintf(stderr, "zle argumenty (sem/shm/port)\n");
        return 1;
    }

    // pripojenie procesu na zdielanu pamat
    void* shMemory = shmat(shm_sm2, nullptr, 0);
    if (shMemory == reinterpret_cast<void*>(-1)) {
        die_perror("zdielana pamat sm2 shmat");
    }

    // vytvorenie TCP socketu
    int sockfd = socket(PF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) die_perror("chyba otvorenia socketu proc_d");

    // nastavenie adresy servera
    sockaddr_in serv_addr{};
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(static_cast<uint16_t>(portno));

    // pripojenie na localhost (127.0.0.1)
    if (inet_pton(AF_INET, "127.0.0.1", &serv_addr.sin_addr) != 1) {
        die_perror("inet_pton error proc_d");
    }

    // pripojenie na TCP server
    if (connect(sockfd, reinterpret_cast<sockaddr*>(&serv_addr), sizeof(serv_addr)) < 0) {
        die_perror("chyba pripojenia proc_d");
    }

    // signal pre korektne ukoncenie procesu
    if (signal(SIGINT, on_exit_sigint) == SIG_ERR) {
        die_perror("signal SIGINT error");
    }

    // posleme rodicovi signal, ze proces D uz bezi
    kill(getppid(), SIGUSR1);

    // hlavny cyklus procesu
    // caka na data v zdielanej pamati a posiela ich cez socket
    while (true) {
        // cakanie na signal, ze data su pripravene
        if (!sem_wait_word_ready()) return 1;

        // posleme pevnych 150 bajtov, tak ako je v zadani
        if (!write_all(sockfd, shMemory, 150)) {
            std::perror("write to socket proc_d");
            // pre istotu uvolnime semafor, aby sa system nezablokoval
            sem_unlock_writer();
            return 1;
        }

        // uvolnime zapis do zdielanej pamate
        if (!sem_unlock_writer()) return 1;

        // vycistime pamet, aby bolo jasne, ze sa caka na nove data
        std::memset(shMemory, 0, 150);

        // male oneskorenie, aby proces nebezal zbytocne rychlo
        sleep(1);
    }

    // tento kod sa nikdy nevykona
    return 0;
}
