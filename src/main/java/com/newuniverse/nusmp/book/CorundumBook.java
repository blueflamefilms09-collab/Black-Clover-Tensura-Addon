package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
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

/** Corundum Magic: hard gem blasts, cracking shards, gem armour and the Ideal Closer. */
public class CorundumBook extends GrimoireBook {
    static final int COLOR = 0xFFFF6A7A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("corundum_strike", "Corundum Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.CORUNDUM_FX1, null, ElementBook.NONE)),
            // 0.54: the full grimoire, appended after the first page
            BookPage.starter("ruby_shards", "Ruby Shards", ElementBook.volley(3, 4f, 1.7, false, VfxShape.CORUNDUM_FX1, ElementBook.ignite(3))),
            BookPage.starter("sapphire_lance", "Sapphire Lance", ElementBook.line(7f, 18, 1.0, VfxShape.CORUNDUM_FX1,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2)))),
            BookPage.mid("corundum_fracture", "Corundum Fracture", CorundumArts::fracture),
            BookPage.mid("hard_blast", "Hard Blast", ElementBook.nova(10f, 5, true, VfxShape.CORUNDUM_FX3,
                    ElementBook.all(ElementBook.knock(1.2), ElementBook.lift(0.4)))),
            BookPage.mid("corundum_rush", "Corundum Rush", ElementBook.dash(6f, 9, false, VfxShape.CORUNDUM_FX1, ElementBook.knock(0.8))),
            BookPage.mid("gem_wall", "Gem Wall", CorundumArts::gemWall),
            BookPage.mid("gem_armour", "Gem Armour", ElementBook.empower(400, 3f, true, VfxShape.CORUNDUM_FX3,
                    () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, 1),
                    () -> new MobEffectInstance(MobEffects.ABSORPTION, 400, 1))),
            BookPage.zone("corundum_bastion", "Corundum Bastion", CorundumArts::bastion),
            BookPage.signature("ideal_closer", "Ideal Closer", CorundumArts::idealCloser).withAnim("signature"),
            BookPage.daily("star_corundum", "Star Corundum", CorundumArts::starCorundum).withCooldown(6000),
            // 0.54: the summoned models and the gem armour layer
            BookPage.mid("ideal_closer_fist", "Ideal Closer Fist", CorundumSummons::idealCloserFist).withCooldown(40),
            BookPage.signature("gem_plate_armour", "Gem Plate Armour", CorundumSummons::gemPlateArmour).withAnim("signature"));

    public CorundumBook() { super(MagicType.CORUNDUM, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Gem Armour and the Ideal Closer: melee hits deal extra while the armour holds. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("EmpowerUntil"))
            amount.set(amount.get() + BalanceLaw.damage(target, tag.getFloat("EmpowerBonus"), masteryFrac(i)));
        return true;
    }

    /** The gem skin throws a little of every blow back at the attacker. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong("ThornsUntil") && source.getEntity() instanceof LivingEntity att
                && att != owner && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS))
            att.hurt(owner.damageSources().thorns(owner), 2f);
        return true;
    }
}
