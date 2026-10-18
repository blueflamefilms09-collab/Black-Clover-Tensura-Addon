# Hero weapons (1.62.0): the weapons of The Rising of the Shield Hero

This is the BINDING design of the whole job. Every builder reads it first; where a builder's brief and this file disagree, this file wins and the builder
says so in the report. The code of the contract (the catalogue, the bond data, the skill API, the item registration, the stub entry points) is in
`src/main/java/com/newuniverse/nusmp/heroweapon/**` and `.../client/heroweapon/**`; read it before this text.

## 0. What the owner asked for, and new versus replacement

The owner's request (2026-10-18), kept as the specification:

* **The Four Holy Weapons (Cardinal Weapons)**, summoned from other worlds against the Waves of Calamity:
  * *Legendary Shield* (Naofumi Iwatani): absolute defence, party support and counter-attacks; it cannot deal direct damage easily on its own, so the wielder
    relies on absorbing materials, on specialised forms (the Wrath / Curse Series, the Soul Eater Shield) and on allies for offence.
  * *Legendary Sword* (Ren Amaki): high-speed physical damage and agility, heavy offensive strikes, later the curse series.
  * *Legendary Spear* (Motoyasu Kitamura): long-reach piercing attacks, high mobility, and a unique time-reversal property tied to its lore.
  * *Legendary Bow* (Itsuki Kawasumi): ranged artillery, multi-shot, tactical versatility.
* **The Seven Star / Vassal Weapons**, which serve alongside or beneath the Holy Weapons and protect the worlds. They can be chosen by native residents or
  summoned people, and their holders do **not** suffer the anti-party experience penalty of the four Holy heroes. Named by the owner: Vassal Scythe (L'Arc
  Berg), Vassal Fan (Glass), Vassal Carriage (historically Fitoaria); the other types: Axe, Hammer, Gauntlet, Claw, Whip, Staff, Projectile (throwing
  weapon), Katana, Mirror, Musical Instrument, Harpoon, Sewing Set, Chain, Book.
* **Mechanics**:
  1. *Absorption and crafting*: weapons absorb raw materials, monster parts or items into a central gem to unlock new specialised forms, stats and skills.
  2. *Curse Series*: unlocked when the wielder suffers extreme emotional trauma or despair; abilities tied to the seven deadly sins; a heavy physical or
     mental cost.
  3. *Restrictions*: Holy Weapons reject non-class weapons, are permanently bound to the user's body, and prevent the heroes from gaining experience when
     they stand too close to one another.

**New or replacement (CLAUDE.md rule 5).** Nothing like this exists in the mod: a search for Shield Hero, Naofumi, legendary shield, vassal, cardinal and
curse series finds only the relic cards "Hero's Shield" and "Aegis of the Abyss" (D&amp;D-style relic cards of the Relics of Many Worlds, unrelated items) and the
Tensura engravings "holy weapon". So the whole job is **NEW**; nothing is replaced and nothing is removed. The only existing things it touches are the shared
registries (creative tab, weapon renderer list, command tree, smoke prefixes, engravings table, lang), and only by appending.

## 1. Canon used (fandom wiki search summaries; the pages themselves could not be opened from the build machine, so every canon claim is "from a summary")

* Vassal Weapons: https://shield-hero.fandom.com/wiki/Vassal_Weapons . Raphtalia's world has the Seven Star Weapons (hammer, claw, axe, whip, staff, gauntlet,
  projectile) and the carriage (often not counted: "seven known weapons that grant hero status"); Glass' world has fan, scythe, katana, book, mirror, musical
  instrument, harpoon (and a boat, which the owner did not list); S'yne's world has the sewing set and the chain.
* Legendary Weapons: https://shield-hero.fandom.com/wiki/Legendary_Weapons . Holders cannot gain experience when close to each other (the weapons interfere);
  holders can handle other weapons but are shocked when they try to fight with one; a bound hero cannot switch to a weapon outside the bound weapon's class.
  Power-up methods: https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods .
* Curse Series: https://shield-hero.fandom.com/wiki/Curse_Series and `/Weapon_List`. Unlocked by an extreme psychological stimulus (a defence mechanism of the weapon).
  Shield = Wrath (called Rage); downsides: self-burning curse, loss of all SP, bleeding, or -30% to all stats except defence, by ability. Sword (Ren) = Greed and
  Gluttony (unlocked on discovering a betrayal). Spear (Motoyasu) = Lust. Bow (Itsuki) = Pride. Vassal weapon holders can also use cursed weapons. The Bless
  Series (the Shield of Compassion) suppresses the curse. Envy and Sloth: no Holy Weapon found for them; by design they are given to two vassal weapons.

