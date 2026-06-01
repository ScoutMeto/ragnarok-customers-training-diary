# Development log

> **Stav fází + co je další.** Aktualizuj při dokončení každé fáze.
> Pro kontext projektu viz `CLAUDE.md`. Pro hlubší architekturní rozhodnutí viz `architecture.md`.

---

## 📊 Stav fází (k 2026-05-31)

| # | Fáze | Stav | Kde |
|---|------|------|-----|
| 0 | Foundation | ✅ DONE | mergnuto do `develop` |
| 1 | MVP diary | ✅ DONE | mergnuto do `develop` |
| 2 | Trenér + skupinové tréninky + komentáře | ✅ DONE | na `develop` |
| 3 | Rozšířené typy cviků (10 typů) | ✅ DONE | na `develop` |
| 4 | Timer / stopky | ✅ DONE | na `develop` |
| 5 | Statistiky (klient + admin overview) | ✅ DONE | na `develop` |
| 8 | Individuální plány od trenéra (TrainingTemplate + CoachPlan) | ✅ DONE | na `develop` |
| R | UI redesign (Forge design system) | ✅ DONE | na `develop` |
| 6 | Email confirmation + notifikace | ✅ DONE | na `develop` |
| 7.1 | Integrace s rezervacemi — kalendář + rezervace 1 klikem | ✅ DONE | na `develop` |
| 7.2 | Rezervace: cancel + moje rezervace + historie | ⏳ TODO (vyžaduje admin přístup do rez. systému) | — |
| **9** | **Quick wins (ScoutMeto feedback): difficulty 3 úrovně × 9 labelů, nové tagy, smazat CUSTOMIZING, šablona save bug, gym overview cleanup** | ⏳ **IN PROGRESS** | na `develop` |
| 10 | Inactive účty (read-only mód pro neplatiče) | ✅ DONE | na `develop` |
| 11 | Per-exercise tagy + equipment override + CARDIO/CORE typy | ⏳ TODO | — |
| 12 | Refactor typů: set tabulka jen FREEFORM, composite bez limitu, circuit per-round záznam | ⏳ TODO | — |
| 13 | Nové typy: StrongFirst ladder, Interval, KB sport time | ⏳ TODO | — |
| 14 | Korunka (favorite) + flag (per-trénink) | ✅ DONE (filter v deníku → Phase 15) | na `develop` |
| 15 | Rozšířené statistiky (objem per tag/cvik/série/reps, nejvyšší reps) | ⏳ TODO | — |
| 16 | Audit copy-to-private flow (skupinové → osobní) | ⏳ TODO | — |
| 17 | Správa katalogu cviků (CRUD + per-user + system) | ✅ DONE (popisy 92 cviků → 17c TODO) | na `develop` |

**Branching strategie (od Fáze 2):**
- `master` = stable releases (zatím prázdné, mergne se ze `develop` ručně při stabilizaci)
- `develop` = veškerý vývoj
- Pro Phase N **nevytvářet** separátní feature branch — commity rovnou na `develop`
- Hotfix nebo rizikový experiment může mít krátkodobou `feature/...` branch a merge zpět

**Branch status:** `develop` má Phase 0 + Phase 1 mergnuté přes `--no-ff` merge commits.
Lokálně. **Nepushnuto na origin.** (Pushnout můžeme na žádost.)

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

## ✅ Fáze 2 — Trenér + skupinové tréninky + komentáře (DONE)

**Branch:** přímo na `develop` (8 + 5 fix commitů = 13)
**Cíl:** trenér uvidí všechny klienty a jejich deníky, vytváří **skupinové tréninky**
(strukturované, s cviky+sety, jako klientovy vlastní) viditelné všem klientům pro daný den.
Komentáře pod detailem tréninku.

### ⚠️ Mid-phase pivot

Původně byla Fáze 2 navrhnutá s `GroupLessonPlan` (samostatná entita s datum+čas+název+popis).
Po implementaci uživatel upřesnil: "My jsme chtěli skupinové tréninky které by Admin vytvořil
na konkrétní den a tenhle trénink by se všem klientům zobrazoval v deníku nebo na dashboardu
pro daný den. Plán lekcí ani tvorbu a management skupinových lekcí tady nechceme."

