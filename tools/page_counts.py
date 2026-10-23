#!/usr/bin/env python3
"""How many spell pages does every grimoire really have?  (read-only on the sources; no game needed)

Rebuilds, for EVERY grimoire book, the final ordered family page list that `GrimoireBook.familyAll()` gives at runtime:

    book's own pages (class / factory method / switch case, the *Ext / *Arts / *Spells `pages()` it joins)
      -> CharacterPages / CharacterPagesBulls / CharacterPagesGold   (character pages appended inside the book class)
      -> CharacterPagesRest.append                                    (GrimoireBook.familyAll)
      -> BasicPages.append                                            (GrimoireBook.familyAll, last of all)

`pages()` then adds the universal pages (Spirit Dive + Spirit Burst for the four spirit books, Devil Union, Summon, Store / Draw Weapon,
Mana Strike / Zone, the wizard's signature spells, Omni Volley, Pressure Space, True Magic, the spirit control pages); they are NOT spells of
the family and are not counted here (they come after `familyCount()`).

Visibility (charset.CharacterSetRegistry.hidden):
  * basic grimoire (bound to nobody): everything except mode 0 and except the pages some character set marks `exclusive` or
    charset.BasicSignatures reserves (Soul Corpse numbered undead stay listed: SoulCorpseAccess.alwaysListed);
  * grimoire bound to a character with a set in that book: mode 0 + the pages of his set + the pages reserved for him.

    python3 -B tools/page_counts.py                 print the table
    python3 -B tools/page_counts.py --golden        print the golden file (src/test/resources/pages/page_counts.golden.txt)
    python3 -B tools/page_counts.py --md            print the generated tables (docs/magic/page_counts.md section 7)
    python3 -B tools/page_counts.py --md-update     rewrite the two generated blocks of docs/magic/page_counts.md (tables, phase 2 plan)
    python3 -B tools/page_counts.py --wiki          wiki spells (repo copies in .github/wiki) that are not yet a page of the book
    python3 -B tools/page_counts.py --plan          per book: shortfall, wiki gap, reusable VfxShape / PropKind
    python3 -B tools/page_counts.py --ids BOOK      print the ordered page ids of one book (hand verification)
    python3 -B tools/page_counts.py --check         exit 1 if a set lists a page its book does not have (a sign the extractor is wrong)

Every number is read from the sources; the only things done by hand are the dispatches the compiler resolves (which Ext a magic joins, which
switch case of EarthFamilyBooks / TreeRebuild / SongRebuild a magic uses): see SPECIAL below.  The ORDER of ids inside a book is exact for the
plain books and approximate where a book joins several lists (mode 0 is always right); the COUNTS are what the tables use.
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
SRC = os.path.join(ROOT, 'src/main/java/com/newuniverse/nusmp')
SETS = os.path.join(ROOT, 'src/main/resources/data/nusmp/character_spells')
GOAL = 15

# ------------------------------------------------------------------------------------------------ source helpers
_strip_cache = {}


def strip(src):
    """Java source without comments (strings are kept, so '//' inside a string is safe)."""
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == '\\' else 1
            out.append(src[i:j + 1]); i = j + 1
        elif c == "'":
            j = i + 1
            while j < n and src[j] != "'":
                j += 2 if src[j] == '\\' else 1
            out.append(src[i:j + 1]); i = j + 1
        elif src.startswith('//', i):
            j = src.find('\n', i)
            i = n if j < 0 else j
        elif src.startswith('/*', i):
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
        else:
            out.append(c); i += 1
    return ''.join(out)


def read(path):
    if path not in _strip_cache:
        with open(path, encoding='utf-8') as f:
            _strip_cache[path] = strip(f.read())
    return _strip_cache[path]


CLASS_FILE = {}
for p in glob.glob(SRC + '/**/*.java', recursive=True):
    CLASS_FILE.setdefault(os.path.basename(p)[:-5], p)


def match_close(s, open_idx, o='(', c=')'):
    """Index of the bracket that closes the one at open_idx (strings skipped)."""
    depth, i, n = 0, open_idx, len(s)
    while i < n:
        ch = s[i]
        if ch == '"':
            i += 1
            while i < n and s[i] != '"':
                i += 2 if s[i] == '\\' else 1
        elif ch == o:
            depth += 1
        elif ch == c:
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return n - 1


def method_region(text, name, ret=r'(?:List<BookPage>|GrimoireBook|\w+Book)'):
    """Text of `static <ret> name(...) { ... }` (first match), or None."""
    m = re.search(r'\b(?:public |private |protected )?(?:static )?' + ret + r'\s+' + re.escape(name) + r'\s*\(', text)
    if not m:
        return None
    close = match_close(text, m.end() - 1)
    b = text.find('{', close)
    if b < 0:
        return None
    return text[m.start():match_close(text, b, '{', '}') + 1]


# ------------------------------------------------------------------------------------------------ page definitions
ID = r'("[A-Za-z0-9_]+"|[A-Za-z_][\w.]*)'
DEF = re.compile(r'(?:\bBookPage\.)?\b(?:starter|mid|zone|signature|daily)\(\s*' + ID + r'\s*,|\bnew\s+BookPage\(\s*' + ID + r'\s*,|\bword\(\s*("[A-Za-z0-9_]+")\s*,')
MODEPAGE = re.compile(r'\bmodePage\(\s*SongMode\.(\w+)\s*\)')           # song.SongPages: the page id is the enum's pageId
REF = re.compile(r'\b(\w+)\.(pages|latePages)\(([^)]*)\)')
NEWBOOK = re.compile(r'\bnew\s+(\w+Book)\(')
CONST_CACHE = {}


def const(name, path=None):
    """Resolve `Class.NAME` / `NAME` (a String constant, also `String A = "x", B = "y";`) to its literal, or None."""
    key = (name, path if '.' not in name else None)
    if key in CONST_CACHE:
        return CONST_CACHE[key]
    cls, _, field = name.rpartition('.')
    val = None
    if cls and cls.split('.')[-1] in CLASS_FILE:
        files = [CLASS_FILE[cls.split('.')[-1]]]
    elif cls:
        files = list(CLASS_FILE.values())
    else:
        files = ([path] if path else []) + list(CLASS_FILE.values())
    for f in files:
        m = re.search(r'(?<![\w.])' + re.escape(field) + r'\s*=\s*"([^"]*)"', read(f))
        if m:
            val = m.group(1); break
    CONST_CACHE[key] = val
    return val


def pid(tok, path=None):
    if tok.startswith('"'):
        return tok[1:-1]
    if re.fullmatch(r'[A-Z][A-Z0-9_]*', tok.split('.')[-1]):
        return const(tok, path)
    return None          # a variable (the helper's own `id` parameter), not a page


WARN = []
NAMES = {}          # page id -> display name (second string literal of the page definition), for the wiki comparison


def ids_in(text, path, depth=0, follow=True):
    """Ordered page ids defined in `text`, following `X.pages()` / `X.latePages()` calls and `new XBook(` into their own text."""
    out = []
    if depth > 4:
        return out
    pos = [(m.start(), 'def', m) for m in DEF.finditer(text)]
    pos += [(m.start(), 'mode', m) for m in MODEPAGE.finditer(text)]
    if follow:
        pos += [(m.start(), 'ref', m) for m in REF.finditer(text)]
        pos += [(m.start(), 'new', m) for m in NEWBOOK.finditer(text)]
    if follow:
        for lm in re.finditer(r'static\s+List<BookPage>\s+(\w+)\(\)', read(path)):
            name = lm.group(1)
            if name in ('pages', 'familyList'):
                continue
            for q in re.finditer(r'(?<![\w.])' + name + r'\(\)', text):
                if not text[max(0, q.start() - 30):q.start()].rstrip().endswith('List<BookPage>'):
                    pos.append((q.start(), 'local', q))
    pos.sort(key=lambda t: t[0])
    deferred = []
    for _, kind, m in pos:
        if kind == 'local':
            region = method_region(read(path), m.group(0)[:-2], r'List<BookPage>')
            if region and region not in text:
                out.extend(ids_in(region, path, depth + 1))
            continue
        if kind == 'mode':
            e = re.search(r'\b' + m.group(1) + r'\(\s*\d+\s*,\s*"([a-z0-9_]+)"', read(CLASS_FILE['SongMode']))
            if e:
                out.append(e.group(1))
            continue
        if kind == 'def':
            tok = m.group(1) or m.group(2) or m.group(3)
            v = pid(tok, path)
            if v is None:
                if '.' in tok or tok.isupper():
                    WARN.append('%s: unresolved page id %s' % (os.path.basename(path), tok))
                continue
            out.append(v)
            nm = re.match(r'\s*"([^"]*)"', text[m.end():])
            if nm:
                NAMES.setdefault(v, nm.group(1))
        elif kind == 'ref':
            cls, meth, arg = m.group(1), m.group(2), m.group(3).strip()
            f = CLASS_FILE.get(cls)
            if not f or f == path:
                continue
            body = read(f)
            region = method_region(body, meth, r'(?:List<BookPage>|Set<\w+>|\w+)')
            if cls == 'ExclusiveExt':                      # switch on the magic: take the case of the argument
                mg = re.search(r'MagicType\.(\w+)', arg)
                out.extend(exclusive(mg.group(1) if mg else None, depth))
                continue
            if region is None:
                WARN.append('%s: %s.%s() has no method body' % (os.path.basename(path), cls, meth))
                continue
            got = ids_in(region, f, depth + 1)
            out.extend(got or ids_in(body, f, depth + 1))      # `return PAGES;`: the list is a field of the class
        else:
            cls = m.group(1)
            f = CLASS_FILE.get(cls)
            if f and f != path and cls not in ('ElementBook',):
                deferred.extend(ids_in(read(f), f, depth + 1, follow=False))      # BoneBook's remake pages come after the list handed to its constructor
    return out + deferred


def exclusive(magic, depth=0):
    f = CLASS_FILE['ExclusiveExt']
    body = read(f)
    m = re.search(r'case\s+' + re.escape(magic or '?') + r'\s*->\s*([^;]+);', body)
    if not m:
        return []
    c = re.search(r'(\w+)\.pages\(\)', m.group(1))
    if not c or c.group(1) not in CLASS_FILE:
        return []
    cf = CLASS_FILE[c.group(1)]
    region = method_region(read(cf), 'pages', r'\w+(?:<\w+>)?')
    got = ids_in(region, cf, depth + 1) if region else []
    return got or ids_in(read(cf), cf, depth + 1)


def uniq(ids):
    seen, out = set(), []
    for i in ids:
        if i not in seen:
            seen.add(i); out.append(i)
    return out


# ------------------------------------------------------------------------------------------------ the registered books
def registered_books():
    """[(registry path, class, factory or '')] in NUSkills order, then the generic wiki attributes (MagicType names)."""
    t = read(os.path.join(SRC, 'skill/NUSkills.java'))
    out = []
    for m in re.finditer(r'book\("(book_[a-z_]+|forbidden_magic)",\s*com\.newuniverse\.nusmp\.book\.(\w+)(?:::(\w+))?\)', t):
        out.append((m.group(1), m.group(2), m.group(3) or ''))
    return out


WIKI_NEW = ['AIR', 'HAIR', 'MEMORY', 'MINERAL', 'MODIFICATION', 'MUCUS', 'MUD', 'NAIL', 'PERMEATION', 'POISON_PLANT', 'RED_OCHRE', 'ROCK', 'SANDSTONE', 'SCALE',
            'SHAKUDO', 'SKIN', 'SMOKE', 'SNOW', 'SONG', 'SOUL_CORPSE', 'SOUL', 'SOUND', 'SPIKE', 'SWITCHING', 'TONGUE', 'TREE', 'STONE', 'VINE', 'VORTEX', 'WING']
# the dispatch the compiler resolves for a generic wiki attribute (WikiBooks.attribute): which book class / factory builds it
SPECIAL = {
    'AIR': ('AirBook', 'create'), 'SMOKE': ('SmokeBook', None), 'SNOW': ('SnowBook', None), 'WING': ('WingBook', None), 'VORTEX': ('VortexBook', None),
    'MODIFICATION': ('ModificationBook', None),
    'SONG': ('SongBook', None), 'SOUND': ('SoundBook', None), 'SOUL': ('SoulBook', None), 'SOUL_CORPSE': ('SoulCorpseBook', None), 'MEMORY': ('MemoryBook', None),
    'TREE': ('TreeArts', 'pages'), 'VINE': ('VineArts', 'pages'), 'POISON_PLANT': ('PoisonPlantArts', 'pages'), 'MUD': ('MudArts', 'pages'), 'MUCUS': ('MucusArts', 'pages'),
}
EARTH_CASES = ('SANDSTONE', 'ROCK', 'RED_OCHRE', 'STONE', 'MINERAL')


def generic_five():
    t = read(CLASS_FILE['WikiBooks'])
    region = method_region(t, 'attribute', r'GrimoireBook')
    return ids_in(region[region.index('String name'):], CLASS_FILE['WikiBooks'], follow=False)


def earth_case(magic):
    t = read(CLASS_FILE['EarthFamilyBooks'])
    start = t.index('case ' + magic + ' ->') if magic != 'MINERAL' else t.index('default -> List.of(')
    ends = [t.find(k, start + 8) for k in ('case ', 'default ->', 'return new EarthFamilyBook')]
    end = min(e for e in ends if e > 0)
    return ids_in(t[start:end], CLASS_FILE['EarthFamilyBooks'], follow=False)


def family_of_wiki(magic):
    """Own pages of a generic wiki attribute (the five generic ids, the exclusive ext, or the rebuilt book)."""
    if magic in EARTH_CASES:
        return earth_case(magic)
    if magic in SPECIAL:
        cls, meth = SPECIAL[magic]
        f = CLASS_FILE[cls]
        body = read(f)
        if meth == 'pages':
            region = method_region(body, 'pages', r'\w+(?:<\w+>)?')
            return ids_in(region, f) + exclusive(magic)
        return book_ids(cls, '', body)
    return generic_five() + exclusive(magic)


def book_ids(cls, factory, body=None):
    """Own pages of a registered book class (or one factory method of a shared class)."""
    f = CLASS_FILE[cls]
    body = body if body is not None else read(f)
    if factory and factory != 'new':
        region = method_region(body, factory, r'GrimoireBook')
        if region is None:
            raise SystemExit('no factory %s.%s' % (cls, factory))
        return ids_in(region, f)
    return ids_in(body, f)


# the character pages appended inside the book classes: CharacterPages (Yuno ...), Bulls, Gold; and the two engine appenders
def appenders():
    """{class: {book: [ids]}} read from the `register("book_x", ... List.of(...))` blocks."""
    out = {}
    for cls in ('CharacterPages', 'CharacterPagesBulls', 'CharacterPagesGold', 'CharacterPagesRest', 'BasicPages'):
        t = read(CLASS_FILE[cls])
        d = {}
        for m in re.finditer(r'register\(\s*"(book_[a-z_]+)"\s*,', t):
            j = t.index('List.of(', m.end())
            d[m.group(1)] = ids_in(t[j:match_close(t, j + 7)], CLASS_FILE[cls], follow=False)
        out[cls] = d
    return out


def used_by_book(cls):
    """Which appender classes does the book source (or ElementBook for the magic-keyed ones) call?  Returns the set of books whose class calls `cls.<fn>(`."""
    calls = {}
    for f in glob.glob(SRC + '/book/*.java'):
        n = os.path.basename(f)[:-5]
        if n.startswith('CharacterPages') or n == 'BasicPages':
            continue
        for m in re.finditer(r'\b' + cls + r'\.(\w+)\(', read(f)):
            calls.setdefault(m.group(1), set()).add(n)
    return calls


# ------------------------------------------------------------------------------------------------ the engine's visibility rules
def basic_signatures():
    t = read(CLASS_FILE['BasicSignatures'])
    return [(m.group(1), m.group(2), m.group(3)) for m in re.finditer(r'reserve\("(book_[a-z_]+)",\s*"([a-z0-9_]+)",\s*"([a-z]+)"', t)]


def load_sets():
    sets = {}
    for f in sorted(glob.glob(SETS + '/*.json')):
        j = json.load(open(f, encoding='utf-8'))
        sets[j['canon']] = j
    return sets


def soul_corpse_always_listed():
    t = read(CLASS_FILE['SoulCorpseAccess'])
    m = re.search(r'isRadesOnly\([^)]*\)\s*\{(.*?)\n    \}', t, re.S)
    ids = re.findall(r'"([a-z0-9_]+)"', m.group(1)) if m else []
    arr = re.search(r'RADES_ONLY\s*=\s*(?:Set|List)\.of\((.*?)\)', t, re.S)
    if arr:
        ids = re.findall(r'"([a-z0-9_]+)"', arr.group(1))
    return set(ids)


# ------------------------------------------------------------------------------------------------ the model
def build():
    ap = appenders()
    books = {}          # registry path -> dict
    order = []

    def add(path, label, own, kind):
        order.append(path)
        books[path] = {'path': path, 'label': label, 'kind': kind, 'own': uniq(own)}

    for path, cls, factory in registered_books():
        add(path, cls + ('.' + factory if factory else ''), book_ids(cls, factory), 'class')
    reg_paths = set(order)
    for magic in WIKI_NEW:
        path = 'book_' + magic.lower()
        if path in reg_paths:
            continue
        add(path, 'WikiBooks.' + magic, family_of_wiki(magic), 'wiki')
    # magic-keyed ElementBook appenders (Bulls.forMagic) and the per-book ones: read what the book calls
    for name, d in ap.items():
        for b, ids in d.items():
            if b in books:
                books[b].setdefault('app', {})[name] = ids
    # the ordered final lists
    for b in books.values():
        a = b.get('app', {})
        seq = b['own'] + a.get('CharacterPages', []) + a.get('CharacterPagesBulls', []) + a.get('CharacterPagesGold', []) + a.get('CharacterPagesRest', []) + a.get('BasicPages', [])
        b['pages'] = uniq(seq)
    return books, order, ap


def analyse():
    books, order, ap = build()
    sets = load_sets()
    sigs = basic_signatures()
    reserved = {(b, p): o for b, p, o in sigs}
    alw = soul_corpse_always_listed()
    excl = {}
    for canon, j in sets.items():
        for s in j['spells']:
            if s.get('exclusive'):
                excl[(j['book'], s['page'])] = canon
    res = []
    for path in order:
        b = books[path]
        pages = b['pages']
        basic = []
        for k, p in enumerate(pages):
            hid = k > 0 and not (path == 'book_soul_corpse' and p in alw) and ((path, p) in excl or (path, p) in reserved)
            if not hid:
                basic.append(p)
        chars = {}
        missing = {}
        for canon, j in sets.items():
            if j['book'] != path:
                continue
            listed = [s['page'] for s in j['spells']]
            vis = [p for k, p in enumerate(pages) if k == 0 or p in listed or reserved.get((path, p)) == canon]
            chars[canon] = len(vis)
            miss = [p for p in listed if p not in pages]
            if miss:
                missing[canon] = miss
        a = b.get('app', {})
        res.append({
            'book': path, 'label': b['label'], 'magic': path[5:] if path.startswith('book_') else path, 'pages': pages, 'total': len(pages),
            'own': len(b['own']), 'char_pages': sum(len(a.get(k, [])) for k in ('CharacterPages', 'CharacterPagesBulls', 'CharacterPagesGold')),
            'rest': len(a.get('CharacterPagesRest', [])), 'basic_added': len(a.get('BasicPages', [])),
            'basic': len(basic), 'hidden_basic': len(pages) - len(basic), 'chars': chars, 'set_missing': missing,
            'hidden_excl': sum(1 for k, p in enumerate(pages) if k > 0 and (path, p) in excl and not (path == 'book_soul_corpse' and p in alw)),
            'hidden_resv': sum(1 for k, p in enumerate(pages) if k > 0 and (path, p) in reserved and (path, p) not in excl),
        })
    return res, sets


# ------------------------------------------------------------------------------------------------ shortfalls and the wiki comparison
def shortfalls(res, sets):
    """Per book: pages missing for the basic grimoire, per character grimoire, and the new pages needed if every new page is shared."""
    excl = {}
    for canon, j in sets.items():
        for s in j['spells']:
            if s.get('exclusive'):
                excl[(j['book'], s['page'])] = canon
    resv = {(b, p): o for b, p, o in basic_signatures()}
    out = {}
    for r in res:
        b = r['book']
        need_basic = max(0, GOAL - r['basic'])
        chars = {}
        for c, n in r['chars'].items():
            pool = [p for k, p in enumerate(r['pages']) if k == 0 or (((b, p) not in excl or excl[(b, p)] == c) and ((b, p) not in resv or resv[(b, p)] == c))]
            chars[c] = {'now': n, 'short': max(0, GOAL - n), 'pool': len(pool), 'new_needed': max(0, GOAL - len(pool))}
        new_pages = max([need_basic] + [v['new_needed'] for v in chars.values()])
        out[b] = {'need_basic': need_basic, 'never_written': max(0, GOAL - r['total']), 'chars': chars, 'new_pages': new_pages,
                  'family_after': r['total'] + new_pages}
    return out


WIKI_DIR = os.path.join(ROOT, '.github', 'wiki')
WIKI_BOOK = {'barrier_magic_attribute': 'book_barrier', 'imitation_magic': 'book_copy', 'seal_magic': 'book_sealing', 'world_tree_magic': 'book_world_tree'}


def wiki_spells():
    """{book: [spell names]} from the repo's own copies of the English wiki attribute pages (`-- spell pages:` line), where a page copy exists."""
    out = {}
    for f in sorted(glob.glob(WIKI_DIR + '/*magic*.txt')):
        stem = os.path.basename(f)[:-4]
        book = WIKI_BOOK.get(stem) or 'book_' + re.sub(r'_magic$', '', stem)
        for line in open(f, encoding='utf-8'):
            if line.startswith('-- spell pages:'):
                names = [x.strip() for x in line.split(':', 1)[1].split(';') if x.strip() and not x.strip().startswith('(none')]
                out[book] = [re.sub(r'\s*\(spell\)$', '', n) for n in names]
    return out


