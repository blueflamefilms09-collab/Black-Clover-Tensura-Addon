package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing of the vassal weapons can be played by its author, so each one registers a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}, key {@code hero.weapon.<pkg>}; the smoke script expects the prefix {@code hero}). The five weapons share this one body.
 * It checks that the weapon's forms file is in the jar and passes {@link FormsFile}, that every item and tag the forms ask for exists in the real registries,
 * that the logic registered exactly the skills of its numbers table with budget-sized cooldowns and costs, and that the forms the game loaded agree with the
 * file. Then, with a stand-in player on the surface at the world spawn, bonded to the weapon and holding it, it casts every skill once (and every skill of every
 * loaded form under that form, so the passives run too) beside a dummy monster and calls every hook, each in its own try/catch: a skill may refuse (false),
 * but nothing may throw. Everything it made is removed again and the bond is released. Tests may touch the world (the CI world is a throw-away one).
 */
public final class WeaponSelfTest {
    private WeaponSelfTest() {}

    /** Registers {@code hero.weapon.<pkg>}; called from the weapon logic's constructor. */
    public static void register(HeroKind kind, FormsFile.Expect expect) {
        SelfTests.register("hero.weapon." + kind.pkg(), server -> run(server, kind, expect));
    }

    @FunctionalInterface
    private interface Act {
        void run() throws Exception;
    }

    private static void guard(List<String> bad, String what, Act act) {
        try {
            act.run();
        } catch (Throwable t) {
            bad.add(what + " threw " + t);
        }
    }

