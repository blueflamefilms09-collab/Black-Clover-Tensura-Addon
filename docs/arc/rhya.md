# Rhya, the Disloyal of the Third Eye (boss, saga 1, Abyssal road)

Why this page exists: it records what the story says about Rhya, how the fight was built from it, why each move and each counter is what it is, the numbers, and
what is not verified. Rhya is the member of the Third Eye that no other builder covers (Vetto belongs to the first builder of saga 1, Fana is built beside him),
so he is also the carrier of the Third Eye decision. Shared parts (base class, the bond, config, lead lines) are in [midnight_sun.md](midnight_sun.md). Code:
`arc/midnight/RhyaBoss.java`, numbers in `RhyaTable.java` and `RhyaMath.java`, the memory in `ImitationMemory.java`, tests in `RhyaTest.java`. Model:
`tools/arc/char_rhya.py`.

## Canon (what the story says)

* Rhya (Raia in some translations) is an elf soul reincarnated into a human body, a member of the Third Eye of the Eye of the Midnight Sun with Vetto and Fana;
  his epithet is "Disloyal" (the opposite of the Clover Kingdom's loyalty; the Japanese title is also rendered "Unfaithful" or "Treachery"). He is laid back and
  quick to fight, a little unwilling. A hidden third eye sits in the middle of his forehead under his hair. (blackclover.fandom.com Rhya; tvtropes.org
  BlackCloverEyeOfTheMidnightSun; deltiasgaming.com; soapcentral.com on Rhya; villains.fandom.com Rhya)
* His magic is Copy (Imitation) Magic: by touching another mage's grimoire he copies its spells, and the story shows him with fire, water, light and spatial
  magic, with Salamander's fire, Patolli's Light Sword of Judgment and Heaven-Splitting Flash, and with Yami's Dark Cloaked Avidya Slash; he also has Seal Magic
  and Transformation Magic (he wore the face of a captain), and no other magic of the story can do what he does. (otakuen.com on Rhya; blackclover.fandom.com
  Rhya; en.mogimogy.com list of his copied magics)
* The fight with Mereoleona (episode 91): he throws the Dark Cloaked Avidya Slash and she punches the spell into flame; he vanishes with Invisible Scout and
  strikes from behind with Aqua Javelin; she punches him away; he copies her to exploit her fire and cannot keep up. (blackclover.fandom.com Mereoleona_Vermillion_vs._Rhya,
  Episode_91; tv.apple.com episode page)
* Appearance: a tall young man (181 cm) with messy black hair patched with white, black eyes, a red tattoo on the left cheek and the hidden third eye. As an
  elf after the second reincarnation: hair mostly white with black patches, gold eyes, pointed ears, the tattoo reaching down the jaw and over the eye.
  (blackclover.fandom.com Rhya, via search; the wiki gives no outfit, so the model wears the light hooded robe of the Eye over a dark shirt, an addition)

## Design in one paragraph

The fight is a mirror held up to the party. He learns the magic that hits him, or the magic of the grimoire he touches, and answers in its colours and with its
rider; and he knows that magic so well that it hurts him less. The answer is to change what you throw. Around that: the story's copies (Avidya Slash, Aqua
Javelin, Salamander's Breath, the Light Sword), the Invisible Scout, Seal Magic, and a Stolen Spellbook that rains his borrowed magics down in turn. At the
Gates the third eye opens and he holds two magics at once.

## Mechanics and counters (the three that only he has)

1. **Copy Magic** (`ImitationMemory`). Two ways to be learned: a grimoire page that hits him stamps him (the mod's `CopyMemory` writes the book and the time on
   the target just before it hurts), and Copy Magic: Touch, where he dashes to a foe and lays a hand on its grimoire. What he holds is the NEWEST magic; meeting a
   magic again moves it to the front instead of filling a second slot; it fades after 24 seconds (480 ticks). The first blow of a magic lands whole, every
   later one of a magic he still remembers deals 70% (a resistance, never immunity). A sword, a Tensura skill or any damage that is not a grimoire page carries no
   stamp, is not learned and is not resisted. He answers in what he holds: the colour round his body shows it, the action bar names it before each Imitation
   move, and the bolts, lance, nova and the Spellbook's salvos wear its colours and carry its rider (burn, slow, weaken, poison, wither, shove, and blindness or
   levitation only through the shared control rule). Counter: change what you throw. One magic is resisted; two magics thrown in turn beat a memory of one;
   weapons and skills are untouched; and at the Gates (capacity two) you need three. The touch is a dash with its line and landing ring drawn: leave the ring and
   he learns nothing.
