package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.RelicFlags;
import com.newuniverse.nusmp.relics.wave.Wave26Rules.Command;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: Frank is the one relic of the wave with a story that unfolds over days (a curse that follows you as a rabbit, a Foresight that asks
 * you to misbehave, five failures and a visit at night, a countdown only a wish can stop), so its whole state machine and its handlers live in
 * one class. Every number is in {@link Wave26Rules}; the per-player values are kept in the player's persistent data, so a relog, a death or a
 * server restart does not reset a curse. Nothing here can kill: the final curse is a long, dreadful but survivable ordeal.
 */
final class Wave26Frank {
    private Wave26Frank() {}

    private static final String K_UNTIL = "fs_until", K_ROUND = "fs_round", K_CMD = "fs_cmd", K_CMD_AT = "fs_cmd_at", K_NEXT = "fs_next",
            K_FAILS = "fails", K_PENDING = "visit_pending", K_COUNT_DAY = "count_day", K_NOTE_DAY = "note_day", K_LIFTED = "lifted_until",
            K_STILL = "still", K_STILL_POS = "still_pos";
    private static final String QUOTE = "\"Wake up... Come, closer. 28 days, 6 hours, 42 minutes, 12 seconds. That is when your world will end.\"";

    /** The little brown bunny each carrier of an unworn Frank is followed by. */
    private static final Map<UUID, UUID> BUNNIES = new HashMap<>();

