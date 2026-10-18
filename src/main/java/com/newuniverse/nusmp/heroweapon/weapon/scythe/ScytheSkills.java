package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the nine techniques of the Vassal Scythe, one small method each, so the whole weapon reads as a list: a full circle (Full Moon Army), a thrown disc that
 * comes back (Flying Circle), a pushing gust (Wind Slice), the finisher that reaps the weak and heals a little (Soul Reap), the thrust that grows with armour
 * (Protection Break), four swings in a row (Deadly Dance), a second scythe at the wielder's side (Floating Scythe), the ultimate that spends the souls
 * (Soul Harvest) and the cut that strips blessings and does no damage (Scythe 0). Every number comes from {@link ScytheNumbers} and {@link ReapRules}, every
 * word from {@link ScytheText}; damage, heals and costs go through {@code HeroCtx} (the balance law), effects through the mod's own effect system, and nothing
 * changes a block or spawns an entity. A technique with nothing to act on refuses (returns false) so it costs nothing, and every technique above 8 raw damage
 * shows a tell first.
 */
final class ScytheSkills {
    private ScytheSkills() {}

    // the palette: spirit violet, pale soul light, the dark violet of a severed blessing
    private static final int SPIRIT = 0xFFB98CFF, SOUL = 0xFFE3D2FF, VOID = 0xFF3A1F52;
    private static final int SPIRIT_RGB = 0xA45BFF;

    static List<HeroSkill> all() {
        List<HeroSkill> l = new ArrayList<>();
        l.add(fullMoonArmy());
        l.add(flyingCircle());
        l.add(windSlice());
        l.add(soulReap());
        l.add(protectionBreak());
        l.add(deadlyDance());
        l.add(floatingScythe());
        l.add(soulHarvest());
        l.add(scytheZero());
        return List.copyOf(l);
    }

    private static SimpleSkill make(String id, SimpleSkill.Body body) {
        Budget.Spec s = ScytheNumbers.spec(id);
        return SimpleSkill.of(id, ScytheText.skillName(id), ScytheText.skillDesc(id), s.cooldown(), s.cost(), body);
    }

    // ------------------------------------------------------------------ shared pieces
    /** The most souls this wielder's form can hold. */
    static int soulCap(HeroCtx c) {
        return c.form.has(ScytheNumbers.P_DRAGON_SOUL) ? ScytheNumbers.SOUL_CAP_DRAGON : ScytheNumbers.SOUL_CAP;
    }

    /** Gathers a soul for a kill (when the form has the passive) and tells the wielder. */
    static void gatherSoul(HeroCtx c) {
        if (!c.form.has(ScytheNumbers.P_SOUL_TITHE)) return;
        SoulLedger souls = ScytheState.SOULS.of(c.player.getUUID());
        if (souls.gain(1, soulCap(c), c.level.getGameTime())) c.say("Souls " + souls.souls(c.level.getGameTime()) + "/" + soulCap(c));
    }

    /** Removes every beneficial effect from a foe; returns how many were removed. */
    static int stripBlessings(LivingEntity e) {
        int n = 0;
        for (MobEffectInstance inst : new ArrayList<>(e.getActiveEffects())) {
            if (inst.getEffect().value().isBeneficial() && e.removeEffect(inst.getEffect())) n++;
        }
        return n;
    }

    /** Removes ONE beneficial effect from a foe (the Null Edge); true when one was removed. */
    static boolean stripOne(LivingEntity e) {
        for (MobEffectInstance inst : new ArrayList<>(e.getActiveEffects())) {
            if (inst.getEffect().value().isBeneficial() && e.removeEffect(inst.getEffect())) return true;
        }
        return false;
    }

    private static boolean weak(LivingEntity e) {
        return ReapRules.isWeak(e.getHealth(), e.getMaxHealth(), e instanceof Player, BalanceLaw.isBoss(e));
    }

    // ------------------------------------------------------------------ Full Moon Army
    private static HeroSkill fullMoonArmy() {
        return make(ScytheNumbers.FULL_MOON_ARMY, c -> {
            Vec3 feet = c.player.position();
            List<LivingEntity> foes = GlassKit.foes(c, feet, ScytheNumbers.MOON_RADIUS, ScytheNumbers.MOON_TARGETS);
            if (foes.isEmpty()) {
                c.say("The scythe's circle is empty.");
                return false;
            }
            for (LivingEntity e : foes) if (GlassKit.strike(c, e, ScytheNumbers.MOON_RAW)) GlassKit.push(c, e, feet, ScytheNumbers.MOON_SHOVE);
            c.fx(VfxShape.SLASH_SCYTHE, GlassKit.chest(c.player), GlassKit.chest(c.player).add(c.look()), SPIRIT, 14, (float) ScytheNumbers.MOON_RADIUS);
            c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
            GlassKit.swing(c);
            return true;
        });
    }

