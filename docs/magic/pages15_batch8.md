# Pages 15, batch 8: Seal, Transform and Body

Owner goal: every grimoire has 15 spells. This batch owns these books (and no other): `book_sealing`, `book_skin`, `book_tongue`, `book_switching`, `book_recombination`, `book_transmutation`, `book_butoh`, `book_modification`, `book_hair`, `book_nail`, `book_scale`, `book_spike`, `book_chain`, `book_barrier`.

How to build a page: `docs/magic/pages15_recipe.md`. Files this batch edits: `src/main/java/com/newuniverse/nusmp/book/pages15/Batch8.java`, `tools/lang/pages15_batch8.json`,
`src/test/resources/armour_policy/pages15_batch8_new_pages.json` and this file; plus (owner's rule for character grimoires) the four set files `character_spells/secre.json`, `henry.json`, `grey.json`,
`kiato.json`, and the test `Pages15Batch8Test`. Nothing in this batch could be played: everything below was checked by unit tests, scan tests and the type-check against the API stubs only.

**94 new pages in 14 books, all appended after every old page (no mode, unlock bit or mastery moves), behind the server setting `magic.pagesFifteen`.** Every basic grimoire of the batch now shows 15 pages
(`python3 -B tools/page_counts.py`: barrier, butoh, chain, hair, modification, nail, recombination, scale, sealing, skin, spike, switching, tongue, transmutation all at 15); the four character grimoires
(Secre, Henry, Grey, Kiato) show 15 as well.

## Pages

One table row per page, exactly this shape (the test `Pages15SourceScanTest` reads it): book, id, name, tier (starter / mid / zone / signature), origin (`original`, or
`canon: <source>` naming the English wiki page, the Japanese wiki page, or the chapter / episode; say which wiki each came from), then free columns (what it does, the shape and effect it reuses, worst case).

| Book | Id | Name | Tier | Origin | What it does | Reuses | Worst case |
|---|---|---|---|---|---|---|---|
| `book_sealing` | `seal_stamp` | Seal Magic: Sealing Stamp | starter | original | Seal Magic: Sealing Stamp - a small seal is thrown at the first foe it meets and bursts there: the foes in 1.5 blocks take a light hit, a mage among them finds the grimoire shut for two seconds (a boss for one), and all are slowed. Damage scales with your EP. | bolt + SEALING_FX | 4 targets, 3 packets |
| `book_sealing` | `sealing_needles` | Seal Magic: Sealing Needles | starter | original | Seal Magic: Sealing Needles - four needles of sealing light fly out and seek the nearest foes; each hit is small but cuts the foe's casting for one and a half seconds (Magic Jamming). Damage scales with your EP. | volley (homing) + SEALING_FX1 | 4 bolts, 5 packets |
| `book_sealing` | `seal_ward_mark` | Seal Magic: Ward Mark | starter | original | Seal Magic: Ward Mark - a ward is sealed onto your own skin: a seal laid on you by another mage breaks at once, and for eight seconds you have Absorption II and Resistance I. | custom self buff + SEALING_FX2 | self only, 2 packets |
| `book_sealing` | `seal_rune_lattice` | Seal Magic: Rune Lattice | mid | original | Seal Magic: Rune Lattice - a lattice of runes unfolds in a wide cone ahead of you: every foe in it takes a hit, is slowed (Slowness III, 2.5 s) and cannot cast for two seconds. Damage scales with your EP. | cone + SEALING_FX2 | 6 targets, 3 packets |
| `book_sealing` | `seal_gate_closing` | Seal Magic: Closing Gate | mid | canon: en-wiki: Sealing Magic (attribute page: opens and closes objects; the spell name is the mod's) | Seal Magic: Closing Gate - the seal that shuts what is open is thrown down a 14 block line: every foe on it is hit, shoved back and has its casting shut for 2.5 seconds. Damage scales with your EP. | line + SEALING_FX1 | 6 targets, 2 packets |
| `book_sealing` | `protective_seal` | Seal Magic: Protective Seal | mid | original | Seal Magic: Protective Seal - a ward seal on you and up to five allies within eight blocks: Absorption II and Resistance I for ten seconds. | custom team buff + SEALING_FX2 | 6 allies, 7 packets |
| `book_sealing` | `seal_sigil_burst` | Seal Magic: Sigil Burst | mid | original | Seal Magic: Sigil Burst - a sigil is laid where you look and bursts: foes within 4.5 blocks are hit, pushed away and cannot cast for two seconds. Damage scales with your EP. | nova (at aim) + SEALING_FX3 | 6 targets, 2 packets |
| `book_sealing` | `seal_sigil_field` | Seal Magic: Sigil Field | zone | original | Seal Magic: Sigil Field - a field of sigils lies for four seconds where you look; every half second the foes inside take a pulse, are slowed and cannot cast. Damage scales with your EP. | field (at aim) + SEALING_FX2 | 8 targets per pulse, 8 pulses, zone slot 1 |
| `book_sealing` | `seal_pillars` | Seal Magic: Pillars of Seal | zone | original | Seal Magic: Pillars of Seal - six pillars of sealing light rise one after another along a 12 block line ahead of you: every foe on it is hit, thrown up a little, slowed and cannot cast for two seconds. Damage scales with your EP. | custom pillars + SEALING_FX2 | 8 targets, 7 packets |
| `book_sealing` | `forbidden_seal_art` | Seal Magic: Forbidden Seal Art | signature | canon: ja-search: Secre's forbidden art against the devil (封緘魔法; no spell name is known, the page name is the mod's) | Seal Magic: Forbidden Seal Art - the art that sealed a devil, aimed at the foe in your crosshair (18 blocks): it and the foes within five blocks of it are struck hard, rooted (hard control: shares the twelve-second lock, shortened for players and bosses), their grimoires shut for up to eight seconds and their casting cut. Damage scales with your EP. | custom (control) + SEALING_FX2/3, SEAL_CHAINS | 10 targets, 9 packets |

## Research sources

Rule 10 order: English wiki first, Japanese wiki as the fallback. The fandom pages cannot be fetched from this sandbox (WebSearch returns only titles and a one-paragraph summary), so everything below is from
search summaries and from the repo's own copies of the English wiki (`.github/wiki/seal_magic.txt`, `barrier_magic_attribute.txt`, `butoh_magic.txt`, `chain_magic.txt`, the attribute pages with their `-- spell pages:` line).

* **Sealing Magic** (`book_sealing`): the repo copy of the wiki lists only Trinity Seal Magic (already a page). The attribute (Sealing Magic) "opens and closes objects" (wiki intro line), which is the canon behind Closing Gate;
  Secre's `character_spells/secre.json` `planned` entry names the forbidden art she used against the devil (ja search summaries, no spell name), which is the canon behind Forbidden Seal Art. Everything else is original.
* **Skin Magic**: no wiki page found by the English or Japanese searches. The only canon datum is a FandomWire article ("Black Clover Dark Disciples ranked", an English fan site, not the wiki) saying that Svenkin Gatard
  of Vanica's Dark Disciples can soften or harden his skin depending on the attack: that is the canon behind Hardened Hide and Soft Skin (the spell names are the mod's).
