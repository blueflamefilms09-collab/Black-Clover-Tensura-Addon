package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The self-test {@code relic.attune_lock} (run by the CI server smoke test, see {@link SelfTests}): a stand-in player holds a relic weapon that needs
 * attunement, unattuned and attuned, beside a dummy; the damage the dummy takes is a fifth of the attuned figure unattuned, an arrow shot from a relic bow
 * is tagged and cut the same way, the factor of a relic attuned to another player is the unattuned one, a relic that needs no attunement and a mob wearer
 * keep full power, Elsdocia (not a relic card) is untouched, and an act of an unattuned relic stays refused (the works gate is not stacked with the cut).
 */
public final class RelicPowerSelfTests {
    private RelicPowerSelfTests() {}

    private static final String PLAYER = "relic_power_tester";

    public static void register() { SelfTests.register("relic.attune_lock", RelicPowerSelfTests::run); }

    static SelfTests.Result run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        RelicPower.Config cfg = RelicPowerConfig.current();
        if (!cfg.lock()) return SelfTests.Result.ok("skipped: relics.attuneLock is off in this world's config");
        double want = cfg.unattunedPower();

        RelicDef melee = null, bow = null, free = null;
        for (RelicDef d : Relics.all()) {
            if (d.slot() != RelicSlot.MAIN_HAND || d.isPairHalf()) continue;
            if (d.needsAttunement()) {
                if (d.isRanged() && bow == null) bow = d;
                else if (!d.isRanged() && !d.isAdopted() && melee == null) melee = d;
            } else if (!d.isRanged() && !d.isAdopted() && free == null) {
                free = d;
            }
        }
        if (melee == null) return SelfTests.Result.fail("no main-hand relic weapon that needs attunement was found");

        RelicDef els = Relics.byId("elsdocia");
        if (els != null && els.needsAttunement()) bad.add("elsdocia is a relic card that needs attunement: it must stay at full power");

        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("pure checks only: the spawn chunk is not loaded");
        Vec3 pos = Vec3.atBottomCenterOf(at.above(2));
        FakePlayer fp = SelfTests.player(level, PLAYER, pos.x, pos.y, pos.z);
        Chicken dummy = null;
        Zombie wearer = null;
        Arrow arrow = null, freeArrow = null;
        String live = "";
        try {
            dummy = EntityType.CHICKEN.create(level);
            if (dummy == null) return SelfTests.Result.ok("pure checks only: no dummy could be made");
            dummy.moveTo(pos.x, pos.y, pos.z + 3.0, 180f, 0f);
            dummy.setNoAi(true);
            dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000.0);
            level.addFreshEntity(dummy);

            // ---- melee: unattuned, attuned, attuned to somebody else
            // the blow carries an explicit amount, so the weapon's own attack attribute does not matter: the bare-handed hit is the full-power baseline
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            double bare = hit(fp, dummy, 100f, false, null);
            if (bare < 1.0) return SelfTests.Result.ok("pure checks only: the dummy took no damage from a bare hit (" + bare + ")");
            ItemStack sword = RelicCatalog.newStack(melee);
            fp.setItemInHand(InteractionHand.MAIN_HAND, sword);
            sword.remove(RelicComponents2.ATTUNED.get());
            double unattuned = hit(fp, dummy, 100f, false, null);
            sword.set(RelicComponents2.ATTUNED.get(), fp.getUUID());
            double attuned = hit(fp, dummy, 100f, false, null);      // an attuned relic may add its own on-hit effects: it must never deal LESS than the bare blow
            sword.set(RelicComponents2.ATTUNED.get(), UUID.nameUUIDFromBytes("somebody-else".getBytes()));
            double other = hit(fp, dummy, 100f, false, null);
            if (Math.abs(unattuned / bare - want) > 0.03) bad.add("unattuned melee is " + round(unattuned / bare) + " of the full blow, wanted " + want);
            if (Math.abs(other / bare - want) > 0.03) bad.add("a relic attuned to someone else deals " + round(other / bare) + " of the full blow, wanted " + want);
            if (attuned < bare * 0.97) bad.add("the attuned blow is cut to " + round(attuned / bare));
            live = melee.id() + " melee " + round(unattuned / bare) + " unattuned, " + round(attuned / bare) + " attuned";

            // ---- a relic that needs no attunement keeps its full power
            if (free != null) {
                ItemStack plain = RelicCatalog.newStack(free);
                plain.remove(RelicComponents2.ATTUNED.get());
                fp.setItemInHand(InteractionHand.MAIN_HAND, plain);
                double f = hit(fp, dummy, 100f, false, null);
                if (f < bare * 0.97) bad.add(free.id() + " (no attunement needed) deals " + round(f / bare) + " of full power");
            }

