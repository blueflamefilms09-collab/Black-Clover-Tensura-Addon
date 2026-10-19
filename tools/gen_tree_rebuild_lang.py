"""Writes the lang fragments of the nature-magic rebuild: tools/lang/<key>_rebuild.json for tree, vine, plant, poison_plant, mud and mucus (flat {"key": "text"} objects, English, plain ASCII quotes,
no newlines). They hold the new description of every page of the rebuilt books (the five generic pages keep their ids: their texts here replace the generic ones of en_us.json when the fragments
are merged; the appended pages are new) and the new description of each book. The names of the pages are literal strings in the code (book/TreeArts.java ...), so no key is needed for them.

    python3 -B tools/gen_tree_rebuild_lang.py
"""
import json
import os
import sys

sys.dont_write_bytecode = True
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "lang")

BOOKS = {
    "tree": ("Tree Magic",
             "A grimoire of heartwood and living trunks. Its pages throw splinters, burst roots out of the ground, raise groves and a great tree, grow a bark armour over you, call a treant, and wall you in with oak. Summon your grimoire, select a page, and press the skill key to cast.",
             {
                 "attribute_bolt": "Tree Magic: Splinter Shot - a sharpened shaft of wood that pierces everything on its line and slows it.",
                 "attribute_cut": "Tree Magic: Root Lance - lances of root burst out of the ground in a line in front of you, one after another: each cuts, throws up and roots whoever stands on it.",
                 "attribute_field": "Tree Magic: Grove - a grove grows at the aimed point for 8 seconds: foes in it are cut and held by roots, friends in it mend. Size scales with your EP.",
                 "attribute_domain": "Tree Magic: Great Tree Rising - a great trunk rises under everything round the aimed point: heavy damage, thrown up, then held by its roots for a while.",
                 "attribute_guard": "Tree Magic: Bark Armor - the Heartwood armour grows over you for 10 seconds (it forges in piece by piece and shatters off at the end): blows are softened and whoever strikes you is pricked and rooted.",
                 "heartwood_guardian": "Tree Magic: Heartwood Guardian - a treant of heartwood rises beside you for 20 seconds, follows you and slams the nearest foe. It can be hit.",
                 "seedling_volley": "Tree Magic: Seedling Volley - five seeds that sprout into rooting thorns where they land.",
                 "timber_wall": "Tree Magic: Timber Wall - a wall of oak trunks rises in front of you; foes in front of it are thrown back.",
                 "verdant_canopy": "Tree Magic: Verdant Canopy - a canopy spreads over you for 10 seconds: friends beneath it mend and regenerate, foes are slowed.",
                 "heartwood_fist": "Tree Magic: Heartwood Charge - you charge forward with a fist of heartwood, hitting everything on the way and throwing it back."}),
    "vine": ("Vine Magic",
             "A grimoire of living vines, thorns and tendrils. Its pages fling thorns, crack a vine whip, tangle a thicket, whirl every vine in reach inward, grow a thorned armour over you, and call a serpent and walls of vine. Summon your grimoire, select a page, and press the skill key to cast.",
             {
                 "attribute_bolt": "Vine Magic: Thorn Dart - a thorn flung on a vine: it pierces everything on its line and pricks with poison.",
                 "attribute_cut": "Vine Magic: Vine Whip - a whip of vine along a line: it cuts everything on it and hauls it toward you.",
                 "attribute_field": "Vine Magic: Thicket - a thicket at the aimed point for 8 seconds: foes are tangled, pricked and drained of a little magicule. Size scales with your EP.",
                 "attribute_domain": "Vine Magic: Verdant Maelstrom - every vine round the aimed point whirls inward: foes are dragged to the middle, cut and held.",
                 "attribute_guard": "Vine Magic: Thorn Mantle - the Thornbine armour grows over you for 10 seconds, vines climbing the body piece by piece: blows are softened and whoever strikes you is rooted and pricked.",
                 "vine_serpent": "Vine Magic: Vine Serpent - a serpent of vine hunts the nearest foe for 15 seconds, biting, rooting and poisoning. It can be hit.",
                 "vine_wall": "Vine Magic: Thicket Wall - a wall of living vines rises in front of you; thorns tear whoever leans on it.",
                 "vine_swing": "Vine Magic: Vine Swing - a vine thrown at the aimed point hauls you there; foes at the landing are caught.",
                 "strangling_bloom": "Vine Magic: Strangling Bloom - vines bind one foe (hard control, shares the 12 second control lock): they tighten and feed on its magicules.",
                 "lash_barrage": "Vine Magic: Lash Barrage - for 3 seconds whips of vine lash at up to three foes at a time."}),
    "plant": ("Plant Magic",
              "A grimoire that blooms when nobody is looking. It guides healing like a flower guidepost, opens a dream world of a hundred flowers where friends mend and foes stumble, and calls blooms, a flower sprite, an ivy wall, a petal storm and a blossom armour. Summon your grimoire, select a page, and press the skill key to cast.",
              {
                  "guidepost": "Flowers bloom along a path from you to the one you look at (or to yourself) and their light mends what it touches: 20 percent of the maximum health.",
                  "hundred_flowers": "A dream world of a hundred flowers opens round you for 8 seconds: friends rest in it and mend; enemies are tangled and cut by thorns.",
                  "seed_bloom": "Plant Magic: Seed Bloom - a seed that blossoms where it lands: foes are hurt and slowed, friends in the blast heal.",
                  "healing_bloom": "Plant Magic: Healing Bloom - a great bloom grows at the aimed point and heals everyone friendly near it for about 8 seconds, slowing foes.",
                  "flower_sprite": "Plant Magic: Flower Sprite - a flower sprite circles you for 30 seconds: it mends the most hurt friend and spits petals at foes. It can be hit.",
                  "ivy_barrier": "Plant Magic: Ivy Barrier - a wall of flowering leaves that dissolves everything shot at it.",
                  "blossom_dress": "Plant Magic: Blossom Dress - the Blossom armour drifts onto you for 10 seconds, petal by petal: blows are softened, each one mends you a little, and you regenerate.",
                  "petal_storm": "Plant Magic: Petal Storm - for 8 seconds a storm of petals circles you: foes are cut and slowed, friends mend."}),
    "poison_plant": ("Poison Plant Magic",
                     "A grimoire of toxic plants. Its vines skewer and tunnel through the ground, its blooms breathe clouds of poison gas, its traps snap shut, and its antidote undoes it all. Summon your grimoire, select a page, and press the skill key to cast.",
                     {
                         "attribute_bolt": "Poison Plant Magic: Venom Thorn - a hooked thorn that pierces and poisons.",
                         "attribute_cut": "Poison Plant Magic: Burrowing Venom Vine - a vine tunnels along the ground and erupts under everything on a line: damage, thrown up, poisoned.",
                         "attribute_field": "Poison Plant Magic: Miasma Bloom - a bloom opens at the aimed point and breathes a cloud of poison gas for 9 seconds: damage, poison, blindness.",
                         "attribute_domain": "Poison Plant Magic: Toxic Garden - three blooms and two trap maws spring up round you with a heavy poison zone for 10 seconds.",
                         "attribute_guard": "Poison Plant Magic: Venomflora Armor - the Venomflora armour lunges onto you for 10 seconds: blows are softened and whoever strikes you is poisoned.",
                         "trap_maw": "Poison Plant Magic: Trap Maw - a carnivorous trap that snaps on the first foe to come near, again every 2.5 seconds, for 12 seconds: damage, strong poison, rooted.",
                         "spore_burst": "Poison Plant Magic: Spore Burst - a cone of spores: foes are blinded, made sick and poisoned.",
                         "antivenom_graft": "Poison Plant Magic: Antivenom Graft - you graft an antidote on yourself and friends near you: harmful effects are cleansed and a little health returns.",
                         "pitcher_swallow": "Poison Plant Magic: Pitcher Swallow - a pitcher plant swallows one foe (hard control, shares the 12 second control lock): it is held, digested and drained.",
                         "thorn_volley": "Poison Plant Magic: Toxic Thorn Volley - five homing venom thorns."}),
    "mud": ("Mud Magic",
            "A grimoire of mud. Its pages throw heavy globs, send a wave of mud, turn the ground to a pit, burst a bog open, wrap you in a mud armour, and call a golem, spouts and walls. Summon your grimoire, select a page, and press the skill key to cast.",
            {
                "attribute_bolt": "Mud Magic: Mud Ball - a heavy glob of mud that bursts on the first foe and slows it.",
                "attribute_cut": "Mud Magic: Mud Wave - a wave of mud along a line: it cuts everything on it, throws it back and slows it.",
                "attribute_field": "Mud Magic: Mud Pit - the ground at the aimed point turns to a pit for 8 seconds: foes sink in it, are slowed hard and weakened. Size scales with your EP.",
                "attribute_domain": "Mud Magic: Bog Domain - the ground round the aimed point erupts: heavy damage, thrown back, then mired for a long time.",
                "attribute_guard": "Mud Magic: Mud Armor - the Bogplate armour drops onto you for 10 seconds, clod by clod: blows are softened by a third and whoever strikes you is slowed.",
                "mud_golem": "Mud Magic: Mud Golem - a golem of mud rises beside you for 20 seconds, follows you and pounds the nearest foe. It can be hit.",
                "mud_spout": "Mud Magic: Mud Spout - spouts of mud burst under up to four foes near the aimed point: damage and thrown up.",
                "mud_wall": "Mud Magic: Mud Wall - a wall of packed mud rises in front of you; foes in front of it are thrown back.",
                "mud_shackle": "Mud Magic: Mud Shackle - mud closes round one foe's legs (hard control, shares the 12 second control lock).",
                "mud_barrage": "Mud Magic: Mud Barrage - for 3 seconds globs of mud rain on the aimed point."}),
    "mucus": ("Mucus Magic",
              "A grimoire of mucus that smothers and entraps, soaks up blows and drinks the magic of whoever it holds. Its pages shoot blobs, lash tendrils, spread sticky fields, engulf everything near you, wrap you in a gel armour, and call a construct, walls and bubbles. Summon your grimoire, select a page, and press the skill key to cast.",
              {
                  "attribute_bolt": "Mucus Magic: Mucus Shot - a blob of mucus that bursts on the first foe, slows it and takes a little of its magicule.",
                  "attribute_cut": "Mucus Magic: Mucus Tendril - a tendril lashes along a line: it cuts, slows and drags everything on it toward you.",
                  "attribute_field": "Mucus Magic: Sticky Field - a field of mucus at the aimed point for 8 seconds: foes in it are slowed hard and their magicules are absorbed. Size scales with your EP.",
                  "attribute_domain": "Mucus Magic: Engulfing Tide - a tide of mucus engulfs everything round you: damage, slowed, silenced and drained.",
                  "attribute_guard": "Mucus Magic: Mucus Armor - the Gelplate armour spreads over you for 10 seconds like slime: blows are soaked (a third less), feed you a little, and drain the magicules of whoever strikes you.",
                  "mucus_construct": "Mucus Magic: Mucus Construct - a mound of mucus beside you for 15 seconds swallows every projectile near it and engulfs foes. It can be hit.",
                  "smother": "Mucus Magic: Smother - mucus closes over one foe (hard control, shares the 12 second control lock): slowed, silenced, blinded and drained.",
                  "absorption_wall": "Mucus Magic: Absorption Wall - a wall of mucus that dissolves what is shot at it and drains the magicules of foes beyond it.",
                  "sticky_trail": "Mucus Magic: Sticky Trail - you leave a trail of mucus for 6 seconds that slows whoever steps in it.",
                  "mucus_burst": "Mucus Magic: Bubble Burst - bubbles of mucus drift to the aimed point and burst one after another: damage and slowed."}),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    for key, (name, desc, pages) in BOOKS.items():
        frag = {"nusmp.skill.book_%s.description" % key: desc}
        for pid, text in pages.items():
            frag["nusmp.page.book_%s.%s" % (key, pid)] = text
        path = os.path.join(OUT, key + "_rebuild.json")
        with open(path, "w") as f:
            json.dump(frag, f, indent=2, ensure_ascii=True)
            f.write("\n")
        print("wrote %-34s %2d keys" % ("tools/lang/" + key + "_rebuild.json", len(frag)))


if __name__ == "__main__":
    main()
