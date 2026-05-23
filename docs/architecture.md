# Architecture — Ragnarok Customers Training Diary

> Hlubší pohled na klíčová architektonická rozhodnutí. Pro rychlý kontext viz `CLAUDE.md`.
> Pro stav fází viz `docs/development-log.md`.

---

## 1. System context

Diary je **jedna ze dvou** aplikací pro tělocvičnu Ragnarok:

```
┌─────────────────────────────┐         ┌─────────────────────────────────┐
│  ragnarok-customers-        │         │  ragnarok_customers_             │
│  training-diary             │ ──REST──▶  reservation_system             │
│  (THIS PROJECT)             │         │  (https://github.com/ScoutMeto) │
│                             │         │                                  │
│  • Klient loguje tréninky   │         │  • Klient se rezervuje na lekce │
│  • Trenér vidí klienty      │         │  • Stabilní + plný dat           │
│  • Statistiky, plán lekcí   │         │  • NEDOTÝKÁME SE                 │
└─────────────────────────────┘         └─────────────────────────────────┘
        │                                          ▲
        │ samostatná PostgreSQL DB                 │ samostatná PostgreSQL DB
        ▼                                          │
┌─────────────────┐                          ┌─────────────────┐
│ diary DB        │                          │ reservation DB  │
│ (ragnarok_      │                          │ (existující)    │
│  diary_database)│                          │                 │
└─────────────────┘                          └─────────────────┘
```

**Diary konzumuje rezervační API přes REST** (Fáze 7). Žádné sdílené DB.
Žádný shared session/SSO ve Fázi 1 — to lze přidat později, pokud bude chuť.

---

## 2. Auth & authorization

### Sjednocený `account` model

**Rozhodnutí:** jedna tabulka `account` s rolí enum (USER, ADMIN) místo dvou oddělených tabulek.

**Důvody:**
- Původní kód měl `UserEntity` + `AdminEntity` se duplicitními poli a duplicitní auth chain
- Vedlo to ke konfliktům beanů (`authenticationManager` bean × 2)
- Copy-paste bugům (`UserServiceImpl.getCurrentUserInfo` přetypovával na `AdminEntity`)
- Jedna tabulka, jeden filter chain, role-based authorize je standard

**Trade-off:** kdyby v budoucnu admin měl výrazně víc atributů než user (např. trenérská
certifikace, hodinová sazba), můžeme:
- Buď nechat všechno v `account` a nullable
- Nebo udělat `coach_profile` tabulku 1:1 s `account` (kde role=ADMIN)

### Single `SecurityFilterChain`

Místo dvou (admin chain priority 1 + user chain priority 2):
```
SecurityConfiguration
└─ SecurityFilterChain {
     /, /login, /register, statika, /api/auth/register  → permitAll
     /admin/**, /api/admin/**                          → ROLE_ADMIN
     vše ostatní                                       → authenticated
   }
```

Spring Security autodetekuje `UserDetailsService` + `PasswordEncoder` bean a vyrobí
`AuthenticationManager` sám. **Žádný custom `DaoAuthenticationProvider` bean** —
předtím způsoboval `InitializeUserDetailsManagerConfigurer` warning.

### CSRF

- Zapnutý pro Thymeleaf formuláře (auto-injektovaný token přes `th:action`)
- Vypnutý pro `/api/**` (REST API volá JS přes fetch, používá session cookie)

---

## 3. Domain model

```
┌──────────────────┐
│ account          │
│  id (PK)         │
│  email (UQ)      │
│  role            │
│  ...             │
└────────┬─────────┘
         │ owner
         │ 1:N (cascade delete)
         ▼
┌──────────────────────────────────┐
│ training                         │
│  id (PK)                         │
│  owner_id → account              │
│  training_date, start, end       │
│  name, difficulty enum, rpe, notes│
└────────┬─────────────────────────┘
         │ 1:N (cascade)               M:N
         ▼                              ┌─────────────────┐
┌──────────────────────────────┐ ◄─────│ training_tag    │
│ training_exercise            │       │  id (PK)        │
│  id (PK)                     │       │  name, color    │
│  training_id                 │       │  is_system      │
│  order_index, type           │       │  owner_id?      │
│  catalog_item_id ───┐ XOR    │       └─────────────────┘
│  custom_name ───────┘        │
│  rpe, notes                  │
└────────┬─────────────────────┘
         │ 1:N (cascade)
         ▼
┌──────────────────────────────┐
│ exercise_set                 │
│  id (PK)                     │
│  training_exercise_id        │
│  set_index                   │
│  weight_kg, reps, rpe, note  │  ← všechno nullable
└──────────────────────────────┘

┌──────────────────────────────┐
│ exercise_catalog_item        │  ← system seed V3 (92 cviků) + Phase 2 custom by admin
│  id (PK)                     │
│  name, body_region,          │
│  movement_pattern, equipment │
│  primary_muscle, description │
│  is_system, created_by, active│
└──────────────────────────────┘
```

