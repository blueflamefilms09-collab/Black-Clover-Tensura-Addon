"""Weapon-form models of the VASSAL CHAIN (base vassal_chain, size class whip), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_chain.py --only tempered [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids: vassal_chain__tempered_chain, __hydra_chain, __orochi_chain, __idle_chain, __drowsy_chain, __long_afternoon.
Every form keeps the base chain's frame (mesh space of vassal_chain: the leather grip up the y axis from -6.5 to 46, the clasp with the stone at y 45..67, then the chain sweeping
up and over toward -x and ending near (-90, 155)): same hand, same grip, same size class.  Designs: Tempered Chain = heat-tempered heavy links, a second chain wrapped on the
grip and an open iron manacle to bind with; Hydra Chain = the chain forks into three strands that each end in a serpent's head; Orochi Chain = eight strands, eight heads, round a
crowned hub; Idle Chain (Sloth) = fat rusted links arching and sagging to a sleepy-eyed boulder that drags; Chain of the Long Nap = the chain wound twice round an alarm clock
with two bells and a spring that snaps; Chain of the Long Afternoon = three heavy chains carrying a stopped, melting sun-clock and a ledger with the bill.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_vassal_common_b as C  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import TAU, rgb  # noqa: E402
from forms_vassal_common_b import V  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
C.setup("vassal_chain", "whip", os.path.join(HERE, "forms_manifest_vassal_chain.json"), "vassal_chain", glow=("FFFFFF", 0.045, 0.45, 1.0))
mat, glowmat = C.mat, C.glowmat
MAIN = [(0.0, 64.5, 0.0), (1.2, 86.0, 0.0), (-4.5, 112.0, 0.0), (-18.5, 135.5, 0.0), (-40.0, 149.5, 0.0), (-63.5, 153.6, 0.0)]       # the base chain's path


def grip_set(P, rr, *, leather="leather", wrap="wrap", metal="iron", trim="gold", gem="gem", gem_r=4.2):
    """The hand's part of every chain: the wrapped grip (parts grip*), a pommel cap, the clasp with its stone, a knuckle bow."""
    K.grip_wrapped("grip", 3.4, 45.0, 3.1, P[leather], P[wrap], turns=9.0, cord=0.5, bands=(4.2, 44.2), band_mat=P[trim], swell=0.07, part="grip", seed=6, squash=0.95)
    C.loft("grip_cap", [K.bl.ring(y, K.bl.circle(r, 12)) for y, r in ((-6.5, 0.01), (-5.6, 3.2), (-2.6, 5.1), (0.4, 5.3), (2.6, 4.2), (3.6, 3.2))], P[metal], "grip_cap")
    C.loft("grip_cap_band", [K.bl.ring(y, K.bl.circle(r, 12)) for y, r in ((-1.2, 4.9), (-0.6, 5.5), (0.6, 5.5), (1.2, 4.9))], P[trim], "grip_cap")
    C.loft("clasp", [K.bl.ring(y, K.bl.circle(r, 12)) for y, r in ((45.0, 3.5), (46.6, 6.0), (51.5, 6.8), (57.5, 6.3), (62.5, 4.4), (66.9, 2.8))], P[metal], "clasp")
    for y, r in ((46.4, 6.25), (59.6, 5.6)):
        C.loft("clasp_band%d" % y, [K.bl.ring(yy, K.bl.circle(rad, 12)) for yy, rad in ((y - 1.2, r - 0.3), (y - 0.7, r + 0.45), (y + 0.7, r + 0.45), (y + 1.2, r - 0.3))], P[trim], "clasp")
    K.gem_setting("gem", V(0.0, 52.0, 6.4), gem_r, P[gem], P[trim], normal=(0, 0, 1), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.45)
    bow = K.bl.smooth_path([V(3.0, 44.6, 0), V(8.2, 45.6, 0), V(11.4, 38.5, 0), V(11.6, 24.0, 0), V(11.3, 10.5, 0), V(8.0, 3.4, 0), V(3.0, 2.6, 0)], 3)
    K.bl.sweep("knuckle_bow", bow, K.bl.rrect(0.8, 1.6, 0.35), P[metal], part="knuckle_bow")


def chain_along(name, pts, mat_, link_len=12.6, link_w=7.6, wire=1.0, max_links=14, part="links", up=(0, 0, 1)):
    return K.chain(name, [V(p) for p in pts], link_len, link_w, wire, mat_, max_links=max_links, part=part, up=up)


def tang(pts, i):
    a, b = V(pts[max(i - 1, 0)]), V(pts[min(i + 1, len(pts) - 1)])
    return (b - a).normalized()


