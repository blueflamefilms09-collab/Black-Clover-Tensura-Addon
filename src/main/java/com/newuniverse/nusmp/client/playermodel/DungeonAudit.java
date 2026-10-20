package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.guard.AbyssalLooks;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Why: phase 2 moves more than three hundred people onto the player model, and nothing of it can be seen by its author, so what can be proved without a person is proved here, with no game class
 * (the same audit runs in the unit tests, on the class path of the jar, and in the CI server's self-test {@code render.playermodel2}):
 * <ul>
 *   <li>the tables merge without a duplicate id and every look of {@code looks_dungeon.json} passes the kit's own checks ({@link PlayerModelAudit}: a 64x64 skin, a glow map where it says so, a haircut
 *       that exists, extras with their x_ roots and textures);</li>
 *   <li>at least forty distinct skins, each with a non-empty second layer where the look says it has one (an archetype's plates and cloth), and the glow map the size of the skin;</li>
 *   <li>every mob of the Foundry's asset index is EITHER a person with the look {@code foundry_<id>} OR listed as a creature in {@code dungeon_legacy.json} (never both, never neither), so a mob
 *       is never invisible and never drawn twice, and a stance named by a look is one the kit knows;</li>
 *   <li>the Living Armor, the Floor Warden, the Goliath's two forms and the twelve skins of the three puppets exist;</li>
 *   <li>the loader of the abyssal bosses: every humanoid abyssal look has its three phases in the table, and {@link DungeonLooks#guardian} maps each to a look that exists;</li>
 *   <li>the flag's rule: off gives every entity back to its old renderer, on draws exactly those with a look.</li>
 * </ul>
 */
public final class DungeonAudit {
    private DungeonAudit() {}

    public static final String LEGACY = "playermodel/dungeon_legacy.json", INDEX = "foundry/index.json";

    public static List<String> run(Function<String, InputStream> open) {
        List<String> bad = new ArrayList<>();
        PlayerLookTables.Merged merged = PlayerLookTables.load(open);
        bad.addAll(merged.problems());
        Map<String, PlayerLook> looks = merged.looks();
        int archetypes = 0;
        Set<Integer> skinHashes = new HashSet<>();
        for (PlayerLook l : looks.values()) {
            if (!l.id().startsWith(DungeonLooks.ARCHETYPE_PREFIX)) continue;
            archetypes++;
            String path = SkinPath.resolve(l.skin());
            try (InputStream in = open.apply(path)) {
                if (in == null) continue;                                                // PlayerModelAudit reports the missing file
                java.awt.image.BufferedImage img = ImageIO.read(in);
                if (img == null || img.getWidth() != 64 || img.getHeight() != 64) continue;
                int[] px = img.getRGB(0, 0, 64, 64, null, 0, 64);
                skinHashes.add(java.util.Arrays.hashCode(px));
                if (!secondLayerUsed(img)) bad.add("archetype " + l.id() + ": its skin has an empty second layer (hat, jacket, sleeves, pants)");
            } catch (Exception e) {
                bad.add(path + ": " + e);
            }
        }
        if (archetypes < 40) bad.add("only " + archetypes + " archetype looks (dg_*), at least 40 are promised");
        if (skinHashes.size() != archetypes) bad.add("the archetype skins are not all distinct (" + skinHashes.size() + " of " + archetypes + ")");
        for (String id : List.of(DungeonLooks.LIVING_ARMOR, DungeonLooks.FLOOR_WARDEN, DungeonLooks.GOLIATH, DungeonLooks.GOLIATH_FORM2))
            if (!looks.containsKey(id)) bad.add("the look '" + id + "' is missing");
        for (String puppet : List.of("corpse_jimmy", "corpse_alfredo", "corpse_carl"))
            for (int s = 0; s <= DungeonLooks.MAX_STAGE; s++) if (!looks.containsKey(DungeonLooks.corpse(puppet, s))) bad.add("the look '" + DungeonLooks.corpse(puppet, s) + "' is missing");
        for (PlayerLook l : looks.values()) if (!Stances.known(l.stance())) bad.add("look " + l.id() + ": the stance '" + l.stance() + "' is not one the kit knows");
        foundry(open, looks, bad);
        abyssal(looks, bad);
        if (DungeonLooks.usesKit(false, true)) bad.add("the flag off must give the old renderer");
        if (!DungeonLooks.usesKit(true, true) || DungeonLooks.usesKit(true, false)) bad.add("the flag on must draw exactly the entities that have a look");
        for (String entity : DungeonLooks.ENTITIES) {
            String look = DungeonLooks.lookFor(entity, entity.equals("foundry_mob") ? "x" : "", 0);
            if (look.isEmpty() && !entity.equals("labyrinth_guardian")) bad.add("the entity " + entity + " has no look");
        }
        return bad;
    }

    /** Does the skin paint anything on the second layer (the texels of hat, jacket, sleeves and pants)? */
    static boolean secondLayerUsed(java.awt.image.BufferedImage img) {
        int[][] rects = {{32, 0, 32, 16}, {16, 32, 24, 16}, {40, 32, 16, 16}, {0, 32, 16, 16}, {0, 48, 16, 16}, {48, 48, 16, 16}};   // hat, jacket, right sleeve, right pants, left pants, left sleeve
        for (int[] r : rects)
            for (int y = r[1]; y < r[1] + r[3]; y++)
                for (int x = r[0]; x < r[0] + r[2]; x++) if (((img.getRGB(x, y) >>> 24) & 0xFF) != 0) return true;
        return false;
    }

    private static void foundry(Function<String, InputStream> open, Map<String, PlayerLook> looks, List<String> bad) {
        Set<String> index = new HashSet<>(), creatures = new HashSet<>();
        try (InputStream in = open.apply(INDEX)) {
            if (in == null) {
                bad.add(INDEX + " is missing");
                return;
            }
            JsonObject mobs = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("mobs");
            index.addAll(mobs.keySet());
        } catch (Exception e) {
            bad.add(INDEX + ": " + e);
            return;
        }
        try (InputStream in = open.apply(LEGACY)) {
            if (in == null) {
                bad.add(LEGACY + " is missing");
                return;
            }
            JsonObject c = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("creatures");
            for (Map.Entry<String, JsonElement> e : c.entrySet()) creatures.add(e.getKey());
        } catch (Exception e) {
            bad.add(LEGACY + ": " + e);
            return;
        }
        int converted = 0;
        for (String id : index) {
            boolean person = looks.containsKey(DungeonLooks.foundry(id)), creature = creatures.contains(id);
            if (person && creature) bad.add("the foundry mob " + id + " is both a person (it has a look) and a creature (it is listed in " + LEGACY + ")");
            else if (!person && !creature) bad.add("the foundry mob " + id + " is neither a person (no look foundry_" + id + ") nor a listed creature: it would be drawn by nothing sure");
            if (person) converted++;
        }
        for (String id : creatures) if (!index.contains(id)) bad.add(LEGACY + " lists " + id + " that is not a foundry mob");
        for (String id : looks.keySet()) {
            if (id.startsWith(DungeonLooks.FOUNDRY_PREFIX) && !index.contains(id.substring(DungeonLooks.FOUNDRY_PREFIX.length()))) bad.add("the look " + id + " names no foundry mob");
        }
        if (converted < 250) bad.add("only " + converted + " foundry mobs are on the player model");
        for (PlayerLook l : looks.values()) {
            if (l.id().startsWith(DungeonLooks.FOUNDRY_PREFIX) && (l.scale() < 0.3f || l.scale() > 5f)) bad.add("look " + l.id() + ": a scale of " + l.scale());
        }
    }

    private static void abyssal(Map<String, PlayerLook> looks, List<String> bad) {
        int humanoids = 0;
        for (AbyssalLooks.Look al : AbyssalLooks.all().values()) {
            for (int phase = 1; phase <= 3; phase++) {
                String id = DungeonLooks.guardian(al.id(), phase);
                if (al.humanoid()) {
                    if (id.isEmpty() || !looks.containsKey(id)) bad.add("the abyssal look " + al.id() + " phase " + phase + " maps to '" + id + "', which is not in the table");
                } else if (!id.isEmpty()) bad.add("the creature look " + al.id() + " must keep its own model but maps to '" + id + "'");
            }
            if (al.humanoid()) humanoids++;
        }
        if (humanoids < 9) bad.add("only " + humanoids + " humanoid abyssal looks");
        if (!DungeonLooks.guardian("", 1).equals(DungeonLooks.FLOOR_WARDEN)) bad.add("a Guardian with no look must be the Floor Warden's body");
        if (!DungeonLooks.guardian("no_such_look", 2).equals(DungeonLooks.FLOOR_WARDEN)) bad.add("a Guardian that names an unknown look keeps the old look");
    }
}
