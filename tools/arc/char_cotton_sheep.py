"""Why: Charmy's cotton sheep (entity nusmp:cotton_sheep, entity/CottonSheepEntity), summoned by Cotton Magic in three jobs (striker, cook, binder), drawn like the anime's: tall, upright,
fluffy, with a pale long face, round pale eyes and curled horns; the cook wears a chef's hat and a blue neckerchief (that one is char_cotton_sheep_cook.py). The old model was a stack of wool
tufts (tools/gen_cotton_textures.py). This is the art kit's version on the player's rig: a tall, widened body in cream wool (fur-textured tunic, sleeves and shorts, a mane of wool for
hair, a wool ruff at the shoulders), a pale long face with round pale eyes, drooping ears and curled ram horns, hooves for feet and a tuft of a tail. Attack is the punch (the leap and the
hug are the renderer's poses). The colours are the old model's. See docs/arc/remodel.md; the design is shared with the cook through tools/remodel/sheep.py.
"""
from remodel import sheep

ENTITIES = ["cotton_sheep"]
SPEC = sheep.spec(False)
