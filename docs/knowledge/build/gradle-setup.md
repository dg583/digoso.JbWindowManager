# Gradle setup against Rider 2026.3

As of 2026-11-19 · Evidence: successful run of `./gradlew test buildPlugin` (smoke test 1/1 green at the time, `build/distributions/digoso.JbWindowManager-0.1.0.zip`; the smoke test has since been removed).

## Confirmed tool versions
| What | Version | Note |
|---|---|---|
| Rider (target) | 2026.3, build 263.7307.258 | `sinceBuild = 263` |
| IntelliJ Platform Gradle Plugin | **2.20.1** | 2.10.2 fails: it does not know the `namespace` attribute in the 2026.3 module descriptors |
| Kotlin | **2.4.0** | 2.1.20 fails: Rider 2026.3 ships Kotlin metadata 2.4.0 |
| Gradle | 9.8.0 | wrapper generated, ran with JDK 21 |
| JDK | Temurin 21 (`~/.jdks/jdk-21.0.13+9`) | set as `JAVA_HOME` for Gradle; `jvmToolchain(21)` |

## Quirks
- **Rider as platform:** `useInstaller = true` is not supported for Rider, hence `useInstaller = false` (plugin issue 1852).
- **Local Rider instead of download:** the property `riderPath` in `~/.gradle/gradle.properties` (per machine, not in the repo) points at the installed Rider. Without it the build downloads Rider 2026.3. Example: `riderPath=C:/Users/<name>/AppData/Local/Programs/Rider 2`.
- **Tests:** `testFramework(TestFrameworkType.Platform)` plus `junit:junit:4.13.2` works.
- **Kotlin warning in the log:** compiling printed messages about `kotlin_module` metadata of the platform JARs (only with a Kotlin version that is too old). Gone with 2.4.0.
- **Windows/Bash:** `python3` in Git Bash hangs (Store stub), do not use it for scripts.
- **Gson:** `com.google.gson` is available on the Rider 2026.3 classpath, no own dependency needed (compile and tests green, 2026-11-19).
