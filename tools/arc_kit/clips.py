"""Why: the recipes of the clips. Every character gets idle, walk, run, attack, cast, cast_big, hurt, transform and death (and spawn / chest_gem_reveal when asked), made
from the same recipes so the 40 characters move alike and nothing has to be keyed by hand; the weapon decides how the arm swings (a sword slashes, a spear thrusts, a
staff casts, bare hands punch), the build decides the stride, and the cloth, hair, tails and wings follow the body with a lag. The timing follows the Zogratis clock
(client.zogratis.ZogratisPose): an attack lands about 0.38 s into its clip (the clock starts it 0.2 s in), a cast_big winds up to 80 percent of its length and slams
there, then settles.
"""
import math

import numpy as np

from . import props as Pr
from .anim import Clip, ArmIK, GRID, SIN, COS, TAU, lowpass, dir_from, side_dir
from .core import catmull
from .rig import rot_matrix

# ------------------------------------------------------------------------------------------------------------------------ how each prop is held at rest:
# (elevation, azimuth, reach, phi, tilt) for a RIGHT hand (a left-hand prop mirrors the tilt): where the holding wrist is (see ArmIK.target) and where the long axis of
# the prop points (see anim.dir_from; a positive tilt points it across the body, a negative one away from it)
HOLD = {
    "sword": (-50, 36, 0.84, 24, 4), "katana": (-54, 34, 0.82, -24, 18), "dagger": (-48, 34, 0.80, 40, 0), "greatsword": (-44, 22, 0.76, 72, 0),
    "spear": (-46, 20, 0.74, 82, 0), "trident": (-46, 20, 0.74, 82, 0), "staff": (-42, 26, 0.80, 86, 0), "wand": (-38, 28, 0.80, 34, 0),
    "axe": (-48, 34, 0.84, 58, 0), "hammer": (-48, 34, 0.84, 58, 0), "mace": (-48, 34, 0.84, 58, 0), "scythe": (-44, 22, 0.76, 78, 0),
    "bow": (-6, 24, 0.96, 90, 0), "grimoire": (-30, 18, 0.80, 80, 0), "orb": (-30, 24, 0.82, 80, 0), "shield": (-40, 34, 0.75, 90, 0),
}
HANG = (-77, 41, 0.99)          # the wrist of a relaxed hanging arm (see recipes.py)
GUARD = (-36, 30, 0.74)         # the wrist of the arm that carries a shield
COUPLED = ("idle", "walk", "run", "attack", "cast", "hurt")          # the clips in which both hands of a two-handed prop stay on the shaft (in the others the off hand moves freely)
ONE_SHOT = ("attack", "hurt", "transform", "death", "cast_big", "spawn", "chest_gem_reveal")
HOVER_PX = 5.0                  # how high a hovering character (clips.idle = "hover") floats, model pixels


def v3(*a):
    return np.array(a, float)