# ================================================================ 1. TEMPERED CHAIN
def tempered_palette():
    P = {}
    P["steel"] = mat("tc_steel", (150, 150, 160), 1.0, 0.28, bump="hammer", bump_scale=0.6, bump_strength=0.16, dirt=0.6, scratches=0.6, mottle=0.4,
                     cramp=("y", 60.0, 160.0, [(0.0, rgb(214, 172, 96)), (0.35, rgb(176, 110, 150)), (0.7, rgb(86, 100, 190)), (1.0, rgb(60, 128, 210))]), wear_color=rgb(236, 236, 244))
    P["iron"] = mat("tc_iron", (44, 46, 56), 1.0, 0.42, bump="hammer", bump_scale=0.6, bump_strength=0.35, groove=rgb(14, 14, 20), groove_amount=0.7, dirt=0.9, scratches=0.6, mottle_color=rgb(80, 40, 30))
    P["coal"] = mat("tc_coal", (24, 22, 24), 0.0, 0.6, bump="hammer", bump_scale=1.4, bump_strength=0.8, dirt=0.3, wear=0.7, wear_color=rgb(120, 110, 110), wear_metal=0.0, emit=rgb(255, 90, 20), emit_strength=0.5, emit_cracks=0.9)
    P["gold"] = mat("tc_gold", (196, 146, 60), 1.0, 0.32, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.75)
    P["leather"] = mat("tc_leather", (70, 44, 28), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8)
    P["wrap"] = mat("tc_wrap", (96, 60, 34), 0.0, 0.7, bump="cross", bump_strength=0.9, groove=rgb(26, 14, 6), groove_amount=0.9, dirt=0.8, wear_metal=0.0)
    P["gem"] = glowmat("tc_gem", (255, 130, 40), 1.0, 0.6, rim=(rgb(255, 210, 120), 0.4))
    P["ember"] = glowmat("tc_ember", (255, 110, 30), 0.9, 0.8)
    return P


def tempered_chain(opts):
    C.begin(opts)
    C.weights(links=1.1, wrapped=1.0, manacle=1.2, grip=1.0, clasp=1.0, spikes=0.9)
    P = tempered_palette()
    rr = K.rng(7301)
    grip_set(P, rr, metal="iron")
    chain_along("link", MAIN, P["steel"], 12.6, 7.4, 1.35, 13)
    # a second, finer chain wound four times round the grip (it wraps the wielder) with a free end
    hp = [V(4.5 * math.cos(a), 6.0 + 2.3 * a, 4.5 * math.sin(a)) for a in [0.5 * k for k in range(0, 22)]]
    K.chain("wound", hp, 7.0, 4.2, 0.8, P["steel"], max_links=14, part="wound", up=(0, 1, 0))
    # coal lumps glowing in the clasp's collar
    for k in range(6):
        a = TAU * k / 6 + 0.4
        K.shard("coal%d" % k, V(6.6 * math.cos(a), 49.0 + 2.0 * (k % 2), 6.6 * math.sin(a)), 1.5, P["coal"], rr, part="coal", sides=5, stretch=1.1, dir_=V(math.cos(a), 0.2, math.sin(a)))
    # the manacle at the end: two heavy half-cuffs on a hinge, teeth inside, spikes outside, a ring to the chain
    c = V(-80.0, 154.0, 0.0)
    R = 12.5
    def arc(a0, a1, z, n=9):
        return [c + V(R * math.cos(math.radians(a0 + (a1 - a0) * i / (n - 1))), R * math.sin(math.radians(a0 + (a1 - a0) * i / (n - 1))), z) for i in range(n)]
    K.tube("jaw_up", arc(-10.0, 175.0, 2.4), 2.4, P["iron"], sides=8, sub=2, part="manacle")
    K.tube("jaw_dn", arc(185.0, 350.0, -2.4), 2.4, P["iron"], sides=8, sub=2, part="manacle")
    K.tube("heat", arc(5.0, 170.0, 2.4, 7), 0.8, P["ember"], sides=5, sub=1, part="heat") if False else None
    K.bl.lathe("hinge", [(0.01, -5.2), (2.8, -4.8), (3.0, -2.0), (3.0, 2.0), (2.8, 4.8), (0.01, 5.2)], P["steel"], sides=10, origin=tuple(c + V(R, 0, 0)), axis="z", part="manacle")
    K.bl.lathe("hinge_ring", [(3.0, -1.0), (3.7, -0.6), (3.7, 0.6), (3.0, 1.0)], P["gold"], sides=10, origin=tuple(c + V(R, 0, 4.0)), axis="z", part="manacle")
    for i, a in enumerate((20.0, 55.0, 90.0, 125.0, 160.0, 200.0, 240.0, 275.0, 310.0)):
        ar = math.radians(a)
        p0 = c + V((R - 2.0) * math.cos(ar), (R - 2.0) * math.sin(ar), 2.4 if a < 180 else -2.4)
        K.tapered("tooth%d" % i, [p0, p0 - V(1.5 * math.cos(ar), 1.5 * math.sin(ar), 0), p0 - V(3.6 * math.cos(ar), 3.6 * math.sin(ar), 0)], 0.9, 0.1, P["steel"], sides=4, sub=1, part="manacle_teeth")
    for i, a in enumerate((60.0, 100.0, 140.0)):
        ar = math.radians(a)
        p0 = c + V((R + 2.2) * math.cos(ar), (R + 2.2) * math.sin(ar), 2.4)
        K.tapered("mspike%d" % i, [p0, p0 + V(3.0 * math.cos(ar), 3.0 * math.sin(ar), 0), p0 + V(9.0 * math.cos(ar), 9.0 * math.sin(ar), 0)], 2.0, 0.15, P["steel"], sides=4, sub=1, part="spikes")
    K.link("manacle_ring", V(-66.0, 154.0, 0.0), V(1, 0, 0), V(0, 0, 1), 13.0, 8.0, 1.5, P["steel"], sides=5, segs=10, part="links")
    K.rivets("cuff_rv", [(c + V(R * math.cos(math.radians(a)), R * math.sin(math.radians(a)), 4.9 * (1 if a < 180 else -1)), (0, 0, 1 if a < 180 else -1)) for a in (40.0, 80.0, 120.0, 220.0, 260.0, 300.0)], P["gold"], r=1.0, rng_=rr, detail=False)
    return K.quad_estimate()


