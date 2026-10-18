"""Why: the 200 mobs of the HOLY road of the Mega-Dungeon (floors +1..+1000), 40 for each of the five tiers (tier t = floors 200(t-1)+1 .. 200t, DndRules.tierOf). Radiant
hosts, knightly orders, guild adventurers, fairy courts, beastfolk of the wilds, celestial constructs: the Holy road is the climb, so even its monsters wear the light.
One row per line, eight fields separated by ' | ' (format and every token: tools/foundry/roster_common.py and docs/labyrinth/foundry.md):

    Name | faction | build | palette (3 colour names) | source | look tokens | archetype | abilities

The `source` is the series or D&D monster the DESIGN is inspired by; names and looks are the mod's own, never a copied character. Rows are appended, never rewritten.
"""

TIERS = {}

TIERS[1] = """
Tunnel Kobold | beast | small | rust sand iron | D&D: kobold | skin=red ears=pointed horns=small tail=lizard hair=crop/ember face=grin/amber top=vest/-/leather legs=shorts boots=bare prop=dagger | skirmisher | dash swarm_call
Hearth Sprite | elemental | child | flame honey ember | Fairy Tail | skin=fair ears=elf wings=fairy hair=twin_tails/flame face=cheerful/amber top=dress/thigh legs=shorts boots=shoes prop=wand glow=sunfire | caster | fire_bolt mending_light
Dawn Squire | construct | average | white gold sky | Black Clover | skin=light hair=short/wheat face=stern/azure top=tabard/knee/plate armor=pauldrons+gorget cape=cape prop=sword off=shield | tank | shield_bash shield_wall
Greenwood Archer | beast | slim | moss leather sage | Dungeon Meshi | skin=tan hair=ponytail/chestnut face=calm/jade top=tunic/thigh cape=hooded_cloak/hood prop=bow acc=bandana+quiver | archer | volley hunters_mark
Straw Warden | construct | lanky | wheat leather ember | D&D: scarecrow | skin=olive mask=full/sand hair=messy/wheat face=dead/ember top=tunic/knee/cloth/patches ragged sleeves=torn legs=baggy boots=bare gloves=wraps prop=scythe acc=pipe | brute | cleave thorn_skin
Dewdrop Slimeling | slime | child | aqua mint pearl | Tensura | skin=aqua hair=bob/aqua face=cheerful/pearl/blush top=dress/thigh/-/dots legs=bare boots=bare ears=fin acc=ribbon | support | mending_light regrowth
Chapel Novice | cultist | average | cream sky gold | D&D: cleric | skin=fair hair=bob/chestnut face=shy/azure top=robe/knee/-/-/cross neck=necklace belt=plain prop=mace | support | heal_pulse ward_allies
Spore Forager | beast | small | red cream moss | Dungeon Meshi | skin=pale hair=afro/red face=cheerful/lemon/freckles top=vest/-/leather legs=shorts boots=shoes acc=satchel+flower prop=dagger | skirmisher | pounce acid_spit
Wisp Lantern Fae | elemental | child | lumen ice mint | Spirited Away | skin=pearl wings=fairy hair=twin_tails/lumen face=calm/aether top=dress/hip legs=bare boots=bare halo=ring prop=orb glow=aether | caster | holy_lance mending_light
Tin Footman | construct | average | pewter bronze brass | Fullmetal Alchemist | skin=grey mask=visor/pewter hair=bald top=uniform/hip/plate armor=pauldrons+gauntlets/plate legs=trousers boots=greaves prop=spear idle=guard | brute | lunge shield_bash
Clover Page | cultist | slim | forest white lime | Black Clover | skin=light hair=spiky/lime face=cheerful/emerald top=tabard/thigh/-/-/clover legs=shorts boots=boots/ankle acc=headband prop=wand | caster | fire_bolt dash
Marsh Reed Piper | aquatic | slim | teal sage sand | Princess Mononoke | skin=green ears=fin hair=long/sage face=calm/teal/freckles top=tunic/knee/-/stripes legs=leggings boots=sandals prop=staff acc=flower | support | root regrowth
Boar-Tusk Hunter | beast | broad | brown leather bone | Monster Hunter | skin=tan hair=mohawk/umber face=angry/amber/war_paint top=vest/-/fur armor=pauldrons/bone horns=small/bone legs=chaps boots=boots/knee prop=axe | brute | enrage cleave
Hedge Witch Apprentice | cultist | slim | grape moss amber | D&D: druid | skin=olive hair=braid/umber face=smirk/jade top=robe/ankle/-/patches acc=witch_hat+satchel boots=shoes prop=staff | caster | root fire_bolt
Candle Chorister | cultist | average | ivory gold dawn | Frieren | skin=fair hair=long_wavy/halo face=calm/sky top=robe/floor/-/-/sun halo=ring neck=necklace prop=grimoire cast=pray glow=lumen | support | mending_light radiant_aura
Pebble Sentry | construct | small | granite moss iron | D&D: animated armor | skin=grey mask=full/granite top=tunic/thigh/plate armor=helm+bracers/plate legs=shorts boots=greaves prop=hammer | tank | stoneskin ground_slam
Bramble Pup | beast | child | fern brown sand | Frieren | skin=tan ears=wolf tail=wolf hair=shaggy/brown face=cheerful/amber top=vest/hip/fur legs=shorts boots=bare gloves=fingerless | skirmisher | pounce call_wolves
Dawn Gate Cadet | construct | average | white navy gold | Fate | skin=light hair=side_part/gold face=stern/azure top=uniform/thigh/-/-/star legs=trousers boots=boots/knee gloves=gloves prop=katana cape=cape | skirmisher | lunge shield_wall
Honey Bee Courier | insect | small | honey ink lemon | Made in Abyss | skin=fair wings=fairy/honey hair=bob/ink face=cheerful/lemon top=jumpsuit/-/leather/stripes legs=bare boots=shoes acc=goggles+satchel prop=dagger | flier | sky_dive swarm_call
Cairn Dwarf Miner | ogre | small | umber iron copper | D&D: dwarf | skin=tan hair=bald face=stern/amber/beard top=vest/-/leather neck=belts legs=trousers boots=boots/calf gloves=gloves acc=goggles prop=hammer | brute | ground_slam troll_blood
Kite Goblin Scout | goblin | small | olive rust bone | Goblin Slayer | skin=green ears=pointed hair=bald face=sinister/amber/scar_cheek_l top=vest/-/leather legs=shorts boots=bare acc=bandana prop=dagger | assassin | shadow_step lunge
Fen Frog Acolyte | aquatic | small | swamp lime bone | Konosuba | skin=green ears=fin hair=bald face=cheerful/lemon eyes=big top=robe/knee/-/dots acc=circlet neck=strap legs=bare boots=bare prop=wand | caster | acid_spit mending_light
Sunmote Beetle Guard | insect | broad | bronze amber gold | D&D: giant beetle | skin=olive horns=single/bronze armor=pauldrons+breastplate+greaves/scale top=bare legs=baggy boots=greaves face=angry/amber prop=spear | charger | pounce ground_slam
Hall Brawler Monk | cultist | broad | rust cream saffron | Fairy Tail | skin=tan hair=pompadour/brown face=grin/amber/scar_diag top=vest/-/-/-/flame legs=hakama boots=geta gloves=wraps bandage=forearm_l+forearm_r | brute | whirlwind dash
Briar Rose Duelist | beast | slim | rose moss white | Re:Zero | skin=pale hair=drills/rose face=smirk/rose/lipstick top=dress/knee/-/-/heart neck=choker boots=boots/knee prop=sword lashes | skirmisher | thorn_skin lunge
Pale Fawn Messenger | beast | lanky | cream sage white | Princess Mononoke | skin=pale horns=antlers/cream ears=elf hair=long/cream face=calm/spirit top=robe/ankle/-/-/tree legs=bare boots=bare tail=cat/cream | support | regrowth ward_allies
Salt Bastion Sentinel | construct | huge | pewter sand white | Dark Souls | skin=grey top=tabard/knee/plate/-/crest armor=helm+pauldrons+breastplate/plate legs=trousers boots=sabatons cape=tattered/long prop=hammer off=shield | tank | bulwark shield_bash
Ink Cap Mage | elemental | slim | ink violet silver | Frieren | skin=ashen hair=wizard/silver face=cold/violet top=robe/floor/-/runes acc=wizard_hat boots=shoes prop=grimoire | caster | ice_bind shadow_bolt
Thistle Bard Lad | cultist | average | violet gold ivory | D&D: bard | skin=light hair=curtains/chestnut face=smirk/violet top=coat/knee/-/stripes acc=beret+earrings boots=boots/knee prop=wand | support | war_cry mending_light
Ember Fox Kit | beast | child | ember cream gold | Naruto | skin=fair ears=fox tail=fox hair=spiky/ember face=grin/amber/cheek_lines top=jumpsuit/-/-/stripes legs=baggy boots=bare prop=dagger | assassin | shadow_dash pounce
Cobblestone Gargoyle Pup | construct | small | granite smoke bone | D&D: gargoyle | skin=grey wings=bat/granite horns=small/granite tail=devil hair=bald face=angry/amber top=bare legs=shorts boots=bare | flier | sky_dive stoneskin
Wren Song Herald | beast | slim | sky white cream | Made in Abyss | skin=pale wings=feathered/white hair=ponytail/sky face=cheerful/sky top=tunic/thigh/-/-/wings legs=leggings boots=sandals acc=ribbon | flier | sky_dive heal_pulse
Gilded Beetle Knight | insect | average | gold bronze white | Overlord | skin=olive top=tabard/knee/scale armor=helm+pauldrons+gauntlets/scale horns=single/gold cape=cape prop=sword off=shield | tank | shield_wall cleave
Kiln Pot Golem | construct | broad | clay terracotta honey | Fullmetal Alchemist | skin=clay mask=full/clay hair=bald top=bare armor=pauldrons+bracers/plate legs=baggy boots=bare prop=hammer | brute | ground_slam enrage
Bellflower Maid Golem | construct | slim | lavender white gold | Re:Zero | skin=ivory mask=veil/white hair=twin_buns/lavender face=calm/violet lashes top=dress/ankle/-/-/heart acc=ribbon neck=choker boots=shoes prop=dagger | assassin | shadow_step blink
Rushlight Hunter | cultist | lanky | umber sand ember | Goblin Slayer | skin=tan mask=half/umber hair=undercut/umber face=cold/ember top=coat/calf/leather armor=pauldrons/leather cape=hooded_cloak/hood boots=boots/knee prop=bow | archer | triple_shot hunters_mark
Stone Pillar Wrestler | ogre | huge | granite saffron brick | Record of Ragnarok | skin=brown hair=bald face=grin/amber/scar_brow_l top=bare legs=hakama boots=bare gloves=wraps bandage=chest horns=bull/bone | charger | leap_slam enrage
Quill Archivist | cultist | child | cream ink gold | Dungeon Meshi | skin=fair hair=bob/ink face=shy/amber top=robe/knee/-/-/eye acc=glasses+satchel boots=shoes prop=grimoire | summoner | conjure_imps ward_allies
Seed Pod Dryad | elemental | slim | fern lime moss | D&D: dryad | skin=green hair=long_wavy/lime ears=elf face=calm/jade/freckles top=dress/knee/-/-/tree legs=bare boots=bare acc=flower | support | regrowth root
Soot Smith Apprentice | ogre | average | charcoal copper ember | D&D: duergar | skin=ashen hair=crop/ink face=stern/ember top=tank/-/leather neck=strap belt=double+pouches legs=trousers boots=boots/calf gloves=gauntlets prop=axe | brute | cleave burning_aura
Morning Dew Naiad | aquatic | slim | aqua pearl cerulean | Frieren | skin=pearl ears=fin hair=long/aqua face=calm/aqua top=dress/floor/-/-/moon legs=bare boots=bare acc=tiara prop=trident | caster | ice_bind mending_light
Crumb Trail Dragonet | beast | child | bronze honey ember | D&D: pseudodragon | skin=bronze tail=dragon/bronze wings=dragon/bronze horns=small/gold hair=bald face=cheerful/amber top=bare legs=shorts boots=bare | flier | fire_bolt sky_dive
"""
