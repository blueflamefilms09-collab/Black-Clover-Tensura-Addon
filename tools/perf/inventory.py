#!/usr/bin/env python3
"""
Static inventory of the mod's continuous work (docs/performance_1_99.md).

Reads src/main/java and lists, with how often it runs, what one call does, what it scales with and a verdict:
  * every event handler of the hot events (player / server / level / entity / client tick, render stage / living / gui layer, entity join, damage events), methods and addListener lambdas;
  * every tick() / aiStep() / customServerAiStep() / baseTick() of the mod's entities, block entities, goals and schedulers;
  * every client render() of a layer, renderer or overlay, and every VFX layer of vfx/client/layer.
Nothing is run: the frequency comes from the throttle in the first lines of the body (tickCount % N, getGameTime() % N, (t & 3) ...), the work from the calls in the body, the scaling from the event.
A method that the performance pass wrapped in a probe section (a thin method that keeps the old name and calls NAME0 between PerfProbe.begin and PerfProbe.end) is listed once, under its old name,
with the probe section in the verdict.

Usage:   python3 tools/perf/inventory.py                 prints the markdown to stdout
         python3 tools/perf/inventory.py --write         replaces the block between the inventory markers of docs/performance_1_99.md
The verdict of a row is "OK" / "WATCH" by rule; FIXED and REVIEWED below carry the findings of the human pass (they win over the rule), so a WATCH row is a row nobody has read yet.
"""
import os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')
SRC = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'newuniverse', 'nusmp')
DOC = os.path.join(ROOT, 'docs', 'performance_1_99.md')
BEGIN, END = '<!-- inventory:begin -->', '<!-- inventory:end -->'

EVENTS = ["PlayerTickEvent", "ServerTickEvent", "LevelTickEvent", "EntityTickEvent", "ClientTickEvent", "RenderLevelStageEvent", "RenderPlayerEvent", "RenderLivingEvent",
          "RenderGuiLayerEvent", "RenderGuiEvent", "EntityJoinLevelEvent", "LivingIncomingDamageEvent", "LivingHurtEvent", "LivingDamageEvent", "RenderHandEvent"]
EV = "|".join(EVENTS)
SIG = re.compile(r'(?:(?:public|private|protected|static|final|synchronized)\s+)*[\w<>\[\]\.,? ]+\s+(\w+)\s*\(\s*(?:final\s+)?([\w\.]*(?:%s)[\w\.]*)\s+(\w+)\s*\)\s*(?:throws [\w, .]+)?\s*\{' % EV)
LAM = re.compile(r'addListener\(\s*(?:[\w\.]+\s*,\s*)?\(\s*(?:final\s+)?([\w\.]*(?:%s)[\w\.]*)\s+(\w+)\s*\)\s*->\s*(\{)?' % EV)
METHOD_NAMES = ('tick', 'aiStep', 'customServerAiStep', 'baseTick', 'serverTick', 'clientTick', 'tickPart', 'render', 'renderToBuffer', 'setupAnim')
METH = re.compile(r'(?:public|protected|private)?\s*(?:static\s+)?(?:final\s+)?void\s+(%s)0?\s*\(([^)]*)\)\s*\{' % '|'.join(METHOD_NAMES))
CLASS = re.compile(r'\bclass\s+(\w+)(?:<[^{]*?>)?\s+extends\s+([\w\.<>, ?]+?)(?:\s+implements[^{]*)?\s*\{')

HEAVY = {
    'entity scan': r'getEntitiesOfClass|getEntities\(|getNearbyEntities',
    'player list': r'\.players\(\)|getPlayers\(\)',
    'inventory': r'getInventory\(\)|getContainerSize|getItemBySlot',
    'curios': r'Curios',
    'registry': r'BuiltInRegistries|registryAccess|lookupOrThrow',
    'locations': r'ResourceLocation\.|new ResourceLocation|fromNamespaceAndPath',
    'reflection': r'Class\.forName|getMethod|getDeclaredMethod|getDeclaredField|\.invoke\(',
    'streams': r'\.stream\(\)|\.collect\(',
    'strings': r'"\s*\+|\+\s*"|String\.format|\.formatted',
    'blocks': r'getBlockState|betweenClosed|MutableBlockPos|setBlock|getBlockEntity',
    'packets': r'PacketDistributor|sendToPlayer|send\(',
    'particles': r'sendParticles|addParticle',
    'attributes': r'getAttribute\(|addTransientModifier|removeModifier',
    'effects': r'addEffect|removeEffect|hasEffect',
    'skills': r'SkillAPI|getSkillsFrom|getSkillStorage',
}
HEAVY_RX = {k: re.compile(v) for k, v in HEAVY.items()}
LOOP = re.compile(r'\bfor\s*\(|\bwhile\s*\(')
COSTLY = ('entity scan', 'inventory', 'curios', 'reflection', 'streams', 'blocks', 'locations', 'registry')

