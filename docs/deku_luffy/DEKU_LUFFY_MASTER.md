# Deku and Luffy: endgame bosses, drops and hidden races for multiverse-of-anime

> **For the Claude session building the mod.** Read this file first. It contains:
> - two bosses at their **end-of-series peak**: Final-War Deku, and Gear 5 / Nika Luffy as of the Elbaph arc (wiki covers up to roughly Ch. 1198)
> - every drop (quirks, devil fruit, Haki, gear)
> - **hidden Tensura races** with evolution ladders
> - a reference-image pack for models and VFX
>
> **Sources:** the MHA and One Piece Fandom wikis, read 2026-11-22 through the wiki API and summarised. The full lore notes are in `DEKU_LORE.md` and `LUFFY_LORE.md` in this folder.
> - **Canon** = names, looks, powers and limits.
> - **Design** = every number, phase structure, drop rate and race. The user asked for these to be **as overpowered as the source**, so they are deliberately busted.
> - Tune in config. Nothing here has been built or tested.
>
> **Build on the mod's existing systems** (see `TENSURA_ADDON_GUIDE.md` / `MASTER_GUIDE.md`):
> - bosses: `ArcBoss` / `ZogratisBoss`, the same pattern as `EscanorBoss` and the Sinbad drop in `../sinbad_boss/`
> - skills: `NUSkill` extends Tensura `Skill`
> - drops: the relic system (`RelicDef` / `Act` / `Passive`, Grade `GOD` / `GENESIS` / `AKASHIC`)
> - races: Tensura `DefaultRace`, with the DxD `LineRace` ladder pattern

## Contents
1. Power tier and global rules
2. **Deku boss**
3. **Luffy boss**
4. **Drops**: One For All, the vestige quirks and Deku's gear · the Nika fruit, Haki and Luffy's gear
5. **Hidden races**: the Quirk lines (Hero / Villain) and the Sea lines (D. bloodline, Giant, Fish-Man, Mink, Lunarian)
6. Implementation map (classes, VFX shapes, assets)
7. Reference images and Minecraft skins

---

## 1. Power tier and global rules
- Both are **God-class ultimate bosses**, above every current arc boss (Escanor 3,000 HP, Sinbad 3,200 HP). Use them as late-game **world bosses** or the end of a saga.

  | | Deku (Final War) | Luffy (Gear 5 / Nika) |
  |---|---|---|
  | Max HP | **≈1.18 million across 7 forms** (see §1.1) | **≈1.36 million across 7 forms** (see §1.1) |
  | Attack (form 1 → final) | 90 → 190 | 110 → 230 |
  | Armor / toughness | 30 / 20 | 25 / 15 (rubber: see passives) |
  | Speed | 0.42 (fastest boss in the mod) | 0.36 (0.5 in Gear 5) |
  | Knockback resistance | 1.0 | 1.0 |
  | Tensura EP (for EP-ratio mechanics) | 80,000,000 | 120,000,000 |
  | Player DPS budget | `BossBudget` stage caps per phase | same |

- **EP-ratio effects** (one-shots and shredding, trnightmare style):
  - Never on bosses.
  - Players: at most 60% max HP per hit (stricter than the usual 40% PvP cap), so the fight is brutal but survivable.
  - The real threat is **pressure**: Deku's speed, Luffy's constant cartoon chaos.
- **Arena:** both bosses edit terrain through `tryChange` / `ledger()` (Luffy's rubberized ground, Deku's craters). **Everything is restored** when the fight ends (`restoreBlocks`).
### 1.1 The form system (overrides every "phase by HP %" below)
**How it works:**
1. **Each form has its own full health bar.** Damage is **clamped so HP can't drop below 1%** of the current form's bar. Overkill is discarded. Even a one-shot only takes it to 1%.
2. **At 1% the boss "boots up" its next form:**
   - **5 s transformation**, fully **invulnerable** (damage ignored, CC immune)
   - the canon transformation line and VFX play
   - the new form's **max HP is set** and the bar **refills to 100%** (with the mod's existing refill glow)
   - its new moves are unlocked
3. Forms **stack**: every earlier move stays available, and the new form adds its own moves on top.
4. **Only the final (peak) form can die.** Its 1% floor is removed.
5. **Escalation per form:** damage +15%, speed +5%, move wind-ups −8% (minimum 50% of the original).
6. **Multiplayer scaling:** every form's HP × (1 + 0.6 × (players in the fight − 1)), capped at ×5.
7. **Canon downtime is kept inside forms**:
   - Gearshift recoil, Gear 4 ending, and Gear 5 aging all become **damage windows within a form**, never free skips
   - each of these windows can **take a form to its 1%**, but never past it

**Build it on what the mod already has:**
- **Clamp:** Zagred's `clampToThreshold` pattern, in `hurt`/`lastCap`: `floor = 0.01 × maxHP` while `form < FINAL`.
- **Trigger:** in `bossTick`, when HP ≤ floor + 0.5 and you're not already transforming, call `beginForm(next)`.
- **`beginForm(next)`:**
  - set `invulnerableUntil = now + 100`
  - set the `MAX_HEALTH` attribute modifier (`nusmp:<boss>_form`) to the new form's HP
  - `refillForMode("form_" + next, 100)` (the existing `PhaseRefill`, which shows the "second bar" message)
  - `setForm(modelIndex)`, say the line
- **`PowerCurve` gating:** leave `minPower` gates on, but power alone no longer unlocks moves. **`form ≥ requiredForm`** does. Add a `form` column to the move table.
- **Save `form` in NBT.** On reload, a boss mid-transform finishes the refill.
- **DPS budget:** set `budgetFinalStage()` = 6 and `budgetStage()` = form − 1 so each form gets its own `BossBudget` cap. A form should last **2–4 minutes for a 4-player group** at those HP values; tune them in config.

