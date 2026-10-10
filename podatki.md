# Opis podatkov

Ta dokument opisuje podatke, ki se v sistemu SportSync prenašajo med moduli aplikacije, zunanjimi storitvami in uporabnikom: katere dokumente imamo, kakšna je njihova vsebina, oblika in tipi polj. Med moduli znotraj aplikacije gre za direktne Java klice (objekti, ne surov JSON), v tabeli spodaj pa je prikazana logična oblika podatka.

## 1. Pregled prenesenih dokumentov

| Dokument | Od → do | Oblika | Kdaj |
|----------|---------|--------|------|
| Odgovor športnega API-ja (tekme, dirke, rezultati) | zunanji API → sport modul | JSON | periodično |
| `SportEvent` (skupen format dogodka) | sport modul → calendar / notification modul, vmesnik | Java objekt (interno) / JSON (do vmesnika) | na zahtevo |
| `Team` (seznam ekip) | sport modul → vmesnik | JSON | ob izbiri ekip |
| `Subscription` (naročnine uporabnika) | vmesnik ↔ user modul, user modul → ostali moduli | JSON (do vmesnika) / Java objekt (interno) | ob spremembi / na zahtevo |
| `EventFinished` (obvestilo o koncu dogodka) | sport modul → notification modul | Java objekt (interni klic) | ob koncu dogodka |
| ICS koledar | calendar modul → koledarska aplikacija | iCalendar (RFC 5545), `text/calendar` | ko koledar osvežuje naročnino |
| Google Calendar dogodek | calendar modul → Google Calendar API | JSON | ob novem/spremenjenem dogodku |
| Telegram sporočilo | notification modul → Telegram Bot API | JSON (besedilo v HTML) | po dogodku, tedensko |
| Telegram posodobitev (ukaz `/start`) | Telegram → notification modul (webhook) | JSON | ko uporabnik piše botu |

Vsi datumi in časi se prenašajo v **UTC** v obliki ISO 8601 (npr. `2026-10-18T15:30:00Z`). V lokalni čas jih pretvori šele vmesnik oziroma koledarska aplikacija.

---

## 2. Šifranti (enum)

| Šifrant | Vrednosti |
|---------|-----------|
| `Sport` | `FOOTBALL`, `F1`, `BASKETBALL` |
| `EventType` | `MATCH`, `F1_PRACTICE`, `F1_QUALIFYING`, `F1_SPRINT_QUALIFYING`, `F1_SPRINT`, `F1_RACE` |
| `EventStatus` | `SCHEDULED`, `LIVE`, `FINISHED`, `POSTPONED`, `CANCELLED` |
| `F1Session` (izbira uporabnika) | `PRACTICE`, `QUALIFYING`, `SPRINT`, `RACE` |

Izbira `SPRINT` vključuje sprint kvalifikacije in sprint, izbira `PRACTICE` vse tri treninge.

---

## 3. Skupen format dogodka – `SportEvent`

Najpomembnejši dokument v sistemu. Vsak adapter v sport modulu odgovor zunanjega API-ja pretvori v ta format.

| Polje | Tip | Obvezno | Opis |
|-------|-----|---------|------|
| `id` | string | da | interni ID, oblika `{vir}:{zunanjiId}`, npr. `fd:537812` |
| `sport` | `Sport` | da | šport |
| `type` | `EventType` | da | vrsta dogodka |
| `competition` | string | da | tekmovanje, npr. `Premier League`, `Formula 1 2026` |
| `title` | string | da | naslov za koledar, npr. `Arsenal vs Chelsea` |
| `startTime` | string (ISO 8601, UTC) | da | začetek |
| `endTime` | string (ISO 8601, UTC) | da | ocenjen konec (nogomet +2 h, NBA +2,5 h, dirka +2 h, ostale F1 seje +1 h) |
| `venue` | string | ne | prizorišče oziroma dirkališče |
| `status` | `EventStatus` | da | stanje dogodka |
| `participants` | `Participant[]` | da | ekipe; pri F1 prazno (dogodek pripada vsem naročnikom F1) |
| `result` | `EventResult` | ne | prisoten, ko je `status = FINISHED` |
| `source` | string | da | ime vira, npr. `football-data.org` |
| `lastUpdated` | string (ISO 8601, UTC) | da | čas zadnje posodobitve |

`Participant`

| Polje | Tip | Opis |
|-------|-----|------|
| `teamId` | string | interni ID ekipe, npr. `fd:57` |
| `name` | string | ime ekipe |
| `role` | string | `HOME` ali `AWAY` |

