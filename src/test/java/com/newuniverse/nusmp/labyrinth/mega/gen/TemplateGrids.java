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
import java.util.zip.GZIPInputStream;

/**
 * Why: the clearance rule is only worth something if it is proved on the template files that ship, and the shim that runs the unit tests has no Minecraft. This is the
 * smallest NBT reader that can read a structure template (gzip, the tag types of the format) and turn it into a {@link ClearanceGrid}: block ids, which blocks carry a
 * block entity, which blocks are bottom-half stairs. Test code only.
 */
final class TemplateGrids {
    private TemplateGrids() {}

    /** Root of the shipped templates. */
    static final Path ROOT = Path.of("src/main/resources/data/nusmp/structure");

    /** The template id {@code nusmp:mega/hall/x} read from disk, or null when the file does not exist. */
    static ClearanceGrid load(String templateId) throws IOException {
        String p = templateId.contains(":") ? templateId.substring(templateId.indexOf(':') + 1) : templateId;
        Path f = ROOT.resolve(p + ".nbt");
        if (!Files.exists(f)) return null;
        byte[] raw = Files.readAllBytes(f);
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(raw)); DataInputStream d = new DataInputStream(in)) {
            d.readByte();                                   // root tag type (compound)
            d.readUTF();                                    // root name
            Map<String, Object> root = compound(d);
            return grid(root);
        }
    }

    @SuppressWarnings("unchecked")
    private static ClearanceGrid grid(Map<String, Object> root) {
        List<Object> size = (List<Object>) root.get("size");
        int sx = (Integer) size.get(0), sy = (Integer) size.get(1), sz = (Integer) size.get(2);
        ClearanceGrid g = new ClearanceGrid(sx, sy, sz);
        List<Object> palette = (List<Object>) root.get("palette");
        List<Object> blocks = (List<Object>) root.get("blocks");
        for (Object o : blocks) {
            Map<String, Object> b = (Map<String, Object>) o;
            int state = (Integer) b.get("state");
            Map<String, Object> entry = (Map<String, Object>) palette.get(state);
            String name = (String) entry.get("Name");
            Map<String, Object> props = (Map<String, Object>) entry.get("Properties");
            List<Object> pos = (List<Object>) b.get("pos");
            boolean half = name.endsWith("_stairs") && props != null && "bottom".equals(props.get("half"));
            g.set((Integer) pos.get(0), (Integer) pos.get(1), (Integer) pos.get(2), name, b.containsKey("nbt"), half);
        }
        return g;
    }

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
