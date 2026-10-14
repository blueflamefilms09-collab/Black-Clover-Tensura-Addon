"""weapon_classes: the class table of weapon sizes and the id -> class guess (docs/weapon_scale_orientation.md).

Sizes are APPARENT blocks: the mesh length along its axis times the third-person display scale of the model json (1 block = 1 metre; Steve is
1.8 m), i.e. what a viewer sees of the weapon held in a hand. `min` / `max` bound the whole weapon, `head_min` (axes, hammers) the widest part
of the head across. `grip` is the share of the length (from the butt) where the hand is: scaling a weapon keeps that point fixed.

The table is the one source of truth: tools/relics/weapon_classes.json holds it (plus the reviewed id -> class map) and
src/main/resources/assets/nusmp/weapon_classes.json is the copy the game reads (audit_weapon_scale.py --write keeps them equal);
client/relics/item/WeaponClasses.java carries the same numbers as its built-in fallback and the same keyword guess. A test checks all three.
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
CLASSES_JSON = os.path.join(HERE, "weapon_classes.json")
ASSET_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "weapon_classes.json")

# name: (min, max, head_min, grip)   (head_min 0 = not checked)
# 1.34.0: the owner's size rule ("unless the weapon should be small, make it bigger; a great sword should be massive"). Apparent blocks.
CLASSES = {
    "dagger":     (0.45, 0.70, 0.0, 0.20),                  # dagger / knife
    "shortsword": (1.00, 1.30, 0.0, 0.20),
    "longsword":  (1.40, 1.80, 0.0, 0.20),
    "rapier":     (1.00, 1.30, 0.0, 0.20),
    "katana":     (1.40, 1.80, 0.0, 0.20),
    "sabre":      (1.00, 1.30, 0.0, 0.20),
    "dadao":      (1.80, 2.40, 0.0, 0.20),                  # a broad chopping sabre: the card of the Time-Bender calls it greatsword class
    "greatsword": (2.20, 3.00, 0.0, 0.22),                  # claymore / zweihander: massive, the blade as wide as a hand
    "handaxe":    (1.10, 1.40, 0.50, 0.25),                 # an axe held in one hand: head 0.5 - 0.7
    "battleaxe":  (1.60, 2.00, 0.75, 0.25),                 # head 0.75 - 1.0
    "greataxe":   (2.40, 3.00, 1.10, 0.28),                 # head 1.1 - 1.5
    "crescent":   (1.60, 2.00, 0.75, 0.25),
    "scythe":     (2.00, 2.60, 0.60, 0.30),
    "cleaver":    (0.90, 1.20, 0.45, 0.25),
    "spear":      (2.80, 3.60, 0.0, 0.33),                  # spear / glaive / halberd
    "glaive":     (2.80, 3.60, 0.0, 0.33),
    "trident":    (2.80, 3.60, 0.0, 0.33),
    "staff":      (2.00, 2.60, 0.0, 0.40),
    "whip":       (1.60, 2.40, 0.0, 0.15),
    "coilwhip":   (1.10, 1.70, 0.0, 0.15),                  # a whip held coiled or with a short stock (the viper whip, the junglethorn vine)
    "crossbow":   (1.00, 1.20, 0.0, 0.50),
    "bow":        (1.30, 1.60, 0.0, 0.50),
    "hammer":     (1.30, 1.80, 0.50, 0.25),                 # head 0.5 - 0.8
    "greathammer": (2.20, 2.80, 0.90, 0.28),
    "mace":       (1.30, 1.80, 0.50, 0.25),
    "club":       (1.30, 1.80, 0.0, 0.25),
    "wand":       (0.80, 1.20, 0.0, 0.20),
    "shield":     (0.90, 1.20, 0.0, 0.50),
    "item":       (0.0, 9.0, 0.0, 0.50),                    # a held trinket / book / charm: only `ITEM_MIN_EXTENT` applies (kept for old data)
    "handitem":   (0.50, 0.90, 0.0, 0.50),                  # 1.34.0: the held-item class (lantern, compass, chalice, page, charm): its largest dimension 0.5 - 0.9
}
ITEM_CLASSES = ("item", "handitem")                       # classes measured by their largest dimension, not their length
ITEM_MIN_EXTENT = 0.50                                    # blocks: the largest apparent dimension of a held item is never below this (1.34.0: was 0.40)
OVERSIZE_FACTOR = 1.5                                     # only a weapon longer than this times `max` is reported as too big (never auto-shrunk)

# ordered keyword guess for ids the table does not know (the Java fallback WeaponClasses.guess has the same list, in the same order)
KEYWORDS = [
    ("greataxe", "greataxe"), ("great_axe", "greataxe"), ("handaxe", "handaxe"), ("hand_axe", "handaxe"), ("crescent", "crescent"),
    ("scythe", "scythe"), ("axe", "battleaxe"), ("greatsword", "greatsword"), ("katana", "katana"), ("dadao", "dadao"), ("sabre", "sabre"),
    ("saber", "sabre"), ("scimitar", "sabre"), ("rapier", "rapier"), ("longsword", "longsword"), ("gladius", "shortsword"),
    ("xiphos", "shortsword"), ("shortsword", "shortsword"), ("dagger", "dagger"), ("knife", "dagger"), ("dirk", "dagger"), ("stiletto", "dagger"),
    ("cleaver", "cleaver"), ("trident", "trident"), ("glaive", "glaive"), ("halberd", "glaive"), ("naginata", "glaive"), ("spear", "spear"),
    ("lance", "spear"), ("pike", "spear"), ("staff", "staff"), ("wand", "wand"), ("whip", "whip"), ("crossbow", "crossbow"), ("longbow", "bow"), ("shortbow", "bow"), ("bow", "crossbow"),
    ("greathammer", "greathammer"), ("great_hammer", "greathammer"), ("hammer", "hammer"), ("maul", "hammer"), ("mace", "mace"), ("club", "club"), ("shield", "shield"), ("sword", "longsword"), ("blade", "longsword"),
]
STATE_SUFFIX = re.compile(r"(_(pull[123]|charged|spent|genesis|5leaf|liquid|off|blue|left)|(?<=lantern)_red)$")


def base_id(item_id):
    """The id of the rest mesh a state mesh (bianca_pull2, needler_charged ...) belongs to."""
    return STATE_SUFFIX.sub("", item_id)


def guess_class(item_id):
    """The keyword guess (ordered; first hit wins); 'item' when nothing matches."""
    for key, cls in KEYWORDS:
        if key in item_id:
            return cls
    return "item"


def load(path=CLASSES_JSON):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def class_of(item_id, data):
    """The reviewed class of an id (state meshes inherit from their base id), else the keyword guess."""
    w = data.get("weapons", {})
    for i in (item_id, base_id(item_id)):
        if i in w:
            return w[i]["class"] if isinstance(w[i], dict) else w[i]
    return guess_class(item_id)


def is_item_class(cls):
    return cls in ITEM_CLASSES


def table(data=None):
    """name -> dict(min, max, head_min, grip) from the json if it has a table, else the built-in one."""
    if data and "classes" in data:
        return data["classes"]
    return {k: {"min": v[0], "max": v[1], "head_min": v[2], "grip": v[3]} for k, v in CLASSES.items()}