* **Tongue Magic, Switching Magic**: English and Japanese searches found no page, no user and no spell for either attribute. All their pages are original (in the style of the existing generic five pages).
* **Hair Magic** (English wiki attribute page, read through the search summary): hair "grows quickly to great lengths and stays prehensile", can be shaped into weapons, thin strands slip past other magic's defences, strands are
  woven into walls that block attacks. These four abilities are the canon behind Hair Reach, Hair Spear, Hair Needles and Hair Wall (the spell names are the mod's). Hair Forest is original.
* **Nail Magic**: English wiki profile of Hischer Ongg (Dark Disciple of Vanica): he lengthens and hardens his fingernails at will (search summary). Canon behind Elongated Nails and Hardened Nails; no nail spell names could be found.
  The Japanese search (爪魔法 ヒシャー) returned only series overview pages.
* **Scale Magic, Spike Magic, Modification Magic, Chain Magic, Barrier Magic, Butoh Magic, Recombination Magic, Transmutation Magic**: the repo wiki copies list only the spells that are already pages (Chain: Pitless Viper, Scorpio Chain,
  Heavy Chain, Magic Binding Iron Chain Formation; Barrier: Voodoo Regalia / Shangrila / Kingdom; Butoh: Sea God Slash / Descent); the searches found no further spell names. Grey's Transformation Magic (the wiki: she can
  transform other things, such as rocks) is the canon behind Living Statue; the squad's existing `character_spells` notes (repo) were read for Henry and Kiato. All other pages of these books are original, in the book's own naming.
