# Devils of many worlds (3.76.0): the Devil Contract system finished

**What was missing.** The 3.48.0 system had tiers, the assimilation key, the Devil Union, the markings and the sacrifices, but only four invented grimoire devils, no ritual and no summoning, and its notes said: "the attribute has no combat effect yet". This release adds the devils of eight anime worlds, a different ritual and summoning for each world, and makes every attribute act in a fight. Everything old is kept: the grimoire pact (`/devilpact`), the four Infernal Contracts, the Anti-Magic union.

## How a player uses it

1. `/devil worlds` (or use an Infernal Contract and click **[Or call a devil of another world]**): the eight worlds, each a clickable line; hovering shows what its ritual asks and costs.
2. Click a world (`/devil world <world>`): its devils, each a clickable **[Call ...]** line with tier, attribute and what the attribute does.
3. Click a devil (`/devil call <devil>`): if you meet the world's conditions the ritual begins where you stand. Stay within 3.5 blocks of that spot until it ends.
4. The devil makes its offer in chat: power, price, **[Sign]** / **[Refuse]** (30 seconds).
5. Signed: you hold a normal Devil Contract with that devil's tier and attribute: the Devil key (N), assimilation, markings, horns, wings, the Devil Union all work as in 3.48.0. `/devil status` names the devil and its world. One bond per mage still holds (a Spirit, a Devil or a Djinn).

## The eight worlds

| world | ritual | what it asks | the summoning you see | lasting price |
|---|---|---|---|---|
| Black Clover | Devil-Binding Ritual (200 ticks) | a relic tied to a devil: one Infernal Contract, spent | the red-black devil circle and a dark field; the devil strikes four times (15 % of your maximum health each, never the last heart) with a dark burst; a circle explosion when it yields | none: an equal contract |
| Chainsaw Man | a bargain of flesh (80 ticks) | nothing: you call its name | a pool with grasping hands, a toothed maw rising from it, a rain of blood, a blood sigil | flesh: Fox Devil two hearts, Curse Devil half your health (years of life), Future Devil and Ghost Devil your right eye |
| Black Butler | Faustian contract (140 ticks) | despair: health at half or lower | a violet dark field, black feathers falling, the seal drawn in the air, a flare at your eye | the seal on your right eye, and your soul: every death takes all experience levels |
| Blue Exorcist | Tamer's summoning circle (160 ticks) | a sheet of paper (spent) and a drop of blood (two hearts now) | a small blue circle, three lines of the appeal, a spirit aura, a burst of flame in the demon's colour | none, but if your health falls under a third or you leave the circle the demon turns on you (3 hearts) |
| Tensura | Daemon summoning (180 ticks) | an offering of bodies (32 rotten flesh, spent) and a name (half of your maximum magicules) | a ring gate to the underworld, souls spiralling in, a pillar in the daemon's colour | none |
| Dororo | pact in the Hall of Hell (160 ticks) | a roof over your head (no open sky) | a ring of stone, a curse flying to you, lightning from above and arcs of blood | your left eye and two hearts; it pays 32 emeralds, 8 diamonds, 16 gold ingots |
| Berserk | the Behelit's Eclipse (200 ticks) | your lowest point: health at a quarter or lower, and 30 experience levels | a black sun overhead, a zone of nothing, a downpour of blood, the Brand sigil | all your experience levels now, half your health, and the Brand (monsters hit you a fifth harder) |
| Fairy Tail | a Book of Zeref (140 ticks) | a book (spent) and ether: a third of your maximum magicules | a wheel of runes, letters lifting off the page, a magic circle, demon fire | none |

Preview: `docs/previews/devil_worlds/summonings.png` (three effects per world; offline previewer).

## The 26 devils

Black Clover: Liebe (High, Anti-Magic), Lucifero (Supreme, Gravity), Beelzebub (Supreme, Spatial), Astaroth (Supreme, Time), Megicula (High, Curse), Lucifugus (High, Word). Chainsaw Man: the Fox Devil (Mid, Beast), the Curse Devil (High, Curse), the Future Devil (High, Foresight), the Ghost Devil (Mid, Ghost). Black Butler: Sebastian Michaelis (Supreme, Soul). Blue Exorcist: a kin of Iblis (Mid, Flame), of Egyn (Mid, Water), of Amaimon (Mid, Earth), of Astaroth (Low, Rot), of Lucifer (High, Light). Tensura: a daemon of the line of Noir (Supreme, Curse), Blanc (High, Light), Jaune (High, Flame), Violet (High, Rot), Rouge (Supreme, Soul), Vert (High, Earth), Bleu (High, Water). Dororo: a demon of the Hall of Hell (High, Beast). Berserk: the God Hand (Supreme, Causality). Fairy Tail: an Etherious (High, Ether).

