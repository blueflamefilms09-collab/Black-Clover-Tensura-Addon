## Title

`[Boss] Riven Remake — adaptive Black Bulls bard boss, anime skill forge, custom geo animations, animated boss bar`

## Labels

`boss`, `black-clover`, `tensura`, `ai`, `client`, `animation`

## Agent prompt (paste this first)

```
You are implementing a new raid boss inside the existing NeoForge mod
"Multiverse" (modId `nusmp`, version 0.91.0+), Minecraft 1.21.1,
loader neoforge [21.1,). Do not start a new mod.

Hard dependencies already declared in META-INF/neoforge.mods.toml:
- tensura [2.0.1.0,)
- manascore_skill [4.0.0.2,)
- manascore_storage [4.0.0.2,)
Optional: bettercombat (data-only weapon_attributes).

Follow the existing Zagred boss as the pattern, do not invent a parallel framework:
- com.newuniverse.nusmp.entity.ZagredBossEntity
- com.newuniverse.nusmp.entity.ZagredAttacks / ZagredDefense
- com.newuniverse.nusmp.entity.ZagredStatePayload
- client ZagredRenderer / ZagredModel
- assets/nusmp/geo/entity + assets/nusmp/animations/entity
- custom geo pipeline: com.newuniverse.nusmp.client.geo.GeoAnim / GeoDraw / GeoModels
- Black Clover systems already in this jar: blackclover.*, book.* (grimoires,
  MagicType, SpellRuntime, Kingdom, GrimoireItem), aura.*, vfx PropPainters
- Tensura bridge already in this jar: book.TensuraShots, entity.TensuraCaster,
  manascore skill registration (see nusmp.skill.* lang keys)

Character: Riven Remake, alias Fictional Remake, hero title "The Black Bulls' Bard".
The name on the sheet is the in-game name. Do not shorten it to Riven or Remake.
Human, 19, 5'11", Black Bulls, Clover Kingdom, Bard/Warlock multiclass,
Support/DPS. Quirk "Fictional Remake": belief-powered reality rewrite.
He can manifest any weapon, construct, or skill whose fiction he can sell
to the fight — but only through the sandboxed skill registry below.

Deliver:
1. Server entity + brain that threat-scans each engaged player and counter-picks.
2. A data-driven Anime Skill Codex so he can cast skills inspired by any anime,
   including Tensura skills and this mod's Black Clover grimoire magic.
3. GeckoLib-style geo model, a full custom animation set, and an animated boss bar.
4. Compatibility with Tensura races/skills/magicules and this mod's Black Clover
   magic, anti-magic, and grimoire rules. He must be smart, not a DPS sponge.

Do not call the public internet on the server thread. Skill research is async,
admin-gated, schema-validated, and sandboxed. No arbitrary code from the web.
```

## Hero name lock (use these strings verbatim)

The sheet header says Marquis Remake. In game the hero name is **Riven Remake**. Marquis was a rank, not a name. Riven stays: split story, rewrite, Black Bull bard. Alias and title do not change.

| Use | Exact string |
| --- | --- |
| Hero name / nametag / boss-bar title | Riven Remake |
| Boss-bar subtitle, phase 1 | The Black Bulls' Bard |
| Alias, shown under the name on acquire | Fictional Remake |
| Phase 3 subtitle | Fictional Remake — Final Form |
| Sheet banner, arena title | A Dreamer · A Fighter · A Story Still Being Written |
| Footer mark on the boss bar | Fictional Remake |
| Lang entity | `entity.nusmp.riven_remake` = Riven Remake |
| Lang spawn egg | `item.nusmp.riven_remake_spawn_egg` = Riven Remake Spawn Egg |

Boss bar layout matches the sheet header: small crown, `RIVEN REMAKE` in the display face, then `THE BLACK BULLS' BARD` as the subtitle. Phase 2 swaps the subtitle to `FICTIONAL REMAKE`. Phase 3 swaps it to `FINAL FORM`.

## Canon (do not drift)

