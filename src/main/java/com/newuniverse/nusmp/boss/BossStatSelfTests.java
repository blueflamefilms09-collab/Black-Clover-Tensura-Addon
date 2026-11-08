package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the Black Clover bosses are three times as strong (3.64.0) and nobody can watch that in a game here, so the CI server smoke test does: key {@code boss.stats}. It summons one Black Clover
 * boss and one of another work, and checks that the first carries the maximum-health modifier of the setting on a full bar and the second carries none. Server safe, both bosses are removed at the end.
 */
final class BossStatSelfTests {
    private BossStatSelfTests() {}

    static void register() { SelfTests.register("boss.stats", BossStatSelfTests::run); }

    private static ArcCharacter pick(boolean blackClover) {
        for (ArcCharacter c : ArcRoster.bosses())
            if (BossStatMath.applies(c.source(), true) == blackClover && ArcSpawner.typeOf(c.id()) != null) return c;
        return null;
    }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return Result.ok("skipped: the spawn chunk is not loaded");
        double mult = BossStatMath.clamp(BossRulesConfig.blackCloverStatMultiplier());
        ArcCharacter bc = pick(true), other = pick(false);
        if (bc == null) return Result.fail("no Black Clover boss can be summoned");
        ArcBoss a = null, b = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, bc, Vec3.atBottomCenterOf(at.above(2)).add(6, 0, 0), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return Result.ok("skipped: " + bc.id() + " was not summoned (" + r.outcome() + ")");
            a = r.boss();
            AttributeInstance max = a.getAttribute(Attributes.MAX_HEALTH);
            AttributeModifier m = max == null ? null : max.getModifier(BossStatBuff.HEALTH_ID);
            if (mult > 1.0) {
                if (m == null || Math.abs(m.amount() - (mult - 1.0)) > 1.0e-9) return Result.fail(bc.id() + " has no x" + mult + " health modifier");
                if (Math.abs(a.getHealth() - a.getMaxHealth()) > 0.5f) return Result.fail(bc.id() + " is not at a full bar after the buff: " + a.getHealth() + " / " + a.getMaxHealth());
            } else if (m != null) return Result.fail(bc.id() + " has the modifier although the setting is off");
            String note = "";
            if (other != null) {
                ArcSpawner.Result r2 = ArcSpawner.summon(level, other, Vec3.atBottomCenterOf(at.above(2)).add(-6, 0, 0), 0f, List.of(), true);
                if (r2.outcome() == ArcSpawner.Outcome.SPAWNED) {
                    b = r2.boss();
                    AttributeInstance max2 = b.getAttribute(Attributes.MAX_HEALTH);
                    if (max2 != null && max2.getModifier(BossStatBuff.HEALTH_ID) != null) return Result.fail(other.id() + " (" + other.source() + ") was buffed");
                    note = ", " + other.id() + " (" + other.source() + ") untouched";
                }
            }
            return Result.ok(bc.id() + " x" + mult + " health (" + (int) a.getMaxHealth() + ") on a full bar" + note);
        } finally {
            if (a != null) a.discard();
            if (b != null) b.discard();
        }
    }
}
