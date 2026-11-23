# Changelog

## 2.7.3-cozy.5 — 2026-11-23

- New `/npcmd copyworld <from> <to> [confirm]` (`citizenscmd.copyworld`, op): copies every NPC in one
  world to the same coordinates in another, with skins, traits and click commands. Without `confirm`
  it only reports what it would copy. Copies remember their original, so running it again skips NPCs
  that already have a copy there, and copies are never copied back. The originals are untouched.
- Fix: NPC data matched ids loosely, so copying NPC 1 (`/npc copy`) also copied NPC 12's commands onto
  a wrong id, and removing NPC 1 cleared NPC 12's cached commands until a reload. Ids now match exactly.
- 19 tests pass on Paper 26.2 and 26.3. Not yet run on a live server.

## 2.7.3-cozy.4 — 2026-09-25

- Maintain a single Java 25 JAR for Paper 26.2 and 26.3, compiled against 26.2 build 117 and declaring API 26.2.
- Verify the same compiled classes against Paper 26.3 build 40 alpha.
- Preserve existing NPC command data, cooldowns, one-time actions, sound fixes, temporary permission cleanup and reload handling. No data migration or gameplay changes.
- Keep upstream update checks disabled. The separate Citizens plugin must support the running Minecraft version.


## 2.7.3-cozy.3 — 2026-09-14

- Fix Bukkit sound names such as ENTITY_VILLAGER_YES falling through to raw
  resource-key playback and throwing an IdentifierException on Paper 26.2.
- Resolve legacy names directly with case normalization, without Sound.values/name scanning.
- Preserve complete dotted/namespaced sound names and parse optional volume/pitch separately.
- Correct pitch assignment, reject malformed arguments, and log the affected NPC.
- Preserve existing sound entries and PlugManX lifecycle handling.
- Clean build passes all 14 tests; five sound tests cover parsing, playback arguments,
  the reported command lookup path, and invalid-name handling. Actual audible playback
  remains a server-side verification step.

## 2.7.3-cozy.2 — 2026-09-14

- Remove owned command-map entries explicitly; upstream Triumph unregister is a no-op.
- Unregister listeners and plugin channels, clear state, and continue cleanup after failures.
- Fully detach temporary player permissions, including nested commands, exceptions and unload.
- Run confirmation timers on the main thread and cancel pending tasks on unload.
- Close the cooldown saver under its save lock so old tasks cannot overwrite new data.
- Abort invalid NPC/cooldown loading without overwriting existing data during shutdown.
- Add six reload regression tests; clean Java 25 build passes all nine tests.
- Actual PlugManX reload on a running server remains a manual verification step.

## 2.7.3-cozy.1 — 2026-09-14

- Start the private Cozy Crafters maintenance fork from upstream `ccd1161`.
  Its Java sources match upstream's 2.7.2 release commit `2800e82`.
- Target Paper API 26.2.build.117-stable, Java 25, and Citizens API 2.0.43-SNAPSHOT.
- Use Paper-provided Adventure instead of bundling the legacy Bukkit bridge.
- Disable upstream update checking for this separately versioned fork.
- Flush cooldowns on disable and serialize saves to avoid overlapping writes.
- Cancel tasks, unregister commands/channels, shut down metrics, and clear API references.
- Keep the plugin name, commands, permissions, configuration and NPC data formats.
- Update CI and add three compatibility regression tests.

Validation: clean Maven verify passes on Java 25 (3 tests, 0 failures).
In-game NPC interactions and optional integrations still require staging validation.