DEFAULT_FREQ = {
    'PlayerTickEvent': 'every tick', 'ServerTickEvent': 'every tick', 'LevelTickEvent': 'every tick', 'EntityTickEvent': 'every tick', 'ClientTickEvent': 'every client tick',
    'RenderLevelStageEvent': 'every frame (several stages)', 'RenderPlayerEvent': 'every frame', 'RenderLivingEvent': 'every frame', 'RenderGuiLayerEvent': 'every frame',
    'RenderGuiEvent': 'every frame', 'RenderHandEvent': 'every frame', 'EntityJoinLevelEvent': 'on an entity joining', 'LivingIncomingDamageEvent': 'on a hit',
    'LivingHurtEvent': 'on a hit', 'LivingDamageEvent': 'on a hit',
}
SCALES = {
    'PlayerTickEvent': 'per player', 'ServerTickEvent': 'global (loops players of its dimension)', 'LevelTickEvent': 'per level', 'EntityTickEvent': 'per entity, all levels',
    'ClientTickEvent': 'client only', 'RenderLevelStageEvent': 'per frame', 'RenderPlayerEvent': 'per visible player', 'RenderLivingEvent': 'per visible entity',
    'RenderGuiLayerEvent': 'per frame x HUD layer', 'RenderGuiEvent': 'per frame', 'RenderHandEvent': 'per frame', 'EntityJoinLevelEvent': 'per spawn',
    'LivingIncomingDamageEvent': 'per hit', 'LivingHurtEvent': 'per hit', 'LivingDamageEvent': 'per hit',
}

