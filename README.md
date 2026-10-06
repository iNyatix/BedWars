# BedWars

Plugin na turniej BedWars organizowany przez Samorząd Uczniowski Elektronika. Cztery drużyny, wspólne dołączanie przez party, wybór koloru, konfiguracja mapy w grze i panel dla osoby prowadzącej mecz.

**Wersja:** 3.12.0  
**Autor:** Mateusz Nowosielski  
**Środowisko:** Spigot 1.8.8, Java 8, CSWM / SwoftyWorldManager

Na serwerze może być zapisanych kilka aren, ale jednocześnie działa jeden mecz. Domyślnie gra czeka na start wydany przez administratora. Drużyny to czerwoni, zieloni, niebiescy i biali; standardowo po 4 osoby.

## Spis treści

- [Uruchomienie](#uruchomienie)
- [Konfiguracja mapy](#konfiguracja-mapy)
- [Prowadzenie turnieju](#prowadzenie-turnieju)
- [Komendy](#komendy)
- [Party i kolory](#party-i-kolory)
- [Zasady meczu](#zasady-meczu)
- [Sklep i generatory](#sklep-i-generatory)
- [Obserwacja i panel admina](#obserwacja-i-panel-admina)
- [Pliki i ustawienia](#pliki-i-ustawienia)
- [Diagnostyka i historia](#diagnostyka-i-historia)
- [Aktualizacja i problemy](#aktualizacja-i-problemy)
- [Budowanie ze źródeł](#budowanie-ze-źródeł)

## Uruchomienie

Potrzebujesz działającego serwera 1.8.8 oraz wersji Continued Slime World Manager zgodnej z tym serwerem. BedWars szuka pluginu o nazwie `SwoftyWorldManager`, a następnie `SlimeWorldManager`, i korzysta z jego loadera `file`.

CSWM musi być poprawnie uruchomiony razem ze wszystkimi elementami wymaganymi przez jego wersję. Sam BedWars nie dostarcza silnika serwera ani CSWM. Bez działającego managera światów można wejść do konfiguratora, ale nie da się zatwierdzić mapy i przygotować kopii do gry.

NPC, hologramy, sklep i party są obsługiwane przez BedWars. Nie trzeba do nich instalować Citizens, HolographicDisplays, Vault ani osobnego pluginu party. Plugin nie wymaga bazy danych.

### Instalacja

1. Wyłącz serwer.
2. Wgraj `BedWars-3.12.0.jar` do `plugins`. Zostaw tylko jeden JAR BedWars.
3. Sprawdź, czy masz zainstalowany i skonfigurowany CSWM.
4. Wgraj rozpakowany folder mapy obok folderu `world`, np. `Rooftop`. W środku powinien znajdować się plik `level.dat` i dane świata. Nie wrzucaj ZIP-a do `plugins/BedWars`.
5. Uruchom serwer. BedWars utworzy pliki w `plugins/BedWars`.
6. Nadaj osobie konfigurującej uprawnienie `bedwars.admin`. Operator ma je domyślnie; z konsoli możesz użyć `op Nick`.
7. Wejdź na serwer i wpisz `/bw status`. Sprawdź, czy plugin widzi CSWM. Przy pierwszym uruchomieniu brak skonfigurowanej areny jest normalny.

Komendy w tym poradniku są zapisane do użycia w grze. W konsoli pomijasz `/`; polecenia zapisujące pozycję gracza oraz otwierające GUI wykonujesz w grze.

Plugin jest przeznaczony na serwer turniejowy. Wejście do lobby, kolejki i konfiguratora czyści ekwipunek. Nie służy do zachowywania wyposażenia z innych trybów gry.

## Konfiguracja mapy

### 1. Główne lobby

Stań tam, gdzie mają pojawiać się gracze po wejściu na serwer i po zakończeniu meczu, a następnie wpisz:

```text
/bw ustawlobby
```

**Główne lobby musi znajdować się w innym świecie niż arena.** Przykładowo: lobby w `world`, arena w `Rooftop`. Mapa meczu jest wymieniana po każdej grze, więc nie może jednocześnie służyć za główne lobby.

Bez własnego ustawienia plugin korzysta z `world`, X: 0, Y: 67, Z: 0. Ustaw prawidłowe miejsce przed wpuszczeniem graczy.

### 2. Wejście do konfiguratora

```text
/konfiguruj Rooftop
```

Podaj dokładną nazwę folderu mapy. Nazwa może zawierać litery bez polskich znaków, cyfry, kropkę, podkreślenie i myślnik; bez spacji i fragmentu `..`. TAB podpowiada dostępne mapy. Folder musi już istnieć i zawierać `level.dat`.

Plugin załaduje mapę, przeniesie Cię na nią, włączy Creative i da diamentowy kilof konfiguratora. **PPM kilofem otwiera menu.** Gdy wybierzesz ustawienie wymagające wskazania bloku, zamknie się menu — wtedy kliknij kilofem odpowiedni blok.

Nie konfiguruj światów zaczynających się od `bw_`, `bw_tpl_` ani `bw-tpl-`. Są to nazwy zarezerwowane dla kopii i szablonów. Do zmian zawsze otwieraj mapę źródłową, np. `Rooftop`.

### 3. Punkty główne i border

W części z punktami głównymi ustaw:

| Punkt | Jak go ustawić |
| --- | --- |
| Poczekalnia areny | Stań w miejscu, w którym mają czekać zapisani do meczu gracze, i kliknij przycisk ustawienia poczekalni. |
| Spawn obserwatorów | Stań w bezpiecznym miejscu z widokiem na mapę i zapisz punkt obserwacyjny. |
| Środek worldbordera | Stań na środku mapy i kliknij ustawienie środka. Liczą się współrzędne X i Z. |
| Zasięg worldbordera | Kliknij ustawienie zasięgu i wpisz liczbę na czacie. `anuluj` przerywa wpisywanie. |

Zasięg jest **promieniem**, czyli odległością od środka do krawędzi. Wpisanie `120` daje kwadrat 240×240 bloków. Konfigurator przyjmuje promień od 10 do 5000 bloków. Obejmij nim wszystkie wyspy i miejsca, po których mają poruszać się gracze.

Poczekalnia areny i główne lobby to dwa różne punkty. Poczekalnia jest częścią mapy, a główne lobby ustawiasz komendą `/bw ustawlobby` w osobnym świecie.

### 4. Bazy drużyn

Otwórz drużyny i skonfiguruj każdą z czterech baz:

| Ustawienie | Co zrobić |
| --- | --- |
| Spawn drużyny | Stań w miejscu odrodzenia, ustaw kierunek patrzenia i zapisz spawn. |
| Łóżko | Wybierz ustawianie łóżka i kliknij kilofem istniejący blok łóżka. |
| Generator bazy | Wybierz generator i kliknij blok pod miejscem, z którego mają wypadać surowce. |
| Pierwszy punkt strefy bez budowania | Kliknij jeden narożnik chronionego obszaru. |
| Drugi punkt strefy bez budowania | Kliknij przeciwległy narożnik. |

Strefa bez budowania jest prostokątem wyznaczonym po X i Z, chronionym na całej wysokości. Użyj jej do zabezpieczenia wnętrza bazy, generatora lub podejścia do NPC. Nie obejmuj nią miejsca, w którym gracze mają obudowywać łóżko.

W konfiguracji muszą być kompletne **wszystkie cztery drużyny**, nawet jeśli w danej rundzie zagrają tylko dwie albo trzy. Przy starcie meczu łóżka drużyn bez uczestników są usuwane i nie liczą się do wyniku.

### 5. Generatory neutralne i sklepy

W zakładce generatorów dodaj punkty diamentów i szmaragdów. Po wybraniu rodzaju generatora kliknij blok pod miejscem pojawiania się surowca. Wymagany jest co najmniej jeden generator każdego rodzaju; dodaj tyle, ile przewiduje mapa.

W zakładce sklepów ustaw w każdej bazie dwa NPC: sklep z przedmiotami i sklep ulepszeń drużyny. Punkt również wybierasz kliknięciem bloku. Łącznie potrzebne są co najmniej **4 sklepy z przedmiotami i 4 sklepy ulepszeń**.

W tym samym menu można zmieniać ceny, ilości i waluty przedmiotów, wyłączać pozycje sklepu oraz ustawiać ceny narzędzi i ulepszeń. Te ustawienia są wspólne dla serwera, a nie osobne dla każdej mapy.

Podczas konfiguracji hologramy oznaczają zapisane punkty. Nie są dekoracją meczu; plugin usuwa oznaczenia konfiguratora przed publikacją mapy. Hologramy generatorów pokazywane podczas gry mają osobne ustawienie.

### 6. Sprawdzenie i zatwierdzenie

W menu sprawdź brakujące elementy albo użyj:

```text
/bw sprawdz
```

Gdy konfiguracja jest kompletna, kliknij **Zatwierdź mapę**, a potem kliknij ten przycisk drugi raz w ciągu 10 sekund. W tym momencie plugin zapisuje mapę, przenosi graczy do głównego lobby i importuje czysty szablon do CSWM. Poczekaj na komunikat, że kopia mapy jest gotowa.

Nie trzeba osobno wykonywać `/swm import`. BedWars robi import przy zatwierdzaniu.

Punkty są zapisywane podczas edycji, ale zmiany mapy trzeba jeszcze opublikować przyciskiem zatwierdzenia. Wyjście z konfiguratora z nieopublikowanymi zmianami wymaga `Shift + klik` przycisku wyjścia.

### Co dzieje się z mapą po meczu

`Rooftop` pozostaje mapą do edycji. Jej zatwierdzona wersja trafia do CSWM jako szablon `bw_tpl_rooftop`. Na mecz plugin tworzy osobną kopię o nazwie zaczynającej się od `bw_rooftop_`.

Po rozgrywce gracze wracają do głównego lobby, zużyta kopia jest wyładowywana bez zapisywania zmian, a następna runda dostaje nową kopię szablonu. Zniszczone łóżka, TNT, woda i postawione bloki nie przechodzą do następnego meczu. Czyszczone są również ekwipunki i enderchesty graczy.

Wybrana arena jest zapamiętywana w `game.activeArena`. Po restarcie serwera plugin sam przygotowuje jej kopię. Ponowne otwieranie konfiguratora i zatwierdzanie poprawnie zapisanej mapy nie jest potrzebne.

## Prowadzenie turnieju

Po jednorazowej konfiguracji mapy każdą rundę możesz poprowadzić tak:

1. Sprawdź `/bw status`. Jeśli zmieniasz mapę, użyj `/bw wybierz Rooftop` i poczekaj na załadowanie kopii.
2. Gracze tworzą party, a liderzy wybierają kolory przez `/party kolor`.
3. Gracze dołączają kompasem w lobby albo przez `/bw dolacz`.
4. Sprawdź składy komendą `/bw druzyny`. W razie potrzeby przypisz gracza przez `/bw ustawdruzyne Nick czerwoni`.
5. Uruchom `/bw start`. Rozpocznie się odliczanie od 5 sekund.
6. Jako sędzia wejdź przez `/bw obserwuj`. Na pasku dostaniesz przedmioty do obserwacji i panel admina.
7. Po wygranej plugin pokaże wynik i TOP 3 finalnych zabójstw. Domyślnie po 10 sekundach wróci do lobby i przygotuje czystą mapę.

Po resecie gracze ponownie zapisują się do kolejki. Party i wybrany kolor pozostają na następną rundę, dopóki party istnieje w tej sesji serwera.

### Liczba graczy i start

`game.playersPerTeam` określa liczbę miejsc w jednej drużynie. Dla 8 stanowisk ustaw `2`, co daje 4×2 miejsca. `game.minPlayers` jest dolnym limitem startu, a nie liczbą osób wymaganą w każdej drużynie.

Do startu potrzebna jest kompletna mapa, gotowa kopia świata i przynajmniej dwie drużyny z graczami online. `/bw start` i `/bw wymusstart` działają tak samo: uruchamiają 5 sekund odliczania i nadal sprawdzają te warunki.

Domyślne `game.autoStartWhenFull: false` zostawia start administratorowi. Po ustawieniu `true` odliczanie ruszy automatycznie dopiero po zapełnieniu wszystkich miejsc. Dla automatycznego startu czas ustala `game.countdownSeconds`, domyślnie 20 sekund. Jeśli ktoś wyjdzie i poczekalnia przestanie być pełna, automatyczne odliczanie zostanie anulowane.

## Komendy

### Gracze

| Komenda | Działanie |
| --- | --- |
| `/bw` | Wyświetla pomoc. |
| `/bw dolacz` | Dołącza do kolejki. Członek party zapisuje też pozostałych członków online. |
| `/bw opusc` | Opuszcza kolejkę albo tryb obserwacji i wraca do lobby. Zawodnik nie może w ten sposób wyjść z trwającego meczu. |
| `/bw druzyna` | Pokazuje przydzieloną drużynę. |

Angielskie odpowiedniki to `/bw join`, `/bw leave` i `/bw team`.

### Administracja

Wszystkie poniższe komendy wymagają `bedwars.admin`. Plugin nie wprowadza osobnych rang.

| Komenda | Działanie |
| --- | --- |
| `/konfiguruj <folder_mapy>` | Otwiera konfigurator istniejącej mapy. |
| `/bw ustawlobby` | Zapisuje główne lobby w obecnym miejscu. |
| `/bw lista` | Pokazuje zapisane areny. |
| `/bw wybierz <mapa>` | Wybiera arenę i przygotowuje kopię jej szablonu. |
| `/bw sprawdz` | Wypisuje brakujące punkty konfiguracji aktywnej areny. |
| `/bw druzyny` | Pokazuje składy wszystkich drużyn. |
| `/bw ustawdruzyne <nick> <kolor>` | Przypisuje gracza do drużyny przed meczem, jeśli jest miejsce. |
| `/bw start` lub `/bw wymusstart` | Rozpoczyna 5-sekundowe odliczanie. |
| `/bw pauza` | Wstrzymuje mecz lub odliczanie. |
| `/bw wznow` | Wznawia wstrzymaną grę. |
| `/bw nastepnafaza` | Przestawia czas meczu na kolejne wydarzenie, także na zamknięcie bordera. Działa podczas gry, po wznowieniu pauzy. |
| `/bw zatrzymaj` | Anuluje mecz; po około 3 sekundach następuje reset. |
| `/bw reset` | Od razu czyści stan rozgrywki i przygotowuje świeżą mapę. To reset techniczny, bez normalnego podsumowania wyniku. |
| `/bw obserwuj` | Dołącza administratora do obserwacji trwającego lub wstrzymanego meczu. |
| `/bw panel` | Otwiera panel administratora obserwującego mecz. |
| `/bw obserwator <nick>` | Przełącza wskazanego gracza w obserwację. |
| `/bw wskrzes <nick>` | Przywraca uczestnika, który jest wyeliminowany, odradza się lub obserwuje. Gracz musi być online; działa również podczas pauzy. |
| `/bw status` | Pokazuje stan areny, CSWM i dane o wydajności serwera. |
| `/bw przeladuj` | Wczytuje `config.yml` i `messages.yml`. Nie służy do importowania map ani podmiany JAR-a. |

Kolory w komendach: `czerwoni`, `zieloni`, `niebiescy`, `biali`. Działają również `red`, `green`, `blue`, `white`.

Są też polecenia do ręcznej konfiguracji. Najwygodniej korzystać z GUI, szczególnie przy ustawianiu łóżek i chronionych obszarów.

| Komenda | Działanie |
| --- | --- |
| `/bw utworz <nazwa>` | Tworzy wpis areny w świecie, w którym stoisz. |
| `/bw usun <nazwa>` | Usuwa wpis areny. Nie jest poleceniem do sprzątania kopii świata. |
| `/bw ustawspawn <kolor>` | Zapisuje spawn drużyny w obecnym miejscu. |
| `/bw ustawlozko <kolor>` | Zapisuje pozycję bloku zajmowanego przez gracza jako punkt łóżka; w GUI wybierasz samo łóżko kliknięciem. |
| `/bw dodajgenerator diamenty` | Dodaje generator diamentów w obecnym miejscu. |
| `/bw dodajgenerator szmaragdy` | Dodaje generator szmaragdów w obecnym miejscu. |
| `/bw ustawsklep przedmioty` | Dodaje NPC sklepu z przedmiotami. |
| `/bw ustawsklep ulepszenia` | Dodaje NPC ulepszeń. |

Mapy i ich punkty zmieniaj poza meczem. Po edycji opublikuj szablon w konfiguratorze.

## Party i kolory

Nie ma osobnej komendy tworzenia party. Lider tworzy je, zapraszając pierwszego gracza:

```text
/party zapros Nick
```

Zaproszony gracz wpisuje `/party akceptuj`. Domyślnie ma na to 60 sekund. Party mieści maksymalnie tyle osób, ile wynosi `game.playersPerTeam`. Samo `/party kolor` też tworzy jednoosobowe party, jeśli jeszcze żadnego nie masz.

| Komenda | Działanie |
| --- | --- |
| `/party` | Pomoc. |
| `/party zapros <nick>` | Zaprasza gracza online. Dostępne dla lidera. |
| `/party akceptuj` | Przyjmuje aktywne zaproszenie. |
| `/party lista` | Pokazuje lidera, skład i wybrany kolor. |
| `/party kolor` | Otwiera wybór koloru drużyny. Kolor zmienia lider przed startem meczu. |
| `/party wyrzuc <nick>` | Lider usuwa wskazanego gracza online z party. |
| `/party opusc` | Opuszcza party. Jeśli wychodzi lider, przywództwo przejmuje kolejny członek. |
| `/party rozwiaz` | Lider rozwiązuje całe party. |

`/p` jest skrótem `/party`. Działają też podkomendy `invite`, `accept`, `info`, `color`, `kick`, `leave` i `disband`.

Po wyborze konkretnego koloru party trafia do tej drużyny przy kolejnych dołączeniach. Jeśli nie ma w niej miejsca dla całej grupy, dołączenie zostanie odrzucone — plugin nie przeniesie party sam do innego koloru. Można wtedy zwolnić miejsca albo wybrać inny kolor. Wybór automatyczny pozwala pluginowi znaleźć drużynę, która pomieści grupę.

Wystarczy, że dowolna osoba z party kliknie kompas lub wpisze `/bw dolacz`. Do kolejki dołączą członkowie party będący online, bez rozdzielania ich między drużyny. Osoba offline nie zajmuje z tego powodu miejsca w kolejce.

**Kolor nie jest rezerwacją całej drużyny.** Niepełne party może dzielić ją z innymi graczami, a inna grupa może wcześniej zająć miejsca. Na turnieju ustal kolory przed zapisaniem składów.

Party i jego kolor działają między meczami, ale nie są zapisywane na dysku. Po restarcie serwera trzeba utworzyć party i wybrać kolor ponownie. Wyjście z party samo w sobie nie zmienia już nadanego przydziału do drużyny.

## Zasady meczu

### Łóżka, śmierć i odrodzenie

Dopóki drużyna ma łóżko, gracze wracają do gry po śmierci. Domyślny respawn trwa 5 sekund, a po odrodzeniu gracz dostaje 3 sekundy ochrony. Atakowanie przeciwnika lub użycie przedmiotu może tę ochronę zakończyć wcześniej.

Zniszczenie łóżka odbiera drużynie kolejne odrodzenia. Gracze dostają title z informacją o utracie łóżka, a na serwerze odtwarzany jest dźwięk śmierci smoka Endu. Następna śmierć takiego zawodnika jest finalna: gracz przechodzi w obserwację, a w miejscu eliminacji pojawia się wizualny piorun.

Gracz, którego odliczanie respawnu już trwało w chwili utraty łóżka, kończy rozpoczęte odrodzenie. Title informuje go wtedy, że nie będzie miał następnego respawnu.

Surowce z ekwipunku zabitego gracza przechodzą do zabójcy. Jeśli nie ma przypisanego zabójcy, są upuszczane w miejscu śmierci. Ostatnie trafienie jest pamiętane przez 15 sekund, dzięki czemu zepchnięcie w przepaść może zaliczyć zabójstwo.

Wygrywa ostatnia drużyna, która ma zawodników pozostających w grze. Po zwycięstwie pojawiają się title i fajerwerki bez obrażeń oraz zestawienie trzech graczy z największą liczbą finalnych zabójstw.

### Ekwipunek i walka

- Gracz zaczyna z drewnianym mieczem i skórzaną zbroją w kolorze drużyny.
- Drewnianego miecza nie można wyrzucić. Lepszy miecz można oddać lub wyrzucić; jeśli gracz zostanie bez miecza, dostaje drewniany.
- Zakup lepszego miecza usuwa słabsze miecze z ekwipunku. Po śmierci wraca drewniany.
- Kupiona zbroja i nożyce zostają po odrodzeniu do końca meczu. Ulepszenie zbroi zmienia spodnie i buty; hełm i napierśnik pozostają skórzane.
- Kilof i siekiera tracą po śmierci jeden poziom, ale kupione narzędzie nie spada poniżej pierwszego poziomu.
- Stałych narzędzi nie można wyrzucać, a zbroi zdejmować w trakcie gry. Przedmioty zawodników nie zużywają wytrzymałości. Crafting jest zablokowany.
- Głód nie spada. Przy `game.regenerationMultiplier: 0.75` regeneracja przywraca o 25% mniej HP w tym samym czasie. Dotyczy również efektu regeneracji, ale nie natychmiastowego leczenia.
- Friendly fire jest domyślnie wyłączony.
- Wyrzucanie surowców nad przepaścią jest blokowane, żeby nie pozbywać się ich tuż przed śmiercią.

Enderchest przechowuje rzeczy w obrębie meczu, także pomiędzy zwykłymi odrodzeniami. Przy dołączeniu do nowej gry i przy resecie jest czyszczony.

### Budowanie, wysokość i świat

Gracze mogą niszczyć bloki postawione w meczu oraz łóżka przeciwników. Zwykłe bloki mapy są chronione. Strefy baz wyznaczone w konfiguratorze blokują stawianie bloków na całej wysokości. Wokół generatorów diamentów i szmaragdów domyślnie działa dodatkowa strefa 3×3.

Są dwa osobne ograniczenia wysokości:

| Ustawienie | Domyślne działanie |
| --- | --- |
| `heightPenalty.y: 105` | Od Y ≥ 105 aktywny zawodnik otrzymuje karę 4 HP na sekundę, czyli 2 serca, oraz ostrzeżenie. |
| `game.buildHeight: 120` | Nie można stawiać bloków powyżej Y = 120. |

Próg przepaści ustawia `game.voidY`, domyślnie `0`.

Deszcz, burze i zwykłe moby są domyślnie wyłączone w światach obsługiwanych przez BedWars. NPC, stworzenia kupowane w sklepie i smoki nagłej śmierci nadal działają. Czas dnia można opcjonalnie zatrzymać przez `world.freezeTime` i `world.time`.

### Fazy i nagła śmierć

Czasy w `game.timers` są liczone **od rozpoczęcia meczu**, nie od poprzedniej fazy.

| Czas domyślny | Wydarzenie | Klucz |
| --- | --- | --- |
| 6:00 | Diamenty II | `diamond2` |
| 12:00 | Szmaragdy II | `emerald2` |
| 18:00 | Diamenty III | `diamond3` |
| 24:00 | Szmaragdy III | `emerald3` |
| 30:00 | Utrata wszystkich pozostałych łóżek | `bedsGone` |
| 40:00 | Smoki i rozpoczęcie zwężania bordera | `suddenDeath` |
| 50:00 | Border osiąga końcowy rozmiar | `end` |

Podczas nagłej śmierci border zmniejsza się wokół środka ustawionego w konfiguratorze. Domyślnie kończy na rozmiarze **1×1 blok**. Przed pełnym zamknięciem gracz poza borderem traci 4 HP na sekundę.

Po zamknięciu obrażenia otrzymują wszyscy pozostali zawodnicy, również ci wewnątrz bordera. Zaczynają się od 4 HP na sekundę i rosną co sekundę o 1 HP, do 20 HP. Plugin odejmuje je bezpośrednio od zdrowia. `game.timers.end` nie kończy meczu remisem — gra trwa do wyłonienia zwycięskiej drużyny.

Przy rozliczaniu tych obrażeń najpierw obsługiwani są gracze z mniejszą ilością HP; przy równym HP kolejność jest losowana. Gdy po eliminacji zostanie jedna drużyna, mecz kończy się przed rozliczeniem kolejnych graczy. Ma to znaczenie, jeśli kilka osób ma zginąć w tej samej sekundzie.

Każda pozostająca w grze drużyna domyślnie dodaje jednego smoka, a po zakupie wzmocnienia smoka — dwa. Liczby smoków, końcowy promień i obrażenia są w `suddenDeath`. Mechanizm obrażeń końcowych ma dolne limity, żeby nie dało się wyłączyć rozstrzygnięcia meczu samym ustawieniem obrażeń na zero.

Ręczne `/bw zatrzymaj` jest anulowaniem meczu przez administratora i nie przyznaje zwycięstwa.

### Rozłączenie i pauza

Zawodnik ma domyślnie 180 sekund na powrót po rozłączeniu. Po powrocie dostaje odrodzenie z podstawowym wyposażeniem i zachowanymi stałymi zakupami; nie jest przywracany dokładny ekwipunek sprzed rozłączenia. Po przekroczeniu limitu zostaje wyeliminowany.

W nagłej śmierci nie ma oczekiwania na reconnect: rozłączenie oznacza eliminację. Gracze, którzy pozostają offline przy wejściu w tę fazę, również odpadają.

Pauza zatrzymuje czas meczu, generatory, odliczanie respawnu i zwężanie bordera oraz blokuje obrażenia zawodników. Nie zamraża wszystkich zadań serwera; np. czas życia już przywołanych stworzeń sklepowych nadal biegnie. Limit reconnectu jest liczony zegarem, a jego przekroczenie sprawdzane po wznowieniu gry.

## Sklep i generatory

### Zakupy

PPM na NPC z przedmiotami otwiera sklep. Płaci się surowcami z ekwipunku: żelazem, złotem, diamentami lub szmaragdami. Ceny, liczby sztuk i waluty można zmienić w konfiguratorze lub w `config.yml`.

| Kategoria | Zawartość |
| --- | --- |
| Bloki | Wełna drużyny, glina, szkło, end stone, drabiny, drewno, obsydian. |
| Walka | Miecze kamienny, żelazny i diamentowy oraz kij odrzutu. |
| Zbroja | Kolczugowe, żelazne i diamentowe spodnie z butami. |
| Narzędzia | Nożyce oraz cztery poziomy kilofa i siekiery. |
| Łuki | Strzały, zwykły łuk, łuk z Power I i łuk z Power I + Punch I. |
| Mikstury | Szybkość II na 45 s, skok V na 45 s i niewidzialność na 30 s. |
| Użytkowe | Złote jabłka, bedbug, strażnik snów, fireball, TNT, perła, woda, jajo mostowe, magiczne mleko i gąbki. |

**Szybki zakup:** `Shift + klik` przedmiotu w kategorii dodaje go do własnej listy. `Shift + klik` w szybkim zakupie usuwa pozycję. Układ jest zapisywany w `quickbuy.yml` dla danego gracza i zostaje po restarcie. Lista ma 21 miejsc; dodanie pozycji do pełnej listy zastępuje ostatnie miejsce.

### Przedmioty specjalne

- **TNT** zapala się po postawieniu; domyślny zapalnik ma 40 ticków, czyli około 2 sekund. TNT jump ma własne ustawienia siły i promienia.
- **Fireball** jest wystrzeliwany PPM. Obsługuje fireball jump; domyślnie wybuch nie podpala bloków.
- **Bedbug** to śnieżka, która przy trafieniu przywołuje silverfisha na 15 sekund.
- **Strażnik snów** przywołuje golema na 240 sekund. Golem i bedbug nie wybierają graczy swojej drużyny jako celu.
- **Jajo mostowe** tworzy most z wełny podczas lotu; domyślnie działa maksymalnie 80 ticków.
- **Magiczne mleko** przez 30 sekund pozwala przechodzić przez obszary pułapek bez ich uruchamiania.
- **Niewidzialność** ukrywa także zbroję i trzymany przedmiot. Zadawanie ciosów nie zdejmuje efektu. Trafienie przez gracza lub jego pocisk ujawnia niewidzialnego zawodnika; ujawnia go też pułapka alarmowa. Po wypiciu domyślnej mikstury niewidzialności usuwana jest pusta butelka.

Czasy znajdziesz w `items`, a parametry skoków w `jumps`. Poziom efektu mikstury zapisuje się jako `amplifier`: `0` oznacza poziom I, `1` poziom II itd.

### Ulepszenia drużyny

Drugi NPC sprzedaje ulepszenia za diamenty. Zakup działa na całą drużynę i zostaje do końca meczu.

| Ulepszenie | Działanie | Domyślna cena w diamentach |
| --- | --- | --- |
| Ostrzone miecze | Sharpness I na mieczach. | 8 |
| Wzmocniona zbroja | Protection I–IV. | 5 / 10 / 20 / 30 |
| Szalony górnik | Haste I–II. | 4 / 6 |
| Kuźnia | Cztery poziomy tempa generatora; ostatni odblokowuje szmaragdy. | 4 / 8 / 12 / 16 |
| Strefa leczenia | Regeneracja w pobliżu spawnu własnej bazy. | 3 |
| Wzmocnienie smoka | Więcej smoków podczas nagłej śmierci. | 5 |

Można kupić do trzech pułapek do kolejki. Pierwsza kosztuje 1 diament, druga 2, trzecia 4. Cena zależy od liczby pułapek już czekających w kolejce.

| Pułapka | Efekt domyślny |
| --- | --- |
| To pułapka! | Ślepota i spowolnienie intruza przez 8 s. |
| Kontratakująca | Szybkość I i skok II dla drużyny przez 15 s. |
| Alarmowa | Usunięcie niewidzialności intruza i powiadomienie drużyny. |
| Zmęczenie górnika | Mining Fatigue I dla intruza przez 10 s. |

Pułapki zużywają się w kolejności zakupu. Zasięg bazy ustala `game.baseRadius`, domyślnie 16 bloków od spawnu. Domyślny odstęp między uruchomieniami pułapek przez danego intruza wynosi 10 sekund.

### Generatory

Generatory bazowe pracują z mnożnikiem `game.baseGeneratorSpeed: 1.5`. Bez ulepszeń oznacza to średnio żelazo co około 1,33 s i złoto co około 5,33 s. Mnożnik dotyczy również baz po ulepszeniu kuźni.

Przy włączonym `generators.sharing.enabled` każdy aktywny zawodnik w promieniu 2,35 bloku od generatora bazowego dostaje własną sztukę surowca. Nie trzeba dzielić jednego dropu między dwie stojące obok osoby. Ta zasada nie dotyczy neutralnych generatorów diamentów i szmaragdów.

| Generator neutralny | Poziom I | Poziom II | Poziom III |
| --- | --- | --- | --- |
| Diamenty | 30 s | 18 s | 12 s |
| Szmaragdy | 65 s | 45 s | 35 s |

Domyślne limity leżących surowców to 64 żelaza, 32 złota i 8 szmaragdów przy generatorze bazy oraz 48 surowców przy generatorze neutralnym. Po osiągnięciu limitu generator czeka na zebranie przedmiotów. Interwały, limity i poziom kuźni odblokowujący szmaragdy są konfigurowalne.

Nad generatorami neutralnymi wyświetlane są rodzaj surowca, poziom i odliczanie do następnego dropu.

## Obserwacja i panel admina

Administrator dołącza komendą `/bw obserwuj` już po rozpoczęciu meczu. Aktywny zawodnik nie może użyć jej do wyjścia z walki. Po finalnej śmierci gracz otrzymuje zwykły tryb obserwacji automatycznie.

Obserwator jest niewidoczny dla pozostałych, może latać i nie wpływa na walkę, bloki ani przedmioty. Na pasku ma:

| Przedmiot domyślny | Działanie |
| --- | --- |
| Kompas | Lista zawodników z wyborem osoby do podglądu. |
| Komparator | Zakończenie podglądu i powrót do swobodnego lotu przy punkcie obserwacyjnym. |
| Łóżko | Powrót do głównego lobby. |
| Gwiazda Netheru, tylko admin | Panel sterowania meczem. |

Podgląd przenosi obserwatora nad wskazanego zawodnika i aktualizuje informacje na scoreboardzie. Kamera nie przełącza się na widok z jego oczu. Gdy odległość między nimi przekroczy 50 bloków, obserwator jest ponownie teleportowany do zawodnika. Śmierć lub wyjście obserwowanego gracza kończy podgląd.

W panelu admina dostępne są: ingerencja w świat, pauza/wznowienie, następna faza, lista zawodników do wskrzeszenia oraz dane o wydajności serwera. Można go również otworzyć przez `/bw panel`.

**Ingerencja** włącza Creative, pozwala budować, usuwać bloki, otwierać skrzynie i operować przedmiotami. Administrator staje się wtedy widoczny, a zmiana trybu jest ogłaszana na czacie. Nadal nie jest zawodnikiem i nie może walczyć w PvP. Po wyłączeniu ingerencji wraca do zwykłej obserwacji; wyposażenie używane podczas ingerencji jest czyszczone.

Sam operator biorący udział w meczu nie dostaje przez to dostępu do ingerencji. Panel wymaga administratora obserwującego grę, który nie jest aktywnym zawodnikiem.

## Pliki i ustawienia

Pliki pluginu znajdują się w `plugins/BedWars`:

| Plik | Do czego służy |
| --- | --- |
| `config.yml` | Ustawienia meczu, sklep, generatory, efekty, świat, ikony i ogłoszenia cykliczne. |
| `messages.yml` | Komunikaty, GUI, nazwy i opisy, title, scoreboard, TAB, NPC i hologramy. |
| `arenas.yml` | Punkty map zapisane przez konfigurator. |
| `worldborders.properties` | Środek i promień bordera dla każdej mapy. |
| `quickbuy.yml` | Zapisane szybkie zakupy graczy. |
| `historia-meczow.csv` | Wyniki i statystyki zakończonych lub anulowanych meczów. |

Szablony map są przechowywane przez CSWM. Przy przenoszeniu serwera zachowaj także jego dane oraz foldery źródłowe map — sam `arenas.yml` zawiera punkty, a nie bloki świata.

### Najczęściej zmieniane ustawienia

| Klucz w `config.yml` | Domyślnie | Znaczenie |
| --- | --- | --- |
| `game.playersPerTeam` | `4` | Liczba miejsc w drużynie. |
| `game.minPlayers` | `2` | Minimum graczy online do startu; nadal potrzebne są dwie drużyny. |
| `game.autoStartWhenFull` | `false` | Automatyczny start po zapełnieniu całej kolejki. |
| `game.countdownSeconds` | `20` | Odliczanie automatycznego startu. |
| `game.respawnSeconds` | `5` | Czas odrodzenia. |
| `game.respawnProtectionSeconds` | `3` | Ochrona po odrodzeniu. |
| `game.reconnectSeconds` | `180` | Czas na ponowne połączenie przed nagłą śmiercią. |
| `game.returnToLobbySeconds` | `10` | Czas od zwycięstwa do resetu. |
| `game.baseGeneratorSpeed` | `1.5` | Mnożnik tempa generatorów bazowych. |
| `game.regenerationMultiplier` | `0.75` | Mnożnik ilości regenerowanego zdrowia. |
| `game.friendlyFire` | `false` | Obrażenia między członkami jednej drużyny. |
| `heightPenalty.y` | `105` | Wysokość rozpoczęcia kary. |
| `heightPenalty.damage` | `4` | Kara wysokości w HP na sekundę. |
| `world.disableWeather` | `true` | Blokada pogody. |
| `world.disableMobs` | `true` | Blokada zwykłych mobów. |

Pozostałe grupy to `game.timers`, `shop`, `upgrades`, `generators`, `items`, `traps`, `party`, `suddenDeath`, `jumps`, `animations`, `effects`, `sounds`, `fireworks`, `scoreboard`, `tab`, `holograms`, `menus.icons`, `logging` i `announcements`. Szczegółowe przykłady są w [CONFIGURATION.md](CONFIGURATION.md), a wszystkie wartości domyślne w [config.yml](src/main/resources/config.yml).

### Własne teksty i wygląd

W `messages.yml` zmieniasz wartości, pozostawiając nazwy kluczy. Kolory zapisujesz przez `&`, np. `&a`, `&c`, `&6`, a pogrubienie przez `&l`. Plik zapisuj w UTF-8.

Zmienne mają postać `{PLAYER}`, `{TEAM}`, `{SECONDS}` lub `{VALUE1}`. Korzystaj ze zmiennych występujących w danym domyślnym wpisie; nie każda zmienna działa we wszystkich wiadomościach. PlaceholderAPI nie jest wymagane ani używane do ich podstawiania.

Przykładowa zmiana animacji oczekiwania:

```yaml
titles:
  waiting:
    title:
      - '&b&lOCZEKUJE'
      - '&f&lOCZEKUJE'
    subtitle: '&7Kliknij kompas, aby dołączyć{DOTS}'
    joined-subtitle: '&7Czekamy na start{DOTS} &8| {TEAM}'
    next-game-subtitle: '&7Czekamy na kolejny mecz{DOTS}'
    dots: ['.', '..', '...']
```

To fragment istniejącego `messages.yml`, nie jego zamiennik. Edytuj obecną sekcję `titles.waiting`, bez dopisywania drugiego `titles` na końcu pliku.

Lista w polu title określa kolejne klatki. Tempo animacji ustawiasz osobno w `config.yml`, np. `animations.waiting.frameTicks: 20`. Nominalnie 20 ticków to sekunda, a animacje są odświeżane co 4 ticki. Oczekiwanie, start, respawn, utrata łóżka, eliminacja, zwycięstwo i porażka mają własne teksty i ustawienia.

Pusty tekst `''` wycisza pojedynczy komunikat czatu. Lista w zwykłej wiadomości daje kilka linii. Klatki animacji, tekst zwycięstwa i subtitle edytujesz w `messages.yml`; stary `config.yml -> messages.victoryTitle` służy do migracji przy pierwszym utworzeniu nowego wpisu.

Własne układy scoreboardu są w `scoreboard.layouts`, a nagłówek, stopki i format nicków TAB w `tab` w pliku wiadomości. Scoreboard obsługuje do 15 linii. Długie teksty są skracane do limitów 1.8: m.in. 32 znaki w tytule GUI i scoreboardu oraz 40 znaków w linii scoreboardu. Domyślne oznaczenia serwera odnoszą się do Elektronika.

Nazwy przedmiotów sklepu znajdziesz w `shop.items` w `messages.yml`. Możesz dopisać im listę `lore` ze zmiennymi `{PRICE}` i `{CURRENCY}`. Identyfikatory w wiadomościach używają myślników, np. `stone-sword`, a w konfiguracji cen podkreśleń, np. `stone_sword`.

Przełączniki efektów są w `effects`. Poszczególne dźwięki mają własne `enabled`, `name`, `volume` i `pitch` w `sounds`; używaj nazw dźwięków z wersji 1.8.8. Czas, odstęp, promień i moc fajerwerków ustawiasz w `fireworks`. Ikony menu zmieniasz przez `menus.icons`, zachowując funkcje przycisków.

Cykliczne komunikaty są w `config.yml -> announcements`: przełącznik, odstęp w sekundach, prefiks i ponumerowane wiadomości. Domyślnie pojawiają się co 180 sekund.

### Przeładowanie

Po zmianie `config.yml` lub `messages.yml` wpisz:

```text
/bw przeladuj
```

Plugin sprawdza pliki, aktywuje ustawienia, zamyka otwarte menu i odświeża NPC, scoreboard oraz TAB. Błędny YAML lub nieprawidłowy typ wartości zatrzymuje przeładowanie, pozostawiając poprzednią aktywną konfigurację. Popraw plik i wykonaj komendę ponownie.

Przedmioty już wydane graczom mogą zachować starą nazwę do ponownego wydania. Zmiany liczby miejsc i harmonogramu faz rób pomiędzy meczami. Przy starcie dopisywane są brakujące ustawienia, a brakujące wiadomości również podczas przeładowania; własne wartości zostają.

Wczesne błędy uruchomienia, potrzebne zanim da się wczytać plik wiadomości, oraz identyfikatory techniczne pozostają stałe.

## Diagnostyka i historia

`/bw status` pokazuje stan meczu, liczbę graczy, wybraną mapę, kompletność punktów, wykrytego managera światów oraz dostępność szablonu i kopii areny.

W części technicznej znajdziesz:

- TPS z 1, 5 i 15 minut oraz średni i maksymalny odstęp między tickami z ostatnich 10 sekund;
- CPU procesu Java, systemu i głównego wątku oraz liczbę procesorów logicznych;
- pamięć JVM: użyty heap, przydzieloną pamięć, limit i non-heap;
- RAM systemu widziany przez JVM, statystyki garbage collectora i liczbę wątków;
- czas działania JVM, wolny dysk, liczbę graczy i view distance;
- liczbę światów, załadowanych chunków, encji i leżących przedmiotów;
- czas meczu, obserwatorów, bloki postawione przez graczy, rozmiar bordera oraz wersje serwera, Javy i systemu.

Nie każdy system udostępnia wszystkie pomiary CPU i RAM. Niedostępna wartość jest oznaczana w wyniku. Odstęp ticków obejmuje również oczekiwanie serwera, więc nie jest tym samym co czas pracy nad pojedynczym tickiem. Gdy plugin nie odczyta TPS Spigota, korzysta z własnej historii pomiarów i podaje jej długość.

Przy `logging.matches: true` plugin zapisuje `historia-meczow.csv`. Każdy uczestnik ma własny wiersz z datą, mapą, czasem gry, zwycięzcą, nickiem, drużyną, zabójstwami, finalnymi zabójstwami, zniszczonymi łóżkami, śmierciami i licznikiem surowców. Mecz zakończony przez `/bw zatrzymaj` ma wynik `ANULOWANY`. Można otworzyć ten plik w arkuszu kalkulacyjnym; separatorem jest przecinek.

## Aktualizacja i problemy

### Podmiana wersji

Wyłącz serwer, podmień JAR i uruchom go ponownie. Zachowaj `plugins/BedWars`, dane CSWM i mapy źródłowe. Nie usuwaj konfiguracji tylko po to, żeby pojawiły się nowe opcje — brakujące wpisy są dopisywane automatycznie.

Podmiana JAR-a wymaga restartu. `/bw przeladuj` wczytuje pliki ustawień, ale nie ładuje nowego kodu pluginu.

### Nie da się zatwierdzić mapy

Użyj `/bw sprawdz`. Najczęściej brakuje generatora bazy, jednego z punktów strefy bez budowania, bordera albo wymaganej liczby NPC. Sprawdź też, czy główne lobby jest na innym świecie i czy `/bw status` widzi działający CSWM. Przycisk zatwierdzenia wymaga dwóch kliknięć w ciągu 10 sekund.

### Po restarcie mapa się nie ładuje

Sprawdź `game.activeArena`, wynik `/bw status` i błąd CSWM w konsoli. Plugin przez około minutę ponawia próbę, jeśli manager światów nie jest jeszcze gotowy. `/bw wybierz <mapa>` lub dołączenie do kolejki może ponowić przygotowanie kopii.

Kompletna arena może odtworzyć brakujący szablon z zachowanego folderu źródłowego. Jeśli nie ma ani szablonu CSWM, ani źródłowej mapy, same punkty w `arenas.yml` nie wystarczą. Przywróć dane mapy, zamiast ponownie klikać zatwierdzenie bez sprawdzenia błędu.

### Gracze są w lobby, ale mecz nie startuje

Najpierw muszą dołączyć kompasem lub przez `/bw dolacz`. Samo wejście na serwer nie oznacza zapisu do meczu. Sprawdź `/bw druzyny`: do startu potrzebne są co najmniej dwie obsadzone drużyny, wymagana liczba graczy i gotowa mapa. Przy domyślnych ustawieniach start wydaje administrator przez `/bw start`.

### Party nie trafia do wybranego koloru

Sprawdź `/party lista`, liczbę miejsc w drużynie i osoby już przypisane do niej przez `/bw druzyny`. Party musi zmieścić się w całości. Po restarcie utwórz party i wybierz kolor ponownie.

### Zmiana wiadomości nie jest widoczna

Sprawdź, czy edytujesz `messages.yml` w folderze uruchomionego pluginu, i wykonaj `/bw przeladuj`. Błąd wcięć lub typu wartości oznacza, że nadal działa poprzednia konfiguracja. Przedmioty w ekwipunku mogą dostać nową nazwę dopiero po ponownym wydaniu.

### Nie można budować albo obserwator nie może otworzyć skrzyni

Dla zawodnika sprawdź strefy bez budowania, ochronę generatorów i limit wysokości. Zwykły obserwator nie ma dostępu do ingerencji. Administrator musi otworzyć panel i świadomie włączyć tryb ingerencji.

### Widać oznaczenia konfiguratora albo brakuje NPC

Upewnij się, że grasz na przygotowanej kopii meczu, a nie na mapie otwartej do edycji. Zatwierdź mapę i poczekaj na zakończenie przygotowania kopii. Sprawdź zapisane punkty sklepów, opcje hologramów oraz komunikaty w konsoli.

## Budowanie ze źródeł

Projekt jest przygotowany pod Java 8 i API Spigot `1.8.8-R0.1-SNAPSHOT`. Do budowania potrzebujesz JDK, Mavena i dostępu do repozytoriów zależności z `pom.xml`.

W katalogu zawierającym `pom.xml` wykonaj:

```sh
mvn clean package
```

Gotowy plugin znajdziesz w `target/BedWars.jar`. Sam zestaw testów uruchomisz przez `mvn test`.

Źródła są w `src/main/java/space/nyatix/bedwars`, domyślne pliki w `src/main/resources`, a testy w `src/test/java`. Główne części kodu to `command`, `config`, `game`, `gui`, `listener`, `message`, `model`, `shop` i `util`.

Przy pluginie korzystałem również z pomocy ChatGPT przy monotonnych sprawach, nie polecam korzystać tylko z tej opcji, aczkolwiek do monotonnych spraw które dobrze mu się wyjaśni (chociażby analiza kodu i stworzenie takiego README (całego sam również nie pisałem, projekt szkolny)) jest okej, ale nie stawiałbym w 100% na AI, dużo poprawek niż korzyści. Notuje dla osób które dostrzegły by w takim README nutkę AI, a nie o to chodzi.
