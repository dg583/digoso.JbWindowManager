# Gradle-Setup gegen Rider 2026.3

Stand: 2026-11-19 · Beleg: erfolgreicher Lauf `./gradlew test buildPlugin` (Smoke-Test 1/1 grün, `build/distributions/LayoutManager-0.1.0.zip`).

## Bestätigte Werkzeug-Versionen
| Was | Version | Anmerkung |
|---|---|---|
| Rider (Ziel) | 2026.3, Build 263.7307.258 | `sinceBuild = 263` |
| IntelliJ Platform Gradle Plugin | **2.20.1** | 2.10.2 scheitert: kennt das Attribut `namespace` in den Modulbeschreibungen von 2026.3 nicht |
| Kotlin | **2.4.0** | 2.1.20 scheitert: Rider 2026.3 liefert Kotlin-Metadaten 2.4.0 |
| Gradle | 9.8.0 | Wrapper erzeugt, lief mit JDK 21 |
| JDK | Temurin 21 (`~/.jdks/jdk-21.0.13+9`) | als `JAVA_HOME` für Gradle gesetzt; `jvmToolchain(21)` |

## Eigenheiten
- **Rider als Plattform:** `useInstaller = true` wird für Rider nicht unterstützt, daher `useInstaller = false` (Plugin-Issue 1852).
- **Lokale Rider statt Download:** Die Property `riderPath` in `~/.gradle/gradle.properties` (pro Maschine, nicht im Repo) zeigt auf die installierte Rider. Ohne die Property lädt der Build Rider 2026.3 herunter. Beispiel: `riderPath=C:/Users/<name>/AppData/Local/Programs/Rider 2`.
- **Tests:** `testFramework(TestFrameworkType.Platform)` plus `junit:junit:4.13.2` funktioniert.
- **Kotlin-Warnung im Log:** Beim Kompilieren erschienen Meldungen zu `kotlin_module`-Metadaten der Plattform-JARs (nur bei zu altem Kotlin). Mit 2.4.0 weg.
- **Windows/Bash:** `python3` im Git-Bash hängt (Store-Stub), nicht für Skripte verwenden.
