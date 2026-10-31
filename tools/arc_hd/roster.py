"""Which kit characters the HD arc pass owns (docs/arc/hd_arc.md).

Owner decision (3.39): the Seven Deadly Sins and the Black Clover characters become HD (256x256 skins, the same UV layout as the 64x64 ones); DanMachi, the generic NPCs, the merchants and the
other franchises are NOT touched. The ids below are the look names of assets/nusmp/playermodel/looks.json (a second form is `<id>_form2`, a Dark Disciple devil is `dark_disciple_<devil>`).

Franchise source (CLAUDE.md rule 10): the repo's own franchise map, docs/arc/sdsa.md and docs/arc/midnight_sun.md (the Seven Deadly Sins arcs and the Eye of the Midnight Sun arc) and
docs/arc/npcb.md (the Black Clover knights and the Zogratis family), which in turn cite the Black Clover and Seven Deadly Sins wikis. No wiki page was fetched for this tool.
"""
import sys

sys.dont_write_bytecode = True

SDS = ["meliodas", "diane", "ban", "king", "gowther", "merlin", "escanor"]

BLACK_CLOVER = [
    "asta", "yuno", "noelle_silva", "yami_sukehiro", "mimosa_vermillion", "nozel_silva", "luck_voltia", "magna_swing", "finral_roulacase", "charmy_pappitson", "gordon_agrippa",
    "vanessa_enoteca", "klaus_lunettes", "secre_swallowtail", "zora_ideale", "gauche_adlai", "marx_francois", "solid_silva", "fuegoleon_vermillion", "mereoleona_vermillion",
    "leopold_vermillion", "julius_novachrono", "william_vangeance", "nacht_faust",
    # the Zogratis family and the Eye of the Midnight Sun
    "dante_zogratis", "vanica_zogratis", "zenon_zogratis", "lucius_zogratis", "fana", "heath_grice", "lotus_whomalt", "morris_libardirt", "patolli", "rades_spirito", "revchi_salik",
    "rhya", "mars", "lucifero", "udaeus", "vetto", "zagred", "zagred_legacy",
    # the Dark Disciple and its four devils, the Devil Avatar and its Megicula skin
    "dark_disciple", "dark_disciple_astaroth", "dark_disciple_beelzebub", "dark_disciple_lucifero", "dark_disciple_megicula", "devil_avatar", "devil_avatar_megicula",
]

# Black Clover NPCs that the kit also draws but that are NOT in the owner's list for this pass (docs/arc/npcb.md): the pipeline handles them with the same command, the integrator decides.
BLACK_CLOVER_NOT_LISTED = ["charlotte_roselei", "kahono", "kiato", "dorothy_unsworth", "jack_the_ripper", "grey", "black_market_witch"]

# never touched by this pass: DanMachi, the generic and hub NPCs, the Order god and the painted knight, the tower wizard
UNTOUCHED = ["ais_wallenstein", "bell_cranel", "eina_tulle", "hestia", "liliruca_arde", "welf_crozzo", "npc_default", "merchant_npc", "quest_giver_npc", "god_of_order", "painted_knight",
             "tower_wizard"]

BASE_IDS = SDS + BLACK_CLOVER


def franchise(name):
    """'sds', 'bc' or None for a look name (a `_form2` follows its base id)."""
    base = name[:-6] if name.endswith("_form2") else name
    if base in SDS:
        return "sds"
    if base in BLACK_CLOVER:
        return "bc"
    return None


def expand(ids, table_names):
    """Base ids -> look names: an id plus its `_form2` when the look table has one. `table_names` = every look name that exists."""
    out = []
    for i in ids:
        for n in (i, i + "_form2"):
            if n in table_names and n not in out:
                out.append(n)
    return out


if __name__ == "__main__":
    print(len(SDS), "SDS,", len(BLACK_CLOVER), "Black Clover base ids")
