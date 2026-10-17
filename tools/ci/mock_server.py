#!/usr/bin/env python3
"""
A stand-in for the dedicated server, for testing tools/ci/server_smoke.py (the RCON protocol, the log reader, the poll loops, the parsers) on a machine that
cannot start Minecraft:  python3 tools/ci/server_smoke.py --no-deps --quiet --cmd "python3 tools/ci/mock_server.py" --sectors 3 --restart

It prints a boot log, serves RCON on port 25575 with password "smoke", answers the commands the smoke script sends in the formats the real
`/multiverse mega` commands print (docs/labyrinth/mega.md), keeps its sectors in a file next to itself so a restart finds them, and exits on `stop`.
It is not a model of the game: it only proves that the script reads what the real server will print.

1.58.0 (dungeon parties): it also speaks the party commands in the formats `PartyLines` and `CommandLines` print (`party list`, `status party <n>`, `expand holy|abyssal party <n>`,
`sector <id>` with its stairs line), keeping the floors of a party on that party's lane (Holy z = -k * 4096, Abyssal z = 300000 + k * 4096), so the script's party phase can be run
against it. Set MOCK_PARTY_FAULT to one of: wrong_lane (a floor of party 1 stands on party 0's lane), leak (party 1's floors show up in the plain `status`), wrong_chain (the stairs of
party 1's first floor lead to a floor of party 0), no_party_list (`party list` is unknown): the party phase must then FAIL (or skip, for the last).
"""
import json
import os
import socket
import struct
import sys
import threading
import time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RUN = os.path.join(ROOT, "run", "server")
STATE = os.path.join(ROOT, "run", "smoke", "mock_state.json")
sectors = []
polls = {}
stop = threading.Event()
FAULT = os.environ.get("MOCK_PARTY_FAULT", "")
STEP = 4096
LANE_Z = 300000
PITCH = 198


def load():
    global sectors
    if os.path.exists(STATE):
        sectors = json.load(open(STATE))


def save():
    os.makedirs(os.path.dirname(STATE), exist_ok=True)
    json.dump(sectors, open(STATE, "w"))


def line(s):
    print(s, flush=True)


def lane(road, party):
    return LANE_Z + party * STEP if road == "abyssal" else -party * STEP


def sector_line(s):
    base = "sector %d %s theme=%s threat=%d rooms=%d edges=%d pieces=%d relics=%d region=%d,%d,%d,%d" % (
        s["id"], s["state"], s["theme"], s["id"] + 1, 120, 150, 300, 2, *s["region"])
    if "road" not in s:
        return base
    sign = 1 if s["road"] == "holy" else -1
    fl = "%+d" % (sign * (s["chain"] + 1))
    base += " road=%s chain=%d floors=%s..%s" % (s["road"], s["chain"], fl, fl)
    return base + (" party=%d" % s["party"] if s.get("party", 0) else "")


def of_party(party):
    return [s for s in sectors if s.get("party", 0) == party]


def status(party=0):
    shown = of_party(party)
    if FAULT == "leak" and party == 0:
        shown = list(sectors)
    out = ["mega: expansionLevel=%d sectors=%d job=idle" % (max(0, len(sectors) - 1), len(sectors))]
    known = sorted({s.get("party", 0) for s in sectors})
    if party != 0 or len(known) > 1:
        out.append("party: showing party %d (parties with floors: %s; status party <n> shows another)" % (party, ",".join(str(k) for k in known) or "none"))
    floors = [s for s in shown if "road" in s]
    if floors:
        for road in ("holy", "abyssal"):
            chain = sorted([s for s in floors if s["road"] == road], key=lambda s: s["chain"])
            if not chain:
                out.append("road %s: not opened" % road)
                continue
            sign = 1 if road == "holy" else -1
            out.append("road %s: sectors=%d ids=%s floors=%+d..%+d state=%s" % (road, len(chain), ",".join(str(s["id"]) for s in chain), sign, sign * len(chain), chain[-1]["state"]))
            out.extend(sector_line(s) for s in chain)
    else:
        out.extend(sector_line(s) for s in shown)
    return "\n".join(out)


