package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.antimagic.Nullification;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** Crystal Magic: bolts and shields of faceted crystal, prism beams, a far-seeing scope and shattering shards. */
public class CrystalBook extends GrimoireBook {
    static final int COLOR = 0xFFA0E0FF;

    private static ElementBook.Rider slow(int ticks, int amp) {
        return ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
    }

    private final List<BookPage> pages = List.of(
            // starter -> mid -> zone -> signature -> daily (the first page is the original stub, kept)
            BookPage.starter("crystal_strike", "Crystal Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CRYSTAL_FX1, null,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.GLOWING, 100, 0)))),
            BookPage.starter("crystal_shards", "Crystal Shards", ElementBook.volley(5, 4f, 1.5, false, VfxShape.CRYSTAL_FX1,
                    ElementBook.all(slow(30, 0), (t, p) -> Nullification.bleed(t, 0.005)))),
            BookPage.mid("prism_beam", "Prism Beam", CrystalArts::prism),
            BookPage.mid("shatter_burst", "Shatter Burst", CrystalArts::shatter),
            BookPage.mid("crystal_cage", "Crystal Cage", CrystalArts::cage),
            BookPage.mid("prism_step", "Prism Step", ElementBook.dash(7f, 10, false, VfxShape.CRYSTAL_FX1,
                    ElementBook.all(ElementBook.effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 40, 0)), slow(30, 1)))).withAnim("side"),
            BookPage.mid("crystal_scope", "Crystal Magic: Crystal Scope", CrystalArts::scope),
            BookPage.mid("crystal_armor", "Crystal Armor", ElementBook.empower(300, 4f, true, VfxShape.CRYSTAL_FX3,
                    () -> new MobEffectInstance(MobEffects.ABSORPTION, 300, 2), () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0))),
            BookPage.zone("crystal_garden", "Crystal Garden", CrystalArts::garden),
            BookPage.zone("prism_dome", "Prism Dome", CrystalArts::dome),
            BookPage.signature("grand_prism", "Grand Prism Cannon", CrystalArts::cannon).withAnim("signature"),
            BookPage.daily("crystal_resonance", "Crystal Resonance", CrystalArts::resonance).withCooldown(12000).withAnim("up"));

    public CrystalBook() { super(MagicType.CRYSTAL, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
