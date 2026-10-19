package com.newuniverse.nusmp.skilltree.hk;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import com.newuniverse.nusmp.skilltree.kit.TechniqueSkill;
import com.newuniverse.nusmp.skilltree.kit.KitWorld;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the twelve techniques of the four Heavy Knight routes are small and share one shape (read my numbers from the route file at my node level, pay my share of the magicules, do one clear thing,
 * start the cooldown of my table), so they are nested classes of one file on top of {@link TechniqueSkill}. A technique writes only {@code cast}; since 1.96.0 it is run as a MODE of the book's unique skill
 * ({@code UniqueBooks.useArt} calls {@code castDirect}), which decides the level, the cooldown and the awakening from its mastery. Each does what its name says with ordinary game rules and no particle spam: the Provoke turns hostile creatures onto the caster with the creature's own target field (the
 * same thing a hit does) and steels the caster; the Shield Wall is the stock Resistance and Absorption effects, shorter than the cooldown so it can never be kept up for ever; the Shield Bash, Power
 * Strike and Crescent Cut hit through the balance law ({@code AbilityKit.hit}, so the damage caps hold); the Swift Step is a short dash with a breath of Resistance; the Gambler's Coin and the Jackpot are
 * the Lady Luck gamble, where the caster's Luck attribute tilts the odds (so the passive Luck of the route matters to its own techniques) and nothing worse than a missed buff can happen.
 */
public final class HkTechniques {
    private HkTechniques() {}

    abstract static class Base extends TechniqueSkill {
        Base(String route, String node, String texture) { super(route, node, texture, HkRoute::fallback); }

        final ServerLevel lvl(ServerPlayer p) { return p.serverLevel(); }
    }

