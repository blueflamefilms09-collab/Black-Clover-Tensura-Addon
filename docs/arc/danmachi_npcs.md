# The friends of the Dungeon: six NPCs of the DanMachi saga

Part of [`danmachi.md`](danmachi.md). Six friendly characters of the shared NPC entity (`nusmp:arc_npc`, see `docs/arc/core.md`): a record each in `DanMachiNpcs`, a model each in `tools/arc/char_<id>.py`
(the art kit), a dialogue file each in `data/nusmp/arc/dialogue/<id>.json`. They never fight; they greet, give hints that follow the story, and four of them ask for the fall of one monster.

## 1. Roles (and what the shared entity does not have)

The shared NPC entity has **no shop and no quest system** (the core says so), so the roles of the smith and of the supporter are their words: each has `shop` lines that a future shop would speak (they are never said
otherwise; a test checks only these two have any), and the requests are driven by `DanMachiQuests` through the arc system's quest stages (`TalkPlan`: 0 none, 1 offered, 2 done, 3 thanked):

| friend | id | asks for | the request is offered to a player who has not beaten it | thanked after |
|---|---|---|---|---|
| Eina Tulle | `eina_tulle` | the Minotaur | "an open request at the counter ..." | "You brought the Minotaur down ..." |
| Welf Crozzo | `welf_crozzo` | the Black Goliath | "I want to know what that black hide is made of ..." | "... its hide breaks under three heavy blows in a row ..." |
| Liliruca Arde | `liliruca_arde` | the Amphisbaena | "Do you want a job? The two-headed dragon in the Water City ..." | "You beat it! ... Thank you, {player}." |
| Ais Wallenstein | `ais_wallenstein` | Udaeus | "...Udaeus, on the thirty-seventh floor. I want to know how strong you are ..." | "You beat Udaeus. ...Thank you. I will remember it." |

Bell Cranel and Hestia ask for nothing. When the monster falls, every fighter credited with the kill (`ArcStory.credited`: the online fighters, and their FTB teammates when the share switches are on)
has the stage moved to 2 (not from 2 or 3), is told in chat, and the friend thanks them once, the next time they talk. The offer is spoken once, when the player first talks to the friend after they have the story to hear it; a player who beat the monster before ever
hearing the offer (for example credited by the unlock command) is not offered a request that is already done and hears the hints instead. `quests: false` in the config leaves the requests offered but never finished by themselves.

**Hints follow the story**: each friend has, besides general advice, one hint for each of five moments (no DanMachi monster beaten; the Minotaur beaten; and the Black Goliath; and the Amphisbaena; all four), each tied to the monsters with `defeated`/`notDefeated` and
mixed with the general hints (`priority 0`, story hints three times as likely), so the next monster is named and the one already beaten never is. They teach the counters in the friend's voice (Eina the wall and the Level; Bell the same from his side; Welf the hide; Lili the mist and the red head; Ais the ribs; Hestia
the mood). Greetings: two lines for the first meeting that depend on nothing else (so a newcomer in any saga hears one), three or four for old friends. All lines are plain ASCII, in the author's own words (no line of the series is quoted), each keyed `arc.dialogue.<id>.<n>`
(125 keys in `frag/dm.lang.json` with the four entity names).

## 2. The six