**Deku: 7 forms (peak = form 7)**
| # | Form (model) | HP | New abilities (moves in §2.4) | Transformation line (design) |
|---|---|---|---|---|
| 1 | **Full Cowl 20%, Shoot Style** (Zeta) | 50,000 | St. Louis Smash, Manchester Smash, kicks | — |
| 2 | **Full Cowl 45% + Air Force** (Zeta) | 80,000 | Delaware Smash Air Force, St. Louis Smash Air Force, Texas Smash | "Forty-five percent… now it doesn't even hurt." |
| 3 | **Vestiges Awaken: Full Cowl 100%** (Zeta, blue lightning) | 120,000 | **Blackwhip** (Pinpoint Focus), **Float** (flight passive), **Smokescreen** (Full Blast), **Danger Sense** passive, 1,000,000% Delaware Detroit Smash | "Everyone… lend me your power!" (vestige ghosts flash) |
| 4 | **Faux 100% / Fa Jin** (damaged Zeta) | 160,000 | Faux 100 Percent, Faux 100%: Manchester Smash, **Blackchain**, Fa Jin charges, New Hampshire / Oklahoma Smash | "Fa Jin, charge it all!" |
| 5 | **Gearshift: Overdrive "120%"** (damaged Zeta, blue Gearshift aura) | 220,000 | **Transmission**, Low Gear, **Detroit Smash: Quintuple**, Overdrive. The 5-minute Gearshift timer runs; **recoil = 15 s +100% damage window** | "Gearshift… TOP GEAR!" |
| 6 | **Deku Overlay** (`_form2`) | 260,000 | Overlay Rampage (6 tendrils), Blackwhip Shield, Quirk Transfer, Wyoming Smash | (silent, Blackwhip pulls his body up) |
| 7 ★ | **One For All: All Nine** (Origin shirt + all 8 vestige ghosts manifested, gold-green aura) | **300,000** (can die) | Everything above at full speed, **United States of Smash**, and the **Final Smash** (sky-clearing, Blackwhip anchors to hide behind) at 50% | "This is… One For All. All of us!" |

**Total: 1,190,000 HP.** Damage goes from base to ×2.0 by form 7.

**Luffy: 7 forms (peak = form 7)**
| # | Form (model) | HP | New abilities (moves in §3.4) | Transformation line (design) |
|---|---|---|---|---|
| 1 | **Base + Haki** (Elbaph outfit, black-hardened arms) | 60,000 | Jet Pistol (base Pistol), Gatling, Rocket, Armament, Observation dodge | — |
| 2 | **Gear 2** (steam, pink skin) | 90,000 | Jet Pistol, Jet Gatling / Hawk Gatling, **Red Hawk** | "Gear… Second!" (pumping stance, steam burst) |
| 3 | **Gear 3 + Advanced Haki** (giant black fists) | 130,000 | Elephant Gun, Elephant Gatling, Grizzly Magnum, **Red Roc**, Haki **Emission** punch; **Conqueror's aura** turns on | "Gear… Third!" (thumb bite, inflating fist) |
| 4 | **Gear 4: Boundman** | 170,000 | Kong Gun, **King Kong Gun**, Leo Bazooka, Rhino Schneider, Kong Organ | "Gear… Fourth! Boundman!" (blowing into the arm, Haki spreading) |
| 5 | **Gear 4: Snakeman ⇄ Tankman** (swaps every 20 s) | 210,000 | Jet Culverin, Black Mamba, **King Cobra**, **Hydra** (Conqueror-infused); Tankman Cannonball. At form end, the **10 s no-Haki window** happens inside the form | "Snakeman." / "Tankman!" |
| 6 | **Gear 5: Sun God Nika** | 300,000 | Rubberize the World, **Kaminari**, Mogura Pistol, **Dawn Cymbal**, **Star Gun**, Dawn Rocket, Dawn Gatling, Kraken Gatling, Laughter, cartoon dodge. Drums of Liberation music starts. | (heartbeat drums, "dondottotto", laughing) |
| 7 ★ | **Gear 5 Peak: Gigant + Monkey form** (giant Gear 5 → black monkey) | **400,000** (can die) | **Gomu Gomu no Gigant**, Nawatobi, **Majin**, Dawn Thor Bullet; at 30% the **aged exhaustion window** then **Monkey form**; at 10% **Bajrang Gun** (Seastone-ring mechanic) | "This is the peak… I'm at my peak!" (the canon "I've reached my peak" moment) |

**Total: 1,360,000 HP.** Damage goes from base to ×2.0 by form 7.

**Read the phase lists in §2.4 and §3.4 through this table:**

| §2.4 / §3.4 heading | Deku forms | Luffy forms |
|---|---|---|
| "Phase 1" | 1–2 | 1–3 |
| "Phase 2" | 3–4 | 4–5 |
| "Phase 3" | 5 | 6 |
| "Phase 4" | 6 | 7 |
| "Phase 5" | 7 | — |

The **HP-percentage ranges in those headings no longer apply.** Within the peak form, use the percentage triggers given in the table above: Deku's Final Smash at 50%; Luffy's exhaustion at 30% and Bajrang Gun at 10%.

- **Music / SFX hooks:**
  - Luffy: the **Drums of Liberation** heartbeat ("dondottotto") loops through Gear 5.
  - Deku: Gearshift's **delayed sonic boom**, where the hit lands before its sound.

---

## 2. Boss: Izuku Midoriya, "Deku" (One For All, 9th holder)

### 2.1 Look per phase (refs in `refs/deku/look/`)
| Phase | Model / texture | Ref image |
|---|---|---|
| 1–2 | **Costume Zeta**: dark green bodysuit with gold lines, white Air Force Gloves (green palms, yellow knuckle sights), red Mid-Gauntlets, red boots with black Iron Soles, Gran Torino's cape as a scarf, chipped metal respirator | `Izuku_Midoriya_Costume_Zeta_Anime_.png` |
| 3 | Damaged Zeta: respirator gone, right gauntlet gone, **right arm wrapped in black Blackwhip coils**, cape in rags | `Deku_punches_Tomura_with_all_his_might.png` |
| 4 | **Deku Overlay**: shirtless, 4 Blackwhip tendrils from the back + 2 from the torso, black claws, dark lines over the skin, jaw-like mask (`_form2` model) | `Deku_Overlay_Anime_.png` |
| 5 (embers) | **Costume Origin**: light-blue All Might T-shirt, bandages; faint ember glow | `Embers_of_One_For_All.png` |
| Alt skin | "Dark Deku" (Epsilon) for an optional variant encounter | `Izuku_Midoriya_Costume_Epsilon_Masked_Anime_.png` |