Source: the Riven Remake character sheet.

| Field | Value |
| --- | --- |
| Name | Riven Remake |
| Alias | Fictional Remake |
| Title | The Black Bulls' Bard |
| Age / birthday | 19 / June 13 |
| Height | 5'11" |
| Race | Human |
| Class | Bard (College of Lore) / Warlock (Hexblade), multiclass |
| Affiliation | Black Bulls |
| Kingdom | Clover Kingdom |
| Role | Support / DPS, chaotic good |
| Point buy | STR 10, DEX 16, CON 14, INT 14, WIS 12, CHA 18 |
| Personality | Loyal, funny, calm until the story peaks, protective, a little shy, big dreamer |
| Lines | "Chaos builds the best stories." / "I might not be the strongest, but I'll make sure my story hits harder than anyone expected." / "It's not just a game. It's my other life." |

Visual lock:

- Messy black hair, pale skin, eyes that burn violet-blue when casting.
- Black high-collar coat with silver filigree and the Black Bull skull on the chest.
- Floating five-leaf-styled grimoire orbiting his left side, pages rewriting themselves.
- Magic read is electric blue-violet lightning, not Zagred's word-magic gold/black.
- Castle / Clover Kingdom silhouette only as arena dressing, not part of the hitbox.

Quirk — Fictional Remake:

- Rewrites a small part of the fight through story, symbol, and belief.
- Stronger the more players are watching, low on health, or buffed by his songs.
- Can enhance himself, mark an ally, or spawn a temporary construct (weapon, shield, portal, clone).
- Cannot permanently rewrite a player who "doesn't believe" — mechanically, a player with anti-magic, a Tensura barrier, or a GrimoireGuard up resists the rewrite unless Riven spends a phase resource.
- Emotional high (phase 2 / last 30% HP) upgrades every skill one tier.

Power kit the sheet already names (all must exist as first-party skills, not only codex imports):

- Grimoire Manipulation (summon / flip / rewrite pages)
- Story Manifestation (temporary constructs: weapons, shields, portals)
- Soul Bond (link a player; damage or healing is shared on his terms)
- Shadow Step (short blink, leaves an afterimage that casts once)
- Bardic Songs (boost / heal / discord)
- Eldritch Blast, Hex, Bardic Inspiration, Jack of All Trades, Expertise
- Final Form `???` — phase 3, "the story where he wins"

## Goal

Riven is a raid boss and a sparring partner. He is not a reskin of Zagred.

He should feel like a lore bard who has read every anime in the Multiverse pack and will pick the shortest path to end the current target — then change the path when the target changes gear, race, or skill.

## Non-goals

- Do not remove or rename Zagred.
- Do not take a hard dependency on any Black Clover mod other than this jar. This mod *is* the Black Clover layer. If the user later loads an external Black Clover mod, integrate through a soft optional hook, never a required modId.
- Do not let the boss execute downloaded Java, JS, or commands.
- Do not HTTP-call from `Entity.tick` or any server thread.

## Architecture

Package root stays `com.newuniverse.nusmp`.

```
entity/riven/RivenBossEntity.java          # PathfinderMob, boss, synced phase
entity/riven/RivenBrain.java               # utility AI, target + plan
entity/riven/ThreatScan.java               # gear / skill / resistance profile
entity/riven/KillPlan.java                 # scored action list for one target
entity/riven/RivenAttacks.java             # cast dispatch, mirrors ZagredAttacks
entity/riven/RivenStatePayload.java        # phase, cast id, song, boss-bar style
entity/riven/StoryConstructEntity.java     # manifested weapon / shield / clone
skill/codex/AnimeSkill.java                # data record, not executable code
skill/codex/AnimeSkillCodex.java           # loaded JSON registry
skill/codex/SkillForge.java                # async, admin-only research job
skill/codex/SkillSandbox.java              # allow-list of effect primitives
client/riven/RivenRenderer.java            # geo + aura + grimoire, mirrors ZagredRenderer
client/riven/RivenBossBar.java             # animated overlay
assets/nusmp/geo/entity/riven_remake.geo.json
assets/nusmp/animations/entity/riven_remake.animation.json
assets/nusmp/textures/entity/riven_remake.png
assets/nusmp/skills/codex/*.json           # shipped anime skills
data/nusmp/boss/riven_remake.json          # phases, HP, music, drops
```

