package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.List;
import java.util.Map;

/**
 * Why: nothing here can run in the game, and a sector plan that is "correct" by the audit can still be an ugly map, so the plan is written as a small
 * JSON file that two other tools read without the game: {@code tools/labyrinth/mega_preview.py --plan} draws it as a tabletop-dungeon map (rooms as
 * coloured boxes by kind, doorways as gaps, loops as rings), and the lead's compositor stitches the REAL template files together from the {@code pieces}
 * list into a top-down picture of the whole sector. For that second use the list is exact: every piece, door caps and the tunnel included, with the
 * template id, the clockwise quarter turns, the world cell where template cell (0,0,0) goes (the position the game's piece is built with) and the parent
 * piece, so a piece's world cell for a template cell c is {@code origin + rotate(rotation, c)} with the vanilla transform ({@code RotationTest} pins it).
 * Written by hand (no JSON library) so it works in the pure test class path; strings are escaped.
 */
public final class PlanJson {
    private PlanJson() {}

    /**
     * The plan as JSON text. {@code entry} is the sector's entry floor cell and {@code forward} its growth direction; {@code audit} the failures the audit found
     * (empty = passed); {@code tries} and {@code fallback} say how the plan was reached; {@code label} is free text for the file header (for example the sector id).
     */
    public static String write(Result r, Vec entry, Facing forward, List<String> audit, int tries, boolean fallback, String label) {
        StringBuilder b = new StringBuilder(64 * 1024);
        b.append("{\n");
        b.append(" \"label\": ").append(str(label)).append(",\n");
        b.append(" \"seed\": ").append(r.seed).append(",\n");
        b.append(" \"tries\": ").append(tries).append(",\n");
        b.append(" \"fallback\": ").append(fallback).append(",\n");
        Box g = r.region;
        b.append(" \"region\": [").append(g.minX()).append(", ").append(g.minZ()).append(", ").append(g.maxX()).append(", ").append(g.maxZ()).append("],\n");
        b.append(" \"entry\": [").append(entry.x()).append(", ").append(entry.y()).append(", ").append(entry.z()).append(", ").append(str(forward.lower())).append("],\n");

        b.append(" \"audit\": {\"ok\": ").append(audit.isEmpty()).append(", \"failures\": [");
        for (int i = 0; i < audit.size(); i++) b.append(i > 0 ? ", " : "").append(str(audit.get(i)));
        b.append("]},\n");

        int[] ids = r.roomIds();
        b.append(" \"rooms\": [");
        boolean first = true;
        for (Placed p : r.pieces) {
            if (ids[p.index] < 0) continue;
            b.append(first ? "\n  " : ",\n  ");
            first = false;
            b.append("{\"id\": ").append(ids[p.index]).append(", \"kind\": ").append(str(p.shape.kind.name())).append(", \"template\": ").append(str(p.shape.id))
                    .append(", \"link\": ").append(p.link).append(", \"box\": [").append(p.box.minX()).append(", ").append(p.box.minZ()).append(", ").append(p.box.maxX())
                    .append(", ").append(p.box.maxZ()).append("]}");
        }
        b.append("\n ],\n");

        b.append(" \"doors\": [");
        first = true;
        for (Placed p : r.pieces) {
            if (p.parent < 0 || ids[p.index] < 0 || ids[p.parent] < 0) continue;
            Placed par = r.pieces.get(p.parent);
            first = door(b, first, par.socketCell(p.parentSocket), par.socketFacing(p.parentSocket));
        }
        for (Result.Loop l : r.loops) first = door(b, first, l.cell(), l.facing());
        b.append("],\n");

        b.append(" \"loops\": [");
        for (int i = 0; i < r.loops.size(); i++) {
            Vec c = r.loops.get(i).cell();
            b.append(i > 0 ? ", " : "").append('[').append(c.x()).append(", ").append(c.z()).append(']');
        }
        b.append("],\n");

        b.append(" \"pieces\": [");
        first = true;
        for (Placed p : r.pieces) {
            b.append(first ? "\n  " : ",\n  ");
            first = false;
            b.append("{\"template\": ").append(str(p.shape.id)).append(", \"rotation\": ").append(p.rot).append(", \"x\": ").append(p.origin.x()).append(", \"y\": ").append(p.origin.y())
                    .append(", \"z\": ").append(p.origin.z()).append(", \"depth\": ").append(p.depth).append(", \"parent\": ").append(p.parent)
                    .append(", \"kind\": ").append(str(p.shape.kind.name())).append(", \"link\": ").append(p.link).append(", \"cap\": ").append(p.shape.kind == RoomKind.CAP)
                    .append(", \"box\": [").append(p.box.minX()).append(", ").append(p.box.minY()).append(", ").append(p.box.minZ()).append(", ").append(p.box.maxX()).append(", ")
                    .append(p.box.maxY()).append(", ").append(p.box.maxZ()).append("]}");
        }
        b.append("\n ],\n");

        Result.Expansion e = r.expansion;
        b.append(" \"expansion\": ");
        if (e == null) b.append("null,\n");
        else b.append("{\"piece\": ").append(e.piece()).append(", \"seal\": [").append(e.seal().x()).append(", ").append(e.seal().y()).append(", ").append(e.seal().z()).append("], \"facing\": ")
                .append(str(e.facing().lower())).append(", \"size\": [").append(e.w()).append(", ").append(e.h()).append(", ").append(e.d()).append("], \"deadEnd\": ").append(e.deadEnd()).append("},\n");

        b.append(" \"stats\": {");
        int i = 0;
        for (Map.Entry<String, Integer> s : r.stats.entrySet()) b.append(i++ > 0 ? ", " : "").append(str(s.getKey())).append(": ").append(s.getValue());
        b.append("},\n");
        b.append(" \"warnings\": [");
        for (int k = 0; k < r.warnings.size(); k++) b.append(k > 0 ? ", " : "").append(str(r.warnings.get(k)));
        b.append("]\n}\n");
        return b.toString();
    }

    /** One doorway of the map: the top-left cell of its 3-wide opening (the preview draws it as a 3x1 or 1x3 gap). */
    private static boolean door(StringBuilder b, boolean first, Vec cell, Facing f) {
        int x = f.alongX() ? cell.x() : cell.x() - 1;
        int z = f.alongX() ? cell.z() - 1 : cell.z();
        if (!first) b.append(", ");
        b.append('[').append(x).append(", ").append(z).append(", ").append(str(f.lower())).append(']');
        return false;
    }

    /** A JSON string literal. */
    static String str(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }
}
