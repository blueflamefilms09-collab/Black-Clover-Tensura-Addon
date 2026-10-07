package com.newuniverse.nusmp.seal;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.aura.PlayerAuras;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.ext.AttributeEvents;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Secre Swallowtail's Seal Magic (0.56, light-blue / cyan neon): Branching Array, Eternal Prison, Wound Sealing, Orbital Bind. The four pages are
 * locked to CanonBook.SECRE (CharacterSpells) and checked again at cast time (holdsSecreGrimoire). VFX go out with the cyan default colour.
 */
public final class SecreSeals {
    private SecreSeals() {}

    public static final int CYAN = 0xFF5AD8FF;
    private static final ResourceLocation BIND_MOD = ResourceLocation.fromNamespaceAndPath("nusmp", "orbital_bind");
    private static boolean registered;

    /** The four pages, appended to the Sealing book (the page ids are save keys). */
    public static List<BookPage> pages() {
        return List.of(
                BookPage.zone("branching_array", "Seal Magic: Branching Array", SecreSeals::branchingArray),
                BookPage.signature("eternal_prison", "Seal Magic: Eternal Prison", SecreSeals::eternalPrison).withAnim("out"),
                BookPage.mid("wound_sealing", "Seal Magic: Wound Sealing", SecreSeals::woundSealing),
                BookPage.zone("orbital_bind", "Seal Magic: Orbital Bind", SecreSeals::orbitalBind));
    }

    /** True only when the player's bound grimoire is Secre Swallowtail's. */
    public static boolean holdsSecreGrimoire(ServerPlayer p) { return CharacterSpells.characterOf(p) == CanonBook.SECRE; }

    private static boolean secre(ServerPlayer p) {
        if (holdsSecreGrimoire(p)) return true;
        GrimoireBook.fail(p, "Only Secre Swallowtail's grimoire can cast this.");
        return false;
    }

