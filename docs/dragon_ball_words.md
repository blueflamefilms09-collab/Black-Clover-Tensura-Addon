# The words of the Dragon Balls, taught ball by ball (4.95.0)

Owner request: "players will be taught the words as they get each of the Dragon Balls, and then be given a prompt to basically just copy and paste it into chat", and "give me the summoning phrases for both the Super Dragon Balls and regular Dragon Balls".

## What a player sees
* The first time a player holds a ball (picked up, or found in a bag within five seconds), the dragon whispers the next piece of the set's call in chat, for example `Shenron: Ball 1 of 7. Listen: "takkaradoppo".`
* A second line shows everything known so far with two buttons: **[Copy]** (to the clipboard, then Ctrl+V in chat) and **[Put in chat]** (fills the chat box, press Enter).
* The order is the order the player finds the balls, not the stars. Finding a ball twice teaches nothing new. The last ball always completes the phrase.
* A short phrase gives one word per ball (later balls say the dragon stirs); a long phrase is cut into even groups over the first six balls.
* `/dragonwords [earth|super]` (any player) shows what you know again, with the buttons.
* Summoning itself is unchanged: the same words, said or typed, with the whole set near you. A player who already knows them never needs the lesson.

## Setting
`dragonBalls.teachWords` (server, `nusmp-dragonballs-server.toml`, default true). false = nothing is taught and nothing is sent, exactly as before.

## The summoning phrases (config lists `earthPhrases` / `superPhrases`; case and punctuation ignored)
The lesson teaches the FIRST entry of each list.

| Set | Phrase | Source |
|---|---|---|
| Earth (taught) | `Takkaradoppo Pakkoradoppo Yaikanaan` | the owner's brief; not found on any page this session could reach |
| Earth | `Come forth, Shenron` / `Arise, Shenron` | English wiki, Shenron (the GT dub call is "Arise, Shenron") |
| Earth | `Eternal Dragon, by your name I summon you forth: Shenron` (NEW) | English Dragon Ball wiki, "Dragon Ball (object)": the DBZ dub formula |
| Earth | `Ideyo Shenron` / `Detekoi Shenron` | Japanese call 出でよ神龍; Detekoi unverified |
| Namekian (accepted by the Earth set, NEW) | `Takkaraputo Popporunga Pupiritto Paro` (also `Takkarapto ...`) | official Dragon Ball site news page and Dragon Ball Fandom (Porunga trivia); spellings differ between sources |
| Super (taught) | `Come forth, Dragon of the Gods, and grant my wish, pretty peas` | English wiki, Super Shenron (the Japanese joke swaps "chonmage" for "chodai", "please") |
| Super | `Come forth, Divine Dragon, and grant my wish, peas and carrots` | dub variant, uncertain |
| Super | `Come forth, Dragon of the Gods` / `Come forth, Divine Dragon` | short forms so a typo in the long line is not punished |

Wiki rule (CLAUDE.md 10): dragonball.fandom.com (English and Japanese) could not be reached from the build session (host not allowed), so the phrases were checked by web search only: Namekian and the DBZ / GT lines are confirmed by those search results; the exact Super Shenron line and the Japanese lines are NOT confirmed.
Existing servers keep the lists already in their config file; delete the two list lines to get the new defaults.

## Cost and safety
A bit test per ball per five seconds (the ball's own tick). One saved row per player (`nusmp_dragonball_words`, at most 4096 rows, the oldest is dropped to make room, two small int masks each). Two chat lines per newly found ball; no packets of its own, no entities, no scans. Cleared by nothing automatically (a player keeps what they learned); `DbLessons.forget` wipes a row. Server side only; the buttons are plain chat click actions.
Self-tests `dbwords.rules` and `dbwords.teach`; unit test `PhraseLessonsTest`.