Register the entity in `NUEntities`. Add lang keys in `assets/nusmp/lang/en_us.json`. Add a spawn egg and a creative-tab entry next to the existing boss relics (`item/BossRelics`).

## Threat scan (the "fastest way to kill them" AI)

On acquire, and again every 2 seconds or on target swap, `ThreatScan` builds a profile. It only reads server-known state. It does not guess off-client.

Scan:

- Health, absorption, armor, armor toughness, magic resist if present, active potion effects.
- Held item + offhand (weapon attributes if Better Combat is loaded, else vanilla attack damage / attack speed).
- Armor slots, totem, shield, elytra.
- This mod: grimoire magic type, leaf count, anti-magic / NihilityZone, ModeArmor, SpiritBond, active aura.
- Tensura, via the already-required ManasCore APIs: race, rank, magicule pressure, intrinsic skills, barriers, regeneration, spiritual / material body flags. Fail soft if a skill id is unknown.
- Distance, vertical difference, line of sight, whether they are mounted or flying.
- Recent damage types that actually hurt them in this fight (remember the last 8 hits).

`KillPlan` scores every legal skill for this target. Score is higher when:

- The skill's damage type is not resisted (fire vs a salamander race scores low; anti-magic vs a mage scores high; severance vs a barrier scores high).
- The skill's range matches the current gap (Shadow Step + Eldritch Blast vs a kiter; Story Manifestation shield vs a melee rusher).
- Cast time fits the target's attack speed (don't channel a 3s song into a Tensura speedster unless a construct is body-blocking).
- Mana / story-charge cost is affordable.
- The skill is not on cooldown and was not just i-framed.

He commits to the top plan for at most 4 seconds, then rescan. If the target drinks a gapple, swaps to a Tensura barrier, or pops anti-magic, he aborts and replans immediately.

Personality filter, so he stays a bard and not a turret:

- Opens with a spoken line and Bardic Inspiration on himself, not a delete button.
- Below 70% he starts counter-picking for real.
- Below 30% (emotional high) plan scores for lethal skills are multiplied by 1.5 and Final Form unlocks.
- He banters. He does not spawn-camp. Leash to the arena.

Difficulty scales with player count: +20% HP and +1 simultaneous construct per extra player, capped at 4.

## Anime skill forge

He can use any weapon, ability, or skill that exists as a codex entry or as a native bridge. He cannot invent arbitrary bytecode mid-fight.

Shipped codex (`assets/nusmp/skills/codex/`) covers archetypes, each mapped onto the sandbox primitives already used by `SpellRuntime` and `TensuraShots`:

- Black Clover: flame, lightning, spatial, light, dark, anti-magic *resistance* (he is not Zagred and does not get full nullification), grimoire constructs, Black Bull charge.
- Tensura: magicule bolt, barrier pierce, steel thread, predator-style copy *of a codex skill only*, not of player NBT.
- One file per other franchise already named by the mod description (DanMachi, Fire Force, Jujutsu Kaisen, JoJo) plus a generic pool: sword rain, domain-style arena, healing aria, time-slow field, clone, grapple chain.

Each JSON skill:

```json
{
  "id": "nusmp:story_severance",
  "anime": "black_clover",
  "name": "Page of Severance",
  "tier": 2,
  "cast_ticks": 12,
  "range": 16,
  "primitives": ["projectile", "magic_damage", "brief_silence"],
  "counters": ["barrier", "caster", "healer"],
  "resisted_by": ["anti_magic", "magic_null"],
  "animation": "cast_grimoire",
  "line": "This page wasn't in your story."
}
```

