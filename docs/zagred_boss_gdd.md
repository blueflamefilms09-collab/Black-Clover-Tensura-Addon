# Zagred, the Devil of Kotodama: boss design document (0.51)

Status: **design only, nothing in this file is in the game yet.** It extends the 0.47-0.49 Zagred boss
(`entity/ZagredBossEntity.java`, `book/KotodamaWords.java`, `entity/TensuraCaster.java`, `client/ZagredModel.java`).
Names in `code font` are real classes and methods of the mod. Numbers are starting values to tune in play.

Sections: 0 fit with the existing boss, 1 Word Soul attacks, 2 phase transitions, 3 loot, 4 implementation logic, 5 supporting
design (daemons, presentation, scaling).

## 0. How the new design fits what the mod already has (new vs replacement)

The prompt's boss and the shipped boss are the same character, so most of it is an **addition**, not a rebuild.

| Design part | In the mod today | Verdict |
|---|---|---|
| 100/50/25% beats | Phases switch at 75, 50 and 25% (`enterPhase`) | **Keep.** The 50% and 25% entries are where the Arch Daemons arrive. 75% keeps its adaptation beat. |
| Word Soul (Kotodama) attacks | 16 words, utility AI (`choose`), reticle telegraph (`CAST_TICKS`) | **Keep, and add** the four signature attacks below on the same words system. |
| Physical / Abnormal / Spiritual nullification | Only the phase-4 "takes 15% unless anti-magic" rule | **New** (section 4.1). The phase-4 rule stays as the last-act layer. |
| Holy / Demonic / natural resistance | Phase-2 adaptation (element the party used most takes 20%) | **New** flat resistances, stacked under adaptation. |
| Ultra-speed regeneration | none | **New** (small, rate-limited, stops while the barrier is down). |
| Multilayer Barrier | none (Shatter only breaks projectiles) | **New** (section 4.2). |
| Thought Acceleration | none (it only reacts through words) | **New** (section 4.3). |
| Grimoire Daemons (3 tiers) | none. Summons in the mod are painted beasts, mirror doubles and spirit lords | **New** (section 5.1). |
| Asymmetrical wings, floating grimoire, emissive runes, text-glyph aura | A tall black body with black flakes and a crimson aura shader | **Replacement for the look only, and only if the owner confirms.** The two reference images in this request show a different, maroon, horned build with one ragged wing; the 0.49 build follows the earlier images. Both can be kept as selectable skins (rule 2). |
| Drops | Kotodama reward flag plus Zagred's five-leaf grimoire (config `kotodamaBossReward`, off) | **Keep, and add** three items (section 3). |

## 1. Word Soul attack patterns

Zagred's speech is the mechanic: he **says a word, the word appears as glowing glyphs, and the world obeys** unless the party
reads and answers it. Every attack below has one tell (glyph ring or text on the ground), a wind-up never shorter than 8 ticks,
and one answer that always works.

Damage is dealt through `KotodamaWords.hurt` / `spirit` so it scales with the victim's EP like the rest of the mod, using the
boss multiplier `1.6 + 0.3 x phase`. Values below are "at phase 1 against an average party member".

### 1.1 "Halt": Decree of Stillness (existing word, reworked tell)
- **Wind-up:** 15 ticks, a violet ring of the word HALT grows on the ground, radius 12.
- **Effect:** everyone inside the ring still standing in it when it closes is bound for 1.5 s (Tensura paralysis effect, jammed
  skills). The bind is shortened by 0.5 s per ally within 3 blocks who is not bound (a rescue tap).
- **Counter-play:** leave the ring before it closes (sprint, dodge, ender pearl), break line of sight, cut the glyphs with a
  demon sword, answer with the counter-word Reject, or be carrying the Circlet (section 3).
- **Why it is fair:** the ring is large but slow, the bind is short, and it is the only attack that roots.
- **Cooldown:** 260 ticks (as now); cannot chain with Redact.

### 1.2 "Redact": Sentence of Ruin (new signature attack)
- **Wind-up:** 20 ticks. Zagred "writes" a sentence of 5 to 8 black-red letters across the floor, in a curved line toward the
  target. Each letter is a 2-block glyph.
