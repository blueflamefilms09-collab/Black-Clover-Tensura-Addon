package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.elite.skills.rules.UniqueRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Void Edge (Unique, designed for this mod; the original addon's source was not available): a blade that cuts through defences. Mode 1, Imbue, wraps your melee in the void for twenty seconds
 * (thirty mastered): every melee blow you land also deals a second hit of void damage that IGNORES ARMOUR, worth a share of the blow that grows with the armour the target wears (15% against
 * none, +2% per armour point, 60% at most), breaks the shield of a player who blocks it, and strips 30% of the target's armour for six seconds. Mode 2, Void Slash, sends a cutting wave eight
 * to ten blocks along your sight that hits every foe on it with the same armour-ignoring damage. Allies, pets, summons and you are never cut. Numbers: {@link UniqueRules.VoidEdge}. A mastered
 * Void Edge can become Void Sovereign (data/nusmp/elite/skills/evolutions.json).
 */
public class VoidEdgeSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("void_edge", "Void Edge",
            "Unique. Imbue your melee with the void: each blow deals a second, armour-ignoring hit, breaks shields and strips armour. Void Slash sends a cutting wave through everything in a line.");

    static final String UNTIL = "EdgeUntil";
    private static final ResourceLocation SHRED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "void_edge_shred");
    private static boolean cutting;

    public VoidEdgeSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("void_edge.imbue", "Imbue Edge", UniqueRules.VoidEdge.IMBUE_COST, UniqueRules.VoidEdge.IMBUE_COOLDOWN),
                EliteMode.of("void_edge.slash", "Void Slash", UniqueRules.VoidEdge.SLASH_COST, UniqueRules.VoidEdge.SLASH_COOLDOWN));
    }

    @Override protected int color() { return 0xFF6A3FC0; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean mastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        if (mode == 0) {
            instance.getOrCreateTag().putLong(UNTIL, level.getGameTime() + UniqueRules.VoidEdge.imbueTicks(mastered));
            instance.markDirty();
            EliteBridge.follow(level, VfxShape.DARK_SLASH_BLADE, player, player.getEyePosition(), 0xFF6A3FC0, 30, 0.8f);
            EliteBridge.burst(level, player.position().add(0, 1, 0), true, 10, 0.5);
            EliteBridge.sound(level, player.position(), SoundEvents.ENDERMAN_STARE, 0.5f, 1.8f);
            EliteBridge.actionbar(player, SkillMsg.t("void_edge.imbued", "The void clings to your blade.").withStyle(ChatFormatting.DARK_PURPLE));
            return true;
        }
        Vec3 start = player.getEyePosition().add(0, -0.3, 0);
        Vec3 end = EliteBridge.lookPoint(player, UniqueRules.VoidEdge.SLASH_RANGE + (mastered ? 2.0 : 0.0));
        double mastery = masteryFrac(instance);
        for (LivingEntity t : EliteBridge.foesAlong(player, start, end, UniqueRules.VoidEdge.SLASH_WIDTH)) {
            float dmg = (float) (BalanceLaw.damage(player, t, (float) (UniqueRules.VoidEdge.SLASH_DAMAGE * EliteSkillsConfig.damageScale()), mastery));
            t.hurt(player.damageSources().indirectMagic(player, player), dmg);
            afterCut(t);
        }
        EliteBridge.shape(level, VfxShape.DARK_THRUST, start, end, 0xFF6A3FC0, 14, 1.0f);
        EliteBridge.beam(level, start, end, true);
        EliteBridge.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
        return true;
    }

    /** Breaks a blocking player's shield and strips some armour for a few seconds. */
    private static void afterCut(LivingEntity target) {
        if (target instanceof Player p && p.isBlocking()) p.disableShield();
        TimedModifiers.apply(target, Attributes.ARMOR, SHRED_ID, -UniqueRules.VoidEdge.SHRED, UniqueRules.VoidEdge.SHRED_TICKS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    /** Passive while imbued: the second, armour-ignoring hit (queued for the next tick so the target's invulnerability frames cannot swallow either blow). */
    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (cutting || !EliteSkillsConfig.enabled() || !(owner instanceof ServerPlayer player) || source.getDirectEntity() != owner || source.is(DamageTypeTags.IS_PROJECTILE)) return true;
        if (!UniqueRules.VoidEdge.active(owner.level().getGameTime(), instance.getOrCreateTag().getLong(UNTIL)) || amount.get() <= 0) return true;
        if (!EliteBridge.isAimable(player, target)) return true;
        double share = UniqueRules.VoidEdge.voidShare(target.getArmorValue(), masteredBy(instance, owner));
        float extra = (float) (amount.get() * share * EliteSkillsConfig.damageScale());
        if (extra <= 0f) return true;
        EliteDefer.later(1, () -> {
            if (!target.isAlive() || !player.isAlive()) return;
            cutting = true;
            try {
                target.invulnerableTime = 0;
                target.hurt(player.damageSources().indirectMagic(player, player), BalanceLaw.cap(target, extra));
            } finally {
                cutting = false;
            }
            afterCut(target);
            if (player.level() instanceof ServerLevel sl) EliteBridge.spray(sl, true, target.position().add(0, target.getBbHeight() * 0.6, 0), 6, 0.3, 0.3, 0.3, 0.04);
        });
        return true;
    }
}