class Ctx:
    """What the recipes of one character need to know: the rig, the spec, which hand holds what, and the IK of both arms."""

    def __init__(self, R, S):
        self.R, self.S, self.d = R, S, R.d
        self.arm = {"l": ArmIK(R, 1), "r": ArmIK(R, -1)}
        props = R.facts.get("props", {})
        self.main_slot = "right" if "right" in props else ("left" if "left" in props else None)
        self.main = props.get(self.main_slot) if self.main_slot else None
        self.kind = S["props"][self.main_slot]["kind"] if self.main_slot else None
        self.holder = ("r" if self.main_slot == "right" else "l") if self.main_slot else None
        if self.kind == "shield":                                  # a shield is worn, not swung: the other hand may still hold a prop
            other = "left" if self.main_slot == "right" else "right"
            if other in props:
                self.main_slot, self.main = other, props[other]
                self.kind = S["props"][other]["kind"]
                self.holder = "r" if other == "right" else "l"
            else:
                self.main_slot = self.main = self.kind = self.holder = None
        self.shield_side = None
        for slot, pr in S["props"].items():
            if pr["kind"] == "shield" and slot in ("left", "right"):
                self.shield_side = "r" if slot == "right" else "l"
        self.two = bool(self.main and self.main.two and S["props"][self.main_slot].get("two_hand") is not False)        # `two_hand: false` carries a two-handed prop in one hand
        self.role = S["role"]
        att = S["clips"]["attack"]
        self.attack_style = att if att != "auto" else (self.main.attack if self.main else "punch")
        cs = S["clips"]["cast"]
        self.cast_style = cs if cs != "auto" else (self.main.cast if self.main and self.kind in ("grimoire", "staff", "wand", "orb") else ("gesture" if self.role == "npc" else "magic"))
        if self.kind == "grimoire" and cs == "auto":
            self.cast_style = "book"
        self.idle_style = S["clips"]["idle"] if S["clips"]["idle"] != "auto" else "relaxed"
        self.long = bool(getattr(R, "body", None) and R.body.long_robe)
        self.skirt = bool(getattr(R, "body", None) and R.body.skirt)
        self.hover = self.idle_style == "hover"
        self.last_q = {}
        self.ik_arms = set()
        self.fit = None                                   # the hold of a two-handed prop fitted to this body: (hold tuple, shift of the shaft)
        self.rest_key = None                              # the wrist key of the off hand at that hold
        self._last_holder_keys = {}                       # the wrist keys the holding arm followed in each clip
        # Both hands stay on a two-handed prop only when this body's arms can really reach the shaft with natural elbows (see _two_handed_fit); the short arms of the broad build
        # cannot, and a character that cannot carries the prop in one hand and lets the other arm hang, the way a guard carries a spear.
        self.coupled = bool(self.two and self.holder and (self.kind == "bow" or self._two_handed_fit()[2]))

    def has(self, bone):
        return self.R.has(bone)

    # ---- the arms
    def hold(self):
        """The rest pose of the main prop for the hand that holds it: HOLD is written for the right hand, the lateral tilt of a left-hand prop is mirrored."""
        if not self.kind:
            return None
        if self.coupled and self.kind != "bow":
            return self._two_handed_fit()[0]
        el, az, rc, ph, ti = HOLD[self.kind]
        return (el, az, rc, ph, ti if self.holder == "r" else -ti)

    def _two_handed_fit(self):
        """The rest hold of a two-handed prop, fitted to this body. The arms of a blocky body are short: with a shaft held upright in front of the chest the off hand cannot
        reach the grip. So the shaft is tilted across the body (the way a spear or a greatsword is carried at the ready) and shifted a little toward the middle until both
        hands can close on it; the tilt and shift that cost the least are used. Everything else (every clip's first and last pose, the swings' offsets) hangs on this one
        answer, so all the clips of a character agree on how the weapon is held."""
        if self.fit is not None:
            return self.fit
        el0, az0, rc0, ph0, ti0 = HOLD[self.kind]
        sgn = 1.0 if self.holder == "r" else -1.0
        A1 = self.arm[self.holder]
        A2 = self.arm["l" if self.holder == "r" else "r"]
        pd = self.main
        grip = tuple(self.S["props"][self.main_slot]["grip"] or pd.grip)
        best = None
        saved = (A1.rho_max, A2.rho_max)
        A1.rho_max, A2.rho_max = 0.9 * A1.U, 0.9 * A2.U                          # a rest hold keeps some slack: the arms are not at full stretch while the weapon is only carried
        try:
            for dti in (0.0, 15.0, 30.0, 45.0, 60.0, 75.0):
                for dph in (0.0, -10.0, -20.0, -30.0):
                    for drc in (0.0, 0.12, 0.24):
                        hold = (el0, az0, min(0.97, rc0 + drc), ph0 + dph, sgn * (ti0 + dti))
                        d1 = dir_from(hold[3], hold[4])
                        sides = None if pd.orient is None else [(pd.orient[0], side_dir(pd.orient[1], d1))]
                        shift = np.zeros(3)
                        for _ in range(6):
                            q1, m1 = A1.solve_track(np.array([A1.target(hold[0], hold[1], hold[2], shift)]))
                            h1 = A1.wrist_track(q1, [d1], grip, sides=sides)
                            q2, m2 = self._off_hand_solution(None, q1, h1)
                            if max(np.abs(m1).max(), np.abs(m2).max()) < 0.4:
                                break
                            shift = np.clip(shift - 0.5 * (m1[0] + m2[0]), -12.0, 12.0)
                        miss = float(np.linalg.norm(m1[0]) ** 2 + np.linalg.norm(m2[0]) ** 2)
                        sink = A1.depth(q1[0]) + A2.depth(q2[0])
                        ease = float(A1.last_costs[0] + A2.last_costs[0])           # a tie-breaker: the less the elbows have to leave their natural place, the better
                        cost = miss + 3.0 * sink + 0.05 * ease + 0.0006 * (dti * dti + dph * dph) + 4.0 * drc * drc + 0.04 * float(np.linalg.norm(shift))
                        if best is None or cost < best[0] - 1e-9:
                            best = (cost, hold, shift.copy(), miss <= 4.0 and sink <= 0.55)
        finally:
            A1.rho_max, A2.rho_max = saved
        ok = best[3]
        if not ok:                                                                 # the arms cannot reach: the usual one-handed carry, nothing shifted
            el0, az0, rc0, ph0, ti0 = HOLD[self.kind]
            self.fit = ((el0, az0, rc0, ph0, sgn * ti0), np.zeros(3), False)
        else:
            self.fit = (best[1], best[2], True)
            # pin the elbows of both arms at this hold, so that every clip that starts and ends there takes the same pose as the idle (a rest hold sits at the edge of what the
            # arms can reach, and two elbows that cost nearly the same would otherwise be picked at random)
            hold, shift = best[1], best[2]
            d1 = dir_from(hold[3], hold[4])
            sides = None if pd.orient is None else [(pd.orient[0], side_dir(pd.orient[1], d1))]
            T1 = A1.target(hold[0], hold[1], hold[2], shift)
            A1.pin(T1)
            q1, _ = A1.solve_track(np.array([T1]))
            h1 = A1.wrist_track(q1, [d1], grip, sides=sides)
            grip_pt = self._grip2_points(None, q1, h1)
            wrist = self._off_hand_wrists(A2, grip_pt)
            A2.pin(wrist[0])
            q2, _ = A2.solve_track(wrist)
            A2.pin(grip_pt[0] + A2.fk(q2[0])[2] @ np.array([0.0, self.d.hand / 2.0, 0.0]))        # where second_hand puts the wrist (see there)
        return self.fit

    def arm_track(self, c, side, keys, holder=False):
        """The arm of `side` follows the wrist keys [(t, elev, azim, reach, phi_or_None, tilt)] through IK (and the hand turns the prop to `phi` / `tilt` when it holds
        one). Writes arm / forearm / hand tracks (replacing anything set before). A two-handed prop is pulled toward the reach of the off hand: wherever the off hand could
        not get to the shaft, the whole shaft (and so the holding hand) is shifted toward it, so both hands stay on the weapon."""
        A = self.arm[side]
        n = c.n + 1
        ks = sorted(keys, key=lambda k: k[0])
        if holder:
            self._last_holder_keys[c.name] = ks
        elev, azim, reach, phi, tilt = _key_columns(c, ks)
        want_prop = holder and self.kind and any(k[4] is not None for k in ks)
        pd = self.main
        grip = tuple(self.S["props"][self.main_slot]["grip"] or pd.grip) if (want_prop and pd) else (0.0, 0.0, 0.0)
        pitch = c.rot["body"][:, 0] if c.has("body") else np.zeros(n)           # the weapon's angle is meant against the horizon, the body may lean
        dirs = sides = None
        if want_prop:
            dirs = [dir_from(phi[i] - pitch[i], tilt[i]) for i in range(n)]
            sides = None if self.main.orient is None else [(self.main.orient[0], side_dir(self.main.orient[1], dirs[i])) for i in range(n)]
        bias = np.zeros((n, 3))
        carried = bool(want_prop and self.coupled and self.holder == side and self.kind != "bow")
        pulled = carried and c.name in COUPLED
        if carried:
            hold, shift, _ = self._two_handed_fit()                                   # where the shaft rests, shifted so both hands can reach it
            # The shift of the rest hold is felt fully at the rest hold and fades out as the wrist leaves it (a swing that goes round the side of the body must not be carried
            # into the torso by a shift that only fits the front). Every clip starts and ends at the rest hold, so every clip starts and ends on the idle's pose.
            T_hold = A.target(hold[0], hold[1], hold[2])
            away = np.array([np.linalg.norm(A.target(elev[i], azim[i], reach[i]) - T_hold) for i in range(n)])
            weight = np.clip(away / (0.3 * A.U), 0.0, 1.0)[:, None]
            base = shift[None, :] * (1.0 - weight)
            bias = base.copy()
        if pulled:
            corr = np.zeros((n, 3))
        for attempt in range(5 if pulled else 1):
            qa, miss1 = A.solve_track(np.array([A.target(elev[i], azim[i], reach[i], bias[i]) for i in range(n)]))
            hand_r = A.wrist_track(qa, dirs, grip, sides=sides) if want_prop else np.zeros((n, 3))
            if not pulled:
                break
            _, miss2 = self._off_hand_solution(c, qa, hand_r)
            err = miss1 + miss2                                                       # what the two arms fall short by: the shaft moves toward where both can hold it
            if np.abs(err * weight).max() < 0.8:
                break
            corr = _smooth_rows(corr - 0.5 * err, c.loop) * weight
            corr[:, 2] = np.minimum(corr[:, 2], 0.0)                                    # the shaft may be brought forward, never back into the chest
            norm = np.linalg.norm(corr, axis=1)
            corr = corr * np.minimum(1.0, 7.0 / np.maximum(norm, 1e-9))[:, None]
            bias = base + corr
        c.put("arm_" + side, "rot", qa[:, :3])
        c.put("forearm_" + side, "rot", np.stack([qa[:, 3], np.zeros(n), np.zeros(n)], 1))
        if want_prop:
            c.put("hand_" + side, "rot", hand_r)
        self.last_q[(c.name, side)] = (qa, hand_r)
        self.ik_arms.add((c.name, side))

    def _grip2_points(self, c, q_hold, h_hold, extra=None):
        """Where the grip of the off hand is on the held prop (the prop's `grip2` point, in the body frame) at every sample, for the holder's joint angles and hand turns."""
        R, d, holder = self.R, self.d, self.holder
        pbone = "prop_" + holder
        G = np.array(R.m.bones[pbone]["pivot"], float)
        g2 = np.array(self.main.grip2, float) * max(0.6, min(2.4, d.top / 32.0))
        out = np.zeros((len(q_hold), 3))
        for i in range(len(q_hold)):
            gg = g2 if extra is None else extra(float(c.ts[i]))
            pose = {"arm_" + holder: {"rot": tuple(q_hold[i][:3])}, "forearm_" + holder: {"rot": (q_hold[i][3], 0, 0)}, "hand_" + holder: {"rot": tuple(h_hold[i])}}
            out[i] = R.pose_point(pbone, G + gg, pose)
        return out

    def _off_hand_wrists(self, A2, pts):
        """Where the off wrist has to be for its hand to close on the grip points `pts`: half a hand short of the grip along the arm (a first guess; second_hand refines it)."""
        v = pts - A2.Pa
        r = np.maximum(np.linalg.norm(v, axis=1), 1e-6)
        return pts - v / r[:, None] * np.minimum(r, self.d.hand * 0.5)[:, None]

    def _off_hand_wrists_exact(self, A2, pts):
        """Where the off wrist has to be for its hand to close on the grip points `pts`: the first guess (half a hand short of the grip along the arm), refined once with the
        direction the forearm really has when it reaches that guess."""
        q1, _ = A2.solve_track(self._off_hand_wrists(A2, pts))
        return np.array([pts[i] + A2.fk(q1[i])[2] @ np.array([0.0, self.d.hand / 2.0, 0.0]) for i in range(len(pts))])

    def _off_hand_solution(self, c, q_hold, h_hold):
        """The off arm solved onto the grip on the shaft (one pass: the wrist half a hand short of the grip along the arm): its joint angles and how far (a vector per
        sample) its wrist stops short of where the grip needs it."""
        A2 = self.arm["l" if self.holder == "r" else "r"]
        return A2.solve_track(self._off_hand_wrists(A2, self._grip2_points(c, q_hold, h_hold)))

    def release_off_hand(self, c, keys):
        """The off hand of a two-handed prop in a violent swing: it stays on the shaft while it can reach it and lets go when the swing takes the shaft out of its reach, to swing
        free along `keys` (the wrist keys of a free arm, as free_arm takes them) and close on the shaft again as it comes back. The hand never hovers beside a weapon it cannot
        hold: it is on it or it is clearly off it."""
        side = "l" if self.holder == "r" else "r"
        A2 = self.arm[side]
        n = c.n + 1
        hold_q, hold_h = self.last_q[(c.name, self.holder)]
        grip = self._off_hand_wrists_exact(A2, self._grip2_points(c, hold_q, hold_h))
        el, az, rc, _, _ = _key_columns(c, sorted(keys, key=lambda k: k[0]))
        free = np.array([A2.target(el[i], az[i], rc[i]) for i in range(n)])
        _, miss = A2.solve_track(grip)
        w = np.clip(1.0 - (np.linalg.norm(miss, axis=1) - 1.0) / 3.0, 0.0, 1.0)[:, None]
        w = np.clip(_smooth_rows(np.repeat(w, 3, axis=1), c.loop, 2)[:, :1], 0.0, 1.0)
        # on the shaft for sure at the rest hold (the clip starts and ends there), free only as the swing takes the shaft away
        hold = self._two_handed_fit()[0]
        A1 = self.arm[self.holder]
        T_hold = A1.target(hold[0], hold[1], hold[2])
        el1, az1, rc1, _, _ = _key_columns(c, [(k[0],) + tuple(k[1:]) for k in self._last_holder_keys.get(c.name, [])]) if self._last_holder_keys.get(c.name) else (None,) * 5
        if el1 is not None:
            away = np.array([np.linalg.norm(A1.target(el1[i], az1[i], rc1[i]) - T_hold) for i in range(n)])
            near = np.clip(1.0 - (away - 0.1 * A1.U) / (0.2 * A1.U), 0.0, 1.0)[:, None]
            w = np.maximum(w, near)
        q, _ = A2.solve_track(w * grip + (1.0 - w) * free)
        c.put("arm_" + side, "rot", q[:, :3])
        c.put("forearm_" + side, "rot", np.stack([q[:, 3], np.zeros(n), np.zeros(n)], 1))
        self.ik_arms.add((c.name, side))

    def string_targets(self, c, draw):
        """Where the wrist of the string hand of a bow is at every sample: `draw` runs 0 (at rest) to 1 (drawn). At rest the string hand hangs at its side (the bow hand is a
        whole body width away: reaching across for the string would drag the arm through the chest); drawn, it rests beside the cheek, in front of the chest."""
        side = "r" if self.holder == "l" else "l"
        A = self.arm[side]
        d = self.d
        hold_q, hold_h = self.last_q[(c.name, self.holder)]
        pbone = "prop_" + self.holder
        G = np.array(self.R.m.bones[pbone]["pivot"], float)
        out = np.zeros((len(draw), 3))
        hang = A.target(*HANG)
        for i in range(len(draw)):
            pose = {"arm_" + self.holder: {"rot": tuple(hold_q[i][:3])}, "forearm_" + self.holder: {"rot": (hold_q[i][3], 0, 0)}, "hand_" + self.holder: {"rot": tuple(hold_h[i])}}
            p = self.R.pose_point(pbone, G, pose)
            cheek = np.array([A.sg * 2.8, d.head_y + 2.0, -(d.td / 2.0 + 3.2)])
            out[i] = hang + (cheek - hang) * draw[i]
        return out

    def second_hand(self, c, extra=None):
        """The off hand of a two-handed prop grips the shaft: its wrist is solved onto the point `grip2` of the prop, at every sample."""
        if not (self.coupled and self.holder and (c.name, self.holder) in self.last_q):
            return
        side = "l" if self.holder == "r" else "r"
        if (c.name, side) in self.ik_arms:                      # the recipe already moved this hand (the string hand of a bow draws, the off hand of a punch ...)
            return
        hold_q, hold_h = self.last_q[(c.name, self.holder)]
        A2 = self.arm[side]
        d = self.d
        n = c.n + 1
        if self.kind == "bow":                                    # the string hand rests on its own side of the chest (see string_targets)
            q, _ = A2.solve_track(self.string_targets(c, np.zeros(n)))
            c.put("arm_" + side, "rot", q[:, :3])
            c.put("forearm_" + side, "rot", np.stack([q[:, 3], np.zeros(n), np.zeros(n)], 1))
            self.ik_arms.add((c.name, side))
            return
        pts = self._grip2_points(c, hold_q, hold_h, extra)
        q2, _ = A2.solve_track(self._off_hand_wrists_exact(A2, pts))
        c.put("arm_" + side, "rot", q2[:, :3])
        c.put("forearm_" + side, "rot", np.stack([q2[:, 3], np.zeros(n), np.zeros(n)], 1))
        self.ik_arms.add((c.name, side))

    def off_rest(self):
        """The wrist key (elevation, azimuth, reach) of the off hand of a two-handed prop at the rest hold, where the idle keeps it on the shaft."""
        if self.rest_key is None:
            hold, shift, _ = self._two_handed_fit()
            A1 = self.arm[self.holder]
            A2 = self.arm["l" if self.holder == "r" else "r"]
            pd = self.main
            grip = tuple(self.S["props"][self.main_slot]["grip"] or pd.grip)
            d1 = dir_from(hold[3], hold[4])
            sides = None if pd.orient is None else [(pd.orient[0], side_dir(pd.orient[1], d1))]
            q1, _ = A1.solve_track(np.array([A1.target(hold[0], hold[1], hold[2], shift)]))
            h1 = A1.wrist_track(q1, [d1], grip, sides=sides)
            W = self._off_hand_wrists(A2, self._grip2_points(None, q1, h1))[0]
            q2, _ = A2.solve_track(np.array([W]))
            W = A2.fk(q2[0])[0]                                      # where the off wrist really is at the rest hold
            v = W - A2.Pa
            rho = float(np.linalg.norm(v))
            u = v / rho
            self.rest_key = (math.degrees(math.asin(max(-1.0, min(1.0, u[1])))), math.degrees(math.atan2(A2.sg * u[0], -u[2])), rho / A2.U)
        return self.rest_key

    def free_arm(self, c, side, keys):
        """A free arm through the same IK without a prop. The arm that carries a shield rests raised behind it, and the off hand of a two-handed prop rests on the shaft, not
        hanging: a key that asks for the hanging arm gets that pose instead, so every clip starts and ends on the pose the idle holds."""
        rest = None
        if side == self.shield_side:
            rest = GUARD
        elif self.coupled and self.holder and side != self.holder and self.kind != "bow":
            rest = self.off_rest()
        if rest is not None:
            keys = [(k[0],) + (tuple(rest) if tuple(k[1:4]) == HANG else tuple(k[1:4])) + tuple(k[4:]) for k in keys]
        self.arm_track(c, side, keys, holder=False)

    def hanging(self, c, side, sway=1.0, swing=None, bend=12.0, out=9.0, ph=0.0):
        """An arm hanging at the side, breathing a little (FK). swing: an optional array of degrees for the shoulder (a walk)."""
        sg = 1 if side == "l" else -1
        th = c.theta()
        ax = 4.0 + 1.6 * sway * np.sin(th + ph)
        if swing is not None:
            ax = ax + swing
        arm = np.stack([ax, np.zeros_like(th), sg * (out + 0.9 * sway * np.sin(th + ph + 1.0))], 1)
        fore = np.stack([bend + 2.0 * sway * np.sin(th + ph + 0.6), np.zeros_like(th), np.zeros_like(th)], 1)
        c.add("arm_" + side, "rot", arm)
        c.add("forearm_" + side, "rot", fore)

    def rest_hold(self, c, bob=None, sway=1.0):
        """The holding arm at the prop's rest pose, breathing; bob = an array (degrees of elevation) for a walk."""
        hold = self.hold()
        el, az, rc, ph, ti = hold
        th = c.theta() if c.loop else None
        if c.loop:
            keys = [(float(t), el + (bob[i] if bob is not None else 0.0) + 1.2 * sway * SIN(th[i]), az + 1.5 * sway * SIN(th[i] + 1.0), rc, ph + 1.5 * sway * SIN(th[i] + 0.4), ti)
                    for i, t in enumerate(c.ts)]
            keys = keys[::2] + ([keys[-1]] if (len(keys) - 1) % 2 else [])
        else:
            keys = [(0.0, el, az, rc, ph, ti), (c.length, el, az, rc, ph, ti)]
        self.arm_track(c, self.holder, keys, holder=True)

    def finish(self, c, ground=True, clear=True):
        self.second_hand(c)
        if self.hover:                                            # a hovering character floats HOVER_PX above the floor in every clip, and drops when it dies
            lift = np.full(c.n + 1, HOVER_PX)
            if c.name == "death":
                lift = HOVER_PX * (1.0 - np.clip(c.ts / 0.9, 0.0, 1.0))
            c.add("root", "pos", np.stack([np.zeros_like(lift), lift, np.zeros_like(lift)], 1))
        if ground:
            ground_feet(self, c)
        if clear:
            clearances(self, c)


