package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.SummonRules.Card;
import com.newuniverse.nusmp.skill.rules.SummonSpecies;
import com.newuniverse.nusmp.skill.rules.SummonerRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Why: the canon pass of the Summoner (docs/summoner_rework.md, Hell Mode wiki) adds a cast for each summon type that is not a creature card (Insect debuffs, Fish buffs,
 * Spirit long-range attack, Dragon area attack, the Angel Merus) and for Awakening and Storage. Each is a bounded effect on foes or allies (never more than
 * {@link SkillTargets#MAX_FOES} entities, the Angel eight, no entity spawned except a visual-only lightning bolt per Angel target), paid and put on cooldown by
 * {@link SummonerSkill}. Nothing here keeps state, so there is nothing to clean up. Server side only. The colours are the mod's own: the wiki describes none.
 */
final class SummonerCanon {
    private SummonerCanon() {}

    private static final int GREEN = 0xA8D04A, BLUE = 0x58B8E8, VIOLET = 0xC8A0FF, RED = 0xFF7038, GOLD = 0xFFE860;

    /** Insect (debuffs on enemies): poison, slowness and weakness on every foe around, for a while. Returns how many were touched. */
    static int insect(ServerPlayer p, Tier tier) {
        double range = SummonerRules.insectRange(tier);
        int ticks = SummonerRules.insectSeconds(tier) * 20, amp = tier.ultimate() ? 1 : 0;
        List<LivingEntity> foes = SkillTargets.foes(p, p.position(), range);
        for (LivingEntity f : foes) {
            f.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, amp));
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
            f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, amp));
            SkillFx.burst(p.serverLevel(), f.position().add(0, f.getBbHeight() * 0.5, 0), true, 6, 0.4);
        }
        if (!foes.isEmpty()) SkillFx.ring(p.serverLevel(), p.position(), range, GREEN);
        if (!foes.isEmpty()) Summons.perform(p, SummonSpecies.HATCH, tier);          // 4.106.0: Hatch shows the cast
        return foes.size();
    }

    /** Fish (buffs): speed, haste and jump for you and your allies around. Always touches the caster. */
    static int fish(ServerPlayer p, Tier tier) {
        int ticks = SummonerRules.fishSeconds(tier) * 20;
        List<LivingEntity> allies = new java.util.ArrayList<>(SkillTargets.alliesAround(p, p.position(), SummonerRules.fishRange()));
        if (!allies.contains(p)) allies.add(p);
        for (LivingEntity a : allies) {
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, true, true));
            a.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 0, true, true));
            a.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 0, true, true));
            SkillFx.burst(p.serverLevel(), a.position().add(0, a.getBbHeight() * 0.5, 0), false, 4, 0.3);
        }
        SkillFx.ring(p.serverLevel(), p.position(), SummonerRules.fishRange(), BLUE);
        Summons.perform(p, SummonSpecies.GENBU, tier);                              // 4.106.0: Genbu shows the cast
        return allies.size();
    }

    /**
     * Spirit (long-range attack): Maria's psychokinesis lifts and hits the foe you look at; from Summoning level 7 (or as the Ultimate) Ellie's gravity also drags the foes
     * around it down (slowness) for a moment. One target plus at most eight around it.
     */
    static int spirit(ServerPlayer p, Tier tier, ManasSkillInstance instance, double masteryFrac) {
        LivingEntity t = SkillTargets.lookFoe(p, SummonerRules.spiritRange());
        if (t == null) return 0;
        SkillDamage.hit(p, t, SummonerRules.spiritDamage(tier), masteryFrac, SkillDamage.Kind.MAGIC, instance);
        t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
        SkillFx.beam(p.serverLevel(), p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), false);
        SkillFx.burst(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.5, 0), false, 10, 0.5);
        if (tier.ultimate() || SummonerRules.summoningLevel(masteryFrac) >= SummonerRules.MAX_LEVEL) {
            int n = 0;
            for (LivingEntity f : SkillTargets.foes(p, t.position(), 5.0)) {
                if (f == t || n++ >= 8) continue;
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            SkillFx.ring(p.serverLevel(), t.position(), 5.0, VIOLET);
        }
        Summons.perform(p, SummonSpecies.ELLIE, tier);                              // 4.106.0: Ellie shows the cast
        return 1;
    }

    /** Dragon (area attack): a burst of fire in a cone in front of you (Dradra's Spitting Fire): damage and burning. */
    static int dragon(ServerPlayer p, Tier tier, ManasSkillInstance instance, double masteryFrac) {
        List<LivingEntity> foes = SkillTargets.foesInCone(p, SummonerRules.dragonRange(tier), 0.5);
        for (LivingEntity f : foes) {
            if (SkillDamage.hit(p, f, SummonerRules.dragonDamage(tier), masteryFrac, SkillDamage.Kind.MAGIC, instance) > 0f) f.igniteForSeconds(5);
            SkillFx.burst(p.serverLevel(), f.position().add(0, f.getBbHeight() * 0.5, 0), false, 8, 0.5);
        }
        if (!foes.isEmpty()) SkillFx.ring(p.serverLevel(), p.position(), SummonerRules.dragonRange(tier) * 0.5, RED);
        if (!foes.isEmpty()) Summons.perform(p, SummonSpecies.DRADRA, tier);        // 4.106.0: Dradra shows the cast
        return foes.size();
    }

    /** Angel (Merus' Thunder of Judgement): lightning on the eight nearest foes, damage and paralysis (a long, strong slowness). A visual-only bolt each; nothing is set on fire. */
    static int angel(ServerPlayer p, Tier tier, ManasSkillInstance instance, double masteryFrac) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> foes = SkillTargets.foes(p, p.position(), 16.0);   // nearest first already
        int n = 0;
        for (LivingEntity f : foes) {
            if (n >= SummonerRules.ANGEL_TARGETS) break;
            n++;
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(f.position());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            SkillDamage.hit(p, f, SummonerRules.angelDamage(tier), masteryFrac, SkillDamage.Kind.MAGIC, instance);
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
            f.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 3));
        }
        if (n > 0) SkillFx.ring(level, p.position(), 16.0, GOLD);
        if (n > 0) Summons.perform(p, SummonSpecies.MERUS, tier);                   // 4.106.0: Merus, the one angel, shows the cast
        return n;
    }

    /**
     * Awakening: each summon uses its awakened skill. Beasts and legions: strength and speed (Hayate); stones: resistance, and for your party too (Steely's Sacrifice);
     * spirits: speed. With the Bird type open the Hawk's Farsight marks every hostile creature within 64 blocks; with the Grass type open Appo's Herb regenerates the
     * allies around. At most the cap's summons, 48 marked creatures, 24 allies.
     */
    static int awaken(ServerPlayer p, Tier tier, boolean bird, boolean grass) {
        int ticks = SummonerRules.awakenSeconds(tier) * 20, n = 0;
        boolean stone = false;
        for (Mob m : Summons.owned(p)) {
            int card = m.getPersistentData().getInt(Summons.CARD);
            if (card == Card.STONE.ordinal()) {
                m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2, true, true));
                stone = true;
            } else if (card == Card.WRAITH.ordinal()) {
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1, true, true));
            } else {
                m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1, true, true));
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1, true, true));
            }
            SkillFx.burst(p.serverLevel(), m.position().add(0, m.getBbHeight() * 0.5, 0), true, 8, 0.4);
            n++;
        }
        if (stone) {
            for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), 24.0)) a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, true, true));
        }
        if (bird) {
            Vec3 here = p.position();
            List<LivingEntity> seen = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(64.0),
                    e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(here) <= 64.0 * 64.0);
            seen.sort(Comparator.comparingDouble(e -> e.distanceToSqr(here)));
            for (int i = 0; i < seen.size() && i < 48; i++) seen.get(i).addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, true, false));
            n += Math.min(1, seen.size());
        }
        if (grass) {
            for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), 16.0)) a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1, true, true));
            n++;
        }
        if (n > 0) SkillFx.ring(p.serverLevel(), p.position(), 6.0, GOLD);
        return n;
    }

    /** Storage: the Grimoire's page that holds things without limit (here your ender chest: it keeps its contents, away from the world, and is yours alone). */
    static void openStorage(ServerPlayer p) {
        p.openMenu(new SimpleMenuProvider((id, inv, who) -> ChestMenu.threeRows(id, inv, who.getEnderChestInventory()), Component.translatable("container.enderchest")));
    }
}