`EventResult` – vsebina je odvisna od športa:

| Polje | Tip | Šport | Opis |
|-------|-----|-------|------|
| `homeScore` | integer | nogomet, NBA | točke/goli domačih |
| `awayScore` | integer | nogomet, NBA | točke/goli gostov |
| `halfTime` | object `{home, away}` | nogomet | rezultat ob polčasu |
| `periods` | object[] `{period, home, away}` | NBA | rezultat po četrtinah |
| `standingsPosition` | object `{teamId, position}`[] | nogomet | mesto ekip na lestvici po tekmi |
| `classification` | object[] `{position, driver, team, time}` | F1 | prvih 10 uvrščenih |
| `fastestLap` | object `{driver, time}` | F1 | najhitrejši krog (samo dirka) |

### Primer – nogomet (končana tekma)

```json
{
  "id": "fd:537812",
  "sport": "FOOTBALL",
  "type": "MATCH",
  "competition": "Premier League",
  "title": "Arsenal vs Chelsea",
  "startTime": "2026-10-18T15:30:00Z",
  "endTime": "2026-10-18T17:30:00Z",
  "venue": "Emirates Stadium",
  "status": "FINISHED",
  "participants": [
    { "teamId": "fd:57", "name": "Arsenal", "role": "HOME" },
    { "teamId": "fd:61", "name": "Chelsea", "role": "AWAY" }
  ],
  "result": {
    "homeScore": 2,
    "awayScore": 1,
    "halfTime": { "home": 1, "away": 0 },
    "standingsPosition": [
      { "teamId": "fd:57", "position": 1 },
      { "teamId": "fd:61", "position": 5 }
    ]
  },
  "source": "football-data.org",
  "lastUpdated": "2026-10-18T17:41:12Z"
}
```

### Primer – Formula 1 (dirka, še ni začela)

```json
{
  "id": "jolpica:2026-20-RACE",
  "sport": "F1",
  "type": "F1_RACE",
  "competition": "Formula 1 2026",
  "title": "F1 – Mexico City Grand Prix (Race)",
  "startTime": "2026-11-01T20:00:00Z",
  "endTime": "2026-11-01T22:00:00Z",
  "venue": "Autódromo Hermanos Rodríguez",
  "status": "SCHEDULED",
  "participants": [],
  "result": null,
  "source": "jolpica-f1",
  "lastUpdated": "2026-10-10T06:00:00Z"
}
```

### Primer – NBA (končana tekma)

```json
{
  "id": "bdl:18447170",
  "sport": "BASKETBALL",
  "type": "MATCH",
  "competition": "NBA",
  "title": "Denver Nuggets vs Boston Celtics",
  "startTime": "2026-10-21T00:00:00Z",
  "endTime": "2026-10-21T02:30:00Z",
  "venue": null,
  "status": "FINISHED",
  "participants": [
    { "teamId": "bdl:8", "name": "Denver Nuggets", "role": "HOME" },
    { "teamId": "bdl:2", "name": "Boston Celtics", "role": "AWAY" }
  ],
  "result": {
    "homeScore": 112,
    "awayScore": 108,
    "periods": [
      { "period": 1, "home": 28, "away": 25 },
      { "period": 2, "home": 30, "away": 27 },
      { "period": 3, "home": 26, "away": 31 },
      { "period": 4, "home": 28, "away": 25 }
    ]
  },
  "source": "balldontlie",
  "lastUpdated": "2026-10-21T02:44:05Z"
}
```

*Vrednosti v primerih so izmišljene in služijo le prikazu strukture.*

---

## 4. Pretvorba iz zunanjih virov v `SportEvent`

| `SportEvent` | football-data.org (`match`) | Jolpica-F1 (`Race`) | balldontlie (`game`) |
|--------------|-----------------------------|---------------------|----------------------|
| `id` | `"fd:" + id` | `"jolpica:" + season + "-" + round + "-" + seja` | `"bdl:" + id` |
| `startTime` | `utcDate` | `{seja}.date` + `{seja}.time` (npr. `Qualifying.date`) | `datetime` |
| `title` | `homeTeam.name + " vs " + awayTeam.name` | `"F1 – " + raceName + " (" + seja + ")"` | `home_team.full_name + " vs " + visitor_team.full_name` |
| `venue` | `venue` (če obstaja) | `Circuit.circuitName` | – |
| `status` | `SCHEDULED`/`TIMED` → `SCHEDULED`; `IN_PLAY`/`PAUSED` → `LIVE`; `FINISHED`; `POSTPONED`/`SUSPENDED` → `POSTPONED`; `CANCELLED` | iz časa seje in obstoja rezultatov | `status_state`: `scheduled`, `in_progress` → `LIVE`, `final` → `FINISHED`, `postponed`, `canceled` |
| `homeScore` / `awayScore` | `score.fullTime.home` / `.away` | – | `home_team_score` / `visitor_team_score` |
| `halfTime` | `score.halfTime` | – | – |
| `periods` | – | – | `home_q1..q4`, `visitor_q1..q4` |
| `classification` | – | `/results/` ali `/qualifying/` ali `/sprint/` | – |

