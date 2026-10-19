"""Writes docs/attributes/{tree,vine,plant,poison_plant,mud,mucus}_rebuild.md, the docs of the 1.93.0 nature-magic rebuild.

    python3 -B tools/gen_tree_rebuild_docs.py
"""
import os
import sys

sys.dont_write_bytecode = True
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "attributes")

COMMON_NOT_VERIFIED = [
    "Everything visual in the running game. The previews (`tools/render_preview`, `tools/vfx_preview`) are re-implementations of the render layers, so colours, depth sorting against real terrain, shaders and the first-person view are unchecked.",
    "The canon wiki could not be reached from the build sandbox (no network), so every spell is built from the canon descriptions that were known and from what the magic is, not from a page of the wiki. Names are in the canon idiom; where a name is invented it is an honest nearest version.",
    "The slim-arm fit of the arm pieces is only checked by arithmetic (the pieces squeeze about the inner edge to 0.75 for the slim model), not on a real slim skin.",
    "Prop behaviour (follow, strike, expiry, being hit) and spell balance (damage, durations, cooldowns) were never played; they follow the same helpers as the other books but the numbers are first guesses.",
    "The vertex count of the armour is above the advisory 6000 per player for some magics (see the armour section); it was measured in the preview, not on a frame time.",
    "`EquipAnimationTest` and `MagicEnumAuditTest` need Minecraft classes and could not run in the JUnit shim; `GeoAssetsTest` and `TreeRulesTest` were run and pass.",
    "Merge: the shared one-line registrations (Aura, PropKind, VfxShape, AuraRegistry, PropRegistry, PropPainterRegistry, VfxManager, WikiBooks) can conflict with other builders; they are listed in the lead report.",
]

M = {}

M["tree"] = dict(
    title="Tree Magic", enum="TREE", aura="Aura.TREE", armour="Heartwood armour", cls="TreeArts",
    canon=[
        "Black Clover: Tree Magic is the attribute of creating and controlling trees and wood (the grimoires of the World Tree family). No wiki page of it was reachable; the spells are built from the name and what the magic is.",
        "Look: bark, heartwood with golden tree-rings, living branches, leaves, acorns.",
    ],
    armour_desc="Eleven GeoLite pieces (`tools/gen_tree_rebuild_geo.py`) forged in and shattered out through `client/aura/EquipAnimation`, over a bark skin overlay.",
    armour_pieces=[
        ("helm", "head", 0, "from above", "bark helm with antlers of living branches"),
        ("crown", "head", 12, "from above", "floating ring of leaves (glow, detail)"),
        ("chest", "body", 2, "from the front", "layered bark plates with a glowing heartwood tree-ring (glow)"),
        ("back", "body", 4, "from behind", "a young tree growing from the back"),
        ("pauld_r / pauld_l", "arms", "3 / 5", "from the sides", "burl pauldrons that sprout"),
        ("bracer_r / bracer_l", "arms", "6 / 8", "from the sides", "root-fingered bracers"),
        ("greave_r / greave_l", "legs", "9 / 11", "from below", "log greaves"),
        ("aux", "body", 12, "from below", "four orbiting acorns (glow, detail)"),
    ],
    armour_fx="Sprites: leaves and motes, a ring sigil on the ground (additive). Style colour 0xFFC866.",
    armour_rule="Softening 0.85 of every blow while it runs (10 s); whoever strikes the wearer is pricked and rooted.",
    summons=[
        ("tree_guardian", "TREE_1 variant 0", "27 bones, 62 cubes", "idle, rise, slam", "a treant of heartwood: trunk body, root feet, branch arms, a crown of leaves, glowing heart. Follows the caster and slams the nearest foe for 20 s; can be hit."),
        ("tree_lance", "TREE_1 variant 1", "20 bones, 25 cubes", "idle, thrust", "a lance of root that bursts out of the ground; used by Root Lance, 1.5 s each."),
    ],
    spells=[
        ("attribute_bolt", "Splinter Shot", "KEPT id, remade", "starter: bolt 9 damage, pierces, Slowness II 2.5 s"),
        ("attribute_cut", "Root Lance", "KEPT id, remade", "mid: 4 to 7 root lances in a line, one every 2 ticks"),
        ("attribute_field", "Grove", "KEPT id, remade", "zone: radius 5.5 x EP scale, 8 s, foes 3 damage per second and rooted, friends mend 2.5 percent per second"),
        ("attribute_domain", "Great Tree Rising", "KEPT id, remade", "signature: radius 6 x scale, 15 damage, thrown up, rooted (control capped by BalanceLaw), cooldown 45 s"),
        ("attribute_guard", "Bark Armor", "KEPT id, remade", "mid: Heartwood armour 10 s, +4 empower"),
        ("heartwood_guardian", "Heartwood Guardian", "NEW", "signature: treant summon 20 s, cooldown 60 s"),
        ("seedling_volley", "Seedling Volley", "NEW", "mid: five seeds, 5 damage each, rooted 2 s"),
        ("timber_wall", "Timber Wall", "NEW", "mid: 5 x 3 oak logs for 10 s, foes in front thrown back, 4 damage"),
        ("verdant_canopy", "Verdant Canopy", "NEW", "zone: radius 7 x scale, 10 s, friends mend 3 percent per second and regenerate, foes slowed and 2 damage per two seconds"),
        ("heartwood_fist", "Heartwood Charge", "NEW", "mid: dash 13 damage, throws back, Slowness II 2 s"),
    ],
)

