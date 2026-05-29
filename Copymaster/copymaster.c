#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "options.h"


void FatalError(char c, const char* msg, int exit_status);
void PrintCopymasterOptions(struct CopymasterOptions* cpm_options);

// pomocna funkcia na kopirovanie; chunk je velkost bufferu
static int copy_fd(int fd_in, int fd_out, size_t chunk, int slow)
{
    // ak slow==1 -> maximalizuj pocet sys volani (1 bajt)
    if (slow) {
        char b;
        ssize_t r;
        while ((r = read(fd_in, &b, 1)) > 0) {
            ssize_t w = write(fd_out, &b, 1);
            if (w != 1) return -1; // chyba zapisu
        }
        return (r < 0) ? -1 : 0; // chyba citania ak r<0
    }

    // rychlejsie kopirovanie s vacsim bufferom
    char *buf = (char*)malloc(chunk);
    if (!buf) return -1;

    ssize_t r = 0;
    while ((r = read(fd_in, buf, chunk)) > 0) {
        ssize_t off = 0;
        while (off < r) {
            ssize_t w = write(fd_out, buf + off, (size_t)(r - off));
            if (w <= 0) { free(buf); return -1; }
            off += w;
        }
    }
    free(buf);
    return (r < 0) ? -1 : 0;
}

int main(int argc, char* argv[])
{
    struct CopymasterOptions cpm_options = ParseCopymasterOptions(argc, argv);

    //-------------------------------------------------------------------
    // Kontrola hodnot prepinacov
    //-------------------------------------------------------------------

    // Vypis hodnot prepinacov odstrante z finalnej verzie
    
    PrintCopymasterOptions(&cpm_options);
    
    //-------------------------------------------------------------------
    // Osetrenie prepinacov pred kopirovanim
    //-------------------------------------------------------------------
    
    if (cpm_options.fast && cpm_options.slow) {
        fprintf(stderr, "KONFLIKT PREPINACOV\n");
        exit(42);
    }
    if (cpm_options.create && cpm_options.overwrite) {
        fprintf(stderr, "KONFLIKT PREPINACOV\n");
        exit(42);
    }
    
    //-------------------------------------------------------------------
    // Kopirovanie suborov
    //-------------------------------------------------------------------
    
    // TODO Implementovat kopirovanie suborov
    //-------------------------------------------------------------------
// Kopirovanie suborov (B, -c, -o, -a) + modifikatory -s, -f
//-------------------------------------------------------------------

    // nastavenie stylu kopirovania z options.c
    size_t chunk = 4096;    // default
    int slow = 0;           // 0 = normal, 1 = velmi pomale (1B)
    if (cpm_options.fast)  chunk = 64 * 1024;   // -f minimalizuj pocet read/write
    if (cpm_options.slow)  slow  = 1;          // -s maximalizuj pocet read/write

    int fd_in  = -1;
    int fd_out = -1;
    struct stat st_in;

    // ziskaj info o infile (existencia + prava pre B)
    if (stat(cpm_options.infile, &st_in) == -1) {
        FatalError(cpm_options.create ? 'c' :
                   cpm_options.overwrite ? 'o' :
                   cpm_options.append ? 'a' : 'B',
                   "SUBOR NEEXISTUJE",
                   cpm_options.create ? 23 :
                   cpm_options.overwrite ? 24 :
                   cpm_options.append ? 22 : 21);
    }

    // otvor infile na citanie
    fd_in = open(cpm_options.infile, O_RDONLY);
    if (fd_in == -1) {
        FatalError(cpm_options.create ? 'c' :
                   cpm_options.overwrite ? 'o' :
                   cpm_options.append ? 'a' : 'B',
                   "INA CHYBA",
                   cpm_options.create ? 23 :
                   cpm_options.overwrite ? 24 :
                   cpm_options.append ? 22 : 21);
    }

    if (cpm_options.create) {
        // -c: outfile nesmie existovat
        if (access(cpm_options.outfile, F_OK) == 0) {
            close(fd_in);
            FatalError('c', "SUBOR EXISTUJE", 23);
        }
        // volitelne: validacia oktalnych prav
        if (cpm_options.create_mode > 0777) {
            close(fd_in);
            FatalError('c', "ZLE PRAVA", 23);
        }

        // vytvor len ak neexistuje
        fd_out = open(cpm_options.outfile, O_WRONLY | O_CREAT | O_EXCL, cpm_options.create_mode);
        if (fd_out == -1) {
            close(fd_in);
            FatalError('c', "INA CHYBA", 23);
        }

        if (copy_fd(fd_in, fd_out, chunk, slow) == -1) {
            close(fd_in); close(fd_out);
            FatalError('c', "INA CHYBA", 23);
        }

        close(fd_in); close(fd_out);
    }
    else if (cpm_options.overwrite) {
        // -o: outfile musi existovat; zachovat inode -> otvor bez O_CREAT
        if (access(cpm_options.outfile, F_OK) != 0) {
            close(fd_in);
            FatalError('o', "SUBOR NEEXISTUJE", 24);
        }

        fd_out = open(cpm_options.outfile, O_WRONLY | O_TRUNC);
        if (fd_out == -1) {
            close(fd_in);
            FatalError('o', "INA CHYBA", 24);
        }

        if (copy_fd(fd_in, fd_out, chunk, slow) == -1) {
            close(fd_in); close(fd_out);
            FatalError('o', "INA CHYBA", 24);
        }

        close(fd_in); close(fd_out);
    }
    else if (cpm_options.append) {
        // -a: outfile musi existovat; zapis na koniec
        if (access(cpm_options.outfile, F_OK) != 0) {
            close(fd_in);
            FatalError('a', "SUBOR NEEXISTUJE", 22);
        }

        fd_out = open(cpm_options.outfile, O_WRONLY | O_APPEND);
        if (fd_out == -1) {
            close(fd_in);
            FatalError('a', "INA CHYBA", 22);
        }

        if (copy_fd(fd_in, fd_out, chunk, slow) == -1) {
            close(fd_in); close(fd_out);
            FatalError('a', "INA CHYBA", 22);
        }

        close(fd_in); close(fd_out);
    }
    else {
        // BEZ PREPINACOV (B): vytvor outfile s rovnakymi pravami ako infile
        fd_out = open(cpm_options.outfile, O_WRONLY | O_CREAT | O_TRUNC, st_in.st_mode & 0777);
        if (fd_out == -1) {
            close(fd_in);
            FatalError('B', "INA CHYBA", 21);
        }

        if (copy_fd(fd_in, fd_out, chunk, slow) == -1) {
            close(fd_in); close(fd_out);
            FatalError('B', "INA CHYBA", 21);
        }

        close(fd_in); close(fd_out);
    }


    // cpm_options.infile
    // cpm_options.outfile
    
    //-------------------------------------------------------------------
    // Vypis adresara
    //-------------------------------------------------------------------
    
    if (cpm_options.directory) {
        // TODO Implementovat vypis adresara
    }
        
    //-------------------------------------------------------------------
    // Osetrenie prepinacov po kopirovani
    //-------------------------------------------------------------------
    
    // TODO Implementovat osetrenie prepinacov po kopirovani
    
    return 0;
}


