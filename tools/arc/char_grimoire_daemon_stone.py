"""Why: the Overwrite rule stone (entity nusmp:grimoire_daemon, tier STONE): one of the five glyph slabs of Zagred's "Overwrite"; destroying three cuts the sentence. It is not a
creature, so its character is the nearest thing the art kit can stand on the player's rig: a small stone idol with its hands pressed together, its face a blank of cracked rock, in front of
a tall carved tablet with a glowing glyph (made in `extra`, tools/remodel/daemons.py). It does not hover or move. See docs/arc/remodel.md.
"""
from remodel import daemons

ENTITIES = []
SPEC = daemons.spec("stone", "grimoire_daemon_stone")


def extra(kit, rig):
    daemons.stone_slab(kit, rig)
