"""Why: the owner asked for the dungeon's humanoids to be distinct people with their own HD skins ("goblin, hobgoblin, orc, orc chief, kobold, bugbear, skeleton knight, skeleton archer, ghoul,
lich, necromancer, death knight, cultist, priest, paladin, angelic knight, demon knight, imp, warlock, witch, bandit, mercenary, guard, merchant, quest giver, guildmaster, dwarf smith, elf ranger ...").
These are those people: 56 ARCHETYPES, each written as ONE LINE in the language of the Mob Foundry's roster (tools/foundry/roster_common.py: `Name | faction | build | three palette colours |
source | look tokens | combat archetype | wear recipe`), so the same art kit, the same painter and the same vocabulary build them, and anyone who can read a foundry row can read these. The eighth
field is not abilities here but the WEAR RECIPE (tools/dungeon_skins/recipes.py: grime, scars, rot, tears, rib cage, cracks ...). Three tokens are added to the foundry's: `bigears` (the long ears
of a goblin), `tusks=orc|boar|small` and `beard=dwarf|short`, `snout=reptile` (3D parts of the player model, tools/dungeon_skins/parts.py) because the art kit draws those on the head box and the
player model's head has to wear them as parts.

Each archetype becomes a look `dg_<id>` (assets/nusmp/playermodel/looks_dungeon.json) with its own skin `textures/entity/playermodel/dg_<id>.png` (+ `_glow`), its own hair, its extras (horns, wings,
tails, halos, capes from the art kit's model of the row; ears, tusks, beards, snouts from the parts library) and a stance. The Foundry mobs that are goblins, orcs, undead, demons and so on take the
same recipes and the same face / hair defaults of their family when their own row does not say otherwise (foundry.py), which is what makes a goblin raid look like one tribe.
"""
import roster_common as RC

