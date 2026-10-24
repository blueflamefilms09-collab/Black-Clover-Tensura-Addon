package com.newuniverse.nusmp.ultimate;

import com.newuniverse.nusmp.avatar.cast.AvatarSkillList;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.ultimate.UltimateRules.Cancel;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, so the real game proves what can be proved without a person (the CI server smoke test runs these; key prefix {@code magic.ultimate}).
 * <ul>
 *   <li>{@code magic.ultimate.rules}: the table is sound; every Ultimate is the LAST family page of its real book, has a handler, a name, a lang text and real effect shapes and potion effects; the gate is closed for a
 *       fresh player in every way it is asked (the verdict, the page, the grimoire's own cast, the Avatar's list); the bounds hold (worst-case packets, targets, damage, cooldown).</li>
 *   <li>{@code magic.ultimate.cast}: with stand-in players: one tracked entry per caster, the global cap, the movement hold and its removal, every cancel reason (nothing spent, a lockout), the release (the whole magicule is
 *       taken, the cooldown set, at most 24 foes hit, an ally / a creative player / the caster / a far mob untouched, the debuffs applied) and a clean end.</li>
 * </ul>
 */
public final class UltimateSelfTests {
    private UltimateSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.ultimate.rules", UltimateSelfTests::rules);
        SelfTests.register("magic.ultimate.cast", UltimateSelfTests::cast);
    }

    private static GrimoireBook book(String path) {
        for (var h : NUSkills.BOOKS) if (h.getId().getPath().equals(path)) return h.get();
        return null;
    }

    private static Set<String> langKeys() {
        Set<String> keys = new HashSet<>();
        try (InputStream in = UltimateSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
            if (in != null) {
                var o = com.google.gson.JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var e : o.entrySet()) keys.add(e.getKey());
            }
        } catch (Exception ignored) {
            // an unreadable lang file shows up below as a missing key
        }
        return keys;
    }

    private static Result rules(MinecraftServer server) {
        List<String> problems = UltimateRegistry.problems();
        if (!problems.isEmpty()) return Result.fail("the table: " + problems.get(0));
        if (!UltimateConfig.enabled()) return Result.fail("ultimate.enabled does not default to true");
        if (UltimateRegistry.all().size() < 8) return Result.fail("only " + UltimateRegistry.all().size() + " Ultimates are registered");
        Set<String> lang = langKeys();
        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "ultimate_rules", 8.5, 120, 8.5);
        int packets = 0;
        for (UltimateDef d : UltimateRegistry.all()) {
            GrimoireBook b = book(d.bookPath());
            if (b == null) return Result.fail(d.id() + ": " + d.bookPath() + " is not a registered book");
            List<BookPage> pages = b.pages();
            int fam = b.familyCount();
            BookPage last = b.page(fam - 1);
            if (last == null || !last.id().equals(d.id())) return Result.fail(d.bookPath() + ": the last family page is " + (last == null ? "none" : last.id()) + ", not " + d.id());
            if (UltimatePages.extraCount(d.bookPath()) != 1) return Result.fail(d.bookPath() + ": extraCount is not 1");
            if (last.cast() == null) return Result.fail(d.id() + " has no handler");
            if (last.costPercent() != 0 || last.cooldown() != 0) return Result.fail(d.id() + ": the page must cost nothing in the grimoire flow (the manager charges the whole mana at the release)");
            for (BookPage pg : pages.subList(0, fam - 1)) if (pg.id().equals(d.id())) return Result.fail(d.id() + " appears twice in " + d.bookPath());
            if (fam > 64) return Result.fail(d.bookPath() + " has " + fam + " family pages, over the 64 the unlock mask holds");
            if (!lang.contains(d.langKey())) return Result.fail("no lang text " + d.langKey() + " (merge tools/lang/ultimate.json)");
            for (String sh : new String[]{d.chargeShape(), d.burstShape(), d.echoShape()}) {
                try { VfxShape.valueOf(sh); } catch (IllegalArgumentException x) { return Result.fail(d.id() + ": missing VfxShape " + sh); }
                if (sh.startsWith("TIME_")) return Result.fail(d.id() + ": the Time Magic effects are not to be reused");
            }
            for (UltimateDef.Debuff db : d.debuffs())
                if (BuiltInRegistries.MOB_EFFECT.getOptional(ResourceLocation.parse(db.effect())).isEmpty()) return Result.fail(d.id() + ": unknown potion effect " + db.effect());
            // closed for a fresh player in every way it can be asked
            ManasSkillInstance inst = b.createDefaultInstance();
            if (UltimateManager.verdict(p, b, inst).open()) return Result.fail("a fresh player passes the gate of " + d.id());
            if (UltimatePages.block(b, last, p, inst) == null) return Result.fail(d.id() + ": the page is not blocked for a fresh player");
            if (b.usable(inst, p, fam - 1)) return Result.fail(d.id() + " is usable for a fresh player");
            if (UltimateManager.begin(b, inst, p, fam - 1, d)) return Result.fail(d.id() + " began a wind-up for a fresh player");
            if (UltimateManager.casting(p) || UltimateManager.activeCount() != 0) return Result.fail("a refused cast left an entry");
            if (AvatarSkillList.allowedPage(d.id(), 0.0)) return Result.fail("the Avatar's cast list allows " + d.id());
            packets = Math.max(packets, UltimateRules.packetsFor(UltimateRules.windupTicks(d.windupTicks(), 1.0)));
            if (UltimateRules.pulseRaw(d.damage(), 2.0, 0, 0.0, d.radius()) > UltimateRules.MAX_RAW) return Result.fail(d.id() + ": a pulse can go over the raw damage clamp");
        }
        if (packets > UltimateRules.MAX_EFFECT_PACKETS) return Result.fail("a cast can send " + packets + " effect packets, over " + UltimateRules.MAX_EFFECT_PACKETS);
        if (UltimateGate.passes(1.0, UltimateGate.Progress.NONE)) return Result.fail("a player with nothing passes the gate");
        if (!UltimateGate.passes(1.0, new UltimateGate.Progress(1.0, 14, 1_000_000, false))) return Result.fail("a player at the top of the ladder is refused");
        if (UltimateRules.cooldownTicks(1) < UltimateRules.MIN_COOLDOWN_TICKS) return Result.fail("the cooldown can be configured under its floor");
        return Result.ok(UltimateRegistry.all().size() + " Ultimates, each the last family page of its book, with handler, lang text, shapes and potion effects; closed for a fresh player in 6 ways; worst case "
                + packets + " effect packets per cast, " + UltimateRules.MAX_TARGETS + " targets x " + UltimateRules.PULSES + " pulses");
    }

    private static Result cast(MinecraftServer server) {
        ServerLevel level = server.overworld();
        UltimateManager.clearAll();
        UltimateDef def = UltimateRegistry.byId("ultimate_fire");
        GrimoireBook fire = book(def.bookPath());
        ManasSkillInstance inst = fire.createDefaultInstance();
        FakePlayer a = SelfTests.standIn(level, "ultimate_a", 8.5, 120, 8.5), b = SelfTests.standIn(level, "ultimate_b", 8.5, 120, 40.5), c = SelfTests.standIn(level, "ultimate_c", 8.5, 120, 72.5);
        int before = UltimateManager.activeCount(), canceled = 0;

        // one tracked entry per caster, the global cap, the movement hold
        long cd0 = UltimatePlayer.cooldownUntil(a);
        if (!UltimateManager.startForTest(a, def, fire, inst, 100)) return Result.fail("the test cast did not start");
        if (UltimateManager.startForTest(a, def, fire, inst, 100)) return Result.fail("a second cast started for the same caster");
        int cap = UltimateConfig.maxSimultaneous();
        int started = 1;
        FakePlayer[] others = {b, c};
        for (FakePlayer o : others) if (UltimateManager.startForTest(o, def, fire, inst, 100)) started++;
        if (UltimateManager.activeCount() != before + Math.min(cap, started)) return Result.fail("the global cap " + cap + " did not hold: " + UltimateManager.activeCount() + " casts for " + started + " starts");
        if (started > cap) return Result.fail(started + " casts started with a cap of " + cap);
        var speed = a.getAttribute(Attributes.MOVEMENT_SPEED);
        if (!speed.hasModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "ultimate_lock_speed"))) return Result.fail("the caster is not held in place during the wind-up");

        // every cancel reason: the hold is taken off, the entry is gone, a lockout is set (not for a server stop)
        for (Cancel why : new Cancel[]{Cancel.DEATH, Cancel.DIMENSION, Cancel.DAMAGE, Cancel.SEALED, Cancel.SNEAK, Cancel.LOW_MANA, Cancel.LOGOUT}) {
            if (!UltimateManager.casting(a) && !UltimateManager.startForTest(a, def, fire, inst, 100)) return Result.fail("could not restart a cast for " + why);
            UltimateManager.cancel(a, why);
            canceled++;
            if (UltimateManager.casting(a)) return Result.fail("the entry outlived the cancel " + why);
            if (speed.hasModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "ultimate_lock_speed"))) return Result.fail("the movement hold outlived the cancel " + why);
            if (a.getAttribute(Attributes.JUMP_STRENGTH).hasModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "ultimate_lock_jump"))) return Result.fail("the jump hold outlived the cancel " + why);
            if (UltimatePlayer.lockUntil(a) <= level.getGameTime()) return Result.fail("no lockout after " + why);
            if (UltimatePlayer.cooldownUntil(a) != cd0) return Result.fail("a cancelled cast started the long cooldown (" + why + ")");
        }
        UltimateManager.cancel(b, Cancel.ADMIN);
        UltimateManager.cancel(c, Cancel.ADMIN);
        if (UltimateManager.activeCount() != before) return Result.fail("entries left over: " + (UltimateManager.activeCount() - before));
        UltimatePlayer.setLockUntil(a, 0);

        // the release: the whole magicule, the cooldown, the foes hit and the others not
        var ex = TensuraStorages.getExistenceFrom(a);
        boolean mana = ex != null;
        if (mana) {
            ex.setMagicule(EnergyHelper.getMaxMagicule(a));
            ex.markDirty();
        }
        double cx = a.getX(), cy = a.getY(), cz = a.getZ();
        List<Pig> pigs = new ArrayList<>();
        Pig near = null, far = null;
        int crowd = UltimateRules.MAX_TARGETS + 8;
        for (int i = 0; i < crowd + 2; i++) {
            Pig z = EntityType.PIG.create(level);
            if (z == null) return Result.fail("could not create a test pig");
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
            z.setHealth(2000f);
            z.setNoAi(true);
            z.setRemainingFireTicks(0);
            z.moveTo(cx + 2 + (i % 5) * 0.4, cy, cz + (i / 5) * 0.3);
            level.addFreshEntity(z);
            pigs.add(z);
        }
        near = pigs.get(0);
        far = pigs.get(pigs.size() - 1);
        far.moveTo(cx + 80, cy, cz);        // far outside any radius
        FakePlayer creative = SelfTests.standIn(level, "ultimate_creative", cx - 2, cy, cz);
        creative.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        float creativeHealth = creative.getHealth();
        try {
            if (!UltimateManager.startForTest(a, def, fire, inst, 100)) return Result.fail("the release test cast did not start");
            UltimateManager.forceRelease(a);
            var s = UltimateManager.session(a);
            if (s == null || !s.released) return Result.fail("the release did not happen");
            if (mana && ex.getMagicule() > 1e-6 && !a.isCreative()) return Result.fail("the release left " + ex.getMagicule() + " magicule: it must take ALL of it");
            if (UltimatePlayer.cooldownUntil(a) < a.level().getGameTime() + UltimateRules.MIN_COOLDOWN_TICKS) return Result.fail("the cooldown after a release is under the minimum");
            float selfHealth = a.getHealth();      // after the mana law's blackout (the price of burning it all), before the pulses
            UltimateManager.forcePulses(a);
            int hit = 0, burning = 0, slowed = 0;
            for (Pig z : pigs) {
                if (z == far) continue;
                if (z.getHealth() < 2000f) hit++;
                if (z.getRemainingFireTicks() > 0) burning++;
                if (z.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) slowed++;
            }
            if (hit == 0) return Result.fail("no foe was hurt by the release");
            if (hit > UltimateRules.MAX_TARGETS) return Result.fail(hit + " foes were hurt, over the " + UltimateRules.MAX_TARGETS + " target cap");
            if (burning == 0 || slowed == 0) return Result.fail("the debuffs did not reach the foes (burning " + burning + ", slowed " + slowed + ")");
            if (far.getHealth() < 2000f) return Result.fail("a mob far outside the radius was hurt");
            if (creative.getHealth() < creativeHealth) return Result.fail("a creative player was hurt");
            if (a.getHealth() < selfHealth) return Result.fail("the caster was hurt by their own Ultimate");
            if (UltimateManager.casting(a) || UltimateManager.activeCount() != before) return Result.fail("the entry outlived the last pulse");
            if (speed.hasModifier(ResourceLocation.fromNamespaceAndPath("nusmp", "ultimate_lock_speed"))) return Result.fail("the movement hold outlived the release");
            // the cooldown refuses a second cast at once (the gate refuses it first for a stand-in: both are a refusal with nothing started)
            if (UltimateManager.begin(fire, inst, a, 0, def) || UltimateManager.casting(a)) return Result.fail("a second Ultimate began during the cooldown");
            return Result.ok("one entry per caster, cap " + cap + ", hold on and off, " + canceled + " cancel reasons clean, release took all magicule" + (mana ? "" : " (stand-in has no magicule data: that check skipped)")
                    + ", " + hit + " of " + (crowd + 1) + " near mobs hit (cap " + UltimateRules.MAX_TARGETS + "), burning " + burning + ", slowed " + slowed + ", far mob / creative player / caster untouched");
        } finally {
            for (Pig z : pigs) z.discard();
            UltimateManager.clearAll();
            UltimatePlayer.setCooldownUntil(a, 0);
            UltimatePlayer.setLockUntil(a, 0);
        }
    }
}
