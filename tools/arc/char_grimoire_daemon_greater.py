"""Why: the greater Grimoire Daemon (entity nusmp:grimoire_daemon, tier GREATER: dark and fire, artillery with horns and a three-leaf grimoire emitting glowing text, a blow to its back
takes double). Crimson robe, devil horns, an ember-orange glow; the shared design is in tools/remodel/daemons.py (docs/arc/remodel.md).
"""
from remodel import daemons

ENTITIES = []          # drawn by the grimoire_daemon entity through its tier (see client/remodel/RemodelTable), so no entity id of its own
SPEC = daemons.spec("greater", "grimoire_daemon_greater")
