package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.FictionRules;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * All Fiction (Kumagawa Misogi): the Ultimate evolution of Book Maker (a standalone skill otherwise): make things never have happened. Rebuilt in 3.46.0 from two modes to ten; the first two keep their
 * places, ids and numbers. Undo: your health returns to what it was five seconds ago and harmful effects vanish. Fiction: erase the creature you look at (a boss, or a player when the config protects
 * them, instead loses its effects and a quarter of its health). Annul Damage: for six seconds hits are made not to have happened until half your maximum health is used up. Rewind Step: you stand where you
 * stood up to five seconds ago. Erase Effects: a foe loses every effect, an ally (or you) every harmful one. Nullify Strike: the next hit within ten seconds never happens. Unmake: hostile projectiles
 * around you never existed. Share the Fiction: allies near you are cleansed and healed. Stand Up: tells you when your death can be made fiction again. World Fiction: a zone around where you stood,
 * for six seconds, that cleanses allies and erases hostile projectiles once a second. Passive: once every fifteen minutes your own death becomes fiction and you stand up at full health.
 * <p>
 * State is kept in the skill tag of the one player who owns it (nothing static, nothing shared): the five-second health memory (H0..H4, unchanged), the five-second position memory (PX/PY/PZ0..4, PN,
 * PDim), the window ends as game times (AnnulUntil, NullifyUntil, WorldUntil) and the zone (WorldX/Y/Z, WorldDim). The tick writes a few numbers once a second; the zone pulses once a second for six
 * seconds. A press looks at no more than 24 creatures, 32 projectiles. Numbers: {@link FictionRules}. Switch: server setting {@code allFictionRebuilt} (off = the old two modes, the new hooks do nothing).
 */