    // ------------------------------------------------------------------ hooks (once)
    /** Registers the prison's damage hooks and the stuck-mob sweeper. Called from SealExt.pages(). */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        AttributeEvents.incoming(e -> {
            LivingEntity victim = e.getEntity();
            var src = e.getSource();
            if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            if (imprisoned(victim) || (src.getEntity() instanceof LivingEntity a && imprisoned(a))) e.setCanceled(true);
        });
        AttributeEvents.playerTick(p -> {                                  // a mob left held by a stopped server is let go
            long now = p.level().getGameTime();
            if (now % 100 != 0) return;
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48), x -> x.getPersistentData().getBoolean("nusmp_prison_held")))
                if (!imprisoned(m)) release(m);
        });
    }

    public static boolean imprisoned(LivingEntity e) {
        return e.getPersistentData().getLong("nusmp_prison_until") > e.level().getGameTime();
    }

    private static boolean valid(LivingEntity t) {
        return t != null && !(t instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator()));
    }

    // ------------------------------------------------------------------ Branching Array
    static boolean branchingArray(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!secre(p)) return false;
        ServerLevel level = p.serverLevel();
        List<LivingEntity> foes = new ArrayList<>();
        for (LivingEntity e : GrimoireBook.around(p, p.position(), 8)) {
            if (foes.size() >= 24 || !valid(e)) continue;
            if (e instanceof Enemy || e instanceof Player || e instanceof Mob m && m.getTarget() == p) foes.add(e);
        }
        boolean opened = unlockLooked(p);
        if (foes.isEmpty() && !opened) { GrimoireBook.fail(p, "Nothing to lock down."); return false; }
        for (LivingEntity t : foes) {
            int ticks = BalanceLaw.isBoss(t) ? 20 : 100;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
            if (t instanceof Player) t.getPersistentData().putLong("nusmp_sealed_until", level.getGameTime() + ticks);
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.send(level, VfxShape.SEAL_BRANCH, hands(p), p.position().add(0, 1, 0), CYAN, 36, 8f);
        return true;
    }

    /** Unlocks the lockable container (a chest with a lock key) under the crosshair within 8 blocks. */
    private static boolean unlockLooked(ServerPlayer p) {
        HitResult hit = p.pick(8, 1f, false);
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) return false;
        ServerLevel level = p.serverLevel();
        BlockEntity be = level.getBlockEntity(bh.getBlockPos());
        if (!(be instanceof BaseContainerBlockEntity)) return false;
        CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
        if (!tag.contains("lock")) return false;
        tag.remove("lock");
        be.loadWithComponents(tag, level.registryAccess());
        be.setChanged();
        return true;
    }

    // ------------------------------------------------------------------ Eternal Prison
    static boolean eternalPrison(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!secre(p)) return false;
        LivingEntity t = GrimoireBook.target(p, 16);
        if (!valid(t)) { GrimoireBook.fail(p, "Nothing to imprison."); return false; }
        if (!GrimoireBook.control(p)) return false;
        ServerLevel level = p.serverLevel();
        int ticks = BalanceLaw.isBoss(t) ? 30 : 120;
        imprison(t, ticks);
        b.castCircle(p, 1.4f);
        float edge = Math.max(1f, Math.max(t.getBbWidth(), t.getBbHeight()));
        Vec3 centre = t.position().add(0, t.getBbHeight() / 2, 0);
        MagicProps.spawn(level, PropKind.SEALING_2, centre, 0f, edge, ticks, 0, p);
        VfxSpawn.send(level, VfxShape.SEAL_CUBE, centre, t.position(), CYAN, ticks, edge);
        return true;
    }

    /** Encases the entity: no moving, no damage taken, no damage dealt, until the time is up. */
    static void imprison(LivingEntity t, int ticks) {
        ServerLevel level = (ServerLevel) t.level();
        long now = level.getGameTime();
        boolean held = imprisoned(t);
        CompoundTag d = t.getPersistentData();
        d.putLong("nusmp_prison_until", now + ticks);
        Vec3 pin = t.position();
        if (t instanceof Mob m) {
            if (!held) { d.putBoolean("nusmp_prison_wasnoai", m.isNoAi()); d.putBoolean("nusmp_prison_held", true); }
            m.setNoAi(true);
            m.setTarget(null);
        } else if (t instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false));
            d.putLong("nusmp_sealed_until", now + ticks);
        }
        SpellRuntime.zone(level, ticks, 1, age -> {
            if (!t.isAlive()) return;
            t.setDeltaMovement(0, 0, 0);
            t.hurtMarked = true;
            if (t instanceof ServerPlayer sp && sp.position().distanceToSqr(pin) > 0.25) sp.connection.teleport(pin.x, pin.y, pin.z, sp.getYRot(), sp.getXRot());
            else if (t instanceof Mob && t.position().distanceToSqr(pin) > 0.25) t.setPos(pin.x, pin.y, pin.z);
        });
        SpellRuntime.later(level, ticks + 1, () -> { if (t instanceof Mob m && !imprisoned(m)) release(m); });
    }

    /** Gives a held mob its mind back. */
    static void release(Mob m) {
        CompoundTag d = m.getPersistentData();
        if (!d.getBoolean("nusmp_prison_held")) return;
        m.setNoAi(d.getBoolean("nusmp_prison_wasnoai"));
        d.remove("nusmp_prison_held");
        d.remove("nusmp_prison_wasnoai");
    }

    // ------------------------------------------------------------------ Wound Sealing
    static boolean woundSealing(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!secre(p)) return false;
        LivingEntity t = GrimoireBook.target(p, 12);
        if (t == null) t = p;
        t.heal(t.getMaxHealth() * 0.3f);
        List<MobEffectInstance> bad = new ArrayList<>();
        for (MobEffectInstance e : t.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(e);
        for (MobEffectInstance e : bad) t.removeEffect(e.getEffect());
        if (t.getRemainingFireTicks() > 0) t.setRemainingFireTicks(0);
        b.castCircle(p, 0.7f);
        VfxSpawn.send(p.serverLevel(), VfxShape.SEAL_WOUND, hands(p), t.position().add(0, t.getBbHeight() * 0.55, 0), CYAN, 40, 1f);
        return true;
    }

    // ------------------------------------------------------------------ Orbital Bind
    static boolean orbitalBind(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!secre(p)) return false;
        LivingEntity t = GrimoireBook.target(p, 12);
        if (t == null || !(t.isAlliedTo(p) || t instanceof Player) || !valid(t)) t = p;
        final LivingEntity ally = t;
        ServerLevel level = p.serverLevel();
        final int ticks = 600;
        AttributeInstance mr = ally.getAttribute(TensuraAttributes.MAGIC_RESISTANCE);
        if (mr != null) {
            if (mr.hasModifier(BIND_MOD)) mr.removeModifier(BIND_MOD);
            mr.addTransientModifier(new AttributeModifier(BIND_MOD, 0.3, AttributeModifier.Operation.ADD_VALUE));
            SpellRuntime.later(level, ticks, () -> {
                AttributeInstance a = ally.getAttribute(TensuraAttributes.MAGIC_RESISTANCE);
                if (a != null && a.hasModifier(BIND_MOD)) a.removeModifier(BIND_MOD);
            });
        } else {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 0));
        }
        if (ally instanceof ServerPlayer sp) PlayerAuras.set(sp, Aura.SEALING, ticks, 1);
        b.castCircle(p, 0.9f);
        final float radius = Math.max(1.2f, ally.getBbHeight() * 0.9f);
        SpellRuntime.zone(level, ticks, 80, age -> {
            if (!ally.isAlive()) return;
            VfxSpawn.send(level, VfxShape.SEAL_ORBIT, ally.position().add(0, ally.getBbHeight() / 2, 0), ally.position(), CYAN, 80, radius);
        });
        return true;
    }

    // ------------------------------------------------------------------ helpers
    /** Roughly the caster's right hand. */
    private static Vec3 hands(ServerPlayer p) {
        Vec3 look = p.getViewVector(1f);
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize().scale(0.35);
        return p.getEyePosition().add(right).add(0, -0.45, 0).add(look.scale(0.5));
    }
}
