"""Writes the shipped Anime Skill Codex of the Riven Remake boss: data/nusmp/riven_codex/*.json (run from the repo root)."""
import json, os

OUT = "src/main/resources/data/nusmp/riven_codex"
M = ["magic_damage"]
S = [
 # id, anime, name, tier, cast, range, primitives, counters, resisted_by, animation, line
 ("eldritch_blast","dnd","Eldritch Blast",1,9,24,["projectile","magic_damage"],["kiter","caster"],["anti_magic","magic_null"],"eldritch_blast","Two fingers. One bad idea."),
 ("hex","dnd","Hex",1,10,18,["slow","silence"],["caster","rusher"],["anti_magic"],"cast_grimoire","You look cursed. Let me help."),
 ("bardic_inspiration","dnd","Bardic Inspiration",1,16,0,["song_buff","heal"],[],[],"cast_song","Every story needs a good opening."),
 ("shadow_step","dnd","Shadow Step",2,7,18,["blink"],["kiter","flyer"],[],"shadow_step","Next page."),
 ("story_shield","fictional_remake","Story Manifestation: Bull-Crest Ward",2,10,6,["summon_construct","shield"],["rusher","melee"],[],"manifest_shield","Chapter two: you cannot reach me."),
 ("story_weapon","fictional_remake","Story Manifestation: Blade of Pages",2,12,6,["summon_construct","physical_damage"],["caster","kiter"],["physical_null"],"manifest_weapon","Written in, just now."),
 ("soul_bond","fictional_remake","Soul Bond",2,14,16,["pull","song_debuff"],["kiter","flyer"],["anti_magic"],"soul_bond","We're in this together. Whether you like it or not."),
 ("grimoire_manipulation","black_clover","Grimoire Manipulation",2,12,16,["pull","magic_damage"],["kiter","caster"],["anti_magic"],"cast_grimoire","Flip. Rewrite. Read."),
 ("page_of_severance","black_clover","Page of Severance",2,12,16,["projectile","magic_damage","silence"],["barrier","caster","healer"],["anti_magic","magic_null"],"cast_grimoire","This page wasn't in your story."),
 ("dex_combo","dnd","Dex Combo",1,8,4,["melee_arc","physical_damage"],["caster","healer"],["physical_null"],"sword_combo_1","Light on my feet, heavy on the plot."),
 ("healing_aria","generic","Healing Aria",2,20,0,["heal","song_buff"],[],[],"cast_song","Intermission."),
 ("discord","dnd","Discord",2,18,14,["song_debuff","slow"],["rusher","melee"],["anti_magic"],"cast_song","A dissonant chord, just for you."),
 ("jack_of_all_trades","dnd","Jack of All Trades",3,12,16,["copy_codex_skill"],[],[],"cast_grimoire","Never read the same story twice."),
 ("final_form_barrage","fictional_remake","The Story Where He Wins",4,20,20,["projectile","magic_damage","magic_damage"],["barrier","tank","armored"],["anti_magic","magic_null"],"cast_grimoire","This is the story where I win."),
 # Black Clover
 ("bc_flame","black_clover","Flame Page",2,12,16,["projectile","magic_damage"],["melee","rusher"],["fire_immune","anti_magic"],"cast_grimoire","Burn bright."),
 ("bc_lightning","black_clover","Lightning Page",2,10,18,["projectile","magic_damage"],["armored","kiter"],["anti_magic"],"cast_grimoire","Blue-violet and fast."),
 ("bc_spatial","black_clover","Spatial Page",2,10,18,["blink","magic_damage"],["kiter","flyer"],["anti_magic"],"shadow_step","Wrong room."),
 ("bc_light","black_clover","Light Page",2,12,16,["projectile","magic_damage","heal"],["caster"],["anti_magic"],"cast_grimoire","Let there be a plot."),
 ("bc_dark","black_clover","Dark Page",2,14,14,["magic_damage","slow"],["tank","armored"],["anti_magic"],"cast_grimoire","The ink bleeds."),
 ("black_bull_charge","black_clover","Black Bull Charge",3,10,10,["blink","melee_arc","physical_damage"],["caster","healer"],["physical_null"],"sword_combo_2","Black Bulls never retreat."),
 # Tensura
 ("tensura_magicule_bolt","tensura","Magicule Bolt",2,10,20,["projectile","magic_damage"],["melee","rusher"],["anti_magic","magic_null"],"eldritch_blast","Pure magicules."),
 ("tensura_barrier_pierce","tensura","Barrier Pierce",3,14,14,["projectile","magic_damage"],["barrier","tank"],["anti_magic"],"cast_grimoire","Barriers are just paragraphs."),
 ("tensura_steel_thread","tensura","Steel Thread",2,10,12,["pull","slow"],["kiter","flyer","rusher"],["anti_magic"],"soul_bond","Caught."),
 ("tensura_predator_copy","tensura","Predator Copy",3,16,16,["copy_codex_skill"],[],[],"cast_grimoire","I'll borrow that."),
 # other franchises
 ("danmachi_firebolt","danmachi","Firebolt",1,6,18,["projectile","magic_damage"],["kiter"],["fire_immune","anti_magic"],"eldritch_blast","Firebolt!"),
 ("fireforce_ignition","fire_force","Ignition Kick",2,9,6,["melee_arc","magic_damage"],["melee","armored"],["fire_immune"],"sword_combo_3","Ignite."),
 ("jjk_domain_slash","jujutsu_kaisen","Dismantle",3,12,12,["melee_arc","physical_damage","magic_damage"],["tank","barrier"],["physical_null","anti_magic"],"sword_combo_3","Know your place."),
 ("jojo_time_slow","jojo","Zero Time",3,16,14,["slow","song_debuff"],["rusher","kiter"],["anti_magic"],"cast_grimoire","Za Warudo, kind of."),
 # generic pool
 ("generic_sword_rain","generic","Sword Rain",3,20,16,["projectile","physical_damage"],["armored","tank","kiter"],["physical_null"],"manifest_weapon","Fifty swords, one story."),
 ("generic_domain_arena","generic","Domain of the Story",3,24,10,["slow","song_debuff","magic_damage"],["rusher","melee"],["anti_magic"],"cast_song","Welcome to my chapter."),
 ("generic_clone","generic","Clone",2,12,0,["summon_construct","blink"],["melee","rusher"],[],"manifest_shield","Two of me."),
 ("generic_grapple_chain","generic","Grapple Chain",2,10,14,["pull","physical_damage"],["kiter","flyer"],["physical_null"],"soul_bond","Get over here."),
]
os.makedirs(OUT, exist_ok=True)
for id_, anime, name, tier, cast, rng, prims, counters, res, anim, line in S:
    d = {"id": "nusmp:" + id_, "anime": anime, "name": name, "tier": tier, "cast_ticks": cast, "range": rng, "primitives": prims,
         "counters": counters, "resisted_by": res, "animation": anim, "line": line}
    with open(os.path.join(OUT, id_ + ".json"), "w") as f: json.dump(d, f, indent=2); f.write("\n")
print(len(S), "skills")
