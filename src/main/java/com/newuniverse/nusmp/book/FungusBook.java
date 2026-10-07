package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Fungus Magic: spore clouds, mycelium roots, and the heavy and towering mushrooms. */
public class FungusBook extends GrimoireBook {
    static final int COLOR = 0xFFC0A04A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("fungus_strike", "Spore Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.FUNGUS_FX1, null, ElementBook.NONE)),
            // 0.55: the real book (appended)
            BookPage.starter("spore_burst", "Spore Burst", FungusArts::sporeBurst),
            BookPage.starter("mycelium_roots", "Mycelium Roots", FungusArts::roots),
            BookPage.mid("heavy_mr_mushroom", "Fungus Magic: Heavy Mr. Mushroom", FungusArts::heavy).withCooldown(240),
            BookPage.mid("running_mr_mushroom", "Fungus Magic: Running Mr. Mushroom", FungusArts::running).withAnim("sweep"),
            BookPage.mid("talking_mr_mushroom", "Fungus Magic: Talking Mr. Mushroom", FungusArts::talking),
            BookPage.mid("spore_armor", "Spore Armor", FungusArts::armor).withCooldown(400),
            BookPage.mid("mushroom_wall", "Mushroom Wall", FungusArts::wall),
            BookPage.mid("mycelium_mending", "Mycelium Mending", FungusArts::mending),
            BookPage.zone("spore_cloud", "Spore Cloud", FungusArts::cloud),
            BookPage.signature("towering_mr_mushroom", "Fungus Magic: Towering Mr. Mushroom", FungusArts::tower).withAnim("signature"),
            BookPage.daily("mycelium_bloom", "Mycelium Bloom", FungusArts::bloom).withCooldown(24000));

    public FungusBook() { super(MagicType.FUNGUS, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Spore Armor: while it lasts, your melee hits poison and whoever strikes you is poisoned in turn. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < i.getOrCreateTag().getLong(FungusArts.ARMOR)) FungusArts.infect(target, 80, 0);
        return true;
    }

    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong(FungusArts.ARMOR) && source.getEntity() instanceof LivingEntity att
                && att != owner && !att.isAlliedTo(owner)) {
            amount.set(amount.get() * 0.85f);
            FungusArts.infect(att, 100, 1);
        }
        return true;
    }
}
