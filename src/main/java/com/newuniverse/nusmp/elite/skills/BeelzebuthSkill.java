package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.UltimateRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Beelzebuth (Ultimate, designed for this mod from the Tensura Lord of Gluttony; the original addon's source was not available): hunger that feeds on what it kills. PASSIVE: every kill returns
 * health (10%, a boss 30%) and magicules (5%, a boss 15%) and adds a Hunger stack (ten at most, a boss three, each lasting a minute) that makes everything you hit harder by 4% per stack.
 * Mode 1, Devour: bite the foe you look at (six blocks) for heavy damage; half of it returns as health and a share of your magicules is restored. Mode 2, Maw: a ring of hunger within eleven
 * blocks drags every foe toward you and bites them (the drag is a hard control: it obeys the shared 12-second lock). Mode 3, Domain: for six seconds the foes within eight blocks lose 4% of their
 * maximum health every second (capped for players by the balance law), and half of what is eaten heals you. It is not the "Maw of Beelzebuth" ability of the skill-line grid, which is a separate,
 * older thing. Numbers: {@link UltimateRules.Beelzebuth}. Beelzebuth is the evolution of Scavenger in data/nusmp/elite/skills/evolutions.json.
 */
public class BeelzebuthSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("beelzebuth", "Beelzebuth",
            "Ultimate. Gluttony: kills heal you, feed your magicules and stack Hunger (more damage). Devour bites a foe and heals you, Maw drags foes in, Domain eats at everything around you.");

    static final String HUNGER = "Hunger", HUNGER_UNTIL = "HungerUntil";

    public BeelzebuthSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("beelzebuth.devour", "Devour", UltimateRules.Beelzebuth.DEVOUR_COST, UltimateRules.Beelzebuth.DEVOUR_COOLDOWN),
                EliteMode.of("beelzebuth.maw", "Maw", UltimateRules.Beelzebuth.MAW_COST, UltimateRules.Beelzebuth.MAW_COOLDOWN),
                EliteMode.of("beelzebuth.domain", "Domain of Hunger", UltimateRules.Beelzebuth.DOMAIN_COST, UltimateRules.Beelzebuth.DOMAIN_COOLDOWN));
    }

    @Override protected int color() { return 0xFF5A1A3A; }

    static int hunger(ManasSkillInstance instance, LivingEntity e) {
        var tag = instance.getOrCreateTag();
        return e.level().getGameTime() < tag.getLong(HUNGER_UNTIL) ? tag.getInt(HUNGER) : 0;
    }

    /** The Hunger bonus on a blow by this owner (1.0 with no stacks). */
    static double hungerMultiplier(ManasSkillInstance instance, LivingEntity owner) { return UltimateRules.Beelzebuth.hungerBonus(hunger(instance, owner)); }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean ownMastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        double mastery = masteryFrac(instance);
        double hungry = EliteSkillsConfig.damageScale();      // the Hunger bonus itself is applied by the damage handler to every blow of the owner, these included
        switch (mode) {
            case 0 -> {
                LivingEntity t = EliteBridge.lookFoe(player, UltimateRules.Beelzebuth.DEVOUR_RANGE);
                if (t == null) {
                    EliteBridge.fail(player, SkillMsg.t("beelzebuth.no_prey", "Nothing within reach to devour."));
                    return false;
                }
                float dealt = EliteBridge.hit(player, t, UltimateRules.Beelzebuth.DEVOUR_DAMAGE * hungry, mastery, false);
                if (dealt > 0f) BalanceLaw.heal(player, (float) (dealt * UltimateRules.Beelzebuth.devourLifesteal(ownMastered)));
                EliteUtil.addMagicule(player, EliteUtil.maxMagicule(player) * UltimateRules.Beelzebuth.devourMagicule(ownMastered));
                EliteBridge.shape(level, VfxShape.SHADOW_POOL, t.position(), t.position().add(0, 1, 0), 0xFF5A1A3A, 20, 1.0f);
                EliteBridge.burst(level, t.position().add(0, t.getBbHeight() * 0.5, 0), true, 14, 0.6);
                EliteBridge.sound(level, t.position(), SoundEvents.GENERIC_EAT, 1.0f, 0.5f);
                return true;
            }
            case 1 -> {
                Vec3 at = player.position();
                List<LivingEntity> prey = EliteBridge.foes(player, at, UltimateRules.Beelzebuth.MAW_RADIUS);
                if (prey.isEmpty()) {
                    EliteBridge.fail(player, SkillMsg.t("beelzebuth.nobody", "Nobody is within reach of the maw."));
                    return false;
                }
                boolean drag = BalanceLaw.beginControl(player);
                for (LivingEntity t : prey) {
                    if (drag) {
                        Vec3 to = at.subtract(t.position());
                        if (to.lengthSqr() > 1.0) {
                            t.setDeltaMovement(t.getDeltaMovement().scale(0.4).add(to.normalize().scale(UltimateRules.Beelzebuth.MAW_PULL * (BalanceLaw.isBoss(t) ? 0.4 : 1.0))));
                            t.hurtMarked = true;
                        }
                    }
                    EliteBridge.hit(player, t, UltimateRules.Beelzebuth.MAW_DAMAGE * hungry, mastery, true);
                }
                EliteBridge.shape(level, VfxShape.SHADOW_POOL, at, at.add(0, 1, 0), 0xFF5A1A3A, 30, (float) (UltimateRules.Beelzebuth.MAW_RADIUS / 5.0));
                EliteBridge.sound(level, at, SoundEvents.RAVAGER_ROAR, 0.8f, 0.6f);
                return true;
            }
            default -> {
                final double radius = UltimateRules.Beelzebuth.DOMAIN_RADIUS;
                EliteBridge.follow(level, VfxShape.SHADOW_POOL, player, player.position().add(0, 0.1, 0), 0xFF5A1A3A, UltimateRules.Beelzebuth.DOMAIN_PULSES * 20, (float) (radius / 4.0));
                EliteBridge.startZone(player, 20, UltimateRules.Beelzebuth.DOMAIN_PULSES, (owner, lvl, index) -> {
                    float eaten = 0f;
                    for (LivingEntity t : EliteBridge.foes(owner, owner.position(), radius)) {
                        double bite = Math.max(3.0, t.getMaxHealth() * UltimateRules.Beelzebuth.DOMAIN_BITE);
                        eaten += EliteBridge.hit(owner, t, bite * EliteSkillsConfig.damageScale(), mastery, true);
                        EliteBridge.spray(lvl, true, t.position().add(0, t.getBbHeight() * 0.5, 0), 4, 0.3, 0.3, 0.3, 0.02);
                    }
                    if (eaten > 0f) BalanceLaw.heal(owner, eaten * 0.5f);
                });
                EliteBridge.sound(level, player.position(), SoundEvents.WARDEN_HEARTBEAT, 1.0f, 0.6f);
                EliteBridge.actionbar(player, SkillMsg.t("beelzebuth.domain", "Hunger spreads around you.").withStyle(ChatFormatting.DARK_RED));
                return true;
            }
        }
    }

    /** A kill by the owner: heal, magicules, a Hunger stack. Called by the elite kill handler. */
    static void onKill(ManasSkillInstance instance, ServerPlayer killer, LivingEntity victim) {
        boolean boss = BalanceLaw.isBoss(victim);
        BalanceLaw.heal(killer, (float) (killer.getMaxHealth() * UltimateRules.Beelzebuth.killHeal(boss)));
        EliteUtil.addMagicule(killer, EliteUtil.maxMagicule(killer) * UltimateRules.Beelzebuth.killMagicule(boss));
        var tag = instance.getOrCreateTag();
        int stacks = Math.min(UltimateRules.Beelzebuth.STACK_CAP, hunger(instance, killer) + UltimateRules.Beelzebuth.stacksForKill(boss));
        tag.putInt(HUNGER, stacks);
        tag.putLong(HUNGER_UNTIL, killer.level().getGameTime() + UltimateRules.Beelzebuth.STACK_TICKS);
        instance.markDirty();
        EliteBridge.spray(killer.serverLevel(), true, killer.position().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0.03);
    }
}
