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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Briar Magic: rooting vines, bleeding thorn walls, the corpse-hunting briar tree and the queen of briars. */
public class BriarBook extends GrimoireBook {
    static final int COLOR = 0xFF8AFF4A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("briar_strike", "Briar Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BRIAR_FX1, null, ElementBook.NONE)),
            // 0.55: the real book (appended)
            BookPage.starter("thorn_lash", "Thorn Lash", BriarArts::lash).withAnim("sweep"),
            BookPage.starter("creeping_vines", "Creeping Vines", BriarArts::vines),
            BookPage.mid("bleeding_thorn_wall", "Bleeding Thorn Wall", BriarArts::wall),
            BookPage.mid("rose_scent", "Rose Scent", BriarArts::scent),
            BookPage.mid("thorn_reflect", "Thorn Reflect", BriarArts::dress).withCooldown(400),
            BookPage.mid("briar_leap", "Briar Leap", BriarArts::leap).withAnim("up"),
            BookPage.zone("briar_garden", "Briar Garden", BriarArts::garden),
            BookPage.signature("corpse_hunting_briar_tree", "Briar Creation Magic: Corpse-Hunting Briar Tree", BriarArts::tree).withAnim("signature"),
            BookPage.signature("queen_of_briars", "True Briar Magic: Queen of Briars", BriarArts::queen).withAnim("signature"),
            BookPage.daily("rose_garden_bloom", "Rose Garden Bloom", BriarArts::bloom).withCooldown(24000));

    public BriarBook() { super(MagicType.BRIAR, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Queen of Briars: while it lasts, your melee hits cut deeper, wound and feed on the victim's magicules. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < i.getOrCreateTag().getLong(BriarArts.QUEEN)) {
            amount.set(amount.get() + BalanceLaw.damage(target, 3f, masteryFrac(i)));
            BriarArts.wound(target, 60, 0);
            EnergyBridge.drain(target, owner, 0.004);
        }
        return true;
    }

    /** Thorn Reflect and the Queen's dress: whoever strikes you is pricked, wounded and loses part of the blow's worth. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        long now = owner.level().getGameTime();
        var tag = i.getOrCreateTag();
        if ((now < tag.getLong(BriarArts.REFLECT) || now < tag.getLong(BriarArts.QUEEN)) && source.getEntity() instanceof LivingEntity att
                && att != owner && !att.isAlliedTo(owner) && !source.is(DamageTypes.THORNS)) {
            float hit = amount.get();
            amount.set(hit * 0.85f);
            att.hurt(owner.damageSources().thorns(owner), Math.min(8f, 1.5f + hit * 0.4f));
            BriarArts.wound(att, 80, 0);
        }
        return true;
    }
}