def norm(s):
    return re.sub(r'[^a-z0-9]', '', s.lower().replace('é', 'e').replace('è', 'e').replace('ö', 'o').replace('ê', 'e'))


def wiki_gap(res, wiki):
    """{book: (listed, missing)}: wiki spell names of the book's attribute page that no page of the book carries yet (name or id contains it)."""
    out = {}
    for r in res:
        names = wiki.get(r['book'])
        if names is None:
            continue
        have = [norm(NAMES.get(p, '')) + '|' + norm(p) for p in r['pages']]
        miss = []
        for n in names:
            k = norm(n)
            if not any(k in h for h in have):
                miss.append(n)
        out[r['book']] = (names, miss)
    return out


# ------------------------------------------------------------------------------------------------ outputs
def golden(res):
    lines = ['# grimoire page counts per book (family pages: GrimoireBook.familyCount()), made by tools/page_counts.py; phase 2 may only INCREASE these (PageCountsTest)',
             '# book total basic_visible | character=pages_visible ...']
    for r in sorted(res, key=lambda r: r['book']):
        ch = ' '.join('%s=%d' % (c, n) for c, n in sorted(r['chars'].items()))
        lines.append('%s %d %d |%s' % (r['book'], r['total'], r['basic'], (' ' + ch) if ch else ''))
    return '\n'.join(lines) + '\n'


