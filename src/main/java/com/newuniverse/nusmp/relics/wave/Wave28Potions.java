package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the potions are the only cards of the wave whose effect plays out over minutes after the item is gone, so their timers and the things they do
 * live together, away from the worn relics.
 * <p>
 * Wave 28, the two potions: Executioner's Bane (survive a beheading for three hours of the day) and the Perculiar Puking Poison (six pukes of
 * something strange). Both are drunk, both keep their state in the player's persistent data and run their clocks from the once-a-second pass.
 */
final class Wave28Potions {
    private Wave28Potions() {}

    // ================================================================ Executioner's Bane
    private static final String H_UNTIL = "head_until", H_SAVE = "head_save_at";

    static boolean beheaded(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        return d != null && p.level().getGameTime() < d.getLong(H_UNTIL);
    }

    /**
     * Drinking it: for three hours of the day (3000 ticks) you survive being beheaded. Here that is: a killing blow leaves you on a single hit point
     * (once every ten seconds, with Resistance V for three seconds; a second killing blow inside the gap is the head being destroyed and goes through).
     * The price: you are slowed and weak, you cannot eat or drink, and if nothing reattaches the head in time (sleeping in a bed, or the Sunheart
     * Medallion's cures) the body gives out and loses half its health. Tensura's pain nullification is lent, the head still feels.
     */
    static void behead(ActCtx c) {
        ServerPlayer p = c.player();
        if (beheaded(p)) {                                           // already without a head: the vial is handed back
            if (!p.isCreative()) c.stack().grow(1);
            Wave28Util.tell(p, "You are already beheaded.", ChatFormatting.GRAY);
            return;
        }
        CompoundTag d = Wave28Util.data(p);
        d.putLong(H_UNTIL, p.level().getGameTime() + Wave28Rules.HEAD_TICKS);
        d.putLong(H_SAVE, 0L);
        Fx.lendSkill(p, "tensura:pain_nullification", Wave28Rules.HEAD_TICKS);
        Wave28Util.give(p, 100, 0, "minecraft:slowness");
        Wave28Util.say(p, "Your head comes away from your body, and you live. For three hours of the day you survive being beheaded: you are slow and weak, "
                + "you cannot eat or drink, and a killing blow will leave you on one hit point (once every ten seconds). Sleep in a bed to put your head back.", ChatFormatting.DARK_RED);
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.7, 0), RukhFx.Alignment.DARK, 12);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.6f, 0.6f);
    }

    /** Before a blow is applied: a killing blow on a beheaded player becomes one that leaves a single hit point. True if it was turned. */
    static boolean saveFromBlow(ServerPlayer p, float health, float damage, net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre e) {
        CompoundTag d = Wave28Util.peek(p);
        long now = p.level().getGameTime();
        if (d == null || now >= d.getLong(H_UNTIL) || !Wave28Rules.beheadedSaved(health, damage, now, d.getLong(H_SAVE))) return false;
        e.setNewDamage(Wave28Rules.beheadedDamage(health, damage, now, d.getLong(H_SAVE)));
        d.putLong(H_SAVE, now + Wave28Rules.HEAD_SAVE_GAP);
        Wave28Util.give(p, Wave28Rules.HEAD_GRACE_TICKS, 4, "minecraft:resistance");
        Wave28Util.tell(p, "The blow takes your head off again, and you hang on by a thread.", ChatFormatting.DARK_RED);
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.5, 0), RukhFx.Alignment.DARK, 8);
        return true;
    }

    /** The head is put back (a night's sleep, or a cure of the Sunheart Medallion): the wound heals and the effect ends. */
    static void reattach(ServerPlayer p) {
        if (!beheaded(p)) return;
        Wave28Util.data(p).putLong(H_UNTIL, 0L);
        Fx.heal(p, 8f);
        Wave28Util.remove(p, "minecraft:slowness", "minecraft:weakness");
        Wave28Util.say(p, "Your head is back on your shoulders and the wound closes.", ChatFormatting.AQUA);
        Wave28Util.burst(p.serverLevel(), p.position().add(0, 1.7, 0), RukhFx.Alignment.LIGHT, 8);
    }

    /** Once a second: keep the beheaded state's weakness, and when the three hours end unmended let the body give out (never to the last hit point). */
    static void headPass(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        if (d == null || d.getLong(H_UNTIL) <= 0L) return;
        long now = p.level().getGameTime();
        if (now < d.getLong(H_UNTIL)) {
            Wave28Util.giveQuiet(p, 60, 0, "minecraft:slowness");
            Wave28Util.giveQuiet(p, 60, 0, "minecraft:weakness");
            Fx.lendSkill(p, "tensura:pain_nullification", 100);
            return;
        }
        d.putLong(H_UNTIL, 0L);
        float hit = Wave28Rules.collapseDamage(p.getHealth());
        if (hit > 0f) p.hurt(p.damageSources().magic(), hit);
        Wave28Util.give(p, 400, 2, "minecraft:slowness");
        Wave28Util.give(p, 600, 1, "minecraft:weakness");
        Wave28Util.give(p, 300, 0, "minecraft:hunger");
        Wave28Util.say(p, "The three hours are over and the head was not mended: your body gives out.", ChatFormatting.DARK_RED);
    }

    /** A new body after death does not carry the state over. */
    static void clear(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        if (d == null) return;
        d.putLong(H_UNTIL, 0L);
        d.putInt(P_LEFT, 0);
    }

    // ================================================================ Perculiar Puking Poison
    private static final String P_LEFT = "puke_left", P_NEXT = "puke_next";

    private static final String[] PUKE_NAMES = {
            "luminous rainbow slime", "a broom", "several large rocks", "acid", "a live fish", "bubbles", "a book", "blood", "some carrots", "a ship's anchor",
            "live ducklings", "a well-crafted chair", "necrotic mush", "a fist-sized ball of glass", "a large cactus", "a swarm of bugs", "deer antlers",
            "a copy of your own head", "electrical plasma", "a full size coffin"};

    /**
     * Drinking it: a Constitution save (a d20 plus one for every four points of health above twenty, against 15). Fail it and you are poisoned for an
     * hour of the day (1000 ticks: weak and queasy), and about every ten minutes of it (167 ticks) you puke up one of twenty strange things, six times.
     */
    static void drink(ActCtx c) {
        ServerPlayer p = c.player();
        int roll = p.getRandom().nextInt(20);
        if (!Wave28Rules.failsSave(roll, Wave28Rules.conMod(p.getMaxHealth()))) {
            Wave28Util.say(p, "You keep it down: your constitution shrugs the poison off (rolled " + (roll + 1) + ").", ChatFormatting.GREEN);
            return;
        }
        CompoundTag d = Wave28Util.data(p);
        d.putInt(P_LEFT, Wave28Rules.PUKE_COUNT);
        d.putLong(P_NEXT, p.level().getGameTime() + Wave28Rules.PUKE_GAP);
        Wave28Util.give(p, Wave28Rules.PUKE_COUNT * Wave28Rules.PUKE_GAP, 0, "minecraft:weakness");
        Wave28Util.give(p, 100, 0, "minecraft:nausea");
        Wave28Util.say(p, "You are poisoned (rolled " + (roll + 1) + " against 15). Something is going to come up.", ChatFormatting.DARK_GREEN);
    }

    /** Once a second: when the next puke is due, one of the twenty things comes up. */
    static void pukePass(ServerPlayer p) {
        CompoundTag d = Wave28Util.peek(p);
        if (d == null || d.getInt(P_LEFT) <= 0) return;
        long now = p.level().getGameTime();
        if (now < d.getLong(P_NEXT)) return;
        int left = d.getInt(P_LEFT) - 1;
        d.putInt(P_LEFT, left);
        d.putLong(P_NEXT, now + Wave28Rules.PUKE_GAP);
        puke(p, Wave28Rules.puke(p.getRandom().nextInt(20)));
    }

    private static void give(ServerPlayer p, String id, int n) { Wave28Util.spit(p, Wave28Util.stack(id, n)); }

    private static void puke(ServerPlayer p, Wave28Rules.Puke k) {
        ServerLevel l = p.serverLevel();
        Vec3 mouth = p.getEyePosition().add(p.getViewVector(1f).scale(0.4));
        String what = PUKE_NAMES[k.ordinal()];
        switch (k) {
            case RAINBOW_SLIME -> {
                give(p, "minecraft:slime_ball", 3);
                Wave28Util.give(p, 200, 0, "minecraft:glowing");
            }
            case BROOM -> {
                give(p, "minecraft:stick", 1);
                give(p, "minecraft:wheat", 3);
            }
            case ROCKS -> give(p, "minecraft:cobblestone", 4);
            case ACID -> {
                if (p.getHealth() > 6f) p.hurt(p.damageSources().magic(), 2f);
                Wave28Util.give(p, 100, 0, "tensura:corrosion", "minecraft:poison");
            }
            case FISH -> give(p, "minecraft:cod", 1);
            case BUBBLES -> {
                Wave28Util.give(p, 200, 0, "minecraft:water_breathing");
                Wave28Util.give(p, 100, 0, "minecraft:slow_falling");
            }
            case BOOK -> give(p, "minecraft:book", 1);
            case BLOOD -> {
                int n = Wave28Rules.bloodDamage(p.getRandom().nextInt(4));
                if (p.getHealth() > n + 1f) p.hurt(p.damageSources().generic(), n);
            }
            case CARROTS -> give(p, "minecraft:carrot", Wave28Rules.carrots(p.getRandom().nextInt(6)));
            case ANCHOR -> {
                give(p, "minecraft:iron_ingot", 2);
                give(p, "minecraft:chain", 3);
                Wave28Util.give(p, 60, 1, "minecraft:slowness");
            }
            case DUCKLINGS -> {
                for (int i = 0; i < 3; i++) {
                    LivingEntity chick = Fx.summon(p, "minecraft:chicken", 3000);
                    if (chick instanceof AgeableMob a) a.setAge(-24000);
                }
            }
            case CHAIR -> give(p, "minecraft:oak_stairs", 1);
            case NECROTIC_MUSH -> {
                give(p, "minecraft:rotten_flesh", 3);
                Wave28Util.give(p, 200, 0, "minecraft:hunger");
            }
            case GLASS_BALL -> give(p, "minecraft:glass", 1);
            case CACTUS -> give(p, "minecraft:cactus", 2);
            case BUGS -> {
                give(p, "minecraft:spider_eye", 2);
                Wave28Util.give(p, 100, 0, "minecraft:nausea");
            }
            case ANTLERS -> give(p, "minecraft:bone", 2);
            case OWN_HEAD -> {
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                try {
                    head.set(DataComponents.PROFILE, new ResolvableProfile(p.getGameProfile()));
                } catch (RuntimeException | LinkageError plain) {
                    head = new ItemStack(Items.SKELETON_SKULL);
                }
                Wave28Util.spit(p, head);
            }
            case PLASMA -> {
                for (LivingEntity t : Wave28Util.near(l, p.position(), 3.0, e -> Wave28Util.foe(p, e))) Fx.damage(p, t, 4f, "minecraft:lightning_bolt");
                Wave28Util.give(p, 60, 0, "minecraft:slowness");
                Fx.ring(l, p.position(), 3.0, 0xA0D8FF);
            }
            case COFFIN -> {
                give(p, "minecraft:chest", 1);
                give(p, "minecraft:bone", 1);
            }
        }
        Wave28Util.burst(l, mouth, k == Wave28Rules.Puke.BUBBLES || k == Wave28Rules.Puke.RAINBOW_SLIME ? RukhFx.Alignment.LIGHT : RukhFx.Alignment.DARK, 6);
        l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.7f, 0.6f);
        Wave28Util.tell(p, "You puke up " + what + ".", ChatFormatting.DARK_GREEN);
    }
}
