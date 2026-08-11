# Demo fixtures

Files in `fixtures/` are **known open-source samples** used to demonstrate the
Black Duck Snippet Matching API on a pull request.

They stay under `demo/` on `main` so the baseline tree stays clean. Copy a
fixture into `src/` on a feature branch to simulate a developer introducing
OSS-like code.

## Recommended walkthrough

```bash
git checkout main
git pull
git checkout -b demo/snippet-license-run

# Stage "developer introduces OSS-like code"
cp demo/fixtures/DeserializationTask.java src/main/java/DeserializationTask.java

git add src/main/java/DeserializationTask.java
git commit -m "Add deserialization helper (demo)"
git push -u origin HEAD
```

Open a pull request into `main`. Within about a minute you should see a bot
comment titled **Snippet License Analysis Results**, similar to:

- **Project:** WebGoat, **Version:** v2023.0  
- **License Name:** GNU General Public License v2.0 or later  
- **License Type:** `RECIPROCAL`

Only the **Snippet Analysis** workflow should run (no CodeQL).