## 2. The catalogue (the `HeroKind` enum; entries are only appended)

| enum | item id | name | class | style | canon holder / sins |
|---|---|---|---|---|---|
| SHIELD | `legendary_shield` | Legendary Shield | HOLY | BLOCK | Naofumi Iwatani; Wrath |
| SWORD | `legendary_sword` | Legendary Sword | HOLY | SWING | Ren Amaki; Greed, Gluttony |
| SPEAR | `legendary_spear` | Legendary Spear | HOLY | SWING | Motoyasu Kitamura; Lust |
| BOW | `legendary_bow` | Legendary Bow | HOLY | DRAW | Itsuki Kawasumi; Pride |
| HAMMER | `vassal_hammer` | Vassal Hammer | SEVEN_STAR | SWING | |
| CLAW | `vassal_claw` | Vassal Claw | SEVEN_STAR | SWING | |
| AXE | `vassal_axe` | Vassal Axe | SEVEN_STAR | SWING | |
| WHIP | `vassal_whip` | Vassal Whip | SEVEN_STAR | SWING | |
| STAFF | `vassal_staff` | Vassal Staff | SEVEN_STAR | CAST | |
| GAUNTLET | `vassal_gauntlet` | Vassal Gauntlet | SEVEN_STAR | SWING | |
| PROJECTILE | `vassal_projectile` | Vassal Projectile | SEVEN_STAR | THROW | (a throwing weapon) |
| CARRIAGE | `vassal_carriage` | Vassal Carriage | VASSAL | RIDE | Fitoaria |
| FAN | `vassal_fan` | Vassal Fan | VASSAL | SWING | Glass |
| SCYTHE | `vassal_scythe` | Vassal Scythe | VASSAL | SWING | L'Arc Berg |
| KATANA | `vassal_katana` | Vassal Katana | VASSAL | SWING | |
| BOOK | `vassal_book` | Vassal Book | VASSAL | CAST | |
| MIRROR | `vassal_mirror` | Vassal Mirror | VASSAL | CAST | (by design: Envy) |
| INSTRUMENT | `vassal_instrument` | Vassal Musical Instrument | VASSAL | CAST | |
| HARPOON | `vassal_harpoon` | Vassal Harpoon | VASSAL | THROW | |
| SEWING_SET | `vassal_sewing_set` | Vassal Sewing Set | VASSAL | CAST | |
| CHAIN | `vassal_chain` | Vassal Chain | VASSAL | SWING | (by design: Sloth) |

The vassal weapons' item ids all say `vassal_`; the Seven Star ones are a CLASS (`SEVEN_STAR`), shown in the tooltip, because the wiki titles them "Vassal Hammer",
"Vassal Whip" and so on. The Boat of Glass' world is not in the owner's list and is not built. The enum is the only list of weapons: never write one out by hand.

## 3. Architecture and who owns what

Package `com.newuniverse.nusmp.heroweapon` (server and shared), `com.newuniverse.nusmp.client.heroweapon` (client). One entry point, `HeroWeaponSystem.init`, called
from the mod constructor (already wired by the lead), starts everything in a fixed order: items, data components and the bond attachment on the mod bus; the
weapon logics (`HeroBootstrap`); then `HeroCore.init`, `FormsSystem.init`, `CurseSystem.init`, `GemSystem.init`. Each of those four registers its OWN config file
and its own events, payloads and self-tests, so the shared start-up never changes. Client: `HeroClient.init` (starts `FormsClient`, `CurseClient`).