→ Smazán `lesson/` balíček + `/admin/lessons` + `/lessons`. Místo toho rozšířena `Training`
entita o `visibility` (PRIVATE/GROUP) a admin tvoří group tréninky přes podobný formulář
jako klient. Detaily: viz `architecture.md` sekce "Group training visibility".

### Co bylo dodáno

- **V5 migrace** — `account.deleted_at`, `training_comment` (group_lesson_plan z V5 zrušen v V6)
- **V6 migrace** — `training.visibility` PRIVATE/GROUP, `training.created_by_id`,
  owner nullable, CHECK constraints, partial index pro group dotazy
- **JPA entity**:
  - `TrainingEntity` — visibility enum, owner nullable, createdBy FK
  - `TrainingVisibility` enum (PRIVATE, GROUP)
  - `TrainingCommentEntity` — komentáře
  - Soft-delete na `AccountEntity` (`deletedAt` field, `isEnabled()` false když smazán)
- **Service vrstva**:
  - `TrainingService.createGroup/updateGroup/deleteGroup/listGroupTrainingsForDay/listAllGroupTrainings`
  - `TrainingService.getAnyTraining` + `listTrainingsOf` — admin bypass
  - `TrainingCommentService` — komentáře (PRIVATE owner/admin; GROUP kdokoliv)
  - `AccountService.softDelete` — anonymizace email/jméno/telefon
- **Admin sekce**:
  - `/admin/accounts` — list, detail (s deníkem klienta), create, edit, soft-delete
  - `/admin/group-trainings` — CRUD pro skupinové tréninky (vlastní form, redirect na /diary/{id})
- **Klient view**:
  - `/diary` — sekce "Dnes v gymu" (group tréninky pro dnešek) + "Moje tréninky"
  - `/diary/{id}` — funguje pro PRIVATE i GROUP (read-only pro group klientovi)
- **Komentáře** pod detailem tréninku — zobrazují jméno + email autora
- **Dashboard**: karty + sekce "Dnes v gymu"
- **17 Phase 2 testů** (9 features + 8 group training)

### Klíčová rozhodnutí (z odpovědí trenéra)

- Skupinové tréninky **strukturované** (cviky + sety jako klientovy vlastní)
- **Víc group tréninků na den** povoleno (ranní KB + odpolední cardio)
- Klient + group **vedle sebe** (samostatné záznamy, žádná template/log vazba)
- Group má jen **systémové tagy** (custom patří userům)
- V komentářích zobrazujeme **jméno + email autora**
- **Soft delete** s anonymizací (email se přepíše na `deleted-{id}-{uuid}@deleted.local`)
- Bootstrap admin login používá soft-delete-aware lookup (`findByEmailAndDeletedAtIsNull`)
- Group tréninky nemají owner; PRIVATE musí mít owner (DB CHECK constraint)
- Po create/edit group tréninku redirect na `/diary/{id}` — detail je sdílený

### Stav testů
```
32/32 zelených (7 + 8 + 9 + 8)
```

---

## ✅ Fáze 3 — Rozšířené typy cviků (DONE)

**Cíl:** kromě FREEFORM přidat 10 typů cvičení s vlastní strukturou.

**Architektonické rozhodnutí:** **normalizované tabulky per type** (1:1 s `training_exercise`
přes shared PK MapsId). Sdílené tabulky pro skupiny podobných typů (NumericSeries pro
Ladder/Stepladder/Pyramid; CompositeSet pro Superset/Complex).

### Co bylo dodáno

**5 commitů, 5 migrací (V7–V11):**
- **V7** — EMOM, Tabata, AMRAP (`emom_config` + `emom_minute_override`, analogicky Tabata,
  AMRAP s plánem i výsledkem v jedné entitě)
- **V8** — Circuit (`circuit_config` + `circuit_step` + `circuit_round_rest`)
- **V9** — Ladder/Stepladder/Pyramid (sdílená `numeric_series_config` se start/peak/step
  nebo CSV override)
- **V10** — Superset/Complex (sdílená `composite_set_config` + `composite_set_step`,
  Complex má `shared_weight_kg`)
- **V11** — Straight Sets (3×8, 5×5 atd.)

**Java:**
- `training/types/{emom,tabata,amrap,circuit,series,composite,straight}/` balíčky
- `ExerciseTypeConfigMapper` (apply DTO → entity, dispatch dle type)
- `ExerciseTypeConfigToInputMapper` (opačně pro edit form)
- TrainingExerciseEntity má 7 nových `@OneToOne` configs (cascade ALL + orphanRemoval)

