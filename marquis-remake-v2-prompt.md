# GitHub issue / agent prompt — Marquis Remake, remade boss

New boss issue for Multiverse `nusmp` 0.97.0. Replaces the old Riven visual. Same NeoForge 1.21.1 stack: tensura [2.0.1.0,), manascore_skill, manascore_storage. Follow ZagredBossEntity, SpellRuntime, WeaponEngravings, and `data/nusmp/dimension/dream.json`.

---

## Title

`[Boss] Marquis Remake — Minecraft skin body, sheet coat, MHA quirks, random grimoire, Evil Eye, god avatars`

## Labels

`boss`, `model`, `mha`, `black-clover`, `tensura`, `vfx`

## Agent prompt

```
Build Marquis Remake as a raid boss in nusmp 0.97.0. This is Minecraft.
Every feat below is an arena mechanic with a tell, a hitbox, and a timer.
Nothing deletes the world, the dimension folder, player inventories, or the server.

Hero name on the sheet is the in-game name: Marquis Remake.
Title: The Black Bulls' Bard. Alias: Fictional Remake.
Banner: A Dreamer · A Fighter · A Story Still Being Written.

Two image locks:
1. Character sheet. Black high-collar coat, silver filigree, Black Bull skull on the chest,
   messy black-brown hair, violet-blue eyes, floating grimoire, blue-violet lightning.
2. Minecraft skin. Player proportions. Brown hair, blue sweatshirt with a white crest,
   gray pants, white shoes. This is the body players see in first person range.
   The coat and grimoire are geo layers that grow in, not a replacement mob that
   stops reading as Minecraft.

300,000,000 EP via the Tensura API. Not plunderable. Not a second damage multiplier.
Hit table below is the damage. Appraisal still reports 300 million EP.
```

## Model

Player-scale humanoid, 1.8 blocks, Steve hitbox. Texture is the supplied skin. Geo layers, same pipeline as `assets/nusmp/geo/entity`:

- Phase 1: skin only, grimoire closed on the hip, eyes dim.
- Phase 2: black coat and bull emblem fade on over the sweatshirt. Eyes go violet. Grimoire opens and orbits.
- Phase 3: lightning on the coat, page shards, gold rift ring.
- Phase 4: page-wings and a cracked bull crown on the same rig. Do not swap entity ids.

Animations stay on the player skeleton: idle, walk, cast, gearshift dash, compress, evil-eye, summon, hit, phase change, death. 0.4–1.4s. They must interrupt into hit.

## How the abilities work

All boss_bound. Predator, Usurper, and Copy Magic get nothing.

### New Order

Touch or a 6-block gaze, 1.0s tell, eyes and hand glow white-violet. One order active. He barks the rule. Orders last 8s and only inside the arena.

Legal orders, picked from what ThreatScan saw:

- "Your magic is slow." Target's next two nusmp or Tensura casts take +50% warmup.
- "That weapon is heavy." Held weapon attack speed halved for 6s. Item is not deleted.
- "The ground is stone." Gold ring becomes solid for 4s so Island Fall has a floor tell.
- "I am faster." His next Gearshift is instant.
- He cannot order "you are dead", "your items are gone", or "the world unloads".

A second order replaces the first. Anti-magic fizzles the order.

### Fa Jin

Melee and dashes store kinetic charge, shown as white rings on the sweatshirt crest. At 5 stacks the next punch releases them in a 4-block cone. Normal damage plus a knockback. Charge clears on release or after 10s. He cannot hold charge through a stagger.

### Gearshift

Touched target, including himself, a bolt, or a summoned avatar, gets a speed rank for 4s.

- Low: 40% speed. Used on a player who is kiting.
- Top: 200% speed. Used on himself or a Zeus punch.
- He cannot set a player to 0. Minimum is Low.

Tell is a red-to-blue trail on the touched entity. Boss bar prints the rank.

### Compress

He marble-compresses one block, one construct, or one player who failed a tell. The marble is a visible purple-black sphere in his offhand for 3s, then it restores at his feet or shatters for damage.

- Player marble: they are removed from the fight for 3s, no damage while marbled, then released. Not an inventory wipe. Logout releases them at the forest anchor.
- Block marble: arena block only. Restored at phase end.
- He cannot compress the boss bar, the rift instance, or a player outside the leash.

### Black Clover, random from this mod

On each combat-type roll he draws one grimoire book already registered in nusmp (flame, spatial, light, dark, shadow, dream, legion, key, mirror, iron, time, and the rest of `nusmp.skill.grimoire_*`). Cast goes through SpellRuntime. The drawn book is named on the bar. Anti-magic and NihilityZone stagger him and burn the drawn page. He redraws next roll. He does not get Anti-Magic Lord.

### Evil Eye

God-scale, arena-scale. Both eyes open, violet, 0.8s tell, then a cone.

The marked target gains Existence Reduction for 6s:

- Ultraspeed Regeneration, totems, absorption regen, natural regen, mod healing, and avatar recovery do nothing while marked.
- Current HP does not drop by itself. The eye does not delete the entity.
- A hit taken while marked cannot be healed until the mark ends.
- Works on players, Tensura high-EP bodies, and his own god avatars. A summoned god cannot regen while he looks at it. He will not look at his own avatar unless it is broken and he is dismissing it.
- Boss bar shows the eye. Subtitle: "This page ends."

Phase 4 mark lasts 8s. Still not a permanent world erase.

## God summons

One avatar at a time. Summon is a 1.4s cast. The avatar is a Minecraft entity in the arena, 12s life, then it breaks into pages. It is not omnipotent. It cannot load chunks outside the rift, place bedrock, or run commands. If he is staggered, the avatar cracks early.