# ================================================================ 2. HYDRA CHAIN
def hydra_palette():
    P = {}
    P["scale"] = mat("hc_scale", (46, 110, 78), 0.0, 0.4, bump="scales", bump_scale=1.4, bump_strength=0.9, groove=rgb(12, 36, 26), groove_amount=0.8, dirt=0.6, wear=0.5, wear_color=rgb(150, 210, 150), wear_metal=0.0,
                     mottle=0.9, mottle_color=rgb(20, 60, 44))
    P["scale2"] = mat("hc_scale2", (72, 150, 96), 0.0, 0.4, bump="scales", bump_scale=1.4, bump_strength=0.9, groove=rgb(16, 50, 32), groove_amount=0.8, dirt=0.6, wear=0.5, wear_metal=0.0, mottle=0.9, mottle_color=rgb(30, 90, 50))
    P["link"] = mat("hc_link", (92, 120, 110), 1.0, 0.32, bump="hammer", bump_scale=0.6, bump_strength=0.16, dirt=0.7, scratches=0.5, mottle_color=rgb(30, 60, 50))
    P["iron"] = mat("hc_iron", (36, 54, 50), 1.0, 0.42, bump="hammer", bump_scale=0.6, bump_strength=0.3, groove=rgb(10, 18, 16), groove_amount=0.7, dirt=0.9)
    P["fang"] = mat("hc_fang", (236, 232, 208), 0.0, 0.35, bump="brushed", bump_strength=0.1, dirt=0.5, wear=0.3, wear_metal=0.0, bevel=0.1)
    P["gold"] = mat("hc_gold", (190, 150, 60), 1.0, 0.32, bump="hammer", bump_strength=0.14, dirt=0.75)
    P["leather"] = mat("hc_leather", (60, 56, 40), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8)
    P["wrap"] = mat("hc_wrap", (40, 80, 60), 0.0, 0.7, bump="cross", bump_strength=0.9, groove=rgb(8, 24, 16), groove_amount=0.9, dirt=0.8, wear_metal=0.0)
    P["gem"] = glowmat("hc_gem", (120, 255, 90), 1.0, 0.6, rim=(rgb(220, 255, 190), 0.4))
    P["eye"] = glowmat("hc_eye", (180, 255, 60), 1.1, 0.4)
    P["venom"] = glowmat("hc_venom", (120, 255, 80), 0.9, 0.7)
    return P


def strand(P, name, pts, head_size, rr, links=4, link_len=10.0, link_w=6.0, neck_len=16.0, horn=False, mat_scale="scale", spines=3):
    """A strand: a few links, then a scaled neck swelling into a serpent's head that looks along the strand."""
    chain_along(name + "_l", pts, P["link"], link_len, link_w, 1.0, links, part="links")
    end = V(pts[-1])
    d = tang(pts, len(pts) - 1)
    n = V(-d.y, d.x, 0.0)
    if n.y < 0:
        n = -n
    p1, p2, p3 = end + d * (neck_len * 0.35) + n * (neck_len * 0.05), end + d * (neck_len * 0.7) + n * (neck_len * 0.28), end + d * (neck_len * 0.9) + n * (neck_len * 0.42)
    K.tapered(name + "_neck", [end, p1, p2, p3], 2.3, 1.6, P[mat_scale], sides=8, sub=2, part="necks")
    for k in range(spines):                                                                         # a crest of glowing spines along the neck
        p = [p1, p2, p3][k + 3 - spines] + V(0, 0, 1.4)
        K.tapered("%s_spine%d" % (name, k), [p, p + V(0, 0, 1.6), p + V(0, 0, 3.6) + n * 0.4], 0.7, 0.06, P["venom"], sides=4, sub=1, part="crest")
    hd = (d * 0.9 - n * 0.35).normalized()
    hu = V(-hd.y, hd.x, 0.0)
    if hu.y < 0:
        hu = -hu
    C.snake_head(name, p3 - hd * 1.5, hd, head_size, P[mat_scale], P["fang"], P["eye"], up=hu, part="heads", jaw_open=0.5, horn=horn)


