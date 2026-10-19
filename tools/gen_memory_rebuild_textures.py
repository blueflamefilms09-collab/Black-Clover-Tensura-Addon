"""Memory Magic (1.93.0 rebuild) particle textures: a film strip, a photo frame, a film reel, a glass shard, a ripple of film frames and a four-point memory spark.

    python3 -B tools/gen_memory_rebuild_textures.py       # writes textures/particle/memory_*.png (white + alpha, tinted by the layers)

memory_film_v   32x256  a film strip running along it (VfxVertexBuffer.beam: v runs along): two rows of sprocket holes and a column of frames
memory_frame    128     a photo frame with a thin picture mat and a corner flare
memory_reel     256     a film reel seen from the front: rim, six round holes, a hub, with a loose strip curling out
memory_shard    64      a long glass shard with a bright edge line
memory_ripple   128     a ring of small film frames between two thin circles (the ground sigil)
memory_spark    64      a four-point spark with a soft core (remembered light)
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_texkit import Canvas, save_particle   # noqa: E402


def film_v():
    c = Canvas(32, 256, seed=41)
    c.poly([(0.05, 0.0), (0.95, 0.0), (0.95, 1.0), (0.05, 1.0)], 150)
    for k in range(16):
        y = (k + 0.5) / 16.0
        c.poly([(0.1, y - 0.016), (0.2, y - 0.016), (0.2, y + 0.016), (0.1, y + 0.016)], 255)
        c.poly([(0.8, y - 0.016), (0.9, y - 0.016), (0.9, y + 0.016), (0.8, y + 0.016)], 255)
    for k in range(7):
        y0 = 0.03 + k * 0.137
        c.line([(0.3, y0), (0.7, y0), (0.7, y0 + 0.11), (0.3, y0 + 0.11), (0.3, y0)], 0.012, 255, round_ends=False)
    save_particle("memory_film_v", c.finish(blur=0.4, halo=0.4, halo_r=1.5))


def frame():
    c = Canvas(128, seed=42)
    c.glow(0.5, 0.5, 0.5, 40)
    for pts in (((0.1, 0.1), (0.9, 0.1)), ((0.1, 0.9), (0.9, 0.9)), ((0.1, 0.1), (0.1, 0.9)), ((0.9, 0.1), (0.9, 0.9))):
        c.line(list(pts), 0.025, 255, round_ends=False)
    for pts in (((0.2, 0.2), (0.8, 0.2)), ((0.2, 0.8), (0.8, 0.8)), ((0.2, 0.2), (0.2, 0.8)), ((0.8, 0.2), (0.8, 0.8))):
        c.line(list(pts), 0.008, 200, round_ends=False)
    c.star(0.12, 0.12, 0.09, 4, 0.2, 255)
    save_particle("memory_frame", c.finish(blur=0.5, halo=0.4, halo_r=1.8))


def reel():
    c = Canvas(256, seed=43)
    c.ring(0.5, 0.5, 0.46, 0.012, 255)
    c.ring(0.5, 0.5, 0.40, 0.006, 200)
    for k in range(6):
        t = k * math.pi / 3 + math.pi / 6
        c.ring(0.5 + 0.22 * math.cos(t), 0.5 + 0.22 * math.sin(t), 0.075, 0.012, 255)
        c.line([(0.5 + 0.1 * math.cos(t), 0.5 + 0.1 * math.sin(t)), (0.5 + 0.145 * math.cos(t), 0.5 + 0.145 * math.sin(t))], 0.008, 220)
    c.ring(0.5, 0.5, 0.08, 0.012, 255)
    c.disc(0.5, 0.5, 0.035, 255)
    for k in range(12):                                                                 # a loose strip curling out of the rim
        t = k / 11.0
        x, y = 0.5 + 0.46 + 0.1 * t, 0.5 + 0.05 + 0.4 * t * t
        c.disc(x, y, 0.014, 200)
    c.glow(0.5, 0.5, 0.35, 40)
    save_particle("memory_reel", c.finish(blur=0.5, halo=0.4, halo_r=2.0))


def shard():
    c = Canvas(64, seed=44)
    c.poly([(0.5, 0.0), (0.7, 0.35), (0.6, 1.0), (0.42, 0.9), (0.32, 0.4)], 220)
    c.line([(0.5, 0.05), (0.6, 0.95)], 0.012, 255, round_ends=False)
    c.line([(0.5, 0.05), (0.33, 0.4)], 0.01, 255, round_ends=False)
    c.glow(0.5, 0.5, 0.3, 50)
    save_particle("memory_shard", c.finish(blur=0.4, halo=0.4, halo_r=1.6))


def ripple():
    c = Canvas(128, seed=45)
    c.ring(0.5, 0.5, 0.47, 0.008, 255)
    c.ring(0.5, 0.5, 0.33, 0.006, 220)
    for k in range(16):
        t = k * math.pi / 8
        r = 0.4
        cx, cy = 0.5 + r * math.cos(t), 0.5 + r * math.sin(t)
        dx, dy = math.cos(t) * 0.04, math.sin(t) * 0.04
        px, py = -math.sin(t) * 0.032, math.cos(t) * 0.032
        c.line([(cx - dx - px, cy - dy - py), (cx + dx - px, cy + dy - py), (cx + dx + px, cy + dy + py), (cx - dx + px, cy - dy + py), (cx - dx - px, cy - dy - py)], 0.006, 255, round_ends=False)
    c.glow(0.5, 0.5, 0.3, 40)
    save_particle("memory_ripple", c.finish(blur=0.5, halo=0.35, halo_r=1.8))


def spark():
    c = Canvas(64, seed=46)
    c.glow(0.5, 0.5, 0.3, 200)
    c.star(0.5, 0.5, 0.46, 4, 0.1, 255)
    c.star(0.5, 0.5, 0.26, 4, 0.2, 255, rot=math.pi / 4)
    save_particle("memory_spark", c.finish(blur=0.4, halo=0.4, halo_r=1.6))


if __name__ == "__main__":
    film_v()
    frame()
    reel()
    shard()
    ripple()
    spark()
