// proc_serv2.cpp
// proces SERV2
// UDP server, ktory prijima spravy a zapisuje ich do suboru serv2.txt

#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <cstring>

#include <unistd.h>
#include <signal.h>
#include <fcntl.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

// obsluha signalu SIGINT
// proces sa hned ukonci
static void on_exit_sigint(int /*sig*/) {
    _exit(0);
}

// pomocna funkcia na vypis chyby a ukoncenie programu
static void die_perror(const char* msg) {
    std::perror(msg);
    _exit(1);
}

int main(int argc, char* argv[]) {
    // ocakavame jeden argument - port, na ktorom bude UDP server pocuvat
    if (argc < 2) {
        std::fprintf(stderr, "pouzitie: %s <port>\n", argv[0]);
        return 1;
    }

    int portno = std::atoi(argv[1]);
    if (portno <= 0 || portno > 65535) {
        std::fprintf(stderr, "zly port\n");
        return 1;
    }

    // vytvorime UDP socket
    int sockfd = socket(PF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        die_perror("chyba otvorenia socketu proc_serv2");
    }

    // nastavime adresu servera
    sockaddr_in serv_addr{};
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(static_cast<uint16_t>(portno));

    // naviazeme socket na port
    if (bind(sockfd, reinterpret_cast<sockaddr*>(&serv_addr), sizeof(serv_addr)) < 0) {
        die_perror("chyba bind proc_serv2");
    }

    // nastavime signal na korektne ukoncenie procesu
    if (signal(SIGINT, on_exit_sigint) == SIG_ERR) {
        die_perror("signal SIGINT error");
    }

    // dame rodicovi vediet, ze proces SERV2 je pripraveny
    kill(getppid(), SIGUSR1);

    // otvorime vystupny subor, kde budeme ukladat prijate spravy
    int fd = open("serv2.txt", O_CREAT | O_WRONLY | O_TRUNC, 0666);
    if (fd == -1) {
        die_perror("chyba otvorenia serv2.txt");
    }

    // buffer pre prijatu UDP spravu
    char buffer[151]; // +1 na ukoncovaci znak
    std::memset(buffer, 0, sizeof(buffer));

    // prijmeme presne 10 sprav
    int pocet = 0;
    while (true) {
        if (pocet >= 10) break;

        sockaddr_in cli_addr{};
        socklen_t cli_len = sizeof(cli_addr);

        // prijatie spravy od klienta
        // recvfrom sa pouziva pri UDP komunikacii
        ssize_t n = recvfrom(sockfd, buffer, 150, 0,
                             reinterpret_cast<sockaddr*>(&cli_addr), &cli_len);
        if (n < 0) {
            if (errno == EINTR) continue; // prerusenie signalom
            die_perror("recvfrom error proc_serv2");
        }

        // ukoncime prijaty text nulou, aby sa dal zapisat ako string
        if (n > 150) n = 150;
        buffer[n] = '\0';

        // zapiseme spravu do suboru
        if (write(fd, buffer, std::strlen(buffer)) < 0) {
            die_perror("write serv2.txt error");
        }

        // zapiseme novy riadok
        if (write(fd, "\n", 1) < 0) {
            die_perror("write newline error");
        }

        pocet++;
    }

    // zavrieme subor aj socket
    close(fd);
    close(sockfd);

    return 0;
}