    // ================================================================ the act
    /** Foresight (once a day): an hour of seeing the immediate future, on Frank's terms. */
    static void foresight(ActCtx c) {
        ServerPlayer p = c.player();
        long now = p.level().getGameTime();
        CompoundTag d = Wave26Util.data(p);
        d.putLong(K_UNTIL, now + Wave26Rules.FORESIGHT_TICKS);
        d.putInt(K_ROUND, 0);
        d.putLong(K_CMD_AT, 0L);
        d.putLong(K_NEXT, now + 10 * 20);
        Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_26, p, FxTone.LIGHT, 12);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.8f);
        Wave26Util.say(p, "The mask twists into a pale metal bunny. Frank whispers: \"Foresight is yours for an hour. Do as I say, and it stays.\"", ChatFormatting.LIGHT_PURPLE);
    }

    static boolean active(ServerPlayer p) {
        CompoundTag d = Wave26Util.peek(p);
        return d != null && d.getLong(K_UNTIL) > p.level().getGameTime();
    }

    // ================================================================ blows and the curse
    /** The factor on a blow of this player: stronger under Foresight, weaker for a carrier whose mask is off and whose curse is not lifted. */
    static float outFactor(ServerPlayer p) {
        if (!Wave26Wear.carries(p, Wave26Util.C_FRANK)) return 1.0f;
        boolean worn = RelicFlags.has(p, Wave26Rules.F_FRANK);
        CompoundTag d = Wave26Util.peek(p);
        boolean lifted = d != null && d.getLong(K_LIFTED) > p.level().getGameTime();
        return Wave26Rules.outFactor(worn && active(p), !worn && !lifted);
    }

    /** The factor on a blow that lands on this wearer: under Foresight every foe has disadvantage. */
    static float inFactor(ServerPlayer p) {
        return RelicFlags.has(p, Wave26Rules.F_FRANK) && active(p) ? Wave26Rules.FORESIGHT_IN : 1.0f;
    }

    // ================================================================ the clock (once a second for every player)
    static void pass(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        boolean carry = Wave26Wear.carries(p, Wave26Util.C_FRANK);
        CompoundTag d = carry ? Wave26Util.data(p) : Wave26Util.peek(p);
        if (d == null) { dropBunny(sl.getServer(), p.getUUID()); return; }
        boolean worn = RelicFlags.has(p, Wave26Rules.F_FRANK);
        boolean lifted = d.getLong(K_LIFTED) > now;

        // ---- the bunny that follows the carrier of an unworn mask
        if (carry && !worn && !lifted) bunny(p); else dropBunny(sl.getServer(), p.getUUID());

        // ---- Foresight and Frank's commands
        long until = d.getLong(K_UNTIL);
        if (until > 0) {
            if (now >= until) endForesight(p, d, "Foresight ends; the world goes dull again.");
            else if (!worn) endForesight(p, d, "You took the mask off: Foresight ends.");
            else {
                Wave26Util.giveQuiet(p, 80, 0, "tensura:future_vision");
                Wave26Util.giveQuiet(p, 80, 0, "tensura:presence_sense");
                commands(p, d, now);
            }
        }

        // ---- the final curse: the nighttime visit and the countdown
        if (carry) {
            long today = Wave26Util.today(sl);
            long dayTime = sl.getDayTime() % Wave26Rules.DAY;
            if (d.getBoolean(K_PENDING) && dayTime >= 13000) visit(p, d, today);
            if (d.contains(K_COUNT_DAY) && d.getLong(K_NOTE_DAY) != today && dayTime < 4000) countdown(p, d, today);
        }
    }

    private static void endForesight(ServerPlayer p, CompoundTag d, String why) {
        d.putLong(K_UNTIL, 0L);
        d.putLong(K_CMD_AT, 0L);
        Wave26Util.remove(p, "tensura:future_vision", "tensura:presence_sense");
        Wave26Util.tell(p, why, ChatFormatting.LIGHT_PURPLE);
    }

    private static void commands(ServerPlayer p, CompoundTag d, long now) {
        long at = d.getLong(K_CMD_AT);
        if (at == 0L) {
            if (now < d.getLong(K_NEXT)) return;
            Command cmd = Wave26Rules.commandFor(d.getInt(K_ROUND));
            d.putInt(K_CMD, cmd.ordinal());
            d.putLong(K_CMD_AT, now + Wave26Rules.COMMAND_WINDOW);
            d.putInt(K_STILL, 0);
            Wave26Util.say(p, "Frank says: \"" + cmd.text + ". Within three minutes.\"", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        if (Wave26Rules.commandFor(d.getInt(K_ROUND)) == Command.HOLD_STILL) {
            long pos = p.blockPosition().asLong();
            if (p.isShiftKeyDown() && d.getLong(K_STILL_POS) == pos) d.putInt(K_STILL, d.getInt(K_STILL) + 20); else d.putInt(K_STILL, 0);
            d.putLong(K_STILL_POS, pos);
            if (d.getInt(K_STILL) >= Wave26Rules.STILL_TICKS) { done(p, d, now); return; }
        }
        if (now > at) {
            int fails = Wave26Rules.afterFail(d.getInt(K_FAILS));
            d.putInt(K_FAILS, fails);
            endForesight(p, d, "You failed Frank (" + fails + "/" + Wave26Rules.FAILS_FOR_VISIT + "): the spell ends at once.");
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RABBIT_DEATH, SoundSource.PLAYERS, 1.0f, 0.5f);
            if (Wave26Rules.visitEarned(fails)) d.putBoolean(K_PENDING, true);
        }
    }

    private static void done(ServerPlayer p, CompoundTag d, long now) {
        d.putInt(K_ROUND, d.getInt(K_ROUND) + 1);
        d.putLong(K_CMD_AT, 0L);
        d.putLong(K_NEXT, now + Wave26Rules.COMMAND_GAP);
        Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_26, p, FxTone.LIGHT, 8);
        Wave26Util.tell(p, "Frank giggles. \"Good. Very good.\"", ChatFormatting.LIGHT_PURPLE);
    }

    /** The handlers of the three commands: a blow on a peaceful creature, a thrown item. */
    static void onStrike(ServerPlayer p, Entity target) {
        CompoundTag d = Wave26Util.peek(p);
        if (d == null || d.getLong(K_CMD_AT) == 0L || !RelicFlags.has(p, Wave26Rules.F_FRANK)) return;
        if (Wave26Rules.commandFor(d.getInt(K_ROUND)) != Command.STRIKE_PEACEFUL) return;
        if (target instanceof Animal a && !(a instanceof OwnableEntity own && own.getOwnerUUID() != null) && !(a instanceof Enemy)) done(p, d, p.level().getGameTime());
    }

    static void onToss(ServerPlayer p) {
        CompoundTag d = Wave26Util.peek(p);
        if (d == null || d.getLong(K_CMD_AT) == 0L || !RelicFlags.has(p, Wave26Rules.F_FRANK)) return;
        if (Wave26Rules.commandFor(d.getInt(K_ROUND)) == Command.THROW_AWAY) done(p, d, p.level().getGameTime());
    }

    // ================================================================ the final curse
    private static void visit(ServerPlayer p, CompoundTag d, long today) {
        d.putBoolean(K_PENDING, false);
        d.putInt(K_FAILS, 0);
        d.putLong(K_COUNT_DAY, today);
        d.putLong(K_NOTE_DAY, today);
        Wave26Util.give(p, 120, 0, "minecraft:darkness");
        Fx.gather(p.serverLevel(), FxThemes.RELIC_WAVE_26, p, FxTone.DARK, 12);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
        Wave26Util.say(p, "In the night, a figure in a stupid bunny suit sits at the foot of your bed.", ChatFormatting.DARK_RED);
        Wave26Util.say(p, QUOTE, ChatFormatting.DARK_RED);
        Wave26Util.say(p, "(The countdown has begun. Only a wish, an enchanted golden apple, can break it.)", ChatFormatting.GRAY);
    }

    private static void countdown(ServerPlayer p, CompoundTag d, long today) {
        d.putLong(K_NOTE_DAY, today);
        int left = Wave26Rules.daysLeft(d.getLong(K_COUNT_DAY), today);
        if (left > 0) {
            Wave26Util.say(p, "Frank's countdown: " + left + " days.", ChatFormatting.DARK_RED);
            return;
        }
        Wave26Util.say(p, "\"Your world ends.\" The sky goes out for a while... and comes back. The countdown begins again.", ChatFormatting.DARK_RED);
        Wave26Util.give(p, 30 * 20, 0, "minecraft:darkness");
        Wave26Util.give(p, 15 * 20, 0, "minecraft:nausea");
        Wave26Util.give(p, 30 * 20, 1, "minecraft:weakness");
        d.putLong(K_COUNT_DAY, today);
    }

    /** A golden apple is the nearest "remove curse" (the mask's disadvantage is lifted for a day); an enchanted one is the nearest "wish". */
    static void onEat(ServerPlayer p, ItemStack stack) {
        if (!Wave26Wear.carries(p, Wave26Util.C_FRANK)) return;
        CompoundTag d = Wave26Util.data(p);
        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            d.remove(K_COUNT_DAY);
            d.putBoolean(K_PENDING, false);
            d.putInt(K_FAILS, 0);
            d.putLong(K_LIFTED, p.level().getGameTime() + Wave26Rules.REMOVE_CURSE_TICKS * 3L);
            Wave26Util.say(p, "A wish is made. Frank's countdown is broken, and the curse lets go for three days.", ChatFormatting.GOLD);
        } else if (stack.is(Items.GOLDEN_APPLE)) {
            d.putLong(K_LIFTED, p.level().getGameTime() + Wave26Rules.REMOVE_CURSE_TICKS);
            Wave26Util.say(p, "Remove Curse: Frank's hold on your blows is lifted for a day.", ChatFormatting.GOLD);
        }
    }

    // ================================================================ the bunny
    private static void bunny(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        UUID id = BUNNIES.get(p.getUUID());
        Entity e = id == null ? null : sl.getEntity(id);
        if (!(e instanceof Rabbit r) || !r.isAlive()) {
            Rabbit made = EntityType.RABBIT.create(sl);
            if (made == null) return;
            made.moveTo(p.getX() + 1.0, p.getY(), p.getZ() + 1.0, p.getYRot(), 0f);
            made.setVariant(Rabbit.Variant.BROWN);
            made.setCustomName(Component.literal("Frank"));
            made.getPersistentData().putUUID(Wave26Util.OWNER_KEY, p.getUUID());           // a relic ally: nobody's spell targets it
            if (sl.addFreshEntity(made)) BUNNIES.put(p.getUUID(), made.getUUID());
            return;
        }
        double dist = r.distanceTo(p);
        if (dist > 24.0) r.teleportTo(p.getX() + 1.0, p.getY(), p.getZ() + 1.0);
        else if (dist > 3.0) r.getNavigation().moveTo(p, 1.3);
    }

    private static void dropBunny(MinecraftServer server, UUID owner) {
        UUID id = BUNNIES.remove(owner);
        if (id == null || server == null) return;
        for (ServerLevel l : server.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null) { e.discard(); return; }
        }
    }

    static void forget(ServerPlayer p) {
        dropBunny(p.getServer(), p.getUUID());
    }

    static void clearAll(MinecraftServer server) {
        for (UUID owner : new HashSet<>(BUNNIES.keySet())) dropBunny(server, owner);
        BUNNIES.clear();
    }
}
