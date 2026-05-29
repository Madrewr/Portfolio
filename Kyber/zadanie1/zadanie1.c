#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define VELKOST_BUFFERA    4096

// Funkcia na spracovanie hesla - ak je v uvodzovkach, tak ich vyhodi
char *spracuj_heslo(const char *vstup)
{
   int dlzka = strlen(vstup);

   if (dlzka == 0)
   {
      char *prazdny_retazec = malloc(1);
      if (!prazdny_retazec)
      {
         printf("chyba\n");
         return(NULL);
      }
      prazdny_retazec[0] = '\0';    // Ak je prazdne heslo, vrat prazdny retazec
      return(prazdny_retazec);
   }
   if ((vstup[0] == '"' && vstup[dlzka - 1] == '"') || (vstup[0] == '\'' && vstup[dlzka - 1] == '\''))
   {
      char *novy_retazec = malloc(dlzka - 1);
      if (!novy_retazec)
      {
         printf("chyba\n");
         return(NULL);
      }
      strncpy(novy_retazec, vstup + 1, dlzka - 2);     // Skopiruje retazec bez uvodzoviek
      novy_retazec[dlzka - 2] = '\0';
      return(novy_retazec);
   }

   // Ak nie su uvodzovky, alokujeme pamäť a skopírujeme pôvodný reťazec
   char *kopie_retazca = malloc(dlzka + 1);
   if (!kopie_retazca)
   {
      printf("chyba\n");
      return(NULL);
   }
   strcpy(kopie_retazca, vstup);
   return(kopie_retazca);
}

// XOR sifrovanie / desifrovanie suboru
int xor_sifrovanie(const char *vstupny_subor, const char *vystupny_subor, const char *heslo)
{
   FILE *vstup = fopen(vstupny_subor, "rb");   // Otvorenie vstupneho suboru na citanie

   if (!vstup)
   {
      printf("chyba\n");
      return(-1);
   }

   FILE *vystup = fopen(vystupny_subor, "wb");   // Otvorenie vystupneho suboru na zapis
   if (!vystup)
   {
      printf("chyba\n");
      fclose(vstup);
      return(-1);
   }

   int           dlzka_hesla = strlen(heslo);
   int           index_hesla = 0;
   unsigned char buffer[VELKOST_BUFFERA];   // Buffer na citanie dat
   size_t        precitane_bajty;

   while ((precitane_bajty = fread(buffer, 1, VELKOST_BUFFERA, vstup)) > 0)
   {
      for (size_t i = 0; i < precitane_bajty; i++)
      {
         buffer[i]  ^= heslo[index_hesla];      // XOR kazdy bajt s heslom
         index_hesla = (index_hesla + 1) % dlzka_hesla;
      }

      if (fwrite(buffer, 1, precitane_bajty, vystup) != precitane_bajty)      // Zapis do suboru
      {
         printf("chyba\n");
         fclose(vstup);
         fclose(vystup);
         return(-1);
      }
   }

   if (ferror(vstup))     // Ak bola chyba pri citani
   {
      printf("chyba\n");
      fclose(vstup);
      fclose(vystup);
      return(-1);
   }

   fclose(vstup);
   fclose(vystup);
   return(0);
}

// Hlavna funkcia
int main(int argc, char *argv[])
{
   int   sifrovanie     = -1;
   char *heslo          = NULL;
   char *vstupny_subor  = NULL;
   char *vystupny_subor = NULL;
   int   pocet_s = 0, pocet_d = 0, pocet_p = 0, pocet_i = 0, pocet_o = 0;

   // Kontrola argumentov z prikazoveho riadku
   for (int i = 1; i < argc; i++)
   {
      if (strcmp(argv[i], "-s") == 0)
      {
         sifrovanie = 1;
         pocet_s++;
      }
      else if (strcmp(argv[i], "-d") == 0)
      {
         sifrovanie = 0;
         pocet_d++;
      }
      else if (strcmp(argv[i], "-p") == 0)
      {
         pocet_p++;
         if (i + 1 >= argc)
         {
            printf("chyba\n");
            return(EXIT_FAILURE);
         }
         heslo = spracuj_heslo(argv[++i]);
         if (!heslo)
         {
            return(EXIT_FAILURE);
         }
      }
      else if (strcmp(argv[i], "-i") == 0)
      {
         if (i + 1 < argc)
         {
            vstupny_subor = argv[++i];
            pocet_i++;
         }
         else
         {
            printf("chyba\n");
            return(EXIT_FAILURE);
         }
      }
      else if (strcmp(argv[i], "-o") == 0)
      {
         if (i + 1 < argc)
         {
            vystupny_subor = argv[++i];
            pocet_o++;
         }
         else
         {
            printf("chyba\n");
            return(EXIT_FAILURE);
         }
      }
      else
      {
         printf("chyba\n");
         return(EXIT_FAILURE);
      }
   }

   // Kontrola, ci neboli duplikovane prepinace
   if (pocet_s > 1 || pocet_d > 1 || pocet_p > 1 || pocet_i > 1 || pocet_o > 1)
   {
      printf("chyba\n");
      return(EXIT_FAILURE);
   }

   // Kontrola ci su vsetky potrebne argumenty zadane
   if (sifrovanie == -1 || !heslo || !vstupny_subor || !vystupny_subor)
   {
      printf("chyba\n");
      return(EXIT_FAILURE);
   }

   // Kontrola ci nie su zadane oba prepinace -s a -d naraz
   if (pocet_s + pocet_d != 1)
   {
      printf("chyba\n");
      return(EXIT_FAILURE);
   }

   // Kontrola ci vstupny a vystupny subor nie su rovnake
   if (strcmp(vstupny_subor, vystupny_subor) == 0)
   {
      printf("chyba\n");
      return(EXIT_FAILURE);
   }

   // Spustenie sifrovania alebo desifrovania
   if (xor_sifrovanie(vstupny_subor, vystupny_subor, heslo) == -1)
   {
      return(EXIT_FAILURE);
   }
   free(heslo);   // Uvolnenie alokovanej pamate
   return(EXIT_SUCCESS);
}
