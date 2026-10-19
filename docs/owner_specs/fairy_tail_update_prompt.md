**System Role & Objective**
You are helping extend the Multiverse of Anime mod (internal mod ID: `multiverse`) — a NeoForge 1.21.1 addon for Tensura: Reincarnated that already implements Black Clover content as Tensura skills.

Study how the existing Black Clover system works and match that style exactly:
- Skills are registered as Tensura Unique or Ultimate skills with multiple modes.
- Modes represent individual spells, techniques, or armor/ability sets.
- Skills use Tensura’s learning, mastery, Magicule/Aura costs, cooldowns, and EP systems.
- Strong visual effects (VFX) are expected — match the quality of the existing Time Magic / Fire / Water / Wind / Earth layers.
- Weapons, armors, and relics get Tensura engravings.
- Everything must be compatible with the existing skill book / mode system and the player-model kit.
- Follow the coding standards, version bumping rules, and VFX guidelines from the project’s CLAUDE.md.
- Mod version should be bumped appropriately (e.g. +0.1.0).
- Build target is NeoForge 1.21.1 + current Tensura: Reincarnated + ManasCore.

**Goal:** Add a large set of Fairy Tail abilities as Tensura skills, with substantial content.

### Special Requirements

**1. Erza Scarlet — Requip: The Knight (Ultimate Skill)**
Implement Erza’s Equip Magic as a full **Ultimate skill**.
Include as many of her major armors as practical. Each armor should be a distinct mode (or clear sub-mode) with unique buffs, resistances, attack modes, and abilities. 
**IMPORTANT:** The Equip Magic armor swaps MUST utilize the exact same armor equip animation sequence used by the Djinn Full Equip system. 

**Core Armors to Include:**
- Heart Kreuz Armor (balanced all-rounder)
- Heaven’s Wheel Armor (many floating swords, high mobility / flight-like movement, multi-target attacks)
- Flame Empress Armor (strong fire attacks + fire resistance)
- Lightning Empress Armor (lightning attacks + speed boost)
- Sea Empress Armor (water attacks + underwater bonuses)
- Black Wing Armor (high speed, wind/dark attacks, flight)
- Clear Heart Clothing (very high speed and attack speed, lower defense)
- Nakagami Armor (anti-magic / barrier-piercing attacks, high Magicule cost, strong against magic users)
- Heaven’s Wheel Armor: Grand Chariot (upgraded version with stronger sword rain)
- Adamantine Armor (high defense + reflecting attacks)
- Purgatory Armor (high damage, self-damage or high cost tradeoff)
- Flight Armor / Wind God Armor variants if space allows
- Fairy Armor / other late-series armors if feasible

**Behavior Requirements:**
- Switching armors costs Magicule/Aura and has a short cooldown or animation (using the Djinn Full Equip animation sequence).
- Each active armor dynamically applies NeoForge `AttributeModifier`s (movement speed, attack damage, defense) and specific special abilities.
- Include sword summoning and telekinesis-style attacks (especially on Heaven’s Wheel).
- VFX for armor swap (flash of light + particles matching the armor’s theme).
- Make it an Ultimate skill only (high EP requirement or mastery of a related Unique recommended).

**2. Lucy Heartfilia — Celestial Spirit Magic + Spirit King**
- Implement Celestial Spirit Magic as a Unique skill.
- **Celestial Keys are physical items.**
  - Gold Keys for all 12 Zodiac spirits: Aquarius, Taurus, Leo, Virgo, Sagittarius, Gemini, Cancer, Scorpio, Capricorn, Pisces, Aries, Libra.
  - Optional Silver Keys for weaker spirits.
- The skill allows summoning a spirit only if the player has the corresponding key (in inventory or a dedicated Celestial Key inventory/slot).
- Star Dress forms as advanced modes or mastery unlocks.

**Spirit King System:**
- When a player collects all 12 Zodiac Gold Keys, they unlock the ability to challenge the Celestial Spirit King.
- This should be a meaningful encounter:
  - Option A: A boss fight against the Spirit King (high difficulty, unique mechanics).
  - Option B: A powerful temporary summon of the Spirit King with a long cooldown and high cost.
  - Option C: A special Ultimate-tier mode unlocked only after defeating or earning the Spirit King’s recognition.
- Rewards should feel significant (permanent buff, unique item, skill upgrade, or large EP/Magicule reward).
- Strong VFX for opening the Celestial Gate and the Spirit King’s appearance.
- Track key collection (server-side or capability) so the system knows when all 12 are owned.

### Other Fairy Tail Skills
Also add substantial content for:
- Fire Dragon Slayer Magic (Natsu) — Unique → Ultimate evolution
- Ice-Make (Gray)
- Sky Dragon Slayer Magic (Wendy)
- Lightning Dragon Slayer Magic (Laxus)
- Iron Dragon Slayer Magic (Gajeel)
- Crash (Gildarts)
- Water Magic (Juvia)

**Execution Steps**
Please generate the codebase modularly. Start with:
1.  The `multiverse` Skill Registry class defining Erza's Ultimate Skill and Lucy's Unique Skill.
2.  The complete class for `RequipTheKnightUltimate.java`, detailing the `AttributeModifier` map for swapping between Erza's armors and the hook linking it to the Djinn Full Equip animation.
3.  The Item Registry for the 12 Zodiac Keys and the logic block checking the player's inventory to trigger the Spirit King boss spawn.

Do not skip any Java logic. Use proper NeoForge 1.21.1 event busses and Tensura capability getters.