package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/** Eyeball Magic: floating eyes that scan, slow and petrify, shoot eye bolts and share their sight. */
public class EyeballBook extends GrimoireBook {
    static final int COLOR = 0xFFFF3A3A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("eyeball_strike", "Eyeball Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.EYEBALL_FX1, VfxShape.EYEBALL_FX3,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0)))),
            BookPage.starter("sniper_eye", "Sniper Eye", EyeballArts::sniper),
            BookPage.mid("roving_eyes", "Roving Eyes", ElementBook.volley(4, 5f, 1.25, true, VfxShape.EYEBALL_FX1,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)))),
            BookPage.mid("petrifying_gaze", "Petrifying Gaze", EyeballArts::petrify),
            BookPage.mid("magicule_scan", "Magicule Scan", EyeballArts::scan).withAnim("out"),
            BookPage.mid("eye_swap", "Eye Swap", EyeballArts::swap),
            BookPage.mid("sight_share", "Sight Share", EyeballArts::sightShare),
            BookPage.mid("guardian_eyes", "Guardian Eyes", EyeballArts::guardians),
            BookPage.mid("scouting_eyes", "Scouting Eyes", EyeballArts::scouts),
            BookPage.zone("jamming_gaze_field", "Jamming Gaze Field", EyeballArts::jammingField).withAnim("slam"),
            BookPage.signature("catoblepas_s_evil_eye", "Catoblepas's Evil Eye", EyeballArts::evilEye).withAnim("signature"),
            BookPage.daily("swarm_of_eyes", "Swarm of Eyes", EyeballArts::swarm).withCooldown(12000).withAnim("up"));

    public EyeballBook() { super(MagicType.EYEBALL, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }
}
