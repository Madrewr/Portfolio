
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define SUBOR "hesla.csv" // Nazov suboru kde su ulozene heslo, meno, kluce
#define MAX_RIADOK 256   // Maximalna dlzka suboru
#define HASH_DLZKA 32    // Dlzka vystupneho hashu

// Vlastna hashovacia funkcia
void XOR_sifrovanie(const char *vstup, char *vystup) {
    unsigned char hash[HASH_DLZKA] = {0}; // uchovanie hashu
    int dlzka = strlen(vstup);
    
    for (int i = 0; i < dlzka; i++) {
        hash[i % HASH_DLZKA] ^= vstup[i];
        hash[i % HASH_DLZKA] = (hash[i % HASH_DLZKA] << 3) | (hash[i % HASH_DLZKA] >> 5);
    }
    
    for (int i = 0; i < HASH_DLZKA; i++) {
        sprintf(vystup + (i * 2), "%02x", hash[i]); //zmeni to na hexa sustavu
    }
}

// Funkcia na overenie prihlasenia uzivatela
int overenie_pouzivatela(const char *meno, const char *heslo, const char *kluc) {
    // Otvori subor na citanie (vsetky hesla a kluce su ulozene v subore)
    FILE *subor = fopen(SUBOR, "r");
    if (!subor) { // Ak sa subor nepodarilo otvorit, vypiseme chybu
        printf("subor sa nepodarilo otvorit\n");
        return 0; // Vracame 0, lebo prihlasenie zlyhalo
    }
    
    char riadok[MAX_RIADOK]; // Pre citanie jednotlivych riadkov zo suboru
    char hash_hesla[HASH_DLZKA * 2 + 1]; // Pre ulozenie hash-u zadaného hesla
    XOR_sifrovanie(heslo, hash_hesla); // Vypocita hash pre zadane heslo
    
    // Prechadzame subor riadok po riadku
    while (fgets(riadok, sizeof(riadok), subor)) {
        // Definujeme premenne pre meno, heslo a kluce zo suboru
        char meno_subor[50], heslo_subor[HASH_DLZKA * 2 + 1], kluce[200];
        
        // Nacita meno, heslo a kluce zo suboru
        if (sscanf(riadok, "%[^:]:%[^:]:%[^\n]", meno_subor, heslo_subor, kluce) == 3) {
            // Porovname meno a heslo zo suboru s tymi, co zadal uzivatel
            if (strcmp(meno, meno_subor) == 0 && strcmp(hash_hesla, heslo_subor) == 0) {
                // Ak su mena a hesla rovnake, kontrolujeme kluc
                char *token = strtok(kluce, ","); // Rozdelime kluce podla ciarky
                while (token) {
                    if (strcmp(token, kluc) == 0) { // Porovname kazdy kluc
                        fclose(subor); // Uzavri subor, ak bol kluc spravny
                        return 1; // Prihlasenie uspesne
                    }
                    token = strtok(NULL, ","); // Prejdeme na dalsi kluc
                }
            }
        }
    }
    fclose(subor); // Uzavri subor po skonceni citania
    return 0; // Ak kluc nie je spravny, vraciame 0, prihlasenie neuspesne
}

// Funkcia na odstranenie kluca z ulozeneho zaznamu
void odstran_kluc(const char *meno, const char *kluc) {
    // Otvorime subor na citanie
    FILE *subor = fopen(SUBOR, "r");
    if (!subor) return; // Ak subor neexistuje, nic nerobime

    // Vytvorime dočasny subor na zapisovanie
    FILE *temp = fopen("temp.csv", "w");
    if (!temp) { 
        fclose(subor); // Ak sa temp subor nepodari otvorit, zatvorime subor a nic nerobime
        return;
    }

    char riadok[MAX_RIADOK]; // Buffor na citanie riadkov zo suboru
    int nasiel = 0; // Pripomienka, ci sme nasli a odstranili kluc

    // Prechadzame riadok po riadku zo suboru
    while (fgets(riadok, sizeof(riadok), subor)) {
        // Definujeme premenne pre meno, heslo a kluce zo suboru
        char meno_subor[50], heslo_subor[HASH_DLZKA * 2 + 1], kluce[200];

        // Nacita meno, heslo a kluce zo suboru
        if (sscanf(riadok, "%[^:]:%[^:]:%[^\n]", meno_subor, heslo_subor, kluce) == 3) {
            // Ak sa meno zo suboru zhoduje s tym, co chceme, pokracujeme
            if (strcmp(meno, meno_subor) == 0) {
                char nove_kluce[200] = ""; // Ukladame nove kluce bez toho, ktory chceme odstranit
                char *token = strtok(kluce, ","); // Rozdelime kluce podla ciarky
                int je_prvy = 1; // Flag, ci je to prvy kluc, aby sme vedeli, ci pridavat ciarku

                // Prechadzame cez kluce a odstranujeme ten, ktory nevyhovuje
                while (token) {
                    if (strcmp(token, kluc) != 0) { // Ak kluc nie je ten, ktory mame odstranit
                        if (!je_prvy) strcat(nove_kluce, ","); // Ak nie je prvy, pridaj ciarku
                        strcat(nove_kluce, token); // Pridaj kluc do noveho zoznamu klucov
                        je_prvy = 0; // Zmeni sa, ked pridame prvy kluc
                    } 
                    else {
                        nasiel = 1; // Kluc bol skutocne odstranený
                    }
                    token = strtok(NULL, ","); // Prejdi na dalsi kluc
                }

                // Zapiseme meno, heslo a nove kluce do docasneho suboru
                fprintf(temp, "%s:%s:%s\n", meno_subor, heslo_subor, nove_kluce);
            } 
            else {
                // Ak meno nesedi, zapiseme riadok bez zmeny
                fprintf(temp, "%s", riadok);
            }
        } 
        else {
            // Ak riadok nie je v spravnom formate, zapiseme ho bez zmeny
            fprintf(temp, "%s", riadok);
        }
    }

    fclose(subor); // Uzavri subor po citani
    fclose(temp);  // Uzavri docasny subor po zapise

    if (nasiel) { // Ak sme nasli kluc na odstranenie
        remove(SUBOR); // Odstranime povodny subor
        rename("temp.csv", SUBOR); // Premenovame docasny subor na povodny
    }
    else {
        remove("temp.csv"); // Ak nebol kluc odstraneny, zmazeme docasny subor
    }
}


int main() {
    char meno[50], heslo[50], kluc[20];
    
    printf("meno: ");
    scanf("%49s", meno);
    printf("heslo: ");
    scanf("%49s", heslo);
    printf("overovaci kluc: ");
    scanf("%19s", kluc);
    
    if (overenie_pouzivatela(meno, heslo, kluc)) {
        odstran_kluc(meno, kluc);
        printf("ok\n");
    }
    else {
        printf("chyba\n");
    }

    return 0;
}
