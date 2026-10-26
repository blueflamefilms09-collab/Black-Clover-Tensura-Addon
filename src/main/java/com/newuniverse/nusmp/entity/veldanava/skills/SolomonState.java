package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the small pieces of server state Solomon's skill and events share: the attribute modifiers of Infinite Grimoire (maximum Magicule and regeneration), the registry of the standing Realms of Magic, the
 * count of elemental projectiles a wearer has absorbed this second, and the second the last foresight message was shown. All per-player state is keyed by the player's UUID or kept on the player's own
 * data; every map holds only players who are using the skill right now (one entry per owner, at most {@link JudgmentFields} caps) and every entry is dropped on death, logout, forgetting the skill and
 * server stop. Server thread only.
 */
public final class SolomonState {
    private SolomonState() {}

    public static final String LAST_FORESIGHT = "nusmp_solomon_foresight";
    private static final ResourceLocation MAX_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "solomon_max_magicule");
    private static final ResourceLocation REGEN_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "solomon_magicule_regen");

    // ---------------------------------------------------------------- Infinite Grimoire: the pool and the regeneration
    /**
     * Sets or removes the two modifiers of Infinite Grimoire (they are transient: not saved, and the skill's once-a-second look puts them back after a login). The current value is replaced only when it is
     * not the wanted one, so a steady second changes nothing.
     */
    public static void applyMagic(ServerPlayer p, boolean on, boolean mastered) {
        set(p, TensuraAttributes.MAX_MAGICULE, MAX_ID, on ? (mastered ? SolomonRules.MAX_MP_BONUS_MASTERED : SolomonRules.MAX_MP_BONUS) : 0.0);
        set(p, TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, REGEN_ID, on ? (mastered ? SolomonRules.REGEN_BONUS_MASTERED : SolomonRules.REGEN_BONUS) : 0.0);
    }

    private static void set(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier now = inst.getModifier(id);
        if (amount <= 0.0) {
            if (now != null) inst.removeModifier(id);
            return;
        }
        if (now != null && now.amount() == amount) return;
        if (now != null) inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /** True when the player carries the Infinite Grimoire pool modifier (the self-test reads it). */
    public static boolean hasMagicModifier(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
        return inst != null && inst.getModifier(MAX_ID) != null;
    }

    // ---------------------------------------------------------------- the standing Realms of Magic
    private record Realm(ResourceKey<Level> dimension, Vec3 at, long until) {}

    private static final Map<UUID, Realm> REALMS = new HashMap<>();

    /** Records the realm of an owner for {@link SolomonRules#REALM_GRACE_TICKS} more ticks (the Realm renews it every pulse). */
    public static void realm(ServerPlayer owner) {
        REALMS.put(owner.getUUID(), new Realm(owner.level().dimension(), owner.position(), owner.level().getGameTime() + SolomonRules.REALM_GRACE_TICKS));
    }

    public static void endRealm(UUID owner) { REALMS.remove(owner); }

    public static int realmCount() { return REALMS.size(); }

    /** True when a standing Realm of Magic covers this creature and its owner counts the creature as an ally (or is the creature): its spells are free and its chant is one tick. Returns on its first line when none stands. */
    public static boolean realmCovers(LivingEntity victim) {
        if (REALMS.isEmpty()) return false;
        MinecraftServer server = victim.getServer();
        if (server == null) return false;
        long now = victim.level().getGameTime();
        double r2 = SolomonRules.REALM_RADIUS * SolomonRules.REALM_RADIUS;
        for (Iterator<Map.Entry<UUID, Realm>> it = REALMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Realm> en = it.next();
            Realm r = en.getValue();
            if (r.until() <= now) { it.remove(); continue; }
            if (!r.dimension().equals(victim.level().dimension()) || r.at().distanceToSqr(victim.position()) > r2) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(en.getKey());
            if (owner == null || !owner.isAlive()) continue;
            if (owner == victim || Foes.isAlly(owner, victim)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- Elemental Sovereignty: how many projectiles a second
    private static final Map<UUID, long[]> ABSORBED = new HashMap<>();

    /** True when the wearer may absorb one more elemental projectile this second (at most {@link SolomonRules#ABSORB_PER_SECOND}); the count is taken. */
    public static boolean takeAbsorb(ServerPlayer p) {
        long second = p.level().getGameTime() / 20L;
        long[] c = ABSORBED.computeIfAbsent(p.getUUID(), k -> new long[]{second, 0L});
        if (c[0] != second) { c[0] = second; c[1] = 0L; }
        if (c[1] >= SolomonRules.ABSORB_PER_SECOND) return false;
        c[1]++;
        return true;
    }

    // ---------------------------------------------------------------- Omnipotent Magic: the foresight message
    /** True at most once every {@link SolomonRules#FORESIGHT_MESSAGE_GAP} ticks per player (the message is shown and the time is taken). */
    public static boolean foresightMessage(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(LAST_FORESIGHT)) return false;
        p.getPersistentData().putLong(LAST_FORESIGHT, now + SolomonRules.FORESIGHT_MESSAGE_GAP);
        return true;
    }

    // ---------------------------------------------------------------- clearing
    /** Everything this skill kept for the player: the modifiers, the realm, the counters, the casts remembered, the copy is kept (it is the player's). */
    public static void clear(ServerPlayer p) {
        applyMagic(p, false, false);
        leave(p);
    }

    /** Death and logout: the temporary things go (the toggles stay on the skill, the modifiers are transient). */
    public static void leave(ServerPlayer p) {
        REALMS.remove(p.getUUID());
        ABSORBED.remove(p.getUUID());
        SolomonCasts.leave(p.getUUID());
    }

    /** Tests and the server stop: forgets every entry without touching a player. */
    static void clearAll() {
        REALMS.clear();
        ABSORBED.clear();
        SolomonCasts.clearAll();
    }
}
