# Belial rework (the Djinn of Magi: Hakuryuu Ren's Djinn)

Source of every canon detail: **owner notes** (not checked against a wiki in this change; nothing here is invented beyond them). The owner notes say: roughly Low 6-B to 6-B attack
potency, faster than light, real danger is hax; the scythe Belior Goldorezza ignores physical durability by severing the target's spirit and sending it to another dimension; the
Extreme Magic is Belior Zauto; he bypasses every null, wields a scythe, and it is white. Wiki research (CLAUDE.md rule 10) was not done for this change.

## What it is

A **replacement** of the placeholder Belial (fire greatsword "Lord of Hollow Thrones", Extreme "Belial Inanis", acts hollow_cleave / throne_ash), behind the SERVER setting
`belialCanon` (default **true**). With it **off** the old Belial is back exactly (title, affinity, colours, look, Half Equip special, Extreme, weapon row, acts, items).

| Part | Canon Belial (setting on) |
|---|---|
| Title / form | "Severer of Spirits" / "Pale-Scythe Maso" |
| Look | white bone 0xE8EEF2, teal glow 0x58B8D8, spike horns, bat wings (membrane), no tail, no flames; affinity LIGHT (no new enum) |
| Half Equip special | SLICE (existing) with weakness, 60 ticks |
| Extreme Magic | "Belial Belior Zauto: Severance of the Spirit" (NOVA, radius 16, damage 30), goes through `BelialSever` |
| Weapon | scythe, items `djinn_weapon_belial_scythe` and `djinn_weapon_belial_scythe_dark` |
| Act 1 | `spirit_sever`: wide cone, every foe takes a sever hit |
| Act 2 | `exile`: nearest up to 3 foes are sent to "another dimension" for 3-6 s |
| Engravings | severance 3, soul_eater 1, barrier_piercing 2, magic_interference 1, enervation 1 (dark adds ruination) |
| Better Combat | `weapon_attributes/djinn_weapon_belial_scythe.json` and `_dark.json` (parent bettercombat:scythe, two-handed) |

## How the sever works (`BelialSever`, `BelialSeverRules`)

A sever is a hit of the damage type `anime_multiverse:spirit_sever`, tagged into `bypasses_armor`, `_cooldown`, `_effects`, `_enchantments`, `_resistance`, `_shield` and
`_invulnerability`. Armour, enchantments, Resistance, shields, the hit cooldown and the invulnerable flag do not apply. If another mod's listener still eats the hit, the missing part
is taken straight off the health (a sever is not a "magic" or "physical" blow, so no null has anything to nullify). It is never beyond the game's balance:

* amount = power + fraction of max health (act 8%, Extreme 12%); a player takes half the fraction, a boss 15% of the fraction and half the power;
* **per hit** cap: mob 30% of max health + 8, player 25% + 8, boss 3% (no floor), hard cap 400;
* **per second** cap per target (rolling 20 tick window): mob 60% + 8, player 40% + 8, boss 6%;
* then `BalanceLaw.cap` (PvP hit cap and the damage-scale ceiling) like every other hit;
* creative / spectator players and allies are never hit; an exiled target is not hit.

## How the exile works (`BelialExile`)

"Another dimension" is an **in-place ghost state**, not a real dimension change (safe, no chunk loads, no lost mobs): the victim is invisible, cannot act (mobs: NoAI + NoGravity;
players: Slowness 9 + Jump 128), is held at its spot every 4 ticks, cannot be hurt (everything except `/kill` and the void is cancelled) and cannot hurt, and monsters stop hunting it.

* 3 s base, +1 s in a Full Equip, +0.5 s for a dark Djinn; 3-6 s for a mob, **at most 4 s for a player**;
* never a boss (BalanceLaw / BossIdentity), never a creative or spectator player, never a player with a boss within 40 blocks (the "boss fight" guard); other mods add a guard with
  `BelialExile.addVeto(Predicate<LivingEntity>)` (no generic safe-area API exists in the mod, so safe areas are only covered through that hook);
* at most **3 victims per cast**, **4 per caster**, **24 on the server**; a victim that returns is immune for 5 s;
* victims that are refused or beyond the 3 take a lighter sever (half power, half fraction);
* cleanup: end of the time, victim dies / is removed, victim or caster logs out, dimension change, server stop. A mob saved mid-exile carries `nusmp_belial_exile` in its persistent
  data; when it is loaded without a running exile its NoAI / NoGravity flags are given back.

## Settings (SERVER, `nusmp-belial-server.toml`, section `belial`)

* `belialCanon` (default true): off = the old Belial.
* `exile` (default true): off = the exile act only severs.