def _key_columns(c, ks):
    """The five columns (elevation, azimuth, reach, phi, tilt) of a wrist key list [(t, el, az, rc, phi_or_None, tilt)], sampled on the grid of the clip c."""
    cols = []
    for i in range(1, 6):
        pts = [(k[0], (k[i] if k[i] is not None else 0.0, 0.0, 0.0)) for k in ks]
        if len(pts) == 1:
            pts = [(0.0, pts[0][1]), (c.length, pts[0][1])]
        cols.append(np.array([catmull(pts, float(t))[0] if len(pts) > 2 else _lin1(pts, float(t)) for t in c.ts]))
    return cols


def _smooth_rows(a, loop, r=2):
    """A moving average over the rows of a sampled signal (a loop wraps around its repeated last sample)."""
    src = a[:-1] if loop else a
    m = src.shape[0]
    acc = np.zeros_like(src)
    for k in range(-r, r + 1):
        acc += np.roll(src, k, axis=0) if loop else src[np.clip(np.arange(m) - k, 0, m - 1)]
    acc /= (2 * r + 1)
    return np.concatenate([acc, acc[:1]]) if loop else acc


def _lin1(pts, t):
    if t <= pts[0][0]:
        return pts[0][1][0]
    if t >= pts[-1][0]:
        return pts[-1][1][0]
    for k in range(len(pts) - 1):
        if pts[k][0] <= t <= pts[k + 1][0]:
            u = (t - pts[k][0]) / max(1e-9, pts[k + 1][0] - pts[k][0])
            return pts[k][1][0] + (pts[k + 1][1][0] - pts[k][1][0]) * u
    return pts[-1][1][0]


