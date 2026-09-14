# Changelog

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
