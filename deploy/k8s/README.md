# Kubernetes

1. Cambia endpoints e imagen en `configmap.yaml` y `deployment.yaml`.
2. Crea el secreto sin subir valores reales:

```bash
kubectl apply -f namespace.yaml
kubectl create secret generic paycore-secrets \
  --namespace paycore \
  --from-literal=DB_PASSWORD='...' \
  --from-literal=API_KEY='...'
kubectl apply -k .
```

En EKS usa External Secrets + Secrets Manager e IRSA en lugar del secreto manual.