### 2.2 VFX palette (refs in `refs/deku/*`)
| Power | Look to reproduce | Suggested existing `VfxShape` |
|---|---|---|
| One For All / Full Cowl | **red vein lines** on the body, **green/cyan lightning** crackling over it, glowing green eyes | `LIGHTNINGFX_ZONE` (green), follow-aura every 40 ticks |
| Full Cowl 100% | hair spikes up and glows, eye-trail streaks, lightning turns **blue**, skin reddens | `LIGHTNING_FIEND` (blue) |
| Gearshift | **blue aura** on whatever is accelerated; sonic booms; **sound arrives after the hit** | `HERCULES_SHOCKWAVE` + a delayed sound (`later(6, ...)`) |
| Fa Jin | **red energy** building up while charging | `LIGHTNINGFX_CAST` (red) |
| Danger Sense | **yellow lightning bolt** next to his head | `FX_LIGHTNING_ARC` (yellow) |
| Blackwhip / Blackchain | **black tendrils with a teal outline**; chains | `THREAD_STRINGS` (black/teal), `SHADOW_UNITE` |
| Smokescreen | **purple smoke**; with Low Gear, blue-purple and hanging still | `DARKFX_ZONE` (purple) |
| Float | plain hover; pair with Air Force air trails | `WIND_RING` small |
| Smash shockwaves | air-pressure blasts, craters | `HERCULES_SHOCKWAVE`, `SLASH_WAVE`, `WIND_GALE` |
| The vestiges | ghost silhouettes of the 8 past holders behind him | custom ghost entities, or `LIGHT_FLARE` silhouettes |