def hydra_chain(opts):
    C.begin(opts)
    C.weights(links=1.1, heads=1.3, necks=1.0, grip=1.0, clasp=1.0, crest=0.8)
    P = hydra_palette()
    rr = K.rng(7302)
    grip_set(P, rr, metal="iron", trim="gold")
    hub = V(-34.0, 140.0, 0.0)
    chain_along("main", MAIN[:4] + [tuple(hub)], P["link"], 12.6, 7.4, 1.1, 9)
    K.blob("hub", hub, (7.0, 7.0, 6.0), P["iron"], seed=3, lump=0.05, rings=7, segs=10, part="hub")
    C.loft("hub_ring", [C.ring_z(z, 7.8, 7.8, hub.x, hub.y, 3.5) for z in (-1.6, 1.6)], P["gold"], "hub")
    K.gem_setting("hub_gem", hub + V(0, 0, 6.0), 2.4, P["gem"], P["gold"], normal=(0, 0, 1), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.4)
    # the three heads: up and over, straight out, and down
    strand(P, "h1", [tuple(hub), (-43.0, 156.0, 1.0), (-52.0, 168.0, 2.0)], 24.0, rr, links=5, link_len=7.0, link_w=5.0, neck_len=18.0)
    strand(P, "h2", [tuple(hub), (-58.0, 147.0, -1.0), (-76.0, 151.0, 0.0)], 26.0, rr, links=5, link_len=7.0, link_w=5.0, neck_len=18.0, horn=True, mat_scale="scale2")
    strand(P, "h3", [tuple(hub), (-47.0, 124.0, -1.0), (-60.0, 112.0, -2.0)], 23.0, rr, links=5, link_len=7.0, link_w=5.0, neck_len=18.0)
    # venom dripping from the fangs of the middle head
    for k in range(2):
        K.drip("venom%d" % k, V(-104.0 + 3.0 * k, 144.0 - 3.0 * k, (1 - 2 * k) * 2.0), 8.0 + 3.0 * k, 0.9, P["venom"], seed=k + 2, sides=5, part="venom")
    return K.quad_estimate()


# ================================================================ 3. OROCHI CHAIN
def orochi_palette():
    P = hydra_palette()
    P["scale"] = mat("oc_scale", (44, 50, 110), 0.0, 0.4, bump="scales", bump_scale=1.4, bump_strength=0.9, groove=rgb(10, 12, 40), groove_amount=0.8, dirt=0.6, wear=0.5, wear_color=rgb(140, 170, 230), wear_metal=0.0,
                     mottle=0.9, mottle_color=rgb(24, 90, 110))
    P["scale2"] = mat("oc_scale2", (40, 120, 130), 0.0, 0.4, bump="scales", bump_scale=1.4, bump_strength=0.9, groove=rgb(8, 36, 44), groove_amount=0.8, dirt=0.6, wear=0.5, wear_color=rgb(160, 230, 230), wear_metal=0.0,
                      mottle=0.9, mottle_color=rgb(30, 60, 120))
    P["gold"] = mat("oc_gold", (214, 170, 70), 1.0, 0.28, bump="hammer", bump_strength=0.14, dirt=0.7, mottle_color=rgb(120, 80, 30))
    P["link"] = mat("oc_link", (130, 150, 170), 1.0, 0.3, bump="hammer", bump_scale=0.6, bump_strength=0.16, dirt=0.6, scratches=0.5)
    P["gem"] = glowmat("oc_gem", (70, 230, 235), 1.0, 0.6, rim=(rgb(210, 255, 255), 0.5))
    P["eye"] = glowmat("oc_eye", (255, 70, 80), 1.1, 0.4)
    P["venom"] = glowmat("oc_venom", (80, 230, 240), 0.9, 0.7)
    return P


