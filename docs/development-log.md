# Development log

> **Stav fází + co je další.** Aktualizuj při dokončení každé fáze.
> Pro kontext projektu viz `CLAUDE.md`. Pro hlubší architekturní rozhodnutí viz `architecture.md`.

---

## 📊 Stav fází (k 2026-05-23)

| # | Fáze | Stav | Branch |
|---|------|------|--------|
| 0 | Foundation | ✅ DONE | `feature/phase-0-foundation` |
| 1 | MVP diary | ✅ DONE | `feature/phase-1-mvp-diary` |
| 2 | Trenér + GroupLessonPlan | ⏳ **NEXT** | – |
| 3 | Rozšířené typy cviků | – | – |
| 4 | Timer / stopky | – | – |
| 5 | Statistiky | – | – |
| 6 | Email notifikace | – | – |
| 7 | Integrace s rezervacemi | – | – |
| 8 | Individuální plány od trenéra | – | – |

**Branch status:** všechno lokálně, **nepushnuto na origin**. Žádný PR/merge zatím.

---

## ✅ Fáze 0 — Foundation (DONE)

**Branch:** `feature/phase-0-foundation` (9 commitů)
**Cíl:** čistý základ — login, register, role, Thymeleaf skeleton, žádné staré bugy.

### Co bylo dodáno

- Sjednocený `account` model (USER/ADMIN role)
- Flyway V1 migrace
- Jeden `SecurityFilterChain` s role-based authorize
- AdminInitializer pro bootstrap admina (`admin@admin.cz / heslo123`)
- Registrace + login + logout (form + REST)
- Thymeleaf layout + login/register/dashboard
- 7 smoke testů (account flow)
- DB heslo přes env var (`DB_PASSWORD`, default `postgres`)
- `application-prod.properties` pro Railway
- Hard delete starého kódu (User+Admin+Training+Exercise+Customizing+Emom+Circuit) — 64 souborů smazáno

### Klíčové opravy (z původního kódu)

- `POST /api/createNewTraining` neukládal nic → controller smazán
- `UserServiceImpl.getCurrentUserInfo` casting na `AdminEntity` → odstraněno s celým souborem
- Konflikt dvou `authenticationManager` beans → vyřešeno jedním filter chainem
- Heslo k DB v gitu → env var s lokálním fallbackem
- `EntityScan` na neexistující `com.matejmarek.*` → odstraněno
- `System.out.println` → SLF4J

---

## ✅ Fáze 1 — MVP diary (DONE)

**Branch:** `feature/phase-1-mvp-diary` (8 commitů včetně hotfixu)
**Cíl:** klient se přihlásí, zaloguje vlastní trénink se cviky a sety, vidí ho v deníku.

### Co bylo dodáno

- **V2 migrace** — `exercise_catalog_item`, `training_tag`, `training`, `training_exercise`,
  `exercise_set`, `training_tag_link`
- **V3 seed** — 92 cviků pro KB/funkční gym
- **V4 seed** — 8 systémových tagů (Kettlebell, Cardio, Bodyweight, OS Resets, ...)
- **JPA entity model**: `TrainingEntity`, `TrainingExerciseEntity`, `ExerciseSetEntity`,
  `ExerciseCatalogItemEntity`, `TrainingTagEntity`
- **Enums**: `TrainingDifficulty` (LIGHT/MEDIUM/HARD), `TrainingExerciseType` (FREEFORM + budoucí),
  `BodyRegion`, `MovementPattern`, `Equipment`
- **Service vrstva**: `TrainingService` s ownership check, `ExerciseCatalogService`, `TrainingTagService`
- **DTO**: `TrainingInput`, `TrainingExerciseInput`, `SetInput` (form-backing) + Jakarta validace
- **REST API**: `/api/exercise-catalog?q=`, `/api/tags`, globální error handler
- **Thymeleaf UI**: `/diary` list, `/diary/{id}` detail, `/diary/new` + `/diary/{id}/edit` form
- **JS**: `static/js/diary-form.js` — dynamic add/remove cviků a setů, auto-renumber, XOR autoclear
- **Dashboard update**: ukazuje 3 nejnovější tréninky
- **8 ownership testů** — User1 nemůže R/W trénink User2

### Tests
```
15/15 zelených (7 account-flow + 8 ownership)
```

### Hotfix během vývoje
- `#fields.hasGlobalErrors()` byl mimo `<form th:object>` → 500 na GET /diary/new
- Oprava: přesunout dovnitř form elementu

