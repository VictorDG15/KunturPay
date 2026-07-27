# Guía para presentar el proyecto

## Pitch de 60 segundos

> Construí KunturPay, un orquestador de pagos en Java 21 y Spring Boot. El problema
> principal no era hacer un CRUD, sino garantizar que un reintento del cliente no
> duplique el cobro y que ningún evento se pierda entre PostgreSQL y Kafka. Usé
> idempotencia distribuida en Redis, una segunda defensa única en PostgreSQL y
> Transactional Outbox. El dominio está aislado con arquitectura hexagonal,
> controla autorización, captura y reembolsos, y usa optimistic locking para
> concurrencia. Incluí Flyway, Testcontainers, ArchUnit, JaCoCo, Sonar, Docker,
> GitHub Actions y manifiestos Kubernetes preparados para EKS.

## Demo de 5 minutos

1. Enseña la estructura y abre `Payment.java`.
2. Ejecuta un pago autorizado desde Postman.
3. Repite exactamente la petición: debe devolver el mismo pago.
4. Cambia el monto manteniendo la llave: debe responder 409.
5. Captura y luego reembolsa parcialmente.
6. Enseña `payments` y `outbox_events` en PostgreSQL.
7. Explica que Kafka puede caer sin perder el evento.
8. Abre las pruebas de dominio y ArchitectureTest.

## Preguntas y respuestas

### ¿Por qué Outbox?

Una transacción distribuida entre PostgreSQL y Kafka aumenta acoplamiento y no
siempre está disponible. Outbox guarda el cambio de negocio y el mensaje en una
transacción local. Luego publica con entrega al menos una vez.

### ¿Eso garantiza exactamente una vez?

No de extremo a extremo. Garantiza que no perdemos el evento, pero puede haber
duplicados. Cada evento tiene `eventId`; el consumidor debe usar inbox/deduplicación
y hacer su operación idempotente.

### ¿Por qué Redis y PostgreSQL?

Redis permite reservar llaves con baja latencia y TTL. PostgreSQL conserva el
estado financiero y la unicidad comercio/orden. Redis es optimización y primera
barrera; no debe ser la única garantía.

### ¿Qué pasa si el adquirente autoriza y la DB cae?

Es el caso crítico. En producción usaría una referencia idempotente enviada al
adquirente, estado `UNKNOWN`, consulta posterior y un proceso de reconciliación.
Nunca repetiría a ciegas una autorización cuyo resultado fue incierto.

### ¿Cómo llevarías esto a AWS?

Contenedor en EKS, RDS Multi-AZ, ElastiCache, ECR, Secrets Manager e IRSA. Para
eventos evaluaría MSK/Confluent si necesito streaming y orden por partición, o
EventBridge + SQS si necesito integración administrada y fan-out. Lambdas sirven
para consumidores breves como notificaciones o auditoría.

### Kafka versus EventBridge

Kafka ofrece alto throughput, replay, retención y orden por partición. EventBridge
reduce operación y enruta eventos entre servicios AWS, pero no sustituye todos
los casos de streaming. La decisión depende del patrón y del volumen.

### ¿Cómo escalar?

Escalo pods horizontalmente. La key Kafka es `paymentId` para conservar orden por
pago. El publisher usa lotes y locks; para mayor volumen usaría `SKIP LOCKED`,
particionamiento de Outbox o CDC con Debezium.

## Mejoras para una segunda versión

- OAuth2 Resource Server y scopes por comercio.
- Adaptador real de EventBridge.
- SQS con DLQ y Lambda de notificaciones.
- OpenTelemetry hacia Dynatrace.
- Pruebas de contrato con Pact.
- k6 para throughput, p95 y reintentos.
- Terraform para VPC, EKS, RDS, ElastiCache y observabilidad.
