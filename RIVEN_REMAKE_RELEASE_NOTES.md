# Riven Remake — Release Notes

**Version:** 0.1.0-ALPHA  
**Date:** 2026-10-09  
**Status:** In Development

## Overview

Riven Remake is a new adaptive raid boss for the Multiverse mod (nusmp 0.91.0+), featuring intelligent threat-scanning AI, anime-inspired skill forging, and a fully animated boss encounter.

## What's New

### Boss Features
- **Adaptive AI**: Threat-scans each player every 2 seconds and counter-picks optimal skills based on:
  - Armor, resistance, and potion effects
  - Grimoire magic type and leaf count
  - Tensura race, rank, magicule pressure, and barriers
  - Recent damage history (last 8 hits)
  - Distance, vertical difference, line of sight

- **Three-Phase Fight**:
  - Phase 1 (100–70% HP): Opens with Bardic Inspiration; counter-picking begins
  - Phase 2 (70–30% HP): Gains Story Manifestation (construct summoning); all skills +1 tier
  - Phase 3 (0–30% HP / Final Form): Page-wing manifestation; can chain two skills per turn

- **Anime Skill Forge**: Data-driven codex system (JSON) supporting:
  - Black Clover spells (flame, lightning, spatial, light, dark, anti-magic resistance, grimoire constructs)
  - Tensura abilities (magicule bolt, barrier pierce, steel thread, predator copy)
  - Cross-franchise skills (DanMachi, Fire Force, Jujutsu Kaisen, JoJo, generic)
  - Admin-only skill research with sandboxed compilation and schema validation
  - No arbitrary code execution; all skills validated against sandbox primitives

- **Animated Boss Bar**: Custom overlay with:
  - Violet-to-blue gradient HP fill + traveling lightning shimmer
  - Phase indicator (I / II / III in roman numerals)
  - Skill name typing during cast
  - Color-blind safe (HP shown as number)
  - Hides outside arena leash

- **Full Spell Kit**:
  - Grimoire Manipulation (summon, flip, rewrite pages)
  - Story Manifestation (temporary constructs scale with player count)
  - Soul Bond (explicit player linking for damage/healing)
  - Shadow Step (blink + afterimage cast)
  - Bardic Songs (buff, heal, discord)
  - Eldritch Blast, Hex, Jack of All Trades, Expertise
  - Final Form (???—"the story where he wins")

### Compatibility & Integration

| System | Support |
| --- | --- |
| **Tensura** | Full: race, barriers, magicule pressure, regeneration, spiritual/material body flags |
| **Black Clover** (this mod) | Full: grimoire magic types, anti-magic resistance, Black Bull emblem, Kingdom system |
| **ManasCore** | Intrinsic skill registry + safe codex loading |
| **Better Combat** | Optional: manifested weapons inherit weapon attributes |
| **Zagred Boss** | Shared arena support (independent AI state, no coupled logic in v1) |

### Arena & Visuals

- **Forest Arena**: Towering pines, glowing mossy ground, cosmic story-warp portal above
- **Atmosphere**: Floating islands, page-like constructs, violet-blue lightning, orange-red cosmic glow
- **Phase Progression**:
  - Phase 1: Ominous ripples in the forest
  - Phase 2: Storm intensifies; sky fractures into floating islands
  - Final Form: Glowing page-winged manifestation with grounded forest backdrop
