package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;

/**
 * 1.99.0: the one place where a page cast meets the armour layer. GrimoireBook calls {@link #beforeCast} (an armour page of a worn armour only takes it off: free, no cooldown, no spell)
 * and {@link #afterCast} (a finished cast: an armour page puts the armour on; EVERY OTHER PAGE LEAVES THE ARMOUR LAYER ALONE, so casting a bolt never equips anything and never resets
 * the timer of an armour that is already on). The decision is the pure {@link ArmourPolicy#forCast}.
 *
 * <p>The magics that have no armour (policy NONE) and used to wear one after every cast get a light shimmer instead where their look was carried by the armour: the magic's own
 * zone effect, small, following the caster (policy field {@code shimmer}); no armour piece is drawn.
 */
public final class ArmourCast {
    private ArmourCast() {}

    /** The ticks the shimmer of a NONE magic lasts and its power (a soft glow, a fraction of the zone effect). */
    static final int SHIMMER_TICKS = 14;
    static final float SHIMMER_POWER = 0.4f;

    private static int styleOf(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, String magic, String pageId) {
        boolean mastered = b.masteryFrac(i) >= 0.75 || GrimoireBook.inDevilState(p);
        return ArmourPolicy.get().styleFor(magic, pageId, p.isShiftKeyDown(), mastered);
    }

    /** True when this cast was only "take the armour off": the caller then ends the cast (nothing is spent, no cooldown, no spell runs). */
    public static boolean beforeCast(ServerPlayer p, GrimoireBook b, ManasSkillInstance i, String pageId) {
        try {
            String magic = b.magicNow().name();
            if (ArmourPolicy.get().forCast(magic, pageId) != ArmourPolicy.Instruction.ARMOUR) return false;
            if (!ArmourRules.onlyTakesOff(ArmourState.held(p), magic, styleOf(b, i, p, magic, pageId))) return false;
            ArmourState.takeOff(p);
            return true;
        } catch (RuntimeException e) {
            LOG.error("[nusmp] armour: beforeCast failed for page {}", pageId, e);
            return false;
        }
    }

    /** After a successful cast of {@code pageId} (mode {@code mode} of the book). */
    public static void afterCast(ServerPlayer p, GrimoireBook b, ManasSkillInstance i, String pageId, int mode) {
        try {
            afterCastChecked(p, b, i, pageId, mode);
        } catch (RuntimeException e) {
            LOG.error("[nusmp] armour: afterCast failed for page {}", pageId, e);      // a bug here must never break the cast that already succeeded
        }
    }

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static void afterCastChecked(ServerPlayer p, GrimoireBook b, ManasSkillInstance i, String pageId, int mode) {
        String magic = b.magicNow().name();
        ArmourPolicy.Entry e = ArmourPolicy.get().entry(magic);
        ArmourPolicy.Instruction ins = ArmourPolicy.get().forCast(magic, pageId);
        if (ins == ArmourPolicy.Instruction.NONE) {                       // the usual case: the armour layer is not touched
            if (e != null && !e.shimmer().isEmpty() && mode < b.familyCount()) shimmer(p, b, e.shimmer());
            return;
        }
        if (ins == ArmourPolicy.Instruction.FORM || e == null) return;      // the form pages run their own code (SpiritDiveForm)
        int style = styleOf(b, i, p, magic, pageId);
        ArmourRules.Held held = ArmourState.held(p);
        switch (ArmourRules.armourPage(held, magic, style)) {
            case PUT_ON, RESTYLE -> ArmourState.putOn(p, e, style, false, 1f);
            case SWAP -> { ArmourState.takeOff(p); ArmourState.putOn(p, e, style, false, 1f); }
            case TAKE_OFF -> ArmourState.takeOff(p);                        // the cast itself changed the style back: same page, same style, worn: off
            case KEEP -> { }                                                // a FORM is on: the form's look is the one that shows
        }
    }

    private static void shimmer(ServerPlayer p, GrimoireBook b, String shape) {
        try {
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.valueOf(shape), p, p.position().add(0, 1, 0), b.color, SHIMMER_TICKS, SHIMMER_POWER);
        } catch (IllegalArgumentException ignored) {
            // an unknown shape name in the data is not worth a crash: no shimmer
        }
    }
}
