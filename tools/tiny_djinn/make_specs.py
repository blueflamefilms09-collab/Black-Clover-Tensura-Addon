"""Writes specs.json from the table below (kept so the table can be edited as Python and regenerated; gen_tiny_djinn.py reads specs.json).

Row: id, name, body colour, glow colour (both copied from relics/djinn/Djinn.java, checked by TinyDjinnAssetsTest), skin tone, (hair, cloth, cloth2, trim, metal, eye) hexes,
the look (recipe names of tdparts.py) and a one-line note for docs/tiny_djinn.md.
"""
import json
import os

ROWS = [
    ("baal", "Baal", "3C8CFF", "BFE4FF", "fair", ("9CC8FF", "2A5FC8", "BCD6FF", "F0E6A0", "5A6A90", "DFF2FF"),
     dict(eyes="sharp", mouth="flat", chest="plate", pauldron="spiked", hair="spiky", head=["bolt_crown"], ears="long", cape="mantle", weapon="lance", tail="bolt", acc="orbit_sparks"),
     "storm diadem of three forked bolts, swept-back blue hair, a thunder lance"),
    ("amon", "Amon", "FF5A14", "FFD27A", "tan", ("FF7A1E", "8A1E0A", "FF8C28", "FFD060", "3A1A10", "FFE680"),
     dict(eyes="sharp", mouth="fang", chest="plate", pauldron="big", hair="flame", head=["flame_crown", "horns_ram"], ears="none", cape="scarf", weapon="firesword", tail="flame", acc="orbit_embers"),
     "flame crown and ram horns, hair of fire, a flaming greatsword"),
    ("vinea", "Vinea", "2D9CDB", "B5F0FF", "blue", ("3AC8E8", "1E6FA8", "9FE0F4", "E8F8FF", "5C8CAE", "B5F0FF"),
     dict(eyes="round", mouth="smile", chest="robe", pauldron="round", hair="long", head=["fin_tiara"], ears="fin", cape="veil", weapon="trident", tail="water", acc="orbit_bubbles"),
     "fin tiara and fin ears, long tide-blue hair, a veil, a trident"),
    ("focalor", "Focalor", "62D6A0", "E6FFF1", "fair", ("D8FFF0", "2E9C78", "E6FFF1", "C8F0E0", "6AA090", "A0FFD8"),
     dict(eyes="calm", mouth="smile", chest="vest", pauldron="round", hair="short", head=["wind_plume"], ears="long", cape="scarf", weapon="glaive", tail="wind", acc="orbit_rings"),
     "a swept wind plume and temple wings, a long scarf, a glaive, a wisp that spirals"),
    ("zagan", "Zagan", "58C75A", "E4FFB8", "tan", ("3E8F3A", "2E7A38", "A8E070", "F0D870", "6A5A3A", "C8FF90"),
     dict(eyes="round", mouth="smile", chest="vest", pauldron="round", hair="moss", head=["antlers_leaves"], ears="leaf", cape="mantle", weapon="bloom_staff", tail="vine", acc="orbit_petals"),
     "antlers hung with oak leaves, moss hair, a blooming staff, petals in orbit"),
    ("zepar", "Zepar", "D43A3A", "FFB9A0", "red", ("2A1010", "6A1818", "A02828", "C8A060", "4A4A56", "FFB9A0"),
     dict(eyes="visor", mouth="flat", chest="plate", pauldron="big", cover="helm", hair="none", head=["great_helm", "ram_horns_dark"], ears="none", cape="none", weapon="axe", tail="smoke", acc="orbit_boulders", glove=True),
     "great helm with ram horns, huge pauldrons, a war axe, boulders in orbit"),
    ("furfur", "Furfur", "B77BFF", "F1DDFF", "violet", ("F1DDFF", "6A3CB8", "C8A0FF", "F0D8FF", "5A4A78", "F1DDFF"),
     dict(eyes="calm", mouth="open", chest="robe", pauldron="round", hair="short", head=["bell_crown"], ears="cup", cape="mantle", weapon="horn", tail="wind", acc="orbit_rings"),
     "a crown of bells, speaker cups on the ears, a war horn, rings of sound"),
    ("marchosias", "Marchosias", "E23A22", "FFA878", "tan", ("5A1A14", "7A2418", "C85030", "FFA878", "3A2A28", "FFD060"),
     dict(eyes="slit", mouth="fang", mark="whisker", chest="vest", pauldron="spiked", hair="none", head=["wolf_cowl"], ears="wolf", cape="mantle", weapon="claws", tail="flame", acc="orbit_embers"),
     "a wolf cowl with pricked ears, a fur mantle, claws, a tail of embers"),
    ("sitri", "Sitri", "FFE070", "FFFFFF", "pale", ("FFE8A0", "F4D060", "FFFFFF", "FFF4B8", "C8A850", "FFC850"),
     dict(eyes="round", mouth="smile", chest="robe", pauldron="round", hair="bun", head=["sun_crown"], ears="none", cape="veil", weapon="bow", tail="smoke", acc="orbit_suns"),
     "a crown of sunrays, twin buns, a golden bow, small suns in orbit"),
    ("bifron", "Bifron", "7B4DFF", "D2C2FF", "ash", ("3A2A78", "4A2CB8", "8C70FF", "D2C2FF", "3A3458", "D2C2FF"),
     dict(eyes="visor", mouth="flat", chest="plate", pauldron="spiked", hair="none", head=["storm_visor"], ears="none", cape="tattered", weapon="storm_glaive", tail="bolt", acc="orbit_sparks"),
     "a storm visor with forked antlers, a torn cloak, a stormfang glaive"),
    ("paimon", "Paimon", "4FC3D6", "DFFAFF", "fair", ("F4FFFF", "2C98B0", "DFFAFF", "C8F4FF", "7AA8B8", "8CE8FF"),
     dict(eyes="round", mouth="smile", chest="robe", pauldron="round", hair="white_mane", head=["crescent_circlet"], ears="none", cape="cape", weapon="moon_glaive", tail="wind", acc="moon"),
     "a crescent circlet, a great white mane, a crescent moon behind, a moon glaive"),
    ("vassago", "Vassago", "2F55D8", "B8CCFF", "pale", ("1A2A6A", "1C34A0", "6A8CF0", "B8CCFF", "28306A", "B8CCFF"),
     dict(eyes="hooded", mouth="flat", chest="robe", pauldron="spiked", hair="none", head=["bone_mask", "horns_ram"], ears="none", cape="tattered", weapon="scythe", tail="water", acc="orbit_skulls"),
     "a bone mask with abyssal ram horns, a torn cloak, a scythe, skulls in orbit"),
    ("bael", "Bael", "C9A227", "FFF0A0", "tan", ("6A4A1A", "8A1428", "C9A227", "FFF0A0", "4A3A1A", "FFF0A0"),
     dict(eyes="sharp", mouth="smile", chest="plate", pauldron="big", hair="short", head=["spire_crown"], ears="cat", cape="cape", weapon="claymore", tail="smoke", acc="throne"),
     "a crown of nine spires, cat ears, a royal cape, the high back of a throne, a claymore"),
    ("agares", "Agares", "1FA37A", "B8FFE0", "green", ("1A4A3A", "1A6A58", "6AD8B0", "E8E0A0", "3A5A50", "FFE060"),
     dict(eyes="slit", mouth="fang", chest="harness", pauldron="spiked", cover="hood", hair="none", head=["croc_snout"], ears="none", cape="plates", weapon="harpoon", tail="water", acc="jaws"),
     "a crocodile snout across the brow with eyes on the crown, dorsal plates, open jaws behind, a harpoon"),
    ("valefor", "Valefor", "5A3DB5", "E0D2FF", "fair", ("2A1A58", "3A2A90", "6A50D0", "E0D2FF", "2A2048", "E0D2FF"),
     dict(eyes="sharp", mouth="smile", chest="harness", pauldron="round", cover="hood", hair="none", head=["hood", "bolt_temples"], ears="donkey", cape="scarf", weapon="daggers", tail="bolt", acc="keys"),
     "a thief's hood with ass's ears and a trailing scarf, crossed daggers, three stolen keys"),
    ("barbatos", "Barbatos", "2E7D32", "CFFFB0", "tan", ("3A6A28", "2A6A30", "6AB040", "C8A060", "5A4A2A", "CFFFB0"),
     dict(eyes="calm", mouth="smile", chest="vest", pauldron="round", hair="moss", head=["antlers_leaves"], ears="leaf", cape="mantle", weapon="antler_bow", tail="vine", acc="quiver"),
     "great antlers with oak leaves, moss hair, a recurve bow with antler tips, a quiver"),
    ("gusion", "Gusion", "3D7EAA", "D6F2FF", "fair", ("8CC8E8", "2A5E8A", "B0DCF4", "E8F6FF", "6A8CA4", "D6F2FF"),
     dict(eyes="calm", mouth="smile", chest="robe", pauldron="round", hair="short", head=["pane_crown"], ears="none", cape="veil", weapon="mirror_rapier", tail="water", acc="mirror"),
     "a crown of standing glass panes, a mirror on the brow, a rapier, a floating tide mirror"),
    ("vepar", "Vepar", "00A6B8", "BFFCFF", "teal", ("0A8A6A", "08788A", "70D8E0", "F0C860", "3A8A90", "BFFCFF"),
     dict(eyes="round", mouth="smile", chest="robe", pauldron="round", hair="long", head=["coral_crown"], ears="fin", cape="veil", weapon="anchor", tail="water", acc="orbit_bubbles"),
     "a coral crown, a shell at each temple, fin ears, seaweed hair, a ship's anchor"),
    ("shax", "Shax", "8E8EA8", "F0F0FF", "pale", ("F0F0FF", "B0B0C8", "F0F0FF", "D0D0E8", "8A8AA0", "C8C8E8"),
     dict(eyes="calm", mouth="veil", chest="robe", pauldron="round", hair="short", head=["dove_wings"], ears="none", cape="wings", weapon="fans", tail="feather", acc="doves"),
     "a dove's wings above the brow, a veil over the mouth, a short wing cape, two war fans, three doves"),
    ("buer", "Buer", "9CCC3B", "F4FFC0", "tan", ("6A8A28", "5A8A28", "C8E870", "F0E080", "6A5A30", "F4FFC0"),
     dict(eyes="round", mouth="smile", chest="vest", pauldron="round", hair="braids", head=["wheel_crown"], ears="leaf", cape="mantle", weapon="wheel_staff", tail="vine", acc="wheel"),
     "a small wheel on the crown, herb sprigs, leaf ears, beaded braids, a staff with a wheel and a serpent"),
    ("andrealphus", "Andrealphus", "2F6FDB", "A8D4FF", "fair", ("1A3A8A", "1A4AB0", "50C8D8", "F0D060", "2A3A6A", "A8D4FF"),
     dict(eyes="round", mouth="smile", chest="robe", pauldron="round", hair="short", head=["aigrette"], ears="round", cape="feather", weapon="star_lance", tail="feather", acc="peacock"),
     "an aigrette of three stalks, a peacock fan behind with eyes of light, a star lance"),
    ("kimaris", "Kimaris", "3A2A3F", "FF6A8A", "ash", ("3A2A3F", "3A2A3F", "6A3A6A", "FF6A8A", "26202E", "FF6A8A"),
     dict(eyes="sharp", mouth="flat", chest="plate", pauldron="spiked", hair="short", head=["horse_plate"], ears="none", cape="tattered", weapon="pennon_lance", tail="flame", acc="horseshoe"),
     "a steed's face plate with glowing eyes, tall horse ears, a flaming mane, a cavalry lance, a horseshoe behind"),
    ("amdusias", "Amdusias", "D58AF0", "FFE3FF", "pale", ("FFE3FF", "B060D0", "F4D0FF", "FFE3FF", "8A6AA0", "FFC8FF"),
     dict(eyes="round", mouth="smile", chest="robe", pauldron="round", hair="long", head=["horns_spiral"], ears="deer", cape="scarf", weapon="horn_rapier", tail="smoke", acc="forks"),
     "a spiral unicorn horn on the brow, deer ears, a tuning fork at each temple, a horn-bladed rapier"),
    ("belial", "Belial", "7A0F2E", "FF7096", "grey", ("1A0A12", "2A0A18", "7A0F2E", "D0A040", "1A1018", "FF7096"),
     dict(eyes="sharp", mouth="flat", chest="plate", pauldron="spiked", hair="spiky", head=["hollow_crown"], ears="none", cape="tattered", weapon="hollow_sword", tail="flame", acc="orbit_runes"),
     "a crown that is only its frame, horns curling into hoops, a greatsword that is an outline round a glowing hollow"),
    ("decarabia", "Decarabia", "E0B45A", "FFF4C8", "tan", ("B87840", "B87840", "F0E0B8", "FFF4C8", "8A6A3A", "FFF4C8"),
     dict(eyes="round", mouth="smile", mark="star", chest="plate", pauldron="big", cover="bald", hair="none", head=["carapace_cap"], ears="none", cape="plates", weapon="pincers", tail="smoke", acc="crab_legs"),
     "a carapace cap with a star, eyes on stalks, pincers at the temples, crab legs behind, a pair of claws"),
    ("seere", "Seere", "E8E8C0", "FFFFFF", "fair", ("F8F8E0", "E0E0B0", "FFFFF0", "F0D890", "A8A888", "C8E0A0"),
     dict(eyes="wide", mouth="smile", chest="vest", pauldron="round", hair="short", head=["wind_plume"], ears="none", cape="scarf", weapon="sabre", tail="wind", acc="pinwheel"),
     "small wings at the temples, a swept streamer, a swift sabre, a pinwheel of wind blades"),
    ("dantalion", "Dantalion", "C0A0FF", "F2E8FF", "pale", ("F2E8FF", "7A58C8", "E0D0FF", "F0D890", "6A5A90", "F2E8FF"),
     dict(eyes="round", mouth="smile", mark="runes", chest="robe", pauldron="round", hair="short", head=["scholar_cap"], ears="none", cape="cape", weapon="tome_staff", tail="smoke", acc="tome"),
     "a flat scholar's cap with a face at each temple, a gown, a staff topped with a chained tome, floating books"),
    ("andromalius", "Andromalius", "2B2B6B", "9CA8FF", "ash", ("1A1A50", "1A1A50", "3A3A90", "9CA8FF", "20203C", "9CA8FF"),
     dict(eyes="three", mouth="flat", chest="robe", pauldron="round", cover="hood", hair="none", head=["third_eye_hood"], ears="none", cape="tattered", weapon="spear_eye", tail="tendril", acc="eye_ring"),
     "a hood with a third eye and a serpent coiled round it, a spear with an eye in the blade, a ring of watching eyes"),
    ("glasya_labolas", "Glasya-Labolas", "A00000", "FF9090", "red", ("2A0A0A", "5A0A0A", "A00000", "B08060", "2A2024", "FF9090"),
     dict(eyes="slit", mouth="fang", mark="scar", chest="harness", pauldron="spiked", hair="short", head=["hound_skull"], ears="none", cape="tattered", weapon="claws", tail="tendril", acc="maw"),
     "a hound's skull over the brow, folded ears, a collar of spikes, swept horns, a ring of fangs behind, slaughter claws"),
    ("haagenti", "Haagenti", "B87333", "FFD9A0", "brown", ("3A2414", "6A4020", "B87333", "FFD9A0", "5A4A3A", "FFD9A0"),
     dict(eyes="round", mouth="smile", chest="plate", pauldron="big", hair="short", head=["goggles", "horns_bull"], ears="round", cape="mantle", weapon="cleaver", tail="smoke", acc="flask"),
     "bull's horns with bronze caps, goggles on the brow, a nose ring, a bronze cleaver, an alembic flask"),
    ("crocell", "Crocell", "9ADCF0", "FFFFFF", "pale", ("FFFFFF", "6AB0D8", "E0F6FF", "FFFFFF", "8AB0C8", "9ADCF0"),
     dict(eyes="calm", mouth="smile", beard="E8F8FF", chest="robe", pauldron="round", hair="short", head=["ice_crown", "horns_ice"], ears="none", cape="icecloak", weapon="crystal_staff", tail="ice", acc="snowflake"),
     "a crown of standing ice, crystal horns, an icicle beard, a crystal staff, a snowflake behind"),
    ("furcas", "Furcas", "AAB7C8", "FFFFFF", "fair", ("F0F4F8", "7A88A0", "C8D4E4", "E8EEF8", "6A7488", "C8E4FF"),
     dict(eyes="calm", mouth="flat", beard="F0F4F8", chest="plate", pauldron="big", cover="helm", hair="none", head=["sallet"], ears="none", cape="icecloak", weapon="halberd", tail="ice", acc="shield"),
     "an open sallet with a frozen plume, a long white beard, a torn knight's cloak with icicles, a halberd, a kite shield"),
    ("balam", "Balam", "D1622A", "FFE0A0", "tan", ("5A2A14", "A04818", "E08A40", "FFE0A0", "5A3A20", "FFE0A0"),
     dict(eyes="round", mouth="smile", chest="robe", pauldron="round", hair="none", head=["three_faces"], ears="none", cape="scarf", weapon="flame_trident", tail="flame", acc="orbit_embers"),
     "three small faces on the brow, lyre horns, ram curls, a flame crown, a flaming trident"),
    ("alloces", "Alloces", "E8892B", "FFF0B0", "tan", ("E0A030", "B05A14", "F0B040", "FFF0B0", "6A4A20", "FFD040"),
     dict(eyes="slit", mouth="fang", mark="whisker", chest="plate", pauldron="big", hair="mane", head=["lion_brow"], ears="round", cape="scarf", weapon="lion_sword", tail="flame", acc="sun_disc"),
     "a brow plate with two lion's eyes, a mane of locks, round ears, the sun of a mane behind, a lion-guard longsword"),
    ("camio", "Camio", "2A3340", "A0C8FF", "ash", ("10141C", "1A222E", "3A4658", "A0C8FF", "10141C", "A0C8FF"),
     dict(eyes="round", mouth="flat", chest="vest", pauldron="round", cover="hood", hair="none", head=["beak_hood"], ears="none", cape="feather", weapon="quill_staff", tail="feather", acc="crows"),
     "a crow's beak hood, a ruff of black feathers, a feather cloak, a quill staff with a crow, three crows circling"),
    ("murmur", "Murmur", "6B3FA0", "E8C8FF", "ash", ("E8C8FF", "4A2878", "A070D8", "E8C8FF", "3A2A50", "E8C8FF"),
     dict(eyes="round", mouth="flat", chest="harness", pauldron="spiked", hair="dreads", head=["skull_mask"], ears="none", cape="plates", weapon="drum_mace", tail="wind", acc="drum"),
     "a skull mask with a drumstick standing from each temple, dreadlocks with bones, a rib cape, the great bass drum, a drum mace"),
]


def main():
    out = []
    for (i, name, col, glow, skin, p, look, note) in ROWS:
        hair, cloth, cloth2, trim, metal, eye = p
        d = {"id": i, "name": name, "color": col, "glow": glow, "skin": skin,
             "pal": {"hair": hair, "cloth": cloth, "cloth2": cloth2, "trim": trim, "metal": metal, "accent": col, "glow": glow, "eye": eye}}
        d.update(look)
        d["note"] = note
        out.append(d)
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "specs.json")
    with open(path, "w") as f:
        json.dump({"version": 1, "djinn": out}, f, indent=1)
    print(len(out), "specs")


if __name__ == "__main__":
    main()
