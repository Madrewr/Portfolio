// zadanie.cpp
// hlavny proces (rodic)
// vytvori rury, semafory, zdielane pamate a potom spusti jednotlive procesy cez fork+exec
// medzi procesmi sa pouziva kombinacia: rury, shm, semafory, signaly a siet

#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <cstring>

#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <sys/shm.h>
#include <sys/wait.h>

// niekde treba definovat union semun pre semctl(SETVAL)
union semun {
    int val;
    struct semid_ds *buf;
    unsigned short *array;
#if defined(__linux__)
    struct seminfo *__buf;
#endif
};

// handler pre SIGUSR1
// potomkovia posielaju SIGUSR1 aby rodic vedel, ze uz su spusteni a pripravení
static void ding_handler(int sig) {
    std::printf("odchytenie signalu %d\n", sig);
    std::fflush(stdout);
}

// pomocna funkcia - vypise chybu a skonci program
static void safe_perror_exit(const char* msg) {
    std::perror(msg);
    std::exit(EXIT_FAILURE);
}

// nastavi hodnotu jedneho semafora v semaforovej sade
static void set_sem_value(int semid, int semnum, int value) {
    semun arg;
    arg.val = value;
    if (semctl(semid, semnum, SETVAL, arg) == -1) {
        safe_perror_exit("semctl SETVAL error");
    }
}

// spusti program v potomkovi cez execve
// ked execve zlyha, proces sa musi ukoncit, inak by pokracoval dalej a robil bordel
static void exec_child(const char* prog, char* const argv_exec[]) {
    // spustime binarku (vsetko bezi v tom istom adresari)
    execve(prog, argv_exec, nullptr);

    // ak sa execve nepodarilo, sme stale v potomkovi a musime skončit
    std::perror("execve error");
    _exit(127);
}

