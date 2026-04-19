# AGENTS.md

Guidance for AI coding agents (Claude Code, Codex, Junie, Pi, etc.) working in this repository.
Human contributors should read [`README.md`](./README.md) and [`DEVELOPMENT.md`](./DEVELOPMENT.md) first.

## Project overview

- **Name:** AI Agent CLI Bridge
- **Type:** IntelliJ Platform plugin (JVM)
- **Language:** Kotlin (JVM 21), with Java source compatibility 21
- **Build system:** Gradle (Kotlin DSL) + `org.jetbrains.intellij.platform` plugin
- **Target IDE:** IntelliJ‑based IDE `2025.3.4`+ (`sinceBuild = 252.25557`)
- **Plugin ID:** `at.hannos.ai-agent-cli-bridge`
- **Package root:** `at.hannos.aiagentclibridge`

The plugin launches a preconfigured AI CLI terminal inside the IDE and sends context
(selections, file/folder references, errors, VCS changes) into it using each tool's
native reference syntax.

## Repository layout

```
.
├── build.gradle.kts              Gradle build (IntelliJ Platform plugin, Kotlin, JVM 21)
├── settings.gradle.kts
├── gradle.properties
├── gradle/                       Gradle wrapper
├── src/main/
│   ├── kotlin/at/hannos/aiagentclibridge/
│   │   ├── action/               IDE actions (send selection/file/error, AI review, terminal launcher)
│   │   ├── config/               Settings UI & persistent state (AiAgentCliBridgeConfigurable, …)
│   │   ├── console/              Console filter making file paths clickable
│   │   └── terminal/             AI terminal integration
│   └── resources/
│       ├── META-INF/plugin.xml   Plugin descriptor (actions, extensions, deps)
│       └── icons/                Plugin icons (buttonIcon.svg, …)
├── README.md                     User-facing documentation
├── DEVELOPMENT.md                Template/development notes
├── TESTING.md                    Manual testing notes
├── TODO.md / IDEAS.md            Open items & ideas
└── tools.md                      Reference syntax per supported AI CLI tool
```

Do not create files in `.junie/` unless explicitly requested.

## Build, run, test

Always use the Gradle wrapper. All commands run from the project root.

| Task                   | Command                        | Purpose                                              |
|------------------------|--------------------------------|------------------------------------------------------|
| Build plugin ZIP       | `./gradlew buildPlugin`        | Produces ZIP in `build/distributions/`               |
| Run sandbox IDE        | `./gradlew runIde`             | Launches a sandbox IDE with the plugin for manual QA |
| Compile only           | `./gradlew classes`            | Fast compile check                                   |
| Run tests              | `./gradlew test`               | Executes JUnit tests (IntelliJ test framework)       |
| Verify plugin          | `./gradlew verifyPlugin`       | Compatibility check                                  |

On Windows use `gradlew.bat` instead of `./gradlew`.

> Note: `runIde` is long-running and opens a GUI — do not start it in automated/headless
> agent sessions unless the user explicitly asks for it.

## Conventions for agents

### Code style
- Match the existing Kotlin style in the codebase: 4‑space indent, no wildcard imports,
  idiomatic Kotlin (`val`/`data class`/extension functions where appropriate).
- Target JVM 21 features; do not introduce libraries that require a newer JVM.
- Comments are sparse in this project — do not add explanatory comments unless the user
  asks for them or the logic is genuinely non-obvious. Use the same language as existing
  comments (English).

### Package / file conventions
- New production code goes under `src/main/kotlin/at/hannos/aiagentclibridge/<layer>/`.
- Pick the layer (`action`, `config`, `console`, `terminal`, …) that matches the concern;
  create a new sub-package only if no existing one fits.
- File name = top-level class name (PascalCase, `.kt`).
- Register new `AnAction`, extensions, intention actions, and console filters in
  [`src/main/resources/META-INF/plugin.xml`](./src/main/resources/META-INF/plugin.xml).

### IntelliJ Platform specifics
- Prefer platform APIs (`AnAction`, `Service`, `PersistentStateComponent`,
  `ConsoleFilterProvider`, `IntentionAction`) over ad-hoc solutions.
- UI interactions must happen on the EDT; long-running work via
  `ProgressManager`/coroutines/background threads.
- New bundled-plugin dependencies must be declared in both `build.gradle.kts`
  (`bundledPlugin(...)`) and `plugin.xml` (`<depends>`).
- Keep `sinceBuild` in `build.gradle.kts` consistent with the target IDE version.

### AI CLI tool reference syntax
When producing or manipulating file references that are meant to be sent to an AI CLI tool,
follow the per‑tool format defined in [`tools.md`](./tools.md):

- **Claude Code:** `@path/to/File.kt`, `@path/to/File.kt#L22`, `@path/to/File.kt#L22-24`
- **Codex:** `path/to/File.kt`, `path/to/File.kt:22`, `path/to/File.kt:22-23`
- **Pi:** same path style as Codex; Windows-style backslashes are acceptable
- **Junie:** TBD (see `tools.md`)

Console file-path parsing (clickable links) is handled by
`at.hannos.aiagentclibridge.console.FilePathFilterProvider` and must keep supporting
relative + absolute paths with optional `:line` and `:line:column` suffixes.

## Testing expectations

- For trivial changes (typos, small UI strings, doc updates), manual verification is fine.
- For non-trivial logic (actions, console filter, terminal integration, settings
  persistence), add or update tests under `src/test/kotlin/...` using the IntelliJ
  Platform test framework (`testFramework(... TestFrameworkType.Platform)` is already
  configured in `build.gradle.kts`).
- Do not weaken, disable, or delete existing tests to make a build pass.
- Run at minimum `./gradlew test` (and ideally `./gradlew buildPlugin`) before submitting
  non-trivial changes.

## Pull requests / commits

- Keep changes focused and minimal; do not perform unrelated refactors.
- Do not commit on behalf of the user unless explicitly asked.
- When a commit is requested, add Junie as co-author via the trailer flag, e.g.
  `git commit -m "..." --trailer "Co-authored-by: Junie <junie@jetbrains.com>"`.

## Safety & scope

- Do not modify files under `.junie/` unless the task explicitly requires it.
- Do not introduce network calls, telemetry, or new runtime dependencies without an
  explicit request.
- Treat `plugin.xml`, `build.gradle.kts`, and `gradle.properties` as high-impact files —
  change them only when necessary and keep edits minimal.
- Prefer editing existing files over creating new ones when the functionality naturally
  fits somewhere.
