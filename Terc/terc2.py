import pygame
import time
import random
import mysql.connector
import math 
from sys import exit

# Trieda reprezentujuca tlacidlo v Pygame
class PygameButton():
    def __init__(self, text, font, width, height, pos, bg, fg, border = 0, border_radius = 0):
        # Nastavenie vlastnosti tlacidla
        self.rect = pygame.Rect(pos,(width, height))
        self.bg = bg  # Farba pozadia
        self.fg = fg  # Farba textu
        self.border = border  # Hrubka okraja
        self.border_radius = border_radius  # Zaoblenie rohov
        self.font = font  # Pouzite pismo
        self.text = text  # Text na tlacidle
        self.active = False  # Stav tlacidla (aktivne/neaktivne)
        self.toggle_color1 = bg  # Zakladna farba
        self.toggle_text1 = text  # Zakladny text
        self.hover_bg = bg  # Farba pri hover
        self.hover_fg = fg  # Farba textu pri hover

    def draw(self, screen, scroll = (0, 0)):
        # Vykresli tlacidlo na obrazovku
        self.text_render = self.font.render(self.text, True, self.fg)
        self.text_rect = self.text_render.get_rect(center = self.rect.center)
        self.text_rect.center = self.rect.center
        pygame.draw.rect(screen, self.bg, (self.rect.x - scroll[0], self.rect.y - scroll[1], self.rect.width, self.rect.height), self.border, self.border_radius)
        screen.blit(self.text_render, (self.text_rect.x - scroll[0], self.text_rect.y - scroll[1], self.text_rect.width, self.text_rect.height))

    def check_click(self, mouse_pos):
        # Skontroluje, ci bolo tlacidlo kliknute
        self.mouse_pos = mouse_pos
        return self.rect.collidepoint(self.mouse_pos)

    def toggle(self, toggle_color, toggle_text = "", checkforclick = True, mouse_pos = (0, 0)):
        # Prepne stav tlacidla (aktivne/inaktivne)
        changed = False
        self.toggle_color2 = toggle_color
        if toggle_text != "":
            self.toggle_text2 = toggle_text
        else:
            self.toggle_text2 = self.toggle_text1
        if checkforclick: 
            if self.check_click(mouse_pos):
                self.active = not self.active
                changed = True
        else:
            self.active = not self.active
        if self.active:
            self.bg = self.toggle_color2
            self.hover_bg = self.toggle_color2
            self.text = self.toggle_text2
        else:
            self.bg = self.toggle_color1
            self.hover_bg = self.toggle_color1
            self.text = self.toggle_text1
        
        return changed
    
    def hover(self, mouse_pos, bg = "", fg = ""):
        # Zmeni farby tlacidla pri najeti mysou
        if self.rect.collidepoint(mouse_pos):
            if bg:
                self.bg = bg
            if fg:
                self.fg = fg
        else:
            self.bg = self.hover_bg
            self.fg = self.hover_fg

# Funkcia simulujuca Galtonovu dosku
def galtonBoard(hladina, pozicia, rozptyl=1):
    if hladina <= 0:
        return pozicia
    if random.randint(0, 1) == 0:
        return galtonBoard(hladina - 1, pozicia - rozptyl, rozptyl)
    else:
        return galtonBoard(hladina - 1, pozicia + rozptyl, rozptyl) 

