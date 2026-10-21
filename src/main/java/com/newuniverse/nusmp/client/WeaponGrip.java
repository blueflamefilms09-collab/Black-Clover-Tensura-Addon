package com.newuniverse.nusmp.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.visual.grip.GripFinder;
import com.newuniverse.nusmp.visual.grip.GripMath;
import com.newuniverse.nusmp.visual.grip.GripState;
import com.newuniverse.nusmp.visual.grip.GripTable;
import com.newuniverse.nusmp.client.relics.item.WeaponClasses;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemDisplayContext;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 1.111.0 (docs/weapon_grip.md), client glue: finds the grip point of a loaded weapon mesh and hands {@link WeaponRenderer} / the upgraded-weapon renderer the shift that puts
 * it into the hand. The decisions are pure ({@code client/relics/item/GripFinder, GripMath, GripState, GripTable}, covered by JUnit); this class only reads the resources
 * (assets/nusmp/weapon_grips.json and the {@code display} block of the item model json) and maps the display context to an index.
 * <p>
 * Kill switch: {@code -Dnusmp.weaponGrip=off} on the client JVM restores the 1.110.0 placement exactly (the mesh on the old sprite's diagonal, the oversize rule about
 * its quarter point). A mesh whose grip cannot be found (no body, a broken json) is placed the old way as well.
 */
public final class WeaponGrip {
    private WeaponGrip() {}

    private static GripTable table;
    private static final Map<String, GripMath.Display[]> DISPLAYS = new HashMap<>();

    /** Is the grip placement on? (default yes; -Dnusmp.weaponGrip=off restores the old placement). */
    public static boolean enabled() {
        return !"off".equalsIgnoreCase(System.getProperty("nusmp.weaponGrip", "on"));
    }

    public static void clear() {
        table = null;
        demonSlayer = null;
        DISPLAYS.clear();
    }

    /** The context index of {@link GripState}, -1 for a context that has no grip (slot, ground, frame, head). */
    public static int index(ItemDisplayContext ctx) {
        return switch (ctx) {
            case THIRD_PERSON_RIGHT_HAND -> GripState.TP_RIGHT;
            case THIRD_PERSON_LEFT_HAND -> GripState.TP_LEFT;
            case FIRST_PERSON_RIGHT_HAND -> GripState.FP_RIGHT;
            case FIRST_PERSON_LEFT_HAND -> GripState.FP_LEFT;
            default -> -1;
        };
    }