def table(res, sets):
    sf = shortfalls(res, sets)
    rows = sorted(res, key=lambda r: (-sf[r['book']]['need_basic'], r['book']))
    print('%-22s %5s %5s %6s  %s' % ('book', 'total', 'basic', 'short', 'characters (pages visible)'))
    for r in rows:
        print('%-22s %5d %5d %6d  %s' % (r['book'], r['total'], r['basic'], sf[r['book']]['need_basic'], ' '.join('%s=%d' % (c, n) for c, n in sorted(r['chars'].items()))))


def totals(res, sets):
    sf = shortfalls(res, sets)
    t = {'books': len(res), 'pages': sum(r['total'] for r in res), 'basic_pages': sum(r['basic'] for r in res),
         'short_basic': sum(v['need_basic'] for v in sf.values()), 'books_short_basic': sum(1 for v in sf.values() if v['need_basic']),
         'never_written': sum(v['never_written'] for v in sf.values()), 'chars': sum(len(r['chars']) for r in res),
         'short_chars': sum(c['short'] for v in sf.values() for c in v['chars'].values()),
         'chars_short': sum(1 for v in sf.values() for c in v['chars'].values() if c['short']),
         'new_pages_all': sum(v['new_pages'] for v in sf.values())}
    t['hidden_part'] = t['short_basic'] - t['never_written']
    return t


