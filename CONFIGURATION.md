# Konfiguracja BedWars 3.12.0

## Pliki i przeładowanie

Wszystkie poniższe pliki znajdują się w `plugins/BedWars`.

| Plik | Zawartość |
| --- | --- |
| `messages.yml` | Teksty interfejsu, komunikaty, animacje title, układy scoreboardu i TAB |
| `config.yml` | Rozgrywka, sklep, efekty, ikony, świat i ogłoszenia cykliczne |
| `arenas.yml` | Punkty i konfiguracja map zapisane przez konfigurator |
| `worldborders.properties` | Środek i zasięg bordera dla każdej mapy |
| `quickbuy.yml` | Szybkie zakupy graczy |

`/bw przeladuj` wymaga `bedwars.admin` i działa także z konsoli. Sprawdza obydwa pliki przed aktywacją zmian, zamyka otwarte menu i odświeża NPC, scoreboard oraz TAB. Błędny YAML, nieprawidłowe typy lub niemożliwy rozmiar kolejki zatrzymują przeładowanie. Po poprawieniu pliku wykonaj komendę ponownie.

Nowe teksty są odczytywane przy wysyłaniu komunikatu lub budowaniu menu/przedmiotu. Przedmioty już znajdujące się w ekwipunku zachowują dotychczasową nazwę do ponownego wydania. Ich działanie pozostaje poprawne po zmianie nazwy. Czasy rozpoczętych, jednorazowych animacji zmienią się przy następnym wyświetleniu; zawartość klatek aktualizuje się na bieżąco. Ustawienia liczby miejsc i faz najlepiej zmieniać między meczami.

Brakujące klucze wiadomości dopisywane są automatycznie, a brakujące ustawienia główne przy starcie pluginu. Stary `config.yml` nie jest zastępowany domyślnym. Pierwsze utworzenie wpisu `titles.victory.text` importuje dotychczasowy `messages.victoryTitle`. Od tej pory edytuj tytuł w `messages.yml`.

## Wiadomości

Kolory mają składnię `&a`, `&c`, `&6`; formatowanie: `&l`, `&o`, `&r`. Obsługiwane są polskie znaki i UTF-8. Nazwy kluczy pozostają stałe, zmieniasz ich wartości.

Zmienne są zapisane wielkimi literami w nawiasach: `{PLAYER}`, `{TEAM}`, `{SECONDS}`, `{ERROR}`. Dla starszych komunikatów są też opisane w domyślnym wpisie `{VALUE1}`, `{VALUE2}` itd. Zachowaj nazwy występujące w danym wpisie; możesz przestawiać ich kolejność lub pomijać je. Każdy wpis otrzymuje własny zestaw zmiennych, bez PlaceholderAPI. Nieznana zmienna pozostaje zwykłym tekstem.

Puste `''` wycisza dany komunikat czatu. Lista zamiast tekstu daje kilka linii. Wyjątkiem jest `titles`, gdzie lista określa kolejne klatki animacji.

```yaml
commands:
  no-permission: '&cNie masz dostępu do tej komendy.'
configuration:
  reloaded:
    - '&aZapisane ustawienia są już aktywne.'
    - '&7Miłej gry!'
```

Katalog ma 1090 wpisów. Grupy odpowiadają funkcjom: `party`, `party-color-menu`, `game`, `shop`, `config-menu`, `spectator`, `admin-match-menu`, `server-diagnostics` itd. Najprościej wyszukać dotychczasowy tekst i zmienić jego wartość. Stałe komunikaty o uszkodzonych plikach potrzebne przed uruchomieniem katalogu oraz identyfikatory techniczne nie korzystają z tłumaczeń.

## Title

Przykład w `messages.yml`:

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
  victory:
    text: 'ZWYCIĘSTWO!'
    title: ['&6&l{TEXT}', '&e&l{TEXT}']
    subtitle: '&fWygrała twoja drużyna! &8| {TEAM}'
