"""Animation clips of the anime Goliath (placeholder: idle only)."""
import math
SIN, COS, TAU = math.sin, math.cos, 2 * math.pi


def all_clips(a):
    a.clip("idle", 4.0, True)
