# Named mobs and relics (1.50.0)

Owner request: "make it so named mobs like goblins and stuff can attune to relics".

## How it works
- **Eligible mobs:** a living non-player `Mob` that has a name (name tag, Tensura naming) or an owner (tamed). With `relics_mobs.namedOnly=false` any mob qualifies.
- **Give a relic:** sneak + use a relic on the mob. Weapons go into an empty main hand, helmets / chestplates / boots into the empty armour slot, everything else (rings, charms, cloaks, belts, gloves, necklaces) into a small pack stored in the mob's persistent data (`nusmp_mob_relics`).
- **Attunement:** a relic that needs attunement is bound to the mob's own UUID at that moment (there is no five-second ritual for a mob). A mob keeps at most 3 attuned relics (like a player) and at most `relics_mobs.maxCarried` (4) relics in all.
- **What the mob gets:** every second its relics' attribute passives (transient modifiers `nusmp:mobrelic/<id>/<n>`), effect passives (quiet ambient effects) and resistances (through the normal relic damage listener). The relic's tooltip numbers apply; the grade's existence bonus, skill leases and **acts do not** (acts are written for a ServerPlayer).
- **Take it back:** sneak + empty hand on the mob returns the last relic (only to the player who gave it, or an operator) and cuts the bond.
- **Death:** the pack drops with the mob and the bond is cut, so a player can attune the relic again.

## Code
`relics/MobRelics.java` (events, pack, pass), `relics/MobRelicsConfig.java` (`nusmp-mobrelics-server.toml`), a one-line pack read in `Relics.scan`, a one-line resistances gate in `RelicEngine.onIncomingDamage`, `MobRelics.register(bus)` in `RelicEngine.register`.

## Unverified
Nothing was run in game. Tensura's own naming screen sets a custom name, which is what `eligible` reads; if Tensura named mobs show no custom name in your pack, tell me what marks them and I will read that instead. Whether a Tensura mob keeps a relic in its hand/armour slot (its AI may re-equip) is untested.
