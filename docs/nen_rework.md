# Nen rework (3.57.0 content, owner request: "the nen ability need the VFX remade ... and the abilities for it suck, remake them")

Nothing here was compiled with Gradle or seen in a game (see Caveats). The VFX were rendered with `tools/vfx_preview/preview.py` against the real layer code; the pure rules and source scans were run with plain javac.

## What was built
The ten basics keep their ids, order and lang keys but all do something clearer. The six Hatsu are now signature moves. Modes 0..16 are NOT renumbered; no new mode was needed (the six Hatsu keep their slots).

| Mode | Effect now | Cost | Cooldown |
|---|---|---|---|
| Ten | 15 s guard (-20%), heals a little every few seconds | 600 | 10 s |
| Zetsu | 20 s hidden, heals faster, the blow that ends it is an ambush (x1.75) | 500 | 20 s |
| Ren | Burst: knockback and damage around you (max 24 foes), then strength and speed | 1600 | 30 s |
| Gyo | Reveal and mark foes in front (take 1.2x for a while) | 400 | 15 s |
| En | 12 s sense field: shows creatures, pulses, the first blow every 6 s is halved | 1200 | 20 s |
| Ken | 8 s guard that reflects a share of each blow back through the balance law | 1200 | 25 s |
| Ko | Stores aura for up to 3 s: strike 22 (at once) to 44 (full); Enhancement spends the charge | 2000 | 30 s |
| Shu / In / Ryu | weapon wrap +6 / unseen, 1.3x / aura flow, 1.25x dealt, 0.8x taken | 800 / 700 / 1200 | 15 / 15 / 25 s |
| Enhancement | Rushing fist (reach 6): hit scaled by Ko charge, wears armour durability (never breaks it), marks | 1800 | 10 s |
| Emission | Homing volley: 4 bullets (6 with Vow), one per 3 ticks, bent onto foes in a cone | 2000 | 13 s |
| Transmutation | By stance: sneak = gum (roots up to 6), air = rubber (leap + shock on landing, up to 12), else lightning chain (3 jumps, 5 with Vow) | 2000 | 12 s |
| Conjuration | 3 chains (5 with Vow) hold foes (control time capped by BalanceLaw), hit every second, burst on expiry | 2200 | 15 s |
| Manipulation | Needle: a mob is redirected onto the nearest other foe every 10 ticks; a player is weakened, slowed, made nauseous | 2200 | 21 s |
| Specialization | Field of 9 blocks (11 with Vow): each second weakens, hurts and steals up to 2 beneficial effects (max 12.5 s, up to Level III) | 2600 | 30 s |
| Vow and Limitation | Every Hatsu above is boosted (as before, the Vow-boosted version of each move is larger/longer); no Ten/Zetsu/Ken | 0 | 60 s |

Series logic kept: Ten/Zetsu/Ren/Hatsu, the six types and the Water Divination type affinity, Vow and Limitation.

## VFX
`tools/gen_nen_textures.py` (deterministic) writes 19 textures `textures/particle/nen_*.png`. New shapes (appended after `NEN_BEAM`): `NEN_FLOW` (aura tongues of fire, 8 kinds), `NEN_BURST`, `NEN_SENSE` (En dome), `NEN_EYES` (Gyo), `NEN_FIST`, `NEN_BULLET`, `NEN_SURGE` (lightning / gum / rubber), `NEN_LINK` (chain), `NEN_NEEDLE`, `NEN_DOMAIN`. Drawn by the new `NenArtLayer`. Every send goes through `skill/NenFx.java`.
Switches: `nenVfx=false` gives the 3.46.0 particle puffs; `nenVfx=true, nenVfxV2=false` gives the 3.48.5 shapes (still registered and drawn by `NenLayer`); both true (default) is the new look.

## NEW / REPLACED / KEPT
- NEW: NenHatsu, NenFx, NenArtLayer, 10 shapes, 19 textures, generator, preview scenes (`tools/vfx_preview/scenes.d/nen.json`), setting `nenVfxV2` (server config), tests `NenReworkRulesTest`, `NenReworkSourceScanTest`, `NenTexturesTest`, lang fragment `tools/lang/nen_rework.json`.
- REPLACED (owner asked): numbers and behaviour of the basics and the six Hatsu (`NenRules`, `NenSkill`); the default look.
- KEPT: modes 0..16 order, all ids and old lang keys, old public `NenRules` methods, old shapes and `NenLayer` (reachable with `nenVfxV2=false`).
- Shared files appended only: `VfxShape.java` (10 constants after `NEN_BEAM`), `NUConfig.java` (`NEN_VFX_V2`), `VfxManager.java` (one `register(new NenArtLayer())`).

## Worst-case cost
- Server: one tag read per tick for an idle player; constructs only while their windows run. Per press at most 24 creatures (`SkillTargets`); 6 bullets, 5 chains, 1 needle, 1 field of at most 9 pulses (one query per second). No entities spawned, no chunk loads; state is in the owner's tag keyed by owner, cleared on death.
- Network: auras every 40 ticks, at most 3 per refresh, nearby players only through `VfxSpawn` budgets; chain links resent every ~10 ticks (about 10 packets/s for 5 chains).
- Client (preview vertex counts, activation cap 400): Ten 144, Zetsu 32, Ren 256 (worst), Ken 164, Ko 152, Vow 192, Shu 60, Ryu 196, burst 80, En 112, Gyo 40, fist 72, bullet 64, lightning 84, gum 36, rubber 52, chain 128, needle 80, domain 124. LOD through `ctx.seg`, distance culling by the VFX manager.

## Caveats
- Not built with Gradle, not run in a game; combat numbers are untuned. Heals use `p.heal` (not the balance law).
- Sources for Nen canon are the author's knowledge of Hunter x Hunter; no wiki was fetched.
- Preview shows shape and budget, not world lighting. Ko's fist sits apart from the body in the preview.
