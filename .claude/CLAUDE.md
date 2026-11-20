# CLAUDE.md

JetBrains plugin for Rider (Kotlin + Gradle) that manages IDE layouts as **profiles**. Context and decisions: `docs/PLAN.md`.

## Working style
- Language: project artifacts (code, comments, docs, commit messages) are in English. Conversation with the user is in German.
- Smallest working solution, no speculative abstractions.
- No code before the design in `docs/superpowers/specs/` is approved.

## Knowledge base (mandatory)
Record new findings in `docs/knowledge/` **immediately**, **organized by topic**. Do not let them get lost in chat answers, `docs/PLAN.md` or commit messages.

Structure:
```
docs/knowledge/
  <topic>/            # one folder per topic, kebab-case
    <subtopic>.md     # one file per self-contained finding
  README.md           # index: one entry per file (link + one line)
```

Topic folders (create new ones if needed, but check first whether an existing one fits):
- `intellij-platform/` – APIs and behavior of the IntelliJ Platform (ToolWindowManager, WindowManager, layout persistence, services, actions)
- `rider/` – Rider specifics (frontend/backend split, `Window > Layouts`)
- `profile/` – design and decisions for profile management (data model, storage, format, usage)
- `build/` – Gradle, IntelliJ Platform Gradle Plugin, versions, `runIde`, signing/publishing
- `testing/` – test strategy, sandbox findings

Rules:
1. **Before writing**, check `docs/knowledge/README.md` and the matching topic folder. Update an existing file instead of creating a duplicate.
2. Per file: heading, finding in a few sentences, **source/evidence** (doc link, class, test), date (`YYYY-MM-DD`). Mark anything uncertain as "unverified".
3. New or changed file → add an entry to `docs/knowledge/README.md`.
4. Correct or delete findings that have become wrong, do not leave them standing.
5. `docs/PLAN.md` stays the short overview (goal, decisions, next steps) and points to `docs/knowledge/`. Details belong there.
6. Record only what is not already evident from the code or the git history.
