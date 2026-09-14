# Cozy Crafters private CitizensCMD fork

Private maintenance fork for **Paper 26.2 / Java 25**, starting with
**2.7.3-cozy.3**. Keeps the `CitizensCMD` plugin name, `/npcmd`, permissions,
NPC IDs, and existing data paths. Built against Citizens API 2.0.43-SNAPSHOT;
the server must also run a Citizens build that supports Minecraft 26.2.

## Build

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home mvn clean verify
```

On other systems, set `JAVA_HOME` to the installed Java 25 JDK.
Install `target/CitizensCMD-2.7.3-cozy.3.jar`, not the `original-` JAR.
Maven resolves dependencies from their public repositories; Citizens and Triumph
remain snapshot dependencies inherited from the upstream build setup.

## Upgrade from 2.7.2

1. Stop the server and back up `plugins/CitizensCMD/` and `plugins/Citizens/`.
   Run `/npcmd reload` before stopping the old plugin to flush recent cooldowns.
2. Replace the old CitizensCMD JAR with the new JAR; leave only one copy installed.
3. Keep the complete CitizensCMD data folder, including `config.yml`, `lang/`,
   `data/saves.yml`, and `data/cooldowns.yml`. No data migration is required.
4. Start with Paper 26.2, Java 25, and a compatible Citizens build.
5. Check `/npcmd`, left/right clicks, console/player/permission commands,
   delayed commands, messages, sounds, prices, and cooldowns using a test NPC.
   Confirm one-time actions remain consumed after restart.

Vault and PlaceholderAPI remain optional. Test prices with the server's actual
Vault economy provider and placeholders with its installed expansions. A staging
server should also test with both optional plugins absent and a PlugManX reload.

## NPC sounds

With an NPC selected, add a sound using `sound [volume] [pitch]`:

```text
/npcmd add sound ENTITY_VILLAGER_YES 1 1
/npcmd add sound minecraft:entity.villager.yes 0.7 1.4
```

Volume and pitch both default to 1 when omitted. Existing Bukkit names remain
supported, along with Minecraft resource keys and resource-pack sound keys.
Client resource-pack sounds require that sound to exist in the player's pack.
Malformed entries now log a warning identifying the NPC. Version 2.7.3-cozy.3
fixes the uppercase-name playback failure, dotted/namespaced parsing, and the
old bug that wrote pitch into volume. Existing saved entries need no migration.

## PlugManX reloads

Version 2.7.3-cozy.2 adds explicit command-map and listener cleanup, removes
all temporary permission attachments (including when commands throw), closes the
cooldown saver before unload, and keeps confirmation timers on the server thread.
Cleanup continues if an individual step fails; errors are logged.

For replacing an already installed JAR:

1. Run `/plugman unload CitizensCMD` and confirm it unloaded.
2. Replace the JAR, keeping only one CitizensCMD JAR in `plugins/`.
3. Run `/plugman load CitizensCMD`.

For testing the installed version, run `/plugman reload CitizensCMD`, then check
`/npcmd` help/tab completion and NPC clicks. Repeat once and confirm each click
runs once and cooldowns/one-time actions remain intact. Reload CitizensCMD while
Citizens, Vault and the economy provider remain loaded. Pending delayed commands
and unconfirmed payments are cancelled at unload; they are not replayed.

These lifecycle behaviors have automated coverage; an actual PlugManX/Paper
reload still needs to be checked on the server. No server was modified here.
Command reference: https://github.com/Test-Account666/PlugManX

## Maintenance

- Local branch: `cozy/26.2`; `upstream` points to HexedHero/CitizensCMD.
- No private GitHub repository has been created and nothing has been pushed.
- Upstream update checks are disabled, including when an existing config retains
  `check-updates: true`, so this fork does not recommend replacing itself with 2.7.2.
- Messaging now uses Paper's native Adventure API. The documented
  `CitizensCMD.getApi()` remains; the old internal `getAudiences()` bridge is removed.
- Shutdown flushes cooldowns, cancels tasks, unregisters commands/channels,
  shuts down metrics, and clears static API/economy references.
- Fourteen automated tests cover data preservation, invalid-file protection, late saves,
  command removal, temporary permissions, cleanup failures, config, and chat parsing.
- Build and automated tests pass. Actual NPC clicks, optional integrations, and
  PlugManX reload have **not** been tested on a running server.

See [CHANGELOG.md](CHANGELOG.md) and [NOTICE.md](NOTICE.md).

---

## Upstream project information

![CitizensCMD Logo](https://i.imgur.com/Tlweggt.png)
[![Spigot Project](https://img.shields.io/badge/Spigot-CitizensCMD-blue.svg?longCache=true&style=flat-square)](https://www.spigotmc.org/resources/30224/)
[![GitHub issues](https://img.shields.io/github/issues/HexedHero/CitizensCMD.svg?longCache=true&style=flat-square)](https://github.com/HexedHero/CitizensCMD/issues)
![GitHub issues](https://img.shields.io/github/last-commit/HexedHero/CitizensCMD.svg?longCache=true&style=flat-square)
[![GitHub issues](https://img.shields.io/badge/Guide-Wiki-blue.svg?longCache=true&style=flat-square)](https://github.com/HexedHero/CitizensCMD/wiki)
![GitHub](https://img.shields.io/github/license/HexedHero/CitizensCMD.svg?style=flat-square)

# CitizensCMD
**CitizensCMD** is an addition to the plugin [Citizens](https://www.spigotmc.org/resources/13811/) that allows you to execute commands by clicking an NPC.
This plugin **NEEDS** Citizens to be installed, get it [here](https://www.spigotmc.org/resources/13811/).

## Guide
Need help? Check out the [wiki](https://github.com/HexedHero/CitizensCMD/wiki) for more informations!

## Useful links

  + [Project page](https://www.spigotmc.org/resources/30224/)
  + [Citizens page](https://www.spigotmc.org/resources/13811/)
  + [Wiki](https://github.com/HexedHero/CitizensCMD/wiki)
  + [Issues](https://github.com/HexedHero/CitizensCMD/issues)

## Author

+ **HexedHero** (Maintainer)
+ **Mateus Moreira** - LichtHund - [@LichtHund](https://twitter.com/LichtHund) (Creator)

*A special thanks to **Glare** - [darbyjack](https://github.com/darbyjack); For helping me build this.*
