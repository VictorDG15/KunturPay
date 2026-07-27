# Contribución y workflow

## Gitflow

- `main`: versiones productivas etiquetadas.
- `develop`: integración del siguiente release.
- `feature/<ticket>-<descripcion>`: funcionalidad desde `develop`.
- `release/<version>`: estabilización previa a producción.
- `hotfix/<ticket>-<descripcion>`: corrección urgente desde `main`.

Ejemplo:

```bash
git switch develop
git pull --ff-only
git switch -c feature/PAY-101-idempotent-payments
git commit -m "feat(payment): add distributed idempotency"
git push -u origin feature/PAY-101-idempotent-payments
```

## Antes del pull request

```bash
./mvnw clean verify
./mvnw -Pintegration verify
```

Checklist:

- Pruebas para reglas nuevas y casos negativos.
- Sin PAN, CVV, API keys o contraseñas en código/logs.
- Migración Flyway backward compatible.
- Evento versionado sin romper consumidores.
- OpenAPI y Postman actualizados.
- Métricas y logs con correlation ID.
- Reintentos idempotentes.
- Al menos una aprobación de Code Review.
- Sonar sin nuevas vulnerabilidades ni code smells críticos.

## Commits

Usar Conventional Commits:

```text
feat(payment): add partial refunds
fix(outbox): preserve retry after Kafka timeout
test(payment): cover concurrent capture
docs(aws): describe EventBridge adapter
```
