# Changelog

## [0.99.0] — Elsdocia + Key Magic Restoration

**Release Date:** 2026-10-10

### Added

#### Elsdocia: the first Wizard King's legacy sword
- New God-tier weapon: **Elsdocia** (`nusmp:elsdocia`) — broad crystal-like blade with a clover emblem near the golden hilt (per the Black Clover wiki)
- Custom engraving: **Legacy of the Wizard Kings** (Tensura enchantment, level 1) — additive damage in main hand
- Standard Tensura engravings: Magic Interference 2, Magicule Absorption 2, Barrier Piercing 1
- 3D in-hand mesh (broad crystal blade, clover-crest plate on both faces, ornate gold hilt), 16×16 sprite stays the inventory icon
- Mechanics:
  - **Absorbs/stores/releases magic:** hitting a foe drains 2% of their magicule pool into the sword's charge (`ElsdociaCharge`, 0-1000)
  - **Legacy Release** (right-click): releases stored charge as a broad spatial rift; heals the wielder proportional to spent charge; boosted by Key Magic grimoire
  - **Legacy Gate** (sneak+right-click): requires a floating Key Magic grimoire + ≥100 stored charge; opens a 32-block spatial rift that drains 4% of each hit foe's magicules
  - **Key Magic synergy:** with a Key Magic grimoire floating, Legacy Release reaches 28 blocks (vs 22), hits for ×1.35 damage, and Legacy Gate becomes usable
  - Country-destroying lore scale is lore-only (tooltip); no terrain destruction
- Lore tooltips (per wiki): broad crystal-like blade / clover emblem; crafted by Lemiel Silvamillion Clover; used by Asta to defeat Conrad; contains a piece of the past Wizard Kings' souls
- EPIC rarity, Netherite tier, unbreakable legacy blade

#### Key Magic restoration
- Restored the physical **Magic Key** item (`nusmp:magic_key`) — uses the mini-key model's 64x64 texture as its item icon, dropped by Janus Baptism, consumed by Janus Abigail for extra gates
- Restored `KeyArts.janusBaptism` to drop a Magic Key on cast (was a tag-charge-only system)
- Restored `KeyArts.janusAbigail` to consume held Magic Keys (keys fill the door slots after the legacy charges, with four extra doors total) to extend to 10 doors
- Improved `KeyAura` to use the textured `great_key` model at 0.08 scale (was `mini_key` at 0.3) — the later approved improvement, not an exact recreation of `v0.82.0-b126`

### Fixed
- `WeaponEngravings` now parses fully-qualified enchantment ids (e.g. `nusmp:legacy_of_the_wizard_kings`); short ids still resolve to the `tensura` namespace
- Build fix: `BronzeAura.piece` used nonexistent `PartPose.x()/y()/z()` accessors — now uses the public `x/y/z` fields
- Build fix: `BronzeProps` used nonexistent `MagicPropEntity.hasLineOfSight` — now uses a block raycast between the prop and its target (same behavior)
- These Bronze build errors were pre-existing on the default branch (`760f10a`) and prevented any build before 0.99.0

## [0.98.0] — Marquis Remake Raid Boss

**Release Date:** 2026-10-09

### Added

#### Marquis Remake Boss (0.1.0-ALPHA)
- New raid boss: **Marquis Remake** — Minecraft-skin-body Black Bulls bard with god-summon mechanics and New Order reality rewrites
- Player-scale humanoid model (1.8 blocks, Steve hitbox) with Minecraft skin + geo coat layers (4 phases)
- Four-phase fight progression:
  - Phase 1 (Bard): Fa Jin charge + New Order + random grimoire
  - Phase 2 (Hack): 75% HP, coat fade-in, Gearshift + Compress + Evil Eye
  - Phase 3 (Rift): 60s timer, dimension pull to `nusmp:story_rift`, arena-scale mechanics
  - Phase 4 (Final Form): 25% HP, page-wings + cracked crown, The Creator god summon