def magic_names():
    """{MAGIC_NAME: display name} from blackclover/MagicType.java."""
    t = read(CLASS_FILE['MagicType'])
    return {m.group(1): m.group(2) for m in re.finditer(r'^\s{4}([A-Z_]+)\("([^"]+)"', t, re.M)}


def book_magic_name(path, names):
    key = 'FLAME' if path == 'book_fire' else path[5:].upper() if path.startswith('book_') else 'FORBIDDEN'
    return names.get(key) or ('Forbidden Magic (devil book)' if path == 'forbidden_magic' else path)


def assets_for(path):
    """VfxShape / PropKind constants named after the book's magic (what a new page of this book can reuse without a new asset)."""
    key = 'FLAME' if path == 'book_fire' else path[5:].upper()
    shapes = re.findall(r'^\s*([A-Z][A-Z0-9_]*)[,;(]', read(CLASS_FILE['VfxShape']), re.M)
    props = re.findall(r'^\s*([A-Z][A-Z0-9_]*)[,;(]', read(CLASS_FILE['PropKind']), re.M)
    alias = {'BOOK_COPY': 'IMITATION'}
    k2 = [key] + ([alias[path.upper()]] if path.upper() in alias else []) + (['EARTHFAM'] if key in ('SANDSTONE', 'ROCK', 'RED_OCHRE', 'STONE', 'MINERAL') else []) \
        + (['DARKFX'] if key == 'DARK' else ['WATERFX'] if key == 'WATER' else ['LIGHTNINGFX'] if key == 'LIGHTNING' else ['FLAMEFX', 'FIRE'] if key == 'FLAME' else [])
    def pick(names):
        return [n for n in names if any(n == k or n.startswith(k + '_') or n.startswith(k + 'FX') for k in k2)]
    return pick(shapes), pick(props)