    static GripTable table() {
        if (table != null) return table;
        GripTable t = GripTable.EMPTY;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DemonSlayerRenderer.rl("weapon_grips.json"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) sb.append(buf, 0, n);
                    t = GripTable.parse(sb.toString());
                }
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] weapon_grips.json failed to load; class grips only", ex);
        }
        table = t;
        return t;
    }

    /**
     * The grip state of a mesh given its body vertices and facts, or null (placed the old way). {@code classes} is the class table; {@code gripLine} the mesh's {@code grip}
     * line (NaN = none), {@code flipped} whether the runtime normaliser turned the mesh over, {@code explicit} its {@code gripxyz} or null.
     */
    static GripState resolve(String id, List<float[]> body, WeaponClasses classes, float gripLine, boolean flipped, float[] explicit) {
        if (!enabled()) return null;
        try {
            GripTable t = table();
            String cls = t.gripClass(id, classes);
            WeaponClasses.Cls row = classes.named(cls);
            if (row == null) row = classes.classOf(id);
            GripFinder.Grip g = GripState.find(body, cls, row.grip(), t.fraction(id), gripLine, flipped, flipped ? null : explicit);      // a stated point is in the mesh as the file has it, so not for a mesh the runtime turned over
            if (g == null) return null;
            return new GripState(g, displays(id), GripState.box(body));
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] grip of weapon mesh {} could not be found; it is placed the old way", id, ex);
            return null;
        }
    }

    /**
     * The grip of an upgraded mesh (weapon_meshes_vu): its static and animated body quads at rest. {@code fraction} NaN = find it, else (the lod mesh) reuse the full mesh's
     * fraction so both land on the same spot. null = placed the old way.
     */
    public static GripState forVu(String meshId, com.newuniverse.nusmp.visual.genesis.VuMesh mesh, float fraction) {
        if (!enabled() || mesh == null) return null;
        List<float[]> body = GripState.vuBody(mesh, true);                                   // the static body ("-"): the orbiting parts and rings would blur the profile
        if (body.isEmpty()) body = GripState.vuBody(mesh, false);
        if (body.isEmpty()) return null;
        try {
            GripTable t = table();
            WeaponClasses classes = WeaponRenderer.classes();
            String base = WeaponClasses.baseId(meshId);
            String cls = t.gripClass(base, classes);
            WeaponClasses.Cls row = classes.named(cls);
            if (row == null) row = classes.classOf(base);
            float ov = Float.isNaN(fraction) ? t.fraction(base) : fraction;
            GripFinder.Grip g = GripState.find(body, cls, row.grip(), ov, Float.NaN, false, t.point(base));
            return g == null ? null : new GripState(g, displays(base), GripState.box(body));
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] grip of upgraded mesh {} could not be found; it is placed the old way", meshId, ex);
            return null;
        }
    }

    private static GripState demonSlayer;

    /**
     * The grip of the pixel-built Genesis Demon-Slayer ({@link DemonSlayerMesh}, used when the upgraded model is off): its grip box is y -22 to -2 of the mesh (hilt() there),
     * the middle of it is the hand. Placement {0.516, 0.523, 45, 0.98/64, 11.75} as DemonSlayerMesh.draw.
     */
    static GripState demonSlayer() {
        if (!enabled()) return null;
        if (demonSlayer == null) demonSlayer = new GripState(new GripFinder.Grip(0f, -12f, 0f, 0.37f, GripFinder.Source.GRIPXYZ), displays("demon_slayer_sword"));
        return demonSlayer;
    }

    /** The four hand entries {third right, third left, first right, first left} of an item's model json (a state mesh reads its base id's), NONE where an entry is missing. */
    static GripMath.Display[] displays(String id) {
        GripMath.Display[] cached = DISPLAYS.get(id);
        if (cached != null) return cached;
        GripMath.Display[] out = {GripMath.Display.NONE, GripMath.Display.NONE, GripMath.Display.NONE, GripMath.Display.NONE};
        for (String cand : new String[]{id, WeaponClasses.baseId(id)}) {
            try {
                Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(DemonSlayerRenderer.rl("models/item/" + cand + ".json"));
                if (res.isEmpty()) continue;
                try (BufferedReader in = res.get().openAsReader()) {
                    JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                    JsonElement disp = o.get("display");
                    if (disp != null && disp.isJsonObject()) {
                        String[] keys = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand"};
                        for (int i = 0; i < 4; i++) {
                            JsonElement e = disp.getAsJsonObject().get(keys[i]);
                            if (e != null && e.isJsonObject()) out[i] = entry(e.getAsJsonObject());
                        }
                    }
                }
                break;
            } catch (Exception ignored) {
                // an unreadable json: no transform (the hand point is the cube's centre)
            }
        }
        DISPLAYS.put(id, out);
        return out;
    }

    /** One display entry as vanilla reads it (ItemTransform.Deserializer): translation clamped to +-80 px, scale to +-4, missing parts 0 / 0 / 1. */
    private static GripMath.Display entry(JsonObject o) {
        float[] r = vec(o.get("rotation"), 0f), t = vec(o.get("translation"), 0f), s = vec(o.get("scale"), 1f);
        for (int i = 0; i < 3; i++) {
            t[i] = Math.max(-80f, Math.min(80f, t[i]));
            s[i] = Math.max(-4f, Math.min(4f, s[i]));
        }
        return new GripMath.Display(r[0], r[1], r[2], t[0], t[1], t[2], s[0]);
    }

    private static float[] vec(JsonElement e, float fill) {
        float[] v = {fill, fill, fill};
        if (e != null && e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            for (int i = 0; i < 3 && i < a.size(); i++) v[i] = a.get(i).getAsFloat();
        }
        return v;
    }
}
