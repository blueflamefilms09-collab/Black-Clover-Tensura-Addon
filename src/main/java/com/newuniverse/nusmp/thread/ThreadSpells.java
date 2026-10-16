package com.newuniverse.nusmp.thread;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Vanessa's Thread Magic gained five things in 1.43.0 and each one is a spell body that the grimoire page list points at. They
 * live here, apart from {@code ThreadBook}, so the book keeps only its page list and the old code, and the new behaviour has one
 * place to read and to test through its pure helpers ({@link ThreadMath}, {@link ProtectedSet}, {@link FateRules}):
 * <ul>
 *   <li>{@link #callRouge}: the Rouge page in entity mode (summons the cat; Sneak + cast on a player or pet names them).</li>
 *   <li>{@link #arrestingDarkWeave}: one target bound completely, in a cocoon of thread.</li>
 *   <li>{@link #dancingDollRecall}: a thread shot that smoothly pulls an ally to safety, or an enemy to the caster.</li>
 *   <li>{@link #threadTrapGeneration}: tripwire nodes in a line or a ring.</li>
 *   <li>{@link #reinforcement}: the Reinforcement Magic and Mana Skin toggle, its upkeep and its heat immunity.</li>
 * </ul>
 * Control spells use the shared balance law ({@code BalanceLaw.beginControl} / {@code controlTicks}); bosses keep their rules.
 */
public final class ThreadSpells {
    private ThreadSpells() {}

    public static final int RED = 0xFFFF3355;
    public static final String REINFORCE_TAG = "Reinforce", REINFORCE_UNTIL = "nusmp_reinforce_until";
    private static final ResourceLocation SKIN_ATTACK = ResourceLocation.fromNamespaceAndPath("nusmp", "reinforce_attack"),
            SKIN_ARMOR = ResourceLocation.fromNamespaceAndPath("nusmp", "reinforce_armor");

    // ================================================================ Rouge (entity mode)
    /** The Rouge page when {@code thread.rougeEntity} is on. Sneak + cast on a player or pet toggles them on Rouge's list instead. */
    public static boolean callRouge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p) {
        if (p.isShiftKeyDown()) {
            LivingEntity t = GrimoireBook.target(p, 16);
            if (t != null) { RougeManager.toggleAlly(p, i, t); return false; }           // choosing allies costs nothing and starts no cooldown
        }
        if (RougeManager.mana(p) <= 0.0) { GrimoireBook.fail(p, "Rouge cannot be woven without mana."); return false; }
        b.castCircle(p, 0.8f);
        RougeEntity cat = RougeManager.summon(p);
        if (cat == null) { GrimoireBook.fail(p, "The thread would not take the shape of a cat."); return false; }
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ROUGE_FATE_TAP, cat, cat.position().add(0, 0.3, 0), RED, 24, 1.6f);
        p.displayClientMessage(Component.literal("Rouge watches over you and " + RougeManager.allies(p).size() + " chosen allies, your pets and your party.").withStyle(ChatFormatting.RED), true);
        return true;
    }

    // ================================================================ Arresting Dark Weave
    public static boolean arrestingDarkWeave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "No one to arrest."); return false; }
        if (t.isAlliedTo(p) || isFriend(p, t)) { GrimoireBook.fail(p, "The thread will not bind a friend."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int base = ThreadConfig.arrestTicks();
        int ticks = ThreadMath.bindTicks(base, BalanceLaw.controlTicks(t, base));
        t.addEffect(new MobEffectInstance(ThreadEffects.BOUND, ticks, 0, false, true));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        if (t instanceof Mob m) m.getNavigation().stop();
        b.castCircle(p, 0.7f);
        ServerLevel sl = p.serverLevel();
        b.vfx(p, VfxShape.THREAD_STRINGS, p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.3, 0), t.getBoundingBox().getCenter(), 20, 1f);
        VfxSpawn.sendFollowing(sl, VfxShape.THREAD_COCOON, t, t.position().add(0, t.getBbHeight() * 0.5, 0), RED, ticks + 6, Math.max(0.6f, t.getBbHeight() / 1.8f));
        return true;
    }

    // ================================================================ Dancing Doll: Recall
    public static boolean dancingDollRecall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null) { GrimoireBook.fail(p, "No one to string up."); return false; }
        ServerLevel sl = p.serverLevel();
        boolean friend = t.isAlliedTo(p) || isFriend(p, t);
        Vec3 dest;
        if (friend) {
            if (t.distanceToSqr(p) < 2.5 * 2.5) { GrimoireBook.fail(p, "They are already beside you."); return false; }
            dest = SafeSpot.firstOk(sl, t, SafeSpot.ringAround(p.position(), 1.7, p.getYRot() * Math.PI / 180.0 + Math.PI));
        } else {
            if (BalanceLaw.isBoss(t)) { GrimoireBook.fail(p, "Its strings will not hold."); return false; }
            if (!GrimoireBook.control(p)) return false;
            Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
            Vec3 toward = away.lengthSqr() < 1e-4 ? Vec3.ZERO : away.normalize().scale(3.0);
            dest = SafeSpot.firstOk(sl, t, p.position().add(toward));
        }
        if (dest == null) { GrimoireBook.fail(p, "There is no safe place to set them down."); return false; }
        b.castCircle(p, 0.7f);
        Vec3 hand = p.getEyePosition().add(p.getViewVector(1f).scale(0.6)).add(0, -0.3, 0);
        b.vfx(p, VfxShape.THREAD_STRINGS, hand, t.getBoundingBox().getCenter(), 14, 1f);
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, t.position().add(0, 0.5, 0), dest.add(0, 0.5, 0), RED, 12, 0.9f);
        final Vec3 target = dest;
        final int steps = 8;
        SpellRuntime.zone(sl, steps, 1, age -> {
            if (!t.isAlive()) return;
            if (age >= steps) {                                                          // the last step: set them down exactly
                if (t.position().distanceToSqr(target) > 1.0 && SafeSpot.ok(sl, t, target)) t.teleportTo(target.x, target.y, target.z);
                t.setDeltaMovement(Vec3.ZERO);
                t.hurtMarked = true;
                t.fallDistance = 0f;
                return;
            }
            double[] v = ThreadMath.pullVelocity(new double[]{t.getX(), t.getY(), t.getZ()}, new double[]{target.x, target.y, target.z}, steps - age, 1.2);
            t.setDeltaMovement(v[0], v[1] + 0.04, v[2]);
            t.hurtMarked = true;
            t.fallDistance = 0f;
        });
        return true;
    }

    // ================================================================ Thread Trap Generation
    public static boolean threadTrapGeneration(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel sl = p.serverLevel();
        int cap = ThreadConfig.trapMax();
        int count = cap;
        boolean ring = p.isShiftKeyDown();
        List<double[]> spots;
        if (ring) {
            spots = ThreadMath.ring(p.getX(), p.getZ(), ThreadMath.ringRadius(count, 2.0), count, p.getYRot() * Math.PI / 180.0);
        } else {
            Vec3 look = p.getViewVector(1f), side = new Vec3(-look.z, 0, look.x);
            if (side.lengthSqr() < 1e-4) side = new Vec3(1, 0, 0);
            Vec3 centre = GrimoireBook.aim(p, 12);
            double half = (count - 1) * 1.6 / 2.0;
            spots = ThreadMath.line(centre.x - side.normalize().x * half, centre.z - side.normalize().z * half, side.x, side.z, count, 1.6, 0.0);
        }
        List<ThreadTrapEntity> placed = new ArrayList<>();
        for (double[] s : spots) {
            Vec3 g = ground(sl, s[0], s[1], p.getY() + 2);
            if (g == null) continue;
            ThreadTrapEntity t = ThreadEntities.THREAD_TRAP.get().create(sl);
            if (t == null) continue;
            t.setPos(g.x, g.y, g.z);
            t.setOwner(p, ThreadConfig.trapTicks());
            sl.addFreshEntity(t);
            ThreadTrapEntity.track(t, cap);
            placed.add(t);
        }
        if (placed.isEmpty()) { GrimoireBook.fail(p, "There is nowhere to string a thread."); return false; }
        b.castCircle(p, 0.8f);
        for (int k = 0; k + 1 < placed.size() || (ring && k < placed.size()); k++) {
            ThreadTrapEntity a = placed.get(k), c = placed.get((k + 1) % placed.size());
            if (a == c) break;
            VfxSpawn.send(sl, VfxShape.THREAD_TRAP_LINE, a.position().add(0, 0.4, 0), c.position().add(0, 0.4, 0), RED, 50, 0.9f);
        }
        p.displayClientMessage(Component.literal(placed.size() + " threads strung. You will feel them tremble.").withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** The standing height for a trap at (x, z), looking down from startY; null if there is no ground within 8 blocks. */
    private static Vec3 ground(ServerLevel sl, double x, double z, double startY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Math.floor(x), Math.floor(startY), Math.floor(z));
        for (int n = 0; n < 8; n++, pos.move(Direction.DOWN)) {
            BlockPos below = pos.below();
            if (sl.getBlockState(pos).getCollisionShape(sl, pos).isEmpty() && sl.getBlockState(below).isFaceSturdy(sl, below, Direction.UP)) return new Vec3(x, pos.getY(), z);
        }
        return null;
    }

    // ================================================================ Reinforcement Magic and Mana Skin
    public static boolean reinforcement(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var tag = i.getOrCreateTag();
        boolean on = !tag.getBoolean(REINFORCE_TAG);
        if (on && RougeManager.mana(p) <= 0.0) { GrimoireBook.fail(p, "You have no mana to wear as a skin."); return false; }
        tag.putBoolean(REINFORCE_TAG, on);
        i.markDirty();
        if (on) {
            apply(p);
            b.castCircle(p, 0.8f);
            p.displayClientMessage(Component.literal("Reinforcement Magic and Mana Skin.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        } else {
            clear(p);
            p.displayClientMessage(Component.literal("The thread skin loosens.").withStyle(ChatFormatting.GRAY), true);
        }
        return true;
    }

    /** Once a second while the book ticks: burn the upkeep, renew the buffs, show the skin. */
    public static void reinforceTick(ManasSkillInstance i, ServerPlayer p) {
        if (!i.getOrCreateTag().getBoolean(REINFORCE_TAG)) return;
        double drain = ThreadMath.reinforceDrain(RougeManager.maxMana(p), ThreadConfig.reinforceDrain());
        if (!ThreadMath.canKeepSkin(RougeManager.mana(p), drain) || !RougeManager.pay(p, drain)) {
            i.getOrCreateTag().putBoolean(REINFORCE_TAG, false);
            i.markDirty();
            clear(p);
            p.displayClientMessage(Component.literal("Your mana runs dry. The thread skin comes apart.").withStyle(ChatFormatting.RED), true);
            return;
        }
        apply(p);
    }

    private static void apply(ServerPlayer p) {
        p.getPersistentData().putLong(REINFORCE_UNTIL, p.level().getGameTime() + 45);
        modifier(p, Attributes.ATTACK_DAMAGE, SKIN_ATTACK, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        modifier(p, Attributes.ARMOR, SKIN_ARMOR, 4.0, AttributeModifier.Operation.ADD_VALUE, true);
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, true, false));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.THREAD_SKIN, p, p.position().add(0, 1, 0), RED, 30, 1f);
    }

    /** Takes the skin off (also used if the book is forgotten or the mana is gone). */
    public static void clear(ServerPlayer p) {
        p.getPersistentData().remove(REINFORCE_UNTIL);
        modifier(p, Attributes.ATTACK_DAMAGE, SKIN_ATTACK, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
        modifier(p, Attributes.ARMOR, SKIN_ARMOR, 0, AttributeModifier.Operation.ADD_VALUE, false);
    }

    private static void modifier(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, ResourceLocation id, double v,
                                 AttributeModifier.Operation op, boolean on) {
        AttributeInstance inst = e.getAttribute(a);
        if (inst == null) return;
        if (on && !inst.hasModifier(id)) inst.addTransientModifier(new AttributeModifier(id, v, op));
        if (!on && inst.hasModifier(id)) inst.removeModifier(id);
    }

    /** Fire, lava and hot floors do nothing while the skin is on. Wire in NUSMP: {@code NeoForge.EVENT_BUS.addListener(ThreadSpells::onIncomingDamage)}. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !(v instanceof Player) || v.getPersistentData().getLong(REINFORCE_UNTIL) <= v.level().getGameTime()) return;
        var s = e.getSource();
        if (s.is(DamageTypes.IN_FIRE) || s.is(DamageTypes.ON_FIRE) || s.is(DamageTypes.LAVA) || s.is(DamageTypes.HOT_FLOOR)) {
            e.setCanceled(true);
            v.clearFire();
        }
    }

    // ================================================================ shared
    /** A player or pet the caster counts as theirs (Rouge's own definition, so the spells and the cat agree). */
    private static boolean isFriend(ServerPlayer caster, LivingEntity t) {
        if (t instanceof OwnableEntity o && caster.getUUID().equals(o.getOwnerUUID())) return true;
        RougeEntity cat = RougeManager.of(caster.getUUID());
        if (cat != null && cat.isProtecting(t)) return true;
        return t instanceof Player && com.newuniverse.nusmp.multiverse.Squads.partyMembers(caster).contains(t.getUUID());
    }
}
