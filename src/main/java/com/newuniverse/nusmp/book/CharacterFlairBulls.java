package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.charset.BullsFlairPalette;
import com.newuniverse.nusmp.charset.CharacterSets;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the owner asked that a character's grimoire look 10x better than the basic one (docs/magic/character_spells_bulls.md). After a successful cast of a grimoire bound to a Black Bulls
 * member that has a spell set, this adds 1 to 3 small signature layers in his colour on top of the page's own effect (see {@link BullsFlairPalette}). Server side, nearby players only through
 * {@link VfxSpawn} (inside the per-tick packet budget); nothing is stored per player outside the player's own persistent data; worst case 3 packets per cast and one flair per 6 ticks per player.
 * A grimoire without a set, the basic grimoire and the server switch {@code magic.characterSpellSets=false} get nothing.
 */
public final class CharacterFlairBulls {
    private CharacterFlairBulls() {}

    private static final String LAST = "nusmp_flair_tick";

    /** Called by {@code GrimoireBook.castPage} after a successful cast. */
    public static void onCast(ServerPlayer p, ManasSkillInstance inst, BookPage page) {
        if (page == null || !CharacterSets.enabled()) return;
        var tag = inst.getTag();
        String canon = tag == null ? "" : tag.getString("Canon");
        BullsFlairPalette.Look look = BullsFlairPalette.of(canon);
        if (look == null || CharacterSets.registry().of(canon) == null) return;
        int packets = BullsFlairPalette.packetsFor(page.costPercent());
        if (packets <= 0) return;
        long now = p.level().getGameTime();
        if (!BullsFlairPalette.gapOk(now, p.getPersistentData().getLong(LAST))) return;
        p.getPersistentData().putLong(LAST, now);
        VfxShape ring = shape(look.ring()), burst = shape(look.burst()), aura = shape(look.aura());
        float power = packets == 1 ? 0.9f : packets == 2 ? 1.2f : 1.6f;
        Vec3 feet = p.position();
        VfxSpawn.send(p.serverLevel(), ring, feet, feet.add(0, 0.2, 0), look.color(), 26, power);
        if (packets >= 2) {
            Vec3 at = GrimoireBook.aim(p, 14);
            VfxSpawn.send(p.serverLevel(), burst, at, at.add(0, 1, 0), look.color(), 20, power * 0.9f);
        }
        if (packets >= 3) VfxSpawn.sendFollowing(p.serverLevel(), aura, p, feet.add(0, 1, 0), look.color(), 40, power * 0.7f);
    }

    private static VfxShape shape(String name) {
        try { return VfxShape.valueOf(name); } catch (IllegalArgumentException e) { return VfxShape.MAGIC_CIRCLE; }
    }
}