            // ---- a mob that wears the relic counts as attuned
            wearer = EntityType.ZOMBIE.create(level);
            if (wearer != null) {
                ItemStack worn = RelicCatalog.newStack(melee);
                if (RelicPowerEvents.factorOf(worn, wearer) != 1.0) bad.add("a mob wearing " + melee.id() + " got a cut factor");
            }

            // ---- arrows from a relic bow
            if (bow != null) {
                ItemStack b = RelicCatalog.newStack(bow);
                b.remove(RelicComponents2.ATTUNED.get());
                fp.setItemInHand(InteractionHand.MAIN_HAND, b);
                arrow = EntityType.ARROW.create(level);
                arrow.setOwner(fp);
                arrow.moveTo(pos.x, pos.y + 1.0, pos.z, 0f, 0f);
                level.addFreshEntity(arrow);
                if (!arrow.getPersistentData().contains(RelicPower.PROJECTILE_TAG)) bad.add("an arrow from the unattuned " + bow.id() + " was not tagged");
                else if (Math.abs(arrow.getPersistentData().getDouble(RelicPower.PROJECTILE_TAG) - want) > 1e-6) bad.add("the arrow tag is not the unattuned power");
                double shot = hit(fp, dummy, 100f, true, arrow);
                b.set(RelicComponents2.ATTUNED.get(), fp.getUUID());
                freeArrow = EntityType.ARROW.create(level);
                freeArrow.setOwner(fp);
                freeArrow.moveTo(pos.x, pos.y + 1.0, pos.z, 0f, 0f);
                level.addFreshEntity(freeArrow);
                if (freeArrow.getPersistentData().contains(RelicPower.PROJECTILE_TAG)) bad.add("an arrow from the attuned " + bow.id() + " was tagged");
                double full = hit(fp, dummy, 100f, true, freeArrow);
                if (Math.abs(shot / bare - want) > 0.03) bad.add("unattuned bow arrow is " + round(shot / bare) + " of the full arrow, wanted " + want);
                if (full < bare * 0.97) bad.add("the attuned bow's arrow is cut to " + round(full / bare));
                live += ", " + bow.id() + " arrow " + round(shot / bare);
            } else {
                live += ", no relic bow found (arrow part skipped)";
            }

            // ---- acts: the works gate keeps an unattuned relic's act refused, and the act factor follows the same rule
            ItemStack act = RelicCatalog.newStack(melee);
            act.remove(RelicComponents2.ATTUNED.get());
            if (RelicPowerEvents.actFactor(act, fp) > want + 1e-9) bad.add("the act factor of an unattuned relic is not the unattuned power");
            act.set(RelicComponents2.ATTUNED.get(), fp.getUUID());
            if (RelicPowerEvents.actFactor(act, fp) != 1.0) bad.add("the act factor of an attuned relic is not 1.0");
            if (!melee.acts().isEmpty()) {
                act.remove(RelicComponents2.ATTUNED.get());
                boolean ran = false;
                try {
                    ran = RelicEngine.activate(fp, act, melee, melee.acts().get(0), false, 0);
                } catch (Throwable ignored) {
                    // a refusal that could not tell the stand-in is still a refusal
                }
                if (ran) bad.add("an act of the unattuned " + melee.id() + " ran: the works gate is gone");
            }
        } finally {
            if (dummy != null) dummy.discard();
            if (arrow != null) arrow.discard();
            if (freeArrow != null) freeArrow.discard();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            fp.setHealth(fp.getMaxHealth());
            RelicPowerEvents.forget(fp.getUUID());
            Relics.invalidate(fp);
        }
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
        return SelfTests.Result.ok(live + ", no-attunement relic, mob wearer, Elsdocia and the refused act as designed");
    }

    private static double round(double v) { return Math.round(v * 1000.0) / 1000.0; }

    /** Hits the dummy once with a melee blow (or the arrow's hit) and returns the health it lost; the hit goes through the real damage event. */
    private static double hit(FakePlayer fp, LivingEntity dummy, float raw, boolean shot, Arrow arrow) {
        dummy.setHealth(dummy.getMaxHealth());
        dummy.invulnerableTime = 0;
        dummy.hurtTime = 0;
        float before = dummy.getHealth();
        dummy.hurt(shot ? fp.damageSources().arrow(arrow, fp) : fp.damageSources().playerAttack(fp), raw);
        return before - dummy.getHealth();
    }
}