En F1 vikend (`Race` v Jolpica) se razdeli v **več dogodkov** `SportEvent`: `FirstPractice`, `SecondPractice`, `ThirdPractice`, `Qualifying`, `SprintQualifying`, `Sprint` in sama dirka. Uporabnik dobi v koledar le seje, ki jih je izbral.

---

## 5. Entitete v bazi

Vse spodnje entitete so tabele v eni skupni PostgreSQL bazi; razdelitev po modulih je zgolj logična (kateri modul je lastnik posamezne tabele).

### user modul

**`User`**

| Polje | Tip | Opis |
|-------|-----|------|
| `id` | UUID | primarni ključ |
| `email` | string (unikaten) | e-pošta za prijavo |
| `passwordHash` | string | zgoščeno geslo (BCrypt) |
| `displayName` | string | prikazno ime |
| `calendarToken` | string (unikaten, naključen) | del osebnega ICS URL-ja |
| `telegramChatId` | long, lahko `null` | nastavi se ob povezavi s Telegramom |
| `telegramLinkCode` | string, lahko `null` | enkratna koda za povezavo |
| `googleRefreshToken` | string (šifriran), lahko `null` | za dostop do Google Calendar |
| `weeklySummary` | boolean | ali želi tedenski pregled |
| `createdAt` | timestamp | čas registracije |