M["vine"] = dict(
    title="Vine Magic", enum="VINE", aura="Aura.VINE", armour="Thornbine armour", cls="VineArts",
    canon=[
        "Black Clover: Vine Magic is the attribute of creating and controlling vines, thorns and tendrils. No wiki page of it was reachable; built from the name and what the magic is.",
        "Look: green braided vines, thorns, buds with a lime glow.",
    ],
    armour_desc="Eleven GeoLite pieces (`tools/gen_vine_rebuild_geo.py`); every piece grows in from below, as if the vines climbed the body from the ground.",
    armour_pieces=[
        ("helm", "head", 0, "from below", "braided circlet with tendril horns"),
        ("crown", "head", 12, "from below", "ring of thorns and buds above the head (glow, detail)"),
        ("chest", "body", 2, "from below", "criss-cross bands with a glowing bud (glow)"),
        ("back", "body", 4, "from below", "four jointed vine whips arching over the shoulders"),
        ("pauld_r / pauld_l", "arms", "3 / 5", "from below", "a fan of leaves on each shoulder"),
        ("bracer_r / bracer_l", "arms", "6 / 8", "from below", "spiral bracers with groping tendril fingers"),
        ("greave_r / greave_l", "legs", "9 / 11", "from below", "wrapped greaves"),
        ("aux", "body", 12, "from below", "five thorn seeds spiralling round the body (detail)"),
    ],
    armour_fx="Sprites: leaves, lime motes, a thorn-ring sigil (additive). Style colour 0xB6FF5A.",
    armour_rule="Softening 0.90 of every blow while it runs (10 s); whoever strikes the wearer is rooted and pricked.",
    summons=[
        ("vine_serpent", "VINE_1 variant 0", "18 bones, 43 cubes", "idle, rise, strike", "a jointed serpent of vine with a thorned head that bites, roots and poisons the nearest foe for 15 s; can be hit."),
        ("vine_whip", "VINE_1 variant 1", "19 bones, 29 cubes", "idle, lash", "a jointed whip of vine that rises from the ground and lashes; used by Lash Barrage (0.7 s each)."),
        ("vine_wall", "VINE_1 variant 2", "13 bones, 61 cubes", "idle", "a wall of woven living vines with thorns; 10 s."),
    ],
    spells=[
        ("attribute_bolt", "Thorn Dart", "KEPT id, remade", "starter: bolt 9 damage, pierces, poison prick"),
        ("attribute_cut", "Vine Whip", "KEPT id, remade", "mid: line, 8 damage, hauls foes toward the caster"),
        ("attribute_field", "Thicket", "KEPT id, remade", "zone: 8 s, tangle, 2.5 damage per second, small magicule drain"),
        ("attribute_domain", "Verdant Maelstrom", "KEPT id, remade", "signature: 3 s of inward whirl, 6 damage per second plus a wound, cooldown 45 s"),
        ("attribute_guard", "Thorn Mantle", "KEPT id, remade", "mid: Thornbine armour 10 s"),
        ("vine_serpent", "Vine Serpent", "NEW", "signature: serpent summon 15 s, cooldown 50 s"),
        ("vine_wall", "Thicket Wall", "NEW", "mid: wall summon 10 s"),
        ("vine_swing", "Vine Swing", "NEW", "mid: hauls the caster to the aim point, 5 damage and rooted to foes at the landing"),
        ("strangling_bloom", "Strangling Bloom", "NEW", "mid: hard control on one foe (BalanceLaw.controlTicks, shares the 12 s lock), 5 damage then 2.5 per second, magicule drain, bleed"),
        ("lash_barrage", "Lash Barrage", "NEW", "zone: 3 s, whips at up to three foes, 4 damage each"),
    ],
)

