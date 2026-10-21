package com.newuniverse.nusmp.visual.shields;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.110.0: checks a shield item's model jsons against {@link ShieldPreset} (a pure check over parsed json, no Minecraft classes): used by ShieldModelsTest over the
 * shipped files and by the in-game self-test {@code visual.shields.models} over the files inside the running jar.
 */
public final class ShieldModelCheck {
    private ShieldModelCheck() {}

    /** What is wrong with the model of a shield ({@code lowered}) and its raised model ({@code raised}); an empty list means both are right. */
    public static List<String> problems(String id, JsonObject lowered, JsonObject raised) {
        List<String> bad = new ArrayList<>();
        if (lowered == null) return List.of(id + ": no model json");
        if (raised == null) bad.add(id + ": no " + id + "_blocking model json");
        if (!"minecraft:builtin/entity".equals(str(lowered, "parent"))) bad.add(id + ": parent must be minecraft:builtin/entity (the 3D mesh is drawn by the item renderer)");
        checkDisplay(id, "", lowered, false, bad);
        if (raised != null) {
            if (!"minecraft:builtin/entity".equals(str(raised, "parent"))) bad.add(id + "_blocking: parent must be minecraft:builtin/entity");
            checkDisplay(id, "_blocking", raised, true, bad);
        }
        boolean override = false;
        JsonElement ov = lowered.get("overrides");
        if (ov != null && ov.isJsonArray()) {
            for (JsonElement e : ov.getAsJsonArray()) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                JsonObject pred = o.has("predicate") && o.get("predicate").isJsonObject() ? o.getAsJsonObject("predicate") : null;
                if (pred != null && pred.has("blocking") && pred.get("blocking").getAsDouble() == 1.0 && ("nusmp:item/" + id + "_blocking").equals(str(o, "model"))) override = true;
            }
        }
        if (!override) bad.add(id + ": no {\"predicate\": {\"blocking\": 1}} override to nusmp:item/" + id + "_blocking");
        return bad;
    }

    private static void checkDisplay(String id, String suffix, JsonObject model, boolean blocking, List<String> bad) {
        JsonObject display = model.has("display") && model.get("display").isJsonObject() ? model.getAsJsonObject("display") : null;
        if (display == null) {
            bad.add(id + suffix + ": no display block");
            return;
        }
        for (String ctx : ShieldPreset.CONTEXTS) {
            double[] got = row(display.get(ctx));
            if (got == null) {
                bad.add(id + suffix + ": display." + ctx + " missing or malformed");
                continue;
            }
            if (!ShieldPreset.within(ShieldPreset.of(ctx, blocking), got))
                bad.add(id + suffix + ": display." + ctx + " is not vanilla's shield" + (blocking ? "_blocking" : "") + " value");
        }
    }

    /** {rotation xyz, translation xyz, scale xyz} of one display entry, or null. */
    public static double[] row(JsonElement e) {
        if (e == null || !e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        double[] out = new double[9];
        String[] keys = {"rotation", "translation", "scale"};
        for (int k = 0; k < 3; k++) {
            JsonElement a = o.get(keys[k]);
            if (a == null || !a.isJsonArray()) return null;
            JsonArray arr = a.getAsJsonArray();
            if (arr.size() != 3) return null;
            for (int i = 0; i < 3; i++) out[k * 3 + i] = arr.get(i).getAsDouble();
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }
}
