# Summoner of Beginnings: real anime summons with their own models (plan, nothing built yet)

Owner request: every ability of the Summoner skill should summon a custom-modelled monster from the actual anime (Hell Mode, Allen), the skill should be very overpowered as an Ultimate, and a Unique version too.
Sources: Hell Mode wiki (English, hell-mode.fandom.com, read through its public api.php; the Japanese wiki was not needed). Summon pages: type, rank, abilities. The wiki pages give no usable written description of the looks (only a size for two insects), so the models need the owner's reference pictures.

## What canon gives us
* 43 named summons in 9 families: Insect, Beast, Bird, Grass, Stone, Fish, Spirit, Dragon, Angel (one angel, Merus). Ranks H to A (S exists in theory).
* Summons of one family and rank look alike. Ranks buff two stats (+1 at H up to +20 at D and above in the wiki table).
* Class tiers (Summons page, chapter 84): **Soldier** (all stats +2,500, awakened skills every 6 h), **General** (+5,000, 3 h, at most three per species, larger body, turns same-species summons into Soldiers), **King** (+10,000, 1 h, one per species, turns summons into Generals). Skills **Kingnification** and **Command** make them. This is the canon way to make the skill overpowered.
* Synthesis tree: Bird = Insect + Beast, Grass = Insect + Bird, Stone = Bird + Grass, Fish = Grass + Stone, Spirit = Bird + Fish, Dragon = Spirit + Stone (70 MP).

## The 43 summons (type, rank, key ability), from the wiki
Insect: Denka H (Swarm), Hopper G (Provoke), Sucker F (Suction), Ageha E (Scale Powder), Spidey D (Spider Silk), Pion C (Paralyze Needle), Alipon B (Acid, Oviposition), Hatch A (Needle work, Egg laying).
Beast: Mousey H (Proliferate), Moley G (Dig), Pochi F (Loyal Dog), Tama E (Borrowed cat), Teddy D, Bacon C (Charge), Cerorin B (3 / 9 Bite Combo, the three-headed hound), Hayate A (Gale Attack, Ground Shrink, Instant Wolf Kill).
Bird: Chappy G (Voice Mimic), Poppo F (Transmission), Hawk E (Hawk Eye, Far Sight), Horo D (White Night), Fran C (Breakthrough), Griff B (Soaring, Skyream), Tsubame A (Nesting, Nest Building).
Grass: Appo F (Aroma), Mametaro E, Tater D (Seed of Magic), Mapo C (Garnish, Potherb), Peachy B (Blessing of Earth / Heaven), Solarin A (Silver Beans).
Stone: Wally E, Bron D (Protect), Steely C (Substitute, Sacrifice), Mira B (Reflect, Total Reflect), Rocanel A (Absorption).
Fish: Belly D (Splash, Disperse), Finny C (Shark Oil, Shark Skin), Genbu B (Turtle Shield, Turtle Barrier), Tacos A (Smoke Bomb, Camouflage).
Spirit: Maria C (Psychokinesis), Ellie B (Gravity, Black Hole), Okiyosan A (5-Inch Nail, Ghost, Curse of the Dead).
Dragon: Dradra B (Spitting Fire, The Fire of Rage). Angel: Merus A (Attribute Granting, Angel's Halo).

## How it would be built (each step behind a setting that restores today's skill)
1. One summon entity (`SummonBeast`, one entity type, the species in a synced id) with GeckoLib models and animations (idle, walk, attack, ability), made as `tools/blockbench/summoner/<name>.bbmodel` with the game files exported from it. Blender and Blockbench cannot run in the cloud session, so these would be script-written (the same fallback as `tools/blockbench/devil_trial.bbmodel`) and reported as such, never as app-made.
2. Each mode of the skill summons its family's signature creature instead of a wolf or an iron golem: Beast Card = Cerorin / Hayate, Stone Card = Rocanel / Mira, Bird Card = Griff / Tsubame, Spirit = Okiyosan / Ellie, plus Insect, Grass, Fish, Dragon, Angel cards. Synthesis climbs the family ranks as in the anime.
3. Abilities of the summons are their canon abilities, bounded (caps per player, cooldowns, cleanup on death, logout and dimension change).
4. Unique Summoner: Soldiers (+2,500 stats) and a higher cap. Ultimate Summoner of Beginnings: Kingnification and Command, Generals and Kings, all families open from the start, twice the cap, the awakened skills on the canon shortened cooldowns.
5. Server rules (rule 9, 11): summons are server entities owned by a player UUID, hard caps per player and per server, no per-tick world scans, AI goals reused, models drawn only for nearby viewers with a quad budget, one set of NBT keys.

## What I need from the owner
Reference pictures (anime screenshots or art) of at least the signature summons: Cerorin, Hayate, Griff, Tsubame, Hatch, Rocanel, Genbu, Okiyosan, Ellie, Dradra, Merus, Solarin. Any others are welcome; without a picture a model is my guess from the wiki's words only, and it will look generic.
Decisions: how many species to build in the first release (suggested: the 12 above), and whether Soldier / General / King should be the main "overpowered" lever (suggested yes).

## Not built
Nothing in this file is implemented yet.
