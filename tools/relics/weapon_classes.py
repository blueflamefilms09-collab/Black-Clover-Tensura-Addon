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
CLASSES = {
    "dagger":     (0.35, 0.55, 0.0, 0.20),
    "shortsword": (0.80, 1.00, 0.0, 0.20),
    "longsword":  (1.10, 1.30, 0.0, 0.20),
    "rapier":     (1.10, 1.30, 0.0, 0.20),
    "katana":     (1.10, 1.30, 0.0, 0.20),
    "sabre":      (1.00, 1.25, 0.0, 0.20),
    "dadao":      (1.20, 1.45, 0.0, 0.20),
    "greatsword": (1.50, 1.90, 0.0, 0.22),
    "handaxe":    (0.70, 0.90, 0.30, 0.25),
    "battleaxe":  (1.10, 1.30, 0.45, 0.25),
    "greataxe":   (1.60, 2.00, 0.70, 0.28),
    "crescent":   (1.20, 1.50, 0.50, 0.25),
    "scythe":     (1.50, 1.90, 0.50, 0.30),
    "cleaver":    (0.55, 0.80, 0.25, 0.25),
    "spear":      (1.80, 2.40, 0.0, 0.33),
    "glaive":     (1.80, 2.40, 0.0, 0.33),
    "trident":    (1.80, 2.40, 0.0, 0.33),
    "staff":      (1.60, 2.00, 0.0, 0.40),
    "whip":       (1.50, 2.00, 0.0, 0.15),
    "coilwhip":   (0.90, 1.40, 0.0, 0.15),                  # a whip held coiled or with a short stock (the viper whip, the junglethorn vine)
    "crossbow":   (0.70, 0.95, 0.0, 0.50),
    "hammer":     (1.10, 1.50, 0.40, 0.25),
    "mace":       (0.80, 1.10, 0.0, 0.25),
    "club":       (0.90, 1.40, 0.0, 0.25),
    "wand":       (0.35, 0.55, 0.0, 0.20),
    "shield":     (0.70, 1.10, 0.0, 0.50),
    "item":       (0.0, 9.0, 0.0, 0.50),                  # a held trinket / book / charm: only `ITEM_MIN_EXTENT` applies
}
ITEM_MIN_EXTENT = 0.40                                    # blocks: the largest apparent dimension of a held item is never below this
OVERSIZE_FACTOR = 1.5                                     # only a weapon longer than this times `max` is reported as too big (never auto-shrunk)

# ordered keyword guess for ids the table does not know (the Java fallback WeaponClasses.guess has the same list, in the same order)
KEYWORDS = [
    ("greataxe", "greataxe"), ("great_axe", "greataxe"), ("handaxe", "handaxe"), ("hand_axe", "handaxe"), ("crescent", "crescent"),
    ("scythe", "scythe"), ("axe", "battleaxe"), ("greatsword", "greatsword"), ("katana", "katana"), ("dadao", "dadao"), ("sabre", "sabre"),
    ("saber", "sabre"), ("scimitar", "sabre"), ("rapier", "rapier"), ("longsword", "longsword"), ("gladius", "shortsword"),
    ("xiphos", "shortsword"), ("shortsword", "shortsword"), ("dagger", "dagger"), ("knife", "dagger"), ("dirk", "dagger"), ("stiletto", "dagger"),
    ("cleaver", "cleaver"), ("trident", "trident"), ("glaive", "glaive"), ("halberd", "glaive"), ("naginata", "glaive"), ("spear", "spear"),
    ("lance", "spear"), ("pike", "spear"), ("staff", "staff"), ("wand", "wand"), ("whip", "whip"), ("crossbow", "crossbow"), ("bow", "crossbow"),
    ("hammer", "hammer"), ("maul", "hammer"), ("mace", "mace"), ("club", "club"), ("shield", "shield"), ("sword", "longsword"), ("blade", "longsword"),
]
STATE_SUFFIX = re.compile(r"(_(pull[123]|charged|spent|genesis|5leaf|liquid|off|blue)|(?<=lantern)_red)$")


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


def table(data=None):
    """name -> dict(min, max, head_min, grip) from the json if it has a table, else the built-in one."""
    if data and "classes" in data:
        return data["classes"]
    return {k: {"min": v[0], "max": v[1], "head_min": v[2], "grip": v[3]} for k, v in CLASSES.items()}