### Známé limity Fáze 1
- Trénink může mít jen typ FREEFORM (specifické typy EMOM/Circuit/Tabata přijdou v Phase 3)
- Žádné statistiky (Phase 5)
- Žádný group plán (Phase 2)
- Žádný trenérský přístup k cizím tréninkům (Phase 2)
- Žádná integrace s rezervacemi (Phase 7)

---

## ⏳ Fáze 2 — Trenér + GroupLessonPlan (NEXT)

**Cíl:** trenér uvidí všechny klienty a jejich deníky. Zveřejní plán skupinových lekcí. Klient
vidí plán ±1 týden. Trenér komentuje konkrétní trénink klienta.

### Scope

#### A) Admin sekce
- `/admin/accounts` — seznam klientů (table view: jméno, email, telefon, count tréninků, poslední aktivita)
- `/admin/accounts/{id}` — detail klienta + jeho deník (read-only view tréninků)
- `/admin/accounts/new` — založit nový účet (USER nebo ADMIN; jen role ADMIN smí zakládat další ADMINy)
- `/admin/accounts/{id}/edit` — editace profilu
- `/admin/accounts/{id}/delete` — smazat účet (cascade smaže jeho tréninky)
- Admin bypass v `TrainingService` — `getAnyTraining(admin, id)` ignoruje owner check

#### B) `GroupLessonPlan` entita + UI
- **V5 migrace**: `group_lesson_plan` (date, start_time, end_time, lesson_name, coach_id FK,
  capacity, description, reservation_link?)
- **V6 migrace** (nepovinné, pokud chceš recurring): `parent_group_lesson_plan_id` pro řadu opakovaných
- **Admin UI** `/admin/lessons` — CRUD na plán lekcí (kalendář pohled)
- **Klient UI** `/lessons` (nebo na dashboardu) — read-only kalendář, filter
  `WHERE date BETWEEN now()-7d AND now()+7d`
- **FullCalendar.io** pro kalendář (CDN nebo webjar)

#### C) Trenérovy komentáře k tréninku klienta
- **V7 migrace**: `training_comment` (training_id FK, author_id FK na account, text, created_at)
- Pod `/diary/{id}` detailem zobrazit komentáře + form pro přidání (jen pro admina nebo majitele)
- E-mail notifikace? — odložit do Phase 6

#### D) Tests
- Admin smí číst trénink kteréhokoliv klienta
- USER nesmí přistoupit na `/admin/**` (vrátí 403)
- USER nesmí komentovat cizí trénink

### Otevřené otázky pro Phase 2 (probrat s userem před začátkem)

1. **Recurring group lessons?** Trenér zveřejní lekci jednorázově, nebo jako šablonu (každé pondělí 18:00)?
2. **Kapacita lekce** — jen informativní, nebo má mít vazbu na rezervační systém?
3. **Trenér v komentářích vidí jen jméno klienta, nebo full email?** GDPR-friendly view?
4. **Smazání klienta** — soft delete (anonymizovat) nebo hard cascade?

---

## 📋 Fáze 3 — Rozšířené typy cviků (BUDOUCÍ)

**Cíl:** kromě FREEFORM přidat EMOM, Circuit, Tabata, AMRAP, Ladder, Stepladder, Pyramid,
Superset, Straight sets, Complex.

**Architektonické rozhodnutí (otevřené):** normalized tables per type (`emom_config`,
`circuit_config`, ...) nebo `type_config JSONB`? Pravděpodobně normalized, viz `architecture.md`.

**Per type:** vlastní Thymeleaf fragment, JS validace, výpočty (EMOM total reps =
totalMinutes × defaultReps; Tabata countdown logic).

**Pozn.:** trénink může mít smíchané typy (pyramid pro dřepy + AMRAP pro kliky) — model už podporuje.

---

## 🕒 Fáze 4 — Timer / stopky (BUDOUCÍ)

Pure-JS modul pro EMOM/Circuit/Tabata countdown. Wake Lock API
(`navigator.wakeLock.request('screen')`) ať telefon nezhasne. Modal/fullscreen z detail/edit form.

---

## 📊 Fáze 5 — Statistiky (BUDOUCÍ)