# ------------------------------------------------------------------------------------------------------------------------ follow-through
def follow(ctx, c, wind=0.0, osc=1.0, flutter=1.0, wing_flap=4.0, wing_fold=0.0):
    """Hair, cloth, tails, wings, floating things: they hang from the body and lag behind it. `wind` = degrees they are blown backward, `osc` scales the wobble."""
    R = ctx.R
    th = c.theta()
    body = c.get("body", "rot")
    pitch = body[:, 0]
    yaw = body[:, 1]
    roll = body[:, 2]
    by = c.get("body", "pos")[:, 1] + c.get("root", "pos")[:, 1]
    dp = pitch - lowpass(pitch, 0.10, c.loop)
    dy = yaw - lowpass(yaw, 0.10, c.loop)
    dv = by - lowpass(by, 0.08, c.loop)
    z = np.zeros_like(th)

    def put(bone, x, y=None, zz=None):
        if not ctx.has(bone):
            return
        c.add(bone, "rot", np.stack([x, z if y is None else y, z if zz is None else zz], 1))
    put("hair", -0.25 * wind - 1.0 * dp - 1.4 * dv + 1.4 * osc * np.sin(th + 1.2), 0.0 * z, 1.0 * osc * np.sin(th) - 0.5 * dy)
    put("hair_back", -0.9 * wind - 1.7 * dp - 1.6 * dv + 1.2 * osc * np.sin(th + 0.4), 0.0 * z, 1.2 * osc * np.sin(th + 1.4) - 0.8 * dy)
    for sg, suf in ((1, "_l"), (-1, "_r")):
        put("hair_tail" + suf, -0.7 * wind - 1.5 * dp - 1.6 * dv + 1.2 * osc * np.sin(th + 0.9 + sg), 0.0 * z, sg * (3.0 + 1.5 * osc * np.sin(th + 0.5 * sg)) - 0.8 * dy)
    put("cape", -0.95 * wind - 1.5 * dp - 1.2 * dv + 1.3 * osc * np.sin(th + 0.5), 0.0 * z, 1.0 * osc * np.sin(th + 1.0) - 0.9 * dy)
    put("cape_low", -0.5 * wind - 1.8 * dp - 1.5 * dv + 2.0 * osc * np.sin(th + 1.2), 0.0 * z, 1.6 * osc * np.sin(th + 1.9) - 1.2 * dy)
    put("ribbon", -1.1 * wind - 2.0 * dp + 3.0 * osc * np.sin(th + 0.3), 0.0 * z, 4.0 * osc * np.sin(th + 1.7))
    put("sash", -0.4 * wind - 1.5 * dp + 2.0 * osc * np.sin(th + 0.9), 0.0 * z, 1.5 * osc * np.sin(th + 0.2))
    put("tassel", -0.4 * wind - 1.5 * dp + 2.0 * osc * np.sin(th + 0.2))
    put("watch", -0.3 * wind - 1.0 * dp + 2.5 * osc * np.sin(th + 0.5), 0.0 * z, 1.5 * osc * np.sin(th))
    put("hat", -0.4 * wind - 1.0 * dp + 1.0 * osc * np.sin(th + 0.8))
    for k in range(1, 6):
        put("tail%d" % k, (-0.2 * wind - 0.8 * dp) * (0.6 + 0.2 * k) + 1.5 * osc * np.sin(th + 0.6 * k), 5.0 * (0.5 + 0.4 * k) * flutter * np.sin(th + 0.7 * k) - 0.7 * dy, z)
    for sg, suf in ((1, "_l"), (-1, "_r")):
        put("ear" + suf, 2.0 * osc * np.sin(th + 2.0) * 0.0 - 0.6 * dp, 0.0 * z, sg * 1.5 * osc * np.sin(2 * th + sg))
        if ctx.has("wing" + suf):
            flap = wing_flap * np.sin(th * 1.0 + 0.4)
            put("wing" + suf, z, -sg * (0.4 * flap) - sg * 0.6 * dp, -sg * (flap + wing_fold))
            put("wing_tip" + suf, z, 0.0 * z, -sg * (0.6 * wing_flap * np.sin(th + 1.2) + 0.3 * wing_fold))
    if ctx.has("halo"):                                                    # a halo bobs, and in a long loop it turns once (a ring is symmetric; a broken one or thorns show it)
        c.add("halo", "pos", np.stack([z, 0.5 * np.sin(th), z], 1))
        if c.loop and c.length >= 2.5:
            c.add("halo", "rot", np.stack([z, z, 360.0 * c.ts / c.length], 1))
    if ctx.has("prop_float"):
        r = 3.0
        c.add("prop_float", "pos", np.stack([r * np.sin(th), 1.2 * np.sin(2 * th), r * np.cos(th) - r], 1))
        c.add("prop_float", "rot", np.stack([z, 0.0 * z + 40.0 * np.sin(th), z], 1))
    for a in R.facts.get("animate", []):                                  # the bones a character's `extra` asked to have moved (Rig.animate)
        b = a["bone"]
        if not ctx.has(b):
            continue
        sw, ph = a["sway"], a["phase"]
        if sw:
            put(b, sw * (-0.8 * a["wind"] * wind - 1.5 * dp - 1.2 * dv + 1.4 * osc * np.sin(th + ph)), 0.0 * z, sw * (1.2 * osc * np.sin(th + ph + 1.3) - 0.8 * dy))
        if a["spin"]:
            turns = a["spin"] * c.length
            ang = (360.0 * round(turns) * c.ts / c.length) if c.loop and abs(turns) >= 0.5 else 360.0 * a["spin"] * c.ts
            rot = np.zeros((c.n + 1, 3))
            rot[:, a["axis"]] = ang
            c.add(b, "rot", rot)
        if a["pulse"]:
            k = 1.0 + a["pulse"] * np.sin(th + ph)
            c.add(b, "scale", np.stack([k, k, k], 1))


