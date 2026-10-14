# Discovery: undiscovered things stay hidden

Items, relics, grimoire pages, spells, creatures and Codex entries that a player has not found yet are not listed with their real name,
icon or text. They show as a dark slot with a question mark and `???`. This document is the single reference for the system
(package `com.newuniverse.nusmp.core.discovery`, client part `client.discovery`).

## One source of truth

Each player has a **discovered set**: a set of short string keys, saved with the player (data attachment `nusmp:discovery`, copied on death
and unioned on every clone), mirrored to the client by the packet `nusmp:discovery_sync` (the whole set at login and respawn, in chunks of at most
800 keys; later only the keys found just now; every packet also carries the viewer's show-all state).

The client holds the mirror in `DiscoveryCache` (plain statics, no client classes, safe from common code). Every screen asks the cache, never the server.
Until the first sync of a session arrives the cache shows everything, so a server without the system behaves exactly as before.

### Keys (`DiscoveryRules`)

| Key | Meaning | Found when |
| --- | --- | --- |
| `item:nusmp:<path>` | an item of the mod (relics and weapons included) | picked up, crafted, smelted, received, given by an operator, or ever seen in the inventory, armour, off-hand or Grimoire Slot |
| `page:<book>:<pageId>` | a grimoire page | unlocked (mastery, command), or a base page the book can use (Summon, Store, Mana Strike, Devil Union with a forbidden cover, signature spells of a fused book) |
| `skill:nusmp:<path>` | a skill of the mod | learned |
| `magic:<name>` | a magic attribute | the player holds a grimoire of it |
| `mob:nusmp:<path>` | a creature of the mod (boss, spirit lord) | the player hurt it, was hurt by it or killed it |
| `spiritlord:<element>` | one of the four elemental Spirit Lords | fought one, or bonded to it |
| `boss:<id>` | reserved for boss entries | not set by the tracker yet; a boss entry is also shown by its drops |

Only the mod's own namespace is tracked. Another mod's items are never hidden.

### Who sees everything

* Creative mode, always.
* Everybody, when the server config `discovery.showAll` is true (file `nusmp-discovery-server.toml`; default **false**).
* Operators (permission 2) and the console in the command lists (they can `/give` anything anyway). Operators in survival do **not** see everything in the Codex
  (a single-player world with cheats on would otherwise never hide anything); use creative mode or the config for that.

## The tracker (`DiscoveryTracker`, server)

* Events: `ItemEntityPickupEvent.Post`, `PlayerEvent.ItemCraftedEvent`, `ItemSmeltedEvent`, `LivingDamageEvent.Post`, `LivingDeathEvent`, login, logout,
  `PlayerEvent.Clone`, respawn.
* The scan: every player is scanned on their own tick (spread over 20 ticks by entity id). Once a second the inventory is compared slot by slot **by item reference**
  against the previous scan (`DiscoveryRules.diffSlots`), so an unchanged inventory costs 42 reference comparisons and nothing else. Every two seconds the learned skills are read; a
  grimoire's pages are diffed by its unlock bitmask, and the always-open base pages are re-read every ten seconds. The same scan notices creative mode being entered or left and
  sends the new show-all state.
* A new key goes to a per-player queue and is sent as one delta on the next tick.
* All per-player state is dropped at logout.

## Grandfathering (existing worlds lose nothing)

A player whose data version is below `DiscoveryRules.VERSION` gets the migration pass at the first login after the update (`DiscoveryTracker.migrate`): everything in their inventory,
Grimoire Slot, ender chest and worn Curios, every learned skill of the mod, every open page of every grimoire, the magic of those grimoires, and a bonded spirit count as found.
Nothing the player owns or has unlocked is ever hidden. A brand-new player simply gets an empty set (plus what they start with) and the version stamp.

## What the UI hides

| Place | Behaviour |
| --- | --- |
| Codex, `Weapons` and `Relics` catalogue (`CodexPages.CatalogPage`) | an undiscovered item is a dark slot with `?`, the name `???` and the line "Undiscovered. Find it in the world to unlock this entry."; no icon, name, tooltip text or stats are drawn |
| Codex, `Bestiary` and `Spirit Lords` (`CodexPages.TextPage`) | an entry with discovery keys (third element of the entry array, joined with `|`) shows `???` until any one key is found |
| Four Kingdoms grimoire book (`FourKingdomsScreen`) | already listed only the pages the server reports as unlocked (`MultiverseSync` `PageList`); unchanged |
| Tensura's ability window | lists learned skills only; a locked family page already reads "Sealed Page"; base pages that are not usable yet read `???` (see integration edit, `GrimoireBook.getModeName`) |
| Mastery notification `A page stirs (...)` | the page is not named until it opens |
| `/multiverse relic list`, `info` and the id suggestions | a player sees only the relics they found, the others as `???`; `info` of an undiscovered relic answers exactly like an unknown id; operators and the console see all |
| `/multiverse charter status` | a boss whose Charter Grimoire the player has not found shows `???` (no claimant, no day) |

Things that are physically in front of the player are not hidden: an item lying on the ground, in a chest, in a trader's hands or in another player's hand shows its name and tooltip,
because the player can see it. It becomes *discovered* the moment it is in their inventory.

Not hidden on purpose: the counts in the status (`Pages 3/9`), the player's own skills, anything the player owns.

## Rules for new UI code

1. Ask `DiscoveryCache` (client) or `DiscoveryTracker.visibleTo(player, key)` / `canSee(commandSource, key)` (server). Never invent a second list.
2. Keys come from `DiscoveryKeys.item(stack)`, `DiscoveryKeys.skill(id)`, `DiscoveryRules.pageKey(book, pageId)`, `magicKey`, `mobKey`, `spiritLordKey`.
3. An undiscovered entry draws `DiscoveryDraw.icon(...)` and the text `DiscoveryRules.QUESTION` (`???`) and the hint `DiscoveryDraw.hint()`; it must not call `getTooltipLines`, `getHoverName`
   or render the real icon.
4. `DiscoveryCache.label(key, realName)` returns the name or `???` in one call; `DiscoveryCache.addListener` tells a screen about keys found just now (for a toast or a glow).
5. A new thing that should become discoverable by an action other than owning it calls `DiscoveryTracker.discover(serverPlayer, key)` from its server code.

## Wiring (one call each side)

* Mod constructor, both sides: `com.newuniverse.nusmp.core.discovery.Discovery.init(modEventBus, modContainer);`
  (registers the attachment, the payload, the server config and the game-bus tracker).
* Client-only block: `com.newuniverse.nusmp.client.discovery.DiscoveryClient.init();` (clears the cache when the player leaves a world).
* Lang keys: `nusmp.discovery.hint`, `nusmp.discovery.undiscovered`.

## Tests

`src/test/java/com/newuniverse/nusmp/core/discovery`: `DiscoveryRulesTest` (keys, visibility, labels, the slot diff, bitmask, chunking, migration), `DiscoveryDataTest`
(adding, caps, merge on clone, snapshot), `DiscoveryCacheTest` (not-synced shows all, replace and delta, show-all, listeners).
Nothing has been run in game: the tracker, the packet and the screens were type-checked only.
