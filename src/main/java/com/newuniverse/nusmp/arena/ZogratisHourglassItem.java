package com.newuniverse.nusmp.arena;

import com.newuniverse.nusmp.multiverse.Squads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Why: Lucius answers only in his own arena (1.46.0), so the players need a way to be carried there and back that is a deliberate act
 * (hold right click, three seconds, a ring of Rukh light closing round you) and not a door anyone can walk through by accident. The Hourglass of
 * the Final Hour is that act. Outside the arena it takes the holder, and every player within eight blocks (party members first, then the nearest;
 * {@link HourglassRules#pickTravellers}), into the arena; inside the arena the same hold takes them home, and mid-fight that hold lasts ten seconds,
 * so leaving a lost fight is allowed but costs time. It refuses in the Labyrinth and the dungeon dimensions, refuses to enter while the boss is fighting
 * (the fight is closed to latecomers) and while the user was hurt in the last five seconds (it is not an escape from another fight), and puts a 60-second
 * cooldown on itself when it brings someone home. The decisions are {@link HourglassRules}; this class only reads the facts, runs the channel with its
 * particles (the mod's own Rukh light, drawn by the server, nothing client-side) and calls {@link ArenaTravel}. The client cannot know the arena's state, so it
 * always shows the three-second animation; the server decides the real length.
 */
public class ZogratisHourglassItem extends Item {
    public ZogratisHourglassItem(Properties props) { super(props); }

    // ---------------------------------------------------------------- the facts
    private static HourglassRules.Decision decide(Player p) {
        boolean fight = p.getServer() != null && ArenaDirector.fightRunning(p.getServer());
        long sinceHurt = p.getLastHurtByMob() == null ? Long.MAX_VALUE : Math.max(0, p.tickCount - p.getLastHurtByMobTimestamp());
        return HourglassRules.decide(ArenaTravel.placeOf(p.level()), fight, sinceHurt);
    }

    private static String denialText(HourglassRules.Denial d) {
        return switch (d) {
            case FORBIDDEN_DIMENSION -> "The hourglass is silent here: time does not run the same way inside the Labyrinth and the dungeons.";
            case RECENTLY_HURT -> "The sand will not flow while you are in danger. Catch your breath first (five seconds).";
            case FIGHT_IN_PROGRESS -> "The Final Hour is already underway. It cannot be entered until the fight is over.";
            case NONE -> "";
        };
    }

    // ---------------------------------------------------------------- use
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        HourglassRules.Decision d = decide(player);
        if (!d.allowed()) {
            player.displayClientMessage(Component.literal(denialText(d.denial())).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (level instanceof ServerLevel sl) sl.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8f, 1.6f);
        player.displayClientMessage(Component.literal(d.action() == HourglassRules.Action.ENTER
                ? "The sand begins to fall. Hold still for " + d.channelTicks() / 20 + " seconds."
                : "The sand runs back. Hold for " + d.channelTicks() / 20 + " seconds to leave.").withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player p && !p.level().isClientSide) {
            HourglassRules.Decision d = decide(p);
            if (d.allowed()) return d.channelTicks();
        }
        return HourglassRules.ENTER_TICKS;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !(level instanceof ServerLevel sl)) return;
        HourglassRules.Decision d = decide(p);
        if (!d.allowed()) {
            p.stopUsingItem();
            p.displayClientMessage(Component.literal(denialText(d.denial())).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (d.action() == HourglassRules.Action.ENTER && p.hurtTime > 0) {                    // a blow breaks the way in; the way out is not breakable
            p.stopUsingItem();
            p.displayClientMessage(Component.literal("The sand scatters: you were struck.").withStyle(ChatFormatting.RED), true);
            return;
        }
        int total = getUseDuration(stack, p), elapsed = Math.max(0, total - remaining);
        if (elapsed % 4 != 0) return;
        ring(sl, p, 1.0 + 1.8 * (1.0 - elapsed / (double) Math.max(1, total)), elapsed);
        if (elapsed % 20 == 0) {
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, 0.6f + 0.8f * elapsed / Math.max(1, total));
            p.displayClientMessage(Component.literal(((total - elapsed + 19) / 20) + "...").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** A closing ring of Rukh light round the user (the mod's own particles, drawn by the server through the Rukh budget). */
    private static void ring(ServerLevel sl, Player p, double radius, int phase) {
        FxTone light = FxTone.of(false);
        int n = 10;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + phase * 0.12;
            Fx.burst(sl, FxThemes.ARENA_LUCIUS, p.getX() + Math.cos(a) * radius, p.getY() + 0.15 + (phase % 16) * 0.12, p.getZ() + Math.sin(a) * radius, light, 1);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer user)) return stack;
        HourglassRules.Decision d = decide(user);
        if (!d.allowed()) {
            user.displayClientMessage(Component.literal(denialText(d.denial())).withStyle(ChatFormatting.GRAY), true);
            return stack;
        }
        com.newuniverse.nusmp.quests.NusmpEvents.fire(user, "arena.hourglass_used", d.action() == HourglassRules.Action.LEAVE ? "leave" : "enter");
        if (d.action() == HourglassRules.Action.LEAVE) {
            if (ArenaTravel.leave(user)) user.getCooldowns().addCooldown(this, HourglassRules.COOLDOWN_TICKS);
            return stack;
        }
        Set<UUID> party = Squads.partyMembers(user);
        List<HourglassRules.Candidate> near = new ArrayList<>();
        for (ServerPlayer other : ((ServerLevel) level).players()) {
            if (other == user || !other.isAlive() || other.isSpectator()) continue;
            near.add(new HourglassRules.Candidate(other.getUUID(), other.distanceTo(user), party != null && party.contains(other.getUUID()), hostileTo(user, other)));
        }
        List<UUID> chosen = HourglassRules.pickTravellers(near, HourglassRules.COMPANION_RANGE, HourglassRules.MAX_COMPANIONS);
        List<ServerPlayer> companions = new ArrayList<>();
        for (UUID id : chosen) if (((ServerLevel) level).getPlayerByUUID(id) instanceof ServerPlayer sp) companions.add(sp);
        Vec3 at = user.position();
        ServerLevel sl = (ServerLevel) level;
        Fx.burst(sl, FxThemes.ARENA_LUCIUS, at.add(0, 1, 0), FxTone.of(false), 12, 1.2);
        ArenaTravel.enter(user, companions);
        return stack;
    }

    /** Hostile = one of the two was last hurt by the other (a PvP mood). Allies and strangers are not. */
    private static boolean hostileTo(ServerPlayer user, ServerPlayer other) {
        return user.getLastHurtByMob() == other || other.getLastHurtByMob() == user;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof ServerPlayer p) p.displayClientMessage(Component.literal("The sand settles again.").withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nusmp.zogratis_hourglass.tip.kind").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.nusmp.zogratis_hourglass.tip.lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.nusmp.zogratis_hourglass.tip.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nusmp.zogratis_hourglass.tip.rules").withStyle(ChatFormatting.DARK_GRAY));
    }
}
