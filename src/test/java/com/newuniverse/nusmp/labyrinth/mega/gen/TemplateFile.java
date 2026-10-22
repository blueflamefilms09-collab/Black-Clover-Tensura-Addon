package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/**
 * Why: the rebuilt rooms (docs/labyrinth/rooms_rebuilt.md) are proved against the shipped ones on the FILES, with no Minecraft: this reads a structure template (gzip NBT) into plain
 * Java: size, every block (cell, id, properties, block entity data), every entity. It is the one reader the tests of the rebuilt rooms and of the armour stands share. Test code only.
 */
final class TemplateFile {
    /** The root of the shipped templates. */
    static final Path ROOT = Path.of("src/main/resources/data/nusmp/structure");

    record Block(int x, int y, int z, String id, Map<String, String> props, Map<String, Object> nbt) {
        String cell() { return x + "," + y + "," + z; }
    }

    final String name;
    final int sx, sy, sz;
    final Map<String, Block> blocks = new TreeMap<>();
    final List<Map<String, Object>> entities = new ArrayList<>();

    private TemplateFile(String name, int sx, int sy, int sz) {
        this.name = name;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
    }

    /** {@code mega/hall/x} (with or without {@code nusmp:}) read from disk, or null when the file does not exist. */
    static TemplateFile load(String id) throws IOException {
        String p = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        Path f = ROOT.resolve(p + ".nbt");
        if (!Files.exists(f)) return null;
        return read(p, Files.readAllBytes(f));
    }

    @SuppressWarnings("unchecked")
    static TemplateFile read(String name, byte[] raw) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(raw)); DataInputStream d = new DataInputStream(in)) {
            d.readByte();
            d.readUTF();
            Map<String, Object> root = compound(d);
            List<Object> size = (List<Object>) root.get("size");
            TemplateFile t = new TemplateFile(name, (Integer) size.get(0), (Integer) size.get(1), (Integer) size.get(2));
            List<Object> palette = (List<Object>) root.get("palette");
            for (Object o : (List<Object>) root.get("blocks")) {
                Map<String, Object> b = (Map<String, Object>) o;
                Map<String, Object> entry = (Map<String, Object>) palette.get((Integer) b.get("state"));
                Map<String, String> props = new TreeMap<>();
                Map<String, Object> pr = (Map<String, Object>) entry.get("Properties");
                if (pr != null) for (Map.Entry<String, Object> e : pr.entrySet()) props.put(e.getKey(), String.valueOf(e.getValue()));
                List<Object> pos = (List<Object>) b.get("pos");
                Block bl = new Block((Integer) pos.get(0), (Integer) pos.get(1), (Integer) pos.get(2), (String) entry.get("Name"), props, (Map<String, Object>) b.get("nbt"));
                t.blocks.put(bl.cell(), bl);
            }
            List<Object> ents = (List<Object>) root.get("entities");
            if (ents != null) for (Object o : ents) t.entities.add((Map<String, Object>) o);
            return t;
        }
    }

    /** The jigsaw blocks: cell to "orientation name target pool joint final_state selection placement". */
    Map<String, String> jigsaws() {
        Map<String, String> out = new TreeMap<>();
        for (Block b : blocks.values()) {
            if (!b.id().equals("minecraft:jigsaw") || b.nbt() == null) continue;
            Map<String, Object> d = b.nbt();
            out.put(b.cell(), b.props().get("orientation") + " " + d.get("name") + " " + d.get("target") + " " + d.get("pool") + " " + d.get("joint") + " " + d.get("final_state")
                    + " " + d.get("selection_priority") + " " + d.get("placement_priority"));
        }
        return out;
    }

    /** The data markers: cell to metadata text. */
    Map<String, String> markers() {
        Map<String, String> out = new TreeMap<>();
        for (Block b : blocks.values()) if (b.id().equals("minecraft:structure_block") && b.nbt() != null) out.put(b.cell(), String.valueOf(b.nbt().get("metadata")));
        return out;
    }

    int stands() {
        int n = 0;
        for (Map<String, Object> e : entities) {
            Object nbt = e.get("nbt");
            if (nbt instanceof Map<?, ?> m && "minecraft:armor_stand".equals(m.get("id"))) n++;
        }
        return n;
    }

    int maxRow() {
        int m = 0;
        for (Block b : blocks.values()) m = Math.max(m, b.y());
        return m;
    }

    /** A stable fingerprint of all the blocks (cell, id, properties): two templates with different insides have different ones. */
    long fingerprint() {
        long h = 1125899906842597L;
        for (Block b : blocks.values()) {
            h = 31 * h + b.cell().hashCode();
            h = 31 * h + b.id().hashCode();
            h = 31 * h + b.props().hashCode();
        }
        return h;
    }

    // ---------------------------------------------------------------- the NBT reader
    private static Object payload(DataInputStream d, int tag) throws IOException {
        switch (tag) {
            case 1: return (int) d.readByte();
            case 2: return (int) d.readShort();
            case 3: return d.readInt();
            case 4: return d.readLong();
            case 5: return d.readFloat();
            case 6: return d.readDouble();
            case 7: {
                int n = d.readInt();
                d.skipBytes(n);
                return null;
            }
            case 8: return d.readUTF();
            case 9: {
                int type = d.readByte();
                int n = d.readInt();
                List<Object> l = new ArrayList<>(n);
                for (int i = 0; i < n; i++) l.add(payload(d, type));
                return l;
            }
            case 10: return compound(d);
            case 11: {
                int n = d.readInt();
                d.skipBytes(n * 4);
                return null;
            }
            case 12: {
                int n = d.readInt();
                d.skipBytes(n * 8);
                return null;
            }
            default: throw new IOException("unknown tag type " + tag);
        }
    }

    private static Map<String, Object> compound(DataInputStream d) throws IOException {
        Map<String, Object> m = new LinkedHashMap<>();
        while (true) {
            int tag = d.readByte();
            if (tag == 0) return m;
            String name = d.readUTF();
            m.put(name, payload(d, tag));
        }
    }
}
