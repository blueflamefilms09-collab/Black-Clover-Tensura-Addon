# Multiverse of Anime in Tensura

A huge **add-on for Tensura: Reincarnated** that brings anime powers, bosses, dungeons and gear from many series into your world. Skills grow from **Unique** to **Ultimate**, magic comes from **grimoires**, and a living **Mega-Dungeon** gives you somewhere to use it all.

> **Beta.** This mod is very large and has been built and tested by script, not played through end to end. Expect rough edges and please report them (see *Known issues*).

## Requires (NeoForge 1.21.1, Java 21)

- **Tensura: Reincarnated** 2.0.1.0 or newer
- **ManasCore** 4.0.0.2 or newer
- Tensura's own libraries (Architectury API, GeckoLib, SmartBrainLib, TerraBlender): they come with Tensura
- Optional: **Curios API** (relics and weapons fit Curios slots), **Better Combat** (weapon attack data)

Needs to be installed on **both the client and the server**.

## What is inside

- **Unique to Ultimate skills.** Master a Unique skill and it evolves into an Ultimate. Summoner, Faith, Argonaut, Hell Walker, Unbreakable Will, Devil's Footprints (Adolla Burst), Book Maker (All Fiction) and dozens more from Fairy Tail, DanMachi, Tower of God, God of High School and others.
- **New Ultimates with ten modes each:** Sandalphon, Israfil, Jibril, Solomon and Hercules, plus **Nen** (Hunter x Hunter), **Gate of Babylon** (Fate) and **Ki** (Dragon Ball). **All Fiction** was rebuilt to ten modes.
- **Grimoires and magic.** Black Clover style grimoires with many magic attributes, character pages, mode armour overlays, spells with their own effects, and the Genesis Demon-Slayer weapon.
- **Devil Contract (3.48.0).** Sign a devil's price and let it in with the key `N`: assimilation 0 to 100 %, a magicule drain that grows with the level, **horns from 20 %**, **wings and flight from 50 %**, Weg markings by tier, and a strict **five minute Devil Union** at 100 % followed by a 24 hour lock. Prices that last through death: half your maximum health, or an eye.
- **Beast Will.** Bind the will of a mana beast (Dragon, Shadow Panther, Elderwood Guardian, Lion) with assimilation, mastery and strain.
- **Mega-Dungeon.** A floor-by-floor dungeon with a System that guides you, parties, themes, hidden vaults, relics and a **Tier List of bosses** with their own anime powers.
- **Relics, weapons and companions.** Relics of many worlds worn in Curios slots, the Shield Hero Holy and Vassal weapons, the Millennium Items, Djinn, and an **Avatar** companion made at the Soul Forge.
- **Server friendly.** All rules are decided on the server, per-player state is stored per UUID, effects go to nearby players only and run through performance budgets. Gameplay options live in **server config** files, visual ones in **client config** files.

## Useful commands and keys

- Devil Contract: key `N` (rebindable), `/devil status`, `/devilpact accept <price>`. Beast Will: `/beastwill status`.
- Grant a skill: `/tensura edit <player> ability grant nusmp:<skill>` (for example `nusmp:nen`, `nusmp:ki`, `nusmp:gate_of_babylon`).
- Config files (in `config/`): `nusmp-devil-server.toml`, `nusmp-devil-client.toml`, `nusmp-beastwill-server.toml` and more; every gameplay feature has a setting that turns it off or restores the old behaviour.

## Installing

1. Install NeoForge for Minecraft 1.21.1, then Tensura: Reincarnated and ManasCore with their libraries.
2. Put `multiverse-of-anime-<version>.jar` in the `mods` folder (client **and** server). **Delete any older `multiverse-of-anime-*.jar` first.**
3. Old worlds keep working; removed skills stay registered as harmless stubs so saves load.

## Known issues (3.48.0)

- Not yet seen end to end in a running game: the Devil Contract wing and horn shapes, how the Weg markings sit on every skin, and the balance of many numbers may need tuning.
- The devil attribute is not wired to combat yet, your own first-person arm is not marked, and changing dimension does not end the Devil Union.
- The Devil key `N` shares its default with the Hero weapon cycle key; rebind one in Controls.
- Much of the art and models are generated placeholders that can be replaced file for file.
- Voice lines were generated with AI voices.

## Credits and notice

This is an unofficial fan-made add-on. All series, characters and names mentioned (Tensura, Black Clover, DanMachi, Fire Force, Jujutsu Kaisen, JoJo, Hunter x Hunter, Fate, Dragon Ball and others) belong to their respective owners and are used here as fan references only. Not affiliated with or endorsed by any of them. Requires and builds on Tensura: Reincarnated and ManasCore by their authors.
