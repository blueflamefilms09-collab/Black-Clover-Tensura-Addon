# Relics of Many Worlds + the Djinn (1.24.0)

Design and reference for the Relics of Many Worlds system (D&D-style magic items reworked onto Tensura and Black Clover), the Order that keeps them unique, and the Magi Djinn system. Written without a compiler and never run in game; every number is a first estimate. Source cards: the owner's 78 images (87 distinct cards: 16 already in the mod, remade; 70 new, built in waves 1-7). Tensura ids were checked against the owner's Tensura 2.0.1.3 id reference.

Rules the owner set for this system: abilities that base Minecraft cannot do use Tensura skills and stats (lent skills, merged skills) or an invented mechanic; no ability is dropped. No new standalone stat or resource: Magoi is Tensura magicules (aura second), trust/assimilation/regeneration run on Tensura attributes. Djinn powers are temporary armor drawn by render layers (no real item swap). Weapons are 3D meshes built by the owner's lofted-section method (tools/relics/mesh_lib.py).



<!-- 1. The framework -->

# Relics of Many Worlds: the core (package `relics`, `client/relics`)

## 1. Grades (Tensura equipment tiers)
| Grade | tier | name colour | EP lift while worn | loot weight | Djinn vessel |
|---|---|---|---|---|---|
| Normal | 1 | grey | 0 | 40 | no |
| Special | 2 | green | 0 | 28 | no |
| Rare | 3 | aqua | +0.5% | 16 | no |
| Unique | 4 | light purple | +1% | 8 | no |
| Legend | 5 | gold (+ glint) | +2.5% | 4 | yes |
| God | 6 | red (+ glint, particles) | +6% | 1.5 | yes |
| Genesis | 7 | white, cycling colours (+ glint, particles) | +12% | 0.5 | yes |
A card's printed rarity becomes a grade with `Grade.ofCard` (common NORMAL, uncommon SPECIAL, rare RARE, very rare UNIQUE, epic/legendary LEGEND, anything else RARE). GOD and GENESIS are set by hand. The EP lift of all relics that work is summed and capped at +25%.

## 2. What each system stat runs on (owner rule: base Tensura, no new pools)
| System stat | Runs on |
|---|---|
| Mana cost of an act (`Act.mana(percent)`) | Tensura magicules (`TensuraStorages.getExistenceFrom(p).getMagicule()`, maximum `EnergyHelper.getMaxMagicule`), then aura for what they cannot cover, exactly as `KotodamaWords.pay`; times the mod's `DM_COST_MULT` |
| Existence cost (`Act.epPercent(percent)`) | the same Tensura storage: that percent of the maximum magicules AND of the maximum aura; refused if either would drop under 10% |
| Grade power (EP lift) | transient `ADD_MULTIPLIED_TOTAL` modifiers on `tensura:max_magicule` and `tensura:max_aura` (the path `DemonSlayerSwordItem.cutExistence` already uses) |
| Damage of `Fx.damage/aoe` | the mod's `BalanceLaw.damage` (EP scaling, PvP hit cap), then the relics' own cap on players (30% of max health, 45% for `damageHeavy`) |
| Healing (`Fx.heal`) | `BalanceLaw.heal` (50% cap, decay) |
| Lent abilities (`Passive.skill`, `Fx.lendSkill/lendSkills`) | Tensura skills through `MealSkills.grant` (the saved lease; 60-tick lease refreshed every second while worn) |
| Attribute passives | Tensura or vanilla attributes by registry id, transient modifiers with stable ids `nusmp:relic/<id>/<n>` |
| Resistances (`Passive.resist`) | not a Tensura attribute: the engine reduces incoming damage of that damage type or tag (`LivingIncomingDamageEvent`). Tensura resistance attributes can be used instead through `Passive.attr` |
| Cooldowns, uses per day, toggles, charges | plain counters (player persistent data / the stack component `relic_uses`); the owner rule allows these |
Nothing in the core creates a bar or a resource of its own.

## 3. Attunement
`RelicDef.attune()` marks a relic that needs it. It works only for the player it is attuned to (`relic_attuned` = their UUID on the stack). Ritual: sneak and hold use for five seconds (100 ticks); a progress bar shows on the action bar; letting go or standing up breaks it. At most three attuned relics may be carried and work at once (`AttuneRules`: the first three met in the order hands, armour, Curios count). Another player's relic can be taken over by attuning it again. Unattuned relics that need attunement show a faint tooltip ("its power sleeps") and have no passives, no acts. Items the mod already had (adopted defs) keep their own item class, so the engine intercepts `PlayerInteractEvent.RightClickItem` for them: unattuned, a right-click does nothing except run the ritual (the game repeats the click every few ticks while the button is held). Commands: `/multiverse relic attune` (admin, instant), `unattune` (anyone, the held relic).

## 4. The engine (`RelicEngine`)
* Every 20 ticks per player (staggered by entity id): `Relics.loadoutFresh` (hands for hand relics, head/chest armour, Curios through `CuriosLink`), then attribute modifiers (added, changed or removed so they always match), ambient effects (80-tick refresh, no particles), lent skills, the EP lift; a second pass at phase 9 engraves the inventory once (`RelicEngravings`, marker `NUEngraved` shared with `WeaponEngravings`).
* Resist: `LivingIncomingDamageEvent`, players only; reductions of several relics multiply, capped so at least 10% of a hit remains.
* Acts: `RelicEngine.activate` checks attunement, sneak, hold time, cooldown, uses per day, then pays mana and EP, flips toggles, writes counters, sends the cooldown to the client, runs the body in a try/catch. Right-click (`RelicWeaponItem`, `RelicGearItem`): first act, second act while sneaking; a held act (`hold(n)`) charges while the button is down. Worn relics: key V (Use Worn Relic): selects from a ring of all acts, SHIFT+V moves on, held acts charge while the key is down; payload `RelicActPayload` (validated, rate limited, the server rechecks that the relic is worn and working).
* Cooldowns live in the player's persistent data (survive relog and death) and are sent to the client (`RelicCooldownPayload`, also at login); a single-act hand relic also shows the vanilla item cooldown sweep.
* Summons (`Fx.summon`): at most 6 per player, a sweep every 10 ticks drives them from outside (target, path, blow) like the Dungeon's drivers; they vanish when the time is up, the owner dies or leaves, or the server stops.

