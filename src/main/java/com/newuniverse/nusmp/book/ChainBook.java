package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** Chain Magic: binding, dragging and caging chains (wiki spells plus the whip, kunai and grapple chains). */
public class ChainBook extends GrimoireBook {
    static final int COLOR = 0xFFC0C8E0;

    private final List<BookPage> pages = List.of(
            BookPage.starter("chain_strike", "Chain Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CHAIN_FX1, null, ElementBook.NONE)),
            BookPage.starter("chain_whip", "Chain Whip", ElementBook.cone(7f, 6.5, 0.3, VfxShape.CHAIN_FX1,
                    ElementBook.all(ChainArts.drag(0.8), ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1))))).withAnim("sweep"),
            BookPage.starter("scorpio_chain", "Scorpio Chain", ChainArts::scorpio),
            BookPage.mid("pitless_viper", "Dance of the Pitless Viper", ChainArts::viper),
            BookPage.mid("kunai_chains", "Kunai Chains", ElementBook.volley(5, 5f, 1.5, true, VfxShape.CHAIN_FX1, ChainArts.barbs())),
            BookPage.mid("heavy_chain", "Heavy Chain", ChainArts::heavy).withAnim("slam"),
            BookPage.mid("grapple_chain", "Grapple Chain", ChainArts::grapple),
            BookPage.mid("chain_aegis", "Chain Aegis", ChainArts::aegis),
            BookPage.mid("chain_armament", "Chain Armament", ElementBook.empower(400, 3.5f, true, VfxShape.CHAIN_FX3,
                    () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 0))),
            BookPage.zone("chain_cage", "Chain Cage", ChainArts::cage),
            BookPage.signature("iron_chain_formation", "Magic Binding Iron Chain Formation", ChainArts::ironFormation).withAnim("signature"),
            BookPage.daily("chain_reckoning", "Chain Reckoning", ChainArts::reckoning).withCooldown(6000));

    public ChainBook() { super(MagicType.CHAIN, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
