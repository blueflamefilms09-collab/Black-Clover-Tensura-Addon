# Relic sockets and visual evolution (1.29.0)

Players bind a Spirit or an Underworld Devil into a relic through a **Core item**; the bound Core gives the wearer a passive gift and an active ability,
and the relic *looks* different for it (emissive pass in the element's colour, ambient particles, and from Legend tier the `_genesis` look). This is parts 3
and 4 of the end-game scaling prompt, adapted to this repo. Every component below has a **Why** paragraph, as the owner's prompt requires.

Nothing here was run in game: the pure rules are unit-tested (`src/test/.../relics/socket`, `client/relics/item/SocketFxRulesTest`, `VariantRulesSocketTest`)
and the Minecraft-facing classes were type-checked against the API stubs only.

## Adaptations to this repo (and why)

* **Data components, not NBT.** Minecraft 1.21.1 / NeoForge 21.1 stores item state in data components. The socket is the component `nusmp:socket`.
  *Why:* components are saved, synced and diffed by the game; the prompt's "NBT on the ItemStack" does not exist any more in this version.
* **No GeckoLib for weapons.** Weapons here are baked meshes (`assets/nusmp/weapon_meshes/<id>.txt`) drawn by `client/WeaponRenderer`, with variants chosen by
  `client/relics/item/ItemStateVariants` (+ `VariantRules`), and worn relics by `RelicWornLayer`. *Why:* the whole weapon pipeline (display presets, the
  oversize guard, Genesis/five-leaf variants, bows' draw stages) is built on that mesh renderer; a GeoRenderer would fork it and lose all of that. The
  prompt's "swap texture / emissive layer / ambient particles" is exactly what the variant resolver already does, so sockets plug into it.
* **Cores are plain items, the flow has no GUI.** *Why:* a container screen needs a menu, a screen, a packet and a slot layout for a one-item action; the
  sneak-use flow (relic in the main hand, Core in the off hand) is server-only, cannot desync and works with every relic already.

## What was built

| Piece | Where |
|---|---|
| Socket table (6 Spirits, 9 Devils) | `relics/socket/SocketCatalog`, `SocketDef`, `SocketElement`, `SocketFxKind`, `Boon`, `ActSpec` |
| Rules (slots, insert checks, cast trigger, life steal, power) | `relics/socket/SocketRules`, `SocketData` |
| The component | `relics/socket/SocketComponents` (`nusmp:socket`) |
| Effective tier | `relics/socket/SocketGrades` |
| Core items | `relics/socket/SocketItems`, `SocketCoreItem`; icons `tools/relics/gen_core_icons.py` |
| Socketing, drawing out | `relics/socket/SocketService`, `SocketEngine` (sneak-use) |
| Passives, resistances, life steal | `relics/socket/SocketEngine` |
| Active abilities | `relics/socket/SocketActs` |
| Commands | `relics/socket/SocketCommands` |
| Loot | `relics/socket/SocketLoot`, `SocketLootRules` (+ one line in `RelicLootModifier`) |
| Look API for the worn layer | `relics/socket/RelicLook`, `RelicLookRules` |
| Hand-held look | `client/relics/item/ItemStateVariants`, `VariantRules` (edited), `SocketFx`, `SocketFxRules` |

### The sockets: who has them
Weapons (hand relics) and armour relics (head, chest, feet) of effective tier **Rare or above** have **one** socket; **Legend or above have two**. Curios
and consumables have none, unless a wave calls `SocketCatalog.allowSocket("<relic id>")`, which gives that relic one (two from Legend). The effective tier is
`SocketGrades.tier(stack)`: the def's grade, raised by the tier-ascension system's `Tiers.effectiveGrade` when that class exists (found by reflection, or set
with `SocketGrades.useSource`). **Why:** the owner's rule is "RARE+ one, LEGEND+ two"; a def flag (`RelicDef.socket(true)`) would touch a class several agents
edit, so the opt-in is a set in the socket package and the default is the rule itself, with the same effect.

### Using it
* **Socket:** hold the relic in the main hand, a Spirit/Devil Core in the off hand, **sneak and use**. The Core is used up; the answer is a Great Sage line
  ("Answer. The binding of ... is complete (1 of 2 sockets).").
* **Draw out:** the same with an **Empty Element Core** (`tensura:element_core_empty`) in the off hand: the last bound Core comes back as an item and the empty
  core is used up. *Why this item:* it is Tensura's own empty vessel, verified in the id dump, so the flow has a lore reason and needs no new item.
* **Cast:** sneak-use the relic (no Core in the off hand) fires the first bound socket, if the relic has fewer than two acts of its own (a relic's second act
  already lives on sneak-use). **Looking steeply up (pitch -60 or higher) while sneak-using fires the second socket** (or the only one). Any worn relic
  (armour included) can cast with `/multiverse relic socket cast [n]`, and a worn-relic key hook may call `SocketEngine.castWorn(player, n)`.
* **Ops:** `/multiverse relic socket list | cast [n] | add <spirit:fire|devil:megicula|key> | remove [n] | core <player> <key>` and `/multiverse relic unsocket [n]`.
  `add` skips the Core and the bond check; `remove`/`unsocket` return the Core.

### Bonds: what a relic-bound Spirit is
`relics/djinn/Bonds` is the player's own exclusive bond (a Black Clover Spirit, a Devil contract, or a Djinn; Astralis lifts it at a price). **A Core in a relic
never counts as the player's bond:** `Bonds` reads the real state (spirit slots, pacts, grimoire devil, Djinn) and the sockets never write to it. But the
exclusivity still protects the design in one direction: a Spirit Core is **refused** to a player who holds a Devil or a Djinn bond, a Devil Core to one who
holds a Spirit or a Djinn (`SocketRules.insert` asks `BondRules.canBind`, with a red message), and an already bound socket goes **silent** (no passive, no
cast, with a message) while the player holds a conflicting bond, and works again when the conflict is gone. Nothing is lost. Astralis (multi-bond) lifts the
refusal, like for every bond. Operators' `add` is exempt.

### The gifts
Each socket has *Boons* (passive, once a second while the relic is equipped and working: held or worn, attuned where needed, creative/spectator excluded) and
one *Act* (active; costs magicules like every relic act, with a cooldown). Spirits: Fire (Ifrit): fire resistance + Tensura `flame_attack_resistance`,
**Hellflare** (a cone of fire); Water (Undine): water breathing + `water_attack_resistance`, Tidal Surge; Wind (Sylph): slow falling + 6% speed, Gale Step;
Earth (Gnome): 50% knockback resistance, Stone Bulwark; Light: night vision + `light_attack_resistance`, Radiant Mend (heals and cleanses allies); Dark:
night vision + `darkness_attack_resistance`, Umbral Veil. Devils (the nine of `blackclover.Devil`): Liebe (magic resistance, Null Field), Zagred (chant
speed, Word of Halt), **Megicula (curse resistance, Curse-Warding Magic: ward + curse cleanse on teammates)**, Lucifero (gravity resistance, Crushing Weight),
Beelzebub (spatial resistance, Rend Space), Astaroth (attack speed, Time Slip), Lilith (cold resistance, Freezing Aura), Nahamah (fire, Scorching Aura),
Walgner (**8% life steal**, Blood Tithe). Only verified Tensura ids are used (skill ids checked against the id dump; the test pins them).

### Loot
Spirit Cores: 1.2% of a dungeon/stronghold chest, 3% of the deep chests (vault, bastion, ancient city, trial); Devil Cores: 0.8% of the deep chests; the mod's
registered bosses (`BossRegistry`) drop a Spirit Core 10% and a Devil Core 6% of the time. Light and dark are a third as likely as the other four Spirits;
Devils are even. `/multiverse relic socket core <player> <key>` gives any.

## Visual evolution

* **Hand-held weapons.** `ItemStateVariants.resolve` now (a) treats an effective tier >= Legend as the GENESIS variant (the `<id>_genesis` mesh/skin if the
  weapon ships one, else the stronger glow and extra emissive pass that already existed), and (b) puts the colour of the first bound socket in
  `Result.socketRgb()` (0 = none) and turns the stronger glow on. The renderer draws a second emissive pass in that colour with `VariantRules.socketArgb`.
* **Ambient particles.** `SocketFx` (client tick): fire embers, water droplets, wind wisps, earth motes, light motes, dark smoke, round the held weapon or
  the worn piece. Budget: at most 8 holders and 10 particles a tick for the whole screen, only holders within the view distance (and 24 blocks), one burst
  every 2/4/8 ticks (high/medium/low), none at quality 0. The quality is `relicfx.quality`, read by reflection (`SocketFx.SocketQuality`); if the option does
  not exist, or fails, medium is used, so nothing depends on it.
* **Worn relics.** `RelicLook.evolved(ItemStack)` returns `{tier, socketElement, rgb}` (the socket's colour, else the grade's) for the worn layer.
* **Tooltip.** Bound Cores are listed on the relic, and the number of free sockets.

## Why (per component)

* **`SocketCatalog` / `SocketDef` / `Boon` / `ActSpec` (pure data).** The balance of fifteen sockets lives in one table with no Minecraft types, so a test can
  pin it (ids verified, act numbers within bounds, every Devil of the mod present) and a designer changes a number in one place. Passives are deliberately
  modest and permanent; the power is in the ability, which costs magicules and has a cooldown: end-game scaling without making the relic a free stat stick.
* **`SocketRules` (pure).** Every decision that could be argued (how many slots, whether a Core goes in, which socket a sneak-use fires, how much life steal
  returns) is a function with a table-driven test. The hard caps (4 health per hit, 15% of max health per second for life steal; a 1.6x ceiling on ability
  power; resistances multiply but never keep less than 10%) exist because the relics' own rule is that nothing may one-shot or trivialise a boss.
* **`nusmp:socket` data component (a compound of `{id, t}` entries).** Components are the 1.21 way to carry per-stack state, saved and synced, so the tooltip
  and the client particles read it without a packet. A compound (like `relic_uses`) lets the shape grow without a new component id; the bound-at game time is
  kept for lore and future rules. Reads that run every few ticks (`firstId`, `hasAny`) never build a list, so the render path allocates nothing.
* **`SocketGrades` (reflective tier).** The socket count must follow the *effective* tier but must not break if the ascension system is absent or renamed; a
  one-time reflective lookup with the def's grade as the fallback gives both, and a raised tier can never lower a relic.
* **`SocketService` / `SocketEngine` sneak-use.** One server-side flow means one place for the checks and no client-trust: the client only sends the use
  packet it already sends. The event is cancelled on the server only, because cancelling on the client would stop the packet being sent at all.
* **`SocketEngine` passives (own once-a-second pass, offset from the relic engine's).** The relic engine's pass is fixed to a def's static passives; a bound
  socket is per stack, so a second pass reads the stacks the engine already considers working (`Relics.loadout`, which is cached) and uses the engine's own
  idioms (a 60-tick skill lease via `Fx.lendSkill`, ambient effects, transient attribute modifiers removed when they stop applying). The ids are per socket
  so two relics carrying the same Core do not double the gift. Creative players are skipped (they take no damage) and so are spectators.
* **Life steal bypasses `Fx.heal`.** `Fx.heal` is damped after every heal (x0.4 for twenty seconds); a per-hit life steal would keep that window shut and
  starve the real heals of the abilities. It has its own hard per-hit and per-second caps instead.
* **`SocketActs` (one `Act` per socket, run by `RelicEngine.activate`).** The engine already checks attunement, cooldown, mana, existence-point floors and
  creative/spectator, and records the cooldown per relic and act key. Reusing it means a socket ability behaves exactly like a relic's own and no cost logic
  is duplicated. All damage goes through `Fx.damage` (the balance law), healing through `Fx.heal`, targets through `Fx.foesAround` (never allies, summons,
  creative or spectator players, at most 24 foes, radius capped at 32).
* **`SocketLoot`.** Cores come from the places the owner named: chests (one line in the existing relic loot modifier, so Lootr's per-player rolls work the
  same way) and bosses (a drop listener keyed on `BossRegistry`). Low weights, because a Core is a permanent power-up of a relic that is already unique.
* **`RelicLook` / `RelicLookRules`.** The worn layer belongs to another system; a two-function, side-agnostic API (tier, element, colour) lets it evolve its
  look without knowing about components or the catalog.
* **`ItemStateVariants` / `VariantRules` edits.** The resolver is cached per (mesh, stage, variant, socket colour), so the cost of the new inputs is one
  string read and one map lookup per draw on a cache hit; the tier threshold and the pass's alpha are pure functions with tests.
* **`SocketFx` / `SocketFxRules`.** Particles are a client tick, not part of the draw, so a crowd of holders can never make a frame slower; the budget, the
  distance and the quality are pure functions so the "no lag" promise is a test, not a hope. Particle options are built once per socket.

## Integration (for the lead)

```java
// NUSMP.java, constructor, before NUItems.ITEMS.register(modEventBus):
com.newuniverse.nusmp.relics.socket.SocketItems.init();
com.newuniverse.nusmp.relics.socket.SocketComponents.COMPONENTS.register(modEventBus);
// game bus (e.g. next to RelicEngine.register):
com.newuniverse.nusmp.relics.socket.SocketEngine.register(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
com.newuniverse.nusmp.relics.socket.SocketLoot.register(net.neoforged.neoforge.common.NeoForge.EVENT_BUS);
// RelicCommands.addTo, inside the "relic" node:
//   .then(com.newuniverse.nusmp.relics.socket.SocketCommands.node())
//   .then(com.newuniverse.nusmp.relics.socket.SocketCommands.unsocketNode())
// NUCreativeTab: for (var c : SocketItems.all()) output.accept(c.get());
// WeaponRenderer.renderByItem, after the glow block (draws the socket pass; without it a socket still gets the stronger glow):
//   if (state.socketRgb() != 0 && mesh.glowTex != null && !mesh.layers[GLOW].isEmpty())
//       emit(p, buffers.getBuffer(RenderType.entityTranslucentEmissive(mesh.glowTex)), mesh.layers[GLOW],
//            VariantRules.socketArgb(state.socketRgb(), time), 0xF000F0, OverlayTexture.NO_OVERLAY);
```
`ItemFxClient.init` already registers `SocketFx`. Lang: `scratchpad/frag_wpn/w6.lang.json`. Optional: `SocketGrades.useSource(Tiers::...)`,
`SocketCatalog.allowSocket(id)` for Curios relics, and a worn-relic key hook calling `SocketEngine.castWorn`.