public class AllFictionSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("all_fiction", "All Fiction",
            "Ultimate. Make injuries, enemies and even your own death never have happened.");

    private static final String ANNUL_UNTIL = "AnnulUntil", ANNUL_POOL = "AnnulPool", NULLIFY_UNTIL = "NullifyUntil", WORLD_UNTIL = "WorldUntil";

    public AllFictionSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    /** True when the ten-mode rebuild is on; before the server config is loaded (a client on the title screen) it counts as on. */
    static boolean rebuilt() {
        try {
            return NUConfig.ALL_FICTION_REBUILT.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    @Override public int getModes(ManasSkillInstance i) { return FictionRules.wheel(rebuilt()); }

    @Override
    public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
        int n = FictionRules.wheel(rebuilt());
        return Math.floorMod(mode + (reverse ? -1 : 1), n);
    }

    @Override public String getModeId(ManasSkillInstance i, int mode) { return FictionRules.ALL_IDS[Math.floorMod(mode, FictionRules.ALL_MODES)]; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (Math.floorMod(mode, FictionRules.ALL_MODES)) {
            case FictionRules.UNDO -> SkillMsg.mode("fiction.undo", "Undo");
            case FictionRules.ERASE -> SkillMsg.mode("fiction.erase", "Fiction");
            case FictionRules.ANNUL -> SkillMsg.mode("fiction.annul", "Annul Damage");
            case FictionRules.REWIND -> SkillMsg.mode("fiction.rewind", "Rewind Step");
            case FictionRules.UNPICK -> SkillMsg.mode("fiction.unpick", "Erase Effects");
            case FictionRules.NULLIFY -> SkillMsg.mode("fiction.nullify", "Nullify Strike");
            case FictionRules.UNMAKE -> SkillMsg.mode("fiction.unmake", "Unmake");
            case FictionRules.SHARE -> SkillMsg.mode("fiction.share", "Share the Fiction");
            case FictionRules.STAND_UP -> SkillMsg.mode("fiction.standup", "Stand Up");
            default -> SkillMsg.mode("fiction.world", "World Fiction");
        };
    }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    // ---------------------------------------------------------------- the tick: memory and the zone
    /** Once a second: remembers your health for the last five seconds (and, rebuilt, your position), and pulses the World Fiction zone if one is open. */
    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (living.level().isClientSide || living.tickCount % 20 != 0) return;
        CompoundTag tag = instance.getOrCreateTag();
        float[] hist = new float[FictionRules.HISTORY_SECONDS];
        for (int i = 0; i < hist.length; i++) hist[i] = tag.getFloat("H" + i);
        FictionRules.push(hist, living.getHealth());
        for (int i = 0; i < hist.length; i++) tag.putFloat("H" + i, hist[i]);
        if (!rebuilt()) return;

        String dim = living.level().dimension().location().toString();
        if (!dim.equals(tag.getString("PDim"))) {          // another dimension: the memory of where you stood is of another world
            tag.putString("PDim", dim);
            tag.putInt("PN", 0);
        }
        double[] x = new double[FictionRules.HISTORY_SECONDS], y = new double[x.length], z = new double[x.length];
        for (int i = 0; i < x.length; i++) { x[i] = tag.getDouble("PX" + i); y[i] = tag.getDouble("PY" + i); z[i] = tag.getDouble("PZ" + i); }
        FictionRules.pushD(x, living.getX());
        FictionRules.pushD(y, living.getY());
        FictionRules.pushD(z, living.getZ());
        for (int i = 0; i < x.length; i++) { tag.putDouble("PX" + i, x[i]); tag.putDouble("PY" + i, y[i]); tag.putDouble("PZ" + i, z[i]); }
        tag.putInt("PN", Math.min(FictionRules.HISTORY_SECONDS, tag.getInt("PN") + 1));

        long now = living.level().getGameTime();
        if (living instanceof ServerPlayer p && FictionRules.worldPulseDue(tag.getLong(WORLD_UNTIL), now, living.tickCount)) {
            if (dim.equals(tag.getString("WorldDim"))) pulseWorld(p, tag);
            else tag.putLong(WORLD_UNTIL, 0L);              // you left the world the zone was in: it closes
        }
    }

    // ---------------------------------------------------------------- the press
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (mode < 0 || mode >= FictionRules.ALL_MODES) return;
        if (mode >= FictionRules.wheel(rebuilt())) { SkillUtil.fail(player, SkillMsg.t("fiction.off", "That part of All Fiction is switched off on this server.")); return; }
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("fiction.cooldown", "Not yet.")); return; }
        ServerLevel level = player.serverLevel();
        CompoundTag tag = instance.getOrCreateTag();
        long now = level.getGameTime();

        if (mode == FictionRules.STAND_UP) {                // asks nothing and costs nothing: it only tells you
            int secs = FictionRules.standUpSeconds(tag.getLong("DeathReadyAt"), now);
            SkillUtil.actionbar(player, (secs == 0 ? SkillMsg.t("fiction.standup_ready", "Your death is ready to be made fiction.")
                    : SkillMsg.t("fiction.standup_wait", "Your death will be fiction again in %s seconds.", secs)).withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }

        boolean done = switch (mode) {
            case FictionRules.UNDO -> undo(instance, player, level);
            case FictionRules.ERASE -> erase(player, level);
            case FictionRules.ANNUL -> annul(instance, player, level, tag, now);
            case FictionRules.REWIND -> rewind(player, level, tag);
            case FictionRules.UNPICK -> unpick(player, level);
            case FictionRules.NULLIFY -> nullify(instance, player, level, tag, now);
            case FictionRules.UNMAKE -> unmake(player, level);
            case FictionRules.SHARE -> share(player, level);
            default -> world(instance, player, level, tag, now);
        };
        if (!done) return;
        cooldown(instance, mode, DMUtil.cooldown(FictionRules.cooldown(mode)));
        SkillFx.sound(level, player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.5F);
        SkillUtil.castVfx(player, 0xFF5A2A7A);
        grow(instance, player, 1);
    }

    private static boolean pay(ServerPlayer p, int mode) { return SkillUtil.spendMagicules(p, DMUtil.cost(FictionRules.cost(mode))); }

    private static Vec3 body(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.5, 0); }

    /** Undo (unchanged): your health returns to what it was five seconds ago and harmful effects vanish. */
    private boolean undo(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        if (!pay(player, FictionRules.UNDO)) return false;
        float past = instance.getOrCreateTag().getFloat("H" + FictionRules.oldestIndex());
        player.setHealth(FictionRules.undoHealth(past, player.getHealth(), player.getMaxHealth()));
        SkillUtil.cleanse(player);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.undo", "It's All Fiction.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, player.position().add(0, 1, 0), 12, 0.5, 0.8, 0.5, 0.05);
        return true;
    }

    /** Fiction (unchanged): erase the creature you look at; a boss (or a protected player) loses its effects and a quarter of its health instead. */
    private boolean erase(ServerPlayer player, ServerLevel level) {
        LivingEntity target = SkillTargets.lookFoe(player, FictionRules.eraseRange());
        if (target == null) { SkillUtil.fail(player, SkillMsg.t("fiction.no_target", "No target.")); return false; }
        if (!pay(player, FictionRules.ERASE)) return false;
        SkillFx.spray(level, true, body(target), 12, 0.4, 0.8, 0.4, 0.05);
        if (FictionRules.erases(GodTier.fullyAffected(target))) {
            if (target instanceof ServerPlayer) GodTier.kill(player, target);   // a player is killed, never silently removed from the world
            else target.discard();                                              // erased: no body, no drops
        } else {
            target.removeAllEffects();
            GodTier.percentDamage(player, target, FictionRules.eraseBossShare());
        }
        return true;
    }

    /** Annul Damage: hits are made not to have happened until a pool of half your maximum health is used up (or six seconds pass). */
    private boolean annul(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag, long now) {
        if (!pay(player, FictionRules.ANNUL)) return false;
        tag.putLong(ANNUL_UNTIL, now + FictionRules.annulTicks());
        tag.putDouble(ANNUL_POOL, FictionRules.annulPool(player.getMaxHealth()));
        instance.markDirty();
        SkillUtil.actionbar(player, SkillMsg.t("fiction.annul", "Harm is made not to have happened.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 14, 0.5, 0.8, 0.5, 0.04);
        return true;
    }

    /** Rewind Step: you stand where you stood up to five seconds ago, if that place is in this world, loaded, free and not too near or far. */
    private boolean rewind(ServerPlayer player, ServerLevel level, CompoundTag tag) {
        int n = tag.getInt("PN");
        if (n < 2 || !level.dimension().location().toString().equals(tag.getString("PDim"))) {
            SkillUtil.fail(player, SkillMsg.t("fiction.rewind_none", "There is nowhere to rewind to yet."));
            return false;
        }
        int at = n - 1;                                     // the oldest place remembered
        Vec3 dest = new Vec3(tag.getDouble("PX" + at), tag.getDouble("PY" + at), tag.getDouble("PZ" + at));
        BlockPos bp = BlockPos.containing(dest);
        if (!FictionRules.rewindOk(player.position().distanceTo(dest)) || !level.isLoaded(bp) || !level.getWorldBorder().isWithinBounds(bp)
                || !level.noCollision(player, player.getBoundingBox().move(dest.subtract(player.position())))) {
            SkillUtil.fail(player, SkillMsg.t("fiction.rewind_blocked", "That place cannot be stood in now."));
            return false;
        }
        if (!pay(player, FictionRules.REWIND)) return false;
        SkillFx.spray(level, true, body(player), 14, 0.4, 0.8, 0.4, 0.05);
        player.connection.teleport(dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        SkillFx.spray(level, true, dest.add(0, 1, 0), 14, 0.4, 0.8, 0.4, 0.05);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.rewind", "You were never there.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** Erase Effects: a foe you look at loses every effect (the buffs too); an ally you look at, or you, every harmful one. */
    private boolean unpick(ServerPlayer player, ServerLevel level) {
        LivingEntity target = SkillTargets.lookLiving(player, FictionRules.unpickRange());
        if (target == null) target = player;
        boolean foe = target != player && SkillTargets.isFoe(player, target);
        if (!pay(player, FictionRules.UNPICK)) return false;
        if (foe) target.removeAllEffects();
        else SkillUtil.cleanse(target);
        SkillFx.spray(level, true, body(target), 12, 0.4, 0.8, 0.4, 0.05);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.unpick", "Their effects never were.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** Nullify Strike: the next hit within ten seconds never happens. */
    private boolean nullify(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag, long now) {
        if (!pay(player, FictionRules.NULLIFY)) return false;
        tag.putLong(NULLIFY_UNTIL, now + FictionRules.nullifyTicks());
        instance.markDirty();
        SkillUtil.actionbar(player, SkillMsg.t("fiction.nullify", "The next blow will never have happened.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 10, 0.4, 0.8, 0.4, 0.04);
        return true;
    }

    /** Unmake: hostile projectiles within ten blocks never existed (at most 32). */
    private boolean unmake(ServerPlayer player, ServerLevel level) {
        List<Projectile> found = hostileProjectiles(player, level, player.position(), FictionRules.unmakeRadius(), FictionRules.unmakeMax());
        if (found.isEmpty()) { SkillUtil.fail(player, SkillMsg.t("fiction.unmake_none", "Nothing near you to unmake.")); return false; }
        if (!pay(player, FictionRules.UNMAKE)) return false;
        int n = unmakeAll(level, found);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.unmake", "%s projectile(s) made fiction.", n).withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.ring(level, player.position(), FictionRules.unmakeRadius() * 0.6, 0x5A2A7A);
        return true;
    }

    /** Share the Fiction: the allies around you (you included, at most eight) lose their harmful effects and are healed a quarter of their health. */
    private boolean share(ServerPlayer player, ServerLevel level) {
        if (!pay(player, FictionRules.SHARE)) return false;
        List<LivingEntity> allies = SkillTargets.alliesAround(player, player.position(), FictionRules.shareRadius());
        int n = 0;
        for (LivingEntity a : allies) {
            if (n >= FictionRules.shareMax()) break;
            SkillUtil.cleanse(a);
            BalanceLaw.heal(a, (float) (a.getMaxHealth() * FictionRules.shareHeal()));
            SkillFx.spray(level, false, body(a), 6, 0.3, 0.6, 0.3, 0.03);
            n++;
        }
        SkillUtil.actionbar(player, SkillMsg.t("fiction.share", "The fiction is shared with %s.", n).withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** World Fiction: a zone round where you stand, open for six seconds. The first pulse comes now, the rest once a second from the tick. */
    private boolean world(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag, long now) {
        if (!pay(player, FictionRules.WORLD)) return false;
        tag.putLong(WORLD_UNTIL, now + FictionRules.worldTicks());
        tag.putDouble("WorldX", player.getX());
        tag.putDouble("WorldY", player.getY());
        tag.putDouble("WorldZ", player.getZ());
        tag.putString("WorldDim", level.dimension().location().toString());
        instance.markDirty();
        SkillUtil.actionbar(player, SkillMsg.t("fiction.world", "Nothing sticks in this world.").withStyle(ChatFormatting.DARK_PURPLE));
        pulseWorld(player, tag);
        return true;
    }

    /** One pulse of the zone: allies in it are cleansed (at most eight), hostile projectiles in it are erased (at most sixteen). */
    private static void pulseWorld(ServerPlayer p, CompoundTag tag) {
        ServerLevel level = p.serverLevel();
        Vec3 centre = new Vec3(tag.getDouble("WorldX"), tag.getDouble("WorldY"), tag.getDouble("WorldZ"));
        double r = FictionRules.worldRadius();
        int n = 0;
        for (LivingEntity a : SkillTargets.alliesAround(p, centre, r)) {
            if (n++ >= FictionRules.shareMax()) break;
            SkillUtil.cleanse(a);
        }
        unmakeAll(level, hostileProjectiles(p, level, centre, r, FictionRules.worldPulseMaxProjectiles()));
        SkillFx.ring(level, centre, r, 0x5A2A7A);
    }

    // ---------------------------------------------------------------- projectiles
    /** Projectiles in a sphere that are not yours, not your allies' and not your summons' (at most {@code max}, found with one box query). */
    private static List<Projectile> hostileProjectiles(ServerPlayer p, ServerLevel level, Vec3 centre, double radius, int max) {
        AABB box = new AABB(centre, centre).inflate(radius);
        List<Projectile> all = level.getEntitiesOfClass(Projectile.class, box, e -> e.isAlive() && e.distanceToSqr(centre) <= radius * radius && hostile(p, e));
        return all.size() > max ? all.subList(0, max) : all;
    }

    private static boolean hostile(ServerPlayer p, Projectile proj) {
        if (!(proj.getOwner() instanceof LivingEntity owner)) return true;   // thrown by nobody we know (a dispenser, a trap): fair to erase
        return !(owner == p || SkillTargets.isOwnedBy(owner, p.getUUID()) || p.isAlliedTo(owner));
    }

    private static int unmakeAll(ServerLevel level, List<Projectile> projectiles) {
        int n = 0;
        for (Projectile e : projectiles) {
            SkillFx.spray(level, true, e.position(), 3, 0.1, 0.1, 0.1, 0.02);
            e.discard();
            n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- the damage hook
    /** Nullify Strike refuses the next hit whole; Annul Damage takes hits out of its pool. What bypasses invulnerability (the void, kill) is never Fiction's to refuse. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !rebuilt()) return true;
        CompoundTag tag = instance.getOrCreateTag();
        long now = owner.level().getGameTime();
        if (FictionRules.windowOpen(tag.getLong(NULLIFY_UNTIL), now)) {
            tag.putLong(NULLIFY_UNTIL, 0L);
            amount.set(0.0F);
            instance.markDirty();
            if (owner.level() instanceof ServerLevel level) SkillFx.spray(level, true, body(owner), 10, 0.4, 0.8, 0.4, 0.05);
            if (owner instanceof ServerPlayer p) SkillUtil.actionbar(p, SkillMsg.t("fiction.nullified", "That blow never happened.").withStyle(ChatFormatting.DARK_PURPLE));
            return true;
        }
        if (FictionRules.windowOpen(tag.getLong(ANNUL_UNTIL), now)) {
            double pool = tag.getDouble(ANNUL_POOL);
            double taken = FictionRules.annulAbsorb(pool, amount.get());
            amount.set((float) (amount.get() - taken));
            pool -= taken;
            tag.putDouble(ANNUL_POOL, pool);
            if (pool <= 0.0) {                              // the pool is spent: the window closes
                tag.putLong(ANNUL_UNTIL, 0L);
                instance.markDirty();
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- the passive
    /** Once per cooldown your own death becomes fiction. */
    @Override
    public boolean onDeath(ManasSkillInstance instance, LivingEntity owner, DamageSource source) {
        if (!(owner instanceof ServerPlayer player)) return true;
        if (player.level().getGameTime() < instance.getOrCreateTag().getLong("DeathReadyAt")) return true;
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        player.setHealth(player.getMaxHealth());
        player.removeAllEffects();
        player.clearFire();
        var server = player.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(SkillMsg.t("fiction.death", "%s's death has been made fiction.", player.getName())
                    .withStyle(ChatFormatting.DARK_PURPLE), false);
        }
        instance.getOrCreateTag().putLong("DeathReadyAt", player.level().getGameTime() + DMUtil.cooldown(FictionRules.deathCooldown()));
        instance.markDirty();
        return false;
    }
}
