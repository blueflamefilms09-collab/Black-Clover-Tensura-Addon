package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Food Magic: cutlery strikes, pies and spoiled dishes, meals that heal and feed allies, and the wolf that eats spells. */
public class FoodBook extends GrimoireBook {
    static final int COLOR = 0xFFFFC04A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("food_strike", "Feast Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.FOOD_FX1, null, ElementBook.NONE)),
            // 0.55: the real book (appended)
            BookPage.starter("fork_skewer", "Giant Fork", FoodArts::forkSkewer),
            BookPage.starter("carving_cleave", "Carving Knife", FoodArts::carvingCleave).withAnim("sweep"),
            BookPage.mid("pie_barrage", "Pie Barrage", FoodArts::pieBarrage),
            BookPage.mid("rotten_feast", "Rotten Feast", FoodArts::rottenFeast).withCooldown(240),
            BookPage.mid("starving_rush", "Starving Rush", ElementBook.dash(8f, 9, false, VfxShape.FOOD_FX1, ElementBook.leech(2f))),
            BookPage.mid("hearty_meal", "Hearty Meal", FoodArts::heartyMeal).withCooldown(300),
            BookPage.mid("full_stomach", "Full Stomach", FoodArts::fullStomach).withCooldown(600),
            BookPage.mid("silver_platter", "Silver Platter", FoodArts::silverPlatter).withCooldown(300),
            BookPage.zone("banquet_table", "Banquet Table", FoodArts::banquetTable),
            BookPage.signature("glutton_s_banquet", "Glutton's Banquet", FoodArts::gluttonsBanquet).withAnim("signature"),
            BookPage.daily("midnight_feast", "Midnight Feast", FoodArts::midnightFeast).withCooldown(24000));

    public FoodBook() { super(MagicType.FOOD, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Full Stomach: while it lasts, projectiles that hit you are mostly eaten and heal you. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        if (owner.level().getGameTime() < i.getOrCreateTag().getLong(FoodArts.EAT_UNTIL) && source.is(DamageTypeTags.IS_PROJECTILE)) {
            float eaten = amount.get() * 0.6f;
            amount.set(amount.get() - eaten);
            BalanceLaw.heal(owner, eaten * 0.5f);
        }
        return true;
    }
}