```

Odpowiadające ustawienia w `config.yml`:

```yaml
animations:
  waiting:
    enabled: true
    frameTicks: 20
    dotsTicks: 12
  victory:
    enabled: true
    frameTicks: 20
    durationTicks: 200
```

20 ticków to nominalnie sekunda. Silnik animacji działa co 4 ticki. Najkrótszy obsługiwany odstęp to 4 ticki. `waiting`, `countdown` i `respawn` trwają zgodnie ze stanem meczu. `start`, `respawned`, `bed-lost`, `eliminated`, `victory`, `defeat`, `finish` mają osobny czas wyświetlania. Dodatkowe warianty `countdown-last`, `countdown-paused`, `respawn-paused`, `respawn-bed-lost` i `respawn-bed-lost-paused` dziedziczą przełącznik swojej głównej animacji.

Dostępne zmienne title: `{PLAYER}`, `{TEAM}`, `{SECONDS}`, `{WINNER}`, `{TEXT}`, `{DOTS}`. Globalne `effects.titles: false` wyłącza wszystkie animacje.

## Scoreboard i TAB

`scoreboard.layouts.waiting/countdown/paused/running` zawierają listy linii. `running` jest używany również podczas podsumowania. `{TEAMS}` rozwija się do czterech linii opisanych szablonem `scoreboard.team`.

Dostępne zmienne: `{PLAYER}`, `{SUBJECT}`, `{DATE}`, `{MAP}`, `{PLAYERS}`, `{MAX_PLAYERS}`, `{SECONDS}`, `{TEAM}`, `{COLOR}`, `{NEXT_EVENT}`, `{KILLS}`, `{FINAL_KILLS}`, `{BEDS}`, `{OBSERVER}`, `{WATCHING}`, `{TEAMS}`, `{SERVER}` oraz `{RED_NAME}`, `{RED_ALIVE}` i odpowiedniki `GREEN`, `BLUE`, `WHITE`.

`tab.header`, `tab.footer-lobby`, `tab.footer-match` mogą być listami. `tab.name-player`, `tab.name-spectator` i `tab.name-admin` zmieniają wygląd nicków w TAB. Zmiana `teams.<kolor>.name` i `.short` zmienia wyświetlane nazwy, zachowując rzeczywiste kolory drużyn.

Limity Spigot 1.8: 15 linii scoreboardu, 40 znaków w linii, 32 w tytule i nazwie TAB, 16 w prefiksie drużyny. Plugin skraca tekst, nie pozostawiając urwanego znaku koloru. Powtarzające się linie są rozróżniane niewidocznym kolorem. `scoreboard.enabled`, `scoreboard.healthEnabled` i `tab.enabled` wyłączają odpowiednie elementy. Format daty: `scoreboard.dateFormat` w `config.yml`.

## Sklep i menu

`shop.items.<przedmiot>.name` w `messages.yml` to nazwa przedmiotu. Opcjonalna lista `.lore` dodaje własny opis; obsługuje `{PRICE}` i `{CURRENCY}`. Nazwy w katalogu tekstów używają myślników, np. `stone-sword`; istniejące identyfikatory sklepu w `config.yml` zachowują podkreślenia, np. `stone_sword`.

Ceny, waluty, ilości, dostępność przedmiotów i poziomy ulepszeń pozostają w `config.yml`: `shop.items`, `shop.tiered`, `upgrades`. Można je nadal zmieniać przez konfigurator sklepu.

Nazwy, lore, nagłówki i komunikaty GUI są w `messages.yml`. Ikony opisuje `config.yml -> menus.icons`. Ścieżka ikony odpowiada kluczowi jej nazwy, np.:

```yaml
menus:
  icons:
    lobby:
      give-join-item:
        dolacz-do-meczu-turniejowego-ppm:
          material: COMPASS
          data: 0