def stairs_line(s):
    chain = sorted([x for x in sectors if x.get("party", 0) == s.get("party", 0) and x["road"] == s["road"]], key=lambda x: x["chain"])
    on = next((x for x in chain if x["chain"] == s["chain"] + 1), None)
    back = next((x for x in chain if x["chain"] == s["chain"] - 1), None)
    if FAULT == "wrong_chain" and s.get("party", 0) == 1 and s["chain"] == 0:
        on = next((x for x in sectors if x.get("party", 0) == 0), on)
    z = (s["region"][1] + s["region"][3]) // 2
    inner = lambda dx: "%d,64,%d" % (s["region"][0] + dx, z)
    return "  stairs back=%s forward=%s forwardPad=%s leadsOn=%s leadsBack=%s boss=%s climbs=%s" % (
        inner(8), inner(160), inner(150), on["id"] if on else "none", "hub" if s["chain"] == 0 else (back["id"] if back else "none"),
        "true" if (s["chain"] + 1) % 10 == 0 else "false", "up" if s["road"] == "holy" else "down")


def party_list():
    known = sorted({s.get("party", 0) for s in sectors})
    out = ["party: mode=party instanced=true maxParties=64 known=%d sectors=%d" % (len(known), len(sectors))]
    for k in known:
        mine = of_party(k)
        out.append("party %d unowned key=none name=- holy=%d abyssal=%d online=0" % (k, len([s for s in mine if s.get("road") == "holy"]), len([s for s in mine if s.get("road") == "abyssal"])))
    return "\n".join(out)


def expand_party(road, party):
    chain = sorted([s for s in sectors if s.get("party", 0) == party and s.get("road") == road], key=lambda s: s["chain"])
    k = len(chain)
    z = lane(road, 0 if FAULT == "wrong_lane" and party == 1 else party)
    sid = (max(s["id"] for s in sectors) + 1) if sectors else 0
    sectors.append({"id": sid, "state": "GENERATING", "theme": "radiant" if road == "holy" else "abyss", "road": road, "chain": k, "party": party,
                    "region": [k * PITCH, z - 85, k * PITCH + 170, z + 85]})
    save()
    sign = 1 if road == "holy" else -1
    fl = "%+d" % (sign * (k + 1))
    return "expand: queued sector %d on the %s road (floors %s..%s)%s" % (sid, road, fl, fl, " party=%d" % party if party else "")