M["plant"] = dict(
    title="Plant Magic", enum="PLANT", aura="Aura.PLANT", armour="Blossom armour", cls="PlantArts / PlantBook",
    canon=[
        "Black Clover: Plant Magic is the magic of flowers and growing things; its best-known user is Mimosa Vermillion of the Silver Eagles, whose spells grow healing flowers and fern-like fronds and vines. Her named spells are Magic Flower Guidepost (kept) and Dream World of a Hundred Flowers (kept, remade). No wiki page was reachable; the rest is built from that.",
        "Look: pink and white petals, golden flower hearts, fern leaves, soft pollen.",
    ],
    armour_desc="Eleven GeoLite pieces (`tools/gen_plant_rebuild_geo.py`) that drift in from above and the sides like falling petals.",
    armour_pieces=[
        ("helm", "head", 0, "from above", "flower crown base with leaf ribbons"),
        ("crown", "head", 12, "from above", "halo of eight turning petals (glow, detail)"),
        ("chest", "body", 2, "from above", "leaf bodice with a great golden-hearted blossom and a flaring petal skirt (glow)"),
        ("back", "body", 4, "from behind", "two pairs of petal wings that beat"),
        ("pauld_r / pauld_l", "arms", "3 / 5", "from above and the sides", "a half-open bloom on each shoulder"),
        ("bracer_r / bracer_l", "arms", "6 / 8", "from above", "petal-cuffed leaf sleeves"),
        ("greave_r / greave_l", "legs", "9 / 11", "from above", "wrapped greaves"),
        ("aux", "body", 12, "from above", "six small blossoms drifting round the body (detail)"),
    ],
    armour_fx="Sprites: falling petals, rising pollen, a flower sigil on the ground (additive). Style colour 0xFFE690.",
    armour_rule="Softening 0.88 of every blow while it runs (10 s); each blow mends the wearer a little and the wearer regenerates.",
    summons=[
        ("plant_familiar", "PLANT_1 variant 0", "17 bones, 28 cubes", "idle, strike, heal", "a flower sprite: a small body with petal wings and a bud head. Circles the caster 30 s, mends the most hurt friend (heal clip) and spits petals at foes (strike clip); can be hit."),
        ("plant_bloom", "PLANT_1 variant 1", "9 bones, 19 cubes", "idle, open", "a great bloom with a golden heart that opens (open clip); used by Healing Bloom, 8.5 s."),
    ],
    spells=[
        ("guidepost", "Magic Flower Guidepost", "KEPT id, remade", "mid: flowers along the path, heals 20 percent of max health"),
        ("hundred_flowers", "Dream World of a Hundred Flowers", "KEPT id, remade", "signature: 8 s, friends 2 health per second, foes tangled and 2 damage per second"),
        ("yggdrasil", "World Tree Magic: Yggdrasil", "KEPT (borrowed captain spell, untouched)", "unchanged"),
        ("briar_prison", "Briar Magic: Briar Prison", "KEPT (borrowed captain spell, untouched)", "unchanged"),
        ("seed_bloom", "Seed Bloom", "NEW", "starter: a seed that blossoms, 7 damage and slow to foes, 3 health to friends in the blast"),
        ("healing_bloom", "Healing Bloom", "NEW", "mid: great bloom, heals friends about 8 s, slows foes"),
        ("flower_sprite", "Flower Sprite", "NEW", "signature: familiar 30 s, cooldown 50 s"),
        ("ivy_barrier", "Ivy Barrier", "NEW", "mid: wall of flowering leaves 12 s that dissolves bolts"),
        ("blossom_dress", "Blossom Dress", "NEW", "mid: Blossom armour 10 s, cooldown 15 s"),
        ("petal_storm", "Petal Storm", "NEW", "zone: 8 s, foes 2.5 damage and slowed, friends 1.5 health per second"),
    ],
    extra="`book/PlantBook.java` was rewritten (it is this magic's own file): the first four page ids and tiers are exactly the old ones; the last two old pages (the borrowed captain spells) keep their position.",
)

