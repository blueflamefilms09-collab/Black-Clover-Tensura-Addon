package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.blackclover.MagicType;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 1.99.0: the server-authoritative state of the armour a mage wears ({@link ArmourPolicy}). Casting never puts an armour on; the explicit armour page of a TOGGLE magic and the form
 * page of a FORM magic do, through {@link ArmourCast}, and this class keeps it:
 * <ul>
 *   <li><b>State.</b> One armour per player, in the player's persistent data ({@code nusmp_armour}: magic, style, form flag, since). A toggled armour survives a relog (it is forged
 *       in again at login), a FORM does not; nothing survives death (persistent data is not carried to the respawned body, and the death hook clears it anyway).</li>
 *   <li><b>Look.</b> The render layer is the existing {@link Aura} slot of the magic, kept alive with a lease ({@link ArmourRules#LEASE_TICKS}, renewed every
 *       {@link ArmourRules#REFRESH_EVERY} ticks: the clients never forge it twice) and shattered out when it ends ({@link ArmourRules#SHATTER_TICKS}); tracking players get it from
 *       {@link PlayerAuras#onStartTracking}, the wearer again after a dimension change.</li>
 *   <li><b>Stats while it is on.</b> The policy's {@link ArmourPolicy.Bonus}: share of every blow cut, immunity to the magic's own element, attack and speed multipliers (transient
 *       attribute modifiers, removed when it ends), knockback resistance, hidden night vision for forms. Scaled by the configured strength.</li>
 *   <li><b>Upkeep.</b> A toggled armour costs a share of the magicule pool every second (and, for the Silver Guardian, a little health: the mercury is poison); it ends when the pool
 *       cannot pay, when the wearer is knocked out (held in stopped time) or dies. Nothing here touches food or hunger.</li>
 * </ul>
 */
public final class ArmourState {
    private ArmourState() {}

    static final String KEY = "nusmp_armour";
    private static final ResourceLocation M_SPEED = ResourceLocation.fromNamespaceAndPath("nusmp", "armour_speed"),
            M_ATTACK = ResourceLocation.fromNamespaceAndPath("nusmp", "armour_attack"),
            M_KNOCKBACK = ResourceLocation.fromNamespaceAndPath("nusmp", "armour_knockback");
    private static boolean registered;

    /** Registers the damage, tick, death and session hooks once (called from NUSMP). */
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        AttributeEvents.incoming(ArmourState::onIncoming);
        AttributeEvents.playerTick(ArmourState::tick);
        AttributeEvents.death(e -> { if (e.getEntity() instanceof ServerPlayer p) end(p, ArmourRules.Drop.DEATH); });
        AttributeEvents.logout(ArmourState::onLogout);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) onLogin(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) onRespawn(p); });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if (e.getEntity() instanceof ServerPlayer p) sync(p, held(p)); });
    }

    // ------------------------------------------------------------------ reading the state
    /** The armour the player wears (null = none). */
    public static ArmourRules.Held held(LivingEntity e) {
        CompoundTag t = e.getPersistentData();
        if (!t.contains(KEY, 10)) return null;
        CompoundTag a = t.getCompound(KEY);
        String magic = a.getString("M");
        return magic.isEmpty() ? null : new ArmourRules.Held(magic, a.getInt("S"), a.getBoolean("F"), a.getString("A"));
    }

    public static boolean isOn(LivingEntity e, MagicType m) {
        ArmourRules.Held h = held(e);
        return h != null && h.magic().equals(m.name());
    }

    /** True while the player's armour is a FORM (Spirit Dive) of the magic. */
    public static boolean inForm(LivingEntity e, MagicType m) {
        ArmourRules.Held h = held(e);
        return h != null && h.form() && h.magic().equals(m.name());
    }

    /**
     * The factor a spell of magic {@code m} gets from the armour the caster wears: while the armour (or the form) of THIS magic is on, half of its attack bonus strengthens the
     * spell too. 1 when nothing of this magic is worn. Reading the armour never changes it: only an armour page puts it on.
     */
    public static float spellBoost(LivingEntity caster, MagicType m) {
        ArmourRules.Held h = held(caster);
        if (h == null || !h.magic().equals(m.name())) return 1f;
        ArmourPolicy.Entry e = ArmourPolicy.get().entry(h.magic());
        if (e == null) return 1f;
        ArmourPolicy.Bonus b = h.form() ? e.formBonus() : e.bonus();
        return ArmourRules.spellBoost(ArmourRules.scaled(b.attack(), stage(caster), ArmourConfig.bonusScale()));
    }

    // ------------------------------------------------------------------ putting it on and off
    /** The armour page (or form page) of {@code e}'s magic is cast: puts the armour on. The caller has already applied {@link ArmourRules#armourPage} / {@link ArmourRules#formStart}. */
    public static void putOn(ServerPlayer p, ArmourPolicy.Entry e, int style, boolean form, float stage) {
        ArmourRules.Held old = held(p);
        if (old != null) removeBonus(p);
        boolean look = !e.aura().isEmpty() && (!form || e.formAura());
        CompoundTag a = new CompoundTag();
        a.putString("M", e.magic());
        a.putInt("S", style);
        a.putBoolean("F", form);
        a.putString("A", look ? e.aura() : "");
        a.putFloat("G", stage);
        a.putLong("T", p.level().getGameTime());
        p.getPersistentData().put(KEY, a);
        applyBonus(p, e, form, stage);
        sync(p, held(p));
        say(p, form ? "nusmp.armour.form_on" : "nusmp.armour.on", form ? "%s." : "%s forged.", form ? e.shownFormName() : e.name().isEmpty() ? e.magic() : e.name());
    }

    /** Takes the player's armour off (the pieces shatter out); no-op when nothing is worn. */
    public static void takeOff(ServerPlayer p) { end(p, ArmourRules.Drop.NONE); }

    /** Ends the armour for a reason (death: at once, no animation; everything else: the shatter). */
    public static void end(ServerPlayer p, ArmourRules.Drop why) {
        ArmourRules.Held h = held(p);
        if (h == null) return;
        p.getPersistentData().remove(KEY);
        removeBonus(p);
        if (h.form()) SpiritDiveForm.stop(p, h.magic());                                    // a form that ends takes its dive with it
        if (!h.aura().isEmpty()) {
            Aura a = aura(h.aura());
            if (a != null) {
                if (why == ArmourRules.Drop.DEATH) auraClear(p, a);
                else auraSet(p, a, ArmourRules.SHATTER_TICKS, h.style());
            }
        }
        if (why == ArmourRules.Drop.DEATH) return;
        ArmourPolicy.Entry e = ArmourPolicy.get().entry(h.magic());
        String name = e == null ? h.magic() : h.form() ? e.shownFormName() : e.name().isEmpty() ? h.magic() : e.name();
        switch (why) {
            case NO_MANA -> say(p, "nusmp.armour.dry", "Your mana gives out. The %s falls away.", name);
            case KNOCKED_OUT -> say(p, "nusmp.armour.knocked", "Knocked out. The %s shatters.", name);
            case UNKNOWN_MAGIC -> say(p, "nusmp.armour.lost", "The %s answers to nothing and falls away.", name);
            default -> say(p, h.form() ? "nusmp.armour.form_off" : "nusmp.armour.off", h.form() ? "%s ends." : "%s taken off.", name);
        }
    }

    private static Aura aura(String name) {
        try { return Aura.valueOf(name); } catch (IllegalArgumentException ex) { return null; }
    }

    private static void say(ServerPlayer p, String key, String fallback, Object arg) {
        p.displayClientMessage(Component.translatableWithFallback(key, fallback, arg).withStyle(ChatFormatting.GRAY), true);
    }

    /** Tells the clients that see the player what the player wears (a fresh lease; the clients keep the start of an armour they already show). */
    public static void sync(ServerPlayer p, ArmourRules.Held h) {
        if (h == null || h.aura().isEmpty()) return;
        Aura a = aura(h.aura());
        if (a != null) auraSet(p, a, ArmourRules.LEASE_TICKS, h.style());
    }

    /** The render layer of the player changes; a fake player (self-tests, other mods) has no connection: the server table is written, there is nothing to send. */
    private static void auraSet(ServerPlayer p, Aura a, int ticks, int style) {
        try { PlayerAuras.set(p, a, ticks, style); } catch (NullPointerException noConnection) { /* see above */ }
    }

    private static void auraClear(ServerPlayer p, Aura a) {
        try { PlayerAuras.clear(p, a); } catch (NullPointerException noConnection) { /* see above */ }
    }

    // ------------------------------------------------------------------ session events
    private static void onLogout(ServerPlayer p) {
        ArmourRules.Held h = held(p);
        removeBonus(p);                                                       // the modifiers are transient; this keeps a reused entity clean
        if (h != null && h.form()) p.getPersistentData().remove(KEY);         // a form does not outlive the session; a toggled armour does
    }

    private static void onLogin(ServerPlayer p) {
        ArmourRules.Held h = held(p);
        if (h == null) return;
        ArmourPolicy.Entry e = ArmourPolicy.get().entry(h.magic());
        if (e == null || h.form() || !e.armoured() || !knows(p, h.magic())) { p.getPersistentData().remove(KEY); return; }
        CompoundTag a = p.getPersistentData().getCompound(KEY);
        applyBonus(p, e, false, a.contains("G") ? a.getFloat("G") : 1f);
        sync(p, h);                                                           // forged in again for the player and everyone who sees them
    }

    private static void onRespawn(ServerPlayer p) {
        p.getPersistentData().remove(KEY);
        removeBonus(p);
    }

    // ------------------------------------------------------------------ the tick
    private static void tick(ServerPlayer p) {
        if (!p.getPersistentData().contains(KEY, 10)) return;
        ArmourRules.Held h = held(p);
        if (h == null) return;
        ArmourPolicy.Entry e = ArmourPolicy.get().entry(h.magic());
        long now = p.level().getGameTime();
        boolean second = ArmourRules.secondDue(now);
        boolean known = !second || e != null && (h.form() || knows(p, h.magic()));          // the (slower) skill lookup only once a second
        ArmourPolicy.Bonus b = e == null ? ArmourPolicy.Bonus.NONE : (h.form() ? e.formBonus() : e.bonus());
        double max = 0, mana = 0, cost = 0;
        if (second && !h.form() && b.upkeep() > 0.0) {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null) {
                max = EnergyHelper.getMaxMagicule(p);
                mana = ex.getMagicule();
                cost = max * b.upkeep() * ArmourConfig.upkeepScale();
            }
        }
        ArmourRules.Drop d = ArmourRules.drop(p.isAlive(), TimeStop.isFrozen(p), e != null && known, p.isCreative(), second && cost > 0.0, mana, cost);
        if (d != ArmourRules.Drop.NONE) { end(p, d); return; }
        if (second && cost > 0.0) {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null) { ex.setMagicule(ArmourRules.manaAfterSecond(mana, max, cost, 0.0)); ex.markDirty(); }
        }
        if (second && !h.form() && ArmourRules.hpUpkeepAllowed(p.getHealth(), p.isCreative(), b.hpUpkeep())) p.hurt(p.damageSources().magic(), (float) b.hpUpkeep());
        if (second && b.senses()) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, false));
        if (ArmourRules.refreshDue(now)) sync(p, h);
    }

    private static boolean knows(ServerPlayer p, String magic) {
        try {
            MagicType m = MagicType.lookup(magic);
            if (m == null) return false;
            var book = com.newuniverse.nusmp.skill.NUSkills.bookFor(m);
            return book != null && SkillAPI.getSkillsFrom(p).getSkill(book.getRegistryName()).isPresent();
        } catch (RuntimeException | LinkageError ex) {
            return true;                                                      // never drop an armour because a lookup failed
        }
    }

    // ------------------------------------------------------------------ the stats
    private static void onIncoming(LivingIncomingDamageEvent ev) {
        if (!(ev.getEntity() instanceof ServerPlayer p) || !p.getPersistentData().contains(KEY, 10)) return;
        if (ev.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        ArmourRules.Held h = held(p);
        ArmourPolicy.Entry e = h == null ? null : ArmourPolicy.get().entry(h.magic());
        if (e == null) return;
        ArmourPolicy.Bonus b = h.form() ? e.formBonus() : e.bonus();
        if (b.isEmpty()) return;
        if (warded(b.ward(), ev)) { ev.setCanceled(true); p.clearFire(); return; }
        float stage = stage(p);
        float cut = Math.min(0.6f, ArmourRules.scaled(b.cut(), stage, ArmourConfig.bonusScale()));
        if (cut > 0f) ev.setAmount(ev.getAmount() * (1f - cut));
    }

    private static boolean warded(String ward, LivingIncomingDamageEvent ev) {
        return switch (ward) {
            case "fire" -> ev.getSource().is(DamageTypeTags.IS_FIRE);
            case "lightning" -> ev.getSource().is(DamageTypes.LIGHTNING_BOLT);
            default -> false;
        };
    }

    private static float stage(LivingEntity p) {
        CompoundTag t = p.getPersistentData();
        if (!t.contains(KEY, 10)) return 1f;
        CompoundTag a = t.getCompound(KEY);
        return a.contains("G") ? a.getFloat("G") : 1f;
    }

    private static void applyBonus(ServerPlayer p, ArmourPolicy.Entry e, boolean form, float stage) {
        ArmourPolicy.Bonus b = form ? e.formBonus() : e.bonus();
        double scale = ArmourConfig.bonusScale();
        modifier(p, Attributes.MOVEMENT_SPEED, M_SPEED, ArmourRules.scaled(b.speed(), stage, scale), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.ATTACK_DAMAGE, M_ATTACK, ArmourRules.scaled(b.attack(), stage, scale), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, M_KNOCKBACK, Math.min(1.0, ArmourRules.scaled(b.knockback(), stage, scale)), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void removeBonus(ServerPlayer p) {
        modifier(p, Attributes.MOVEMENT_SPEED, M_SPEED, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.ATTACK_DAMAGE, M_ATTACK, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, M_KNOCKBACK, 0, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void modifier(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, ResourceLocation id, double amount,
                                 AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0.0) inst.addTransientModifier(new AttributeModifier(id, amount, op));
    }
}