def handle(cmd):
    c = cmd.strip()
    if c.startswith("execute in nusmp:mega_dungeon run forceload add"):
        return "Marked chunk 0, 0 to be force loaded"
    if c.startswith("execute in nusmp:mega_dungeon run setblock"):
        return "Changed the block at 0, 70, 0"
    if c.startswith("execute in nusmp:mega_dungeon run forceload remove"):
        return "All chunks are no longer being force loaded"
    if c.startswith("locate structure nusmp:mega_gateway"):
        return "The nearest nusmp:mega_gateway is at [310, ~, 55] (314 blocks away)"
    if "place jigsaw" in c:
        return "Generated jigsaw at 300, 64, 0"
    if c == "multiverse mega status" or c.startswith("multiverse mega status party "):
        party = int(c.split()[-1]) if c.startswith("multiverse mega status party ") else 0
        for s in of_party(party):
            if s["state"] == "GENERATING":
                polls[s["id"]] = polls.get(s["id"], 0) + 1
                if polls[s["id"]] >= 2:
                    s["state"] = "ACTIVE"
                    save()
        return status(party)
    if c == "multiverse mega party list":
        return "Unknown or incomplete command, see below for error" if FAULT == "no_party_list" else party_list()
    if c.startswith("multiverse mega expand holy party ") or c.startswith("multiverse mega expand abyssal party "):
        parts = c.split()
        return expand_party(parts[3], int(parts[-1]))
    if c.startswith("multiverse mega sector "):
        sid = int(c.split()[-1])
        for s in sectors:
            if s["id"] == sid:
                return sector_line(s) + ("\n" + stairs_line(s) if "road" in s else "")
        return "sector: refused (no sector %d)" % sid
    if c == "multiverse mega expand":
        if not sectors:
            sectors.append({"id": 0, "state": "GENERATING", "theme": "gravity", "region": [1, -85, 171, 85]})
            save()
            return "expand: queued sector 0"
        return "expand: refused (nothing to expand)"
    if c.startswith("multiverse mega audit "):
        return "audit %s OK" % c.split()[-1]
    if c.startswith("multiverse mega rooms "):
        return "HALL 40\nCORRIDOR 30\nSAFE 3"
    if c.startswith("multiverse mega plan "):
        sid = int(c.split()[-1])
        path = os.path.join(RUN, "mega_plan_%d.json" % sid)
        json.dump({"region": [0, 0, 1, 1], "pieces": [{"template": "nusmp:mega/hall/x", "rotation": 1, "x": 3, "y": 56, "z": 4}]}, open(path, "w"))
        return "plan: " + path
    if c.startswith("multiverse mega unseal "):
        k = int(c.split()[-1])
        last = sectors[-1]["region"]
        sectors.append({"id": k + 1, "state": "GENERATING", "theme": "blood", "region": [last[2] + 12, last[1], last[2] + 12 + 170, last[3]]})
        save()
        return "unseal: sector %d unsealed; next sector queued" % k
    if c.startswith("multiverse mega regen "):
        for s in sectors:
            if s["id"] == int(c.split()[-1]):
                s["state"] = "GENERATING"
                polls[s["id"]] = 0
        save()
        return "regen: queued"
    if c == "reload":
        return "Reloaded!"
    if c == "multiverse mega selftest":
        return "selftest safe.rest OK healed 12.0 -> 20.0\nselftest guard.chain OK sector 0 unsealed\nselftest summary ok=2 fail=0"
    if c.startswith("save-all"):
        return "Saved the game"
    if c.startswith("multiverse"):
        return "Unknown or incomplete command, see below for error"
    return "Unknown or incomplete command, see below for error"


def rcon_loop():
    srv = socket.socket()
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("127.0.0.1", 25575))
    srv.listen(4)
    srv.settimeout(1.0)
    line("[%s] [Server thread/INFO]: RCON running on 0.0.0.0:25575" % time.strftime("%H:%M:%S"))
    while not stop.is_set():
        try:
            conn, _ = srv.accept()
        except socket.timeout:
            continue
        threading.Thread(target=serve, args=(conn,), daemon=True).start()
    srv.close()


def recv(conn, n):
    buf = b""
    while len(buf) < n:
        chunk = conn.recv(n - len(buf))
        if not chunk:
            raise ConnectionError
        buf += chunk
    return buf


def send(conn, rid, kind, text):
    body = text.encode()
    conn.sendall(struct.pack("<iii", len(body) + 10, rid, kind) + body + b"\x00\x00")


def serve(conn):
    try:
        while True:
            (length,) = struct.unpack("<i", recv(conn, 4))
            data = recv(conn, length)
            rid, kind = struct.unpack("<ii", data[:8])
            text = data[8:-2].decode()
            if kind == 3:
                send(conn, rid if text == "smoke" else -1, 2, "")
            elif kind == 2:
                if text.strip() == "stop":
                    send(conn, rid, 0, "Stopping the server")
                    stop.set()
                    return
                send(conn, rid, 0, handle(text))
    except Exception:
        pass
    finally:
        conn.close()


def main():
    os.makedirs(RUN, exist_ok=True)
    load()
    line("[00:00:01] [main/INFO]: Starting minecraft server version 1.21.1")
    line("[00:00:02] [Server thread/INFO]: Loading Datapack registries (mock)")
    t = threading.Thread(target=rcon_loop, daemon=True)
    t.start()
    time.sleep(1.0)
    line("[00:00:03] [Server thread/INFO]: Done (1.234s)! For help, type \"help\"")
    stop.wait()
    line("[00:00:09] [Server thread/INFO]: Stopping server")
    time.sleep(0.5)


if __name__ == "__main__":
    main()
