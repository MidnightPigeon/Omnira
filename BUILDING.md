# Building Omnira

## Requirements

- Java 21.
- Gradle version specified by `gradle/wrapper/gradle-wrapper.properties`.
- Minecraft 1.21.1 and NeoForge 21.1.251 for development.
- The dependencies referenced in `build.gradle` and `config/development-mods.json`.

This checkout currently expects the development modpack at
`../.minecraft/versions/WizardryCraft/mods/`. Create, Sable, Aeronautics,
JEI and the optional test environment are resolved from that location.
Third-party JARs are not committed. A fresh checkout without those dependencies
is not a standalone build environment; supply the matching dependencies or
adapt their paths before building.

## Build

Prefer the cached local Gradle entry point:

```powershell
.\scripts\gradle-local.ps1 build --offline
```

On a new machine without cached Gradle or dependencies, the Wrapper remains
available to obtain the declared Gradle version:

```powershell
.\gradlew.bat build
```

Output: `build/libs/omnira-<version>.jar`. The version is defined by
`mod_version` in `gradle.properties`.

## Tests

Headless server tests use the same installed dependency environment:

```powershell
.\scripts\gradle-local.ps1 runGameTestServer --offline -PgameTestNamespaces=omnira_eventide
```

The required structure fixture is `scripts/tests/spell_arena.snbt`.
Do not start the Minecraft client as an automated validation step.

## Repository Boundaries

Commit technical source, runtime resources, build files, the Gradle Wrapper
and required test inputs. Local design notes, auxiliary development scripts,
save data, previews, caches, logs and built JARs are not release source.

No remote repository is configured automatically. Add this local repository
to GitHub Desktop, review the first commit, then publish using the desired
account, repository name and visibility.
