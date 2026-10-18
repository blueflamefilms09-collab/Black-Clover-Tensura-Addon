"""Why: the arch Grimoire Daemon (entity nusmp:grimoire_daemon, tier ARCH: spatial and forbidden, a multi-winged commander with a five-leaf grimoire that teleports and opens gravity wells and is
Zagred's anchor). Violet robe with gold trim, ram horns, a halo, and TWO pairs of membrane wings (the second pair made in `extra`, tools/remodel/daemons.py), glowing violet.
See docs/arc/remodel.md.
"""
from remodel import daemons

ENTITIES = []
SPEC = daemons.spec("arch", "grimoire_daemon_arch")


def extra(kit, rig):
    daemons.second_wings(kit, rig)