def render_tables(res, sets):
    sf = shortfalls(res, sets)
    mn = magic_names()
    t = totals(res, sets)
    out = []
    out.append('### Totals\n')
    out.append('| What | Number |\n|---|---|')
    out.append('| Grimoire books | %d |' % t['books'])
    out.append('| Family pages registered (all books) | %d (average %.1f per book) |' % (t['pages'], t['pages'] / t['books']))
    out.append('| Pages the BASIC grimoires show | %d (average %.1f per book) |' % (t['basic_pages'], t['basic_pages'] / t['books']))
    out.append('| Books whose basic grimoire is under %d | %d of %d |' % (GOAL, t['books_short_basic'], t['books']))
    out.append('| **Spells missing for "every basic grimoire = %d"** | **%d** (of which never written: %d; written but hidden in the basic book: %d) |' % (GOAL, t['short_basic'], t['never_written'], t['hidden_part']))
    out.append('| Character grimoires (one per character spell set) | %d, of which %d are under %d |' % (t['chars'], t['chars_short'], GOAL))
    out.append('| Pages missing from the character grimoires (sum of 15 - pages visible) | %d |' % t['short_chars'])
    out.append('| **New shared pages so that the basic grimoire AND every character grimoire can reach %d** | **%d** (the %d pages of the basic fix also lift every character\'s pool to 15; the characters\' sets then have to LIST them) |' % (GOAL, t['new_pages_all'], t['short_basic']))
    out.append('')
    nh = [r for r in res if r['hidden_basic'] == 0]
    hh = [r for r in res if r['hidden_basic'] > 0]
    nw_nh = sum(max(0, GOAL - r['total']) for r in nh)
    nw_h = sum(max(0, GOAL - r['total']) for r in hh)
    sb_h = sum(max(0, GOAL - r['basic']) for r in hh)
    out.append('### Why the numbers are low (the causes, in spells)\n')
    out.append('| Cause | Books | Spells |\n|---|---|---|')
    out.append('| A. The book was only ever written with fewer than %d pages and nothing in it is hidden from the basic grimoire | %d | %d |' % (GOAL, sum(1 for r in nh if r['total'] < GOAL), nw_nh))
    out.append('| B. The book has pages hidden in the basic grimoire (exclusive to a character set: %d pages; reserved in `charset.BasicSignatures`: %d pages), and is ALSO under %d pages in total | %d | %d never written + %d hidden = %d |' % (
        sum(r['hidden_excl'] for r in res), sum(r['hidden_resv'] for r in res), GOAL, sum(1 for r in hh if r['total'] < GOAL), nw_h,
        sb_h - nw_h, sb_h))
    out.append('| C. Total of A + B (the missing spells for "basic = %d") | %d | %d |' % (GOAL, t['books_short_basic'], t['short_basic']))
    out.append('| Of the %d: spells that do not exist yet (book total below %d) | | %d |' % (t['short_basic'], GOAL, t['never_written']))
    out.append('| Of the %d: spells that exist but the basic grimoire hides | | %d |' % (t['short_basic'], t['hidden_part']))
    out.append('| Books with %d or more pages registered | %d | - |' % (GOAL, sum(1 for r in res if r['total'] >= GOAL)))
    out.append('| Of those, books whose basic grimoire still shows fewer than %d | %d | %d |' % (GOAL, sum(1 for r in res if r['total'] >= GOAL > r['basic']), sum(GOAL - r['basic'] for r in res if r['total'] >= GOAL > r['basic'])))
    out.append('')
    out.append('Histogram of pages per book: ' + ', '.join('%d pages: %d books' % (k, v) for k, v in sorted(__import__('collections').Counter(r['total'] for r in res).items())) + ' (registered); '
               + ', '.join('%d: %d' % (k, v) for k, v in sorted(__import__('collections').Counter(r['basic'] for r in res).items())) + ' (shown in the basic grimoire, pages: books).\n')
    out.append('### Per book (sorted by shortfall of the basic grimoire)\n')
    out.append('"total" = family pages registered; "hidden" = pages the basic grimoire does not show (exclusive to a character set, or reserved in `charset.BasicSignatures`); "basic" = pages a basic grimoire shows; '
               '"short" = %d - basic; "characters" = `canon: pages visible in his grimoire (short)`. "New pages" = pages to add so the basic book and every character\'s pool reach %d (all shared).\n' % (GOAL, GOAL))
    out.append('| Book | Magic | Total | Hidden | Basic | Short | New pages | Characters |\n|---|---|---|---|---|---|---|---|')
    for r in sorted(res, key=lambda r: (-sf[r['book']]['need_basic'], r['book'])):
        v = sf[r['book']]
        ch = ', '.join('%s: %d (%d)' % (c, n, max(0, GOAL - n)) for c, n in sorted(r['chars'].items())) or '-'
        out.append('| `%s` | %s | %d | %d | %d | %d | %d | %s |' % (r['book'], book_magic_name(r['book'], mn), r['total'], r['hidden_basic'], r['basic'], v['need_basic'], v['new_pages'], ch))
    out.append('')
    out.append('### Per character grimoire (sorted by shortfall)\n')
    out.append('| Character | Book | Pages in his grimoire | Short of %d | Pool he could use (not exclusive to another) |\n|---|---|---|---|---|' % GOAL)
    rows = [(c, r['book'], n, v) for r in res for c, n in r['chars'].items() for v in [sf[r['book']]['chars'][c]]]
    for c, b, n, v in sorted(rows, key=lambda x: (-x[3]['short'], x[0])):
        out.append('| %s | `%s` | %d | %d | %d |' % (c, b, n, v['short'], v['pool']))
    return '\n'.join(out) + '\n'


