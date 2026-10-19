"""djinn36_doc: the lang fragment and the item id list of the new Djinn's art."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import djinn36_roster as R                      # noqa: E402
import djinn36_weapon_data as W                 # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))


def item_ids():
    """The 72 item ids whose model, texture and (for the weapons) mesh the art tools write."""
    out = []
    for n in R.NAMES:
        i = n.lower()
        out += ["djinn_seal_" + i, "djinn_weapon_" + i, "djinn_weapon_" + i + "_dark"]
    return out


def lang():
    out = {}
    for n in R.NAMES:
        i = n.lower()
        _, light, dark, _ = W.KINDS[i]
        out["item.nusmp.djinn_seal_" + i] = "Seal of " + R.row(n)[1]
        out["item.nusmp.djinn_weapon_" + i] = light
        out["item.nusmp.djinn_weapon_" + i + "_dark"] = dark
    return out


def write_lang():
    path = os.path.join(ROOT, "tools", "lang", "djinn36_art.json")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang(), f, indent=2, ensure_ascii=False)
        f.write("\n")
    print("lang", path, len(lang()), "keys")


def write_ids():
    path = os.path.join(ROOT, "docs", "previews", "djinn36", "item_ids.txt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as f:
        f.write("\n".join(item_ids()) + "\n")
    print("ids", path, len(item_ids()))


# ================================================================ docs/djinn_roster_36_art.md
# name -> (head, wings, back piece, weapon in hand)
DESIGN = {
    "BAEL": ("crown of nine spires under two crossing arches with an orb, cat ears, plumes", "rank of swallow-tailed royal standards on a gilded bone", "the high back of a throne: pillars, pointed double arch, upholstered panel, sun-rune", "claymore with a crown for a guard"),
    "AGARES": ("crocodile snout across the brow, eyes on the crown, dorsal scutes and spines", "dorsal sails: barbed spines, scalloped membrane, glowing veins", "open crocodile jaws round the shoulders", "harpoon with two tooth-set barbs and a coil of rope"),
    "VALEFOR": ("thief's hood with a trailing scarf, long ass's ears, a bolt at each temple", "storm-torn cloak: ragged strips with lightning", "three great keys crossed round a heart of storm", "pair of crossed wavy kris with stolen hearts"),
    "BARBATOS": ("great antlers, a crown of oak leaves, moss hair", "boughs: bark bones, forked branches, oak leaves, berries", "horn-bow arching over the shoulders, seven arrows from a quiver", "recurve bow of living wood with antler tips and a nocked arrow"),
    "GUSION": ("crown of five standing glass panes, a mirror on the brow, a breaking wave at each temple", "fan of mirror panes with glowing frames", "the tide mirror: a ring of silver wave-scrolls round a pane of light, surf at its foot", "rapier of silver water with a mirror-disc guard"),
    "VEPAR": ("coral crown, a nautilus shell at each temple, long fin ears, seaweed hair", "manta wings: a broad membrane, ribs of light, a whip at the tip", "a ship's wheel with eight turned handles and a rope of light", "ship's anchor with a chain"),
    "SHAX": ("a dove's wings spread above the brow, a veil sewn over the mouth", "short broad dove's wing: rounded primaries, rows of coverts, an alula", "wreath of olive leaves with three doves in flight", "pair of crossed war fans of nine white feathers"),
    "BUER": ("a small wheel on the crown, herb sprigs, leaf ears, beaded braids", "fronds of healing herb: spines with marching leaf pairs", "the great five-spoked wheel with leaves at the spoke ends and healing crosses", "staff with a serpent and a five-spoked wheel of herbs"),
    "ANDREALPHUS": ("aigrette of three stalks with tufts, a fan of feather crown, swept plumes with eyes", "a peacock's train: long plumes ending in eyes of light", "the peacock fan: eleven plumes with eyes", "lance with a star head and three eyed plumes"),
    "KIMARIS": ("steed's face plate with glowing eyes, tall horse ears, flaming mane", "bat wings: bone fingers, scalloped membrane, fire fringe, wrist claw", "a great horseshoe with glowing nails and two pennons", "cavalry lance with a hand-guard cone and a fire-edged pennon"),
    "AMDUSIAS": ("a spiral unicorn horn on the brow, deer ears, a tuning fork at each temple", "harp wings: a silver frame, strings of light, a spray of feathers", "organ pipes fanned behind the shoulders with a lyre crest", "rapier whose blade is a spiral horn, tuning-fork guard"),
    "BELIAL": ("a crown that is only its frame (three hoops floating), horns curling into hoops", "wings of nothing but their frame: bone fingers, outline, struts, shreds", "the throne with nobody in it: thin pillars, arch, empty crown, open shackles", "greatsword whose blade is an outline round a glowing hollow"),
    "DECARABIA": ("carapace cap with a star on the brow, eyes on stalks, pincers at the temples", "overlapping carapace plates with four jointed crab legs", "a great pentagram in a ring with crab legs", "pair of crab pincers opening about a star"),
    "SEERE": ("small wings at the temples, a swept hair streamer", "narrow scythe-wings of seven long primaries", "a pinwheel of six wind blades round an eye of calm", "swift curved sabre with a feather guard and a ring pommel"),
    "DANTALION": ("flat scholar's cap, a face on the back of the head and at each temple", "fan of loose pages with script of light", "five tomes riding a ring round a great rune", "staff topped with a great chained tome"),
    "ANDROMALIUS": ("hood with a third eye on the forehead and a serpent coiled round it", "lattice of thin bone with watching eyes", "one great eye with rays, a ring of small eyes", "spear with a serpent and an eye in the blade"),
    "GLASYA_LABOLAS": ("hound's skull over the brow, folded ears, collar of spikes, swept horns", "fan of curved blades on bone fingers, toothed", "a ring of fangs (the maw) with three slashes of red light", "slaughter fangs: three great claws on a spiked fist bar"),
    "HAAGENTI": ("bull's horns with bronze caps, goggles on the brow, a nose ring", "bronze riveted blade-feathers with a turning cog at the joint", "an alembic: glass flask of glowing spirit, swan-neck, coil, vial, three cogs", "great bronze cleaver with a toothed back, bull-horn guard, flask pommel"),
    "CROCELL": ("crown of standing ice, an icicle beard, crystal horns", "crystal spines with branching crystals like a snowflake", "a great snowflake", "staff with a head of ice crystals about a snowflake"),
    "FURCAS": ("open sallet with nasal bar, a frozen plume, a long white beard", "a torn knight's cloak with icicles along its edge", "a kite shield with a cross of light, two lances crossed behind", "halberd with a pennon of the ice wind"),
    "BALAM": ("three small faces on the brow (ram, man, bull), lyre horns, ram curls, flame crown", "three plumes of fire, each a fan of four blades with a flame between", "an arch of fire carrying three great faces", "trident with a flame on each prong and three faces on the crossbar"),
    "ALLOCES": ("brow plate with two lion's eyes, a mane of locks and flames, round ears", "a mane of fire: curling locks with a flame along each", "the sun of a mane: a disc, sixteen flame rays, a ring", "longsword with a lion-face guard and a mane at the pommel"),
    "CAMIO": ("crow's beak hood, a ruff of black feathers, feather tufts", "a crow's wing: eight spread primaries with curled tips", "a murder of seven crows circling a small ring, notes of light", "staff ending in a raven's quill with a crow perched on it"),
    "MURMUR": ("skull mask with a drumstick standing from each temple, dreadlocks with bones", "ribs of bone with drumskins stretched between them", "the great bass drum with a skull, two small drums, two mallets crossed", "mace whose head is a drum with a skull skin and a ring of flanges"),
}


def write_doc():
    def frame(pre, i):
        d = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "djinn", f"{pre}_{i}")
        t = 0
        for f in os.listdir(d):
            q = sum(1 for ln in open(os.path.join(d, f)) if ln.startswith("qn"))
            t += q * (5 if f == "tail_seg.txt" else 1)
        return t
    rows, wrows = [], []
    for n in R.NAMES:
        h, w, b, wp = DESIGN[n]
        main, mr, second, sr, anchor = R.AMBIENT[n]
        i = n.lower()
        rows.append(f"| {R.row(n)[1]} | {h} | {w} | {b} | {wp} |")
        wrows.append(f"| {R.row(n)[1]} | {W.KINDS[i][0]} | {R.TARGET[n]} | {main} {mr}/s + {second} {sr}/s, {anchor.lower()} | {frame('u', i)} / {frame('d', i)} | {'no' if n in R.TAIL_NONE else 'yes'} |")
    text = open(os.path.join(HERE, "djinn36_doc_template.md"), encoding="utf-8").read()
    text = text.replace("@@ROWS@@", "\n".join(rows)).replace("@@ROWS2@@", "\n".join(wrows)).replace("@@IDS@@", ", ".join("`" + i + "`" for i in item_ids()))
    path = os.path.join(ROOT, "docs", "djinn_roster_36_art.md")
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print("doc", path)
