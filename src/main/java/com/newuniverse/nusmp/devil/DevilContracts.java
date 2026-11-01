package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.book.ForbiddenMagic;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Why: the server half of the Devil Contract system (3.48.0), all decisions in one place and all of them the server's. A player with a contract can switch assimilation on with a key (an ask, {@link DevilNet.Toggle});
 * once a second the level climbs and drains magicules (or falls), the stat modifiers follow the level, and at 100 % the Devil Union begins: exactly {@code unionSeconds} (5 minutes), then the level drops to 0 and the
 * union is locked for {@code cooldownHours} in-game hours (24 = 24 000 ticks). The sacrifices (half of the maximum health, a lost eye) are kept in the contract, which is copied to the new body on death, and are applied
 * again on every login and respawn. Numbers and decisions are the pure {@link DevilRules}; this class only reads and writes the player.
 *
 * <p>Cost: for a player with no contract every handler is one {@code hasData} lookup. For a player with one the once-a-second step is a few additions, at most one magicule payment and at most three attribute
 * writes (each only when its value changed); no entity is spawned, no world is scanned, nothing is kept in a static map. The state of a player lives in the attachment and goes with the player.
 */
public final class DevilContracts {
    private DevilContracts() {}

    /** The persistent-data key the Anti-Magic Devil Union ({@code DevilFormLimit}) keeps its start time under: a value above 0 means that union is on. */
    static final String OLD_UNION_KEY = "nusmp_devil_form_since";

    private static final ResourceLocation STR = rl("devil_strength"), SPD = rl("devil_speed"), FLY = rl("devil_flight"), HALF = rl("devil_sacrifice_half_hp"), EYE = rl("devil_sacrifice_eye_reach");

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    public static void register(IEventBus bus) {
        bus.addListener(DevilContracts::onTick);
        bus.addListener(DevilContracts::onLogin);
        bus.addListener(DevilContracts::onLogout);
        bus.addListener(DevilContracts::onRespawn);
        bus.addListener(DevilContracts::onDimension);
        bus.addListener(DevilContracts::onDeath);
        bus.addListener(DevilContracts::onClone);
        bus.addListener(DevilContracts::onStartTracking);
    }

    static long time(ServerPlayer p) { return p.serverLevel().getServer().overworld().getGameTime(); }

    public static boolean oldUnionActive(ServerPlayer p) { return p.getPersistentData().getLong(OLD_UNION_KEY) > 0; }

