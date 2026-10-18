"""Why: the cook job of Charmy's cotton sheep (entity nusmp:cotton_sheep, role COOK: it stands on the kitchen cloud, flips pans and ladles and steams): the same wool sheep with a chef's hat, a
blue neckerchief and a frying pan in its hand. The kit builds one body per file, so the cook is its own character, made from the same spec function as the plain sheep's
(tools/remodel/sheep.py); the renderer picks it by the entity's role. See docs/arc/remodel.md.
"""
from remodel import sheep

ENTITIES = []          # drawn by the cotton_sheep entity through its role (client/remodel/RemodelTable)
SPEC = sheep.spec(True)
