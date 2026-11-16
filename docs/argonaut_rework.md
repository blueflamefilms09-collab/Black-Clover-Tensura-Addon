# Argonaut rework: Firebolt folded into Argonaut, then made wiki-accurate

Owner request (4.4.0): "Firebolts should be mixed in with the Argonaut skill ... Argonaut Hero should be called whatever that main character from that anime is. It should give it his name plus something else."
Follow-up: the DanMachi Fandom wiki was fetched, and the owner asked for the abilities to follow the anime in names, mechanics, order of unlocks, limits, drawbacks and the look of the effects. Where the wiki is silent the guess is marked **design** below, never canon.
Nothing here was run in a game (no Gradle in the session; CI is the compiler). Section 8 lists what is unverified.

## 1. Sources (English wiki first, page level)

All from `danmachi.fandom.com` (English wiki, wikitext fetched). The Japanese wiki was not needed. Not fetched at all: no page for "Level 6", "Rabbit Rush" or any Level 6 Bell entry exists in the fetched set (the wiki's Bell page stops at Level 5).

| Fact | Page |
|---|---|
| Bell's Magic is Firebolt only; his Skills are Liaris Freese, Argonaut, Ox Slayer, Vanadis Tevere | `Bell_Cranel` (infobox, Magic, Skills) |
| Firebolt: no chant, only the name is said, so it is quick; used at short range and at most medium range, **long range is its weakest point**, a distant fast target is hard to hit; a weak magic for lack of a chant but strong from Bell's level; "He sometimes combines it with Argonaut for stronger damage" | `Bell_Cranel` (Magic), `Magic` (table: "No chant magic that can be used quickly") |
| Argonaut (英雄願望, Eiyū Ganbō): a charge for an active action; spends **stamina and mind**; one powerful attack Hestia calls the **"hero's strike"**; longer charge = stronger **and** costlier; one charge in one place at a time; full charge **3 min, 4 min at Level 4, 5 min at Level 5**; the charge makes a noise like **a small chime** | `Bell_Cranel` (Skills), `Skills` table |
| Can strengthen attacks that come from him or help him move; the charge dissipates **when he is attacked or loses focus**; a charge put into something else fades a few moments after it leaves his hand; dispelling at will does **not** refund stamina and mind; **recoil** damages whatever is charged (destroys weapons, injured his hand on Dix), worse the longer the charge | `Bell_Cranel` (Skills) |
| In a **Limit Off** the charge's power grows exponentially and it sounds like a grand bell: **Grand Bell** (大鐘楼, グランド・ベル); it also seems to restore and raise the morale of those who hear it | `Bell_Cranel` (Skills), `Terminology` (Limit Off) |
| Growth order: Argonaut gained at Level 2; in the fight with Lyd he became able to use it **while moving**; after Asterius he devised **dual charge** (二重集束, two charges at once) | `Bell_Cranel` (Skills) |
| **Argo Vesta** (聖火の英斬): Firebolt cast onto the Hestia Knife, then Argonaut dual-charged onto both; light particles; the blade becomes sword-thick and short-sword-long and glows **crimson** as the charge grows; the slash gives a **bright flash, a heatwave and a shockwave**. A variant flies the slash off on Firebolt's projectile nature: the **"pseudo-Afterglow"** | `Bell_Cranel` (Skills), `Bell_Cranel_Equipment` (Hestia Knife survives Argonaut and Argo Vesta) |
| Argonaut-charged Firebolt examples: 3 min at Level 2 destroyed the Black Goliath's head and exploded on a wall; 20 s charge broke a Level 5's ribs; 60 s charged Firebolt destroyed a throne room; 4 min charge with Limit Off destroyed Nidhogg | `Bell_Cranel` (Skills), `Bell_Cranel_Synopsis` |
| Liaris Freese (憧憬一途): faster growth **as long as he is in love**, strength tied to the strength of the feeling; **immune to charms** as a side effect; lets him exceed his limits; ceases if his feelings changed | `Bell_Cranel` (Skills, trivia), `Skills` table |
| Bell is the **reincarnation of Argonaut**; Argonaut's nicknames: **Hero of the Beginning**, Clown, Fool, Navis the Guide | `Bell_Cranel` (intro), `Argonaut` |
| Bell's aliases: **Little Rookie** (Level 2, former), **Rabbit Foot** (Level 4, former), **Regulus Arne** (current, Level 5); nicknames Record Holder, Ox Slayer | `Bell_Cranel` (infobox, intro) |

## 2. What the 4.4.0 version had invented

| 4.4.0 | Wiki says |
|---|---|
| Firebolt is "lightning-fire", plus a visual lightning on every hit | Not stated: Firebolt is simply a no-chant fire magic. (The orange bolt and the visual-only lightning are kept as looks; the text no longer claims it.) |
| Firebolt reaches 32 / 48 blocks at full strength | Short to medium range; long range is the weak point |
| A charged **plain strike** burns what it hits (the "Firebolt fire") | The hero's strike is a plain charge. Fire comes from Firebolt itself: Argonaut-charged Firebolt, and Argo Vesta |
| Cap 5 bells (Unique), 10 (Ultimate) from the start | 3, then 4, then 5 minutes |
| A bell costs magicules only | Stamina and mind |
| Nothing breaks a charge; no recoil; walking while charging is free | A hit breaks it; recoil on the charged item; charging while moving is learned later; Dual Charge later still |
| Modes: Strike, Beam, Firebolt; names "Hero's Strike", "Hero's Beam" | "Hero's strike" is the canon name of the attack; the beam matches the **pseudo-Afterglow**; the Grand Bell is the enhanced charge |
| Ultimate named "Bell Cranel, Hero Argonaut" | see section 5 |

## 3. The new Argonaut (setting `danmachi.argonautWikiCanon`, default on, needs the merge)

**Modes (index order, append-only).** Argonaut: 0 **Hero's Strike**, 1 **Firebolt**, 2 **Charged Firebolt**, 3 **Argo Vesta**. Hero's Argonaut: 0 **Grand Bell** (strike), 1 **Pseudo-Afterglow** (the old Hero's Beam, same id `argonaut.beam`), 2 Firebolt, 3 Charged Firebolt, 4 Argo Vesta. Old indexes keep their meaning; a saved index past the live range falls back to the strike. Mode ids `argonaut.strike`, `argonaut.beam`, `argonaut.firebolt` and the lang keys are kept; only the display texts changed.

| Mechanic | Canon source | Implementation |
|---|---|---|
| Hold to charge, a small chime per step; stamina and mind | Skills, Bell_Cranel | Existing bell chime per second (vanilla bell block sound); each bell pays magicules (mind) as before **and** `0.8` vanilla exhaustion (stamina; five bells = one food point) |
| 3 : 4 : 5 minutes | Bell_Cranel | **Ratio kept, minutes scaled to bells (one bell per second)**: Unique caps at 3 bells, 4 from half mastery, 5 once mastered. The "Level 4 / Level 5" are mapped to mastery (**design**). Ultimate keeps ten (the Limit Off grand bell, **design** number) |
| Grand Bell sound for the Ultimate | Bell_Cranel (Limit Off) | The Ultimate rings the resonating bell (`BELL_RESONATE`); the Unique the small chime |
| Hit breaks the charge, no refund | Bell_Cranel | The caster's health is compared each tick while held; any loss ends the hold and the charge (no refund, as in the wiki) |
| Charging while moving is learned later | Bell_Cranel (Lyd) | Until 25 % mastery (**design** threshold) the charge breaks if you drift more than 1 block from where you began; the Ultimate may always walk |
| Dual Charge, then Argo Vesta | Bell_Cranel (after Asterius) | The Argo Vesta mode is locked below 50 % mastery (**design** threshold; also the 4-bell cap), the Ultimate has it at once. Dual Charge is expressed as Argo Vesta only (no separate "two charges at once" mode: **not implemented**) |
| Argonaut-charged Firebolt | Bell_Cranel, Synopsis | **Charged Firebolt** mode: hold, then release one Firebolt carrying the bells. Damage x(1 + 0.8 per bell) (5 bells = 5x; **design** numbers), blast +0.3 block per bell (max +3), a wider crimson bolt; pays Firebolt's price at release. Not range-weakened ("flew on a great distance") |
| Argo Vesta | Bell_Cranel | Firebolt's price is paid at release (it is cast onto the blade); the cone strike hits x1.25 harder, burns everything it hits for 2 + bells seconds (max 12), knocks back x1.5 (the shockwave). If the price cannot be paid it is a plain strike. Looks: a crimson blade, a white flash, a fire burst (heatwave), a projected crescent (shockwave) and a muffled boom; while charging a crimson flare brightens with every bell |
| Pseudo-Afterglow | Bell_Cranel | Hero's piercing beam (kept, same numbers) renamed; it burns what it pierces (Firebolt flying on the slash) |
| Recoil | Bell_Cranel | After a Hero's Strike / Argo Vesta release the item in the main hand loses `bells - 1` durability (half for the Ultimate). Never destroys it outright (**design** softening; the wiki says weapons break). Hand injury: **not implemented** (it would damage the player; see section 8) |
| Firebolt chantless, fast, weak at long range | Bell_Cranel, Magic | Press = instant (kept). New range falloff: full damage to 16 blocks, falling linearly to 50 % at the end of its range (**design** numbers), plain bolt only |
| Mastery | design | A Firebolt that hit teaches Argonaut 0.5; a charged release teaches its bells if it hit (as before) |

**Unmentioned in the wiki, so not canon:** every number above marked design, the prices (`argonautMagiculePerCharge` 200, `fireboltMagiculeCost` 100, cooldowns 200 / 20), Firebolt's damage 8 and range 32 / 48, the visual lightning, beam length 12 to 42, Vesta's x1.25 and x1.5. The Rabbit Rush twin-knife combo is not in the fetched pages and is not built.

## 4. NEW / REPLACED / KEPT

| Part | Status |
|---|---|
| Modes Charged Firebolt (3 / 4) and Argo Vesta (4 / 5); `ChargeRules.Strike` CHARGED_FIREBOLT, ARGO_VESTA appended | **NEW** |
| 3 : 4 : 5 cap, stamina per bell, hit breaks the charge, moving-charge unlock, Argo Vesta unlock, recoil, Firebolt range falloff, charged-Firebolt damage | **NEW** (`ArgonautMergeRules`, `ChargeRules.canonMaxCharge`, `FireboltRules.chargedFactor`) |
| Setting `danmachi.argonautWikiCanon` (SERVER, default true); off = exactly 4.4.0 | **NEW** |
| Self-test keys `argonaut.modes`, `argonaut.rules` (prefix `argonaut` already in `SMOKE_EXPECT`) | extended |
| Burning plain strike (mode 0 burned in 4.4.0) | **REPLACED** under canon (only Argo Vesta and the beam burn); **KEPT** with the setting off |
| Mode names: Argonaut/Hero's Strike -> Hero's Strike / Grand Bell, Hero's Beam -> Pseudo-Afterglow; descriptions of Argonaut, Hero's Argonaut, Firebolt, Liaris Freese | **REPLACED** (text only, `tools/lang/argonaut_canon.json`) |
| Firebolt's shot code | refactored into `FireboltSkill.fire` (shared), behaviour of the plain bolt KEPT except the falloff |
| The Firebolt mode, migration `ArgonautMerge`, setting `argonautFireboltMerge`, ids, config keys, icons, VFX shapes, evolution ARGONAUT -> HEROS_ARGONAUT, Liaris Freese code | **KEPT** |

## 5. Name of the Ultimate

**"Bell Cranel, Hero Argonaut" is kept** (registry id `heros_argonaut` unchanged). The wiki gives no such title for Bell; it is a composition: his name (the owner asked for it), "Argonaut" (his skill and his past life, **the Hero of the Beginning**, whose reincarnation he is: `Argonaut`, `Bell_Cranel` intro) and "Hero" (the evolved form). The title is therefore grounded in canon by the Hero of the Beginning link, but it is **not a canon alias**. Canon aliases for the owner to choose from: **Regulus Arne** (his current title, Level 5), Little Rookie, Rabbit Foot, Record Holder, Ox Slayer; "Hero of the Beginning" is Argonaut's, not Bell's. The Hero description now says "reincarnation of Argonaut, the Hero of the Beginning". Decision for the owner: keep, or switch to "Bell Cranel, Regulus Arne" (one string in `heros_argonaut.json`, `forge_tables.py`, the lang file, `HerosArgonautSkill`, README).

## 6. Liaris Freese

Canon: growth as long as he is in love, strength by feeling, charm immunity, exceeds limits. The mod has the growth part (kills raise mastery); the description now says it is the growth that lasts as long as the feeling, and that charm immunity is not built (Tensura's charm handling not touched here; not part of this task). Code, numbers and ids unchanged.

## 7. Bounded, server-safe, worst case

- All rules on the server; the client only draws the existing shapes (`LIGHT_BLADE`, `LIGHT_FLARE`, `FIRE_BURST`, `SLASH_WAVE`, `SKILL_FIREBOLT`, `SKILL_ARGONAUT`); no new client class, packet or shape.
- State per player on the skill instance (`Paid`, `Blocked`, `Hp`, `Px`, `Pz`); nothing static, nothing to clean on logout (reset at the next press).
- Per tick while a player holds the key: two float reads and, for the moving check, one distance; no allocation, no world scan. Hold effects unchanged except a crimson flare packet per bell in Argo Vesta: at most one chime + one flare packet per second per charging player, <= cap (10) per hold.
- Release (at most one per cooldown, 200 ticks default): Charged Firebolt = 1 ray cast, <= 24 splash targets (`SkillTargets.MAX_FOES`), 1 visual lightning entity (short-lived), 3 effect packets. Argo Vesta = <= 24 targets ignited, 5 effect packets, 2 sounds. Recoil = one `hurtAndBreak`. Instant Firebolt: unchanged (1 cast per 20 ticks, 3 packets).
- Setting off restores 4.4.0 exactly (modes, burn, cap, no stamina, no break).

## 8. Tests and unverified

- `ArgonautMergeRulesTest`: ordinals pinned, mode table incl. canon, burn table, 3:4:5 cap, unlocks, hit break, recoil, falloff, charged factor. `argonaut.modes` / `argonaut.rules` self-tests check the live mode ids and the canon tables.
- **Not seen in a game / not compiled** (only the pure rules classes were compiled and spot-run with javac): the break-on-hit via health compare (absorption and potion regeneration edge cases), the moving-charge break feel, the held-time cap against the real key handler, `hurtAndBreak` on the main hand, the crimson / flash / heatwave / shockwave look (shapes reused, colours and sizes are guesses), the lang override (the coordinator applies `tools/lang/argonaut_canon.json`; it overrides keys already in `en_us.json`, so the merge must overwrite).
- Not implemented: dispelling a charge at will, hand injury on recoil, the morale effect of the grand bell, charm immunity, Rabbit Rush, a separate two-things-at-once Dual Charge.
- Decisions for the owner: the title (section 5), whether recoil may destroy weapons (the wiki says it does), whether mastery stands in for Bell's Levels (it does here).

## 9. Firebolt holders (unchanged from 4.4.0)

`firebolt` stays registered and castable. `ArgonautMerge` moves holders at login and respawn: a Firebolt holder gets `argonaut` (unless they hold it or Hero's Argonaut) and `firebolt` is forgotten; the holder is stamped `FireboltMigrated`; with `argonautFireboltMerge` off the next login gives Firebolt back. Argonaut starts at 0 mastery (the old Firebolt mastery is not carried). Cost: two registry reads per login.