Allowed primitives (sandbox): projectile, melee_arc, magic_damage, physical_damage, heal, shield, blink, pull, silence, slow, summon_construct, song_buff, song_debuff, copy_codex_skill. Unknown primitive = skill rejected at load.

Research command, admin only, config-gated, default off:

- `/nusmp riven research <anime> <ability name>`
- Runs off-thread. May call a configured HTTP endpoint that returns text. The server never loads code from the response.
- A small compiler turns the text into a candidate JSON skill using only the primitive allow-list, then writes it to `config/nusmp/codex-inbox/`.
- An op runs `/nusmp riven accept <id>` to move it into the live codex.
- If the endpoint is unset, the command tells the op to drop a JSON file in the inbox. That is the supported path for "any anime".
- Rate-limit, timeout, and a denylist for endpoints. No scan of players is ever sent off-server. Threat scans stay in memory.

Native bridges, preferred over codex copies when the id matches:

- Tensura / ManasCore skill cast through the same path `TensuraCaster` already uses.
- This mod's grimoire books through `SpellRuntime`, so flame / spatial / light / etc. stay consistent with players.
- Better Combat weapon attributes on manifested weapons if that mod is loaded.

## Animations

Use the existing geo pipeline (`client/geo`), same as other `assets/nusmp/animations/entity/*.animation.json`. One model, one animation file, clips below. All clips interrupt cleanly into `hit` and `death`.

| Clip | Length | Use |
| --- | --- | --- |
| idle | loop | weight shift, coat, floating grimoire bob |
| walk / strafe | loop | coat tails, grimoire trails |
| talk | 1.2s | phase barks, book gesture |
| cast_grimoire | 0.8s | page flip, violet lightning up the arm |
| cast_song | 1.6s | bardic, mouth open, notes as particles |
| eldritch_blast | 0.45s | two-finger, recoil |
| shadow_step | 0.35s | dissolve to afterimage |
| manifest_weapon | 0.6s | story construct assembles from pages |
| manifest_shield | 0.5s | bull-crest ward |
| soul_bond | 0.7s | thread from chest emblem to target |
| sword_combo_1/2/3 | 0.4s each | dex-based, not strength-based |
| hit | 0.3s | flinch, eyes flare |
| stagger | 0.8s | anti-magic or barrier pierce landed |
| phase2 | 1.4s | coat lightning, grimoire goes five-leaf |
| final_form | 2.2s | the `???` form, wings of pages, eyes full violet |
| death | 2.0s | book closes, lightning fades, bull emblem cracks |

Particles and sounds go through the existing aura / subtitle style (`subtitles.entity.nusmp.*`). Add subtitles for every bark.

Final form is a second texture + bone visibility on the same geo (page-wings, crown of the bull skull), not a second entity, so the boss bar never swaps uuid.

## Animated boss bar

Custom client overlay, not the vanilla boss bar alone. Vanilla `BossEvent` still exists so the locator and sound ducking work.

Bar behavior:

- Name plate: small crown + `RIVEN REMAKE` + subtitle that swaps with phase (`THE BLACK BULLS' BARD` → `FICTIONAL REMAKE` → `FINAL FORM`).
- A thin footer under the pips reads `Fictional Remake`, same as the sheet's bottom mark.
- Fill is a violet-to-blue gradient with a traveling lightning shimmer, masked by current HP.
- Black Bull skull sits on the left; it cracks at 70% and 30%.
- Under the bar, the current skill name types on during cast, then burns out.
- Phase 2 adds a second thin "story charge" pip row.
- Phase 3 pulses the whole bar on the beat of the cast animation.
- Color-blind safe: HP also shown as a number, phase as a roman numeral.
- Hides when the player is outside the arena leash.

Sync with `RivenStatePayload` (phase, hp fraction, cast id, charge). Do not poll the entity from the client every frame for logic; interpolate the payload.

## Compatibility

