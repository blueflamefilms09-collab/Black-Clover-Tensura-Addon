# Infinity Stones: canon audit of the owner's design

Audit of `docs/infinity_stones_design_owner.md` against the Marvel wikis and against this repo (4.8.x). Nothing was built or changed.
Verdicts: **OK** = matches canon. **FIX** = canon says otherwise, or it breaks a repo rule. **ADD** = canon has it, the design lacks it. **MOD** = a mod-only invention (fine, but not canon).

Source tags: **[MDB]** = marvel.fandom.com (comics summaries), **[MCU]** = marvelcinematicuniverse.fandom.com, **[repo]** = a file in this repo.
Everything was read in English; the Japanese fallback was not needed. Lists of what could not be fetched are in section 9.

---

## 0. Read this first: five things the design does not know

| # | Finding | Why it matters |
|---|---|---|
| 1 | **The six stones, the Infinity Gauntlet, a Snap and a Greater Wish already exist** (Wave 30, `relics/wave/Wave30Stones.java`, `Wave30.java`, `Wave30Tune.java`, `Wave30Rules.java`). They are God-grade CHARM relics, work when merely **carried** (inventory or Curios), have **no EP gate**, and are D&D-flavoured (franchise `dnd`). The Gauntlet (HANDS, Genesis) adds +12% per carried stone; with all six it has a once-a-day mob-only Snap (half current health of hostile mobs within 48 blocks, a boss only 10% of max, never a player, never a block, 30 s toll) and a 5-option Greater Wish. A `mind_stone` helm (Wave 26, Vision's stone) also exists. Models, worn models, advancements (`stones_all.json`) and a quest chapter exist too. | CLAUDE.md rule 5: this is a **rework**, not a new feature. Rule 2: the old items and acts stay registered. The design's "six new RelicDefs" would create **twelve** unique stones. |
| 2 | **One-per-server already exists**: the God of Order's ledger (`relics/order/UniqueLedger`, `LedgerRules`, `RelicScanner`) keeps one valid serial per relic id and flags duplicates. | Design 2.6 `StoneOwnership` SavedData duplicates it. Reuse the ledger; add only the "return to shrine" timer. |
| 3 | **Grade AKASHIC lifts the wearer's max EP by +20%** (`Grade.epBonus`, applied in `RelicEngine` line ~198 as `ADD_MULTIPLIED_TOTAL`). | **Gate loophole**: a player at 0.85 billion base EP equips the stone, gets +20%, passes 1B, and the gate is satisfied by the stone itself. Worse, if the gate re-checks every second, the stone can flap (unequip drops EP below 1B, which unequips it). The gate must read EP **without** the stone's own bonus. |
| 4 | **The slot cannot be "granted" at 1B EP.** Curios slots are data (`data/anime_multiverse/curios/slots/*.json`, `entities/relics.json`) and this mod does not compile against the Curios API (`CurioEquipRules` says so). All existing slots (`relic_head`, `charm` ...) are always present. | Add `infinity_stone` (size 1) as an **always-present** slot and gate the **equip** (and re-check each second). "Remove the slot if EP drops" is not possible; "stone goes silent, stays in the slot" is. |
| 5 | **The design's own numbers break existing balance rules** in three places: control lengths (Time Lock, Dominion), healing (Soulworld 10%/s) and boss damage (Erase the Army 50%). See 3, 4 and 5. | `BalanceLaw.controlTicks` caps: 5 s mob, 3 s player, 1 s boss. `BalanceLaw.heal`: 50% max-health cap per cast. Repo convention: share-based acts take at most 10% of a boss. |

---

## 1. Canon facts used (with sources)

### 1.1 The feedback circuit (the design has the right order)

[MDB] pages `Space Gem`, `Power Gem`, `Time Gem`, `Reality Gem`, `Soul Gem`, `Mind Gem` (each "Properties") and `Infinity Gems` ("Infinity Watch" list):

| Stone | "how much of its potential you can tap depends on your mastery of ..." | Possessing the other stone gives better control of ... |
|---|---|---|
| Mind | Soul | Power |
| Power | Mind | Space |
| Space | Power | Time |
| Time | Space | Reality |
| Reality | Time | Soul |
| Soul | Reality | Mind |

So **Mind -> Power -> Space -> Time -> Reality -> Soul -> Mind** is correct, and "the earlier stone helps the later one" is the right direction. **OK.**
Extra canon the design lacks: with all six the circuit is a **positive feedback loop** and the Gems "exceed cosmic beings like Eternity" but only **work in their native universe**; only the Living Tribunal (and God Emperor Doom via the Beyonders) overcame them ([MDB] `Infinity Gems`, "Infinity Watch").

### 1.2 Per-stone canon, comic versus MCU, with limits and costs

| Stone | Comics ([MDB]) | MCU ([MCU]) | Real limits / costs |
|---|---|---|---|
| **Space** | Teleport self and others anywhere you can picture, "regardless of distance or preventive measures, such as walls or spells"; speed; gravity manipulation; black holes; be in many places at once at peak (`Space Gem`). | Portals and wormholes; Tesseract is the container; "unlimited" energy source (Hydra weapons, Light-Speed Engine); can empower a person (Carol Danvers) (`Space Stone`). | Prolonged contact melts steel or banishes weak holders through a wormhole; **banished the Red Skull to Vormir** as unworthy; gamma emission lets others **track** it (`Space Stone`). |
| **Mind** | Telepathy, telekinesis, empathy; at peak links with all minds at once (`Mind Gem`). | Touch-to-heart mind control via the Scepter; astral projection; **grants sentience** (Ultron, Vision); reshapes matter (Loki's scepter and armour, Vision's density); hacking (burned Ultron out of the Internet); beams; enhances lesser beings, lethally (Maximoffs) (`Mind Stone`, `Scepter`). | Control breaks with a blow to the head or Scarlet Witch's magic; **blocked by the arc reactor**; the stone itself **influenced Loki** and started the Helicarrier quarrel (`Mind Stone`, `Scepter`). |
| **Reality** | Alter reality, resurrect the dead, distort reality; after the rebirth it observes other realities and talks to other Reality wielders; aliases Aether, Heart of Gaia, Wuxian Seed (`Reality Gem`). **Warlock beat Magus with a fake Reality Gem** (`Infinity Gems`, "Infinity War"). | Aether is a liquid; illusions you can touch (Knowhere), transmutation (guns to bubbles, wreckage to bats), bodies split apart (`Reality Stone`). | Thanos's transmutations of living things were **temporary and non-lethal**; the stone can be **counterfeited** in the comics. |
| **Power** | Access to all energy; unlimited stamina and strength; **duplicates almost any ability**; supplies the other gems; unlimited power supply for any machine (`Power Gem`). | Raw beams, waves, empowered punches; passive strength and durability; overloads objects; torture without killing (`Power Stone`). | **Explodes against organic matter, proportional to target size** unless in a container; groups can **share the load** (Guardians linking hands) (`Power Stone`). |
| **Time** | Total control of past, present, future: stop, rewind, travel, age control, **trap enemies or worlds in endless loops**; precognition (`Time Gem`). | Mandala "dial"; area or object selected, people **excluded**; reversed the Sanctum; **loop** that restarts the timeline on the wielder's death with memory (Dormammu); viewed 14,000,605 futures (`Time Stone`). | Misuse can **erase the user**, open space-time breaks, time loops; Dark Dimension beings are immune (`Time Stone`). |
| **Soul** | See, attack, **steal or trap souls in Soulworld**; "Cold Light of Truth"; revert beings to their natural state; reanimate the dead; **protects the wielder from soul attacks**; "karmic blast" that knocks out; **sentient, hungers for souls** (`Soul Gem`). Comics have **no Vormir price**. | Life and death; **price: sacrifice someone you love** (Vormir); sacrificed souls **cannot be revived even with six stones**; the unworthy become the Stonekeeper; finds the real Strange among illusions with Power (`Soul Stone`). | The Vormir price is **MCU only**. Comic cost is the stone's hunger and its Soulworld (Magus was trapped in it). |
| **Death** (comics, optional) | Made by Thanos after he subdued Mistress Death: awareness of everyone about to die and their cause, and death inducement; **not part of the circuit** (`Infinity Gems`, "Infinity Watch"). | none | Chose Phil Coulson as new embodiment of death and left Thanos. |
| **Ego** (comics, optional) | **Ultraverse (Earth-93060)**, not mainline: the original being's psyche, embeds in living hosts and **possesses** them (`Ego Gem`, `Infinity Gems` "Ultraverse"). | none | Destroyed when Nemesis was defeated. |

### 1.3 Gauntlet and Snap canon

- **Gauntlet** ([MCU] `Infinity Gauntlet`, `Nano Gauntlet`): fist for small feats, **snap** for universal ones; restraining the hand blocks it; each stone slotted gives a painful surge; the surge is **inversely proportional to wielder strength** (Thanos: seconds; Hulk: protracted burns); a full-power use **burns the Gauntlet and the wielder and cannot be repaired even with the stones**; the Stones are used singly **or in combinations** (Space+Power redirects energy, Soul+Power finds the real Strange, Time+Reality+Soul heals a wound).
- **Comics** ([MDB] `Infinity Gauntlet`, `Infinity Gauntlet (Event)`, `Infinity Gems`): Thanos halves life for Death; Nebula **reset time by 24 hours**; Warlock took it with the Soul Gem; the **Living Tribunal ruled the gems too dangerous to use together**; Mister Fantastic **could not will them out of existence**; the Illuminati each held one (Iron Man held Reality, later Space); **Iron Man was the first human to wield the Gauntlet**; Gamora and later Loki's Cosmic Avengers assembled sets (Infinity Wars); Ultron took the Soul Gem (`Ultron (Earth-616)`, "Infinity Countdown").
- **MCU snaps**: Thanos random half; **Hulk's snap restored everyone**; **Tony's snap erased only Thanos and his army and killed Tony** through the Mark LXXXV, which lacks the Gauntlet's protection; the Nano Gauntlet's nanites **moved the stones into the suit's hand without Thanos noticing** (`Nano Gauntlet`, `Iron Man Armor: Mark LXXXV`). Endgame also **returned the stones to their timelines** (`Infinity Stones`).

---

## 2. Part 1 of the design (lore summary): audit

| Design line | Verdict | Note |
|---|---|---|
| 1.1 six stones, dangerous to wield, power backlash grows with target | **OK** | `Power Stone`. |
| 1.1 containers (Tesseract, Scepter, Aether, Orb, Eye, Gauntlet) | **OK** | Add Universal Weapon (Ronan) as the Power conduit (`Power Stone`). |
| 1.1 stones are sentient | **OK** | Space banished Red Skull (`Space Stone`), Mind steered Loki (`Mind Stone`); comics: Soul Gem hungers (`Soul Gem`), all gems sentient (`Infinity Gems`). |
| 1.1 gamma radiation | **OK** | `Nano Gauntlet`. Also **ADD** "tracking by gamma" as the Space limit. |
| 1.1 circuit order | **OK** | See 1.1 above. **ADD** native-universe-only and Living Tribunal limit. |
| 1.1 Death Stone, Ego | **OK, refine** | Death: [MDB] `Infinity Gems`. Ego: **Ultraverse only**, destroyed, possession-based. Call it "Ultraverse", not "older comics". |
| 1.2 Space / Mind / Reality / Power / Time / Soul rows | **OK**, missing items | See sections 3.x. |
| 1.3 Nano Gauntlet transfers stones to a suit | **OK** | `Nano Gauntlet`. **ADD** it happens by physical contact and can be done **to another wearer** (Thanos did not notice): a theft route to design around. |
| 1.3 Mark LXXXV | **OK** | `Iron Man Armor: Mark LXXXV`. |
| 1.3 Scepter (blast, barrier, touch control, astral projection, blade) | **OK** | `Scepter`. **ADD** gate key for the Tesseract; blades match Mjolnir in close combat. |
| 1.3 Ultron "created from the Mind Stone's intelligence" | **FIX** | MCU: Ultron was a Stark AI that **stole the Scepter** and used its intelligence; the stone **gave sentience** and powered Vision's body (`Loki's Scepter` [MDB], `Mind Stone`, `Ultron`). |
| 1.4 Snap kills or revives at any scale; Hulk and Tony | **OK** | `Infinity Gauntlet`, `Iron Man Armor: Mark LXXXV`. **ADD** Thanos's was random half. |

---

## 3. Part 2 of the design: each stone

Costs in the design are "% of max magicule" (`Act.mana`, integer percent, 0 to 100) and match the repo style. Damage should use the **GOD ultimate class** (`BalanceLaw.damage(..., UltimateClass.GOD)`, total scale capped at x36), not the unclassified path (up to x72 before `stoneScale`), otherwise a x2 stone scale gives x144.

### 3.1 Space

| Design item | Verdict | Canon / repo note |
|---|---|---|
| Portal Step, 128 blocks, walls ignored, wards respected | **OK** + **FIX** | Canon ignores walls **and wards** (`Space Gem`); respecting wards is a deliberate, good deviation, say so in the tooltip. Bound: loaded chunks only (`hasChunkAt`), never load chunks; safe-landing check (reuse `Wave30Stones.destination` logic). |
| Shift: 2-way portal pair 30 s | **OK** | Canon portals (`Space Stone`). Bound: 1 pair per player, server cap (reuse `Wave30Key` portal cap). |
| Spatial Fold: pull or push enemies | **OK** | Canon: alters distance, gravity, black holes (`Space Gem`). `FORCE_MOVEMENT_EVENT` / `NO_FORCED_MOVE` were **not found in this repo** (grep): confirm they are Tensura API names before relying on them. |
| +30% speed | **OK** | Canon (`Space Gem`). |
| Can't be pulled or knocked back | **MOD** | Fine. |
| **ADD** | | Space as **power source**: sockets in suits/Scepter get cooldown or energy bonus (canon: powers engines and weapons). Scepter + Space = "gate key" (`Scepter`). **Unworthy holder is banished** (teleport to spawn, never damage) instead of "burning". **Tracking**: holder shows on other stone-bearers' compass (gamma). |

### 3.2 Mind

| Design item | Verdict | Canon / repo note |
|---|---|---|
| Dominion: touch, non-boss, 60 s ally; breaks at 30% heavy hit | **OK** | Canon: touch to the heart; breaks by a head blow (`Scepter`). Add **arc-reactor immunity**: Iron Man suits and Ultron/robot bodies shrug it off (canon: Stark's reactor absorbed it). |
| Players get 5 s "Confused" | **FIX** | `BalanceLaw.controlTicks` caps players at **3 s**, bosses 1 s, mobs 5 s, plus a shared 12 s hard-control cooldown (`beginControl`). |
| Psionic Reach: telepathy HUD, telekinesis lift and throw | **OK** | Canon telepathy and telekinesis (`Mind Gem`). The HUD needs **server data** (EP is not on the client): see lag table. |
| Passives: control/fear immunity | **OK** | Fine. |
| +25% mastery gain; enemies -10% attack | **MOD** | Fine. The -10% attack echoes the stone's anger influence on nearby minds (`Scepter`). |
| **ADD** | | The stone **influences its holder** (Loki): rare whisper that nudges nothing mechanically, or a small drawback. **Awaken a construct**: Mind socket in an Avatar/companion or Ultron armour grants "sentience" (stats or a skill), canon for Ultron and Vision. **Matter shaping** (Loki's armour, Vision's density): optional third mode. **Hacking**: Vision burned Ultron from the net, so "disable relic actives of robots" belongs to Mind too. Comics: **link every mind at once** at peak (Gauntlet only). Lethal enhancement of lesser beings (Maximoffs): skip or boss-only story. |

### 3.3 Reality

| Design item | Verdict | Canon / repo note |
|---|---|---|
| Rewrite: projectile/weapon to no damage; mob turns harmless 8 s, reverts | **OK** | Canon: guns to bubbles, temporary transformations (`Reality Stone`). Bosses excluded: **OK**. Reuse `Wave30Events.shrink` (reversible, tracked). |
| False World: illusion clone fights 20 s, you turn invisible | **OK** | Canon: Knowhere illusion that fights (`Reality Stone`). Bound: 1 clone per player, short life, no loot (reuse `RelicSummons`). |
| 20% chance a debuff doesn't happen; see through illusions; +15% elemental | **MOD** | See-through-illusions is canon-flavoured. |
| **ADD** | | **Counterfeits**: comics Warlock used a fake Reality Gem (`Infinity War`): make the Order's duplicate census (`LedgerRules` DUPLICATE) the lore hook ("a copy is a fake"). Comics later power: see other realities and talk to other Reality wielders (cosmetic scrying). **Aether** as the liquid carrier/quest weapon (already in the design as decorative). Limit to state in text: living-body changes are **temporary**. |

### 3.4 Power

| Design item | Verdict | Canon / repo note |
|---|---|---|
| Power Surge: charged beam, recoil 5% max HP without a carrier | **OK** | Canon: backlash without a container (`Power Stone`). Consider scaling recoil with **target mass/EP** (canon: "bigger target, bigger surge"). Recoil must never kill (leave a heart). |
| Infinite Might: +100% attack, punches explode | **OK** | Canon empowered punches (Thanos KO'd Captain Marvel). Explosions: **no block damage**, per-player rate cap. |
| +40% max HP/armor; skills -15% cost; relic cooldowns -20% | **OK / MOD** | Canon: strength, durability, supplies the other gems. Check stacking with the existing Power Stone passives (+50% pools, double regen). |
| **ADD** | | **Share the load**: allies in range split recoil (Guardians). **Power Mimicry**: copy one ability briefly (skip unless wanted). **Unlimited power supply** for suits (pairs with Space). |

### 3.5 Time

| Design item | Verdict | Canon / repo note |
|---|---|---|
| Rewind self 5 s | **OK** | `Time Stone`. Also the existing Greater Wish "Reroll" does a smaller version. |
| Shift: rewind a block area (8 blocks, 30 s) | **OK** + **FIX** | Canon (Sanctum, Mind Stone repair). But a raw block rollback **can undo other players' building and restore chest contents (dupes)**. Journal only blocks destroyed by explosions or mobs, exclude containers, cap blocks per cast and per tick. |
| Time Lock: 3/5/10 s boss/player/other | **FIX** | `BalanceLaw.controlTicks`: **1 s boss, 3 s player, 5 s mob** (+ diminishing returns). Canon is selective area (`Time Stone`): "excluding allies" **OK**. |
| Passives: cooldowns -25%, slow immunity, auto-dodge hit >=20% max HP per 60 s | **MOD** | Auto-dodge overlaps the existing Foresight guard (first blow per 20 s turned aside). Pick one. |
| **ADD** | | **Time loop** (Strange): death restarts a short window once per day, with memory; **Foresight as a futures preview** (Eye of Agamotto's "where is the boss" is already in the design as a quest item). **Age control** (comics): optional, reversible crop growth or item repair (reuse Paradox's `Turn the Clock`). **Limits to state**: misuse can erase the user, loops are costly. Time VFX: **do not touch** `TimeMagicLayer` (rule 3); the stone's visuals must be separate. |

### 3.6 Soul

| Design item | Verdict | Canon / repo note |
|---|---|---|
| Vormir trial: offer top skill, **50% max EP** or a named subordinate | **FIX** | The Vormir price is **MCU only** (`Soul Stone`); the comics have no price (`Soul Gem`). "50% max EP" drops the player under the 1B gate, which un-equips the stone (design 2.1 says so). Use the skill or the subordinate, or a fixed EP **tax below the gate margin**. A "named subordinate" lost for good is canon-style but is a **griefable loss**: confirm screen, never another player. |
| Soul Grasp: 3 s lock; mobs below 40% EP "taken into Soulworld": instant kill + loot | **FIX** | Canon is **trap, not kill**: souls live in Soulworld, can be freed (Drax and the Surfer escaped; Magus is held) (`Soul Gem`, "Infinity Gauntlet" section). Instant kill also breaks the repo's fairness rule (nothing kills instantly, no player below a heart). Suggest: trap for N seconds, release with damage cap, loot only on a normal kill. |
| Soulworld: pocket dimension 15 s, heal 10% max HP per second | **FIX** | Never create a real dimension at runtime; make it a zone effect. 10%/s x 15 s = 150%, but `BalanceLaw.heal` caps a cast at **50%** (and 40% effectiveness if healed in the last 20 s). Soul sight to reveal the real body: **OK** (canon needs Power too: `Power Stone`). |
| Survive death at 20% HP once per 10 min | **FIX** | Existing Soul Stone gives one True Resurrection **per game day** (`Wave30Events.onDeath`). Canon: the stone **reanimates** and protects from soul attacks; it does not self-save every 10 minutes. Keep once per day, or tie it to a stored soul (existing mechanic). |
| Soul resistance +40% | **OK** | Canon: protects from soul-based attacks (`Soul Gem`); consider much higher or immunity to Tensura "spirit" damage. |
| **ADD** | | **Sentient hunger**: the stone wants souls (canon `Soul Gem`): kills near you feed it (the existing ledger stores up to 12). **Stonekeeper curse** for an unworthy holder (below the EP gate) as the failure state. **Access skills and memories of trapped souls** (comics): read-only skill peek. Sacrificed souls are **never revivable** by Restore/Bring Back (design says so: **OK**). |

### 3.7 Death and Ego (optional)

| Item | Verdict | Note |
|---|---|---|
| Death Stone: sense entities below 15% HP, execute non-boss below 10% HP once a minute | **OK** (sense) / **FIX** (execute) | Canon: awareness of who is about to die and how, and death inducement (`Infinity Gems`). Execution must exclude **players** and cap by target EP ratio; the repo has no instant-kill precedent for bosses. Not in the circuit: keep it out of synergy math. |
| Ego Gem | **OK, skip** | Ultraverse-only possession gem (`Ego Gem`). If ever built, it is a villain/boss mechanic, not a player stone. |

### 3.8 Stone scaling

Design: `stoneScale = 1 + log10(EP / 1e9)`, capped at 2. **OK**, with two notes:
1. The cap is reached at **10 billion EP**, so progression ends one decade above the gate. Cap at 3 (100B) or add a mastery axis if the owner expects more.
2. `BalanceLaw.epScale` is already at x12 from about 25M EP, so at 1B every EP-based number is flat. This design is right to move to effect-based power and %-of-pool costs. Early/mid/late can be read as: **1B EP** = base values; **10B** = +1.0 of `stoneScale` (cap); the rest of the scaling must come from circuit, carriers and strain.

---

## 4. Carriers

| Carrier | Design says | Verdict | Canon and repo note |
|---|---|---|---|
| **Infinity Gauntlet** (6, wheel, combos, Snap) | | **OK** + **ADD** | Combos in canon: Space+Power, Soul+Power, Time+Reality+Soul (`Infinity Gauntlet`, `Power Stone`, `Soul Stone`). **Surge**: design says flat 10% max HP; canon is **inverse to strength**: scale with EP over the gate (brief at high EP). **Restraint** blocking use: **OK**. The existing Gauntlet (`the_infinity_gauntlet`, HANDS, Genesis) is **the same item**: socket stones into it rather than adding a second Gauntlet. **ADD** Living Tribunal rule as a server switch ("stones cannot all be used together" = `nusmpSnap` off). |
| **Nano Gauntlet** | 2x surge, Burned Arm 1 h | **OK** | Canon `Nano Gauntlet`: burns persist, needs gamma-resistant wielder. **ADD** the **contact transfer** (suit takes stones from a gauntlet) must be **self only, same player** in the mod (a player-to-player version is a theft/PvP exploit against one-per-server stones). |
| **Mark LXXXV** (6 stones, Snap kills you) | | **OK** (canon) / **FIX** (rule) | Canon: Tony died. The design's "kills you" conflicts with the repo's wave fairness ("no act takes a player below a heart") and the owner's own earlier "not so evil". This is the **owner's call**: see question 4. **ADD** arc reactor = Mind-control immune; flight, repulsor, unibeam, finger lasers, X-ray HUD are all `Mark LXXXV` capabilities: **OK**. |
| **Iron Man family** | only Mk LXXXV | **ADD** | Suggested set: Mark LXXXV (nano, 6 via gauntlet), **Mark L** (Mk 50, nano, the suit Thanos outclassed), **Hulkbuster** (Mk XLIII/XLIV, 11 arc reactors, heavy, slow) (`Hulkbuster`), **Iron Legion** (drones, JARVIS-controlled, hijackable by Ultron) (`Iron Legion`), **Bleeding Edge** (comics Model 37, stored in the body, Extremis-successor, **Uru-merged "Iron Destroyer"**) (`Iron Man Armor Model 37` [MDB]). Comics: Iron Man **held Reality then Space** and was **the first human to use the Gauntlet** (`Infinity Gauntlet` [MDB]): a good lore reason for the Iron Man suit to hold **1 socket**, and only the Nano Gauntlet route holds 6. |
| **Ultron armour** | 2 sockets, sentries, hack, -10% max HP per stone | **FIX** (partly) | MCU Ultron: hive of Sentries, Internet travel, upgrades, hijacked the Iron Legion (`Ultron`, `Iron Legion`). **Weak to lightning/EMP: not supported** by the pages read; canon weaknesses are **Vision's Mindspace and being burned out of the net** (`Mind Stone`) and, in the comics, **Antarctic Vibranium** (Hank Pym destroyed Ultron Prime: `Ultron (Earth-616)`). Comics: **Ultron took the Soul Gem and turned a planet into Ultrons** (Infinity Countdown). `What If... Ultron Won?` ([MCU]): Ultron with **all six** killed Thanos with Mind and spawned Sentries with Reality: supports raising sockets above 2 as an endgame boss version. |
| **Loki's Scepter** (Mind only) | blast, barrier, touch control, astral projection, blade, anger aura hurts you | **OK** | `Scepter`: barrier, astral link, blades, mind control, **gate key with the Tesseract**, subtle mind influence. The "+10% damage from allies" drawback is a **MOD**; canon drawback is the stone **influencing the holder**. |
| **Vision** | missing | **ADD** | `Vision`: Mind Stone in the forehead; density control, flight, beam; Thanos tore the stone out and killed him ([MCU] `Mind Stone`, `Vision`). Comics: Vision died to Thanos (`Infinity Gauntlet Vol 1 4`) and was revived by Nebula ([MDB] `Vision (Earth-616)`). As a mod feature: a **companion that carries the Mind Stone**; reuse `AvatarRelicBridge` / `MobRelics` (named mobs wear relics) and `TensuraBridge.maxEp` for his own gate. Risk: a **stone inside a killable mob** (theft, dupes, death). Keep the item in the owner's inventory and only **mirror its effect** onto Vision. |

---

## 5. The Snap options

| # | Design option | Verdict | Canon and note |
|---|---|---|---|
| 1 | Restore Snap (Hulk) | **OK** | `Infinity Gauntlet`, `Nano Gauntlet` (Hulk restored the Snap victims). Needs a **bounded list of recent deaths** (UUIDs, last 10 min, cap 32) and consent prompt; excludes Soul sacrifices. |
| 2 | Erase the Army (Tony) | **OK** / **FIX** | Canon: only Thanos and his army (`Iron Man Armor: Mark LXXXV`). **Bosses 50% max HP: FIX**, repo convention is **10%** (`Wave30Rules.snap`, `SNAP_BOSS_FRACTION`), plus once per boss per day. **128 blocks**: scan cost, cap processed targets (40, as the existing Snap) and spread over ticks. Exclude pets, villagers, named mobs, allies (`Wave30Kit.ALLY_OWNER`). |
| 3 | Rebuild Snap | **OK** / **FIX** | Canon: Time reversal (Sanctum, `Time Stone`). 128 blocks x 10 min is **unbounded**. Use the explosion/mob-grief journal only, cap blocks (e.g. 8192), spread, no containers, skip claimed land. |
| 4 | Purge Snap | **OK** | Canon: Soul reverts beings to natural state (`Soul Gem`), Reality. Cheap. |
| 5 | Grant Snap (+100% stats, 5 min) | **MOD** | Canon: Power bestows abilities (`Power Gem`). +100% is large; use a ceiling like the existing Great Wish buffs (Resistance II, 10 min). |
| 6 | Bring Back Snap | **OK** | Canon Soul reanimation and Time reversal. Reuse the corpse/gravestone system if it holds items (`corpse` package). Sacrificed souls stay dead. |
| 7 | Rewrite the Sky | **OK** / **FIX** | Weather and time are cheap. **"Regrow a biome" is an unbounded world edit**: drop it or make it a visual only. |
| 8 | Cast Back the Stones | **OK** | Canon: Endgame returned them (`Infinity Stones`); comics: Warlock released them, Gamora's Warp World. Use the ledger's lost/revive path (`LedgerRules`). |
| **ADD** | | | **Thanos's random half** (canon, evil, skip). **Nebula's 24-hour reset** (`Infinity Gauntlet` [MDB]): a "Reset Snap" restoring friends' HP/position to 30 s ago, no world edit. **Destroy the stones** is **not** available: Reed Richards failed (`Infinity Gauntlet` [MDB]). **Never revivable** list. **The Gauntlet is irreparable** in the MCU: design's "repair with Uru" is a **gameplay softening**, fine. |

**Costs.** Design (gauntlet breaks, -50% max HP 1 h, stones dark 24 h, Burned Arm, Tony dies) is **OK** for canon. Keep: broadcast line (**OK**), 5 s held gesture interruptible by stun/bind (**OK**: `Infinity Gauntlet` restraint). Config switch `nusmpSnap` (the design's `nusmp_snap`; repo gamerules are camelCase, e.g. `nusmpPvpHitCapPercent`).

---

## 6. Where the design is silent: what exists to reuse

| Need | Reuse | Where |
|---|---|---|
| Equip a Curios relic by right-click | `RelicCurioEquip`, `CurioEquipRules.pick` | `relics/` |
| Slot data | slot json + `entities/relics.json` + `data/curios/tags/item/*.json` | `data/anime_multiverse/curios`, `data/curios/tags` |
| 1B EP read | `TensuraBridge.maxEp(e)`, `baseMaxEp(e)`; `EnergyHelper.getMaxEP`; gate pattern `ManaCoreRules.mayUse`, `ManaZoneGate.needEp` | `avatar/TensuraBridge`, `manacore`, `mana/zone` |
| Largest existing EP gate | `BeastWillDef` clamps `requireEp` to **1e9**; ManaCore top rank SS = 500,000; Mana Zone 2,000,000 | so 1B is the highest gate in the mod |
| Acts, passives, costs | `Act` (`.mana(%)`, `.epPercent`, `.usesPerDay`, `.hold`, `.toggle`), `Passive.attr/effect/resist/flag/skill`, `CostMath` (EP floor 10%) | `relics/` |
| Sockets in an item | `relics/socket/*` (`SocketComponents`, sneak-use insert, `allowSocket`) | `docs/relic_sockets_and_evolution.md` |
| Chest armour, `Look.SUIT` | `RelicArmorItem`, `RelicWornLayer`; head/feet go to Curios | `relics/`, `docs/relic_head_feet_slots.md` |
| Uniqueness, duplicates, scan | `UniqueLedger`, `LedgerRules`, `RelicScanner`, `ScanCursor` | `relics/order` |
| Damage, caps, heal, control | `BalanceLaw.damage/cap/heal/controlTicks`, `UltimateClass.GOD`, `NUGameRules.pvpHitCap` | `balance/`, `skill/rules/UltimateClass` |
| Perf | `PerfBudgets.particles / effectPacket`, `PerfProbe`, `PerfSourceScanTest` | `perf/` |
| Weapon extras | `WeaponEngravings`, Better Combat `gen_attributes.py`, `tools/gen_weapon_models.py` | `item/`, `tools/` |
| Named mob wearing relics | `AvatarRelicBridge`, `MobRelics`, `MobRelicsConfig.maxCarried` | `avatar/inventory`, `relics/` |
| Existing stones/Gauntlet/Snap | `Wave30*` | `relics/wave/` |

**What does not exist yet**: any Iron Man, Ultron, Hulkbuster, Loki or Marvel-franchise item (greps over `src/`, `docs/`, `tools/` found only the stones and Loki the Norse skill); a Marvel franchise tag (stones are tagged `dnd` in `relics/franchise_map.json`).

---

## 7. Server safety and lag: worst cases for the design's effects

Rules applied: CLAUDE.md 9 and 11. Per-player state by UUID, cleaned on logout, death, dimension change. Effects sent to nearby players only, through `PerfBudgets`.

| Effect | Worst case per cast | Packets | Needed bound |
|---|---|---|---|
| Portal Step | 1 raycast <=128 steps, 1 landing check | 2 effect bursts | loaded chunks only; cooldown 8 s |
| Portal pair | 2 portal anchors, 30 s | 0 per tick (event on entry) | 1 pair per player, global cap (reuse Key's) |
| Spatial Fold | 1 AABB query radius 16 | 1 burst | move at most 40 mobs |
| Dominion | 1 target | 1 burst | one active ally per player |
| Psionic Reach HUD | 1 query radius 48 per second | 1 packet/s per player, <=32 entries (~1 KB) | server-built list; off when toggled off; 100 players = 100 packets/s |
| Telekinesis | 1 held target | 0 | position set each tick for one target only |
| Rewrite | 1 target | 1 | reversible, tracked |
| False World | 1 clone entity, 20 s | 1 | 1 per player, short life, no drops, global cap |
| Power Surge / Infinite Might | 1 beam raycast; punches: 1 query per hit | 1 burst per hit | explosion **no block damage**, <=3 per second per player |
| Rewind (self) | ring buffer of 100 samples per player (~5 KB) | 1 | sample every tick only for stone holders; free on unequip |
| Rewind (blocks) | journal + restore | many block updates | journal cap (e.g. 4096 per level), restore <=256 blocks per tick, <=8192 per cast |
| Time Lock | 1 query radius 12 | 1 | 40 targets; control caps from `BalanceLaw.controlTicks` |
| Soul Grasp | 1 target | 1 | trap not kill; no real dimension |
| Soulworld | zone effect, 15 s | 1 | heal through `BalanceLaw.heal` |
| Death Stone sense | 1 query per second, radius 24 | 1 packet/s | 40 entries |
| Snap (any) | up to 40 targets or journal | 1 broadcast + bursts | spread over ticks, once per week, no player killed by option 2 |
| Snap menu | client screen to server packet | 1 per use | server validates sender holds the gauntlet and six stones, choice in range, cooldown, rate limit |
| Gate check | 1 EP read and slot read per second per wearer | 0 | read `baseMaxEp`, not the lifted value |
| "Return to shrine / no chests" | event-driven (container events, login, 10-minute check) | 0 | **no chest scans**; use the Order's bounded `RelicScanner` census |

Anti-grief to add: no option may take a **player** below a heart; every world edit is bounded and reversible; **boss damage via 10% of max** (existing convention) and `BalanceLaw.isBoss` for the exclusion; stone acts **off inside the Labyrinth and Living Dungeon** (precedent: Warren's Key). Settings that restore old behaviour: `infinityStones.enabled`, `.requireEp` (default 1e9), `.legacyCarry` (keep Wave 30 carry behaviour as is), `.snapEnabled`, `.maxSockets`. Server configs, not client (rule 9). Self-tests: add `infinity.*` prefix to the smoke list; extend `PerfSourceScanTest` for the journal and ring buffer patterns.

---

## 8. Open points: yes/no questions

Only the points the design leaves open or gets wrong.

1. Keep the **same six relic ids** (rework the Wave 30 stones in place; old carried behaviour kept behind `legacyCarry`) instead of 12 new stones?
2. Make `infinity_stone` an **always-present slot** with an equip-time EP gate (silent when EP falls), since Curios slots cannot be granted at runtime?
3. Measure the 1B gate on EP **without** the stone's own +20% AKASHIC bonus (use base max EP)?
4. Keep **"Snap through Mark LXXXV kills the wearer"**, with a confirm screen and the gamerule? (Answer no gives: wearer left at 1 heart, suit destroyed, Burned debuff.)
5. Replace the Soul price **"50% max EP"** with a skill or subordinate offering only, so the player does not fall below the gate?
6. Cap **boss damage from Snap option 2 at 10% of max HP** (repo convention) instead of 50%?
7. Limit **Rebuild Snap** to blocks destroyed by explosions or mobs, no containers, at most 8192 blocks?
8. Use `BalanceLaw.controlTicks` caps (1 s boss, 3 s player, 5 s mob) for Time Lock and Dominion instead of the design's 3/5/10 s and 5 s?
9. Change Soul Grasp from **instant kill** to **trap and release**, and the Soulworld heal to the 50% `BalanceLaw.heal` cap?
10. Disable stone acts **inside the Labyrinth and Living Dungeon** ("only works in its native universe"), and build Vision as a companion that **mirrors** the Mind Stone rather than holding it?

---

## 9. Caveats and what could not be fetched

- **Not read**: individual comic issues. Every comics fact comes from Marvel Database summaries (`Infinity Gems`, six gem pages, `Infinity Gauntlet`, `Infinity Gauntlet (Event)`, `Infinity Countdown`, `Loki's Scepter`, `Ultron (Earth-616)`, `Vision (Earth-616)`, `Ego Gem`). "Every Marvel comic" is not possible; Thanos Quest, Infinity Crusade, Infinity War, the Ultimates and Hickman's Avengers appear only through those summaries (the Illuminati gem split is in `Infinity Gems`). `Thanos Quest` resolved to a 318-character redirect, the `Infinity War (Event)` page returned nothing.
- **Not fetched**: English Wikipedia (HTTP 429 from the first call, even with a generic user agent). `Infinity Ultron/Age of Ultron` returned the Marvel Rivals cosmetic instead; Marvel Rivals' game rules and other video games were **not researched**. `Bleeding Edge` MCU page and `Loki's Scepter` MCU page do not exist under those titles (the MCU page is `Scepter`).
- **Partly read**: MCU `Ultron` and `Vision` pages (Powers sections only); `Hulkbuster`, `Iron Legion` (Capabilities only); `Mark LXXXV` (Capabilities and history). I did not find canon support for "Ultron weak to lightning / EMP" and say so above.
- **Repo**: nothing was run or built. EP of 1 billion is **not shown reachable** in play (only `BeastWillDef` bounds and test values touch 1e9): the gate may be unreachable without Tensura EP growth beyond what this repo tests. The numbers in sections 3 to 7 are bounds to tune, not tested balance.
- **Unverified API names**: `FORCE_MOVEMENT_EVENT`, `NO_FORCED_MOVE` (not in this repo; may be Tensura's).
- **Rule 14 (Blender)**: if the owner wants the suits, Sceptre and Vision built, each is a Blender model (suits as a chest relic plus a worn mesh layer in the style of the Valkyrie v2 armour layer; Sceptre as a weapon with Better Combat attributes and engravings; Vision as an entity with skin and animation). Not started; this audit builds nothing.
