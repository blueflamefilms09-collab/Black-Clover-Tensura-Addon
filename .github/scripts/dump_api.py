"""Dumps the public + protected API (javap) of the dependency classes named by .github/api-request.txt into .github/api/api.tar.gz.

Each non-comment line of the request is a regex matched against class names in path form without ".class"
(for example ^net/minecraft/world/entity/[A-Za-z0-9_$]+$). All jars under ~/.gradle and the workspace build folder are scanned;
a class found in several jars is taken from the first. Output: one text file per top-level package group, packed into one tar.gz,
so a session that cannot download the jars can grep exact signatures instead of guessing them.
"""
import io
import os
import re
import subprocess
import sys
import tarfile
import zipfile
from collections import defaultdict

patterns = []
with open(".github/api-request.txt", encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if line and not line.startswith("#"):
            patterns.append(re.compile(line))

roots = [os.path.expanduser("~/.gradle"), "build", os.path.expanduser("~/.m2")]
jars = []
for root in roots:
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if fn.endswith(".jar") and not fn.endswith("-sources.jar") and not fn.endswith("-javadoc.jar"):
                jars.append(os.path.join(dp, fn))
# 1.55.0: a class found in several jars is taken from the FIRST, and the Gradle cache holds several Minecraft jars (NeoGradle keeps the outputs of its
# intermediate steps), some with half-renamed copies of a few classes (MinecraftServer extends an obfuscated name there). The right one is the jar that
# holds BOTH Minecraft's MinecraftServer and NeoForge's own NeoForge class (the fully mapped, patched game), so that jar goes first, then any jar that
# only holds MinecraftServer, then the rest. The job log lists the candidates so a wrong pick is visible.
def _has(path, entry):
    try:
        with zipfile.ZipFile(path) as z:
            z.getinfo(entry)
            return True
    except Exception:
        return False


def _rank(path):
    mc = _has(path, "net/minecraft/server/MinecraftServer.class")
    nf = _has(path, "net/neoforged/neoforge/common/NeoForge.class")
    if mc and nf:
        return 0
    if mc:
        return 1
    if re.match(r"neoforge-21[^/]*\.jar$", os.path.basename(path)):
        return 2
    return 3


ranks = {p: _rank(p) for p in jars}
jars.sort(key=lambda p: (ranks[p], p))
for p in jars:
    if ranks[p] <= 1:
        print("minecraft jar candidate (rank %d): %s" % (ranks[p], p))
print("jars:", len(jars))

seen = set()
per_jar = defaultdict(list)
for j in jars:
    try:
        names = zipfile.ZipFile(j).namelist()
    except Exception:
        continue
    for n in names:
        if not n.endswith(".class") or n.endswith("module-info.class") or n.endswith("package-info.class"):
            continue
        c = n[:-6]
        if c in seen:
            continue
        if any(p.search(c) for p in patterns):
            seen.add(c)
            per_jar[j].append(c.replace("/", "."))
print("classes:", len(seen), "in", len(per_jar), "jars")

groups = defaultdict(io.StringIO)
for j, classes in per_jar.items():
    classes.sort()
    for i in range(0, len(classes), 120):
        batch = classes[i:i + 120]
        out = subprocess.run(["javap", "-protected", "-constants", "-cp", j] + batch, capture_output=True, text=True)
        text = out.stdout
        # split per class so each lands in its group file
        for chunk in re.split(r"(?m)^(?=Compiled from )", text):
            m = re.search(r"(?m)^(?:public |protected |abstract |final |sealed |static |non-sealed )*(?:class|interface|enum|record|@interface) ([\w.$]+)", chunk)
            if not m:
                continue
            parts = m.group(1).split(".")
            g = "_".join(parts[:3] if parts[0] != "net" or parts[1] != "minecraft" else parts[:4])
            groups[g].write(chunk.rstrip() + "\n\n")
os.makedirs(".github/api", exist_ok=True)
total = 0
with tarfile.open(".github/api/api.tar.gz", "w:gz") as tar:
    for g, buf in sorted(groups.items()):
        data = buf.getvalue().encode("utf-8")
        total += len(data)
        ti = tarfile.TarInfo(g + ".txt")
        ti.size = len(data)
        tar.addfile(ti, io.BytesIO(data))
print("files:", len(groups), "text bytes:", total, "tar.gz bytes:", os.path.getsize(".github/api/api.tar.gz"))