# The phase 2 batches: 8 builders, no two share a book (so no two touch the same page list). Balanced on missing spells; the themes keep the wiki research of a batch together.
BATCHES = [
    ('Batch 1: Flame, Lightning, Star and Thread', 'fire magma explosion demon_fire star lightning storm sword reinforcement thread'),
    ('Batch 2: Water, Ice, Fluids and Art', 'water mist ice snow demon_ice demon_water bubble ice_wedge gel black_oil mucus food mud eye eyeball creation painting'),
    ('Batch 3: Wind, Air, Sound, Mind and Soul', 'wind air smoke vortex wing sound song memory permeation dream illusion soul soul_corpse'),
    ('Batch 4: Earth, Stone and Metal', 'earth sand sandstone rock red_ochre stone mineral crystal glass corundum iron copper bronze steel mercury shakudo'),
    ('Batch 5: Light, Dark and Curse', 'light demon_light dark shadow curse curse_warding anti_magic forbidden ash blood bone'),
    ('Batch 6: Space, Time, Game and Mirror', 'time spatial gravity compass key mirror copy dice game legion slash kotodama'),
    ('Batch 7: Nature and Beast', 'tree vine plant poison_plant poison fungus cherry_blossom briar world_tree cotton beast demon_beast body'),
    ('Batch 8: Seal, Transform and Body', 'sealing chain butoh barrier recombination transmutation modification hair nail skin tongue scale spike switching'),
]