# ---------------------------------------------------------------------------------------------------------------- the human pass
# key = "path/File.java#method" (path relative to the mod package); value = note
FIXED = {
    'vfx/client/VfxManager.java#render': 'level of detail by distance, vertex budget of the frame, cull, per-layer probe',
    'vfx/client/VfxManager.java#spawn': 'running-effect cap follows the quality',
    'vfx/client/FxRenderer.java#render': 'cull, cached piece textures, scratch vectors',
    'client/aura/PlayerAuraLayer.java#render': 'aura LOD by distance',
    'client/labyrinth/foundry/FoundryRenderer.java#render': 'per-mob kit cached (no strings per frame)',
    'client/remodel/AbyssalRenderer.java#render': 'spec cached per look and phase',
    'item/CuriosLink.java#onPlayerTick': 'modifier ids cached',
    'blackclover/TimeStop.java#onEntityTick': 'empty-map early exit',
    'relics/MobRelics.java#onTick': 'modulo before the config read',
    'item/ElsdociaEvents.java#onPlayerTick': 'period follows entityScanInterval',
    'client/ui/GuiScale.java#onLayerPost': 'nothing-pushed early exit',
    'client/relics/RelicLayerGuard.java#onPre': 'last-renderer shortcut',
    'client/relics/RelicWornLayer.java#render': 'box quads without arrays; glow location cached',
    'client/mode/ModeArmorLayer.java#render': 'library key built once per mode',
    'client/WeaponRenderer.java#renderByItem': 'two scratch vectors for the whole mesh (two new vectors per vertex before)',
    'vfx/VfxSpawn.java#send': 'effect packets of one tick go through the budget',
}
REVIEWED = {
    'book/TimeBook.java#passiveTick': 'one skill-storage lookup per player per tick; Time Magic is left as it is (owner rule 3)',
    'charter/CharterEvents.java#onPlayerTick': 'CharterMirror.pass returns at once except one tick in twenty per player (staggered by id)',
    'song/SongTicker.java#onPlayerTick': 'reads one int when no song mode is on',
    'butoh/ButohEvents.java#onPlayerTick': 'ButohDance.tick returns after two map reads unless a dance session exists',
    'labyrinth/mega/gen/MegaGenRegistry.java#onPlayerTick': 'MegaTravel.rememberLanding returns after a modulo outside the dungeon',
    'labyrinth/mega/system/SystemEvents.java#onPlayerTick': 'SystemQuests.tickPlayer: every 5 ticks, dimension test first',
    'shadow/UniteWatcher.java#onServerTick': 'loops online players, a few field reads each',
    'labyrinth/mega/floors/FloorTracker.java#onServerTick': 'every TRACK_EVERY_TICKS (config, 10), dungeon dimension test first',
    'labyrinth/mega/puzzle/PuzzleDirector.java#onServerTick': 'returns when there is no dungeon; scan every 10 ticks; active puzzles only',
    'labyrinth/mega/mechanics/MechanicsDirector.java#onServerTick': 'returns when there is no dungeon; scan every 10 ticks; active mechanics only',
    'labyrinth/mega/system/FloorInfo.java#tick': 'once a second per dungeon player, signature compared before any message',
    'boss/bar/BossBarSync.java#onServerTick': 'returns at once with no boss bar; sends only to players not yet told',
    'compat/create/CreateTrapDirector.java#onServerTick': 'inert without Create; every 2 ticks; refresh of near traps every 10 ticks',
    'labyrinth/mega/hub/HubRules.java#onServerTick': 'every 100 ticks, Sanctuary only',
    'labyrinth/mega/party/PartyGuard.java#onServerTick': 'every 20 ticks, dungeon only',
    'labyrinth/mega/travel/StairTravel.java#onServerTick': 'every 4 ticks, dungeon only',
    'labyrinth/mega/ExpansionTrigger.java#onServerTick': 'every 20 ticks, only while a sector waits',
    'antimagic/NihilityZone.java#onServerTick': 'iterates live zones only (none almost always); one entity scan per zone per pass',
    'relics/WornSync.java#onEntityTick': 'one tick in twenty, exits for any mob that never carried a relic',
    'blackclover/GrimoireSlot.java#onPlayerTick': 'slot filled: one stack compare per tick (the change detector of the hip sync); slot empty: one pass over 37 slots with an instanceof each',
    'core/discovery/DiscoveryTracker.java#onTick': 'already staggered per player: inventory scan every 20 ticks, skills and Curios every 40, each on the player id phase',
    'book/SpellRuntime.java#onServerTick': 'walks only the live bolts and zones (empty almost always)',
    'client/relics/fx/RelicFxClient.java#onClientTick': 'budgeted: players within range and a particle cap per tick (WornBudget)',
    'client/relics/item/SocketFx.java#onClientTick': 'budgeted: MAX_HOLDERS and MAX_PARTICLES_PER_TICK; the quality is re-read every 200 ticks',
    'avatar/ritual/SoulForgeRitual.java#tick': 'a ritual that exists only while one is performed',
    'dungeon/nexus/relic/Attunement.java#tick': 'no clock is made for a player who never attunes: one item class test per tick',
    'labyrinth/mega/foundry/FoundryAbilityRunner.java#tick': 'counters; the entity scan only while a dash runs (0.5 block box)',
    'labyrinth/trap/PitfallTileBlock.java#tick': 'a scheduled block tick (fires when the trap is armed), not a poll',
    'labyrinth/trap/SnufferGlyphBlock.java#tick': 'a scheduled block tick with a bounded box',
    'prop/VortexProps.java#tick': 'a spell prop alive for a few seconds; the pull needs the box every tick',
    'relics/wave/Wave5Hemlock.java#tick': 'called by the once-a-second player pass',
    'relics/wave/Wave5Huntress.java#tick': 'called by the once-a-second player pass',
    'skill/Summons.java#tick': 'called by the owner pass once a second',
    'thread/ThreadTrapEntity.java#tick': 'projectile catch needs every tick (a bolt moves up to 3 blocks per tick and is tested along its segment); the living-entity touch test runs every 2 ticks',
    'client/elite/hub/QuestOverlay.java#render': 'at most five quests, one clipped string each',
    'client/playermodel/KitRelicLayer.java#render': 'the "collect" is WornExec.collect (a plain method, no stream); reads the 10-tick relic cache; allocation free',
    'client/relics/HumanoidRelicLayer.java#render': 'the "collect" is WornExec.collect (no stream); the candidate test is allocation free and exits for every entity without a relic',
}
SCREEN_HINT = re.compile(r'Screen|Page|Menu|Gui|Codex')


