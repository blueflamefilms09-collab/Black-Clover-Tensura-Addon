# The spirit aura, remade (3.77.0)

**Asked for:** "Remove or remake this effect to look better, I don't like it; that goes for any abilities or power that uses this effect" (the `SPIRIT_AURA` picture: blue spiral glyphs circling the head and shoulders).

**What it is now** (`vfx/client/layer/AuraLayer.aura`; before/after `docs/previews/spirit_aura/before_after.png`): a rising aura in the caster's colour. Eight tongues of flame stand round the body from the feet past the head, each with its own height and flicker; four thinner, brighter ones run inside them; sparks climb through; the body glows and breathes; a rune band turns on the ground and a pulse of light runs out from the feet every second and a half. It ignites in the first tenth of its life and dies down at the end. The spiral glyphs are gone.

**Where it shows** (one change, every user): the effect is one shape that about fifty abilities send, so all of them changed at once and none had to be edited: the grimoire "empower" pages (Star Shield, Mana Reinforcement, Full Body Reinforcement, Beast Form and the like), Spirit Dive, the Anti-Magic forms and the Anti-Magic Lord, the Forbidden pages and the devil's presence, the Salamander link, the character arts of the Black Bulls and the Golden Dawn, the Hero Gauntlet, Scythe and Katana buffs, the Magic Gear, the avatar evolution, naming and ritual moments, the Yandere frenzy, Ban's focus, the Spirit Lord entities, two Ultimate fallbacks and the Blue Exorcist devil ritual.

**REPLACED** (the owner asked): the look of `SPIRIT_AURA`. The old spirals are removed, not kept behind a setting, because the owner asked for them to go. **KEPT**: the shape's id, colour, duration and size meaning (nothing that sends it changed), the texture file `spirit_spiral.png` (unused now), the other two pictures of the same layer (mana charge, weapon constructs).

**Cost:** client drawing only, no packet or server change. 252 vertices at full detail (the shape's budget is 400; the old picture used about 140), fewer at lower particle settings (the counts go through `ctx.seg`).

**Not seen in a game.** The picture is from the offline previewer. A very dark colour (the Anti-Magic black) shows as a pale grey flame, because the effect is drawn additively.
