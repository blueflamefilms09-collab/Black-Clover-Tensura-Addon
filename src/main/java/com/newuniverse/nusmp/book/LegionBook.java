package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.balance.BalanceLaw;
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

/** Legion Magic: summoned chess-piece soldiers that strike, hold, guard and swap places with the mage (spells in LegionArts). */
public class LegionBook extends GrimoireBook {
    static final int COLOR = 0xFFD0C080;
    public static final String CHESSBOARD_ID = "legion_chessboard";

    private final List<BookPage> pages = List.of(
            BookPage.starter("legion_strike", "Legion Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.LEGION_FX1, VfxShape.LEGION_FX3,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0)))),
            // 0.55: the real book (appended)
            BookPage.starter("pawn_march", "Pawn March", LegionArts::pawnMarch),
            BookPage.mid("muster_soldiers", "Muster Soldiers", LegionArts::muster),
            BookPage.mid("knight_gambit", "Knight Gambit", LegionArts::knightGambit),
            BookPage.mid("soldier_snare", "Soldier Snare", LegionArts::snare),
            BookPage.mid("commanders_banner", "Commander's Banner", LegionArts::banner).withCooldown(400),
            BookPage.mid("rook_barricade", "Rook Barricade", LegionArts::barricade).withAnim("slam").withCooldown(240),
            BookPage.mid("blink_castling", "Blink Castling", LegionArts::blinkCastling).withCooldown(100),
            BookPage.zone("gehenna_game", "Gehenna Game", LegionArts::gehenna),
            BookPage.signature("endless_domination", "Endless Domination", LegionArts::domination).withAnim("signature"),
            BookPage.daily("end_empress", "End Empress", LegionArts::endEmpress).withAnim("signature").withCooldown(24000),
            BookPage.starter(CHESSBOARD_ID, "Chessboard Toggle", (b, i, p, mode) -> true));

    public LegionBook() { super(MagicType.LEGION, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Commander's Banner: while it flies, your melee blows deal extra damage. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("LegionRallyUntil")) {
            amount.set(amount.get() + BalanceLaw.damage(target, tag.getFloat("LegionRallyBonus"), masteryFrac(i)));
        }
        return true;
    }
}