def batch_books(spec):
    return ['forbidden_magic' if x == 'forbidden' else 'book_' + x for x in spec.split()]


def render_plan(res, sets):
    sf = shortfalls(res, sets)
    by = {r['book']: r for r in res}
    gap = wiki_gap(res, wiki_spells())
    mn = magic_names()
    seen = []
    out = []
    out.append('| Batch | Books | Missing spells (basic = %d) | Character set lists to extend |\n|---|---|---|---|' % GOAL)
    for title, spec in BATCHES:
        bs = batch_books(spec)
        seen += bs
        out.append('| %s | %d | %d | %d |' % (title, len(bs), sum(sf[b]['need_basic'] for b in bs), sum(c['short'] for b in bs for c in sf[b]['chars'].values())))
    assert sorted(seen) == sorted(by), (set(by) - set(seen), set(seen) - set(by))
    out.append('')
    for title, spec in BATCHES:
        bs = batch_books(spec)
        out.append('#### %s\n' % title)
        out.append('| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |\n|---|---|---|---|---|---|---|')
        for b in sorted(bs, key=lambda b: -sf[b]['need_basic']):
            r = by[b]
            sh, pr = assets_for(b)
            g = gap.get(b)
            ch = ', '.join('%s %d (+%d to list)' % (c, n, max(0, GOAL - n)) for c, n in sorted(r['chars'].items())) or '-'
            out.append('| `%s` | %s | %d / %d | %d | %s | %s | %s |' % (b, book_magic_name(b, mn), r['total'], r['basic'], sf[b]['new_pages'], ch,
                                                                    ('; '.join(g[1]) or 'none (all covered)') if g else 'no repo copy of the wiki page', (' '.join(sh + pr)) or '-'))
        out.append('')
    return '\n'.join(out) + '\n'