# ------------------------------------------------------------------------------------------------------------------------ feet on the ground, limbs out of the body
def _foot_min_y(R, pose):
    lo = 1e9
    for b in ("shin_l", "shin_r"):
        for cube in R.m.bones[b]["cubes"]:
            o, s = cube["origin"], cube["size"]
            for k in range(8):
                p = (o[0] + (s[0] if k & 1 else 0), o[1] + (s[1] if k & 2 else 0), o[2] + (s[2] if k & 4 else 0))
                lo = min(lo, R.pose_point(b, p, pose)[1])
    return lo


def ground_feet(ctx, c, limit=(-7.0, 2.5)):
    """The lowest point of the feet is the floor in every sample: the root is lowered (or lifted) by what the legs leave over. A hovering character skips it."""
    if ctx.hover or c.name in ("death", "transform") or not (c.has("leg_l") or c.has("leg_r") or c.has("shin_l") or c.has("shin_r")):
        return
    R = ctx.R
    offs = np.zeros(c.n + 1)
    for i in range(c.n + 1):
        pose = {}
        for b in ("leg_l", "leg_r", "shin_l", "shin_r"):
            if c.has(b):
                pose[b] = {"rot": tuple(c.rot[b][i])}
        y = _foot_min_y(R, pose)
        offs[i] = min(limit[1], max(limit[0], -y))
    offs = np.minimum(offs, 0.0) * 0.0 + offs
    c.add("root", "pos", np.stack([np.zeros_like(offs), offs, np.zeros_like(offs)], 1))


