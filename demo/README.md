# Demo fixtures

Files in `fixtures/` are **known open-source samples** used to demonstrate the
Black Duck Snippet Matching API on a pull request.

## Recommended walkthrough

```bash
git checkout main
git pull
git checkout -b demo/gpl-snippet

# Stage "developer introduces OSS-like code"
cp demo/fixtures/DeserializationTask.java src/main/java/DeserializationTask.java

git add src/main/java/DeserializationTask.java
git commit -m "Add deserialization helper (demo)"
git push -u origin demo/gpl-snippet
```

Open a pull request into `main`. Within a minute or two you should see a bot
comment titled **Snippet License Analysis Results**, similar to:

- **Project:** WebGoat, **Version:** v2023.0  
- **License Name:** GNU General Public License v2.0 or later  
- **License Type:** `RECIPROCAL`

