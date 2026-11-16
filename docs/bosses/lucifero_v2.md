# Lucifero v2: the OP boss and the new look

Owner request: "[he] should be a really OP boss, he shouldn't be weak, and his forms should look actually good like the actual anime, not so blocky. The NPC itself can be like the Minecraft player, but his armors can be specially designed for him. And his armors could drop."
The name was voice-dictated as "Lucius"; it is read here as **Lucifero** (the Spade devil boss, `lucifero`). The mod also has a boss called Lucius Zogratis (`lucius`, the God of Order body); he is NOT touched.
If the owner meant him, say so and the same work is a follow-up (the drop framework already takes him as one more catalog line, see `docs/bosses/boss_drops.md`).
Nothing here was played in a game; the Java was not compiled in this session (CI is the compiler) and the unit tests were not run.

## 1. NEW / REPLACED / KEPT

| | What |
| --- | --- |
| NEW | OP mode (server setting `spade.opMode`, default ON): 7000 health, +6 armour, +6 toughness, opens at 40% power, phases 12% sooner, two more gravity moves. |
| NEW | Move 14 `weight_of_the_throne` and move 15 `lens_barrage` (appended to the table, so the client's move names by index stay valid). |
| NEW | Three Blender bodies: `lucifero_v2` (the king), `lucifero_v2_form2` (wings spread, from 80% power), `lucifero_v2_gates` (the Gates), each with a `_lod` far copy, a skin and a glow map. Client setting `lucifero.newBody` (default ON). |
| NEW | Lucifero's armour as drops (4 pieces), his trophy "Heart of Gravity" (`docs/bosses/boss_drops.md`). |
| REPLACED | Nothing. |
| KEPT | The 13 old moves with their numbers, the roster stats (3500 / 20 / 12, pinned by the roster test), the old bodies `lucifero` and `lucifero_form2` (+ LODs), their skins, previews and the art-kit fallback, and the old tests. `spade.opMode = false` and `lucifero.newBody = false` give the old boss and the old look. |

## 2. OP mode (server side)

| What | Old | OP mode (default) |
| --- | --- | --- |
| Maximum health | 3500 | 7000 (+3500, the SS tier of the tier list) |
| Armour / toughness | 20 / 12 | 26 / 18 |
| Opening power | 20% | 40% (he starts at the second step: the comb, the grasp, the wandering weight are open from the first second) |
| Phase gates (85 / 65 / 45 / 25% health) | on his real health | on `max health x 1.12` (they arrive 12% of his health sooner, so the Gates open with about 30% of his bar left) |
| Moves | 13 | 15 |

**The global Black Clover multiplier stacks on top (3.64.0, `bossRules.blackCloverStatMultiplier`, default 3, `BossStatBuff`).** Lucifero is a Black Clover boss, so in a real game his
health is the table above times 3: the old Lucifero has 10500, the OP Lucifero 21000 (the buff is a multiplied-total modifier and the OP bonus is an additive one on the base, so they multiply),
and his hits are tripled too (a buffed hit never takes more than half of a victim's maximum health). The 7000 is the "SS tier" number of the tier list, the same number system as every other boss's table value.
If 21000 is too much, lower `opMode`'s `OP_HEALTH_BONUS` in `LuciferoTable` (one constant) or the global multiplier; with `opMode = false` and the multiplier at 1 he is the original 3500.

How it is done: the extra health, armour and toughness are TRANSIENT attribute modifiers (ids `anime_multiverse:lucifero_op_health/armor/toughness`) added in the constructor of the entity, so they are never
saved and are added again when he is loaded (the saved health is read after them). The mode is read once when he is built and kept for his life. The roster stats are untouched. A client has no server
config, so it assumes the longer move book (names are read by index; a shorter server book is a prefix of it).

### The two new moves (base damage before the framework's damage law, which turns it into the real hit as for every other move)

| # | Id / label | Power | Wind-up | Cooldown | Range | Weight | Base dmg | What it does | Counter |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 14 | `weight_of_the_throne` Weight of the Throne | 40 | 28 | 340 | 0-16 | 4 (crowd) | 12 per ring | Three rings of weight spread out from him (radius 4.5, 8.5, 12.5; band 2.2), 12 ticks apart. A foe standing on the ground inside a band is crushed (gravity-press type); each foe is hit by each ring at most once and the three rings together are less than one ultimate. | Jump over each ring (airborne foes are not hit) or stand between the bands. The rings are drawn on the floor during the wind-up. |
| 15 | `lens_barrage` Gravity Lens Barrage | 60 | 30 | 400 | 4-32 | 3 | 9 per lens | Up to four marks (the first on the target, the rest on other foes or scattered), each a gravity lens (the same lens as Distortion Step) opened 8 ticks after the last. | Step off the ring and column that mark each lens during the wind-up. |

### Why this is still inside the Balance Law

* Every hit goes through `SpadeFx.crush` (= `bossDamage` plus the Tensura gravity damage type), so the Balance Law caps it (a sliver of the victim's maximum health, at most 40% of a player's health per hit) and the damage law scales it by power and by the victim's existence points.
* Crowd control only through the shared lock (`BalanceLaw.beginControl`): the new moves have none; they are damage and knockdown only.
* The incoming-damage budget of the framework (`dpsBudget`, 6% of his maximum health per second, 9% in the final phase) is a cap on how fast the party can kill him; with 7000 health the cap in absolute numbers is higher (420 / 630 a second), which is the point of a harder boss.
* No free healing: the one heal is the constructor setting him to full health once, under `BossHealGuard.allow`.

### Cost, worst case (rule 11)

| What | Worst case |
| --- | --- |
| Entities per move | None spawned. Throne: three delayed timers on the one boss; Barrage: up to 4 lens timers. All go through `later(...)` (the shared spell-runtime timer; each callback does nothing once the boss is dead or removed) and are wrapped in `SpadeFx.safe`. |
| Targets scanned | Once per ring (3) or per lens (4), on the foes of the arena (`foes(r)`, the framework's bounded list); no world scan, no chunk load. |
| Packets | The framework's `Tells` and `ZogratisVfx` effects, sent to nearby players only through the perf budgets (no new packet type). |
| Client draw | Throne tell: 3 rings drawn every third tick of the 28-tick wind-up; Barrage tell: up to 4 rings and columns. |

## 3. The new look (client side)

| Body | When | Quads (body + glow) | Far copy |
| --- | --- | --- | --- |
| `lucifero_v2` | base, up to 80% power | 2565 | 1123 |
| `lucifero_v2_form2` | from 80% power, wings spread | 2679 | 1145 |
| `lucifero_v2_gates` | the Gates are open | 2733 | 1132 |

All three are under the 2800 quad budget; the far copy is under 60% with no glow pass; the choice is the pure function `BossMeshRouting.luciferoMesh` (setting off: the old `lucifero` / `lucifero_form2`).
Only one body is drawn per Lucifero, and `MeshBossRenderer` already draws the far copy at distance.

* **Body.** A player-shaped body (Steve proportions scaled 1.34, 43 model units, about 2.7 blocks): head, two slim arms, two legs, in the grey skin of his devil form, long white hair with a fringe and long locks down the back, two pairs of horns (a big curved pair and a lower pair), pointed ears, a white-glowing pair of eyes, a third eye on the brow (glow) and veins.
* **Armour as separate parts.** `chest_plate` (collar, belt, rune gem that glows, ridges), `tasset`, `pauldron_l/r` (spiked, rimmed), `bracer_l/r` (fins), `greave_l/r` (knee spike, boot) and a `crown` (spikes, gem, horn guards), all in violet-black with gold. They are not part of the rig roles, so each follows the part it hangs from (the pose solver gives a part with an unknown name its parent's matrix), which lets the armour move with the limb without a new animation.
* **Wings.** Folded in the base body, spread in the second form. **Gates:** four wings are turned into four clawed jointed arms beside his two hands (the Six Limbs move), and a halo of eight horns floats behind the head.
* **Skins.** 1024 px skins (a lighter, finer shader than the old body so the face reads), glow maps for the eyes, the third eye, the rune gem and the veins.
* **Previews:** `docs/bosses/previews/lucifero_v2_blender.png`, `lucifero_v2_blender_back.png`, `lucifero_v2_blender_slam.png`, `lucifero_v2_form2_blender(.png|_back.png)`, `lucifero_v2_gates_blender(.png|_back.png)`.
* **Made in Blender** with the existing pipeline (`tools/blender/boss_lucifero_v2.py`, `bl_lib.py`, `bl_entity.py`): modelled, unwrapped and baked there, not hand-placed from code boxes.
* **Honest caveats.** The previews show a figure that is smaller than the wing span, so the face is small in them; "like the anime" is limited by a 2800-quad budget and a 1024 px skin, so it reads as a clean stylised player-body with armour, not as a cel-shaded portrait. The anime reference was not looked at again in this change (no wiki was fetched; the shape comes from the old design notes in `docs/arc/lucifero.md`). It has not been seen in a game.

## 4. Settings

| File | Key | Default | Effect |
| --- | --- | --- | --- |
| `nusmp-spade-server.toml` | `[spade] opMode` | true | OP Lucifero; false = the old boss (3500 health, 13 moves). Read when he is spawned. |
| client playermodel config | `[playermodel.lucifero] newBody` | true | The new bodies; false = the wave 3 Blender body. |
| `nusmp-bossdrops-server.toml` | `[bossdrops] enabled`, `chanceScale`, `maxStacksPerKill` | true, 1.0, 12 | The drops (`docs/bosses/boss_drops.md`). |

## 5. Tests

* `LuciferoTableTest` (updated): 15 moves in the order of the table; the OP numbers (3500 + 3500 = 7000, positive armour and toughness bonus, phase shift above 1, opening power 40, the new moves' damage is sane and they are not ultimates).
* `LuciferoV2Test` (new, reads the shipped meshes with the game's own parser): all 8 rig roles plus both wings, the nine armour parts each hanging from the part it follows, the halo on the Gates body, quads at most 2800, far copy under 60% with no glow, feet on the floor, skins square powers of two up to 1024, previews under 300 KB, the old bodies still there.
* `BossDropAssetsTest`: the setting `lucifero.newBody`, the routing strings of the three new bodies and of the old two, `spade.opMode` and the guarded modifier.
* `BossRework3Test` (old) is untouched and still describes the old bodies, which are kept.

## 6. Unverified, and for the owner

* Not compiled or run here; nothing was seen in a game. Please look at the three bodies in a game and say whether the face, the horns and the wings read as the anime; the budget leaves room to spend on the head if the owner wants it.
* The names "Weight of the Throne" and "Gravity Lens Barrage" and their numbers are design choices that follow the gravity theme of the manga fight, not canon moves; no wiki was fetched.
* OP numbers (7000, +6, +6, 40%, 12%) are one block of constants in `LuciferoTable` if they should move.
* Decision: should the other Zogratis siblings (Dante, Zenon, Lucius, Vanica) get the same OP treatment? Not done; the owner only named Lucifero.
