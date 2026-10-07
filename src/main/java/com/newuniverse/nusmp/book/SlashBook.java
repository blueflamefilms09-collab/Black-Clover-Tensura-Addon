package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Slash Magic (0.45, new; Jack the Ripper; VIZ: "Severing Magic"). After the wiki: mana blades that "can slash through
 * anything", long blades from the forearms for close combat and projected slashes for range.
 * <ul>
 *   <li><b>Adaptive cutting</b> ({@link #adapt}): every Slash hit on a defended foe (armour 8+, toughness, an Aura barrier,
 *       Resistance or absorption) reads what it is made of and leaves a permanent mark on it (up to 10). Each mark adds 5%
 *       damage and ignores another tenth of its armour; at 10 its defence is severed outright (and Tensura fragility).</li>
 *   <li><b>Aura:</b> the forearm blades and the Ripper Dash are physical: they draw on Aura while they last.</li>
 * </ul>
 * Pages: Slash Wave, Forearm Blades, Ripper Dash, Death Scythe (wiki).
 */
public class SlashBook extends GrimoireBook {
    static final int GREEN = 0xFF48FF7A;
    static final String K_ADAPT = "nusmp_slash_adapt", K_BLADES = "nusmp_slash_blades_until";

    private final List<BookPage> pages = List.of(
            BookPage.starter("slash_wave", "Slash Wave", SlashBook::slashWave),
            BookPage.mid("forearm_blades", "Forearm Blades", SlashBook::forearmBlades).withCooldown(400),
            BookPage.mid("ripper_dash", "Ripper Dash", SlashBook::ripperDash).withCooldown(100),
            BookPage.signature("death_scythe", "Death Scythe", SlashBook::deathScythe).withCooldown(900));

    public SlashBook() { super(MagicType.SLASH, GREEN); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** How defended a target is (what the blades adapt to). */
    static boolean defended(LivingEntity t) {
        return t.getArmorValue() >= 8 || t.getAttributeValue(Attributes.ARMOR_TOUGHNESS) > 0 || EnergyBridge.auraBarrier(t)
                || t.hasEffect(MobEffects.DAMAGE_RESISTANCE) || t.getAbsorptionAmount() > 0;
    }

    /** One adaptive cut: reads the defence, marks it for good, and returns the damage after the marks so far. */
    public static float adapt(ServerPlayer p, LivingEntity t, float raw) {
        CompoundTag d = t.getPersistentData();
        int marks = d.getInt(K_ADAPT);
        if (defended(t) && marks < 10) {
            marks++;
            d.putInt(K_ADAPT, marks);
            if (marks == 10) {
                p.displayClientMessage(Component.literal("Their defence is severed.").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
                EnergyBridge.effect(t, "fragility", 200, 1);
            }
        }
        float out = raw * (1 + 0.05f * marks);
        return out * EnergyBridge.armourBypass(t, p.damageSources().generic(), out, marks / 10f);
    }

    static void cut(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float raw) {
        b.hurt(i, p, t, mode, adapt(p, t, raw * EnergyBridge.scale(p)));
    }

    /** Slash Wave: a jagged crescent of green mana flung from the forearm, cutting through everything in a line. */
    static boolean slashWave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        var hit = p.level().clip(new ClipContext(eye, eye.add(dir.scale(24)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        b.vfx(p, VfxShape.SLASH_WAVE, eye.add(0, -0.2, 0), hit.getLocation(), 12, 1.1f * EnergyBridge.scale(p));
        SpellRuntime.bolt(p, eye, dir.scale(2.4), 1.2, 10, true, null, (bolt, t) -> cut(b, i, p, t, mode, 8f), null);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
        return true;
    }

    /** Forearm Blades: long jagged blades along both forearms for 20 s; every melee blow cuts and adapts. Draws Aura. */
    static boolean forearmBlades(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!EnergyBridge.aura(p, 0.02, 6)) { fail(p, "Not enough aura to hold the blades."); return false; }
        ServerLevel level = p.serverLevel();
        p.getPersistentData().putLong(K_BLADES, level.getGameTime() + 400);
        VfxSpawn.sendFollowing(level, VfxShape.SLASH_BLADES, p, p.position(), GREEN, 400, 1f + 0.2f * EnergyBridge.scale(p));
        SpellRuntime.zone(level, 400, 20, age -> {                                    // the blades are held with aura
            if (age == 0 || p.getPersistentData().getLong(K_BLADES) <= level.getGameTime()) return;
            if (!EnergyBridge.aura(p, 0.01, 2)) {
                p.getPersistentData().putLong(K_BLADES, 0);
                p.displayClientMessage(Component.literal("Your aura gives out; the blades fade.").withStyle(ChatFormatting.GRAY), true);
            }
        });
        level.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1f, 1.5f);
        return true;
    }

    /** Melee blows with the forearm blades out cut harder and adapt (NeoForge event, any book). */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getDirectEntity() instanceof ServerPlayer p) || e.getSource().getEntity() != p) return;
        if (p.getPersistentData().getLong(K_BLADES) <= p.level().getGameTime() || e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) return;
        LivingEntity t = e.getEntity();
        e.setAmount(adapt(p, t, e.getAmount() + 5f * EnergyBridge.scale(p)));
        VfxSpawn.send(p.serverLevel(), VfxShape.SLASH_WAVE, p.getEyePosition(), t.getBoundingBox().getCenter(), GREEN, 8, 0.6f);
    }

    /** Ripper Dash: a burst of speed that carries you 10 blocks through your foes, cutting each one. Draws Aura. */
    static boolean ripperDash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!EnergyBridge.aura(p, 0.025, 8)) { fail(p, "Not enough aura to dash."); return false; }
        Vec3 look = p.getViewVector(1f).multiply(1, 0.2, 1).normalize(), from = p.position();
        var hit = p.level().clip(new ClipContext(p.getEyePosition(), p.getEyePosition().add(look.scale(10)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = hit.getLocation().subtract(look.scale(0.7)).subtract(0, p.getEyeHeight(), 0);
        for (LivingEntity t : along(p, from.add(0, 1, 0), to.add(0, 1, 0), 1.3)) cut(b, i, p, t, mode, 7f);
        b.vfx(p, VfxShape.SLASH_WAVE, from.add(0, 1, 0), to.add(0, 1, 0), 10, 1.4f);
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0;
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 0.8f);
        return true;
    }

    /** Death Scythe (wiki): a vast scythe of slash magic reaps everything around you in one sweep. */
    static boolean deathScythe(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 7 * GrimoireBook.size(i, p) * (0.9f + 0.1f * EnergyBridge.power(p));
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.SLASH_SCYTHE, p.position().add(0, 1, 0), p.position().add(p.getViewVector(1f)).add(0, 1, 0), 18, r);
        SpellRuntime.later(p.serverLevel(), 5, () -> {
            for (LivingEntity t : around(p, p.position(), r)) cut(b, i, p, t, mode, 15f);
        });
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.6f, 0.4f);
        return true;
    }
}
