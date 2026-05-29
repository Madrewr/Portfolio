#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_MENO 100
#define MAX_DETI 100
#define MAX_CESTA 512

// Struktura pre uzol (subor alebo adresar)
typedef struct Uzol {
    char meno[MAX_MENO];       // Meno suboru alebo adresara
    char vlastnik[MAX_MENO];   // Vlastnik suboru alebo adresara
    int prava;                 // Prava: rwx: 4,2,1
    int je_adresar;            // 1, ak je to adresar, inak 0
    struct Uzol *rodic;        // Odkaz na rodica uzla (adresar)
    struct Uzol *deti[MAX_DETI];  // Pole deti (subory alebo podadresare)
    int pocet_deti;            // Pocet deti
    char obsah[1024];          // Obsah suboru (ak je to subor)
} Uzol;

Uzol *koren;  // Korenovy adresar (root)
Uzol *aktualny_adresar;  // Aktualny pracovny adresar
char aktualny_vlastnik[MAX_MENO] = "marek";  // Aktualny pouzivatel

// Funkcia na inicializaciu suboroveho systemu
void init_fs() {
    // Alokujeme pamat pre korenovy adresar
    koren = (Uzol*)malloc(sizeof(Uzol));
    strcpy(koren->meno, "/");  // Korenovy adresar sa vola "/"
    strcpy(koren->vlastnik, aktualny_vlastnik);
    koren->prava = 7;  // rwx: plne prava
    koren->je_adresar = 1;  // Je to adresar
    koren->rodic = NULL;  // Koren nemá rodica
    koren->pocet_deti = 0;  // Na zaciatku nema ziadne deti
    aktualny_adresar = koren;  // Nastavime aktualny adresar na koren
}

// Funkcia na najdenie uzla podla cesty
Uzol* find_node(Uzol *zaciatok, char *cesta, int vytvor_adresare, int *chyba) {
    // Ak cesta zacina lomitkom, zacneme od korena
    if (cesta[0] == '/') zaciatok = koren;
    char docasna[MAX_CESTA];
    strcpy(docasna, cesta);  // Kopia cesty pre dalsie spracovanie
    char *token = strtok(docasna, "/");  // Rozdelime cestu podla lomitok
    Uzol *aktualny = zaciatok;

    while (token) {
        int najdeny = 0;
        // Prechadzame vsetkymi detmi aktualneho uzla
        for (int i = 0; i < aktualny->pocet_deti; i++) {
            // Ak najdeme uzol so zhodnym menom, pokracujeme s nim
            if (strcmp(aktualny->deti[i]->meno, token) == 0) {
                aktualny = aktualny->deti[i];
                najdeny = 1;
                break;
            }
        }

        // Ak sme nenasli uzol a mame vytvorit adresar, vytvorime novy
        if (!najdeny) {
            if (vytvor_adresare) {
                Uzol *novy_adresar = (Uzol*)malloc(sizeof(Uzol));
                strcpy(novy_adresar->meno, token);
                strcpy(novy_adresar->vlastnik, aktualny_vlastnik);
                novy_adresar->prava = 7;  // rwx: plne prava
                novy_adresar->je_adresar = 1;  // Je to adresar
                novy_adresar->rodic = aktualny;
                novy_adresar->pocet_deti = 0;  // Tento adresar bude mat 0 deti
                aktualny->deti[aktualny->pocet_deti++] = novy_adresar;  // Pridame novy adresar medzi deti
                aktualny = novy_adresar;  // Pokracujeme v praci s novym adresarom
            } else {
                if (chyba) *chyba = 1;  // Nastavime chybu, ak nechceme vytvarat adresare
                return NULL;
            }
        }

        token = strtok(NULL, "/");  // Pokracujeme v rozdelovani cesty
    }

    return aktualny;  // Vraciame posledny najdeny uzol
}