    // ------------------------------------------------------------------ Flying Circle
    private static HeroSkill flyingCircle() {
        return make(ScytheNumbers.FLYING_CIRCLE, c -> {
            Vec3 eye = c.eye(), look = c.look();
            Vec3 start = eye.add(look.scale(0.6));
            Vec3 end = GlassKit.clipEnd(c, start, start.add(look.scale(ScytheNumbers.CIRCLE_RANGE)));
            List<LivingEntity> out = GlassKit.foesAlong(c, start, end, ScytheNumbers.CIRCLE_WIDTH, ScytheNumbers.CIRCLE_TARGETS);
            if (out.isEmpty()) {
                c.say("The disc has nothing in its path.");
                return false;
            }
            for (LivingEntity e : out) GlassKit.strike(c, e, ScytheNumbers.CIRCLE_RAW_OUT);
            c.fx(VfxShape.SLASH_WAVE, start, end, SPIRIT, 12, 1.4f);
            c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.5f);
            GlassKit.swing(c);
            GlassKit.later(c, ScytheNumbers.CIRCLE_BACK_AFTER, () -> {
                Vec3 home = GlassKit.chest(c.player);
                for (LivingEntity e : GlassKit.foesAlong(c, end, home, ScytheNumbers.CIRCLE_WIDTH, ScytheNumbers.CIRCLE_TARGETS)) GlassKit.strike(c, e, ScytheNumbers.CIRCLE_RAW_BACK);
                c.fx(VfxShape.SLASH_WAVE, end, home, SOUL, 12, 1.2f);
                c.sound(SoundEvents.TRIDENT_RETURN, 0.8f, 1.4f);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Wind Slice
    private static HeroSkill windSlice() {
        return make(ScytheNumbers.WIND_SLICE, c -> {
            List<LivingEntity> foes = GlassKit.foesInCone(c, ScytheNumbers.SLICE_RANGE, ScytheNumbers.SLICE_HALF_ANGLE, ScytheNumbers.SLICE_TARGETS);
            if (foes.isEmpty()) {
                c.say("The gust has nothing in front of it.");
                return false;
            }
            Vec3 feet = c.player.position(), look = c.look();
            for (LivingEntity e : foes) if (GlassKit.strike(c, e, ScytheNumbers.SLICE_RAW)) GlassKit.push(c, e, feet, ScytheNumbers.SLICE_SHOVE);
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            flat = flat.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
            Vec3 ahead = feet.add(flat.scale(2.2));
            c.fx(VfxShape.WIND_TORNADO, ahead, ahead, SOUL, 18, 1.1f);
            c.fx(VfxShape.WIND_RING, feet, feet.add(0.0, 1.0, 0.0), SPIRIT, 16, 1.0f);
            c.sound(SoundEvents.ENDER_DRAGON_FLAP, 0.9f, 0.9f);
            GlassKit.swing(c);
            return true;
        });
    }

    // ------------------------------------------------------------------ Soul Reap
    private static HeroSkill soulReap() {
        return make(ScytheNumbers.REAP, c -> {
            if (GlassKit.foesInCone(c, ScytheNumbers.REAP_RANGE + 1.5, ScytheNumbers.REAP_HALF_ANGLE + 15.0, 1).isEmpty()) {
                c.say("There is nothing to reap in front of you.");
                return false;
            }
            Vec3 feet = c.player.position();
            // the tell: a violet ring at your feet and soul light gathering on the blade
            GlassKit.ring(c, feet, 3.0, SPIRIT_RGB);
            c.fx(VfxShape.SOUL_VORTEX, feet, feet, SOUL, ScytheNumbers.REAP_DELAY, 2.0f);
            c.sound(SoundEvents.SOUL_ESCAPE.value(), 0.9f, 0.8f);
            GlassKit.swing(c);
            GlassKit.later(c, ScytheNumbers.REAP_DELAY, () -> {
                int reaped = 0;
                for (LivingEntity e : GlassKit.foesInCone(c, ScytheNumbers.REAP_RANGE, ScytheNumbers.REAP_HALF_ANGLE, ScytheNumbers.REAP_TARGETS)) {
                    GlassKit.strike(c, e, ReapRules.reapRaw(weak(e)));
                    if (!e.isAlive()) {
                        reaped++;
                        if (reaped <= 3) c.fx(VfxShape.CORPSE_RAISE, e.position(), e.position().add(0.0, 1.0, 0.0), SOUL, 22, 1.0f);
                    }
                }
                if (reaped > 0) c.heal(c.player, (float) ReapRules.reapHeal(reaped, c.player.getMaxHealth()));
                Vec3 chest = GlassKit.chest(c.player);
                c.fx(VfxShape.SLASH_SCYTHE, chest, chest.add(c.look()), SPIRIT, 14, 4.2f);
                c.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Protection Break
    private static HeroSkill protectionBreak() {
        return make(ScytheNumbers.PROTECTION_BREAK, c -> {
            LivingEntity target = GlassKit.focus(c, ScytheNumbers.BREAK_RANGE);
            if (target == null) {
                c.say("No foe within reach on your crosshair.");
                return false;
            }
            Vec3 centre = target.getBoundingBox().getCenter();
            // the tell: a ring under the foe and spirit light gathering on the blade
            GlassKit.ring(c, target.position(), 1.6, SPIRIT_RGB);
            c.fx(VfxShape.STAR_MAGIC, GlassKit.chest(c.player).add(c.look().scale(0.6)), centre, SOUL, ScytheNumbers.BREAK_DELAY + 2, 0.9f);
            c.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.7f);
            GlassKit.swing(c);
            GlassKit.later(c, ScytheNumbers.BREAK_DELAY, () -> {
                if (!target.isAlive() || target.distanceTo(c.player) > ScytheNumbers.BREAK_RANGE + 2.0) {
                    c.say("The chop missed its mark.");
                    return;
                }
                if (GlassKit.strike(c, target, ReapRules.breakRaw(target.getArmorValue()))) GlassKit.push(c, target, c.player.position(), 0.5);
                c.fx(VfxShape.GRAVITY_IMPACT, target.position(), target.position(), SPIRIT, 16, 1.4f);
                c.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Deadly Dance
    private static HeroSkill deadlyDance() {
        return make(ScytheNumbers.DEADLY_DANCE, c -> {
            if (GlassKit.foesInCone(c, ScytheNumbers.DANCE_RANGE + 1.0, ScytheNumbers.DANCE_HALF_ANGLE, 1).isEmpty()) {
                c.say("There is nobody in front of you to dance with.");
                return false;
            }
            GlassKit.pulses(c, ScytheNumbers.DANCE_SWINGS, ScytheNumbers.DANCE_INTERVAL, i -> {
                Vec3 feet = c.player.position();
                for (LivingEntity e : GlassKit.foesInCone(c, ScytheNumbers.DANCE_RANGE, ScytheNumbers.DANCE_HALF_ANGLE, ScytheNumbers.DANCE_TARGETS)) {
                    if (GlassKit.strike(c, e, ScytheNumbers.DANCE_RAW, true)) GlassKit.push(c, e, feet, ScytheNumbers.DANCE_SHOVE);       // a quick series: every swing lands on a mob
                }
                Vec3 chest = GlassKit.chest(c.player);
                c.fx(VfxShape.SLASH_SCYTHE, chest, chest.add(c.look().yRot((float) (i * Math.PI * 0.5))), i % 2 == 0 ? SPIRIT : SOUL, 8, 2.6f);
                c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.0f + 0.1f * i);
                GlassKit.swing(c);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Floating Scythe
    private static HeroSkill floatingScythe() {
        return make(ScytheNumbers.FLOATING_SCYTHE, c -> {
            UUID id = c.player.getUUID();
            int span = ScytheNumbers.FLOAT_STRIKES * ScytheNumbers.FLOAT_INTERVAL;
            if (ScytheState.FLOATING.active(id, c.level.getGameTime())) {
                c.say("A floating scythe is already out.");
                return false;
            }
            ScytheState.FLOATING.set(id, c.level.getGameTime() + span);
            GlassKit.follow(c, VfxShape.SPIRIT_AURA, SPIRIT, span, 1.0f);
            c.sound(SoundEvents.VEX_CHARGE, 0.9f, 0.7f);
            GlassKit.pulses(c, ScytheNumbers.FLOAT_STRIKES, ScytheNumbers.FLOAT_INTERVAL, i -> {
                List<LivingEntity> near = GlassKit.foes(c, c.player.position(), ScytheNumbers.FLOAT_RANGE, 1);
                if (!near.isEmpty()) {
                    LivingEntity e = near.get(0);
                    Vec3 centre = e.getBoundingBox().getCenter();
                    GlassKit.strike(c, e, ScytheNumbers.FLOAT_RAW);
                    c.fx(VfxShape.SLASH_SCYTHE, centre, centre.add(c.look()), SOUL, 8, 1.5f);
                    c.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);
                }
                if (i == ScytheNumbers.FLOAT_STRIKES - 1) ScytheState.FLOATING.clear(id);
            });
            return true;
        });
    }

    // ------------------------------------------------------------------ Soul Harvest
    private static HeroSkill soulHarvest() {
        return make(ScytheNumbers.SOUL_HARVEST, c -> {
            Vec3 feet = c.player.position();
            if (GlassKit.foes(c, feet, ScytheNumbers.HARVEST_RADIUS, 1).isEmpty()) {
                c.say("There is nothing to harvest within " + (int) ScytheNumbers.HARVEST_RADIUS + " blocks.");
                return false;
            }
            UUID id = c.player.getUUID();
            int held = ScytheState.SOULS.of(id).souls(c.level.getGameTime());
            // the tell: a wide violet ring and the souls spiralling in
            GlassKit.ring(c, feet, ScytheNumbers.HARVEST_RADIUS, SPIRIT_RGB);
            c.fx(VfxShape.SOUL_VORTEX, feet, feet, SPIRIT, ScytheNumbers.HARVEST_DELAY, (float) ScytheNumbers.HARVEST_RADIUS);
            c.sound(SoundEvents.SOUL_ESCAPE.value(), 1.0f, 0.6f);
            c.say(held > 0 ? "Souls gathered: " + held : "No souls gathered; the harvest will be weaker.");
            GlassKit.swing(c);
            GlassKit.later(c, ScytheNumbers.HARVEST_DELAY, () -> {
                List<LivingEntity> foes = GlassKit.foes(c, c.player.position(), ScytheNumbers.HARVEST_RADIUS, ScytheNumbers.HARVEST_TARGETS);
                int souls = foes.isEmpty() ? 0 : ScytheState.SOULS.of(id).spend(c.level.getGameTime());      // the souls are kept when every foe has left the circle
                int cap = soulCap(c);
                int reaped = 0;
                for (LivingEntity e : foes) {
                    GlassKit.strike(c, e, ReapRules.harvestRaw(souls, weak(e), cap));
                    if (!e.isAlive()) {
                        reaped++;
                        if (reaped <= 4) c.fx(VfxShape.CORPSE_RAISE, e.position(), e.position().add(0.0, 1.0, 0.0), SOUL, 24, 1.2f);
                    }
                }
                if (reaped > 0) c.heal(c.player, (float) ReapRules.harvestHeal(reaped, c.player.getMaxHealth()));
                Vec3 chest = GlassKit.chest(c.player);
                c.fx(VfxShape.SLASH_SCYTHE, chest, chest.add(c.look()), SPIRIT, 16, (float) ScytheNumbers.HARVEST_RADIUS);
                c.fx(VfxShape.SLASH_SCYTHE, chest, chest.add(c.look().yRot((float) Math.PI)), SOUL, 16, (float) (ScytheNumbers.HARVEST_RADIUS * 0.8));
                c.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.6f);
                c.sound(SoundEvents.WITHER_SHOOT, 0.5f, 1.4f);
            });
            return true;
        }).asUltimate();
    }

    // ------------------------------------------------------------------ Scythe 0
    private static HeroSkill scytheZero() {
        return make(ScytheNumbers.SCYTHE_ZERO, c -> {
            List<LivingEntity> foes = GlassKit.foesInCone(c, ScytheNumbers.ZERO_RANGE, ScytheNumbers.ZERO_HALF_ANGLE, ScytheNumbers.ZERO_TARGETS);
            List<LivingEntity> touched = new ArrayList<>();
            for (LivingEntity e : foes) if (stripBlessings(e) > 0) touched.add(e);
            if (touched.isEmpty()) {
                c.say("Scythe 0 finds nothing to cut.");
                return false;
            }
            Vec3 eye = c.eye();
            for (int i = 0; i < Math.min(4, touched.size()); i++) c.fx(VfxShape.ANTI_MAGIC_SLASH, eye, touched.get(i).getBoundingBox().getCenter(), VOID, 12, 1.2f);
            c.sound(SoundEvents.GRINDSTONE_USE, 0.8f, 0.6f);
            c.say("Scythe 0 cut the blessings from " + touched.size() + (touched.size() == 1 ? " foe." : " foes."));
            GlassKit.swing(c);
            return true;
        });
    }
}
