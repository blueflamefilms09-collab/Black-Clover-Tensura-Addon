package com.newuniverse.nusmp.relics.vu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: every act of an upgraded relic gets a signature effect that is visibly its own (a golden gate for the Gate of Babylon, a sealing cube for the Prison Realm, a drowned tide for
 * the orrery ...), and which effect is data, not a line of Java per act: data/nusmp/visual_upgrade/act_vfx.txt, written by tools/visual_upgrade/charms_genesis_worn. Pure (no Minecraft
 * types), so the file is checked in tests. The effects themselves are the mod's own VFX shapes; only shapes that take a point and a size are allowed ({@link #SHAPES}), none of the
 * zone or Time Magic ones.
 * <pre>
 * act &lt;relic id&gt; &lt;act key | *&gt; &lt;step&gt; [&lt;step&gt; ...]       step = SHAPE:RRGGBB:power:duration:where       where = self | front | ground | sky
 * </pre>
 * A key of {@code *} is the effect of every act of the relic that has no line of its own. At most {@link #MAX_STEPS} steps per act.
 */
public final class VuActTable {
    public static final int MAX_STEPS = 3;
    /** The VFX shapes the table may name: the ones that stand at a point (or run from a point toward another) and read only the colour, power and duration. */
    public static final Set<String> SHAPES = Set.of("MAGIC_CIRCLE", "MAGIC_CIRCLE_EXPLOSION", "SPATIAL_RIFT", "SPIRIT_AURA", "WEAPON_CONSTRUCTS", "LIGHT_FLARE", "LIGHT_BLADE",
            "SPACE_PORTAL", "SEAL_CHAINS", "SEAL_TRINITY", "ALCHEMY_CIRCLE", "DIVINE_PILLAR", "DIVINE_HALO", "DIVINE_SIGIL", "DIVINE_RAY", "SOUL_VORTEX", "STAR_MAGIC", "FIRE_PILLAR",
            "FIRE_BURST", "FIRE_WILD", "WIND_RING", "LIGHTNING_GOD", "LIGHTNING_FIEND", "SHADOW_POOL", "SHADOW_UNITE", "KOTO_SWORDS", "KOTO_AURA", "DEMON_METEOR", "SPACE_RINGGATE",
            "SPACE_CRACK", "SPACE_TEAR", "BLOOD_SIGIL", "GRAVITY_WELL", "MERCURY_DOME", "COMPASS_ARRAY", "GAME_BOARD", "DREAM_MANIFEST", "MIRROR_FRAME", "MIRROR_ARRAY", "SONG_RING",
            "TREE_ROOTS", "TREE_CANOPY", "WATER_RING", "WATER_NEST", "WATER_CRADLE", "EARTH_CLAWS", "SLASH_SCYTHE", "DICE_D20", "THREAD_WEB", "SPACE_CUBE", "MANA_CHARGE", "DEMON_CIRCLE");
    public static final Set<String> WHERE = Set.of("self", "front", "ground", "sky");

    /** One effect of an act. */
    public record Step(String shape, int rgb, float power, int duration, String where) {}

    private final Map<String, List<Step>> acts = new HashMap<>();
    private final List<String> problems = new ArrayList<>();

    public static VuActTable parse(Iterable<String> lines) {
        VuActTable t = new VuActTable();
        int ln = 0;
        for (String raw : lines) {
            ln++;
            String line = raw;
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] v = line.split("\\s+");
            if (v.length < 4 || !v[0].equals("act")) {
                t.problems.add("line " + ln + ": expected 'act <relic id> <act key|*> <step>...'");
                continue;
            }
            if (v.length - 3 > MAX_STEPS) {
                t.problems.add("line " + ln + ": more than " + MAX_STEPS + " steps");
                continue;
            }
            List<Step> steps = new ArrayList<>();
            boolean ok = true;
            for (int i = 3; i < v.length; i++) {
                String[] f = v[i].split(":");
                try {
                    if (f.length != 5) throw new IllegalArgumentException("a step is SHAPE:RRGGBB:power:duration:where");
                    String shape = f[0].toUpperCase(Locale.ROOT);
                    if (!SHAPES.contains(shape)) throw new IllegalArgumentException("shape " + shape + " is not allowed");
                    if (!WHERE.contains(f[4])) throw new IllegalArgumentException("where " + f[4]);
                    steps.add(new Step(shape, Integer.parseInt(f[1], 16) & 0xFFFFFF, Math.max(0.2f, Math.min(8f, Float.parseFloat(f[2]))), Math.max(4, Math.min(160, Integer.parseInt(f[3]))), f[4]));
                } catch (RuntimeException bad) {
                    t.problems.add("line " + ln + ": " + bad.getMessage());
                    ok = false;
                    break;
                }
            }
            if (ok) t.acts.put(v[1] + "/" + v[2], List.copyOf(steps));
        }
        return t;
    }

    /** The steps of an act: its own line, else the relic's {@code *} line, else none. */
    public List<Step> steps(String relicId, String actKey) {
        List<Step> s = acts.get(relicId + "/" + actKey);
        if (s == null) s = acts.get(relicId + "/*");
        return s == null ? List.of() : s;
    }

    /** Does the relic have any effect line? */
    public boolean has(String relicId) {
        for (String k : acts.keySet()) if (k.startsWith(relicId + "/")) return true;
        return false;
    }

    public int lineCount() { return acts.size(); }

    public Iterable<String> keys() { return acts.keySet(); }

    public List<String> problems() { return problems; }
}