```

`data` jest opcjonalne. Używaj nazw materiałów ze Spigot 1.8.8. Nieprawidłowy materiał wraca do domyślnego. Funkcje przycisków, ich sloty i uprawnienia pozostają stałe. Tytuły menu są ograniczone do 32 znaków. Specjalne przedmioty mają własny znacznik; GUI i zakupy działają także wtedy, gdy różne przyciski mają identyczną nazwę.

## Ustawienia rozgrywki

| Grupa w `config.yml` | Co zmienia |
| --- | --- |
| `game` | Liczbę miejsc, start, respawn, reconnect, ochronę, wysokość budowania, próżnię, regenerację, tempo baz i powrót do lobby |
| `game.timers` | Czasy faz od początku meczu; `end` oznacza zamknięcie bordera |
| `generators.base` | Interwały żelaza/złota dla poziomów kuźni 0–4, szmaragdy i limity dropów |
| `generators.neutral` | Interwały diamentów/szmaragdów dla poziomów I–III oraz limit |
| `generators.sharing` | Współdzielenie surowców w bazach i promień |
| `generators.holograms` | Widoczność hologramów generatorów |
| `heightPenalty` | Włączenie kary, próg Y, HP/s i odstęp ostrzeżeń |
| `items.potions` | Czas i amplifier mikstur; amplifier 0 to poziom I |
| `items.magicMilk` | Czas odporności na pułapki |
| `items.tnt`, `items.fireball` | Zapalnik TNT, siłę wybuchu i podpalanie przez kulę |
| `items.defender`, `items.bedbug`, `items.bridgeEgg` | Czas działania stworzeń i jaja mostowego |
| `traps` | Czas efektów i odstęp aktywowania pułapek |
| `party` | Czas ważności zaproszenia |
| `suddenDeath` | Docelowy promień bordera, obrażenia i liczbę smoków |
| `jumps` | TNT jump i fireball jump |
| `effects`, `sounds`, `fireworks`, `animations` | Przełączniki efektów, ich wygląd, dźwięki i czasy |
| `world` | Blokadę pogody/mobów i opcjonalne zatrzymanie pory dnia |
| `holograms.configuration` | Widoczność znaczników konfiguratora |
| `announcements` | Cykliczne ogłoszenia, prefiks i odstęp |

Interwały generatorów bazowych są dzielone przez `game.baseGeneratorSpeed`, domyślnie 1.5. Limity oznaczają liczbę leżących surowców w pobliżu generatora. Czas jest podany w sekundach, chyba że klucz kończy się `Ticks`.

Kara wysokości domyślnie zaczyna się przy Y >= 105 i zadaje 4 HP/s, czyli 2 serca/s. `game.regenerationMultiplier: 0.75` daje 75% normalnego leczenia w tym samym czasie. To mnożnik ilości leczenia, nie wydłużenie odstępu o 25%.

Nagła śmierć nadal musi wyłonić zwycięzcę. Obrażeń po zamknięciu bordera nie można wyłączyć; `maximumDamage` ma minimum 20 HP, a przyrost minimum 0.1 HP/s. Finalny promień ma minimum 0.5 bloku (border 1×1). Czas `end` nie kończy meczu remisem. Ręczne zatrzymanie meczu przez admina nadal oznacza anulowanie.

Dźwięki mają `enabled`, `name`, `volume`, `pitch` osobno dla każdego zdarzenia. Nieprawidłowa nazwa dźwięku używa wartości domyślnej. `effects.sounds` jest globalnym przełącznikiem. Fajerwerki mają czas, odstęp, promień oraz moc i nadal nie zadają obrażeń.

Pogoda i obce moby pozostają domyślnie wyłączone. Blokady dotyczą lobby, aren i ich kopii; stworzenia BedWars oraz NPC działają normalnie. `freezeTime: false` nie narzuca pory dnia. Wyłączenie tej opcji po jej aktywowaniu w bieżącej sesji przywraca cykl dnia.

Skrajne wartości nowych opcji są ograniczane do zakresów obsługiwanych przez grę. Uprawnienia, identyfikatory komend, protokół pakietów, zasady ochrony i wymaganie rozstrzygnięcia meczu nie są przełącznikami kosmetycznymi.