`TrainingAnalysisService`:
- `maxWeightForExercise(userId, catalogItemId, period)` — PR
- `totalVolume(userId, period)` — Σ weight × reps
- `setsPerBodyPart(userId, period)`, `setsPerMovementPattern(...)`
- `rpeTrend(userId, period)` — týdenní průměr
- `prHistory(userId, catalogItemId)` — vývoj PR v čase
- `frequencyHeatmap(userId, period)` — kalendářní heat-mapa (GitHub style)
- **+ navíc dle požadavku trenéra:**
  - Vývoj objemu kg/reps/sets pro vybraný cvik
  - Volume per difficulty (lehký/střední/těžký)
  - RPE rozložení v dnech: kalendářní heatmap + line chart (vedle sebe, user volí měsíc/rok)

Chart.js. Stránka `/analysis` s formulářem (metric × period × cvik).

---

## 📧 Fáze 6 — Email notifikace (BUDOUCÍ)

Spring Mail (Gmail SMTP nebo Mailgun přes env vars). `@Scheduled` cron každý večer pošle
upomínku klientům, kteří mají na zítra GroupLessonPlan. Per-account `email_notifications_enabled` flag
(už existuje na `account`).

---

## 🔗 Fáze 7 — Integrace s rezervacemi (BUDOUCÍ)

**Pravidla:**
- Diary konzumuje **REST API** rezervačního systému (https://github.com/ScoutMeto/ragnarok_customers_reservation_system)
- Rezervační systém NESAHÁT — je stabilní, plný dat
- Klient v profilu vidí svoje rezervace (GET), může vytvořit (POST), upravit/smazat (PUT/DELETE)
- Autorizace dle jména/příjmení/emailu — vynucuje rezervační systém
- `RestClient` bean v diary backendu, mezi-aplikační API key v env var

**Formulář pro novou rezervaci:**
- Pre-fill z profilu klienta: jméno, příjmení, email, telefon (volitelně)
- "Počet míst" default = 1, klient může změnit

---

## 📝 Fáze 8 — Individuální plány od trenéra (BUDOUCÍ)

**Dva souběžné mechanismy:**

### 8a) `TrainingTemplate` (strukturovaný)
- Trenér si vytvoří šablonu tréninku (cviky + sety jako u běžného tréninku)
- Přiřadí klientovi + datum → vytvoří se z toho instance `Training` (klient ji vidí jako "úkol")
- Klient může logovat výsledky proti šabloně

### 8b) `CoachPlan` (volný text)
- Tabulka: `coach_plan(id, client_id, author_id, title, valid_from, valid_to, body_markdown, ...)`
- Trenér napíše víceúrovňový plán na týdny/měsíce dopředu jako markdown
- Klient čte, ale loguje si sám strukturovaně (přes /diary/new)

---

## 🔄 Continuous tasks (běží napříč fázemi)

| Task | Kde |
|------|-----|
| Tests | `src/test/java/.../` — happy path per fáze |
| Migrations | `src/main/resources/db/migration/V_next__...sql` |
| Bezpečnost | CSRF na Thymeleaf, vypnuté na /api/**, BCrypt |
| Logging | SLF4J + Logback, **žádné System.out.println** |
| Validace | Jakarta Bean Validation na DTO |
| Deploy | Railway (po Phase 0 zatím lokálně) |

---

## 🚨 Open issues / blockers / TODO

- [ ] **Pushnout** `feature/phase-0-foundation` + `feature/phase-1-mvp-diary` na origin (kdy?)
- [ ] **Mergnout** Fázi 0 + 1 do master po review kolegou
- [ ] **Railway provision** — vytvořit Postgres service pro diary v Railway dashboardu (uživatel řekl "počká na první deploy")
- [ ] **Vyjasnit Phase 2 otázky** — recurring lessons, kapacita ↔ rezervace, soft vs hard delete
- [ ] **Aktualizovat Stav fází v této tabulce** po dokončení Phase 2

## 🔧 Známé hotfixy (history)

| Date | Fix | Commit |
|------|-----|--------|
| 2026-05-23 | Lombok `setIsSystem` → `setSystem` (boolean is-prefix) | `eedd7e6` |
| 2026-05-23 | `#fields.hasGlobalErrors()` přesunut dovnitř `<form th:object>` | `c9dbd94` |
| 2026-05-23 | Default DB heslo `Meto1990` → `postgres` | `256c705` |
| 2026-05-23 | UI scaling pro 4K monitor (font 17-20px, container 1600px) | `ddf9296` |
| 2026-05-23 | `spring.jpa.open-in-view=false` → `=true` (LazyInitException na detail.html) | – |