def _box_corners(cube):
    o, s = cube["origin"], cube["size"]
    return [(o[0] + (s[0] if k & 1 else 0), o[1] + (s[1] if k & 2 else 0), o[2] + (s[2] if k & 4 else 0)) for k in range(8)]


def clearances(ctx, c):
    """An arm never enters the torso (its shoulder is swung outward until the elbow and fist are clear), a cape never enters a leg."""
    R, d = ctx.R, ctx.d
    tw, td = d.tw, d.td
    for side, sg in (("l", 1), ("r", -1)):
        if not (c.has("arm_" + side)) or (c.name, side) in ctx.ik_arms:
            continue                                              # an arm solved by IK already keeps out of the torso (see ArmIK.solve)
        arm = c.rot["arm_" + side]
        fore = c.rot["forearm_" + side]
        for i in range(c.n + 1):
            for _ in range(24):
                depth = _arm_depth(R, d, side, arm[i], fore[i][0], sg)
                if depth <= 0.6:
                    break
                arm[i][2] += sg * 2.0
                if abs(arm[i][2]) > 100:
                    break
    # the cape against the legs: tilt it back until it clears
    if c.has("cape") or c.has("cape_low"):
        _clear_cape(ctx, c)


def _arm_depth(R, d, side, arm, fx, sg):
    """How far the arm's boxes (upper arm, forearm) reach into the torso box in the body frame (pixels)."""
    pose = {"arm_" + side: {"rot": tuple(arm)}, "forearm_" + side: {"rot": (fx, 0, 0)}}
    hx, hy, hz = d.tw / 2 - 0.05, (d.hip_y + 0.3, d.sh_y - 0.1), d.td / 2 - 0.05
    worst = 0.0
    for b in ("arm_" + side, "forearm_" + side):
        for cube in R.m.bones[b]["cubes"][:1]:
            for p in _box_corners(cube):
                if b.startswith("arm_") and p[1] > d.sh_piv - 1.0:
                    continue                                    # the top of the upper arm overlaps the shoulder by design
                q = R.pose_point(b, p, pose)
                if abs(q[0]) < hx and hy[0] < q[1] < hy[1] and abs(q[2]) < hz:
                    worst = max(worst, min(hx - abs(q[0]), hz - abs(q[2])))
    return worst


