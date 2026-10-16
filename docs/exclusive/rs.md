# Rades Spirito: Soul Corpse Magic (1.43.0)

Nothing here was run in game. Type-checked against API stubs (stub gaps only), pure logic unit tested, models/overlays/VFX previewed headless.

## New vs replacement
All NEW. The generic Soul Corpse book (5 pages) is untouched; five pages are appended (modes 5..9) in `book/ext/SoulCorpseExt`.
Nothing replaced, nothing removed.

## Pages
| id | tier | what |
|---|---|---|
| `corpse_reanimate` | mid | raises the newest of the dead of the last 60 s (up to `corpse.raiseMax`) |
| `corpse_jimmy` / `corpse_alfredo` / `corpse_carl` | mid | summons a named puppet (3 min). Roles are INVENTED (canon only names them): Jimmy fast brawler, Alfredo slow tank, Carl ranged caster (bone-shard volleys, Soul Corpse bolt, never heals) |
| `true_soul_corpse` | signature | arms the pact; sets its own cooldown (`corpse.trueCooldown`) |

Suggested locks: `corpse_reanimate` open to anyone with the magic; jimmy/alfredo/carl/true_soul_corpse Rades only.

## How it works (the why of each class, package `corpse`)
- `CorpseLedger` (pure): 60 s window, cap `corpse.ledgerMax`, oldest evicted first. Kept per caster, saved in the player's persistent data.
- `CorpseDecay` (pure): stage 0..3 by quarter of life, bleed per second (only down), damage x1.3, overlay alpha.
- `CorpseRoster` (pure): max live bodies per owner, oldest crumbles.
- `PactRules` (pure): window, 3 allies, one use each, no boss, no unsaveable death, decay window blocks a second save.
- `NamedCorpses` (pure): stat table of the three puppets.
- `Corpses`: what a body IS (persistent tag `nusmp_corpse`), ledger, raising, the half-second sweep (foe pick, path, blow in reach, bleed, stage, wisps, end of life), foe rules.
- `Pacts`: the death interception (LivingDeathEvent, HIGH), revive, decay, mana at payout (`CorpseMana`).
- `CorpseHooks`: all event listeners. `CorpseEffects`: marker effect `nusmp:soul_corpse` (amplifier = decay stage, so no packet; the server also rebroadcasts the vanilla effect packet on a change). `CorpseEntities`/`NamedCorpseEntity`: the three puppets. `CorpseTags`: `nusmp:corpse_immune`.
- Client: `CorpseDecayLayer` (every living renderer, overlay textures per stage + additive wisp pass, render types cached), `NamedCorpseRenderer` (GeoLite, stage skins), `CorpseClient`.
- VFX: `CorpseLayer` (CORPSE_RAISE, CORPSE_PACT), same INFERNAL palette as WikiMagicLayer.

## Reality check: True Soul Corpse
A player on the respawn screen cannot be revived. The pact therefore cancels the death inside the death event: 40% health, harmful effects cleared, 3 s resistance, same spot, Soul Corpse look and -25% max health for `corpse.reviveDecay` s, no second save. Pets/summons: a pet that fell within `corpse.petWindow` s before the cast, or falls within that time after it, is re-raised as a reanimated body. Not saved: bosses, voids/kill commands (bypass-invulnerability damage), anything when the caster is offline/too far (`corpse.pactRange`) or cannot pay `corpse.reviveManaPercent`. Tapping allies: sneak + use on a player while armed. Pacts live in memory (a server stop ends them).

## Config (`nusmp-corpse-server.toml`, `CorpseConfig`)
enabled, ledgerMax, ledgerRadius, raiseMax, maxActive, lifeSeconds, damageMult, namedLifeSeconds, trueCooldown, pactMinutes, revivePlayers, reviveHealth, reviveDecay, reviveMaxHealthCut, reviveManaPercent, pactRange, petWindow (all under `corpse`).

## Safety
Control: none used. Bosses/players/NPCs/dragon/immune tag never raised; damage to players capped by `BalanceLaw.cap`; no healing (LivingHealEvent cancelled); no block changes; no drops/XP; bodies discarded on logout/stop/orphan join.

## Assets / tools
`python3 -B tools/gen_corpse_assets.py` writes the overlays, three GeoLite models, animations and 4 skins + glow each. Previews: `tools/render_preview/scenes.d/corpse_named.json`, `tools/vfx_preview/scenes.d/corpse.json`. Tests: `src/test/.../corpse`.

## Unverified
Effect update broadcast (`ServerChunkCache.broadcastAndSend`), `LivingChangeTargetEvent` cancel behaviour on Tensura brains, TargetGoal subclass, overlay UV fit on non-humanoid models (patterned so it reads on any), pet raise of exotic mobs, balance numbers.
