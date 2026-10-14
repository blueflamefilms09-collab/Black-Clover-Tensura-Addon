package com.newuniverse.nusmp.relics.pair;

import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The game-bus side of weapon pairs.
 * <ul>
 *   <li><b>The off-hand blow.</b> After a player's melee blow landed with one half of a pair in the main hand and the other half in the off hand, the
 *       off-hand half strikes a second, real blow: {@link PairRules#OFFHAND_FRACTION} of the first, with its own cooldown, through the mod's damage helper
 *       ({@link Fx#damage}: EP scaling, the PvP hit cap, no creative or spectator target). A guard flag makes the blow invisible to every other hook
 *       ({@link #striking()}), so it can never trigger itself or a wave's own blow rules again. A creative or spectator wielder gets no second blow.</li>
 *   <li><b>The first-login grant.</b> A player who holds a right half (the single item of the old versions) and whose world never issued a left half gets
 *       the left half once, through the ledger, so old worlds complete their pairs.</li>
 * </ul>
 */
public final class PairHooks {
    private PairHooks() {}

    private static final Logger LOG = LoggerFactory.getLogger("nusmp");
    private static final String K_NEXT = "nusmp_pair_next";
    private static boolean registered;
    private static boolean striking;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(PairHooks::onDamagePost);
        NeoForge.EVENT_BUS.addListener(PairHooks::onLogin);
    }

    /** True while the off-hand blow is being dealt: every other relic hook must treat the blow as its own and stay out. */
    public static boolean striking() { return striking; }

    /** Does this entity hold both halves of one pair, one in each hand, and does each half work for it (attuned where needed)? */
    public static boolean active(LivingEntity e) {
        if (e == null) return false;
        ItemStack main = e.getMainHandItem(), off = e.getOffhandItem();
        RelicDef md = Relics.defOf(main), od = Relics.defOf(off);
        if (md == null || od == null || !PairRules.pairActive(md.id(), od.id())) return false;
        return Attune.works(main, md, e) && Attune.works(off, od, e);
    }

    // ================================================================ the off-hand blow
    private static void onDamagePost(LivingDamageEvent.Post e) {
        if (striking) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p || !src.is(DamageTypes.PLAYER_ATTACK)) return;
        LivingEntity victim = e.getEntity();
        if (victim == p || !victim.isAlive() || victim.level().isClientSide()) return;
        if (p.isCreative() || p.isSpectator()) return;
        try {
            if (!active(p)) return;
            RelicDef off = Relics.defOf(p.getOffhandItem());
            long now = p.level().getGameTime();
            CompoundTag data = p.getPersistentData();
            if (!PairRules.ready(now, data.getLong(K_NEXT))) return;
            float dmg = PairRules.offHandDamage(e.getNewDamage());
            if (dmg <= 0f) return;
            data.putLong(K_NEXT, PairRules.nextAllowed(now));
            strike(p, victim, off, dmg);
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] pair off-hand blow failed", failed);
        }
    }

    private static void strike(ServerPlayer p, LivingEntity victim, RelicDef off, float dmg) {
        striking = true;
        try {
            Vec3 v = victim.getDeltaMovement();
            victim.invulnerableTime = 0;
            Fx.damage(p, victim, dmg, "minecraft:player_attack");
            victim.setDeltaMovement(v);                                                // no second shove
            p.swing(InteractionHand.OFF_HAND, true);
            if (p.level() instanceof ServerLevel l) {
                l.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 5, 0.2, 0.25, 0.2, 0.15);
            }
            PairEffects.OffHandEffect fx = PairEffects.of(off.pairBaseId());
            if (fx != null && victim.isAlive()) fx.strike(p, victim, off);
        } finally {
            striking = false;
        }
    }

    // ================================================================ the first-login grant
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        try {
            java.util.Map<String, RelicDef> rights = new java.util.LinkedHashMap<>();
            java.util.Map<String, ItemStack> rightStacks = new java.util.HashMap<>();
            java.util.Set<String> leftsHeld = new java.util.HashSet<>();
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                RelicDef d = Relics.defOf(p.getInventory().getItem(i));
                if (d == null || !d.isPairHalf()) continue;
                if (d.isPairLeft()) leftsHeld.add(d.pairBaseId());
                else {
                    rights.put(d.id(), d);
                    rightStacks.put(d.id(), p.getInventory().getItem(i));
                }
            }
            for (RelicDef right : rights.values()) {
                if (leftsHeld.contains(right.id())) continue;
                if (!PairRules.needsLeftGrant(true, PairGrant.leftEverIssued(p.getServer(), right))) continue;
                RelicDef left = Relics.byId(right.pairPartnerId());
                if (left == null) continue;
                ItemStack given = PairGrant.issueOne(p.serverLevel(), left);           // only the left half: the right one is in this inventory already
                if (given.isEmpty()) continue;
                ItemStack rs = rightStacks.get(right.id());
                if (right.needsAttunement() && rs != null && Attune.isMine(rs, p)) Attune.bind(p, given);      // an attuned right half: the new left half is attuned with it
                if (!p.getInventory().add(given)) p.drop(given, false);
                p.sendSystemMessage(Component.translatable("message.nusmp.relic.pair_completed", Component.translatable("item.nusmp." + left.id())).withStyle(ChatFormatting.AQUA));
            }
        } catch (RuntimeException | LinkageError failed) {
            LOG.error("[nusmp] pair first-login grant failed", failed);
        }
    }
}
