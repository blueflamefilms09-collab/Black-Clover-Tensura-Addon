# Game Magic (0.49)

**New.** Game Magic belongs to Gifso of the Spade Kingdom (the Black Clover wiki): "a magic that oversees and manipulates events within an area, like a game."
- **Grimoire:** `MagicType.GAME`, book `book_game`, rolled for a FANTASY soul.
- **Source:** `book/GameBook.java`.
- **Rules:** D&D. Every roll is a d20 plus your EP modifier (+0 to +4). A natural 1 or 20 does something special.
- **Look:** the dice tumble on screen (the d20 VFX from Dice Magic).

## Pages

| Page | Cooldown | What it does |
|---|---|---|
| **Game Board** | 60 s | The preparation the wiki asks for: magic poured into the ground. It makes a 16-block board of glowing squares for 2 min. Every 10 s each piece on it rolls a saving throw: foes who fail are cursed, and allies who succeed are blessed. Your spells on the board are 50% stronger. |
| **Monster Toy** | 30 s | The wiki's offensive spell. A game-piece monster rises where you look. Its size is rolled on a d20: under 8 an einherjar, 8 to 17 a random beast or chimera, 18 or more a giant. It fights and casts spells for you. |
| **Temple Shuffle** | 20 s | The wiki's support spell. Every foe on your board is moved to a random square, and your allies close in round you. Needs the board. |
| **Initiative** | 25 s | You and each foe within 12 blocks roll d20. Foes you beat lose their turn (slowed, magic jammed). If you beat them all, you move first (speed). |
| **Dungeon Master's Verdict** | 45 s | Each foe on the board makes a DC 12 saving throw. A failure takes 4d6 × your EP scaling; a natural 1 doubles it. If you roll a natural 20, everyone fails. |
| **Trap Squares** | 30 s | Six hidden squares on your board for a minute. A foe who steps on one is snared and hurt. Needs the board. |

Cooldowns are before the global cooldown factor.

**VFX:** `GAME_BOARD`, a round board of glowing gold squares with a turning rune rim and points of light hopping across the squares (`vfx/client/layer/GameLayer.java`, texture from `tools/gen_game_textures.py`). Preview: `docs/vfx_previews/game_board.png`.

## Not verified
None of it has been run in game. The 16-block board's per-second checks have not been measured for performance.
