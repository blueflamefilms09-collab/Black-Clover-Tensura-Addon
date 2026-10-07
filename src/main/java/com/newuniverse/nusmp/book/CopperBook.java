package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Copper Magic: red shine mace, current that jumps between bodies, rust and copper plating. */
public class CopperBook extends GrimoireBook {
    static final int COLOR = 0xFFFF9A4A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("copper_strike", "Copper Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.COPPER_FX1, null, ElementBook.NONE)),
            BookPage.starter("conduction_arc", "Conduction Arc", CopperArts::arc),
            BookPage.mid("red_shine_mace", "Red Shine Mace", CopperArts::mace).withAnim("slam"),
            BookPage.mid("copper_wire_bind", "Copper Wire Bind", CopperArts::wireBind),
            BookPage.mid("verdigris_blight", "Verdigris Blight", CopperArts::verdigris),
            BookPage.mid("rail_dash", "Rail Dash", CopperArts.railDash()),
            BookPage.mid("galvanic_edge", "Galvanic Edge", CopperArts::galvanicEdge),
            BookPage.mid("copper_plating", "Copper Plating", CopperArts::plating),
            BookPage.mid("copper_bulwark", "Copper Bulwark", CopperArts::bulwark),
            BookPage.zone("conduction_field", "Conduction Field", CopperArts::field),
            BookPage.signature("copper_tempest", "Copper Tempest", CopperArts::tempest).withAnim("signature"),
            BookPage.daily("plating_restoration", "Plating Restoration", CopperArts::restoration).withCooldown(24000));

    /** Set while a Galvanic Edge arc deals its own damage, so the arc cannot trigger another arc. */
    private static boolean arcing;

    public CopperBook() { super(MagicType.COPPER, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Galvanic Edge: melee hits deal extra and a share of the blow arcs to the nearest other foe. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (arcing || source.getDirectEntity() != owner || !(owner instanceof ServerPlayer p) || owner.level().getGameTime() >= tag.getLong("EdgeUntil")) return true;
        amount.set(amount.get() + BalanceLaw.damage(target, 4f, masteryFrac(i)));
        LivingEntity other = null;
        for (LivingEntity o : around(p, target.getBoundingBox().getCenter(), 5))
            if (o != target && (other == null || o.distanceToSqr(target) < other.distanceToSqr(target))) other = o;
        if (other != null) {
            arcing = true;
            try { hurt(i, p, other, tag.getInt("EdgeMode"), 4f); } finally { arcing = false; }
            CopperArts.slow(other, 30, 1);
            vfx(p, VfxShape.COPPER_FX1, target.getBoundingBox().getCenter(), other.getBoundingBox().getCenter(), 8, 0.5f);
        }
        return true;
    }

    /** Copper Plating: physical blows are cut by a third and the attacker is shocked. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() >= i.getOrCreateTag().getLong("PlateUntil")) return true;
        if (!com.newuniverse.nusmp.antimagic.AntiMagic.isMagic(source)) amount.set(amount.get() * 0.67f);
        if (source.getEntity() instanceof LivingEntity att && att != owner && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
            att.hurt(owner.damageSources().thorns(owner), 3f);
            CopperArts.slow(att, 20, 1);
        }
        return true;
    }
}
