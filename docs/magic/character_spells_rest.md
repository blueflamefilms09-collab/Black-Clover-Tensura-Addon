# Character spell sets, the rest of the cast: captains, Spade, Heart, Diamond and the Seabed Temple

Owner request (same as the pilots, docs/magic/character_spells.md): a character's grimoire holds the spells he used in the anime, in the order he got them, and a character's casts look far richer than a basic grimoire's. This document is the second content phase: thirteen more characters on the engine of the pilots. Read the pilot document first for the schema and the acquisition rules; nothing in the engine changed, only data, a few pages, a VFX layer and four small hooks (section 2).

Nothing here was seen in a game. What was checked: the pure rules, the loader and the thirteen shipped files by JUnit (`CharacterSetRestTest`, `CharacterSetRestSourceScanTest`, the pilots' `charset.*Test`, `ArmourPolicyTest`), the sources by scan tests, the type check against the API stubs (only the known "abstract; cannot be instantiated" artefacts remain, a negative control was run). What the CI server smoke test will check in the real game: self-test `magic.character_spells_rest`.

The server switch of the pilots, `magic.characterSpellSets` (default `true`), turns everything here back too: the whole book for every grimoire, pages opening by mastery order, and no flourish layers. The three pages that other books reached by name, the old lock lines and every old page stay.

## 1. Who is covered

| Character | Result | Book | Pages in the set | New pages |
|---|---|---|---|---|
| Charlotte Roselei | DONE | `book_briar` (CanonBook magic moved from PLANT, see 7.1) | 8 | Blue Rose Paradise, Scarlet-Braided Beautiful Battle Spear |
| Jack the Ripper | DONE | `book_slash` | 5 | none |
| Rill Boismortier | DONE | `book_painting` | 13 | Souterrain Groundswell, Sirene's Game, Four-Headed Lindworm, Phantom Dragon Vouivre's Shout, Souterrain Giant, Twilight of Valhalla |
| Dorothy Unsworth | DONE | `book_dream` | 4 | none |
| Zenon Zogratis | DONE | `book_bone` | 5 | none |
| Vanica Zogratis | DONE | `book_blood` | 4 | Red Beast, Herd of Red Beasts |
| Dante Zogratis | DONE | `book_gravity` | 8 | none |
| Lucius Zogratis | DONE | `book_soul` (CanonBook magic moved from DARK, see 7.1) | 7 | none |
| Rades Spirito | DONE | `book_soul_corpse` | 15 | none |
| Gadjah | DONE | `book_lightning` | 4 | Tenjiolia, Apocalypra Astrauza |
| Mars | DONE | `book_mineral` (CanonBook magic moved from EARTH, see 7.1) | 4 | Laevateinn |
| Kahono | DONE | `book_song` | 5 | Heal Lullaby, Mother Lullaby |
| Kiato | DONE | `book_butoh` | 6 | none |
| Floga | SKIPPED | | | No spell of his has a name in any source that could be reached (English wiki snapshot and search, Japanese search): the wiki's only fact is that he is the Heart Kingdom's Fire spirit guardian who borrows natural mana and trained Leopold. Inventing a set would break "never invent spells"; his grimoire stays the whole Fire book as today. |
| Lolopechka | SKIPPED | | | The sources say she has no attack spell (until Megicula) and name no Water spell of hers (a water trap on Vanica, waterways, healing, Undine). Her grimoire stays the whole Water book as today. |
| Zagred | SKIPPED | | | The only named spell is Noad Nephesh (a forbidden reincarnation spell, not a page); Kotodama Magic is spoken commands, and the book's words are this mod's design (docs/kotodama_spec.md). Nothing canon-sourced to order, so no set. |

Every other CanonBook id belongs to the other two builders (Black Bulls: Vanessa, Charmy, Luck, Magna, Gauche, Gordon, Zora, Grey, Henry, Finral, Nacht, Secre, Letoile; Golden Dawn, Crimson Lion, Silver Eagle, Wizard King: Mimosa, William, Klaus, Langris, Lemiel, Julius, Mereoleona, Leopold, Kirsch, Karna, Nozel, Kaiser) or to the pilots (Yuno, Noelle, Fuegoleon, Asta, Yami). No id of `CanonBook` is left uncovered by the three builders and the pilots. All ids of the brief exist.

## 2. What was edited and added

| Part | File |
|---|---|
| The 13 data files | `src/main/resources/data/nusmp/character_spells/{charlotte,jack,rill,dorothy,zenon,vanica,dante,lucius,rades,gadjah,mars,kahono,kiato}.json` |
| The 15 new pages and their casts | `book/CharacterPagesRest.java`, `book/CharacterArtsRest.java` (new classes, the pilots' files are untouched) |
| The flourish (section 5) | `charset/CharacterFlourishRest.java` (game side), `charset/FlourishTable.java` (pure table) |
| The CI self-test | `charset/CharacterRestSelfTests.java`, prefix `magic.character_spells_rest` |
| Lang | `tools/lang/character_spells_rest.json` (the page texts; `en_us.json` is merged by the lead) |
| Tests | `CharacterSetRestTest` (data, 11 cases), `CharacterSetRestSourceScanTest` (sources, 8 cases) |

One-line (append-only) edits of existing files, each marked with a comment:

* `book/GrimoireBook.java`: `pages()` and `familyCount()` now ask a small private `familyAll()` instead of `familyPages()`; `familyAll()` is the book's family list followed by `CharacterPagesRest.append(bookPath, list)` (the same list when the book has nothing added). This one hook replaces the per-book `withCharacters` wrapper the pilots used, so none of the 13 book classes needed an edit. One more line after `CharacterSets.onCast(...)`: `CharacterFlourishRest.after(player, instance, p)`.
* `book/CharacterPages.java`: `isAdded` also asks `CharacterPagesRest.isAdded` (an added page is locked to its character). `addedTo` is NOT changed on purpose (the pilots' self-test reads it).
* `charset/CharacterSetRegistry.java`: the 13 ids appended to `SHIPPED`.
* `charset/CharacterSetBoot.java`: `CharacterRestSelfTests.register()`.
* `grimoire/CanonBook.java`: three magic strings (7.1).
* `data/nusmp/armour_policy.json`: `new_pages` added to BRIAR, PAINTING, BLOOD, LIGHTNING, MINERAL, SONG (the guard `ArmourPolicyTest` asks for every appended page).
* `src/test/.../CharacterSetSourceScanTest.java`: the check "a source file is known for the book" became "skip the books of the later builders" (their own scan test covers them). The pilots' assertions are unchanged.

## 3. Research record (rule 10) and what could not be fetched

English wiki first, Japanese as the fallback. Every fandom host and the Japanese wiki hosts fail from the build machine, so the facts come from (a) the repo's own snapshot of the English wiki pages (`.github/wiki`: the attribute pages with their spell lists, and the intro and description of each spell page, for Briar, Slash, Painting, Bone, Blood, Gravity, Body, Butoh, Kotodama and about eighty spell pages), and (b) the web search summaries of the live pages: the English pages of Charlotte, Jack, Rill, Dorothy, Vanica, Zenon, Dante, Lucius, Floga, Gadjah, Lolopechka, Mars, Kahono, Kiato, Rades, Zagred, Wild Bursting Flame, Salamander's Breath; the Japanese search for Floga, Lolopechka, Gadjah (ja.wikipedia, pixiv encyclopedia, namu.wiki list) and Mars. Source ids in the files: `en_wiki` (snapshot or search of the English wiki), `ja_search` (the Japanese fallback), `design` (this mod; the sources give no timing).

Could not be fetched at all: blackclover.fandom.com pages themselves (every character page), ja.wikipedia pages, the pixiv encyclopedia pages, namu.wiki; so no chapter or episode number was read for any spell. The only numbers found: Dainsleif is chapter 310 (a spell dictionary, in the Bone note of the Light/Bone document), Queen of Briars' unlocking is around chapter 277 (a CBR headline). The `origin` of most entries (anime or manga) is therefore best knowledge, not read from a page; entries from the late arcs are marked `manga`, Sea God's Descent is `anime_only` (the wiki says so).

## 4. The 15 pages this release added, and how they look and cost

Appended after the older pages of their book (ids new, nothing moved), each locked to its character, tiers as every page (cost, cooldown, rank gate by tier). The casts are server-decided; the picture is a LAYER of existing `VfxShape`s in the character's colours (no new shape, texture or sound). "(inferred)" = the sources gave the name only.

| Page | Character | Tier / base cooldown | What it does | VFX layers |
|---|---|---|---|---|
| `blue_rose_paradise` | Charlotte | zone / 30 s | 5 s at the aim point (22 blocks), radius 5: 3 damage a second, slow II, weakness, a wither on a foe is cleared (curse drunk by the blue roses) | two concentric ELF_CIRCLE (blue, deep blue) + BRIAR_FX2 + BRIAR_FX3 pulses |
| `scarlet_braided_spear` (inferred) | Charlotte | mid / 13 s | 16-block line, 13 damage, poison 3 s, light push; up to 8 foes | BRIAR_FX1 + THREAD_LINE (red) + BRIAR_FX3 at the tip |
| `souterrain_groundswell` | Rill | mid / 12 s | 13-block lane, 9 damage, slow II, blind 1.5 s, push | PAINT_STROKE + PAINT_TRAIL + EARTH_RISE (earth brown) |
| `sirenes_game` | Rill | mid / 12 s | 13 blocks, 9 damage, fire put out, slow, push | PAINT_STROKE + PAINT_BEAST + WATER_SPLASH + WATER_RING |
| `four_headed_lindworm` | Rill | zone / 25 s | 14-block fan, 8 damage to up to 8 foes, one of burn, slow, lift, weakness each | PAINT_BEAST + four PAINT_TRAIL (fire, ice, wind, earth colours) |
| `vouivres_shout` | Rill | zone / 25 s | blast bolt (44 blocks) bursting in 3.5 blocks: 15 damage, thrown back; slow fall 5 s for the painter | PAINT_EMERGE + PAINT_BEAST + MAGIC_CIRCLE_EXPLOSION + PAINT_TRAIL |
| `souterrain_giant` | Rill | zone / 30 s | at the aim point, three slams over 3 s, radius 4.5, 7 damage each | PAINT_EMERGE + PAINT_BEAST + EARTH_RISE + 3 x EARTH_FISSURE |
| `twilight_of_valhalla` | Rill | signature / 75 s | after 3 s, for 8 s: you and allies within 6 blocks (up to 8) gain Regeneration II, Resistance II, Absorption III | PAINT_EMERGE + PAINT_BEAST + DIVINE_HALO + MAGIC_CIRCLE_EXPLOSION at the start of the effect |
| `red_beast` | Vanica | mid / 25 s | costs 3 health; 8 s attached to you: 3 damage every half second to up to 6 foes within 5 blocks, 0.4 health back per hit | BLOOD_SIGIL + following BLOOD_FX1 and BLOOD_ARCS + a BLOOD_CRESCENT every 2 s |
| `herd_of_red_beasts` | Vanica | zone / 35 s | costs 4 health; 5 s on 7 blocks at the aim point: 3.5 damage every half second to up to 8 foes, slow | BLOOD_SIGIL + BLOOD_DOWNPOUR + BLOOD_FX1 + a BLOOD_LANCE every 1.5 s |
| `tenjiolia` | Gadjah | mid / 10 s | 30-block line from the palm, bent toward what you look at, 14 damage, slow | LIGHTNING_SPEAR + FX_LIGHTNING_ARC + LIGHTNINGFX_CAST + LIGHTNINGFX_BURST |
| `apocalypra_astrauza` | Gadjah | signature / 90 s | costs 4 hearts of life force (refused if it would leave less than 1 heart); 0.6 s of three mana arrays, then a 40-block beam, 34 damage to up to 8 foes, thrown back | three ELF_CIRCLE arrays + LIGHTNING_GOD + FX_LIGHTNING_ARC + LIGHTNINGFX_BURST |
| `laevateinn` (inferred) | Mars | signature / 45 s | 7-block cone, 22 damage to up to 8 foes, weakness, thrown back | EARTHFAM_CAST + CRYSTAL_FX1 + EARTHFAM_BURST |
| `heal_lullaby` | Kahono | mid / 20 s | you and allies within 8 blocks heal 25 percent (the usual healing decay) | following SONG_NOTES + SONG_RING + ELF_CIRCLE |
| `mother_lullaby` | Kahono | mid / 25 s | one target within 16 blocks: held, blind, weak for about 4 s; a hard control that shares the 12 s lock | following SONG_NOTES + SONG_RING + DREAM_TRANSITION |

The existing pages the sets reuse under a canon name are not renamed (rule 2): Mars's Nemean Armor is the existing Mineral Guard and his Harpe the existing Lance Volley; Kahono's Destruction Beat is the Song book's mode 0 (Staccato Bolt) and her Rhythm Cushion is Aria Armour. The set file says so in the note of the entry.

## 5. The flourish: the character's colours around every cast

A basic grimoire casts what its page draws. A grimoire bound to one of the thirteen characters adds two more layers of existing shapes in his palette around every cast of any page (the first at the feet or following, the second at the aim point or along the line): Charlotte BRIAR_FX2 blue + BRIAR_FX3 red, Jack SLASH_BLADES + SLASH_WAVE green, Rill PAINT_PALETTE gold + PAINT_SPLAT blue, Dorothy DREAM_MANIFEST + DREAM_TRANSITION pink, Zenon BONE_FX1 + SPACE_CRACK violet, Vanica BLOOD_SIGIL + BLOOD_ARCS, Dante GRAVITY_IMPACT + GRAVITY_AURA, Lucius SOUL_VORTEX + DIVINE_HALO, Rades CORPSE_PACT + SOUL_CORPSE_FX1, Gadjah FX_LIGHTNING_ARC + LIGHTNINGFX_BURST gold, Mars CRYSTAL_FX1 + EARTHFAM_BURST, Kahono SONG_NOTES + SONG_RING, Kiato BUTOH_DANCE_RIPPLE + BUTOH_AFTERIMAGE. The new pages above are not given the flourish (they already stack 4 to 6 layers themselves). It is data (`FlourishTable`) and the pilots, the other builders' characters and the basic grimoire do not have it; the integrator may extend the table or lift it into the engine.

## 6. How they cast (the owner stressed it)

* The new pages choose the body animation that fits the gesture: Gadjah's two lightning pages use the thrust (the hand held out), Rill's paintings the sweep (the brush stroke), Charlotte's Blue Rose Paradise the slam (the hands on the ground) and her spear the thrust, Kahono's songs the raised hand, Mars's Laevateinn the overhead slam.
* Where a magic already had its signature casting in the repo, nothing was changed or duplicated: Rill's palette and brush are real items the grimoire manifests (`PaintStudio`), Kahono's old singing system (`song.SongSkill`), Rades's corpses (`corpse.*`), Dante's regalia, Zenon's spatial Dainsleif (`book.LightBoneJa*`).
* Not done (said plainly): no new animation clip, no cast-item requirement and no new key. Charlotte's whip, Jack's forearm blades as an item, Kiato's sword dance with a weapon (`WEAPON_DANCE`) and Kahono's choir pose would each need a clip or an item; they are listed for a later round.

## 7. Decisions that touch the old

### 7.1 Three CanonBook entries moved to their own attribute

A character set lives in the book of the character's `CanonBook.magic`, and the engine binds a canon only to the grimoire of that magic. When `CanonBook` was written (0.34) the mod had no Briar, Soul or Mineral book, so "the closest one" was used (its own comment says so). They exist now, and the sets could not reach the real spells otherwise:

| Entry | Was | Now | Why |
|---|---|---|---|
| `CHARLOTTE` | PLANT | BRIAR | Briar Magic is her attribute (Corpse-Hunting Briar Tree, Queen of Briars are Briar pages) |
| `LUCIUS` | DARK | SOUL | Soul Magic is his attribute; the Paladin pages live in the Soul book (locked to him since 1.44.0, but a Dark canon grimoire could never reach them) |
| `MARS` | EARTH | MINERAL | Mineral Magic is his attribute (Harpe, Nemean Armor, Laevateinn) |

Nothing is lost: a grimoire already bound with the old pair (for example PLANT + `charlotte`) keeps its book and its Canon tag and works as before (the whole book, the old lock line of Briar Prison). A new canon grimoire of these three is the new book. The creative tab and `/multiverse grimoire canon` read the entry, so they hand out the new pair. This is a REPLACEMENT (rule 5) the owner can revert by editing the three strings.

### 7.2 What a character set hides

A page the character's grimoire does not list is hidden from HIS grimoire only (it stays in the basic book unless another set marks it exclusive). The "Not in his grimoire" note of each file lists them, and says which ones are this mod's own pages that no source gave to the character.

## 8. Per-character tables (generated from the files)

### Charlotte Roselei (`charlotte`, book `book_briar`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `briar_strike` | Briar Magic (briars generated and thrown) | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `thorn_lash` | Briar whip | - | anime | mastery 6% | KEPT | no | en_wiki |
| 3 | `creeping_vines` | Briar binding | - | anime | mastery 12% | KEPT | no | en_wiki |
| 4 | `rose_scent` | Rose scent | - | anime | mastery 20% | KEPT | no | en_wiki |
| 5 | `blue_rose_paradise` | Briar Magic: Blue Rose Paradise | - | anime | mastery 30% | NEW | yes | en_wiki |
| 6 | `corpse_hunting_briar_tree` | Briar Creation Magic: Corpse-Hunting Briar Tree | - | anime | `blue_rose_paradise` cast 2 x AND mastery 50% | KEPT | yes | en_wiki |
| 7 | `queen_of_briars` | True Briar Magic: Queen of Briars | - | manga | `corpse_hunting_briar_tree` cast 2 x AND (flag `mana_zone` OR mastery 80%) | KEPT | yes | en_wiki |
| 8 | `scarlet_braided_spear` | True Briar Magic: Scarlet-Braided Beautiful Battle Spear | - | manga | `queen_of_briars` cast 1 x AND mastery 85% | NEW | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* True Briar Magic mana array (the dress and crown of the Queen of Briars) (manga; en_wiki): Part of Queen of Briars (the existing page); the dress itself would be an armour and the armour policy keeps Briar without one.
* Mana Skin (anime; en_wiki): A body coat of mana, not a grimoire page.

Notes: Her CanonBook entry changed from Plant Magic to Briar Magic (it is the series' attribute and the mod has had a Briar book since the 0.53 attribute expansion): a grimoire already bound with the old pair (PLANT, charlotte) keeps working as before and keeps the old lock line of Briar Prison in the Plant book. Not in her grimoire (kept in the basic Briar book): Thorn Reflect, Bleeding Thorn Wall, Briar Leap, Briar Garden and Rose Garden Bloom are this mod's own pages and were not found in any source.

### Jack the Ripper (`jack`, book `book_slash`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `slash_wave` | Slash Magic (projected slashes) | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `forearm_blades` | Slash Magic (forearm blades) | - | anime | mastery 8% | KEPT | no | en_wiki |
| 3 | `death_scythe` | Slash Magic: Death Scythe | - | anime | `forearm_blades` cast 2 x AND mastery 25% | KEPT | yes | en_wiki |
| 4 | `adaptive_edge` | Slash Magic (blades that adapt) | - | anime | `death_scythe` cast 2 x AND mastery 45% | KEPT | no | en_wiki |
| 5 | `lunatic_slash` | Slash Magic: Death Scythe: Lunatic Slash | - | anime | `death_scythe` cast 3 x AND mastery 60% | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Reinforcement Magic (leg strength) (anime; en_wiki): A second attribute, not a Slash page.
* Slash Magic: Ripper Cut (anime; design): The Sword book's page of that name is locked to Jack since 0.34 (Slash Magic used to be filed under Sword); it stays there and is not in this set.

Notes: Not in his grimoire (kept in the basic Slash book): Ripper Dash is this mod's own page and was not found in any source for Jack.

### Rill Boismortier (`rill`, book `book_painting`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `brushstroke` | Painting Magic (brush and palette) | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `spring_of_restriction` | Painting Magic: Spring of Restriction | - | anime | mastery 8% | KEPT | no | en_wiki |
| 3 | `souterrain_groundswell` | Painting Magic: Souterrain Groundswell | - | anime | mastery 20% | NEW | yes | en_wiki |
| 4 | `sirenes_game` | Painting Magic: Sirene's Game | - | anime | mastery 26% | NEW | yes | en_wiki |
| 5 | `deux_tempetes` | Painting Magic: Deux Tempetes of Fire and Ice | - | anime | mastery 34% | KEPT | yes | en_wiki |
| 6 | `four_headed_lindworm` | Painting Magic: Four-Headed Lindworm | - | anime | `deux_tempetes` cast 2 x AND mastery 42% | NEW | yes | en_wiki |
| 7 | `vouivres_shout` | Painting Magic: Phantom Dragon Vouivre's Shout | - | anime | `sirenes_game` cast 2 x AND mastery 50% | NEW | yes | en_wiki |
| 8 | `souterrain_giant` | Painting Magic: Souterrain Giant | - | anime | `sirenes_game` cast 3 x AND mastery 56% | NEW | yes | en_wiki |
| 9 | `gods_game` | Painting Magic: God's Game | - | anime | mastery 62% | KEPT | yes | en_wiki |
| 10 | `master_of_valhalla` | Painting Magic: Master of Valhalla | - | anime | `souterrain_giant` cast 2 x AND mastery 70% | KEPT | yes | en_wiki |
| 11 | `painted_menagerie` | Painted Menagerie | - | anime | mastery 76% | KEPT | yes | design |
| 12 | `twilight_of_valhalla` | Painting Magic: Twilight of Valhalla | - | anime | `master_of_valhalla` open AND mastery 82% | NEW | yes | en_wiki |
| 13 | `elemental_quintet` | Painting Magic: Elemental Quintet | - | anime | `deux_tempetes` open AND `four_headed_lindworm` open AND mastery 90% | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Invasion of Heat-Infused Ice (anime; en_wiki): A magic circle that pours ice over a target; supplementary. Not built yet (no page).
* Souterrain Groundswell / Sirene's Game as Lira's (anime; en_wiki): Lira shares the attribute; her grimoire is not in the CanonBook list.

Notes: Not in his grimoire (kept in the basic Painting book): Souterrain Giant's Strong Arm (the wiki gives it to Lira, and a grimoire has only 12 page slots), Camouflage, Living Illustration and Counter Palette are this mod's own pages and were not found on the wiki's spell list for Rill.

### Dorothy Unsworth (`dorothy`, book `book_dream`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `dream_haze` | Dream Magic (a drowsy haze) | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `dream_world` | Dream Magic (a dream world under her control) | - | anime | mastery 20% | KEPT | no | en_wiki |
| 3 | `imagination_manifestation` | Dream Magic (imagination made real in the dream) | - | anime | mastery 35% | KEPT | no | design |
| 4 | `glamour_world` | Dream Magic: Glamour World | 幻惑の界 | anime | `dream_world` cast 2 x AND mastery 50% | KEPT | yes | en_wiki |

Notes: Glamour World is the only Dream spell the sources name for her. Not in her grimoire (kept in the basic Dream book): Sweet Dream is this mod's own page and was not found in any source for Dorothy.

### Zenon Zogratis (`zenon`, book `book_bone`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `bone_spear` | Bone Magic (Bone Bullets / bones shaped by hand) | 骨弾 | anime | from the start | KEPT | no | en_wiki |
| 2 | `bone_armor` | Bone Armor | - | anime | mastery 10% | KEPT | no | ja_search |
| 3 | `ossuary` | Bone Magic: Eternal Fangs | 無間骨牙 | anime | mastery 20% | KEPT | yes | en_wiki |
| 4 | `demon_sword_dainsleif` | Bone x Spatial Magic: Demon Sword Dainsleif | 魔剣ダーインスレイヴ | manga | `ossuary` cast 3 x AND mastery 50% | KEPT | yes | en_wiki |
| 5 | `spatial_rupture` | Bone x Spatial Magic: Spatial Rupture | - | manga | `demon_sword_dainsleif` cast 3 x AND mastery 70% | KEPT | yes | en_wiki |

Notes: Not in his grimoire (kept in the basic Bone book): Rib Cage and Bone Rain are this mod's own pages (old book), not found in any source. His devil Beelzebub and the spatial cubes of the Spatial Mana Domination are not pages.

### Vanica Zogratis (`vanica`, book `book_blood`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `blood_bullet` | Blood Magic (blood shot from the body) | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `blood_whip` | Blood Magic (blood shaped into a restraint or weapon) | - | anime | mastery 12% | KEPT | no | en_wiki |
| 3 | `red_beast` | Blood Magic: Red Beast | - | anime | mastery 30% | NEW | yes | en_wiki |
| 4 | `herd_of_red_beasts` | Blood Magic: Herd of Red Beasts | - | anime | `red_beast` cast 3 x AND mastery 55% | NEW | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Lifeblood Scythe (anime; en_wiki): A Blood Creation spell; the wiki snapshot gives no user.
* Curse-Breaking Blood Cocoon (anime; en_wiki): A blood healing spell; no user found.
* Jets of Blood / Puppet Bloodflow / Scaffold of Fresh Blood (anime; en_wiki): On the Blood Magic page; users not confirmed for Vanica.
* Curse-Warding Magic: Decaying World (anime; en_wiki): Her second attribute (from the devil); lives in the Curse-Warding book, not in this one.

Notes: Not in her grimoire (kept in the basic Blood book): Crimson Rain, Blood Pact and Hemorrhage are this mod's own pages and were not found in any source for Vanica. The Red Pack named by one fan page was not confirmed and is not built.

### Dante Zogratis (`dante`, book `book_gravity`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `heavy_infighting` | Gravity Magic: Heavy Infighting | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `weighted_blows` | Gravity Magic (added weight) | - | anime | mastery 12% | KEPT | no | en_wiki |
| 3 | `lightened_step` | Gravity Magic (lessened gravity) | - | anime | mastery 18% | KEPT | no | en_wiki |
| 4 | `heavy_infighting_gladiator` | Gravity Magic: Heavy Infighting Gladiator | - | anime | `heavy_infighting` cast 3 x AND mastery 28% | KEPT | yes | en_wiki |
| 5 | `crushing_weight` | Gravity Magic (slamming a target down) | - | anime | mastery 36% | KEPT | no | en_wiki |
| 6 | `demon_king` | Gravity Magic: Presence of the Demon King | - | anime | mastery 50% | KEPT | yes | en_wiki |
| 7 | `evil_gods_pressure_craft` | Gravity Magic: Evil God's Pressure Craft | - | anime | `heavy_infighting_gladiator` cast 2 x AND mastery 65% | KEPT | yes | en_wiki |
| 8 | `gravity_sphere` | Gravity Magic (small black hole; the Gravity Singularity small form) | - | anime | `demon_king` open AND mastery 80% | KEPT | no | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Gravity Singularity (manga; en_wiki): A small black hole that destroys everything in a short range: the book's forbidden ritual, never a page.
* Body Magic: Body Compression and his flesh (anime; en_wiki): His original attribute; the Body book is not his CanonBook entry.

Notes: Gravity Well is this mod's own page and was not found in any source for Dante (kept in the basic Gravity book). Dante's gravity came from the devil Lucifero; his Body Magic was his own.

### Lucius Zogratis (`lucius`, book `book_soul`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `attribute_bolt` | Soul Magic (a soul-flame bolt) | - | anime | from the start | KEPT | no | design |
| 2 | `soul_bind` | Soul Magic (touching the head to seize the soul) | - | anime | mastery 12% | KEPT | no | en_wiki |
| 3 | `attribute_cut` | Soul Magic (severing the soul) | - | anime | mastery 22% | KEPT | no | design |
| 4 | `paladin_conversion` | Soul Magic: Paladin Conversion | - | manga | `soul_bind` cast 2 x AND mastery 38% | KEPT | yes | en_wiki |
| 5 | `paladin_recall` | Soul Magic: Paladin Recall | - | manga | `paladin_conversion` open AND mastery 50% | KEPT | yes | en_wiki |
| 6 | `soul_harvest` | Soul Magic (harvesting souls) | - | manga | mastery 65% | KEPT | no | design |
| 7 | `soul_ascension` | Soul Magic (ascension) | - | manga | mastery 85% | KEPT | no | design |

Canon spells with no page (documented, ignored by the engine):

* Bone Bullets, Time Magic, Gravity, Body, Blood (manga; ja_search): Attributes he gained later by absorbing his siblings and a contract; each lives in its own book.
* Clone creation (Soul, Body, Blood and Bone together) (manga; en_wiki): A union of four attributes; no page.

Notes: His CanonBook entry changed from Dark Magic to Soul Magic (the series' attribute; the mod has a Soul book since 1.44.0): a grimoire already bound with the old pair (DARK, lucius) keeps working as before. Soul Magic has no named spells on the wiki, so only the two Paladin pages are canon; the other pages are marked design.

### Rades Spirito (`rades`, book `book_soul_corpse`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `attribute_bolt` | Soul Corpse (strings of mana on the dead) | - | anime | `corpse_reanimate` open | KEPT | no | en_wiki |
| 2 | `corpse_reanimate` | Soul Corpse Magic: Reanimation | - | anime | from the start | KEPT | no | en_wiki |
| 3 | `corpse_marionette` | Soul Corpse Magic: Marionette | - | anime | from the start | KEPT | no | en_wiki |
| 4 | `attribute_cut` | Soul Corpse (puppet strings) | - | anime | mastery 10% | KEPT | no | en_wiki |
| 5 | `attribute_guard` | Soul Corpse (a plate of corpses) | - | anime | mastery 15% | KEPT | no | design |
| 6 | `corpse_carl` | No.1 Carl | - | anime | mastery 20% | KEPT | yes | en_wiki |
| 7 | `corpse_alfredo` | No.2 Alfredo | - | anime | mastery 30% | KEPT | yes | en_wiki |
| 8 | `attribute_domain` | Soul Corpse (Necropolis) | - | anime | mastery 30% | KEPT | no | en_wiki |
| 9 | `corpse_jimmy` | No.4 Jimmy | - | anime | mastery 40% | KEPT | yes | en_wiki |
| 10 | `attribute_field` | Soul Corpse (Grave Field) | - | anime | mastery 45% | KEPT | no | design |
| 11 | `corpse_legion` | Soul Corpse (Legion) | - | anime | mastery 50% | KEPT | no | en_wiki |
| 12 | `corpse_swap` | Soul Corpse (corpse swap) | - | anime | mastery 55% | KEPT | no | design |
| 13 | `corpse_david` | No.3 David | - | anime | mastery 60% | KEPT | yes | en_wiki |
| 14 | `true_soul_corpse` | True Soul Corpse Magic | - | manga | mastery 70% | KEPT | yes | en_wiki |
| 15 | `corpse_michael_caesal` | No.0 Michael Caesal | - | anime | mastery 80% | KEPT | yes | en_wiki |

Canon spells with no page (documented, ignored by the engine):

* Soul Abductor (anime; en_wiki): Revives the soul by putting it back into the body; only on the user and those who have been with the user a long time. Not built (no page).

Notes: Every rule is mastery only on purpose: the CI self-test magic.soul_corpse_cast opens his six pages at full mastery and a page_uses rule would keep them shut there. The numbering No.0 to No.4 follows the page names of the mod; which corpse the anime showed first was not verified.

### Gadjah (`gadjah`, book `book_lightning`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `chain` | Lightning Magic (lightning from the palms) | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `pulsaranta` | Lightning Magic: Pulsaranta | 体電乱流 | anime | mastery 15% | KEPT | no | ja_search |
| 3 | `tenjiolia` | Lightning Magic: Tenjiolia | 天迅亜音走 | anime | `chain` cast 2 x AND mastery 30% | NEW | yes | ja_search |
| 4 | `apocalypra_astrauza` | Lightning Ultimate Magic: Apocalypra Astrauza | 終焉の奏轟 | manga | `tenjiolia` cast 3 x AND (flag `mana_zone` OR mastery 85%) | NEW | yes | ja_search |

Canon spells with no page (documented, ignored by the engine):

* True Lightning Magic (anime; ja_search): The Heart Kingdom technique of drawing natural mana (took him two years): it is the mana method, not a page.

Notes: Pulsaranta is not exclusive (it is a shared page); Thunder God's Boots, Thunder Fiend, Rising Salim and the other Lightning pages belong to other characters or to this mod and are not in his set.

### Mars (`mars`, book `book_mineral`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `attribute_bolt` | Mineral Magic (a lance of ore) | - | anime | from the start | KEPT | no | design |
| 2 | `attribute_guard` | Mineral Magic: Nemean Armor | ネメアの鎧 | anime | mastery 10% | KEPT | no | ja_search |
| 3 | `lance_volley` | Mineral Creation Magic: Harpe | ハルパー | anime | mastery 22% | KEPT | no | en_wiki |
| 4 | `laevateinn` | Mineral Creation Magic: Laevateinn | レーヴァテイン | anime | `lance_volley` cast 3 x AND mastery 45% | NEW | yes | ja_search |

Canon spells with no page (documented, ignored by the engine):

* Heavy Armored Titan (anime; ja_search): A mineral offensive spell on the wiki's navigation; no description found.
* Talos Puppet / Talos Puppet Cluster (anime; ja_search): Ore doubles of himself (supplementary); the Mineral book's spires are not the same thing.
* Flame Magic: Phoenix Feather Robe (anime; ja_search): His second attribute (healing by fire); lives in the Fire book.

Notes: His CanonBook entry changed from Earth Magic to Mineral Magic (the series' attribute; the mod has a Mineral book): a grimoire already bound with the old pair (EARTH, mars) keeps working as before. Not in his grimoire (kept in the basic Mineral book): Mineral Manifestation, Mineral Field, Mineral Domain and Crystal Spire are this mod's own pages and were not found in any source for him.

### Kahono (`kahono`, book `book_song`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `attribute_bolt` | Song Magic: Destruction Beat | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `attribute_guard` | Song Magic: Rhythm Cushion | - | anime | mastery 10% | KEPT | no | en_wiki |
| 3 | `heal_lullaby` | Song Magic: Heal Lullaby | - | anime | mastery 18% | NEW | yes | en_wiki |
| 4 | `mother_lullaby` | Song Magic: Mother Lullaby | - | anime | `heal_lullaby` cast 2 x AND mastery 32% | NEW | yes | en_wiki |
| 5 | `song_crescendo` | Crescendo | - | anime | mastery 70% | KEPT | yes | design |

Canon spells with no page (documented, ignored by the engine):

* Union Magic: Sea God Slash (anime; en_wiki): With Kiato: her song boosts his sword dance. It is Kiato's page in the Butoh book; a Song-book copy was not built.
* Union Magic: Sea God's Descent (anime_only; en_wiki): With Kiato, anime only; Kiato's page.

Notes: Her old singing system (song.SongSkill: hymn, lullaby, barrier, requiem) is a separate skill and is untouched. Not in her grimoire (kept in the basic Song book): Staccato-style cuts, the Choir field and domain, Choir of Spirits, Metronome, Harmony and Grand Finale are this mod's own pages.

### Kiato (`kiato`, book `book_butoh`)

| # | Page id | Canon name | Japanese | Origin | Opens when | Page | Exclusive | Source |
|---|---|---|---|---|---|---|---|---|
| 1 | `butoh_strike` | Butoh Magic (a strike danced) | - | anime | from the start | KEPT | no | en_wiki |
| 2 | `butoh_trance_dance` | Butoh Magic (trance) | - | anime | mastery 10% | KEPT | no | en_wiki |
| 3 | `butoh_dance` | Butoh Dance | - | anime | mastery 18% | KEPT | yes | design |
| 4 | `butoh_dash_deflect` | Deflect Dash | - | anime | mastery 26% | KEPT | yes | design |
| 5 | `butoh_sea_god_slash` | Union Magic: Sea God Slash | - | anime | `butoh_trance_dance` cast 2 x AND mastery 40% | KEPT | yes | en_wiki |
| 6 | `butoh_sea_god_descent` | Union Magic: Sea God's Descent | - | anime_only | `butoh_sea_god_slash` cast 3 x AND mastery 65% | KEPT | yes | en_wiki |

Notes: Not in his set (kept in the basic Butoh book): Flowing Water Blade, Rhythm Wave, Ebb Step, Tidal Veil, Undertow, Metronome Field and Seabed Hymn are this mod's own pages and were not found as Kiato's spells.

## 9. Server and performance (rules 9 and 11)

* The server decides everything: the rules run on the server in the pilots' unlock tick (every 2 seconds per player, one book); the client only asks "is this page hidden". No packet was added, no config except the pilots' switch. The grimoire's own tag carries the use counts (`CsUses`), so all state is per player and goes with the player; nothing is static per player. The flourish stamp (`nusmp_flourish_rest`) is on the player's own data.
* Worst case per cast of a new page: at most 8 effect packets by the source count (a zone adds at most one more per 20 to 40 ticks), all through `VfxSpawn` (nearby players only, inside the per-tick budget); one zone per cast of at most 11 s that runs every 10 to 20 ticks; every hit loop is capped at 8 targets (6 for the Red Beast); the delayed parts (Souterrain Giant's slams, Apocalypra's beam) check the caster is alive; no entity, block or chunk is made or loaded. Cooldowns are 10 to 90 s; Vanica's two spells and Gadjah's Ultimate Magic cost health but never kill (refused when they would leave under one heart; `MagicGear.selfDamageMult` applies).
* Worst case of the flourish: 2 effect packets per cast, at most one flourish per player per 12 ticks, one aim query (a ray of 18 blocks), nothing for a free page. With 50 players casting the heaviest rotation once per cooldown, that is about 2 packets per player per second at the very most (a starter has 4 s cooldown), far inside the effect budget.
* Hit loops reuse `GrimoireBook.around` / `along` (one box query each). Nothing runs per tick except the zones above. No food or hunger is touched (the scan test pins it).
* The `familyAll()` hook: one reference compare per call and a cached list; the append runs once per book (and again only if a book returns a different list).

## 10. NEW / REPLACED / KEPT

| Part | Verdict |
|---|---|
| The 13 character sets, the 15 added pages and their casts, the flourish layer, the self-test, the lang fragment, the two test classes | NEW |
| A grimoire of Charlotte / Jack / Rill / Dorothy / Zenon / Vanica / Dante / Lucius / Rades / Gadjah / Mars / Kahono / Kiato showing only his pages and opening them by his rule | REPLACED (the whole book opening by mastery order; with the switch off it is back) |
| The basic grimoire hiding these characters' signature spells (the pages marked exclusive) | REPLACED (`basicBookHidesSignatures` false restores it) |
| CanonBook magic of Charlotte (PLANT to BRIAR), Lucius (DARK to SOUL), Mars (EARTH to MINERAL) | REPLACED (7.1) |
| Every page id, name, order, cast, cost and unlock bit of the existing books | KEPT |
| The old lock lines (`CharacterSpells`): Briar Prison in the Plant book, the Paladin pages, Crescendo, Butoh Dance, Deflect Dash, Painted Menagerie, Master of Valhalla, Rades's six | KEPT (the legacy ones are inside the owner's set so they stay reachable) |
| The Soul Corpse fix (generic summons open for any holder, only the numbered undead and True Soul Corpse Magic are Rades's) | KEPT (pinned by `CharacterSetRestTest.theSoulCorpseFixStaysTrueForRades` and the self-test) |
| Floga, Lolopechka, Zagred, and every character of the other builders | KEPT as they are |

## 11. Tests and checks

* `CharacterSetRestTest` (11): the 13 files are shipped and load with no problem; book and magic follow the CanonBook; the three moved canon books; sources and notes; a new grimoire opens one or two spells; a spell that waits for another never opens at full mastery alone; the signature rules (Queen of Briars, Dainsleif, Spatial Rupture, Lunatic Slash, Apocalypra); the old lock lines of these characters are inside their sets; the Soul Corpse fix for Rades; no page is exclusive to two characters and the shared pages are not; the flourish table.
* `CharacterSetRestSourceScanTest` (8): every page a file names exists in the real book source; the 15 added pages are new, owned and exclusive; their text and policy line exist; the hooks and the old lock lines are in place; no client class; no world scan, entity, block, chunk or food in the new code; the packet counts; the three pay-with-life pages refuse. A negative control (a typo in a page id) failed the right tests and was reverted.
* Ran green: the pilots' `CharacterSetRegistryTest`, `CharacterSetLoaderTest`, `CharacterSetSourceScanTest`, `SpellRuleTest`; `ArmourPolicyTest`, `ServerSafetyTest`, `PerfSourceScanTest`, `ConfigFileNamesTest`, `CanonNamingTest`, `CanonGrimoireAssetsTest`, `ParticleThemesTest`, `FlatPropsSourceScanTest`, `WikiStylesTest`, `MagicRebuildRulesTest`, `MagicRebuildAssetsTest`, `SpatialPageOrderTest`, `IsolationTest`, `SoulCorpseAccessTest`, `SoulCorpsePagesSourceScanTest`, `ScreenBlurGuardTest`, `FxSourceScanTest`. One test fails that is not from this release: `AlignmentDataTest.thePageNamesTheKeywordsTouchAreExactlyTheKnownOnes` names the pilots' `black_crescent_moon` and `deep_black_blade` (the keyword scan sees "black" or "moon"); none of this release's pages is in its list. `MagicEnumAuditTest`, `CanonBookMagicTest` and the other tests that need Minecraft classes cannot run in the offline shim.
* CI self-test `magic.character_spells_rest` (`CharacterRestSelfTests`): the files load; every page resolves as a family page of the real book of the real magic of the real CanonBook; fewer than 32 family pages; the appended pages follow the older ones in order, belong to a character and carry the owner lock; on a real instance of every book bound to every character his pages are shown, the others hidden, mode 0 never hidden, the basic grimoire hides his signature spells, a character without a file hides nothing; the signature rules wait; the Soul Corpse ownership agrees with `SoulCorpseAccess`; every character has a two-layer flourish. The lead adds the prefix `magic.character_spells_rest` to the smoke list.

## 12. Not verified, and decisions for the owner

* Nothing was run in a game: no effect was seen, no page opening in order, no hidden page in the page list, no cooldown, no packet. "10x cooler" is the layering plan (3 to 6 existing shapes per new page, two more around every cast, a palette per character), not a seen result.
* No chapter or episode number was read for any spell (section 3), so the ORDER in each file is the owner's rule (a spell after the one it grows from) plus this mod's design from the arcs, marked `design`; the `origin` flags (anime or manga) are best knowledge. Spell effects named but not described in the sources are marked inferred (Scarlet-Braided Beautiful Battle Spear, Laevateinn, Spatial Rupture's slashes).
* The cast numbers (damage, radius, durations) are this mod's; the sources only say what each spell is.
* **Decision 1, page slots.** A grimoire has 6 page slots growing to 12 with mastery (16 with a contract), and the pilots' engine keeps that rule. Rill's set has 13 pages, so the last one he opens (Elemental Quintet) needs a spirit or devil contract, or a slot rule for character sets (one line in `MasteryPages.tryUnlock`). Every other new set is 8 pages or fewer, Rades's Soul Corpse pages are slot-free by the Soul Corpse fix.
* **Decision 2, Lucius's pages.** Soul Magic has no named spell on the wiki, so only the two Paladin pages are canon; the other five Soul pages are marked `design` (the touch on the head, severing, harvesting, ascension). The owner may want only the two.
* **Decision 3, Floga, Lolopechka, Zagred** have no set (section 1); give the names if the owner has a source.
* **Decision 4, the three moved canon books** (7.1).
* **Decision 5, the flourish** is a table of this builder; the other builders' characters and the pilots can join it by adding a row.

