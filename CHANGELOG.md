# Changelog

## [Unreleased]

### Added

#### Riven Remake Boss (0.1.0-ALPHA)
- New raid boss: **Riven Remake** — adaptive Black Bulls bard with intelligent AI and anime skill forging
- Server entity with threat-scanning brain (`ThreatScan`, `KillPlan`) that counter-picks skills every 2 seconds
- Three-phase fight progression (70% / 30% HP transitions with visual and mechanical upgrades)
- Final Form: page-wing manifestation with chained skill casting
- GeckoLib geo model + custom animation set (14 clips: idle, walk, cast variants, phase transitions, death)
- Animated boss bar overlay with violet-to-blue gradient, traveling lightning shimmer, and phase indicators
- Anime Skill Codex: JSON-based, data-driven skill system supporting Black Clover, Tensura, and cross-franchise spells
- Skill Forge: admin-only research command with optional HTTP endpoint, sandboxed compilation, and schema validation
- Story Manifestation: temporary construct summoning (weapons, shields, portals) that scale with player count
- Soul Bond: explicit player linking for damage/healing exchange
- Shadow Step: short blink with afterimage cast
- Bardic Songs: buff, heal, and discord effects
- Full Grimoire Manipulation: summon, flip, and rewrite pages in combat

#### Compatibility & Integration
- Full Tensura integration: respects race, barriers, magicule pressure, regeneration, and spirit/material body flags
- Black Clover system integration: grimoire magic types, anti-magic resistance (not nullification), Black Bull emblem
- Better Combat compatibility: manifested swords inherit weapon attributes
- ManasCore skill registry: intrinsic skill `nusmp:fictional_remake`, safe codex loading with unknown ID fallback
- Soft optional hooks for external Black Clover mods; no hard dependency, no crashes on missing mods

#### Arena & Visuals
- Forest arena with towering pines, glowing mossy ground, and cosmic story-warp portal above
- Floating island fragments and page-like constructs
- Violet-blue lightning with orange-red cosmic glow
- Phase 1: ominous ripples
- Phase 2: storm intensifies, sky fractures
- Final Form: glowing page-winged manifestation
- Boss-bar relic drop on defeat

#### Configuration
- `config/nusmp-riven.toml` with toggles for research, endpoint, scaling, and codex inbox
- Base health: 600, scales +200 per additional player (cap 4)
- Re-plan ticks: 40 (2 seconds)
- Research disabled by default for security

#### Documentation
- Full boss specification in `RIVEN_REMAKE_BOSS.md`
- Release notes in `RIVEN_REMAKE_RELEASE_NOTES.md`
- Threat scan + AI decision logic documented
- Anime Skill Codex schema and sandbox primitives explained
- Integration guide for Tensura, Black Clover, and Better Combat
- Fight script and acceptance criteria

### Technical

- Package root: `com.newuniverse.nusmp`
- Entity registration in `NUEntities`
- Lang keys in `assets/nusmp/lang/en_us.json`
- Spawn egg and creative-tab entry next to existing boss relics
- Custom geo pipeline: `client/geo` (GeoAnim, GeoDraw, GeoModels)
- Asset structure:
  - `assets/nusmp/geo/entity/riven_remake.geo.json`
  - `assets/nusmp/animations/entity/riven_remake.animation.json`
  - `assets/nusmp/textures/entity/riven_remake.png`
  - `assets/nusmp/skills/codex/*.json` (starter codex)
  - `data/nusmp/boss/riven_remake.json` (phases, drops, music)

### Dependencies

- NeoForge: [21.1,)
- Minecraft: 1.21.1
- Hard:
  - tensura [2.0.1.0,)
  - manascore_skill [4.0.0.2,)
  - manascore_storage [4.0.0.2,)
- Optional: bettercombat (weapon attributes only)

### Testing & Quality Assurance

- Threat scan validates against server-known state only (no client guessing)
- Recent damage history (last 8 hits) tracked for accurate resistances
- Skill research endpoint disabled by default; admin opt-in only
- No arbitrary code execution: all skills validated against sandbox primitives
- Unknown skill IDs fail soft; no classloader crashes
- Dedicated server + client sync via `RivenStatePayload` (phase, HP, cast ID, charge)
- Boss bar UUID stable across all phases (no entity swaps)
- Zagred unchanged; shared arena but independent AI state

### Known Issues & Limitations

- v1: Riven + Zagred share arena but do not share AI state; no coupled boss logic
- Research endpoint untested in production; recommend testing on private server first
- Anti-magic resistance differs from Zagred (Riven resists, not nullifies) — intentional design choice
- Player skill storage is read-only except via Soul Bond debuff — by design

### Future Enhancements

- Coupled Riven + Zagred boss mode (shared arena, coordinated attacks)
- Extended codex with more anime franchises
- Boss dialogue system (barks, phase transitions, player callouts)
- Configurable arena themes (forest / castle / void variations)
- Replay system for kill logs and threat scan debugging
- Web UI for codex management (admins)

---

## Versioning

This release uses **semantic versioning**: `MAJOR.MINOR.PATCH-PRERELEASE`

- `0.1.0-ALPHA`: Initial development version, feature-complete but untested in production
- Target `1.0.0` when all acceptance criteria pass and performance validated on 4-player servers

## Contributors

- **MarcusBlu**: Design, spec, and initial implementation
- **blueflamefilms09-collab**: Repository maintainer

---

## How to Report Issues

1. Use the GitHub issue tracker with labels: `boss`, `black-clover`, `tensura`, `ai`, `client`, `animation`
2. Include: reproduction steps, player count, gear/race of test subject, expected vs. actual behavior
3. Attach debug logs (search for `Riven` in `logs/latest.log`) and threat scan output (debug level)
4. For codex research issues, include the skill JSON or research command used

---

**Last Updated:** 2026-10-09