- **Effect:** the letters detonate **in reading order**, 6 ticks apart, each a 2.5-block blast (9 damage plus spiritual damage 1).
  The last letter is larger (4 blocks) and leaves a lingering "redacted" patch for 4 s that nullifies healing and buffs inside it.
- **Counter-play:** it is a rhythm pattern. Read the order, step between the letters, finish outside the last one. A demon sword
  or any anti-magic strike on a letter cancels that letter and every letter after it. Fliers are safe from letters but not from
  Fall (1.3), which is why he chooses it against them.
- **Why it is fair:** the sequence is always left-to-right along a visible curve; the gap between blasts is always walkable.

### 1.3 "Fall": Verdict of Gravity (existing word, extended)
- **Wind-up:** 18 ticks, the word FALL drops from above as a column of text over each airborne target (and over the party's
  centre if nobody flies).
- **Effect:** airborne targets are pulled to the ground at once (no fall damage) and pinned for 1 s. Then a crushing ring
  expands from Zagred's feet: 0.8 blocks high, 2 blocks per tick, 11 damage, knockback away.
- **Counter-play:** jump over the ring (it is low), shield through it, or block it with a placed barrier. Gravity or Spatial
  Magic users can ignore the pull by being grounded on the word's last tick. Flying is the cue, not a punishment: the ring is the
  real damage, and it is jumpable.
- **Cooldown:** 300 ticks.

### 1.4 "Overwrite": Rewrite the Rules (ultimate, phase 3 and 4 only)
- **Wind-up:** 40 ticks of visible chanting while five floating glyph-stones (the **rule stones**) orbit him at 6 blocks.
  He is channelling, so the barrier does not refresh.
- **Effect:** he writes **one rule on the arena** for 12 s, shown in big text above the boss bar. Breaking the rule costs 6
  spiritual damage per second to the offender.
  - "No fire" / "No ice" / "No lightning": elemental spells of that element hurt the caster.
  - "No flying": airborne players take the damage.
  - "No standing still": a player who stops for more than 1 s takes it. Good for the melee players.
  - "No healing": healing is undone as it happens (a rescue is still possible by killing a rule stone).
- **Counter-play:** obey the rule (the table is short and always announced), or destroy **three of the five rule stones**
  (each has the HP of a Greater Daemon) to cancel the rule early and stagger Zagred for 2 s. The Arch Daemons guard stones in
  later phases. Anti-magic blows cut the sentence in one hit.
- **Cooldown:** 900 ticks; the first cast comes no earlier than 20 s after the phase starts.
- **Why it is fair:** every rule has an obvious way to obey it and an obvious reward for breaking the stones, and it never
  removes an ability for more than 12 s.

### Selection, not a script
All four go through the existing utility AI (`choose`, `score`, `ready`): Redact scores higher against clustered targets and
close fights, Fall against fliers, Halt against grouped targets, Overwrite once per phase window. The AI never chooses the
same word twice in a row (add a one-slot memory), and Redact and Halt never overlap, so a fight is a conversation, not a loop.

## 2. Phase transitions

The mod keeps its four phases (75 / 50 / 25 % marks). The design's three "acts" sit on top of them.

| Act | Health | Phase mapping | Theme |
|---|---|---|---|
| I: The Reader | 100-50% | phase 1 and phase 2 (adaptation at 75%) | measured, tests the party |
| II: The Writer | 50-25% | phase 3 | rewrites the arena |
| III: The Author | 25-0% | phase 4 | the true Word Soul |

### 2.1 What changes in each act (besides summoning the Arch Daemons)

| | Act I | Act II (50%) | Act III (25%) |
|---|---|---|---|
| AI think interval | 10 ticks | 8 | 6 |
| Telegraph (`CAST_TICKS`) | 15 | 12 | 10 (never below 8) |
| Word cooldowns | x1.0 | x0.85 | x0.7 |
| Move speed (base 0.28) | +0% | +8% | +15% |
| Multilayer Barrier layers | 3 | 4 | 5 |
| Thought Acceleration tokens | 4 | 6 | 8 |
| Daemon packs | Lesser swarm, small | Lesser swarm + Greater turrets | all tiers + a second Arch Daemon |
| Words unlocked | Halt, Shatter, Fall, Reveal, Seal, Reject, Redact | + Fear, Drain, Sleep, Swords, Petrify, Overwrite | + Heal (once), Trident, all of the above stronger |
| Special rule | none | the arena floods with underworld matter (existing) | only anti-magic or three elements at once can wound the body (existing) |
| Position | prefers mid range | hovers 2 blocks up while Lesser daemons screen him | descends and stalks (the final act is personal) |

