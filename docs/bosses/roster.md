# Dungeon boss roster (owner's list, rules in docs/bosses/RULES.md)

Source: the owner's message, spelling as typed. Nothing here is built yet. Ranks follow docs/bosses/RULES.md (flat F to SSS, one rank per 20 floors, A to SSS in rifts until the dungeon is deep enough).

| Rank | Bosses |
|---|---|
| SSS | (none yet) |
| SS | (none yet) |
| S | Funny Valentine, Isshiki Otsutsuki, Mewtwo, Zamasu |
| A | Kaguya Otsutsuki, Pucci, Frieza |
| B | Kaido, Blackbeard, Starrk |
| C | Killua, Hisoka, Gin Ichimaru, Grimmjow |
| D | Mahito, Tomura Shigaraki, Baruka (Solo Leveling) |
| E | Muzan, "Hallow" (Bleach), Akaza |
| F | Shou Tucker + Chimera Daughter, Light Yagami (GAG boss, see below), Ranga |

23 bosses. This answers "how many bosses per rank": F 3, E 3, D 3, C 4, B 3, A 3, S 4, SS 0, SSS 0.

## Light Yagami (F, gag boss)
Not a real fight. An over-animated, anime-style cutscene: Light raises his hand with the pen and writes the player's name in the Death Note.

Reference clip from the owner: `Full-Death-Note` (108 s, 480x360, the potato-chip scene), watched as frames only (3 contact sheets, one frame per 3 s); its audio and Japanese text were not transcribed. What the frames show, in order, which is the style the gag copies:
1. Extreme close-ups of Light's eyes and face, hard black shadow cut across the face, hair falling over one eye.
2. Dramatic insert shots of the object (the chip bag, labelled in katakana) as if it were a weapon; Ryuk upside-down on the bed in the dark background.
3. A close-up of the pen gripped in the hand, then the pen tip raised; a very serious tone for a silly act.
4. Quick cuts between tight shots (eye, hand, pen, bag, mug), then a wide shot of the room (desk, monitor, shelves).
5. A split-screen / panel burst (four boxes at once) at the peak of the monologue.
6. A hidden TV under the bag, and Light writing from a high overhead angle in a green-lit pool of light.
7. A cut to L and the investigators in a dark room, then back to Light at the desk in a wide silhouette shot.

How the gag will use it (design, not built):
- Beats: eyes close-up -> dark shadow slash -> the hand rising with the pen (slow, held) -> an insert on the pen tip -> panel burst -> the name appears on the page -> a quick over-dramatic reaction -> punchline.
- Mod tools: the existing boss intro / title-card path and the Time Magic style of short scripted VFX (do not touch the Time Magic VFX files), black bars, speed lines, screen shake, a few client-only overlay quads. All of it is client drawing; the server only starts and ends the scene for the nearby party.
- Bounds (rule 11): one scene of a few seconds, at most a couple of overlay quads per frame, shown only to players near Light, skipped for far players.
- DECIDED (owner: both): the gag has TWO parts in one scene. Part 1, the chip beat: Light's over-serious inner monologue while he takes and eats a potato chip (close-ups of the bag, the dramatic chip, Ryuk behind him). Part 2, the pen beat: he raises his hand and pen and writes the player's name in the Death Note. The chip beat is the joke setup, the pen beat is the punchline.
- OPEN: what happens at the end of the gag (proposal: harmless, a joke defeat for Light and a funny reward, never real player death).

## Compared with what the mod already has (rule 5)
- The released 3.33.0 tier-list batch already holds 16 bosses on floors 1-20: Kurozumi Orochi, Mr. 2 Bon Clay, Sogou Nobuyuki, Charlos, Yammy Llargo, Zetsu (F) and Malty S. Melromarc, Rachel, Jugram Haschwalth, "Big Mom" Charlotte Linlin, Code, Bertholdt Hoover, Morgan, Foxy, Shou Tucker, Ao (D). Those were placed by the old owner tier list (docs/bosses/tierlist.md), which used different tiers.
- **Overlap:** Shou Tucker exists as a D-tier boss. The new list wants Shou Tucker + Chimera Daughter at F. Proposal: keep the old one (rule 2) and add the F version as a new boss with the daughter; the owner can later ask for the old one to be removed.
- **NEW (not in the mod):** all other 22 bosses above.
- **KEPT:** the 16 bosses of 3.33.0 and the Zogratis bosses stay as they are. Whether they fit the new F-SSS ladder is for the owner to say (they are on the old tier letters).
- The new list ranks characters differently from the old tier list (for example Funny Valentine was SSS+++ there, S here). The new list is the one for the dungeon rules.

## Names to confirm
- "Hallow from bleach": a Hollow? (which kind: Gillian, Adjuchas, Vasto Lorde, or a Hollow Ichigo?)
- "Ranga": which series? (several characters use the name)
- "Grimjoww" read as Grimmjow Jaegerjaquez; "Isshiki Otsusuki" read as Isshiki Otsutsuki (Boruto); "Baruka" read as Baruka (Solo Leveling).

## Gaps
- No SS and no SSS bosses yet. The rift keys (each boss drops the key to the next rank) need a boss for SS and SSS, or S can be the end of the chain for now.
- No obvious ice-based character is on the list, so the "two ice bosses" idea from the rules needs a decision (add two ice bosses, or pick two from this list and give them an ice side, e.g. Frieza as an ice-flavoured rank-A boss).

## Research (CLAUDE.md rule 10)
Canon moves per boss come from the English wiki first, the Japanese wiki as the fallback. Not done yet; each entry must say which source it came from.
