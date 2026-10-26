# Naloga 2: Parser – SportSync

Razčlenjevalnik, ki s strani [formula1.com](https://www.formula1.com) prebere **voznike**, **moštva** in **proge** trenutne sezone ter jih shrani v JSON. To so podatki, ki jih Jolpica-F1 API (ki ga SportSync uporablja za razpored in rezultate) **nima**: statistika kariere in sezone, profil moštva (šef ekipe, šasija, pogonska enota …), podatki o progi (dolžina, rekord kroga) in URL-ji slik za prikaz v vmesniku.

## Kako deluje

Stran formula1.com je strežniško izrisana (Next.js), zato so vsi podatki že v HTML-ju in brskalnik (Puppeteer) ni potreben – HTML prenese in razčleni knjižnica **Jsoup**.

| Datoteka | Vir | Kaj prebere |
|----------|-----|-------------|
| `drivers.json` | `/en/drivers` → `/en/drivers/{id}` | ime, številka, država, moštvo, datum in kraj rojstva, slika, statistika sezone in kariere |
| `teams.json` | `/en/teams` → `/en/teams/{id}` | polno ime, sedež, šef ekipe, tehnični šef, šasija, pogonska enota, rezervni vozniki, prvi nastop, vozniki (ID-ji iz `drivers.json`), logo, slika dirkalnika, statistika |
| `circuits.json` | `/en/racing/{leto}` → `/en/racing/{leto}/{id}` | krog, termin, uradno ime dirke, kraj, dolžina proge, št. krogov, dolžina dirke, prva dirka, rekord kroga (čas, voznik, leto), zemljevid proge |

- Statistika je na strani v parih `<dt>`/`<dd>`: pred `<dl>` z razredom `DataGrid-module_rows` je sezona, v njem kariera, za njim profil.
- Kraj in uradno ime dirke sta prebrana iz JSON-LD (`schema.org/SportsEvent`).
- Števila so pretvorjena v številske tipe (`"236"` → `236`, `"5.793km"` → `5.793`), datum rojstva v `YYYY-MM-DD`.
- Med zahtevki je zamik 1 s; ob napaki se zahtevek ponovi do 3-krat. Če stran kljub temu ne uspe, se izpiše opozorilo in parser nadaljuje (`robots.txt` scraping dovoljuje).

## Koda (`src/main/java/si/um/feri/sportsync/scraper/`)

- `F1Parser` – razčlenjevanje HTML-ja (brez omrežja, testirano v `F1ParserTest`)
- `F1Scraper` – prenašanje strani, zamik in ponovni poskusi
- `ScraperRunner` – zagon in zapis JSON
- `model/` – `Voznik`, `Mostvo`, `Proga`

## Zagon

V mapi `backend/` (na Linux/macOS `./mvnw`):

```
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=scraper"
```

Profil `scraper` izklopi nalogo 1 in vklopi parser. Izhod je v `output/f1/`, primer izhoda pa v [example_output/f1](example_output/f1). Druga sezona ali zamik: `"-Dspring-boot.run.arguments=--scraper.season=2025 --scraper.delay-ms=2000"`.

```
Vozniki (https://www.formula1.com/en/drivers):
  [1/22] voznik george-russell
  ...
Zapisano: ...\backend\output\f1\drivers.json (22 zapisov)
Mostva (https://www.formula1.com/en/teams):
  [3/11] Opozorilo: mostvo red-bull-racing preskocen (HTTP error fetching URL. Status=500, ...)
  ...
Zapisano: ...\backend\output\f1\teams.json (9 zapisov)
Proge sezone 2026 (https://www.formula1.com/en/racing/2026):
  ...
Zapisano: ...\backend\output\f1\circuits.json (23 zapisov)

Skupaj: 22 voznikov, 9 mostev, 23 prog
```

Strani `red-bull-racing` in `williams` na formula1.com v času izdelave (26. 10. 2026) vračata HTTP 500 (napaka na strani strežnika, tudi ob ponovnih poskusih), zato manjkata v `teams.json`.

## Primer zapisa (`circuits.json`)

```json
{
  "id" : "italy",
  "krog" : 13,
  "termin" : "04 - 06 Sep",
  "uradnoIme" : "FORMULA 1 PIRELLI GRAN PREMIO D’ITALIA 2026",
  "kraj" : "Monza",
  "drzava" : "Italy",
  "dolzinaKm" : 5.793,
  "steviloKrogov" : 53,
  "dolzinaDirkeKm" : 306.72,
  "prvaDirka" : 1950,
  "rekordKroga" : "1:20.901",
  "rekordVoznik" : "Lando Norris",
  "rekordLeto" : 2025,
  "zemljevidUrl" : "https://media.formula1.com/image/upload/c_fit,h_704/q_auto/v1740000001/common/f1/2026/track/2026trackmonzadetailed.webp",
  "url" : "https://www.formula1.com/en/racing/2026/italy"
}
```
