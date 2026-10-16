package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: True Soul Corpse Magic. Minecraft cannot bring back a player who has reached the respawn screen, so the pact works inside the
 * death event, while the death can still be cancelled: if the dying player is covered by an armed pact ({@link PactRules}: the caster,
 * or one of up to three tapped players, once each) the death is cancelled, they come back at 40% health with bad effects cleared and a
 * moment of invulnerability, on the same spot, as a Soul Corpse: for {@code corpse.reviveDecay} seconds they wear the decay overlay,
 * carry 25% less maximum health and cannot be saved again. The pact caster pays mana at that moment (through {@link CorpseMana}); if
 * they cannot, the death goes ahead. A boss is never saved, and neither is a death that bypasses invulnerability (the void, a kill
 * command), because those cannot be undone from inside the event.
 * <p>
 * The pact state is in memory: a server stop ends every pact (documented). A second window re-raises the caster's fallen pets and
 * summons (see {@link Corpses#reraisePets}).
 */
public final class Pacts {
    private Pacts() {}

    static final PactRules<UUID> RULES = new PactRules<>();
    /** Caster id -> game time until which a pet of theirs that dies is re-raised on the spot. */
    private static final Map<UUID, Long> PET_WINDOW = new HashMap<>();

    private static final ResourceLocation CUT_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "soul_corpse_revived");
    private static final String UNTIL = "nusmp_corpse_revived_until";

    // ---------------------------------------------------------------- arming and tapping
    /** Arms the caster's pact; false if one is already running. Also opens the pet window. */
    static boolean arm(ServerPlayer caster) {
        long now = caster.level().getGameTime();
        if (!RULES.arm(caster.getUUID(), now, CorpseConfig.pactTicks())) return false;
        PET_WINDOW.put(caster.getUUID(), now + CorpseConfig.petWindowTicks());
        return true;
    }

    static PactRules.Tap tap(ServerPlayer caster, ServerPlayer ally) {
        return RULES.tap(caster.getUUID(), ally.getUUID(), caster.level().getGameTime());
    }

    static boolean armed(ServerPlayer caster) { return RULES.armed(caster.getUUID(), caster.level().getGameTime()); }

    static boolean disarm(ServerPlayer caster) {
        PET_WINDOW.remove(caster.getUUID());
        return RULES.disarm(caster.getUUID());
    }

    static boolean petWindowOpen(UUID caster, long now) {
        Long end = PET_WINDOW.get(caster);
        return end != null && now < end;
    }

    /** True if 'e' is a player in 'caster''s pact (so the caster's corpses never treat them as a foe). */
    static boolean covers(UUID caster, Entity e) {
        return e != null && RULES.allies(caster).contains(e.getUUID());
    }

    // ---------------------------------------------------------------- the death
    /** Runs first among the death listeners (HIGH): cancels a covered player's death and revives them. */
    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer dying) || dying.level().isClientSide) return;
        if (!CorpseConfig.enabled() || !CorpseConfig.revivePlayers()) return;
        long now = dying.level().getGameTime();
        boolean boss = BossIdentity.isBoss(dying);
        boolean unsaveable = e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        if (RULES.check(dying.getUUID(), boss, unsaveable, now) != PactRules.Verdict.REVIVE) return;
        MinecraftServer server = dying.getServer();
        if (server == null) return;
        UUID casterId = RULES.pactFor(dying.getUUID(), now);
        ServerPlayer payer = casterId == null ? null : server.getPlayerList().getPlayer(casterId);
        if (payer == null || (payer != dying && (!payer.isAlive() || payer.level() != dying.level() || payer.distanceToSqr(dying) > sq(CorpseConfig.pactRange())))) return;
        if (!CorpseMana.pay(payer, CorpseConfig.reviveManaPercent())) {
            payer.displayClientMessage(Component.literal("Your pact cannot pay for a soul: not enough mana.").withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        RULES.commit(dying.getUUID(), now, CorpseConfig.reviveDecayTicks());
        e.setCanceled(true);
        revive(dying, now);
        if (payer != dying) payer.displayClientMessage(Component.literal(dying.getGameProfile().getName() + " was pulled back by your pact.").withStyle(ChatFormatting.GRAY), true);
    }

    private static double sq(double v) { return v * v; }

    /** The revival itself: 40% health, bad effects gone, a moment of safety, the decay look and the maximum health cut. */
    private static void revive(ServerPlayer p, long now) {
        applyCut(p);
        for (MobEffectInstance inst : new ArrayList<>(p.getActiveEffects()))
            if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) p.removeEffect(inst.getEffect());
        p.clearFire();
        p.setAirSupply(p.getMaxAirSupply());
        p.fallDistance = 0f;
        p.setHealth((float) Math.max(1.0, p.getMaxHealth() * CorpseConfig.reviveHealth()));
        p.invulnerableTime = 60;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4, false, false, false));
        long end = now + CorpseConfig.reviveDecayTicks();
        p.getPersistentData().putLong(UNTIL, end);
        Corpses.mark(p, 0);
        ServerLevel sl = p.serverLevel();
        CorpseVfx.pact(sl, p, 40, true);
        CorpseVfx.raise(sl, p.position(), 1.3f, 36);
        sl.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.4f, 0.6f);
        p.displayClientMessage(Component.literal("You rise again as a Soul Corpse. The pact will not save you twice.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private static void applyCut(ServerPlayer p) {
        AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;
        max.removeModifier(CUT_ID);
        max.addTransientModifier(new AttributeModifier(CUT_ID, -CorpseConfig.reviveMaxHealthCut(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void endDecay(ServerPlayer p) {
        AttributeInstance max = p.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) max.removeModifier(CUT_ID);
        p.getPersistentData().remove(UNTIL);
        Corpses.unmark(p);
    }

    // ---------------------------------------------------------------- ticking
    /** Every half second: refresh the decay look of the revived, free those whose time is up, drop dead pacts. */
    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 5) return;
        long now = server.overworld().getGameTime();
        for (UUID id : RULES.decayingIds(now)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive()) continue;
            long end = RULES.decayEnd(id);
            int life = CorpseConfig.reviveDecayTicks();
            Corpses.mark(p, CorpseDecay.stage(end - life, now, life));
        }
        for (UUID id : RULES.prune(now)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) endDecay(p);
        }
        PET_WINDOW.values().removeIf(end -> now >= end);
    }

    static void onRespawn(ServerPlayer p) {
        RULES.clearDecay(p.getUUID());
        endDecay(p);
    }

    /** A player who logs back in mid-decay gets the look and the cut back; one whose time ran out while away is freed. */
    static void onLogin(ServerPlayer p) {
        long end = p.getPersistentData().getLong(UNTIL);
        long now = p.level().getGameTime();
        if (end > now) {
            RULES.restoreDecay(p.getUUID(), end);
            applyCut(p);
        } else {
            endDecay(p);
        }
    }

    static void clear() {
        RULES.clear();
        PET_WINDOW.clear();
    }

    /** Nearest player the caster is looking at within 'range' blocks (for tapping an ally), or null. */
    static ServerPlayer lookedAt(ServerPlayer caster, double range) {
        net.minecraft.world.entity.LivingEntity t = com.newuniverse.nusmp.book.GrimoireBook.target(caster, range);
        return t instanceof ServerPlayer sp ? sp : null;
    }

    /** One line for the player: whether the pact is armed, for how long and how many allies it holds. */
    static String status(ServerPlayer caster) {
        long now = caster.level().getGameTime();
        if (!RULES.armed(caster.getUUID(), now)) return "No pact is armed.";
        return "Pact armed: " + RULES.remaining(caster.getUUID(), now) / 20 + "s left, " + RULES.allies(caster.getUUID()).size() + "/" + PactRules.MAX_ALLIES + " allies tapped.";
    }
}
