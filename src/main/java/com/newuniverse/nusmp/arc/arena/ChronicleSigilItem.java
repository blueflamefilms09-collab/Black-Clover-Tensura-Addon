package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.hub.ChronicleSite;
import com.newuniverse.nusmp.arc.world.ArcWorldConfig;
import com.newuniverse.nusmp.arena.HourglassRules;
import com.newuniverse.nusmp.arena.ZogratisArena;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.multiverse.Squads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
 * Why: a story boss answers only in its arena, so the players need a way to be carried there and back that is a deliberate act (hold right click, three seconds,
 * a ring of Rukh light closing round you) and not a door anyone can walk through by accident. The Chronicle Sigil is that act, the Final Hour's Hourglass built
 * for the whole story. At the Chronicle in the Hub (near the lectern) the hold takes the user, and every player within eight blocks (party members first, then the
 * nearest: {@code HourglassRules.pickTravellers}, the Hourglass's own rule), to the arena of the boss the user chose on the lectern (or, if they chose nothing,
 * the next boss of their story); inside an arena the same hold takes them home, and mid-fight that hold lasts ten seconds, so leaving a lost fight is allowed but
 * costs time. Sneaking and clicking cycles the choice without going to the lectern. It refuses away from the Hub and the arenas, refuses to enter while the user
 * was hurt in the last five seconds (it is not an escape from another fight), while the chosen arena is fighting (a fight is closed to latecomers) or resting,
 * and puts a 30-second cooldown on itself when it brings someone home. The decisions are the pure {@link ChronicleSigilRules}; this class only reads the facts,
 * runs the channel with its particles (the mod's own Rukh light, drawn by the server) and calls {@link ArcArenaTravel}. The client cannot know the facts, so it
 * always shows the three-second animation; the server decides the real length.
 */
public class ChronicleSigilItem extends Item {
    public ChronicleSigilItem(Properties props) { super(props); }

    /** How near the lectern (blocks, from the middle of the rotunda) the Chronicle answers. */
    public static final double CHRONICLE_REACH = 9.0;

    // ---------------------------------------------------------------- the facts
    private record Facts(ChronicleSigilRules.Decision decision, ArcCharacter pick) {}

    private static ChronicleSigilRules.Place placeOf(Level level) {
        if (ArcArena.in(level) || ZogratisArena.in(level)) return ChronicleSigilRules.Place.ARENA;
        if (Labyrinth.inSanctuary(level)) return ChronicleSigilRules.Place.HUB;
        return ChronicleSigilRules.Place.ELSEWHERE;
    }

    private static Facts facts(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ChronicleSigilRules.Place place = placeOf(p.level());
        ArcCharacter pick = place == ChronicleSigilRules.Place.HUB ? ChronicleLectern.pickOf(p) : null;
        boolean near = false;
        if (place == ChronicleSigilRules.Place.HUB) {
            double dx = p.getX() - (ChronicleSite.LECTERN_X + 0.5), dy = p.getY() - (ChronicleSite.PAVE_Y + 1), dz = p.getZ() - (ChronicleSite.LECTERN_Z + 0.5);
            near = dx * dx + dz * dz <= CHRONICLE_REACH * CHRONICLE_REACH && Math.abs(dy) <= 8.0;
        }
        ArcArenaDirector.Status st = place == ChronicleSigilRules.Place.HUB ? ArcArenaDirector.status(server, pick) : ArcArenaDirector.Status.IDLE;
        boolean fight = place == ChronicleSigilRules.Place.ARENA ? ArcArenaDirector.fightRunningAt(server, p) : st.fighting();
        long sinceHurt = p.getLastHurtByMob() == null ? Long.MAX_VALUE : Math.max(0, p.tickCount - p.getLastHurtByMobTimestamp());
        return new Facts(ChronicleSigilRules.decide(ArcWorldConfig.arenas(), place, near, fight, sinceHurt, pick != null, st.resting()), pick);
    }

    private static String denialText(ChronicleSigilRules.Denial d) {
        return switch (d) {
            case SWITCHED_OFF -> "The Chronicle is closed on this server.";
            case WRONG_PLACE -> "The sigil is silent here. It answers at the Chronicle in the Sanctuary Hub, and inside an arena.";
            case TOO_FAR -> "Stand at the lectern of the Chronicle, in the rotunda south of the plaza.";
            case RECENTLY_HURT -> "The ink will not run while you are in danger. Catch your breath first (five seconds).";
            case NOTHING_TO_FIGHT -> "The Chronicle has nobody to carry you to: every boss that is open to you is beaten, and replays are off.";
            case FIGHT_IN_PROGRESS -> "That fight is already underway. It cannot be entered until it is over.";
            case RESTING -> "That arena is resting after its last fight. Try again in a few minutes.";
            case NONE -> "";
        };
    }

    // ---------------------------------------------------------------- use
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {                                                       // sneaking: only the choice changes
            if (!level.isClientSide && player instanceof ServerPlayer sp && ArcWorldConfig.arenas()) ChronicleLectern.cycle(sp);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (level.isClientSide) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.fail(stack);
        Facts f = facts(sp);
        ChronicleSigilRules.Decision d = f.decision();
        if (!d.allowed()) {
            player.displayClientMessage(Component.literal(denialText(d.denial())).withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (level instanceof ServerLevel sl) sl.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.8f, 1.4f);
        player.displayClientMessage(Component.literal(d.action() == ChronicleSigilRules.Action.ENTER
                ? "The pages begin to turn. Hold still for " + d.channelTicks() / 20 + " seconds: " + f.pick().fullName() + "."
                : "The pages turn back. Hold for " + d.channelTicks() / 20 + " seconds to leave.").withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            ChronicleSigilRules.Decision d = facts(p).decision();
            if (d.allowed()) return d.channelTicks();
        }
        return ChronicleSigilRules.ENTER_TICKS;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof ServerPlayer p) || !(level instanceof ServerLevel sl)) return;
        ChronicleSigilRules.Decision d = facts(p).decision();
        if (!d.allowed()) {
            p.stopUsingItem();
            p.displayClientMessage(Component.literal(denialText(d.denial())).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (d.action() == ChronicleSigilRules.Action.ENTER && p.hurtTime > 0) {              // a blow breaks the way in; the way out is not breakable
            p.stopUsingItem();
            p.displayClientMessage(Component.literal("The pages scatter: you were struck.").withStyle(ChatFormatting.RED), true);
            return;
        }
        int total = getUseDuration(stack, p), elapsed = Math.max(0, total - remaining);
        if (elapsed % 4 != 0) return;
        ring(sl, p, 1.0 + 1.8 * (1.0 - elapsed / (double) Math.max(1, total)), elapsed);
        if (elapsed % 20 == 0) {
            sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.7f, 0.7f + 0.8f * elapsed / Math.max(1, total));
            p.displayClientMessage(Component.literal(((total - elapsed + 19) / 20) + "...").withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** A closing ring of Rukh light round the user (the mod's own particles, drawn by the server through the Rukh budget). */
    private static void ring(ServerLevel sl, Player p, double radius, int phase) {
        FxTone light = FxTone.of(false);
        int n = 10;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + phase * 0.12;
            Fx.burst(sl, FxThemes.ARENA_CHRONICLE, p.getX() + Math.cos(a) * radius, p.getY() + 0.15 + (phase % 16) * 0.12, p.getZ() + Math.sin(a) * radius, light, 1);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer user) || !(level instanceof ServerLevel sl)) return stack;
        Facts f = facts(user);
        ChronicleSigilRules.Decision d = f.decision();
        if (!d.allowed()) {
            user.displayClientMessage(Component.literal(denialText(d.denial())).withStyle(ChatFormatting.GRAY), true);
            return stack;
        }
        if (d.action() == ChronicleSigilRules.Action.LEAVE) {
            if (ArcArenaTravel.leave(user)) user.getCooldowns().addCooldown(this, ChronicleSigilRules.COOLDOWN_TICKS);
            return stack;
        }
        ArcCharacter pick = f.pick();
        if (pick == null) return stack;
        Set<UUID> party = Squads.partyMembers(user);
        List<HourglassRules.Candidate> near = new ArrayList<>();
        for (ServerPlayer other : sl.players()) {
            if (other == user || !other.isAlive() || other.isSpectator()) continue;
            near.add(new HourglassRules.Candidate(other.getUUID(), other.distanceTo(user), party != null && party.contains(other.getUUID()), hostileTo(user, other)));
        }
        List<UUID> chosen = HourglassRules.pickTravellers(near, HourglassRules.COMPANION_RANGE, HourglassRules.MAX_COMPANIONS);
        List<ServerPlayer> companions = new ArrayList<>();
        for (UUID id : chosen) if (sl.getPlayerByUUID(id) instanceof ServerPlayer sp) companions.add(sp);
        Vec3 at = user.position();
        Fx.burst(sl, FxThemes.ARENA_CHRONICLE, at.add(0, 1, 0), FxTone.of(false), 12, 1.2);
        ArcArenaTravel.enter(user, companions, pick);
        return stack;
    }

    /** Hostile = one of the two was last hurt by the other (a PvP mood). Allies and strangers are not. */
    private static boolean hostileTo(ServerPlayer user, ServerPlayer other) {
        return user.getLastHurtByMob() == other || other.getLastHurtByMob() == user;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof ServerPlayer p) p.displayClientMessage(Component.literal("The pages settle again.").withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nusmp.chronicle_sigil.tip.kind").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.nusmp.chronicle_sigil.tip.lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.nusmp.chronicle_sigil.tip.use").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nusmp.chronicle_sigil.tip.rules").withStyle(ChatFormatting.DARK_GRAY));
    }
}