**Eina Tulle** (`eina_tulle`, "the Guild Adviser") and the Chronicle's guide. Canon (as summarised): the half-elf adviser of the Orario Guild who looks after Bell's records and his safety; long brown hair, pointed ears, glasses, the Guild's uniform.
Model: `slim`, 1.8 blocks, brown hair, the kit's elf ears, glasses, the dark green uniform with a gold trim, a white blouse and a cravat; **added for the game**: the open ledger in her left hand (the kit's grimoire, with a soft glow like the Chronicle).
Voice colour `#D0B060`. She opens the request of the Minotaur and explains the Dungeon Level in plain words (the Guild's "Level" books).

**Bell Cranel** (`bell_cranel`, "the White Rabbit"). Canon: the white-haired, red-eyed boy of about fourteen of the Hestia Familia, the hero; a light chest guard, a white shirt, a short black knife (the Hestia Knife).
Model: `slim`, 1.8 blocks, shaggy white hair, red eyes, a cheerful face, a plain breastplate and bracers, a dagger with a blue glow (**added**: the sign of the Falna that grows with every Level). No request.

**Hestia** (`hestia`, "the Hearth Goddess"). Canon: a short goddess with long black hair in twin tails tied with a blue ribbon, blue eyes, a white dress, and a ribbon wound round her arms (from the series' key art as the summaries describe it; the exact winding is not verified).
Model: `small`, 1.5 blocks, twin tails with blue ties, a white dress with a blue hem and sash, **the ribbon on the arms is the kit's bandage in blue** (the kit has no cord prop), blue glow on the trim. No request.

**Welf Crozzo** (`welf_crozzo`, "the Smith"). Canon: a sturdy young smith of the Hephaistos Familia (then Bell's friend), red-brown hair, a headband, a leather work outfit, a great magic sword on his back (a Crozzo magic sword).
Model: `broad`, 1.95 blocks, spiky red-brown hair, a red headband, a leather vest and arm guards, **a hammer in the hand (added: a smith is shown at work)** and the great sword on his back with an ember glow. `shop` lines; asks for the Black Goliath.

**Liliruca Arde** (`liliruca_arde`, "Lili, the Supporter"). Canon: a pallum (a people that stays child-sized) supporter with short auburn hair, a hooded cloak and an enormous backpack.
Model: `small`, 1.45 blocks, short auburn hair, a vest over a shirt, shorts, the kit's backpack and a short mantle, a short knife in the hand (**added**: the kit has no crossbow). `shop` lines; asks for the Amphisbaena.

**Ais Wallenstein** (`ais_wallenstein`, "the Sword Princess"). Canon: the swordswoman of the Loki Familia who saved Bell on the 5th floor; long golden hair, golden eyes, a calm, nearly expressionless face, dark blue battle clothes under a silver
breastplate with light armour on the arms and shins, a straight sword; her magic is the wind.
Model: `slim`, 1.85 blocks, long golden hair, amber eyes, a navy tunic with silver trim, a breastplate, bracers and greaves, a sword with a pale green glow (**added**: the wind of her magic). Asks for Udaeus.

Sources (standard page titles of the wiki, **not opened here**: the fetch tool was blocked and the last searches were refused): `https://danmachi.fandom.com/wiki/Bell_Cranel`, `.../Hestia`, `.../Ais_Wallenstein`, `.../Liliruca_Arde`, `.../Welf_Crozzo`, `.../Eina_Tulle`;
`https://en.wikipedia.org/wiki/Is_It_Wrong_to_Try_to_Pick_Up_Girls_in_a_Dungeon%3F`.

## 3. Numbers and rules

Hitbox: the shared entity's (0.6 x 1.95); the model is drawn at its own size (Hestia 1.5, Lili 1.44, Welf 1.94). Voice colours (the chat line's colour): Eina `#D0B060`, Bell `#E8E8F4`, Hestia `#3A78D8`, Welf `#C8782A`, Lili `#B4683C`, Ais `#E0B040`.
The records name no tier and no health (an NPC has none). Story: they are open from the start (no prerequisites); the order in the roster is the order the story meets them (Eina, Bell, Hestia, Welf, Lili, Ais).

## 4. Verified and not

Tested: that the six files and six records are the same six ids, that every friend greets a stranger with a line that depends on nothing else, that every hint is tied to monsters in story order and never names one already beaten, that a request is
offered only before the fall and thanked only after (with the real dialogue rules), that only the smith and the supporter have shop lines, that every line is ASCII with the key the generator numbers. Looked at: the front and face of all six, the back of Lili.
**Not verified**: the lines in the game (the chat colour, the farewell timing), the canon facts above (summaries only), whether the models read as the characters in the game's lighting (the previews are the kit's software rasteriser), and the talking clip of a friend
holding a prop (Eina's ledger, Welf's hammer) in the shared NPC renderer.
