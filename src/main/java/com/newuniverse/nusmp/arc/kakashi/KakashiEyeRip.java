package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.skill.SharinganSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why (5.8.0, owner request): three times in the fight (each time he enters a new phase) Kakashi is stunned for {@code eyeWindowTicks}; a player who runs up and right-clicks him plays the eye-rip
 * mini-game ({@link EyeRipRules}) and a player who pulls cleanly enough times rips out his eye and gains the unique skill Sharingan ({@link SharinganSkill}, to be evolved into the Rinnegan later). One
 * instance per boss, so nothing is static and everything is gone with the boss. Every rule is checked here on the server (reach, window, click gap, cap, the skill already held); the client only sends the
 * normal right click. Cost: only while a window is open (at most 8 seconds, three times a fight): at most {@code eyeSessions} players, one action-bar line each every second tick, nothing else.
 */
final class KakashiEyeRip {
    private static final class Session {
        long start, lastClick;
        int progress, pull;
    }

    private final Map<UUID, Session> sessions = new HashMap<>(4);
    private long openUntil;
    private int opened;

    void reset() { sessions.clear(); openUntil = 0L; opened = 0; }

    boolean isOpen(long now) { return openUntil > now; }

    int windowsOpened() { return opened; }

    /** He entered phase {@code to}: his guard is open for the window (his stun is set by the caller). */
    void open(KakashiBoss b, ServerLevel sl, int to, long now) {
        if (!KakashiConfig.eyeRip() || !EyeRipRules.windowFor(to)) return;
        sessions.clear();
        opened++;
        openUntil = now + KakashiConfig.eyeWindowTicks();
        KakashiFx.ring(sl, KakashiFx.KAMUI, b.position(), 2.5, 16);
        tell(b, sl, "His eye is bare! Run up and right-click him, then right-click again with the marker in the zone to rip it out (" + opened + "/" + EyeRipRules.WINDOWS + ").", ChatFormatting.DARK_RED);
    }

    int stunTicks() { return KakashiConfig.eyeRip() ? KakashiConfig.eyeWindowTicks() : 0; }

    /** A right click on him. True when the mini-game took it (the click is spent). */
    boolean interact(KakashiBoss b, ServerPlayer p, long now) {
        boolean open = isOpen(now);
        Session s = sessions.get(p.getUUID());
        boolean has = SharinganSkill.has(p);
        if (!EyeRipRules.canBegin(KakashiConfig.eyeRip(), open, has, sessions.size(), KakashiConfig.eyeSessions(), s != null)) {
            if (open && has) p.displayClientMessage(Component.literal("You already carry the Sharingan.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (p.distanceToSqr(b) > EyeRipRules.REACH * EyeRipRules.REACH) {
            p.displayClientMessage(Component.literal("Too far: get closer to grip the eye.").withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        if (s == null) {
            s = new Session();
            s.start = now;
            s.lastClick = now;
            sessions.put(p.getUUID(), s);
            p.displayClientMessage(Component.literal("You grip his eye. Right-click when the | is inside the ===== zone.").withStyle(ChatFormatting.RED), true);
            return true;
        }
        if (!EyeRipRules.clickAllowed(now, s.lastClick)) return true;
        s.lastClick = now;
        long seed = s.start + p.getUUID().getLeastSignificantBits();
        EyeRipRules.Pull r = EyeRipRules.pull(EyeRipRules.marker(now - s.start), EyeRipRules.zoneStart(seed, s.pull), s.progress, KakashiConfig.eyePulls());
        s.progress = r.progress();
        s.pull++;
        ServerLevel sl = p.serverLevel();
        if (r.ripped()) {
            sessions.remove(p.getUUID());
            SharinganSkill.grant(p);
            sl.playSound(null, b.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.0f, 1.4f);
            KakashiFx.burst(sl, KakashiFx.KAMUI, b.position().add(0, b.getBbHeight() * 0.8, 0), 16, 0.8);
            tell(b, sl, p.getGameProfile().getName() + " tears out Kakashi's eye!", ChatFormatting.DARK_RED);
        } else if (r.clean()) {
            sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 0.8f + 0.2f * s.progress);
            p.displayClientMessage(Component.literal("Clean pull " + s.progress + "/" + KakashiConfig.eyePulls() + ".").withStyle(ChatFormatting.GOLD), true);
        } else {
            sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8f, 0.6f);
            p.displayClientMessage(Component.literal("Bad pull: the eye slips. Start again.").withStyle(ChatFormatting.RED), true);
            double hurt = KakashiConfig.eyeBacklash();
            if (hurt > 0) p.hurt(b.damageSources().mobAttack(b), (float) hurt);
        }
        return true;
    }

    /** Called every tick of a fight; does nothing unless a window is open. */
    void tick(KakashiBoss b, ServerLevel sl, long now) {
        if (openUntil == 0L) return;
        if (now >= openUntil) {
            openUntil = 0L;
            sessions.clear();
            tell(b, sl, "His eye closes again.", ChatFormatting.GRAY);
            return;
        }
        if (now % 2 != 0 || sessions.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, Session>> it = sessions.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Session> e = it.next();
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || !p.isAlive() || p.level() != sl || p.distanceToSqr(b) > 36.0) { it.remove(); continue; }
            Session s = e.getValue();
            long seed = s.start + p.getUUID().getLeastSignificantBits();
            p.displayClientMessage(Component.literal("[" + EyeRipRules.bar(EyeRipRules.marker(now - s.start), EyeRipRules.zoneStart(seed, s.pull)) + "]  " + s.progress + "/" + KakashiConfig.eyePulls())
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /** One line to the players near him (at most the players of the level within 48 blocks: no world query). */
    private static void tell(KakashiBoss b, ServerLevel sl, String text, ChatFormatting color) {
        for (ServerPlayer q : sl.players()) if (q.distanceToSqr(b) <= 48.0 * 48.0) q.sendSystemMessage(Component.literal(text).withStyle(color));
    }
}
