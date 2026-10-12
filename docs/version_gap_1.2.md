# Version gap check: v1.0.0 -> 1.1.0 -> 1.2.0

Three lines of work existed in parallel; 1.2.0 is the merge of all of them (it compiles in CI and passes its tests; nothing was run in game).

| Line | What it brought | In 1.2.0 |
|---|---|---|
| v1.0.0 (owner's other AI) | Riven Remake / Marquis raid boss (4 phases, New Order, God Avatars, loot), Zagred bored-portal teleport, wiki VFX scale fixes, grimoire design generator | yes |
| 0.99.0 (Start branch) | Elsdocia sword + Legacy engraving, Key Magic restoration (Magic Key item, Janus Abigail extra doors, great-key aura), Bronze build fixes | yes |
| 0.100 - 0.102 / 1.1.0 | 7 reference weapons (Quickling's Ivy, Magebane Longsword, Prism Blade, Thundercleave, Bloodbound Katana, Permafrost Sabre, Cyclone Rapier), Story Page and Gale Feather drops, the Codex UI (page turn, wheel scroll, random poses, status page), the four Black Clover Spirit Lord bodies, Elsdocia remake, boss spawn guard | yes (1.1.0 did not compile; fixed in 1.2.0) |
| 0.54 - 0.58 (this session's line) | casting animations, Yami's sword draw, store / draw weapon pages, all 29 new magics with ~12 themed pages each, VFX for every new magic (3x size), Ice and Dark VFX, Sealing / Secre spells and VFX, sounds for the new effects, weapon item tags, Beast / Key / Corundum / Bronze summons and render layers | yes |

## What is still missing or incomplete (relative to the requests so far)

1. **Summons / render layers (props, auras, geo models) still stubs:** Glass, Barrier, Mercury / Silver, Iron, Copper (and every other attribute without a summon). Beast, Key, Corundum and Bronze are real.
2. **Sylph boss fight:** `WindSpiritLordEntity` is still the 0.53 stub (attributes and summon only); the Black Clover Spirit Lord bodies (`ElementalSpiritLordEntity`) are a separate, finished feature.
3. **Custom weapon sheets:** 7 of about 20 items are built; 8 more weapons are in progress (Magebane Shield, Weapon of Resurgence, Bolt Thrower, Crossbow of Swift Death, Club of Rot, Doom Hammer, Brooch Pin, Hand of Providence). Not built: the relics on the sheets (Arcanist's Circlet, Godking Crown, Skitch's Smuggling Coat, Genie Gin, Book of Mispells, Tome of "Wait, That's Illegal", Pact of the Deck, Infernal Contract).
4. **Nothing has been run in game.** Unverified: the boss spawn fix, the Codex screen, the Spirit Lord bodies, the Dimension Slash shader, the Four Kingdoms tooltip mixin, every new spell's balance and every Tensura effect id used by the new books (an unknown id silently does nothing).
5. **Version numbers:** the git tag `v1.1.0` points at an unrelated older commit (760f10a); CI releases are tagged `v<version>-b<run>` and are not affected.