    // ================================================================ Oath of the Heavy Armor
    public static final class Provoke extends Base {
        public Provoke() { super(HkRoute.OATH, HkRoute.PROVOKE, "item/goat_horn"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double radius = num("radius", lv);
            int cap = whole("targets", lv), ticks = Math.max(20, whole("resist_seconds", lv) * 20);
            if (!pay(p, lv)) return false;
            List<LivingEntity> foes = KitWorld.nearest(KitWorld.hostile(p, AbilityKit.around(p, p.position(), radius)), p.position(), cap);
            for (LivingEntity foe : foes) if (foe instanceof Mob m) m.setTarget(p);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0, true, false, true));
            AbilityKit.fx(lvl(p), VfxShape.WIND_RING, p.position(), p.position(), HkRoute.STEEL, 30, (float) Math.min(2.0, radius / 6.0));
            AbilityKit.sound(p, SoundEvents.RAVAGER_ROAR, 0.7f, 0.8f);
            return true;
        }
    }

    public static final class ShieldWall extends Base {
        public ShieldWall() { super(HkRoute.OATH, HkRoute.SHIELD_WALL, "item/iron_chestplate"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            int ticks = Math.max(20, whole("seconds", lv) * 20), amp = Math.max(0, whole("resist_amp", lv));
            int absorbAmp = Math.max(0, (int) Math.round(num("absorb_hearts", lv) / 2.0) - 1);
            if (!pay(p, lv)) return false;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, amp, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, absorbAmp, true, false, true));
            AbilityKit.fx(lvl(p), VfxShape.MIRROR_PANE, p.position().add(0, 1.0, 0), p.position().add(0, 1.0, 0).add(p.getViewVector(1f)), HkRoute.STEEL, 40, 1.2f);
            AbilityKit.sound(p, SoundEvents.ANVIL_PLACE, 0.6f, 0.9f);
            return true;
        }
    }

    public static final class ShieldBash extends Base {
        public ShieldBash() { super(HkRoute.OATH, HkRoute.SHIELD_BASH, "item/iron_ingot"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double range = num("range", lv), half = num("half_angle", lv), push = num("knockback", lv);
            int cap = whole("targets", lv), slow = Math.max(20, whole("slow_seconds", lv) * 20);
            float raw = (float) num("damage", lv);
            if (!pay(p, lv)) return false;
            double mastery = KitWorld.masteryFrac(instance);
            for (LivingEntity foe : KitWorld.nearest(KitWorld.hostile(p, AbilityKit.cone(p, range, half)), p.getEyePosition(), cap)) {
                AbilityKit.hit(p, foe, raw, mastery);
                KitWorld.shove(foe, p, push);
                foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1, false, true, true));
            }
            Vec3 at = p.getEyePosition().add(p.getViewVector(1f).scale(1.2));
            AbilityKit.fx(lvl(p), VfxShape.WIND_RING, at, at, HkRoute.STEEL, 14, 0.8f);
            AbilityKit.sound(p, SoundEvents.SHIELD_BLOCK, 0.9f, 0.7f);
            return true;
        }
    }

    // ================================================================ Basic Swordsmanship
    public static final class PowerStrike extends Base {
        public PowerStrike() { super(HkRoute.SWORD, HkRoute.POWER_STRIKE, "item/iron_sword"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double reach = num("reach", lv), push = num("knockback", lv);
            float raw = (float) num("damage", lv);
            List<LivingEntity> foes = KitWorld.nearest(KitWorld.hostile(p, AbilityKit.cone(p, reach, 25)), p.getEyePosition(), 1);
            if (foes.isEmpty()) {
                AbilityKit.say(p, "No foe in reach.");
                return false;
            }
            if (!pay(p, lv)) return false;
            LivingEntity foe = foes.get(0);
            AbilityKit.hit(p, foe, raw, KitWorld.masteryFrac(instance));
            KitWorld.shove(foe, p, push);
            AbilityKit.fx(lvl(p), VfxShape.WIND_SLASH, p.getEyePosition(), KitWorld.center(foe), HkRoute.BLADE, 10, 0.9f);
            AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.9f);
            return true;
        }
    }

    public static final class SwiftStep extends Base {
        public SwiftStep() { super(HkRoute.SWORD, HkRoute.SWIFT_STEP, "item/feather"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double distance = num("distance", lv);
            int resist = Math.max(10, whole("resist_ticks", lv));
            if (!pay(p, lv)) return false;
            Vec3 dir = KitWorld.flatLook(p);
            double speed = distance * 0.27;                       // the ground's friction eats most of a velocity: this carries about {@code distance} blocks
            p.setDeltaMovement(dir.x * speed, 0.12, dir.z * speed);
            p.hurtMarked = true;
            p.fallDistance = 0f;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, resist, 1, true, false, true));
            Vec3 from = p.position().add(0, 1.0, 0);
            AbilityKit.fx(lvl(p), VfxShape.WIND_SLASH, from, from.add(dir.scale(distance)), HkRoute.BLADE, 10, 0.8f);
            AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
            return true;
        }
    }

    public static final class CrescentCut extends Base {
        public CrescentCut() { super(HkRoute.SWORD, HkRoute.CRESCENT_CUT, "item/diamond_sword"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double range = num("range", lv), half = num("half_angle", lv), push = num("knockback", lv);
            int cap = whole("targets", lv);
            float raw = (float) num("damage", lv);
            if (!pay(p, lv)) return false;
            double mastery = KitWorld.masteryFrac(instance);
            for (LivingEntity foe : KitWorld.nearest(KitWorld.hostile(p, AbilityKit.cone(p, range, half)), p.getEyePosition(), cap)) {
                AbilityKit.hit(p, foe, raw, mastery);
                KitWorld.shove(foe, p, push);
            }
            Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
            AbilityKit.fx(lvl(p), VfxShape.WIND_SLASH, eye.add(look.scale(0.5)), eye.add(look.scale(range)), HkRoute.BLADE, 14, 1.4f);
            AbilityKit.sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
            return true;
        }
    }

    // ================================================================ Lady Luck
    public static final class GamblersCoin extends Base {
        public GamblersCoin() { super(HkRoute.LUCK, HkRoute.GAMBLERS_COIN, "item/gold_nugget"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            int ticks = Math.max(20, whole("seconds", lv) * 20);
            double chance = Math.min(0.9, num("heads_chance", lv) + 0.02 * Math.max(0.0, p.getAttributeValue(Attributes.LUCK)));
            if (!pay(p, lv)) return false;
            boolean heads = p.getRandom().nextDouble() < chance;
            if (heads) {
                p.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks, 1, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, true, false, true));
                AbilityKit.say(p, "Heads. Lady Luck smiles.");
            } else {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.max(40, ticks / 3), 0, true, false, true));
                AbilityKit.say(p, "Tails. A lesson, then.");
            }
            AbilityKit.fx(lvl(p), VfxShape.SPIRIT_AURA, p.position(), p.position().add(0, 1.0, 0), heads ? HkRoute.GOLD : HkRoute.STEEL, 30, 0.8f);
            AbilityKit.sound(p, heads ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, 0.8f, heads ? 1.4f : 1.0f);
            return true;
        }
    }

    public static final class Jackpot extends Base {
        public Jackpot() { super(HkRoute.LUCK, HkRoute.JACKPOT, "item/emerald"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            int emeralds = whole("emeralds", lv), xp = whole("xp", lv);
            if (!pay(p, lv)) return false;
            int luck = (int) Math.max(0.0, p.getAttributeValue(Attributes.LUCK));
            int count = emeralds + p.getRandom().nextInt(1 + luck / 2 + 1);
            p.spawnAtLocation(new ItemStack(Items.EMERALD, count));
            ExperienceOrb.award(lvl(p), p.position(), xp + luck * 5);
            AbilityKit.fx(lvl(p), VfxShape.SPIRIT_AURA, p.position(), p.position().add(0, 1.0, 0), HkRoute.GOLD, 60, 1.4f);
            AbilityKit.sound(p, SoundEvents.PLAYER_LEVELUP, 0.9f, 1.2f);
            AbilityKit.say(p, "JACKPOT!");
            return true;
        }
    }

    // ================================================================ Smoldering Fang of Insanity
    public static final class EmberBite extends Base {
        public EmberBite() { super(HkRoute.FANG, HkRoute.EMBER_BITE, "item/bone"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double range = num("range", lv);
            float raw = (float) num("damage", lv);
            List<LivingEntity> foes = KitWorld.nearest(KitWorld.hostile(p, AbilityKit.cone(p, range, 30)), p.getEyePosition(), 1);
            if (foes.isEmpty()) {
                AbilityKit.say(p, "No foe in reach.");
                return false;
            }
            if (!pay(p, lv)) return false;
            LivingEntity foe = foes.get(0);
            AbilityKit.hit(p, foe, raw, KitWorld.masteryFrac(instance));
            foe.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.max(20, whole("wither_seconds", lv) * 20), 0, false, true, true));
            AbilityKit.heal(p, (float) (raw * num("life_steal", lv)));
            AbilityKit.fx(lvl(p), VfxShape.WIND_SLASH, p.getEyePosition(), KitWorld.center(foe), HkRoute.EMBER, 10, 0.8f);
            AbilityKit.sound(p, SoundEvents.WOLF_GROWL, 0.9f, 1.2f);
            return true;
        }
    }

    public static final class Insanity extends Base {
        public Insanity() { super(HkRoute.FANG, HkRoute.INSANITY, "item/fermented_spider_eye"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            int ticks = Math.max(40, whole("seconds", lv) * 20);
            if (!pay(p, lv)) return false;
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, Math.max(0, whole("strength_amp", lv)), true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, Math.max(0, whole("speed_amp", lv)), true, false, true));
            p.getPersistentData().putLong(HkPassives.MAD_UNTIL, p.serverLevel().getGameTime() + ticks);
            AbilityKit.fx(lvl(p), VfxShape.SPIRIT_AURA, p.position(), p.position().add(0, 1.0, 0), HkRoute.EMBER, 40, 1.2f);
            AbilityKit.sound(p, SoundEvents.WOLF_HOWL, 0.8f, 0.7f);
            AbilityKit.say(p, "The fang is smouldering. You stop guarding.");
            return true;
        }
    }

    public static final class HowlOfRuin extends Base {
        public HowlOfRuin() { super(HkRoute.FANG, HkRoute.HOWL_OF_RUIN, "item/goat_horn"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double radius = num("radius", lv), push = num("knockback", lv);
            int cap = whole("targets", lv), weak = Math.max(20, whole("weak_seconds", lv) * 20), slow = Math.max(20, whole("slow_seconds", lv) * 20);
            if (!pay(p, lv)) return false;
            for (LivingEntity foe : KitWorld.nearest(KitWorld.hostile(p, AbilityKit.around(p, p.position(), radius)), p.position(), cap)) {
                foe.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, weak, 1, false, true, true));
                foe.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1, false, true, true));
                KitWorld.shove(foe, p, push);
            }
            AbilityKit.fx(lvl(p), VfxShape.WIND_RING, p.position(), p.position(), HkRoute.ASH, 30, (float) Math.min(2.2, radius / 5.0));
            AbilityKit.sound(p, SoundEvents.WOLF_HOWL, 1.0f, 0.5f);
            return true;
        }
    }

    public static final class FangOfInsanity extends Base {
        public FangOfInsanity() { super(HkRoute.FANG, HkRoute.FANG_OF_INSANITY, "item/wither_skeleton_skull"); }

        @Override
        protected boolean cast(ServerPlayer p, ManasSkillInstance instance, int lv, int heldTicks, int mode) {
            double reach = num("reach", lv);
            float raw = (float) num("damage", lv);
            List<LivingEntity> foes = KitWorld.nearest(KitWorld.hostile(p, AbilityKit.cone(p, reach, 35)), p.getEyePosition(), 1);
            if (foes.isEmpty()) {
                AbilityKit.say(p, "No foe in reach.");
                return false;
            }
            if (!pay(p, lv)) return false;
            LivingEntity foe = foes.get(0);
            Vec3 toward = KitWorld.center(foe).subtract(p.position());
            Vec3 dir = new Vec3(toward.x, 0, toward.z).normalize();
            p.setDeltaMovement(dir.x * 1.1, 0.25, dir.z * 1.1);                    // the leap that closes the distance
            p.hurtMarked = true;
            p.fallDistance = 0f;
            AbilityKit.hit(p, foe, raw, KitWorld.masteryFrac(instance));
            foe.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.max(20, whole("wither_seconds", lv) * 20), 1, false, true, true));
            AbilityKit.heal(p, (float) (raw * num("heal_pct", lv)));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.max(20, whole("exhaust_seconds", lv) * 20), 0, true, false, true));
            AbilityKit.fx(lvl(p), VfxShape.WIND_SLASH, p.getEyePosition(), KitWorld.center(foe), HkRoute.EMBER, 16, 1.6f);
            AbilityKit.fx(lvl(p), VfxShape.SPIRIT_AURA, KitWorld.center(foe), KitWorld.center(foe).add(0, 0.8, 0), HkRoute.ASH, 40, 1.2f);
            AbilityKit.sound(p, SoundEvents.WOLF_GROWL, 1.0f, 0.6f);
            return true;
        }
    }
}
