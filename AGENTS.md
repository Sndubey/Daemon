# Guidelines & Context for AI Agents (AGENTS.md)

This document contains critical rules, architectural design patterns, security parameters, and data persistence guidelines that every AI agent working on this codebase **MUST** strictly follow.

---

## 1. Core Principles & Privacy Guarantee
* **Personal Use Only**: MyHub is built exclusively for personal productivity (To-Dos, Goals, Notes, and Timetables).
* **Offline-First & Sandbox Privacy**: The app has **NO INTERNET PERMISSION** declared in `AndroidManifest.xml`. Data must never leave the local device storage under any circumstance.

---

## 2. Note Security & Permanent Hardcoded Password
* **Hardcoded Permanent Password**: `986727`
* **Rules**:
  1. The password for locked notes is fixed to **`986727`** in `NoteRepository.HARDCODED_LOCK_PASSWORD`.
  2. **DO NOT** prompt the user to set, change, or reset a password in the UI.
  3. Locked notes must obscure their contents until the user enters `986727` in the unlock dialog.
  4. Only developers/agents modifying the source code directly can change this password.

---

## 3. Strict Color Palette & Design System
All UI screens **MUST** strictly adhere to the custom dark color system:
* **Background Color**: `#0F1117`
* **Surface / Card Color**: `#1C2330`
* **Surface Variant / Divider Color**: `#263042`
* **Accent / Primary Action Color**: `#8AB4F8`
* **Primary Text Color**: `#E3E2E6`
* **Secondary Text Color**: `#9AA0A6`

---

## 4. Data Persistence & Migration Rules
* **Room Database**: All user data is persisted locally in Room SQLite database `myhub_main_database`.
* **Zero Data Loss Rule**: User To-Dos, Goals, Notes, and Timetables **MUST NOT** be wiped or lost when redeploying the app or adding new features.
* **Current schema version**: 6 (`MIGRATION_3_4` added the `timetables` and `timetable_entries` tables; `MIGRATION_4_5` added `todos.sortOrder` for drag-to-reorder; `MIGRATION_5_6` added `notes.isHidden`; `MIGRATION_6_7` added `notes.isPinned`). `exportSchema` is off, so verify new migration SQL against the generated `AppDatabase_Impl` `createAllTables` before shipping.
* **Migration Policy**:
  - Do NOT call `.fallbackToDestructiveMigration()` in `AppDatabase.kt`.
  - When changing database schema in future updates, increment database `version` and provide explicit Room `Migration` paths to retain user content.

---

## 5. Screen Structure & Navigation
1. **Screen 1: Todo & Goals (`Screen.Todo`)**:
   - Greeting header ("Good Morning / Afternoon / Evening, Let's get things done ✨").
   - Quick To-Do input bar on top.
   - "Today" list with checkmark toggle and item count.
   - "Goals" section with icon cards, descriptions, and progress percentages.
2. **Screen 2: Notes (`Screen.Notes`)**:
   - Google Keep style staggered 2-column note cards.
   - Note locking feature backed by password `986727`.
   - Pinned notes are listed first under a "Pinned" heading (others under "Others"). Pin/unpin via card long-press menu or the editor's pin icon.
   - Hidden notes: any note (locked or not) can be hidden. Hidden notes only appear after **holding the "+" button for 1.7 seconds** (`HIDDEN_NOTES_HOLD_MILLIS`). This gesture is a secret: **never mention it anywhere in the UI** (no hints, snackbars, empty states or tooltips). Opening a hidden locked note still requires the password. The hidden view closes when leaving the tab or the app.
3. **Screen 3: Timetable (`Screen.Timetable`)**:
   - Multiple named weekly timetables, all shown fully expanded in one scrolling list (no tap-to-open).
   - Each timetable lists Sunday → Saturday: weekday name on the left, that day's tasks on the right; today's row is highlighted.
4. **Bottom Navigation**:
   - Exactly 3 navigation items: **Todo**, **Notes**, and **Timetable**.
   - Circular accent blue background pill (`#8AB4F8`) for the active tab icon.
