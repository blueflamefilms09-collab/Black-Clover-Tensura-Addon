package com.newuniverse.nusmp.shadow;

import com.newuniverse.nusmp.blackclover.ModeArmor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: a Devil Unite mode is one fact (which devil, until when) that three things follow: the stats on the player, the armour overlay on the
 * client, and the mode's abilities. This is the single owner of that fact on the server: it stores the {@link UniteSlot} in the player's
 * persistent data, puts the attribute changes of {@link UniteKind} on and takes them off, and tells {@code ModeArmor} to show or drop the overlay.
 * Attribute modifiers are transient (not saved), so {@link #refresh} puts them back after a relog while the mode is still running.
 */
public final class UniteState {
    private UniteState() {}

    private static final String KIND = "nusmp_unite_kind", UNTIL = "nusmp_unite_until", OLD = "nusmp_unite_old";

    private static ResourceLocation id(String n) { return ResourceLocation.fromNamespaceAndPath("nusmp", "unite_" + n); }

    private static final ResourceLocation ARMOR = id("armor"), TOUGH = id("toughness"), SPEED = id("speed"), KB = id("knockback"), JUMP = id("jump"),
            FALL = id("fall"), ATTACK = id("attack");

    public static UniteSlot slot(LivingEntity e) {
        var d = e.getPersistentData();
        return d.contains(KIND) ? new UniteSlot(d.getInt(KIND), d.getLong(UNTIL)) : UniteSlot.NONE;
    }

    private static void save(ServerPlayer p, UniteSlot s) {
        if (s.kind() < 0) { p.getPersistentData().remove(KIND); p.getPersistentData().remove(UNTIL); return; }
        p.getPersistentData().putInt(KIND, s.kind());
        p.getPersistentData().putLong(UNTIL, s.until());
    }

    /** Did an old quick page begin the running mode? */
    public static boolean fromOldPage(ServerPlayer p) { return p.getPersistentData().getBoolean(OLD); }

    /** The mode this entity is in right now (players only), or null. */
    public static UniteKind active(LivingEntity e) {
        return e instanceof ServerPlayer ? slot(e).running(e.level().getGameTime()) : null;
    }

    /** Starts (or refreshes) a mode for {@code ticks}: replaces a different running mode, applies the stats, shows the overlay. */
    public static void start(ServerPlayer p, UniteKind kind, int ticks) {
        start(p, kind, ticks, false);
    }

    /** The same; {@code fromOldPage} marks a mode that one of the old quick pages (Canis, Gallus, Canis x Felis) began, so the config switch for their extras applies. */
    public static void start(ServerPlayer p, UniteKind kind, int ticks, boolean fromOldPage) {
        long now = p.level().getGameTime();
        UniteSlot old = slot(p);
        if (old.replaces(kind, now)) clearStats(p, old.running(now));
        save(p, UniteSlot.start(kind, now, ticks));
        p.getPersistentData().putBoolean(OLD, fromOldPage);
        applyStats(p, kind);
        com.newuniverse.nusmp.quests.NusmpEvents.fire(p, "unite.mode", kind.name().toLowerCase(java.util.Locale.ROOT));
        if (ShadowConfig.overlays()) ModeArmor.start(p, ModeArmor.Mode.valueOf(kind.overlay), ticks);
    }

    /** Ends the mode now (early end, death, expiry): stats off, overlay off, sight off. */
    public static void stop(ServerPlayer p) {
        UniteSlot s = slot(p);
        if (s.kind() < 0) return;
        UniteKind k = UniteKind.of(s.kind());
        if (k != null) {
            clearStats(p, k);
            ModeArmor.stop(p, ModeArmor.Mode.valueOf(k.overlay));
        }
        ShadowSight.clear(p);
        UniteAbilities.endNightEyes(p);
        save(p, UniteSlot.NONE);
        p.getPersistentData().remove(OLD);
    }

    /** Puts the transient stats back if they are missing (after a relog) and removes them if the mode is over. */
    public static void refresh(ServerPlayer p) {
        UniteSlot s = slot(p);
        long now = p.level().getGameTime();
        if (s.expired(now)) { stop(p); return; }
        UniteKind k = s.running(now);
        if (k == null) { if (s.kind() >= 0) stop(p); return; }
        applyStats(p, k);
    }

    private static void applyStats(ServerPlayer p, UniteKind k) {
        put(p, Attributes.ARMOR, ARMOR, k.armor, AttributeModifier.Operation.ADD_VALUE);
        put(p, Attributes.ARMOR_TOUGHNESS, TOUGH, k.toughness, AttributeModifier.Operation.ADD_VALUE);
        put(p, Attributes.MOVEMENT_SPEED, SPEED, k.speed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        put(p, Attributes.KNOCKBACK_RESISTANCE, KB, k.knockbackResistance, AttributeModifier.Operation.ADD_VALUE);
        put(p, Attributes.JUMP_STRENGTH, JUMP, k.jump, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        put(p, Attributes.SAFE_FALL_DISTANCE, FALL, k.safeFall, AttributeModifier.Operation.ADD_VALUE);
        put(p, Attributes.ATTACK_DAMAGE, ATTACK, k.attack, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void clearStats(ServerPlayer p, UniteKind k) {
        for (Holder<Attribute> a : new Holder[]{Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.MOVEMENT_SPEED, Attributes.KNOCKBACK_RESISTANCE,
                Attributes.JUMP_STRENGTH, Attributes.SAFE_FALL_DISTANCE, Attributes.ATTACK_DAMAGE}) {
            AttributeInstance inst = p.getAttribute(a);
            if (inst == null) continue;
            for (ResourceLocation r : new ResourceLocation[]{ARMOR, TOUGH, SPEED, KB, JUMP, FALL, ATTACK}) inst.removeModifier(r);
        }
    }

    private static void put(ServerPlayer p, Holder<Attribute> attr, ResourceLocation id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        if (amount == 0) { inst.removeModifier(id); return; }
        AttributeModifier have = inst.getModifier(id);
        if (have != null && have.amount() == amount && have.operation() == op) return;       // already on: nothing to rebuild
        inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, op));
    }
}