### Klíčová designová rozhodnutí

#### 1. **Trénink = 1 osoba** (1:1, ne M:N)
Klient si loguje **svoje** tréninky. Trenér uvidí cizí jen v admin sekci (Phase 2 — read přes
`getAnyTraining` bypass). M:N (skupinová lekce přiřazená víc klientům) jsme zamítli — v praxi
každý klient stejně provede mírně jinak.

#### 2. **Skupinový plán je SEPARATE entita** (`GroupLessonPlan`, Phase 2)
Trenér zveřejní plán lekcí (název, čas, kapacita) — klienti vidí jen jako kalendář ±1 týden.
**Není to `Training`** s flagem "public". Důvody:
- Group plan má jiný životní cyklus (rolling, opakující se týdenně)
- Cliento `Training` má jiné pole (RPE, sety...) které u group plánu nedávají smysl
- Jednodušší filter "viditelný do týdne dozadu"

#### 3. **XOR `catalog_item_id` vs `custom_name`**
Cvik referuje katalog **nebo** má vlastní název, **ne obojí**. Vynuceno:
- DB constraintem `exercise_name_xor`
- Service validací (`TrainingInput.isNamingValid()`)
- JS automaticky maže opačné pole, když user vyplní jedno (UX)

Důvod: globální katalog umožní statistiky napříč klienty ("kolik lidí dělá KB swing"). Custom
name dovolí klientovi přidat exotický cvik bez čekání na admin updates.

#### 4. **Per-set logging s nullable poli**
Set má `weight_kg`, `reps`, `rpe`, `note` — **všechno nullable**. Klient zaznamená jen to, co chce:
- KB swing: jen reps + možná RPE
- Bodyweight push-up: jen reps
- Strict press: weight + reps + RPE
- Plank: jen note "60s hold"

Empty sets (všechna pole null) jsou silently dropnuty na save (viz `TrainingService.isSetEmpty`).
Klient může do formuláře zadat 5 prázdných řádků, save je odfiltruje.

#### 5. **RPE na 3 úrovních (trénink / cvik / set)**
- **Trénink:** subjektivní pocit z celé lekce
- **Cvik:** intenzita jednoho cviku napříč sety
- **Set:** intenzita konkrétní série (Phase 2 statistics: RPE trend per exercise)

Vše nullable, 1–10.

#### 6. **Difficulty (LIGHT/MEDIUM/HARD) jen na tréninku**
Klient si značí sám PO tréninku — subjektivní. Použito pro statistiky "kg nazvedaných v lehkých
vs těžkých trénincích". Trenér NENASTAVUJE.

#### 7. **Tagy: system + per-user custom**
System tagy (KB, CARDIO, BODYWEIGHT, OS Resets, ...) jsou v V4 seed migraci, viditelné všem.
Custom tagy patří uživateli (`owner_id NOT NULL`), nikdo jiný je nevidí.

DB constraint `tag_system_xor_owner` vynucuje vzájemnou exklusi.

#### 8. **`TrainingExerciseType` enum jako VARCHAR**
V DB jako string (`VARCHAR(32)`), v Javě jako enum. Přidávání nových typů (Phase 3: Tabata,
AMRAP, Ladder, ...) **nepotřebuje DB migraci** — jen rozšíření enumu.

---

## 4. Service vrstva — ownership pattern

```java
public TrainingEntity getMyTraining(AccountEntity owner, Long trainingId) {
    return trainingRepository.findByIdAndOwner_Id(trainingId, owner.getId())
            .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));
}
```

**Princip:** každá metoda, která čte nebo modifikuje training, dostane `AccountEntity owner` a
filtruje na `owner_id`. Pokud trénink neexistuje **nebo** patří jinému uživateli, vyhodí
`NotFoundException` — záměrně **ne** `ForbiddenException`, abychom neprozrazovali existenci
cizích záznamů.

