# Naloga 1: XML in JSON – SportSync

Konzolni del SportSync backenda (Spring Boot), ki prebere tri med seboj povezane XML datoteke, jih poveže prek ID-jev, filtrira po izbiri uporabnika, izpiše rezultat v tabelo in ga izvozi v `filtrirano.json` in `filtrirano.xml`.

Ker zunanji športni API-ji (football-data.org, Jolpica-F1, balldontlie) vračajo JSON, so XML podatki **izmišljeni**, a sledijo entitetam iz [podatki.md](../podatki.md): `Team`, `SportEvent` ter `User` z `TeamSubscription`.

## Podatki (`data/`)

| Datoteka | Entiteta | Zapisov | ID | Povezave |
|----------|----------|---------|----|----------|
| `ekipe.xml` | ekipa (nogomet, NBA) | 14 | `<ekipa id="fd:57">` | – |
| `dogodki.xml` | dogodek (tekma, F1 seja) | 18 | `<dogodek id="fd:537812">` | `domaciId`, `gostjeId` → ekipa |
| `uporabniki.xml` | uporabnik z naročninami | 12 | `<uporabnik id="u1">` | `<narocnina teamId="fd:57"/>` → ekipa |

- **Datumi** v obliki `YYYY-MM-DD`: `dogodek/datum`, `uporabnik/registriran`; ura (`HH:mm`, UTC) je ločeno polje.
- **Številčna polja:** `ustanovljena`, `rezultatDomaci`, `rezultatGostje`, `telegramChatId`; **besedilna:** `ime`, `tekmovanje`, `prizorisce` …
- **Prazne / manjkajoče vrednosti:** `<grbUrl></grbUrl>`, `<prizorisce></prizorisce>` (NBA), `<telegramChatId></telegramChatId>`, `<narocnine/>` (uporabnik u3); rezultat je izpuščen pri neodigranih dogodkih, ekipe pri F1, `telegramChatId` pri u5 in u10. Prazen element se prebere kot `null`.

## Delovanje

Koda je v paketu `si.um.feri.sportsync.xmljson`:

1. **Branje XML** – `XmlLoader` z Jackson XML deserializira datoteke v Java recorde (`model/Ekipa`, `Dogodek`, `Uporabnik`, `Narocnina`).
2. **Povezovanje** – `DataLinker` zamenja `domaciId`/`gostjeId` s celima ekipama (`PovezanDogodek`) in preveri, da naročnine uporabnikov (`teamId`) kažejo na obstoječe ekipe; filter `--uporabnik` nato prek naročnin poveže uporabnika z dogodki. Neznani ali podvojeni ID-ji se izpišejo kot opozorilo.
3. **Filtriranje** – `EventFilter`; filter se poda z argumenti (`FilterParser`) ali, če argumentov ni, v interaktivnem meniju (`InteraktivniMeni`). Več filtrov hkrati se kombinira (AND), rezultat je urejen po datumu in uri.
4. **Izpis** – `ConsoleTable` (tabela v konzoli).
5. **Izvoz** – `Exporter` zapiše `output/filtrirano.json` (seznam objektov) in `output/filtrirano.xml` (`<filtriraniDogodki><dogodek id="…">…`).

Vse se izvede ob zagonu aplikacije (`XmlJsonRunner`); spletni strežnik je zaenkrat izklopljen (`spring.main.web-application-type=none` v `application.properties`).

### Filtri

| Argument | Pomen | Primer |
|----------|-------|--------|
| `--sport` | šport: `FOOTBALL`, `F1`, `BASKETBALL` | `--sport=F1` |
| `--ekipa` | dogodki, kjer nastopa ekipa (ID) | `--ekipa=fd:57` |
| `--tekmovanje` | tekmovanje (ne glede na velike/male črke) | `--tekmovanje="Premier League"` |
| `--status` | `SCHEDULED`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED` | `--status=FINISHED` |
| `--od`, `--do` | datumsko okno (vključno) | `--od=2026-10-01 --do=2026-10-31` |
| `--uporabnik` | dogodki ekip, ki jih spremlja uporabnik (+ F1, če ima `f1=true`) | `--uporabnik=u1` |

Neveljavna vrednost (npr. `--sport=TENIS`, `--od=2026-13-01`, neznan ID) izpiše napako in nič ne izvozi.

## Zagon

Zahteve: JDK 17+. Ukaze poženi v mapi `backend/` (poti `data/` in `output/` so relativne nanjo).

```bash
# z argumenti
./mvnw spring-boot:run -Dspring-boot.run.arguments="--sport=FOOTBALL --od=2026-10-01"

# brez argumentov -> interaktivni meni (Enter = brez omejitve)
./mvnw spring-boot:run