ROWS = """
# ---- the green and the grey: goblins, orcs, ogres, kobolds
Goblin Scrapper | goblin | small | olive rust bone | D&D: goblin | skin=green bigears hair=bald face=mad/amber/fang_marks nose=sharp mouth=grin top=vest/hip/leather ragged legs=shorts boots=bare gloves=wraps belt=plain+pouches acc=goggles prop=dagger | skirmisher | goblin
Goblin Shaman | goblin | small | moss ink bone | D&D: goblin shaman | skin=green bigears hair=wild_mane/ash face=sinister/lumen/third_eye nose=sharp top=robe/knee/cloth/runes neck=necklace belt=plain+pouches boots=bare prop=staff glow=spirit eglow | caster | goblin
Goblin Archer | goblin | small | swamp leather bone | D&D: goblin | skin=green bigears hair=hood/swamp face=cold/amber nose=sharp top=tunic/thigh/leather legs=baggy boots=sandals gloves=fingerless cape=hooded_cloak/ragged/hood acc=quiver prop=bow | archer | goblin
Hobgoblin Legionnaire | goblin | broad | rust iron bone | D&D: hobgoblin | skin=red pointed hair=crop/ink face=stern/amber/scar_diag top=tabard/knee/chain legs=trousers boots=boots/calf gloves=gauntlets armor=helm+breastplate+pauldrons+greaves/plate prop=sword off=shield belt=double | tank | orc
Orc Raider | ogre | broad | moss iron bone | D&D: orc | skin=green tusks=orc hair=mohawk/ink face=angry/amber/war_paint nose=wide top=vest/hip/leather legs=baggy boots=sandals gloves=wraps armor=pauldrons+bracers/leather prop=axe belt=plain | brute | orc
Orc Warchief | ogre | huge | crimson iron gold | D&D: orc war chief | skin=olive tusks=orc hair=wild_mane/ash face=angry/amber/scar_cheek_l+war_paint nose=wide top=bare legs=baggy boots=sabatons gloves=gauntlets armor=helm+breastplate+pauldrons+gauntlets/plate cape=mantle/long prop=greatsword belt=double | brute | orc
Kobold Trapper | beast | small | rust sand iron | D&D: kobold | skin=red snout=reptile horns=small/bone tail=lizard/rust hair=bald face=sinister/amber nose=none top=vest/hip/leather boots=sandals gloves=wraps acc=goggles prop=dagger | skirmisher | goblin
Bugbear Brute | goblin | broad | umber leather bone | D&D: bugbear | skin=brown tusks=small hair=shaggy/umber face=mad/amber/scar_brow_l nose=wide top=vest/hip/fur legs=baggy boots=sandals gloves=wraps armor=pauldrons/leather neck=fur prop=mace | brute | orc
Gnoll Hunter | beast | lanky | umber sand bone | D&D: gnoll | skin=tan ears=wolf tail=wolf/rust hair=wild_mane/umber face=mad/amber/fang_marks mouth=fangs nose=wide top=vest/hip/leather legs=shorts boots=bare gloves=wraps prop=spear | skirmisher | beastman
Cave Troll | ogre | huge | moss mud bone | D&D: troll | skin=green hair=bald face=dead/amber nose=wide mouth=fangs top=bare legs=shorts boots=bare gloves=wraps bandage=chest prop=hammer | brute | troll
Hill Ogre | ogre | huge | tan leather bone | D&D: ogre | skin=olive hair=shaggy/umber face=mad/ink nose=wide top=tunic/thigh/fur ragged legs=baggy boots=sandals neck=fur prop=mace | brute | orc
# ---- the dead
Skeleton Knight | undead | average | iron bone ash | D&D: skeleton | skin=undead mask=skull/bone hair=bald top=tabard/knee/chain ragged legs=trousers boots=greaves gloves=gauntlets armor=helm+breastplate+pauldrons+greaves/plate cape=tattered prop=sword off=shield glow=spirit eglow | tank | skeleton
Skeleton Archer | undead | lanky | bone ash rust | D&D: skeleton archer | skin=undead mask=skull/bone hair=bald top=vest/hip/cloth ragged legs=baggy boots=bare acc=quiver prop=bow glow=spirit eglow | archer | skeleton
Zombie Labourer | undead | average | mud olive rust | D&D: zombie | skin=undead hair=shaggy/mud face=dead/ash/stitches top=tunic/thigh/cloth ragged sleeves=torn legs=baggy boots=bare | brute | zombie
Ghoul | undead | lanky | ash mud bone | D&D: ghoul | skin=grey hair=shaggy/ash face=mad/amber/veins mouth=fangs nose=none top=bare legs=baggy boots=bare gloves=wraps bandage=chest | assassin | ghoul
Wight Sentinel | undead | broad | slate ink silver | D&D: wight | skin=ashen hair=bald face=cold/aether/cracks top=tabard/knee/chain legs=trousers boots=greaves gloves=gauntlets armor=helm+breastplate+pauldrons/plate cape=cape prop=sword glow=aether eglow | tank | skeleton
Wraith | undead | lanky | void ink spirit | D&D: wraith | skin=ashen hair=hood/ink face=cold/spirit top=robe/floor/cloth ragged legs=bare boots=bare cape=hooded_cloak/ragged/hood/long mask=veil/ink prop=scythe glow=spirit eglow wings=fairy/smoke | assassin | spectre
Banshee | undead | slim | pearl fog spirit | D&D: banshee | skin=pale hair=long_wavy/white face=sad/spirit/tear_l+tear_r top=dress/floor/cloth ragged legs=bare boots=bare glow=spirit eglow | caster | spectre
Vampire Noble | undead | average | wine ink gold | D&D: vampire | skin=pale hair=slick/ink face=smirk/crimson/stubble mouth=fangs nose=sharp top=coat/knee/cloth legs=trousers boots=boots/calf gloves=gloves cape=cape/long neck=cravat prop=sword | assassin | spectre
Lich | undead | lanky | indigo bone gold | D&D: lich | skin=undead mask=skull/bone hair=bald acc=crown crown3d top=robe/floor/cloth/runes legs=bare boots=bare gloves=wraps neck=necklace prop=staff glow=voidlight eglow | caster | lich
Necromancer | culti | slim | ink grape bone | D&D: necromancer | skin=pale hair=hood/ink face=cold/voidlight top=robe/floor/cloth/runes sleeves=wide legs=bare boots=bare neck=belts belt=plain+books prop=orb off=grimoire glow=voidlight | caster | cultist
Death Knight | undead | broad | ink iron crimson | D&D: death knight | skin=undead mask=full/ink hair=bald top=tabard/knee/plate legs=trousers boots=sabatons gloves=gauntlets armor=helm+breastplate+pauldrons+gauntlets+greaves/plate cape=cape/long horns=bull/iron prop=greatsword glow=hellfire eglow aglow | tank | knight
Mummy | undead | average | sand bone rust | D&D: mummy | skin=tan hair=bald face=dead/amber top=bare legs=baggy boots=bare bandage=chest+arm_l+arm_r+leg_l+leg_r neck=necklace | brute | mummy
# ---- the pit
Imp | demon | small | crimson soot gold | D&D: imp | skin=red horns=small/bone wings=bat/ember tail=devil/crimson hair=bald face=sinister/amber/fang_marks nose=sharp top=bare legs=shorts boots=bare prop=dagger | skirmisher | imp
Demon Knight | demon | broad | ink crimson gold | D&D: pit fiend | skin=red horns=bull/bone wings=bat/ink tail=devil/crimson hair=bald face=angry/amber top=tabard/knee/plate legs=trousers boots=sabatons gloves=gauntlets armor=helm+breastplate+pauldrons+greaves/plate cape=tattered prop=greatsword glow=hellfire eglow | brute | demon
Succubus Broker | demon | slim | wine ink gold | D&D: succubus | skin=pale horns=devil/gold wings=bat/wine tail=devil/wine hair=long_wavy/ink face=smirk/crimson/lipstick+mole lashes top=dress/floor/cloth hemtrim neck=choker boots=bare prop=wand | caster | demon
Warlock | culti | slim | grape ink amber | D&D: warlock | skin=fair hair=slick/ink face=cold/amber/third_eye top=coat/knee/cloth/runes sleeves=wide legs=trousers boots=boots/calf gloves=gloves cape=mantle neck=necklace prop=orb off=grimoire glow=voidlight | caster | cultist
Cultist | culti | average | ink wine bone | D&D: cultist | skin=pale hair=hood/ink face=cold/ink top=robe/floor/cloth/runes sleeves=wide legs=bare boots=bare mask=veil/ink neck=belts prop=dagger | skirmisher | cultist
Dark Priest | culti | average | ink blood gold | D&D: acolyte of evil | skin=ashen hair=bald face=sinister/crimson/cracks top=robe/floor/cloth/runes legs=bare boots=sandals neck=necklace cape=mantle prop=mace glow=bloodlight | support | cultist
Plague Doctor | culti | slim | ink bone ash | Bloodborne | skin=pale hair=hood/ink mask=plague/bone top=coat/knee/leather legs=trousers boots=boots/calf gloves=gloves cape=cape neck=belts acc=satchel prop=scythe | support | cultist
Executioner | culti | huge | ink blood iron | D&D: executioner | skin=grey mask=full/ink hair=hood/ink top=bare legs=baggy boots=boots/calf gloves=gauntlets armor=pauldrons+bracers/plate prop=axe | brute | knight
# ---- the light
Priest | culti | average | white gold sky | D&D: priest | skin=fair hair=short/ash face=calm/sky top=robe/floor/cloth hemtrim sleeves=wide legs=bare boots=shoes neck=necklace cape=mantle prop=staff glow=lumen | support | priest
Cleric | culti | average | sky iron gold | D&D: cleric | skin=light hair=short/chestnut face=stern/sky top=tabard/knee/chain legs=trousers boots=boots/calf gloves=gloves armor=helm+pauldrons/chain prop=mace off=shield | support | priest
Paladin | const | broad | platinum gold azure | D&D: paladin | skin=light hair=crop/gold face=stern/azure top=tabard/knee/plate legs=trousers boots=sabatons gloves=gauntlets armor=helm+breastplate+pauldrons+bracers+greaves/plate cape=mantle/long prop=sword off=shield glow=lumen | tank | paladin
Angelic Knight | eleme | broad | white gold sky | D&D: deva | skin=pearl wings=angel/white halo=ring hair=long/platinum face=calm/lumen top=tabard/knee/plate legs=trousers boots=greaves gloves=gauntlets armor=helm+breastplate+pauldrons+greaves/plate cape=cape prop=spear glow=lumen eglow wglow | tank | holy
Monk | culti | average | ochre rust bone | D&D: monk | skin=tan hair=bald face=calm/amber top=vest/hip/cloth legs=baggy boots=sandals gloves=wraps belt=sash neck=necklace | skirmisher | peasant
# ---- the people of the dungeon
Witch | culti | slim | grape ink amber | D&D: hag | skin=olive hair=long/ash face=sinister/lumen/mole nose=sharp top=dress/floor/cloth/patches acc=witch_hat boots=shoes belt=plain+pouches prop=staff | caster | witch
Druid | beast | lanky | forest moss bone | D&D: druid | skin=olive hair=wild_mane/umber face=calm/lichen top=robe/knee/cloth/patches cape=hooded_cloak/hood neck=necklace boots=sandals horns=antlers/bone prop=staff | support | fey
Elf Ranger | beast | slim | forest leather gold | D&D: ranger | skin=fair ears=elf hair=long/chestnut face=calm/jade top=tunic/thigh/leather legs=leggings boots=boots/knee gloves=fingerless cape=hooded_cloak/hood acc=quiver prop=bow | archer | elf
Dwarf Smith | const | small | iron leather copper | D&D: dwarf | skin=tan beard=dwarf hair=wild_mane/copper face=stern/amber top=tabard/thigh/leather legs=trousers boots=boots/calf gloves=gauntlets belt=double+pouches prop=hammer | tank | dwarf
Bandit | culti | average | leather umber iron | D&D: bandit | skin=tan hair=hood/umber mask=half/umber face=cold/amber/stubble top=vest/hip/leather legs=trousers boots=boots/calf gloves=fingerless belt=double+pouches prop=dagger | skirmisher | peasant
Mercenary | culti | broad | steel leather rust | D&D: veteran | skin=tan hair=crop/umber face=stern/amber/scar_cheek_r top=tunic/thigh/chain legs=trousers boots=boots/calf gloves=gloves armor=pauldrons+bracers/chain cape=cape prop=sword off=dagger | brute | knight
Town Guard | const | average | navy iron gold | D&D: guard | skin=light hair=crop/chestnut face=stern/sky top=tabard/knee/chain legs=trousers boots=boots/calf gloves=gloves armor=helm+pauldrons/chain prop=spear off=shield | tank | knight
Merchant | culti | average | leather gold wine | D&D: noble | skin=tan hair=short/ink face=smirk/amber/moustache top=tunic/thigh/cloth hemtrim legs=trousers boots=boots/calf belt=double+pouches acc=satchel neck=necklace | support | peasant
Quest Giver | culti | average | wine gold bone | D&D: commoner | skin=light hair=side_part/chestnut face=calm/sky top=coat/knee/cloth hemtrim legs=trousers boots=boots/calf belt=plain acc=pocket_watch | support | peasant
Guildmaster | culti | broad | navy gold ink | D&D: guildmaster | skin=fair hair=undercut/ash face=stern/amber/scar_eye_l+beard acc=eyepatch top=coat/knee/cloth hemtrim legs=trousers boots=boots/calf gloves=gloves cape=mantle neck=cravat prop=greatsword belt=double | tank | knight
Assassin | culti | lanky | ink charcoal crimson | D&D: assassin | skin=pale hair=hood/ink mask=half/ink face=cold/crimson top=vest/hip/leather legs=leggings boots=boots/calf gloves=wraps cape=hooded_cloak/hood prop=dagger off=dagger | assassin | peasant
# ---- the rest of the dark: gargoyles, golems, ice, fire, sea, beast-folk
Gargoyle | const | broad | granite smoke bone | D&D: gargoyle | skin=grey wings=bat/granite horns=small/granite tail=devil/granite hair=bald face=angry/amber nose=wide top=bare legs=shorts boots=bare gloves=wraps | brute | stone
Stone Golem | const | huge | granite moss slate | D&D: stone golem | skin=grey hair=bald face=dead/amber nose=wide top=bare legs=shorts boots=bare gloves=none armor=pauldrons+breastplate/plate glow=sunfire eglow | tank | stone
Frost Revenant | undead | broad | ice frost steel | D&D: revenant | skin=blue hair=bald mask=half/ice face=cold/aether top=tabard/knee/plate legs=trousers boots=sabatons gloves=gauntlets armor=helm+breastplate+pauldrons+greaves/plate cape=tattered prop=greatsword glow=moonlight eglow | tank | ice
Pyromancer | eleme | slim | ember flame gold | Fairy Tail | skin=fair hair=spiky/ember face=smirk/sunfire top=coat/knee/cloth/runes legs=trousers boots=boots/calf gloves=fingerless prop=wand glow=sunfire | caster | fire
Sahuagin Warrior | aquatic | broad | teal deep bone | D&D: sahuagin | skin=green ears=fin tail=lizard/teal hair=bald face=angry/amber/fang_marks nose=none top=bare legs=shorts boots=bare gloves=wraps armor=pauldrons/scale prop=trident | brute | merfolk
Wolfkin Warrior | beast | broad | umber leather bone | D&D: werewolf | skin=tan ears=wolf tail=wolf/umber hair=wild_mane/umber face=angry/amber mouth=fangs top=vest/hip/fur legs=baggy boots=bare gloves=wraps armor=pauldrons/leather prop=axe | brute | beastman
Lizardfolk Spearman | beast | lanky | jade bone rust | D&D: lizardfolk | skin=green snout=reptile tail=lizard/jade horns=small/bone hair=bald face=cold/amber nose=none top=vest/hip/scale legs=shorts boots=bare gloves=wraps prop=spear | skirmisher | lizard
Harbinger of Ash | demon | lanky | soot ember bone | D&D: azer | skin=grey horns=devil/iron hair=bald face=sinister/hellfire/cracks top=robe/floor/cloth/runes legs=bare boots=bare gloves=gauntlets cape=tattered prop=staff glow=hellfire eglow | caster | fire
"""


def rows():
    """The parsed archetypes: dicts of the foundry's row format plus 'id' (dg_<slug>), 'wear' (the recipe name) and the raw 'line'."""
    out = []
    for n, line in enumerate(ROWS.strip().splitlines(), 1):
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        r = RC.parse_row(s, "holy", 1, "archetypes line %d" % n)
        r["wear"] = r["abilities"][0] if r["abilities"] else ""
        r["id"] = "dg_" + r["id"]
        r["line"] = s
        out.append(r)
    return out


def ids():
    return [r["id"] for r in rows()]
