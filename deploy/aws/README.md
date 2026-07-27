# Siguiente fase: AWS

La aplicación compila y funciona localmente sin una cuenta AWS. Esta carpeta
define el orden recomendado para convertirla en una demo cloud sin mezclar
credenciales o infraestructura incompleta con el núcleo.

## Arquitectura objetivo

- ECR: imagen del servicio.
- EKS: KunturPay con dos réplicas y HPA.
- RDS PostgreSQL Multi-AZ: pagos y Outbox.
- ElastiCache Redis: idempotencia.
- MSK/Confluent o EventBridge: bus según el caso.
- SQS + DLQ: desacoplar consumidores.
- Lambda Java 21: notificación/auditoría.
- Secrets Manager: credenciales.
- CloudWatch + OpenTelemetry/Dynatrace: observabilidad.

## Orden de implementación

1. Crear Terraform remoto (S3 + DynamoDB lock).
2. VPC privada, subnets en dos AZ y endpoints.
3. RDS y ElastiCache sin exposición pública.
4. ECR, EKS e IRSA.
5. External Secrets para materializar secretos en Kubernetes.
6. ALB Controller, TLS con ACM y WAF.
7. Bus, SQS, DLQ y Lambda.
8. Alarmas, dashboards, budgets y pruebas de recuperación.

## Variables que no deben entrar a Git

```text
DB_PASSWORD
API_KEY
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
SONAR_TOKEN
```

## Decisión pendiente

Elegir una de estas rutas:

- **MSK/Confluent**: mejor para alto volumen, orden, replay y ecosistema Kafka.
- **EventBridge + SQS**: mejor para integración AWS administrada y menor operación.

El código actual encapsula esa decisión detrás del Outbox publisher.
