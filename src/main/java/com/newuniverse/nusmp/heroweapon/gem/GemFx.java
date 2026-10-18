package com.newuniverse.nusmp.heroweapon.gem;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Why: the Choosing has to be seen and heard, because a player who holds a button for five seconds needs to know it is working, how long is left, and when it
 * broke or why; and the moment a weapon answers is the one moment of this whole job that should feel like the series. Everything the player perceives is here and
 * nowhere in the decisions: the ritual circle (the holy sigil of the Lucius boss effects, turning under the player's feet with its rim closing over exactly the
 * ritual's length, light rays standing up in the last third), the motes gathering once in a while in the colour of the weapon (the Holy Calling Gem walks through
 * the four jewel colours, one a second, because four weapons are listening), the countdown in the action bar, the pillar of light and four flares when a weapon
 * answers, and the title "The Legendary Shield has chosen you." The screen is never shaken and the camera never touched. Effects use only the mod's own
 * {@code VfxSpawn} shapes and {@code Tells}; the config switch {@code effects} turns the circle, the motes and the sounds off (the words stay). Nothing here may
 * throw into the ritual: a failed effect is logged at debug level and skipped.
 */
final class GemFx {
    private GemFx() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** An action-bar line (the countdown and the refusals). */
    static void say(ServerPlayer p, Component line) {
        try {
            p.displayClientMessage(line, true);
        } catch (RuntimeException e) {
            LOG.debug("gem message failed", e);
        }
    }

    private static void sound(ServerLevel sl, ServerPlayer p, SoundEvent s, float volume, float pitch) {
        sl.playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * Sends one effect at a point. The seed is made even on purpose: the divine effects read bit 0 of the seed as "draw the dim telegraph form", and the ritual wants
     * their full form (the closing ring and the standing rays of the sigil, the strike form of the pillar) every time, not half of the time.
     */
    private static void spawn(ServerLevel sl, VfxShape shape, Vec3 at, int rgb, int duration, float power) {
        long seed = sl.random.nextLong() & ~1L;
        VfxSpawn.send(sl, new VfxPayload(shape.ordinal(), at, at, GemCatalog.argb(rgb), duration, power, -1, seed));
    }

    /** The light of the ritual at tick {@code n}: the weapon's own, or for the Holy Calling Gem the next of the four jewel colours each second. */
    static int colourAt(HeroKind seal, int n) {
        return seal != null ? GemCatalog.colour(seal) : GemCatalog.FOUR_COLOURS[Math.floorMod(n / 20, GemCatalog.FOUR_COLOURS.length)];
    }

    // ------------------------------------------------------------------ the ritual begins
    static void start(ServerPlayer p, HeroKind seal, int totalTicks) {
        say(p, GemText.msg(GemMessages.START, String.valueOf(Math.max(1, totalTicks / 20))).withStyle(ChatFormatting.GOLD));
        if (!GemConfig.effects() || !(p.level() instanceof ServerLevel sl)) return;
        try {
            int rgb = seal == null ? GemCatalog.HOLY_WHITE : GemCatalog.colour(seal);
            Vec3 feet = p.position().add(0.0, 0.06, 0.0);
            sound(sl, p, SoundEvents.BELL_RESONATE, 0.8f, 1.5f);
            spawn(sl, VfxShape.DIVINE_SIGIL, feet, rgb, totalTicks + 2, seal == null ? 1.9f : 1.5f);
            Tells.ring(sl, p.position(), seal == null ? 2.2 : 1.8, rgb);
        } catch (RuntimeException e) {
            LOG.debug("gem start effect failed", e);
        }
    }

    // ------------------------------------------------------------------ one channelled tick
    static void channel(ServerPlayer p, HeroKind seal, GemRitual r) {
        int n = r.elapsed();
        if (n <= 0 || r.ended()) return;
        if (n % 20 == 0) say(p, GemText.msg(GemMessages.COUNT, String.valueOf(r.secondsLeft())).withStyle(ChatFormatting.GOLD));
        if (!GemConfig.effects() || !(p.level() instanceof ServerLevel sl)) return;
        try {
            if (n % 20 == 0) sound(sl, p, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f, (float) (0.7 + 0.8 * r.progress()));
            if (n % 10 == 5) Tells.gather(sl, p.position().add(0.0, 1.0, 0.0), 1.5, colourAt(seal, n), 6);
        } catch (RuntimeException e) {
            LOG.debug("gem channel effect failed", e);
        }
    }

    // ------------------------------------------------------------------ the ritual broke
    static void broken(ServerPlayer p, GemRitual.Interrupt why) {
        if (why == null) return;
        say(p, GemText.msg(GemMessages.interruptKey(why)).withStyle(why == GemRitual.Interrupt.HURT ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (!GemConfig.effects() || !(p.level() instanceof ServerLevel sl)) return;
        try {
            sound(sl, p, SoundEvents.AMETHYST_BLOCK_BREAK, 0.7f, 0.8f);
            Tells.burst(sl, p.position().add(0.0, 1.0, 0.0), 0x9A9AB0, 12);
        } catch (RuntimeException e) {
            LOG.debug("gem broken effect failed", e);
        }
    }

    // ------------------------------------------------------------------ a weapon answered
    static void chosen(ServerPlayer p, HeroKind kind, boolean holyGem) {
        int rgb = GemCatalog.colour(kind);
        try {
            MutableComponent title = GemText.msg(GemMessages.TITLE, kind.display()).withColor(rgb).withStyle(ChatFormatting.BOLD);
            MutableComponent sub = kind.holder().isEmpty() ? GemText.msg(GemMessages.SUBTITLE_PLAIN) : GemText.msg(GemMessages.SUBTITLE_HOLDER, kind.holder());
            if (p.connection != null) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                p.connection.send(new ClientboundSetTitleTextPacket(title));
                p.connection.send(new ClientboundSetSubtitleTextPacket(sub.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            }
            p.sendSystemMessage(GemText.msg(GemMessages.CHOSEN, kind.display()).withColor(rgb));
        } catch (RuntimeException e) {
            LOG.debug("gem title failed", e);
        }
        if (!GemConfig.effects() || !(p.level() instanceof ServerLevel sl)) return;
        try {
            Vec3 feet = p.position().add(0.0, 0.05, 0.0), chest = p.position().add(0.0, 1.1, 0.0);
            sound(sl, p, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.2f);
            sound(sl, p, SoundEvents.PLAYER_LEVELUP, 0.9f, 0.8f);
            if (holyGem) {                                                // four lights, one of which answered: a flare of each jewel colour round the player
                double[][] at = {{2.2, 0.0}, {0.0, 2.2}, {-2.2, 0.0}, {0.0, -2.2}};
                for (int i = 0; i < at.length; i++) {
                    Vec3 v = chest.add(at[i][0], 0.2, at[i][1]);
                    spawn(sl, VfxShape.LIGHT_FLARE, v, GemCatalog.FOUR_COLOURS[i], 14, 1.1f);
                }
            }
            spawn(sl, VfxShape.DIVINE_PILLAR, feet, rgb, 30, 1.3f);
            spawn(sl, VfxShape.LIGHT_FLARE, chest, rgb, 14, 1.8f);
            Tells.burst(sl, chest, rgb, 30);
        } catch (RuntimeException e) {
            LOG.debug("gem chosen effect failed", e);
        }
    }
}
