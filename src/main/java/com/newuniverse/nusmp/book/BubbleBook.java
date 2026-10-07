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

/** Bubble Magic: bubbles that trap and float enemies, burst and throw them up, shield, and refresh allies. */
public class BubbleBook extends GrimoireBook {
    static final int COLOR = 0xFFA0D8FF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("bubble_strike", "Bubble Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BUBBLE_FX1, null, ElementBook.NONE)),
            // 0.55: the real book (appended)
            BookPage.starter("bubble_barrage", "Bubble Barrage", ElementBook.volley(5, 3f, 1.3, false, VfxShape.BUBBLE_FX1,
                    ElementBook.all(ElementBook.lift(0.25), ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0))))),
            BookPage.mid("foam_blind", "Foam Blind", ElementBook.cone(4f, 7, 0.6, VfxShape.BUBBLE_FX1, BubbleArts.FOAM)),
            BookPage.mid("bubble_trap", "Bubble Trap", BubbleArts::trap).withCooldown(240),
            BookPage.mid("bubble_burst", "Bubble Burst", BubbleArts::burst).withAnim("slam"),
            BookPage.mid("bubble_ride", "Bubble Ride", BubbleArts::ride).withAnim("up"),
            BookPage.mid("bubble_barrier", "Bubble Barrier", BubbleArts::shield).withCooldown(260),
            BookPage.mid("bubble_skin", "Bubble Skin", BubbleArts::skin).withCooldown(400),
            BookPage.zone("bubble_refresher", "Bubble Healing Magic: Bubble Refresher", BubbleArts::refresher),
            BookPage.zone("bubble_mines", "Bubble Mines", BubbleArts::mines),
            BookPage.signature("great_bubble", "Great Bubble", BubbleArts::greatBubble).withAnim("signature"),
            BookPage.daily("rainbow_cocoon", "Rainbow Cocoon", BubbleArts::cocoon).withCooldown(24000));

    public BubbleBook() { super(MagicType.BUBBLE, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Bubble Skin: while it lasts, your melee hits pop a bubble under the target and throw it up. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < i.getOrCreateTag().getLong("BubbleSkinUntil")) {
            BubbleArts.knockUp(target, 0.6);
            if (!com.newuniverse.nusmp.balance.BalanceLaw.isBoss(target)) target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
        }
        return true;
    }
}