The tier, the attribute given to a devil where the source names none, the colours and every number are this mod's design, not canon.

## What an attribute does in a fight (new)

While the devil is let in (assimilation 20 % or more), the host's own blows, at most once a second, deal extra damage and carry the attribute's effect. The extra damage is the blow x the attribute's bonus x a power that grows with the level (Low 0.4, Mid 0.6, High 0.8, Supreme 1.0 at 100 %; x 1.5 in the Devil Union), never more than three times the blow; against a player it is held at 15 % of their maximum health.

| attribute | bonus | effect |
|---|---|---|
| Gravity | 30 % | Slowness III, 3 s |
| Spatial | 55 % | none: the damage is the effect |
| Curse | 25 % | Wither II, 4 s |
| Anti-Magic | 30 % | removes Resistance, Absorption, Regeneration, Fire Resistance |
| Flame | 35 % | fire, 6 s |
| Word | 20 % | Weakness II, 4 s |
| Time | 20 % | Slowness V, 1.5 s |
| Beast | 50 % | none |
| Foresight | 35 % | Glowing, 5 s |
| Ghost | 15 % | Slowness VI, 2.5 s |
| Soul | 25 % | heals the host a quarter of the blow (at most 3 hearts) |
| Rot | 20 % | Poison II, 5 s |
| Earth | 35 % | Slowness II, 2 s |
| Water | 25 % | Mining Fatigue II, 4 s |
| Light | 40 % | Glowing, 5 s |
| Ether | 55 % | none |
| Causality | 60 % | heals the host a tenth of the blow |

Hard slows are shortened on bosses by the mod's control rule (`BalanceLaw.controlTicks`).

## Settings (`serverconfig/nusmp-devil-server.toml`)

`devil.worlds.enabled` (true): false = no ritual begins; signed contracts stay. `devil.attribute.combatEffects` (true): false = the attribute is a label again, as before 3.76.0. `devil.attribute.effectScale` (1.0, 0 to 10). The master switch `devil.enabled` still turns the whole system off.

## Sources (rule 10)

The wiki pages themselves could not be opened from the build machine (the fandom hosts do not resolve there), so every canon claim below comes from web-search summaries of the English wikis and of articles that cite them; nothing was read on a Japanese wiki. Where a search returned nothing usable the thing was dropped.