### 2.2 The transition itself (3 seconds, fair to the party)
1. At the threshold he becomes **invulnerable** and speaks a line; the existing KOTO_SHATTER wave plays.
2. A glyph circle opens at the arena's edge; the Arch Daemon rises and the daemons arrive.
3. **The barrier fully refreshes** (the party loses its damage on it, but gets the next 3 s free of attacks).
4. Players inside the arena get a short, readable cue (sound, text) and the boss bar notches.
5. No attack lands during the transition; thresholds cannot be skipped by a burst (the transition caps the damage at the
   threshold, so a single hit cannot take him from 55% to 20%).

### 2.3 Other behaviour that scales
- **Targeting:** Act I targets the nearest threat, Act II prefers the strongest caster (`Nullification.maxEP`), Act III prefers
  the player who dealt the most damage to the barrier in the last 10 s.
- **Anti-cheese:** a 40-block leash; leaving it makes him speak "Banish" at the offender instead of chasing. He backs away from
  anti-magic users (existing) but never leaves the arena.
- **Pity:** if the party wipes twice in a row, the next attempt starts with the barrier at -1 layer. Never an invisible stat buff.

## 3. Loot

Boss-gated. Every player who damaged him gets **Heart of Words** (a crafting material) and one item rolled from the table
below, with no repeats until all three have been obtained (a pity counter, stored on the player). The existing Kotodama reward
flag and Zagred's five-leaf grimoire stay under their config.

### 3.1 Quill-blade "Last Word" (weapon)
- **Look:** a long black feather-bladed rapier, red text running down the fuller, built like the Demon-Slayer (3D model, glow,
  void). Follows CLAUDE.md rules 6 and 7.
- **Engravings (rule 6):** Barrier Piercing II, Severance I.
- **Ability:** each hit puts a "redacted" mark on the target (shown as a struck-out word). **Three marks within 5 s** strips
  one Tensura buff and one barrier layer from the target for 3 s. 20 s cooldown. It does nothing to bosses' phase rules.
- **Why:** it answers the Multilayer Barrier with a technique, not just damage.

### 3.2 Shroud of Margins (relic / chest piece)
- **Ability:** the wearer has a personal **three-layer barrier**; each layer absorbs one hit of up to 15% of max health, then
  re-forms after 20 s. It never stacks with armour enchantments' immunity, and it does not stop spiritual damage.
- **Why:** the party sees the boss's own mechanic and can use it, but it is a defensive trade (no extra damage).

### 3.3 Circlet of Quickened Thought (relic)
- **Ability:** -12% on all grimoire page cooldowns; sneak + use triggers **Overclock** for 6 s: cast and attack speed x1.5, and
  the first attack you dodge in that window refunds one second of cooldown. 90 s cooldown.
- **Why:** it is Thought Acceleration for the player, but it is rate-limited like the boss's version.

## 4. Implementation logic

Environment: NeoForge 1.21.1. `ZagredBossEntity.hurt` already runs the adaptation and phase-4 rules; the new rules go in
front of those.

