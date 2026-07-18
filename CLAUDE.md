# CLAUDE.md — Project context for AI assistant

> **Tento soubor čti jako první při každé nové session.** Jsou v něm všechny klíčové
> informace o projektu, abys nemusel/nemusela číst desítky souborů pro pochopení kontextu.

---

## 1. Projekt v jedné větě

**Tréninkový deník** pro klienty (~jednoho gymu) Ragnarok — klient si loguje vlastní tréninky
(cviky, sety, RPE), vidí týdenní program lekcí od trenéra, prohlíží statistiky a propojuje se
s **samostatným** [rezervačním systémem](https://github.com/ScoutMeto/ragnarok_customers_reservation_system).

- **Single-tenant** (1 tělocvična, ~2 trenéři, N klientů)
- **Volné tempo** vývoje, žádná deadline
- **User je česky mluvící** (komentáře v kódu i odpovědi jsou česky)
- **Provoz na Railway** (po Fázi 0 zatím lokálně)

## 2. Tech stack

| Vrstva | Volba |
|--------|-------|
| Java | **21 LTS** |
| Framework | **Spring Boot 3.4.4** |
| DB | **PostgreSQL 17** (lokálně + Railway) |
| Schema mgmt | **Flyway** (V1, V2, V3, V4 ...) — NEpoužíváme `ddl-auto=update` v produkci |
| ORM | JPA / Hibernate (ddl-auto=validate v produkci, create-drop v testech) |
| Frontend | **Thymeleaf + Bootstrap 5** (server-rendered, žádný React) |
| Security | Spring Security session-based, BCrypt, **jeden filter chain**, role-based |
| Testy | JUnit 5, MockMvc, H2 in-memory (profile `test`) |
| DTO mapping | MapStruct (zatím málo použité) |
| Build | Maven (mvnw wrapper) |

## 3. Struktura balíčků (feature-based)

```
com.ragnarok.ragnarok_customers_training_diary/
├── RagnarokCustomersTrainingDiaryApplication.java     # main
├── account/                # Sjednocený User+Admin model (role enum + soft delete)
│   ├── AccountEntity.java          # implements UserDetails
│   ├── AccountRole.java            # USER, ADMIN
│   ├── AccountRepository.java
│   ├── AccountService.java         # registrace
│   ├── AccountUserDetailsService.java
│   ├── AccountRestController.java  # /api/auth/*
│   ├── EmailAlreadyTakenException.java
│   └── dto/                # RegistrationRequest, AccountResponse (records)
├── catalog/                # Globální katalog cviků
│   ├── ExerciseCatalogItemEntity.java
│   ├── ExerciseCatalogItemRepository.java
│   ├── ExerciseCatalogService.java
│   ├── ExerciseCatalogRestController.java   # /api/exercise-catalog
│   ├── BodyRegion.java, MovementPattern.java, Equipment.java   # enums
├── tag/                    # Tagy (system + per-user custom)
│   ├── TrainingTagEntity.java
│   ├── TrainingTagRepository.java
│   ├── TrainingTagService.java
│   └── TrainingTagRestController.java       # /api/tags
├── training/               # Tréninkový deník (PRIVATE + GROUP + TEMPLATE)
│   ├── TrainingEntity.java         # visibility=PRIVATE/GROUP/TEMPLATE + sourceTemplate FK
│   ├── TrainingExerciseEntity.java # cvik v tréninku (XOR catalog_item / custom_name)
│   │                               # + OneToOne per-type configs (emom/tabata/amrap/circuit/...)
│   ├── ExerciseSetEntity.java      # set cviku (weight, reps, RPE, note — vše nullable)
│   ├── TrainingDifficulty.java     # enum LIGHT/MEDIUM/HARD
│   ├── TrainingVisibility.java     # enum PRIVATE/GROUP/TEMPLATE
│   ├── TrainingExerciseType.java   # FREEFORM/CUSTOMIZING/EMOM/TABATA/AMRAP/CIRCUIT/
│   │                               # LADDER/STEPLADDER/PYRAMID/SUPERSET/COMPLEX/STRAIGHT_SETS
│   ├── TrainingCommentEntity.java  # komentář (autor klient nebo admin)
│   ├── *Repository.java
│   ├── TrainingService.java        # business logic + ownership check + group ops + copyGroupToPrivate
│   ├── TrainingCommentService.java
│   ├── DiaryPageController.java    # /diary/** (Thymeleaf, klient + group view + copy)
│   ├── dto/                # TrainingInput, TrainingExerciseInput, SetInput, per-type configy
│   └── types/                      # Per-type configy (Fáze 3)
│       ├── ExerciseTypeConfigMapper.java  # apply DTO -> entity (dispatch dle type)
│       ├── ExerciseTypeConfigToInputMapper.java  # opačně pro edit form
│       ├── emom/    EmomConfig + EmomMinuteOverride
│       ├── tabata/  TabataConfig + TabataRoundOverride
│       ├── amrap/   AmrapConfig (cíl + výsledek v jedné entitě)
│       ├── circuit/ CircuitConfig + CircuitStep + CircuitRoundRest
│       ├── series/  NumericSeriesConfig (sdílený pro LADDER/STEPLADDER/PYRAMID)
│       ├── composite/ CompositeSetConfig + CompositeSetStep (sdílený pro SUPERSET/COMPLEX)
│       └── straight/ StraightSetsConfig
├── analysis/               # Statistiky (Fáze 5)
│   ├── AnalysisService.java        # JPQL agregace (volume, PR, RPE, body region, ...)
│   ├── AnalysisRestController.java # /api/analysis/** (JSON pro Chart.js)
│   └── AnalysisPageController.java # /analysis (klient) - + admin/overview je v admin/
├── admin/                  # Admin sekce (jen ROLE_ADMIN)
│   ├── AdminAccountController.java         # /admin/accounts CRUD (soft-delete)
│   ├── AdminAccountForm.java
│   ├── AdminGroupTrainingController.java   # /admin/group-trainings CRUD
│   ├── AdminTrainingTemplateController.java # /admin/training-templates CRUD + assign (Phase 8)
│   ├── AdminCoachPlanController.java       # /admin/accounts/{id}/coach-plans CRUD (Phase 8)
│   └── AdminOverviewController.java        # /admin/overview gym-wide statistiky
├── coach/                  # Coach plány (Phase 8)
│   ├── CoachPlanEntity.java        # markdown plan: client_id, author_id, title, body, valid_from/to
│   ├── CoachPlanRepository.java    # + findActiveForClient JPQL
│   ├── CoachPlanService.java       # CRUD + getForClientOrAdmin autorizace
│   ├── MarkdownRenderer.java       # commonmark-java wrapper (escapeHtml=true)
│   └── MyPlanPageController.java   # /my-plan + /my-plan/{id} (klient view)
├── web/                    # General Thymeleaf controllers
│   ├── PageController.java         # /, /login, /register, /dashboard
│   └── RegistrationForm.java
├── configuration/
│   ├── SecurityConfiguration.java  # JEDEN filter chain
│   ├── WebConfiguration.java       # CORS
│   └── AdminInitializer.java       # bootstrap admin@admin.cz / heslo123
└── common/
    ├── NotFoundException.java
    ├── ForbiddenException.java
    └── GlobalRestExceptionHandler.java  # mapuje výjimky -> HTTP 400/403/404
```

## 4. Datový model (high-level)

```
account (USER nebo ADMIN, + deleted_at pro soft delete)
   └─ training (1:N)                       [visibility=PRIVATE: owner_id NOT NULL]
                                            [visibility=GROUP:    owner_id NULL]
                                            [visibility=TEMPLATE: owner_id NULL]
                                            [created_by_id (kdo založil)]
                                            [source_template_id → training (Phase 8)]
         └─ training_exercise (1:N)        [type, catalog_item_id XOR custom_name]
               └─ exercise_set (1:N)       [weight_kg, reps, rpe, note — nullable]

coach_plan                 ─── markdown plán od trenéra (client_id, author_id, title,
                               body_markdown, valid_from, valid_to)  -- Phase 8
training_comment           ─── komentář k tréninku (text, author, created_at)
exercise_catalog_item      ─── system seed (V3, 92 cviků) + custom by admin (Phase 2+)
training_tag               ─── system tags (V4, 8 tagů) + per-user custom
training_tag_link          ─── M:N (training × tag)
```

**Klíčová pravidla:**
- **PRIVATE trénink** patří 1 klientovi (owner_id NOT NULL), vidí jen majitel (a admin)
- **GROUP trénink** vytvoří admin (createdBy=admin), owner=NULL, vidí všichni klienti
  pro daný den (training_date)
- **TEMPLATE trénink** (Phase 8) je šablona od admina (owner=NULL); po `assignTemplateToClient`
  vznikne klientovi PRIVATE kopie se `source_template_id` FK na šablonu
- DB CHECK constraint: `visibility=PRIVATE → owner_id NOT NULL`; GROUP/TEMPLATE smí owner=NULL
- Cvik buď z katalogu (catalog_item_id) NEBO custom název — XOR vynucený DB constraintem
- RPE existuje na 3 úrovních: trénink / cvik / set, všude nullable, 1–10
- Difficulty (LIGHT/MEDIUM/HARD) jen na tréninku, nullable, klient si nastavuje sám
- Tagy: system (globální) + custom (per-user, owner_id NOT NULL)
- Statistiky (`/analysis`, `/admin/overview`) počítají JEN PRIVATE — GROUP/TEMPLATE ignorují

## 5. Časté příkazy

```bash
# Build + run
./mvnw clean install
./mvnw spring-boot:run

# Jen testy
./mvnw test

# Specifický test
./mvnw test -Dtest=TrainingOwnershipTest

# Lokální Postgres (heslo: postgres)
PGPASSWORD=postgres "/c/Program Files/PostgreSQL/17/bin/psql.exe" -U postgres -h localhost -p 5432 -d ragnarok_diary_database

# Drop + recreate lokální DB (musí být zastavená app!)
PGPASSWORD=postgres "/c/Program Files/PostgreSQL/17/bin/psql.exe" -U postgres -c "DROP DATABASE ragnarok_diary_database"
PGPASSWORD=postgres "/c/Program Files/PostgreSQL/17/bin/psql.exe" -U postgres -c "CREATE DATABASE ragnarok_diary_database WITH ENCODING='UTF8'"

# Git workflow — develop je integration branch, master je stable
git checkout develop
# ... commits přímo na develop ...
# Master se merguje až při stabilním release (později)
```

## 6. Databáze — lokální dev

| Pole | Hodnota |
|------|---------|
| Host | `localhost` |
| Port | `5432` |
| DB | `ragnarok_diary_database` |
| User | `postgres` |
| Password | `postgres` |

**PostgreSQL 17** v `C:\Program Files\PostgreSQL\17\`, služba `postgresql-x64-17`.
DBeaver connection jméno: `Ragnarok Diary (local)`.

**Aplikace tahá heslo z env var `DB_PASSWORD`, default je `postgres`.** Změnu udělej buď
env var, nebo úpravou `application.properties` (jen pro lokální dev; v gitu má fallback).

## 7. Bootstrap admin

Při startu se v `AdminInitializer` vytvoří admin účet, pokud ještě neexistuje:
- Email: `admin@admin.cz`
- Heslo: `heslo123`
- Role: `ADMIN`

Lze přebít env proměnnými `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, atd.
**Na Railway nastavit silnější heslo!**

## 8. Stav fází (k 2026-05-25)

| # | Fáze | Stav | Kde |
|---|------|------|-----|
| 0 | Foundation (account, security, Thymeleaf skeleton) | ✅ DONE | mergnuto do `develop` |
| 1 | MVP diary (klient si zapíše trénink) | ✅ DONE | mergnuto do `develop` |
| 2 | Trenér + skupinové tréninky + komentáře | ✅ DONE | na `develop` |
| 3 | Rozšířené typy cviků (10 typů — EMOM/Tabata/AMRAP/Circuit/Ladder/Stepladder/Pyramid/Superset/Complex/StraightSets) | ✅ DONE | na `develop` |
| 4 | Timer / stopky + wake lock | ✅ DONE | na `develop` |
| 5 | Statistiky (klient `/analysis` + admin `/admin/overview`) | ✅ DONE | na `develop` |
| 8 | Individuální plány od trenéra (TrainingTemplate + CoachPlan markdown) | ✅ DONE | na `develop` |
| — | UI redesign (Forge design system, Phase R) | ✅ DONE | na `develop` |
| — | Design sjednocení s ragnarokostrava.cz — brick #9F371B, Montserrat, přepínač Light/Batman Mode | ✅ DONE | na `develop` (2026-06-11) |
| — | ScoutMeto kolo 5 — pohlaví/cyklus, kondiční metriky, MTF zóny, cyklus-kalendář, admin notifikace, design fixy (Batman kontrast/oddělení formuláře/logout) | ✅ DONE | na `develop` (2026-06-13, V32) |
| — | ScoutMeto kolo 6 — logo do lišty (Tréninkový deník/Přehled), skupinový textový trénink (nabídka→My-plan), impersonace exit fix, vlastní MTF, admin dashboard cleanup, přejmenování spánkového RPE | ✅ DONE | na `develop` (2026-06-19, V33) |
| — | ScoutMeto kolo 7 — šablony merge, přiřadit/duplikovat, publikace draftů + týdenní nabídka, stránkování 20/20, zrušení rezervace u lekce (30min), náhradník/waitlist, počet záznamů, měsíční deník, mazání textových plánů, členství, sekce Výhody | ✅ DONE | na `develop` (2026-07-02, V36–V39) |
| — | Admin správa rezervací z deníku — přihlášení klienti u lekce + zrušení komukoli (klíčovaný endpoint 7.2) | ✅ DONE | na `develop` (2026-07-03) |
| — | ScoutMeto kolo 8 — Náplň tréninku/Části, KB sport podrobný záznam (handswitch/bilaterální), EMOM/Tabata inline tabulky, Circuit náčiní+tagy per cvik, Ladder/Stepladder/Pyramid generovaná tabulka s validací, Freeform Odpočinek, tag Carry (metry/sekundy), katalog→náčiní prefill, CORE pryč, max 1 rezervace na lekci | ✅ DONE | na `develop` (2026-07-11, V40) |
| — | ScoutMeto kolo 9 — účty (Poslat do záhrobí/Probrat, Zrodit vikinga + notborn hláška), živé tabulky (reps/kg okamžitě, 2 zátěže = součet), circuit XOR + Carry per krok, mazání řádků série, tag Isometrie (jen sekundy), počeštění tagů, export deníku v JSON + AI schema na e-mail | ✅ DONE | na `develop` (2026-07-18, V41) |
| 6 | Email confirmation při registraci + notifikace (cron + 3 eventy) | ✅ DONE | na `develop` |
| 7.1 | Integrace s rezervačním systémem — kalendář + rezervace 1 klikem (jen veřejné API) | ✅ DONE | na `develop` |
| 7.2 | Rezervace: cancel + "moje rezervace" + historie (sdílený API klíč, cross-repo) | ✅ DONE | na `develop` (deploy: klíč v obou appkách!) |
| 9 | Quick wins (ScoutMeto feedback): difficulty 3 úrovně × 9 labelů, nové tagy, smazat CUSTOMIZING, šablona save bug, gym overview cleanup | ⏳ IN PROGRESS | na `develop` |
| 10–17 | Další ScoutMeto požadavky (inactive účty, per-exercise tagy/equipment, nové typy, korunka/flag, rozšířené statistiky, katalog cviků CRUD) | ⏳ TODO | viz `docs/development-log.md` |

**Branching strategie:** `develop` = veškerý vývoj. `master` = stable release (zatím se nepoužívá,
mergnutí ze `develop` proběhne ručně při stabilizaci verze pro produkci).

### ScoutMeto feedback (25.5.2026) — klíčová rozhodnutí pro Phase 9–17

- **Difficulty (A13):** ZŮSTÁVAJÍ 3 úrovně (statistiky = 3 sloupce), ale uživatel
  vybírá z 9 konkrétních labelů; v roletě se NEzobrazí slova light/medium/hard.
  - Úroveň 1: „lehký trénink", „deload", „rychlost"
  - Úroveň 2: „silově-kondiční trénink", „sběr opakování", „drill", „výuka"
  - Úroveň 3: „rozvoj maximální síly", „testování"
- **Equipment (A14):** default jen 3 (bez pomůcek / kettlebell / osa) + custom per-user (uchované, smazatelné)
- **Korunka (A16):** per-instance (konkrétní cvik v konkrétním tréninku)
- **Flag (B8):** per-trénink
- **Inactive účet (E1):** read-only, skupinové lekce nezobrazovat
- **StrongFirst ladder (A10):** jeden cvik, set 1+rest, 2+rest, ... až ladder_height, pak cyklus znovu; L/P u unilaterálních
- **KB sport time (A12):** reps za čas + volitelný multiswitch s libovolnou délkou intervalu
- **Circuit (A3):** vlastní per-round záznam, editovatelný (cvik lze v kole vyřadit/nahradit)
- **Katalog cviků (Phase 17):** každý klient si edituje VLASTNÍ katalog. Seed 92 = system (owner=null).
  Admin přidá → vidí všichni. Klient přidá/upraví → vidí jen on (copy-on-write pattern jako tagy).

Detail jednotlivých fází viz `docs/development-log.md` a `docs/architecture.md`.

## 9. Důležité konvence

### Vždy
- **Branche:**
  - `master` — stable releases (zatím prázdné, mergne se ze `develop` při stabilizaci)
  - `develop` — integration, **veškerý vývoj se odehrává tady**
  - Pro Phase N **netvořit** separátní feature branch — commity rovnou na `develop`
  - Pro rizikový experiment/hotfix lze krátkodobou `feature/...` branch, merge zpět do develop
- **Commit messages** v Conventional Commits stylu (`feat:`, `fix:`, `chore:`, `docs:`, `test:`)
- **Flyway migrace** pro každou schema změnu (V5, V6, …) — nikdy nesahat na existující migraci!
- **Bezpečnost na service vrstvě** — controllery se neptají na ownership, service ano (viz `TrainingService.getMyTraining`)
- **Per-set logging nepovinné** — klient zaznamenává jen to, co chce sledovat
- **Validace přes Jakarta Bean Validation** (@NotBlank, @Email, @Min, @Max) na DTO

### Nikdy
- Nepoužívej `System.out.println` — vždy SLF4J (`Logger log = LoggerFactory.getLogger(...)`)
- Neupravuj existující Flyway migraci — vytvoř novou (V_next__fix.sql)
- Nepřepisuj heslo defaultem v `application-prod.properties` — vše přes env vars
- Necommituj secrets do gitu (DB heslo má fallback jen v `application.properties` pro lokální dev)
- Nepoužívej `@RequestController` nebo `@Controller` mix v jedné třídě — buď REST nebo Thymeleaf

## 10. Známé gotchas (na co jsme narazili)

### Lombok + boolean `isFoo` field
Pro field `private boolean isSystem` Lombok generuje:
- Getter: `isSystem()` ✅
- Setter: `setSystem()` ❌ (DROPS the "is" prefix, ne `setIsSystem`)

Když píšeš setter call, použij `entity.setSystem(true)`, ne `setIsSystem`.

### Thymeleaf `#fields` mimo `<form th:object>`
`#fields.hasGlobalErrors()`, `#fields.hasErrors(...)`, `#fields.errors(...)` MUSÍ být uvnitř
`<form th:object="${formObject}">`. Mimo form vyhodí `TemplateProcessingException`.

### Thymeleaf static cache
`spring.thymeleaf.cache=false` v lokálním profilu, ale **statické soubory (CSS, JS)** se
cachují agresivně browserem. Pro vývoj používej Ctrl+F5 (hard refresh).

### ⚠️ `th:each` proměnná NESMÍ být SpEL operátor (`eq`, `ne`, `lt`, `gt`, `le`, `ge`, `or`, `and`, `not`, `div`, `mod`)
`th:each="eq : ${list}"` vyhodí za běhu `IllegalArgumentException: Iteration variable cannot be null`,
protože `eq` je textový SpEL operátor (= rovnost). Render se **uťne uprostřed** → zbytek stránky
(včetně `<script>` na konci) se neodešle → vypadá to jako úplně jiný bug (mrtvé tlačítko, prázdný
seznam). Použij neutrální název (`item`, `equip`, `ex`, ...). Stálo to hodinu debugu v Phase 19 retestu.

### `mvnw spring-boot:run` servíruje šablony z `target/classes`, ne z `src`
Při běhu přes `spring-boot:run` se Thymeleaf šablony čtou z `target/classes/templates/...`
(zkopírované při `process-resources` na startu). I s `spring.thymeleaf.cache=false` se **úprava
souboru v `src/main/resources` neprojeví bez restartu** (nebo bez devtools / ručního re-copy).
Po editaci šablony app restartuj, jinak testuješ starou verzi (stálo to debug v kolo 5 retestu).

### Postgres `user` je reserved keyword
Proto je tabulka `account`, ne `user` — historicky byla `user`, opraveno v Fázi 0.

### Spring Security: nepoužívej duplicitní `@Configuration` s `authenticationManager` bean
Mít jen JEDEN `SecurityFilterChain` bean, autokonfigurace zařídí zbytek z `UserDetailsService` +
`PasswordEncoder` beanů. Custom `DaoAuthenticationProvider` bean není potřeba.

### XOR constraints v DB
`exercise_name_xor`: `(catalog_item_id IS NOT NULL AND custom_name IS NULL) OR
(catalog_item_id IS NULL AND custom_name IS NOT NULL)`. Pokud chceš povolit obojí jako null
(což aktuálně NEDOVOLÍME), upravuj v migraci.

### OSIV (Open Session In View) je ZAPNUTÝ
`spring.jpa.open-in-view=true` — záměrně, pragmatická volba pro malou Thymeleaf aplikaci.
Bez něj Thymeleaf rendering hodí `LazyInitializationException` na `training.tags`, `training.exercises.sets`
atd. Alternativa (`@EntityGraph` na každém query) je hodně víc kódu a riskuje `MultipleBagFetchException`.
Zrevidovat v Phase 5 (statistiky) nebo když výkon začne škrtit.

### IntelliJ Run okno blokuje port 8080
Když uživatel zapomene zastavit IntelliJ Spring Boot run a spustí znovu, dostaneš
`Port 8080 was already in use`. Najdi proces (`Get-NetTCPConnection -LocalPort 8080`)
a `Stop-Process -Id <pid> -Force` (admin nebo majitel procesu).

## 11. Kontext o uživateli / klientovi

- **Marek** (vývojář) — komunikuje česky, spíš začátečník-mírně pokročilý v Javě/Spring
- **Jeho kamarád** (trenér v Ragnaroku) — produktový "klient", review plánu a feedback
- **2 trenéři + N klientů** — single-tenant gym
- **Tělocvična Ragnarok** se zaměřuje na **kettlebell / funkční trénink / OS-Resets**
- **Komunikace probíhá česky** — odpovídám i komentuju kód česky (proměnné a Javadoc anglicky pro standard)

## 12. Klíčové soubory pro Read (když potřebuješ kontext)

| Co potřebuješ | Soubor |
|---------------|--------|
| Stav fází + co dál | `docs/development-log.md` |
| Architekturní rozhodnutí | `docs/architecture.md` |
| Aktuální plán Fáze N | `docs/development-log.md` |
| Domain model | `src/main/resources/db/migration/V2__init_training_diary.sql` |
| Bezpečnostní config | `src/main/java/.../configuration/SecurityConfiguration.java` |
| Hlavní business logic | `src/main/java/.../training/TrainingService.java` |
| Form binding | `src/main/java/.../training/dto/TrainingInput.java` |
| Default credentials | `src/main/resources/application.properties` |

## 13. Když začínáš novou session

1. **Přečti tento soubor** (CLAUDE.md)
2. **Přečti `docs/development-log.md`** — kde jsme skončili, co je další
3. **Zjisti stav branche** (`git branch --show-current`, `git log --oneline -5`)
4. **Zjisti, co user chce** — buď pokračovat fází, nebo opravit konkrétní bug
5. **Až pak otevři konkrétní soubory**, které k tomu úkolu potřebuješ

To je hlavní úspora tokenů — **nečti zbytečně soubory, které ti CLAUDE.md popsal jako
black-box** (např. `AccountUserDetailsService` — pokud nepotřebuješ měnit auth, neotevírej ho).