## NEW / REPLACED / KEPT (CLAUDE.md rule 5)

* **NEW**: `BelialCanon`, `BelialConfig`, `BelialSever`, `BelialSeverRules`, `BelialExile`, `BelialSelfTests`; damage type `spirit_sever` and `bypasses_invulnerability` tag;
  items `djinn_weapon_belial_scythe[_dark]` (+ models, 16x16 icons, Better Combat files, grip class glaive, franchise map "magi"); acts `spirit_sever`, `exile`; lang; client roster row `BELIAL_BONE`;
  tests `BelialSeverRulesTest`, `BelialCanonTest`.
* **REPLACED (when `belialCanon` is on)**: Belial's title, affinity, colours, form name, look, trait, Extreme Magic (Inanis becomes Belior Zauto), the weapon you get from the
  Djinn engine (the scythe instead of the greatsword), his particle / roster style, his Extreme damage type (`belial_canon` -> `spirit_sever`).
* **KEPT**: the old Belial row in the `Djinn` enum, `djinn_weapon_belial` and `_dark` (still registered, loadable, listed), their acts, models, engravings and Better Combat files;
  all enum entries (nothing inserted); the old fire Extreme type entry.

## Worst-case cost (CLAUDE.md rules 9 and 11)

* An act touches at most 12 foes (`MAX_TARGETS`); a sever is one registry lookup, one hurt and a map lookup per foe; windows table swept every 40 ticks, at most 512 entries.
* Exile: at most 24 victims on the server, 3 per cast; per tick only a map iteration over those entries, a hold every 4 ticks, a puff every 10 ticks; two entity scans only at the moment an
  exile starts (a 40-block boss check for player victims, a 24-block hunter clear for mobs). No per-tick world scan, no chunk load (the return needs the chunk already loaded).
* Packets: none new. Particles go through the existing Djinn helpers (nearby players, perf budgets): per cast about 2 arcs of 24-32 points plus 2 small bursts per target.
* Client: one more roster row, no new per-frame work. A missing scythe mesh falls back to the icon sprite.

## Not done / not verified

* **Not compiled and not run** in this session (no working Gradle here); CI is the first compile. The new tests and self-tests (`belial.wiring`, `belial.sever`, `belial.exile`) are unrun.
* **No 3D model**: the scythe has no Blender mesh and no entity skin yet (rule 14 not met); in hand it shows the sprite (a new 16x16 white / dark scythe icon, drawn with PIL).
  TODO: model it through `tools/blender/bl_lib.py`, add `weapon_meshes/djinn_weapon_belial_scythe[_dark].txt` and skins `textures/entity/weapon/...`, then add the ids to the asset test lists.
* **`djinn/belial_bone/` full-equip pieces** are not hooked: `DjinnMeshes` loads `djinn/<key>.txt` by the Djinn's id, so the canon look cannot swap pieces without a loader change.
  TODO: one line in `DjinnMeshes` to look for `belial_bone/` while canon is on, once the pieces exist. Until then the old Belial pieces are drawn (coloured by the canon colours).
* `DjinnRoster36.java` is generated by `tools/relics/djinn36_java.py`; the `BELIAL_BONE` row and `keyOf` were added by hand, so a regeneration would drop them.
* Totem of Undying does not stop a sever (the type bypasses invulnerability); a boss is still cut to 3% per hit and 6% per second.
* Extreme Magic / acts bypass "magic immunity" by design of the type tags; other mods' own immunity systems that cancel damage events are answered by the direct health fallback.
* The old-Belial `DjinnSelfTests`/`DjinnTableTest` rows still hold with the setting off; with it on they test the engine, not the old row.
* Nothing was seen in a game: look, sounds, particle density and the feel of the two acts are unjudged.

---

# Belial rework 2: mind magic, dead weight, five senses, the skeletal grip, Dual Djinn

Source of every canon detail: **the owner's pasted notes** (authoritative). Wiki research (rule 10, English wiki first, Japanese wiki as fallback) was **not fetched** in this change; nothing here goes beyond the owner's text. Not covered: the dungeon parts (Illusionary Trials / Subjective Illusion Shifting, Manifestation of Inner Conflict, Punishment of Lies / Flesh Punishment of Lies), by the owner's word "not now".

Lore facts (also in the lang key `belial.lore`): Belial is the 68th Djinn of the 72, the Djinn of Truth and Conviction (Shinjitsu / Danzai), 8th Type (Life Magic); first owner Hakuryuu Ren, his Metal Vessel is the left shoulder armor with Belial's eight-pointed star; his Household Vessel was wielded by Seiryuu Ri; he can change size freely and Dual-Equips with Zagan (both Life Magic) without switching forms. Djinn Form: five eyes, gold jewelry, dragon bones on the upper arms and in the swept-back hair. Full Equip: long white-silver hair, two extra eyes on the forehead, four arms, jagged black wings, a spectral skeletal dragon that lunges to grab, crush and restrain. (Look only; the 3D parts are the Blender TODO above.)