    /** True (with a message) while the Devil Union of this system is on: the Anti-Magic Black Form can not begin then. Called by {@code AntiMagicBook}. */
    public static boolean refuseOldUnion(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.inUnion()) return false;
        msg(p, "union_blocks_black", "You are in the Devil Union. The Black Form will not answer.", ChatFormatting.RED);
        return true;
    }

    // ---------------------------------------------------------------- the contract
    /**
     * A devil was bound (called by {@code ForbiddenMagic.bind} after it succeeded): the tier comes from the price (the harder the price, the stronger the devil), the attribute from the magic of the grimoire, a new
     * price (half the health, an eye) is added to the sacrifices. The three old prices did what they always did.
     */
    public static void grant(ServerPlayer p, String price, String magic) {
        DevilContract c = DevilStore.of(p);
        c.tier = DevilRules.tierForPrice(price);
        c.attribute = DevilAttribute.forMagic(magic);
        Sacrifice s = DevilRules.sacrificeForPrice(price);
        if (s != null) c.sacrifices.add(s);
        c.resetLive();
        applySacrifices(p, c);
        applyStats(p, c);
        syncNow(p, c);
    }

    /** A pact that was made before 3.48.0 has no contract data: it gets one once, with the tier its price buys. */
    static void backfill(ServerPlayer p) {
        DevilContract have = DevilStore.peek(p);
        if (have != null && have.hasContract()) return;
        ForbiddenMagic.PactDevil devil = ForbiddenMagic.devilOf(p);
        if (devil == null) return;
        CompoundTag d = ForbiddenMagic.data(p);
        String price = d.getInt("nusmp_devil_hp") > 0 ? "maxhp" : d.getDouble("nusmp_mana_tax") > 0 ? "tax" : "seal";
        DevilContract c = DevilStore.of(p);
        c.tier = DevilRules.tierForPrice(price);
        c.attribute = switch (devil) { case IGNIVAR -> DevilAttribute.FLAME; case GRAVEMAW -> DevilAttribute.GRAVITY; case KAIROVORE -> DevilAttribute.TIME; default -> DevilAttribute.WORD; };
    }

    // ---------------------------------------------------------------- the key
    /** The ask of the key. Everything is checked here again; the client only said "toggle". */
    static void onToggleAsk(ServerPlayer p) {
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) return;
        long now = time(p);
        if (!DevilRules.askAllowed(c.lastAsk, now)) return;
        c.lastAsk = now;
        if (!DevilConfig.enabled()) { msg(p, "off", "The devil is silent.", ChatFormatting.GRAY); return; }
        if (c.inUnion()) { msg(p, "in_union", "The union holds you. It ends by itself.", ChatFormatting.GRAY); return; }
        if (oldUnionActive(p)) { msg(p, "old_union", "The Anti-Magic union is on. Only one devil at a time.", ChatFormatting.RED); return; }
        if (c.assimilating) {
            c.assimilating = false;
            msg(p, "withdraw", "You push the devil back.", ChatFormatting.GRAY);
        } else {
            if (!hasMana(p, drain(c, c.assimilation))) { msg(p, "no_mana", "Not enough magicules to hold a devil.", ChatFormatting.RED); return; }
            c.assimilating = true;
            msg(p, "embrace", "You let the devil in.", ChatFormatting.DARK_PURPLE);
        }
        syncNow(p, c);
    }

    // ---------------------------------------------------------------- the second
    static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) return;
        second(p, c, time(p));
    }

    static void second(ServerPlayer p, DevilContract c, long now) {
        if (!DevilConfig.enabled()) {
            if (c.assimilation > 0 || c.assimilating || c.inUnion()) { c.assimilation = 0; c.assimilating = false; c.unionUntil = 0; }
            applyStats(p, c);
            applySacrifices(p, c);
            syncIfChanged(p, c, now);
            return;
        }
        if (c.inUnion()) {
            if (DevilRules.unionDue(c.unionUntil, now)) { endUnion(p, c, now, "time"); return; }
            if (!spendMana(p, DevilRules.unionDrain(c.tier) * DevilConfig.drainScale())) { endUnion(p, c, now, "mana"); return; }
        } else {
            boolean cooling = DevilRules.cooling(c.unionReadyAt, now);
            if (c.assimilating && !spendMana(p, drain(c, c.assimilation))) {
                c.assimilating = false;
                msg(p, "dry", "Your magicules run dry. The devil withdraws.", ChatFormatting.RED);
            }
            c.assimilation = DevilRules.step(c.assimilation, c.assimilating, c.tier, DevilRules.cap(cooling));
            if (c.assimilating && !cooling && c.assimilation >= 100f) beginUnion(p, c, now);
        }
        applyStats(p, c);
        syncIfChanged(p, c, now);
    }

    private static double drain(DevilContract c, float level) { return DevilRules.drain(c.tier, level) * DevilConfig.drainScale(); }

    private static void beginUnion(ServerPlayer p, DevilContract c, long now) {
        c.unionUntil = DevilRules.unionEnd(now, DevilConfig.unionTicks());
        c.assimilation = 100f;
        c.assimilating = false;
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 0.7f);
        ForbiddenMagic.PactDevil devil = ForbiddenMagic.devilOf(p);
        ForbiddenMagic.subtitle(p, Component.translatableWithFallback("nusmp.devil.msg.union_begins", "%s unites with %s.", p.getName().getString(), devil == null ? "a devil" : devil.displayName));
        syncNow(p, c);
    }

    /** The union is over (time, mana, death or logout): the level goes to 0 and the cooldown starts. {@code why} is "time", "mana", "death" or "quiet". */
    static void endUnion(ServerPlayer p, DevilContract c, long now, String why) {
        c.unionUntil = 0;
        c.assimilation = 0f;
        c.assimilating = false;
        boolean cool = !"death".equals(why) || DevilConfig.deathStartsCooldown();
        if (cool) c.unionReadyAt = Math.max(c.unionReadyAt, DevilRules.readyAfter(now, DevilConfig.cooldownTicks()));
        applyStats(p, c);
        syncNow(p, c);
        switch (why) {
            case "time" -> msg(p, "union_time", "The union breaks. Five minutes are over.", ChatFormatting.DARK_RED);
            case "mana" -> msg(p, "union_mana", "Your magicules run dry. The union breaks.", ChatFormatting.DARK_RED);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- stats and sacrifices
    static void applyStats(ServerPlayer p, DevilContract c) {
        boolean on = DevilConfig.enabled() && c.hasContract();
        float level = on ? c.assimilation : 0f;
        boolean union = on && c.inUnion();
        double scale = DevilConfig.statScale();
        setModifier(p, Attributes.ATTACK_DAMAGE, STR, on ? DevilRules.strength(c.tier, level, union, scale) : 0, AttributeModifier.Operation.ADD_VALUE, false);
        setModifier(p, Attributes.MOVEMENT_SPEED, SPD, on ? DevilRules.speed(c.tier, level, union, scale) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        flight(p, c, on && DevilConfig.allowFlight() && DevilRules.flight(level));
    }

    /** The permanent costs, applied again on every login and respawn (the attachment is the source of truth). Idempotent. */
    static void applySacrifices(ServerPlayer p, DevilContract c) {
        boolean on = DevilConfig.enabled();
        boolean half = on && DevilConfig.halfHealth() && c.sacrifices.contains(Sacrifice.HALF_HEALTH);
        setModifier(p, Attributes.MAX_HEALTH, HALF, half ? DevilRules.HALF_HEALTH_MULT : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        int eyes = on && DevilConfig.eyeReach() ? c.eyes() : 0;
        setModifier(p, Attributes.ENTITY_INTERACTION_RANGE, EYE, DevilRules.eyeReachPenalty(eyes), AttributeModifier.Operation.ADD_VALUE, false);
    }

    /** Sets a modifier to an amount (0 removes it); does nothing when it already has that amount, because every attribute change is a packet. */
    static void setModifier(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op, boolean permanent) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier cur = inst.getModifier(id);
        if (amount == 0) {
            if (cur != null) inst.removeModifier(id);
            return;
        }
        if (cur != null && Math.abs(cur.amount() - amount) < 1e-9 && cur.operation() == op) return;
        if (cur != null) inst.removeModifier(id);
        AttributeModifier m = new AttributeModifier(id, amount, op);
        if (permanent) inst.addPermanentModifier(m); else inst.addTransientModifier(m);
    }

    private static void flight(ServerPlayer p, DevilContract c, boolean on) {
        AttributeInstance fly = p.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (fly == null) return;
        if (on) {
            if (fly.getModifier(FLY) == null) fly.addTransientModifier(new AttributeModifier(FLY, 1.0, AttributeModifier.Operation.ADD_VALUE));
            Abilities ab = p.getAbilities();
            if (!ab.mayfly) { ab.mayfly = true; p.onUpdateAbilities(); }
            c.flightOn = true;
        } else if (c.flightOn || fly.getModifier(FLY) != null) {
            fly.removeModifier(FLY);
            c.flightOn = false;
            if (p.isCreative() || p.isSpectator() || p.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) > 0) return;     // another power still gives flight
            Abilities ab = p.getAbilities();
            boolean wasFlying = ab.flying;
            ab.mayfly = false;
            ab.flying = false;
            p.onUpdateAbilities();
            if (wasFlying) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, false));          // nobody drops out of the sky
        }
    }

    // ---------------------------------------------------------------- life cycle
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        backfill(p);
        DevilContract c = DevilStore.peek(p);
        if (c == null) return;
        c.assimilating = false;
        long now = time(p);
        if (c.inUnion()) endUnion(p, c, now, "quiet");        // a union does not survive a missing player (a crash: the logout handler never ran)
        else c.assimilation = 0f;
        c.sentSelf = -1;
        c.sentVisual = -1;
        applySacrifices(p, c);
        applyStats(p, c);
        syncNow(p, c);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) return;
        c.assimilating = false;
        if (c.inUnion()) endUnion(p, c, time(p), "quiet"); else c.assimilation = 0f;
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null) return;
        applySacrifices(p, c);
        applyStats(p, c);
        syncNow(p, c);
    }

    static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) return;
        applyStats(p, c);
        c.sentSelf = -1;
        c.sentVisual = -1;
        syncNow(p, c);
    }

    static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null || !c.hasContract()) return;
        if (c.inUnion()) endUnion(p, c, time(p), "death");
        else { c.assimilation = 0f; c.assimilating = false; }
    }

    /** The data was copied to the new body by NeoForge; only the live part starts again from nothing. */
    static void onClone(PlayerEvent.Clone e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DevilContract c = DevilStore.peek(p);
        if (c == null) return;
        c.resetLive();
    }

    /** Someone who starts seeing a player with a level sees it at once. */
    static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getTarget() instanceof ServerPlayer target) || !(e.getEntity() instanceof ServerPlayer viewer)) return;
        DevilContract c = DevilStore.peek(target);
        if (c == null || !c.hasContract() || c.assimilation <= 0f) return;
        DevilNet.sendVisualTo(viewer, visual(target, c));
    }

    // ---------------------------------------------------------------- sync
    static DevilNet.Visual visual(ServerPlayer p, DevilContract c) { return new DevilNet.Visual(p.getId(), c.tier.ordinal(), DevilRules.bucket(c.assimilation) * 5, c.inUnion()); }

    private static int visualKey(DevilContract c) {
        int b = DevilRules.bucket(c.assimilation);
        return c.inUnion() || b > 0 ? (c.tier.ordinal() << 16) | (b << 8) | (c.inUnion() ? 1 : 0) : 0;
    }

    private static int selfKey(ServerPlayer p, DevilContract c, long now) {
        int flags = flags(p, c, now);
        return c.tier.ordinal() | (c.attribute.ordinal() << 3) | (Math.round(c.assimilation) << 6) | (flags << 13) | (c.sacrificeMask() << 17);
    }

    private static int flags(ServerPlayer p, DevilContract c, long now) {
        return (c.assimilating ? DevilNet.F_ASSIMILATING : 0) | (c.inUnion() ? DevilNet.F_UNION : 0) | (DevilRules.cooling(c.unionReadyAt, now) ? DevilNet.F_COOLING : 0) | (oldUnionActive(p) ? DevilNet.F_BLOCKED : 0);
    }

    /** Sends what changed since the last time (at most one packet to the owner and one to the viewers per call). */
    static void syncIfChanged(ServerPlayer p, DevilContract c, long now) {
        int self = selfKey(p, c, now);
        if (self != c.sentSelf) { c.sentSelf = self; DevilNet.sendSelf(p, buildSync(p, c, now)); }
        int vis = visualKey(c);
        if (vis != c.sentVisual) {
            boolean nothingBefore = c.sentVisual == -1 && vis == 0;
            c.sentVisual = vis;
            if (!nothingBefore) DevilNet.sendVisual(p, visual(p, c));
        }
    }

    static void syncNow(ServerPlayer p, DevilContract c) {
        c.sentSelf = -1;
        syncIfChanged(p, c, time(p));
    }

    static DevilNet.Sync buildSync(ServerPlayer p, DevilContract c, long now) {
        return new DevilNet.Sync(c.tier.ordinal(), c.attribute.ordinal(), Math.round(c.assimilation), flags(p, c, now), c.sacrificeMask(),
                DevilRules.secondsLeft(c.unionUntil, now), DevilRules.secondsLeft(c.unionReadyAt, now));
    }

    // ---------------------------------------------------------------- magicules and messages
    static boolean hasMana(ServerPlayer p, double cost) {
        if (cost <= 0) return true;
        IExistence ex = TensuraStorages.getExistenceFrom(p);
        return ex != null && ex.getMagicule() >= cost;
    }

    /** Spends magicules if the player has enough (false and nothing spent otherwise). Creative players pay nothing. */
    static boolean spendMana(ServerPlayer p, double cost) {
        if (cost <= 0 || p.isCreative()) return true;
        IExistence ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || ex.getMagicule() < cost) return false;
        ex.setMagicule(ex.getMagicule() - cost);
        ex.markDirty();
        return true;
    }

    private static void msg(ServerPlayer p, String key, String fallback, ChatFormatting color) {
        p.displayClientMessage(Component.translatableWithFallback("nusmp.devil.msg." + key, fallback).withStyle(color), true);
    }
}