**UI:**
- Dropdown typů ve formuláři (klient + admin group)
- Per-type `.type-config.type-XYZ` sekce v form.html, JS přepíná viditelnost
- 6–8 fixních řádků pro nested steps (Circuit, Composite) — prázdné se filtrují backend
- Detail render per-type (info card s parametry + steps table)

**Bonus:**
- `POST /diary/{id}/copy` — klient zkopíruje GROUP trénink jako svůj PRIVATE s prázdnými
  sety (šablona pro zaznamenání vlastních výkonů). Tlačítko „📥 Zkopírovat do mého deníku"
  na detail group tréninku.

---

## ✅ Fáze 4 — Timer / stopky (DONE)

**Cíl:** real-time průvodce tréninkem pro mobilní telefon.

### Co bylo dodáno

- **`static/js/timer.js`** — `TrainingTimer.runEmom/runTabata/runCircuit/runAmrap`
- **Web Audio API beep** při přechodu fází (short 880Hz, long 660Hz)
- **navigator.wakeLock.request('screen')** — telefon nezhasne
- **Fullscreen modal** s velkým mm:ss displejem (12rem, mobile 7rem)
- **Mute toggle** (per-session)
- **Pauza/Pokračovat/Zavřít** tlačítka
- Per-typ stavový stroj:
  - EMOM: countdown v aktuální minutě, beep při nové minutě, optional minute override
  - Tabata: cyklus work/rest × R kol, displej PRÁCE vs Pauza
  - AMRAP: countdown od timecapu, beep v posledních 10 s
  - Circuit: generuje sekvenci [work, rest, work, rest, ..., roundRest] dle steps[]
- Integrace v `diary/detail.html`: tlačítko „⏱ Spustit" u každého cviku s validní config

---

## ✅ Fáze 5 — Statistiky (DONE)

**Cíl:** data, kvůli kterým si klient appku oblíbí + admin overview.

### Co bylo dodáno

**`AnalysisService`** (JPQL agregace přímo na entity):
- `totalVolumeByDay` — SUM(weight × reps) per den
- `maxWeightForExercise` — PR pro daný cvik
- `prHistory` — vývoj max-weight v čase
- `setsPerBodyRegion` / `setsPerMovementPattern` — bar chart distribuce
- `rpeTrend` — průměrné RPE per den
- `frequencyHeatmap` — počet tréninků per den (GitHub-style heatmap)
- `volumePerDifficulty` — objem podle LIGHT/MEDIUM/HARD
- `rpePerDay` — RPE per den pro kalendářní heatmap
- `adminOverview` — gym-wide: aktivní klienti, počet tréninků (private/group),
  total volume, top 10 nejaktivnějších klientů

**REST API `/api/analysis/*`** — 9 endpointů + `/admin/overview` (ROLE_ADMIN guard)

**Klient UI `/analysis`:**
- Period filter (7/30/90/365 dní + vlastní rozsah)
- Chart.js (z CDN): 6 grafů (line + bar) + HTML heatmap
- Tooltip na heatmap cell ukazuje datum + počet + RPE

**Admin UI `/admin/overview`:**
- 4 KPI karty: aktivní klienti, osobní tréninky, skupinové, total volume
- Top 10 aktivních klientů (tabulka)

**Pravidla pro počítání:**
- Statistiky **pouze z PRIVATE** tréninků daného klienta
- Pokud klient absolvoval GROUP a chce ho započítat, použije Phase 3.5 „Zkopírovat do
  mého deníku" → vytvoří se PRIVATE kopie

### Stav testů
```
37/37 zelených (7 + 8 + 9 + 8 + 5 nových AnalysisSmokeTest)
```

---

## ✅ Fáze 8 — Individuální plány od trenéra (DONE, 2026-05-24)

**Cíl:** trenér umí klientovi předat strukturovaný trénink (template) *i* volný textový
plán (coach plan). Fáze 6 (email) a 7 (rezervace) odloženy na později — user explicitně řekl
"6 7 bych nechal nakonec a udělal Phase 8".

### Co bylo dodáno

