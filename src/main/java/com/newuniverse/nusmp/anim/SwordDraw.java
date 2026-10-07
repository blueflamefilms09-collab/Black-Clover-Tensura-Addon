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
 */
public final class SwordDraw {
    private SwordDraw() {}

    public static final int ANTI_MAGIC = 0, SWORD_MAGIC = 1;
    /** The tick at which the hilt leaves the grimoire. */
    public static final int GIVE_TICK = 12;
    public static final int DURATION = 24;

    /** Plays the draw and hands over 'items' at tick 12 (the first into the main hand if it is empty, the rest into the inventory). */
    public static void draw(ServerPlayer p, int style, ItemStack... items) {
        ServerLevel sl = p.serverLevel();
        boolean am = style == ANTI_MAGIC;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new SwordDrawPayload(p.getId(), style));
        Vec3 hip = hip(p);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, hip, hip.add(0, 0.3, 0), am ? 0xFF0A0408 : 0xFFFFF4C0, 12, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, am ? 0.6f : 1.3f);
        SpellRuntime.later(sl, 6, () -> {
            if (!p.isAlive()) return;
            Vec3 h = hip(p);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, h.add(0, 0.2, 0), h.add(0, 0.7, 0), am ? 0xFF7A0A1E : 0xFFB8D8FF, 12, 0.9f);
        });
        SpellRuntime.later(sl, GIVE_TICK, () -> {
            for (int k = 0; k < items.length; k++) give(p, items[k], k == 0);
            if (!p.isAlive()) return;
            Vec3 h = hip(p).add(0, 0.6, 0);
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, h, h.add(0, 1.2, 0), am ? 0xFFC0102A : 0xFFFFFFFF, 16, 1.5f);
            if (am) {                                                       // heavy, metallic, with a low crack of the void
                sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.1f, 0.5f);
                sl.playSound(null, p.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.5f, 0.4f);
            } else {                                                        // a bright, sharp blade-draw chime
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
