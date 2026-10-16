"""Prints the computed list for docs/relic_hand_models.md: for every relic, what it was in the hand before 1.48.0 and what it is now.

    python3 tools/relics/hand_report.py            (markdown to stdout)

"Was": a weapon had its mesh (weapon_meshes), the relics listed in hand_models_before.txt (head and feet armour) had the 1.34.0 object form, every other relic's item json was a flat
`item/generated` sprite (the list was read off the item jsons before gen_hand_item_models.py converted them). "Now" is the hand catalog (a signature hand model), else the worn library (the models it wears), with the hold style.
"""
import collections
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import hand_preview as H  # noqa: E402
import relic_inventory as RI  # noqa: E402


def main():
    rs, _ = RI.relics()
    old3d = {l.strip() for l in open(os.path.join(HERE, "hand_models_before.txt"), encoding="utf8") if l.strip() and not l.startswith("#")}
    c = H.catalogs()
    rows, count = [], collections.Counter()
    for r in sorted(rs, key=lambda r: (r["slot"], r["id"])):
        if r["slot"] == "MAIN_HAND":
            count["weapon (already 3D)", r["slot"]] += 1
            continue
        if r["adopted"]:
            count["older item (already 3D)", r["slot"]] += 1
            continue
        pcs, src = H.pieces_of(r["id"], r["slot"], r["look"])
        (fit, xfs), style = c["styles"].style_for(r["id"], r["slot"], r["look"])
        was = "1.34.0 object form" if r["id"] in old3d else "flat sprite"
        now = ", ".join(os.path.basename(p[0])[:-4] + ("@" + p[1] if p[1] else "") + ("~" if p[2] else "") for p in pcs)
        kind = "signature hand model" if src == "sig" else "worn model" if src == "worn" else "NONE"
        count[was + " -> " + kind, r["slot"]] += 1
        rows.append((r["slot"], r["id"], r["name"], was, kind, now, style, r["grade"]))
    print("| slot | relic id | name | was | now | models | hold style |")
    print("|---|---|---|---|---|---|---|")
    for slot, i, name, was, kind, now, style, grade in rows:
        print("| %s | `%s` | %s | %s | %s | %s | %s |" % (slot, i, name.replace("|", "/"), was, kind, now, style))
    print()
    print("<!-- counts -->")
    tot = collections.Counter()
    for (k, slot), n in sorted(count.items()):
        print("%s | %s | %d" % (k, slot, n))
        tot[k] += n
    print(dict(tot))


if __name__ == "__main__":
    main()
