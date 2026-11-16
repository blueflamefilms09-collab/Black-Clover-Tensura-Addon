# Boss Cores: cores from the deep bosses and four armour families

Owner request: from about floor 250 on, every Decennial boss drops a core; four armour families are crafted from the cores (HP and resistance, weapon damage, ability regeneration, progression).
Code: `labyrinth/mega/cores/`. Config: `nusmp-boss-cores-server.toml` (SERVER). Assets: `python3 -B tools/gen_boss_core_assets.py`. CI self-tests: prefix `cores.`.

Everything here is NEW (no existing item, recipe or boss drop was replaced or changed; the relic drop of `DecennialRewards` and the loot armour are untouched). Nothing was run in a game.

## What a boss pays

The Mega-Dungeon has a boss on every 10th floor (level L on floor 10 L, Holy positive, Evil negative). On death `DecennialLayer.onDeath` calls the relic hook as before and then `BossCoreAward.award`,
with the same guards: no admin-called boss, not a standalone hall, a killer is needed (attacker, credited player, or the nearest player within 64 blocks), paid once per hall
(`DecennialData.paid/claimLoot`, own key `cores:<hall>`, so the relic key is untouched). Only |floor| >= `minFloor` (250) pays. The cores are ONE stack that sits where the boss fell, glowing,
invulnerable and never despawning; players within 64 blocks get a message.

| Rule (pure, `BossCoreRules`) | Value |
|---|---|
| Family | `|L| mod 4`: 0 Aegis, 1 Striker, 2 Wellspring, 3 Ascendant (the side does not matter) |
| Wellspring resource | `(|L| / 4) mod 4`: 0 Magicule, 1 Aura, 2 Ki, 3 Nihility (each resource every 16 levels = 160 floors) |
| First paying level | 25 (floor 250) is a Striker boss; floor 249 and above pays nothing (no boss stands there anyway) |
| Count | band 250-499: 2, 500-749: 3, 750-999: 4, 1000+: 5 (all edges and counts are config) |

## The cores (7) and the armour (28 pieces)

Cores: `core_aegis`, `core_striker`, `core_wellspring_magicule`, `core_wellspring_aura`, `core_wellspring_ki`, `core_wellspring_nihility`, `core_ascendant`.
Armour: `<set>_helmet|chestplate|leggings|boots` for the seven sets (`aegis`, `striker`, `wellspring_magicule`, `wellspring_aura`, `wellspring_ki`, `wellspring_nihility`, `ascendant`).
The Wellspring is one set PER RESOURCE (4 sets), because a piece is one item with one effect. Recipes are shapeless, made of the set's core only: helmet 3, chestplate 5, leggings 4, boots 3 (15 per set).
(Cost is fixed in the recipe files; the generator is `tools/gen_boss_core_assets.py`.)

| Set | Armour / toughness (helmet, chest, legs, boots) | Per piece | 2 pieces | 4 pieces |
|---|---|---|---|---|
| Aegis | 4 9 7 4 / 4.0, knockback resistance 0.1 | +4 max health (2 hearts); 5% less damage from damage types of other mods | +3% | +7% (replaces the 2 piece bonus); capped at 60% |
| Striker | 3 8 6 3 / 3.0 | +1.5 attack damage (attribute, any held weapon) | +8% damage on your hits | second damage type: 10% of the damage dealt again as magic damage (bypasses armour), credited to you |
| Wellspring (each) | 3 7 5 3 / 2.0 | 0.5% of the pool's maximum per second | x1.5 | x2.0; capped at 6% per second |
| Ascendant | 3 7 5 3 / 2.0 | 0.5% of a slain creature's EP added to your base EP | x1.5 | x2.0; one kill never adds more than 0.5% of your own base EP |

All numbers are in the server config, clamped by the spec. Switching `bossCores.enabled` off removes every effect (the plain armour points stay).

### Mixing

Set bonuses are counted per set (2 pieces = tier 1, 4 pieces = tier 2). The tiers of all sets worn together share one budget, `maxSetBonusTiers` (default 2): 4+0 or 2+2 pieces get everything, but a
config of 1 lets only one half set count. The per-piece numbers (health, attack damage, resistance per piece) are never limited by this, since four slots already limit them.

## The families