Test `TrainingOwnershipTest` ověřuje:
- alice nemůže číst Bobův trénink (NotFound, ne 403)
- alice nemůže editovat Bobův trénink
- alice nemůže smazat Bobův trénink
- `listMyTrainings(alice)` vrátí jen alicin

**Admin bypass přijde v Phase 2** — `TrainingService.getAnyTraining(admin, trainingId)`
nebo separátní `AdminTrainingService`.

---

## 5. REST API vs Thymeleaf strategie

**Fáze 1:**
- **Thymeleaf** řídí trénink CRUD (`/diary/**`)
- **REST** existuje jen pro lookup (`/api/exercise-catalog`, `/api/tags`, `/api/auth/*`)

**Důvod:** server-side rendering je rychlejší pro vývoj jednoduchého UI, méně JS bagážu, snadné
pro Marka (vývojář s méně JS zkušenostmi). REST endpointy pro celý training CRUD přidáme **v Phase 7**,
když budeme integrovat s rezervačním systémem (nebo dříve, kdyby přišel požadavek na mobilní klient).

**Globální REST error handler** (`GlobalRestExceptionHandler`) mapuje výjimky na strukturovaný JSON.
Thymeleaf controllery používají `RedirectAttributes flashError/flashSuccess` + `BindingResult`.

---

## 6. Frontend: Bootstrap 5 + minimal JS

- **Žádný React/Vue** — Thymeleaf + jQuery-free vanilla JS
- **Bootstrap 5 přes webjar** — verze v `pom.xml`, ne CDN
- **Custom CSS** v `static/css/app.css` — scaling pro 4K monitory
- **Dynamic form** (přidat/odebrat cvik/set): `static/js/diary-form.js` — template element cloning,
  event delegation, auto-renumber pro Spring form-binding

**Pro Phase 4 (timer):** zvážíme malý vanilla JS countdown modul. Pravděpodobně bez framework.
**Pro Phase 5 (statistiky):** Chart.js (jednoduchá, lightweight, dobrá dokumentace).

---

## 7. Phase plán — high-level

| # | Fáze | Hlavní přínos | Klíčová entita |
|---|------|---------------|----------------|
| 0 | Foundation | Login, register, role | `Account` |
| 1 | MVP diary | Klient loguje | `Training` + `TrainingExercise` + `ExerciseSet` |
| 2 | Trenér + GroupLessonPlan | Admin sekce, plán lekcí, komentáře | `GroupLessonPlan` + `TrainingComment` |
| 3 | Rozšířené typy cviků | EMOM, Circuit, Tabata, ... | `EmomConfig`, `CircuitConfig`, ... (nebo JSON? rozhodnout v P3) |
| 4 | Timer / stopky | UX pro mobilní telefon během tréninku | (JS only) |
| 5 | Statistiky | Hlavní hodnota — proč si appku oblíbí | (read-only agregace) |
| 6 | Email notifikace | Připomenutí lekcí | Spring Mail + @Scheduled |
| 7 | Integrace s rezervacemi | Klient rezervuje z profilu | RestClient k second app |
| 8 | Individuální plány od trenéra | Template + textový plán | `TrainingTemplate` + `CoachPlan` |

Detailní rozpis viz `docs/development-log.md`.

---

## 8. Open architectural questions (rezerva na pozdější rozhodnutí)

### Phase 3: Strategy pattern vs JSON pro typy cviků
Pro EMOM, Circuit atd. potřebujeme různá data per type. Možnosti:
- **A) Normalized**: `emom_config`, `circuit_config`, ... tabulky 1:1 s `training_exercise`
- **B) JSONB**: jeden sloupec `type_config JSONB` v `training_exercise`

Trade-off: A je type-safe, B je flexibilní. Pravděpodobně **A**, je tu málo typů (8-10).

### Phase 5: Materialized views vs ad-hoc queries
Statistiky budou prvotřídně počítány JPQL. Pokud bude pomalé, zvážíme materialized views
(`mv_user_volume_per_week`).

### Phase 7: SSO mezi diary a reservation
Aktuálně: dva separátní login. Možná v budoucnu shared JWT / OAuth2 — ale **NE V PHASE 7**,
to je out of scope; jen REST call mezi appkami.

### Mobile app
Aktuálně responsive web. PWA (installable) → low-cost upgrade kdykoliv. Native app pravděpodobně
nikdy (single-tenant, malý gym).
