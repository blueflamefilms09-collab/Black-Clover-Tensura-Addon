package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.EarthFxStyle;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0 The grimoire of the five wiki earth-family magics (Sandstone, Rock, Red Ochre, Stone, Mineral): an {@link ElementBook} (so the Empower and thorns tags of the temporary
 * armour work) whose effects are the earth-family layer instead of the shared wiki layer. The page shapes of {@link ElementBook} still work here: a page that asks for the
 * old WIKI_MAGIC_CAST / FIELD / BURST shape gets this magic's own EARTHFAM_CAST / ZONE / BURST in its own palette. The pages themselves are built in {@link EarthFamilyBooks}.
 * Damage is earth-elemental.
 */
public final class EarthFamilyBook extends ElementBook {
    public EarthFamilyBook(MagicType magic, int color, List<BookPage> pages) {
        super(magic, color, TensuraDamageTypes.EARTH_ELEMENTAL, pages);
    }

    private int style() { return EarthFxStyle.style(magic.name()); }

    @Override
    public void vfx(ServerPlayer player, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxShape mapped = switch (shape) {
            case WIKI_MAGIC_CAST -> VfxShape.EARTHFAM_CAST;
            case WIKI_MAGIC_FIELD -> VfxShape.EARTHFAM_ZONE;
            case WIKI_MAGIC_BURST -> VfxShape.EARTHFAM_BURST;
            default -> shape;
        };
        if (mapped == shape) { super.vfx(player, shape, from, to, ticks, power); return; }
        VfxSpawn.send(player.serverLevel(), mapped, from, to, EarthFxStyle.color(style()), ticks, power);
    }

    @Override
    public void castCircle(ServerPlayer player, float power) {
        VfxSpawn.send(player.serverLevel(), VfxShape.EARTHFAM_BURST, player.position().add(0, 0.05, 0), player.getEyePosition(), EarthFxStyle.color(style()), 16, 0.7f * Math.max(0.5f, power));
    }

    @Override
    public void impact(ServerPlayer player, Vec3 at, float power) {
        VfxSpawn.send(player.serverLevel(), VfxShape.EARTHFAM_BURST, at, at.add(0, 1, 0), EarthFxStyle.color(style()), 20, Math.max(0.6f, power * 1.6f));
    }
}
