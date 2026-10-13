package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Wave 3, the Alchemist's Belt: six vials (red healing, blue shield, green regeneration, yellow enlightening, purple love, black undeath), each
 * once a day (the engine counts the uses on the belt; they return at dawn, which is the "leave it in contact with the belt for eight hours" of the
 * card, since the recipe ingredients have no meaning here).
 */
final class Wave3Belt {
    private Wave3Belt() {}

    private static final String K_UNDEAD = "undead_until", K_FORT = "undead_spent";
    private static final int UNDEAD_TICKS = 3600;

    private static void puff(ServerPlayer p, int rgb) {
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(Wave3Util.dust(rgb, 1.4f), p.getX(), p.getY() + 1.0, p.getZ(), 18, 0.4, 0.6, 0.4, 0.04);
        sl.playSound(null, p.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0f, 1.2f);
    }

    /** Healing Potion (red): ten health at once (the balance law may cut it if you were healed a moment ago). */
    static void red(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.heal(p, 10f);
        puff(p, 0xE0343A);
    }

    /** Shield Potion (blue): about eight points of absorption for three minutes. */
    static void blue(ActCtx c) {
        ServerPlayer p = c.player();
        Fx.shield(p, 8f, 3600);
        puff(p, 0x3A78E8);
    }

    /**
     * Regeneration Potion (green): in direct sunlight, Regeneration II for a minute; elsewhere a lesser Regeneration I. Where Tensura has it, Self
     * Regeneration is lent for three minutes: that is the "lost limbs grow back".
     */
    static void green(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        boolean sun = sl.isDay() && sl.canSeeSky(p.blockPosition());
        Wave3Util.give(p, sun ? 1200 : 400, sun ? 1 : 0, "minecraft:regeneration");
        boolean limbs = Fx.lendSkill(p, "tensura:self_regeneration", 3600);
        puff(p, 0x3CC85A);
        Wave3Util.tell(p, sun ? "The sun feeds the potion." + (limbs ? " Lost limbs will grow back." : "") : "Without the sun the potion is weak." + (limbs ? " Lost limbs will grow back." : ""), ChatFormatting.GREEN);
    }

    /** Enlightening Potion (yellow): the Crusader's Mantle: you and your friends within ten blocks fight with a bright strength for three minutes. */
    static void yellow(ActCtx c) {
        ServerPlayer p = c.player();
        for (LivingEntity e : Wave3Util.friendsAround(p, 10.0)) {
            Wave3Util.give(e, 3600, 0, "minecraft:strength");
            Wave3Util.give(e, 200, 0, "minecraft:glowing");
        }
        puff(p, 0xFFC83C);
    }

    /** Love Potion (purple): the creature in front of you (within six blocks, not a player, not a boss, not too far above you in existence) falls for you for ninety seconds and will not fight you; strike it and the spell breaks. */
    static void purple(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel sl = p.serverLevel();
        Wave3Util.Aim a = Wave3Util.aim(p, 6.0);
        if (a.hit() == null || !Wave3Util.bends(p, a.hit())) {
            Wave3Util.tell(p, "Love Potion: no creature in reach that the potion can touch; the vial is wasted.", ChatFormatting.GRAY);
            return;
        }
        Mob m = (Mob) a.hit();
        if (m instanceof Animal an) an.setInLove(p);
        Wave3Field.charm(m, p, 1800, false);
        sl.sendParticles(ParticleTypes.HEART, m.getX(), m.getEyeY() + 0.3, m.getZ(), 10, 0.4, 0.3, 0.4, 0.02);
        puff(p, 0xA050E0);
    }

    /**
     * Undeath Potion (black): for three minutes you need no air or food, poison means nothing to you (Tensura's Poison Resistance is lent), and the
     * first blow that would kill you leaves you on one health point (Undead Fortitude).
     */
    static void black(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave3Util.data(p);
        d.putLong(K_UNDEAD, p.serverLevel().getGameTime() + UNDEAD_TICKS);
        d.putBoolean(K_FORT, false);
        Wave3Util.giveQuiet(p, UNDEAD_TICKS, 0, "minecraft:water_breathing");
        Wave3Util.remove(p, "minecraft:poison");
        Fx.lendSkill(p, "tensura:poison_resistance", UNDEAD_TICKS);
        p.serverLevel().sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.4, 0.7, 0.4, 0.04);
        puff(p, 0x2A2230);
        Wave3Util.tell(p, "Undeath: you need neither air nor food for three minutes, and death may pass you by once.", ChatFormatting.DARK_PURPLE);
    }

    static boolean undead(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        return d != null && d.getLong(K_UNDEAD) > p.serverLevel().getGameTime();
    }

    /** Once a second: while the black potion holds, hunger does not drop under fourteen; when it ends, it is said. */
    static void pass(ServerPlayer p) {
        CompoundTag d = Wave3Util.peek(p);
        if (d == null) return;
        long until = d.getLong(K_UNDEAD);
        if (until <= 0) return;
        if (p.serverLevel().getGameTime() >= until) {
            d.putLong(K_UNDEAD, 0);
            Wave3Util.tell(p, "The undeath potion wears off.", ChatFormatting.GRAY);
            return;
        }
        FoodData food = p.getFoodData();
        if (food.getFoodLevel() < 14) food.setFoodLevel(14);
    }

    /** Undead Fortitude: the first killing blow during the potion is cut to leave one health point. */
    static void fortitude(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0f || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        CompoundTag d = Wave3Util.peek(p);
        if (d == null || d.getLong(K_UNDEAD) <= p.serverLevel().getGameTime() || d.getBoolean(K_FORT)) return;
        float cut = Wave3Rules.fortitudeCut(e.getAmount(), p.getHealth() + p.getAbsorptionAmount(), false);
        if (cut >= e.getAmount()) return;
        e.setAmount(cut);
        d.putBoolean(K_FORT, true);
        Vec3 at = p.position();
        p.serverLevel().sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 24, 0.4, 0.7, 0.4, 0.06);
        Wave3Util.say(p, "Undead Fortitude: the blow should have killed you; you stay on your feet.");
    }
}
