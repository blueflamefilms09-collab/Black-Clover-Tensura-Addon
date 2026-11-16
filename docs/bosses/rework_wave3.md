# Boss rework wave 3

Three parts, done in this order as far as they went: (1) Blender entity bodies, (2) new signature moves with themed effects for the early, Midnight Sun and Seven Deadly Sins
bosses, (3) the Decennial bosses (one visual signature and one extra move per tier family). Not built, not versioned and not released here: the lead bumps the version and writes the
changelog.

## What is new, replaced, kept

| Part | NEW | REPLACED | KEPT |
|---|---|---|---|
| 1. Bodies | Blender bodies of **Lucifero** (`lucifero`, `lucifero_form2`: bat wings, two horn pairs, white hair, a third eye in the second form) and the **God of Order** (`god_of_order`, `god_of_order_gates`: layered robes, gold stole, halo, ledger, staff; the Gates form adds a second tilted halo and six floating pages). Two new pose actions (`BEAM`, `BOW`), three new part roles (`wing_l`, `wing_r`, `tail`), `BossMeshRouting`, two client settings. | Nothing is removed. The kit's player-model look of both bosses is replaced as the *default* look only. | The kit renderer is the fallback (setting off, or a missing or broken mesh file). `ROLES` and `ROLE_NAMES` stay at 8 (the Goliath and the Minotaur are untouched). Every old pose curve. |
| 2. Moves | One appended move for each of 16 arc bosses (table below), a hook `appendedMoves()` in `ZogratisBoss` (empty by default), a pure rows class, one self-test key. | Nothing. Moves are appended at the end of each list, so no index moves. | Every old move and index. Diane's own chooser lets the appended move through; the other choosers pick from the whole book. |
| 3. Decennial | Ten tier families (side x D&D tier of 20 levels), each with one visual signature (a `VfxShape` and a colour) and one extra move (circle, donut with a safe middle, or a line), all data in `tier_families.json`, written by `tools/gen_decennial_roster.py`. | Nothing. | The 14 signature mechanics, the lair actions, all 200 profile files (the generator rewrites them byte for byte). |

### The appended moves (all bounded shapes of `TierMove`; sources below)

| Boss | Move | Shape | Effect shape |
|---|---|---|---|
| Heath Grice | Glacier Bloom | ring wave, frost | ICE_FX1 |
| Vetto | Bound Hail | marked circles, slow | SEAL_CHAINS |
| Lotus Whomalt | Poison Lotus Bloom | ring wave, poison | POISON_CURTAIN |
| Mars | War Drum Quake | slam | EARTH_RISE |
| Revchi Salik | Storm Anvil | volley | LIGHTNING_FIEND |
| Fana | Radiant Verdict | beam | DIVINE_RAY |
| Rhya | Rooted Dirge | marked circles, slow | TREE_ROOTS |
| Rades Spirito | Thrall Tide | wisp swarm | SOUL_VORTEX |
| Patolli | Loaded Dice | marked circles | DICE_D20 |
| Meliodas | Hellblaze | ring wave, burn | DEMON_FIRE_FX1 |
| Diane | Foundation | slam | EARTH_FISSURE |
| King | Spirit Spear Barrage | volley | FX_SPIRIT_NOVA |
| Escanor | Sunshine | beam, burn | FIRE_PILLAR |
| Ban | Hunter Fest | cleave | SLASH_BLADES |
| Gowther | Nightmare Teller | marked circles, blind | DREAM_DOME |
| Merlin | Infinity | marked circles | SPACE_RINGGATE |

Damage is the boss's own attack times 0.9 to 1.6 through `ZogratisBoss.strike`, the framework's damage law (never over 45 percent of a victim's maximum health in a hit, never over
6 percent a second). Every move has a wind-up of at least 12 ticks and its kind's own telegraph, a cooldown of at least 100 ticks, power 25 to 45 (never an ultimate).

### The decennial families

