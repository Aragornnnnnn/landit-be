# Deepgram Aura 2 TTS Voice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one active OpenRouter Deepgram Aura 2 male voice record to `tts_voice`.

**Architecture:** Add a versioned Flyway data migration only. Keep the existing `TtsVoice` entity, API contracts, and uniqueness constraint unchanged.

**Tech Stack:** Java 21, Spring Boot 4, Gradle, Flyway, PostgreSQL/H2 integration tests.

## Global Constraints

- Use the exact provider, model, provider voice ID, gender, and accent locale supplied by the user.
- Store `description` as `굵은 남성 음성` and `status` as `ACTIVE`.
- Use `CURRENT_TIMESTAMP` for `created_at` and `updated_at`.
- Do not change the entity or public API.

---

### Task 1: Add the Aura 2 seed regression test

**Files:**
- Modify: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`

**Interfaces:**
- Consumes: the migrated `tts_voice` table.
- Produces: a test proving the exact Aura 2 seed row exists with all requested attributes.

- [ ] **Step 1: Write the failing test**

Add a test that queries `tts_voice` for `provider = 'OPENROUTER'`, `model = 'deepgram/aura-2'`, and `provider_voice_id = 'aura-2-orpheus-en'`, then asserts one row with `MALE`, `굵은 남성 음성`, `EN_US`, and `ACTIVE`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests --no-daemon`

Expected: the new assertion fails because the Aura 2 row is not yet present.

### Task 2: Add the Flyway migration

**Files:**
- Create: `src/main/resources/db/migration/V38__add_deepgram_aura_2_tts_voice.sql`

**Interfaces:**
- Consumes: the existing `tts_voice` schema and unique constraint.
- Produces: one active Aura 2 `tts_voice` row.

- [ ] **Step 1: Write the minimal migration**

Insert one row with the exact values below and `CURRENT_TIMESTAMP` for both timestamps:

```sql
INSERT INTO tts_voice (
    provider,
    model,
    provider_voice_id,
    gender,
    description,
    accent_locale,
    status,
    created_at,
    updated_at
)
VALUES (
    'OPENROUTER',
    'deepgram/aura-2',
    'aura-2-orpheus-en',
    'MALE',
    '굵은 남성 음성',
    'EN_US',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

- [ ] **Step 2: Run the focused test**

Run: `./gradlew test --tests com.landit.landitbe.DatabaseSchemaIntegrationTests --no-daemon`

Expected: the Aura 2 seed assertion passes.

### Task 3: Run repository verification and commit

**Files:**
- Verify: `docs/tasks/LAN-261/design.md`
- Verify: `docs/superpowers/plans/2026-08-06-tts-voice-aura-2.md`
- Verify: `src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java`
- Verify: `src/main/resources/db/migration/V38__add_deepgram_aura_2_tts_voice.sql`

- [ ] **Step 1: Run the full check**

Run: `./gradlew check --rerun-tasks --no-daemon`

Expected: Gradle exits with code 0.

- [ ] **Step 2: Check the diff**

Run: `git diff --check`.

Expected: no whitespace errors.

- [ ] **Step 3: Commit the logical change**

Run:

```bash
git add docs/tasks/LAN-261/design.md docs/superpowers/plans/2026-08-06-tts-voice-aura-2.md src/test/java/com/landit/landitbe/DatabaseSchemaIntegrationTests.java src/main/resources/db/migration/V38__add_deepgram_aura_2_tts_voice.sql
git commit -m "feat: add Deepgram Aura 2 TTS voice"
```

Expected: one commit containing the design, test, and migration.