M["poison_plant"] = dict(
    title="Poison Plant Magic", enum="POISON_PLANT", aura="Aura.POISON_PLANT", armour="Venomflora armour", cls="PoisonPlantArts",
    canon=[
        "Black Clover: Poison Plant Magic creates and controls toxic plants. The canon description known: vines that skewer targets and tunnel through the ground, and plants that release a dense cloud of gaseous poison. Burrowing Venom Vine and Miasma Bloom follow that description; the rest is the nearest version. The wiki page itself was not reachable.",
        "Look: violet and acid-green flora, glowing spore pods, carnivorous traps, venom drops.",
    ],
    armour_desc="Eleven GeoLite pieces (`tools/gen_poison_plant_rebuild_geo.py`) that lunge in from the sides like snapping traps.",
    armour_pieces=[
        ("helm", "head", 0, "from the sides", "flytrap hood whose two toothed lobes open and snap"),
        ("crown", "head", 12, "from above", "ring of glowing spore pods (detail)"),
        ("chest", "body", 2, "from the front", "three swollen gourd pods with glowing veins (glow)"),
        ("back", "body", 4, "from behind", "a pitcher plant with an opening lid and a venom pool"),
        ("pauld_r / pauld_l", "arms", "3 / 5", "from the sides", "spiked pod pauldrons"),
        ("bracer_r / bracer_l", "arms", "6 / 8", "from the sides", "toxic leaf bracers with hooked venom claws"),
        ("greave_r / greave_l", "legs", "9 / 11", "from the sides", "bulb greaves"),
        ("aux", "body", 12, "from below", "four low spore pods (detail)"),
    ],
    armour_fx="Sprites: rising spores, dripping venom, a toxic sigil (additive). Style colour 0xB8FF30.",
    armour_rule="Softening 0.92 of every blow while it runs (10 s); whoever strikes the wearer is poisoned.",
    summons=[
        ("poison_plant_bloom", "POISON_PLANT_1 variant 0", "12 bones, 33 cubes", "idle, open", "a poison bloom that opens and breathes a gas cloud (3 to 4 s of the zone); used by Miasma Bloom and Toxic Garden."),
        ("poison_plant_maw", "POISON_PLANT_1 variant 1", "9 bones, 36 cubes", "idle, snap", "a carnivorous trap with a toothed maw that snaps on the first foe near it, again every 2.5 s; used by Trap Maw (12 s) and Toxic Garden."),
    ],
    spells=[
        ("attribute_bolt", "Venom Thorn", "KEPT id, remade", "starter: bolt 9 damage, pierces, venom wound"),
        ("attribute_cut", "Burrowing Venom Vine", "KEPT id, remade", "mid: vine tunnels along the ground and erupts, 9 damage, thrown up, poisoned"),
        ("attribute_field", "Miasma Bloom", "KEPT id, remade", "zone: 9 s gas cloud, poison, blindness"),
        ("attribute_domain", "Toxic Garden", "KEPT id, remade", "signature: three blooms and two maws, 10 s, 3 damage per second, cooldown 45 s"),
        ("attribute_guard", "Venomflora Armor", "KEPT id, remade", "mid: Venomflora armour 10 s"),
        ("trap_maw", "Trap Maw", "NEW", "mid: maw summon 12 s"),
        ("spore_burst", "Spore Burst", "NEW", "mid: cone, 4 damage, blind, nausea, poison"),
        ("antivenom_graft", "Antivenom Graft", "NEW", "mid: cleanses harmful effects of the caster and friends near, heals 12 percent of max health, cooldown 15 s"),
        ("pitcher_swallow", "Pitcher Swallow", "NEW", "mid: hard control on one foe (shares the 12 s lock), 2.5 damage per 0.25 s tick window, drain"),
        ("thorn_volley", "Toxic Thorn Volley", "NEW", "mid: five homing venom thorns"),
    ],
)