API ključi za football-data.org in balldontlie so lastni aplikaciji (okoljske spremenljivke na strežniku), zato jih `User` ne vsebuje. Glej [zunanje-storitve.md](zunanje-storitve.md#pridobivanje-rezultata-po-dogodku).

**`TeamSubscription`**

| Polje | Tip | Opis |
|-------|-----|------|
| `id` | long | primarni ključ |
| `userId` | UUID | tuji ključ na `User` |
| `sport` | `Sport` | `FOOTBALL` ali `BASKETBALL` |
| `teamId` | string | npr. `fd:57`, `bdl:8` |
| `teamName` | string | za prikaz |

**`F1Subscription`**

| Polje | Tip | Opis |
|-------|-----|------|
| `userId` | UUID | primarni in tuji ključ |
| `sessions` | `F1Session[]` | izbrane seje, npr. `[QUALIFYING, RACE]` |

### sport modul

**`Team`** – `id` (string), `sport`, `name`, `shortName`, `competition`, `crestUrl` (string, lahko `null`)

**`SportEvent`** – shranjen v obliki iz poglavja 3 (rezultat kot JSON stolpec); za preverjanje rezultata po koncu dogodka se uporabljata obstoječa polja `endTime` in `status`, brez dodatnih polj (glej [zunanje-storitve.md](zunanje-storitve.md#pridobivanje-rezultata-po-dogodku))

### calendar modul

**`GoogleSyncedEvent`** – `userId` (UUID), `eventId` (string), `googleEventId` (string), `lastSyncedAt` (timestamp). Služi za posodabljanje in brisanje že vpisanih dogodkov.

### notification modul

**`SentNotification`** – `id`, `userId`, `eventId` (lahko `null` za tedenski pregled), `type` (`EVENT_SUMMARY` / `WEEKLY_OVERVIEW`), `sentAt`. Prepreči, da bi uporabnik isti povzetek dobil dvakrat.

---

## 6. Dokumenti med moduli

### Naročnine uporabnika – `GET /api/users/me/subscriptions` (user modul)

Resnični REST endpoint, ki ga kliče vmesnik.

```json
{
  "userId": "3f2c1a9e-7b44-4c1e-9a2d-8f0e6b1c2d3a",
  "teams": [
    { "sport": "FOOTBALL", "teamId": "fd:57", "teamName": "Arsenal" },
    { "sport": "BASKETBALL", "teamId": "bdl:8", "teamName": "Denver Nuggets" }
  ],
  "f1": { "enabled": true, "sessions": ["QUALIFYING", "SPRINT", "RACE"] },
  "weeklySummary": true
}
```

### Obvestilo o koncu dogodka (notification modul)

Ni več pravi REST klic, ampak neposredni interni klic `notificationService.handleEventFinished(event, finishedAt)` (sport modul → notification modul, oboje znotraj iste aplikacije). Oblika podatka, ki se posreduje:

```json
{
  "event": { "...": "SportEvent iz poglavja 3, status FINISHED" },
  "finishedAt": "2026-10-18T17:24:00Z"
}
```

Sporočila se pošljejo asinhrono (`@Async` / scheduler v notification modulu), da klic ne blokira sport modula.

### Naročniki dogodka (interni klic v user modulu)

Prej `GET /api/subscriptions?teamId=fd:57`, zdaj neposredni klic npr. `userService.findSubscribers(teamId)` oz. `findSubscribers(sport, session)`:

```json
[
  { "userId": "3f2c1a9e-7b44-4c1e-9a2d-8f0e6b1c2d3a", "telegramChatId": 123456789 }
]
```

---

## 7. ICS koledar (calendar modul → koledarska aplikacija)

- **URL:** `GET /calendar/{calendarToken}.ics`
- **Oblika:** iCalendar (RFC 5545), `Content-Type: text/calendar; charset=utf-8`
- **Vsebina:** vsi prihajajoči dogodki uporabnika in dogodki zadnjih 30 dni

```
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//SportSync//EN
X-WR-CALNAME:SportSync
BEGIN:VEVENT
UID:fd-537812@sportsync
DTSTAMP:20261010T060000Z
DTSTART:20261018T153000Z
DTEND:20261018T173000Z
SUMMARY:⚽ Arsenal vs Chelsea
LOCATION:Emirates Stadium
DESCRIPTION:Premier League
END:VEVENT
BEGIN:VEVENT
UID:jolpica-2026-20-RACE@sportsync
DTSTAMP:20261010T060000Z
DTSTART:20261101T200000Z
DTEND:20261101T220000Z
SUMMARY:🏎️ F1 – Mexico City Grand Prix (Race)
LOCATION:Autódromo Hermanos Rodríguez
END:VEVENT
END:VCALENDAR
```

`UID` ostane enak, tudi če se termin spremeni, zato koledar dogodek posodobi namesto da bi ga podvojil.

---

## 8. Google Calendar dogodek (calendar modul → Google)

`POST https://www.googleapis.com/calendar/v3/calendars/primary/events`

```json
{
  "summary": "⚽ Arsenal vs Chelsea",
  "location": "Emirates Stadium",
  "description": "Premier League – SportSync",
  "start": { "dateTime": "2026-10-18T15:30:00Z" },
  "end":   { "dateTime": "2026-10-18T17:30:00Z" },
  "extendedProperties": { "private": { "sportsyncEventId": "fd:537812" } }
}
```

Google vrne `id` dogodka, ki ga shranimo v `GoogleSyncedEvent` za kasnejše posodobitve (`PATCH`) ali brisanje (`DELETE`).

---

## 9. Telegram sporočila (notification modul → Telegram)

`POST https://api.telegram.org/bot<TOKEN>/sendMessage`

```json
{
  "chat_id": 123456789,
  "parse_mode": "HTML",
  "text": "⚽ <b>Arsenal 2 : 1 Chelsea</b>\nPremier League · HT 1 : 0\nArsenal is now 1st in the table."
}
```

Predloge sporočil po športih:

| Šport | Vsebina povzetka |
|-------|------------------|
| Nogomet | končni rezultat, rezultat ob polčasu, mesto ekip na lestvici |
| NBA | končni rezultat, rezultat po četrtinah |
| F1 kvalifikacije / sprint / dirka | prvih 10, pri dirki še najhitrejši krog |
| F1 treningi | samo v koledarju, brez povzetka |

Primer tedenskega pregleda:

```
📅 Your week (Oct 19 – Oct 25)
Tue 21:00  ⚽ Arsenal vs PSG (Champions League)
Wed 02:00  🏀 Nuggets vs Lakers
Sat 16:00  ⚽ Brentford vs Arsenal
Sun 21:00  🏎️ F1 – United States GP (Race)
```

Časi v tedenskem pregledu so prikazani v uporabnikovem časovnem pasu.

### Ukaz `/start` (Telegram → webhook notification modul)

```json
{
  "update_id": 912345678,
  "message": {
    "message_id": 15,
    "chat": { "id": 123456789, "type": "private" },
    "text": "/start a8f3k2"
  }
}
```

`a8f3k2` je enkratna koda iz `User.telegramLinkCode`.