def rel(p):
    return os.path.relpath(p, SRC).replace(os.sep, '/')


def body_from(src, i):
    depth = 0
    j = i
    n = len(src)
    instr = None
    while j < n:
        c = src[j]
        if instr:
            if c == '\\':
                j += 2
                continue
            if c == instr:
                instr = None
        else:
            if c == '/' and src[j:j + 2] == '//':
                k = src.find('\n', j)
                j = k if k >= 0 else n
                continue
            if c == '/' and src[j:j + 2] == '/*':
                k = src.find('*/', j)
                j = k + 2 if k >= 0 else n
                continue
            if c in '"\'':
                instr = c
            elif c == '{':
                depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    return src[i:j + 1]
        j += 1
    return src[i:]


def code_lines(body):
    return [l.strip() for l in body[1:].split('\n') if l.strip() and not l.strip().startswith(('//', '*', '/*'))]


def throttle(first):
    """(period, staggered) read from the first lines of a body; period None when there is none."""
    for rx in (r'(?:getGameTime\(\)|getTickCount\(\)|tickCount|[A-Za-z]*[Tt]ick|now|time|t|age)\s*%\s*(\w+)\s*(?:!=|==)', r'%\s*(\w+)\s*(?:!=|==)\s*\w'):
        m = re.search(rx, first)
        if m:
            tok = m.group(1)
            return (int(tok) if tok.isdigit() else tok), bool(re.search(r'getId\(\)', first))
    m = re.search(r'\(\s*(?:\w+\.)?\w*\s*&\s*(\d+)\s*\)\s*(?:!=|==)', first)
    if m:
        return int(m.group(1)) + 1, False
    return None, False


def work(body):
    tags = [k for k, rx in HEAVY_RX.items() if rx.search(body)]
    if LOOP.search(body):
        tags.append('loop')
    return tags


def probe_of(src, body, name):
    """Is this body a probe wrapper (it calls NAME0 between PerfProbe.begin and PerfProbe.end)? Returns the section name or None."""
    if 'PerfProbe.begin()' in body and re.search(r'\b%s0\(' % re.escape(name), body):
        m = re.search(r'PerfProbe\.end\((\w+),', body)
        if m:
            sec = re.search(r'%s\s*=\s*[^;]*section\("([^"]+)"\)' % re.escape(m.group(1)), src)
            if sec:
                return sec.group(1)
    return None


def fold(src, found):
    """found: list of dicts with name/body. Drops the wrappers, renames NAME0 to NAME and attaches the probe section."""
    names = {f['name'] for f in found}
    wrappers = {}
    for f in found:
        if f['name'] + '0' in names:
            sec = probe_of(src, f['body'], f['name'])
            if sec:
                wrappers[f['name']] = sec
    out = []
    for f in found:
        if f['name'] in wrappers:
            continue
        if f['name'].endswith('0') and f['name'][:-1] in wrappers:
            f = dict(f, name=f['name'][:-1], probe=wrappers[f['name'][:-1]])
        out.append(f)
    return out


def collect_handlers():
    rows = []
    for dp, dn, fn in os.walk(SRC):
        for f in sorted(fn):
            if not f.endswith('.java'):
                continue
            p = os.path.join(dp, f)
            src = open(p, encoding='utf-8', errors='replace').read()
            if 'Event' not in src:
                continue
            found = []
            for m in SIG.finditer(src):
                found.append(dict(name=m.group(1), ev=m.group(2), body=body_from(src, m.end() - 1), pos=m.start()))
            for m in LAM.finditer(src):
                if m.group(3):
                    b = body_from(src, m.end() - 1)
                else:
                    k = src.find(');', m.end())
                    b = src[m.end():k]
                found.append(dict(name='(lambda)', ev=m.group(1), body=b, pos=m.start()))
            for d in fold(src, found):
                ev = d['ev']
                evn = next((e for e in EVENTS if e in ev), ev)
                variant = ev.split('.')[-1] if ev.split('.')[-1] in ('Pre', 'Post') else ''
                lines = code_lines(d['body'])
                first = ' '.join(lines[:8])
                period, stag = throttle(first)
                rows.append(dict(file=rel(p), line=src.count('\n', 0, d['pos']) + 1, name=d['name'], event=evn + ('.' + variant if variant else ''), ev=evn, loc=len(lines),
                                 period=period, stag=stag, work=work(d['body']), probe=d.get('probe')))
    return rows