// Funkcia na najdenie dietata v adresari
Uzol* find_child(Uzol* rodic, const char* meno) {
    // Prechadzame vsetkymi detmi rodica
    for (int i = 0; i < rodic->pocet_deti; i++) {
        if (strcmp(rodic->deti[i]->meno, meno) == 0) {
            return rodic->deti[i];  // Ak najdeme dieta so zhodnym menom, vratime ho
        }
    }
    return NULL;  // Ak dieta neexistuje, vratime NULL
}

// Funkcia na vytvorenie noveho uzla
Uzol* create_node(const char* meno, int je_adresar, Uzol* rodic) {
    Uzol* uzol = (Uzol*)malloc(sizeof(Uzol));
    strcpy(uzol->meno, meno);
    strcpy(uzol->vlastnik, aktualny_vlastnik);
    uzol->prava = 7;  // rwx: plne prava
    uzol->je_adresar = je_adresar;  // Nastavime, ci je to adresar alebo subor
    uzol->rodic = rodic;  // Nastavime rodica
    uzol->pocet_deti = 0;  // Na zaciatku nema ziadne deti
    uzol->obsah[0] = '\0';  // Ak je to subor, obsah je prazdny
    return uzol;
}

// Funkcia na pridanie dietata k rodicovi
void add_child(Uzol* rodic, Uzol* dieta) {
    // Ak je pocet deti v rodicovi dosiahnuty maximalny limit
    if (rodic->pocet_deti >= MAX_DETI) {
        printf("Dosiahnuty maximalny pocet deti\n");
        return;
    }
    // Pridame dieta do pole deti rodica
    rodic->deti[rodic->pocet_deti++] = dieta;
}

// Funkcia na odstranenie dietata z rodica
void remove_child(Uzol* rodic, Uzol* dieta) {
    int index = -1;
    // Hladame index dietata v poli deti
    for (int i = 0; i < rodic->pocet_deti; i++) {
        if (rodic->deti[i] == dieta) {
            index = i;
            break;
        }
    }
    // Ak dieta neexistuje, nic nerobime
    if (index == -1) return;

    // Posuvame vsetky deti za tymto indexom o jedno miesto dozadu
    for (int i = index; i < rodic->pocet_deti - 1; i++) {
        rodic->deti[i] = rodic->deti[i + 1];
    }
    // Zmensime pocet deti
    rodic->pocet_deti--;
    // Uvolnime pamat pre dieta
    free(dieta);
}

// Funkcia na vypisanie obsahu adresara
void cmd_ls(char *cesta) {
    Uzol *ciel;

    // Ak je cesta prazdna, alebo je to aktualny adresar, nastavime ciel na aktualny adresar
    if (!cesta || strcmp(cesta, ".") == 0 || strcmp(cesta, "./") == 0) {
        ciel = aktualny_adresar;
    }
    // Ak je cesta "..", nastavime ciel na rodica aktualneho adresara
    else if (cesta && strcmp(cesta, "..") == 0) {
        ciel = aktualny_adresar->rodic;
    } 
    else {
        // Ak ide o inu cestu, hladame uzol podla tejto cesty
        ciel = find_node(aktualny_adresar, cesta, 0, NULL);
    }

    // Ak sme nenasli ciel, vypiseme chybu
    if (!ciel) {
        printf("chyba\n");
        return;
    }

    // Ak nemame prava na citanie (read), vypiseme chybu
    if (!(ciel->prava & 4)) {
        printf("chyba prav\n");
        return;
    }

    // Ak to nie je adresar, vypiseme jeho detaily
    if (!ciel->je_adresar) {
        printf("%s %s %c%c%c\n", ciel->meno, ciel->vlastnik,
               (ciel->prava & 4) ? 'r' : '-',
               (ciel->prava & 2) ? 'w' : '-',
               (ciel->prava & 1) ? 'x' : '-');
        return;
    }

    // Ak adresar nema deti, vypiseme "ziaden subor"
    if (ciel->pocet_deti == 0) {
        printf("ziaden subor\n");
        return;
    }

    // Inak, vypiseme deti adresara
    for (int i = 0; i < ciel->pocet_deti; i++) {
        Uzol *dieta = ciel->deti[i];
        printf("%s %s %c%c%c\n", dieta->meno, dieta->vlastnik,
               (dieta->prava & 4) ? 'r' : '-',
               (dieta->prava & 2) ? 'w' : '-',
               (dieta->prava & 1) ? 'x' : '-');
    }
}