### 4.1 Defensive profile (order of operations in `hurt`)
```
amount = incoming
1. class = classify(source)            // physical / projectile / magic / spiritual / abnormal / anti-magic / mythical
2. if class == ABNORMAL and effect-instance is poison/wither/rot/paralysis:  cancel the effect (nullified)
3. if class == SPIRITUAL (mental, soul, instant death):                       amount = 0 unless anti-magic
4. if class == PHYSICAL and not mythical(source):                             amount = 0   // see mythical() below
5. resistances:   holy and demonic x0.5,  natural (fire, water, wind, earth) x0.6
6. adaptation (phase 2+): adapted element x0.2                               // exists
7. barrier:       amount = barrier.absorb(source, amount)                      // 4.2
8. phase-4 rule:  x0.15 unless anti-magic or exposed                          // exists
9. thought acceleration may dodge the hit first                               // 4.3, runs before 1 in practice
10. super.hurt(source, amount)
```
`mythical(source)`: a demon sword / Tensura "mythical" tag / an attacker whose EP is above a config threshold /
a hit that carries a conceptual damage type (the Genesis Demon-Slayer's `conceptual_severance`). Physical damage that is not
mythical does 0 and **the party is told once**: "Your blade cannot touch Zagred. Magic can." (never a silent zero).

Regeneration ("Ultra-Speed Regeneration"): heal 0.4% of max health per second, but **never in the 3 s after a barrier layer
breaks** and never while the barrier is fully down (the "Reform" window). It fixes chip damage but cannot undo burst.

### 4.2 Multilayer Barrier
```java
final class BarrierStack {
    static final class Layer { float cap, hp; Grain grain; }          // grain: KINETIC, ARCANE or SPIRITUAL
    Layer[] layers; long nextRefresh; long reformUntil;

    // the layer order is outermost first; each layer is 6% of the boss's max health
    float absorb(DamageSource s, float amount, long now) {
        if (now < reformUntil) return amount * 1.5f;                      // vulnerable while he reforms
        Layer l = outermostAlive();
        if (l == null) { startReform(now); return amount; }
        float k = matches(l.grain, s) ? 1.0f : 0.4f;                      // a layer's grain takes full damage, other kinds 40%
        float absorbed = Math.min(l.hp, amount * k);
        l.hp -= absorbed;
        if (l.hp <= 0) onLayerBroken(l, now);                              // sound, particles, 3 s regeneration lockout
        return 0f;                                                         // all of the hit was spent on the layer
    }

    void tick(long now) {
        if (now < reformUntil) return;
        if (allBroken()) { startReform(now); return; }
        if (now >= nextRefresh) {                                         // auto-refresh: one layer per 8 s, 4 s with an Arch Daemon
            Layer weakest = mostDamagedLayer(); weakest.hp = Math.min(weakest.cap, weakest.hp + weakest.cap * 0.5f);
            nextRefresh = now + (archDaemonAlive() ? 80 : 160);
        }
    }

    void startReform(long now) {                                          // he writes the barrier again: 3 s window, no refresh, no regen
        reformUntil = now + 60;  boss.say("\"Again.\"");  boss.channel(60);
        resetAllLayers(now + 60);
    }
}
```
Rules for the player:
- The barrier is visible: layers are rings of text around him and the boss bar shows the count.
- A layer's grain is shown by its colour (white kinetic, violet arcane, red spiritual).
- Breaking **all** layers opens a 3 s damage window where he takes 1.5x; it then re-forms with one fewer layer for 20 s
  (so the party can choose to rush him or to sustain).
- Arch Daemons are **anchors**: while one lives the refresh is twice as fast and he takes 25% less. Killing an anchor breaks
  one layer outright.

### 4.3 Thought Acceleration (dodge and counter)
A model of reaction, not a hitbox hack: the boss spends **tokens**, regains them slowly, and every dodge costs one.
```java
final class Reflex {
    int tokens, max;  long nextRegen, noDodgeUntil;  Vec3 lastDodge;

    void tick(ServerLevel sl, long now) {
        if (now >= nextRegen && tokens < max) { tokens++; nextRegen = now + regenTicks(phase); }   // 40 / 32 / 24 / 20 ticks
        for (Threat th : scan(boss, 10)) {                                  // projectiles, fast melee wind-ups, area casts targeting the boss
            if (tokens == 0 || now < noDodgeUntil) continue;
            double eta = timeToImpact(th, boss);                           // using the projectile's motion and the attacker's reach
            if (eta > 0.15 * 20 && eta < 0.45 * 20 && th.hitsBoss()) {      // only reacts in a real window: too early or too late = no dodge
                dodge(th, now);
                tokens--; noDodgeUntil = now + 8;                           // never twice in 8 ticks
            }
        }
    }

    void dodge(Threat th, long now) {
        Vec3 away = perpendicular(th.direction()).scale(sideSign() * 3.0);  // a 3-block sidestep, with a visible afterimage
        boss.teleportTo(boss.position().add(away));                          // invulnerable for 4 ticks
        boss.setInvulnerableUntil(now + 4);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, ...);                      // the tell
        if (th.attacker() != null && th.attacker().distanceToSqr(boss) < 36) counter(th.attacker(), now);  // a counter-word
    }

    void counter(LivingEntity who, long now) {                                // a tiny Reverse: knockback and 3 spiritual damage, no stun
        if (rng.nextFloat() < 0.5f) KotodamaWords.spirit(boss, who, 3);
    }
}
```
How the party beats it (designed in, not an accident):
- **Overload:** two or more simultaneous threats exceed one dodge; area attacks and multi-hit spells drain tokens fast.
- **Bait:** a cheap projectile spends a token. Tokens are shown as pips on the boss bar so the party can read them.
- **Ignore:** anti-magic strikes, Time or Spatial attacks and hits from beyond 10 blocks are never scanned.
- **Floor:** if tokens hit zero, he is "out of thought" for 2 s: he does not dodge and is visibly slower.
- **Fairness:** he cannot dodge during a transition, a Redact channel or an Overwrite channel, and a dodge is never a teleport
  out of the arena.

### 4.4 Persistence and sync
- Barrier layers (hp, grain), tokens, rule stones and the current act are saved in `addAdditionalSaveData`.
- A sync payload (like `ZagredStatePayload`) carries layers, tokens and the current rule to the renderer and the boss bar.
- All new knobs go into gamerules or config (`nusmpZagredBoss` stays the master switch).

## 5. Supporting design

### 5.1 Grimoire Daemons (summon tiers)

| Tier | Role | Health | Behaviour | Counter | Spawn |
|---|---|---|---|---|---|
| Lesser (Ash / Earth) | swarmers: floating tattered robes, a single-leaf grimoire | 12 | fire a low-damage bolt every 3 s (4 damage; blindness 2 s or slowness 2 s), drift toward the target | any hit kills them; sweeping attacks and AoE | packs of 3-6, a new pack every 15 s, hard cap of 12 alive, fewer with fewer players |
| Greater (Dark / Fire) | artillery: horned, three-leaf grimoire emitting glowing text | 70 | stationary or slow; telegraphs a ground circle (radius 3, 1.5 s), then 12 damage plus burn | close the book by hitting the grimoire from behind; stay out of the circle | 2 in Act II, 3-4 in Act III |
| Arch (Spatial / Forbidden) | mini-boss commanders: multi-winged, large five-leaf grimoire | 300 | teleports, creates gravity wells, shields Zagred (anchor, see 4.2) | focus it; interrupt its channel by cutting the grimoire | one at 50%, a second at 25% |

Daemons use the mod's existing `SummonBrain` (Guard / Caster) and `TensuraCaster` to pick spells. They are mobs of the boss
and do not drop loot. Grimoire daemons' leaf counts match the mod's grimoire tiers (one, three and five leaves).

### 5.2 Presentation (maps onto the renderer)
- **Asymmetry:** the left wing is torn and shorter and flaps out of phase with the right. Build it in the model generator
  (`tools/gen_zagred_true_form.py`) as a second set of wing parts, not a shader trick.
- **Floating grimoire:** an independent animation loop: it bobs, snaps open and flips its pages quickly while a word is being cast.
- **Emissive:** eyes and runes glow during Overwrite and the transitions, using the existing aura shader.
- **Aura:** black and red text characters (the Kotodama glyph textures already exist) float off him instead of flakes.
- **Daemon models:** built like the Demon-Slayer: a pure-Java mesh with a generator (`gen_weapon_models.py` style).

### 5.3 Scaling
- Boss health scales with the number of players in the arena at the time he is summoned (+60% per extra player up to 5).
- Daemon packs scale with the party; the Arch Daemons do not (they are fixed fights).
- All hits are clamped by the same PvP / boss cap used elsewhere (`BalanceLaw`), so one overpowered player cannot trivialise it.

### 5.4 What is not decided yet
- Whether to restyle the model to the two new reference images (a second skin) or keep the 0.49 look.
- Whether to build the daemon models first or the barrier and reflex first.
- Exact numbers: all values above are first guesses to be tuned in play.