All of it is behind the SERVER setting `belialCanon` and its own sub-setting (defaults on). Everything is server-side, keyed by UUID, cleaned up on logout, death and dimension change.

## Plain-words rules

| What | How it is used | Rule |
|---|---|---|
| **Belior Goldoreza** (act 1, `spirit_sever`, shown under this name now; id kept) | right-click | the sever as before, plus **dead weight**: the struck foe is numb and limp: Slowness I and Weakness I and **no normal healing** (regeneration, potions, food: the heal event is cancelled) for 18 s (mob) / 8 s (player), +2 s in a Full Equip, at most 20 s. A boss gets no dead weight, only the sever's boss caps (3% per hit, 6% per second). Setting `deadWeight`. |
| **Belior Zakera** (NEW act 3, `belior_zakera`) | sprint + right-click, from the Half Equip | foes of a cone (at most **6**) forget the fight. **Reach 10 blocks in a Half Equip (the base Metal Vessel), 24 in a Full Equip.** A mob loses its target for 3-6 s and cannot pick a new one for the first 3 s, except the foe it was turned on (half the time another foe of the same cone). A player gets distorted senses: Nausea (4-6 s) and Darkness (3 s), then the balance law's control-time cap and diminishing returns. A boss is never mind-controlled: only Darkness 2 s and a small strain through the sever's boss caps. **Counterplay:** a target that holds a shield up, or carries a barrier effect, is not affected but takes a small bounded mental strain (25% of the act's power through `BelialSever`, hence `BalanceLaw.cap`) and a short Nausea (2 s) and Darkness (1.5 s). The mod has **no Borg model**; the shield and the barrier effect are the nearest things it reads. Setting `zakera`. |
| **Belior Zauto** (Extreme Magic, changed) | the Extreme Magic key | as before (a sever, 12% of max health per strike), the cast now shows the chant **"Spirit of Truth and Judgement — Assemble your power and bring to the earth the nothingness to judge everything!"** (lang `belial.chant.zauto`), and the blurb says it is a roar: **every victim loses its five senses** for a bounded time: sight (Blindness), hearing (Nausea), touch (Slowness II), taste (Weakness), smell (Mining Fatigue). 5 s for a mob, 3 s for a player, +1 s for a dark Djinn, never more than 7 s; Blindness, Nausea and Slowness go through the control-time cap. A boss loses none (only the sever's caps). **The permanent loss and the soul sent to Ill Ilah's dimension are flavour text only**: the loss is time-limited by design. Setting `senses`. |
| **Skeletal dragon grip** (NEW, passive) | in a Full Equip, the bearer's melee blow | the dragon grips the foe that was just struck and holds it where it stands for **2 s**; the victim stays hittable (a mob has no AI, a player is slowed to a stop and pulled back if shoved); **one victim per caster, 12 s cooldown per caster** (owner floor: 10 s), 16 holds on the server, the victim is immune for 5 s afterwards. Never a boss, a creative/spectator player, an ally, an exiled target, or anything `BelialExile.addVeto` guards (the exile's own guards, including the 40-block boss check for a player). Setting `boneGrip`. |
| **Dual Djinn** (NEW) | look straight down (pitch 75 or more) + right-click with Belial's scythe | see below. Setting `dualDjinn`. |

### Dual Djinn: what exists and what does not

The mod has **one Djinn per player and one engine state** (`DjinnState` holds one `djinn`, one phase, one weapon loan; a household member holds a lent one only if they own none; the ledger has one owner per Djinn). A real second simultaneous equip would need a second engine state, a second vessel in the loan/overlay, a second Extreme clock and HUD sync: not done.

The smallest safe version, what is built: the player owns the **canon Belial** and is **also a member of Zagan's household** (the ledger entry only, no second Djinn record). `/multiverse djinn share` and `accept` now allow that one case (`HouseholdRules.dualLink`: setting on, ally owns the canon Belial, the Djinn lent is Zagan; a Spirit or Devil still refuses; Zagan's household stays at two). While the link is live and the Belial Equip is Half or Full, a right-click with the scythe **looking straight down** casts **Zagan's first act (Bloom Heal) through Belial's weapon**, with its own cooldown slot (`ActClock` slot 3), Zagan's Magoi cost paid from the bearer's pool, at 85% of Zagan's power, Belial's Equip never changes. `/multiverse djinn leave` ends it; if Zagan's owner releases, the ledger clears the household and the link is gone. **Not done:** Zagan's second act, his Extreme Magic, his own weapon or his vessel item at the same time, and the other direction (a Zagan owner receiving Belial). The Metal Vessel item is not changed.

