"""The particle registry of the custom library: one decorated drawing function per particle id, carrying the catalog data
(family, frames, texture size, behaviour text and the physics profile the game reads from data/nusmp/particle_catalog.json).

Profile keys (units are the game's): gravity (+falls, -rises; yd -= 0.04 * gravity per tick), drag (velocity multiplier per tick), spin (roll, rad/tick),
swirl (rad/tick the horizontal velocity turns), sway (sinusoidal horizontal drift, blocks/tick), jitter (random velocity noise per tick),
size [start, end] (full width in blocks), curve (linear | pop | ease_out | pulse | hold), fade [in, out] (fractions of the life),
life [min, max] ticks, light (normal | full), blend (translucent | add), anim (age | loop | random) with loop = ticks per frame, collide.
"""
import zlib

import numpy as np

SPECS = {}
ORDER = []

FAMILY_DEFAULTS = {
    "fire": dict(gravity=-0.10, drag=0.95, spin=0.0, swirl=0.0, sway=0.0, jitter=0.004, size=[0.20, 0.06], curve="linear", fade=[0.05, 0.45], life=[14, 26], light="full", blend="add", anim="loop", loop=2, collide=False),
    "glow": dict(gravity=-0.02, drag=0.96, spin=0.0, swirl=0.0, sway=0.0, jitter=0.002, size=[0.20, 0.10], curve="linear", fade=[0.12, 0.45], life=[16, 30], light="full", blend="add", anim="loop", loop=3, collide=False),
    "pulse": dict(gravity=0.0, drag=0.9, spin=0.0, swirl=0.0, sway=0.0, jitter=0.0, size=[0.5, 1.6], curve="ease_out", fade=[0.0, 0.6], life=[10, 16], light="full", blend="add", anim="age", loop=1, collide=False),
    "rune": dict(gravity=-0.01, drag=0.9, spin=0.0, swirl=0.0, sway=0.0, jitter=0.0, size=[0.30, 0.34], curve="pop", fade=[0.18, 0.40], life=[24, 36], light="full", blend="add", anim="age", loop=1, collide=False),
    "shard": dict(gravity=0.35, drag=0.97, spin=0.22, swirl=0.0, sway=0.0, jitter=0.0, size=[0.18, 0.12], curve="linear", fade=[0.05, 0.35], life=[22, 38], light="normal", blend="translucent", anim="loop", loop=2, collide=True),
    "feather": dict(gravity=0.05, drag=0.93, spin=0.0, swirl=0.0, sway=0.012, jitter=0.002, size=[0.30, 0.26], curve="hold", fade=[0.08, 0.35], life=[36, 60], light="normal", blend="translucent", anim="loop", loop=3, collide=False),
    "flora": dict(gravity=0.06, drag=0.94, spin=0.05, swirl=0.0, sway=0.014, jitter=0.002, size=[0.22, 0.20], curve="hold", fade=[0.08, 0.30], life=[34, 56], light="normal", blend="translucent", anim="loop", loop=3, collide=False),
    "water": dict(gravity=0.20, drag=0.96, spin=0.0, swirl=0.0, sway=0.0, jitter=0.002, size=[0.20, 0.16], curve="linear", fade=[0.05, 0.30], life=[20, 36], light="normal", blend="translucent", anim="age", loop=1, collide=False),
    "earth": dict(gravity=0.55, drag=0.95, spin=0.2, swirl=0.0, sway=0.0, jitter=0.0, size=[0.16, 0.12], curve="linear", fade=[0.0, 0.25], life=[18, 30], light="normal", blend="translucent", anim="loop", loop=2, collide=True),
    "tool": dict(gravity=0.10, drag=0.94, spin=0.1, swirl=0.0, sway=0.006, jitter=0.0, size=[0.24, 0.20], curve="hold", fade=[0.08, 0.30], life=[26, 44], light="normal", blend="translucent", anim="loop", loop=3, collide=False),
    "combat": dict(gravity=0.0, drag=0.92, spin=0.0, swirl=0.0, sway=0.0, jitter=0.0, size=[0.6, 1.2], curve="ease_out", fade=[0.0, 0.5], life=[8, 14], light="full", blend="add", anim="age", loop=1, collide=False),
}


def particle(pid, family, frames, px=16, tint=False, behaviour="", group=None, **profile):
    prof = dict(FAMILY_DEFAULTS[family])
    prof.update(profile)

    def deco(fn):
        assert pid not in SPECS, pid
        SPECS[pid] = dict(id=pid, family=family, group=group or family, frames=frames, px=px, tint=tint, behaviour=behaviour, profile=prof, fn=fn)
        ORDER.append(pid)
        return fn
    return deco


def rng_for(pid, frame):
    return np.random.default_rng(zlib.crc32(pid.encode()) * 31 + frame)