# Funkcia na zobrazenie tabulky vysledkov
def tabulka():
    while True:
        mouse_pos = pygame.mouse.get_pos()
        okno.fill("white")
        #vyber udaje z tabulky vysledkov
        cursor.execute("SELECT * FROM vysledky ORDER BY vysledok DESC")
        vysledky = cursor.fetchall()
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                exit()
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    program()
            if event.type == pygame.MOUSEBUTTONDOWN:
                if event.button == 1:
                    if tlacidlo_spat.check_click(mouse_pos):
                        program()
        # Vykreslenie tabulky
        text = font.render(f"PORADIE | MENO | CAS | SKORE", True, "black")
        text_rect = text.get_rect(center=(sirka // 2, 150))
        okno.blit(text, text_rect)
        y = text_rect.bottom + 50
        for index, i in enumerate(vysledky):
            if index < 10:  # Zobraz iba prvych 10 vysledkov
                text = font.render(f"{index + 1}. | {i[1]} | {i[2]} | {i[3]}", True, "black")
                text_rect = text.get_rect(center=(sirka // 2, y))
                okno.blit(text, text_rect)
                y += 50
        tlacidlo_spat.draw(okno)
        tlacidlo_spat.hover(mouse_pos, "blue")
        pygame.display.update()
        clock.tick(60)

# Rozmery okna
sirka = 1200
vyska = 800
velkost_terca = 750  # Velkost terca v pixeloch
velkost_mieridla = 200  # Velkost mieridla v pixeloch

# Inicializacia pygame
pygame.init()
okno = pygame.display.set_mode((sirka, vyska))
clock = pygame.time.Clock()

# Nacitanie obrazkov
terc = pygame.image.load("terc.jpg").convert()
terc = pygame.transform.scale(terc, (velkost_terca, velkost_terca))
terc_rect = terc.get_rect(topleft = (25, 25))
mieridlo = pygame.image.load("mieridlo.png").convert_alpha()
mieridlo = pygame.transform.scale(mieridlo, (velkost_mieridla, velkost_mieridla))
mieridlo_rect = mieridlo.get_rect()

# Definicia pisma
font = pygame.font.Font(None, 35)


def zobraz_textove_pole(okno, font, text):
    """Vykreslí textové pole s textom uprostred obrazovky."""
    # Nastavenie pozadia na biele
    okno.fill("white")
    # Vytvorenie textu na obrazovke
    sprava = font.render(text, True, "black")
    # Určenie pozície textu na obrazovke (v strede horizontálne, o 50 pixelov vyššie)
    sprava_rect = sprava.get_rect(center=(sirka // 2, vyska // 2 - 50))
    # Vykreslenie textu na obrazovku
    okno.blit(sprava, sprava_rect)


def zadanie_mena_v_okne(okno, font):
    """Zobrazí textové pole na zadanie mena a vráti zadané meno."""
    meno = ""
    zadavanie = True
    tlacidlo_spat = PygameButton("späť", font, 200, 80, (sirka // 2 - 100, vyska // 2 + 100), "black", "white")

    while zadavanie:
        okno.fill("white")  # Nastavenie pozadia na biele
        # Zobrazenie pokynu pre zadanie mena
        zobraz_textove_pole(okno, font, "Zadajte svoje meno (max 20 znakov):")
        
        pygame.mouse.set_visible(True)

        # Zobrazenie aktuálne zadaného mena
        meno_text = font.render(meno, True, "black")
        meno_rect = meno_text.get_rect(center=(sirka // 2, vyska // 2))  # Umiestnenie textu do stredu
        okno.blit(meno_text, meno_rect)


        # Vykreslenie tlačidla "späť"
        mouse_pos = pygame.mouse.get_pos()
        tlacidlo_spat.draw(okno)
        tlacidlo_spat.hover(mouse_pos, "blue")

        pygame.display.update()

        # Spracovanie udalostí
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                exit()  # Ukončenie programu
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_RETURN:  # Stlačenie Enter pre potvrdenie mena
                    zadavanie = False  # Ukončenie zadávania mena
                elif event.key == pygame.K_BACKSPACE:  # Zmazanie posledného znaku
                    meno = meno[:-1]
                else:
                    # Pridanie znaku ak meno nie je dlhšie ako 20 znakov
                    if len(meno) < 20:
                        meno += event.unicode
            if event.type == pygame.MOUSEBUTTONDOWN:
                if event.button == 1:
                    if tlacidlo_spat.check_click(mouse_pos):
                        program()  # Ak hráč klikne na "späť", vráti sa na úvod

    return meno  # Vráti zadané meno


def program():
    strely = []
    nepresnost = [0, 0]
    nepresnost2 = [0, 0]
    pocitadlo = 0
    pocet_striel = 0
    pocet_bodov = 0
    smer_vetra = -1
    vietor = 0
    rozptyl = 0
    strelba_hra = False
    podmienka = False
    povolena_strelba = True
    while True:
        okno.fill("white")
        mouse_pos = pygame.mouse.get_pos()
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                exit()
            if event.type == pygame.MOUSEBUTTONDOWN:
                if event.button == 1:
                    if mouse_pos[0] < 860:
                        podmienka = True
                    #tlacidlo smer vetra
                    if tlacidlo_smer_vetra.toggle("black", "smer vetra: zlava", mouse_pos=mouse_pos):
                        if tlacidlo_smer_vetra.active:
                            smer_vetra = 1 
                        else:
                            smer_vetra = -1
                    #tlacidlo na rozptyl
                    if tlacidlo_rozptyl.check_click(mouse_pos):
                        rozptyl += 1
                        if rozptyl > 10:
                            rozptyl = 0
                        tlacidlo_rozptyl.text = f"rozptyl - {rozptyl}"
                    # tlacidlo na vietor
                    if tlacidlo_vietor.check_click(mouse_pos):
                        vietor += 1
                        if vietor > 10:
                            vietor = 0
                        tlacidlo_vietor.text = f"vietor - {vietor}"
                    # tlacidlo na povolena strelba
                    if tlacidlo_strelba.check_click(mouse_pos):
                        strely = []
                        strelba_hra = not strelba_hra
                    # tlacidlo na zobrazenie tabulky
                    if tlacidlo_skore.check_click(mouse_pos):
                        tabulka()
                    # tlacidlo na rovnomerne strielanie
                    if tlacidlo_rovnomerne.check_click(mouse_pos):
                        strely = []
                        for i in range(100):
                            x = random.randint(-velkost_terca//2, velkost_terca//2)
                            y = random.randint(-velkost_terca//2, velkost_terca//2)
                            strely.append([terc_rect.center[0] + x + vietor * 20 * smer_vetra, terc_rect.center[1] + y])
                    # tlacidlo na strielanie ked moj ciel je stred (tlacidlo normalne)
                    if tlacidlo_normalne.check_click(mouse_pos):
                        strely = []
                        for i in range(100):
                            x = galtonBoard(velkost_terca, 0, 3)
                            y = galtonBoard(velkost_terca, 0, 3)
                            strely.append([terc_rect.center[0] + x + vietor * 20 * smer_vetra, terc_rect.center[1] + y])
            if event.type == pygame.MOUSEBUTTONUP:
                if event.button == 1:
                    povolena_strelba = True
            if event.type == pygame.MOUSEMOTION:
                cas1 = time.time()
        # tlacidlo strelba
        if strelba_hra and podmienka and povolena_strelba and mouse_pos[0] < 860:
            nepresnost2 = [0, 0]
            # ked mys nestoji na jednom mieste dlhsie ako je 0.5 s tak moja strela bude nepresnejsia
            if time.time() - cas1 < 0.5:
                nepresnost2[0] = random.randint(-300, 300)
                nepresnost2[1] = random.randint(-300, 300)
            cas1 = time.time()
            # posun mojej strely zavislosti na vetru a smere vetra
            x = mouse_pos[0] + nepresnost[0] + nepresnost2[0] + vietor * 20 * smer_vetra
            y = mouse_pos[1] + nepresnost[1] + nepresnost2[1]
            strely.append([x, y])

            body = int(max(0, (velkost_terca//2 - math.sqrt(abs(terc_rect.center[0] - x)**2 + abs(terc_rect.center[1] - y)**2)) * 100 // ((velkost_terca//2)*10) + 1))
            print(f"{pocet_striel + 1} - {body}b")
            pocet_striel += 1
            pocet_bodov += body
            # po desiatich strelach vyskoci okno pre zapis do tabulky
            if pocet_striel == 10:
                pocet_striel = 0
                strely = []
                priemer = pocet_bodov / 10
                print(f"priemer - {priemer}")
                pocet_bodov = 0
                meno = zadanie_mena_v_okne(okno, font)  # Funkcia na zadanie mena
                #vlozenie hodnot do tabulky
                sqlprikaz = "INSERT INTO vysledky (meno, vysledok) VALUES (%s, %s)"
                cursor.execute(sqlprikaz, (meno, priemer))
                mydb.commit()
            povolena_strelba = False
            podmienka = False
        okno.blit(terc, terc_rect)
        # Vykreslenie skóre priamo nad terčom
        text_skore = font.render(f"Body: {pocet_bodov}", True, "black")
        okno.blit(text_skore, (terc_rect.center[0] - text_skore.get_width() // 2, terc_rect.center[1] - velkost_terca // 2 - 20))
        # Vykreslenie strely
        for strela in strely:
            pygame.draw.circle(okno, "red", strela, 7)
        # Zmena farby ked ukazujem na tlacidlo
        for tlacidlo in tlacidla:
            tlacidlo.draw(okno)
            tlacidlo.hover(mouse_pos, "blue")
        # Vykreslenie mieritka
        if strelba_hra:
            pocitadlo += 1
            if pocitadlo % 12 == 0:
                nepresnost[0] = galtonBoard(rozptyl * 65, 0)
                nepresnost[1] = galtonBoard(rozptyl * 65, 0)
            mieridlo_rect.center = mouse_pos[0] + nepresnost[0], mouse_pos[1] + nepresnost[1]
            if mouse_pos[0] > 860:
                pygame.mouse.set_visible(True)
            else:
                pygame.mouse.set_visible(False)
                okno.blit(mieridlo, mieridlo_rect)
        
        pygame.display.update()
        clock.tick(60)

sirka_tlacidla = 300
vyska_tlacidla = 80
tlacidlo_rovnomerne = PygameButton("rovnomerne", font, sirka_tlacidla, vyska_tlacidla, (875, 70), "black", "white")
tlacidlo_normalne = PygameButton("normalne", font, sirka_tlacidla, vyska_tlacidla, (875, 170), "black", "white")
tlacidlo_strelba = PygameButton("strelba", font, sirka_tlacidla, vyska_tlacidla, (875, 270), "black", "white")
tlacidlo_rozptyl = PygameButton("rozptyl - 0", font, sirka_tlacidla, vyska_tlacidla, (875, 370), "black", "white")
tlacidlo_vietor = PygameButton("vietor - 0", font, sirka_tlacidla, vyska_tlacidla, (875, 470), "black", "white")
tlacidlo_smer_vetra = PygameButton("smer vetra: sprava", font, sirka_tlacidla, vyska_tlacidla, (875, 570), "black", "white")
tlacidlo_skore = PygameButton("tabulka", font, sirka_tlacidla, vyska_tlacidla, (875, 670), "black", "white")
tlacidlo_spat = PygameButton("spat", font, 200, 80, (975, 700), "black", "white")
tlacidla = [tlacidlo_rovnomerne, tlacidlo_normalne, tlacidlo_strelba, tlacidlo_rozptyl, tlacidlo_vietor, tlacidlo_smer_vetra, tlacidlo_skore]   

#spojenie s database
try:
    mydb = mysql.connector.connect(
        host="localhost",
        user="root",
        password="",
        database="terc"
    )
    cursor = mydb.cursor()
except mysql.connector.Error as err:
    print("chyba pripojenia:", err)

program()