M["mud"] = dict(
    title="Mud Magic", enum="MUD", aura="Aura.MUD", armour="Bogplate armour", cls="MudArts",
    canon=[
        "Black Clover: Mud Magic creates and controls mud. No wiki page of it was reachable; built from the name and what the magic is.",
        "Look: wet brown mud, amber cracks, thick clods, bubbling surface.",
    ],
    armour_desc="Eleven GeoLite pieces (`tools/gen_mud_rebuild_geo.py`) that drop onto the body like thrown clods (large, heavy upward offsets).",
    armour_pieces=[
        ("helm", "head", 0, "from above", "thick cowl with a visor slit and amber eyes"),
        ("crown", "head", 12, "from above", "orbiting clods (glow, detail)"),
        ("chest", "body", 2, "from above", "three cracked slabs with amber seams and a pectoral boulder (glow)"),
        ("back", "body", 4, "from above", "a hunched hump with clod hands (glow)"),
        ("pauld_r / pauld_l", "arms", "3 / 5", "from above", "big mounds on the shoulders"),
        ("bracer_r / bracer_l", "arms", "6 / 8", "from above", "massive gauntlet fists"),
        ("greave_r / greave_l", "legs", "9 / 11", "from above", "wading boots with a splash flare"),
        ("aux", "body", 12, "from above", "four orbiting globs (glow, detail)"),
    ],
    armour_fx="Sprites: falling droplets, rising dust, a cracked-earth sigil under the feet. Style colour 0xFF9A3C.",
    armour_rule="Softening 0.70 of every blow while it runs (10 s), the hardest of the six; whoever strikes the wearer is slowed.",
    summons=[
        ("mud_golem", "MUD_1 variant 0", "9 bones, 41 cubes", "idle, rise, slam", "a golem of mud with a boulder chest, amber cracks and clod fists. Follows the caster and pounds the nearest foe for 20 s; can be hit."),
        ("mud_spout", "MUD_1 variant 1", "14 bones, 30 cubes", "idle, erupt", "a spout of mud that bursts from the ground; used by Mud Spout, Mud Wall and Mud Shackle (1.5 s each)."),
    ],
    spells=[
        ("attribute_bolt", "Mud Ball", "KEPT id, remade", "starter: bolt 9 damage, bursts, slows"),
        ("attribute_cut", "Mud Wave", "KEPT id, remade", "mid: line 10 wide, thrown back, slowed"),
        ("attribute_field", "Mud Pit", "KEPT id, remade", "zone: 8 s, sinks and slows hard, 2 damage per second, weakness"),
        ("attribute_domain", "Bog Domain", "KEPT id, remade", "signature: 15 damage, thrown back, then mired 6 s, cooldown 45 s"),
        ("attribute_guard", "Mud Armor", "KEPT id, remade", "mid: Bogplate armour 10 s"),
        ("mud_golem", "Mud Golem", "NEW", "signature: golem summon 20 s, cooldown 60 s"),
        ("mud_spout", "Mud Spout", "NEW", "mid: spouts under up to four foes, damage, thrown up"),
        ("mud_wall", "Mud Wall", "NEW", "mid: wall of packed mud, foes in front thrown back, 4 damage"),
        ("mud_shackle", "Mud Shackle", "NEW", "mid: hard control on one foe (shares the 12 s lock), 4 damage"),
        ("mud_barrage", "Mud Barrage", "NEW", "zone: 3 s of globs on the aim point, 4 damage each"),
    ],
)