2. **The Invisible Scout.** He turns invisible in the last 12 ticks of the wind-up and appears 2.4 blocks behind the target, striking a ring of 2.6 blocks. The ring is
   drawn on the floor from the start, so the whole move is "step out of the circle"; any blow that lands on him shows him at once. The effect is the vanilla
   invisibility; see Unverified for how it renders.
3. **The Third Eye** (the Gates and the bond). At 100% the eye opens: memory capacity two (the lance and the nova use the older magic, the bolts the newest) and the
   second model. And while any other member of the trio (Vetto, Fana) stands, beating one of them makes him rage one step (see [midnight_sun.md](midnight_sun.md)).

## The moves (base damage before the boss's damage law, the guard and `damageScale`)

| move | min power | wind-up | cooldown | range | weight | recovery | damage and shape |
|------|-----------|---------|----------|-------|--------|----------|------------------|
| Copy Magic: Touch | 20 | 18 | 260 | 0 to 30 | 4 | 8 | a dash drawn as a line to a spot just in front of the target and a ring of 2.4 blocks: 7 and the touch (mechanic 1) |
| Imitation: Bolts | 20 | 18 | 90 | 5 to 36 | 6 | 8 | 3 to 6 bolts in a fan (1.3 blocks a tick, radius 0.6), the Tower table's 6.5 each, in the magic's colours with its rider; lines lock 8 ticks before |
| Imitation: Lance | 20 | 22 | 160 | 6 to 36 | 4 | 10 | a beam 28 blocks long and 1.2 half width, the Tower table's 11, locks 10 ticks before; wears the older magic at the Gates |
| Dark Cloaked Avidya Slash | 20 | 22 | 150 | 5 to 26 | 4 | 10 | a lane 20 blocks long and 3.2 half width drawn with its edges, 12; leave the lane |
| Aqua Javelin | 20 | 20 | 130 | 8 to 38 | 4 | 8 | a thin lane 34 blocks long, 0.8 half width, locks 10 ticks before, 10 |
| Invisible Scout | 20 | 24 | 210 | 5 to 40 | 4 | 10 | mechanic 2, 13 (a body blow: only a shield cuts it) |
| Imitation: Nova | 40 | 28 | 250 | 0 to 34 | 4 | 12 | crowd: 1 to 2 rings of 5 blocks marked under the foes, the Tower table's 13, with the magic's rider and a shove |
| Seal Magic: Chains | 40 | 28 | 260 | 0 to 30 | 3 | 10 | 1 (2 from 80%) rings of 3 blocks marked under the foes: 6 and a root of 25 ticks through the shared control rule |
| Copied: Salamander's Breath | 60 | 24 | 220 | 2 to 16 | 3 | 8 | a cone of 11 blocks and 28 degrees, 9, burning 2 s |
| Copied: Light Sword of Judgment | 60 | 22 | 200 | 5 to 34 | 3 | 8 | 3 to 5 swords at 3.5 blocks a tick, 5.5 each, locks 8 ticks before |
| Stolen Spellbook (ultimate) | 60 | 50 | 1250 | 0 to 40 | 5 | 60 | crowd: 5 salvos 14 ticks apart, each in another stolen magic (the ones he holds first, then the four of the story), each marking up to 5 rings of 3 blocks under the foes for 18 ticks, 9 each with the rider; over in under 6 seconds |