def collect_methods():
    rows = []
    for dp, dn, fn in os.walk(SRC):
        for f in sorted(fn):
            if not f.endswith('.java'):
                continue
            p = os.path.join(dp, f)
            src = open(p, encoding='utf-8', errors='replace').read()
            r = rel(p)
            client = r.startswith('client/') or r.startswith('vfx/client') or '/client/' in r
            cm = CLASS.search(src)
            cls = cm.group(1) if cm else f[:-5]
            sup = cm.group(2).strip() if cm else ''
            found = []
            for m in METH.finditer(src):
                name = m.group(1) + ('0' if src[m.end(1)] == '0' else '')
                if m.group(1) in ('render', 'renderToBuffer', 'setupAnim') and not client:
                    continue
                found.append(dict(name=name, body=body_from(src, m.end() - 1), pos=m.start()))
            for d in fold(src, found):
                lines = code_lines(d['body'])
                period, stag = throttle(' '.join(lines[:8]))
                rows.append(dict(file=r, line=src.count('\n', 0, d['pos']) + 1, name=d['name'], cls=cls, sup=sup, loc=len(lines), period=period, stag=stag, work=work(d['body']),
                                 client=client, probe=d.get('probe')))
    return rows


def freq_text(row, event=None):
    if row['period'] is not None:
        return 'every %s ticks%s' % (row['period'], ' (staggered by id)' if row['stag'] else '')
    if event:
        return DEFAULT_FREQ.get(event, 'every tick')
    return 'every frame' if row.get('client') and row['name'].startswith(('render', 'setupAnim')) else 'every tick'


def esc(s):
    return s.replace('|', '\\|')


def tag(row, text):
    return text + (' [probe %s]' % row['probe'] if row.get('probe') else '')


def verdict_handler(row):
    key = '%s#%s' % (row['file'], row['name'])
    if key in FIXED:
        return tag(row, 'FIXED: ' + FIXED[key])
    if key in REVIEWED:
        return tag(row, 'OK: ' + REVIEWED[key])
    costly = [x for x in row['work'] if x in COSTLY]
    if row['period'] is not None:
        return tag(row, 'OK: throttled')
    if row['ev'] in ('LivingIncomingDamageEvent', 'LivingHurtEvent', 'LivingDamageEvent', 'EntityJoinLevelEvent'):
        return tag(row, 'OK: event driven' + (' (real work only for its own targets)' if costly else ''))
    if row['loc'] <= 8 and not costly:
        return tag(row, 'OK: cheap gate, delegates')
    if not costly:
        return tag(row, 'OK: light')
    return tag(row, 'WATCH: ' + ', '.join(costly) + ', no throttle in the first lines')


def verdict_method(row):
    key = '%s#%s' % (row['file'], row['name'])
    if key in FIXED:
        return tag(row, 'FIXED: ' + FIXED[key])
    if key in REVIEWED:
        return tag(row, 'OK: ' + REVIEWED[key])
    costly = [x for x in row['work'] if x in COSTLY]
    if row['period'] is not None:
        return tag(row, 'OK: throttled')
    if row['name'] in ('render', 'renderToBuffer', 'setupAnim'):
        if SCREEN_HINT.search(row['cls']) or SCREEN_HINT.search(row['sup']) or '/gui/' in row['file'] or '/ui/' in row['file']:
            return tag(row, 'OK: screen or HUD page, drawn only while open')
        bad = [x for x in row['work'] if x in ('locations', 'strings', 'reflection', 'streams', 'registry')]
        return tag(row, 'WATCH: ' + ', '.join(bad) if bad else 'OK')
    if not costly:
        return tag(row, 'OK: light')
    if row['client']:
        return tag(row, 'OK: client, bounded by what is on screen')
    return tag(row, 'WATCH: ' + ', '.join(costly))


def table_handlers(rows):
    out = ['| Handler | Event | Frequency | Work per call | Scales with | Verdict |', '|---|---|---|---|---|---|']
    order = {e: i for i, e in enumerate(EVENTS)}
    for r in sorted(rows, key=lambda r: (order.get(r['ev'], 99), r['file'], r['line'])):
        out.append('| `%s#%s` | %s | %s | %s | %s | %s |' % (r['file'], r['name'], r['event'], freq_text(r, r['ev']), esc(', '.join(r['work']) or 'trivial'), SCALES.get(r['ev'], ''), esc(verdict_handler(r))))
    return out


