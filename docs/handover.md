# Rapunzel — Session Handover Document
## Single Source of Truth for Multi-Session Development

> **RULE:** Every session starts by reading this file. Every session ends by updating this file. No exceptions.

---

## 1. Project Identity

| Field | Value |
|-------|-------|
| **Repository** | `https://github.com/Zapier-codes/Rapunzel` |
| **Base Project** | Emaki (Aatricks/Emaki) — rebrand in progress |
| **Target Name** | Rapunzel (configurable via `RAPUNZEL_APP_NAME` secret) |
| **Package** | `io.aatricks.easyreader` (legacy, to be made dynamic) |
| **Application ID** | `io.aatricks.novelscraper` (legacy, to be made dynamic) |
| **Architecture Doc** | `docs/ARCHITECTURE.md` |
| **This Doc** | `docs/handover.md` |

---

## 2. Session Protocol

### 2.1 At Session Start (MANDATORY)

1. Read this `handover.md` top to bottom
2. Check the **Current State** section (§3) for the last completed phase
3. Check the **Next Session Pointer** (§5) for what to work on
4. Check the **Known Issues** section (§6) for blockers
5. Run `./gradlew :app:assembleStandardDebug` to verify the repo builds
6. Begin work

### 2.2 At Session End (MANDATORY)

1. Ensure code compiles: `./gradlew :app:assembleStandardDebug`
2. Run tests: `./gradlew :app:testStandardDebugUnitTest`
3. Run detekt: `./gradlew :app:detekt`
4. Update this `handover.md`:
   - Add entry to **Completed Work Log** (§4)
   - Update **Current State** (§3)
   - Write **Next Session Pointer** (§5)
   - Update **Known Issues** (§6) if any
5. Stage all changes: `git add -A`
6. Commit: `git commit -m "<phase>: <description>"`
7. Generate patch: `git diff HEAD~1 > session-N.patch` (or `git format-patch -1`)
8. Provide the patch file to the user for download + push

### 2.3 Patch File Convention

```bash
# Generate patch from last commit
git format-patch -1 HEAD --stdout > session-NNN.patch

# Or if multiple commits
git format-patch HEAD~N --stdout > session-NNN.patch
```

**Naming:** `session-001.patch`, `session-002.patch`, etc.  
**Delivery:** Save to `/mnt/agents/output/` and provide download link.

---

## 3. Current State Tracker

### 3.1 Overall Progress

```
Phase 0: Foundation & Rebrand        [░░░░░░░░░░] 0%  ← CURRENT
Phase 1: Config & CI Hardening       [░░░░░░░░░░] 0%
Phase 2: Pawns SDK                   [░░░░░░░░░░] 0%
Phase 3: Wattpad Integration         [░░░░░░░░░░] 0%
Phase 4: Royal Road Integration      [░░░░░░░░░░] 0%
Phase 5: Inkitt Integration          [░░░░░░░░░░] 0%
Phase 6: Supabase Backend            [░░░░░░░░░░] 0%
Phase 7: Plugin System               [░░░░░░░░░░] 0%
Phase 8: AI RAG                      [░░░░░░░░░░] 0%
Phase 9: EPUB Export                 [░░░░░░░░░░] 0%
Phase 10: Polish & Launch            [░░░░░░░░░░] 0%
```

### 3.2 What Works Today (Baseline)

| Feature | Status | Notes |
|---------|--------|-------|
| Local EPUB reader | ✅ Working | Full parser, cover extraction |
| Local PDF reader | ✅ Working | PdfRenderer, page cache |
| Local TXT reader | ✅ Working | Chapter detection, encoding fallback |
| Local CBZ reader | ✅ Working | Zip-of-images, cover thumbnail |
| Web scrapers (4) | ✅ Working | Asura, MangaBat, NovelFire, Novelight |
| Room library | ✅ Working | Books, progress, annotations |
| Compose UI | ✅ Working | Library, reader, settings, explore |
| AI summarization | ✅ Working | Extractive, AI flavor only |
| Chapter download | ✅ Working | WorkManager queue |
| File import | ✅ Working | System picker + "Open with" |
| CI (basic) | ✅ Working | Lint, test, detekt, assembleDebug |

### 3.3 What Is Broken / Missing

| Feature | Status | Blocker |
|---------|--------|---------|
| Dynamic branding | ❌ Missing | Hardcoded strings everywhere |
| GitHub secrets CI | ❌ Missing | CI uses static config |
| Pawns SDK | ❌ Missing | Not integrated |
| Wattpad API | ❌ Missing | No client exists |
| Royal Road scraper | ❌ Missing | No source exists |
| Inkitt API | ❌ Missing | No client exists |
| Supabase sync | ❌ Missing | No backend connected |
| Plugin system | ❌ Partial | Interface exists, no registry |
| AI RAG Q&A | ❌ Missing | Only extractive summary |
| EPUB export | ❌ Missing | No exporter exists |
| Release CI | ❌ Missing | No signed release builds |

---

## 4. Completed Work Log

> **Format:** `Session #NNN | Date | Phase | Description | Commit SHA`

| Session | Date | Phase | Description | Commit |
|---------|------|-------|-------------|--------|
| — | — | — | *No sessions completed yet* | — |

---

## 5. Next Session Pointer

### 5.1 Immediate Next Task

**Phase 0: Foundation & Rebrand — Step 1**

Implement the dynamic `AppConfig` system and `BuildConfig` injection pipeline:

1. **Create `AppConfig.kt`** in `app/src/main/java/io/aatricks/easyreader/config/`
   - Singleton class exposing all dynamic config fields
   - Reads from `BuildConfig` fields generated at build time