- **Aegis**. Damage reduction runs in `LivingIncomingDamageEvent` on the victim player: a damage type whose registry namespace is not `minecraft` (Tensura, Tensura No Index with its Coronzon type, this mod's own types) is cut;
  `extraResisted types` lists exact ids on top (ids of the other mod were not verifiable here, so nothing is listed by default; write e.g. `tensuranoindex:coronzon` if its type id is that). Damage that bypasses invulnerability (void, `/kill`) is never cut.
- **Striker**. The attack damage is an attribute modifier of the piece's slot (added by `ItemAttributeModifierEvent`, so it follows the config). Two pieces: the event raises the wearer's hit. Four pieces: after the hit landed
  (`LivingDamageEvent.Post`), 10% of the damage dealt is dealt again as `indirectMagic` damage, with the victim's invulnerability frames put back afterwards. Guards: a recursion flag (the second hit does not trigger itself)
  and at most `maxSecondTypeHitsPerTick` (3) second hits per player per tick (area weapons).
- **Wellspring**, once a second per wearing player (spread by entity id):
  - Magicule: `ex.setMagicule`, same calls as `saiyan/SaiyanKi.regen`.
  - Aura: `ex.setAura`, same calls as `book/EnergyBridge`.
  - Ki: in this mod Ki IS the magicule (3.63.0, `SaiyanKi`), and `SaiyanManager` has no separate Ki pool to refill. So the Ki set refills the magicule pool, and a Saiyan gets `kiSaiyanFactor` (1.5) times as much. For a non-Saiyan it equals the Magicule set.
  - Nihility: no Nihility pool exists in this mod or in Tensura (Nihility is a field of the Black Meteorite in `antimagic/Nullification`). The smallest honest meaning chosen: freedom from what holds a caster back. Every second each active
    skill cooldown (ManasCore `ManasSkillInstance`) falls by an extra `nihilityCooldownPercentPerPiece` percent per piece (6%, so 24% / 48% with the factor, at least by 1 per second; the rule has no unit because the cooldown unit of the skill API was not verified),
    and the full set wipes one harmful effect each second (`nihilityCleanseAtFour`). It does not touch other players and does not make skills cheaper.
- **Ascendant**. On `LivingDeathEvent` with the wearer as killer: `EP bonus = victim EP x share x pieces x tier factor`, capped at `epCapPercentOfBase` of the wearer's base EP, added with `TensuraBridge.addBaseEp` (the repo's
  proven EP write; Tensura's own EP gain on a kill is not changed, this is added on top). Killing a player pays nothing unless `epFromPlayers` is on. **Telesma and Malice** of Tensura No Index (mod id `tensuranoindex`) are a STUB:
  `QuenchedBridge` can tell the mod is loaded and has the one hook where a Telesma/Malice gain would be scaled, but it does nothing, because no API of that mod could be verified from this repo (the compat pack only references its items and
  entities). It touches no class of that mod, so a server with or without it cannot crash because of it.

## Server and lag (rules 9 and 11)

Server side only: no client class, no packet, no entity spawn (except the one drop stack per boss death), no chunk load, no world scan. State: one `UUID -> {tick, hits}` table for the Striker bound, dropped at logout, death and
server stop. The server config is read through safe getters (the creative menu asks for attribute modifiers on the client before any server config is loaded).
Worst case: every handler returns after four slot reads and an instanceof for a player who wears nothing; the 1-second pass visits only online players who wear a Wellspring piece (a Nihility wearer walks his skill list once a second:
a few dozen skills); at most 3 extra hits per player per tick; 1 saved-data lookup and 1 item entity per boss death; 0 packets. A `PerfProbe` section `s.bosscores.tick` times the one-second pass.

## Checks

- `BossCoreRulesTest` (pure): family and resource by level, both sides, band edges (249 pays nothing), clamps, tiers and the mixing cap, the numbers.
- `BossCoreSourceScanTest`: no client class, no static player, no scans, no packets, hook beside the relic with the same guards, recursion guard, one config file, spread tick.
- `BossCoreAssetsTest`: icon, model and name of the 35 items, two layers of every set, a recipe per piece, creative tab and franchise rule (cores and armour show in the Tensura franchise tab and in the dungeon/armour tabs).
- CI self-tests `cores.rules`, `cores.items`, `cores.world` (registry, a real hit with four Striker pieces on a zombie, the per-tick bound, cleanup, a Wellspring refill when the stand-in has Tensura data). `cores` is in the smoke list.

## Honest caveats

- Not played. The balance numbers are first guesses (the cost is 15 cores per set; a Wellspring resource comes only every 160 floors, so about 9 Wellspring bosses per resource exist on each side of the dungeon).
- The armour is drawn with the vanilla layer textures (`bosscore_<set>_layer_1/2.png`, Pillow, deterministic); there is no 3D model made in Blender and no entry in the worn-model library, and the icons are 16 x 16 pixel art. Models in Blender were not made.
- Aegis resists this mod's own damage types too (anything outside `minecraft`); `resistOtherModTypes` turns the namespace rule off.
- Ki has no separate effect for non-Saiyans; Nihility is an interpretation; Telesma and Malice do nothing.
- The Striker second hit resets the victim's invulnerability frames for the instant of the hit and then restores them; the vanilla "last hurt" value ends up as the second hit's, so a quick third hit can land slightly harder than vanilla allows.
- Recipes are shapeless and cores-only; if a mix with other materials is wanted, edit `tools/gen_boss_core_assets.py` and run it again.