| System | Required behavior |
| --- | --- |
| Tensura | Reads race / skills / magicules. His magic damage respects Tensura barriers and resistances. His manifested weapon can be blocked by Tensura physical immunity. He will not soft-lock a spiritual body; if physical damage is invalid he switches to magic or severance. |
| ManasCore skill + storage | Register `nusmp:fictional_remake` as his intrinsic. Do not write into player skill storage except via Soul Bond's explicit, timed debuff. |
| This Black Clover layer | Uses `MagicType`, `SpellRuntime`, `GrimoireItem`, `Kingdom.CLOVER`, Black Bull emblem. Anti-magic and `NihilityZone` stagger him and force a replan. He can still Shadow Step out. |
| External Black Clover mod | Optional. If a known API is absent, skip. Never crash on missing mod. |
| Better Combat | Manifested swords ship a `data/nusmp/weapon_attributes` file. |
| Zagred | Can share an arena but they do not share AI state. Riven banters if Zagred is in range; no coupled boss logic in v1. |

## Fight script (acceptance)

1. Spawn in a marked arena. Boss bar appears with idle lightning.
2. He talks, summons the grimoire, casts Bardic Inspiration, then scans.
3. Against an unarmored mage he opens with Shadow Step + Eldritch Blast, then a silence page.
4. Against a Tensura physical tank he stops melee within 4 seconds and swaps to magicule / severance.
5. Against a Black Clover anti-magic user he staggers, replans, and uses constructs + physical combos.
6. At 70% he plays `phase2` and gains Story Manifestation (one construct).
7. At 30% he plays `final_form`, story charge fills, and the kill plan may chain two skills.
8. On death the grimoire closes, drops a Black Bull bard relic, and the bar cracks apart.
9. `/nusmp riven research` does nothing harmful with the endpoint unset, and never runs on the server thread when enabled.
10. Dedicated server + client: animations and bar match the payload. No classloader crash if Tensura skill ids drift; unknown ids are ignored.

## Config

`config/nusmp-riven.toml`

- `enableResearch` default false
- `researchEndpoint` default empty
- `baseHealth` default 600
- `perPlayerHealth` default 200
- `replanTicks` default 40
- `allowCodexInbox` default true

## Done when

- Builds against the current `nusmp` sources and the declared Tensura / ManasCore versions.
- One new boss, one geo, one animation file, one boss-bar overlay, one codex folder.
- Existing Zagred fight is unchanged.
- A player can lose to him for a readable reason (the plan that killed them is logged at debug).

## Visual references

Use the following as the visual moodboard for Riven Remake's arena reveal and final-form spectacle.

**Arena Atmosphere:**
- A deep forest arena with towering pines, glowing mossy ground, and a massive cosmic story-shape tearing open the sky above.
- The sky is a fiery orange-red and violet glow with floating islands, a pale moon, and immense glowing lightning arcs around a surreal, page-like reality construct.
- An enormous glowing sigil-like shape framed by forest canopy, floating island architecture, and a stylized storm of radiant magical energy.

**Visual Direction:**
- Dense enchanted forest with a towering, story-shaped magical portal above
- Floating island fragments and drifting page-like constructs
- Violet-blue lightning energy with orange-red cosmic glow
- A grand, cinematic "truth of the story" vibe rather than a floating abstract effect
- Riven stands beneath it in a grounded, human pose, not in a dramatic anime action pose, emphasizing the contrast between his calm presence and the impossible sky above

**Phase Progression:**
- **Phase 1:** Ominous story-ripple energy in the forest
- **Phase 2:** The storm intensifies, the sky fractures into floating story islands
- **Final Form:** The shape above becomes a massive glowing page-winged manifestation, with the forest backdrop still grounded and real

## Notes for whoever files this

The boss is smart because he counter-picks from a real scan of Tensura and Black Clover state, not because he downloads code. "Any anime" is the inbox plus the research command. That keeps a public server from executing whatever a webpage returns.

---

**Repository Info:**
- Language Composition: Java (72.4%), Python (27.5%), GLSL (0.1%)
- Repo ID: 1406645817
- Added: 2026-10-09