def orochi_chain(opts):
    C.begin(opts)
    C.weights(links=1.0, heads=1.3, necks=0.9, hub=1.2, grip=1.0, clasp=1.0)
    P = orochi_palette()
    rr = K.rng(7303)
    grip_set(P, rr, metal="iron", trim="gold")
    hub = V(-40.0, 134.0, 0.0)
    chain_along("main", MAIN[:3] + [(-22.0, 124.0, 0.0), tuple(hub)], P["link"], 12.6, 7.4, 1.1, 6)
    # the crowned hub: a stepped octagon with a river-blue stone each side, a ring of eight gold sockets
    octo = [(hub.x + 11.0 * math.cos(TAU * i / 8 + 0.39), hub.y + 11.0 * math.sin(TAU * i / 8 + 0.39)) for i in range(8)]
    for s in (1, -1):
        K.stepped_plate("hubplate%d" % s, octo, 1.6 * s, P["gold"], steps=3, step_h=0.9, inset=1.2, part="hub", up=s, bevel_w=0.1)
        K.gem_setting("hubgem%d" % s, hub + V(0, 0, 5.2 * s), 4.2, P["gem"], P["gold"], normal=(0, 0, s), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
    C.loft("hub_core", [C.ring_z(z, 9.0, 9.0, hub.x, hub.y, 4.0) for z in (-1.8, 1.8)], P["iron"], "hub")
    K.bl.lathe("halo", [(18.0, -1.0), (20.0, -1.0), (20.4, 0.0), (20.0, 1.0), (18.0, 1.0)], P["gold"], sides=32, origin=tuple(hub + V(0, 0, -2.4)), axis="z", part="halo", caps=(False, False))
    # eight strands fanned over the left half-plane, alternately in front of and behind the hub's plane
    angs = (84.0, 110.0, 136.0, 160.0, 186.0, 212.0, 240.0, 268.0)
    for i, ang in enumerate(angs):
        a = math.radians(ang)
        d = V(math.cos(a), math.sin(a), 0.0)
        z = 3.2 * (1 if i % 2 else -1)
        ln = 30.0 + 4.0 * (i % 3)
        p0 = hub + d * 10.0 + V(0, 0, z * 0.4)
        pts = [tuple(p0), tuple(p0 + d * ln * 0.5 + V(0, 0, z * 0.8)), tuple(p0 + d * ln + V(0, 0, z))]
        strand(P, "o%d" % i, pts, 14.0, rr, links=2, spines=1, link_len=8.0, link_w=5.0, neck_len=11.0, horn=(i % 4 == 0), mat_scale="scale" if i % 2 == 0 else "scale2")
    return K.quad_estimate()


# ================================================================ 4. IDLE CHAIN  (Sloth: fat rusted links, an arching sag, a sleepy-eyed boulder that drags)
def idle_palette():
    P = {}
    P["link"] = mat("ic_link", (92, 80, 88), 1.0, 0.5, bump="hammer", bump_scale=0.6, bump_strength=0.35, groove=rgb(30, 22, 24), groove_amount=0.7, dirt=1.0, scratches=0.5, mottle=1.0, mottle_color=rgb(118, 62, 36),
                    wear_color=rgb(170, 150, 160))
    P["rock"] = mat("ic_rock", (86, 78, 96), 0.2, 0.7, bump="hammer", bump_scale=0.5, bump_strength=0.7, groove=rgb(26, 22, 34), groove_amount=0.7, dirt=0.9, wear=0.5, wear_color=rgb(150, 140, 160), wear_metal=0.0,
                    mottle=1.0, mottle_color=rgb(56, 46, 70))
    P["moss"] = mat("ic_moss", (70, 98, 54), 0.0, 0.8, bump="leather", bump_scale=2.0, bump_strength=0.6, dirt=0.3, wear=0.2, wear_metal=0.0, mottle=1.0, mottle_color=rgb(110, 120, 60), bevel=0.05)
    P["web"] = mat("ic_web", (214, 212, 224), 0.0, 0.5, dirt=0.0, wear=0.0, bevel=0.0)
    P["iron"] = mat("ic_iron", (48, 44, 56), 1.0, 0.45, bump="hammer", bump_scale=0.6, bump_strength=0.4, groove=rgb(14, 12, 18), groove_amount=0.7, dirt=0.95, mottle_color=rgb(90, 44, 30))
    P["gold"] = mat("ic_gold", (150, 120, 70), 1.0, 0.45, bump="hammer", bump_strength=0.2, dirt=0.9)
    P["leather"] = mat("ic_leather", (58, 44, 52), 0.0, 0.75, bump="leather", bump_strength=0.5, dirt=0.9)
    P["wrap"] = mat("ic_wrap", (80, 66, 84), 0.0, 0.7, bump="cross", bump_strength=0.9, groove=rgb(20, 16, 24), groove_amount=0.9, dirt=0.9, wear_metal=0.0)
    P["gem"] = glowmat("ic_gem", (170, 150, 255), 0.7, 0.6, rim=(rgb(230, 220, 255), 0.4))
    P["lit"] = glowmat("ic_lit", (160, 140, 240), 0.55, 0.7)
    return P


def idle_chain(opts):
    C.begin(opts)
    C.weights(links=1.1, weight=1.2, moss=0.8, web=0.6, grip=1.0, clasp=1.0, eye=1.0)
    P = idle_palette()
    rr = K.rng(7304)
    grip_set(P, rr, metal="iron", trim="gold", gem="gem")
    arch = [(0.0, 64.5, 0.0), (2.0, 98.0, 0.0), (-8.0, 138.0, 0.0), (-32.0, 170.0, 0.0), (-62.0, 168.0, 0.0), (-80.0, 146.0, 0.0), (-88.0, 124.0, 0.0)]
    chain_along("fat", arch, P["link"], 19.0, 12.0, 2.5, 24)
    # the boulder: an anchor-ball with a shut eyelid, drooping brow and lashes, moss on its crown, cobweb to the chain
    W = V(-92.0, 106.0, 0.0)
    K.blob("rock", W, (23.0, 19.0, 19.0), P["rock"], seed=5, lump=0.16, lump_scale=0.06, rings=11, segs=16, part="weight")
    K.bl.lathe("ring_bolt", [(1.5, 0.0), (2.9, 1.2), (3.0, 3.0), (1.6, 3.8)], P["iron"], sides=8, origin=tuple(W + V(0, 18.0, 0)), axis="y", part="weight", caps=(False, False))
    for s in (1, -1):
        zf = 18.2 * s
        K.filigree("lid%d" % s, [(W.x - 15.0, W.y + 2.0), (W.x - 6.0, W.y - 4.5), (W.x + 6.0, W.y - 4.5), (W.x + 15.0, W.y + 2.0)], P["lit"], w=1.0, h=0.7, z=zf, side=s, sub=3, taper=(1.0, 1.0), part="eyelid")
        K.filigree("brow%d" % s, [(W.x - 17.0, W.y + 8.0), (W.x - 6.0, W.y + 12.5), (W.x + 8.0, W.y + 10.5), (W.x + 17.0, W.y + 4.0)], P["iron"], w=1.7, h=1.2, z=zf - 0.4 * s, side=s, sub=3, taper=(1.0, 0.8), part="brow")
        for k in range(7):
            x = W.x - 12.0 + 4.0 * k
            y0 = W.y - 4.5 + 0.12 * (x - W.x) ** 2 / 10.0 * 0.0
            K.tapered("lash%d_%d" % (s, k), [V(x, y0, zf), V(x - 0.4, y0 - 3.0, zf + 0.8 * s), V(x - 1.2, y0 - 5.6, zf + 1.0 * s)], 0.4, 0.05, P["iron"], sides=4, sub=1, part="eyelid")
    for k in range(5):
        a = TAU * k / 5 + 0.5
        K.blob("moss%d" % k, W + V(9.0 * math.cos(a), 15.0 + 3.0 * math.sin(a * 2), 9.0 * math.sin(a)), (6.0, 3.0, 6.0), P["moss"], seed=k + 11, lump=0.3, rings=5, segs=8, part="moss")
    for k in range(4):                                                                              # cobweb strands from the chain to the boulder
        t = 0.3 + 0.15 * k
        p0 = V(-80.0, 146.0, 0.0) + V(-8.0 * t, -22.0 * t, 0.0)
        K.tube("web%d" % k, [p0, p0 + V(5.0, -6.0, 5.0 * (k - 1.5)), W + V(10.0 - 6.0 * k, 17.0, 7.0 * (k - 1.5))], 0.14, P["web"], sides=4, sub=2, part="web")
    # a stub of chain dragging from the boulder across the ground (slows what it strikes, and you)
    chain_along("drag", [(-92.0, 90.0, 0.0), (-84.0, 78.0, 2.0), (-74.0, 68.0, 3.0), (-64.0, 66.0, 3.0)], P["link"], 13.0, 8.4, 2.0, 5, part="drag")
    return K.quad_estimate()


# ================================================================ 5. CHAIN OF THE LONG NAP  (the chain coiled round an alarm clock; a spring that snaps)
def nap_palette():
    P = idle_palette()
    P["clock"] = mat("dc_clock", (46, 54, 110), 0.6, 0.35, bump="hammer", bump_scale=0.6, bump_strength=0.2, dirt=0.7, scratches=0.5, mottle=0.7, mottle_color=rgb(24, 28, 70), wear_color=rgb(150, 160, 220))
    P["brass"] = mat("dc_brass", (216, 168, 76), 1.0, 0.28, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.65, mottle_color=rgb(120, 76, 24), wear_color=rgb(255, 240, 190))
    P["face"] = mat("dc_face", (240, 230, 206), 0.0, 0.5, dirt=0.5, wear=0.15, wear_metal=0.0, bevel=0.0)
    P["glow"] = glowmat("dc_glow", (255, 190, 90), 0.85, 0.6)
    P["night"] = mat("dc_night", (26, 24, 50), 0.0, 0.7, dirt=0.3, wear=0.0, bevel=0.0)
    P["link"] = mat("dc_link", (110, 108, 130), 1.0, 0.4, bump="hammer", bump_scale=0.6, bump_strength=0.25, dirt=0.8, scratches=0.5, mottle_color=rgb(70, 60, 100))
    return P


def drowsy_chain(opts):
    C.begin(opts)
    C.weights(links=1.1, clock=1.2, bells=1.1, spring=0.9, grip=1.0, clasp=1.0, face=1.0)
    P = nap_palette()
    rr = K.rng(7305)
    grip_set(P, rr, metal="clock", trim="brass", gem="glow")
    CK = V(-86.0, 130.0, 0.0)
    R = 21.0
    # the chain climbs and loops round the clock's body twice, slack at first
    run = [(0.0, 64.5, 0.0), (2.0, 90.0, 0.0), (-8.0, 118.0, 0.0), (-30.0, 144.0, 0.0), (-52.0, 154.0, -1.0), (-71.4, 148.4, -2.0)]
    chain_along("run", run, P["link"], 13.0, 8.0, 1.5, 9)
    turns = [CK + V((R + 2.6) * math.cos(a), (R + 2.6) * math.sin(a), -2.0 + 11.0 * (a - 0.9) / (TAU * 2.2)) for a in [TAU * 2.2 * k / 26.0 + 0.9 for k in range(27)]]
    chain_along("coil", [tuple(p) for p in turns], P["link"], 10.0, 6.4, 1.3, 14, part="coil")
    # the alarm clock: a drum, a front glass with a numbered face and two hands, a back plate, two bells on top with a hammer between, feet
    for s in (1, -1):
        K.bl.lathe("drum%d" % s, [(0.01, 0.0), (R - 1.0, 0.0), (R, 3.0), (R, 7.0 if s > 0 else 7.0)], P["clock"], sides=24, origin=tuple(CK + V(0, 0, 0.0)), axis="z", part="clock", caps=(False, True)) if s > 0 else None
    C.loft("drum", [[CK + V((R * (1.0 if k else 0.97)) * math.cos(a), (R * (1.0 if k else 0.97)) * math.sin(a), z) for a in [TAU * j / 24 for j in range(24)]] for k, z in enumerate((-8.0, -6.0, 6.0, 8.5))], P["clock"], "clock")
    K.bl.lathe("bezel", [(R - 3.0, 0.0), (R + 0.2, 0.0), (R + 0.8, 1.6), (R - 2.0, 2.4)], P["brass"], sides=24, origin=tuple(CK + V(0, 0, 8.4)), axis="z", part="clock_trim", caps=(False, False))
    K.bl.lathe("face", [(R - 3.2, 0.0), (R - 3.2, 0.4)], P["face"], sides=24, origin=tuple(CK + V(0, 0, 8.5)), axis="z", part="clock_face", caps=(True, True))
    for k in range(12):
        a = TAU * k / 12
        q = CK + V((R - 5.2) * math.sin(a), (R - 5.2) * math.cos(a), 9.2)
        K.rivets("num%d" % k, [(q, (0, 0, 1))], P["glow"] if k % 3 == 0 else P["night"], r=1.2 if k % 3 == 0 else 0.8, sides=6, rng_=rr, detail=False)
    for ang, ln, w in ((-35.0, 8.0, 0.8), (70.0, 12.0, 0.6)):
        a = math.radians(ang)
        K.bar("hand%d" % int(ang), CK + V(0, 0, 9.5), CK + V(ln * math.cos(a), ln * math.sin(a), 9.5), w, 0.35, P["glow"], part="clock_hands", taper=0.5, bevel_w=0.0)
    for sg in (1, -1):
        bc = CK + V(11.0 * sg, R + 4.0, 0.0)
        K.bl.lathe("bell%d" % sg, [(0.01, 0.0), (3.6, 0.6), (7.0, 2.4), (8.8, 5.2), (9.4, 6.0), (9.0, 6.6)], P["brass"], sides=14, origin=tuple(bc), axis="y", part="bells", caps=(True, False))
        K.tube("bell_post%d" % sg, [CK + V(7.0 * sg, R - 3.0, 0.0), bc + V(0, 0.4, 0)], 0.9, P["brass"], sides=6, sub=1, part="bells")
        K.tube("foot%d" % sg, [CK + V(13.0 * sg, -R + 6.0, 0.0), CK + V(17.0 * sg, -R - 1.0, 0.0), CK + V(20.0 * sg, -R - 4.0, 0.0)], 1.6, P["brass"], sides=6, sub=2, part="clock_trim")
    K.bar("hammer", CK + V(0, R + 6.0, 0.0), CK + V(0, R + 15.0, 0.0), 0.6, 0.6, P["brass"], part="bells", bevel_w=0.0)
    K.blob("hammer_ball", CK + V(0, R + 16.0, 0.0), (2.2, 2.2, 2.2), P["brass"], seed=4, lump=0.0, rings=5, segs=8, part="bells")
    # the spring that snaps: a coil from the back of the clock to a hooked jaw hanging below
    K.spring("snap", CK + V(-R - 1.0, 0.0, 0.0), CK + V(-R - 14.0, -12.0, 0.0), 3.4, 0.6, 5, P["brass"], sides=5, part="spring")
    K.tapered("hook", [CK + V(-R - 14.0, -12.0, 0.0), CK + V(-R - 19.0, -17.0, 0.0), CK + V(-R - 15.0, -23.0, 0.0), CK + V(-R - 10.0, -19.0, 0.0)], 1.4, 0.3, P["brass"], sides=6, sub=2, part="spring")
    return K.quad_estimate()


# ================================================================ 6. CHAIN OF THE LONG AFTERNOON  (three heavy chains, a stopped melting sun-clock, a ledger with the bill)
def afternoon_palette():
    P = idle_palette()
    P["sun"] = mat("la_sun", (240, 170, 60), 1.0, 0.3, bump="hammer", bump_scale=0.6, bump_strength=0.18, dirt=0.6, scratches=0.5, mottle=0.8, mottle_color=rgb(150, 70, 30), wear_color=rgb(255, 235, 170))
    P["dusk"] = mat("la_dusk", (110, 44, 90), 0.4, 0.45, bump="hammer", bump_strength=0.2, dirt=0.7, mottle=0.8, mottle_color=rgb(50, 20, 60), wear_color=rgb(220, 140, 200))
    P["face"] = mat("la_face", (246, 226, 180), 0.0, 0.5, dirt=0.4, wear=0.2, wear_metal=0.0, bevel=0.0)
    P["glow"] = glowmat("la_glow", (255, 150, 60), 0.9, 0.7)
    P["glow2"] = glowmat("la_glow2", (240, 80, 150), 0.8, 0.7)
    P["paper"] = mat("la_paper", (232, 214, 170), 0.0, 0.8, bump="brushed", bump_strength=0.5, groove=rgb(150, 110, 60), groove_amount=0.6, dirt=0.7, wear=0.3, wear_metal=0.0, bevel=0.0)
    P["link"] = mat("la_link", (104, 82, 84), 1.0, 0.46, bump="hammer", bump_scale=0.6, bump_strength=0.3, dirt=0.95, scratches=0.5, mottle=1.0, mottle_color=rgb(150, 70, 36), wear_color=rgb(210, 170, 150))
    return P


def long_afternoon(opts):
    C.begin(opts)
    C.weights(links=1.1, sun=1.2, ledger=1.0, drips=0.9, grip=1.0, clasp=1.0, face=1.0)
    P = afternoon_palette()
    rr = K.rng(7306)
    grip_set(P, rr, metal="dusk", trim="sun", gem="glow", gem_r=4.6)
    CK = V(-84.0, 140.0, 0.0)
    R = 25.0
    # three heavy chains bundled out of the clasp, drooping and parting before the sun-clock
    for k, (dz, dy) in enumerate(((0.0, 0.0), (5.0, -6.0), (-5.0, 4.0))):
        pts = [(0.0 + 0.5 * k, 64.5, dz * 0.4), (1.2, 86.0, dz * 0.8), (-4.5, 108.0 + dy * 0.4, dz), (-20.0, 128.0 + dy * 0.6, dz), (-44.0, 138.0 + dy, dz * 1.1), (-CK.x * -0.0 + CK.x + R - 1.0, CK.y + dy * 0.8 - 6.0 * (k == 1), dz * 1.2)]
        chain_along("ch%d" % k, pts, P["link"], 12.0, 7.4, 1.5, 11, part="chains")
    # the sun-clock: a stopped dial (four o'clock) in a dusk-iron ring, rays at the rim, melting down at the bottom
    for s in (1, -1):
        K.bl.lathe("dial%d" % s, [(R - 3.0, 0.0), (R - 3.0, 0.4)], P["face"], sides=28, origin=tuple(CK + V(0, 0, 4.5 if s > 0 else -4.9)), axis="z", part="dial", caps=(True, True))
        K.bl.lathe("rim%d" % s, [(R - 3.4, 0.0), (R + 0.4, 0.0), (R + 1.2, 1.6 * s), (R - 3.0, 2.6 * s)], P["sun"], sides=28, origin=tuple(CK + V(0, 0, 4.4 * s)), axis="z", part="sun_rim", caps=(False, False))
    C.loft("dial_core", [[CK + V(R * math.cos(a), R * math.sin(a), z) for a in [TAU * j / 28 for j in range(28)]] for z in (-4.6, 4.6)], P["dusk"], "sun")
    for s in (1, -1):
        for k in range(12):
            a = TAU * k / 12
            q = CK + V((R - 6.0) * math.sin(a), (R - 6.0) * math.cos(a), 5.1 * s)
            K.rivets("num%d_%d" % (s, k), [(q, (0, 0, s))], P["glow"] if k % 3 == 0 else P["dusk"], r=1.5 if k % 3 == 0 else 0.9, sides=6, rng_=rr, detail=False)
        for ang, ln, w in ((-60.0, 12.0, 1.0), (120.0, 7.0, 1.4)):                                    # hands: stopped near four
            a = math.radians(ang)
            K.bar("hand%d_%d" % (s, int(ang)), CK + V(0, 0, 5.5 * s), CK + V(ln * math.cos(a), ln * math.sin(a), 5.5 * s), w, 0.5, P["glow"], part="hands", taper=0.5, bevel_w=0.0)
    for k in range(14):                                                                             # rays round the rim: a low sun
        a = TAU * k / 14 + 0.12
        if -2.4 < a - TAU * 0.74 < 2.4 and False:
            continue
        L = 7.0 + 4.0 * (k % 2)
        p0 = CK + V((R + 1.0) * math.cos(a), (R + 1.0) * math.sin(a), 0.0)
        K.tapered("sray%d" % k, [p0, p0 + V(L * 0.5 * math.cos(a), L * 0.5 * math.sin(a), 0), p0 + V(L * math.cos(a), L * math.sin(a), 0)], 2.0, 0.15, P["sun"], sides=4, sub=1, part="rays")
    for k, (dx, ln) in enumerate(((-12.0, 16.0), (-3.0, 24.0), (7.0, 13.0), (15.0, 19.0))):         # the melt: the rim runs down in drips
        K.drip("melt%d" % k, CK + V(dx, -R + 2.0, 4.4 * (1 if k % 2 else -1)), ln, 2.4, P["glow2"] if k % 2 else P["glow"], seed=k + 20, sides=6, part="drips")
    # the ledger: a scroll hanging from the dial with the bill written in glowing script, sealed with wax
    K.bl.lathe("scroll_rod", [(0.9, -9.0), (0.9, 9.0)], P["sun"], sides=6, origin=tuple(CK + V(0, -R - 6.0, 0.0)), axis="x", part="ledger", caps=(False, False))
    K.tube("scroll_cord", [CK + V(0, -R + 1.0, 0.0), CK + V(0, -R - 3.0, 0.0), CK + V(0, -R - 6.0, 0.0)], 0.5, P["sun"], sides=4, sub=1, part="ledger")
    path = [CK + V(0.6 * math.sin(k * 0.7), -R - 6.5 - 3.4 * k, 0.5 * math.sin(k * 0.5)) for k in range(6)]
    K.ribbon("scroll", path, 14.0, P["paper"], facing=(0, 0, 1), thick=0.1, part="ledger", fray=1.4, rng_=rr, edge_wave=0.04, notch=0.0)
    for k in range(4):
        K.engrave_lines("bill%d" % k, [[(CK.x - 5.0, CK.y - R - 9.5 - 3.4 * k), (CK.x + 5.0 - (k % 2) * 3.0, CK.y - R - 9.5 - 3.4 * k)]], P["glow"], side=1, z=0.55, w=0.3, part="bill", detail=True)
    K.blob("wax", CK + V(0, -R - 21.0, 0.8), (2.6, 2.6, 1.2), P["glow2"], seed=3, lump=0.1, rings=5, segs=8, part="ledger")
    return K.quad_estimate()


C.register("tempered", tempered_chain, "tempered_chain")
C.register("hydra", hydra_chain, "hydra_chain")
C.register("orochi", orochi_chain, "orochi_chain")
C.register("idle", idle_chain, "idle_chain")
C.register("drowsy", drowsy_chain, "drowsy_chain")
C.register("afternoon", long_afternoon, "long_afternoon")

if __name__ == "__main__":
    C.main(sys.argv[1:])