def update_doc(res, sets):
    doc = os.path.join(ROOT, 'docs', 'magic', 'page_counts.md')
    text = open(doc, encoding='utf-8').read()
    for name, body in (('tables', render_tables(res, sets)), ('plan', render_plan(res, sets))):
        a, b = '<!-- GENERATED:%s (python3 -B tools/page_counts.py --md-update) -->' % name, '<!-- /GENERATED:%s -->' % name
        i, j = text.index(a) + len(a), text.index(b)
        text = text[:i] + '\n' + body + text[j:]
    open(doc, 'w', encoding='utf-8').write(text)
    print('updated', doc)


def main():
    a = sys.argv[1:]
    res, sets = analyse()
    if '--md' in a:
        sys.stdout.write(render_tables(res, sets)); return
    if '--md-update' in a:
        update_doc(res, sets); return
    if '--plan' in a:
        sf = shortfalls(res, sets)
        gap = wiki_gap(res, wiki_spells())
        for r in res:
            sh, pr = assets_for(r['book'])
            g = gap.get(r['book'])
            print('%-22s total %2d basic %2d short %2d | wiki unmatched: %s | shapes: %s | props: %s' % (
                r['book'], r['total'], r['basic'], sf[r['book']]['need_basic'], ('; '.join(g[1]) or '-') if g else 'no local wiki copy', ' '.join(sh) or '-', ' '.join(pr) or '-'))
        return
    if '--golden' in a:
        sys.stdout.write(golden(res)); return
    if '--json' in a:
        json.dump({'goal': GOAL, 'books': res, 'shortfalls': shortfalls(res, sets), 'totals': totals(res, sets)}, sys.stdout, indent=1); return
    if '--ids' in a:
        want = a[a.index('--ids') + 1]
        for r in res:
            if r['book'] == want:
                print(r['book'], r['total'], 'own', r['own'], 'char', r['char_pages'], 'rest', r['rest'], 'basic_added', r['basic_added'])
                for k, p in enumerate(r['pages']):
                    print(' %2d %s  (%s)' % (k, p, NAMES.get(p, '?')))
        return
    if '--wiki' in a:
        for b, (listed, miss) in sorted(wiki_gap(res, wiki_spells()).items()):
            print('%-22s wiki lists %2d, not yet a page: %s' % (b, len(listed), '; '.join(miss) or '-'))
        return
    if '--totals' in a:
        print(json.dumps(totals(res, sets), indent=1)); return
    bad = [(r['book'], c, m) for r in res for c, m in r['set_missing'].items()]
    table(res, sets)
    print(json.dumps(totals(res, sets)))
    for w in sorted(set(WARN)):
        print('WARN', w)
    for b, c, m in bad:
        print('SET PAGE NOT IN BOOK', b, c, m)
    if '--check' in a and (bad or WARN):
        sys.exit(1)


if __name__ == '__main__':
    main()


