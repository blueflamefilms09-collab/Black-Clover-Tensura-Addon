package com.newuniverse.nusmp.anim;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 0.52: the Grimoire Sword Draw (docs/grimoire_sword_draw.md). When Anti-Magic or Sword Magic summons a weapon, the player does not
 * just receive it: the grimoire opens at the hip, the main arm reaches down into its pages, grips the hilt and pulls the sword
 * out. The client plays the pose ({@code client.SwordDrawClient}, 24 ticks); the server keeps the clock:
 * <pre>
 *   tick  0  the grimoire opens and glows at the hip (payload sent, first VFX, page sound)
 *   tick  6  more glow while the arm reaches
 *   tick 12  the hilt leaves the pages: the weapon is placed in the main hand (or the inventory if the hand is full), blade-draw
 *            sound and the burst VFX
 *   tick 24  the pose is over (the client blends it out from tick 20)
 * </pre>
 * 0.54: two more looks (Yami's Dark Magic, and a plain one for every other grimoire) and the REVERSE of the draw: {@link #store} plays
 * the same motion backwards, the weapon sinking into the pages at tick 12 (docs/grimoire_weapon_store.md).
 */
public final class SwordDraw {
    private SwordDraw() {}

    /** Styles: the grimoire the weapon comes from. */
    public static final int ANTI_MAGIC = 0, SWORD_MAGIC = 1, YAMI = 2, GENERIC = 3;
    /** Added to the style in the payload: play the motion backwards (a weapon going back into the book). */
    public static final int REVERSE = 16;
    /** The tick at which the hilt leaves (or enters) the grimoire. */
    public static final int GIVE_TICK = 12;
    public static final int DURATION = 24;

    // core / mid / burst colours of the glow at the hip, per style
    private static final int[] CORE = {0xFF0A0408, 0xFFFFF4C0, 0xFF12041E, 0xFFE8F0FF};
    private static final int[] MID = {0xFF7A0A1E, 0xFFB8D8FF, 0xFF5A1A9A, 0xFFB0C8FF};
    private static final int[] BURST = {0xFFC0102A, 0xFFFFFFFF, 0xFF9A4AE0, 0xFFFFFFFF};

    /** The grimoire's look for a magic: Anti-Magic, Sword Magic and Dark Magic (Yami) have their own, everything else the plain one. */
    public static int styleFor(com.newuniverse.nusmp.blackclover.MagicType magic) {
        if (magic == null) return GENERIC;
        return switch (magic) {
            case ANTI_MAGIC -> ANTI_MAGIC;
            case SWORD -> SWORD_MAGIC;
            case DARK -> YAMI;
            default -> GENERIC;
        };
    }

    private static boolean dark(int style) { return style == ANTI_MAGIC || style == YAMI; }

    /** Plays the draw and hands over 'items' at tick 12 (the first into the main hand if it is empty, the rest into the inventory). */
    public static void draw(ServerPlayer p, int style, ItemStack... items) {
        play(p, style, false, () -> { for (int k = 0; k < items.length; k++) give(p, items[k], k == 0); });
    }

    /** Plays the draw and runs 'atHilt' at tick 12 instead of handing over fixed items (it decides what comes out). */
    public static void drawThen(ServerPlayer p, int style, Runnable atHilt) { play(p, style, false, atHilt); }

    /** Plays the draw backwards: at tick 12 the weapon goes into the pages and 'atHilt' runs (it takes the weapon from the hand and keeps it). */
    public static void store(ServerPlayer p, int style, Runnable atHilt) { play(p, style, true, atHilt); }

    private static void play(ServerPlayer p, int style, boolean reverse, Runnable atHilt) {
        ServerLevel sl = p.serverLevel();
        int st = Math.max(0, Math.min(CORE.length - 1, style & 15));
        boolean dk = dark(st);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new SwordDrawPayload(p.getId(), reverse ? st | REVERSE : st));
        Vec3 hip = hip(p);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, hip, hip.add(0, 0.3, 0), CORE[st], 12, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, dk ? 0.6f : 1.3f);
        SpellRuntime.later(sl, 6, () -> {
            if (!p.isAlive()) return;
            Vec3 h = hip(p);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, h.add(0, 0.2, 0), h.add(0, 0.7, 0), MID[st], 12, 0.9f);
        });
        SpellRuntime.later(sl, GIVE_TICK, () -> {
            atHilt.run();
            if (!p.isAlive()) return;
            Vec3 h = hip(p).add(0, 0.6, 0);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, h, h.add(0, 1.2, 0), BURST[st], 16, reverse ? 1.1f : 1.5f);
            if (reverse) {                                                   // the weapon sinks into the pages and the book closes on it
                sl.playSound(null, p.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 1.3f, dk ? 0.6f : 1.1f);
                sl.playSound(null, p.blockPosition(), dk ? SoundEvents.ANVIL_PLACE : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, dk ? 0.7f : 1.2f, dk ? 0.6f : 0.9f);
            } else if (dk) {                                                 // heavy, metallic, with a low crack of the void
                sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.1f, 0.5f);
                sl.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 0.4f);
            } else {                                                         // a bright, sharp blade-draw chime
                sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.6f);
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6f, 1.5f);
            }
        });
    }

    static void give(ServerPlayer p, ItemStack s, boolean toHand) {
        if (toHand && p.getMainHandItem().isEmpty()) p.setItemInHand(InteractionHand.MAIN_HAND, s);
        else p.getInventory().placeItemBackInInventory(s);
    }

    /** The grimoire's floating spot beside the right hip. */
    static Vec3 hip(ServerPlayer p) {
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return p.position().add(right.scale(0.5)).add(fwd.scale(0.15)).add(0, 0.95, 0);
    }
}
