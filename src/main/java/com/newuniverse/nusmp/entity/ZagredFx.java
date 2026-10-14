package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 1.35.0: the server half of Zagred's move VFX (drawn by {@code ZagredBossLayer}). One place decides what every wind-up and strike
 * looks like, so the signature stays the same: black redaction bars, glyphs that rewrite themselves, text corrupting the air and void
 * stones that carry a rule. Pure sending: no block is placed or broken, and each call is one packet to the players near.
 */
public final class ZagredFx {
    private ZagredFx() {}

    public static final int PALE = 0xFFD8C4FF, RED = 0xFFF2194D, WHITE = 0xFFFFFFFF, INDIGO = 0xFF3A2A9A;

    /** The wind-up of a word: the rewriting ring on the floor at 'at', closing in over `ticks`. */
    public static void ring(ServerLevel sl, Vec3 at, float radius, int ticks, int argb) {
        VfxSpawn.send(sl, VfxShape.ZAGRED_GLYPH_RING, at.add(0, 0.05, 0), at.add(0, 1, 0), argb, Math.max(8, ticks), radius);
    }

    /** A censored lane from a to b: bars slide across it for `ticks`, then cut. */
    public static void inkLane(ServerLevel sl, Vec3 a, Vec3 b, float halfWidth, int ticks) {
        VfxSpawn.send(sl, VfxShape.ZAGRED_INK_BARS, a.add(0, 0.05, 0), b.add(0, 0.05, 0), RED, Math.max(10, ticks), halfWidth);
    }

    /** A void rule stone at 'at' (follows the stone), the glyph chosen by the rule. */
    public static void stone(ServerLevel sl, GrimoireDaemonEntity stone, int rule, int ticks) {
        VfxSpawn.send(sl, new com.newuniverse.nusmp.vfx.VfxPayload(VfxShape.ZAGRED_RULE_STONE.ordinal(), stone.position().subtract(0, 1.0, 0), stone.position(),
                KotodamaWords.VIOLET, ticks, 1f, -1, (long) (rule & 15)));
    }

    /** Text rising out of the floor and corrupting the air over `radius` blocks. */
    public static void corrupt(ServerLevel sl, Vec3 at, float radius, int ticks, int argb) {
        VfxSpawn.send(sl, VfxShape.ZAGRED_TEXT_CORRUPT, at.add(0, 0.05, 0), at.add(0, 1, 0), argb, Math.max(12, ticks), radius);
    }

    /** The sentence of a word lands: from 'a' to 'b', bursting into letters at b. */
    public static void strike(ServerLevel sl, Vec3 a, Vec3 b, int argb, float impact, int ticks) {
        VfxSpawn.send(sl, VfxShape.ZAGRED_REWRITE_STRIKE, a, b, argb, Math.max(10, ticks), impact);
    }

    /** Re-sends the glow of every living rule stone (they last longer than one effect). */
    public static void stones(ServerLevel sl, List<UUID> ids, int rule) {
        for (UUID id : ids) if (sl.getEntity(id) instanceof GrimoireDaemonEntity s) stone(sl, s, rule, 60);
    }

    /** The colour of a word's ring: violet for most, red for the erasing words, pale for the healing and revealing ones. */
    public static int wordColor(KotodamaWords.Word w) {
        return switch (w) {
            case REDACT, OVERWRITE, DRAIN, FEAR -> RED;
            case HEAL, REVEAL -> PALE;
            case SEAL, REJECT, BANISH, SLEEP, PETRIFY -> INDIGO;
            default -> KotodamaWords.VIOLET;
        };
    }
}