**P8.1 — V12 migrace + entity**
- `V12__phase8_templates_and_coach_plans.sql`:
  - `training.visibility` CHECK constraint rozšířen o `TEMPLATE`
  - `training_visibility_owner_check`: TEMPLATE smí mít owner=NULL (jako GROUP)
  - `training.source_template_id BIGINT FK → training(id) ON DELETE SET NULL` + partial index
  - Tabulka `coach_plan(id, client_id, author_id, title, body_markdown TEXT, valid_from, valid_to, ...)`
- Enum `TrainingVisibility` rozšířen: `PRIVATE / GROUP / TEMPLATE`
- `TrainingEntity.sourceTemplate` (self-FK na šablonu, z níž byl trénink přiřazen)
- `CoachPlanEntity` + `CoachPlanRepository` s JPQL `findActiveForClient`

**P8.2 — Services + markdown**
- `commonmark-java 0.24.0` v pom.xml
- `MarkdownRenderer` (escapeHtml=true → XSS safe; raw HTML se escapuje)
- `CoachPlanService` — CRUD + `getForClientOrAdmin` (klient čte jen své, admin všechno)
- `TrainingService` rozšířen o template ops:
  - `listAllTemplates / getTemplate / createTemplate / updateTemplate / deleteTemplate`
  - `assignTemplateToClient(templateId, client, date, toInputMapper)` →
    nový PRIVATE trénink jako kopie šablony se `sourceTemplate` FK; klient si pak
    doplní vlastní váhy / opakování / RPE / poznámky
  - `listTrainingsFromTemplate(templateId)` — admin view, kdo dostal šablonu

**P8.3 — Admin Training Templates UI**
- `AdminTrainingTemplateController` (`/admin/training-templates`):
  - list + new + edit + delete + assign (vybrat klienta + datum)
- Templates: `admin/templates/{list,form,assign}.html`
- Navbar: nový admin dropdown link "Šablony tréninků"

**P8.4 — Admin Coach Plans UI + klient view**
- `AdminCoachPlanController` (`/admin/accounts/{clientId}/coach-plans`):
  - list + new + edit + show + delete; markdown editor + rendered preview přes `MarkdownRenderer`
- `MyPlanPageController` (`/my-plan`, `/my-plan/{id}`):
  - Klient vidí svůj aktuálně platný plán + historii starších; detail s vyrenderovaným markdown
- Templates: `admin/coach-plans/{list,form,show}.html`, `coach/{my-plan,plan-detail}.html`
- Navbar: "Můj plán" pro `!hasRole('ADMIN')`, tlačítko "📋 Plány od trenéra" v `admin/accounts/detail.html`

**P8.5 — Testy + dokumentace**
- Nový `Phase8FeaturesTest` (14 testů): template CRUD, assign → sourceTemplate FK,
  CoachPlan CRUD, ordering, findActive, cross-client forbidden, MarkdownRenderer (basic + XSS escape + null)
- **Test sumář: 51/51 zelených** (7 + 8 + 9 + 14 + 8 + 5)
- CLAUDE.md a development-log.md aktualizovány

### Klíčová rozhodnutí

- **Templates používají `TrainingEntity` s `visibility=TEMPLATE`**, ne separátní tabulku —
  share schema pro cviky/sety/per-type configs, statistiky to ignorují (counts jen PRIVATE)
- **Šablona ↔ instance přes `source_template_id` FK** — ON DELETE SET NULL (smazaná šablona
  netraumatizuje historii klientů)
- **Při přiřazení šablony se kopírují JEN systémové tagy** — custom tagy patří někomu jinému
- **CoachPlan = markdown body** (commonmark-java + escapeHtml) — žádný strukturovaný plánovač,
  flexibilita víc než struktura
- **Klient může mít víc plánů (historie)**, "aktivní" = ten, jehož `valid_from..valid_to` pokrývá today

---

## ⏳ Fáze 6 — Email notifikace (DEFERRED)

Spring Mail (Gmail SMTP nebo Mailgun přes env vars). `@Scheduled` cron každý večer pošle
upomínku klientům, kteří mají na zítra skupinový trénink. Per-account
`email_notifications_enabled` flag (už existuje na `account` od Phase 2).

---

## 🔗 Fáze 7 — Integrace s rezervacemi (DEFERRED)

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
| 2026-05-23 | Phase 2 pivot: GroupLessonPlan → Training.visibility (PRIVATE/GROUP) | `8928cde`→`975bcb5` |
