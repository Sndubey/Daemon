# Guidelines & Context for AI Agents (AGENTS.md)

This document contains critical rules, architectural design patterns, security parameters, and data persistence guidelines that every AI agent working on this codebase **MUST** strictly follow.

---

## 1. Core Principles & Privacy Guarantee
* **Personal Use Only**: MyHub is built exclusively for personal productivity (To-Dos, Goals, and Notes).
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
* **Zero Data Loss Rule**: User To-Dos, Goals, and Notes **MUST NOT** be wiped or lost when redeploying the app or adding new features.
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
3. **Bottom Navigation**:
   - Exactly 2 navigation items: **Todo** and **Notes**.
   - Circular accent blue background pill (`#8AB4F8`) for the active tab icon.