// Funkcia na vytvorenie noveho suboru
void cmd_touch(char *argument) {
    // Ak argument nie je zadany, vypiseme chybu
    if (!argument) { 
        printf("chyba\n"); 
        return; 
    }

    char kopia_cesty[MAX_CESTA];
    strcpy(kopia_cesty, argument);  // Urobime kopiu argumentu

    char *token = strtok(kopia_cesty, "/");
    Uzol *aktualny = aktualny_adresar;
    char *posledne_meno = NULL;

    // Prechadzame cestu a hladame uzol
    while (token) {
        posledne_meno = token;
        token = strtok(NULL, "/");

        Uzol *dalsi = NULL;
        // Hladame deti aktualneho uzla
        for (int i = 0; i < aktualny->pocet_deti; i++) {
            if (strcmp(aktualny->deti[i]->meno, posledne_meno) == 0) {
                dalsi = aktualny->deti[i];
                break;
            }
        }

        // Ak token nie je posledny, musi ist o adresar, ktory musi existovat a mat prava na spustanie
        if (token) {
            if (!dalsi || !dalsi->je_adresar || !(dalsi->prava & 1)) {
                printf("chyba\n");
                return;
            }
            aktualny = dalsi;
        } else {
            // Ak token je posledny, vytvorime subor
            if (dalsi) {
                printf("chyba\n"); // Subor uz existuje
                return;
            }
            // Ak nemame prava na zapis v rodicovskom adresari, vypiseme chybu
            if (!(aktualny->prava & 2)) {
                printf("chyba\n");
                return;
            }

            Uzol *subor = (Uzol*)malloc(sizeof(Uzol));
            strcpy(subor->meno, posledne_meno);
            strcpy(subor->vlastnik, aktualny_vlastnik);
            subor->prava = 7;  // Subor ma plne prava (rwx)
            subor->je_adresar = 0;  // Nie je to adresar
            subor->rodic = aktualny;
            subor->pocet_deti = 0;  // Subor nema deti
            aktualny->deti[aktualny->pocet_deti++] = subor;  // Pridame subor medzi deti
        }
    }
}
// Funkcia na vytvorenie noveho adresara
void cmd_mkdir(char *argument) {
    // Ak argument nie je zadany, vypiseme chybu
    if (!argument) { 
        printf("chyba\n"); 
        return; 
    }

    char kopia_cesty[MAX_CESTA];
    strcpy(kopia_cesty, argument);  // Urobime kopiu argumentu

    char *token = strtok(kopia_cesty, "/");
    Uzol *aktualny = aktualny_adresar;
    char *posledne_meno = NULL;

    // Prechadzame cestu a hladame uzol
    while (token) {
        posledne_meno = token;
        token = strtok(NULL, "/");

        Uzol *dalsi = NULL;
        // Hladame deti aktualneho uzla
        for (int i = 0; i < aktualny->pocet_deti; i++) {
            if (strcmp(aktualny->deti[i]->meno, posledne_meno) == 0) {
                dalsi = aktualny->deti[i];
                break;
            }
        }

        // Ak token nie je posledny, musi ist o adresar, ktory musi existovat a mat prava na spustanie
        if (token) {
            if (!dalsi || !dalsi->je_adresar || !(dalsi->prava & 1)) {
                printf("chyba\n");
                return;
            }
            aktualny = dalsi;
        } else {
            // Ak token je posledny, vytvorime adresar
            if (dalsi) {
                printf("chyba\n"); // Adresar uz existuje
                return;
            }
            // Ak nemame prava na zapis v rodicovskom adresari, vypiseme chybu
            if (!(aktualny->prava & 2)) {
                printf("chyba\n");
                return;
            }

            Uzol *adresar = (Uzol*)malloc(sizeof(Uzol));
            strcpy(adresar->meno, posledne_meno);
            strcpy(adresar->vlastnik, aktualny_vlastnik);
            adresar->prava = 7;  // Adresar ma plne prava (rwx)
            adresar->je_adresar = 1;  // Je to adresar
            adresar->rodic = aktualny;
            adresar->pocet_deti = 0;  // Adresar nema deti
            aktualny->deti[aktualny->pocet_deti++] = adresar;  // Pridame adresar medzi deti
        }
    }
}

