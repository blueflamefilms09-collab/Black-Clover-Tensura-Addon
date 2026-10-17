#!/usr/bin/env python3
"""
Boots the mod in a real dedicated server (NeoForge dev run, headless) and drives it from the console, so the parts of a feature that no unit test can
reach are exercised by the actual game: the datapack registries (dimension, template pools, processor lists, structure sets) parse, every registry of
the mod initialises, the dimensions load, structures place, and, for the Mega-Dungeon, a sector is really generated, audited, expanded and saved.

    python3 tools/ci/server_smoke.py                     # what the CI job runs (builds nothing itself: Gradle does, through runServer)
    python3 tools/ci/server_smoke.py --sectors 3 --restart
    python3 tools/ci/server_smoke.py --cmd "python3 tools/ci/mock_server.py"   # protocol self-test against a stand-in server

Why a script and not a GameTest: a GameTest runs inside the Overworld's test structure and cannot make a custom dimension grow over many ticks; a console
script over RCON can run the real commands (`/multiverse mega expand`, `/place jigsaw`, `/locate`) in the real dimension, read the real log and restart
the server to check what was saved. The script never fails because of other mods' log noise: it fails on (1) a boot that does not finish, (2) an ERROR
or exception in the server log that names this mod, (3) a failed check below. It prints one line per check and GitHub annotations for the summary.
The checks that need a feature that is not in the build yet are SKIPPED (a command or structure that does not exist), never silently passed.

What it checks, in order:
  1. the server reaches "Done" (the whole mod loads, every datapack registry decodes);
  2. no mod-related ERROR or exception in the boot log;
  3. the dimension `nusmp:mega_dungeon` exists and takes blocks;
  4. `/locate structure nusmp:dungeon_gate` (the old arches) and `nusmp:mega_gateway` (the singular Gateway) answer;
  5. the vanilla Jigsaw machinery places our pools and processors (`/place jigsaw nusmp:mega/<pool> nusmp:mega_door 3 x 64 z` in the dimension);
  6. the Mega-Dungeon generator: `/multiverse mega expand` creates sector 0, `status` reaches ACTIVE, `audit 0` is OK; `unseal k` grows sector k+1 up to
     `--sectors`, every audit is OK, regions never closer than the 12-block gap; `regen 0` reproduces the same plan (determinism);
  7. `--restart`: the server is stopped and started again, and `status` still lists the same sectors in the same states (saved data and resume);
  8. the final log has no mod-related ERROR or exception.
"""
import argparse
import json
import os
import re
import shlex
import socket
import struct
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RUN = os.path.join(ROOT, "run", "server")
OUT = os.path.join(ROOT, "run", "smoke")
RCON_PORT = 25575
RCON_PASSWORD = "smoke"
MOD_RE = re.compile(r"(nusmp|multiverse|labyrinth|mega_dungeon|[. ]Mega[A-Z]|megadungeon)", re.I)
NOISE_RE = re.compile(r"(Tensura|tensura|manascore|ManasCore|geckolib|GeckoLib|terrablender|TerraBlender|architectury|smartbrainlib)", re.I)


