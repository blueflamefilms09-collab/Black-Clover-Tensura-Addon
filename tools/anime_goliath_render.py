"""Runs tools/render_preview/preview.py with the game's FlatLit light for geo models: the painted cel shading is the only shading (vanilla's two-light diffuse term is 1 on every face,
the block / sky lightmap still applies), so a preview shows what the game shows once the renderer wraps the consumer in FlatLit (client/prop/FlatLit.java).

    python3 -B tools/anime_goliath_render.py [preview.py arguments]       (set RENDER_PREVIEW_BUILD to a private folder when several agents render at once)

Nothing in tools/render_preview is edited: this file patches the module's `shade` function in memory.
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "render_preview"))
import numpy as np  # noqa: E402
import preview  # noqa: E402


def flat_shade(rt, V):
    rgb = V[..., 5:8] / 255.0
    a = V[..., 8] / 255.0
    if rt["lit"]:
        bl = np.clip(np.round(V[..., 9]), 0, 15).astype(np.int32)
        sk = np.clip(np.round(V[..., 10]), 0, 15).astype(np.int32)
        rgb = rgb * preview.LIGHTMAP[bl, sk]
    return rgb.astype(np.float64), a.astype(np.float64)


preview.shade = flat_shade

if __name__ == "__main__":
    sys.exit(preview.main(sys.argv[1:]))
