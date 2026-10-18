package com.newuniverse.nusmp.heroweapon.weapon.kit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Why: nothing of the four weapons of this builder can be played by its author, so each registers the SELF-TEST {@code hero.weapon.<pkg>} that the CI server
 * smoke test runs in the real game, and the four tests do the same work, so it is written once. The data half reads the weapon's forms file straight from the
 * mod's resources (so it works before and after the forms system is merged) and checks what the weapon's own code promises: the base form first, parents that
 * exist, only the five known slots, every skill id registered for this weapon, at most one ultimate per form. The live half binds the weapon to a stand-in
 * player (a {@code FakePlayer} standing on the surface at the world spawn, looking at a husk four blocks away that cannot move), puts the weapon in its hand, and
 * casts every skill of the weapon once, catching anything thrown (a refusal, a {@code false} return, is fine; an exception is a failure). It also checks that
 * the live skill is the table's row (cost, ultimate flag, a cooldown between the budget floor and the nominal one). It cleans up after itself: the husk is
 * removed, the bond released, the hand emptied, and the weapon's own leftovers of the stand-in dropped through the cleanup hook (which gets the stand-in, so it
 * can never touch a real player's state).
 */
public final class WeaponSelfTest {
    private WeaponSelfTest() {}

    private static final Set<String> SLOTS = Set.of("use", "sneak", "key1", "key2", "key3");

    /** What the weapon wants checked beyond the common casts; return a problem text or null when all is well. */
    @FunctionalInterface
    public interface Extra {
        String check(Fixture f) throws Exception;
    }

    /** The stand-in world of one run. */
    public record Fixture(MinecraftServer server, ServerLevel level, FakePlayer player, HeroCtx ctx, Husk dummy, Vec3 ground) {}