### 2.3 Passives
- **Danger Sense** (canon: reads hostile intent): **25% chance** to sidestep any hit, at most once every 6 s. **Counter (canon):** it is overloaded by many attackers at once. With 4 or more players hitting him inside 3 s, it shuts off for 10 s ("Danger Sense overloaded!").
- **Float**: he flies freely and only lands to kick. **Counter:** knockdown effects ground him for 3 s.
- **Holds back** (canon: won't kill): players at 10% HP or lower are knocked back instead of being hit by finishers. Disable this with a config option for "hard mode".
- **Body recoil**: every 100% smash costs him 1% max HP (canon recoil). Phase 4 removes this (Blackwhip braces his body).

### 2.4 Phases and moves
Numbers are base damage before `strike()` scaling.

**Phase 1: Full Cowl 45%, Shoot Style (100–75% HP)**
| Move | Canon basis | Mechanic | Dmg |
|---|---|---|---|
| St. Louis Smash | spinning roundhouse | cone kick, 7 m | 40 |
| Manchester Smash | flip axe-kick | leap, ground slam, 4 m disc | 55 |
| Delaware Smash Air Force | compressed-air bullets from the gloves | 4 projectiles, slight homing | 22 ×4 |
| St. Louis Smash Air Force | 45% vertical kick that fires an air blade | 24 m line blade | 60 |
| Pinpoint Focus | single Blackwhip strike | fast 18 m tether hit, pulls the target in | 30 |
| Faux 100 Percent | 45% + Fa Jin + Blackwhip slingshot | dash across the arena, then Manchester at the end point | 80 |

**Phase 2: 100% and the vestiges (75–50%)**: Full Cowl 100% look, plus the vestige quirks:
| Move | Mechanic | Dmg |
|---|---|---|
| Blackchain | 3 chain tethers bind up to 3 players (2 s root), then fling them 20 blocks | 45 |
| Full Blast | Smokescreen: purple fog over the whole arena for 8 s (blindness); he gets 3 free sneak-attack crits | — |
| 1,000,000% Delaware Detroit Smash | flick blast, then a 100% punch; 6 m crater | 120 |
| Texas Smash | wind punch, 30 m cone of knockback | 70 |
| New Hampshire Smash | punches the air to rocket backward, body-slamming whoever is behind him | 65 |
| Oklahoma Smash | grab the nearest player, spin, throw in a whirlwind (hits everyone in 5 m) | 60 |
| Fa Jin (charge passive) | every 3 moves he gains a Fa Jin charge (red glow); his next Smash does ×1.5 | — |

**Phase 3: Gearshift / Overdrive "120%" (50–25%)**, with a hard **5-minute timer on the boss bar** (canon):
| Move | Mechanic | Dmg |
|---|---|---|
| Transmission | 4-hit rush that shifts up a gear on each hit (Low → Second → Third → Top Gear); the last hit launches. The sound of each hit plays 6 ticks late. | 40 / 50 / 60 / 120 |
| Low Gear | slows one player to a near freeze (slowness 255 for 3 s), or "freezes" his own smoke in the air as walls | — |
| Overdrive | 6 s window: double movement speed, every move's wind-up halved | — |
| Detroit Smash: Quintuple | 5 Detroit Smashes in one blink on a marked target | 5 × 70 |
| Gear-boost ally (Second Gear on Katsuki) | design flip: he accelerates a player's own projectile back at them | reflected |
- **When the 5 minutes run out (canon recoil): "GEARSHIFT RECOIL".** He drops to the ground for **15 s**, can't breathe, and takes **+100% damage**. This is the DPS window.

**Phase 4: Deku Overlay (25–5%)**: he gets up from the stun, puppeted by Blackwhip (`_form2` model).
| Move | Mechanic | Dmg |
|---|---|---|
| Overlay Rampage | 6 tendrils attack independently, each targeting a different player every 2 s | 25 each |
| Blackwhip Shield | blocks all frontal damage for 4 s; it has to be flanked | — |
| Quirk Transfer (inversion of canon) | marks a player and "forces" a vestige on them. That player gets a random **vestige buff** for 30 s (Float / Smokescreen / Fa Jin) **and** the matching drawback. He gives power away because he is losing. | — |
| Wyoming Smash | both arms at 100%, a spike-down shockwave, 10 m | 150 |

**Phase 5 = Form 7, the peak (300,000 HP, the only Deku form that can die; see §1.1)**
- Costume Origin model. The **8 vestige ghosts appear behind him**, one fading every 10% of the bar.
- **The final ultimate:** "One For All: Final Smash" (the canon finale, unnamed there). After a 6 s telegraph (the sky darkens and clouds gather), a sky-clearing punch:
  - **clears the weather**
  - **damage to everyone in the arena = 70% of max HP**
  - unless they're behind a **Blackwhip anchor**: 3 tendrils he leaves in the ground, which players can stand behind
  - he uses it once, at 50% of the embers bar

### 2.5 Counters (teach them in the boss intro text)
- Many attackers at once overload Danger Sense.
- Hitting him during the Gearshift recoil does double damage.
- Rain or water puts out Smokescreen (canon: humidity ruins it).
- Big shockwave attacks blow the smoke away.
- Slash and sharp damage is normal (no special immunity), but OFA recoil makes him self-damage, so long fights favour the players.

---

## 3. Boss: Monkey D. Luffy, "Straw Hat" / Sun God Nika

### 3.1 Look per phase (refs in `refs/luffy/`)
| Phase | Model / texture | Ref image |
|---|---|---|
| 1 | Elbaph or Wano outfit, straw hat, arms turning black with Armament Haki | `haki/Armament_Hardening.png` |
| 2 | **Gear 4** forms: Boundman (round, inflated, black flame-Haki patterns, steam scarf), Snakeman (lean, flame hair, fire-steam scarf), Tankman (huge belly) | `gear4/Boundman.png`, `Snakeman.png`, `Tankman_Manpuku_Version.png` |
| 3 | **Gear 5**: white flame hair, white clothes, purple sash, red ringed eyes, cloud collar (Elbaph variant: horns) | `gear5_look/Gear_5_Infobox.png` |
| 3-giant | **Gomu Gomu no Gigant**: Gear 5 at giant scale (`setForm(2)`, scale ×6) | `gear5_moves/Luffy_using_Gomu_Gomu_no_Gigant.png` |
| 4 (enrage) | **Monkey form**: pure black body, fangs, swirl marks on the shoulders, cloud rings on the wrists | `gear5_look/Luffy_Monkey_Form.png` |
| Exhausted | **aged and skinny** after Gear 5 drops (the stun state) | `gear5_look/Gear_5_Wears_Off.png` |

### 3.2 VFX palette
| Power | Look | `VfxShape` |
|---|---|---|
| Armament Haki | glossy **black** limbs, red tint, **flame-shaped edges** | `DARK_THRUST`, black-tinted overlay layer |
| Advanced Armament (Ryuo) | orange-red flowing aura with **cherry-blossom** particles; invisible-impact emission | `LIGHT_FLARE` (orange) + cherry particles |
| Conqueror's Haki | **black-and-red lightning**, a shock ring that knocks weak mobs out, sky splitting on a clash | `STORM_BOLT` (black/red), `ULT_DEVOURING_SHADOW` ring |
| Observation / Future Sight | eyes glow **red** | eye glow layer |
| Gear 2 | **steam** and pink skin; Red Hawk sets the fist on **fire** | `FX_WIND_GUST` (white steam), `LIGHT_FLARE` (fire) |
| Gear 3 | giant **black** fist with an air ball at the wrist | scaled fist model |
| Gear 4 | steam or fire scarf, black flame patterns | steam particles |
| Gear 5 | white clouds, cartoon **stars and sparkles**, rubberized terrain wobbling, **lightning he throws** | `STORM_CLOUD`, `LIGHTNING_GOD`, star particles |
| Drums of Liberation | heartbeat pulse rings | `SONG_RING` (white) on the beat |

### 3.3 Passives (canon rubber body)
- **Rubber**:
  - immune to **lightning** damage
  - **blunt / melee damage ×0.3**, unless the attacker has a Haki skill or the hit is a Tensura "Haki / aura-coated" type
  - **projectiles are bounced back** at the shooter (arrows, tridents, fireballs) 50% of the time
- **Weak to slashing** (canon): sword/blade damage ×1.3 (sword and axe tags).
- **Devil Fruit weakness:** while in **water** (feet in a water block) he is drained: slowness and −50% damage, and Gear 5 can't be used. **Seastone** items (drops and craftables) apply "Seastone Cuffed" for 6 s, which disables his moves.
- **Observation Future Sight:** 20% dodge, at most every 8 s. It **doesn't** work while he's restrained (canon).
- **Conqueror's aura:** players with less than 10% of his EP **can't approach within 6 blocks** (knockback and nausea). Non-boss mobs within 20 blocks pass out (despawn as "fainted").

### 3.4 Phases and moves
**Phase 1: Haki base, Gear 2 and Gear 3 (100–75%)**
| Move | Mechanic | Dmg |
|---|---|---|
| Gomu Gomu no Jet Pistol | invisible-speed stretch punch, 20 m line | 45 |
| Gomu Gomu no Jet Gatling / Hawk Gatling | 2 s barrage cone | 8 ×20 |
| Gomu Gomu no Red Hawk | flaming hardened punch, explosion that sets fire | 90 |
| Gomu Gomu no Elephant Gun | giant black fist, 30 m line, knockback | 110 |
| Gomu Gomu no Rocket | grabs a block or player and slingshots | 40 (+ fall) |
| Haki Emission punch | invisible burst: damage at 8 m with **no visible projectile** (telegraph: cherry-blossom flash) | 70 |

**Phase 2: Gear 4 (75–50%)**: cycles **Boundman → Snakeman → Tankman** every 20 s, announced each time.
| Form | Moves |
|---|---|
| **Boundman** (bouncing, airborne) | **Kong Gun** (spring punch 60), **King Kong Gun** (giant compressed fist, 14 m AoE, flattens terrain, 220), **Leo Bazooka** (double palm blast, huge knockback, 130), **Rhino Schneider** (flying dropkick dash, 120), **Kong Organ** (6-arm barrage) |
| **Snakeman** (speed, homing) | **Jet Culverin** (homing punch that accelerates the longer it travels: damage = 20 + 10 per block, max 200), **Black Mamba** (homing barrage, afterimages), **King Cobra** (arm circles the arena for 3 s, then the fist enlarges and slams the marked player, 250), **Hydra** (360° barrage from every direction, Conqueror-infused, 9 × 30) |
| **Tankman** (fortress) | takes **−70% damage**; **Gomu Gomu no Cannonball** sucks a player into his belly and launches them 40 blocks (60 + fall) |
- **At the end of Phase 2 (canon Gear 4 drawback):** he deflates and **can't use Haki for 10 s**. That's a damage window, and his Conqueror's aura is off.

**Phase 3: Gear 5, "the most ridiculous power in the world" (50–15%)**. The Drums of Liberation start. Cartoon physics everywhere.
| Move | Mechanic | Dmg |
|---|---|---|
| **Rubberize the World** (passive, every 10 s) | a 10-block disc of ground becomes "rubber": bouncy blocks for 15 s. Players launch on landing; projectiles bounce. Restored afterwards. | — |
| **Gomu Gomu no Kaminari** | grabs a lightning bolt from the sky (`STORM_BOLT`) and **throws it** like a javelin, 3 bolts at marked spots | 80 each |
| **Gomu Gomu no Mogura Pistol** | punches the floor; the floor beside a player **stretches into a tendril** that hits from the side (ground eruption under a random player, 1 s tell) | 90 |
| **Gomu Gomu no Dawn Cymbal** | giant hands **clap a player flat**: squash model, 3 s stun, then flung | 120 |
| **Gomu Gomu no Star Gun** | spin, then a giant hook; **cartoon stars** burst out on impact; long knockback | 160 |
| **Gomu Gomu no Dawn Rocket** | pulls the terrain back like a slingshot, hair-goggles, whips into a player: a fist that stretches through them, launched far | 180 |
| **Gomu Gomu no Dawn Gatling** | giant hardened fists without wind-up, 3 s barrage cone | 15 × 25 |
| **Gomu Gomu no Dawn Kraken Gatling** | tentacle-like arms in all directions (360°), clears projectiles and summons | 12 × 30 |
| **Gomu Gomu no Gigant** | **becomes a giant** (×6 scale) for 20 s. Stomps flatten 8 m, and **Nawatobi** jump-ropes with a player (dodge by jumping on the beat) | stomp 140 |
| **Gomu Gomu no Majin** | transforms into a **genie** of smoke for 10 s: hovers, untouchable except by Haki, ends with **Suki Ari Bullet** (a sucker punch on whoever hit him most) | 200 |
| **Cartoon dodge** | when hit by a big attack (>15% of a phase bar) his "eyes pop out": he takes the hit flattened, then pops back and **reflects 30%** | — |
| **Laughter** | his laugh gives **all players** Confusion I for 2 s and Speed I (canon: his joy is contagious; the fight gets "fun") | — |

**Phase 4: Bajrang Gun / Monkey form (15–0%)**
- **Gear 5 exhaustion** (canon drawback): at 15% he turns **old** (the aged model) and is stunned for **12 s** at **+75% damage taken**. Unless a player eats food within 10 blocks of him, he **steals the food** (canon: food restores him) and heals 5%.
- Then he triggers **Monkey form**: black body, fangs, faster, more chaotic; mobility on all fours. Every move's wind-up is halved, but **Laughter** stops (no buff).
- **Ultimate: Gomu Gomu no Bajrang Gun**
  - **Telegraph:** a 10 s sky-sized fist inflates above the arena. A gold aura and black-red lightning appear, and the arena's edges glow.
  - **The hit:** 100% of max HP to anyone not inside the **Seastone ring**. Earlier, at 25% HP, he drops 4 "Seastone Fragment" blocks; players must carry them to the marked spot to form the ring, which makes the fist bounce off.
  - **Real-time check:** the fist is "emitted" (Haki), so blocks don't protect you. Only the ring does.
  - It fires once. If he survives it, he collapses (the end of the fight is free damage).

### 3.5 Counters
- **Water** weakens him.
- **Seastone** disables his moves.
- **Swords** hurt more.
- **Haki-type skills** get past the rubber body.
- Lightning skills are useless.
- Blunt / fist players should use Haki or aura coating.
- He's open while **eating**, and right after Gear 4 and Gear 5 end.

---

## 4. Drops: "as overpowered as they need to be"
Drop policy:
- **One guaranteed core drop per kill**: rolled from the boss's core table, weighted toward items the killer doesn't have yet.
- **2–4 gear drops**.
- Core abilities are **Tensura Ultimate skills** (God-class). Gear is **relics** (Grade `GOD` → `AKASHIC`).
- **One per server (optional):** One For All and the Nika fruit, via the `TrulyGodClassOwnership`-style `SavedData` pattern from trnightmare (gamerule, default off).

### 4.1 From Deku
**Core skill: One For All** (Ultimate, God class; obtained by **eating "A Strand of Hair"**, which is canon: DNA ingestion)
- **Stockpile:**
  - +2% max EP per mastery milestone
  - it grows: each **previous holder** (players who had it before, tracked server-wide) adds +10% power
- **Modes:**

  | Mode | What it does | Cost / limit |
  |---|---|---|
  | **Full Cowl %** | toggle; scroll to set 5–100% | buffs scale with %; above your mastery-safe % you **break limbs** (self-damage + weakness) — canon |
  | **Smash** | punch | power = current % |
  | **Shoot Style** | kicks | no arm recoil |
  | **Delaware Air Force** | ranged air bullets | — |
  | **United States of Smash** | ultimate punch | 30% max MP, 120 s cooldown |

- **Vestige awakening:** each vestige quirk unlocks as a sub-skill at mastery 15 / 30 / 45 / 60 / 80 / 100%, in canon order: **Blackwhip, Float, Smokescreen, Danger Sense, Fa Jin, Gearshift**.
  - **Blackwhip:** grapple, swing, bind, carry; at 100% mastery, up to 6 tendrils.
  - **Float:** creative-style flight; drains MP per second.
  - **Smokescreen:** purple fog cloud (blindness); Full Blast covers the whole area.
  - **Danger Sense:** auto-dodge 20%, a "threat" HUD highlight, warns about nearby hostile players.
  - **Fa Jin:** charges while you move, releases ×2.5 on the next hit.
  - **Gearshift:**
    - Transmission combos; Low Gear slows a target; Overdrive (120%) sets every vestige to max for 60 s
    - then **Gearshift Recoil**: paralysis for 20 s, doubled if you reuse it inside 5 min (canon)
- **Transfer:** "Transmit One For All" gives the whole skill to another player (they eat your hair). The previous holder keeps a weak **"Embers"** passive (+10% damage, fading over 7 in-game days). Canon.

**Relic drops**
| Item | Grade | Effect |
|---|---|---|
| **Air Force Gloves** | GOD | Delaware Air Force for anyone (3 bullets, 10 s cooldown); +30% ranged punch damage |
| **Iron Soles** | GOD | store kick impact, so each kick charges the next (+15%, stacks to ×5); no fall damage |
| **Gran Torino's Cape** | GENESIS | Float-lite (slow fall + 3 air dashes); a speed aura for allies |
| **Costume Zeta** (armor set) | GENESIS | 4 pieces; set bonus: Full Cowl-style speed + jump + red-vein visual |
| **Mid-Gauntlets** | GOD | 100% smash without limb recoil, 3 charges |
| **Armor Suitcase** (Melissa & Mei suit) | AKASHIC | Quirkless-friendly armor that grants **non-quirk copies** of Float (jet boots), Blackwhip (energy cables) and Smash (gauntlets). Epilogue canon. Its own "18" case item opens into the suit. |
| **All Might's Vestige** (rare) | AKASHIC | once per death: All Might's ghost appears and casts **United States of Smash** at your killer |

### 4.2 From Luffy
**Core skill: Hito Hito no Mi, Model: Nika** (eaten as the "**Gomu Gomu no Mi**", a purple swirl fruit; texture ref `refs/luffy/base/Digitally_Colored_Gomu_Gomu_no_Mi.png`)
- **Devil Fruit rules:**
  - **one fruit per player**; eating a second fruit **kills you**
  - **can't swim**: water drains you (canon)
  - the fruit **respawns** in a random chest or dungeon when its owner dies, if the server-unique gamerule is on
- **Rubber body passive:** lightning immunity, blunt ×0.3, projectile bounce, no fall damage (you bounce).
- **Gears** unlock with mastery:
  - **Gear 2:** speed burst, Jet moves, steam; drains hunger fast.
  - **Gear 3:** Gigant / Elephant fist moves.
  - **Gear 4:** Boundman / Snakeman / Tankman.
    - When it ends: **no Haki for 10 min** at low mastery, dropping to 0 at max mastery (canon).
  - **Gear 5 (Awakening):**
    - **Requirements:** fruit mastery max **and** the player has **died once while holding Gear 4**. Canon: his heart stopped, then he awakened.
    - **Effects:** cartoon physics, rubberize terrain, grab lightning, Gigant, Dawn moves, Drums of Liberation music.
    - **Cost:** stamina drain. When it ends you're **"aged"** for 60 s (slowness, weakness). Eating restores you instantly.

**Haki skills** (separate Extra-skill line; also craftable from training, see 5.2)
| Skill | Basic → Advanced (mastery) |
|---|---|
| **Armament Haki** | coat limbs black: damage + defence, and hits Logia-type / intangible enemies → **Emission** (hits at range without contact) → **Internal Destruction** (ignores armor) |
| **Observation Haki** | sense entities through walls → **Future Sight** (dodge chance + a ghost preview of the next enemy attack) |
| **Conqueror's Haki** (rare: only "King"-tier races, or a drop) | knock out weak mobs and players (below 10% of your EP) → **Infusion** (attacks gain black lightning, +40% damage, and suppress regeneration / immortality: canon, Elbaph) |

**Relic and item drops**
| Item | Grade | Effect |
|---|---|---|
| **Straw Hat** | AKASHIC | +freedom: immune to slowness, root and mind control; +25% XP; the "Will of D." (see races) |
| **Seastone Cuffs / Seastone Bullet** | GOD | apply "Seastone Cuffed" (disables fruit / quirk skills) for 6 s; also the Luffy counter item |
| **Meat on the Bone** | — | instantly restores Gear exhaustion, Gearshift recoil, and Tensura magicule/aura +50% |
| **Vivre Card** | GENESIS | points to its owner; while it exists, the owner revives once a day at 10% HP |
| **Ragnir** (Loki's hammer, Elbaph) | AKASHIC | thunder hammer that **charges Dawn Thor Bullet** for Nika users; otherwise a lightning maul |
| **Elbaph Viking Helmet** | GENESIS | +armor; in Gear 5, grants the horned look and +20% Gigant damage |
| **Poneglyph Rubbing** | — | a quest item, and an ingredient for the "Pirate King" evolution |

---

## 5. Hidden races (Tensura evolution ladders)
Built as `DefaultRace` subclasses with a **`LineRace`-style enum** (DxD pattern): one config section per line, custom `EvolutionRequirement`s normalised to 100, and starting skills granted with `NoMagiculeCost`.
- **Hidden** = not in the reincarnation menu. You reach them only through evolution requirements, special items, or killing these bosses.
- Stats are ranges for HP, aura (AP) and magicule (MP).
- **Tensura anchors:** Human 20 HP / ~60 MP; Saint or Demon Slime about 500 HP / 400k MP. The top tiers here are intentionally beyond that.

### 5.1 Quirk lines (My Hero Academia): enter from **Human**
**Entry:** "Quirk Awakening". A Human who reaches 10,000 EP has a 5% chance per EP milestone of awakening a **random quirk skill**, or does it guaranteed by eating a **Quirk Orb** (a boss drop).

```
Quirk Bearer ─┬─ HERO line ─ Sidekick → Pro Hero → Top Hero → Symbol of Peace → ★ One For All: Ninth Successor
              └─ VILLAIN line ─ Vigilante/Villain → League Commander → Villain Lord → Symbol of Fear → ★ Demon King of Quirks (All For One)
```
| Tier | Race | HP | AP / MP | Intrinsic skills | Evolution requirement (design) |
|---|---|---|---|---|---|
| 1 | **Quirk Bearer** | 40 | 2k / 5k | Quirk (random), Quirk Mastery | Human + Quirk Awakening |
| 2H | **Sidekick** | 80 | 15k / 20k | + Rescue Instinct (bonus near allies) | 50 hostile kills while protecting a player or villager; 50k EP |
| 3H | **Pro Hero** | 200 | 120k / 150k | + Hero License (villager trade discount, Iron Golem allies), Plus Ultra (low-HP power surge) | 500k EP; Hero quests; 0 player murders |
| 4H | **Top Hero** | 600 | 1M / 1M | + Heroic Presence (fear aura on hostile mobs), quirk awakening (sub-ability) | 5M EP; defeat an arc boss |
| 5H | **Symbol of Peace** | 1,500 | 8M / 8M | + Symbol of Peace (allies in 64 blocks get +20% stats), "I Am Here" (teleport to a downed ally) | 30M EP; Hero Rank #1 (server stat: most boss assists) |
| ★H | **One For All: Ninth Successor** | 3,000 | 25M / 25M | One For All (+ all vestiges unlocked faster), Vestige Council (ghost advisors: buffs) | Symbol of Peace **+ holds One For All at 100% mastery** + beat Deku solo or in a group |
| 2V | **Villain** | 80 | 20k / 15k | + Intimidation | 25 player kills, or 200 villager/golem kills; 50k EP |
| 3V | **League Commander** | 200 | 150k / 120k | + Rally the League (summon 3 "Nomu" minions) | 500k EP; lead a party of 3+ villain-line players |
| 4V | **Villain Lord** | 600 | 1M / 1M | + Decay (touch decays blocks, then flesh; spreads), Warp Gate | 5M EP; destroy an arc boss's arena |
| 5V | **Symbol of Fear** | 1,500 | 8M / 8M | + Paranormal Liberation (allies +20%), Quirk Steal (copy one skill from a killed player for 1 hour) | 30M EP; 100 player kills |
| ★V | **Demon King of Quirks** (All For One) | 3,000 | 25M / 25M | **All For One**: steal and keep up to 8 skills from players and mobs, combine 2 into a fused skill; **Give**: lend a skill to a player | Symbol of Fear + defeat **Deku** while holding Quirk Steal at max mastery |

**Side line: Quirkless** (canon: Deku started Quirkless)
- **Quirkless** (Human with Quirk Awakening refused, via an item) → **Quirkless Prodigy** → **Support-Gear Master**.
- Huge bonuses from all relic and gear items (+100% relic effect) and immunity to quirk-steal and quirk-erase.
- The only line that can wear the **Armor Suitcase** at full power.

### 5.2 Sea lines (One Piece): enter from **Human**, or by reaching a beach biome with a boat
```
Sailor ─┬─ PIRATE (Will of D.) ─ Rookie → Supernova → Worst Generation → Emperor (Yonko) → ★ Pirate King → ★★ Joy Boy
        ├─ GIANT (Elbaph) ─ Young Giant → Elbaph Warrior → Giant Warrior Captain → ★ Ancient Giant
        ├─ FISH-MAN ─ Fish-Man → Fish-Man Karate Master → ★ Sea King's Voice (Poseidon)
        ├─ MINK ─ Mink → Electro Warrior → ★ Sulong (full-moon form)
        └─ LUNARIAN ─ Lunarian → ★ Sky Flame Lord (fire wings, lunarian flame)
```
| Tier | Race | HP | AP / MP | Intrinsic skills | Evolution requirement |
|---|---|---|---|---|---|
| 1 | **Sailor** | 30 | 1k / 1k | Seamanship (boat speed, no seasickness), Observation Haki (latent) | Human who sails 10,000 blocks |
| 2 | **Pirate Rookie** | 80 | 30k / 10k | Armament Haki (basic), Bounty (shows on the HUD; grows with kills) | 50k EP; Bounty ≥ 30M berries (= kills) |
| 3 | **Supernova** | 250 | 300k / 100k | Observation Haki, Crew Bond (+10% stats per crewmate within 32 blocks) | 1M EP; Bounty ≥ 100M; own a ship (a claimed boat) |
| 4 | **Worst Generation** | 700 | 2M / 800k | Advanced Armament (Emission), Future Sight | 10M EP; beat 2 arc bosses; Bounty ≥ 500M |
| 5 | **Emperor (Yonko)** | 1,800 | 15M / 5M | **Conqueror's Haki** (+ Infusion), Emperor's Territory (claimed chunks: allies +stats, mobs won't spawn), Grand Fleet (summon crew NPCs) | 50M EP; Bounty ≥ 3B; a crew of 3+ players |
| ★ | **Pirate King** | 3,500 | 40M / 15M | **Will of D.** (immortal once a day: revive at full HP with a grin), Voice of All Things (talk to and tame any mob, sense bosses through the world), "Freest Person" (immune to all control and CC) | Emperor + 4 Road Poneglyph Rubbings + **the One Piece** (a world treasure in the end-game dungeon) |
| ★★ | **Joy Boy** (hidden-hidden) | 5,000 | 80M / 30M | **Sun God's Heartbeat**: Gear 5 lasts ×3 with no aging drawback; **Liberation**: free all allies from every debuff; **Laughter of Freedom**: an aura where enemies lose 30% damage | Pirate King **+ holds the Nika fruit at max mastery + has beaten Luffy** |

- **Giant (Elbaph):** huge scale (×3–×5 model). Size breaks blocks while walking, so enable it by config. Very high HP. Haki over the giant body. Top tier: **Ancient Giant** (×8, Conqueror's).
- **Fish-Man:** breathes underwater, ×3 swim speed, **Fish-Man Karate** (shockwaves that ignore blunt immunity, so they counter rubber users). Top tier: **Sea King's Voice**, which summons and commands sea monsters (guardians, dolphins, custom sea kings).
- **Mink:** **Electro** (lightning on melee hits); **Sulong** under a full moon (night with a moon phase): ×2 stats, white fur, but berserk if you look at the moon too long.
- **Lunarian:** fire immunity, a flame that can't be put out, wings (glide/flight). Top tier: **Sky Flame Lord**, whose flames turn off on landing (canon: Lunarian flames turn off when exposed). Durable when the fire is lit, fragile when it isn't.
- **Devil Fruit interaction:**
  - any Sea-line race can eat **one** fruit
  - Fish-Man and Sea-line Devil Fruit users lose their swimming perks (the fruit wins)

### 5.3 Cross-line busted interactions (go wild, keep them gated)
- **Joy Boy + Ninth Successor can't co-exist** on one player. The two "Symbols" exclude each other because the race lines are separate.
- **Conqueror's Haki** suppresses **regeneration and immortality skills** (canon: Elbaph), so it counters Tensura regen races. Use it sparingly.
- **One For All vs All For One:** if a Demon King of Quirks touches a OFA holder, they can try to steal **one vestige**, not OFA itself (canon: only Danger Sense was ever stolen).
- **Seastone** (item / block) disables **all** fruit and quirk skills in a 2-block radius. It's the universal counter, and craftable from boss drops only.

---

## 6. Implementation map
| Piece | Build as | Notes |
|---|---|---|
| Deku boss | `arc/mha/DekuBoss extends ArcBoss`, `DekuTable`, `DekuMoves` | copy the structure of `sinbad_boss/`. **Form system from §1.1**: 1% clamp, 5 s invulnerable `beginForm`, a `MAX_HEALTH` modifier per form + `refillForMode`, a `form` column in the move table, form saved in NBT |
| Luffy boss | `arc/onepiece/LuffyBoss extends ArcBoss` | the 7-form system from §1.1 (same clamp and refill). Model indexes: 0 base, 1 Gear 2, 2 Gear 3, 3 Boundman, 4 Snakeman/Tankman, 5 Gear 5, 6 Gigant (scale), 7 monkey. Rubberized terrain through `tryChange` (slime blocks / custom "rubber" block) + `restoreBlocks` |
| ArcSaga | add `MHA("My Hero Academia", 9)`, `ONE_PIECE("One Piece", 10)` | |
| Characters | `ArcCharacter.boss("deku", "Deku").title("One For All's Ninth")…`, `ArcCharacter.boss("luffy", "Monkey D. Luffy").title("Sun God Nika")…` | stats from §1 |
| Damage types | `nusmp:smash` (blunt), `nusmp:haki` (bypasses rubber), `nusmp:conqueror` | tag `haki` into the Tensura magic/physical tags as appropriate |
| Skills | `NUSkill` ultimates: `one_for_all`, `nika_fruit`; extras: `armament_haki`, `observation_haki`, `conquerors_haki`; vestige sub-skills as modes or learning instances | use mastery milestones from the Build Spec; percentage costs |
| Drops | `RelicDef`s at Grade GOD / GENESIS / AKASHIC; armor via `RelicArmorItem` | |
| Races | `race/mha/QuirkLineRace`, `race/onepiece/SeaLineRace` (enum lines × ranks) | inject into reincarnation lists only for tier-1 entry races, if at all (they're hidden) |
| Assets | `geo/entity/arc_deku*.geo.json`, `arc_deku_form2` (Overlay); `arc_luffy*`, `arc_luffy_form2` (Gear 5), plus extra forms through `ArcLook` skins | Deku 1.66 m; Luffy 1.74 m |
| Music | Drums of Liberation heartbeat loop (original composition, not ripped) | `NUSounds` |

## 7. Reference images and Minecraft skins
**Image pack (`refs/`):**
- 30 Deku images and 35 Luffy images from the official Fandom wikis, saved for **reference only**.
- They're copyrighted art. **Do not ship them in the mod.** Use them to model, texture and match VFX.
- `refs/*/index.json` maps every file to its wiki page.

| Folder | Shows |
|---|---|
| `refs/deku/look/` | Costume Zeta, Deku Overlay, Dark Deku, final punch, vestiges, last smash (gif), embers |
| `refs/deku/one_for_all/` | Full Cowl 100%, 45%, Shoot Style (gif), 100% barrage (gif) |
| `refs/deku/gearshift/` | Gearshift, Transmission, Overdrive, Detroit Smash Quintuple (all gifs) |
| `refs/deku/fa_jin/`, `danger_sense/`, `blackwhip/`, `smokescreen/`, `float/` | each vestige in action (Blackchain gif, Faux 100% gif, Float + Air Force gif) |
| `refs/deku/smashes/` | Detroit, Delaware Air Force, Manchester, St. Louis, United States of World Smash |
| `refs/luffy/gear5_look/` | Gear 5 infobox, wanted poster, monkey form, aged after Gear 5 |
| `refs/luffy/gear5_physics/` | terrain rubberization / object creation, elasticity, cartoon effect |
| `refs/luffy/gear5_moves/` | Gigant, Bajrang Gun, Kaminari, Dawn Rocket, Dawn Gatling, Dawn Balloon, Majin, Dawn Thor Bullet |
| `refs/luffy/gear4/` | Gear 4, Boundman, Snakeman, Tankman, King Kong Gun, Black Mamba |
| `refs/luffy/gear3/`, `gear2/` | Gigant Pistol, Red Roc, Gear 2 speed (gif), Red Hawk (gif) |
| `refs/luffy/haki/` | Armament hardening, advanced Armament (gif), Future Sight (gif), Conqueror's (gif), Kaidou punch, clash, Splitting the Heavens |
| `refs/luffy/base/` | rubber elasticity, Pistol, **the Gomu Gomu no Mi fruit** (item texture ref) |

**Minecraft skins** (Planet Minecraft; the site blocks automated downloads, so open the links and use "Download skin"). These are fan skins: **ask the creator before reusing them** in a published mod, or use them only as style reference.
- Deku, Final War (Costume Zeta): https://www.planetminecraft.com/skin/izuku-midoriya-deku-costume-zeta/
- Deku, Full Cowl 100% (revamp): https://www.planetminecraft.com/skin/izuku-midoriya-deku-one-for-all-full-cowl-100-my-hero-academia-revamp/
- Deku, 120% One For All: https://www.planetminecraft.com/skin/izuku-midoriya-deku-120-one-for-all/
- Deku, Shoot Style: https://www.planetminecraft.com/skin/izuku-midorya-deku-my-hero-academia-mha/
- Luffy, Gear 5: https://www.planetminecraft.com/skin/monkey-d-luffy-gear-5-one-piece-6084520/
- Luffy, Gear 5 / Sun God Nika: https://www.planetminecraft.com/skin/monkey-d-luffy-gear-5-sun-god-nika-one-piece-5914770/
- Luffy, Gear 5 (2023): https://www.planetminecraft.com/skin/luffy-gear-5-2023-version-one-piece-1-8/
- Luffy, Gear 5 (Joyboy): https://www.planetminecraft.com/skin/monkey-d-luffy-gear-5-sun-god/
- More: https://www.planetminecraft.com/skins/?keywords=deku+one+for+all and https://www.planetminecraft.com/skins/?keywords=luffy+gear+5

**Existing Minecraft mods to study** (for how others did quirks and fruits; don't copy code or assets):
- One Piece: *Mine Mine no Mi* and its addons (*One Piece World Like*, *Mine Piece HUD*, *One Piece Bosses*), *One Piece (OP Craft)*, *One Piece: The Will of Dirt*
- MHA: *My Hero Craft [MHC]*, *Quirks Unchained*, *Mine Hero RX*
- All on CurseForge.
