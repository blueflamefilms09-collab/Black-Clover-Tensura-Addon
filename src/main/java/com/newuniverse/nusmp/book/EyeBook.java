package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.List;

/** Eye Magic: sight that marks, blinds, weakens, reflects and paralyses. */
public class EyeBook extends GrimoireBook {
    static final int COLOR = 0xFFFF4A9A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("eye_strike", "Eye Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.EYE_FX1, null, ElementBook.NONE)),
            BookPage.starter("piercing_glance", "Piercing Glance", EyeArts::glance),
            BookPage.mid("seeker_eyes", "Seeker Eyes", ElementBook.volley(4, 5f, 1.25, true, VfxShape.EYE_FX1, EyeArts.dazzle())),
            BookPage.mid("mark_of_sight", "Mark of Sight", EyeArts::markOfSight),
            BookPage.mid("blinding_flash", "Blinding Flash", EyeArts::flash).withAnim("out"),
            BookPage.mid("evil_eye", "Evil Eye", EyeArts::evilEye),
            BookPage.mid("keen_sight", "Keen Sight", ElementBook.empower(600, 3f, false, VfxShape.EYE_FX3,
                    () -> new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0), () -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0))),
            BookPage.mid("iris_ward", "Iris Ward", EyeArts::ward),
            BookPage.mid("sight_step", "Sight Step", EyeArts::sightStep),
            BookPage.zone("field_of_eyes", "Field of Eyes", EyeArts::fieldOfEyes).withAnim("slam"),
            BookPage.signature("reflect_iris", "Union Magic: Reflect Iris", EyeArts::reflectIris).withAnim("signature"),
            BookPage.daily("all_seeing_eye", "All-Seeing Eye", EyeArts::allSeeing).withCooldown(12000).withAnim("up"));

    public EyeBook() { super(MagicType.EYE, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