# ali prek zgrajenega JAR-ja
./mvnw -DskipTests package
java -jar target/sportsync-0.0.1-SNAPSHOT.jar --uporabnik=u1 --od=2026-10-15
java -jar target/sportsync-0.0.1-SNAPSHOT.jar --tekmovanje="Premier League" --status=SCHEDULED
```

**PowerShell:** argument `-D...` mora biti v celoti v narekovajih, sicer ga PowerShell pri piki razbije (napaka `Unknown lifecycle phase ".run.arguments=..."`):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--sport=FOOTBALL --od=2026-10-01"
# vrednost s presledkom: enojni narekovaji znotraj dvojnih
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--tekmovanje='Premier League'"
java -jar target\sportsync-0.0.1-SNAPSHOT.jar --tekmovanje="Premier League" --status=SCHEDULED
```

Na Windows uporabi `mvnw.cmd`. Besedila in podatki, ki se izpišejo v terminal, so brez šumnikov (č, š, ž), da se pravilno prikažejo v vsaki konzoli.

### Primer izpisa

```
$ java -jar target/sportsync-0.0.1-SNAPSHOT.jar --uporabnik=u1 --od=2026-10-15
Nalozeno iz ...\backend\data: 14 ekip, 18 dogodkov, 12 uporabnikov

Filter: od=2026-10-15, uporabnik=u1
Uporabnik: Ana (Arsenal, Denver Nuggets, F1)
+------------+-------+------------+------------------+--------------------------------------------+----------+-----------+
| Datum      | Ura   | Sport      | Tekmovanje       | Dogodek                                    | Rezultat | Status    |
+------------+-------+------------+------------------+--------------------------------------------+----------+-----------+
| 2026-10-18 | 15:30 | FOOTBALL   | Premier League   | Arsenal vs Chelsea                         | 2 : 1    | FINISHED  |
| 2026-10-21 | 00:00 | BASKETBALL | NBA              | Denver Nuggets vs Boston Celtics           | -        | SCHEDULED |
| 2026-10-21 | 19:00 | FOOTBALL   | Champions League | Arsenal vs FC Bayern München               | -        | SCHEDULED |
| 2026-10-24 | 21:00 | F1         | Formula 1 2026   | F1 – United States Grand Prix (Qualifying) | -        | SCHEDULED |
| 2026-10-25 | 19:00 | F1         | Formula 1 2026   | F1 – United States Grand Prix (Race)       | -        | SCHEDULED |
| 2026-10-30 | 18:30 | F1         | Formula 1 2026   | F1 – Mexico City Grand Prix (Practice 1)   | -        | SCHEDULED |
| 2026-11-01 | 15:00 | FOOTBALL   | Premier League   | Manchester City vs Arsenal                 | -        | SCHEDULED |
| 2026-11-01 | 20:00 | F1         | Formula 1 2026   | F1 – Mexico City Grand Prix (Race)         | -        | SCHEDULED |
+------------+-------+------------+------------------+--------------------------------------------+----------+-----------+
Skupaj dogodkov: 8
Zapisano: ...\backend\output\filtrirano.json
Zapisano: ...\backend\output\filtrirano.xml
```

Izhod tega zagona je shranjen v [output/filtrirano.json](output/filtrirano.json) in [output/filtrirano.xml](output/filtrirano.xml).

Primer zapisa v JSON:

```json
{
  "id" : "fd:537812",
  "sport" : "FOOTBALL",
  "tip" : "MATCH",
  "tekmovanje" : "Premier League",
  "naslov" : "Arsenal vs Chelsea",
  "datum" : "2026-10-18",
  "ura" : "15:30",
  "prizorisce" : "Emirates Stadium",
  "status" : "FINISHED",
  "domaci" : {
    "id" : "fd:57", "sport" : "FOOTBALL", "ime" : "Arsenal", "kratkoIme" : "ARS",
    "tekmovanje" : "Premier League", "mesto" : "London", "ustanovljena" : 1886,
    "grbUrl" : "https://crests.football-data.org/57.png"
  },
  "gostje" : {
    "id" : "fd:61", "sport" : "FOOTBALL", "ime" : "Chelsea", "kratkoIme" : "CHE",
    "tekmovanje" : "Premier League", "mesto" : "London", "ustanovljena" : 1905,
    "grbUrl" : "https://crests.football-data.org/61.png"
  },
  "rezultatDomaci" : 2,
  "rezultatGostje" : 1
}
```

## Testi

`./mvnw test` zažene obstoječi `SportsyncApplicationTests`; ta nastavi `xmljson.enabled=false`, da se konzolni del med testom ne izvede.

## Oddaja (ZIP)

`data/`, `src/`, `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `README-naloga1.md`, `output/filtrirano.json`, `output/filtrirano.xml`.