- **300,000,000 EP** via Tensura API (non-plunderable, shown on appraisal)
- **New Order**: Reality-rewrite gaze mechanic (6-block range, 1.0s tell). Legal orders: +50% cast warmup, weapon speed halved, ground becomes solid, speed boost. Barks the rule. Anti-magic fizzles.
- **Fa Jin**: Melee/dash kinetic charge (white rings on crest). 5 stacks → 4-block cone punch with knockback. 10s timeout. Cannot hold through stagger.
- **Gearshift**: Speed rank (40% Low / 200% Top) on touch. Red-to-blue motion trail. Minimum Low. 4s duration. Ranks shown on boss bar.
- **Compress**: Marble-compresses blocks, constructs, or failed-tell players. Purple sphere in offhand for 3s. Player: removed from fight 3s, no damage, then released. Block: arena-only, restores at phase end.
- **Random Grimoire**: Draws one registered `nusmp.skill.grimoire_*` book each combat roll, cast via SpellRuntime. Named on boss bar. Anti-magic burns page.
- **Evil Eye**: God-scale, arena-scale. Both eyes, violet, 0.8s tell. Marks target with Existence Reduction: regen (ultra/totem/absorption/natural/mod/avatar) does nothing for 6s. Current HP does not auto-drop. Hits taken while marked cannot heal after mark ends. Works on players, high-EP Tensura bodies, and his own avatars. Phase 4 mark lasts 8s. Boss bar shows eye + subtitle "This page ends."
- **God Avatars** (10 total, one at a time, 12s life):
  - Zeus: One punch + Gearshift Top. 4-tick afterimage tell + 4-block hit. Survives one Island Fall.
  - Beerus: Hakai sphere (3-block radius, arena blocks + constructs only, blocks restore). Players take signature damage only.
  - Truth: Equivalent Exchange. Avatar damage mirrored to last player who hit Marquis (capped at phase signature).
  - Ultimate Madoka: One arrow. Removes one negative on him or one positive on marked player.
  - Arceus: Judgment. Next grimoire draw forced to element target is weakest to (via ThreatScan).
  - Grand Zeno: Erase. Deletes one construct or marble-compresses Evil Eye marked player (releases at anchor with mark intact).
  - Anti-Spiral: Three galaxy-shuriken projectiles (wide, slow tell, dimension-slash shader).
  - Lord of Nightmares: Sea of Chaos pool under gold ring (4s, pauses regen even without Evil Eye).
  - Kami Tenchi: Passive. Arena gravity stabilizes, no elytra, his i-frame window blocked. Dim gold outline, no attack.
  - The Creator: Phase 4 only, once. Law: "healing does not exist here" for 8s, whole rift. Stacks with Evil Eye. Then avatar closes.
- **Combat Type Rolls** (every 12s phase 2, every 8s phase 3): Caster, Hexblade, Legion, Rift, Song, Hack. Named on boss bar.
- **Damage Phases** (signature damage, not multiplier):
  - Phase 1: 22 base
  - Phase 2: 42 base
  - Phase 3: 62 base
  - Phase 4: 85 base
- **Null Types** (1.5s i-frame, phase change only):
  - Phase 2: Physical null 4s, then magic null 4s (alternating). Other damage type still hurts. Repeating nulled type = 0 damage.
  - Phase 3: Spatial null 4s on rift entry.
  - Phase 4: Rotating null 4s.
  - Anti-magic cancels i-frame window if not started.
  - The Creator adds 1.5s i-frame on entrance.
- **Health Scaling**: Base 1200 HP, +400 per extra player (cap 4).
- **Model & Animations**:
  - Phase 1: Minecraft skin only, grimoire closed on hip, eyes dim
  - Phase 2: Black coat + bull emblem fade on over sweatshirt, eyes go violet, grimoire opens and orbits
  - Phase 3: Lightning on coat, page shards, gold rift ring
  - Phase 4: Page-wings and cracked bull crown (same rig, no entity swap)
  - Animations: idle, walk, cast, gearshift dash, compress, evil-eye, summon, hit, phase change, death (0.4–1.4s, interrupt into hit)
- **VFX**:
  - New Order: white text rune at target's feet
  - Fa Jin: white rings on crest, cone flash on release
  - Gearshift: red-to-blue motion trail
  - Compress: purple marble with player's face shrunk on it
  - Evil Eye: full violet eyes, cone (anti_magic_slash tinted violet)
  - Avatars: gold page-body, feature as weapon (not second player model)
  - Fallback to arc_portal if shader fails
- **Loot Table** (`data/nusmp/loot_table/entities/marquis_remake.json`):
  - Guaranteed: Black Bull bard relic, story page trophy, skin-cape cosmetic (sweatshirt crest)
  - One of: Page Edge engraving, Bull Brand engraving, single-use rift key
  - 8% drop: Final Form crown (vanity)
  - No Tensura unique, no New Order item, no Evil Eye item
- **Boss Bar**:
  - Name: Marquis Remake
  - Subtitle swaps by phase
  - Combat type roll displays
  - Gearshift rank display
  - Eye indicator on Evil Eye mark
  - Hides outside arena leash

#### Arena & Visuals
- Story rift dimension (`nusmp:story_rift`, phase 3 mechanic)
- Character sheet visual lock (black high-collar coat, silver filigree, Black Bull skull, violet-blue eyes, floating grimoire)
- Minecraft skin body (brown hair, blue sweatshirt with white crest, gray pants, white shoes, player proportions)
- Coat and grimoire as geo layers that grow in, not replacement entity
- Phase progression shows visual escalation

#### Compatibility
- Tensura race/barrier/EP detection via ManasCore
- Black Clover grimoire book registry (`nusmp.skill.grimoire_*`) integration
- Anti-magic and NihilityZone: stagger Marquis, burn drawn page, force redraw
- ThreatScan integration for New Order order selection and Arceus judgment
- No Predator copy, no Usurper take-over, no Tensura unique skills

