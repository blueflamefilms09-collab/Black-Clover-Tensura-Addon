package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** Cherry Blossom Magic: razor petals that blind and cut, clones and veils of petals, petals that mend, up to the Dance of 100 Million Cherry Blossoms (see CherryBlossomArts). */
public class CherryBlossomBook extends GrimoireBook {
    static final int COLOR = 0xFFFFB0D0;

    private final List<BookPage> pages = List.of(
            BookPage.starter("cherry_blossom_strike", "Sakura Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CHERRY_BLOSSOM_FX1, null, CherryBlossomArts.cut())),
            BookPage.starter("razor_petal_stream", "Cherry Blossom: Razor Petal Stream", ElementBook.volley(6, 3.2f, 1.5, true, VfxShape.CHERRY_BLOSSOM_FX1, CherryBlossomArts.cut())),
            BookPage.mid("petal_fan", "Cherry Blossom: Petal Fan", CherryBlossomArts::fan).withAnim("sweep"),
            BookPage.mid("blossom_snare", "Cherry Blossom: Blossom Snare", CherryBlossomArts::snare),
            BookPage.mid("petal_veil", "Cherry Blossom: Petal Veil", CherryBlossomArts::veil).withAnim("up"),
            BookPage.mid("petal_step", "Cherry Blossom: Petal Step", CherryBlossomArts::step),
            BookPage.mid("beautifying_bloom", "Cherry Blossom: Beautifying Bloom", CherryBlossomArts::bloom).withAnim("up"),
            BookPage.mid("blossom_blessing", "Cherry Blossom: Blossom Blessing", ElementBook.empower(400, 4f, false, VfxShape.CHERRY_BLOSSOM_FX2,
                    () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0), () -> new MobEffectInstance(MobEffects.REGENERATION, 400, 0))),
            BookPage.mid("clones_of_the_beautiful_me", "Cherry Blossom Magic: Clones of the Beautiful Me", CherryBlossomArts::clones).withAnim("up"),
            BookPage.zone("magic_cherry_blossom_blizzard", "Cherry Blossom Magic: Magic Cherry Blossom Blizzard", CherryBlossomArts::blizzard).withAnim("up"),
            BookPage.signature("dance_of_100_million_cherry_blossoms", "Cherry Blossom Magic: Dance of 100 Million Cherry Blossoms", CherryBlossomArts::dance).withAnim("signature"),
            BookPage.daily("everlasting_sakura_grove", "Cherry Blossom: Everlasting Sakura Grove", CherryBlossomArts::grove).withCooldown(24000).withAnim("signature"));

    public CherryBlossomBook() { super(MagicType.CHERRY_BLOSSOM, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