## Files

* **NEW:** `BelialMindRules`, `BelialMind` (Zakera, senses, dead weight, mind table and its events), `BelialGripRules`, `BelialGrip`, `BelialDual`; tests `BelialMindRulesTest`, `BelialGripRulesTest`, `BelialDualTest`; self-tests `belial.mind`, `belial.grip`; `tools/lang/belial_zakera.json`; the act `belior_zakera` (third act of the canon row, `Spec.three`, `ACT_THREE`, `ACT_DUAL`, `DjinnWeaponRules.pickAct`); settings `zakera`, `senses`, `dualDjinn`, `boneGrip`, `deadWeight`.
* **REPLACED (text only):** the display name of act 1 (`Spirit Sever` -> `Belior Goldoreza`) and its description, the scythe item names (`Goldorezza` -> `Goldoreza`), the Extreme blurb of the canon Belial. Ids are all kept.
* **KEPT:** the old Belial row, the two acts' ids and numbers, the exile, every other weapon (`Spec.three` is null for them, the selection is unchanged for them), all saved data.
* **Shared files that only got appended lines or one-line hooks:** `DjinnWeaponRules` (constants, a record component with the old 7-argument constructor kept, `pickAct`), `ActClock` (slots 2 and 3), `DjinnWeaponActs` (act selection, dual path, `belior_zakera` case, dead weight call), `DjinnWeaponItem` (third tooltip line), `DjinnEvents` (two `register` lines), `DjinnMagic.hit` (one line, senses), `DjinnEngine` (one line, chant), `BelialExile` (`vetoed` made package-private, one line refusing a mob that is gripped), `BelialConfig`, `BelialCanon` (blurb), `HouseholdRules` and `DjinnHousehold` (the dual link), `BelialSelfTests`, `en_us.json`.

## Worst-case cost (rules 9 and 11)

* **Zakera:** one cone query (`Fx.foesAround`, at most 24 entities, radius at most 25.5) per cast; at most 6 targets; effects are vanilla (their own packets to tracking players); at most 6 small particle bursts and two arcs of 20-24 points per cast. Table: 48 forgotten mobs on the server, 12 per caster; a once-a-second sweep over at most 48 entries; no per-tick work while it is empty; the redirect picks from the same cone (no extra scan).
* **Senses:** at most 24 victims per Extreme Magic (the burst's own cap), 5 effects each, once per victim and cast.
* **Dead weight:** at most 128 entries on the server, swept once a second; the heal event does one map lookup, only while the map is not empty.
* **Grip:** at most 16 holds; per tick a map iteration over those (one entity lookup and a few field writes each), a hold every 4 ticks, a puff every 10; at start for a player victim one 40-block boss query (the exile's guard), done only after the cooldown and busy checks. Packets: at most 5 hold teleports a second for 2 s once per 12 s per caster. The blow listener exits at its first line unless a player's melee blow.
* **Dual:** one ledger lookup per right-click that looks down with the scythe in hand; nothing per tick.
* No chunk load, no block change, no new packet type, no client class.

## Not done / not verified

* **Not compiled and not run** (no Gradle here); CI is the first compile. The new JUnit tests and the self-tests `belial.mind`, `belial.grip` (and the extended `belial.wiring`) are unrun. The smoke prefix `belial` was already in the list; no new token.
* The heal block assumes `LivingHealEvent` is fired for vanilla `heal` (it is relied on elsewhere in the mod). The sever, direct `setHealth` heals and heals of other mods that bypass the event are not blocked. Totem of Undying is not affected.
* Tensura's barrier *skills* are not read as a Borg (only a raised shield and a barrier-named effect are); a magician with a Tensura barrier skill is not blocked.
* Zakera's mind effect cannot be seen in a game by this change: whether a mob's AI re-picks a target through a path that does not fire `LivingChangeTargetEvent` is unknown. Mobs that were turned on a foe keep it after the 3-6 s (vanilla).
* Sprint + right-click and look down + right-click are the selectors because the weapon has no key for a third act; whether sprinting survives the right-click on every client is not tested.
* The model, wings, four arms, extra eyes and skeletal dragon are still the Blender TODO of the first rework; the grip is only particles and sound.
* Nothing was seen in a game; every number (2 s, 12 s, 8-20 s, ranges 10/24, the 85%) is a design number of this change, the owner's text names only the ranges, the 2 s, the 10 s floor and about 20 s.