#### Configuration
- `config/nusmp-marquis.toml`
- Base health: 1200
- Per-player health: 400
- Avatar duration: 12s
- Evil Eye mark duration: 6s (phase 4: 8s)
- New Order duration: 8s
- Gearshift duration: 4s
- Compress marble duration: 3s

### Technical

- Package root: `com.newuniverse.nusmp`
- Entity: `MarquisRemakeBossEntity`
- Brain: `MarquisBrain` (threat-scan + god summon picker)
- Attacks: `MarquisAttacks` (New Order, Fa Jin, Gearshift, Compress, grimoire cast, Evil Eye, summon)
- Payload: `MarquisStatePayload` (phase, HP, cast ID, combat type, gearshift rank, evil eye mark)
- Renderer: `MarquisRenderer` (geo layers, coat fade, phase-specific textures)
- Boss bar: `MarquisBossBar` (animated overlay, phase subtitle, combat roll, eye indicator)
- Avatars: `GodsAvatarEntity` (10 variants, AI-driven, 12s life, breaks into pages)
- Asset structure:
  - `assets/nusmp/geo/entity/marquis_remake.geo.json` (4 phase variants on one rig)
  - `assets/nusmp/animations/entity/marquis_remake.animation.json` (idle, walk, cast, gearshift, compress, evil-eye, summon, hit, phase, death)
  - `assets/nusmp/textures/entity/marquis_remake_skin.png` (player skin)
  - `assets/nusmp/textures/entity/marquis_remake_coat.png` (coat layer, phases 2–4)
  - `assets/nusmp/textures/entity/marquis_remake_wings.png` (page-wings, phase 4)
  - `assets/nusmp/textures/entity/marquis_remake_crown.png` (cracked crown, phase 4)
  - `assets/nusmp/models/entity/gods/` (10 avatar models, gold page-body variants)
  - `data/nusmp/boss/marquis_remake.json` (phases, HP, drops, rift anchor)
  - `data/nusmp/loot_table/entities/marquis_remake.json` (drops)
  - `data/nusmp/dimension/story_rift_marquis.json` (arena dimension, phase 3)
- Lang keys in `assets/nusmp/lang/en_us.json`:
  - `entity.nusmp.marquis_remake` = Marquis Remake
  - `entity.nusmp.marquis_remake_spawn_egg` = Marquis Remake Spawn Egg
  - `boss.nusmp.marquis_order.*` = Order barks
  - `avatar.nusmp.gods.*` = Avatar names

### Dependencies

- NeoForge: [21.1,)
- Minecraft: 1.21.1
- Hard:
  - tensura [2.0.1.0,)
  - manascore_skill [4.0.0.2,)
  - manascore_storage [4.0.0.2,)
- Optional: bettercombat

### Testing & Quality Assurance

- New Order only uses legal orders (no delete/erase world/inventory commands)
- Fa Jin charge clears on stagger, cannot exceed 5 stacks
- Gearshift minimum is 40% speed (never 0), no negative speed
- Compress marble is visible, has 3s timer, no permanent locks
- Evil Eye stops only regen types (not direct damage), works on avatars
- Avatar summons one at a time, 12s life hard limit, breaks into pages
- The Creator fires once per phase 4, cannot be resummoned
- 300,000,000 EP shows correctly on appraisal, is not plunderable
- Dimension pull has tell + anchor release
- Anti-magic stagger correctly triggers page burn and redraw
- Zagred and dream dimension unchanged
- Dedicated server + client sync via `MarquisStatePayload`
- Boss bar UUID stable across all phases

### Known Issues & Limitations

- v1: Story rift dimension exists only during phase 3; players outside leash cannot enter
- god avatars do not load chunks outside rift (containment by design)
- The Creator law "healing does not exist" is rift-wide, not global (by design)
- Marquis + Zagred can share arena but do not have coupled AI (v2 feature)

### Future Enhancements

- Coupled Marquis + Zagred dual-boss mode
- Extended god summon pool (more anime franchises)
- Boss dialogue system (phase barks, order taunts, avatar summon callouts)
- Configurable arena themes for story rift (forest / castle / void)
- Kill replay system for New Order / Compress analysis

---

## [0.61.0] — Previous Release (Riven Remake)

See [RIVEN_REMAKE_RELEASE_NOTES.md](RIVEN_REMAKE_RELEASE_NOTES.md) for full details on Riven Remake (0.1.0-ALPHA).

---

## Versioning

This mod uses **semantic versioning**: `MAJOR.MINOR.PATCH`

- `0.98.0`: Marquis Remake raid boss added to nusmp 0.97.0 baseline
- Target `1.0.0` when both Riven and Marquis are production-ready

## Contributors

- **MarcusBlu**: Design, spec, and implementation
- **blueflamefilms09-collab**: Repository maintainer

---

**Last Updated:** 2026-10-09