int main(int argc, char* argv[]) {
    // nastavime handler na SIGUSR1
    // rodic bude medzi spustaniami procesov robit pause() a cakat na tento signal
    if (signal(SIGUSR1, ding_handler) == SIG_ERR) {
        safe_perror_exit("signal error");
    }

    // ocakavame 2 argumenty: port pre serv1 a port pre serv2
    if (argc != 3) {
        std::fprintf(stderr, "pouzitie: %s <port_serv1> <port_serv2>\n", argv[0]);
        return EXIT_FAILURE;
    }

    // pid jednotlivych procesov, aby sme ich vedeli neskor ukoncit
    pid_t pid_p1 = -1, pid_p2 = -1, pid_pr = -1;
    pid_t pid_pt = -1, pid_s = -1, pid_d = -1, pid_serv1 = -1, pid_serv2 = -1;

    // vytvorime 2 rury:
    // r1 - posielaju do nej P1/P2 a cita ju PR
    // r2 - zapisuje do nej PR a cita ju T
    int pipe_r1[2]{-1, -1};
    int pipe_r2[2]{-1, -1};

    if (pipe(pipe_r1) == -1) safe_perror_exit("pipe r1 error");
    if (pipe(pipe_r2) == -1) safe_perror_exit("pipe r2 error");

    // pipe fd dame ako text, aby sa dali poslat ako argument cez exec
    char r1_w_str[16], r1_r_str[16];
    char r2_w_str[16], r2_r_str[16];

    std::snprintf(r1_w_str, sizeof(r1_w_str), "%d", pipe_r1[1]);
    std::snprintf(r1_r_str, sizeof(r1_r_str), "%d", pipe_r1[0]);
    std::snprintf(r2_w_str, sizeof(r2_w_str), "%d", pipe_r2[1]);
    std::snprintf(r2_r_str, sizeof(r2_r_str), "%d", pipe_r2[0]);

    // vytvorenie semaforov (2 sady po 2 semafory)
    // ftok potrebuje existujuci subor, preto pouzivame mena binariek
    key_t sem1_key = ftok("proc_t", 'X');
    if (sem1_key == -1) safe_perror_exit("ftok proc_t error");

    key_t sem2_key = ftok("proc_d", 'X');
    if (sem2_key == -1) safe_perror_exit("ftok proc_d error");

    int sem_s1 = semget(sem1_key, 2, IPC_CREAT | 0666);
    if (sem_s1 == -1) safe_perror_exit("semget s1 error");

    int sem_s2 = semget(sem2_key, 2, IPC_CREAT | 0666);
    if (sem_s2 == -1) safe_perror_exit("semget s2 error");

    // nastavime default hodnoty semaforov
    // sem[0] = mutex (zvycajne 1)
    // sem[1] = signal (zvycajne 0)
    set_sem_value(sem_s1, 0, 1);
    set_sem_value(sem_s1, 1, 0);
    set_sem_value(sem_s2, 0, 1);
    set_sem_value(sem_s2, 1, 0);

    // semafor id ako text
    char sem1_str[16], sem2_str[16];
    std::snprintf(sem1_str, sizeof(sem1_str), "%d", sem_s1);
    std::snprintf(sem2_str, sizeof(sem2_str), "%d", sem_s2);

    // vytvorime 2 zdielane pamate (sm1 a sm2)
    // IPC_PRIVATE = vytvori sa nova shm s unikatnym id
    int shm_sm1 = shmget(IPC_PRIVATE, 150, IPC_CREAT | 0666);
    if (shm_sm1 == -1) safe_perror_exit("shmget sm1 error");

    int shm_sm2 = shmget(IPC_PRIVATE, 150, IPC_CREAT | 0666);
    if (shm_sm2 == -1) safe_perror_exit("shmget sm2 error");

    // shm id ako text
    char shm1_str[16], shm2_str[16];
    std::snprintf(shm1_str, sizeof(shm1_str), "%d", shm_sm1);
    std::snprintf(shm2_str, sizeof(shm2_str), "%d", shm_sm2);

    // pripravime nazvy programov pre exec (musia byt char[], nie const char*)
    char prog_p1[] = "proc_p1";
    char prog_p2[] = "proc_p2";
    char prog_pr[] = "proc_pr";
    char prog_t[]  = "proc_t";
    char prog_s[]  = "proc_s";
    char prog_d[]  = "proc_d";
    char prog_serv1[] = "proc_serv1";
    char prog_serv2[] = "proc_serv2";

    // argv pre procesy
    // P1/P2 dostanu fd na zapis do rury r1
    char* argv_p1[] = { prog_p1, r1_w_str, nullptr };
    char* argv_p2[] = { prog_p2, r1_w_str, nullptr };

    // T dostane sem_s1, shm_sm1, a pipe_r2 na citanie
    char* argv_t[] = { prog_t, sem1_str, shm1_str, r2_r_str, nullptr };

    // D dostane sem_s2, shm_sm2 a port (argv[1])
    char* argv_d[] = { prog_d, sem2_str, shm2_str, argv[1], nullptr };

    // S bude robit most medzi shm_sm1/sem_s1 a shm_sm2/sem_s2
    char* argv_s[] = { prog_s, shm1_str, sem1_str, shm2_str, sem2_str, nullptr };

    // serv2 dostane port argv[2], serv1 dostane oba porty
    char* argv_serv2[] = { prog_serv2, argv[2], nullptr };
    char* argv_serv1[] = { prog_serv1, argv[1], argv[2], nullptr };

    // --- P1 ---
    // vytvorime proces P1 a hned cakame na SIGUSR1 (ze uz bezi)
    pid_p1 = fork();
    if (pid_p1 < 0) safe_perror_exit("fork p1 error");
    if (pid_p1 == 0) {
        exec_child("proc_p1", argv_p1);
    }
    pause();

    // --- P2 ---
    pid_p2 = fork();
    if (pid_p2 < 0) safe_perror_exit("fork p2 error");
    if (pid_p2 == 0) {
        exec_child("proc_p2", argv_p2);
    }
    pause();

    // pid P1 a P2 posleme procesu PR ako argumenty (aby im vedel posielat signal)
    char p1_pid_str[16], p2_pid_str[16];
    std::snprintf(p1_pid_str, sizeof(p1_pid_str), "%d", static_cast<int>(pid_p1));
    std::snprintf(p2_pid_str, sizeof(p2_pid_str), "%d", static_cast<int>(pid_p2));

    // PR dostane:
    // pid_p1, pid_p2, pipe_r1 na citanie, pipe_r2 na zapis
    char* argv_pr[] = { prog_pr, p1_pid_str, p2_pid_str, r1_r_str, r2_w_str, nullptr };

    // --- PR ---
    pid_pr = fork();
    if (pid_pr < 0) safe_perror_exit("fork pr error");
    if (pid_pr == 0) {
        exec_child("proc_pr", argv_pr);
    }
    pause();

    // --- T ---
    pid_pt = fork();
    if (pid_pt < 0) safe_perror_exit("fork t error");
    if (pid_pt == 0) {
        exec_child("proc_t", argv_t);
    }
    pause();

    // --- S ---
    pid_s = fork();
    if (pid_s < 0) safe_perror_exit("fork s error");
    if (pid_s == 0) {
        exec_child("proc_s", argv_s);
    }
    pause();

    // --- SERV2 ---
    // serv2 nepotrebujeme pauzovat hned, lebo dalej sa aj tak spusta serv1
    pid_serv2 = fork();
    if (pid_serv2 < 0) safe_perror_exit("fork serv2 error");
    if (pid_serv2 == 0) {
        exec_child("proc_serv2", argv_serv2);
    }

    // --- SERV1 ---
    pid_serv1 = fork();
    if (pid_serv1 < 0) safe_perror_exit("fork serv1 error");
    if (pid_serv1 == 0) {
        exec_child("proc_serv1", argv_serv1);
    }
    pause();

    // --- D ---
    pid_d = fork();
    if (pid_d < 0) safe_perror_exit("fork d error");
    if (pid_d == 0) {
        exec_child("proc_d", argv_d);
    }
    pause();

    // rodic caka, kym serv2 prijme 10 sprav a skonci
    int status = 0;
    std::printf("cakam na ukoncienie serv2\n");
    std::fflush(stdout);

    if (waitpid(pid_serv2, &status, 0) == -1) {
        std::perror("waitpid serv2 error");
    } else {
        std::printf("proces serv2 ukonceny\n");
        std::fflush(stdout);
    }

    // po skonceni serv2 vypneme vsetky procesy cez SIGINT
    auto try_kill = [](pid_t p, int sig) {
        if (p > 0) {
            if (kill(p, sig) == -1) {
                // ESRCH znamena, ze proces uz neexistuje
                if (errno != ESRCH) std::perror("kill error");
            }
        }
    };

    try_kill(pid_p1, SIGINT);
    try_kill(pid_p2, SIGINT);
    try_kill(pid_pr, SIGINT);
    try_kill(pid_pt, SIGINT);
    try_kill(pid_s, SIGINT);
    try_kill(pid_d, SIGINT);
    try_kill(pid_serv1, SIGINT);
    try_kill(pid_serv2, SIGINT);

    // zrusime semafory
    if (semctl(sem_s1, 0, IPC_RMID) == -1) std::perror("chyba zrusenia semafora sem1");
    if (semctl(sem_s2, 0, IPC_RMID) == -1) std::perror("chyba zrusenia semafora sem2");

    // zrusime zdielane pamate
    if (shmctl(shm_sm1, IPC_RMID, nullptr) == -1) std::perror("chyba zrusenia shm1");
    if (shmctl(shm_sm2, IPC_RMID, nullptr) == -1) std::perror("chyba zrusenia shm2");

    // poznamka: rury by sa dali zavriet, ale ked rodic konci, nie je to kriticke
    return 0;
}
