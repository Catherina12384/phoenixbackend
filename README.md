# Phoenix Computers backend

Spring Boot 3.5 · Java 21 · Gradle · PostgreSQL 16 · Flyway · JWT

## Run locally
```bash
docker compose up -d                 # PostgreSQL on :5432
gradle wrapper --gradle-version 8.14 # once, if gradlew is missing (needs Gradle installed)
./gradlew bootRun                    # dev profile: API on :8080, admin@phoenix.local / ChangeMe123!
```
Flyway creates the tables and seeds the 17 dealers on first start.

## Try it
```bash
curl localhost:8080/api/dealers
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@phoenix.local","password":"ChangeMe123!"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -X POST localhost:8080/api/admin/products -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"HP Pavilion 15","category":"Laptops","price":58990,"dealerId":"hp","specs":{"RAM":"16 GB","Storage":"512 GB SSD"}}'
curl 'localhost:8080/api/products?dealer=hp'
```

## Production
Set `JWT_SECRET` (32+ chars), `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `ADMIN_EMAIL` / `ADMIN_PASSWORD`
for the first start, then run the jar without the dev profile (`./gradlew bootJar`).