// Funkcia na odstranenie suboru alebo adresara
void cmd_rm(char *argument) {
    // Ak argument nie je zadany, vypiseme chybu
    if (!argument) { 
        printf("chyba\n"); 
        return; 
    }

    char kopia_cesty[MAX_CESTA];
    strcpy(kopia_cesty, argument);  // Urobime kopiu argumentu

    char *token = strtok(kopia_cesty, "/");
    Uzol *aktualny = aktualny_adresar;
    char *posledne_meno = NULL;

    // Prechadzame cestu a hladame uzol
    while (token) {
        posledne_meno = token;
        token = strtok(NULL, "/");

        Uzol *dalsi = NULL;
        // Hladame deti aktualneho uzla
        for (int i = 0; i < aktualny->pocet_deti; i++) {
            if (strcmp(aktualny->deti[i]->meno, posledne_meno) == 0) {
                dalsi = aktualny->deti[i];
                break;
            }
        }

        // Ak token nie je posledny, musi ist o adresar, ktory musi existovat a mat prava na spustanie
        if (token) {
            if (!dalsi || !dalsi->je_adresar || !(dalsi->prava & 1)) {
                printf("chyba\n");
                return;
            }
            aktualny = dalsi;
        } else {
            // Ak token je posledny, odstranime uzol
            if (!dalsi) {
                printf("chyba\n"); // Uzol neexistuje
                return;
            }
            // Ak nemame prava na zapis v rodicovskom adresari, vypiseme chybu
            if (!(aktualny->prava & 2)) {
                printf("chyba\n");
                return;
            }

            // Najdeme a odstranime uzol zo zoznamu deti
            for (int i = 0; i < aktualny->pocet_deti; i++) {
                if (aktualny->deti[i] == dalsi) {
                    free(dalsi);
                    aktualny->deti[i] = aktualny->deti[--aktualny->pocet_deti];  // Posuvame deti o miesto dozadu
                    return;
                }
            }
        }
    }
}
// Funkcia na zmenu prav suboru alebo adresara
void cmd_chmod(char *argument1, char *argument2) {
    // Ak nie je zadany argument, vypiseme chybu
    if (!argument1 || !argument2) { 
        printf("chyba\n"); 
        return; 
    }

    // Prevod argumentu na cislo pre prava
    int rezim = atoi(argument1);
    // Skontrolujeme, ci je rezim v platnom rozsahu
    if (rezim < 0 || rezim > 7) { 
        printf("chyba\n"); 
        return; 
    }

    // Najdeme cielovy uzol
    Uzol *ciel = find_node(aktualny_adresar, argument2, 0, NULL);
    if (!ciel) { 
        printf("chyba\n"); 
        return; 
    }

    // Nastavime nove prava pre uzol
    ciel->prava = rezim;
}

// Funkcia na zmenu vlastnika suboru alebo adresara
void cmd_chown(char *argument1, char *argument2) {
    // Ak nie je zadany argument, vypiseme chybu
    if (!argument1 || !argument2) { 
        printf("chyba\n"); 
        return; 
    }

    // Najdeme cielovy uzol
    Uzol *ciel = find_node(aktualny_adresar, argument2, 0, NULL);
    if (!ciel) { 
        printf("chyba\n"); 
        return; 
    }

    // Zmenime vlastnika uzla
    strcpy(ciel->vlastnik, argument1);
}

