"""The hair of the HD arc characters: one kit style per look name, with the reason (docs/arc/hd_arc.md, section "Hair").

The 64x64 generator maps the art-kit hair style of each sheet to one of the 23 classic kit styles (tools/playermodel/characters.py HAIR_MAP). The HD pass keeps the classic style where it already fits
the character and moves the rest to the attached 3D `anime_*` cuts of 3.37 (thick layered clumps, nothing floats): spiky hair must read as layered clumps, not fingers, so the spike cuts are used
for the characters with a mane of spikes and the swept and short cuts for the neat ones. The hair COLOUR is never changed here (it stays the sheet's, tools/arc/char_<id>.py).

    python3 -B tools/arc_hd/hair_map.py            # prints the table (markdown) and checks every style exists in hair_styles.json
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
STYLES_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "playermodel", "hair_styles.json")

KEEP = "classic"      # the 64x64 generator's own choice stays (None as the style below means 'whatever the generator chose')

# look name (or base id, which its `_form2` follows unless listed) -> (kit hair style or None, reason). None = bald / no kit hair.
HAIR = {
    # ---- Seven Deadly Sins
    "meliodas": ("anime_spike_swept", "a wild blond crest swept up and back: the big layered spike mass of the owner's reference 154"),
    "diane": ("twin_tails", "KEPT classic: the long twin tails of the Serpent's Sin (no anime twin-tail cut exists)"),
    "ban": ("anime_spiky", "swept-up silver spikes with long side locks: thick vertical clumps"),
    "king": ("anime_short", "a layered brown mop with a fringe over the brow: reference 156"),
    "gowther": ("curtains", "KEPT classic: the centre-parted pink bob"),
    "gowther_form2": ("anime_long", "the long pink hair of his true form, full back and cheek locks"),
    "merlin": ("bob", "KEPT classic: the black bob with the long fringe"),
    "escanor": ("anime_short", "the short brown cut of Lowly Escanor"),
    "escanor_form2": ("anime_mane", "the blazing mane of the Sun's noon: a full chunky mane"),
    # ---- Black Clover: the Black Bulls and the other squads
    "asta": ("anime_spiky", "short upright spikes: thick clumps standing up, the hair of the main hero"),
    "yuno": ("anime_bangs_long", "the parted fringe that sweeps to the temples and leaves the eyes free"),
    "noelle_silva": ("twin_tails", "KEPT classic: two low twin tails"),
    "yami_sukehiro": ("anime_spiky", "short black spikes swept up"),
    "mimosa_vermillion": ("anime_long", "long wavy blond hair, a full back and cheek locks"),
    "nozel_silva": ("anime_swept", "silver hair swept over the brow in a long fringe: the silver reference 155"),
    "luck_voltia": ("anime_short", "short messy blond"),
    "magna_swing": ("mohawk", "KEPT classic: the sheet's mohawk"),
    "finral_roulacase": ("anime_short", "short neat hair with a side fringe"),
    "charmy_pappitson": ("anime_short", "short bob-like hair"),
    "gordon_agrippa": ("anime_swept", "long black hair falling over one eye (a side-swept fringe)"),
    "vanessa_enoteca": ("anime_long", "long wavy pink hair"),
    "klaus_lunettes": ("anime_swept", "a neat side parting with the fringe over the glasses"),
    "secre_swallowtail": ("bob", "KEPT classic: the black bob"),
    "zora_ideale": ("anime_spike_swept", "red hair pushed back into a ragged crest"),
    "gauche_adlai": ("anime_short", "short unkempt black hair"),
    "marx_francois": ("bob", "KEPT classic: the short dark bob"),
    "solid_silva": ("anime_swept", "pale hair with a side parting and a long fringe"),
    "fuegoleon_vermillion": ("anime_swept", "red hair swept back with a fringe: a neat captain"),
    "mereoleona_vermillion": ("anime_mane", "the wild red lion's mane"),
    "leopold_vermillion": ("anime_spiky", "wild red spikes"),
    "julius_novachrono": ("anime_long_open", "medium-long pale hair framing the face, no centre fringe"),
    "william_vangeance": ("anime_swept", "long white hair swept over one eye"),
    "nacht_faust": ("high_ponytail", "KEPT classic: the high ponytail"),
    # ---- the Zogratis family and the Eye of the Midnight Sun
    "dante_zogratis": ("slick", "KEPT classic: dark swept-back hair"),
    "vanica_zogratis": ("bob", "KEPT classic: the black bob"),
    "zenon_zogratis": ("anime_short", "short dark hair"),
    "zenon_zogratis_form2": ("mohawk", "KEPT classic: the pale mohawk of his true form"),
    "lucius_zogratis": ("anime_spike_tall", "tall black spikes, a crown of clumps; the true form is the same cut in white"),
    "fana": ("anime_long", "long pink hair"),
    "fana_form2": ("anime_mane", "her hair lifts into a wild mane in her second form"),
    "heath_grice": ("slick", "KEPT classic: silver hair slicked back"),
    "lotus_whomalt": ("anime_short", "short unkempt hair"),
    "morris_libardirt": ("anime_spiky", "short blond spikes"),
    "patolli": ("braid", "KEPT classic: the long braid"),
    "rades_spirito": ("anime_short", "short messy hair"),
    "rades_spirito_form2": ("anime_mane", "his hair flares into a mane in the second form"),
    "revchi_salik": ("anime_short", "short unkempt dark hair"),
    "rhya": ("anime_short", "short messy dark hair"),
    "rhya_form2": ("anime_spiky", "white spikes in the second form"),
    "mars": ("anime_spiky", "pale spiky hair"),
    "mars_form2": ("anime_spike_tall", "taller pale spikes in the second form"),
    "lucifero": ("anime_long", "long pale hair"),
    "udaeus": (None, "bald: the giant has no hair (the sheet says bald)"),
    "vetto": ("anime_mane", "the wild golden mane"),
    "zagred": ("anime_spike_tall", "tall dark spikes, the devil's crown of clumps"),
    "zagred_legacy": ("spiky_long", "KEPT classic: the long spiky hair"),
    "dark_disciple": ("anime_hood", "the cultist's hood (the 3.37 hood sheet)"),
    "dark_disciple_astaroth": ("anime_hood", "the cultist's hood"),
    "dark_disciple_beelzebub": ("anime_hood", "the cultist's hood"),
    "dark_disciple_lucifero": ("anime_hood", "the cultist's hood"),
    "dark_disciple_megicula": ("anime_hood", "the cultist's hood"),
    "devil_avatar": (None, "bald: the devil's body has no hair (horns are extras)"),
    "devil_avatar_megicula": (None, "bald"),
}


def lookup(name):
    """(style or None, reason) of a look name: its own entry, else its base id's (`<id>_form2`), else None."""
    if name in HAIR:
        return HAIR[name]
    base = name[:-6] if name.endswith("_form2") else name
    return HAIR.get(base)


def style_for(name, classic):
    """The kit hair of a look: the table's style, 'bald' for None, or `classic` (the 64x64 generator's choice) when the id is not in the table."""
    h = lookup(name)
    if h is None:
        return classic
    return "bald" if h[0] is None else h[0]


def reason_for(name):
    h = lookup(name)
    return h[1] if h else "not in the table: the 64x64 generator's classic style"


def known_styles():
    return set(json.load(open(STYLES_JSON))["styles"])


def check():
    known = known_styles()
    bad = ["%s -> %s" % (k, v[0]) for k, v in HAIR.items() if v[0] is not None and v[0] not in known]
    return bad


def markdown(names=None):
    rows = ["| look | kit hair | reason |", "|---|---|---|"]
    for k, (s, why) in HAIR.items():
        if names is None or k in names:
            rows.append("| `%s` | `%s` | %s |" % (k, s or "-", why))
    return "\n".join(rows)


if __name__ == "__main__":
    print(markdown())
    bad = check()
    print("\nunknown styles:", bad or "none")
    sys.exit(1 if bad else 0)