def table_ticks(rows):
    out = ['| Method | Extends | Frequency | Work per call | Scales with | Verdict |', '|---|---|---|---|---|---|']
    for r in sorted((r for r in rows if r['name'] in METHOD_NAMES[:7]), key=lambda r: (r['file'], r['line'])):
        scale = 'per frame' if r['client'] else ('per entity' if r['sup'] else 'per instance')
        out.append('| `%s#%s` (%s) | %s | %s | %s | %s | %s |' % (r['file'], r['name'], r['cls'], esc(r['sup'][:40]), freq_text(r), esc(', '.join(r['work']) or 'trivial'), scale, esc(verdict_method(r))))
    return out


def table_render(rows):
    out = ['| Renderer / layer | Extends | Work per frame | Scales with | Verdict |', '|---|---|---|---|---|']
    for r in sorted((r for r in rows if r['name'] in METHOD_NAMES[7:] and r['client']), key=lambda r: (r['file'], r['line'])):
        out.append('| `%s#%s` (%s) | %s | %s | per visible entity or player | %s |' % (r['file'], r['name'], r['cls'], esc(r['sup'][:40]), esc(', '.join(r['work']) or 'trivial'), esc(verdict_method(r))))
    return out


def table_vfx():
    d = os.path.join(SRC, 'vfx', 'client', 'layer')
    out = ['| VFX layer | Shapes | Vertex budget | Verdict |', '|---|---|---|---|']
    for f in sorted(os.listdir(d)):
        if not f.endswith('Layer.java'):
            continue
        src = open(os.path.join(d, f), encoding='utf-8', errors='replace').read()
        m = re.search(r'Set<VfxShape>\s+shapes\(\)\s*\{[^}]*?return\s+([^;]+);', src, re.S)
        shapes = len(re.findall(r'VfxShape\.\w+', m.group(1))) if m else 0
        bm = re.search(r'int vertexBudget\([^)]*\)\s*\{\s*return\s*([^;]+);', src)
        out.append('| `%s` | %s | %s | %s |' % (f[:-5], shapes or '?', esc(bm.group(1).strip()) if bm else '400 (default)',
                                                'exempt from the level of detail (the Time Magic standard)' if f.startswith('TimeMagic') else
                                                'OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.%s)' % f[:-5]))
    return out


def build():
    h = collect_handlers()
    m = collect_methods()
    watch_h = sum(1 for r in h if verdict_handler(r).startswith('WATCH'))
    ticks = [r for r in m if r['name'] in METHOD_NAMES[:7]]
    rend = [r for r in m if r['name'] in METHOD_NAMES[7:] and r['client']]
    watch_m = sum(1 for r in ticks + rend if verdict_method(r).startswith('WATCH'))
    parts = ['<!-- generated by tools/perf/inventory.py; do not edit by hand: change FIXED / REVIEWED in the script and run it again -->', '']
    parts.append('#### A. Event handlers of the hot events (%d rows, %d marked WATCH)' % (len(h), watch_h))
    parts.append('')
    parts += table_handlers(h)
    parts.append('')
    parts.append('#### B. tick() / aiStep() / customServerAiStep() of entities, block entities, goals and schedulers (%d rows)' % len(ticks))
    parts.append('')
    parts += table_ticks(m)
    parts.append('')
    parts.append('#### C. Client render methods of layers, renderers and overlays (%d rows, %d marked WATCH)' % (len(rend), sum(1 for r in rend if verdict_method(r).startswith('WATCH'))))
    parts.append('')
    parts += table_render(m)
    parts.append('')
    parts.append('#### D. VFX layers (vfx/client/layer)')
    parts.append('')
    parts += table_vfx()
    parts.append('')
    return '\n'.join(parts), watch_h + watch_m, dict(handlers=len(h), ticks=len(ticks), render=len(rend))


if __name__ == '__main__':
    text, n_watch, counts = build()
    if '--write' in sys.argv:
        doc = open(DOC, encoding='utf-8').read()
        a, b = doc.index(BEGIN), doc.index(END)
        open(DOC, 'w', encoding='utf-8').write(doc[:a + len(BEGIN)] + '\n' + text + '\n' + doc[b:])
        print('written', DOC, counts, '; WATCH rows:', n_watch)
    else:
        print(text)
        sys.stderr.write('%s; WATCH rows: %d\n' % (counts, n_watch))
