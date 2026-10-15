# The Relic Forge (1.41.0)

A craftable table (`nusmp:relic_forge`) with a window that does, from menu buttons, what the relics' sneak-use flows do with a second item in the
off hand: raise a relic's grade, bind a Spirit or Devil Core into it, draw a Core back out, and (new) set a Tensura engraving on a mod weapon.
The sneak-use flows are untouched and still work; this is a second doorway, not a replacement.

## How to use it

Craft it: gold ingot, diamond, gold ingot / obsidian, crafting table, obsidian / three iron ingots. Right-click it.

| Slot | Takes | Notes |
|---|---|---|
| 1 Relic | any relic that is not a consumable, or a mod weapon | edited in place |
| 2, 3 Catalysts | any catalyst item of the tier ritual (Tensura ingots, Devil's Core, Genesis Seed) | two slots so a count of 3 or 4 can be split across stacks |
| 4 Core / Book | a Spirit or Devil Core, the Tensura Empty Element Core, or an enchanted book holding a `#tensura:engraving` enchantment | |
| 5 Next (read only) | nothing | shows the catalyst the next grade needs, or the Core the next DRAW OUT returns |

Buttons (tab "Upgrade / Socket"): **Upgrade** runs the tier ritual with the contents of the catalyst slots; **Socket** puts the Core of slot 4 into the
first free socket; **Draw out** (Empty Element Core in slot 4) takes the Core of the chosen socket back; **Next socket** chooses which socket
Draw out takes. Tab "Engrave": **Engrave** applies the book in slot 4 to the weapon in slot 1. A button the server would refuse is dimmed but
still clickable; the click is answered with a Great Sage line in the panel under the window (and the action bar), never with silence. Everything
left on the table goes back to the player when the window closes, as a crafting table does.

## Why it is built this way

* **Better Combat takes the off hand and the use input** for dual wielding, so "relic in one hand, catalyst or Core in the other, sneak and use"
  cannot be triggered. A menu needs neither.
* **No rules are copied.** `TierRitual.attemptFromForge` and the socket calls `SocketService.socket` / `unsocket` are the code the sneak-use flows
  already run, so attunement, the top grade, the cooldown, the magicule/aura price (`CostMath.spendPooled`), the roll, "a failure never destroys the
  relic", the bond rules and the socket counts are the existing ones. `TierRitual.attempt(...)` (sneak-use) now calls the same private `run`, so there is
  one body. Its behaviour is unchanged for a single catalyst stack.
* **The very same stack is edited.** The relic in slot 1 is the ItemStack the services write the tier and the sockets onto; the serial and the
  attunement are components that stay. No block entity holds anything: the menu's container is released to the player on close, so there is no
  stored copy for the Order's ledger to count.
* **The preview slot never shows a relic.** The Order's scanner reads the stacks of every opened menu (except crafting-style result slots); a preview copy of a
  serialised relic would look like a duplicate and bring the God of Order. Slot 5 therefore shows only a catalyst or a Core.
* **Server decides, client mirrors.** The menu packs tier, max tier, sockets used/total, next grade, cost %, chance %, the enabled-button mask, the last
  message, the chosen socket, the forge engraving count, the cooldown seconds and the subject into 13 container-data ints (`ForgeView`); the screen
  prints what `ForgeView` formats and sends only button ids. No packet type was added.
* **No recipe type, no block entity**: a crafting table's design, because that is the smallest thing that cannot lose an item.

### Engraving rules

Engravings are plain enchantments on the stack (`item.WeaponEngravings`, `relics.RelicEngravings`; each marks the stack "NUEngraved" once). The forge
adds enchantments with the ordinary API and leaves their marker alone, so their once-only pass neither undoes nor repeats the forge's work. The mod
has no cap of its own, so the table brings one: **three engravings added by the forge per weapon**, counted in a small custom-data int
("NUForgeEngr"); a weapon that came with five lore engravings is not locked out. A weapon that already has the engraving is refused (it is never
replaced, not even by a higher level); an incompatible enchantment (`Enchantment.areCompatible`) is refused; the level is the book's, capped at the
engraving's own maximum. All checks run before anything is written, then the book is used up (not in creative). A "mod weapon" is a
`MagicWeaponItem`, a `DjinnWeaponItem`, or a relic whose slot is the hand.

## Files and their why

Java (every class opens with a `Why:` paragraph):

| File | Why |
|---|---|
| `relics/forge/ForgeRegistry.java` | Own block, block item (on `NUItems.ITEMS`) and menu type; one `init` call to wire. |
| `relics/forge/RelicForgeBlock.java` | Crafting-table-like block, no block entity. |
| `relics/forge/RelicForgeMenu.java` | All server logic: slots, buttons, synced numbers; calls the existing services. |
| `relics/forge/ForgeRules.java` | Pure: slot acceptance, which button is available, refusal lines, engraving cap. Tested. |
| `relics/forge/ForgeView.java` | Pure: layout of the synced ints and the lines the screen prints. Tested. |
| `relics/forge/ForgeEngraving.java` | Engraving storage access (book reading, compatibility, applying, count). |
| `client/relics/forge/ForgeScreen.java` | The window (anime theme or classic fallback). |
| `client/relics/forge/ForgeClient.java` | `RegisterMenuScreensEvent`. |
| `src/test/.../relics/forge/ForgeRulesTest.java`, `ForgeViewTest.java` | Pure JUnit. |

Edits to existing files (additive): `relics/tier/TierRitual.java` (`Reason`, `Result`, `attemptFromForge`, `cooldownLeft`, `acceptsCatalyst`,
`acceptsRelic`; `attempt` now delegates to the shared `run`), `relics/socket/SocketEngine.java` (`isEmptyElementCore`).

Assets and data:

* `assets/nusmp/blockstates/relic_forge.json`, `models/block/relic_forge.json` (a body with an anvil-like top plate), `models/item/relic_forge.json`
  (parent: the block model).
* `assets/nusmp/textures/block/relic_forge_{side,plate,top,bottom}.png`, made by `tools/gen_forge_textures.py` (dark metal, gold trim, violet rune on
  top). The top carries the glow; the block also gives light level 7.
* `data/nusmp/recipe/relic_forge.json`, `data/nusmp/loot_table/blocks/relic_forge.json` (drops itself), `data/minecraft/tags/block/mineable/pickaxe.json`
  (one value appended).
* `assets/nusmp/codex/rules/relic_forge.json`: the Rules page that explains the table (the Codex's own stock list is another builder's).
* Lang: `block.nusmp.relic_forge`, `container.nusmp.relic_forge`, `nusmp.forge.*` (the fragment is merged into `en_us.json` by the lead).

## Wiring (the lead)

* Mod constructor, before `NUItems.ITEMS.register(modEventBus)`: `com.newuniverse.nusmp.relics.forge.ForgeRegistry.init(modEventBus);`
* Client branch next to `GrimoireSlotClient.init`: `com.newuniverse.nusmp.client.relics.forge.ForgeClient.init(modEventBus);`
* Creative tab: add `ForgeRegistry.RELIC_FORGE_ITEM.get()`.

## Unverified

Nothing was run in the game. Type-checked against the API stubs: the menu, rules, view, registry, `TierRitual` and `SocketEngine` (the stubs lack
`IMenuTypeExtension`, the client GUI packages, the enchantment package and shape classes, so `ForgeRegistry`'s menu factory, `ForgeEngraving` and
`ForgeScreen` were checked by hand against the repo's own use of the same calls). Pure tests were run against the JUnit shim. To check in game: a
relic upgrade through all grades, a failure at a catalyst-losing grade, socket then draw out with the Empty Element Core, a stack of catalysts split
across the two slots, closing the window with items on the table (they return), a Tensura engraving book on a Magic weapon (cap, repeat, conflict),
and the screen in both themes. `tensura:element_core_empty` and the `#tensura:engraving` tag are Tensura's names as the sneak-use flow and the
engraving lists already use them.