void FatalError(char c, const char* msg, int exit_status)
{
    fprintf(stderr, "%c:%d:%s:%s\n", c, errno, strerror(errno), msg);
    exit(exit_status);
}

void PrintCopymasterOptions(struct CopymasterOptions* cpm_options)
{
    if (cpm_options == 0)
        return;
    
    printf("infile:        %s\n", cpm_options->infile);
    printf("outfile:       %s\n", cpm_options->outfile);
    
    printf("fast:          %d\n", cpm_options->fast);
    printf("slow:          %d\n", cpm_options->slow);
    printf("create:        %d\n", cpm_options->create);
    printf("create_mode:   %o\n", (unsigned int)cpm_options->create_mode);
    printf("overwrite:     %d\n", cpm_options->overwrite);
    printf("append:        %d\n", cpm_options->append);
    printf("lseek:         %d\n", cpm_options->lseek);
    
    printf("lseek_options.x:    %d\n", cpm_options->lseek_options.x);
    printf("lseek_options.pos1: %ld\n", cpm_options->lseek_options.pos1);
    printf("lseek_options.pos2: %ld\n", cpm_options->lseek_options.pos2);
    printf("lseek_options.num:  %lu\n", cpm_options->lseek_options.num);
    
    printf("directory:     %d\n", cpm_options->directory);
    printf("delete_opt:    %d\n", cpm_options->delete_opt);
    printf("chmod:         %d\n", cpm_options->chmod);
    printf("chmod_mode:    %o\n", (unsigned int)cpm_options->chmod_mode);
    printf("inode:         %d\n", cpm_options->inode);
    printf("inode_number:  %lu\n", cpm_options->inode_number);
    
    printf("umask:\t%d\n", cpm_options->umask);
    for(unsigned int i=0; i<kUMASK_OPTIONS_MAX_SZ; ++i) {
        if (cpm_options->umask_options[i][0] == 0) {
            // dosli sme na koniec zoznamu nastaveni umask
            break;
        }
        printf("umask_options[%u]: %s\n", i, cpm_options->umask_options[i]);
    }
    
    printf("link:          %d\n", cpm_options->link);
    printf("truncate:      %d\n", cpm_options->truncate);
    printf("truncate_size: %ld\n", cpm_options->truncate_size);
    printf("sparse:        %d\n", cpm_options->sparse);
}
