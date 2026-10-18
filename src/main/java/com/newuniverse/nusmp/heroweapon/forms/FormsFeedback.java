package com.newuniverse.nusmp.heroweapon.forms;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroClass;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: everything the forms system shows a player (a chat line, an action-bar line, a title, a sound, the swirl into the gem) goes through here, for two reasons.
 * The self-tests drive the system with a stand-in player that has no network connection, and a message sent to it would fail; and feedback must never break the
 * thing it reports on, so each call is guarded and a failure is logged once. The colours and shapes of the effects are chosen here too, so the gem looks the same
 * whether it swallowed a stack, learned a form or finished one. Effects use the mod's own effect shapes ({@link VfxSpawn}), never vanilla particles.
 */
final class FormsFeedback {
    private FormsFeedback() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;
    private static final Map<UUID, Component> LATER = new HashMap<>();

    /** True for a person at a keyboard: not a stand-in, and connected. */
    static boolean real(ServerPlayer p) { return p != null && !(p instanceof FakePlayer) && p.connection != null; }

    private static void failed(RuntimeException e) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] hero forms feedback failed once (shown no more)", e);
    }

    static void chat(ServerPlayer p, Component c) {
        if (!real(p)) return;
        try {
            p.sendSystemMessage(c);
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    static void bar(ServerPlayer p, Component c) {
        if (!real(p)) return;
        try {
            p.displayClientMessage(c, true);
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    /**
     * An action-bar line that is sent at the end of the tick instead of now, so that it lands AFTER any line another system puts on the bar in the same tick (the core
     * prints its own line when the cycle key finds nothing to change to, which would hide the reason a lock gave). The last one said in a tick wins.
     */
    static void barLater(ServerPlayer p, Component c) {
        if (real(p)) LATER.put(p.getUUID(), c);
    }

    /** Sends the lines of {@link #barLater}; called once at the end of every server tick. */
    static void flush(MinecraftServer server) {
        if (LATER.isEmpty()) return;
        List<Map.Entry<UUID, Component>> pending = new ArrayList<>(LATER.entrySet());
        LATER.clear();
        for (Map.Entry<UUID, Component> e : pending) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) bar(p, e.getValue());
        }
    }

    static void forget(UUID player) { LATER.remove(player); }

    static void clear() { LATER.clear(); }

    static void title(ServerPlayer p, Component title, Component subtitle) {
        if (!real(p)) return;
        try {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    static void sound(ServerPlayer p, SoundEvent sound, float volume, float pitch) {
        if (!FormsConfig.get(FormsConfig.EFFECTS) || p == null || p instanceof FakePlayer) return;
        try {
            p.level().playSound(null, p.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    /** What the gem swallowed: motes gather round the wielder and the item dissolves into the hand. */
    static void swirl(ServerPlayer p, int rgb) {
        if (!FormsConfig.get(FormsConfig.EFFECTS) || p == null || p instanceof FakePlayer) return;
        try {
            int argb = 0xFF000000 | rgb;
            Vec3 look = p.getViewVector(1f), hand = p.getEyePosition().add(look.scale(0.55)).add(0, -0.45, 0), offered = p.getEyePosition().add(look.scale(1.1)).add(0, -0.25, 0);
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MANA_CHARGE, p, p.position().add(0, 1, 0), argb, 16, 0.8f);
            VfxSpawn.send(p.serverLevel(), VfxShape.KOTO_SHATTER, offered, hand, argb, 16, 0.5f);
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    /** A form was learned: a transmutation circle at the wielder's feet. */
    static void circle(ServerPlayer p, int rgb) {
        if (!FormsConfig.get(FormsConfig.EFFECTS) || p == null || p instanceof FakePlayer) return;
        try {
            Vec3 at = p.position();
            VfxSpawn.send(p.serverLevel(), VfxShape.ALCHEMY_CIRCLE, at, at.add(0, 1, 0), 0xFF000000 | rgb, 30, 1.4f);
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    /** The colour of the weapon's gem for effects: the sin's colour while a curse form is active, else the family's. */
    static int gemColor(HeroBond bond) {
        HeroForm f = HeroForms.active(bond);
        if (f.curse() && f.sin() != null) return f.sin().rgb();
        HeroClass c = bond.kind().heroClass();
        return c == HeroClass.HOLY ? 0xE8C468 : c == HeroClass.SEVEN_STAR ? 0x7CF0B0 : 0x9A7CFF;
    }
}
