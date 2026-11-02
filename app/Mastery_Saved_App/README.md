# Mastery (Offline Study & Academic Intelligence Platform)
## Production Distribution Package

### Overview
**Mastery** is a 100% offline-ready, enterprise-grade Android study suite designed for comprehensive academic preparation (including Ethiopian University Entrance Examination - EUEE and multi-subject high-school curriculum).

---

### App Files in this Folder
- `Mastery.apk`: Complete compiled debug APK with all offline libraries, Room SQLite database, and resources (Ready for direct sideloading or installation via ADB/File Manager).
- `EUEE-Mastery.apk`: Direct alias of the current production build for companion server distribution.
- `RELEASE_INFO.json`: Metadata specifications and verification hashes.

---

### Key Capabilities & Architectural Highlights
1. **100% Local & Offline**:
   - Zero external cloud or internet dependencies.
   - Fully local Room SQLite persistence with custom pre-seeded and custom-imported question banks.
2. **Crash-Resilient Streaming JSON Engine**:
   - Multi-megabyte question ingestion streams through `JsonReader` token-by-token directly into Room on `Dispatchers.IO` in batches of 50.
   - `android:largeHeap="true"` enabled in AndroidManifest to safeguard against memory pressure on budget and flagship devices alike.
   - Supports diverse JSON structures: `prompt` / `question` / `text`, `optionA..D` or `options`/`choices`, `answers` or `correctOptionIndex`.
3. **Clean Presentation Architecture**:
   - Icon-only 4-tab bottom navigation bar (`Home`, `MCQ Practice`, `Timetable`, `Assistant`).
   - File Staging Card replacing raw JSON text display during import.
   - Real-time animated progress bars and live telemetry.
4. **Offline Science & Math Toolkits**:
   - 2D Canvas graphing calculator for equations, polynomials, trigonometric, and logarithmic curves.
   - Interactive Periodic Table of 118 chemical elements with valence, molar mass, and electron configurations.
   - SM-2 Anki-style spaced repetition flashcard trainer.
   - Timetable planner with Android `AlarmManager` integration and custom audio study alerts.
5. **Universal Android OS Support**:
   - Verified across Android 8.0 (API 26) up to Android 14+ (API 34+).
   - Safe `ActivityResultContracts.GetContent` document pickers.

---

### How to Install
1. Transfer `Mastery.apk` to any Android phone or tablet via USB, Bluetooth, or download via the web companion.
2. Open your device's **Files** or **Downloads** app.
3. Tap `Mastery.apk` and select **Install** (allow "Install unknown apps" if prompted).
4. Launch **Mastery** and enjoy a complete, distraction-free study environment!
