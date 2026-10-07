package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import com.newuniverse.nusmp.balance.BalanceLaw;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Barrier Magic: domes, walls, deflection and voodoo regalia. */
public class BarrierBook extends GrimoireBook {
    static final int COLOR = 0xFF6AD0FF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("barrier_strike", "Barrier Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BARRIER_FX1, null, ElementBook.NONE)),
            // 0.54: the real book, appended after the first page (starter, mid, zone, signature, daily)
            BookPage.starter("hex_shards", "Hex Shards", ElementBook.volley(3, 4.5f, 1.5, true, VfxShape.BARRIER_FX1,
                    ElementBook.all(ElementBook.knock(0.3), ElementBook.effect(() -> new MobEffectInstance(MobEffects.WEAKNESS, 60, 0))))),
            BookPage.starter("barrier_step", "Barrier Step", ElementBook.dash(4, 8, false, VfxShape.BARRIER_FX3,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)))).withAnim("side"),
            BookPage.mid("voodoo_regalia", "Voodoo Regalia", BarrierArts::regalia).withAnim("out"),
            BookPage.mid("barrier_cannon", "Barrier Cannon", BarrierArts::cannon),
            BookPage.mid("hexagon_wall", "Hexagon Wall", BarrierArts::wall).withAnim("slam"),
            BookPage.mid("deflecting_pane", "Deflecting Pane", BarrierArts::deflect),
            BookPage.mid("barrier_armour", "Barrier Armour", BarrierArts::armour),
            BookPage.mid("spatial_lock", "Spatial Lock", BarrierArts::lock),
            BookPage.zone("voodoo_shangrila", "Voodoo Shangrila", BarrierArts::shangrila).withAnim("slam"),
            BookPage.signature("voodoo_kingdom", "Voodoo Kingdom", BarrierArts::kingdom).withAnim("signature"),
            BookPage.daily("barrier_reclaim", "Barrier Reclaim", BarrierArts::reclaim).withCooldown(6000));

    public BarrierBook() { super(MagicType.BARRIER, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Voodoo Regalia: the barrier weapon you keep in hand adds to your melee hits for a few seconds. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("EmpowerUntil")) {
            amount.set(amount.get() + BalanceLaw.damage(target, tag.getFloat("EmpowerBonus"), masteryFrac(i)));
        }
        return true;
    }
}
