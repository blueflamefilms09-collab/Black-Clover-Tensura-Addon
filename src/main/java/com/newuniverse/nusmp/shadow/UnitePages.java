package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the casts of the new Devil Unite pages of the Shadow book (Equus, Felis and Gallus's call). The two quick buffs that already existed
 * (Canis, Gallus, Canis x Felis) are not touched; these are separate pages appended after them. Each cast starts the mode through
 * {@link UniteState} (stats and armour overlay) and plays the same shadow aura the old Unite pages play.
 */
public final class UnitePages {
    private UnitePages() {}

    /** Unite Mode: Equus (Slotos): a walking fortress for 30 seconds. */
    public static boolean equus(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return unite(b, p, UniteKind.EQUUS, 1.4f);
    }

    /** Unite Mode: Felis (Plumede): agility, night sight, stealth and dodging for 30 seconds. */
    public static boolean felis(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return unite(b, p, UniteKind.FELIS, 1f);
    }

    /** Gallus's call: stuns everyone within 8 blocks; the second use in the mode roots one enemy. */
    public static boolean gallusCall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return UniteAbilities.gallusCall(b, p);
    }

    private static boolean unite(GrimoireBook b, ServerPlayer p, UniteKind kind, float size) {
        b.castCircle(p, 1.3f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SHADOW_UNITE, p, p.position().add(0, 1, 0), 0, UniteKind.TICKS, size);
        UniteState.start(p, kind, UniteKind.TICKS);
        UniteAbilities.onStart(p, kind, false);
        return true;
    }
}
