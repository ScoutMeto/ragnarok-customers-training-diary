# Ragnarok Customers Training Diary

Tréninkový deník pro klienty tělocvičny Ragnarok. Klient si loguje vlastní
tréninky (cviky, sety, RPE), vidí týdenní program lekcí od trenéra, prohlíží
si svoje statistiky a propojuje se s [rezervačním systémem](https://github.com/ScoutMeto/ragnarok_customers_reservation_system).

## Tech stack

- Java 21 (LTS), Spring Boot 3.4.4
- PostgreSQL 14+ + Flyway migrace
- Thymeleaf + Bootstrap 5 (responzivní)
- Spring Security (session-based, BCrypt, role USER/ADMIN)
- Maven

## Aktuální stav: Fáze 0 (Foundation)

✅ Sjednocený `account` model s rolemi USER/ADMIN
✅ Registrace + login + logout
✅ Bootstrap admin při startu
✅ Thymeleaf skeleton (login, register, dashboard) s rolí-aware navigací
✅ Flyway migrace
✅ 7 smoke testů

Implementace dalších fází (Můj deník, Plán lekcí, Statistiky, ...)
přijde v dalších feature branchích.

## Lokální spuštění

### 1. Předpoklady

- Java 21 (JDK)
- PostgreSQL 14+ běžící na `localhost:5432`
- Maven (nebo použij `mvnw` v repu)

### 2. Vytvořit databázi

```sql
-- Připoj se jako superuser (např. postgres):
CREATE DATABASE ragnarok_diary_database;
```

Pokud máš jiné heslo než `postgres`, nastav env var `DB_PASSWORD` (viz níže).

### 3. Spuštění

```bash
./mvnw spring-boot:run
```

App naběhne na **http://localhost:8080**.

### 4. Defaultní admin

Při prvním startu se vytvoří účet:

| Email | Heslo |
|-------|-------|
| `admin@admin.cz` | `heslo123` |

Lze přebít env proměnnými `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`,
`BOOTSTRAP_ADMIN_FIRST_NAME`, `BOOTSTRAP_ADMIN_LAST_NAME`.

## Env proměnné

| Var | Default (local) | Popis |
|-----|-----------------|-------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ragnarok_diary_database` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB uživatel |
| `DB_PASSWORD` | `postgres` | DB heslo |
| `SPRING_PROFILES_ACTIVE` | (none) | Pro produkci nastav `prod` |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@admin.cz` | Email prvního admina |
| `BOOTSTRAP_ADMIN_PASSWORD` | `heslo123` | Heslo prvního admina |

## Testy

```bash
./mvnw test
```

Spouští 7 smoke testů na H2 in-memory DB (profile `test`).

## Build

```bash
./mvnw clean package
```

Vytvoří spustitelný JAR v `target/`.

## Deployment na Railway

V Railway dashboardu:

1. Vytvoř nový Postgres service → zkopíruj connection údaje
2. V Diary service nastav env proměnné:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (z Postgres service)
   - `BOOTSTRAP_ADMIN_PASSWORD` (silné heslo!)
3. Deploy push do main → Railway buildne přes `./mvnw package` a spustí JAR

## Vývoj — branching

```bash
# Nová feature větev
git checkout -b feature/phase-N-popis

# Po dokončení
git push -u origin feature/phase-N-popis
# Pull request → review → merge do main
```

## Struktura projektu

```
src/main/java/com/ragnarok/ragnarok_customers_training_diary/
├── account/                  # account doména (entity, repo, service, REST controller, DTO)
├── configuration/            # SecurityConfiguration, WebConfiguration, AdminInitializer
├── web/                      # Thymeleaf page controllers
└── RagnarokCustomersTrainingDiaryApplication.java

src/main/resources/
├── db/migration/             # Flyway SQL migrace (V1__, V2__, ...)
├── static/css/               # CSS
├── templates/                # Thymeleaf HTML
└── application*.properties
```
