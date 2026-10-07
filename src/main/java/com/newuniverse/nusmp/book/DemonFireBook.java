package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/**
 * Demon Fire Magic: black flame that ignores fire resistance (it hits as magic and leaves the demon burn), explodes burning foes
 * and rises in pillars. The first page is the original strike; the rest are appended after it.
 */
public class DemonFireBook extends GrimoireBook {
    static final int COLOR = 0xFF8A2AD0;

    private final List<BookPage> pages = List.of(
            BookPage.starter("demon_fire_strike", "Demon Flame Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.DEMON_FIRE_FX1, null, ElementBook.NONE)),
            BookPage.starter("black_flame_bullets", "Black Flame Bullets", DemonFireArts::bullets).withAnim("out"),
            BookPage.mid("soul_scorch", "Soul Scorch", DemonFireArts::soulScorch).withAnim("out"),
            BookPage.mid("ignition_blast", "Ignition Blast", DemonFireArts::ignitionBlast).withAnim("out"),
            BookPage.mid("black_flame_pillar", "Pillar of Black Flame", DemonFireArts::pillar).withAnim("slam"),
            BookPage.mid("hellfire_wave", "Hellfire Wave", DemonFireArts::wave).withAnim("sweep"),
            BookPage.mid("flame_step", "Flame Step", DemonFireArts::flameStep).withAnim("side"),
            BookPage.mid("flame_body", "Flame Body", DemonFireArts::flameBody).withAnim("up"),
            BookPage.zone("black_flame_twister", "Black Flame Twister", DemonFireArts::twister).withAnim("up"),
            BookPage.zone("black_flame_veil", "Veil of Black Flame", DemonFireArts::veil).withAnim("up"),
            BookPage.signature("demon_fire_apocalypse", "Demon Fire Apocalypse", DemonFireArts::apocalypse).withAnim("signature"),
            BookPage.daily("cinder_rebirth", "Cinder Rebirth", DemonFireArts::cinderRebirth).withCooldown(24000).withAnim("up"));

    public DemonFireBook() { super(MagicType.DEMON_FIRE, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