- **Black Clover.** The Devil-Binding Ritual is forbidden; a devil is summoned, normally with a relic linked to it, and fought until it submits; it is the basis of the Devil Union; Asta asked Liebe for an equal contract. Devils and magic: Lucifero Gravity, Beelzebub Spatial, Astaroth Time, Megicula Curse-Warding, Lucifugus Kotodama, Liebe Anti-Magic. (comicbook.com on chapters 266 to 267, epicdope.com, animehunch.com, sportskeeda.com devil rankings.) Design: the four blows stand in for the fight (the devil package spawns no entity).
- **Chainsaw Man.** Contracts are paid in flesh, limbs, organs or lifespan; the Curse Devil takes years of life; the Future Devil lives in Aki's right eye; the Ghost Devil took Himeno's right eye; Aki feeds the Fox Devil parts of his body and calls it with "Kon". (twinfinite.net "How contracts work", sportskeeda.com on Aki's contracts.) The exact part Aki gives the Fox Devil was not found: two hearts is this mod's choice.
- **Black Butler.** Ciel summoned the demon in despair while held captive; the seal was branded on his right eye; the price is his soul when the contract is fulfilled. (Kuroshitsuji wiki chapter 138 summary, Wikipedia.) Design: the soul is taken as experience levels on every death.
- **Blue Exorcist.** A Tamer summons with a circle (on the ground, on paper or as a tattoo), a drop of blood and whatever appeal comes to mind; the familiar needs a strong will or it turns on its summoner; it vanishes if the circle is broken or the summoner loses consciousness. The eight Demon Kings: Lucifer Light, Samael Time, Azazel Spirits, Iblis Fire, Egyn Water, Astaroth Rot, Amaimon Earth, Beelzebub Insects. (Ao no Exorcist wiki pages Meister and Magic, gamerant.com, fictionhorizon.com.) The "kin of" devils are this mod's invention: a Tamer does not summon a King.
- **Tensura.** The seven Primordials and their colours (Noir, Blanc, Jaune, Violet, Rouge, Vert, Bleu); they offer loyalty in exchange for bodies; Rimuru summoned Noir after the Falmuth army's massacre and named him. (Tensura wiki Daemon, Testarossa, Rain pages.) The "daemon of a line" devils and their attributes are this mod's design.
- **Dororo.** Daigo Kagemitsu made a pact with the demons of the Hall of Hell (48 in the manga, 12 in the 2019 anime) for the prosperity of his land, paid with his unborn son's body parts. (Wikipedia Dororo, Dororo wiki Episode 1 and Hyakkimaru.) Design: the price falls on the caller.
- **Berserk.** A Behelit wakes when its bearer reaches the deepest despair, opens the Eclipse and summons the God Hand, who offer the bearer's wish for a sacrifice of what is most precious; the Brand of Sacrifice draws demons to those who bear it. (Berserk wiki Eclipse and Sacrificial Ceremony summaries, cbr.com.) Design: experience levels stand for "what is most precious"; the Brand is harder blows from monsters, not attraction (the devil package scans no world).
- **Fairy Tail.** The Etherious are Zeref's creations and can be summoned through his books; most of Tartaros are such demons. (Fairy Tail wiki Tartarus arc, Wikipedia.) Their "Curse" power could not be confirmed and is not claimed; the attribute is called Ether.
- **Dropped: High School DxD.** The search confirmed only that devils hand out flyers and form contracts; no ritual or price could be sourced, so it was not built.

## Cost (rule 11)

- A player with no ritual: one attachment lookup a tick (`DevilRituals.tick`), as cheap as the once-a-second lookup it joins.
- A running ritual (one per player at most, 4 to 10 seconds, a 60 second gap between two): a distance check a tick; at most 8 mesh effects in all (the last two are the devil itself: a shadow with burning eyes over its sigil, three blocks in front of the caller while the offer is open), one packet each to players within 96 blocks through `PerfBudgets.effectPacket`; 6 particles every 10 ticks through the particle budget; a few chat lines to the caller. The largest effect measured by the previewer is the rune wheel (736 of its 1200 vertex budget); the others are 48 to 296 vertices.
- The attribute effect: on the damage event, at most once a second per host: one multiplication, at most one mob effect, 8 particles.
- No entity is spawned, no block is changed, no world is scanned, nothing is kept in a static collection (`DevilSourceScanTest` pins all of this for the whole package). The ritual and the open offer live in the unsaved part of the contract attachment, so a logout or a death simply ends them.

## NEW / REPLACED / KEPT

NEW: `DevilWorld`, `WorldDevil`, `DevilRitualRules`, `DevilRitualRun`, `DevilRituals`, `DevilCombat`; ten attributes (Beast, Foresight, Ghost, Soul, Rot, Earth, Water, Light, Ether, Causality) and three sacrifices (Flesh, Soul debt, Branded), all appended; `/devil worlds | world | call | sign | refuse`; three settings; `DevilWorldsTest`; self-test `devil.worlds`. REPLACED: nothing. KEPT: the grimoire pact and its four devils, the Infernal Contracts, tiers, assimilation, the union, the markings, the old sacrifices and every 3.48.0 number.

## Not included, not seen

- **Nothing was seen in a game.** The summoning previews are single frames from the offline previewer; three effects (the devil circle, the plain magic circle and the Berserk zone) could not be rendered by it and are known only as existing effects of the mod.
- The devil appears as a shadow with burning eyes over a sigil (an existing effect in its colour), not as a modelled body. On signing, the signer wears the devil whole for three seconds (markings, horns, wings, the union glow). A devil entity with its own model would be a separate job.
- A world devil's contract cannot be ended by the player; an operator's `/devil clear <player>` forgets it (the sacrifices go with it).
- The HUD shows the tier and the attribute, not the devil's name (`/devil status` does).
- The balance of the prices against the tiers is a first proposal.
