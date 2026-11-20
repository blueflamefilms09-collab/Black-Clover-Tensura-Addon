# Berserk of Gluttony weapons: two more techniques and a passive each, and an effect of their own

Series: *Berserk of Gluttony*. Code is in `com.newuniverse.nusmp.sinweapons` (rules, items, effect sender), `com.newuniverse.nusmp.skill.SinWeaponSkill` (the Lord skill) and `com.newuniverse.nusmp.vfx.client.layer.SinWeaponLayer` (the picture).
Request: "fix the weapon vfx for the weapons, and give the berserk weapon/skills more abilities just like the first one" (the first one = the Black Sword Greed, which already has forms, techniques and a skill).
Not seen in a running game. The Java was not compiled here (no Gradle in the session); CI is the first compile. The look was checked only with the headless preview (`tools/vfx_preview`), which is not a Minecraft renderer.

## 1. NEW / REPLACED / KEPT (rule 5)

**NEW** (nothing like these existed)
- 10 techniques, 2 per weapon, as extra modes of the weapon's Lord skill (modes 3 and 4 in the skill's mode list; the mode ids are `ability_a` and `ability_b`). Each has its own cooldown clock.
- 5 passives, 1 per weapon, working on a plain hit of the owner's weapon.
- A themed effect per weapon and per technique (one shape, `VfxShape.SIN_WEAPON`, one client layer `SinWeaponLayer`), small matching flashes on a plain hit and a thread for the Envy theft.
- Server settings `abilities` and `themedVfx` (`nusmp-sinweapons-server.toml`), client setting `classicVfx` (`nusmp-sinweapons-vfx-client.toml`).
- Lang: `tools/lang/berserk_abilities.json` (34 keys, merged into `en_us.json`).
- Preview scenes: `tools/vfx_preview/scenes.d/sin_weapons.json` (26 scenes).
- Pure test `SinAbilitiesTest`.

**REPLACED**: nothing. The generic particle puff is the thing the new effects stand in for, but it is not removed: `themedVfx=false` (server) brings back exactly the old puff for every technique and hit, and the client setting `classicVfx=true` hides the new picture for one player.

**KEPT** (unchanged): the five right-click techniques, their cooldowns, damage, caps and reach (`SinRules.Kind`); the plain-hit slow of Sloth; Covet of Envy; the twin off-hand blow of the Chakrams; the Manifest / Store mode; the owner binding (`soulBound`); `techniques`, `covet`, `damagePercent`; every old lang key; the `SinRules` constants; the skill ids and the two old modes (their order is unchanged, the new modes are appended after them).
Edited existing files that other tests read: `SinSkillSourceScanTest` (the skill now has four modes) and `SaiyanPhaseSourceScanTest` (it located the end of the `VfxShape` enum by the text `GIANT_AVATAR;`, which is now `GIANT_AVATAR,` because `SIN_WEAPON` is appended after it).

## 2. The techniques, in plain words

Every technique: used from the Lord skill (pick the mode, press the key) while HOLDING the weapon. Only its bound owner can use it (the same `SinSkillService.mayUse` check as the old ones). One box query round the wielder (`relics.Fx.foesAround`, hostile creatures and PvP-legal players only; never allies, pets, summons or creative players), cut to the technique's own cap (8 at most). All damage through the Balance Law (`relics.Fx.damage`, which also applies the player cap). The cooldown starts only when the technique did something (a slam with no one near is free; a thrust, a shot, a purge and a blessing cost their cooldown even at nothing). The cooldown is the technique's own clock in the wielder's persistent data; the right-click item cooldown is not touched. The clock is clamped, so a clock set back by a time change cannot lock the wielder out.