    /** Runs the whole test; never throws (the registry would turn a throw into a failure anyway, with less detail). */
    public static SelfTests.Result run(MinecraftServer server, HeroKind kind, List<Budget.Spec> specs, Extra extra, Consumer<FakePlayer> cleanup) {
        List<String> bad = new ArrayList<>();
        int forms = checkForms(kind, bad);
        int live = checkRegistry(kind, specs, bad);
        if (!bad.isEmpty()) return fail(bad);
        String data = forms + " forms and " + live + " skills of the " + kind.display() + " resolve";
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        if (!level.hasChunkAt(spawn)) return SelfTests.Result.ok(data + "; the live casts were skipped: the spawn chunk is not loaded");
        double y = level.getHeight(Heightmap.Types.WORLD_SURFACE, spawn.getX(), spawn.getZ());
        Vec3 ground = new Vec3(spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        FakePlayer fp = SelfTests.player(level, "hero_" + kind.pkg(), ground.x, ground.y, ground.z);
        Husk dummy = null;
        boolean bound = false;
        try {
            if (HeroBonds.of(fp).isBound()) HeroBonds.release(fp);
            String refusal = HeroBonds.bind(fp, kind);
            if (refusal != null) return SelfTests.Result.ok(data + "; the live casts were skipped: the stand-in could not be bound (" + refusal + ")");
            bound = true;
            HeroBond bond = HeroBonds.of(fp);
            bond.setSummoned(true);
            ItemStack stack = HeroItems.stack(kind);
            stack.set(HeroComponents.OWNER.get(), fp.getUUID().toString());
            fp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            fp.setYRot(-90.0f);                                                    // looking east, along +x
            fp.setXRot(0.0f);
            fp.setYHeadRot(-90.0f);
            dummy = EntityType.HUSK.create(level);
            if (dummy == null) return SelfTests.Result.fail("the stand-in target could not be created");
            dummy.moveTo(ground.x + 4.0, ground.y, ground.z, 0.0f, 0.0f);
            dummy.setNoAi(true);
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
            dummy.setHealth(2000.0f);
            level.addFreshEntity(dummy);
            HeroCtx c = HeroCtx.of(fp);
            if (c == null) return SelfTests.Result.fail("no context could be built for the bound stand-in");
            int cast = 0, refused = 0;
            for (HeroSkill s : HeroLogics.skillsOf(kind)) {
                long faults = VbSkill.faults();
                try {
                    if (dummy.isAlive()) dummy.setHealth(dummy.getMaxHealth());
                    dummy.invulnerableTime = 0;
                    if (s.cast(c)) cast++;
                    else refused++;
                } catch (Throwable t) {
                    bad.add(s.id() + " threw " + t);
                }
                if (VbSkill.faults() != faults) bad.add(VbSkill.lastFault());
                Budget.Spec spec = find(specs, s.id());
                if (spec == null) continue;
                int cd = s.cooldownTicks(c);
                if (cd < Budget.floor(spec.kind()) || cd > spec.cooldown()) bad.add(s.id() + " reports a cooldown of " + cd + " outside " + Budget.floor(spec.kind()) + ".." + spec.cooldown());
            }
            if (extra != null) {
                String problem = extra.check(new Fixture(server, level, fp, c, dummy, ground));
                if (problem != null) bad.add(problem);
            }
            if (!bad.isEmpty()) return fail(bad);
            return SelfTests.Result.ok(data + "; " + cast + " cast and " + refused + " refused beside a dummy without an exception");
        } catch (Throwable t) {
            return SelfTests.Result.fail("the live part threw " + t);
        } finally {
            if (dummy != null) dummy.discard();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (bound) HeroBonds.release(fp);
            try {
                if (cleanup != null) cleanup.accept(fp);
            } catch (RuntimeException ignored) {
                // a failing cleanup must not hide the result
            }
        }
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    private static Budget.Spec find(List<Budget.Spec> specs, String id) {
        for (Budget.Spec s : specs) if (s.id().equals(id)) return s;
        return null;
    }

    // ------------------------------------------------------------------ the registry and the table
    private static int checkRegistry(HeroKind kind, List<Budget.Spec> specs, List<String> bad) {
        String prefix = kind.name().toLowerCase() + ".";
        List<HeroSkill> skills = HeroLogics.skillsOf(kind);
        if (!HeroLogics.has(kind)) bad.add("no logic is registered for the " + kind.display());
        Set<String> seen = new HashSet<>();
        for (HeroSkill s : skills) {
            if (!s.id().startsWith(prefix)) bad.add(s.id() + " does not start with " + prefix);
            if (!seen.add(s.id())) bad.add(s.id() + " is listed twice");
            Budget.Spec spec = find(specs, s.id());
            if (spec == null) {
                bad.add(s.id() + " has no row in the numbers table");
                continue;
            }
            if (Math.abs(s.magoiShare() - spec.cost()) > 1.0e-9) bad.add(s.id() + " costs " + s.magoiShare() + " but the table says " + spec.cost());
            if (s.ultimate() != spec.ultimate()) bad.add(s.id() + " disagrees with the table about being an ultimate");
            if (s.name() == null || s.name().isBlank() || s.desc() == null || s.desc().isBlank()) bad.add(s.id() + " has no name or description");
        }
        for (Budget.Spec spec : specs) if (!seen.contains(spec.id())) bad.add(spec.id() + " is in the table but not in the logic");
        for (Budget.Spec spec : specs) bad.addAll(Budget.violations(spec));
        return skills.size();
    }

    // ------------------------------------------------------------------ the forms file
    private static int checkForms(HeroKind kind, List<String> bad) {
        String path = "/data/nusmp/hero/forms/" + kind.itemId() + ".json";
        try (InputStream in = WeaponSelfTest.class.getResourceAsStream(path)) {
            if (in == null) {
                bad.add(path + " is missing from the mod");
                return 0;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("weapon") || !kind.itemId().equals(root.get("weapon").getAsString())) bad.add(path + " names another weapon");
            JsonArray array = root.getAsJsonArray("forms");
            if (array == null || array.size() == 0) {
                bad.add(path + " has no forms");
                return 0;
            }
            Map<String, JsonObject> byId = new HashMap<>();
            for (JsonElement e : array) {
                JsonObject f = e.getAsJsonObject();
                String id = f.get("id").getAsString();
                if (byId.put(id, f) != null) bad.add("form " + id + " appears twice");
            }
            if (!"base".equals(array.get(0).getAsJsonObject().get("id").getAsString())) bad.add("the first form is not the base form");
            for (JsonObject f : byId.values()) checkForm(kind, f, byId, bad);
            List<HeroForm> live = HeroForms.of(kind);
            if (live.size() > 1) {
                for (HeroForm f : live) for (String id : f.skills().values()) if (HeroLogics.skill(id) == null) bad.add("the loaded form " + f.id() + " lists the unknown skill " + id);
                if (live.size() != byId.size()) bad.add("the loader holds " + live.size() + " forms but the file has " + byId.size());
            }
            return byId.size();
        } catch (Exception e) {
            bad.add(path + " could not be read: " + e);
            return 0;
        }
    }

    private static void checkForm(HeroKind kind, JsonObject f, Map<String, JsonObject> byId, List<String> bad) {
        String id = f.get("id").getAsString();
        if (f.has("parents")) for (JsonElement p : f.getAsJsonArray("parents")) if (!byId.containsKey(p.getAsString())) bad.add("form " + id + " has the unknown parent " + p.getAsString());
        if (f.has("skills")) {
            int ultimates = 0;
            for (Map.Entry<String, JsonElement> e : f.getAsJsonObject("skills").entrySet()) {
                if (!SLOTS.contains(e.getKey())) bad.add("form " + id + " uses the unknown slot " + e.getKey());
                HeroSkill s = HeroLogics.skill(e.getValue().getAsString());
                if (s == null) bad.add("form " + id + " lists the unknown skill " + e.getValue().getAsString());
                else {
                    if (!s.id().startsWith(kind.name().toLowerCase() + ".")) bad.add("form " + id + " lists the skill " + s.id() + " of another weapon");
                    if (s.ultimate()) ultimates++;
                }
            }
            if (ultimates > 1) bad.add("form " + id + " has " + ultimates + " ultimates");
        }
    }
}