// Funkcia na zmenu aktualneho adresara
void cmd_cd(char *argument) {
    // Ak nie je zadany argument, vypiseme chybu
    if (!argument) { 
        printf("chyba\n"); 
        return; 
    }

    // Ak je zadany "..", prejdeme do rodicovskeho adresara
    if (strcmp(argument, "..") == 0) {
        if (aktualny_adresar->rodic) 
            aktualny_adresar = aktualny_adresar->rodic;
        return;
    }

    // Najdeme adresar
    Uzol *adresar = find_node(aktualny_adresar, argument, 0, NULL);
    if (!adresar || !adresar->je_adresar || !(adresar->prava & 1)) {
        printf("chyba prav\n");
        return;
    }
    aktualny_adresar = adresar;
}

// Funkcia na vypis obsahu suboru
void cmd_vypis(char *argument) {
    // Ak nie je zadany argument, vypiseme chybu
    if (!argument) { 
        printf("chyba\n"); 
        return; 
    }
    
    // Najdeme subor
    Uzol *subor = find_node(aktualny_adresar, argument, 0, NULL);
    if (!subor || !(subor->prava & 4)) { 
        printf("chyba prav\n"); 
        return; 
    }
    printf("ok\n");
}

// Funkcia na spustenie suboru
void cmd_spusti(char *argument) {
    // Ak nie je zadany argument, vypiseme chybu
    if (!argument) { 
        printf("chyba\n"); 
        return; 
    }

    // Najdeme subor
    Uzol *subor = find_node(aktualny_adresar, argument, 0, NULL);
    if (!subor || !(subor->prava & 1)) { 
        printf("chyba prav\n"); 
        return; 
    }
    printf("ok\n");
}

// Funkcia na zapis do suboru
void cmd_zapis(char *argument) {
    // Ak nie je zadany argument, vypiseme chybu
    if (!argument) { 
        printf("chyba\n"); 
        return; 
    }

    // Najdeme subor
    Uzol *subor = find_node(aktualny_adresar, argument, 0, NULL);
    if (!subor || !(subor->prava & 2)) { 
        printf("chyba prav\n"); 
        return; 
    }
    printf("ok\n");
}

// Hlavna funkcia programu
int main() {
    // Iniciujeme suborovy system
    init_fs();
    char riadok[256];
    
    // Hlavna smycka, caka na prikazy od uzivatela
    while (1) {
        printf("# ");
        if (!fgets(riadok, sizeof(riadok), stdin)) break;
        riadok[strcspn(riadok, "\n")] = '\0';  // Odstranenie noveho riadku

        // Rozdelenie riadku na prikaz a argumenty
        char *prikaz = strtok(riadok, " ");
        char *argument1 = strtok(NULL, " ");
        char *argument2 = strtok(NULL, " ");

        // Spustenie prislusnej funkcie podla prikazu
        if (!prikaz) continue;
        if (strcmp(prikaz, "ls") == 0) cmd_ls(argument1);
        else if (strcmp(prikaz, "touch") == 0) cmd_touch(argument1);
        else if (strcmp(prikaz, "mkdir") == 0) cmd_mkdir(argument1);
        else if (strcmp(prikaz, "rm") == 0) cmd_rm(argument1);
        else if (strcmp(prikaz, "chmod") == 0) cmd_chmod(argument1, argument2);
        else if (strcmp(prikaz, "chown") == 0) cmd_chown(argument1, argument2);
        else if (strcmp(prikaz, "cd") == 0) cmd_cd(argument1);
        else if (strcmp(prikaz, "vypis") == 0) cmd_vypis(argument1);
        else if (strcmp(prikaz, "spusti") == 0) cmd_spusti(argument1);
        else if (strcmp(prikaz, "zapis") == 0) cmd_zapis(argument1);
        else if (strcmp(prikaz, "quit") == 0) break;  // Ukoncenie programu
        else printf("chyba\n");
    }
    return 0;
}
