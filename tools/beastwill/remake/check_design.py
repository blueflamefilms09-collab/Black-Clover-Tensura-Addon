import json,sys,re
KIND={"dragon_sight":(200,24,8,200),"draconic_pulse":(100,8,6,60),"time_exclusion":(100,10,6,600),"shadow_veil":(200,0,1,200),"shadow_dash":(0,0,1,60),"ambush":(0,0,1,200),"vine_lash":(60,10,3,40),"verdant_mend":(200,0,1,400),"restraining_vines":(100,8,6,400),"bark_ward":(200,0,1,400),"mane_surge":(200,0,1,300),"world_howl":(60,12,10,400),"venom_spit":(120,10,6,100),"maelstrom":(100,10,6,240),"dread_gaze":(160,16,6,300)}
CAP={"movement_speed":(.02,.40),"attack_damage":(8,.5),"max_health":(12,.3),"armor":(10,0),"armor_toughness":(5,0),"knockback_resistance":(.9,0),"attack_speed":(0,.3),"step_height":(.6,0),"jump_strength":(0,.4),"safe_fall_distance":(6,0)}
PAS=set("SWIFT REGEN ARMOR STEALTH VENOM BLIGHT FLIGHT ILLUSION FROST FURY BOND TIDE MIGHT SENSE SCORCH".split())
PRIM=set("BEAM BLAST STOP RUSH GUARD BARRAGE DISC SEAL VEIL SCAN CAPSULE PSYCHIC DETONATE".split())
HEX=re.compile(r"^#[0-9A-Fa-f]{6}$")
ALL27="ammit apophis cerberus crested_boar cu_sith dragon elderwood_guardian fenrir flame_serpent hafgufa hydra jormungandr kitsune kraken lion minotaur nidhoggr nue pegasus ridge_wolf scylla_charybdis shadow_panther sphinx thunder_fang_chimera typhon wind_skimmer yamata_no_orochi".split()
def chk(w,errs):
    i=w.get("id","?")
    def e(m): errs.append(f"{i}: {m}")
    for k in("id","name","beast","tag","source","minRank","affinity","lore","dropChance","untrained","core","spirit","acquire","integrate","skill","changes"):
        if k not in w: e(f"missing {k}")
    if errs and any(x.startswith(i+": missing") for x in errs): return
    if w["tag"] not in("CANON-BASE","DESIGN"): e("tag")
    if w["minRank"] not in("A","S","SS"): e("R1 minRank must be A/S/SS")
    lim=0.02 if w["minRank"]=="A" else 0.008
    if not(0<w["dropChance"]<=lim): e(f"R2 dropChance <= {lim}")
    for h in(w["core"].get("tint"),w["core"].get("accent")):
        if not HEX.match(str(h)): e("core hex")
    cs=w["spirit"].get("colors",[])
    if len(cs)!=3 or not all(HEX.match(c) for c in cs): e("spirit needs 3 hex colors")
    for k in("echo","acquire","integrate"):
        if len(w["spirit"].get(k,""))<20: e(f"spirit.{k} too short")
    A,I=w["acquire"],w["integrate"]
    if len(A.get("markings",""))<15: e("R3 acquire.markings (concrete markings) missing")
    if len(I.get("transformation",""))<30: e("R4 integrate.transformation (body parts) missing")
    if not(0.3<=A["drainPct"]<=1.0 and 0.9<=I["drainPct"]<=2.2 and A["drainPct"]<I["drainPct"]): e("drainPct range")
    if not(0.05<=A["strainPerSecond"]<=0.3 and 0.3<=I["strainPerSecond"]<=1.4 and A["strainPerSecond"]<I["strainPerSecond"]): e("strainPerSecond range")
    tot={}
    for nm,P in(("acquire",A),("integrate",I)):
        ab=P["abilities"]
        if len(ab)!=2: e(f"{nm}: need exactly 2 abilities")
        for n,a in enumerate(ab):
            k=a.get("kind")
            if k not in KIND: e(f"{nm}.{a.get('id')}: bad kind {k}");continue
            md,mr,mt,mc=KIND[k]
            if a["slot"]!=n+1: e(f"{nm}.{a['id']}: slot")
            if not(0<a["costPct"]<=25 and 0<=a["strain"]<=40 and 0<=a["range"]<=24 and 0<=a["power"]<=8 and 0<=a["bossFactor"]<=1): e(f"{nm}.{a['id']}: bounds")
            if a["durationTicks"]>md or a["radius"]>mr or a["maxTargets"]>mt or a["cooldownTicks"]<mc: e(f"{nm}.{a['id']}: kind limits {KIND[k]}")
        for s in P["stats"]:
            at=s["attribute"].replace("minecraft:generic.","");c=CAP.get(at)
            if not c: e(f"{nm}: stat {at} not allowed");continue
            lim2=c[0] if s["op"]=="add" else c[1]
            if s["op"] not in("add","mult") or s["amount"]>lim2+1e-9: e(f"{nm}: stat {at} over cap")
            tot.setdefault(nm,{})[(at,s["op"])]=s["amount"]
    for (at,op),v in tot.get("acquire",{}).items():
        iv=tot.get("integrate",{}).get((at,op))
        if iv is not None and v>iv*0.6+1e-9: e(f"R3 acquire stat {at} not clearly smaller than integrate")
    S=w["skill"]
    if not HEX.match(S.get("rgb","")): e("skill rgb")
    if not(0.6<=S.get("scale",0)<=1.0): e("skill scale")
    if not(1<=len(S.get("passives",[]))<=3): e("1-3 passives")
    for p in S.get("passives",[]):
        if p["pas"] not in PAS: e(f"bad pas {p['pas']}")
    if len(S.get("actives",[]))!=2: e("skill needs 2 actives")
    for a in S.get("actives",[])+[S.get("finisher",{})]:
        if a.get("prim") not in PRIM: e(f"bad prim {a.get('prim')}")
    if not re.match(r"^[LGFS]:[A-Z_0-9]+$",S.get("fx","")): e("fx format")
