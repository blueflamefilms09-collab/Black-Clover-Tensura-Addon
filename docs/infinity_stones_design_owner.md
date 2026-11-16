# Infinity Stones: lore summary and design for multiverse-of-anime (nusmp)

> **For the Claude session building the mod.** Researched 2026-11-16 from:
> - the Marvel Database wiki (marvel.fandom.com: `Infinity Stones`, each stone page, `Infinity Gauntlet`), which summarises the comics
> - the Marvel Cinematic Universe wiki (marvelcinematicuniverse.fandom.com: `Infinity Stones`, each stone, `Infinity Gauntlet`, `Nano Gauntlet`, `Scepter`, `Ultron`, `Iron Man Armor: Mark LXXXV`, `Snap`)
>
> Lore is summarised in our own words. **We did not read every Marvel comic.** The comics wiki's summaries of the comics are the source. Design numbers are **proposals to tune**, not tested balance.

---

## Part 1: What the stones are, in canon

### 1.1 Shared rules (comics + MCU)
- **Six stones**, each the embodiment of one aspect of existence: **Space, Mind, Reality, Power, Time, Soul**.
- **Wielding is dangerous.** Only beings of great power can hold or use one directly.
  - MCU: weaker beings are burned, banished or **disintegrated**. Even the strong feel pain.
  - The Power Stone's backlash scales with the size of the target it touches.
- **Containers / conduits** make them usable:
  - **Tesseract** (Space)
  - **Loki's Scepter** (Mind)
  - **Aether** (Reality)
  - **Orb + Ronan's Universal Weapon** (Power)
  - **Eye of Agamotto** (Time)
  - **Infinity Gauntlet** (all six)
- **The stones are sentient.**
  - The Space Stone banished the Red Skull.
  - The Mind Stone subtly drove Loki and stirred up the Avengers' anger.
  - The Mind Stone gave Ultron and Vision sentience.
- **Gamma radiation:** all six together nearly overwhelm an ordinary human (MCU). Only someone resistant (Smart Hulk) or very strong (Thanos) can use the full set and survive. **Tony Stark died** using them through the **Mark LXXXV** armor, which lacked the Gauntlet's protection.
- **Comics "feedback circuit"** (after the stones were remade). Mastery of one stone powers the next:

  > **Mind → Power → Space → Time → Reality → Soul → Mind**

  For example, owning the Mind Stone lets you use the Power Stone better, and owning Power improves Space.
- **Comics extras:** later comics add a **Death Stone** (sense who is about to die, and induce death). Older comics had **Ego** as a seventh gem. Both are optional content.

