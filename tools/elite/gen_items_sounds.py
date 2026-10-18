"""elite/items: synthesises the five short sounds of the item subsystem with ffmpeg (so they are reproducible and tiny) and adds their entries to assets/nusmp/sounds.json.

    python3 -B tools/elite/gen_items_sounds.py [--check]

  elite/plush_squeak_a.ogg, plush_squeak_b.ogg   a rubbery squeak, a rising chirp and a falling one (the block picks one at random and pitches it by the plushie's size)
  elite/crate_unlock.ogg                         a lock click and a creaking lid
  elite/crate_reveal.ogg                         a rising shimmer of four bell tones
  elite/crate_locked.ogg                         a short dull thud
The sound events are registered in elite/items/EliteSounds.java under the same names; the sounds.json entries (and their subtitles) are appended at the END of the file, nothing else in it is touched.
ffmpeg with libvorbis is needed; the generated .ogg files are checked in, so nobody needs ffmpeg to build the mod.
"""
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from art_lib import ASSETS, ensure  # noqa: E402

OUT = os.path.join(ASSETS, "sounds", "elite")
SR = 32000

SOUNDS = {
    "plush_squeak_a": "0.55*sin(2*PI*(1500*t+5200*t*t))*sin(PI*t/0.26)^0.6+0.12*sin(2*PI*(3000*t+10400*t*t))*sin(PI*t/0.26)",
    "plush_squeak_b": "0.55*sin(2*PI*(2600*t-4300*t*t))*sin(PI*t/0.3)^0.6+0.10*sin(2*PI*(5200*t-8600*t*t))*sin(PI*t/0.3)",
    "crate_unlock": "0.5*sin(2*PI*1900*t)*exp(-90*t)+0.4*(random(0)-0.5)*exp(-60*t)+if(gt(t,0.12),0.35*(sin(2*PI*(95*t+60*t*t))+0.5*sin(2*PI*2*(95*t+60*t*t)))*(0.6+0.4*sin(2*PI*17*t))*sin(PI*(t-0.12)/0.6)^0.5,0)",
    "crate_reveal": "0.35*(sin(2*PI*1046.5*t)*exp(-3*t)+if(gt(t,0.1),sin(2*PI*1318.5*(t-0.1))*exp(-3*(t-0.1)),0)+if(gt(t,0.2),sin(2*PI*1568*(t-0.2))*exp(-3*(t-0.2)),0)+if(gt(t,0.3),sin(2*PI*2093*(t-0.3))*exp(-2.5*(t-0.3)),0))",
    "crate_locked": "0.7*sin(2*PI*(70*t-80*t*t))*exp(-14*t)+0.25*(random(0)-0.5)*exp(-45*t)",
}
DURATION = {"plush_squeak_a": 0.26, "plush_squeak_b": 0.3, "crate_unlock": 0.8, "crate_reveal": 1.2, "crate_locked": 0.3}

ENTRIES = {
    "plush_squeak": {"subtitle": "subtitles.nusmp.plush_squeak", "sounds": [{"name": "nusmp:elite/plush_squeak_a"}, {"name": "nusmp:elite/plush_squeak_b"}]},
    "crate_unlock": {"subtitle": "subtitles.nusmp.crate_unlock", "sounds": [{"name": "nusmp:elite/crate_unlock"}]},
    "crate_reveal": {"subtitle": "subtitles.nusmp.crate_reveal", "sounds": [{"name": "nusmp:elite/crate_reveal"}]},
    "crate_locked": {"subtitle": "subtitles.nusmp.crate_locked", "sounds": [{"name": "nusmp:elite/crate_locked"}]},
}


def synth(name):
    ensure(OUT)
    path = os.path.join(OUT, name + ".ogg")
    cmd = ["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "aevalsrc='%s':s=%d:d=%s" % (SOUNDS[name], SR, DURATION[name]), "-c:a", "libvorbis", "-q:a", "2", "-ac", "1", path]
    subprocess.run(cmd, check=True)
    return path


def sounds_json():
    """Appends the four entries to sounds.json as text at the end of the object, leaving every older line as it is."""
    path = os.path.join(ASSETS, "sounds.json")
    text = open(path, encoding="utf-8").read()
    have = json.loads(text)
    missing = {k: v for k, v in ENTRIES.items() if k not in have}
    if not missing:
        return False
    body = text.rstrip()
    assert body.endswith("}")
    body = body[:-1].rstrip()
    add = ",\n".join('  "%s": %s' % (k, json.dumps(v)) for k, v in missing.items())
    open(path, "w", newline="\n", encoding="utf-8").write(body + ",\n" + add + "\n}\n")
    json.loads(open(path, encoding="utf-8").read())
    return True


def main(argv):
    if "--check" in argv:
        bad = [n for n in SOUNDS if not os.path.exists(os.path.join(OUT, n + ".ogg"))]
        have = json.load(open(os.path.join(ASSETS, "sounds.json"), encoding="utf-8"))
        bad += [k for k in ENTRIES if k not in have]
        print("missing:", bad)
        sys.exit(1 if bad else 0)
    for n in SOUNDS:
        p = synth(n)
        print("%-16s %5d bytes" % (n, os.path.getsize(p)))
    print("sounds.json:", "entries appended" if sounds_json() else "entries already there")


if __name__ == "__main__":
    main(sys.argv[1:])
