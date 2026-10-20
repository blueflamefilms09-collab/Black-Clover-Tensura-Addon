#!/usr/bin/env python3
"""Generates the armour policy of 1.99.0: src/main/resources/data/nusmp/armour_policy.json (read by book.ArmourPolicy) and the table of
docs/rebuild/armour_policy.md.

The policy says, for EVERY MagicType, whether the magic has an armour at all (NONE / TOGGLE / FORM), which explicit page puts it on, what it gives
while it is on, and the canon reason. Edit the tables below and run:  python3 -B tools/gen_armour_policy.py   (from the repository root).

Canon note: the Black Clover wiki host cannot be reached from the build sandbox. The reasons are written from the anime / manga as remembered and from
the repository's own copy of the wiki pages (.github/wiki/*.txt: attribute and spell pages); where a statement is only a recollection it says so.
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
MAGIC_TYPE = os.path.join(ROOT, 'src/main/java/com/newuniverse/nusmp/blackclover/MagicType.java')
OUT_JSON = os.path.join(ROOT, 'src/main/resources/data/nusmp/armour_policy.json')
OUT_MD = os.path.join(ROOT, 'docs/rebuild/armour_policy.md')

# ---------------------------------------------------------------------------------------------------------------------------------------------
# The magics that HAVE an armour or a form. Everything else is NONE.
# bonus = what the armour gives while it is on (ArmourPolicy.Bonus): cut, attack, speed (multipliers added to the base), knockback (resistance),
#         ward ("fire" / "lightning"), senses, upkeep (share of the magicule pool per second), absorb (ambient intake per second), hp_upkeep.
# ---------------------------------------------------------------------------------------------------------------------------------------------
ARMOURED = {
    'FLAME': dict(
        kind='FORM', name='Flame Armour', form_name='Spirit Dive: Salamander', aura='FLAME', armour_pages={'flame_armour': 0}, form_pages=['spirit_dive'],
        bonus=dict(cut=0.10, attack=0.08, ward='fire', upkeep=0.004),
        form_bonus=dict(cut=0.15, attack=0.25, speed=0.15, knockback=0.3, ward='fire', senses=True, absorb=0.012),
        canon="Spirit Dive with the fire spirit Salamander: Fuegoleon's arms cloaked in fire mana with draconic scales, claws and fiery wings (Fana hosts it too); "
              "Fuegoleon's Flame Armour is the everyday armour. FORM: Spirit Dive (mana upkeep, ambient intake); explicit page: Flame Armour."),
    'WATER': dict(
        kind='FORM', name='Valkyrie Armour', form_name='Spirit Dive: Undine', aura='WATER', armour_pages={'valkyrie_dress': 1, 'scale_mail': 0}, form_pages=['spirit_dive'],
        bonus=dict(cut=0.12, attack=0.05, speed=0.08, knockback=0.1, upkeep=0.004),
        form_bonus=dict(cut=0.18, attack=0.18, speed=0.22, senses=True, absorb=0.012),
        canon="Noelle Silva's Valkyrie Armor / Valkyrie Dress (water armour of the Sea Dragon) and, with the water spirit Undine, the Saint Valkyrie Dress "
              "(feathered wing-like parts and a halo). Spirit Dive is the form; Valkyrie Dress and Sea Dragon's Scale Mail are the explicit armour pages."),
    'WIND': dict(
        kind='FORM', name='Spirit of Zephyr', form_name='Spirit Dive: Sylph', aura='', armour_pages={}, form_pages=['spirit_dive'], external_pages=['spirit_of_zephyr'],
        form_bonus=dict(cut=0.10, attack=0.15, speed=0.30, senses=True, absorb=0.012),
        canon="Yuno's Spirit Dive with the wind spirit Sylph (Spirit of Zephyr): the first dive transforms only the left half (one wing, half a crown, a ring "
              "round the wrist), the full dive has two wings and a full crown. Spirit Dive is the form; the Zephyr page is the older overlay (blackclover.ModeArmor)."),
    'EARTH': dict(
        kind='FORM', name='Gnome Plate', form_name='Spirit Dive: the earth spirit', aura='WIKI', form_aura=True, armour_pages={'earth_armor': 100}, form_pages=['spirit_dive'],
        bonus=dict(cut=0.16, attack=0.04, knockback=0.4, upkeep=0.004),
        form_bonus=dict(cut=0.22, attack=0.20, speed=0.08, knockback=0.6, senses=True, absorb=0.012),
        canon="The earth spirit exists in the lore but its name, look and host were never shown, so the dive is a generic earth spirit dive (the mod calls the spirit "
              "Gnome). Spirit Dive is the form; Gnome Plate is the explicit armour page."),
    'LIGHTNING': dict(
        kind='TOGGLE', name='Lightning Armour', aura='LIGHTNING', armour_pages={'thunder_fiend': 0, 'black_lightning_battle_fiend': 1, 'raging_lightning': 0},
        bonus=dict(cut=0.06, attack=0.08, speed=0.12, ward='lightning', upkeep=0.005),
        canon="Luck Voltia's lightning armour (Thunder Fiend, Black Lightning Battle Fiend, Raging Lightning: the white-and-gold armour with bolts of "
              "docs/mode_armor_spec.md). The three Fiend pages are the explicit armour pages."),
    'DARK': dict(
        kind='TOGGLE', name='Dark Cloaked', aura='DARK', armour_pages={'dark_cloak': 0},
        bonus=dict(cut=0.10, attack=0.10, upkeep=0.004),
        canon="Yami Sukehiro's Dark Cloaked: his body and blades coated in dark mana (obsidian plate, horned hood, tattered cape in the mod). "
              "Dark Cloaked Guard is the explicit armour page; the Dark Cloaked slashes are spells, not armour."),
    'MERCURY': dict(
        kind='TOGGLE', name='Silver Guardian', aura='MERCURY', armour_pages={'mercury_shield': 0},
        bonus=dict(cut=0.20, upkeep=0.004, hp_upkeep=0.5),
        canon="Mercury Magic: Silver Guardian (repo wiki spell page): the mercury becomes a defensive coating over the whole body, solid enough to block repeated "
              "strikes, flexible enough not to hinder movement, and poisonous to the user (hence the health upkeep). Nozel Silva's magic."),
    'CORUNDUM': dict(
        kind='TOGGLE', name='Gem Plate Armour', aura='CORUNDUM', armour_pages={'gem_plate_armour': 0}, style_by='mastery',
        bonus=dict(cut=0.20, knockback=0.3, attack=0.05, upkeep=0.005),
        canon="Corundum Magic: Ideal Closer (repo wiki spell page): the user's body is completely covered by corundum with a Y-shaped opening for eyes and mouth; "
              "the armour takes a larger humanoid shape. Gem Plate Armour is the explicit armour page (a crystal crown and wings of shards once mastered)."),
    'BEAST': dict(
        kind='TOGGLE', name='Primal Beast Form', aura='BEAST', armour_pages={'primal_beast_form': 0}, style_by='sneak',
        bonus=dict(cut=0.08, attack=0.12, speed=0.10, upkeep=0.004),
        canon="Beast Magic: Rhinoceros Armor (repo wiki spell page): a magic aura in the shape of a rhinoceros round the body, able to stop powerful impacts; the Beast "
              "spells (Bear Claw, Cheetah Charge ...) are animal forms. Primal Beast Form is the explicit page (sneak: the great beast)."),
    'ANTI_MAGIC': dict(
        kind='FORM', name='Devil Union (Black Asta)', aura='', armour_pages={}, form_pages=[], external_pages=['black_form'],
        canon="Asta and the devil Liebe: Devil Union. No mana; Anti-Magic coats the body and erases, cuts or repels any magic it touches; horns, tail, slit pupils and "
              "pitch-black jagged armour. It needs a Devil-Binding Ritual contract (Asta's is an Equal Contract: the pair must synchronise thoughts, pain and "
              "motivation); a strict time limit of five minutes, then the form shatters and the user is incapacitated by muscle pain for about thirty minutes. "
              "The mod's Black Form toggle (antimagic.AntiMagicLordSkill, Demon Mode overlay, Aura gauge) is the form and is left as it is."),
    'SHADOW': dict(
        kind='FORM', name='Devil Unite (Nacht)', aura='', armour_pages={}, form_pages=[],
        external_pages=['unite_canis', 'unite_gallus', 'unite_canis_felis', 'unite_equus', 'unite_felis'],
        canon="Nacht Faust's Devil Unite modes with his devils (Slotos, Gimodelo, Plumede, Walgner): a body-wide transformation with its own armour overlay. "
              "The Unite pages (shadow.UniteState, blackclover.ModeArmor) are the explicit form pages and are left as they are."),
}

# ---------------------------------------------------------------------------------------------------------------------------------------------
# NONE: the reason, the render layers their casts used to equip before 1.99.0 (legacy), and the light effect that carries their look now.
# ---------------------------------------------------------------------------------------------------------------------------------------------
GENERIC = "No worn armour or transformation exists for this attribute in the anime; its spells are shots, zones, constructs and summons."

NONE_NOTES = {
    'EXPLOSION': GENERIC, 'MAGMA': GENERIC, 'ICE': "Ice spells are shots, walls and constructs (repo wiki: Heavenly Ice Fang, Cage of Infinite Hail ...); nothing is worn.",
    'MIST': GENERIC, 'STAR': GENERIC, 'STORM': GENERIC,
    'PLANT': "Plant Magic grows, heals and binds with plants; no one wears an armour of it.", 'SAND': GENERIC,
    'LIGHT': "Light Magic is rays, swords and healing (repo wiki: Healing Ray of Light, Light Sword of Judgment). The Light spirit form exists only as a spirit "
             "lord summon in this mod; a future Light Spirit Dive would be a FORM (follow-up), there is no worn armour to put on today.",
    'SWORD': GENERIC, 'POISON': GENERIC,
    'SPATIAL': "Spatial Magic opens gates and cuts space; nothing is worn. Dimensional Armour (Gate Garb) keeps its blow-turning effect but is no render layer.",
    'MIRROR': "Mirror Magic makes doubles and reflections; nothing is worn. Glass Garb keeps its reflecting effect but is no render layer.",
    'GRAVITY': "Gravity Magic crushes and lifts; nothing is worn. Weighted Blows keeps its heavier blows.",
    'TIME': "Time Magic stops, rewinds and quickens time (Julius Novachrono); nothing is worn.",
    'SEALING': "Seal Magic binds and seals; the ally seal shows as an orbiting seal effect, not as armour.",
    'REINFORCEMENT': GENERIC,
    'BONE': "Bone Magic makes weapons and constructs of bone; Bone Armor keeps its absorption and thorns but is no render layer.",
    'BLOOD': GENERIC, 'CREATION': "Creation Magic makes objects and weapons (Marx); nothing is worn.", 'COPY': GENERIC, 'ILLUSION': GENERIC, 'DREAM': GENERIC,
    'STEEL': GENERIC, 'THREAD': "Thread Magic binds and sews (Vanessa); nothing is worn.",
    'TRANSMUTATION': "Transmutation Magic changes the material of things (Grey); nothing is worn. Alchemic Plate keeps its damage cut but is no render layer.",
    'ASH': "Ash Magic makes clouds, soldiers and traps of ash (Zora); nothing is worn. Ash Shroud keeps its guard effect but is no render layer.",
    'COTTON': "Cotton Magic makes clouds and sheep (Charmy); nothing is worn.",
    'RECOMBINATION': "Recombination Magic reshapes houses and matter; nothing is worn. House Armour keeps its damage cut but is no render layer.",
    'PAINTING': "Painting Magic brings painted creatures to life (Rill); nothing is worn.", 'WORLD_TREE': "World Tree Magic grows great trees (William Vangeance); nothing is worn.",
    'DICE': GENERIC, 'SLASH': "Slash Magic (Jack the Ripper): jagged slashes of green mana; the forearm blades are a following effect again, not an armour.",
    'COMPASS': GENERIC, 'KOTODAMA': "Kotodama is a word-spell magic (the mod's Zagred); nothing is worn.", 'GAME': GENERIC,
    'DEMON_BEAST': GENERIC, 'BODY': "Body Magic reshapes tissue and heals (repo wiki: Body Compression); Titan Form and Hardened Frame keep their stats, the steam effect stays, no armour is drawn.",
    'EYE': GENERIC, 'EYEBALL': GENERIC, 'CURSE': GENERIC, 'CURSE_WARDING': GENERIC,
    'DEMON_FIRE': GENERIC, 'DEMON_ICE': GENERIC, 'DEMON_LIGHT': GENERIC, 'DEMON_WATER': GENERIC,
    'BARRIER': GENERIC, 'KEY': "Key Magic opens gates and locks; the Key Guard keys orbit the caster as constructs, nothing is worn.",
    'CHAIN': GENERIC, 'BUTOH': GENERIC,
    'BRIAR': "Briar Magic: the Queen of Briars wears a dress and crown of briars (repo wiki), but the mod has no briar armour painter (client.aura.BriarAura is an empty stub); "
             "if one is built it becomes a TOGGLE on Queen of Briars (follow-up).",
    'CHERRY_BLOSSOM': GENERIC, 'FUNGUS': GENERIC, 'FOOD': GENERIC, 'CRYSTAL': GENERIC,
    'BRONZE': "Bronze Magic (repo wiki: the Sekke spells) is shots and cannonballs of bronze; Bronze Armour keeps its resistance and thorns but is no render layer.",
    'COPPER': GENERIC, 'IRON': GENERIC, 'BLACK_OIL': GENERIC, 'GEL': GENERIC, 'GLASS': GENERIC, 'BUBBLE': GENERIC, 'ICE_WEDGE': GENERIC, 'LEGION': "Legion Magic makes chess-piece soldiers that look like suits of armour; they are summons, nothing is worn by the caster.",
    'AIR': "Air Magic compresses and cuts air; the Air Cushion keeps its fall protection and deflection but is no render layer.",
    'HAIR': GENERIC, 'MEMORY': GENERIC, 'MINERAL': GENERIC, 'MODIFICATION': GENERIC, 'MUCUS': GENERIC, 'MUD': GENERIC, 'NAIL': GENERIC,
    'PERMEATION': GENERIC, 'POISON_PLANT': GENERIC, 'RED_OCHRE': GENERIC, 'ROCK': GENERIC, 'SANDSTONE': GENERIC,
    'SCALE': GENERIC, 'SHAKUDO': "Shakudo Magic (the wiki: the magic of Cesc): shakudo constructs; the guard keeps its damage cut and answers but is no render layer.",
    'SKIN': GENERIC, 'SMOKE': GENERIC, 'SNOW': GENERIC, 'SONG': GENERIC, 'SOUL_CORPSE': GENERIC, 'SOUL': GENERIC, 'SOUND': GENERIC, 'SPIKE': GENERIC,
    'SWITCHING': GENERIC, 'TONGUE': GENERIC, 'TREE': GENERIC, 'STONE': GENERIC, 'VINE': GENERIC, 'VORTEX': GENERIC, 'WING': "Wing Magic: wings of mana are a flight spell, not a worn armour; the wing glow is a shimmer now.",
}

# The render layers (aura.Aura names) the casts of a NONE magic used to equip, before 1.99.0.
LEGACY = {
    'AIR': ['AIR'], 'SMOKE': ['SMOKE'], 'SNOW': ['SNOW'], 'VORTEX': ['VORTEX'], 'WING': ['WING'], 'ASH': ['ASH'],
    'SHAKUDO': ['SHAKUDO'], 'SCALE': ['SCALE'], 'SPIKE': ['SPIKE'], 'NAIL': ['NAIL'], 'HAIR': ['HAIR'],
    'SONG': ['SONG'], 'SOUND': ['SOUND'], 'SOUL': ['SOUL'], 'SOUL_CORPSE': ['SOUL_CORPSE'], 'MEMORY': ['MEMORY'],
    'SPATIAL': ['SPATIAL'], 'MIRROR': ['MIRROR'], 'PERMEATION': ['PERMEATION'], 'GRAVITY': ['GRAVITY'], 'SLASH': ['SLASH'],
    'TRANSMUTATION': ['TRANSMUTATION'], 'RECOMBINATION': ['RECOMBINATION'], 'MODIFICATION': ['MODIFICATION'],
    'TREE': ['TREE'], 'VINE': ['VINE'], 'PLANT': ['PLANT'], 'POISON_PLANT': ['POISON_PLANT'], 'MUD': ['MUD'], 'MUCUS': ['MUCUS'],
    'BRONZE': ['BRONZE'], 'KEY': ['KEY'], 'BODY': ['BODY'], 'BONE': ['BONE'], 'SEALING': ['SEALING'],
    'SANDSTONE': ['WIKI'], 'ROCK': ['WIKI'], 'RED_OCHRE': ['WIKI'], 'STONE': ['WIKI'], 'MINERAL': ['WIKI'], 'SWITCHING': ['WIKI'], 'SKIN': ['WIKI'], 'TONGUE': ['WIKI'],
}

# The light effect that carries the look of a NONE magic that lost its armour (a VfxShape name: the magic's own zone effect, drawn small and short).
SHIMMER_EARTH = 'EARTHFAM_ZONE'
SHIMMER_SPECIAL = {'SANDSTONE': SHIMMER_EARTH, 'ROCK': SHIMMER_EARTH, 'RED_OCHRE': SHIMMER_EARTH, 'STONE': SHIMMER_EARTH, 'MINERAL': SHIMMER_EARTH,
                   'SWITCHING': 'WIKI_MAGIC_FIELD', 'SKIN': 'WIKI_MAGIC_FIELD', 'TONGUE': 'WIKI_MAGIC_FIELD'}

DEFAULTS = {
    'toggle_bonus': dict(cut=0.0, attack=0.0, speed=0.0, knockback=0.0, ward='', senses=False, upkeep=0.004, absorb=0.0, hp_upkeep=0.0),
    'form_bonus': dict(cut=0.0, attack=0.0, speed=0.0, knockback=0.0, ward='', senses=False, upkeep=0.02, absorb=0.0, hp_upkeep=0.0),
}


def magic_names():
    src = open(MAGIC_TYPE, encoding='utf-8').read()
    return re.findall(r'^\s{4}([A-Z_]+)\("([^"]+)",\s*"([^"]+)",\s*"([A-Z]+)"', src, re.M)


def vfx_shapes():
    src = open(os.path.join(ROOT, 'src/main/java/com/newuniverse/nusmp/vfx/VfxShape.java'), encoding='utf-8').read()
    return set(re.findall(r'\b([A-Z][A-Z0-9_]+)\b', src))


def build():
    shapes = vfx_shapes()
    names = magic_names()
    out = {}
    for key, display, soul in [(n[0], n[1], n[3]) for n in names]:
        if key in ARMOURED:
            a = dict(ARMOURED[key])
            e = {'kind': a.pop('kind'), 'name': a.pop('name'), 'aura': a.pop('aura')}
            if 'form_name' in a:
                e['form_name'] = a.pop('form_name')
            if a.pop('form_aura', False):
                e['form_aura'] = True
            e['armour_pages'] = a.pop('armour_pages')
            if a.get('form_pages'):
                e['form_pages'] = a.pop('form_pages')
            else:
                a.pop('form_pages', None)
            if a.get('external_pages'):
                e['external_pages'] = a.pop('external_pages')
            else:
                a.pop('external_pages', None)
            if 'style_by' in a:
                e['style_by'] = a.pop('style_by')
            for k in ('bonus', 'form_bonus'):
                if k in a:
                    e[k] = {kk: vv for kk, vv in a.pop(k).items()}
            e['canon'] = a.pop('canon')
            assert not a, (key, a)
        else:
            e = {'kind': 'NONE'}
            if key in LEGACY:
                e['legacy_auras'] = LEGACY[key]
                shim = SHIMMER_SPECIAL.get(key, key + '_FX2')
                assert shim in shapes, 'no VfxShape ' + shim
                e['shimmer'] = shim
            e['canon'] = NONE_NOTES.get(key, GENERIC)
        out[key] = e
    return {'version': '1.99.0', 'defaults': DEFAULTS, 'magics': out}


def table_md(policy):
    names = {n[0]: n[1] for n in magic_names()}
    rows = ['| Magic | Kind | Armour / form | Explicit page(s) | Canon |', '|---|---|---|---|---|']
    for key, e in policy['magics'].items():
        pages = list(e.get('armour_pages', {}).keys()) + [p + ' (form)' for p in e.get('form_pages', [])] + [p + ' (older system)' for p in e.get('external_pages', [])]
        rows.append('| %s (`%s`) | %s | %s | %s | %s |' % (names[key], key, e['kind'], e.get('name', 'none'), ', '.join('`%s`' % p for p in pages) or '-', e['canon'].replace('|', '/')))
    return '\n'.join(rows) + '\n'


START = '<!-- policy table: generated by tools/gen_armour_policy.py -->'
END = '<!-- end of generated table -->'


def write_table(md):
    """Puts the table between the two markers of docs/rebuild/armour_policy.md (the rest of that file is written by hand)."""
    if not os.path.exists(OUT_MD):
        return
    s = open(OUT_MD, encoding='utf-8').read()
    if START in s and END in s:
        s = s[:s.index(START) + len(START)] + '\n\n' + md + '\n' + s[s.index(END):]
        open(OUT_MD, 'w', encoding='utf-8').write(s)


NOTE_MARK = '## 1.99.0 armour policy'


def doc_note(key, display, e):
    """The short append-only note for docs/attributes/<key>_rebuild.md."""
    pol = 'See `docs/rebuild/armour_policy.md` for the table and the canon reasons; the code is `book.ArmourPolicy` (data: `data/nusmp/armour_policy.json`).'
    if e['kind'] == 'NONE':
        look = ''
        if e.get('shimmer'):
            look = (' Where the armour carried the magic\'s look, every cast of a family page now sends a small `%s` glow that follows the caster instead '
                    '(no armour piece is drawn).' % e['shimmer'])
        return ('\n' + NOTE_MARK + '\n\n%s has no armour in the anime, so since 1.99.0 **no cast puts the temporary armour of this document on the caster** '
                '(policy kind NONE). Everything described above as the armour (the render layer, its painter, geo models, textures and generator) is KEPT on disk and '
                'registered in `AuraRegistry`, so old saves and tests still resolve; it is simply never started by a page. What the pages did besides the armour '
                '(damage cut, thorns, resistance, constructs, effects) is unchanged.%s %s\n') % (display, look, pol)
    pages = ', '.join('`%s`' % p for p in e.get('armour_pages', {}))
    forms = ', '.join('`%s`' % p for p in e.get('form_pages', []))
    parts = []
    if pages:
        parts.append('The armour is put on **only** by %s (cast it to forge the armour in with the Djinn equip animation; cast it again to take it off, free of cost). '
                     'It stays until it is toggled off, the mana runs out (a little mana is paid every second), the wearer is knocked out or dies, and shatters out with the same animation; '
                     'while it is on it gives the stats of the policy table. A cast of any other page never puts it on and never resets it.' % pages)
    if forms:
        parts.append('The form is %s: Spirit Dive. It is held as long as the mana upkeep is paid; the spirit\'s ambient intake pays most of it; speed, attack and senses are raised while it lasts.' % forms)
    if e.get('external_pages'):
        parts.append('The older form pages (%s) are left as they were; they are explicit pages that start the form overlay of `blackclover.ModeArmor`.' % ', '.join('`%s`' % p for p in e['external_pages']))
    return ('\n' + NOTE_MARK + '\n\n%s is policy kind %s (%s). %s %s\n') % (display, e['kind'], e.get('name', ''), ' '.join(parts), pol)


def append_notes(policy):
    """Appends the note to every docs/attributes/*_rebuild.md that has none yet (append only; safe to run twice)."""
    names = {n[0]: n[1] for n in magic_names()}
    d = os.path.join(ROOT, 'docs/attributes')
    n = 0
    for f in sorted(os.listdir(d)):
        if not f.endswith('_rebuild.md'):
            continue
        key = f[:-len('_rebuild.md')].upper()
        if key not in policy['magics']:
            print('no magic for', f)
            continue
        path = os.path.join(d, f)
        s = open(path, encoding='utf-8').read()
        if NOTE_MARK in s:
            continue
        if not s.endswith('\n'):
            s += '\n'
        open(path, 'w', encoding='utf-8').write(s + doc_note(key, names[key], policy['magics'][key]))
        n += 1
    print('notes appended to', n, 'docs')


def main():
    policy = build()
    if '--notes' in sys.argv:
        append_notes(policy)
        return 0
    os.makedirs(os.path.dirname(OUT_JSON), exist_ok=True)
    with open(OUT_JSON, 'w', encoding='utf-8') as f:
        json.dump(policy, f, indent=1, ensure_ascii=False)
        f.write('\n')
    write_table(table_md(policy))
    kinds = {}
    for e in policy['magics'].values():
        kinds[e['kind']] = kinds.get(e['kind'], 0) + 1
    print('wrote', OUT_JSON, len(policy['magics']), 'magics', kinds)


if __name__ == '__main__':
    sys.exit(main())
