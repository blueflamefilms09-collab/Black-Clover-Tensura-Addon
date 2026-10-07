package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Gel Magic: sticky gel that slows and holds, bouncy pads, an absorbing shield, the gel salamanders. */
public class GelBook extends GrimoireBook {
    static final int COLOR = 0xFF7AFFD8;

    private final List<BookPage> pages = List.of(
            BookPage.starter("gel_strike", "Gel Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.GEL_FX1, VfxShape.GEL_FX3,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1)))),
            // 0.55: the real book (appended)
            BookPage.starter("sticky_gel", "Sticky Gel", GelArts::stickyGel),
            BookPage.mid("gel_burst", "Gel Burst", GelArts::gelBurst).withAnim("slam"),
            BookPage.mid("bouncy_gel_pad", "Bouncy Gel Pad", GelArts::bouncyPad),
            BookPage.mid("gel_smother", "Gel Smother", GelArts::smother).withCooldown(240),
            BookPage.mid("gel_antidote", "Gel Antidote", GelArts::antidote),
            BookPage.mid("absorbing_gel", "Absorbing Gel", GelArts::shield).withCooldown(260),
            BookPage.mid("gel_skin", "Gel Skin", GelArts::skin).withCooldown(400),
            BookPage.zone("gel_mire", "Gel Mire", GelArts::mire),
            BookPage.zone("sticky_salamander", "Gel Magic: Sticky Salamander", (b, i, p, m) -> GelArts.salamander(b, i, p, m, false)),
            BookPage.signature("huge_sticky_salamander", "Gel Magic: Huge Sticky Salamander", (b, i, p, m) -> GelArts.salamander(b, i, p, m, true)).withAnim("signature"),
            BookPage.daily("jelly_cocoon", "Jelly Cocoon", GelArts::cocoon).withCooldown(24000));

    public GelBook() { super(MagicType.GEL, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Gel Skin: while it lasts, your melee hits leave the target stuck in gel. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < i.getOrCreateTag().getLong("GelSkinUntil")) GelArts.stick(target, 60, 1);
        return true;
    }
}
