package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.balance.BalanceLaw;
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

/** Glass Magic: shard volleys, stacking glass cuts, panes that reveal and reflect, the glass castle. */
public class GlassBook extends GrimoireBook {
    static final int COLOR = 0xFFE0FFFF;

    private final List<BookPage> pages = List.of(
            BookPage.starter("glass_strike", "Glass Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.GLASS_FX1, null, ElementBook.NONE)),
            // 0.55: the real book (appended)
            BookPage.starter("shard_volley", "Shard Volley", GlassArts::shardVolley),
            BookPage.starter("glass_lance", "Glass Lance", GlassArts::glassLance),
            BookPage.mid("verre_epee", "Glass Magic: Verre Epee", GlassArts::verreEpee).withCooldown(400).withAnim("sweep"),
            BookPage.mid("pane_step", "Pane Step", GlassArts::paneStep),
            BookPage.mid("verre_detection", "Glass Magic: Verre Detection", GlassArts::verreDetection),
            BookPage.mid("prism_barrier", "Prism Barrier", GlassArts::prismBarrier).withCooldown(260),
            BookPage.mid("glass_shatter", "Glass Shatter", GlassArts::shatter).withAnim("slam"),
            BookPage.zone("shard_rain", "Shard Rain", GlassArts::shardRain),
            BookPage.zone("verre_fleur", "Glass Creation Magic: Verre Fleur", GlassArts::verreFleur).withAnim("up"),
            BookPage.signature("le_chateau_de_verre", "Union Magic: Le Chateau de Verre", GlassArts::chateau).withAnim("signature"),
            BookPage.daily("crystal_carapace", "Crystal Carapace", GlassArts::carapace).withCooldown(24000));

    public GlassBook() { super(MagicType.GLASS, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Verre Epee: while the glass blade lasts, your melee hits cut deeper and leave a stacking bleed. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!GlassArts.busy && owner instanceof ServerPlayer p && source.getDirectEntity() == owner
                && owner.level().getGameTime() < i.getOrCreateTag().getLong("GlassBladeUntil")) {
            amount.set(amount.get() + BalanceLaw.damage(target, 4f, masteryFrac(i)));
            GlassArts.cut(this, i, p, 0, target, 1);
        }
        return true;
    }
}