| Family | Move | Shape | Signature effect |
|---|---|---|---|
| evil 1 (levels -1..-20) | Grave Grasp | circle 5, slow | BONE_FX1, bone brown |
| evil 2 | Blood Lance | line 16 | BLOOD_SIGIL, blood red |
| evil 3 | Hellfire Ring | donut 9, ignite | DEVIL_CIRCLE, ember |
| evil 4 | Black Moon Fall | circle 6, weakness | DARK_BLACK_MOON, violet |
| evil 5 | Nihil Sunder | line 18, drain | NIHILITY_ZONE, slate |
| holy 1 (levels 1..20) | Dawn Flare | circle 5 | LIGHT_FLARE, pale gold |
| holy 2 | Halo Verdict | donut 8, knock-back | DIVINE_HALO, gold |
| holy 3 | Lance of Mercy | line 16, slow | DIVINE_RAY, cyan |
| holy 4 | Pillar of Judgement | circle 6, blind | DIVINE_PILLAR, amber |
| holy 5 | Sigil of the Last Dawn | donut 10, weakness | DIVINE_SIGIL, white |

## Settings (each restores the old behaviour)

| Setting | Side | Default | Off gives |
|---|---|---|---|
| `luciferoBlenderModel`, `godOfOrderBlenderModel` (`nusmp-playermodel-client.toml`) | client, look only | on | the kit's player-model look |
| `bossRework3Moves` (`BossRulesConfig`) | server | on | the old move lists of the 16 bosses (read when a boss is built) |
| `bossRework3DecennialMoves` (`BossRulesConfig`) | server | on | no tier-family move (the signature mechanic and lairs are unchanged) |
| `bossRework1Vfx` (wave 1, reused) | server, looks only | on | no shaped effect of the new moves or the family signature |

## Worst-case cost (rule 11)

* **Draw (client):** one body per boss in view: at most 2569 body quads (1136 beyond 40 blocks, the far copy has no glow) plus 489 glow quads within 48 blocks; two draw
  batches; no allocation per frame (`BossMeshDraw` works into fields). Lucifero in the Gates form is the maximum: 2569 quads. Cull box widened by 2 to 3 blocks for the wings and halo.
* **Moves (server):** per boss one timer check per tick when idle. Per cast: at most 24 foes looked at, a volley of at most 5 bolts (each lives at most 40 ticks), at most 5
  marked circles, 4 wisps; no entity is spawned, no block changes, nothing heals. One boss casts one move at a time; a cooldown of at least 100 ticks.
* **Effects (network):** one shaped effect per cast through `BossMoveVfx` (12 a tick globally, nearby players only). Tell particles are the kind's own (every third tick).
* **Decennial families:** one timer comparison per tick; one cast in at least 8 seconds per boss; per cast the survival players inside the hall are hurt once each (after the
  player's hit cap) and one budgeted effect goes out; the warning is at most 40 particles every fourth tick for 20 to 60 ticks. The signature effect at the feet is one budgeted
  effect every 3 seconds in a fight. Nothing runs when nobody is in the hall.
* **State:** no static per-player state; everything is on the boss and is dropped when it is removed (the layer's `wipe` also resets the family).

## Sources (rule 10)

The wiki could not be reached from the build session, so none of the move names is wiki-verified. NAMED rows (Hellblaze, Foundation, Sunshine, Hunter Fest, Nightmare Teller)
carry names as the series knows them, from memory; the others are THEME or FILLER (named and shaped here). The data keeps this as `TierMove.Basis` in `Rework3Rows`. Please check
them against the English Black Clover wiki (Japanese wiki as the fallback) before they are called canon.

## Models: Blender

Made in Blender through the existing pipeline (`tools/blender/bl_lib.py`, `bl_entity.py`): `tools/blender/boss_lucifero.py` and `boss_god_of_order.py` (modelled, unwrapped and
baked; previews in `docs/bosses/previews/lucifero_*` and `god_of_order_*`). Run `bvenv/bin/python -B tools/blender/boss_lucifero.py` to rebuild.

**Not made (left for the next wave):** the Asterius body (second form of the Minotaur family), the Charybdis Spawn body, and Blender bodies for the Tier List bosses. Their
renderers are unchanged.

## What was not seen in a game

No fight was watched: not the new bodies in a real client (the previews are Blender renders, the in-game lighting and the pose curves are unseen), not the wing beat or the
halo turning, not a move landing. The CI server smoke test covers the move lists and the family data (key `boss.rework3`); nothing covers the look. Gradle was not available in
the session, so only the pure classes were compiled with `javac`; the first real compile is CI. Previews are in `docs/bosses/previews/`.
