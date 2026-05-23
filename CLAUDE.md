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
├── account/                # Sjednocený User+Admin model (role enum)
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
├── training/               # Tréninkový deník
│   ├── TrainingEntity.java         # 1 trénink = 1 klient (owner_id FK)
│   ├── TrainingExerciseEntity.java # cvik v tréninku (XOR catalog_item / custom_name)
│   ├── ExerciseSetEntity.java      # set cviku (weight, reps, RPE, note — vše nullable)
│   ├── TrainingDifficulty.java     # enum LIGHT/MEDIUM/HARD
│   ├── TrainingExerciseType.java   # FREEFORM + budoucí EMOM/CIRCUIT/Tabata/...
│   ├── *Repository.java
│   ├── TrainingService.java        # business logic + ownership check
│   ├── DiaryPageController.java    # /diary/** (Thymeleaf)
│   └── dto/                # TrainingInput, TrainingExerciseInput, SetInput
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
account (USER nebo ADMIN)
   └─ training (1:N)                       [owner_id FK na account]
         └─ training_exercise (1:N)        [type, catalog_item_id XOR custom_name]
               └─ exercise_set (1:N)       [weight_kg, reps, rpe, note — nullable]

exercise_catalog_item   ─── system seed (V3, 92 cviků) + custom by admin (Phase 2)
training_tag            ─── system tags (V4, 8 tagů) + per-user custom
training_tag_link       ─── M:N (training × tag)
```

**Klíčová pravidla:**
- Trénink patří 1 klientovi (owner_id FK na account, nikdy null)
- Cvik buď z katalogu (catalog_item_id) NEBO custom název — XOR vynucený DB constraintem
- RPE existuje na 3 úrovních: trénink / cvik / set, všude nullable, 1–10
- Difficulty (LIGHT/MEDIUM/HARD) jen na tréninku, nullable, klient si nastavuje sám
- Tagy: system (globální) + custom (per-user, owner_id NOT NULL)

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

# Git workflow (vždy feature branch, nikdy přímo na master)
git checkout -b feature/phase-N-popis
# ... commits ...
# Push + PR na GitHubu nebo lokální merge
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

## 8. Stav fází (k 2026-05-23)

| # | Fáze | Stav | Branch |
|---|------|------|--------|
| 0 | Foundation (account, security, Thymeleaf skeleton) | ✅ DONE | `feature/phase-0-foundation` |
| 1 | MVP diary (klient si zapíše trénink) | ✅ DONE | `feature/phase-1-mvp-diary` |
| 2 | Trenér + GroupLessonPlan + komentáře | ⏳ NEXT | – |
| 3 | Rozšířené typy cviků (EMOM, Circuit, Tabata, ...) | – | – |
| 4 | Timer / stopky + wake lock | – | – |
| 5 | TrainingAnalysisService (statistiky) | – | – |
| 6 | Email notifikace | – | – |
| 7 | Integrace s rezervačním systémem (REST API) | – | – |
| 8 | Individuální plány od trenéra (template + text) | – | – |

Detail jednotlivých fází viz `docs/development-log.md` a `docs/architecture.md`.

## 9. Důležité konvence

### Vždy
- **Feature branch** (`feature/phase-N-popis`), 5–9 atomických commitů, na konci review/merge
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
