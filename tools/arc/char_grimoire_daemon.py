"""Why: the lesser Grimoire Daemon, the first tier of Zagred's daemons (entity nusmp:grimoire_daemon, tier LESSER: ash and earth, a swarmer in tattered robes with a single-leaf grimoire,
dies to any hit). One of four kit characters that replace the old cube model (see tools/remodel/daemons.py for the shared design and docs/arc/remodel.md); this file is the
smallest, ash-grey one with an amber glow.
"""
from remodel import daemons

ENTITIES = ["grimoire_daemon"]
SPEC = daemons.spec("lesser", "grimoire_daemon")
