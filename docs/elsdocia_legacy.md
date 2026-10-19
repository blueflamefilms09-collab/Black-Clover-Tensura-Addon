# Elsdocia: Genesis grade, lineage and the Legacy of the Wizard Kings (1.97.0)

Elsdocia is still `MagicWeaponItem.Kind.ELSDOCIA` (not a relic). It keeps its mesh, its Legacy Release technique, its Legacy Gate (sneak technique), its stored
magic and its engravings; everything below is added.

## Grade
Genesis grade (`relics/Grade.GENESIS`, the highest of the seven): white name, "Genesis Grade" tooltip line, the Genesis glow pass of `WeaponRenderer`
(`ItemStateVariants` treats an Elsdocia stack as Genesis), plus a wider fainter halo pass. It is not a relic: it has no `RelicDef`, so it uses no attunement slot,
is never checked for attunement and its tooltip says nothing about it.

## Damage
Base damage 400 (`ElsdociaMath`/`ElsdociaRules`), +25 for each pass to a new, distinct Grand Magic Knight (or Wizard King) owner: 400, 425, 450 ...
`ElsdociaEvents.onAttributes` (`ItemAttributeModifierEvent`) replaces the main-hand attack-damage modifier of every Elsdocia stack (399 plus the player's 1).
This is an explicit exception to the generic Genesis range of `relics/GradeDamage`, which is untouched; that clamp must skip a stack for which
`ElsdociaRules.isLegacyException(ItemStack)` is true.

## Lineage (data component `nusmp:elsdocia_lineage`, saved, synced, copied)
Ordered owners (uuid, name, Black Clover magic type, Knight rank step at the time, whether the pass gave +25), the learned magics and the active ability slot, stored
as text lines (`ElsdociaLineage.encode/decode`; damaged lines are skipped).
- Detection of "passed": the stack is found in another player's inventory or on their cursor (every 5 ticks, `ElsdociaEvents.scan`), which covers trades, chests,
  drop-and-pick-up and death drops. The first player to hold or use it claims it (no pass, no bonus, even if Grand).
- +25 only for a NEW distinct owner (never an owner before) who is rank step 15 or above at that moment. A non-Grand holder can pass it on with no bonus. An earlier
  owner coming back gives no second bonus.
- No way back: the last owner (the one just before the current one) cannot pick it up from the ground (`ItemEntityPickupEvent.Pre` cancelled) and, if it still reaches
  their inventory, it is dropped at their feet; message "Elsdocia has left you and will not return to your hand". Owners further back may take it again. The current
  owner may keep or re-pick it.
- Admin: `/multiverse relic elsdocia reset | owner <player> | lineage` (permission 2, the Elsdocia in the command player's hand). `reset` also wipes the learned magics.

## Legacy of the Wizard Kings (the existing engraving; its id and name are unchanged)
Every distinct Black Clover magic among the owners gives the sword one ability, kept on the stack. Interpretation:
- The ability is the signature page of that magic's grimoire: the dearest (highest cost percent) family page that needs no weapon, a later page winning a tie
  (`ElsdociaLineage.pickSignature`). It is cast through the page's own `BookPage.Cast` with a default instance of the book, guarded by `SafeCast`. A magic with no ported
  book plays its profile's generic fallback effect (visual only).
- Cost: 60% of the page's cost percent (floor 25%) from the Aura pool, magicules if the Aura is short (`EnergyBridge.aura`); restored if the page did nothing. Damage and size
  come from the page, which scales with the wielder's EP through the balance law. Cooldown: 75% of the page's, per magic, on the wielder.
- Use: with no learned magic nothing changes (right-click Legacy Release, sneak Legacy Gate). Once one is learned, sneak + right-click cycles Legacy Release, Legacy Gate and
  the learned magics in learned order; right-click casts the active one; the action bar names it. Legacy Release then runs on its own wielder cooldown (not the item
  cooldown) so the cycling is never locked.
- The tooltip lists "Legacy of the Wizard Kings: N of 105 magics" and the abilities. A held Elsdocia shows one coloured mote per learned magic (at most 8, tinted by soul).

## Files
`item/ElsdociaMath` (pure), `ElsdociaRules`, `ElsdociaLineage` (pure), `ElsdociaComponents`, `ElsdociaEvents`, `ElsdociaArts`, `ElsdociaCommand`, `ElsdociaSelfTests`
(`relic.elsdocia`); tests `ElsdociaLineageTest`, `ElsdociaGlitterTest`.