He summons in this order as phases allow. Phase 2 unlocks 10–7. Phase 3 unlocks 6–4. Phase 4 unlocks 3–1. He picks the one that counters the current target, not all of them.

| Avatar | Minecraft translation of the feat |
| --- | --- |
| Zeus | One punch. Gearshift Top. Tell is a 4-tick afterimage, then a 4-block hit. Not a 10⁻²⁰ second frame. Survives one Island Fall. |
| Beerus | Hakai sphere, 3-block radius, erases arena blocks and constructs only. Blocks restore when the avatar ends. Players take signature damage, they are not erased. |
| Truth | Equivalent Exchange. Damage the avatar takes is mirrored to the last player who hit Marquis, capped at the phase signature. |
| Ultimate Madoka | One arrow. Removes one negative effect on him or one positive effect on the marked player. Does not rewrite the server. |
| Arceus | Judgment. Next grimoire draw is forced to the element the target is weakest to, from ThreatScan. |
| Grand Zeno | Erase. Deletes one construct, or marble-compresses a player who is already Evil Eye marked, then releases them at the anchor with the mark still on. No timeline delete. |
| Anti-Spiral | Three galaxy-shuriken projectiles. Wide, slow tell, dimension-slash shader. |
| Lord of Nightmares | Sea of Chaos pool under the gold ring for 4s. Standing in it pauses regen even without Evil Eye. |
| Kami Tenchi | Passive while alive. Arena gravity stabilizes. Players cannot elytra. His i-frame window will not start. Presence is a dim gold outline, no attack. |
| The Creator | Phase 4 only, once. One law: "healing does not exist here" for 8s, whole rift. Stacks with Evil Eye. Then the avatar closes. This is the ceiling. It does not unmake the world. |

## Phases

HP 1200, +400 per extra player, cap 4. EP stays 300,000,000.

| Phase | When | What changes |
| --- | --- | --- |
| 1 Bard | 0–20s | Skin, Fa Jin, one New Order, random grimoire page. Signature 22. |
| 2 Hack | 20s or 75% | Coat on. Gearshift, Compress, Evil Eye, summons 10–7. Physical null 4s, then magic null 4s. Signature 42. |
| 3 Rift | 60s | Pull leash into `nusmp:story_rift`. Summons 6–4. Spatial null 4s on entry. Signature 62. |
| 4 Final Form | 25% | Wings, crown, summons 3–1, The Creator once. Rotating null 4s. Signature 85. |

I-frames, 1.5s, phase change only, plus 1.5s on The Creator's entrance. Anti-magic cancels a window that has not started. During a null the other damage type still hurts. Repeating the nulled type deals 0.

Combat type rolls every 12s in phase 2, every 8s in phase 3: Caster, Hexblade, Legion, Rift, Song, Hack. Bar names it.

## VFX

Layer shaders already in the jar: dimension_slash, demon_void, zagred_aura recolored violet, fx. Particles: arc_portal, arc_bolt, arc_rune_band, barrier_ring, barrier_shard.

- New Order: white text rune at the target's feet, not a chat command.
- Fa Jin: white rings on the crest, cone flash on release.
- Gearshift: red-to-blue motion trail.
- Compress: purple marble with the skin's face shrunk on it for a player.
- Evil Eye: full violet eyes, cone using anti_magic_slash tinted violet.
- Avatars: gold page-body, the feat as a weapon, not a second player model. Beerus is a sphere. Zeno is a small mark. The Creator is a closing book.

Fallback to arc_portal if a shader fails. Do not crash the join.

## Drops

`data/nusmp/loot_table/entities/marquis_remake.json` on a real kill only.

- Guaranteed: Black Bull bard relic, story page trophy, skin-cape cosmetic that copies the sweatshirt crest.
- One of: Page Edge engraving, Bull Brand engraving, single-use rift key.
- 8%: Final Form crown, vanity.
- No Tensura unique, no New Order item, no Evil Eye item.

## Done when

- In-world he reads as the Minecraft skin, and the coat grows on at phase 2.
- New Order, Fa Jin, Gearshift, and Compress match the rules above.
- A random nusmp grimoire book is drawn each combat roll and named on the bar.
- Evil Eye stops regen, including on a god avatar, and does not delete entities.
- Only one avatar exists, it dies at 12s, and The Creator only fires once.
- 300,000,000 EP shows on appraisal and is not plunderable.
- Zagred and the dream dimension are unchanged.

---

## Visual references

Use the following as the visual references for the revised Marquis Remake sheet and Minecraft-skin-body version.

- Image 3: character sheet poster / hero display featuring a black high-collar coat, violet-blue lightning effects, floating grimoire, and a dramatic crest-and-bull title layout.
- Image 4: Minecraft skin mockup showing a player-scale humanoid with a brown-haired silhouette, blue sweatshirt with a white crest, gray pants, and white shoes — this is the body seen at normal in-game scale before the coat and orbiting grimoire fully grow in.

Use these as the primary references for:
- the character sheet look and title treatment
- the Phase 2 coat-fade effect over the sweatshirt
- the blue-violet magical lighting and dark fantasy palette
- the contrast between the sheet's high-contrast fantasy look and the grounded Minecraft skin body below it

This should read as:
- sheet-art fantasy identity in the upper half
- player-proportion body in the lower half
- the coat and grimoire are layered geo, not a separate entity or replacement body
- the boss remains legible as Minecraft while still feeling like a magical bard-god hybrid

