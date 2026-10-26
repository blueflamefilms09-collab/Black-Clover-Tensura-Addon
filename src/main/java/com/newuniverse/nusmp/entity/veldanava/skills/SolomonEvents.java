package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: the passives of Solomon act inside the game's own events (a blow about to land, a projectile about to hit, an effect about to land), so this is the one subscriber for them, wired once from NUSMP with
 * {@link #register}. It reads the player's {@link JudgmentState} mask (one int, no skill walk) through {@link JudgmentState#onFor}; every handler returns on its first line for a creature that is not an owner of
 * Solomon with the right toggle on. Server side only. Rules (docs/veldanava_skills.md):
 * <ul>
 *   <li><b>Omnipotent Magic</b>: a magic blow the owner deals is not reduced (armour, resistance, enchantments and the target's own resistance are ignored: the damage is the one before them, still limited by the
 *       Balance Law), and a magic blow the owner takes is foreseen: a share of it ({@link SolomonRules#FORESIGHT}, 40% mastered) is not taken.</li>
 *   <li><b>Mana Skin</b>: a blow that would kill is paid for in Magicule, ten for each point, as much as the pool allows.</li>
 *   <li><b>Elemental Sovereignty</b>: no fire, lava, freezing or poison blow lands, no poison takes hold, and an elemental projectile that would hit the owner is absorbed into Magicule (at most ten a second).</li>
 * </ul>
 * Everything is dropped at death, logout and server stop.
 */
public final class SolomonEvents {
    private SolomonEvents() {}

    private static final int CODE = SolomonRules.CODE;

    public static void register(IEventBus bus) {
        bus.addListener(SolomonEvents::onEffect);
        bus.addListener(SolomonEvents::onIncoming);
        bus.addListener(SolomonEvents::onProjectile);
        bus.addListener(EventPriority.LOWEST, SolomonEvents::onDamagePre);
        bus.addListener(SolomonEvents::onDeath);
        bus.addListener(SolomonEvents::onLogout);
        bus.addListener(SolomonEvents::onServerStopping);
    }

    private static boolean serverSide(LivingEntity e) { return !e.level().isClientSide; }

    private static String damagePath(DamageSource src) {
        return src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    // ---------------------------------------------------------------- Elemental Sovereignty: poison does not take hold
    static void onEffect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || !JudgmentState.onFor(p, CODE, 3)) return;
        MobEffectInstance inst = e.getEffectInstance();
        if (inst != null && inst.getEffect().is(MobEffects.POISON)) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ---------------------------------------------------------------- hazards and foresight on the blow about to land
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || e.getAmount() <= 0f || JudgmentState.skill(p) != CODE) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (JudgmentState.onFor(p, CODE, 3) && SolomonRules.hazard(damagePath(src))) {
            e.setCanceled(true);
            return;
        }
        if (JudgmentState.onFor(p, CODE, 1) && src.getEntity() != null && AntiMagic.isMagic(src)) {
            double share = JudgmentState.mastered(p) ? SolomonRules.FORESIGHT_MASTERED : SolomonRules.FORESIGHT;
            e.setAmount((float) (e.getAmount() * (1.0 - share)));
            if (SolomonState.foresightMessage(p))
                p.displayClientMessage(Component.literal("Foreseen: " + (int) Math.round(share * 100.0) + "% of the spell is turned aside.").withStyle(ChatFormatting.GOLD), true);
        }
    }

    // ---------------------------------------------------------------- elemental projectiles become Magicule
    static void onProjectile(ProjectileImpactEvent e) {
        HitResult hit = e.getRayTraceResult();
        if (!(hit instanceof EntityHitResult eh) || !(eh.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || !JudgmentState.onFor(p, CODE, 3)) return;
        Projectile proj = e.getProjectile();
        if (proj.getOwner() == p || !proj.isAlive()) return;
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(proj.getType());
        if (key == null || !SolomonRules.elementalProjectile(key.getPath()) || !SolomonState.takeAbsorb(p)) return;
        e.setCanceled(true);
        SolomonFx.absorb(p.serverLevel(), p, proj.position());
        proj.discard();
        TensuraBridge.refundMagicule(p, SolomonRules.absorbGain(TensuraBridge.maxMagicule(p)));
    }

    // ---------------------------------------------------------------- Mana Skin and Omnipotent Magic, after armour and the other mods have had their say
    static void onDamagePre(LivingDamageEvent.Pre e) {
        LivingEntity who = e.getEntity();
        if (!serverSide(who) || !JudgmentConfig.enabled() || e.getNewDamage() <= 0f) return;
        DamageSource src = e.getSource();
        if (JudgmentConfig.solomonBypass() && src.getEntity() instanceof ServerPlayer atk && JudgmentState.onFor(atk, CODE, 1) && AntiMagic.isMagic(src) && e.getNewDamage() < e.getOriginalDamage())
            e.setNewDamage(BalanceLaw.cap(who, e.getOriginalDamage()));
        if (who instanceof ServerPlayer p && JudgmentState.onFor(p, CODE, 2) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            SolomonRules.Skin skin = SolomonRules.manaSkin(e.getNewDamage(), p.getHealth(), p.getAbsorptionAmount(), TensuraBridge.magicule(p));
            if (skin.cost() > 0.0 && TensuraBridge.spendMagicule(p, skin.cost())) {
                e.setNewDamage(skin.damage());
                SolomonFx.skin(p.serverLevel(), p);
                p.displayClientMessage(Component.literal("Mana Skin: " + (long) skin.cost() + " magicules paid for your life.").withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    // ---------------------------------------------------------------- housekeeping
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SolomonState.leave(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) SolomonState.leave(p);
    }

    static void onServerStopping(ServerStoppingEvent e) { SolomonState.clearAll(); }
}
