# Spirit Lords (0.102)

Two kinds, as in the two source worlds.

## Black Clover Spirit Lords (exclusive, true bodies)
`entity/ElementalSpiritLordEntity` (id `nusmp:elemental_spirit_lord`), drawn by `client/ElementalSpiritRenderer`.
One mage per element on the server (`book/SpiritSlots`); only that mage can call it (Spirit Charm, sneak-use, after the Incarnation) and only one
body of each element can exist. Admin: `/<grimoire command> spirit_body <player> <salamander|undine|sylph|gnome>`.

| Spirit | Body (from the reference art) | Technique | Aura for its master |
| --- | --- | --- | --- |
| Salamander | winged fire dragon, flames down the spine and tail | Flame Breath (cone, ignites) | Fire Resistance |
| Undine | woman of water: foam hair, wave dress, fin ears, orbiting drops | Tidal Lance (slows) + mends her master below 70% | Water Breathing |
| Sylph | green fairy, dragonfly wings, drifting bubbles | Gale Cut (throws foes up) | Speed, Slow Falling |
| Gnome | hunched stone giant, cracked plates, glowing eyes | Earth Slam (stone claws, slows, knockback) | Resistance |

Models: `python3 -B tools/gen_spirit_lords.py [salamander undine sylph gnome]` (writes `geo/entity/spirit_*.geo.json`, animations idle / move / cast / hit,
textures and glow). Undine's water and Sylph's wings are separate translucent models (`spirit_undine_water`, `spirit_sylph_wings`) drawn over the body.
Preview: `python3 -B tools/render_preview/preview.py spirit_salamander_idle spirit_undine_idle spirit_sylph_idle spirit_gnome_idle`.

## Tensura-style spirits (not exclusive, small orbs)
`entity/SpiritLordEntity` (`nusmp:spirit_lord`), drawn by `client/SpiritOrbRenderer`: a small glowing orb in the spirit's colour that follows the player.
Without a Black Clover bond the Spirit Charm calls one: use to pick the element, sneak-use to call it. The Anti-Magic lord (Black Devil) is also an orb.

## Update 3.0.0: controls moved to the spirit control plane

The Spirit Lord (the orb and the true body) can now also be called and sent away from the grimoire of its element ("Summon Spirit / Release Spirit" page) or with `/multiverse spirit summon lord` / `release lord`, with the same rules as the Spirit Lord skill's Call Spirit mode and the Spirit Charm (the spirit must answer, it needs its body, a mage who holds the element's slot gets the true body). "Spirit Combat Mode" (a page, a third mode of the Spirit Lord skill, or `/multiverse spirit combat on`) makes the Lord cast its element's spells with its own mana pool and a share of the owner's magicules instead of for free. Full description, numbers and costs: `docs/magic/spirit_control.md`.
