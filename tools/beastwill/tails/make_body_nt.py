"""Why (docs/beastwill_tails.md): the Kraken's and Scylla's body pieces carry their own cube tentacles (bones tent*, waist*). When the Blender tentacles are worn, this writes the same body piece
WITHOUT those bones: bwf_<stem>_arm_body_nt (geo + animation; the animation keeps only the tracks of the bones left, GeoAssetsTest wants every track to name a real bone). The original files are not touched. Run from the repo root."""
import json, os, sys

RES = os.path.join("src", "main", "resources", "assets", "anime_multiverse")
DROP = {"kraken": ("tent",), "scylla_charybdis": ("waist",)}


def main():
    for stem, prefixes in DROP.items():
        src = os.path.join(RES, "geo", "entity", f"bwf_{stem}_arm_body.geo.json")
        d = json.load(open(src))
        g = d["minecraft:geometry"][0]
        before = len(g["bones"])
        g["bones"] = [b for b in g["bones"] if not b["name"].startswith(prefixes)]
        g["description"]["identifier"] = f"geometry.bwf_{stem}_arm_body_nt"
        with open(os.path.join(RES, "geo", "entity", f"bwf_{stem}_arm_body_nt.geo.json"), "w") as f:
            json.dump(d, f, indent=1)
        a = json.load(open(os.path.join(RES, "animations", "entity", f"bwf_{stem}_arm_body.animation.json")))
        keep = {b["name"] for b in g["bones"]}
        for clip in a["animations"].values():
            if "bones" in clip:
                clip["bones"] = {k: v for k, v in clip["bones"].items() if k in keep}
        with open(os.path.join(RES, "animations", "entity", f"bwf_{stem}_arm_body_nt.animation.json"), "w") as f:
            json.dump(a, f, indent=1)
        print(stem, "bones", before, "->", len(g["bones"]))


if __name__ == "__main__":
    sys.exit(main())