2. **Update `app/build.gradle.kts`**
   - Add `buildConfigField` generation from `gradle.properties`
   - Read properties like `rapunzel.app.name`, `rapunzel.app.version`
   - Generate `APP_NAME`, `APP_VERSION`, `FEATURE_*` booleans

3. **Update `gradle.properties`**
   - Add placeholder properties for all config values
   - Document which ones come from GitHub secrets

4. **Replace hardcoded strings**
   - `strings.xml` app_name → reference `AppConfig.appName`
   - `settings.gradle.kts` root project name → keep as "Rapunzel"
   - Any "Emaki", "EasyReader", "NovelScraper" literals → use AppConfig

5. **Update CI workflow** (`rapunzel-ci.yml`)
   - Inject secrets into `gradle.properties` before build
   - Add aggressive Gradle caching
   - Dynamic release naming

### 5.2 Acceptance Criteria for Next Session

- [ ] `AppConfig.kt` exists and compiles
- [ ] `BuildConfig.APP_NAME` resolves correctly
- [ ] `./gradlew :app:assembleStandardDebug` succeeds
- [ ] `./gradlew :app:testStandardDebugUnitTest` passes
- [ ] `./gradlew :app:detekt` passes
- [ ] No hardcoded "Emaki" / "EasyReader" / "NovelScraper" in user-facing strings
- [ ] CI workflow file updated (may not run until pushed)
- [ ] This `handover.md` updated with session entry

### 5.3 Files Expected to Change

```
app/build.gradle.kts
gradle.properties
settings.gradle.kts
app/src/main/java/io/aatricks/easyreader/config/AppConfig.kt   (NEW)
app/src/main/res/values/strings.xml
.github/workflows/rapunzel-ci.yml                                (NEW or UPDATE)
docs/handover.md                                                 (THIS FILE)
```

---

## 6. Known Issues

| # | Issue | Severity | Workaround | Session Introduced |
|---|-------|----------|------------|-------------------|
| — | *No known issues yet* | — | — | — |

---

## 7. Configuration Reference

### 7.1 GitHub Secrets (Required Before Phase 1)

Set these in `Settings > Secrets and variables > Actions`:

```
RAPUNZEL_APP_NAME=Rapunzel
RAPUNZEL_APP_PACKAGE=io.aatricks.rapunzel
RAPUNZEL_APP_VERSION=1.0.0
RAPUNZEL_VERSION_CODE=100
RAPUNZEL_SUPABASE_URL=https://xxxx.supabase.co
RAPUNZEL_SUPABASE_ANON=eyJ...
RAPUNZEL_PAWNS_API_KEY=...
RAPUNZEL_FEAT_PAWNS=false
RAPUNZEL_FEAT_SUPABASE=false
RAPUNZEL_FEAT_WATTPAD=false
RAPUNZEL_FEAT_RR=false
RAPUNZEL_FEAT_INKITT=false
RAPUNZEL_FEAT_AI_RAG=false
RELEASE_KEYSTORE_B64=<base64-of-keystore>
RELEASE_KEYSTORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

### 7.2 Local Development (Without Secrets)

```bash
# Create local.properties for development
cat > local.properties << 'EOF'
rapunzel.app.name=RapunzelDev
rapunzel.app.version=0.0.1-dev
rapunzel.app.versionCode=1
rapunzel.supabase.url=
rapunzel.supabase.anon=
rapunzel.pawns.apiKey=
rapunzel.feat.pawns=false
rapunzel.feat.supabase=false
rapunzel.feat.wattpad=false
rapunzel.feat.rr=false
rapunzel.feat.inkitt=false
rapunzel.feat.ai.rag=false
EOF
```

### 7.3 Build Commands

```bash
# Debug build
./gradlew :app:assembleStandardDebug

# Run tests
./gradlew :app:testStandardDebugUnitTest

# Run detekt
./gradlew :app:detekt

# Run lint
./gradlew :app:lintStandardDebug

# Release build (requires keystore.properties)
./gradlew :app:assembleStandardRelease

# Full validation (CI path)
./gradlew :app:lintStandardDebug :app:testStandardDebugUnitTest :app:detekt :app:assembleStandardDebug

# Clean
./gradlew clean
```

---

## 8. Session Checklist (Copy-Paste at End)

```markdown
## Session #NNN — YYYY-MM-DD

### What was done:
- [ ] Task 1
- [ ] Task 2

### Files changed:
- `path/to/file.kt`

### Verification:
- [ ] `./gradlew :app:assembleStandardDebug` ✅
- [ ] `./gradlew :app:testStandardDebugUnitTest` ✅
- [ ] `./gradlew :app:detekt` ✅

### Commit:
```
git add -A
git commit -m "phase-X: description"
```

### Patch:
```
git format-patch -1 HEAD --stdout > session-NNN.patch
```

### Next Session Should:
- [ ] Next task description

### Known Issues:
- None / Issue description
```

---

## 9. Contact & Context

- **Architecture decisions:** See `docs/ARCHITECTURE.md`
- **Original blueprint:** See attached `Complete Architect.txt` and `Claude finished the.txt`
- **Repo origin:** `https://github.com/Zapier-codes/Rapunzel` (fork of Emaki)
- **Build tool:** Gradle 8.x + Kotlin DSL + Version Catalog
- **Min SDK:** 30 (Android 11)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 37

---

*Document Version: 1.0.0*  
*Last Updated: 2026-08-22*  
*Updated By: Session #000 (Initialization)*
