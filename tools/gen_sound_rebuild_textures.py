"""Sound Magic (1.93.0 rebuild) particle textures: sound-wave arcs, a speaker cone, a jagged shock ring, a waveform beam, equalizer bars and a ripple ring.

    python3 -B tools/gen_sound_rebuild_textures.py       # writes textures/particle/sound_*.png (white + alpha, tinted by the layers)

sound_wave    128     three broken, thickening arcs round a bright core (the sign of a sound)
sound_cone    128     a speaker cone seen from the front: surround, concentric ribs, a bright dust cap
sound_shock   128     a ring with sharp spikes pointing outward (a sonic boom front)
sound_beam_v  32x256  a waveform zigzag running along it with a bright centre line (VfxVertexBuffer.beam: v runs along)
sound_bars    128     eight equalizer bars of different height (VU / spectrum)
sound_ripple  128     a clean double ring with tick marks
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from song_rebuild_texkit import Canvas, save_particle   # noqa: E402


def wave():
    c = Canvas(128, seed=11)
    c.glow(0.5, 0.5, 0.18, 255)
    for k, r in enumerate((0.15, 0.27, 0.40)):
        for side in (0, 1):
            a0 = side * math.pi - 0.75
            c.arc(0.5, 0.5, r, 0.016 + 0.006 * k, a0, a0 + 1.5, 255 - k * 25)
    save_particle("sound_wave", c.finish(blur=0.5, halo=0.45, halo_r=2.0))


def cone():
    c = Canvas(128, seed=12)
    c.disc(0.5, 0.5, 0.48, 120)
    c.ring(0.5, 0.5, 0.46, 0.02, 255)
    for k in range(6):
        c.ring(0.5, 0.5, 0.40 - k * 0.055, 0.008, 210)
    c.disc(0.5, 0.5, 0.09, 255)
    c.glow(0.5, 0.5, 0.2, 200)
    save_particle("sound_cone", c.finish(blur=0.5, halo=0.3, halo_r=1.6))


def shock():
    c = Canvas(128, seed=13)
    c.ring(0.5, 0.5, 0.36, 0.022, 255)
    n = 20
    for k in range(n):
        t = k * 2 * math.pi / n
        r0, r1 = 0.36, 0.47 if k % 2 == 0 else 0.42
        c.line([(0.5 + r0 * math.cos(t), 0.5 + r0 * math.sin(t)), (0.5 + r1 * math.cos(t), 0.5 + r1 * math.sin(t))], 0.008 if k % 2 else 0.013, 255)
    c.ring(0.5, 0.5, 0.28, 0.008, 150)
    save_particle("sound_shock", c.finish(blur=0.5, halo=0.4, halo_r=2.0))


def beam_v():
    c = Canvas(32, 256, seed=14)
    pts = []
    for k in range(40):
        y = k / 39.0
        amp = 0.34 * math.sin(math.pi * y) ** 0.6
        pts.append((0.5 + amp * math.sin(y * 9 * math.pi) * (1 if k % 2 == 0 else 0.55), y))
    c.line(pts, 0.05, 255)
    c.line([(0.5, 0.0), (0.5, 1.0)], 0.03, 255)
    save_particle("sound_beam_v", c.finish(blur=0.6, halo=0.5, halo_r=2.2))


def bars():
    c = Canvas(128, seed=15)
    hs = (0.35, 0.62, 0.9, 0.55, 0.78, 0.42, 0.7, 0.3)
    for k, h in enumerate(hs):
        x0 = 0.1 + k * 0.1
        for seg in range(int(h * 14)):
            y = 0.9 - seg * 0.058
            c.poly([(x0, y), (x0 + 0.075, y), (x0 + 0.075, y - 0.044), (x0, y - 0.044)], 255 if seg < 11 else 215)
    save_particle("sound_bars", c.finish(blur=0.4, halo=0.35, halo_r=1.6))


def ripple():
    c = Canvas(128, seed=16)
    c.ring(0.5, 0.5, 0.45, 0.014, 255)
    c.ring(0.5, 0.5, 0.36, 0.007, 190)
    for k in range(32):
        t = k * math.pi / 16
        c.line([(0.5 + 0.45 * math.cos(t), 0.5 + 0.45 * math.sin(t)), (0.5 + 0.40 * math.cos(t), 0.5 + 0.40 * math.sin(t))], 0.004, 210)
    c.glow(0.5, 0.5, 0.4, 40)
    save_particle("sound_ripple", c.finish(blur=0.5, halo=0.35, halo_r=1.8))


if __name__ == "__main__":
    wave()
    cone()
    shock()
    beam_v()
    bars()
    ripple()
