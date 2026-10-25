package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Body Rune ledger (3.6.0): the runes Gadjah writes on his own body. The rules and numbers are {@link BodyRuneRules}; this class keeps one session per player, applies the six transient
 * attribute modifiers, and takes them back.
 *
 * <ul>
 *   <li><b>Bounded</b>: one session per player (a second cast refreshes it, never stacks), at most {@link BodyRuneRules#MAX_SESSIONS} on the server; six attribute modifiers per session;
 *       two clientbound packets per session (start, end).</li>
 *   <li><b>Clean</b>: the modifiers are transient (never saved) and have fixed ids ({@link BodyRuneRules#IDS}), removed before they are added; they go when the time runs out, on logout, death,
 *       dimension change, when the server stops and when the setting is switched off (the sweep sees it within five ticks).</li>
 *   <li><b>Cheap</b>: the sweep runs every {@value #SWEEP_EVERY} ticks and only while a session exists; per session it compares two numbers and checks the player is still the same.</li>
 *   <li>Time is the WORLD clock ({@code getGameTime}) everywhere, never the server's tick count.</li>
 * </ul>
 */
public final class BodyRunes {
    private BodyRunes() {}

    public static final int SWEEP_EVERY = 5;

    private static final class Session {
        final UUID owner;
        final ServerPlayer player;
        final ServerLevel level;
        final long until;
        /** The share of each hit the runes turn away (0 unless a full hold with the setting on). */
        final double reduction;

        Session(ServerPlayer p, long until, double reduction) {
            this.owner = p.getUUID();
            this.player = p;
            this.level = p.serverLevel();
            this.until = until;
            this.reduction = reduction;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final PerfProbe.Section PROBE = PerfProbe.section("s.mana.body");

    public static int sessions() { return SESSIONS.size(); }

    public static boolean active(UUID owner) { return SESSIONS.containsKey(owner); }

    /** The world tick a player's runes end at, or -1 (tests, messages). */
    public static long untilOf(UUID owner) {
        Session s = SESSIONS.get(owner);
        return s == null ? -1L : s.until;
    }

    private static ResourceLocation id(int i) { return ResourceLocation.fromNamespaceAndPath("nusmp", BodyRuneRules.IDS[i]); }

    private static Holder<Attribute> attribute(int i) {
        return switch (i) {
            case 0 -> Attributes.MOVEMENT_SPEED;
            case 1 -> Attributes.ATTACK_DAMAGE;
            case 2 -> Attributes.JUMP_STRENGTH;
            case 3 -> Attributes.STEP_HEIGHT;
            case 4 -> Attributes.SAFE_FALL_DISTANCE;
            default -> Attributes.KNOCKBACK_RESISTANCE;
        };
    }

    /** The share of each hit the owner's runes turn away right now, or 0 (tests, messages). */
    public static double reductionOf(UUID owner) {
        Session s = SESSIONS.get(owner);
        return s == null ? 0.0 : s.reduction;
    }

    /** A wearer of a full-hold Body Rune takes a little less from each hit (void and kill damage are never touched). One map read per hit on a player, nothing at all while no body carries runes. */
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (SESSIONS.isEmpty() || !(event.getEntity() instanceof ServerPlayer p)) return;
        Session s = SESSIONS.get(p.getUUID());
        if (s == null || s.reduction <= 0.0) return;
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        event.setAmount(BodyRuneRules.reduced(event.getAmount(), s.reduction));
    }

    // ---------------------------------------------------------------- the cast
    /** Writes the runes on {@code p} for the duration of the charge; false (with a message) when the server is full. A second cast replaces the first (it never stacks). */
    public static boolean start(ServerPlayer p, RuneSpec spec, Charge charge, long now) {
        Session old = SESSIONS.get(p.getUUID());
        if (old == null && SESSIONS.size() >= BodyRuneRules.MAX_SESSIONS) {
            com.newuniverse.nusmp.book.GrimoireBook.fail(p, "The mana of this world is busy with other runes. Try again in a moment.");
            return false;
        }
        BodyRuneRules.Mods m = BodyRuneRules.modsFor(charge, ManaMethodConfig.bodySeconds());
        if (old != null) strip(old.player);
        apply(p, m);
        Session s = new Session(p, now + m.ticks(), ManaMethodConfig.bodyDamageReduction() ? BodyRuneRules.reduction(charge) : 0.0);
        SESSIONS.put(p.getUUID(), s);
        if (m.glideTicks() > 0) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, m.glideTicks(), 0, false, false, true));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9f, 0.8f);
        RuneNet.sendBody(p, RuneNet.PHASE_START, spec.pack(), m.tier(), m.ticks());
        p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_method.body", "Body Rune: the runes settle on your body for %s s", Math.round(m.ticks() / 20.0)).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /** Removes then adds every modifier of the charge: a refresh replaces, never stacks. */
    static void apply(ServerPlayer p, BodyRuneRules.Mods m) {
        for (int i = 0; i < BodyRuneRules.IDS.length; i++) {
            AttributeInstance a = p.getAttribute(attribute(i));
            if (a == null) continue;
            a.removeModifier(id(i));
            double v = m.amount(i);
            if (v > 0.0) a.addTransientModifier(new AttributeModifier(id(i), v, BodyRuneRules.Mods.multiplied(i) ? AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL : AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Is modifier {@code i} (in the order of {@link BodyRuneRules#IDS}) on the player right now? (the self-test) */
    static boolean has(ServerPlayer p, int i) {
        AttributeInstance a = p.getAttribute(attribute(i));
        return a != null && a.getModifier(id(i)) != null;
    }

    /** Takes all six modifiers of a player back (safe to call when none is there). */
    static void strip(ServerPlayer p) {
        for (int i = 0; i < BodyRuneRules.IDS.length; i++) {
            AttributeInstance a = p.getAttribute(attribute(i));
            if (a != null) a.removeModifier(id(i));
        }
    }

    // ---------------------------------------------------------------- the tick
    /** The sweep, once every {@value #SWEEP_EVERY} ticks and only while a session exists. {@code now} is the world clock. */
    public static void tick(MinecraftServer server, long now) {
        if (SESSIONS.isEmpty() || now % SWEEP_EVERY != 0) return;
        long t0 = PerfProbe.begin();
        try {
            sweep(now);
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    /** One pass over the sessions at world tick {@code now} (public for the self-test, which advances time by hand). */
    public static void sweep(long now) {
        for (Iterator<Session> it = SESSIONS.values().iterator(); it.hasNext(); ) {
            Session s = it.next();
            ServerPlayer p = s.player;
            boolean gone = p.isRemoved() || !p.isAlive() || p.level() != s.level;
            if (gone || !ManaMethodConfig.bodyRunes() || !ManaMethodConfig.enabled()) { finish(s, gone, false); it.remove(); continue; }
            if (BodyRuneRules.expired(now, s.until)) {
                finish(s, false, true);
                it.remove();
            }
        }
    }

    private static void finish(Session s, boolean playerGone, boolean message) {
        strip(s.player);
        if (!playerGone) {
            RuneNet.sendBody(s.player, RuneNet.PHASE_END, 0, 0, 0);
            if (message) s.player.displayClientMessage(Component.translatableWithFallback("nusmp.mana_method.body_end", "The Body Rune fades.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    // ---------------------------------------------------------------- clean-up
    public static void cancelOwner(UUID owner) {
        if (SESSIONS.isEmpty()) return;
        Session s = SESSIONS.remove(owner);
        if (s != null) finish(s, s.player.isRemoved(), false);
    }

    public static void clearAll() {
        for (Session s : SESSIONS.values()) strip(s.player);
        SESSIONS.clear();
    }
}