| builder key | owns (create or replace these; nobody else edits them) |
|---|---|
| `hcore` | `HeroCore`, `HeroCommand`, `HeroWeaponItem`, `HeroItems` (properties only), `skill/HeroUse`, `rules/**`, `bond/*Loan*`/sync/net, the config `nusmp-heroweapon-server.toml`, `client/heroweapon/HeroClient` + HUD + keybinds, the renderer hook for the look (`WeaponRenderer`: choose `<id>_curse` when the stack's `nusmp:hero_look` is `curse`), docs/heroweapon/core.md |
| `hforms` | `forms/**` (replacing `HeroForms`, `FormsSystem`, `FormsCommands`), the loader of `data/nusmp/hero/forms/*.json`, absorption, the Weapon Book (screen, packets, key), mastery, bonuses, `client/heroweapon/forms/**`, config `nusmp-heroweapon-forms-server.toml`, docs/heroweapon/forms.md |
| `hcurse` | `curse/**` (replacing `HeroCurse`, `CurseSystem`, `CurseCommands`), `client/heroweapon/curse/**`, config `nusmp-heroweapon-curse-server.toml`, docs/heroweapon/curse.md |
| `hgem` | `gem/**` (replacing `GemSystem`, `GemCommands`), the Calling Gem items, the ritual, recipes, config `nusmp-heroweapon-gem-server.toml`, docs/heroweapon/gem.md |
| `hsa` | `weapon/shield/**`, `weapon/sword/**`, the forms files `legendary_shield.json`, `legendary_sword.json`, docs |
| `hsb` | `weapon/spear/**`, `weapon/bow/**`, `legendary_spear.json`, `legendary_bow.json`, docs |
| `hva` | `weapon/hammer`, `claw`, `axe`, `whip` (+ their forms files, docs) |
| `hvb` | `weapon/staff`, `gauntlet`, `projectile`, `carriage` (+ the Carriage entity and its renderer under `heroweapon/weapon/carriage` and `client/heroweapon/carriage`) |
| `hvc` | `weapon/fan`, `scythe`, `katana`, `book` |
| `hvd` | `weapon/mirror`, `instrument`, `harpoon`, `sewingset`, `chain` |
| `hart1`..`hart4` | `tools/heroweapon/art_<key>.py`, the meshes, textures, item models and icons of their weapons (section 11) |
| the lead | everything else: `HeroKind`, `HeroClass`, `HeroStyle`, `HeroTrigger`, `Sin`, `HeroBootstrap`, `HeroWeaponSystem`, `HeroSelfTests`, `skill/HeroSkill`, `HeroLogic`, `HeroLogics`, `HeroCtx`, `bond/HeroBond`, `HeroBonds`, `HeroLevels`, `forms/HeroForm`, `HeroComponents`, `WeaponEngravings`, `NUSMP`, `NUCreativeTab`, `MultiverseCommands`, `WeaponRenderer.WEAPONS`, `weapon_classes.json`, `en_us.json`, `smoke.yml`, version files, README, CHANGELOG |

If a builder needs a change in a lead-owned class it does NOT edit it; it states the exact change in the report ("LEAD INTEGRATION"). Two exceptions that keep builders unblocked:
`HeroBond.extra(<system>)` is the open area for a system's own saved state (keys `loan` for the core, `curse` for the curse, `book` and `gem` for their systems), and
the weapon logics find everything through `HeroCtx`.

## 4. The bond (`bond/HeroBond`, attachment `nusmp:hero_bond`)

The bond is the TRUTH; the item in the hand is a projection. It holds: the weapon (`HeroKind` or none), the experience (the level is derived), the active form id,
the forms learned, the mastery of each, the forms whose permanent bonus was given, the absorbed items (item id -> count), whether the weapon is summoned, and the
per-system `extra` compounds. It is saved with the player, copied to the new body on death, empty (and not saved) for anyone without a weapon. `HeroBonds.bind(player,
kind)` / `release(player)` are the only ways to bind or release (gates veto, listeners react). **One bond per player** by default (core config `maxBonds`, default 1);
**one holder per Holy Weapon** by default (core config `holyUnique`, default true; kept in a world data file `nusmp_hero_registry.dat` by the core, released when the
holder releases); Seven Star and Vassal weapons may be shared by default (`vassalUnique` false).

Level and power (`HeroLevels`): experience to reach level L is `40 * L^1.7`, level 0..100; power = `1 + 0.012 * level` (1.0 to 2.2); cooldown scale = `1 - 0.0025 * level`
(1.0 to 0.75). Every skill multiplies its raw numbers by `HeroCtx.power` (done for you by `HeroCtx.hit`). Experience is gained ONLY through `HeroXp.gain` (so the
interference rule can refuse it): +1 per melee hit (+3 on a full swing), +2 per skill hit, +kill experience (victim max health / 4, bosses x5), +mastery steps.

## 5. Skills, triggers, the dispatcher

* A form lists up to five techniques by input slot: `use` (right click), `sneak` (sneak + right click), `key1`, `key2`, `key3` (three hero keys). A BLOCK-style
  weapon (the Shield) does not have `use` (the use key raises the guard, as a vanilla shield); its skills sit on `sneak` and the keys.
* A skill is an object (`HeroSkill`): id `<kind>.<name>` (the `kind` is the enum name lower-case: `shield.air_strike`, `sewing_set.mend_thread`), name, description, cooldown, cost
  (share of the wielder's maximum magicules), optional wind-up, `cast(HeroCtx)`. The dispatcher (`HeroUse.fire`, core) checks the weapon is summoned and in the main
  hand, the cooldown (per player per skill, game-time based, shortened by the level), pays the cost (`HeroCtx.pay`), waits out the wind-up, casts, and only when
  `cast` returns true starts the cooldown; a refused cast costs nothing. A skill never starts its own cooldown, never pays its own cost.
* Hooks of the logic (`HeroLogic`): `onTick` (every 5 ticks while summoned), `onHit`, `onDamaged`, `onBlocked`, `onKill`, `onRelease` (drawn / charged use), `onFormChanged`, `onSummon`,
  `onDismiss`. Passives of a form are ids in the forms file; the logic checks `c.form.has("id")`.
* **Balance budget (binding)**: all damage goes through `HeroCtx.hit` (the balance law: EP scaling, the 45% pvp cap, the boss multiplier). Raw base damage per hit:
  ordinary technique 3..12, heavy 10..20, ultimate at most 32 (before the 1.0..2.2 level factor). At most 12 targets per technique. Cooldown: damage techniques at least
  60 ticks, utility at least 100, an ultimate at least 600. Cost: 1..3% of the maximum magicules, an ultimate 5..8%. Healing only through `HeroCtx.heal` (the law caps it; at
  most 25% of the target's max health per technique). Crowd control only after `HeroCtx.control()` (the shared 12 s lock), at most 3 s on a player and 6 s on a mob, never on a boss
  beyond a stagger. No technique may break blocks outside the Mega-Dungeon guard rules (use none), spawn more than 6 entities, or keep a repeating task alive longer than 20 s.
  Effects: only `VfxSpawn` shapes (the `VfxShape` enum has 238), `Tells`, sounds; never vanilla `ParticleTypes` for abilities. Never edit the Time Magic VFX (CLAUDE.md rule 3).
* Every technique must be readable in play: a visible windup or tell for anything above 8 raw damage, and a way for an opponent to answer it.

## 6. Forms: the data (`data/nusmp/hero/forms/<itemId>.json`) and the rules

One file per weapon, owned by the weapon's logic builder. Schema (the record is `forms/HeroForm`; unknown keys are ignored, a malformed form is skipped and logged, a file
that cannot be parsed leaves the weapon with its base form only; the forms builder writes the loader and the validation test):

```json
{
  "weapon": "legendary_shield",
  "forms": [
    { "id": "base", "name": "Small Shield", "desc": "...", "tier": 0, "parents": [], "requires": [], "mastery": 0,
      "stats": { "armor": 2.0 }, "abilityBonus": {}, "skills": { "sneak": "shield.air_strike", "key1": "shield.prison" },
      "passives": ["counter_ready"], "look": "base", "curse": false, "sin": null, "corruptionPerSecond": 0 },
    { "id": "leaf_shield", "name": "Leaf Shield", "desc": "...", "tier": 1, "parents": ["base"],
      "requires": [ { "tag": "minecraft:leaves", "count": 32 }, { "item": "minecraft:glow_berries", "count": 8 } ],
      "mastery": 120, "stats": { "armor": 3.0, "movement_speed": 0.04 }, "abilityBonus": { "armor": 0.5 },
      "skills": { "sneak": "shield.air_strike", "key1": "shield.leaf_cover" }, "passives": [], "look": "base" }
  ]
}
```

* The FIRST form is the base form: id `base`, tier 0, no parents, no requirements, always learned. Its `name` is the weapon's starting shape ("Small Shield").
* `requires`: one to four entries of `{ "item": "<registry id>", "count": n }` or `{ "tag": "<item tag>", "count": n }`, `n` in 4..64 (an entry above 64 is allowed for a top form up to
  256). Every item id must exist (vanilla, `tensura:` or `nusmp:`; check against the id dumps `bash $S/apigrep.sh`; a test resolves them); tags must be vanilla or `c:`/`nusmp:` tags that
  exist. Tier 1 forms ask for common things (leaves, leather, iron, bones), tier 2 for rarer (blaze rods, ender pearls, Tensura monster parts), the top forms for rare (Tensura
  materials, nether star, dragon parts). Nothing may need an item that cannot be obtained in survival.
* `parents`: all must be learned first. Depth up to tier 5. Holy Weapons: 8 to 10 ordinary forms; Seven Star and Vassal weapons: 3 to 5 forms. Curse forms are extra (section 7).
* `mastery`: 40..400 mastery points (a point is a hit, two for a technique, one for a successful block with the form active) to finish the form: finishing grants the
  permanent `abilityBonus` once (applied while the bond exists, summoned or not).
* `stats` / `abilityBonus` keys are the closed set `HeroForm.STAT_KEYS`: `attack_damage`, `attack_speed`, `armor`, `armor_toughness`, `max_health`, `movement_speed` (a fraction:
  0.05 is +5%), `knockback_resistance`, `reach`, `luck`. Budget per form (stats): armor <= 8, armor_toughness <= 4, max_health <= 12, attack_damage <= 14, attack_speed in -3.0..+1.0,
  movement_speed <= 0.2, knockback_resistance <= 0.6, reach <= 2.0, luck <= 3. `abilityBonus` is at most a quarter of that.
* `skills`: slot word to skill id; at most one skill per slot, at most one ultimate per form; every id must exist in the weapon's logic (`HeroLogics.skill`), and a skill of another weapon is refused.
* `passives`: free ids the logic implements; the Weapon Book shows them by name from the lang fragment key `hero.passive.<kind>.<id>`.
* `look`: `base` or `curse`. A form with `curse: true` uses `curse` (the Holy Weapons have a `_curse` mesh and skins; the vassals have no curse look, so their curse forms keep `base` and
  the glow only differs in the corruption HUD).
* **Absorption**: the gem takes items out of the wielder's inventory into `bond.absorbed` (item id to count). A form is AVAILABLE when all its parents are learned and every requirement
  is met by what was absorbed so far (a tag requirement counts every absorbed item in the tag; absorbed items are never consumed by unlocking, they only accumulate; this matches the
  series, where the weapon keeps what it ate). Learning an available form is free and automatic by default (forms config `autoLearn`, default true) or by the Weapon Book's button.
  Which items may be absorbed: those some form of the bonded weapon asks for (config `absorbAnything`, default false, lets the gem take anything for the count). How: (a) the Weapon Book's
  "Feed" buttons, (b) hold the weapon, hold the material in the OFF hand, sneak + right click (the core's item calls `HeroForms.absorbOffhand`; when it returns true the sneak technique is NOT fired): the whole off-hand stack is absorbed, (c) `/multiverse hero form absorb`.
* **Active form**: chosen in the Weapon Book, by the hero key "cycle form" (core keybind, calls the forms API) or `/multiverse hero form set`. Changing form is refused while the
  weapon is on cooldown from a technique (global 2 s lock) and inside a Curse Overrun. The forms system keeps the projection in the hand in step: attribute modifiers while summoned,
  the `nusmp:hero_look` component, the tooltip.
* The Weapon Book (forms builder): a screen opened with a key (default `K`, or a free key the builder verifies) and by `/multiverse hero form book`: the form tree, per form the
  requirements with progress bars, the stats, skills and passives, mastery, the curse state; "Feed", "Learn", "Use" buttons. It must work with the mod's GUI theme (`docs/gui_theme.md`).

## 7. The Curse Series (curse builder)

* Each Holy Weapon has its canon series (section 1); a vassal weapon may carry a series by design (Mirror = Envy, Chain = Sloth; the vassal builders may add one more only with a stated
  reason). A series is its forms in the weapon's file flagged `curse: true` with a `sin` and `corruptionPerSecond`.
* **Despair** (0..1000, per player, in `HeroBond.extra("curse")`): grows when something terrible happens to a bonded wielder: an ally dies within 32 blocks (a tamed animal, the wielder's
  Avatar, a party or FTB team member, an arc NPC friend), a betrayal (damage from an ally, a party member or one's own summon), a near-death (health below 15%, at most once per minute),
  a long defeat (dying while bonded), a lost fight to a boss. It decays slowly while the wielder is safe. The causes and numbers are in the curse doc and config.
* **Unlock**: at despair >= the threshold (config, default 600) the weapon speaks to the wielder (an action-bar line, a screen tint, a sound, the HUD shows "The weapon whispers ...").
  The wielder may ACCEPT with a deliberate action (the Weapon Book's Embrace button, or sneak + the hero key 3 held for 3 s, or `/multiverse hero curse embrace`); only then the series
  is unlocked, permanently, with a price taken once (a permanent tax: max health -4 hearts for the first series, -2 each further, never below 4 hearts, config) and the first curse form
  is learned. Declining lets the despair decay. Nothing ever unlocks a curse by itself.
* **Cost while a curse form is active**: corruption (0..1000) rises by `corruptionPerSecond` and by each technique; the form's own downside by sin (Wrath: burning of the wielder and loss of
  health per second, stronger blows; Pride: the wielder cannot take help (no healing from others) and their sight narrows; Lust: the wielder is drawn to the nearest foe; Greed: the wielder cannot stop
  collecting (drops are pulled in and every technique costs more); Gluttony: hunger drains fast and kills heal; Envy: the wielder copies and loses their own technique at random; Sloth: the wielder slows
  and techniques are delayed). Each downside is numerically bounded (config) and must never kill the wielder outright (health never drops below 1 heart from the curse itself).
* **Overrun**: at corruption 1000 the weapon takes over for 10..20 s: the wielder cannot dismiss or switch, attacks the nearest living thing (allies included), then the curse form ends, the
  corruption falls to 400 and the wielder is weakened for a minute. A death in an Overrun counts as a betrayal for the despair of nearby players.
* **Purification**: corruption falls by itself while no curse form is active; the Bless Series of the series is represented by an item (a purification charm, curse builder) and by resting at a
  Safe Zone of the Mega-Dungeon (the System's rest). The Bless forms themselves are out of scope (listed as a follow-up).
* A curse form can only be activated when the series is unlocked and the form learned; leaving it is always possible except in an Overrun.

## 8. Restrictions (core builder; all server rules, each with a config switch)

1. **Rejection** (class HOLY): a bonded Holy wielder who attacks with, or fires, a weapon-class item that is not of their weapon's class is shocked instead: the blow is cancelled, they take a
   small magic hit (2 hearts at most, never lethal) and are slowed for 1 s; one message per 3 s. Weapon classes: SWORD (swords, katanas, rapiers, this mod's sword kinds), AXE, SPEAR (tridents,
   spear items), BOW (bows, crossbows), SHIELD (vanilla and this mod's shields), BLUNT (maces, hammers, clubs), OTHER_WEAPON (anything with an attack damage modifier of 4 or more not above).
   A Holy wielder's own class is allowed (the Sword hero may use any sword, the Shield hero any shield); tools used as tools (pickaxe, shovel, hoe, axe on a block) are never affected; a
   Seven Star / Vassal weapon in the other hand is allowed. Items tagged `#nusmp:hero_allowed` are always allowed. Creative players are exempt.
2. **Bound to the body**: the weapon exists in the world only as a projection of the bond. It cannot be dropped, thrown out, stored in a chest or other container, put in an item frame, traded,
   or taken by another player; it never drops on death and the bond survives death; it is not lost to void, fire, cactus or a creeper. Anything that leaves the owner's inventory dissolves and
   the bond re-summons on request. The loan machine of the Djinn weapons (`relics/djinn/weapon/WeaponLoan`, a pure state machine tested with random events) is the model; the core may reuse it.
3. **Interference** (class HOLY, `HeroInterference.blocked`): while another Holy wielder is within the repel radius (config `repelRadius`, default 48 blocks, same dimension), neither gains
   experience: no vanilla orbs (`PlayerXpEvent.PickupXp` cancelled), no kill experience for a kill by them (`LivingExperienceDropEvent`), no weapon experience (`HeroXp.gain`), no System
   experience (the System's award goes through a gate the core wires: ask `HeroInterference.blocked(player)`). Both are told once ("The Legendary Weapons repel each other"). Seven Star and
   Vassal wielders neither suffer nor cause it. Config `repelXp` (default true) switches the rule.

## 9. Summon, dismiss, the projection

Key "Summon / put away hero weapon" (core, a free default key it verifies; also sneak + use with the weapon's stowed icon? no: the key and the command only). Summon puts the weapon in the SELECTED
hotbar slot (the item that stood there goes to the first free slot, remembered) exactly as the Djinn weapon does, with the components `nusmp:hero_owner` and `nusmp:hero_look`;
dismiss gives the displaced item back. The weapon is also summoned automatically by the Calling Gem's ritual. Unbreakable, no durability. Creative-tab or `/give` stacks are adopted by their
first holder if that holder has a free bond slot of that kind, else dissolve.

## 10. Acquisition (gem builder; defaults, to be confirmed by the owner)

* Always: `/multiverse hero gem give`, `/multiverse hero bind <player> <weapon>`, the creative tab.
* Survival: the **Calling Gem** (a crafted item, rare late-game recipe) used by holding right click for 5 s starts the **Choosing**: a ritual circle effect, then the nearest unbound Holy Weapon (or,
  for a **Vassal Seal**, the named weapon) binds to the user, with a title and the first form learned. A Holy Calling Gem picks among the Holy Weapons not held (random); a Vassal Seal is crafted
  per weapon from a Calling Gem plus that weapon's signature material (17 recipes). Nothing is lost on a refusal. Details, recipes and balance are the gem builder's, stated in its doc.
* Binding fires the quest hook `hero.bound` and a chat title in the canon style.

## 11. The art contract (four art builders; Genesis Demon-Slayer is the standard, CLAUDE.md rule 7)

For each weapon id `<id>` (the `itemId`) the art builder delivers, deterministically, from `tools/heroweapon/art_<key>.py`:

* `src/main/resources/assets/nusmp/weapon_meshes/<id>.txt`, `textures/entity/weapon/<id>.png`, `<id>_glow.png`: the 3D model in hand, its painted skin and its additive glow skin. A Holy Weapon
  also `weapon_meshes/<id>_curse.txt`, `<id>_curse.png`, `<id>_curse_glow.png`: the cursed look (the same weapon corrupted: spikes, flames, chains, red-black glow; the renderer picks it when the stack's
  `nusmp:hero_look` is `curse`).
* `models/item/<id>.json` (the in-hand model, `builtin/entity` with the display transforms), `models/item/<id>_icon.json` (inventory icon model, handheld) and `textures/item/<id>.png` (16x16 sprite, the
  inventory icon) exactly like `brooch_pin`.
* a fragment `tools/heroweapon/classes_<key>.json` mapping each id to a size class of `assets/nusmp/weapon_classes.json` (the lead merges it; add a new class only if no existing one fits, with the
  min/max length as the owner's rule "real-life sizes" requires).
* Method and tools: `tools/relics/mesh_lib.py` (the owner's model method: lofts, sweeps, lathes, named parts, painted materials, a glow skin; read `tools/relics/wave23_models.py` as the worked
  example and `docs/weapon_scale_orientation.md`, `docs/asset_pipeline.md`), the icon builder pattern of `tools/relics/wave23_icons.py`, the preview `python tools/item_preview/preview_weapons.py` and the mesh
  previews of `tools/relics/preview_mesh.py`. Orientation: mesh -x forward, tip up-forward on the icon's diagonal (bows and shields upright, see the wave 23 header). Budget: 8..14 named parts and at most 2800 quads
  per mesh.
* The look: every weapon has a **gem** (the series' central jewel, on the back of the hand in the show): a glowing cabochon in the grip, shield face, string-nock, hilt or head, in the weapon's colour, on the
  glow skin. Holy Weapons are noble and plain at the base look; their curse look is the same silhouette made monstrous. Vassal weapons are more ornate. Colours and silhouettes follow the series (research with
  WebSearch; say what is from memory).
* Every finished weapon is rendered, LOOKED AT, and corrected; the PNGs go to `$S/previews/<key>/` and are listed in the report. Nothing is claimed that was not seen.
* The Carriage is also drawn as an entity (the vehicle the Carriage summons): the `hvb` builder reuses the weapon's own mesh scaled up through the mesh loader (read-only use of `WeaponRenderer`'s loader).

`HeroAssetsTest` lists every missing file and fails until all of them exist.

## 12. Config files (plain `define` / `defineInRange` only, read through a `get` helper that falls back to the default while the spec is not loaded; copy `labyrinth/dnd/DndConfig.java`)

`nusmp-heroweapon-server.toml` (core: `enabled`, `maxBonds`, `holyUnique`, `vassalUnique`, `holyRejectsOthers`, `repelXp`, `repelRadius`, `weaponExpMultiplier`, `maxLevel`...), `nusmp-heroweapon-forms-server.toml` (forms: `autoLearn`,
`absorbAnything`, `masteryPerHit`, ...), `nusmp-heroweapon-curse-server.toml` (curse: thresholds, taxes, caps), `nusmp-heroweapon-gem-server.toml` (gem: the ritual time, the recipes switch). Client config only if a builder
needs it (`nusmp-heroweapon-client.toml`, core).

## 13. Commands (`/multiverse hero ...`, permission level 2 unless stated; the owners are named in section 3)

Core: `status [player]` (level 0, any player: self), `list`, `summon`/`dismiss` (any player: self), `bind <player> <weapon>`, `release <player>`, `level <player> set <n>`. Forms (`form`): `list [weapon]`, `learn <player> <form>`,
`set <player> <form>`, `absorb <player>`, `reset <player>`, `mastery <player> <form> <n>`. Curse (`curse`): `show [player]`, `despair <player> <n>`, `corruption <player> <n>`, `unlock <player> <sin>`, `clear <player>`,
`embrace` (self), `purify <player>`. Gem (`gem`): `give <player> <holy|seal> [weapon]`, `holders`.

## 14. Quest hooks (`NusmpEvents.fire(player, key, args...)`)

`hero.bound` (weapon), `hero.summoned` (weapon), `hero.form` (weapon, form), `hero.mastery` (weapon, form), `hero.level` (weapon, level), `hero.absorb` (weapon, item), `hero.curse` (weapon, sin), `hero.overrun`
(weapon), `hero.repelled`. Document any other key a builder adds; the lead wires the advancements.

## 15. Self-tests and unit tests (the CI server smoke test expects the prefix `hero`)

`hero.catalog` (the lead: 21 items, logics, skills, forms), core `hero.bond`, `hero.use`, `hero.rules`; forms `hero.forms`, `hero.absorb`; curse `hero.curse`; gem `hero.gem`; each weapon builder ONE test
`hero.weapon.<pkg>`: for each of its weapon's forms the skill ids resolve, and with a stand-in player bonded to the weapon (see `arc/ArcSelfTests` and `labyrinth/mega/SelfTests` for the FakePlayer pattern) every
skill is cast once next to a dummy mob and must not throw (a false return is fine); the test removes what it spawned and releases the bond. Pure logic (numbers, timers, tables, schema validation) lives in classes
without `net.minecraft` imports and has JUnit tests, mutation-checked once (run with `WT=<worktree> KEY=<key> bash $S/mega/jt.sh <class>`).

## 16. Engravings (CLAUDE.md rule 6)

Every weapon gets Tensura engravings that fit its lore, applied once by `item/WeaponEngravings` (the lead integrates the lines from each builder's report). Use only ids that exist in Tensura or this mod; the ids the
table already uses are: `severance`, `barrier_piercing`, `holy_weapon`, `magic_interference`, `swift`, `enervation`, `magic_weapon`, `crushing`, `sturdy`, `energy_steal`, `magicule_absorption`, `soul_eater`,
`elemental_boost`, `restoration`, `vitality`, `magic_protection` and `nusmp:anti_magic`, `nusmp:legacy_of_the_wizard_kings`. Format: `Map.entry("<itemId>", List.of(e("<id>", level), ...))`, 2 to 4 engravings. (A Shield:
magic_protection, sturdy, magic_interference; a Bow: swift, barrier_piercing...) The weapon builder gives the lines and one sentence of WHY per engraving.

## 17. Lang

Item names are already in `en_us.json` (`item.nusmp.<itemId>`). Everything else a builder writes goes into `$S/hero/frag/<key>.lang.json` (a flat JSON object, keys such as `hero.skill.<id>.name`,
`hero.skill.<id>.desc`, `hero.passive.<kind>.<id>`, `hero.msg.<what>`, `tooltip.nusmp.hero.<what>`; ASCII-escaped). Forms carry their English `name` and `desc` inside the JSON. The lead merges the fragments.

## 18. Choices the lead made and the owner may change (to be listed in the hand-over)

* Item ids: `legendary_*` for the four Holy Weapons, `vassal_*` for the other seventeen; the Seven Star weapons are a class shown in the tooltip.
* Envy and Sloth are given to the Vassal Mirror and the Vassal Chain by design (canon ties no Holy Weapon to them).
* One weapon per player, one holder per Holy Weapon, acquisition by the Calling Gem and the commands, the curse unlock by deliberate acceptance only, the experience rule by distance (48 blocks).
* The anti-party rule is read as "no experience for two Holy wielders within 48 blocks" because the owner wrote "stand too close to one another"; the series itself ties it to fighting together.
* The Bless Series, the Boat, the Waves of Calamity as an event, and weapon-specific NPCs are not built.
