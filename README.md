# Harness Enterprise SE Candidate Lab

## Overview

This repo contains all artifacts for the Harness Enterprise SE Candidate Lab, demonstrating:

- **CI**: Build, test, and push a Java Spring Boot app to DockerHub
- **CD**: Deploy to Kubernetes using a **Canary release strategy**
- **Bonus**: Reusable step templates (templatization)

---

## Project Structure

```
harness/
├── app/                        # Java Spring Boot application
│   ├── src/main/...            # Application source
│   ├── src/test/...            # JUnit tests
│   ├── Dockerfile              # Multi-stage Docker build
│   └── pom.xml                 # Maven build descriptor
├── k8s/                        # Kubernetes manifests
│   ├── namespace.yaml
│   ├── deployment.yaml         # Stable deployment (uses Harness artifact expression)
│   └── service.yaml
└── harness/                    # Harness pipeline configs (importable)
    ├── pipeline.yaml           # Full CI + CD pipeline
    ├── maven-step-template.yaml   # Reusable Maven build/test step
    └── step-template-docker-push.yaml  # Reusable Docker build+push step
```

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Docker | 24+ | Must be running |
| minikube | v1.38+ | `brew install minikube` |
| kubectl | v1.32+ | Included with Docker Desktop |
| Java | 17 | For local dev only |
| Maven | 3.9+ | For local dev only |

---

## Step 1: Kubernetes Cluster Setup

```bash
# Start minikube
minikube start --cpus=4 --memory=8192

# Verify cluster
kubectl get nodes
kubectl cluster-info
```

---

## Step 2: Harness Trial & Delegate Install

1. Sign up at https://app.harness.io/auth/#/signup
2. After login, navigate to **Account Settings → Delegates**
3. Click **+ New Delegate** → select **Kubernetes**
4. Download the generated `harness-delegate.yml`
5. Apply it to your cluster:

```bash
kubectl apply -f harness-delegate.yml
kubectl get pods -n harness-delegate   # wait for Running status
```

> **Tip:** The delegate must show **Connected** in the Harness UI before proceeding.

---

## Step 3: Harness CI Pipeline

### Connectors to create first (Account Settings → Connectors)

| Connector | Type | Details |
|-----------|------|---------|
| `dockerhub_connector` | Docker Registry | hub.docker.com + your credentials |
| `github_connector` | GitHub | This repo, with OAuth or PAT |
| `k8s_delegate_connector` | Kubernetes | Use the delegate selector |

### Pipeline Setup

1. Go to **Pipelines → + Create Pipeline** → import from YAML
2. Paste contents of `harness/pipeline.yaml`
3. Set the `dockerhub_username` variable to your DockerHub username

### What the CI stage does

```
Clone Repo → Maven Build & Test (JUnit reports) → Docker Build & Push to DockerHub
```

The **Maven Test Step** and **Docker Build & Push** are both **templatized steps** (bonus).

---

## Step 4: Harness CD – Canary Deployment

### Service setup

- **Deployment type**: Kubernetes
- **Manifest source**: Git → `k8s/` folder in this repo
- **Artifact**: DockerHub → `<your-username>/harness-demo:<pipeline.sequenceId>`

### Infrastructure

- **Environment**: `production`
- **Infrastructure**: Kubernetes (Direct) → namespace `harness-demo`
- **Connector**: `k8s_delegate_connector`

### Canary strategy (in pipeline.yaml)

```
K8sCanaryDeploy (25%) → Approval Gate → K8sCanaryDelete → K8sRollingDeploy
```

On failure → automatic rollback via `K8sCanaryDelete` + `K8sRollingRollback`

Apply the namespace before first run:

```bash
kubectl apply -f k8s/namespace.yaml
```

---

## Step 5: Bonus – Templatization

Two step templates are defined:

### `Maven_Test_Step` (v1.0)
- Runs `mvn clean verify` inside a Maven container
- Publishes JUnit test reports back to Harness
- Referenced in the CI stage via `templateRef: Maven_Test_Step`

### `Docker_Build_and_Push` (v1.0)
- Wraps the `BuildAndPushDockerRegistry` step
- Accepts `connectorRef`, `repo`, and `tags` as runtime inputs
- Referenced in the CI stage via `templateRef: Docker_Build_and_Push`

To create templates in Harness:
1. Go to **Account/Org/Project → Templates → + New Template → Step**
2. Paste YAML from `harness/maven-step-template.yaml` or `harness/step-template-docker-push.yaml`
3. Save and publish the version

---

## Running Locally (optional)

```bash
cd app

# Build
mvn clean package -DskipTests

# Run tests
mvn test

# Build Docker image
docker build -t harness-demo:local .

# Run locally
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
