package com.newuniverse.nusmp.book;

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

/** Butoh Magic: dance magic. Water blades, rhythm waves that buff allies and jam enemies, evasion steps, the sea god's slash and descent. */
public class ButohBook extends GrimoireBook {
    static final int COLOR = 0xFFFF5A8A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("butoh_strike", "Dance Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.BUTOH_FX1, null, ElementBook.NONE)),
            BookPage.starter("butoh_flowing_blade", "Flowing Water Blade", ButohArts::flowingBlade).withAnim("sweep"),
            BookPage.mid("butoh_rhythm_wave", "Rhythm Wave", ButohArts::rhythmWave),
            BookPage.mid("butoh_sea_god_slash", "Union Magic: Sea God Slash", ButohArts::seaGodSlash).withAnim("sweep"),
            BookPage.mid("butoh_ebb_step", "Ebb Step", ButohArts::ebbStep).withAnim("side").withCooldown(100),
            BookPage.mid("butoh_trance_dance", "Trance Dance", ButohArts::tranceDance).withAnim("up").withCooldown(900),
            BookPage.mid("butoh_tidal_veil", "Tidal Veil", ButohArts::tidalVeil).withCooldown(300),
            BookPage.zone("butoh_undertow", "Undertow", ButohArts::undertow).withAnim("slam"),
            BookPage.zone("butoh_metronome_field", "Metronome Field", ButohArts::metronomeField).withAnim("up"),
            BookPage.signature("butoh_sea_god_descent", "Sea God's Descent", ButohArts::seaGodDescent).withAnim("signature"),
            BookPage.daily("butoh_seabed_hymn", "Seabed Hymn", ButohArts::seabedHymn).withCooldown(12000).withAnim("up"),
            // 1.43.0 (Kiato): the dance state and the deflect dash, appended last so no page moves. The dance page toggles (cast again to stop);
            // its release clip is the plain side pose, the looping dance clips come from butoh.DanceChoreographer / client.butoh.DanceClient.
            new BookPage("butoh_dance", "Butoh Dance", "Butoh Dance", 4, 100, 40, ButohArts::dance, "side"),
            BookPage.mid("butoh_dash_deflect", "Deflect Dash", ButohArts::dashDeflect).withAnim("side")
                    .withCooldown(com.newuniverse.nusmp.butoh.DeflectMath.COOLDOWN_TICKS));

    public ButohBook() { super(MagicType.BUTOH, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Trance Dance: melee hits deal a bonus while it lasts. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        var tag = i.getOrCreateTag();
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < tag.getLong("EmpowerUntil")) {
            amount.set(amount.get() + com.newuniverse.nusmp.balance.BalanceLaw.damage(target, tag.getFloat("EmpowerBonus"), masteryFrac(i)));
        }
        // 1.43.0: Butoh pages cast while the dance is on hit harder (+15%, config butoh.danceCastBonus), never past the PvP per-hit cap
        float bonus = com.newuniverse.nusmp.butoh.ButohDance.castBonus(owner);
        if (bonus > 1f && (source.is(TensuraDamageTypes.WATER_ELEMENTAL) || source.is(damageType()))) {
            float boosted = amount.get() * bonus;
            if (target instanceof net.minecraft.world.entity.player.Player)
                boosted = Math.min(boosted, (float) (target.getMaxHealth() * com.newuniverse.nusmp.NUGameRules.pvpHitCap(target.level())));
            amount.set(Math.max(amount.get(), boosted));
        }
        return true;
    }

    /** Trance Dance: one blow in five finds nobody there. Tidal Veil: attackers are splashed back for 2. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        super.onTakenDamage(i, owner, source, amount);
        long now = owner.level().getGameTime();
        var tag = i.getOrCreateTag();
        if (now < tag.getLong(ButohArts.TRANCE) && owner.getRandom().nextFloat() < 0.2f) amount.set(0f);
        if (now < tag.getLong(ButohArts.GUARD) && source.getEntity() instanceof LivingEntity att && att != owner
                && !source.is(DamageTypes.THORNS)) {
            att.hurt(owner.damageSources().thorns(owner), 2f);
        }
        return true;
    }
}