| Weapon | Technique (skill mode) | What it does | Cooldown | Cap | Shape |
|---|---|---|---|---|---|
| Black Axe of Sloth | **Stillness** (3) | Foes within 5 blocks are frozen in place (Slowness VII) for 2 s. No damage. | 14 s | 8 | round the wielder |
| | **Crushing Gravity** (4) | A slam: foes within 4.5 blocks take 10 damage and are driven to the ground (a downward shove of at most 1.0). | 10 s | 8 | round the wielder |
| Black Gunblade of Envy | **Green Eyes** (3) | A line 20 blocks long, 1.5 thick: up to 4 foes on it glow and are weakened for 6 s, and up to 3 beneficial effects in all are taken from them and lent to the wielder (10 s at most each). | 12 s | 4 | line ahead |
| | **Stolen Life** (4) | Foes within 5 blocks take 7 damage; the wielder heals 1.5 per foe hurt, 6 at most. | 12 s | 4 | round the wielder |
| Black Spear of Vanity | **Reflected Thrust** (3) | A thrust and its reflection: a line 8 blocks ahead and 8 behind, 1.0 thick, 9 damage each. | 5 s | 6 | line ahead and behind |
| | **Narcissus** (4) | A cone 8 blocks ahead (half angle 60 degrees): foes are weakened (II) for 5 s and slowed (III) for 3 s. | 13 s | 6 | cone ahead |
| Black Chakrams | **Thrown Rings** (3) | Both rings fly out 14 blocks and come back: foes on the line (1.2 thick) take 10 damage. | 5 s | 4 | line ahead |
| | **Binding Embrace** (4) | The rings close round foes within 4.5 blocks: slowed (II) and weakened for 4 s and pulled towards the wielder (a pull of at most 0.6). | 10 s | 6 | round the wielder |
| Seifort Holy Sword | **Purge** (3) | A column of light: the wielder is cleansed of up to 8 harmful effects, the undead within 6 blocks take 10 damage (x1.5 against them), other foes glow for 5 s. | 12 s | 8 | round the wielder |
| | **Holy Ground** (4) | Sacred ground within 5 blocks: the wielder recovers 6 health and regenerates for 5 s, foes are pushed back (at most 0.9), the undead take 8 damage (x1.5 against them). | 15 s | 8 | round the wielder |

The passives (work on a plain hit, only for the bound owner, only with `abilities` and `techniques` on):

| Weapon | Passive | Effect |
|---|---|---|
| Black Axe of Sloth | **Sinking Weight** | A plain hit on a creature that is already slowed slows it deeper (Slowness II) and longer (3 s) than the old 2 s Slowness I. |
| Black Gunblade of Envy | **Glaring Eyes** | A plain hit makes the creature glow for 3 s. |
| Black Spear of Vanity | **Vain Shine** | A plain hit gives the wielder 2 absorption (one heart) for 4 s, at most every 3 s. |
| Black Chakrams | **Hungry Rings** | A plain hit heals the wielder for half a point, at most every 1.5 s. |
| Seifort Holy Sword | **Smiting Light** | A plain hit sets the undead on fire for 4 s. |

All numbers live in the pure class `SinRules` (`Ability` enum, constants after it). Names and descriptions: `tools/lang/berserk_abilities.json` and `en_us.json`; the skill descriptions in `tools/lang/berserk_skills.json` got one appended sentence naming the two techniques and the passive.

## 3. The effects (VFX fix)

Before: every technique and every hit sent the same small particle puff (`FxThemes.ITEM_ROTDOOM`), so all five weapons looked alike. Now each weapon has its own picture, and each technique its own variant, drawn by one client layer:

| Weapon | Colour | Right-click technique | Extra technique A | Extra technique B |
|---|---|---|---|---|
| Sloth | dark violet | closing gravity rings, falling motes | Stillness: frozen hanging threads | Crushing Gravity: falling shards, crater, shock ring |
| Envy | green | a bending tracer with eyes | Green Eyes: four slit-pupil eyes along the line | Stolen Life: wisps drawn in to the wielder |
| Vanity | magenta | a thrust ending in a mirror disc and shards | Reflected Thrust: a mirror pane with a thrust each way | Narcissus: a framed mirror, orbiting shards, a cone of light |
| Chakrams | red | two spinning ring arcs | Thrown Rings: boomerang rings out and back | Binding Embrace: three closing hoops |
| Holy | rose gold | a cross of light (stem, crossbar, feathers) | Purge: a column of light with rings and feathers | Holy Ground: a lit circle with a ground cross |

A plain hit sends one small matching flash (and a second packet only when a passive proc has its own picture: Vain Shine, Hungry Rings, Sinking Weight, Glaring Eyes, Smiting Light). Covet of Envy draws a thread from the victim to the wielder. A short camera shake only for the Sloth technique and Crushing Gravity.

How it is built: `VfxShape.SIN_WEAPON` (appended after `GIANT_AVATAR`); the effect's `power` carries the weapon and the style (`SinVfx.power(kind, style)` = `ordinal*8 + style + 1`), so five weapons x six styles are one shape and one layer, not 30. Textures are the existing `greed_*` textures plus the shared shard and spark: no new PNG. The effect goes out through the existing `VfxSpawn.send` and so through the per-tick `PerfBudgets.effectPacket` budget. The Time Magic effects were not touched (rule 3).

