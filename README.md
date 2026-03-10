# Harness Enterprise SE Candidate Lab

## Overview

This repo contains all artifacts for the Harness Enterprise SE Candidate Lab, demonstrating:

- **CI**: Build, test, and push a Java Spring Boot app to DockerHub
- **CD**: Deploy to Kubernetes using a **Canary release strategy**
- **Bonus**: Reusable step templates (templatization)

The application is a minimal Spring Boot REST API with two endpoints:
- `GET /` — returns `{ "message": "Hello from Harness Demo App!", "version": "stable" }`
- `GET /health` — Kubernetes liveness/readiness probe

---

## Project Structure

```
harness-demo/
├── app/                                  # Java Spring Boot application
│   ├── src/main/...                      # Application source
│   ├── src/test/...                      # JUnit tests
│   ├── Dockerfile                        # Multi-stage Docker build
│   └── pom.xml                           # Maven build descriptor
├── k8s/                                  # Kubernetes manifests
│   ├── namespace.yaml
│   ├── deployment.yaml                   # App deployment (2 replicas)
│   └── service.yaml                      # ClusterIP service
└── harness/                              # Harness pipeline configs
    ├── pipeline.yaml                     # Full CI + CD pipeline
    ├── maven-step-template.yaml          # Reusable Maven build/test step
    └── step-template-docker-push.yaml    # Reusable Docker build+push step
```

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Docker Desktop | 24+ | Must be running |
| minikube | v1.38+ | `brew install minikube` |
| kubectl | v1.35+ | `brew install kubectl` |
| helm | v4+ | `brew install helm` |

---

## Step 1: Kubernetes Cluster Setup

```bash
# Start minikube with enough resources for Maven builds
minikube start --driver=docker --memory=4096 --cpus=2

# Create required namespaces
kubectl create namespace harness-builds
kubectl create namespace harness-demo
```

---

## Step 2: Harness Trial & Delegate Install

1. Sign up at https://app.harness.io/auth/#/signup
2. Install the delegate into minikube via Helm:

```bash
helm repo add harness-delegate https://app.harness.io/storage/harness-download/delegate-helm-chart/
helm repo update harness-delegate

helm upgrade -i helm-delegate --namespace harness-delegate-ng --create-namespace \
  harness-delegate/harness-delegate-ng \
  --set delegateName=helm-delegate \
  --set accountId=<YOUR_ACCOUNT_ID> \
  --set delegateToken=<YOUR_DELEGATE_TOKEN> \
  --set managerEndpoint=https://app.harness.io \
  --set delegateDockerImage=harness/delegate:26.02.88600 \
  --set replicas=1 --set upgrader.enabled=true
```

3. Verify the delegate is running and shows **Connected** in Harness:

```bash
kubectl get pods -n harness-delegate-ng
```

---

## Step 3: Connectors

Create the following connectors in Harness (**Project Settings → Connectors**):

| Identifier | Type | Details |
|---|---|---|
| `github_connector` | GitHub | Account URL type — `https://github.com/<your-username>` |
| `dockerhub_connector` | Docker Registry | hub.docker.com + your credentials |
| `k8s_connector` | Kubernetes | Use delegate selector `helm-delegate`, `InheritFromDelegate` |

---

## Step 4: Templates

Create both step templates before importing the pipeline (**Project → Templates → + New Template → Step**):

| Template | File | Version |
|---|---|---|
| `Maven_Test_Step` | `harness/maven-step-template.yaml` | 1.0 |
| `Docker_Build_and_Push` | `harness/step-template-docker-push.yaml` | 1.0 |

---

## Step 5: Pipeline

1. Go to **Pipelines → + Create Pipeline → Import from YAML**
2. Paste contents of `harness/pipeline.yaml`
3. Set up the service (`harness_demo_service`) with:
   - Manifest source: GitHub → `k8s/deployment.yaml` + `k8s/service.yaml`
   - Artifact: DockerHub → `<your-username>/harness-demo:latest`
4. Set up environment `production` with infrastructure pointing to `k8s_connector`, namespace `harness-demo`

### CI Stage
```
Clone repo (main) → Maven Build & Test → Docker Build & Push (:latest)
```

### CD Stage — Canary
```
Fetch K8s manifests → Canary Deploy (25%) → Approval Gate → Canary Delete → Rolling Deploy (100%)
```

Failure strategy: automatic rollback via `K8sCanaryDelete` + `K8sRollingRollback`

---

## Step 6: Run the Pipeline

1. Click **Run** on the pipeline
2. When it reaches the **Approval** step, click the step and approve to promote to full rollout
3. Verify the deployment:

```bash
kubectl get pods -n harness-demo
kubectl port-forward svc/harness-demo 8080:80 -n harness-demo
curl http://localhost:8080/
```

---

## Templatization

Two steps are templatized to demonstrate reuse and standardization:

**`Maven_Test_Step`** — runs `mvn clean verify` in a Maven container and publishes JUnit reports to Harness

**`Docker_Build_and_Push`** — wraps the `BuildAndPushDockerRegistry` step with `connectorRef` and `repo` as runtime inputs (`<+input>`), making it reusable across different registries and repos

---

## Running Locally

```bash
cd app

# Run tests
mvn test

# Build
mvn clean package -DskipTests

# Build Docker image
docker build -t harness-demo:local .

# Run
docker run -p 8080:8080 harness-demo:local

# Test
curl http://localhost:8080/
curl http://localhost:8080/health
```

---

## Key Links

- Harness Docs: https://developer.harness.io/
- Delegate Install: https://developer.harness.io/tutorials/platform/install-delegate/
- CI Tutorials: https://developer.harness.io/docs/continuous-integration/get-started/tutorials
- CD K8s Manifest: https://developer.harness.io/tutorials/cd-pipelines/kubernetes/manifest