M["mucus"] = dict(
    title="Mucus Magic", enum="MUCUS", aura="Aura.MUCUS", armour="Gelplate armour", cls="MucusArts",
    canon=[
        "Black Clover: Mucus Magic smothers and entraps, absorbs physical attacks and absorbs magic power (the canon description known). Smother, Absorption Wall and the armour's soaking follow that description. The wiki page itself was not reachable.",
        "Look: translucent teal gel, glowing nuclei, bubbles, drips.",
    ],
    armour_desc="Eleven GeoLite pieces (`tools/gen_mucus_rebuild_geo.py`), ALL on the alpha-blended layer (the `translucent` style flag keeps a fully formed piece off the alpha-tested layer); they slide onto the body from the inside of each limb like a coat of slime.",
    armour_pieces=[
        ("helm", "head", 0, "from above and the front", "gel dome with a glowing nucleus and dripping strings (glow)"),
        ("crown", "head", 12, "from above", "eight bubbles circling above the head (glow, detail)"),
        ("chest", "body", 2, "from the front", "a swollen bladder with its nucleus visible through the layers (glow)"),
        ("back", "body", 4, "from behind", "four waving pseudopod tendrils (glow)"),
        ("pauld_r / pauld_l", "arms", "3 / 5", "from the inside", "stacked wobbling blobs on the shoulders (glow)"),
        ("bracer_r / bracer_l", "arms", "6 / 8", "from the inside", "gel gloves"),
        ("greave_r / greave_l", "legs", "9 / 11", "from the inside", "puddle boots"),
        ("aux", "body", 12, "from below", "six orbiting bubbles (glow, detail)"),
    ],
    armour_fx="Sprites: rising bubbles, falling drips, a ring of gel on the ground (translucent / additive). Style colour 0x8CFFE0.",
    armour_rule="Softening 0.65 of every blow while it runs (10 s), the softest hit of the six; blows feed the wearer a little and drain the magicules of whoever strikes.",
    summons=[
        ("mucus_construct", "MUCUS_1 variant 0", "5 bones, 26 cubes", "idle, rise, absorb", "a mound of gel with a nucleus; the absorb clip plays when it swallows a projectile. Stands beside the caster 15 s, engulfs foes; can be hit."),
        ("mucus_tendril", "MUCUS_1 variant 1", "6 bones, 11 cubes", "idle, strike", "a gel tendril that lashes; used by Mucus Tendril (1 s)."),
    ],
    spells=[
        ("attribute_bolt", "Mucus Shot", "KEPT id, remade", "starter: bolt 9 damage, bursts, slows, takes a little magicule"),
        ("attribute_cut", "Mucus Tendril", "KEPT id, remade", "mid: line, 8 damage, slows, drags toward the caster"),
        ("attribute_field", "Sticky Field", "KEPT id, remade", "zone: 8 s, slowed hard, 2 damage per second, magicule absorbed"),
        ("attribute_domain", "Engulfing Tide", "KEPT id, remade", "signature: 13 damage, slowed, silenced, drained, cooldown 45 s"),
        ("attribute_guard", "Mucus Armor", "KEPT id, remade", "mid: Gelplate armour 10 s"),
        ("mucus_construct", "Mucus Construct", "NEW", "signature: construct summon 15 s, cooldown 50 s"),
        ("smother", "Smother", "NEW", "mid: hard control on one foe (shares the 12 s lock), 4 damage then 2 per second, drain"),
        ("absorption_wall", "Absorption Wall", "NEW", "mid: wall 8 s that dissolves bolts and drains foes beyond it"),
        ("sticky_trail", "Sticky Trail", "NEW", "mid: trail 6 s that slows, 2 damage per 1.5 s"),
        ("mucus_burst", "Bubble Burst", "NEW", "zone: bubbles drift and burst, 6 damage each, slowed"),
    ],
)

VFX = {
    "tree": ("TREE_FX1", "Root Lance: a growth-ring sigil contracts on the caster, a lance of two twisted root straps flies to the target with bark shards trailing", "TREE_FX2", "Grove: a growth-ring sigil turns on the ground, branches grow round the rim one after another and put on leaves, leaves fall and sap rises", "TREE_FX3", "Great Tree Rising: a flash and a ground ring, a crown of root masses erupts upward and leans outward, bark and splinters fly, leaves fall"),
    "vine": ("VINE_FX1", "Thorn Whip: a spiral-tendril sigil contracts on the caster, two vine straps wind round the line like a cracking whip with thorns bristling off them", "VINE_FX2", "Thicket: the sigil turns on the ground, vine columns grow round the rim one after another and curl over at the top, thorns line the inner side", "VINE_FX3", "Verdant Maelstrom: a flash and a thorn star, the ground ring runs out, vines spiral up and lean inward over the centre, thorn shards and leaves fly"),
    "plant": ("PLANT_FX1", "Seed Bloom: a flower mandala contracts on the caster, a golden seed flies on a thin green stem shedding petals in a spiral, a flower opens where it lands", "PLANT_FX2", "Hundred Flowers: the mandala turns on the ground, blossoms pop up over it one after another, petals and pollen rise, a soft golden ring pulses", "PLANT_FX3", "Great Bloom: a flash, a giant flower opens above the centre, a ring of petals runs out, petals fly and fall, pollen rises"),
    "poison_plant": ("POISON_PLANT_FX1", "Venom Thorn: a toxic sigil contracts on the caster while spores are drawn in, a hooked fang flies in a trail of venom drops and gas puffs", "POISON_PLANT_FX2", "Miasma: the sigil turns on the ground, thorny vines topped with spore pods grow round the rim, great gas puffs drift across the zone, spores rise", "POISON_PLANT_FX3", "Toxic Bloom: a flash and a spiked star, a ring of fangs rises round the centre and closes in, a wall of gas runs out, spores and drops fly"),
    "mud": ("MUD_FX1", "Mud Ball: amber fissures spread on the ground sigil while mud gathers, a heavy glob flies on a sagging line of drops and dust", "MUD_FX2", "Mud Pit: the cracked sigil sinks into the ground, ripples run out, blobs of mud rise, swell and burst, clods sink, dust drifts", "MUD_FX3", "Mud Eruption: a flash, a column of mud shoots up and crowns in a splash, clods fly, a ring of dust runs out, the ground splits with amber light"),
    "mucus": ("MUCUS_FX1", "Mucus Shot: a membrane sigil contracts on the caster while a bubble swells, a wobbling blob flies pulling a sticky strand and shedding bubbles", "MUCUS_FX2", "Sticky Field: the membrane mandala turns on the ground, ripples of gel run out, sticky strands rise and sway round the rim, bubbles float up", "MUCUS_FX3", "Engulfing Tide: a giant bubble swells round the centre with a glowing blob inside, strands arc across it, then it bursts in droplets and a ring of gel"),
}

