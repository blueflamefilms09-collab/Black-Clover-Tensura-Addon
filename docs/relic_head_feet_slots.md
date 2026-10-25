# Head and feet relics in the Curios slots (3.12.0)

Owner request ("they don't show anyway ... move them"): the armour-slot head and feet relics (Mask of Mind Melting, Ruby Boots of Planestriding and all the other 115 head and 49 feet relics) were not drawn on the body. They are now worn in the Curios slots **Head Relic** (`relic_head`) and **Feet Relic** (`relic_feet`), which 3.10.0 added, and are drawn by the same worn layer as neck and back relics (Curios eye icon: off = not drawn).

## How it is read

| What you do | What happens |
|---|---|
| Right-click a head / feet relic | It goes to the free place of `relic_head` / `relic_feet`, one copy, with a sound and an action-bar line. Sneak + right-click is still the attunement ritual and the second ability. |
| ... and that Curios place is taken | Nothing is put on; the action bar says to take the worn one off first. It is **not** put on as armour behind your back. |
| ... and Curios is not installed, or the player has no such slot | The old behaviour runs: it goes on as armour. |
| A head / feet relic is in the HEAD / FEET armour slot (put there by a click, a shift-click, a dispenser, an old save) | Within a second it is moved to the free Curios place (the action bar says so). If the place is taken, or Curios is missing, it stays in the armour slot and keeps working; the try is repeated every 10 s. |
| (client side) | The client does not predict the vanilla armour swap for a head / feet relic the server redirects, so its armour slot never shows a helmet the server did not give. |
| A head / feet relic sits in its Curios slot | The relic works like any worn relic, and it adds the **armour points and toughness its armour form had** (`RelicDef.armorDefence`, `armorToughness`) as ordinary attribute modifiers, so nothing is lost by moving. |

Setting (SERVER): `relics.headFeetInCurios` in `nusmp-relicpower-server.toml`, `true` by default. `false` restores the 3.11 behaviour exactly (right-click puts them on as armour, nothing is moved).

## Worst-case cost (CLAUDE.md rule 11)

| What | Bound |
|---|---|
| Server work | Once a second per player, inside the relic pass that already ran: two armour-slot reads and an instance check. The Curios inventory (one reflective map) is read only when a head / feet relic is actually in an armour slot, and after a try that found no place the same player is skipped for 200 ticks. |
| Attribute changes | At most 4 modifiers per player (armour + toughness for one head and one feet relic), hard cap 8, set only when the value changes (the pass already diffs them). |
| Entities, chunks, blocks | None scanned. |
| Packets | None of its own; the changed slots and attributes sync as they always do. |
| Per-player state | One table entry (the back-off) only while a try is pending; removed on logout and on server stop. |
| Client | Only a longer tooltip line. The drawing is the 3.10.0 path (the worn layer, already budgeted). |

## What is not included

* **Named mobs** still wear relics in their armour slots (they have no Curios inventory).
* **Chest relics** stay armour; only head and feet move.
* An item older than the relics (an adopted item) is not in the `relic_head` / `relic_feet` tags and is never redirected.
* The armour-slot display bug itself (the CI client run found the slot empty on the client a few seconds after the server set it) was **not** diagnosed; this change moves the relics out of the slot instead.
* Not seen in a running game: the draw of a Curios-worn head or feet relic is the 3.10.0 worn-layer path and is covered by tests and the CI server smoke test (`relic.headfeet.slots`), not by eye.

## Files

`relics/HeadFeetMoveRules` (pure rules), `relics/HeadFeetCurios` (the move and the kept armour), `relics/RelicCurioEquip` (right-click), `relics/RelicEngine` (the once-a-second call), `relics/RelicPowerConfig` (the setting), `relics/HeadFeetSelfTests` (`relic.headfeet.slots`), tests `HeadFeetMoveRulesTest`, `HeadFeetWiringTest`, `HeadFeetCurioSlotsTest`.