## 5. Tooltip (client)
A component inserted after the item name: wraps to 45% of the screen width (cap 280 px), text scale shrinks to 0.75 before it pages, pages follow the mouse wheel ("Page 2/4"), other lines of the tooltip (enchantments, attributes) are counted into the height budget. Shows stars + grade, slot, attunement state, summary (`item.nusmp.<id>.desc`, else the def's text), costs and how to use each act (live cooldown and uses left), passives, EP lift; SHIFT shows the long card text (`item.nusmp.<id>.card`). Names are coloured by grade (`getName`; the tooltip recolours adopted items), LEGEND and up glint, GOD and GENESIS dropped relics give off motes and their wearers shimmer.

## 6. Worn look
HEAD and CHEST relics are real armour (`RelicArmorItem`, own `DeferredRegister<ArmorMaterial>`), vanilla armour points from `RelicDef.armor`, unbreakable. Their layer texture is painted at run time (`ArmorPainter`, 64x32, by look and palette; PLATE, ROBE, LEATHER, COAT, SUIT, CLOAK/CAPE/MANTLE for chests; CIRCLET, CROWN, HAT, HOOD, PLATE for heads) and registered after every resource reload at `textures/models/armor/relic_<id>_layer_1.png`; a PNG shipped at that path wins. NECK, BACK, BELT, HANDS, CHARM relics are Curios items; `RelicWornLayer` draws small tinted boxes (pendant and chain, cloak/cape/mantle, belt and buckle, gauntlet cuffs, hip charm) on the player model, glow parts full bright.

## 7. Loot, Lootr, Mimics
`RelicLoot.rollUnissued(level, rng, min, max)` picks a relic of that grade range that is not alive in the ledger (weighted by `Grade.weight`) and stamps it through `UniqueLedger.issue`; if the ledger refuses it tries another. A NeoForge global loot modifier (`nusmp:relic_chests`) adds one at a small chance to any loot table whose path contains `chests/` and ancient_city, bastion, trial or vault (5%, RARE..LEGEND), stronghold (4%, SPECIAL..UNIQUE) or dungeon (3%, NORMAL..UNIQUE). Lootr rolls the table once per player at opening time through the same loot code, so each opening can roll once and the ledger keeps any two rolls from producing the same relic. Mimics: a dying entity whose type path is `mimic` (any namespace) or in the tag `nusmp:relic_mimic_types` drops a RARE..LEGEND unissued relic at 6%, or the tame Cloak Mimic at 1.5%. (The global loot modifier is used instead of `LootTableLoadEvent` because 1.21 loot tables cannot be extended in place.)

## 8. Curios data
`tools/relics/gen_curios_tags.py` reads the wave sources (string ids and `Wave6Tune.ID_X`-style constants) and `tools/relics/relic_slots.json`, and merges every curio-slot relic into `data/curios/tags/item/{necklace,back,belt,hands,charm}.json` with `"required": false`, keeping all entries that exist. Re-run it after any wave change (`--check` reports without writing).

## 9. A wave adds a relic in 15 lines
```java
public final class Wave9 {
    public static void defs(Consumer<RelicDef> out) {
        out.accept(RelicDef.of("ring_of_sparks", "Ring of Sparks", Grade.RARE, RelicSlot.HANDS)
            .attune().palette(0x6A5ACD, 0xFFD700, 0x80E0FF).look(Look.GAUNTLETS)
            .desc("Lightning answers your hands.")
            .passive(Passive.attr("minecraft:generic.movement_speed", 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE))
            .passive(Passive.resist("minecraft:lightning_bolt", 0.5))
            .passive(Passive.skill("tensura:magic_sense"))
            .act(Act.of("arc", "Arc", c -> Fx.aoe(c.player(), 6, 4f, "minecraft:lightning_bolt", "minecraft:slowness", 60, 1))
                .cooldown(400).mana(5)));
    }
    public static void hooks() { }
}
```
The catalog registers the item, the tooltip and the key do the rest; the wave supplies the icon and the lang lines.

## 10. Registrations the lead adds
See the report of the core (mod constructor, client section, commands, WeaponRenderer, creative tab, lang).

<!-- 2. Uniqueness and the God of Order -->

# The Order: the uniqueness ledger and the God of Order (package `relics/order`)

**Rule.** Every relic that is not a consumable may exist once per world. A second copy (generated, duplicated, cheated in) brings the God of Order, who takes the extra copy and leaves the valid one alone.

## The ledger (`UniqueLedger`, SavedData `nusmp_relic_ledger`, Overworld storage)
Per relic id: serial (UUID), LIVE/LOST, last place (player / container / ground / mob / nowhere yet), dimension, position, last seen (game tick and real time), last visit of the Order. Stacks carry the data component `relic_serial`.
Decision table (`LedgerRules`, pure, tested): no entry -> ADOPT (first copy; a stack without serial is stamped); LIVE + same serial -> canonical (the second stack with the same serial in one pass is a duplicate); LIVE + no/other serial -> DUPLICATE; LOST + same serial -> REVIVE; LOST + anything else -> ADOPT.
A copy carried by an online player, on a loaded item entity or on a loaded mob is verified before a second sighting elsewhere is accepted as "moved": if the recorded place still holds the serial, the new one is a duplicate. (The stack being dropped is exempt: it is the same object.)
LOST: the item entity of the valid copy was destroyed (burned, void, despawn; not taken by a hopper or container below), an admin `release`, a copy issued by loot/command and never seen for 1 h of game time, or a copy of an ONLINE player not seen for 14 real days.
API for the core: `issue(level, id, stack)` (stamp + record; false if a copy lives), `liveIds(server)`, `isCanonical(stack)`, `scan(server)` (look at everybody next tick).

## Detection (bounded)
Players: inventory, armour, hands, cursor, ender chest, Curios, every 100 ticks staggered by entity id; at most 16 stacks per player per tick (a shulker box/bundle counts for what is inside, one level deep). Containers: the stacks of an opened or closed menu (chests, shulkers, ender chests, backpack mods using menus, **Lootr per-player chests**: their loot is rolled at open time, the core's loot function calls `issue`, so a second roll is refused or rerolled). Item entities on join, mob/armour-stand equipment on change. No world sweep of block entities (a container is judged when somebody opens it). Structure mods and worldgen chests: loot made through the core's loot modifier is issued; a table that names a relic item directly is adopted at first opening and every further copy is a duplicate. **Mimics**: the core's drop handler issues; the ledger judges what is dropped.

## Census (existing worlds)
When the ledger is first made in a world older than two minutes, it waits for the first player, then counts for 10 minutes: the first copy met of each relic is adopted (stamped), extra copies are only listed to their holders and to ops ("In N minutes the Order will come"); a summary goes to ops at the end; then enforcement. `/multiverse relic order census [hold]`, `enforce`, `status`, `release <id>`, `summon <player> <id>`.

## The visit (`OrderDirector`, `OrderPlan`)
One case at a time, a waiting list of 16 relic ids, 5 minutes cooldown per relic. Announcement "[Order] Two of <relic> cannot be." to the holder and ops; 6 s telegraph (spinning circle, closing ring, column of glyphs) 8 blocks from the holder; the God appears, reads (5 s, unhurtable), beams the duplicate to gold dust (2 s), every duplicate at the place is taken (re-judged at that moment: the valid copy is never taken), the holder gets a Notice of Order, it bows and leaves. A blow during reading/beam is remembered: the verdict is finished, then it fights (Decree of Binding, Writ of Light, Circle of Judgement; Erasure takes the duplicates of whoever struck it and ends the visit; forced after 75 s or at 35% health; hard limit 2 min). A creative holder never starts a fight. A holder who leaves the world or dimension calls the visit off. No loot; experience only.

<!-- 3. The Djinn system -->

# The Djinn system (relics/djinn, client/relics/djinn)

Magi's Djinn on top of Tensura's magicules and Black Clover's bonds. One of each Djinn exists; one player holds a given Djinn; a mage holds ONE of a Black Clover Spirit, a Devil or a Djinn (unless the multi-bond relic is worn); a Djinn only binds into a Metal Vessel, a relic of Legend grade or above.

## 1. New or replacement (CLAUDE.md rule 5)
Everything here is NEW: no Djinn, Seal, vessel binding, phase, household or Djinn render existed. The exclusive bond is NEW rules layered onto existing binding methods through small guards (section 3), keeping every old behaviour for a player who holds nothing else.

## 2. Roster (relics/djinn/Djinn.java, the single source; the art script parses it)
Twelve, appended in this order. Affinities cover all eight kinds (Heat, Lightning, Water, Wind x2 each, Life, Strength, Sound, Light x1).

| Djinn | Affinity | Maso form | Half Equip special (extra damage, capped 6) | Extreme Magic (shape, reach, raw dmg) |
|---|---|---|---|---|
| Baal, Lord of Thunder | Lightning | Thunder-Lord | CHAIN: arcs to a second foe, Slowness | Baal Bolg: Judgement of Thunder (9 marked bolts, 14, 26) |
| Amon, Sovereign of Flame | Heat | Flame-Sovereign | MELT: ignites, strips Resistance/Absorption, Weakness | Amon Salos: Crown of the Sun (nova, 12, 28, burn) |
| Vinea, Queen of the Deep | Water | Tide-Queen | FREEZE: Slowness II, frost | Vinea Zalg: The Drowning Sea (wave, 15, 22) |
| Focalor, Warden of Gales | Wind | Gale-Warden | PUSH: knocks the foe back | Focalor Zaleph: Seven-Wind Cyclone (vortex, 14, 20, Nausea) |
| Zagan, Keeper of Life | Life | Verdant | LIFESTEAL: 25% of the blow back to you | Zagan Genesis: Bloom of Life (heal field, 12, 16: allies healed, foes drained) |
| Zepar, Breaker of Giants | Strength | Titan | CRUSH: more against armour, Weakness | Zepar Fang: Titan's Fall (nova, 10, 30, Slowness III) |
| Furfur, Voice of Ruin | Sound | Resonant | DEAFEN: Tensura Silence + Nausea | Furfur Requiem: Chord of Ruin (nova, 16, 20, Nausea) |
| Marchosias, Hunter of Embers | Heat | Ember-Wolf | BRAND: fire + Glowing | Marchosias Hunt: Emberfang Run (beam, 20, 32, burn) |
| Sitri, Dawn's Radiance | Light | Radiant | SMITE: x2 vs the undead, Glowing | Sitri Dawn: Radiance of First Light (nova, 13, 24, Blindness) |
| Bifron, Stormfang | Lightning | Stormfang | PARALYZE: a held breath of Slowness III | Bifron Skyfall: Pillars of Storm (8 marked columns, 13, 24) |
| Paimon, Sovereign of the Sky | Wind | Sky-Sovereign | SLICE: Wither bleed | Paimon Tempest: Heavenly Gale (vortex, 14, 22) |
| Vassago, Lord of the Abyss | Water | Abyssal | DROWN: takes the foe's breath, Slowness | Vassago Maelstrom: Abyssal Tide (vortex, 15, 24) |

Raw damage goes through the balance law (EP scaling) and Fx.damageHeavy (a player takes at most 45% of their health per hit; creative and spectator players, allies and pets are never hit). No Extreme Magic breaks, burns or places a block: particles, velocity and mob effects only.

## 3. The exclusive bond (Bonds.java, BondRules.java)
Read from the real state of the mod:
* **Spirit**: SpiritBond.bonded (the trait record), the Spirit Lord skill of an elemental lord, or owning any of the four SpiritSlots (Salamander, Undine, Sylph, Gnome). The Anti-Magic lord skill is NOT counted as a spirit: it is the devil of the Anti-Magic grimoire.
* **Devil**: a devil contract (the Pacts record of ForbiddenMagic), or a grimoire with a "Devil" tag (five-leaf books, the Anti-Magic book with Liebe), or an awakened Anti-Magic lord., or a signed Infernal Contract (the persistent key `nusmp_infernal_contract` of the remade Devil Contract relic, wave 0). Not every grimoire has a devil, so this is exact.
* **Djinn**: a Djinn the player owns, or one lent to them.
`canBind(p, kind)` is true if the player holds nothing, or only that kind, or `multiBond` (Relics.worn(p, "astralis_the_eye_of_the_universe")). Both ways: edits to existing methods (each a one-line guard; behaviour is otherwise unchanged):
1. `book/ForbiddenMagic.bind`: refuse a devil contract while holding a Spirit/Djinn.
2. `multiverse/SpiritLordSkill.bond`: refuse an elemental spirit-lord bond while holding a Devil/Djinn (the "Anti" type is exempt).
3. `book/GrimoireBook.spiritDive`: refuse the first claim of a spirit while holding a Devil/Djinn.
4. `blackclover/GrimoireAcceptance.grant` (the random roll): a Djinn or Spirit holder's five-leaf roll becomes four-leaf (no devil). Admin and canon grants (grantExact) are NOT guarded: an admin may do anything.
5. `book/SpiritBond.addTrust`: the multi-bond price on the Spirit's trust gain.
6. `relics/wave/Wave0Contract.sign` (one guard added in review): signing an Infernal Contract is refused while a Spirit or a Djinn is held (and the signed contract counts as a Devil for every other check).
**Multi-bond price** (BondRules; tests): every bond's Magoi drain x1.4; no natural magicule regeneration in Full Equip (a -100% modifier on MAGICULE_REGENERATION_MULTIPLIER); each extra bond halves the trust a bond gains. The multi-bond relic allows more KINDS, never a second Djinn.

## 4. Phases and numbers (DjinnPhaseRules.java)
Magoi is the Tensura magicule pool. Every amount is a share of the maximum magicules; a cost is paid from magicules first and bridges to Aura (like the Kotodama words); the pool is never drained below 2%; running dry brings ManaLaw's own strain (exhaust: weakness, slowness, collapse).

| Phase | Entry | Upkeep /s | Needs | Ends |
|---|---|---|---|---|
| 1 Lesser Summon | 3% | 0.3% | 10% bar | 20 s, then 15 s wait |
| 2 Djinn Weapon (Half Equip) | 5% | 0.6% | 15% bar | by hand |
| 3 Full Equip (Maso) | 20% | 0.7% | 35% bar, trust 40 | 90 s, then a backlash and a lockout of 1 to 5 min |
| 4 Extreme Magic | 60% flat | none | 60% bar, trust 70, inside Maso | one strike, cooldown 10 min; 3 s of telegraph |
Multiplied by 1.4 (multi-bond) and 1.25 (assimilation at 90%+). Steps up one at a time; stepping down is always free. A household member gets only phase 1.
* **Maso**: +30% speed, +5 attack, +6 armor, +3 toughness, +0.4 knockback resistance, +8 max health, the Djinn's Tensura element boost +30%, and an EP spike: +25% on Tensura's max magicule and max aura (transient modifiers, the same path as the Wild Surge). Bounded flight: mayfly only if the player did not have it, flying speed 0.075, taken back at the end/death/logout/relog (a saved flag), Slow Falling 10 s on landing, creative/spectator untouched.
* **Ending a Maso**: voluntary < 30 s: no backlash, lockout 1 to 2 min; later: severity 0.27 to 0.8; timeout: severity 1, lockout 5 min; out of Magoi: severity at least 0.6. Backlash = Slowness, Weakness, Nausea, Hunger and food loss, Blindness (severe), and a loss of max health (2 hearts at the worst) that heals back linearly over 3 min (a transient modifier recomputed from a saved end time).
* **Half Equip**: melee blows add elemental damage (25% of the blow, capped at 6) and the Djinn's special; the bearer takes 40-100% less of the Djinn's element (half of it outside Maso).
* **Lesser Summon**: a small bust over the shoulder; every 2 s it strikes the nearest hostile mob within 12 blocks for a little (never a player).

## 5. Stats and what Tensura thing each runs on (the owner's "no new pools" rule)
| Stat | Runs on |
|---|---|
| Magoi (its bar, costs) | Tensura magicules (ExistenceStorage), Aura as the second pool; ManaLaw.exhaust as the strain |
| EP spike in Maso | transient modifiers on TensuraAttributes.MAX_MAGICULE and MAX_AURA (Tensura's EP follows both) |
| Trust (0-100) | a persistent modifier on TensuraAttributes.ABILITY_MASTERY_GAIN, +0.2% per point (id nusmp:djinn_trust); read back from it |
| Assimilation (0-100) | a persistent modifier on TensuraAttributes.ABILITY_LEARNING_GAIN, -0.5% per point (id nusmp:djinn_assimilation), plus a transient element resistance (flat up to +0.3: FLAME/WATER/WIND/LIGHT_RESISTANCE, else MAGIC_RESISTANCE) and, from 50%, +5% of the element boost |
| Backlash (exhaustion, strain, lifespan) | Tensura-era effects (Slowness, Weakness, Nausea, Hunger, Blindness), the food bar, and a transient MAX_HEALTH modifier that heals back |
| Magoi replenishment | natural magicule regeneration; the bonded vessel held or worn = a +25% modifier on MAGICULE_REGENERATION_MULTIPLIER (Heritage-style); Draw Rukh = EnergyHelper.gainMagicule scaled by ManaLaw.density (key H, still, 30 s at most, 0.8% per second at normal density, more in a Grand Magic Zone) |
| Maso/Extreme boosts | vanilla attributes plus TensuraAttributes element boosts |
| Skills of the phase | Tensura skills lent through Fx.lendSkills/MealSkills (leases of 5 s, renewed each second), all ids from the owner's Tensura id list: Lesser magic_sense; Half + danger_sense + the Djinn's manipulation (flame/lightning/water/wind/sound_manipulation, self_regeneration for Life, strengthen_body for Strength, heavenly_eye for Light); Full + the domination skill (flame/lightning/water/wind/sound_domination, ultraspeed_regeneration, steel_strength, all_seeing_eye) + thought_acceleration |
Plain counters (cooldowns, Maso lockout, trust pacing, uses) stay as saved numbers: they are not stats.

## 6. The binding ritual and the economy (DjinnBinding.java, BindingRules.java)
Use a Djinn Seal with a Metal Vessel in the other hand. Price: a full Magoi bar, 25% of current EP (magicules and aura each; at least 10% of the maximum always left), three high-quality magic crystals, and a vessel attuned to you. The Djinn's trial: it may refuse, 5% (Arcane/Saint stage) to 27.5% (Stage 9); a refusal costs half the EP, one crystal, 30 s of Weakness and a one-minute wait, and never harms the Seal or the vessel. Success: the ledger commits first (two players racing for one Djinn cannot both win), the vessel gains djinn_bound + djinn_serial, the bearer's record is written, the server is told. Why this price: the pool and the EP are a toll any strong mage can pay, the crystals force a trip into the Dungeon, the attunement ties it to a relic that is itself unique (and guarded by the God of Order), the trial makes a rushed low-stage mage pay for failing.
**Sources of Seals**: (a) the first time each of the four floor bosses falls in a world (Goliath, the Charybdis Spawn, the Devil Avatar, the Blood-Red Commander: heard through DungeonCollapseEvent) one Seal of a sleeping Djinn goes to the killer; (b) vault/Treasury chests, 3%: `DjinnSources.vaultSeal(level, random)`; (c) players: dropping, trading, and the Seal returns when its owner dies (it falls where they died, glowing, unburnable, never despawning, rescued from the void) or runs `/multiverse djinn release`; (d) admin grant. A Djinn whose Seal nobody has seen for 7 days of running time sleeps again, so none is lost.
**Ledger** (DjinnLedger, SavedData "nusmp_djinn_ledger"): per Djinn SLEEPING / SEALED (a Seal exists) / BOUND (owner, vessel serial, up to 2 household) plus which bosses have paid.
**Household** (DjinnHousehold): `share <player>` offers; the ally has a minute to `accept`; they get only the Lesser Summon, their assimilation rises 0.25%/s of use (ten summons reach the 50% cap, past it the Djinn withdraws), it falls 0.005%/s at rest; `leave`, `unshare`, the owner's release or death end it. The ally must hold no Spirit/Devil (or wear the multi-bond relic).

## 7. Visuals (client/relics/djinn; the armour is a RENDER LAYER, never an item)
`DjinnArmorLayer` (RenderLayer on both player skins) reads the synced phase so everyone sees it. Meshes are mesh_lib-built pieces (lofted sections, smoothed normals, a painted skin + additive glow per Djinn, tools/relics/gen_djinn_art.py + djinn_parts.py): 23 pieces, about 3,000-4,500 quads per Djinn in Maso, parsed once into float arrays, drawn with no per-frame allocation, culled beyond 48 blocks, small details dropped beyond 24.
* **Lesser Summon**: a translucent bust (billboard) of the Djinn over the right shoulder.
* **Half Equip**: gauntlet + vambrace + pauldron on the main-hand arm, a brow crest, and the held weapon sheathed in the element (additive blades, flicker); the server adds a hand trail.
* **Full Equip**: helm (open, visor or circlet) with horns, ears and hair, chest with a glowing core and runes, belt and tassets, both pauldrons, vambraces, gauntlets, greaves, two-segment wings that flap faster in the air and fold when gliding/swimming, a chain tail, a spinning halo; the rune body-art as a second additive pass over the skin (64x64, eyes glow white), and a fresnel aura through the mod's Zagred shader tinted in the Djinn's colour (falls back to nothing if the shader did not load).
* **Forge and shatter**: each piece flies in from its own side over ~20 ticks with a particle ring, and shatters outward over 12 ticks when the phase ends.
* **Per Djinn** (shapes by style row in djinn_parts.py, colours from the table): Baal crescent horns, blade wings, whip tail, spoke halo, open helm, fin pauldrons; Amon ram horns, flame wings, flame tail, ring halo, visor, spiked pauldrons and claws; Vinea crescent horns, veil wings (ribbons), tendril tail, rune halo, circlet, tiered pauldrons; Focalor spike horns, feather wings, whip tail, ring halo; Zagan stag horns, feather wings, tendril tail, rune halo, circlet, domed pauldrons; Zepar ram horns, membrane wings, blade tail, no halo, visor, spikes and claws; Furfur crown spikes, veil wings, tendril tail, rune halo, circlet; Marchosias spike horns, membrane wings, flame tail, open helm, claws; Sitri crown spikes, feather wings, no tail, ring halo, circlet; Bifron stag horns, blade wings, blade tail, spoke halo, visor; Paimon crescent horns, feather wings, whip tail, rune halo, tiered pauldrons; Vassago ram horns, membrane wings, tendril tail, visor, claws. Previews: scratchpad/work_djn/prev (full_sheet.png, half_sheet.png and per-Djinn images).
* **Screen**: a coloured vignette that deepens with the phase, a pulsing colour fringe, a narrowing field of view while an Extreme Magic gathers, a flash and a short camera shake when it strikes, and a HUD bar above the hotbar (phase pips, Magoi, time left, assimilation, trust, Maso lockout and Extreme cooldown). The Zagred aura shader is reused; a full-screen distortion would need a post chain and is not attempted.
* **First person**: RenderArmEvent (empty hand) and RenderHandEvent (item in hand, placement copied from ItemInHandRenderer.renderPlayerArm) dress the owner's arm.
* **Mixins**: none added. The repo already mixes into PlayerModel (PlayerModelMixin: sword draw, cast animation), not into PlayerRenderer; the layer, the events and the GUI layer are enough.

## 8. Commands and keys
Keys (Controls > Multiverse of Anime): J raise the phase (Extreme Magic from Maso), Shift+J dismiss, K lower the phase (or let go of a gathering Extreme Magic), H Draw Rukh. Commands under /multiverse djinn: status [player], release, share, unshare, accept, leave; admin: ledger, grant <player> <djinn> [bind], reset <djinn>, phase <0-4> (no price, needs a Djinn and a vessel).

## 9. Design notes not built
* **Interference** (Magi: magicians cannot use Djinn): not enforced, because nearly every player of this mod has a grimoire; the cost of mixing the two is the shared Magoi pool itself.
* **Djinn Warden** (guardian AI outline, not built): each floor boss's Seal is guarded by a Warden tied to that boss: a shade of the sleeping Djinn that appears when the boss falls and must be beaten (or evaded for 60 s) before the Seal comes loose; it fights only with the Djinn's own Half Equip trait, telegraphs its Extreme Magic like the players', and drops the Seal into the killer's hands. It would be a ZogratisBoss.
* **Dark Djinn**: Black-Rukh vessels made from sacrificed lives: a Dark Djinn would grant Maso with regeneration but cost max health permanently (a persistent MAX_HEALTH modifier) and raise assimilation without a cap; it would need a sacrifice mechanic and is out of scope.


## 10. Review notes (rv5)
* Client armour meshes are loaded lazily, one piece at the first frame that needs it, cached in a [djinn][piece] array (and the textures' ResourceLocations cached), cleared on resource reload; a missing piece is cached as empty and never re-read. About 51,000 quads in 11 MB of text for all twelve; one Djinn is about 3,500-4,900 quads, roughly 0.55 MB of floats once loaded; only the Djinn actually seen is ever parsed.
* The Act payload is validated server side (only the four known actions; spacing of 4 ticks; alive, not spectator; every phase change goes through DjinnPhaseRules.request).
* Keys: J, K, H (the mod's other keys: V use worn relic, R use dungeon relic, G grimoire slot/summon; the others are unbound by default).

<!-- 4. Wave 0: the 16 remade items -->

# Wave 0: the sixteen items the mod already had, remade onto the relic system

Class `relics/wave/Wave0.java` (defs + `hooks()`), helpers `Wave0Rules` (pure, tested), `Wave0Util`, `Wave0Acts`, `Wave0Hooks`, `Wave0Fusion`, `Wave0Contract`.
Every def `adopt`s its item: **no item is registered**, so every id, sprite, 3D model, engraving list and old right-click / sneak-right-click technique stays.
The layer added on top is the card: grade, attunement, passives, acts and hooks. The old techniques are NOT gated by attunement (they are the item itself, the
core cannot gate native `use()` and old worlds keep working); attunement gates everything the card layer adds (flags, passives, acts).

Reading the NEW / REPLACED column: NEW = something the card has and the item lacked, added next to the old. REPLACED = the card supersedes an old rule, the new rule is the one now in force (the old code is still in the repo, it is just overridden in the hooks).

Grade | slot | attune | vessel are as fixed by the lead. Cost units: `mana(n)` = n percent of max magicules; per-day counters are Minecraft days (a long rest); a short rest is a cooldown; a round is one second; a saving throw is `d20 + toughness mod` vs the DC (`Wave0Rules.saveChance`, mod from knockback resistance, max health, armour toughness, +4 boss).

| Card | id | grade / slot | NEW (the card's layer) | REPLACED | Stays as it was | Tensura used (skill ids marked V are verified in the repo; the rest follow the registry's field-name convention and are gated at run time) |
|---|---|---|---|---|---|---|
| Quickling's Ivy | `quicklings_ivy` | RARE / MAIN_HAND, attune | +1 attack damage, finesse (+0.2 attack speed), +1d4 poison on every hit (once per swing), act **Sprout Ivy** (primes the next hit within 10 s: Strength save DC 16 or restrained, re-saves each second, 3 rounds, 1 on a boss; 6 a day, 15 s apart = "2 per short rest"), the critical-hit ivy now also saves each round | the old crit root (flat Slowness) is joined by the saving restraint | Ensnaring Ivy (right-click), Thorn Flurry, engravings, 3D model | Webbed (effect), `tensura:sticky_steel_thread` lent 10 s by Sprout Ivy |
| Magebane Longsword | `magebane_longsword` | UNIQUE / MAIN_HAND, attune | +2 attack damage; a hit on a caster or a buffed creature: concentration save with disadvantage (DC max(10, dmg/2)) or lose a buff + Magic Interference; **reaction**: a creature within 3.5 blocks that begins a ManasCore skill / grimoire page is struck (1 per 3 s); set bonus act **Dispel Magic** (needs the shield attuned too; once until dawn; aimed creature or an 8-block sweep: all buffs (weak ones for sure), spell bolts, wards, jam) | none | Dispel Surge, Spellbreak Ward, parry link with the shield, engravings, model | `tensura:magic_sense` (V) passive, `tensura:magic_jamming` + `tensura:chant_annulment` lent 30 s by Dispel Magic (merged), Magic Interference (effect) |
| Magebane Shield | `magebane_shield` | UNIQUE / MAIN_HAND (held in either hand), attune | +1 armour while held; advantage vs spells of casters within 5 blocks (a fifth less spell damage, and a harmful effect from them is shrugged off with a save at advantage); **Absorb Elements** (auto, once until dawn: first fire / cold / lightning blow halved, matching Tensura resistance lent 30 s, Magic Barrier 10 s, next melee hit returns 3d6); set bonus **cover** (with the longsword: a spell / blast of 6+ is cancelled if the shield is raised, else 50% nothing / else half; once until dawn) | none | blocking, Spellbreak Parry, Absorb Surge, 1/3 magic cut, engravings, model | `tensura:magic_resistance` passive, `tensura:flame_attack_resistance` / `cold_resistance` / `electricity_resistance` lent by Absorb Elements, Magic Barrier (effect) |
| Bolt Thrower | `bolt_thrower` | RARE / MAIN_HAND, attune | +2 attack damage, +2 force on every bolt, +1d8 more vs constructs (golems, colossi, statues); "no loading" already true | none | Force Bolt, Triple Volley, Thrower's Marks, engravings, model | `tensura:farsight` passive (the "+2 to hit" became sight) |
| Brooch Pin | `brooch_pin` | SPECIAL / MAIN_HAND | +1 attack damage drawn; worn in a Curios slot and not drawn = folded: creatures see the wearer 25% less (LivingVisibilityEvent) and Presence Concealment is kept on them ("disadvantage on checks to tell it is a weapon") | none | Piercing Pin, Ruby Ward, Backstab, engravings, model, Curios charm/belt slots | Presence Concealment (effect) |
| Club of Rot | `club_of_rot` | SPECIAL / MAIN_HAND | critical hit: +2d6 necrotic (not undead / construct) and Constitution save DC 15 or Weakness II + Mining Fatigue, saving again each second | none (the old Rot on every hit is the item's own and stays) | Putrid Slam, Plague Cloud, Rot stacks, engravings, model | `tensura:poison_resistance` (V) passive |
| Crossbow of Swift Death | `crossbow_of_swift_death` | RARE / MAIN_HAND, attune | **surprised** target (asleep, from behind, hunting someone else or no one) = crit weight (+60% damage) and Wisdom save DC 12 or frightened (flees; player slowed; boss only weakened); a crossbow with no `Notches` gets 3d6 | none (the old random crit and the execution stay) | Notched Bolt, Death Mark, notch for crit kills, engravings, model | `tensura:coercion` passive, Fear (effect) |
| Doom Hammer | `doom_hammer` | SPECIAL / MAIN_HAND | blow leaving a living non-undead non-construct under 35% = doomed 8 s ("can't be stabilised": healing halved, Curse effect); under 25% = +5d6 necrotic ("may kill it instantly") | none | Doom Slam, Sentence of Doom, armour break, engravings, model | Curse (effect) |
| Weapon of Resurgence | `weapon_of_resurgence` | SPECIAL / MAIN_HAND, attune | +1 attack damage; the paladin bond = attunement + Self Regeneration lent; act **Lay on Hands** from the item's own 40-point pool (10 points = 20 HP to the ally looked at or yourself; 5 more lift an affliction) | none | the pool and its refill (every hit returns a point), Resurgent Smite, Chains of Sanctity, engravings, model | `tensura:self_regeneration` passive |
| The Hand of Providence | `hand_of_providence` | UNIQUE / HANDS (Curios), attune | acts **Mage Hand** (fetches items and XP from 12 blocks, lends Gravity Manipulation 20 s) and **Mage Hand: Lay on Hands** (ally up to 18 blocks: 30% health, regen, cleanse, Healthcare); Holy Resistance lent | slot: the card makes it worn (the card layer works from the Curios hands slot; held in a hand it keeps all old behaviour) | healing fists, Laying On of Hands, Providence Strike, engravings, model | `tensura:holy_attack_resistance` passive, `tensura:gravity_manipulation` lent, Healthcare (effect) |
| Infernal Contract (was Devil Contract) | `devil_contract` | LEGEND / CONSUMABLE, vessel | the four contracts (see below), **one contract at a time**, Curse: Damned, `/infernalpact`, clickable terms appended to the old three | display name (Devil Contract -> Infernal Contract), `.desc` text, **hand model** (red book -> the card's parchment scroll; the old book mesh and skins are kept as `devil_contract_book`) | the three old prices (maxhp / seal / tax) and the devil bound to the grimoire, sprite icon | Sage (V) kept lent for Knowledge, Analytical Appraisal + Magic Sense (ritual, merged), Flame / Lightning Manipulation + Ranged Barrier lent daily for Power, `tensura:max_aura` +10% (Blood), `tensura:max_magicule` +15% (Power) |
| Prism Blade | `prism_blade` | RARE / MAIN_HAND, attune | +1d8 radiant on every hit; **Blinding Radiance** (9 blocks, Con save DC 15 or blinded, re-save each second; once a day); **Chromatic Strike** (next hit within 10 s: +4d8 radiant, Wis save DC 15 or frightened, or charmed when sneaking; 3 a day, 30 s apart) | none (the old Blinding Flash and the seven-colour beam stay as the item's own) | Blinding Flash, Chromatic beam, engravings, model | `tensura:light_attack_resistance` passive, Flashed Blindness + Fear (effects) |
| Permafrost Sabre | `permafrost_sabre` | RARE / MAIN_HAND, attune | +1 attack damage; cold damage halved while held (`#minecraft:is_freezing`, `tensura:ice_elemental`); +1d6 cold on every hit (2d6 as a set); the set's **Boreal Fusion** | none | Whirlwind Frost, Permafrost Sweep, the old pair bonus, engravings, model | `tensura:cold_resistance` passive, Chill (effect), Wind Manipulation + Ice Breath lent by Fusion (merged) |
| Cyclone Rapier | `cyclone_rapier` | RARE / MAIN_HAND, attune | +1 attack damage; Light property = +0.3 attack speed; +1d6 force on every hit (2d6 as a set); the set's **Boreal Fusion** | none | Cyclone Thrust, Gale Lance, the old pair bonus, engravings, model | (shares Fusion's skills) |
| Boreal Blades (set "Fusion") | on `permafrost_sabre` and `cyclone_rapier` (act `boreal_fusion`) | | **Boreal Fusion**: sabre in one hand, rapier in the other, both attuned: a 2.2-block frost whirlwind 3 blocks ahead for 12 s; Strength save DC 16 (disadvantage within 1.6 blocks of you at the cut) or 2d6 cold + 2d6 force and held (max two medium creatures or one large), cut again each second, saves to escape; use again = move it up to 9 blocks toward your aim, sneak = dismiss; one whirlwind each; twice a day | the old "fused" bonus numbers of the two blades are untouched; Fusion is an additional act | (see above) | Frost (effect) |
| Bloodbound Katana | `bloodbound_katana` | LEGEND / MAIN_HAND, vessel | charges now come only from the living (undead and constructs give none); toggle **Blood Sip** (1d4 of your blood per hit, never under 2 hearts, +1 charge, Drain lent); toggle **Bloodflare** (each hit spends 3 charges for +1d8 necrotic, twice a hit at most); a full blade (10) costs the wielder 1 HP a hit | the "any hit feeds the blade" rule (corrected one tick after the item's own on-hit) | max 10 charges, the +0.8 per charge bite, Bloodflare right-click, Crimson Reaping, engravings, model | `tensura:drain` lent while Blood Sip is on |
| Thundercleave | `thundercleave` | RARE / MAIN_HAND, attune | +1 attack damage; two charges (refill at dawn; +1 per 10 s held aloft under a thunderstorm in the open); act **Thunder Sweep** (up to 3 creatures within 3.4 blocks: 2d6 thunder, pushed, deafened = Darkness + Silence 2 s) | none | Thunder Arc, Thunderstorm, arc to a second foe, engravings, model | `tensura:electricity_resistance` passive, Silence (effect) |

## Infernal Contract: the four contracts and the curse
- **One at a time**: signing refuses if a devil already lives in the grimoire (`ForbiddenMagic.devilOf(p) != null`) or a contract is signed (persisted key `nusmp_infernal_contract` in the player's persisted data). **For the Djinn rule: the Devil bond = `ForbiddenMagic.devilOf(p) != null || !persisted("nusmp_infernal_contract").isEmpty()`** (also `Wave0Contract.bound(p)`, package-private).
- **Blood** (5 ability points): full heal, +2 hearts, +1 attack damage, +4% speed, +10% max aura (modifiers re-applied every 2 s while signed).
- **Riches** (15,000 gp): 64 Magic Stones, 8 medium + 4 high-quality magic crystals, 32 emeralds, 8 diamonds, at once (absent items skipped).
- **Knowledge** (Contact Other Plane DC 10 + advantage on a knowledge skill): Luck +2, Sage kept lent; ritual = sneak and use a plain book (5 min cooldown): Sage + Analytical Appraisal + Magic Sense lent 60 s, hostile presences within 40 glow 10 s; a failed DC 10 save (25%) = nausea + weakness (madness).
- **Power** (an extra spell slot per level, or three spells once a day): +15% max magicule, grimoire magic 20% cheaper (`nusmp_mana_tax` -0.20, floor -0.5), and at each new day three Tensura skills (flame, lightning, a ranged barrier) are lent 10 minutes.
- **Curse: Damned**: on death a soul crystal (a Tensura high-quality crystal, or an amethyst shard) named "Soul of <name>" drops where they fell; they respawn Soulless (-30% max health, Weakness) until someone uses the crystal while they are online, or one day passes (never a soft-lock). "Disadvantage on death saves" became that harsher death.
- Reaching it: right-click the contract (old three prices + a second message with four clickable terms), `/infernalpact <blood|riches|knowledge|power>` (hold the contract), or the four acts (`sign_*`) when the engine routes them.

## Tabletop rules replaced
saving throw -> d20 + toughness vs DC (`saveChance`); advantage / disadvantage -> chance squared; round -> 1 s; short rest -> cooldown; long rest / dawn -> Minecraft day counter (per player, persisted); Charisma bonus to charges -> the fixed 10; "+N to attack rolls" -> damage only (Bolt Thrower's to-hit became Farsight); ability checks to spot the brooch -> visibility + Presence Concealment; Mage Hand object manipulation -> fetching items / XP; 15,000 gp -> a bundle of Magic Stones and gems; Contact Other Plane -> the Sage ritual; spell slots -> a bigger magicule pool and cheaper casting; "death save disadvantage" -> the soul crystal.

## Model check against the card art (nothing regenerated except one)
- Fixed (clearly wrong): **Devil Contract** was a red book; the card is a parchment scroll on a dark rod with a pentagram finial, a black claw tip, runes and a blood seal. Rebuilt with mesh_lib (`tools/relics/wave0_models.py`; 16 parts, 2542 quads; skins `textures/entity/weapon/devil_contract[_glow].png`). Old files kept: `weapon_meshes/devil_contract_book.txt`, `textures/entity/weapon/devil_contract_book[_glow].png`. The 16x16 sprite is still the inventory icon. Note: `tools/gen_weapon_models.py` would re-emit the old book to `devil_contract.txt` if run again.
- Left untouched, flagged: the Permafrost Sabre (mesh and sprite are icy pale blue and curved; card is a teal-green blade with a cream wing guard) and the Cyclone Rapier (mesh is silver-teal; card is dark navy with violet crystal facets). They follow the mod's older sprites; a recolour of their skins is offered if wanted. The rest match their cards in silhouette and palette.
- Not testable here: nothing was run in game; the preview tools were used.

## Needs from the rest of the system
- Acts of adopted MAIN_HAND weapons are reached through the Use Worn Relic key (the engine scans both hands); the native right-click stays the item's own.
- The five-second attunement ritual (`RelicEngine.useHeld` / `attuneTick`) is called only by the new item classes: adopted items need `/multiverse relic attune` or a RightClickItem + use-tick hook in the core (sneak-right-click is the old second technique of the weapons).
- Hand of Providence is slot HANDS: with Curios it works worn; without Curios its card layer is inert (held in the hand it keeps its old behaviour).

## Wave 1

# Wave 1: hand-held staves, wands, swords and tools

All ten are MAIN_HAND relics with a 3D mesh (tools/relics/wave1_models.py + wave1_m_*.py, sprites by wave1_icons.py). Wiring: first act = use, second act = sneak + use. A relic with several spells keeps its selection, charges, per-spell cooldowns and uses of the day in its own stack custom data (`nusmp_w1`): use casts the selected spell, sneak + use selects the next. Tensura skills are lent through `Fx.lendSkill(s)` (ids from TENSURA_IDS.md; every one checked against clean.json).

| Card | id | Grade | Slot | Abilities as built | Tabletop rules replaced |
|---|---|---|---|---|---|
| Staff of Moonlight | staff_of_moonlight | UNIQUE (attune) | MAIN_HAND | resist radiant (light_elemental) 50 percent; Night Vision; Magic Sense lent while held ("drawn to places of power"); 10-charge pool, 1d6+4 at dawn: Guiding Bolt 1, Moonbeam 2, Lightwall 5 (+ barrier skills), Sunbeam 6, Star Crown 7 (+ Magic Aura) | light toggle = Night Vision; the d20 destruction = 1 in 20 the staff is dark through the next dawn (never destroyed); spell DC / attack = fixed damage |
| Liquid Sword | liquid_sword | RARE | MAIN_HAND | +2 damage in water (+1 becomes +3), Dolphin's Grace + Water Current Control in water; Liquid Lure 3/day pulls a foe (size and knockback-resistance gate) | Strength save DC 16 = size / knockback resistance; short rest = day |
| Marid's Valor | marids_valor | RARE (attune) | MAIN_HAND | Change State solid/liquid (default solid = the model): liquid +2 reach, +2 in a water duel, Water Blade lent; solid +4 cold, Frost + slow + freezing, Cold Resistance lent; Flash Freeze (solid) turns a 3 x 3 water patch to ice for 1 min | 2d6 cold = +4 (PvP cap); 5 ft cube = 3 x 3 patch; advantage = +2 damage |
| Catstaff | catstaff | UNIQUE (attune) | MAIN_HAND | finesse (attack speed 2.0), +15 percent speed, +4 safe fall, -20 percent missiles and explosions, Danger Sense lent; spells Jump (leap + meow), Speak with Cats (tames and mends cats in 14 blocks, Thought Communication + Telepathy lent), Feline Form 1/day (90 s Speed II, Jump II, Night Vision, Beast Transformation lent) | Dex-save advantage = missile / explosion reduction + Danger Sense; polymorph = Tensura beast transformation + effects; "cat lover" = flavour (cats trust the bearer), not enforced |
| Rainbow Rapier | rainbow_rapier | RARE (attune) | MAIN_HAND | Colour Spray 1/day (8-block cone: Blindness + Tensura Flashed Blindness + Glowing); crit = 2 s invisibility + Presence Concealment (8 s lockout); Recolour (sneak + use) steps a sheep or a wool/carpet/glass/concrete/terracotta block one dye | 2nd level spell = fixed cone; colour any object = sheep and dyeable blocks (never in the Dungeon) |
| Astralis | astralis_the_eye_of_the_universe | GENESIS (attune, vessel) | MAIN_HAND | Arcana Astra: spell damage x1.15 + Magic Aura lent; flag multi_bond; Blinding Stars 1/day (8 blocks, 6 radiant + blind); Dimension Walker 3 charges (+2 with the Amulet of the Sea of Stars via Wave4.walkerBonus): Door 1 (safe teleport 32 + Spatial Motion), Scrying 2 (foes glow 10 s + Farsight, Magic Sense, Analytical Appraisal), Planeshift 3 (to bed / spawn, Spatial Manipulation) | +1 DC / attack = +15 percent spell damage; long rest = dawn; Planeshift = recall (no plane travel into or out of the Dungeon) |
| Deliverance | deliverance | UNIQUE (attune) | MAIN_HAND | Retribution (a fifth of a blow, max 3, returns as light / holy); Divine Smite (8 percent magicules): next blow +6 (+9 undead) and Strengthen Body lent, opens 8 s of Sacred Flame (sneak + use, 4 radiant / holy, 16 blocks) | Divine Smite / sacred flame cantrip as bonus action = the 8 s window |
| Wand of Bubbles | wand_of_bubbles | NORMAL | MAIN_HAND | Blow Bubbles (particles); Air Bubble 1/day (aimed ally or self, Water Breathing 5 min, popped by damage) | 1 hour = 5 minutes; "as a reaction" = sneak + use |
| Viola of Moonlit Spirits | viola_of_moonlit_spirits | RARE | MAIN_HAND (card: CHARM) | Spirit Performance (3 ghosts at night, 1 by day, each turns one blow aside, 1 in 5 also Fear; Musician lent); 5 charges, 1d4+1 at dawn: Dissonant Whispers 1 (psychic + Fear), Moonbeam 2, Gaseous Form 3 (Wind Transform + mist effects) | disadvantage on the first attack = turned-aside blows; roll of 1 / Wisdom save = 1 in 5 Fear; per day regained 1d4+1 = at dawn |
| Book of Mispells | book_of_mispells | RARE | MAIN_HAND (card: CHARM) | seven pages, each once a day, each revealed as its true spell: Massage (regeneration, calms), Mage Armoir (armor-stand wardrobe 30 s + 2 hearts + Body Armor), Fold Person (root 3 s + Tensura Paralysis), Speak with Dad (Resistance + Thought Communication, Overworld only), Ick Storm (20 s slowing sludge), Telepoot (fart sound), Wash (cleanse harmful effects) | (1/day) = once per page per day; Wisdom save = size / knockback resistance |

Skill ids used (all in TENSURA_IDS.md): magic_sense, danger_sense, ranged_barrier, multilayer_barrier, magic_aura, musician, sound_manipulation, wind_transform, water_current_control, water_manipulation, water_blade, cold_resistance, beast_transformation, thought_communication, telepathy, spatial_motion, spatial_manipulation, farsight, analytical_appraisal, light_attack_resistance, strengthen_body, body_armor. Effects: fear, paralysis, frost, flashed_blindness, presence_concealment.
Marids_valor_liquid is an extra mesh (the liquid state) with no item: ready for a state-variant hook in NavClient.meshId.

## Wave 2

# Relics of Many Worlds, wave 2: charms, boots and the cloaks / capes / mantles

Code: `relics/wave/Wave2` (defs and hooks), `Wave2Acts` (the abilities), `Wave2Events` (handlers and shared state), `Wave2Data` (per-stack state in custom data),
`Wave2Mana` (Tensura magicules), pure and tested: `Wave2Rules`, `Wave2Deck`, `Wave2Tome`, `Wave2Lives`. Icons: `tools/relics/wave2_icons.py`.

Conversions used everywhere: 1 block = 3 feet (30 ft = 10 blocks); a long rest is a Minecraft day; a saving throw is a chance read off the target's maximum
health (5% for small things, 50% at 100 HP, 60% for bosses); "advantage" is two rolls, the better kept. Costs are paid by the core before a body runs.

| Card | id | Grade | Slot | What each ability became | Tabletop rule replaced and how |
|---|---|---|---|---|---|
| Sand of the Pocket | `sand_of_the_pocket` | Rare | Charm | Pocket Sand (15 s): nearest creature in a 3.5-block cone, Blindness + Flashed Blindness + Weakness 3 s (1.5 s vs bosses); the Warden is immune (no eyes). Sandcastle (3 min): 4 sand. | Contest of dexterity rolls: the sand always lands, bosses halve it. "Disadvantage on attacks / sight checks": Weakness and Blindness. |
| The Tome of "Wait, That's Illegal" | `the_tome_of_wait_thats_illegal` | Rare | Charm, attune | Every harmful effect on a player goes on file for 10 s. Petition (2/day, 30 s, 3% magicules): appeal the latest failure on you or a player within 10 blocks (or stay open 30 s for the next); lends Analytical Appraisal + Sage 30 s; d20 table 1-3 denied (imp fine 1d6, never the last heart), 4-9 pending (next roll with advantage), 10-15 50% undone, 16-19 undone (1 in 10 a letter follows), 20 undone + the cause weakened, slowed, silenced. Clause 66B: 5% Auditor letter (a paper item). Silence stops it ("not read silently"). Fire damage -15%. | Saves / checks / spell attacks = harmful effects; "spellcaster" = the magicule cost; "sues the flame" = fire resist; "cannot be sold": flavour. |
| Holy Chains | `holy_chains` | Normal | Charm | Every second: evil creatures (undead tag, names with daemon/devil/demon/lich/vampire/...) within 3 blocks take 1 magic damage + Tensura Holy Damage; never players or pets. | Holy symbol = the wearer; turn = second; alignment = tag + names. |
| Pact of the Deck | `pact_of_the_deck` | Special | Charm | 54-card deck in the stack, whole at every dawn. +1 Luck. Throw a Card (1 s): nearest visible foe within 18 blocks; suit = damage type (spades thrown, hearts fire, clubs magic, diamonds cold + slow), 2-10 deal value/2; ace = +30% damage + Fragility 6 s; jack = Slowness III 3 s; queen = throw again (<=2); king = knock down unless it saves; joker = 8-entry wild surge table; critical 1/20 (not vs players) throws two. Draw the Pact (1 min): 3 cards discarded, magicules back 2% + 6% per ace (x2 with two aces: "free spell"), joker surges. Shuffle (5 min) refills at once. | Spell slots, Charisma uses/long rest, +5 card games: deck of 54 per day, Luck; "draw per spell level, two aces free, 99-00 regain a slot": Draw the Pact; dismissable deck = the charm; destroyed deck: not modelled. |
| Phantom Steps | `phantom_steps` | Rare | Charm, attune | Worn: silent steps (step sounds cancelled for others), no vibrations (sculk/Warden), no magma / sweet berry damage, Tensura Presence Concealment, Danger Sense lent. Click the Heels (3/day, 3 min): lends Shadow Motion + Spatial Motion 20 s, phases up to 8 blocks forward through creatures and walls (not in the Dungeon) to the farthest safe spot, invisible 2 s, +4 absorption. | Difficult terrain / 1d10 force for ending inside an object: safe-landing blink. Pressure plates and tripwires cannot be disabled in base Minecraft: Danger Sense warns instead. |
| Cloak of the Depths | `cloak_of_the_depths` | Rare | Back, attune | Worn: water breathing, Dolphin's Grace, Tensura Water Breathing + Hydraulic Propulsion, +0.75 reach. Ink Cloud (1 min): 5-block cloud, foes blinded/darkened/slowed, wearer invisible + fast 3 s. Writhing Touch (5 s, 6% magicules): 1 tentacle (+1 at XP 10/20/30), 3 dmg, Slowness + Weakness 5 s. Depths Grasp (2/day, 90 s, 10% magicules): aimed foe within 14, 5 dmg, rooted 5 s (Slowness VII, Movement Interference, Earth Lock), pulled in. | Swim speed 30 ft: Dolphin's Grace + Hydraulic Propulsion. Darkness spell: ink. Earthen Grasp + Grasping Vine: one ability. Str/Dex penalties: Slowness/Weakness. |
| Cloak of the Moth Rider | `cloak_of_the_moth_rider` | Rare | Back, attune | Flight of the Giant Moth (1/day): Hell Moth (else giant bat, else vex) ally 5 min via Fx.summon; Dismiss the Moth. Fearsome Visage (2/day, 60 s): foes within 6 that see you and fail their save: Tensura Fear, Weakness 8 s, Slowness 5 s, shoved back. Worn: Coercion lent; 1 in 5 hostile mobs (not bosses, not ones you hurt) will not choose you as first target. | Advantage on Intimidation: Coercion + the 1-in-5. Fear spell DC 15 / cone: size-based save, 6-block radius. 1 hour: 5 min (summon cap). Not a mount. |
| Nightstalker's Mantle | `nightstalkers_mantle` | Unique | Back | Worn: Ultrasonic Waves + Sense Soundwave lent; every 2 s hostile mobs within 20 blocks out of sight glow 2.5 s (not when Silenced); Presence Concealment in light < 8. Night Patrol (5 min, 1% EP): 60 s of survival flight (mayfly, cleaned up on logout/respawn/removal) + Gravity Flight lent + slow falling. Wing Dash (6 s): dash 9 blocks flying, 4 otherwise. Shriek (3 min, 1.5% EP): 10-block cone, 10 dmg (6 vs players), half on save; failed: Silence 4 s, stun 2 s; breaks invisibility, glows. | Blindsight: Ultrasonic Waves + glow outline. Fly 40 ft 1 h: 60 s + EP cost. 6d6 thunder capped for PvP; CON save: size-based chance; Stealth advantage: Presence Concealment. |
| Cape of Nine Lives | `cape_of_nine_lives` | Legend (vessel) | Back, attune | A lethal blow (not /kill or the void) leaves 1 HP: Resistance IV 5 s, absorption, Self Regeneration + Pain Resistance lent 30 s; second lethal blow within 2 s free. Each rescue = a cat trait: night vision, -0.5 max HP each (max -4 hearts), from 4: knocks nearby items about. Golden apple lifts cat traits. Ninth rescue: spent, 5 min "cat" (Weakness III, Mining Fatigue III, Speed II, Beast Transformation lent). Make a Wish (30 s): a Nether Star restores nine lives. Count the Lives. | Permanent cat form (Wish / Divine Intervention): 5 min + Nether Star. Greater Restoration: golden apple. Instant-kill negation: any death that is not /kill or void. |
| Cloak of Comfort | `cloak_of_comfort` | Special | Back | Worn: Thermal Fluctuation, Cold and Heat Resistance lent; freezing -90%, magma -90%, fire -15%, powder snow freeze cleared, Tensura Chill cleared. Make Camp (5 min): quiet level ground, heal 6, Regeneration 10 s, 4 food, sets the spawn point. | Desert/tundra climate: resistance skills + damage resists (no weather temperature in the game). Sleep anywhere: camp + spawn point. |
| Cloak of Dichotomy | `cloak_of_dichotomy` | Rare | Back, attune | A blow struck by hand ("smite") makes the next spell within 10 s +20% damage; a spell (magic, indirect magic, any non-vanilla damage type) makes the next blow within 10 s deal +0.6-4.8 radiant (x1.5 and Tensura Holy Damage on evil). Each used once. Read the Balance act. | Divine Smite = hand blow; save DC +2 = +20% spell damage; 1d8 = radiant bonus. |
| Cloak Mimic, tame | `cloak_mimic_tame` | Unique | Back | Shapechanger: cycles six cut/colour names (stored in custom data). Protective: +2 armor, acid damage x0.5, Tensura corrosion cannot apply, Corrosion Resistance lent. Alert: night vision, Danger Sense lent, hostile mobs targeting you from behind glow. Clingy: +0.2 knockback resistance. Hungry: each day change, 10% of magicules, else 4 food (exhaustion effects if nearly empty; 2 hearts if starving). | DC 18 strength to remove: not modelled. Hit dice / levels of exhaustion: magicules, food, Weakness + Slowness. |

Tensura skills lent (all gated at run time by the skill registry; ids from the TR skill classes): verified in the repo: `danger_sense`; from the registry
classes (not verified in play): `analytical_appraisal`, `sage`, `shadow_motion`, `spatial_motion`, `water_breathing`, `hydraulic_propulsion`, `coercion`,
`ultrasonic_waves`, `sense_soundwave`, `gravity_flight`, `self_regeneration`, `pain_resistance`, `beast_transformation`, `thermal_fluctuation_resistance`,
`cold_resistance`, `heat_resistance`, `corrosion_resistance`. Tensura effects: `fear`, `silence`, `flashed_blindness`, `fragility`, `holy_damage`,
`presence_concealment`, `movement_interference`, `earth_lock`, `chill` (all skipped when absent).

## Wave 3

# Wave 3: head, hands, belt and consumables (13 cards, all NEW)

Code: `relics/wave/Wave3*.java` (Wave3 defs and hooks, Wave3Rules pure + test, Wave3Util, Wave3Field, Wave3Head, Wave3Hands, Wave3Belt, Wave3Goods, Wave3Events).
Icons: `tools/relics/wave3_icons.py` (16x16 ASCII maps, grade rim sparkle). Lang fragment: `scratchpad/frag_relics/relics_wave3.lang.json` (39 keys: name, .desc, .card).
No 3D models (no weapon in this wave). HEAD relics are armour items whose worn layer the core generates from palette + look.

House rules: a long rest is a Minecraft day; a saving throw is Tensura existence (a target whose max EP is more than 3x the caster's resists; unknown caster EP means nothing resists); a bonus action is an act with a short cooldown; gold is Magic Stones and emeralds; "+5 to all rolls" is attributes plus lent Tensura skills.

| Card | id | Grade | Slot | What each ability became | Tabletop rule replaced |
|---|---|---|---|---|---|
| Arcanist's Circlet | arcanists_circlet | Special | Head (attune) | Magic Missile act: one of 3 motes flies at the creature you look at (36 blocks), 3 force damage, never misses; all three back 1 minute after the last; the motes left circle the head as particles | "half your level" damage = flat 3; 120 ft = 36 blocks; known spell = the act |
| Pride's Crown | prides_crown | Rare | Head (attune) | Aura of Superiority: +1 Luck; every 2 s hostile mobs within 6 blocks (no players/bosses) whose max EP does not exceed yours get Slowness. Hubris's Fall: once a day a Tensura charm/fright/madness effect (class checks: Fear, Hypnosis, Insanity, Confusion, Lust, Mind Control) aimed at you is refused and 5 s of tensura:fear (vanilla weakness fallback) goes to the sender or nearest foe in 12 | Cha checks = aura + Luck; Wisdom save DC 15 = EP check + boss half; long rest = day |
| Dreamwalker Cowl | dreamwalker_cowl | Rare | Head (attune) | Night vision + Tensura Magic Sense lent while worn (Eyes of the Dreamer); magical sleep (Tensura Sleep, Drowsiness) refused (Master Dreamer); 5 acts: Sleep (1 charge), Calm Emotions (1), Hallucinatory Terrain (2), Shared Imagery, Master Dreamer short dream (regen + resets phantom timer). 3 dream charges refilled by waking from a night's sleep | 30 ft ethereal sight = Magic Sense; 30-ft calm sphere = 9 blocks; 8 h illusion = 40 s + lent Presence Concealment; fall asleep at will = the short dream; DC 15 = EP check |
| Captain's Tricorn | captains_tricorn | Rare | Head (attune) | 8 charges, +2 per dawn (1d6+2 if riding a boat when the day turns); Command (1): Halt, 4 s frozen + calm on a soft mob; Captain's Order (2 per target, max 4): crew in 24 blocks get Strength/Speed/Resistance 3 min, foes within 12 are talked down for 30 s (charm book, broken by striking them); +10% swim speed | mass suggestion 3 h = 30 s foes / 3 min crew; DC 14 = EP check; sea vessel = boat |
| Godking Crown | godking_crown | God | Head (attune, Djinn vessel) | Ascend act (5% EP, 30 min cooldown): 10 min of flight (NeoForge creative-flight attribute + abilities), +4 attack, +5 armour, +2 toughness, +5 Luck, +0.5 KB resist, +15% speed, and three Tensura skills merged and kept lent (Danger Sense, Physical Attack Resistance, Sage). End: 12 radiant-magic damage to foes within 6 and 8 to you (never lethal), glowing, weakness; warnings at 1 min/30 s/10 s/5 s and a ring in the last 5 s; taking the crown off or Dismiss ends it with no blast; slow falling 15 s after | +5 to all rolls = attributes + skills; DC 25 Con save + 9d12 = the telegraphed non-lethal blast and the escape; fly 30 ft = flight |
| Claws of Greed | claws_of_greed | Special | Hands (attune) | +0.3 sneaking speed, Tensura Presence Concealment lent (gated); Seize: once a day a landed blow takes what the mob holds into your free hand | +2 Stealth/theft = sneak speed + concealment; once per long rest = per day |
| Gloves of Instant Cleanliness | gloves_of_instant_cleanliness | Special | Hands | Snap Clean act (10 per day): friends within 3 blocks lose nausea/hunger/blindness/poison/tensura:infection and stop burning; cobwebs and fire blocks within 3 swept (max 24); curses stay. Worn: hunger and nausea wiped every 5 s | 10 ft = 3 blocks; d20/day = 10; recharge at midnight = dawn |
| Elemental Hand Wraps | elemental_hand_wraps | Unique | Hands (attune) | Elemental Infusion act cycles fire/cold/lightning/thunder/acid/none; bare-fist blows deal +3 of the element with riders (ignite, slow+freeze, chain 60%, sonic knockback, corrosion) | +3d6 = flat 3; bonus action = 0.5 s pause; "magical" fists = element damage types |
| Thermo-Gloves | thermo_gloves | Special | Hands | Red/Blue act chooses +2 fire or +2 cold on bare fists; Ignite act (flint-and-steel logic or sets a creature alight); Freeze Water act (3x3x3 frosted ice that thaws); Steam Cloud (2 per day): scald 2, knockback, slow within 3, cloud of 6 radius 15 s darkens + slows foes; resist 15% fire and freezing | 10-ft cube = 3 blocks; 20-ft sphere = 6; prone = strong slowness; 10 min = 15 s |
| Gauntlets of Eldritch Ferocity | gauntlets_of_eldritch_ferocity | Rare | Hands (attune) | Eldritch Blast (2% mana): beam 2-4 (1 counts as 2); Eldritch Ferocity (4% mana): two beams up to 3 each (max 6 per cast); on d20 1-2 the extra beam bites the user 1.5-6 non-lethal and cuts max health (max 6) for 10 min or until sleep; +2 armour | treat 1 as 2 = floor 2; 3d12 necrotic ignoring resistance and killing at 0 = non-lethal bite (deliberately not kept: nothing here kills its wearer) |
| Alchemist's Belt | alchemists_belt | Unique | Belt (attune) | 6 vial acts, 1 use each per day: Healing (10 hp), Shield (8 absorption 3 min), Regeneration (sun: Regen II 1 min; else I; lends Tensura Self Regeneration), Enlightening (Strength 3 min for friends within 10), Love (charms a bendable mob 90 s), Undeath (3 min no air/food, Poison Resistance lent, Undead Fortitude once); +2 max health, +1 Luck | ingredient recharge = dawn; level/CR dice = flat numbers; "artificer or wizard" attunement not kept; DC 18 = EP check; 12 h = 3 min |
| Genie Gin | genie_gin | Unique | Consumable | One act, one wish for a day (24000 ticks): Power (Strength II, Health Boost III, Resistance + 3 lent Tensura skills), Fortune (24 Magic Stones, 32 emeralds, 32 gold, Luck II), Fame (Hero of the Village + humanoids within 24 not shrugging it off stop hunting you until struck); sneak-drink turns the bottle to the next wish without using it up | ability score 30 = effects + skills; 10,000 gp = stash; DC 18 = EP check; 24 h = one MC day |
| Mad Marbles | mad_marbles | Special | Consumable | Pour a 3x3 patch in front of you (30 s, 3 per player): first creature to step on it falls (strong slowness) and the marbles pop in sparks (blind 2-8 s); others slip at most every 3 s | DC 17 / DC 19 saves not rolled; bag of 200 = one pour |

Tensura skill ids used: tensura:magic_sense, tensura:danger_sense, tensura:physical_attack_resistance, tensura:sage, tensura:poison_resistance (verified in the repo); tensura:presence_concealment, tensura:self_regeneration (not verified, gated: Fx.lendSkill returns false and nothing breaks if absent). Tensura effect ids with vanilla fallbacks: tensura:fear, tensura:sleep, tensura:corrosion (all already used elsewhere in the repo); tensura:infection (removal only).

## Wave 4

# Wave 4: the eight neck relics (key `w4`)

All eight are NECK relics (Curios necklaces made by the core; worn look from palette + look) and all need attunement. None is a
weapon: no 3D mesh and no engravings; the 16x16 sprite is the item model (`item/generated`). Code: `relics/wave/Wave4.java` (defs +
`hooks()` + the public `FLAG_OVERSEERS_MIGHT` / `walkerBonus`), the act bodies `Wave4CovenActs`, `Wave4ForestActs`, `Wave4ChargeActs`
(Glass + Dreamer), `Wave4BeastActs`, `Wave4MiscActs` (Voice + Sea of Stars), the listeners `Wave4Handlers`, the once-a-second pass
`Wave4Upkeep`, the glue `Wave4Store` (stack custom data, the wearer's kept data) and `Wave4Look` (crosshair target, ambient effects), and the
pure rules `Wave4Timers`, `Wave4Charges`, `Wave4Glass`, `Wave4Echo`, `Wave4Forest`, `Wave4Beasts`, `Wave4Charm`, `Wave4State` (all
tested: `Wave4ChargesTest`, `Wave4EchoTest`, `Wave4RulesTest`). Icons: `tools/relics/wave4_icons.py`.

State: charges, the Forest blessing, the Dreamer's gills, the Coven's once-a-day stamp and the Beast Masters' creatures live in the amulet's
custom data (they travel with the item and survive relogs). The Pendant of Echoed Souls keeps its once-a-day stamp on the wearer
(`PlayerPersisted/nusmp_w4_echo_at`) so two pendants cannot give two lives. Short-lived marks (Gaze, charm, Truesight, the Glass's armed d6, the
pendant's guard) are bounded id tables in memory, emptied on logout, respawn, dimension change and server stop.

| Card | id | Grade | Attune | Vessel | Costs |
|---|---|---|---|---|---|
| Amulet of the Coven | `amulet_of_the_coven` | LEGEND | yes | yes | Watcher 2% mana / 30 s; Presage 3% mana, 3 hearts of blood, once per day; Gaze 6% mana, 2 per day / 30 s |
| Pendant of Echoed Souls | `pendant_of_echoed_souls` | UNIQUE | yes | no | none (reactive, once per day) |
| Amulet of the Forest | `amulet_of_the_forest` | LEGEND | yes | yes | each figurine 2% mana / 5 s; a blessing lasts a game day |
| Arcane Glass of Seeing | `arcane_glass_of_seeing` | RARE | yes | no | 1 of 6 charges / 5 s |
| Voice of the Mountain | `voice_of_the_mountain` | SPECIAL | yes | no | 2% mana / 60 s |
| Amulet of the Sea of Stars | `amulet_of_the_sea_of_stars` | UNIQUE | yes | no | Star-Eyed 3% mana, once per day |
| Pendant of the Dreamer | `pendant_of_the_dreamer` | RARE | yes | no | 2 / 3 / 3 of 6 charges |
| Beast Masters Amulet | `beast_masters_amulet` | RARE | yes | no | 2 s cooldowns |

## What each ability became

| Card | Mechanic in the game | Tabletop rule replaced, and by what |
|---|---|---|
| Coven: Distant Watcher | Hostile creatures within 40 blocks in the half in front of you glow 12 s; passive Night Vision | "close your eyes and see through it" + darkvision 60 ft + 180 degree arc -> outlines + Night Vision, front half only |
| Coven: Bloody Presage | 3 hearts of blood (cannot take the last ones) -> the creature under the crosshair within 96 blocks (through walls) or whoever hurt you in the last minute (any distance, same dimension) glows 20 s (40 s if it is the one that drew your blood); chat line with name, health, position; once per game day (own stamp on the amulet) | Scrying without slot/components -> blood cost; Wisdom save with disadvantage -> blood-bound = cannot hide, double glow; "10 slashing damage" trigger -> dropped (any time you can pay); "until next dusk" -> once per game day |
| Coven: Weakening Gaze | 10 block burst: foes lose Resistance, Fire Resistance, Absorption, get Weakness 8 s and take +30% from every source for 8 s (hook on incoming damage); allies, pets, summons spared (`Fx.foesAround`); 2 per game day, 30 s cooldown | 30 ft sphere + DC 17 Con save -> 10 blocks, no save; lose resistances / immunities become resistances -> strip resistance effects + 30% more damage (a true immunity is not lifted); "choose up to five unaffected" -> allies always spared |
| Pendant of Echoed Souls | LivingDeathEvent (HIGH): a killing blow (not the void or /kill) -> cancelled, health 1, fire out, 1-4 spectral warriors (`minecraft:vex` through `Fx.summon`, set to 10 health / 5 damage, a minute), and until the last one falls (or 60 s) all damage to the wearer is cancelled and no mob may take the wearer as target; if no warrior spawned, absorption shell for 5 s; once per game day on the wearer; warriors never target players | once per long rest -> once per game day; 1d4 -> 1-4; AC 14, HP 10, 1d6 + proficiency -> 10 HP, ~5 damage; "cannot be the target of an attack or spell effect" -> target change cancelled + damage cancelled (spell effects that are not damage still land) |
| Forest | Seven acts: touch bear / cat / beetle / raccoon / owl / peacock, and "Send the Spirit Away". A blessing is stored on the amulet (spirit + end tick, 24000 ticks), applied once a second as transient attribute modifiers and ambient effects (bear +20% scale, +3 attack, +0.3 knockback resistance, +4 health; cat +25% speed, +4 safe fall, +0.15 jump; beetle +4 armor, +2 toughness, +4 health, heals x1.5 via LivingHealEvent; raccoon +2 luck and no blindness / darkness / nausea; owl Night Vision and charm effects stripped; peacock Hero of the Village + Charm Person: humanoids (vanilla + Tensura id list) within 5 blocks forget the wearer and will not target a player for 20 s, 40+ max health resists and is immune for a game day) | advantage on ability checks -> attribute / effect bonuses; one size larger -> +20% scale; Hit Die maximum -> +50% healing; saves against illusions / charm -> stripping the effects; Charm Person DC 15 at will, immune after a success of the save -> cooldown-limited act, immune after a resist; 24 hours -> a game day |
| Arcane Glass of Seeing | 6 charges kept on the amulet, 1d4+2 back when the dawn day number changes (checked on use); use = spend 1 and roll a d6: 3-6 next blow +50%, 1-2 next blow -30%, for 10 s or one blow (hook on the wearer's outgoing damage); spending the last charge: d20 = 1 cracks the gem, which skips the next dawn's recovery | bonus action + advantage / disadvantage -> +50% / -30% damage on the next blow; "the gem shatters and the amulet becomes non-magical" -> a one-dawn crack (the relic is never destroyed, the lead's rule: nothing is deleted) |
| Voice of the Mountain | Rousing Call: you, your tamed creatures and same-team players within 24 blocks get Speed I and Haste I for 20 s; 60 s cooldown | there is no bard: "attunement by a bard" -> plain attunement; "bardic abilities +25 ft" -> the horn carries a bard's call itself at the widened reach |
| Sea of Stars | Night Vision always; Star-Eyed once per game day: 5 minutes of Truesight (invisible creatures within 24 blocks outlined every 2 s, blindness and darkness removed); resist 40% to magic, indirect_magic, wither and (ids by name, skipped silently if absent) tensura holy_damage, light_elemental, darkness_elemental, curse, magic_generic; Overseer's Might: a flag `overseers_might` (see Integration) | darkvision 120 ft -> Night Vision; Truesight 1 hour -> 5 minutes; resistance (half) -> 40% less; the Astralis rule needs Wave 1's cooperation |
| Dreamer | 6 charges on the pendant, 1d6 back at each midnight; Blessing of the Deep (2): Water Breathing 10 min; Features of the Ancient (3): gills for 10 min (Water Breathing + full water movement efficiency), use again to close for free; Force of Mhigh'Raine (3): a creature in sight within 90 blocks takes 3-12 magic damage through `Fx.damage` (halved on a coin-toss "save", at most 6 vs players), nausea if not saved | Water Breathing spell -> the effect; swim speed = walking speed -> water movement efficiency +1; DC 18 Wisdom save -> coin toss; 3d6+2 psychic -> 3-12 magic scaled to hit points; 8 hours -> 10 minutes; "by a warlock" -> plain attunement |
| Beast Masters Amulet | Store Creature (crosshair, 6 blocks): your own tamed creatures freely, others only at half health or less, never another player's pet, never a relic summon; weight 1 / 2 / 5 / 10 by max health 8 / 16 / 40 / 80, 10 points; the creature's whole saved data (minus UUID, passengers, leash, brain) goes into the amulet's custom data (6000 characters at most each); Release Creature lets the last one out in front of you with its health | CR ladder -> max-health ladder; willing -> owned; Wisdom check DC 14 -> wounded to half health |

## Integration the lead / other waves must know

* **Astralis (Wave 1)**: the Amulet of the Sea of Stars raises the flag `overseers_might` while worn and attuned. Wave 1's Dimension Walker
  pool is `Wave1Casts.WALKER = new Pool("walker", 3, 0, 0)`; its maximum should become `3 + Wave4.walkerBonus(player)` (the helper is
  public, returns 2 or 0), i.e. build the pool for the cast with `new Wave1Rules.Pool("walker", 3 + Wave4.walkerBonus(p), 0, 0)` (the key stays
  "walker", so stored charges carry over). Wave 4 cannot do this itself.
* The core runs `Wave4.hooks()` once from the mod constructor; it registers nine NeoForge game-bus listeners (death HIGH, incoming damage HIGH,
  heal, change target, server tick, logout, changed dimension, respawn, server stopping).
* Needs from the core engine: `ActCtx.stack()` is the live worn stack (the acts edit its custom data); an item may carry several acts
  (Forest: 7, Dreamer: 3, Beast Masters: 2, Coven: 3); the acts that validate after the engine's cost check (Presage, mind-blast...) spend the
  mana / use first when they refuse (their messages say so), so mana costs are kept small (2-6%).

## Tensura merges (owner directive: use Tensura where base Minecraft cannot)

| Relic / ability | Tensura piece used | Id status |
|---|---|---|
| Coven, Distant Watcher | `Fx.lendSkills`: Magic Sense + Danger Sense for 15 s on top of the glow outlines | both verified in the repo |
| Coven, Bloody Presage | `Fx.lendSkill` Sage for 30 s; the chat line adds the target's magicules as a percent (`TensuraStorages` + `EnergyHelper.getMaxMagicule`) | Sage verified; the stat reads compile in the repo |
| Coven, Weakening Gaze | `NihilityZone.start` (the mod's anti-magic zone, 8 s, 10 blocks: shatters barriers and area spells, silences foes, jams their Tensura skills, strips a buff; owner and allies spared) plus the vanilla Resistance strip and the +30% damage mark | existing mod class |
| Echoed Souls | while the guard stands, every effect in the tag `TensuraTags.MobEffects.SKILL_DEBUFF` is removed from the wearer each second ("cannot be the target of a spell effect") | the tag field is in the Tensura jar |
| Forest: cat, beetle, raccoon, owl | lent skills: Danger Sense; Physical Attack Resistance + Poison Resistance; Sage; Magic Sense (lease 100 ticks renewed every second while the blessing lasts and the amulet works) | all five verified |
| Voice of the Mountain | each player roused draws 3% of max magicules back (`IExistence.setMagicule`) | the stat API compiles in the repo |
| Sea of Stars, Star-Eyed | Magic Sense + Sage lent for the 5 minutes of Truesight | both verified |
| Sea of Stars, Ancient Amethyst | also resists the tensura damage types holy_damage, light_elemental, darkness_elemental, curse, magic_generic (read off `TensuraDamageTypes`; the id strings are inferred, an absent one is skipped) | inferred, unverified |
| Dreamer, Force of Mhigh'Raine | damage type `tensura:mind_crush` when the damage-type registry has it (else `minecraft:magic`); a failed resist adds `tensura:fear` and `Nullification.bleed(target, 0.05)` (5% of its magicules, which regenerate) | mind_crush and fear gated at run time; bleed exists in the repo |
| Glass, Gills, Beast Masters | real Minecraft mechanics; nothing on those cards needed Tensura | n/a |

## Wave 5

# Relics of Many Worlds: wave 5 (seven neck relics)

All seven are Curios necklaces (`RelicSlot.NECK`), all need attunement, none has a 3D hand mesh (they are worn, not held): the inventory sprite
(`textures/item/<id>.png`, 16x16) and a flat model (`models/item/<id>.json`, parent `item/generated`) are written by `tools/relics/wave5_icons.py`.
Code: `relics/wave/Wave5*.java` (`Wave5` defs and `hooks()`, `Wave5Hooks` the listeners, one class per relic, `Wave5Pure` the testable rules,
`Wave5Data/Attr/Aim/Skills` helpers). Item state (charges, souls, energy, the Safeguarding record) is kept in the stack's custom data under
`nusmp_w5`; a wearer's short timers (veil, shroud, ward, dragon form) are in the player's persisted data under the same key. No static holds a level,
entity or player, so there is nothing to clear on server stop.

How the core settles things (read from RelicEngine): the cooldown, mana %, EP % and uses-per-day are paid BEFORE the ability's code runs, so wave 5
keeps its own charge pools (Warmage, Lost Souls) and its own once-a-day mark (Lovelorn Locket) where a refused attempt (no foe, no soldier near)
must not use anything up.

| Card | id | Grade | Slot | What each ability became | Tabletop rules replaced, and how |
|---|---|---|---|---|---|
| Amulet of the Warmage | `amulet_of_the_warmage` | RARE | NECK | **Arcane Awareness**: 50% off `magic`, `indirect_magic`, `sonic_boom` damage; +8% max magicules (passive attr); Tensura magicule regeneration +10%, magic cost -5%, magic resistance +0.1 (typed modifiers, once a second); Magic Sense lent while worn (`Passive.skill`). **Extra tricks**: four abilities (Fire Bolt, Ray of Frost, Shocking Grasp, Acid Splash) drawing on one shared pool of 4 charges in the stack; 1d4 back each new day; a cantrip with no foe spends nothing; damage +1 per 10 levels (max +2), never above 6 per target. | +3 Arcana became the Tensura magic stats and Magic Sense; resistance to Force became 50% off the three magic damage types (Minecraft has no Force); "Cantrip from the Wizard list" became four chosen cantrips; the long rest became the new day. |
| Hemlock's Amulet | `hemlocks_amulet` | LEGEND (vessel) | NECK | **Wear the Mask of the Living** (3% mana, 10 s cooldown): 5 min (8 Awakened, 10 Exalted): undead cannot target you and forget you (bounded sweep, 16 mobs a second), Glowing cannot be applied, Tensura presence concealment +0.5 and the Presence Concealment skill lent. **Let the Mask Fall** ends it. Wither damage -50% always. **Awakened** (energy 60): +4 max health, +2 toughness, +25% spiritual health, longer mask. **Exalted** (240): +8 / +4 / +50%, and the **Reliquary**: once per game day a lethal blow is turned aside, you stand at 40% health with Resistance IV 6 s, then are soul-torn (Weakness 90 s, Slowness 30 s). Energy: +2 per undead killed (+12 a boss), +1 from a magic hit (one sip per 5 s), kept in the amulet. | "Appears as a normal human male", "immune to abilities that detect and affect the undead", "advantage on saves vs divination" became the mask, undead-proofing, Glowing immunity and Tensura concealment; "Awakened: hold new enchantments" became the Awakened bonuses (invented); "Exalted: absorbs the soul of the wearer, becoming its Reliquary" became the Exalted rescue and the spiritual-health bonus (invented, and priced by a one-day cooldown and a soul-torn penalty). The rescue does not work against /kill or the void. |
| Amulet of Safeguarding | `amulet_of_safeguarding` | UNIQUE | NECK | **Meditate on the Record** (10 s cooldown): only on a full-moon night, once per 8-day lunar cycle, 30 s without moving 1.6 blocks, being hurt or the night ending; records health, hunger, experience, Tensura magicules and aura, position/dimension and the item list. On a lethal blow (not /kill) the amulet breaks: you are restored (health = max(record, half), hunger/XP/magicules/aura never lower than the record, all effects cleared) and moved back to where you sat at the end of the tick; items not on the record stay where you fell (relics never); the amulet cracks and mends after two more full moons. | The hour became 30 s; the month became the lunar cycle; "items the amulet had no record of disappear" became "stay where you fell" (the record only removes, never creates: no copying, and a unique relic is never destroyed or duplicated); "the amulet breaks" became "cracks and mends" (nothing is deleted); memories became the Tensura existence points; if the place of the record has no room to stand the wearer is restored where they are and keeps everything. |
| Amulet of the Huntress | `amulet_of_the_huntress` | RARE | NECK | **Predator's Stealth**: sneaking speed +50%; a mob farther than 8 blocks cannot target a sneaking wearer and loses them (bounded sweep); Tensura presence concealment +0.3 and the Presence Concealment skill lent while stalking. **Weapons of Choice**: +1 damage with swords, axes, tridents, arrows; +2 with claws and teeth (beastfolk, vampire, lizard/dragon-kin race or blood; bare-handed counts); a sneaking hit on a creature that has not noticed you adds +2. | "+3 on Stealth checks" became the creeping speed, the 8-block rule and Tensura concealment; "shape-changed or have claws/teeth" became the race/blood test (the mod has no shape-change state to read); slashing/piercing became the item tags and arrows. |
| The Amulet of Lost Souls | `the_amulet_of_lost_souls` | LEGEND (vessel) | NECK | Four abilities: **Soul Spell I / II / III** (1 / 2 / 3 charges, cooldown 1 / 2 / 5 s) cast the current soul's spell of that tier; **Commune with the Souls** turns to the next soul (first turn of a day free, later ones cost a charge). 10 charges, 1d10 back each new day. Spending the LAST charge risks a natural 1 on a d20: the soul in use is freed for good and its spells are gone; the eighth freed shatters the ruby and the amulet is no longer magical (it stays as an item). The 24 spells are listed in the lang card; every spell stays under the 6-damage-per-target cap. Tensura merges: Lesser Restoration + Poison Resistance, Spirit Guardians + Physical Attack Resistance, Misty Step + Danger Sense, Aura of Vitality + Self Regeneration (if present). | Spell slots became charges; "choose a soul at the end of a long rest" became Commune; spells became the nearest Minecraft or Tensura effect (rays, cones, blasts, heals, blinks, wards); the last card line ("snilloc's snowball swarm and spirit shroud (3 charges) [?]") is read as Ice Knife 1, Snowball Swarm 2, Spirit Shroud 3; Fireball is a show of particles (no block is touched). |
| Lovelorn Locket | `lovelorn_locket` | SPECIAL | NECK | **Fair dealing**: Hero of the Village kept up while worn (villager prices). **Billet for the Night** (once per game day, counted by the wearer's own mark so a failed try keeps the day): beside a weaponsmith, armourer, toolsmith or iron golem within 24 blocks: respawn point set here, half health, fed, Regeneration II 20 s. | "Advantage on Persuasion with the same gender" became Hero of the Village (a roll that does not exist became a price that does); "any former soldier will house you" became the billet; "turns gold on reunion" is flavour only. |
| Amulet Of Dragon Rage | `amulet_of_dragon_rage` | LEGEND (vessel) | NECK | **Speak the Command Word** (3% of magicules and aura, once per game day): Adult Silver Dragon for 2 min: +8 armour, +4 toughness, +5 attack, +25% speed, +0.8 knockback resistance, +1 step height, +32 safe fall, creative flight (NeoForge `creative_flight`, abilities fallback), a 40-point pool as absorption, body +30% (Tensura height/width), magic resistance +0.3, and Physical Attack Resistance + Danger Sense + Magic Sense lent as one merged form; cold and falls do nothing. **Silver Frost Breath** (8 s cooldown): 9-block cone, 6 cold damage to up to 8 foes, Slowness III, freeze. **Speak the Word Again** ends it (also on time, a relog, death, taking the amulet off). A blow that breaks the pool: the form ends; leftover more than the pool (40) = death; leftover that would reach 0 HP = left at 1 HP, stunned; otherwise taken normally. 30% off freezing damage always. | Monster Manual stats became the modifiers (mental stats stay yours); one hour became 2 minutes; the 24-hour lockout became once per game day; "HP = lesser of current and new maximum" became the pool (transforming does not heal you); "unconscious and unstable" became 1 HP and stunned; "instant death if remaining damage exceeds the dragon's maximum" kept as the pool rule; the Minecraft model is not swapped (size stats + particles). |

## Numbers and fairness
- No ability instantly kills; every active has a cooldown (most 1 to 10 s; the dragon form 24000 ticks plus 3% EP; the Mask 200 ticks plus 3% mana).
- Damage per target per cast never exceeds 6 (`Wave5Pure.cap`), on top of the core's own 30%-of-max-health cap on players; Fx.damage also goes through the balance law.
- Creative and spectator players are never targets (all targeting goes through `Fx.foesAround`).
- Per-tick work: one listener, a once-a-second pass per player (staggered by entity id), the dragon five times a second only while a form is on, the Safeguarding move only when one is pending; sweeps are capped at 16 mobs.
- Every temporary thing ends on death, relog, respawn (`Wave5Hooks.end`): the dragon form and its wings, the Spirit Guardians ward and the Spirit Shroud. The Mask ends by time, by taking the amulet off, and its modifiers are transient.

## Tensura directive (skills and stats)
Skill ids used, all through `Fx.lendSkill/lendSkills` / `Passive.skill` (an id the registry lacks is skipped silently): `tensura:magic_sense`, `tensura:danger_sense`,
`tensura:physical_attack_resistance`, `tensura:poison_resistance` (verified to exist in the repo); `tensura:presence_concealment`, `tensura:self_regeneration` (NOT verified).
Tensura attributes (typed `TensuraAttributes` holders, transient modifiers): `MAGICULE_REGENERATION_MULTIPLIER`, `MAGIC_COST_MULTIPLIER`, `MAGIC_RESISTANCE`,
`PRESENCE_CONCEALMENT`, `MAX_SPIRITUAL_HEALTH`, `HEIGHT_MULTIPLIER`, `WIDTH_MULTIPLIER`; plus the passive attribute id `tensura:max_magicule` (the core's own id). Their scales are not
verified (flat values were chosen small, percentages are of the base). Tensura existence (magicules, aura) is recorded and restored by the Safeguarding amulet through `TensuraStorages.getExistenceFrom`.

## Tests
`Wave5PureTest` (charge refill, soul table and freeing, Hemlock tiers, lunar calendar, trim-to-record, the dragon's lethal-hit rule, Huntress and cantrip numbers) and `Wave5DefsTest`
(ids, grades, slot, vessel flags, attunement, descriptions, act counts and costs). Nothing could be run in the game.

## Wave 6

# Wave 6: ten chest armours and robes

All ten are CHEST relics (real armour items made by the core from the def: armour value, toughness, palette, look). Code: relics/wave/Wave6*.java; pure maths Wave6Pool/Wave6Pools/Wave6Tune (tested). Charges live on the stack (data key w6_<pool>_c/_d), refill per Minecraft day (Overworld day number), at most four missed dawns paid. Order of preference followed: Minecraft mechanic, then Tensura skill/effect/damage type (lent through Fx.lendSkills / Passive.skill, every id gated by the registry), then an invented rule.

| Card | id | Grade | Slot | What each ability became | Tabletop rule replaced -> how |
|---|---|---|---|---|---|
| Skycrystal Armor | skycrystal_armor | LEGEND (vessel, attune) | CHEST 10/3.5 | 10-charge pool (1d8+2 at dawn; last charge: d10=10 -> 1d4+1); Spell Absorption (auto, magic damage cancelled, +1..4 charges); acts Absorb Elements 1, Shield 1, Globe 6, Invulnerability 9; Timelessness shaves poison/wither/hunger/infections; passives +4 HP, kb res, -4% speed | advantage on saves vs magic -> magic damage x0.75, 30% effect save, Tensura Magic Resistance + Abnormal Condition Resistance; spells -> acts with Tensura skills merged (4 element resistances; barrier; magic nullification + jamming; physical/magic/pain nullification); stop aging -> time-effect shave + Self-Regeneration; STR 15 / Stealth disadvantage -> -4% speed; save DC -> none |
| Slime Armour | slime_armour | UNIQUE | CHEST 7/1 | slashing x0.65, bludgeoning x1.3; grapple effects refused; Slime Form 3/day, 60 s, scale x0.45, no items/attacks, no fall damage | advantage vs grapple -> restraint effects (webbed, earth lock, movement interference, Slowness IV+) refused + Paralysis Resistance; 1 inch gap -> 45% size; Tensura physical/pierce/pain resistance in form; "0 HP ends it" -> ends on death |
| Armor of the Defiant Dawn | armor_of_the_defiant_dawn | UNIQUE (attune) | CHEST 8/2 | Dawnlight toggle (night vision, invisible glow in 16, darkness lifted, Magic Sense + All-Seeing Eye); Luminous Retribution 3 charges (1d3 at dawn), auto on a hit >= 4, or by hand (5% mana) | CON save -> distance (<=3 blocks fails; undead always); 4d8 -> 6 (x1.5 undead), Tensura light/holy damage + flashed blindness; 30/15 ft -> 16/6 blocks |
| Suit of the Astral Overseer | suit_of_the_astral_overseer | LEGEND (vessel, attune) | CHEST 6/2 | Zero Gravity toggle (creative-style flight x2 speed, hunger cost, only grants what it took), Slow Falling always; Into the Void 2/day cancels a magic hit + Strength/Speed + Thought Acceleration; Traveler: cleanse + resist on dimension change | fly speed +30 ft -> flight x2; opposing spell roll -> two daily wards; advantage on next spell -> Thought Acceleration; plane travel side effects (memory loss) -> sickness effects lifted (nausea, blindness, darkness, tensura confusion/insanity) from self and allies <= 8 blocks; "by a spellcaster" not enforced |
| Shadow Robes | shadow_robes | UNIQUE (attune) | CHEST 4/1 | freezing x0.5 + Cold/Darkness resistance; Black Veil (sneak in light <=7: invisible + Presence Concealment); Shadow Step 2/day, 12 blocks into a dim place, drags one hunting mob, leaves a 3-pulse cloud | hide action -> sneak invisibility; 60 ft -> 12 blocks; grappled creature -> mob hunting you; CON DC 14 5d12 -> 3 x 2 darkness dmg + wither (6 total) |
| Mistwalker Robe | mistwalker_robe | RARE (attune) | CHEST 3/0.5 | 5 charges (1d4+1): Misty Step 1, Levitate 1, Gaseous Form 2, Far Step 4, Wind Walk 5; each gives 2 absorption hearts; last charge d20=1 -> mist slip | +1 AC -> absorption; robe teleports away -> wearer thrown a few blocks, robe kept; spells -> Tensura Gravity Flight, Wind Transform, Spatial Motion (1 min), Wind Manipulation |
| Neket's Practical Mage Robe | nekets_practical_mage_robe | RARE (attune) | CHEST 3/0.5 | Left/Right Pocket (27 slots each, saved on the robe); Lost and Found (in combat, a non-sneak toss returns to inventory); stored Mage Armor x5 (+1 at dawn, Store Spell 8% mana, optional hook onSpellCast), auto-cast when hit; Mage Armor +4 armour 8 min + Multilayer Barrier | Bag of Holding -> chest menus; mental command -> acts; pickpocket disadvantage -> Danger Sense; four staves hands-free -> +5% max magicules; Ring of Spell Storing -> stored charges |
| Armor of Plot | armor_of_plot | LEGEND (vessel, attune) | CHEST 9/4 | dodge chance 6%..33% by missing health; death save (3 plot points, +1 per dawn) to a third of health; at 0 points Luck -3 and no save/dodge | +4 AC -> 9/4; +4 saves -> resist fire/explosion/projectile/lightning 20% + Abnormal Condition Resistance; "negates half damage" folded in; "DM's discretion loses stats" -> 0 plot points (armour value/resists stay: the core's passives cannot be toggled); -3 Wisdom -> Luck -3 |
| Z'ar Sancuis - Plate of the Red Prophet | zar_sancuis_plate_of_the_red_prophet | LEGEND (vessel, attune) | CHEST 10/4 | Legendary Resistance 2/day (auto on harmful effects >= 2 s, 6 s free window; by hand cleanses the nearest afflicted ally); Healing through Vengeance +2 HP per hit on a hostile; Prophet's Guidance +3 damage and Ally Boost for allies within 6 blocks | twice per rest for Guidance -> 3 s cooldown; spell modifier -> flat values; STR/CON not used |
| Skitch's Smuggling Coat | skitchs_smuggling_coat | UNIQUE | CHEST 5/0.5 | Deep Pockets 54 slots, Seam Pockets 27 slots, saved on the coat | 50 pockets in 3 sizes -> two bags; Investigation DC 20 / action to withdraw -> none; optional attunement -> works for anyone (no .attune()) |

## Notes
- Acts are charged cooldown before the body runs (core): charge-pool acts keep short cooldowns; failed casts (no safe landing, empty pool) cost only the cooldown.
- Hooks needed from outside: none required. Optional: `Wave6Events.onSpellCast(ServerPlayer)` from GrimoireBook.onCast fills Neket's robe.
- Unverified Tensura ids are listed in Wave6Skills (javadoc); all are skipped silently if absent.

## Wave 7

# Wave 7: the ten chest armours (key `w7`)

All ten are CHEST relics (real armour items made by the core; worn layer generated from palette + look). None is a weapon, so no
3D mesh and no engravings. Every ability reads a per-armour flag (`w7_<name>`, set by `Passive.flag`), so the core's attunement
rules (max 3, only the attuned player) decide whether it works. Code: `relics/wave/Wave7.java` (defs + hooks),
`Wave7Events.java` (act bodies, 10-tick player pass, damage/death/visibility/effect hooks), `Wave7Math.java` (pure rules, tested),
`Wave7Skills.java` (the Tensura skill ids). Timers are absolute game-time stamps in the player's persistent data (`nusmp_w7_*`); nothing
static holds players or levels.

Owner directive (use Tensura where Minecraft cannot; never drop an ability): nothing on these cards is dropped. Tensura is used in three ways:
**skills lent** (`Passive.skill` while worn, `Fx.lendSkill(s)` for an act or a state), **Tensura stats as gates** (EP, maximum magicules, maximum
aura read with `EnergyHelper`), and **Tensura/Anti-Magic tools** (`tensura:silence`, `tensura:presence_concealment`, `Nullification.jamAll` /
`shatterBarriers`, the grimoire seal flag `ManaLaw.SEALED`, the existence storage).

| Card | id | Grade | Attune | Vessel | Armour / toughness |
|---|---|---|---|---|---|
| Armour of the Stone Sentinel | `armour_of_the_stone_sentinel` | RARE | yes | no | 7 / 1.0 |
| Starstudded Leathers | `starstudded_leathers` | UNIQUE | yes | no | 7 / 2.0 |
| Coat of the Duelist | `coat_of_the_duelist` | LEGEND | no (one-of-a-kind) | yes | 6 / 2.0 |
| Suit of the Golden Shadow | `suit_of_the_golden_shadow` | LEGEND | yes | yes | 7 / 2.0 |
| Armor of the Lionheart | `armor_of_the_lionheart` | LEGEND | yes | yes | 10 / 3.0 |
| Armor of the Beheaded | `armor_of_the_beheaded` | UNIQUE | yes | no | 9 / 2.0 |
| False Dragon Armor | `false_dragon_armor` | UNIQUE | yes | no | 9 / 3.0 |
| Soul Armor | `soul_armor` | RARE | yes | no | 7 / 1.5 |
| Arcane Conduction Armor | `arcane_conduction_armor` | RARE | yes | no | 5 / 1.0 |
| Delphyne's Cursed Armor | `delphynes_cursed_armor` | LEGEND | yes | yes | 11 / 4.0 |

## Tensura skill ids lent (verified = already used by the repo's meals)
Verified: `tensura:magic_sense`, `tensura:danger_sense`, `tensura:physical_attack_resistance`.
Not verified (named after Tensura's skill registry fields; `Fx.lendSkill` skips an unknown id silently): `thought_acceleration`, `thought_communication`,
`abnormal_condition_resistance`, `flame_attack_resistance`, `heat_resistance`, `dragon_skin`, `magic_resistance`, `magic_aura`, `shadow_motion`,
`hero_haki`, `chant_annulment`. Tensura effects: `tensura:silence` (verified, used by the repo), `tensura:fear` (verified), `tensura:presence_concealment`
(effect; confirmed in the API dump and used by wave 2).

## What each ability became

| Card | Mechanic in the game | Tabletop rule and what it became |
|---|---|---|
| Stone Sentinel | Half hunger (half of the exhaustion gained per second is discarded); half the phantoms near the wearer do not spawn; **stony skin = Tensura Physical Attack Resistance (Passive.skill)** + 15% knockback resistance, -8% speed; trance after 8 s still (visibility x0.2, Night Vision, **Magic Sense lent**), deep trance at the card's literal **10 minutes** (visibility x0.1, Invisible + Presence Concealment) | eat/sleep half -> half hunger + half phantoms; +5 Insight/Investigation/Perception -> Magic Sense + Night Vision; 10 minutes -> kept as the deep trance (8 s trance first); -2 Dex -> -8% speed; "tough and stony" -> Physical Attack Resistance |
| Starstudded Leathers | Emanate Starscape (3/day, 10% mana): 1 min, radius 5 (7 at night), follows the wearer, outlines invisible creatures, burns whoever hits the wearer from inside (5; 3 vs players), **lends Magic Sense + Thought Communication**; Star Bolt (3% mana): 5 magic damage + Glowing; stealth visibility x0.75 (x0.52 sneaking), +0.20 sneaking speed | 3 charges at dusk -> 3 uses per day; Dex save -> fixed hit; **cut-off card text ("... bonus action to send")**: played as two readings, the Star Bolt (send starlight) and Thought Communication (send a message); +5 Stealth -> visibility |
| Coat of the Duelist | Off hand empty: melee damage x0.78; a melee hit gives Speed II 2 s; **Danger Sense (Passive.skill)**; En Garde (5% mana + 0.5% EP, 30 s): next melee blow within 6 s cancelled, attacker shoved and takes 5, Speed II 4 s, **Danger Sense + Thought Acceleration lent for the stance**; Lunge (3% mana): 6-block dash, 5 damage to the first foe ahead; +8% move speed, +10% attack speed | double Dex to AC -> melee reduction + Danger Sense; free move on a hit -> Speed buff + Lunge |
| Suit of the Golden Shadow | **Rogue gate = maximum aura 500** (see below); visibility factor deepens/weakens with training; **Magic Sense (Passive.skill)** + outlines hidden creatures within 12 blocks; Unseen (2/day, 8% mana): invisible 15-45 s (30 at the gate) merged with Presence Concealment and **Shadow Motion**, ends on attack; Fade from View: invisible + Presence Concealment 5 s after a kill; sneak attack +40% (min 3, max 8) scaled by training | "attunement by a Rogue" -> the aura gate (a training scale, no lock-out); +5 Stealth / Perception advantage -> visibility + Magic Sense; twice per long rest -> twice per day; reroll a 1 -> min 3 |
| Armor of the Lionheart | **Abnormal Condition Resistance (Passive.skill)**; Fear removed from wearer and allies within 10 blocks every second and cannot be applied to the wearer; Bolstering Roar (1/day, 1% EP): Strength I, Resistance I, Speed I for 30 s on wearer and allies, **player allies lent Abnormal Condition Resistance (the advantage on the first save), the wearer lent Hero Haki**; Lion's Resolve (2/day, 5% mana): the hurt-most ally takes 25% less damage for 5 s, the wearer 25% more; +2 hearts, 20% knockback resistance, noisy (visibility x1.25) | advantage on saves/attacks/checks -> fear immunity + Abnormal Condition Resistance + three buffs + Hero Haki; +-5 AC -> +-25% damage |
| Armor of the Beheaded | Curse: -1 heart; shots do 15% less; Detach Head (4% mana, 20 s, toggles): 45 s of magical senses (Night Vision, **Magic Sense lent**, Water Breathing), immune to blindness/nausea/darkness, shots x0.6, but no eating or drinking; **Dispel Magic act (6% mana, 30 s)**: washes harmful effects off the wearer, strips the beneficial effects of up to 8 foes within 6 blocks and jams their skills (`Nullification.jamAll`, 6 s, 2 on bosses), breaks magic barriers/spells (`shatterBarriers`), lends Chant Annulment 10 s, **and darkens the magical senses of the headless (yours and any other Armor of the Beheaded in the blast) for 1 minute** | CON save -> the curse falls on all, every attuned wearer may detach the head; Arcane Eye / Magic Mouth senses -> Night Vision + Magic Sense; **"Dispel Magic can disable them for 1 minute" -> the armour's own Dispel Magic art, with the one-minute cost on the senses (immunities stay)**; +2 AC -> armour value |
| False Dragon Armor | 60% less fire damage + **Flame Attack Resistance and Heat Resistance (Passive.skill)**; Fiery Wings (1/day, 12% mana): flight for 2 min with a take-off boost, fire resistance, flame wings (particles), Slow Falling 10 s at the end; flight only taken back if the wings granted it | fire resistance -> 60% + two Tensura resistances; 1 hour -> 2 minutes; once per day -> 1 use per Minecraft day |
| Soul Armor | Each kill of a living (non-undead) creature: +1d4 absorption (cap 8) and **3% of the wearer's maximum magicules and aura return** (Tensura existence storage); first kill wakes the armour (+3 and 10% of the pools); fades on armour off, sleep, or a day; noisy (x1.25) | 1d4 temp HP stack -> cap 8; life energy -> magicules + aura; special attunement (slay a creature) -> the awakening kill; long rest -> sleep or a day |
| Arcane Conduction Armor | **Spellcaster gate = maximum magicules 1,000**: below it only Base Mode works, at/above it every mode works and the suit adds "Intelligence" damage reduction (4% at 1,000, 8% at 10,000, 12% max at 100,000); Reconfigure (2 s) cycles Base (35% less force/magic, **Magic Resistance lent**) / Fortified (12% less, Slowness II, **Physical Attack Resistance lent**) / Stealth (visibility x0.65, **sealed: grimoire cast flag + Tensura silence = magic jamming**, **Presence Concealment when sneaking = the Hide action**) / Burst (+15% force/magic dealt, +10% taken, **Magic Aura lent**) | AC 11 + Int -> armour 5 + Intelligence bonus; +2 AC -> -12% damage; speed halved -> Slowness II; Stealth + Int, Hide as bonus action -> visibility, Presence Concealment; **cannot cast in Stealth Mode -> grimoire seal + silence**; +2 spell attack -> +15%; -2 AC -> +10% damage taken; **spellcaster attunement -> the magicule gate** |
| Delphyne's Cursed Armor | **Strength gate = maximum EP 3,000**: below it the plate drags (Slowness I), at it the tail is as written, above it the tail shoves up to half as hard again; **Dragon Skin, Abnormal Condition Resistance, Thought Communication (Passive.skill)**; 70% less dragon breath, 25% less from any dragon/drake; Fear cannot be applied and is purged; Hero of the Village (villagers understand you); 25% knockback resistance; noisy (x1.25); Tail Sweep (5% mana, 8 s): 4 damage + Slowness II + shove within 4 blocks | AC 18 / +3 -> armour 11/4; **STR 15 -> the EP gate**; resist breath / frightful presence -> ward + Unbowed; speak Draconic / comprehend languages -> Thought Communication + Hero of the Village; Stealth disadvantage -> visibility x1.25; Tail Sweep is an addition drawn from the art |

## The Tensura-stat gates (never a lock-out)
`training(value, gate)` = 1 + 0.5 x log10(value / gate), clamped to 0.5 .. 1.5 (1.0 at the gate, 0.5 at a tenth, 1.5 at ten times; 0.5 with nothing).
- Rogue (Golden Shadow): maximum aura against 500. Scales the visibility factor, the sneak-attack bonus and Unseen's length.
- Spellcaster (Arcane Conduction): maximum magicules against 1,000. A hard threshold for modes beyond Base, plus a scaling Intelligence bonus.
- Strength 15 (Delphyne): maximum EP against 3,000. Below it Slowness I; the Tail Sweep shove scales with training.

## Fairness
Every active has a cooldown (and a cost or a per-day limit); nothing kills instantly; per-use damage is 5 or less (cap 30% of a 20 HP player via Fx);
creative/spectator players are never targeted (Fx); the starscape/parry counter-hits are re-entrancy guarded; the player pass runs once per 10 ticks per
player (staggered by entity id), with at most 12 targets per scan, skill leases are renewed once a second; Dispel Magic's jam is the repo's own temporary,
cooldown-only jam (never touches learned skills or max EP), and it is capped at 8 foes.

## Assets
`tools/relics/wave7_icons.py` draws the ten 16x16 icons (+ `models/item/<id>.json`, parent item/generated). Rim pixels in the grade colour (1 rare, 2 unique, 3 legend),
glow pixels on gems and veins.