### 1.2 Each stone's canon powers
| Stone | Colour (MCU) | Canon powers | Notable limits / drawbacks |
|---|---|---|---|
| **Space** | blue | Teleport self and others anywhere you can picture, ignoring walls and wards. Portals and wormholes, interdimensional travel. Shifts the distance between objects. More speed. Being in many places at once (peak). Near-unlimited energy source for weapons and engines (HYDRA weapons, light-speed engine). Can empower a person (Captain Marvel). | Prolonged contact melts steel or banishes weak holders through a wormhole. Gamma emission lets others track it. |
| **Mind** | yellow (blue in the Scepter) | Telepathy, telekinesis, empathy. **Mind control** by touching the heart (indefinite loyalty). Astral projection and long-range mental contact. Grants sentience to AI. Subtly influences minds nearby, even without a holder. | Control breaks from **a hard blow to the head** (unconsciousness resets the mind) or Scarlet Witch's magic. Blocked if the energy can't spread into the body (Stark's arc reactor absorbed it). |
| **Reality** | red (Aether) | Rewrites reality. **Illusions** real enough to touch and fight. **Transmutes matter**: guns into bubbles, wreckage into bats, bodies temporarily split apart (non-lethal, reverts). Area changes when combined with Space. Comics: alters natural laws locally or universally. | The illusions and transmutations Thanos used on living things were **temporary**. |
| **Power** | purple | Raw energy: beams, waves, blasts, **empowered punches**. Passive **strength and durability** (Ronan survived a moon-killer shot). Comics: grants nearly any physical superpower, effectively invincible, and **fuels the other stones**. Can destroy planets. | The most volatile: **explodes against organic matter** without a container. Groups can share the load (the Guardians linked hands). |
| **Time** | green | Rewind, forward, freeze or loop time for a **selected area or object**, excluding chosen people. Restores destroyed things (the Sanctum, a torn book). **Views possible futures** (Strange saw 14,000,605). Comics: time travel, ageing control, **trapping foes in endless time loops**. | Hard to control without knowledge. A wielder can be overwhelmed. |
| **Soul** | orange | Control over **life and death**. See, attack, **steal or trap souls** (pocket world **Soulworld**). Finds the real body among illusions. Forces the astral form out of the body. Gives objects sentience. Comics: evolve or devolve beings, return them to their natural state. | **Obtained only by sacrificing someone you love** (Vormir). Those sacrificed **can never be revived**, not even with all six. The unworthy are cursed to guard it (Red Skull). |

### 1.3 Carriers that can hold stones
| Carrier | What canon shows |
|---|---|
| **Infinity Gauntlet** (Uru) | Holds all six. Stones snap into their slots and send a painful power surge through the wearer. A **fist** is enough for small effects; a **snap** is needed for universal feats. Restraining the hand blocks it. A full-power use **burns the gauntlet and the wielder**. Very durable, and usable as a melee weapon. |
| **Nano Gauntlet** (Stark nanotech) | A copy of the Infinity Gauntlet that resizes to its user. Has the same snap and the same drawbacks. The Hulk's arm was permanently burned by using it. Its nanites can **move the stones into a compatible Iron Man suit**. |
| **Iron Man Mark LXXXV** | Stark's strongest suit: strength, durability, flight, repulsors, unibeam, finger lasers, a HUD that sees through walls. It **took the stones from the Nano Gauntlet** and snapped. Because it **doesn't shield** the wearer from stone energy, **Tony died**. |
| **Loki's Scepter** | Holds the **Mind Stone** only. Gives energy blasts, a **barrier**, mind control by touching the heart, astral communication, and a passive anger aura. Also works as a bladed melee weapon. |
| **Ultron** | Created from the Mind Stone's intelligence. Robot body with strength, durability, flight, a hive mind of drones, hacking, and data upload. (Comics: Ultron has also used the Soul Gem.) |

### 1.4 The Snap
- **MCU:** all six stones plus a snap can **kill or revive, destroy or recreate**, at any scale from one group to half of all life. Victims turn to dust.
  - **Hulk's snap** brought everyone back.
  - **Tony's snap** erased **only Thanos and his army**.
- **Cost:** it heavily damages the gauntlet and the user. Tony died from it, and Hulk's arm was crippled.
- **Never revivable:** souls sacrificed to the Soul Stone.

---

## Part 2: Design for the mod

### 2.1 The Infinity Stone slot (1 billion EP)
- **New Curios slot type:** `infinity_stone`, 1 slot. Equip through your existing relic system (`RelicDef` / `RelicCurioEquip` / `CurioEquipRules`).
- **Unlock:** max EP ≥ **1,000,000,000**. Grant the slot when this is reached (and the gamerule `nusmp_infinity_stones` is on). Remove it, and unequip the stone safely, if EP drops below 1B.
- **Each stone** is a relic at **Grade AKASHIC (EX-Singularity)** with:
  - **2 actives** (`Act`): cooldown, cost as a % of max magicule, optional uses per day or hold/toggle
  - **2–3 passives** (`Passive`: attribute, effect, resistance, flag)
- **Only one stone** may be worn in the slot. Carriers (2.4) are how you hold more.
- **⚠ Balance note:** `BalanceLaw.epScale` caps at ×12 around **25M EP**. At 1B EP, normal skill damage no longer grows. Stones should use **effect-based power** (control, rule-breaking, EP-ratio effects) and **% of max-MP costs**, not bigger flat damage. They also need a **separate scaling rule**, for example `stoneScale = 1 + log10(EP / 1e9)`, capped at 2.
- **Canon flavour to keep:**
  - Equipping sends a **power surge**: brief damage and screen flash.
  - Each stone **glows in its colour** while worn (`RelicWornLayer`).
  - Stones are **sentient**: rare whispers in the action bar (lang keys).

### 2.2 The six stones: 2 actives + passives each
Costs are a % of max magicule. Cooldowns are in seconds. These are starting proposals.

#### Space Stone (blue)
- **Active 1, Portal Step [press]:** teleport to the block you're looking at (up to 128 blocks), or **shift** to open a 2-way portal pair for 30 s that allies can use. 3% MP, 8 s.
- **Active 2, Spatial Fold [held]:** pull every enemy within 16 blocks to a point in front of you, or **shift** to push them all away (distance warp). It respects `FORCE_MOVEMENT_EVENT` / `NO_FORCED_MOVE`. 6% MP, 25 s.
- **Passives:**
  - +30% movement speed
  - **ignores teleport-blocking**: walls don't stop Portal Step; warded areas still do, to respect protection mods
  - can't be pulled or knocked back by non-ultimate skills

#### Mind Stone (yellow)
- **Active 1, Dominion [press, touch range]:** a **non-boss** target under 60% of your EP becomes a temporary ally for 60 s; players get 5 s of "Confused" instead. Canon counter: taking a heavy hit (≥ 30% max HP) **breaks** the control. 8% MP, 90 s.
- **Active 2, Psionic Reach [toggle]:**
  - telepathy HUD: see all entities' health, EP and targets within 48 blocks
  - telekinesis: hold to lift a target and throw it
  - drains 1% MP per second
- **Passives:**
  - immune to mind control, confusion and fear effects
  - +25% mastery gain (learning speed)
  - nearby enemies get −10% attack (the "anger aura" turned against them)

#### Reality Stone (red)
- **Active 1, Rewrite [press]:** transmute the looked-at target's held projectile or weapon. Its next attack deals **no damage** (canon: guns into bubbles). On mobs, briefly turns them into a harmless creature for 8 s, reverting afterwards (canon: non-lethal and temporary). Not usable on bosses. 7% MP, 45 s.
- **Active 2, False World [held]:** create an illusion clone of yourself that fights for 20 s while you turn **invisible**. Enemies target the clone (canon: Knowhere). 6% MP, 60 s.
- **Passives:**
  - 20% chance to make an incoming non-ultimate **debuff not happen**
  - see through invisibility and illusions
  - +15% to all elemental damage

#### Power Stone (purple)
- **Active 1, Power Surge [charge-held]:** a beam with charge levels (use your `ChargeRules`). At full charge it's a purple wave that knocks everything back.
  - Damage through `BalanceLaw.damage` × `stoneScale`.
  - **Recoil:** take 5% max HP unless you're in a carrier (canon: it hurts to use without a conduit).
  - 4% MP per charge, 20 s.
- **Active 2, Infinite Might [toggle, 30 s]:** +100% attack, +50% armor and knockback resistance, and your punches cause explosions. 10% MP, 120 s.
- **Passives:**
  - +40% max HP and armor
  - **all your other skills cost 15% less** (canon: powers the other stones)
  - empowers any machine or relic (relic cooldowns −20%)

#### Time Stone (green)
- **Active 1, Rewind [press]:** go back to your position, HP and MP from **5 seconds ago**. **Shift** to rewind a **block area** (repairs griefed or broken blocks within 8 blocks of the last 30 s). 8% MP, 60 s.
- **Active 2, Time Lock [held]:** freeze every enemy in a 12-block sphere. Bosses get 3 s, players 5 s, others 10 s. Excludes you and allies. Built as a "frozen" effect, not a world freeze. 12% MP, 120 s.
- **Passives:**
  - all your cooldowns tick 25% faster
  - immune to slow and time effects
  - **Foresight:** a once-per-60 s auto-dodge of a hit that would deal 20% or more of your max HP

#### Soul Stone (orange)
- **Obtaining it (canon-inspired, not evil):** the **Vormir trial**. Players offer their **highest-mastery skill**, **50% of their max EP** or a **named subordinate** at the altar. What's offered is gone for good (canon: sacrifices can never be revived). No killing other players is required.
- **Active 1, Soul Grasp [press]:** rip out the target's soul for 3 s. It can't act, and you see its skills. Mobs below 40% of your EP (non-boss) are **taken into Soulworld**: an instant kill with the loot sent to you. 9% MP, 90 s.
- **Active 2, Soulworld [press]:** open a small pocket dimension for 15 s; you and allies inside heal 10% max HP per second. Or **shift** to see the **true body** (reveals real players among clones and illusions). 6% MP, 120 s.
- **Passives:**
  - once per 10 minutes, **survive death** at 20% HP
  - spiritual (soul) resistance +40%
  - your summons and subordinates gain +25% stats

#### Optional: comics-only stones
- **Death Stone** (comics): sense nearby entities below 15% HP, and execute a non-boss target below 10% HP once per minute.
- **Ego Stone** (comics): skip unless you want a 7th slot item.

### 2.3 Stone synergy: the comics feedback circuit
Bonuses apply when a player **holds two linked stones** through a carrier (2.4). **Mind → Power → Space → Time → Reality → Soul → Mind**: the earlier stone in each pair gives the later one **+25% effect and −15% cooldown**. This rewards collecting stones in order and makes carriers meaningful.

### 2.4 Carriers: how players hold more than one stone
| Carrier | Holds | Extra powers | Drawback (canon) | How to obtain (proposal) |
|---|---|---|---|---|
| **Infinity Gauntlet** (Uru, hand/curio) | **6** | Use any socketed stone's actives through one ability wheel. **Combo casts** (2 stones at once, e.g. Space + Reality = terrain pull, Soul + Time + Reality = full heal). **The Snap** when all 6 are socketed. Melee weapon. | Socketing a stone: surge of 10% max HP damage. Restraining effects (stun, bind) **block all use**. The Snap burns the gauntlet (repair needed) and the wielder (see 2.5). | Forged at Nidavellir-style boss altar with Uru ingots from a raid boss |
| **Nano Gauntlet** (Stark tech) | **6** | Same as the Infinity Gauntlet. Can **transfer stones to a Mark LXXXV** suit worn by the same player instantly. | Weaker protection: the surge is **2×**, and after a Snap you're **left with a long "Burned Arm" debuff** (−50% attack with that arm's hand, 1 hour) | Crafted from Stark tech parts + Vibranium |
| **Iron Man Mark LXXXV** (armor set) | **6** (in the gauntlet) | The suit gives flight, repulsor beam, unibeam, finger lasers, and an X-ray HUD. Lets you **Snap**. | **No stone shielding:** using the Snap through the suit **kills you** (with a canon-themed death message) unless you have Soul Stone revive or anti-death. A **heroic last stand**. | Suit crafted at a Stark workbench |
| **Ultron armor** (vibranium body) | **2** (chest core) | Robot body: no hunger, poison or bleeding. Flight. **Ultron Sentries** (summon drones, hive mind). Hack: disables enemy relic actives for 5 s. Gets the **Mind Stone sentience bonus** (+mastery) if it holds Mind. | Weak to lightning and EMP. Each socketed stone adds −10% max HP (unstable core). | Boss drop from an Ultron boss (fits your boss system) |
| **Loki's Scepter** (weapon) | **Mind only** | Energy blast, bullet-blocking **barrier**, touch mind control (Dominion with a 2 s cast), astral projection (scout as a ghost for 20 s). | The anger aura affects **you** too: +10% damage taken from allies' friendly fire if it's on. | Boss drop from a Chitauri / Loki-themed boss |

