# AI Agent CLI Bridge

An IntelliJ Platform plugin that seamlessly connects your IDE workflow with terminal-based AI coding tools
(such as [Claude Code](https://www.anthropic.com/claude-code), [OpenAI Codex CLI](https://github.com/openai/codex),
[Pi](https://pi.dev/), [Junie CLI](https://www.jetbrains.com/junie/), and similar agents).

It launches a preconfigured AI terminal session inside the IDE and lets you send rich context
(selections, file/folder references, errors, VCS changes) straight into your AI CLI tool — using
each tool's native reference syntax.

> No separate subscription is required for this plugin. You only need an AI CLI tool installed
> and authenticated on your machine.

---

## Features

- **Open a Configured AI Terminal** from the main IDE toolbar — starts a new terminal tab and
  automatically launches the configured AI CLI command.
- **Send Selection to AI Tool** — send the currently selected editor text to the running AI
  terminal from the editor context menu.
- **Send File Reference to AI Tool** — right‑click a file or folder in the Project view to send
  its path to the AI tool using the tool's reference format.
- **Dynamic Selection Actions** — context‑aware actions that adapt to the current editor selection.
- **Send Error to Terminal (Intention Action)** — quick `Alt+Enter` handoff of an error/warning at
  the caret to your AI terminal.
- **AI Review** — run an AI‑powered review of the current VCS changes from the Changes view toolbar.
- **Clickable File Paths in Console** — file paths printed by AI CLI tools in the terminal/console
  become clickable links that open the file in the editor. Supports relative and absolute paths,
  plus optional `:line` and `:line:column` suffixes.

## Supported AI CLI Tools

Each tool uses its own reference syntax when sending file/selection context. See [`tools.md`](./tools.md)
for the exact formats:

| Tool        | File reference example                                      |
|-------------|-------------------------------------------------------------|
| Claude Code | `@src/main/File.kt`, `@src/main/File.kt#L22-24`             |
| Codex       | `src/main/kotlin/.../AiTerminal.kt:21-23`                   |
| Pi          | `src/main/kotlin/.../AiTerminal.kt` / `...:21-23`           |
| Junie       | (see `tools.md`)                                            |

## Requirements

- IntelliJ‑based IDE `2025.3.4` or newer (`sinceBuild = 252.25557`)
- JDK 21
- The bundled **Terminal** plugin enabled
- An installed AI CLI tool of your choice, available on `PATH`

## Installation

### From source

```bash
./gradlew buildPlugin
```

The resulting plugin ZIP will be located at `build/distributions/`.
Install it in your IDE via *Settings → Plugins → ⚙ → Install Plugin from Disk…*.

### Run a sandbox IDE for development

```bash
./gradlew runIde
```

## Configuration

Open *Settings → Tools → AI Agent CLI Bridge* to configure:

- The AI CLI command to launch in the terminal
- Tool‑specific reference formatting
- Other behavior of the plugin actions

## Usage

1. Configure your AI CLI tool under *Settings → Tools → AI Agent CLI Bridge*.
2. Click **Open Configured AI Terminal** in the main toolbar to start an AI session.
3. From the editor, Project view, Problems view, or Changes view, use the corresponding
   *Send to AI Tool* / *AI Review* actions to pipe context into the running session.
4. Click on any file path printed by the AI tool in the terminal to jump to that location in the editor.

## Project Structure

```
src/main/kotlin/at/hannos/aiagentclibridge/
├── action/          IDE actions (send selection/file/error, AI review, terminal launcher)
├── config/          Settings UI and persistent state
├── console/         Console filter making file paths clickable
└── terminal/        AI terminal integration (classic & reworked variants)
src/main/resources/META-INF/plugin.xml   Plugin descriptor
```

## Development

See [`DEVELOPMENT.md`](./DEVELOPMENT.md) for details on the IntelliJ Platform plugin template,
run configurations, and publishing.

Testing notes live in [`TESTING.md`](./TESTING.md).
Open ideas and planned improvements are tracked in [`IDEAS.md`](./IDEAS.md) and [`TODO.md`](./TODO.md).

## License

This project is licensed under the MIT License — see the [`LICENSE`](./LICENSE) file for details.
