package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRole;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.boss.budget.BudgetedBoss;
import com.newuniverse.nusmp.boss.rules.BossHealGuard;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Why: 3.64.0, the Black Clover bosses are three times as strong ({@code bossRules.blackCloverStatMultiplier}, default 3, 1 = off). A boss counts when it is in the boss registry
 * and its roster character is a {@code BOSS} whose source work is "Black Clover" ({@link BossStatMath#applies}): the Dark Triad, Lucius, the Devil Avatar, Zagred, the Midnight Sun
 * elves, the early-arc and Spade Kingdom bosses. Nobody else is touched.
 * <ul>
 * <li><b>Health</b>: when the boss joins a level it gets one permanent maximum-health modifier (fixed id {@code nusmp:boss_stat/health}, +2.0 multiplied-total = x3). It is saved with the
 * boss, so a reload keeps the bar; a changed setting replaces it. The health keeps its share of the bar, written through the boss's heal guard and damage budget like the boss's own refills.
 * The other health systems (arena scaling, floor scaling) are separate modifiers and multiply with it.</li>
 * <li><b>Damage</b>: every hit whose attacker is such a boss is multiplied ({@link BossStatMath#damage}), whether melee, a spell or Lucius's percent true damage, since all of them arrive as an
 * incoming-damage event. A buffed hit never takes more than half of the victim's maximum health in one blow. Other mod bosses are never the victim.</li>
 * </ul>
 * Cost: nothing per tick. One registry lookup per damage event with a non-null attacker, a roster lookup only when that attacker is a registered boss; one modifier per boss at join.
 */
public final class BossStatBuff {
    private BossStatBuff() {}

    static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "boss_stat/health");

    /** The multiplier for this entity: 1 unless it is a registered boss of the Black Clover roster. */
    static double multiplier(Entity e) {
        String id = BossEntities.bossId(e);
        if (id == null) return 1.0;
        ArcCharacter c = ArcRoster.byId(id);
        if (c == null || !BossStatMath.applies(c.source(), c.role() == ArcRole.BOSS)) return 1.0;
        return BossStatMath.clamp(BossRulesConfig.blackCloverStatMultiplier());
    }

    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof LivingEntity b) || !b.isAlive()) return;
        if (BossEntities.bossId(b) == null) return;
        double want = BossStatMath.healthModifier(multiplier(b));
        AttributeInstance max = b.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;
        AttributeModifier have = max.getModifier(HEALTH_ID);
        if (have == null ? want <= 0.0 : Math.abs(have.amount() - want) < 1.0e-9) return;       // nothing to add, or already there (a reload)
        float before = b.getMaxHealth(), hp = b.getHealth();
        if (want <= 0.0) max.removeModifier(HEALTH_ID);
        else max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, want, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        float to = BossStatMath.keepShare(hp, before, b.getMaxHealth());
        if (Math.abs(to - hp) < 1.0e-3f) return;
        Runnable write = () -> BossHealGuard.allow(() -> b.setHealth(to));
        if (b instanceof BudgetedBoss bb) bb.dpsBudget().bypass(write);
        else write.run();
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !(e.getAmount() > 0f)) return;
        Entity attacker = e.getSource().getEntity();
        if (attacker == null || attacker == victim) return;
        double m = multiplier(attacker);
        if (m <= 1.0 || BossEntities.isModBoss(victim)) return;
        e.setAmount(BossStatMath.damage(e.getAmount(), m, victim.getMaxHealth()));
    }
}
