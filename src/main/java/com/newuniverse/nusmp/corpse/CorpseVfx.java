package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the two Soul Corpse spell effects are sent from several places (a body rising, a person pulled back, the pact being armed), and
 * each needs the same colour (the Soul Corpse palette of {@code WikiBooks.color}) and the same payload conventions, so the calls are
 * made here once. CORPSE_RAISE: a soul ripped up out of the ground at 'from' (power = size). CORPSE_PACT: the sigil round the followed
 * caster; seed bit 0 = the short flash form used when the pact pays out.
 */
final class CorpseVfx {
    private CorpseVfx() {}

    /** ARGB of the Soul Corpse palette (the same value WikiBooks.color gives the attribute, so WikiMagicLayer and these agree). */
    static final int PALE = 0xFF6C7899;

    static void raise(ServerLevel level, Vec3 at, float size, int ticks) {
        VfxSpawn.send(level, VfxShape.CORPSE_RAISE, at, at.add(0, 2, 0), PALE, ticks, size);
    }

    static void pact(ServerLevel level, Entity caster, int ticks, boolean flash) {
        VfxSpawn.send(level, new VfxPayload(VfxShape.CORPSE_PACT.ordinal(), caster.position(), caster.position().add(0, 1, 0), PALE, ticks,
                flash ? 1.2f : 1.6f, caster.getId(), (level.random.nextLong() & ~1L) | (flash ? 1L : 0L)));
    }
}
