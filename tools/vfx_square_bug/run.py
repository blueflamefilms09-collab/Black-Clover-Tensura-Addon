#!/usr/bin/env python3
"""
Headless check of the VFX vertex buffer (docs/vfx_square_bug.md).

Compiles the buffer of the 1.93 base commit (git show 40e74fa) and the current one against fake RenderSystem / BufferBuilder classes (tools/vfx_square_bug/fakes), runs the same scenario
(Harness.java) through both and compares what a draw would have seen: per draw the texture, the blend factors, and every vertex bit for bit with its RGBA bytes. The state a draw sees
(colour modulator, cull, depth, blend on/off) is compared separately: there the new buffer is allowed to differ, because it sets all of it per batch.

Usage:  python3 -B tools/vfx_square_bug/run.py [--joml path/to/joml.jar] [--base 40e74fa] [--keep]
Exit code 0 = the vertex data and the texture / blend of every draw are identical, and the colour sweep keeps every alpha byte.
"""
import argparse, glob, os, re, shutil, subprocess, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
SRC = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'newuniverse', 'nusmp', 'vfx', 'client')
BUF = 'src/main/java/com/newuniverse/nusmp/vfx/client/VfxVertexBuffer.java'
SHARED = ['VfxBlend.java', 'VfxPose.java', 'VfxRenderContext.java']


def find_joml(arg):
    cands = [arg, os.environ.get('JOML_JAR')]
    cands += glob.glob(os.path.expanduser('~/.gradle/caches/**/joml-*.jar'), recursive=True)
    cands += glob.glob('/tmp/claude-0/**/joml-1.10.5.jar', recursive=True)
    for c in cands:
        if c and os.path.isfile(c):
            return c
    sys.exit('no JOML jar found: pass --joml')


def build(tag, buffer_source, work, joml):
    out = os.path.join(work, 'out_' + tag)
    src = os.path.join(work, 'src_' + tag)
    os.makedirs(os.path.join(src, 'com/newuniverse/nusmp/vfx/client'), exist_ok=True)
    for f in SHARED:
        shutil.copy(os.path.join(SRC, f), os.path.join(src, 'com/newuniverse/nusmp/vfx/client', f))
    with open(os.path.join(src, 'com/newuniverse/nusmp/vfx/client/VfxVertexBuffer.java'), 'w') as fh:
        fh.write(buffer_source)
    files = [p for p in glob.glob(os.path.join(HERE, 'fakes', '**', '*.java'), recursive=True)]
    files += glob.glob(os.path.join(src, '**', '*.java'), recursive=True)
    files.append(os.path.join(HERE, 'Harness.java'))
    os.makedirs(out, exist_ok=True)
    r = subprocess.run(['javac', '-nowarn', '-d', out, '-cp', joml] + files, capture_output=True, text=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=''))
    if r.returncode != 0:
        sys.exit('compile failed (%s):\n%s' % (tag, r.stdout + r.stderr))
    trace = os.path.join(work, 'trace_' + tag + '.txt')
    r = subprocess.run(['java', '-cp', out + os.pathsep + joml, 'Harness', trace], capture_output=True, text=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=''))
    if r.returncode != 0:
        sys.exit('run failed (%s):\n%s' % (tag, r.stdout + r.stderr))
    with open(trace) as fh:
        return fh.read().splitlines()


def draws(lines):
    """[(frame, texture, blend func, [vertex lines], state dict)] for every draw."""
    out, frame, i = [], -1, 0
    while i < len(lines):
        ln = lines[i]
        if ln.startswith('=== frame'):
            frame = int(ln.split()[-1])
        if ln == 'DRAW':
            st = lines[i + 1]
            m = dict(re.findall(r'(\w+)=(\[[^\]]*\]|\S+)', st))
            j = i + 2
            verts = []
            while not lines[j].startswith('END'):
                verts.append(lines[j])
                j += 1
            out.append((frame, m['tex'], m['func'], verts, m))
            i = j
        i += 1
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--joml')
    ap.add_argument('--base', default='40e74fa')
    ap.add_argument('--keep', action='store_true')
    a = ap.parse_args()
    joml = find_joml(a.joml)
    work = tempfile.mkdtemp(prefix='vfx_square_bug_')
    try:
        old_src = subprocess.run(['git', 'show', '%s:%s' % (a.base, BUF)], cwd=ROOT, capture_output=True, text=True, check=True).stdout
        with open(os.path.join(ROOT, BUF)) as fh:
            new_src = fh.read()
        old = draws(build('old', old_src, work, joml))
        new = draws(build('new', new_src, work, joml))
        print('draws: old %d, new %d; vertices: old %d, new %d' % (len(old), len(new), sum(len(d[3]) for d in old), sum(len(d[3]) for d in new)))
        bad = 0
        if len(old) != len(new):
            print('FAIL: a different number of draws')
            bad += 1
        for k, (o, n) in enumerate(zip(old, new)):
            if o[:4] != n[:4]:
                bad += 1
                if bad < 6:
                    why = 'texture' if o[1] != n[1] else 'blend' if o[2] != n[2] else 'vertices'
                    print('FAIL: draw %d (frame %d) differs in %s: old tex=%s func=%s n=%d, new tex=%s func=%s n=%d' % (k, o[0], why, o[1], o[2], len(o[3]), n[1], n[2], len(n[3])))
        # the state a draw saw: informational (the new buffer sets the colour modulator, cull, depth and blend itself, per batch)
        keys = ['blend', 'cull', 'depthTest', 'depthMask', 'shader', 'shaderColor']
        differing = {}
        for o, n in zip(old, new):
            for key in keys:
                if o[4][key] != n[4][key]:
                    differing.setdefault((o[0], key), set()).add((o[4][key], n[4][key]))
        print('state seen by draws that differs between old and new (informational):')
        if not differing:
            print('  none')
        for (frame, key), vals in sorted(differing.items()):
            print('  frame %d %-11s old/new %s' % (frame, key, sorted(vals)))
        # colour sweep: the RGBA bytes that went in must be the ones that come out (frame 4)
        sweep = [d for d in new if d[0] == 4]
        alphas = set()
        for d in sweep:
            for v in d[3]:
                alphas.add(int(v.split()[-1]))
        print('colour sweep (new): %d draws, %d distinct alpha bytes seen' % (len(sweep), len(alphas)))
        if len(alphas) != 256:
            print('FAIL: the sweep did not keep all 256 alpha values')
            bad += 1
        print('RESULT:', 'identical vertices, textures and blends; every alpha byte kept' if bad == 0 else '%d problem(s)' % bad)
        return 0 if bad == 0 else 1
    finally:
        if a.keep:
            print('kept', work)
        else:
            shutil.rmtree(work, ignore_errors=True)


if __name__ == '__main__':
    sys.exit(main())