VERTS = {"tree": 5552, "vine": 7044, "plant": 6784, "poison_plant": 6440, "mud": 4344, "mucus": 5692}


def table(rows, head):
    out = ["| " + " | ".join(head) + " |", "|" + "|".join(["---"] * len(head)) + "|"]
    for r in rows:
        out.append("| " + " | ".join(str(c) for c in r) + " |")
    return "\n".join(out)


def doc(key):
    d = M[key]
    t = d["title"]
    L = []
    L.append("# %s rebuild (1.93.0)\n" % t)
    L.append("Magic type `MagicType.%s`, grimoire book `book_%s`, painter `%s`, props `PropKind.%s_1`, VFX layer `%sLayer`. Self-tests `magic.tree_rebuild_pages`, `magic.tree_rebuild_assets`, `magic.tree_rebuild_props` (registered once for all six nature magics); JUnit `TreeRulesTest`.\n" % (
        d["enum"], key, d["aura"], d["enum"], "".join(w.capitalize() for w in key.split("_"))))
    L.append("## NEW / REPLACED / KEPT\n")
    rows = [
        ("Temporary armour (%s)" % d["armour"], "NEW", "No armour set existed for this magic. Added next to everything else."),
        ("Summon / construct models", "NEW", ", ".join(s[0] for s in d["summons"])),
        ("Page ids, order, tiers, mastery bits", "KEPT", "Every old id stays at its old position; new pages are appended last (`TreeRulesTest` checks this)."),
        ("Texts and numbers of the five generic pages", "REPLACED", "The generic placeholder spells of the book are remade as %s spells; the ids stay." % t if key != "plant" else "Guidepost and Dream World are remade; the two borrowed captain spells are untouched."),
        ("Page list", "NEW", "Pages appended: " + ", ".join(s[0] for s in d["spells"] if s[2] == "NEW")),
        ("VFX", "NEW", "Three new shapes %s, %s, %s (appended to `VfxShape`); the old `WIKI_MAGIC_*` shapes stay registered and are still used by the book for the cast circle." % (VFX[key][0], VFX[key][2], VFX[key][4])),
        ("Sprites", "NEW", "Eight coloured particle sprites `textures/particle/%s_*.png`." % ("tree_fx" if key == "tree" else key)),
        ("Time Magic files, World Tree / Briar / Cherry Blossom / Fungus files", "KEPT", "Not touched."),
    ]
    L.append(table(rows, ["Part", "State", "Notes"]) + "\n")
    L.append("## Canon sources\n")
    for c in d["canon"]:
        L.append("- " + c)
    L.append("- The wiki was unreachable from the build sandbox. What is stated above as canon is only what was known without the page.\n")
    if d.get("extra"):
        L.append(d["extra"] + "\n")
    L.append("## Armour: %s (`%s`)\n" % (d["armour"], d["aura"]))
    L.append(d["armour_desc"] + " Delay 0 to 12 ticks per piece, forge-in over 8 ticks, shatter over the last 18 ticks, the same `EquipAnimation` as the Djinn Full Equip. First person hides the head pieces; the pieces drop detail at 24 and 56 blocks.\n")
    L.append(table(d["armour_pieces"], ["Piece", "Part", "Delay (ticks)", "Flies in", "Model"]) + "\n")
    L.append(d["armour_fx"] + "\n")
    L.append("Render passes: skin overlay (cutout, translucent while fading), pieces (%s), skin glow (additive), sprites (additive). " % ("translucent" if key == "mucus" else "cutout, glow additive") + d["armour_rule"] + "\n")
    L.append("Preview cost: about %d vertices for the full set in the preview (advisory budget 6000)%s.\n" % (VERTS[key], ", above the advisory budget" if VERTS[key] > 6000 else ""))
    L.append("## Summon and construct models\n")
    L.append(table([(s[0], s[1], s[2], s[3], s[4]) for s in d["summons"]], ["Model", "Prop", "Size", "Clips", "What it is"]) + "\n")
    L.append("GeoLite, box UV with an emissive `_glow` pass, checked by `tools/geo_check.py` and `GeoAssetsTest`. Painted by `client/prop/%sPropPainter` (`TreePropPainter` helpers). Summons are bound to the caster, can be hit and expire; the variant is the low byte of the prop param, the phase bits 8-9, the clock bits 10-15.\n" % "".join(w.capitalize() for w in key.split("_")))
    L.append("## Spells\n")
    L.append("Damage numbers are the base values given to the grimoire's `hurt` (scaled by the book, mastery and EP). Control follows `BalanceLaw.controlTicks` (bosses are capped, hard controls share the 12 second lock).\n")
    L.append(table(d["spells"], ["Page id", "Name", "State", "Numbers"]) + "\n")
    L.append("Lang: `tools/lang/%s_rebuild.json` (book description and a description for every page). Page names are literal strings in `book/%s.java`.\n" % (key, d["cls"].split(" ")[0]))
    L.append("## VFX\n")
    v = VFX[key]
    L.append(table([(v[0], "cast", v[1]), (v[2], "zone", v[3]), (v[4], "impact", v[5])], ["Shape", "Use", "Look"]) + "\n")
    L.append("`vfx/client/layer/%sLayer.java` on the shared blocks of `TreeFx` (ground rings, standing shapes, strands, wind, debris, flash). Each shape stays inside the 400-vertex budget (80 to 184 used). Preview: `python tools/vfx_preview/preview.py` with the scenes in `tools/vfx_preview/scenes.d/%s_rebuild.json`; armour and summons: `python tools/render_preview/preview.py` with `tools/render_preview/scenes.d/%s_rebuild.json`.\n" % (
        "".join(w.capitalize() for w in key.split("_")), key, key))
    L.append("## Self-tests\n")
    L.append("- `magic.tree_rebuild_pages`: every magic resolves to its rebuilt book, whose page ids keep the old ids first in order, append the new ones, have no duplicate and stay at 24 family pages or fewer.")
    L.append("- `magic.tree_rebuild_assets`: every armour piece, summon model, skin and shape of the six magics resolves (66 armour models, 13 summon models, 12 skins, 18 shapes).")
    L.append("- `magic.tree_rebuild_props`: every variant of the six prop kinds is spawned in a scratch level and must come back with the right kind and variant byte.")
    L.append("- JUnit `TreeRulesTest` (6 tests): page order, limits, armour softening, param packing; a mutation check (swapped ids, changed packing) made it fail as it should.")
    L.append("- `GeoAssetsTest` (4 tests) covers every new model and animation.")
    L.append("- The smoke list of the lead should add the `magic` prefix to `SMOKE_EXPECT`.\n")
    L.append("## NOT VERIFIED\n")
    for n in COMMON_NOT_VERIFIED:
        L.append("- " + n)
    return "\n".join(L) + "\n"


def main():
    os.makedirs(OUT, exist_ok=True)
    for key in M:
        path = os.path.join(OUT, key + "_rebuild.md")
        with open(path, "w") as f:
            f.write(doc(key))
        print("wrote docs/attributes/%s_rebuild.md" % key)


if __name__ == "__main__":
    main()