    private static SelfTests.Result run(MinecraftServer server, HeroKind kind, FormsFile.Expect expect) {
        List<String> bad = new ArrayList<>();
        // ---- the file, the registries, the skill table
        JsonObject root = read(kind, bad);
        if (root != null) {
            bad.addAll(FormsFile.check(root, expect));
            for (String id : FormsFile.itemIds(root)) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) bad.add("the forms ask for the unknown item " + id);
            }
            for (String tag : FormsFile.tags(root)) {
                ResourceLocation rl = ResourceLocation.tryParse(tag);
                if (rl == null || BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl)).isEmpty()) bad.add("the forms ask for the unknown item tag " + tag);
            }
        }
        if (!HeroLogics.has(kind)) bad.add("no logic is registered for " + kind);
        List<HeroSkill> skills = HeroLogics.skillsOf(kind);
        Set<String> have = new HashSet<>();
        for (HeroSkill s : skills) have.add(s.id());
        if (!have.equals(expect.skillIds())) bad.add("the registered skills " + have + " differ from the numbers table " + expect.skillIds());
        List<HeroForm> loaded = HeroForms.of(kind);
        if (root != null && loaded.size() > 1) {
            List<String> fileIds = new ArrayList<>(), gameIds = new ArrayList<>();
            for (JsonElement f : root.getAsJsonArray("forms")) if (f.isJsonObject() && f.getAsJsonObject().has("id")) fileIds.add(f.getAsJsonObject().get("id").getAsString());
            for (HeroForm f : loaded) gameIds.add(f.id());
            if (!fileIds.equals(gameIds)) bad.add("the game loaded the forms " + gameIds + " but the file has " + fileIds);
        }
        for (HeroForm f : loaded) for (String id : f.skills().values()) if (HeroLogics.skill(id) == null) bad.add("the form " + f.id() + " names the unknown skill " + id);
        if (!bad.isEmpty() || skills.isEmpty()) return finish(bad, "no cast");

        // ---- cast everything with a stand-in player
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        FakePlayer fp = SelfTests.player(level, "hero_" + kind.pkg(), spawn.getX() + 0.5, y, spawn.getZ() + 0.5);
        if (HeroBonds.has(fp)) HeroBonds.release(fp);
        String refused = HeroBonds.bind(fp, kind);
        if (refused != null) {
            bad.add("the bond was refused: " + refused);
            return finish(bad, "no cast");
        }
        Zombie dummy = null;
        int casts = 0, hooks = 0;
        try {
            fp.setYRot(-90f);
            fp.setXRot(0f);
            dummy = EntityType.ZOMBIE.create(level);
            if (dummy != null) {
                dummy.setPos(fp.getX() + 3.0, fp.getY(), fp.getZ());
                dummy.setNoAi(true);
                dummy.setSilent(true);
                level.addFreshEntity(dummy);
            }
            HeroBond bond = HeroBonds.of(fp);
            bond.setSummoned(true);
            fp.setItemInHand(InteractionHand.MAIN_HAND, HeroItems.stack(kind));
            HeroCtx base = HeroCtx.of(fp);
            if (base == null) {
                bad.add("no context could be built for the bonded stand-in player");
                return finish(bad, "no cast");
            }
            for (HeroSkill s : skills) {
                if (s.ultimate() ? s.cooldownTicks(base) < 600 : s.cooldownTicks(base) < 60) bad.add(s.id() + " has the cooldown " + s.cooldownTicks(base));
                if (s.magoiShare() < 0.01 || s.magoiShare() > 0.08) bad.add(s.id() + " has the cost " + s.magoiShare());
                if (s.windUpTicks() < 0) bad.add(s.id() + " has a negative wind-up");
                final HeroSkill skill = s;
                guard(bad, skill.id(), () -> skill.cast(base));
                casts++;
            }
            for (HeroForm f : loaded) {
                if (f.skills().isEmpty()) continue;
                bond.setForm(f.id());
                HeroCtx under = HeroCtx.of(fp);
                if (under == null) continue;
                for (String id : f.skills().values()) {
                    HeroSkill skill = HeroLogics.skill(id);
                    if (skill == null) continue;
                    guard(bad, id + " under " + f.id(), () -> skill.cast(under));
                    casts++;
                }
                hooks += callHooks(bad, kind, under, fp, dummy);
            }
            bond.setForm("base");
            HeroCtx home = HeroCtx.of(fp);
            if (home != null) hooks += callHooks(bad, kind, home, fp, dummy);
        } finally {
            if (dummy != null) dummy.discard();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            HeroBonds.of(fp).setSummoned(false);
            fp.removeAllEffects();
            fp.setHealth(fp.getMaxHealth());
            HeroBonds.release(fp);
        }
        return finish(bad, casts + " casts and " + hooks + " hook calls beside a dummy monster");
    }

    /** Every hook of the logic once, each guarded; returns how many were called. */
    private static int callHooks(List<String> bad, HeroKind kind, HeroCtx c, FakePlayer fp, Zombie dummy) {
        HeroLogic logic = HeroLogics.of(kind);
        DamageSource blow = dummy != null ? fp.damageSources().mobAttack(dummy) : fp.damageSources().generic();
        guard(bad, "onSummon", () -> logic.onSummon(c));
        guard(bad, "onTick", () -> logic.onTick(c));
        guard(bad, "onDamaged (a blow)", () -> logic.onDamaged(c, blow, 6f));
        guard(bad, "onDamaged (magic)", () -> logic.onDamaged(c, fp.damageSources().magic(), 6f));
        guard(bad, "onBlocked", () -> logic.onBlocked(c, blow, 6f));
        guard(bad, "onRelease", () -> logic.onRelease(c, 15));
        guard(bad, "onFormChanged", () -> logic.onFormChanged(c, c.form, c.form));
        if (dummy != null) {
            guard(bad, "onHit", () -> logic.onHit(c, dummy, 5f, true));
            guard(bad, "onKill", () -> logic.onKill(c, dummy));
        }
        guard(bad, "onDismiss", () -> logic.onDismiss(c));
        return dummy != null ? 9 : 7;
    }

    private static SelfTests.Result finish(List<String> bad, String detail) {
        if (bad.isEmpty()) return SelfTests.Result.ok(detail);
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    private static JsonObject read(HeroKind kind, List<String> bad) {
        String path = "/data/nusmp/hero/forms/" + kind.itemId() + ".json";
        try (InputStream in = WeaponSelfTest.class.getResourceAsStream(path)) {
            if (in == null) {
                bad.add(path + " is not in the jar");
                return null;
            }
            JsonElement e = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            if (!e.isJsonObject()) {
                bad.add(path + " is not a JSON object");
                return null;
            }
            return e.getAsJsonObject();
        } catch (IOException | RuntimeException ex) {
            bad.add(path + " cannot be read: " + ex.getMessage());
            return null;
        }
    }
}
