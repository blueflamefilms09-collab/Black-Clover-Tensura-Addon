#!/usr/bin/env python3
"""
A stand-in for the dedicated server, for testing tools/ci/server_smoke.py (the RCON protocol, the log reader, the poll loops, the parsers) on a machine that
cannot start Minecraft:  python3 tools/ci/server_smoke.py --no-deps --quiet --cmd "python3 tools/ci/mock_server.py" --sectors 3 --restart

It prints a boot log, serves RCON on port 25575 with password "smoke", answers the commands the smoke script sends in the formats the real
`/multiverse mega` commands print (docs/labyrinth/mega.md), keeps its sectors in a file next to itself so a restart finds them, and exits on `stop`.
It is not a model of the game: it only proves that the script reads what the real server will print.
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


def load():
    global sectors
    if os.path.exists(STATE):
        sectors = json.load(open(STATE))


def save():
    os.makedirs(os.path.dirname(STATE), exist_ok=True)
    json.dump(sectors, open(STATE, "w"))


def line(s):
    print(s, flush=True)


def status():
    out = ["mega: expansionLevel=%d sectors=%d job=idle" % (max(0, len(sectors) - 1), len(sectors))]
    for s in sectors:
        out.append("sector %d %s theme=%s threat=%d rooms=%d edges=%d pieces=%d relics=%d region=%d,%d,%d,%d" % (
            s["id"], s["state"], s["theme"], s["id"] + 1, 120, 150, 300, 2, *s["region"]))
    return "\n".join(out)


def handle(cmd):
    c = cmd.strip()
    if c.startswith("execute in nusmp:mega_dungeon run forceload add"):
        return "Marked chunk 0, 0 to be force loaded"
    if c.startswith("execute in nusmp:mega_dungeon run setblock"):
        return "Changed the block at 0, 70, 0"
    if c.startswith("execute in nusmp:mega_dungeon run forceload remove"):
        return "All chunks are no longer being force loaded"
    if c.startswith("locate structure nusmp:dungeon_gate"):
        return "The nearest nusmp:dungeon_gate is at [120, ~, -40] (126 blocks away)"
    if c.startswith("locate structure nusmp:mega_gateway"):
        return "The nearest nusmp:mega_gateway is at [310, ~, 55] (314 blocks away)"
    if "place jigsaw" in c:
        return "Generated jigsaw at 300, 64, 0"
    if c == "multiverse mega status":
        for s in sectors:
            if s["state"] == "GENERATING":
                polls[s["id"]] = polls.get(s["id"], 0) + 1
                if polls[s["id"]] >= 2:
                    s["state"] = "ACTIVE"
                    save()
        return status()
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
