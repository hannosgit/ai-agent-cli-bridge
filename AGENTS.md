# Repository Guidelines

## Project Structure & Module Organization

AI Agent CLI Bridge is a Kotlin IntelliJ Platform plugin targeting JVM 21. Production code lives in `src/main/kotlin/at/hannos/aiagentclibridge/`, organized into `action`, `config`, `console`, `markdown`, and `terminal` packages. Tests mirror these packages under `src/test/kotlin/`. Plugin registrations belong in `src/main/resources/META-INF/plugin.xml`; icons live in `src/main/resources/icons/`. Read `README.md`, `DEVELOPMENT.md`, and `tools.md` before changing behavior.

## Build, Test, and Development Commands

Use JDK 21 and the Gradle wrapper from the repository root; on Windows, use `gradlew.bat`.

- `./gradlew classes` — compile production code.
- `./gradlew test` — run automated tests.
- `./gradlew buildPlugin` — create the plugin ZIP in `build/distributions/`.
- `./gradlew verifyPlugin` — check IDE compatibility.
- `./gradlew runIde` — launch a sandbox IDE for manual testing. Agents must only launch this GUI when explicitly requested.

## Coding Style & Naming Conventions

Use four-space indentation, explicit imports, and idiomatic Kotlin with immutable `val` declarations where practical. Match class and file names (`AiTerminal.kt`); use camelCase for functions and properties. Keep comments sparse and in English. No dedicated formatter or linter is configured.

Prefer IntelliJ platform APIs. Perform UI work on the EDT and long-running work in background tasks. Register new actions and extensions in `plugin.xml`. Declare bundled-plugin dependencies in both the descriptor and Gradle build. Follow `tools.md` for CLI reference syntax.

## Testing Guidelines

Tests use JUnit 4 and IntelliJ Platform fixtures such as `BasePlatformTestCase`. Name classes `*Test` and methods `testDescriptiveBehavior`. Add regression tests for non-trivial logic; preserve existing tests. No numeric coverage threshold is configured.

Run `./gradlew test` and preferably `./gradlew buildPlugin` before submitting logic changes. Documentation-only edits need manual review. Consult `TESTING.md` for terminal variants, supported CLI tools, and Windows/WSL checks.

## Commit & Pull Request Guidelines

History uses short, descriptive subjects such as “Fix terminal title getting overwritten”; no mandatory prefix convention is evident. Keep changes focused. PRs should explain the problem, resulting behavior, and validation; link relevant issues and include screenshots for UI changes.

Agents must not commit unless asked.

## Scope & Configuration

Keep build and descriptor edits minimal. Do not introduce network calls, telemetry, or runtime dependencies without an explicit request.