def main(files):
    ws=[];errs=[]
    for f in files: ws+=json.load(open(f))
    for w in ws: chk(w,errs)
    ids=[w["id"] for w in ws]
    if len(set(ids))!=len(ids): errs.append("duplicate ids")
    for t,name in(("core","tint"),):
        seen={}
        for w in ws:
            c=w["core"].get("tint","").lower()
            if c in seen: errs.append(f"core tint {c} shared by {seen[c]} and {w['id']}")
            seen[c]=w["id"]
    seen={}
    for w in ws:
        c=w["spirit"].get("colors",[""])[0].lower()
        if c in seen: errs.append(f"spirit main colour {c} shared by {seen[c]} and {w['id']}")
        seen[c]=w["id"]
    print(f"{len(ws)} wills checked")
    for e in errs: print("FAIL",e)
    miss=[k+"_will" for k in ALL27 if k+"_will" not in ids]
    if len(ws)>=27 and miss: print("FAIL missing",miss)
    print("OK" if not errs else f"{len(errs)} problems")
    return 1 if errs else 0
if __name__=="__main__" and (len(sys.argv)<2 or sys.argv[1] not in("--roster","--v3")): sys.exit(main(sys.argv[1:]))

def roster(files):
    import itertools
    ws=[]
    for f in files: ws+=json.load(open(f))
    def rgb(h): h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in(0,2,4))
    def d(a,b): return sum((x-y)**2 for x,y in zip(rgb(a),rgb(b)))**.5
    print("-- roster checks --")
    n=0
    for a,b in itertools.combinations(ws,2):
        for lab,fa,fb in(("core",a["core"]["tint"],b["core"]["tint"]),("spirit",a["spirit"]["colors"][0],b["spirit"]["colors"][0])):
            if d(fa,fb)<55: print(f"CLOSE {lab} colour {a['id']} {fa} ~ {b['id']} {fb} (dist {d(fa,fb):.0f})");n+=1
    sig={}
    for w in ws:
        k=tuple(sorted(x["kind"] for p in("acquire","integrate") for x in w[p]["abilities"]))
        sig.setdefault(k,[]).append(w["id"])
    for k,v in sig.items():
        if len(v)>1: print("SAME KIND SET",v,k);n+=1
    ids=[]
    for w in ws:
        for p in("acquire","integrate"):
            for x in w[p]["abilities"]: ids.append((x["id"],w["id"]))
    seen={}
    for i,w in ids:
        if i in seen and seen[i]!=w: print(f"ABILITY ID {i} shared by {seen[i]} and {w}");n+=1
        seen[i]=w
    print(f"{n} roster issues")
if __name__=="__main__" and len(sys.argv)>2 and sys.argv[1]=="--roster": roster(sys.argv[2:])

def chk3(w,errs):
    i=w.get("id","?")
    def e(m): errs.append(f"{i}: {m}")
    if len(w.get("pitch",""))<60: e("pitch (3 sentences) missing")
    elif w["pitch"].count(".")<3: e("pitch needs 3 sentences")
    if len(w["acquire"].get("equip_piece",""))<12: e("acquire.equip_piece missing")
    q=w["integrate"].get("equip",{})
    for k in("weapon","armor","hair_eyes","aura"):
        if len(q.get(k,""))<8: e(f"integrate.equip.{k} missing")
    if len(w["skill"].get("finisher",{}).get("incantation",""))<8: e("finisher.incantation missing")
    low=(w["untrained"]).lower()
    for kw in("awaken","kill"):
        if kw not in low: e(f"untrained should mention '{kw}' (canon addendum 2)")
    blob=json.dumps(w).lower()
    for bad in("djinn","magi:","extreme magic","metal vessel","masō","maso ","zagan","focalor","sinbad","labyrinth of magic"):
        if bad in blob: e(f"owner: do not copy Magi/Djinn content: '{bad}'")
    for bad in("summons a pack","summon a horde","army of"):
        if bad in blob: e(f"no-armies rule: '{bad}'")
def v3(files):
    ws=[];errs=[]
    for f in files: ws+=json.load(open(f))
    for w in ws: chk3(w,errs)
    for x in errs: print("FAIL3",x)
    print(f"{len(ws)} wills v3-checked;", "OK3" if not errs else f"{len(errs)} problems")
if __name__=="__main__" and len(sys.argv)>2 and sys.argv[1]=="--v3": v3(sys.argv[2:])