The mix: close (touch, scout, nova), ranged (bolts, lance, javelin, swords), area (avidya, nova, seal, breath, book), mobility (the scout, the touch's dash),
control (the chains' root), a signature ultimate. Every wind-up is at least 16 ticks on paper (12 at the Gates); the Spellbook's is 50 on paper (37 at the Gates).

## Numbers

* `ArcCharacter`: tier 2, 1450 health, 12 attack, 0.34 speed, armour 5, toughness 2; Abyssal road, floors 40 to 65; themes vampire_castle ("mirrors that show
  nothing"), spatial, shadow, cursed_archive, feywild_grove (tricks and illusions); after vetto and fana; Charter (25%): analytical_appraisal, analyze, invisible,
  magic_sense, heavenly_eye, thought_acceleration, spatial_motion, water_blade, shadow_bind. Colours 0x8E7BE0 and 0xFFD35A. Magic id `copy`.
* Hit box 0.55 by 2.0. `ArcLook`: from `tools/arc/manifest.json` (`rhya` and `rhya_form2`, the bigger of the two forms for the margins): walk 1.21, run 2.15,
  shadow 0.44, grow 1.12 horizontally and 0.79 up.
* Counts by power step (20, 40, 60, 80, 100): bolts 3, 3, 4, 5, 6; light swords 3, 3, 4, 4, 5; novas 1, 1, 2, 2, 2 (the Tower table's cap); chains 1, 1, 1, 2, 2;
  memory capacity 1, and 2 at the Gates. Resonance 0.70, window 480 ticks.
* Defaults when he holds nothing (cycling, so every one comes up): flame, water, light, spatial, dark.

## Tests (`RhyaTest`)

The table obeys the laws; no dead range at the opening; counts rise with power; the shapes agree with the Tower tables (the nova's radius and mark time); every
mark can be left in the time it is shown (touch ring, chains, Spellbook rings, scout ring, the wide slash, the frozen lines); the Spellbook is over in six
seconds; the memory keeps the newest and forgets with time; with the eye open he holds two and meeting one again refreshes it; what he knows hurts him less
(the resonance test was mutation-checked: forcing the factor to 1.0 makes it fail); the defaults cycle through all five; up close he touches and far away he
throws what he borrowed.

## Model

`tools/arc/char_rhya.py`. First form: messy black hair with white patches, black eyes, a red scar-like tattoo on the left cheek, a light hooded robe of the Eye
over a dark shirt and trousers with a double belt, dark boots, and a borrowed grimoire in the left hand with a violet glow. Second form (the Open Eye): white hair
with black patches, gold glowing eyes, pointed ears, the tattoo reaching further with cheek lines, the third eye open on the forehead and a gold glow. Added for
the game and not from the source: the robe (the wiki gives no outfit) and the grimoire's glow. Previews: scratchpad `previews/s1b/arc_rhya_*.png`.

## Unverified

* Nothing was played; every number is a first guess. The resonance (70%) is a strong rule for a party with one magic only: it cuts their damage by 30% after the
  first blow until they switch; weapons and skills avoid it. If that is too harsh, lower the damage scale or the resonance constant (`ImitationMemory.RESONANCE`).
* The learning reads the keys `nusmp_copy_book` and `nusmp_copy_at` that `CopyMemory.remember` writes on the target; the code writes them just before the hurt
  call, so the read in the same tick should see them. Not exercised; a book that cannot be found in the skill registry teaches nothing.
* The touch reads the grimoire of a player foe through `GrimoirePages.grimoireOf`; for an NPC foe he lifts one of the five defaults.
* Invisibility: the arc boss renderer was not read for how it treats the vanilla effect; if it ignores it the scout is just a drawn dash (still fair).
* Imitation shapes (bolt, lance, nova) reuse the Tower table's numbers (`TowerMath`), not its code; the Tower's own cast code belongs to another boss and was not touched.
* Stolen Spellbook, Seal Magic, Copy Magic: Touch as a dash and the Imitation bolts are design additions around the canon facts (the story does not show these
  exact moves).
* Canon summaries come from web search (fan wikis and character sites) and disagree in places (epithet wording, the elf form); the model and the moves follow the most
  consistent reading.

## Weapons

None.
