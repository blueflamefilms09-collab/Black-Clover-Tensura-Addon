# zoneup audit: Mana Zone upgrade, spec bullet by bullet

Builder `zoneup`, self-test prefix `magic.zone_up`. Spec: the owner's master spec, section 2 "Mana Zone" (Gemini prompt text, `93f0c2c2-gemini-code-...txt`).
The mod's Mana Zone (round 1.99.0, `docs/mana/mana_zone_redesign.md`) was read file by file before anything was written.

Sources for lore: the English wiki (blackclover.fandom.com) and its Japanese page were NOT reachable from the sandbox (`getaddrinfo ENOTFOUND`); a web search returned only
fan-site summaries (Mana Zone = control of the ambient mana; Mereoleona strikes "from every angle"). The lore used is the owner's spec text and the mod's existing canon notes.

| # | Spec bullet | Status | Where it is today | Decision |
|---|-------------|--------|-------------------|----------|
| 1 | Spherical domain, 15-20 block radius, centred on the caster | EXISTS (stronger) | `ManaZoneManager.start/radius`, `ManaZoneMath.radiusAt/capRadius`, `mana_zone.json`: base 14, up to 40, grows with EP, ease-out over 60 ticks | KEPT as is (the radius is the cap the new features read) |
| 2a | Speed buff | EXISTS | `ManaZoneManager.start`: attribute modifier `mana_zone_speed` (+20%) | KEPT |
| 2b | Attack buff | MISSING | only a spell-power multiplier through `ManaLaw.powerMult`, no attribute | NEW: `mana_zone_attack` attribute modifier (server setting `upgrade.buffs`) |
| 2c | Knockback-resistance buff | MISSING | none | NEW: `mana_zone_knockback` attribute modifier (same setting) |
| 3 | Evasion: check incoming projectile trajectories and offset the player to dodge | PARTLY | `ManaZoneManager.scan`: a foreign projectile that enters the zone is DESTROYED (a boss's or an over-budget one is only slowed); `onIncomingDamage` cuts every foreseen blow by 15-35% ("predict"). Nothing dodges a projectile that was slowed, was over budget, or was born next to the caster between two scans | NEW: `ZoneEvasion` (predicted dodge from the scan's own projectile list + `ProjectileImpactEvent` backstop), setting `upgrade.evasion` |
| 4a | Omnidirectional cast: client raycast camera to crosshair, up to the zone radius | MISSING | the Omni Volley page only picks foes near the crosshair on the server | NEW: `client/manazone/ZoneCastClient` |
| 4b | Packet with the target `Vec3` / entity id | MISSING (all three zone packets are server to client) | | NEW: `ZoneCastNet.AimPayload` (client to server) |
| 4c | Server verifies the target is inside the zone from its own record and spawns the spell AT the target, not at the hand | MISSING | Omni Volley places bolts round foes it finds itself, one page only | NEW: `ZoneCast` (validation, pending aim, origin override around `GrimoireBook.castPage`) |
| 4d | Existing spells become omnidirectional without rewriting them | MISSING | 700+ page handlers read `getEyePosition` / `getViewVector` / `GrimoireBook.aim/target/along/vfx` | NEW: the caster is posed at the origin looking at the target while the page's own handler runs (position, rotation, velocity restored after), plus the `aim/target/along/vfx` helper overrides |
| 5 | No keybind | n/a | | KEPT: the client only reads the existing skill slot keys; no new `KeyMapping` |
| - | Omni Volley, Pressure Space, bubbles, ally pressure, footholds, upkeep, gate | EXISTS | `ManaZoneActions`, `ManaZoneManager` | KEPT untouched |

Everything new sits behind SERVER settings in `serverconfig/nusmp-mana-zone-server.toml` (section `upgrade`); with all three switches off the zone behaves exactly as in 1.99.0.
