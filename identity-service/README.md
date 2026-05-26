Keycloak
========

To understand the contents of your Keycloak installation, see the [directory structure guide](https://www.keycloak.org/server/directory-structure).

To get help configuring Keycloak via the CLI, run:

on Linux/Unix:

    $ bin/kc.sh

on Windows:

    $ bin\kc.bat

To try Keycloak out in development mode, run: 

on Linux/Unix:

    $ bin/kc.sh start-dev

on Windows:

    $ bin\kc.bat start-dev

After the server boots, open http://localhost:8080 in your web browser. The welcome page will indicate that the server is running.

To get started, check out the [configuration guides](https://www.keycloak.org/guides#server).

## Docker

Keycloak **26.6.2** runs from the official image; this folder supplies `conf/`, `providers/`, and `themes/`.

### Prerequisites

- Docker Engine 24+ and Docker Compose v2

### Production-like (Postgres)

```bash
cd identity-service
cp .env.example .env   # optional — edit passwords
docker compose up --build
```

- Admin console: http://localhost:8081 (default `8081` if `8080` is busy; user `admin` / password from `.env`)
- Postgres: user `postgre`, password `123456`, database `keycloak`
- **pgAdmin (from host):** host `localhost`, port `5433` (mapped; host `5432` is often already in use)

### pgAdmin

1. **Register → Server → Connection**
2. Host: `localhost`
3. Port: `5433`
4. Maintenance database: `keycloak`
5. Username: `postgre`
6. Password: `123456`
7. Save → expand **Databases → keycloak → Schemas → public → Tables**
- Postgres data is stored in the `postgres_data` volume.

### Dev mode (embedded DB, no Postgres)

```bash
docker compose -f docker-compose.dev.yml up --build
```

### Stop

```bash
docker compose down
# docker compose down -v   # also remove Postgres volume
```