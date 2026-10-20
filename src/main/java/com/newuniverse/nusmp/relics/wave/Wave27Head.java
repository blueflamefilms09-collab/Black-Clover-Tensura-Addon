package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicFlags;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: the four headpieces of wave 27 (Soul Searchers, the Mad King's Crown, the Orb of Ancient Sight, the Trickster Mask) as Minecraft mechanics.
 * One static method per ability; the once-a-second upkeep is {@link #pass} (wearers only) and {@link #clocks} (always, so a mask taken off is
 * ended). Everything is kept in the player's persistent data: nothing here survives a world change in memory.
 */
final class Wave27Head {
    private Wave27Head() {}

    // ================================================================ Soul Searchers
    /**
     * Sense Alignment: look at a creature within sixteen blocks and read its true alignment from its soul. Usable once every seven days, never twice
     * on the same creature; the one read is told it was read. A soul far deeper than yours (twice your existence) slips past the glass but the week
     * is spent all the same.
     */
    static void soulSearch(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave27Util.data(p);
        long now = p.level().getGameTime();
        long ready = d.getLong("soul_ready");
        if (!Wave27Rules.weekReady(ready, now)) {
            long days = Math.max(1, (ready - now + Wave27Rules.DAY - 1) / Wave27Rules.DAY);
            Wave27Util.tell(p, "The stained glass is dark for " + days + " more day" + (days == 1 ? "" : "s") + ".", ChatFormatting.GRAY);
            return;
        }
        Wave27Util.Aim a = Wave27Util.aim(p, 16.0, e -> e != p && e.isAlive() && !e.isSpectator() && !(e instanceof Player o && o.isCreative()));
        LivingEntity t = a.hit();
        if (t == null) {
            Wave27Util.tell(p, "Look at someone within sixteen blocks.", ChatFormatting.GRAY);
            return;
        }
        ListTag seen = d.getList("soul_seen", Tag.TAG_STRING);
        String id = t.getUUID().toString();
        for (int i = 0; i < seen.size(); i++) {
            if (id.equals(seen.getString(i))) {
                Wave27Util.tell(p, "You have already looked into that soul.", ChatFormatting.GRAY);
                return;
            }
        }
        seen.add(StringTag.valueOf(id));
        while (seen.size() > Wave27Rules.MAX_READ) seen.remove(0);
        d.put("soul_seen", seen);
        d.putLong("soul_ready", now + Wave27Rules.WEEK);
        ServerLevel lv = p.serverLevel();
        String name = t.getName().getString();
        if (Wave27Util.saves(p, t)) {
            Wave27Util.say(p, "The glass swims and shows you nothing: the soul of " + name + " is far too deep to read.", ChatFormatting.LIGHT_PURPLE);
        } else {
            int kills = t instanceof ServerPlayer sp ? sp.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS)) : 0;
            boolean cursed = Wave27Util.amp(t, "tensura:curse") >= 0;
            boolean tame = t instanceof net.minecraft.world.entity.OwnableEntity o && o.getOwnerUUID() != null || t instanceof Villager
                    || t instanceof net.minecraft.world.entity.animal.IronGolem;
            boolean undead = t.getType().is(EntityTypeTags.UNDEAD);
            boolean monster = t instanceof Enemy;
            Wave27Rules.Alignment al = Wave27Rules.alignment(t instanceof Player, tame, undead, monster, kills, cursed);
            Wave27Util.say(p, name + " reads as: " + al.label() + ".", ChatFormatting.LIGHT_PURPLE);
        }
        if (t instanceof ServerPlayer o) Wave27Util.say(o, p.getName().getString() + " looked into your soul through stained glass.", ChatFormatting.LIGHT_PURPLE);
        else if (t instanceof Mob m) m.getLookControl().setLookAt(p);
        Wave27Util.puff(lv, FxTone.LIGHT, t, 10);
    }

    // ================================================================ The Mad King's Crown
    /** Once a second for a wearer: the wear counter grows; past two days the nightmares and the weakness set in, past six the exhaustion is heavy. */
    static void crownPass(ServerPlayer p) {
        CompoundTag d = Wave27Util.data(p);
        long now = p.level().getGameTime();
        long worn = Wave27Rules.wornAfter(d.getLong("mad_worn"), d.getLong("mad_last"), now);
        d.putLong("mad_worn", worn);
        d.putLong("mad_last", now);
        int ex = Wave27Rules.exhaustion(worn);
        if (ex >= 1) Wave27Util.giveQuiet(p, 100, 0, "minecraft:weakness");
        if (ex >= 2) Wave27Util.giveQuiet(p, 100, 0, "minecraft:mining_fatigue");
        if (ex >= 1 && p.getRandom().nextInt(90) == 0) Wave27Util.tell(p, "A throne, red and wet. It is yours. It was always yours.", ChatFormatting.DARK_RED);
        if (p.tickCount % 80 < 20) com.newuniverse.nusmp.particle.fx.Fx.aura(p.serverLevel(), FxThemes.RELIC_WAVE_27, p, FxTone.DARK);
    }

    /** Waking in the crown after two days of wearing it: insane nightmares of power, ruling and blood (a short nausea and dark, never harm). */
    static void crownWake(ServerPlayer p) {
        CompoundTag d = Wave27Util.peek(p);
        if (d == null || d.getLong("mad_worn") < Wave27Rules.NIGHTMARE_AFTER) return;
        Wave27Util.give(p, 200, 0, "minecraft:nausea");
        Wave27Util.give(p, 120, 0, "minecraft:darkness");
        Wave27Util.say(p, "You dreamed of power, of ruling, and of blood. You woke wearing the crown.", ChatFormatting.DARK_RED);
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_27, p, FxTone.DARK, 10);
    }

    // ================================================================ The Orb of Ancient Sight
    /**
     * Ancient Sight (once a day): every creature with a magical effect within twenty-four blocks (120 feet) glows for twenty seconds, you read what
     * effect each carries, and Tensura's magic sense is lent to you for the same time. Other players are listed but not made to glow.
     */
    static void ancientSight(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel lv = p.serverLevel();
        Fx.lendSkill(p, "tensura:magic_sense", Wave27Rules.ORB_GLOW);
        List<LivingEntity> all = Wave27Util.near(lv, p.position(), Wave27Rules.ORB_RADIUS, e -> e != p && !(e instanceof Player o && o.isCreative()));
        int shown = 0, found = 0;
        for (LivingEntity e : all) {
            if (e.getActiveEffects().isEmpty()) continue;
            found++;
            if (!(e instanceof Player)) Wave27Util.give(e, Wave27Rules.ORB_GLOW, 0, "minecraft:glowing");
            if (shown >= Wave27Rules.ORB_LINES) continue;
            List<String> names = new ArrayList<>();
            for (MobEffectInstance i : e.getActiveEffects()) names.add(i.getEffect().value().getDisplayName().getString() + Wave27Rules.level(i.getAmplifier()));
            Wave27Util.say(p, Wave27Rules.auraLine(e.getName().getString(), names), ChatFormatting.RED);
            shown++;
        }
        if (found == 0) Wave27Util.say(p, "No magic stirs within twenty-four blocks.", ChatFormatting.GRAY);
        else if (found > shown) Wave27Util.say(p, "... and " + (found - shown) + " more auras.", ChatFormatting.GRAY);
        FxRelic.ring(lv, p.position().add(0, 1.0, 0), 3.0, 0xE83A3A);
    }

    // ================================================================ Trickster Mask
    /**
     * Skeleton Guise: for three hours (3000 ticks) everyone who looks at you sees a skeleton: the undead take you for one of their own and do not
     * pick you out, and Tensura's presence concealment hides you from the rest. Once it ends, or the mask comes off, it cannot be used again until
     * the next dawn.
     */
    static void guise(ActCtx c) {
        ServerPlayer p = c.player();
        CompoundTag d = Wave27Util.data(p);
        long now = p.level().getGameTime(), today = Wave27Rules.dayOf(p.level().getDayTime());
        if (now < d.getLong("mask_until")) {
            Wave27Util.tell(p, "You are already a skeleton.", ChatFormatting.GRAY);
            return;
        }
        if (!Wave27Rules.dailyReady(d.getLong("mask_day"), today)) {
            Wave27Util.tell(p, "The mask is spent until the next dawn.", ChatFormatting.GRAY);
            return;
        }
        d.putLong("mask_until", now + Wave27Rules.MASK_TICKS);
        d.putLong("mask_day", Wave27Rules.dailyMark(today));
        Wave27Util.giveQuiet(p, Wave27Rules.MASK_TICKS, 0, "tensura:presence_concealment");
        com.newuniverse.nusmp.particle.fx.Fx.burst(p.serverLevel(), FxThemes.RELIC_WAVE_27, p, FxTone.DARK, 12);
        Wave27Util.say(p, "Everyone who looks at you now sees a full skeleton.", ChatFormatting.AQUA);
    }

    /** Is this undead creature fooled by a masked wearer (it does not pick them as a target unless they just struck it)? */
    static boolean maskHides(Mob m, ServerPlayer p) {
        if (!RelicFlags.has(p, Wave27Rules.F_MASK) || !m.getType().is(EntityTypeTags.UNDEAD) || m.getLastHurtByMob() == p) return false;
        CompoundTag d = Wave27Util.peek(p);
        return d != null && p.level().getGameTime() < d.getLong("mask_until");
    }

    /** Runs for everyone once a second: a guise whose mask has come off ends then (and the dawn lock stays). */
    static void clocks(ServerPlayer p) {
        CompoundTag d = Wave27Util.peek(p);
        if (d == null) return;
        long now = p.level().getGameTime();
        if (now < d.getLong("mask_until") && !RelicFlags.has(p, Wave27Rules.F_MASK)) {
            d.putLong("mask_until", now);
            Wave27Util.effect("tensura:presence_concealment").ifPresent(p::removeEffect);
            Wave27Util.tell(p, "With the mask off, the illusion is gone.", ChatFormatting.GRAY);
        }
    }

    /** Runs once a second for a wearer of any headpiece of this wave. */
    static void pass(ServerPlayer p) {
        if (RelicFlags.has(p, Wave27Rules.F_CROWN)) crownPass(p);
    }
}