Other canon containers make good **decorative quest items**: the **Tesseract** (Space: a portable recharge station for your portals), the **Aether** (Reality: a corrupting liquid weapon), the **Orb** (Power: safe storage, and unsafe to open), and the **Eye of Agamotto** (Time: an amulet that lets a Time Stone user **view futures**, a "where is the boss / dungeon" reveal).

### 2.5 The Snap: "not evil" options (pick any)
The Snap needs all 6 stones in the Infinity Gauntlet, Nano Gauntlet or Mark LXXXV, and a **5 s held gesture that is interrupted by any stun or bind**. It has a **long server cooldown**: once per in-game week per player, or once per server per real day (config).

Options a Snap can offer (a selection menu when it fires):
1. **Restore Snap (Hulk's snap):** fully heal and revive all allies and subordinates in render distance; clears all debuffs. Can't revive Soul-Stone sacrifices.
2. **Erase the Army (Tony's snap):** dust **only hostile mobs and your enemies' summons** within 128 blocks. Bosses take 50% max HP instead. Players are never dusted.
3. **Rebuild Snap:** repair every block griefed or destroyed in a 128-block radius over the last 10 minutes (Time + Reality).
4. **Purge Snap:** remove every curse, corruption and negative status from everyone you choose (Reality + Soul).
5. **Grant Snap:** give all allies in range a temporary ultimate-tier buff (+100% stats for 5 minutes) (Power + Mind).
6. **Bring Back Snap (if enabled):** revive one dead player at their death spot with their items (Soul + Time).
7. **Rewrite the Sky:** cosmetic and world effects such as clearing weather, setting time of day, or regrowing a biome (Reality + Time).
8. **Cast Back the Stones:** scatter all six stones to random dungeons across the world. A "reset" that restarts the hunt (comics: the stones scatter after use).

**Snap costs (canon):**
- **Infinity Gauntlet:** the gauntlet breaks (needs Uru to repair), the wielder loses 50% max HP for 1 hour, and the stones go dark for 24 h.
- **Nano Gauntlet:** as above, plus the **Burned Arm** debuff.
- **Mark LXXXV:** **the wielder dies.** A heroic sacrifice that grants the Snap at **double strength**.
- Every Snap is broadcast to the server with a lang key ("<player> snapped their fingers…").

### 2.6 Server-wide rules
- **One of each stone per server**, like trnightmare's God Class ownership (`SavedData` registry). If the holder leaves for 7+ days, the stone **returns to its dungeon or boss**. Config option: allow duplicates.
- **Gamerule** `nusmp_infinity_stones` (default on) and `nusmp_snap` (default on, set it off for PvP servers).
- **Stones can't be stored** in chests or ender chests. They **teleport back to the holder** or to their shrine (canon: the stones are sentient and seek wielders).
- **PvP safety:** every effect obeys `BalanceLaw.cap` (40% max HP per hit). Dominion, Soul Grasp, Time Lock and the Snap have reduced or no effect on players.

### 2.7 Where each stone is found (proposal)
| Stone | Canon place | In-mod source |
|---|---|---|
| Space | Tesseract (Asgard / S.H.I.E.L.D.) | End-dimension raid: "Tesseract Vault" |
| Mind | Loki's Scepter | Drops from a Loki/Chitauri boss inside the Scepter; pry it out to use it in a slot |
| Reality | Aether (Svartalfheim) | Dark-elf world dungeon |
| Power | Orb on Morag | Ruined-temple dungeon; opening the Orb without a container explodes |
| Time | Eye of Agamotto (Kamar-Taj) | Sorcerer's sanctum quest; winning a duel against a time-loop boss |
| Soul | Vormir | "Vormir" cliff shrine, sacrifice trial (2.2) |

### 2.8 Build checklist (uses the mod's existing systems)
1. Add the Curios slot type `infinity_stone` (data/curios/slots + entities json). Grant it via `CurioEquipRules` when EP ≥ 1e9.
2. Make six `RelicDef`s at Grade AKASHIC with `Act.of(...).cooldown(...).mana(%)` and `Passive.attr/effect/resist/flag`. Costs come from **% of max MP** (`Act.mana`), not flat.
3. Add a `StoneOwnership` `SavedData` for the one-per-server rule, plus a return-to-shrine timer.
4. Carriers:
   - **items**: gauntlets as Curios hand-slot relics; Scepter as a weapon; Mark LXXXV and Ultron as `RelicArmorItem` sets
   - **sockets**: in item data components
5. Synergy circuit: a helper that reads the socketed stones and applies the +25% / −15% bonuses.
6. The Snap: a menu (client screen) → server packet → validate the 6 stones, cooldown and gamerule → apply the chosen effect → pay the cost → broadcast.
7. VFX through `SkillFx`, with the stone colours: blue, yellow, red, purple, green, orange.
8. Lang keys for every name, mode, message and death message. Icons for the 6 stones and 5 carriers.
9. Self-tests (your harness): equip gate at 1B EP, one-per-server, Snap validation, and that no player is dusted.