# ------------------------------------------------------------------------------------------------------ RCON
class Rcon:
    """The Source RCON protocol Minecraft speaks: int32 length, int32 id, int32 type, payload, two NULs. Type 3 logs in, type 2 runs a command."""

    def __init__(self, host, port, password, timeout=60.0):
        self.host, self.port, self.password, self.timeout = host, port, password, timeout
        self.sock = None
        self.next_id = 1

    def connect(self):
        self.sock = socket.create_connection((self.host, self.port), timeout=self.timeout)
        rid = self._send(3, self.password)
        got_id, _, _ = self._read()
        if got_id == -1 or got_id != rid:
            raise ConnectionError("RCON login refused")

    def close(self):
        try:
            if self.sock:
                self.sock.close()
        finally:
            self.sock = None

    def command(self, text):
        rid = self._send(2, text)
        out = []
        while True:
            got_id, _, payload = self._read()
            if got_id == rid:
                out.append(payload)
                if len(payload.encode("utf-8")) < 4096:
                    break
        return "".join(out)

    def _send(self, kind, text):
        rid = self.next_id
        self.next_id += 1
        body = text.encode("utf-8")
        self.sock.sendall(struct.pack("<iii", len(body) + 10, rid, kind) + body + b"\x00\x00")
        return rid

    def _recv_exact(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise ConnectionError("RCON closed")
            buf += chunk
        return buf

    def _read(self):
        (length,) = struct.unpack("<i", self._recv_exact(4))
        data = self._recv_exact(length)
        rid, kind = struct.unpack("<ii", data[:8])
        return rid, kind, data[8:-2].decode("utf-8", "replace")


# ------------------------------------------------------------------------------------------------------ the server process
class Server:
    def __init__(self, cmd, log_path, echo=True):
        self.cmd = cmd
        self.log_path = log_path
        self.echo = echo
        self.lines = []
        self.lock = threading.Lock()
        self.done = threading.Event()
        self.rcon_up = threading.Event()
        self.proc = None
        self.reader = None

    def start(self):
        self.done.clear()
        self.rcon_up.clear()
        self.proc = subprocess.Popen(self.cmd, cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1,
                                     encoding="utf-8", errors="replace")
        self.reader = threading.Thread(target=self._pump, daemon=True)
        self.reader.start()

    def _pump(self):
        mode = "a"
        with open(self.log_path, mode, encoding="utf-8") as f:
            for line in self.proc.stdout:
                f.write(line)
                f.flush()
                with self.lock:
                    self.lines.append(line.rstrip("\n"))
                if self.echo:
                    sys.stdout.write("[server] " + line)
                    sys.stdout.flush()
                if re.search(r"Done \(", line):
                    self.done.set()
                if re.search(r"RCON running on", line):
                    self.rcon_up.set()

    def alive(self):
        return self.proc is not None and self.proc.poll() is None

    def wait_done(self, timeout):
        end = time.time() + timeout
        while time.time() < end:
            if self.done.wait(2.0):
                return True
            if not self.alive():
                return False
        return False

    def snapshot(self):
        with self.lock:
            return list(self.lines)

    def stop(self, rcon=None, timeout=240):
        if rcon is not None:
            try:
                rcon.command("stop")
            except Exception:
                pass
        end = time.time() + timeout
        while self.alive() and time.time() < end:
            time.sleep(1.0)
        if self.alive():
            self.proc.kill()
        try:
            self.proc.wait(timeout=30)
        except Exception:
            pass
        if self.reader is not None:
            self.reader.join(timeout=10)


# ------------------------------------------------------------------------------------------------------ the checks
class Smoke:
    def __init__(self, args):
        self.args = args
        self.results = []
        self.server = None
        self.rcon = None
        self.plan_dir = RUN

    # ---- reporting
    def ok(self, name, detail=""):
        self.results.append(("OK", name, detail))
        print("SMOKE ok    %s %s" % (name, detail))

    def fail(self, name, detail=""):
        self.results.append(("FAIL", name, detail))
        print("SMOKE FAIL  %s %s" % (name, detail))

    def skip(self, name, detail=""):
        self.results.append(("SKIP", name, detail))
        print("SMOKE skip  %s %s" % (name, detail))

    def run(self, command):
        try:
            res = self.rcon.command(command)
        except Exception as e:  # noqa: BLE001
            res = "RCON ERROR: %s" % e
        print("SMOKE > %s\nSMOKE < %s" % (command, res.replace("\n", "\nSMOKE < ")))
        return res

    @staticmethod
    def unknown_command(text):
        return "Unknown or incomplete command" in text or "Unknown command" in text or "Incorrect argument for command" in text

    @staticmethod
    def missing(text):
        """The answer of the game when the thing asked about does not exist in this build (a pool, a structure, a command)."""
        t = text.lower()
        return ("unknown" in t or "there is no template pool" in t or "there is no structure" in t or "does not exist" in t or "no such" in t
                or "incorrect argument for command" in t)

    # ---- preparation
    def prepare(self):
        os.makedirs(RUN, exist_ok=True)
        os.makedirs(OUT, exist_ok=True)
        with open(os.path.join(RUN, "eula.txt"), "w") as f:
            f.write("eula=true\n")
        props = {
            "online-mode": "false", "enable-rcon": "true", "rcon.port": str(RCON_PORT), "rcon.password": RCON_PASSWORD, "broadcast-rcon-to-ops": "false",
            "spawn-protection": "0", "view-distance": "4", "simulation-distance": "4", "max-tick-time": "-1", "spawn-monsters": "false", "spawn-animals": "false",
            "motd": "smoke", "level-name": "world", "level-seed": "20261017", "generate-structures": "true", "sync-chunk-writes": "false",
            "enable-command-block": "true", "allow-flight": "true",
        }
        path = os.path.join(RUN, "server.properties")
        existing = {}
        if os.path.exists(path):
            for ln in open(path):
                if "=" in ln and not ln.startswith("#"):
                    k, v = ln.rstrip("\n").split("=", 1)
                    existing[k] = v
        existing.update(props)
        with open(path, "w") as f:
            for k, v in existing.items():
                f.write("%s=%s\n" % (k, v))
        if not self.args.no_deps:
            self.fetch_terrablender()

    def fetch_terrablender(self):
        """Tensura depends on TerraBlender, which the project does not declare (LIBRARIES.md, "Dev client note"): the dev run needs its jar in run/server/mods."""
        mods = os.path.join(RUN, "mods")
        os.makedirs(mods, exist_ok=True)
        if any(n.lower().startswith("terrablender") for n in os.listdir(mods)):
            print("SMOKE TerraBlender jar already present")
            return
        errors = []
        try:
            q = urllib.parse.urlencode({"loaders": json.dumps(["neoforge"]), "game_versions": json.dumps(["1.21.1"])})
            req = urllib.request.Request("https://api.modrinth.com/v2/project/terrablender/version?" + q, headers={"User-Agent": "nusmp-ci-smoke/1.0"})
            versions = json.load(urllib.request.urlopen(req, timeout=60))
            for v in versions:
                for fobj in v.get("files", []):
                    if fobj.get("primary", True) and fobj["filename"].endswith(".jar"):
                        dest = os.path.join(mods, fobj["filename"])
                        urllib.request.urlretrieve(fobj["url"], dest)
                        print("SMOKE downloaded %s (Modrinth)" % fobj["filename"])
                        return
        except Exception as e:  # noqa: BLE001
            errors.append("modrinth: %s" % e)
        try:
            base = "https://maven.minecraftforge.net/com/github/glitchfiend/TerraBlender-neoforge/"
            html = urllib.request.urlopen(urllib.request.Request(base, headers={"User-Agent": "nusmp-ci-smoke/1.0"}), timeout=60).read().decode("utf-8", "replace")
            vers = sorted({m for m in re.findall(r'href="(1\.21\.1-[0-9.]+)/"', html)}, key=lambda s: [int(x) for x in re.findall(r"\d+", s)])
            if vers:
                v = vers[-1]
                name = "TerraBlender-neoforge-%s.jar" % v
                urllib.request.urlretrieve("%s%s/%s" % (base, v, name), os.path.join(mods, name))
                print("SMOKE downloaded %s (Forge maven)" % name)
                return
        except Exception as e:  # noqa: BLE001
            errors.append("maven: %s" % e)
        raise SystemExit("could not download TerraBlender: " + "; ".join(errors))

    # ---- the phases
    def boot(self, label):
        log = os.path.join(OUT, "server_%s.log" % label)
        if os.path.exists(log):
            os.remove(log)
        self.server = Server(shlex.split(self.args.cmd), log, echo=not self.args.quiet)
        self.server.start()
        if not self.server.wait_done(self.args.boot_timeout):
            self.fail("boot", "the server did not reach 'Done' within %ds (alive=%s)" % (self.args.boot_timeout, self.server.alive()))
            return False
        self.ok("boot", "the server reached Done (all mod registries and datapack registries loaded)")
        end = time.time() + 120
        self.rcon = Rcon("127.0.0.1", RCON_PORT, RCON_PASSWORD)
        last = None
        while time.time() < end:
            try:
                self.rcon.connect()
                return True
            except Exception as e:  # noqa: BLE001
                last = e
                time.sleep(2)
        self.fail("rcon", "could not log in to RCON: %s" % last)
        return False

    def scan_log(self, label):
        bad = []
        for ln in self.server.snapshot():
            if re.search(r"\b(ERROR|FATAL)\b", ln) or re.search(r"Exception|Crash|crash report", ln):
                if MOD_RE.search(ln) and not (NOISE_RE.search(ln) and not re.search(r"nusmp", ln, re.I)):
                    bad.append(ln)
        if bad:
            self.fail("log-" + label, "%d mod-related error lines, first: %s" % (len(bad), bad[0][:300]))
            for ln in bad[:12]:
                print("SMOKE !! " + ln[:400])
        else:
            self.ok("log-" + label, "no mod-related ERROR or exception in the server log")

    def phase_reload(self):
        """/reload runs every datapack reload listener of the mod (themes, factions, compat tables, recipes) against the real registries."""
        r = self.run("reload")
        time.sleep(4)
        (self.ok if "reload" in r.lower() else self.fail)("reload", r.strip()[:160])

    SELFTEST_RE = re.compile(r"^selftest (\S+) (OK|FAIL)[: ]\s*(.*)$", re.M)

    def phase_selftests(self):
        text = self.run("multiverse mega selftest")
        found = 0
        for m in self.SELFTEST_RE.finditer(text):
            key, level, detail = m.groups()
            if key == "summary":
                continue
            found += 1
            (self.ok if level == "OK" else self.fail)("selftest-" + key, detail.strip()[:900])
        if found == 0:
            (self.fail if self.args.strict else self.skip)("selftests", "no self-test reported: " + text.strip()[:160])
        # every system of the build must have registered at least one test (a registry that is not wired reports none and would pass silently)
        keys = [m.group(1) for m in self.SELFTEST_RE.finditer(text) if m.group(1) != "summary"]
        for prefix in [p.strip() for p in (self.args.expect_selftests or "").split(",") if p.strip()]:
            n = sum(1 for k in keys if k.startswith(prefix + "."))
            if n:
                self.ok("selftests-" + prefix, "%d test(s) registered" % n)
            else:
                (self.fail if self.args.strict else self.skip)("selftests-" + prefix, "no self-test with the prefix '%s.' was reported: the registry is not wired" % prefix)

    def phase_dimension(self):
        r = self.run("execute in nusmp:mega_dungeon run forceload add 0 0")
        if re.search(r"[Mm]arked", r):
            self.ok("dimension", "nusmp:mega_dungeon exists and loads")
        elif "Unknown dimension" in r or "dimension" in r.lower() and "unknown" in r.lower():
            self.fail("dimension", "nusmp:mega_dungeon is not a dimension of this server: " + r.strip()[:200])
            return
        else:
            self.fail("dimension", r.strip()[:200])
        r = self.run("execute in nusmp:mega_dungeon run setblock 0 70 0 minecraft:stone")
        (self.ok if "Changed" in r else self.fail)("dimension-setblock", r.strip()[:120])
        self.run("execute in nusmp:mega_dungeon run forceload remove all")

    def phase_locate(self):
        r = self.run("locate structure nusmp:dungeon_gate")
        (self.ok if "nearest" in r.lower() else self.fail)("locate-old-gate", r.strip()[:200])
        r = self.run("locate structure nusmp:mega_gateway")
        if "nearest" in r.lower():
            self.ok("locate-gateway", r.strip()[:200])
        elif self.missing(r) or "not a valid" in r.lower() or "Invalid" in r:
            (self.fail if self.args.strict else self.skip)("locate-gateway", "the structure does not exist in this build: " + r.strip()[:120])
        else:
            self.fail("locate-gateway", r.strip()[:200])

    def phase_jigsaw(self):
        # The test pieces go to z=150000: between the Holy chain (z=0) and the Abyssal chain (z=300000), so they never touch a real sector. The command
        # needs the origin chunk loaded ("That position is not loaded" otherwise), so the area is forceloaded first and released afterwards.
        x, z = 300, 150000
        # The start jigsaw is named by the pool's own entry socket: most rooms enter through nusmp:mega_door, the antechamber through nusmp:mega_onward and a vault through nusmp:mega_vault.
        start = {"antechamber": "nusmp:mega_onward", "vault_1": "nusmp:mega_vault"}
        for pool in ("landing", "side", "safe", "arena", "antechamber", "vault_1", "onward_straight"):
            self.run("execute in nusmp:mega_dungeon run forceload add %d %d %d %d" % (x - 48, z - 48, x + 48, z + 48))
            r = ""
            for _ in range(4):
                time.sleep(2)
                r = self.run("execute in nusmp:mega_dungeon run place jigsaw nusmp:mega/%s %s 3 %d 64 %d" % (pool, start.get(pool, "nusmp:mega_door"), x, z))
                if "not loaded" not in r:
                    break
            self.run("execute in nusmp:mega_dungeon run forceload remove %d %d %d %d" % (x - 48, z - 48, x + 48, z + 48))
            x += 300
            if "Generated jigsaw" in r or "generated jigsaw" in r.lower():
                self.ok("jigsaw-" + pool, "vanilla Jigsaw placed our pool with its processors")
            elif self.missing(r) or "Invalid" in r:
                (self.fail if self.args.strict else self.skip)("jigsaw-" + pool, r.strip()[:120])
            else:
                self.fail("jigsaw-" + pool, r.strip()[:200])

    SECTOR_RE = re.compile(r"sector (\d+) (\w+) theme=(\w+) threat=(\d+) rooms=(\d+) edges=(\d+) pieces=(\d+) relics=(\d+) region=(-?\d+),(-?\d+),(-?\d+),(-?\d+)")

    def status(self):
        text = self.run("multiverse mega status")
        sectors = {}
        for m in self.SECTOR_RE.finditer(text):
            g = m.groups()
            sectors[int(g[0])] = {"state": g[1], "theme": g[2], "threat": int(g[3]), "rooms": int(g[4]), "edges": int(g[5]), "pieces": int(g[6]),
                                  "relics": int(g[7]), "region": tuple(int(v) for v in g[8:12])}
        return text, sectors

    def wait_active(self, sector_id, label):
        end = time.time() + self.args.gen_timeout
        last = {}
        while time.time() < end:
            _, sectors = self.status()
            last = sectors.get(sector_id)
            if last and last["state"] not in ("GENERATING",):
                return last
            time.sleep(5)
        self.fail(label, "sector %d did not leave GENERATING within %ds (last=%s)" % (sector_id, self.args.gen_timeout, last))
        return None

    @staticmethod
    def gap(a, b):
        ax1, az1, ax2, az2 = a
        bx1, bz1, bx2, bz2 = b
        dx = max(bx1 - ax2, ax1 - bx2, 0)
        dz = max(bz1 - az2, az1 - bz2, 0)
        return max(dx, dz)

    def phase_mega(self):
        text, _ = self.status()
        if self.unknown_command(text):
            (self.fail if self.args.strict else self.skip)("mega", "the /multiverse mega command is not in this build")
            return False
        r = self.run("multiverse mega expand")
        if "queued" not in r.lower():
            self.fail("mega-expand", r.strip()[:200])
            return False
        self.ok("mega-expand", r.strip()[:120])
        s0 = self.wait_active(0, "mega-sector-0")
        if not s0:
            return False
        if s0["state"] != "ACTIVE":
            self.fail("mega-sector-0", "state %s" % s0["state"])
            return False
        self.ok("mega-sector-0", "ACTIVE rooms=%d edges=%d pieces=%d relics=%d theme=%s" % (s0["rooms"], s0["edges"], s0["pieces"], s0["relics"], s0["theme"]))
        a = self.run("multiverse mega audit 0")
        (self.ok if "audit 0 OK" in a else self.fail)("mega-audit-0", a.strip()[:300])
        self.run("multiverse mega rooms 0")
        p = self.run("multiverse mega plan 0")
        plan0 = self.read_plan(0)
        for k in range(self.args.sectors - 1):
            u = self.run("multiverse mega unseal %d" % k)
            nxt = k + 1
            sk = self.wait_active(nxt, "mega-sector-%d" % nxt)
            if not sk:
                return False
            (self.ok if sk["state"] == "ACTIVE" else self.fail)("mega-sector-%d" % nxt, "%s rooms=%d pieces=%d theme=%s relics=%d" % (sk["state"], sk["rooms"], sk["pieces"], sk["theme"], sk["relics"]))
            a = self.run("multiverse mega audit %d" % nxt)
            (self.ok if ("audit %d OK" % nxt) in a else self.fail)("mega-audit-%d" % nxt, a.strip()[:300])
        # 1.55.0: the Abyssal road has its own first sector, on its own lane 300000 blocks away; the id is the next global one, read from the reply
        r = self.run("multiverse mega expand abyssal")
        m = re.search(r"queued sector (\d+) on the abyssal road", r)
        if not m:
            (self.fail if self.args.strict else self.skip)("mega-abyssal-expand", r.strip()[:200])
        else:
            aid = int(m.group(1))
            self.ok("mega-abyssal-expand", r.strip()[:120])
            sa = self.wait_active(aid, "mega-abyssal-sector")
            if sa:
                (self.ok if sa["state"] == "ACTIVE" else self.fail)("mega-abyssal-sector", "%s id=%d rooms=%d pieces=%d theme=%s" % (sa["state"], aid, sa["rooms"], sa["pieces"], sa["theme"]))
                a2 = self.run("multiverse mega audit %d" % aid)
                (self.ok if ("audit %d OK" % aid) in a2 else self.fail)("mega-abyssal-audit", a2.strip()[:300])
        _, sectors = self.status()
        ids = sorted(sectors)
        worst = 10 ** 9
        for i in ids:
            for j in ids:
                if i < j and not (j == i + 1):
                    worst = min(worst, self.gap(sectors[i]["region"], sectors[j]["region"]))
        if len(ids) > 2:
            (self.ok if worst >= 12 else self.fail)("mega-region-gap", "closest non-neighbouring regions are %d blocks apart (need 12)" % worst)
        if plan0 is not None:
            self.run("multiverse mega regen 0")
            time.sleep(3)
            s = self.wait_active(0, "mega-regen-0")
            self.run("multiverse mega plan 0")
            again = self.read_plan(0)
            if again is None:
                self.skip("mega-determinism", "no plan file after regen")
            else:
                same = plan0.get("pieces") == again.get("pieces")
                (self.ok if same else self.fail)("mega-determinism", "regen from the saved seed %s the same %d pieces" % ("reproduced" if same else "DID NOT reproduce", len(plan0.get("pieces", []))))
        else:
            self.skip("mega-determinism", "the plan command wrote no readable file")
        self.final_status = self.status()[1]
        return True

    def read_plan(self, sid):
        for d in (RUN, os.path.join(RUN, "world"), ROOT):
            path = os.path.join(d, "mega_plan_%d.json" % sid)
            if os.path.exists(path):
                try:
                    data = json.load(open(path))
                    os.makedirs(OUT, exist_ok=True)
                    with open(os.path.join(OUT, "mega_plan_%d.json" % sid), "w") as f:
                        json.dump(data, f)
                    return data
                except Exception:  # noqa: BLE001
                    return None
        return None

    def phase_restart(self):
        self.run("save-all flush")
        time.sleep(3)
        self.server.stop(self.rcon)
        self.rcon.close()
        if not self.boot("restart"):
            return
        text, sectors = self.status()
        before = getattr(self, "final_status", None)
        if not before:
            self.skip("restart-state", "no sectors before the restart")
        elif {i: s["state"] for i, s in sectors.items()} == {i: s["state"] for i, s in before.items()} and len(sectors) == len(before):
            self.ok("restart-state", "%d sectors, same states after a restart (saved data and resume work)" % len(sectors))
        else:
            self.fail("restart-state", "before=%s after=%s" % ({i: s["state"] for i, s in before.items()}, {i: s["state"] for i, s in sectors.items()}))
        self.scan_log("restart")

    # ---- driver
    def main(self):
        self.prepare()
        if not self.boot("first"):
            self.finish()
            return 1
        self.scan_log("boot")
        try:
            self.phase_dimension()
            self.phase_locate()
            self.phase_jigsaw()
            self.phase_reload()
            ran = self.phase_mega()
            if ran:
                self.phase_selftests()
            self.scan_log("run")
            if self.args.restart and ran:
                self.phase_restart()
        finally:
            try:
                self.server.stop(self.rcon)
            except Exception:  # noqa: BLE001
                pass
        self.finish()
        return 1 if any(r[0] == "FAIL" for r in self.results) else 0

    def dump_tail(self, lines=120):
        """On a failure the job log should show what the server said last (the full logs are uploaded as an artifact, but nobody opens an artifact first)."""
        if self.server is None:
            return
        tail = self.server.snapshot()[-lines:]
        print("\nSMOKE last %d server log lines:" % len(tail))
        for ln in tail:
            print("SMOKE | " + ln[:300])

    def finish(self):
        if any(r[0] == "FAIL" for r in self.results):
            self.dump_tail()
        print("\nSMOKE SUMMARY")
        for level, name, detail in self.results:
            print("  %-5s %-22s %s" % (level, name, detail[:160]))
        fails = [r for r in self.results if r[0] == "FAIL"]
        oks = [r for r in self.results if r[0] == "OK"]
        skips = [r for r in self.results if r[0] == "SKIP"]
        print("::notice title=server smoke::%d ok, %d failed, %d skipped" % (len(oks), len(fails), len(skips)))
        for level, name, detail in (oks + skips)[:8]:
            print("::notice title=smoke %s %s::%s" % (level.lower(), name, detail[:200].replace("\n", " ")))
        for level, name, detail in fails[:10]:
            print("::error title=smoke FAIL %s::%s" % (name, detail[:300].replace("\n", " ")))
        with open(os.path.join(OUT, "summary.json"), "w") as f:
            json.dump([{"level": a, "name": b, "detail": c} for a, b, c in self.results], f, indent=1)


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--cmd", default="./gradlew runServer --no-daemon --console=plain")
    ap.add_argument("--boot-timeout", type=int, default=1800)
    ap.add_argument("--gen-timeout", type=int, default=600)
    ap.add_argument("--sectors", type=int, default=3)
    ap.add_argument("--restart", action="store_true")
    ap.add_argument("--strict", action="store_true", help="a missing pool, structure or command is a FAILURE (the integrated build must have them all)")
    ap.add_argument("--no-deps", action="store_true", help="do not download TerraBlender (self-test with a stand-in server)")
    ap.add_argument("--expect-selftests", default=os.environ.get("SMOKE_EXPECT", ""), help="comma list of self-test key prefixes that must each have at least one test (default: $SMOKE_EXPECT)")
    ap.add_argument("--quiet", action="store_true")
    args = ap.parse_args(argv)
    if os.path.exists(os.path.join(ROOT, "gradlew")):
        try:
            os.chmod(os.path.join(ROOT, "gradlew"), 0o755)
        except OSError:
            pass
    return Smoke(args).main()


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
