# Harness SE Interview Talk Track

## Opening (30 seconds)

"I built a full CI/CD pipeline using Harness to deploy a Java Spring Boot application to Kubernetes.
The pipeline covers the entire software delivery lifecycle — build, test, containerize, and deploy
using a canary release strategy with a manual approval gate. I also templatized key steps to
demonstrate how Harness enables reuse and consistency at scale."

---

## 1. The Application

"The app itself is intentionally simple — a Spring Boot REST API with two endpoints:
- `GET /` returns a JSON greeting and version
- `GET /health` is a Kubernetes health check probe

The point isn't the app — it's the delivery pipeline around it."

---

## 2. Kubernetes & The Delegate

**What to say:**
"Before any pipeline runs, you need a Harness Delegate — a lightweight agent pod that runs
inside your Kubernetes cluster. I installed it via Helm into minikube.

The delegate connects *outbound* to Harness — so there are no inbound firewall rules,
no VPN required. This is huge for enterprise customers who can't expose their clusters to the internet."

**Key points:**
- Delegate = execution engine inside the customer's environment
- Harness cloud sends tasks to the delegate, delegate executes locally
- Works behind firewalls, air-gapped environments, on-prem

**Talking point for customers:**
"Most CI/CD tools require you to open inbound ports or give them network access to your cluster.
Harness flips this — the delegate reaches out, so your security team is happy."

---

## 3. Connectors

**What to say:**
"I configured three connectors — GitHub, DockerHub, and Kubernetes. Connectors are reusable
credential abstractions. You define them once and reference them across every pipeline in the project."

| Connector | Purpose |
|---|---|
| `github_connector` | Clones source code and fetches K8s manifests |
| `dockerhub_connector` | Pushes built images to DockerHub |
| `k8s_connector` | Applies manifests to the Kubernetes cluster |

**Key insight:**
"The Kubernetes connector uses `InheritFromDelegate` — it borrows the delegate's cluster
credentials instead of storing a separate kubeconfig. In production this would point to EKS, GKE,
or AKS instead of minikube."

---

## 4. CI Stage — Build and Test

**What to say:**
"The CI stage does three things: compile and test the app with Maven, then build and push a
Docker image to DockerHub. The Dockerfile uses a multi-stage build — a heavy Maven image
for building, and a lightweight JRE image for the final artifact. This keeps the production
image small and secure."

**Walk through the pipeline:**
```
Clone repo (main branch)
  → Maven build & test (JUnit reports published to Harness)
  → Docker build & push (raphaelokere/harness-demo:latest)
```

**Key Harness feature — test intelligence:**
"Harness can also do Test Intelligence — it identifies which tests are affected by a code change
and only runs those, dramatically reducing CI time. That's a value-add conversation for any
engineering team running thousands of tests."

---

## 5. CD Stage — Canary Deployment

**What to say:**
"The CD stage deploys to Kubernetes using a canary strategy. Here's why that matters:
instead of replacing all instances at once, we deploy the new version to 25% of pods first.
If something goes wrong, only 25% of users are affected — not everyone."

**Walk through the flow:**
```
Fetch K8s manifests from GitHub
  → Canary Deploy (25% of replicas)
  → Manual Approval Gate
  → Canary Delete
  → Full Rolling Deploy (100%)
```

**The approval gate:**
"The approval gate is where a human — a release manager, QA lead, or on-call engineer —
reviews the canary deployment before promoting to full rollout. This is a governance control
that regulated industries like finance and healthcare require."

**Automatic rollback:**
"If anything fails, Harness automatically runs the rollback steps — `K8sCanaryDelete` and
`K8sRollingRollback`. No manual intervention required."

---

## 6. Templatization (Bonus)

**What to say:**
"I templatized two steps — the Maven build and the Docker push. This is one of Harness's
biggest differentiators for enterprise customers."

**How it works:**
- A platform/DevOps team defines the approved, standardized way to build or push
- Application teams reference the template — they don't need to know the implementation details
- If the platform team updates the template (e.g. adds a security scan), every pipeline picks it up automatically
- Version labels (`1.0`) allow gradual rollouts — existing pipelines stay on `1.0` until they opt into `2.0`

**What makes a template truly reusable:**
"Right now the Maven template has some hardcoded values — the Maven image version and working
directory. To make it production-grade, those would become `<+input>` fields with
`allowedValues()` to constrain teams to approved base images. That's the balance Harness enables:
**flexibility with guardrails**."

**The enterprise pitch:**
"Imagine 200 engineering teams all building Java services. Without templates, each team
copy-pastes pipeline YAML and diverges. With Harness templates, the platform team defines
the standard once. Compliance, security scanning, artifact signing — baked in, enforced
everywhere, updated in one place."

---

## 7. Value Proposition Summary

| Pain Point | Harness Solution |
|---|---|
| Brittle Jenkins pipelines, lots of copy-paste | Templates — define once, reuse everywhere |
| Risky big-bang deployments | Canary strategy with approval gates |
| No visibility into what deployed where | Full audit trail, pipeline execution history |
| Opening cluster to CI/CD tools | Delegate architecture — outbound only |
| Slow CI due to running all tests | Test Intelligence — only run affected tests |
| Different teams doing things differently | RBAC + templates enforce standards at scale |

---

## Anticipated Questions

**Q: Why Harness over GitHub Actions or Jenkins?**
"GitHub Actions is great for simple CI but lacks enterprise CD capabilities — canary deployments,
approval gates, rollback strategies, and governance are bolt-ons, not first-class features.
Jenkins requires significant maintenance overhead and plugin management. Harness is purpose-built
for the full delivery lifecycle with enterprise controls out of the box."

**Q: How does this scale to hundreds of services?**
"Templates and RBAC. Platform teams own the templates, app teams consume them. Each team
gets a project with scoped permissions. The delegate architecture means one delegate can serve
an entire cluster — you don't need per-team infrastructure."

**Q: What about security?**
"Harness has built-in secrets management, RBAC at account/org/project level, full audit trails,
and the delegate model means credentials never leave the customer's environment. For regulated
industries, that's often a hard requirement."

**Q: What would you do differently in production?**
"A few things: use `<+pipeline.sequenceId>` or a Git SHA as the image tag instead of `latest`
for full traceability. Add Harness Test Intelligence to speed up CI. Use OPA policies to enforce
governance rules. And add a monitoring/verification step in the canary phase — Harness integrates
with Prometheus, Datadog, and New Relic to automatically verify canary health before approval."

---

## Closing

"The lab demonstrates the core Harness value prop in a concrete, working example —
fast, reliable, governed software delivery. The real conversation with a customer would be
mapping this to their specific stack, compliance requirements, and scale. That's where the
SE role gets interesting."