* **Counts**: 2 pages are marked canon (the ability the page uses is from a source, the page name is the mod's), 8 are original. No page is a 1:1 reproduction of a named canon spell.
* **Could not be fetched at all**: every blackclover.fandom.com page (host closed to the sandbox: Seal Magic, Skin Magic, Tongue Magic, Switching Magic, Scale Magic, Spike Magic, Nail Magic, Hair Magic full text), the Japanese fandom / wikipedia
  pages of the same attributes (the searches returned only anime overview pages and shops), and Hischer Ongg's spell list.

## Character grimoires (set files, appended only)

| Character | Book | Set entries before | After | Added |
|---|---|---|---|---|
| Secre | `book_sealing` | 7 | 15 | seal_stamp, sealing_needles, ward_mark, gate_closing, rune_lattice, protective_seal, sigil_field, forbidden_seal_art (opens after Eternal Prison) |
| Henry | `book_recombination` | 7 | 15 | door_slam, plank_volley, stair_leap, bulwark, chimney_cannon, scaffold_span, plank_snare, hallway_maze |
| Grey | `book_transmutation` | 3 | 15 | sand_shower, copper_bolts, flowering (canon: Grey and Gauche's flowers), rubber_spring, lead_weights, quagmire, brass_whips, alchemic_blades, transmuted_sentinel, salt_flats, alchemist_armour, living_statue (opens after Magic Convert) |
| Kiato | `book_butoh` | 6 | 15 | butoh_hand_sway, butoh_heel_stamp, butoh_flowing_blade, butoh_rhythm_wave, butoh_ebb_step, butoh_tidal_veil, butoh_undertow, butoh_metronome_field, butoh_seabed_hymn |

Entries are `exclusive: false` (the pages stay in the basic book), `source: design` unless a wiki is named, and use the `mastery` / `page_open` rules of the file (no existing entry was changed; only the `notes` text was updated).

## Worst case of the batch (rule 11)

* Every page is ONE server cast. Targets: the tier's cap (4 starter, 6 mid, 8 zone, 10 signature), enforced by `Ctx.foes` / `foesAlong` and by the kit's standard casts. Hits: the tier's damage (7 / 12 / 16 / 26 raw, a zone pulse 2 / 3 / 5 / 7) through
  `GrimoireBook.hurt`, so the balance law scales them like the book's other pages; a custom hit is clamped to 1.5 standard hits and the multi-hit pages (Lashing Barrage, Great Swallow) stay inside that.
* Standing effects: fields use the kit's `field` or `Ctx.zone`: at most 100 ticks, steps of at least 4 ticks (the pages use 8 to 10), at most 2 kit zones per player. Delayed hits (`later`) are at most 100 ticks away and do nothing for a caster who died or left.
  Hair Wall runs a 60-tick zone of 15 steps that deletes at most 4 enemy bolts per step.
* Packets: at most 10 effect packets per cast (the `Fx` helper stops at 9; a zone sends one per pulse until the cap, a field of the kit sends one per pulse as every other kit field does), sent to nearby players by `VfxSpawn`/`GrimoireBook.vfx`, which go through the perf budgets.
* Entities: none are spawned, no block is placed, no chunk is loaded, no world scan beyond the spell's own box (Aggro Switch and the vanish helper read mobs in a 10 / 16 block box once per cast; the team buffs read allies in an 8 block box, at most 6 are touched).
* **Recombination Magic**: `ElementBook.vfx` spawns a `MagicPropEntity` for every effect of this book (up to 100 ticks of life each), so the kit's standard casts (which call it once per effect, a field once per pulse) would spawn
  up to a dozen props per cast. The nine Recombination pages therefore never use a kit cast: they send their effects straight through `VfxSpawn` (`Fx`, no entity at all); a test (`Pages15Batch8Test`) keeps it that way.
* Client draw cost: only shapes that already exist and are already budgeted (`*_FX1..3`, `WIKI_MAGIC_*`, `SEAL_CHAINS`, `BUTOH_AFTERIMAGE`, `BUTOH_DANCE_RIPPLE`); no new asset, no new effect, no new entity type, no new packet.
* Control: Snatch, Wrapping Tongue, Skin Wrap, Lead Weights, Nerve Edit, Spike Snare, Great Swallow, Forbidden Seal Art and Living Statue are hard control and go through `GrimoireBook.control` or the kit's `bind` (the shared 12 second lock) and `BalanceLaw.controlTicks`
  (3 s players, 5 s mobs, 1 s bosses, halved on repeat). Seals shut a mage's grimoire for at most 8 seconds (a boss for 1 s). Pulling and swapping skip bosses where it matters (Snatch, Great Swallow, Aggro Switch).
* Movement pages (Stair Leap, Rubber Spring, Spike Vault, Barrier Stairs, Tongue Swing, Nail Hook) set the caster's own motion once (velocity at most 2.2 a tick for one tick, no repeating task) and give Slow Falling.
* Position swaps (Place Swap, Foe Swap, Grand Reversal, Great Swallow) test the destination with `noCollision` first and skip the move if it would be inside a wall.

## New and replaced (rule 5)

All 94 pages are NEW; nothing is replaced and nothing is removed. In the four set files the old entries are untouched (entries appended, `notes` text updated); some pages that existed but were hidden from those characters' grimoires
(Bulwark, Plank Snare, Quagmire, Alchemic Blades, Transmuted Sentinel, Alchemist's Armour, Flowering and the seven older Butoh pages named above) are now also listed in their sets, which is an addition to the set, not a change of the page.

## Honest caveats

* Not seen in a game. Nothing here was run: the pure tests, the scan tests and the page counter ran; the type-check against the API stubs reports only the stub artefact "MobEffectInstance is abstract", which the existing books show too.
* The balance of the numbers (cooldowns inside the tier bounds, damage from the tier) follows the recipe; the feel of Tongue Swing, Nail Hook and the leaps (a velocity set once) is untested.
* Most pages are original, because the sources for these attributes are thin (see Research sources). The owner's decision on class (c) "mod's own pages" stands in `docs/magic/page_counts.md` section 8.
* Spell names marked canon are canon only in the ability they use; none of them is a name a source gives for a spell.
