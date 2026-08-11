# Snippet Match API demo (GitHub Actions)

Demonstrate **developer → pull request → Black Duck snippet scan → license risk comment**.

When a PR introduces source that matches known open source in the Black Duck KnowledgeBase, GitHub Actions:

1. Authenticates to your Black Duck server.
2. Sends each changed source file to `POST /api/snippet-matching`.
3. Posts (or updates) a PR comment: **Snippet License Analysis Results**.

This is the same flow that produced a comment like:

> **File:** `src/main/java/DeserializationTask.java`  
> - Project: WebGoat, Version: v2023.0  
> - License Name: GNU General Public License v2.0 or later  
> - License Type: `RECIPROCAL`

---

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| Black Duck SCA | Server licensed for **Generative AI Snippet Scanning** (Snippet Matching REST API) |
| API token | Black Duck user token with permission to call the snippet API |
| GitHub repo | Actions enabled; this workflow checked in under `.github/workflows/` |
Classic Signature Scanner `--snippet-matching` is a **different** path. This demo uses the **REST Snippet Matching API** only.

---

## Repository configuration

**Settings → Secrets and variables → Actions**

### Variable

| Name | Example | Description |
|------|---------|-------------|
| `HOSTNAME` | `sca.field-test.blackduck.com` **or** `https://sca.field-test.blackduck.com` | Black Duck host or full base URL (scheme optional) |

### Optional variable

| Name | Default | Description |
|------|---------|-------------|
| `MAX_SNIPPET_FILES` | `10` | Max source files scanned per PR |

### Secret

| Name | Description |
|------|-------------|
| `BLACK_DUCK_API_TOKEN` | Black Duck API token |

No personal GitHub PAT is required. The workflow uses `GITHUB_TOKEN` for PR comments.

---

## Run the demo

```bash
git checkout main
git pull
git checkout -b demo/gpl-snippet

# Simulate a developer pasting / generating code that matches OSS (GPL)
cp demo/fixtures/DeserializationTask.java src/main/java/DeserializationTask.java

git add src/main/java/DeserializationTask.java
git commit -m "Add deserialization helper"
git push -u origin HEAD
```

Open a pull request into `main`, then open the **Conversation** tab.

### Expected result

- Workflow **Snippet Analysis** succeeds.
- A bot comment titled **Snippet License Analysis Results** lists project, version, license name, and license type.
- Risky families (`RECIPROCAL`, `RECIPROCAL_AGPL`, `RECIPROCAL_NETWORK`, `WEAK_RECIPROCAL`) are highlighted.
- Re-pushing to the same PR **updates** the same comment (no spam).
- The Action does **not** commit analysis files onto your branch.

More detail: [demo/README.md](demo/README.md).

---

## How the workflow works

File: [`.github/workflows/snippet-analysis.yml`](.github/workflows/snippet-analysis.yml)

```text
pull_request (opened | reopened | synchronize)
  → checkout PR head
  → list changed files (added/modified source only)
  → POST /api/tokens/authenticate
  → POST /api/snippet-matching  (raw file body, text/plain)
  → create/update PR comment
```

**Scanned extensions (demo set):**  
`.java`, `.c/.cpp/.h`, `.cs`, `.go`, `.js/.ts`, `.py`, `.rb`, `.php`, `.swift`, `.kt`, `.scala`, `.rs`, and a few others — see the workflow regex.

**Not scanned:** deleted files, non-source paths (Markdown, workflow YAML, etc.).

---

## License type legend

Matches are grouped by Black Duck license family, including:

| Type | Typical risk signal |
|------|---------------------|
| `PERMISSIVE` | Often lower obligation (e.g. MIT, Apache) |
| `WEAK_RECIPROCAL` | Weak copyleft (e.g. LGPL-style) |
| `RECIPROCAL` | Strong copyleft (e.g. **GPL**) — demo callout |
| `RECIPROCAL_AGPL` / `RECIPROCAL_NETWORK` | Network/stronger copyleft |
| `UNKNOWN` | Review manually |

Snippet matching is a heuristic; treat results as **review guidance**, not automatic legal conclusions.

---

## Troubleshooting

| Symptom | What to check |
|---------|----------------|
| Auth failure (HTTP 401/403) | Token validity; user permissions; `HOSTNAME` correct |
| Empty matches | Code may not match KB; try `demo/fixtures/DeserializationTask.java`; ensure enough unique lines |
| “Generative AI Compliance” / feature errors | Server registration must include Snippet Matching API capability |
| Comment missing | Workflow permissions: `pull-requests: write`; Actions allowed for the repo |
| Double `https://` | Prefer host-only `HOSTNAME` (`sca.example.com`); full URLs are also accepted |

---

## Repository layout

```text
.github/workflows/snippet-analysis.yml   # PR scan + comment + SARIF
demo/fixtures/                           # Known GPL-matching sample
src/main/java/HelloWorld.java            # Clean baseline on main
pom.xml                                  # Minimal Java project metadata
```

---

## API reference (Black Duck)

- `POST /api/tokens/authenticate` — exchange API token for bearer token  
- `POST /api/snippet-matching` — body = raw source (`Content-Type: text/plain`); response `snippetMatches` keyed by license family  

Product docs also cover classic Signature Scanner snippet modes; this repository intentionally uses only the REST API for a fast PR-time demo.
