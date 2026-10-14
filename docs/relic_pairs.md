# Relic pairs: two weapons that come together (1.29.0)

A pair is two hand items (a right and a left blade) that are one relic of the world: they are obtained together, the world holds one pair, the
pair bonus works only while the main hand holds one half and the off hand the other, and the off-hand half adds a real second blow. Twin Hook
Swords, Whirling Blades and the Twin Swords of Deflection are pairs. Each "Why" paragraph says why the part exists (architecture or balance).

## Declaring a pair for a new weapon

```java
out.accept(RelicDef.of("twin_x", "Twin X Sword (Right)", Grade.RARE, RelicSlot.MAIN_HAND)
        .pair("Twin X Sword (Left)")                    // the catalog adds the item twin_x_left right after this def
        .model3d().look(Look.SWORD).palette(...)
        .passive(...).act(...)                          // the left half gets a copy of everything below, so either half can be the main-hand one
        .melee(4.0, 2.0)                                // damage of ONE blade; the pair deals about 1.6 times that (the off-hand blow is 60%)
        .engrave("swift"));                             // both halves carry the engravings
```

`RelicDef.pair(leftCardName)` marks the def as the RIGHT half (its id is the id the card always had). `RelicCatalog.init` wraps the consumer the waves
write into, so a def that declares a pair also adds `def.pairLeft()` (id `<id>_left`, same grade, look, palette, melee numbers, engravings, passives and
acts) to the list; nothing else in a wave changes. Then:

1. **Meshes**: one blade per hand. Add a builder to `tools/relics/pair_models.py` (`PAIRS`): build ONE blade on its own axis, and the left half is the
   same build mirrored across x before the skin is laid out (`mirror_mesh`). `python tools/relics/pair_models.py <id>` writes `<id>.txt`,
   `<id>_left.txt`, both skins and glow skins, the left item models and the left inventory sprite (a copy of the pair's sprite; the old sprite stays the icon).
2. **Lang** (`item.nusmp.<id>`, `.desc`, `.card` and the same for `<id>_left`); the tooltip line "Pair: needs <other half>" is automatic.
3. **Behaviour**: code that asks "which relic is in the main hand" must use `def.pairBaseId()` (both halves answer as the pair). Ask
   `PairHooks.active(player)` for "both halves in hand and working", and scale pair-only numbers with `PairRules.factor(active)`.
   A pair-specific second-blow effect (a flame, a chill) is registered once in the wave's `hooks()`:
   `PairEffects.onOffHand("twin_x", (player, victim, offHalf) -> ...)`.
4. **Hooks that deal their own damage** call `PairHooks.striking()` (or `Wave0Util.busy()`, which includes it) and stay out while true, so the off-hand blow never triggers them.

## How the pair behaves

* **Obtaining**: `/multiverse relic give <player> <id>` (either half's id) issues every half that does not live yet: both for a new pair, or only the half a
  world lacks. A chest or a Mimic that rolls a pair drops both halves (`RelicLoot.rollUnissuedAll`); the pair is rolled only while neither half lives. The
  creative tab shows both halves side by side (creative copies are adopted by the Order like any first copy).
* **Uniqueness (the Order)**: each half is its own one-of-a-kind relic in the ledger (`<id>` and `<id>_left`), so a world can never hold two rights or two lefts, which
  is exactly one pair. The ledger, the scanner, the Order's visits, losses and releases work on each half unchanged. If one half is lost (destroyed, unseen for
  14 days) the other half is still valid and `give` completes the pair with only the missing half.
* **Pair bonus**: active only while the main hand holds one half and the off hand the other (`PairRules.pairActive`) and both work (attuned where needed).
  One half alone keeps every ability (Sweep the Leg, Spin Attack, Deflect) at 85% of its pair-only numbers, loses the off-hand blow and the pair-only passives
  (the hook climb). Cooldowns are shared by both halves (`RelicEngine.key` uses the base id).
* **Off-hand blow**: after the main hand's melee blow landed, the off-hand half strikes a second blow of 60% of the first, through `Fx.damage` (EP scaling, the
  PvP hit cap, no creative or spectator target), with its own 12 tick cooldown; creative and spectator wielders get no second blow. A guard flag makes it
  invisible to every other hook, so it cannot trigger itself.
* **Attunement**: an attuned pair uses ONE of the three places: attuning one half sets the other half's owner too, and the left half does not count again.
* **Old worlds**: the single item of 1.28 (`twin_hook_swords`, `whirling_blades`, `twin_swords_of_deflection`) stays registered and is now the RIGHT half. On
  first login a player who holds a right half and whose world never issued the left half receives the left half once (and it is attuned with the right one if that
  is theirs). An admin can do the same with `/multiverse relic give`.

## Why (per component)

* **`RelicDef.pair` + `RelicCatalog` wrapper**: a pair is declared where the weapon is declared, in one call, and every other system sees two ordinary relic defs. The
  alternative (one id with a "side" component) would have meant touching the ledger, the loot, the model registry and the tooltip for every call site; two ids reuse all of
  it and keep the old id as the right half, so old saves load without a data migration.
* **One ledger entry per half**: the ledger's rule is "one valid copy per id". Treating a pair as two ids gives "exactly one pair" without changing its decision table
  (`LedgerRules`), and a half lost alone is recoverable alone. The cost, two rows in the Order's list for one pair, is cosmetic.
* **`PairRules` (pure)**: every number (60% blow, 12 tick cooldown, 85% lone factor, cap of 40 on the second blow) and every id decision is a plain function with a
  unit test, so balance can be changed in one place and checked without the game.
* **60% and the cooldown**: a second full blow would double the weapon; 60% makes the pair about 1.6 blades, the price of two hands (a shield or a second tool). Each blade's
  own damage was lowered so a pair deals about what the old single item did (7 to about 8 for the whirling pair, 5 to about 6 for the hooks, 5 to about 7 for the
  deflecting pair, which also kept its crit and speed) while a lone half is a believable fallback. The 12 tick cooldown stops a fast sword from striking twice per swing.
* **`PairHooks.striking()` and `Wave0Util.busy()`**: the extra blow goes through the same damage events as any hit; without a guard each of the waves' hooks (the whirling
  blades' flame, the twin swords' crit) would fire again for it. One flag, consulted by the two entry points the pair weapons pass through, removes recursion.
* **Login grant**: players of existing worlds hold only the old single item; the Order's ledger knows whether a left half was ever issued, so "right half held, left half never
  issued" is a safe test that grants exactly once and never creates a second left.
* **Mirrored left mesh on the same sprite**: vanilla mirrors the pose of an off-hand item; a mesh mirrored across x and drawn with that pose is the true mirror image of the
  right blade, so the left half needs no change in the renderer. The inventory sprite is shared because the old sprite shows the pair.

## Files

`relics/pair/PairRules.java` (pure), `PairHooks.java` (off-hand blow, first-login grant), `PairGrant.java` (issuing), `PairEffects.java` (per pair second-blow effect);
`relics/RelicDef.java` (pair builder), `relics/RelicCatalog.java` (adds the left half, registers the hooks), `relics/Relics.java` and `relics/Attune.java` (attunement of a pair),
`relics/RelicEngine.java` (shared cooldowns), `relics/RelicLoot.java`, `RelicLootModifier.java`, `RelicCommands.java` (both halves together),
`tools/relics/pair_models.py`, `src/test/.../relics/pair/PairRulesTest.java`.