- **Character Visual Lock**:
  - Messy black hair, pale skin, violet-blue eyes when casting
  - Black high-collar coat with silver filigree and Black Bull skull
  - Floating five-leaf-styled grimoire orbiting left side
  - Electric blue-violet magic (not Zagred's word-magic gold/black)

## Configuration

File: `config/nusmp-riven.toml`

| Setting | Default | Purpose |
| --- | --- | --- |
| `enableResearch` | false | Toggle admin skill research command |
| `researchEndpoint` | (empty) | Optional HTTP endpoint for skill generation (requires research enabled) |
| `baseHealth` | 600 | Boss base HP |
| `perPlayerHealth` | 200 | HP added per additional player (scales up to 4 players) |
| `replanTicks` | 40 | Ticks between AI re-scans (2 seconds) |
| `allowCodexInbox` | true | Allow manual JSON drops into `config/nusmp/codex-inbox/` |

## Fighting Riven Remake

### Phase 1: The Black Bulls' Bard
- Opens with Bardic Inspiration and threat-scans.
- Counter-picks based on player gear and resistance profile.
- Personality-driven: banters rather than spawn-camps.

### Phase 2: Fictional Remake (70% HP)
- Gains Story Manifestation (summons one construct).
- All skills upgraded one tier.
- Boss bar subtitle swaps to `FICTIONAL REMAKE`.

### Phase 3: Final Form (30% HP)
- Manifests page-wing form with full violet eyes.
- Can chain two skills per turn.
- Kill plan scores for lethal skills × 1.5 multiplier.
- Bar pulses on animation beat.

### Tips
- **Anti-Magic**: Staggers Riven and forces re-plan. He can still Shadow Step out.
- **Tensura Barrier**: Resists his magic damage; he will switch to severance or physical.
- **Multiple Players**: Scales +20% HP per extra player (cap 4). One construct per extra player.
- **Dodge/Block**: Recent damage history helps Riven adapt. Stay unpredictable.

## Technical Requirements

- NeoForge 21.1+ 
- Minecraft 1.21.1
- Hard dependencies:
  - tensura [2.0.1.0,)
  - manascore_skill [4.0.0.2,)
  - manascore_storage [4.0.0.2,)
- Optional: bettercombat

## Anime Skill Codex

Pre-shipped codex entries cover:
- Black Clover archetypes (flame, lightning, spatial, light, dark, anti-magic resistance, grimoire constructs)
- Tensura archetypes (magicule bolt, barrier pierce, steel thread, predator copy)
- Cross-franchise pool (DanMachi, Fire Force, Jujutsu Kaisen, JoJo, generic)

Admin-only research command allows sandboxed skill addition without code execution:
```
/nusmp riven research <anime> <ability name>
```

Skills are compiled to JSON and placed in `config/nusmp/codex-inbox/`. Review and accept:
```
/nusmp riven accept <skill_id>
```

**Safety**: Skill research runs off-thread. The server never loads code from the endpoint—only text. A small compiler validates the result against sandbox primitives (projectile, melee_arc, magic_damage, physical_damage, heal, shield, blink, pull, silence, slow, summon_construct, song_buff, song_debuff, copy_codex_skill).

## Installation & Testing

1. Pull the `claude/new-session-hn036o` branch.
2. Build against nusmp sources and declared dependencies.
3. Launch with dev workspace; spawn Riven Remake with `/summon nusmp:riven_remake`.
4. Arena leash and music trigger on spawn.
5. Test phase transitions at 70% and 30% HP.
6. Check logs for debug output: search for `Riven` in `logs/latest.log`.

## Known Limitations & Future Work

- v1: Riven and Zagred share an arena but do not share AI state; no coupled boss logic yet.
- Research endpoint disabled by default for security; admins opt-in via config.
- No arbitrary code execution: all skills validated against sandbox primitives before load.
- Player skill storage is read-only except via Soul Bond's explicit, timed debuff.

## Support & Issues

For bugs, balance feedback, or feature requests, file an issue on the repo with the `boss` and `black-clover` labels. Include:
- Reproduction steps
- Player count and gear/race of test subject
- Expected vs. actual behavior
- Debug logs (search `logs/latest.log` for `Riven`)

---

**Next Steps:**
- Finalize geo model and animations
- Ship starter codex JSON files
- Test threat-scan edge cases (Tensura race/skill drifts, anti-magic interactions)
- Validate boss bar sync on dedicated server