def _clear_cape(ctx, c):
    R, d = ctx.R, ctx.d
    for i in range(c.n + 1):
        for _ in range(16):
            pose = {}
            for b in ("leg_l", "leg_r", "shin_l", "shin_r", "cape", "cape_low", "body"):
                if c.has(b, "rot"):
                    pose[b] = {"rot": tuple(c.rot[b][i])}
            pen = 0.0
            for lb in ("shin_l", "shin_r", "leg_l", "leg_r"):
                for cube in R.m.bones[lb]["cubes"]:
                    for p in _box_corners(cube):
                        q = R.pose_point(lb, p, {k: v for k, v in pose.items() if k not in ("cape", "cape_low", "body")})
                        # the cape's rear plane (z of its inside face) rotated by the cape bones
                        pen = max(pen, _cape_depth(R, d, q, {k: pose[k] for k in ("cape", "cape_low") if k in pose}))
            if pen <= 0.3:
                break
            for b in ("cape_low", "cape"):
                if c.has(b, "rot"):
                    c.rot[b][i][0] -= 3.0 if b == "cape_low" else 1.2


def _cape_depth(R, d, q, pose):
    """Is the point q (body frame, upright) inside one of the cape's boxes under the cape bones' rotation? Returns the penetration."""
    worst = 0.0
    for b in ("cape", "cape_low"):
        if not R.has(b):
            continue
        for cube in R.m.bones[b]["cubes"][:1]:
            o, s = cube["origin"], cube["size"]
            # bring q into the cape box's own frame by the inverse of the bone chain: approximate with the rotation about x only (the dominant motion)
            ax = sum(pose[k]["rot"][0] for k in ("cape", "cape_low") if k in pose and (k == b or b == "cape_low"))
            piv = np.array(R.m.bones[b]["pivot"], float)
            Ri = rot_matrix(-ax, 0, 0)
            pl = piv + Ri @ (np.asarray(q) - piv)
            if o[0] < pl[0] < o[0] + s[0] and o[1] < pl[1] < o[1] + s[1] and o[2] < pl[2] < o[2] + s[2]:
                worst = max(worst, min(pl[2] - o[2], o[2] + s[2] - pl[2]))
    return worst