Settings:
- Server `nusmp-sinweapons-server.toml`: `abilities` (default true; false = no extra techniques and no passives, the old behaviour), `themedVfx` (default true; false = the old puff everywhere).
- Client `nusmp-sinweapons-vfx-client.toml`: `sinweapons.classicVfx` (default false; true = the layer draws nothing for this player). Look-only, so it is a client config (rule 9).

## 4. Worst-case cost (rules 9 and 11)

Per use of an extra technique:
- Server entities touched: one box query (`Fx.foesAround`, at most 24 creatures returned), then at most the technique's cap (8) are hit. No entity is spawned, no chunk loaded, no block scanned.
- Effect packets: 1 cast picture + at most 4 hit flashes (`SinRules.HIT_FX_MAX`) + 1 small classic puff = 6 (`themedPacketsPerUse`); before, a technique sent up to 30 puffs (one per target plus the trail). Every packet is sent to nearby players only and goes through `PerfBudgets.effectPacket`; when the per-tick budget is spent, pictures are dropped, never queued.
- Particles: 3 (themed) or 6 (`themedVfx=false`) classic particles per use, plus 6 per target in the old mode as before.
- A plain hit: at most 2 packets (flash + passive picture).
- Storage per player: 10 clocks for the techniques + 1 for the passive in the persistent data (longs); nothing static, nothing keyed by anything but the player's own data; nothing to clean up on logout, death or dimension change (the clocks are plain numbers, clamped so an old value cannot lock anyone out).
- Network: at most 6 effect packets per use, the cooldown of the shortest technique is 5 s, so at most ~1.2 packets per second per wielder from techniques, plus at most 2 per hit.

Client:
- Quads: each activation is capped by the engine at 100 quads (400 vertices). The headless preview reports at most 256 vertices for the biggest picture (Holy Purge), most pictures at most 244.
- Distance culling and the particle setting thin the draw through the existing `ctx.seg(full, min)`; effects of far players use the engine's usual culling. No per-frame allocation beyond the existing layer pattern; textures are static references.

## 5. Tests

- `SinAbilitiesTest` (new, pure): ten techniques, two per weapon in slots 1 and 2; caps <= 8, cooldowns, damage, packets per use; the shapes (area, line, both ways, cone); clock, heal and shove clamps; the old constants did not move; the effect power round-trips; `SIN_WEAPON` appended after `GIANT_AVATAR`; the layer and the client config are registered; the new code uses no client class and keeps no static map; one box query; damage only through `Fx.damage`; every name and description has its lang text in the fragment and in `en_us.json`; the skill modes are appended in order; both settings are present and restore the old behaviour in the source.
- `SinSkillSourceScanTest`: updated to four modes.
- `SinWeaponSelfTests` (prefix `berserk.weapons`, unchanged prefix): all ten techniques on a zombie ahead (no damage to the wielder, hurt or mark, no one-shot but the holy ones against the undead), the technique's own clock, every weapon x style effect through the budget, `SIN_WEAPON` order, empty-area behaviour.
- `SinSkillSelfTests` (prefix `berserk.skills`, unchanged): four modes; Stillness through the skill freezes the zombie and cannot run twice inside its cooldown.
- New self-test prefixes: none.

## 6. Sources (rule 10)

The techniques are this mod's DESIGN, not canon; the wikis name the weapons and their sins but give no game mechanic (see `docs/berserk_weapons.md`, section 1: English wiki `berserkofgluttony.fandom.com` first, `ja.wikipedia.org` as the fallback; the ja fandom wiki was not fetched). The technique names (Stillness, Crushing Gravity, Green Eyes, Stolen Life, Reflected Thrust, Narcissus, Thrown Rings, Binding Embrace, Purge, Holy Ground) and the passive names are placeholders of this design.

## 7. Honest caveats

- Not compiled and not seen in a game. The pure tests run locally; the CI compile and server smoke test are the first real check.
- The look is checked only with the headless preview, which reads the real layer code against stubs; the in-game look (blending, brightness, scale in first person) is unseen.
- No new textures and no new models (the request asked for none; no Blender work).
- `VfxShape` is append-only and other work may also append after `GIANT_AVATAR`: if two changes append, the merge is a one-line conflict, and `SaiyanPhaseSourceScanTest` locates the end of the enum by its text, so its anchor may need adjusting.
- The right-click technique is unchanged; the extra techniques are only in the skill modes (not on a right click), so a player who has no skill but holds the weapon only has the right click and the passive.
- The tooltip lists the extra techniques and the passive; the numbers in the lang text follow `SinRules` by hand (the lang test checks the names, not the numbers).
- The doc's balance numbers are a first guess; they are all in `SinRules` and `damagePercent` scales the damaging ones.
