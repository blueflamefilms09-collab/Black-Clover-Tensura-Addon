package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.UltimateClass;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.skill.rules.FictionRules;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 * God class (docs/all_fiction_rework.md, server setting {@code allFictionGod}), six modes appended after the ten: Deny Skill (a foe loses its effects and its magic is silenced), Overwrite Rule (a zone where a rule
 * is written for twelve seconds), Reversal (up to three hits made not to have happened and written back at the striker), Mass Fiction (mastered only: every foe around you is erased, or stripped and hurt if it is
 * a boss or a player), Nothing Sticks (twenty seconds of harmful effects never staying) and Rewrite Time (the time spent on the lesser modes never was). Passive Retcon: once a second one harmful effect of
 * yours is made not to have happened. Every window is a game time in the owner's own skill tag; every area press looks at no more than 24 creatures and 32 projectiles; every hit goes through the balance law.
 * <p>
 * State is kept in the skill tag of the one player who owns it (nothing static, nothing shared): the five-second health memory (H0..H4, unchanged), the five-second position memory (PX/PY/PZ0..4, PN,
 * PDim), the window ends as game times (AnnulUntil, NullifyUntil, WorldUntil) and the zone (WorldX/Y/Z, WorldDim). The tick writes a few numbers once a second; the zone pulses once a second for six
 * seconds. A press looks at no more than 24 creatures, 32 projectiles. Numbers: {@link FictionRules}. Switch: server setting {@code allFictionRebuilt} (off = the old two modes, the new hooks do nothing).
 */
public class AllFictionSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("all_fiction", "All Fiction",
            "Ultimate (God class). All Fiction, the Great Liar: deny reality and make wounds, effects, blows, enemy skills, time and even your own death never have happened. Eighteen modes, from Undo to Mass Fiction and April Fiction. It cannot erase another Minus and does nothing where there is nothing to erase.");

    private static final String ANNUL_UNTIL = "AnnulUntil", ANNUL_POOL = "AnnulPool", NULLIFY_UNTIL = "NullifyUntil", WORLD_UNTIL = "WorldUntil",
            RULE_UNTIL = "RuleUntil", REVERSAL_UNTIL = "ReversalUntil", REVERSAL_LEFT = "ReversalLeft", SKIN_UNTIL = "SkinUntil";

    public AllFictionSkill() { super(SkillType.ULTIMATE, TEXT, true); }
    @Override public UltimateClass ultimateClass() { return UltimateClass.GOD; }      // docs/ultimate_classes.md

    /** True when the ten-mode rebuild is on; before the server config is loaded (a client on the title screen) it counts as on. */
    static boolean rebuilt() {
        try {
            return NUConfig.ALL_FICTION_REBUILT.get();
        } catch (IllegalStateException notLoaded) {
            return true;
        }
    }

    /** True when the god-class modes are on (needs the rebuild; server setting allFictionGod). */
    static boolean god() { return rebuilt() && NUConfig.allFictionGod(); }

    /** True when the Medaka Box canon pass is on (needs the god-class modes; server setting allFictionCanon): the Minus limit, nothing-to-erase refusals, Erase Presence, April Fiction. */
    static boolean canon() { return god() && NUConfig.allFictionCanon(); }

    /** Canon: All Fiction cannot erase another Minus. Here a Minus is a player who holds Book Maker or any Ultimate skill (the "Minus equivalent"); creatures and bosses are guarded by {@link GodTier#fullyAffected}. */
    static boolean isMinus(LivingEntity e) {
        if (!(e instanceof ServerPlayer p)) return false;
        Skills skills;
        try { skills = SkillAPI.getSkillsFrom(p); } catch (Throwable noSkills) { return false; }
        if (skills == null) return false;
        if (skills.getSkill(NUSkills.BOOK_MAKER.getId()).isPresent()) return true;
        for (ManasSkillInstance i : skills.getLearnedSkills()) if (i.getSkill() instanceof Skill s && s.getType() == Skill.SkillType.ULTIMATE) return true;
        return false;
    }

    private static boolean harmful(LivingEntity e) {
        for (MobEffectInstance m : e.getActiveEffects()) if (!m.getEffect().value().isBeneficial()) return true;
        return false;
    }

    @Override public int getModes(ManasSkillInstance i) { return FictionRules.wheel(rebuilt(), god(), canon()); }

    @Override
    public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
        int n = FictionRules.wheel(rebuilt(), god(), canon());
        return Math.floorMod(mode + (reverse ? -1 : 1), n);
    }

    @Override public String getModeId(ManasSkillInstance i, int mode) { return FictionRules.CANON_IDS[Math.floorMod(mode, FictionRules.CANON_MODES)]; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (Math.floorMod(mode, FictionRules.CANON_MODES)) {
            case FictionRules.UNDO -> SkillMsg.mode("fiction.undo", "Undo");
            case FictionRules.ERASE -> SkillMsg.mode("fiction.erase", "Fiction");
            case FictionRules.ANNUL -> SkillMsg.mode("fiction.annul", "Annul Damage");
            case FictionRules.REWIND -> SkillMsg.mode("fiction.rewind", "Rewind Step");
            case FictionRules.UNPICK -> SkillMsg.mode("fiction.unpick", "Erase Effects");
            case FictionRules.NULLIFY -> SkillMsg.mode("fiction.nullify", "Nullify Strike");
            case FictionRules.UNMAKE -> SkillMsg.mode("fiction.unmake", "Unmake");
            case FictionRules.SHARE -> SkillMsg.mode("fiction.share", "Share the Fiction");
            case FictionRules.STAND_UP -> SkillMsg.mode("fiction.standup", "Stand Up");
            case FictionRules.DENY -> SkillMsg.mode("fiction.deny", "Deny Skill");
            case FictionRules.RULE -> SkillMsg.mode("fiction.rule", "Overwrite Rule");
            case FictionRules.REVERSAL -> SkillMsg.mode("fiction.reversal", "Reversal");
            case FictionRules.MASS -> SkillMsg.mode("fiction.mass", "Mass Fiction");
            case FictionRules.SKIN -> SkillMsg.mode("fiction.skin", "Nothing Sticks");
            case FictionRules.REWRITE_TIME -> SkillMsg.mode("fiction.rewritetime", "Rewrite Time");
            case FictionRules.PRESENCE -> SkillMsg.mode("fiction.presence", "Erase Presence");
            case FictionRules.APRIL -> SkillMsg.mode("fiction.april", "April Fiction");
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
        if (!god()) return;
        if (living instanceof ServerPlayer p && FictionRules.worldPulseDue(tag.getLong(RULE_UNTIL), now, living.tickCount)) {
            if (dim.equals(tag.getString("RuleDim"))) pulseRule(p, tag);
            else tag.putLong(RULE_UNTIL, 0L);               // you left the world the rule was written in: it is crossed out
        }
        if (FictionRules.windowOpen(tag.getLong(SKIN_UNTIL), now)) SkillUtil.cleanse(living);   // Nothing Sticks
        else retcon(living);                                                                    // the passive: one harmful effect a second
    }

    // ---------------------------------------------------------------- the press
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (mode < 0 || mode >= FictionRules.CANON_MODES) return;
        if (mode >= FictionRules.wheel(rebuilt(), god(), canon())) { SkillUtil.fail(player, SkillMsg.t("fiction.off", "That part of All Fiction is switched off on this server.")); return; }
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
            case FictionRules.ERASE -> erase(instance, player, level);
            case FictionRules.ANNUL -> annul(instance, player, level, tag, now);
            case FictionRules.REWIND -> rewind(instance, player, level, tag);
            case FictionRules.UNPICK -> unpick(instance, player, level);
            case FictionRules.NULLIFY -> nullify(instance, player, level, tag, now);
            case FictionRules.UNMAKE -> unmake(instance, player, level);
            case FictionRules.SHARE -> share(instance, player, level);
            case FictionRules.WORLD -> world(instance, player, level, tag, now);
            case FictionRules.DENY -> deny(instance, player, level);
            case FictionRules.RULE -> rule(instance, player, level, tag, now);
            case FictionRules.REVERSAL -> reversal(instance, player, level, tag, now);
            case FictionRules.MASS -> mass(instance, player, level);
            case FictionRules.SKIN -> skin(instance, player, level, tag, now);
            case FictionRules.REWRITE_TIME -> rewriteTime(instance, player, level);
            case FictionRules.PRESENCE -> presence(instance, player, level);
            case FictionRules.APRIL -> april(instance, player, level);
            default -> false;
        };
        if (!done) return;
        cooldown(instance, mode, DMUtil.cooldown(FictionRules.cooldown(mode)));
        SkillFx.sound(level, player.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.5F);
        if (mode != FictionRules.ERASE) SkillAnimeFx.screwRing(level, player.position(), 5);      // VFX wave 3: the screws slam down (Fiction itself drives one into its target)
        SkillUtil.castVfx(player, 0xFF5A2A7A);
        grow(instance, player, 1);
    }

    private static boolean pay(ManasSkillInstance instance, ServerPlayer p, int mode) { return pay(instance, p, mode, DMUtil.cost(FictionRules.cost(mode))); }

    private static Vec3 body(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.5, 0); }

    /** Undo (unchanged): your health returns to what it was five seconds ago and harmful effects vanish. */
    private boolean undo(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        float past = instance.getOrCreateTag().getFloat("H" + FictionRules.oldestIndex());
        if (canon() && !FictionRules.undoHasWork(past, player.getHealth(), player.getMaxHealth(), harmful(player))) {      // canon: no real damage, All Fiction does nothing (and costs nothing)
            SkillUtil.fail(player, SkillMsg.t("fiction.nothing_to_undo", "There is nothing to undo."));
            return false;
        }
        if (!pay(instance, player, FictionRules.UNDO)) return false;
        player.setHealth(FictionRules.undoHealth(past, player.getHealth(), player.getMaxHealth()));
        SkillUtil.cleanse(player);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.undo", "It's All Fiction.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, player.position().add(0, 1, 0), 12, 0.5, 0.8, 0.5, 0.05);
        return true;
    }

    /** Fiction (unchanged): erase the creature you look at; a boss (or a protected player) loses its effects and a quarter of its health instead. */
    private boolean erase(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        LivingEntity target = SkillTargets.lookFoe(player, FictionRules.eraseRange());
        if (target == null) { SkillUtil.fail(player, SkillMsg.t("fiction.no_target", "No target.")); return false; }
        if (!pay(instance, player, FictionRules.ERASE)) return false;
        SkillFx.spray(level, true, body(target), 12, 0.4, 0.8, 0.4, 0.05);
        SkillAnimeFx.screwShot(level, player.getEyePosition(), body(target));            // VFX wave 3: a giant screw is driven into it
        if (canon() && isMinus(target)) {                   // canon: it cannot erase another Minus, it only removes the unwanted part
            target.removeAllEffects();
            SkillUtil.actionbar(player, SkillMsg.t("fiction.minus", "A Minus cannot be erased. Only what was unwanted is gone.").withStyle(ChatFormatting.DARK_PURPLE));
        } else if (FictionRules.canErase(GodTier.fullyAffected(target), canon(), false)) {
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
        if (!pay(instance, player, FictionRules.ANNUL)) return false;
        tag.putLong(ANNUL_UNTIL, now + FictionRules.annulTicks());
        tag.putDouble(ANNUL_POOL, FictionRules.annulPool(player.getMaxHealth()));
        instance.markDirty();
        SkillUtil.actionbar(player, SkillMsg.t("fiction.annul", "Harm is made not to have happened.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 14, 0.5, 0.8, 0.5, 0.04);
        return true;
    }

    /** Rewind Step: you stand where you stood up to five seconds ago, if that place is in this world, loaded, free and not too near or far. */
    private boolean rewind(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag) {
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
        if (!pay(instance, player, FictionRules.REWIND)) return false;
        SkillFx.spray(level, true, body(player), 14, 0.4, 0.8, 0.4, 0.05);
        player.connection.teleport(dest.x, dest.y, dest.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        SkillFx.spray(level, true, dest.add(0, 1, 0), 14, 0.4, 0.8, 0.4, 0.05);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.rewind", "You were never there.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** Erase Effects: a foe you look at loses every effect (the buffs too); an ally you look at, or you, every harmful one. */
    private boolean unpick(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        LivingEntity target = SkillTargets.lookLiving(player, FictionRules.unpickRange());
        if (target == null) target = player;
        boolean foe = target != player && SkillTargets.isFoe(player, target);
        if (canon() && (foe ? target.getActiveEffects().isEmpty() : !harmful(target))) {      // canon: nothing real to erase
            SkillUtil.fail(player, SkillMsg.t("fiction.nothing_to_undo", "There is nothing to undo."));
            return false;
        }
        if (!pay(instance, player, FictionRules.UNPICK)) return false;
        if (foe) target.removeAllEffects();
        else SkillUtil.cleanse(target);
        SkillFx.spray(level, true, body(target), 12, 0.4, 0.8, 0.4, 0.05);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.unpick", "Their effects never were.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** Nullify Strike: the next hit within ten seconds never happens. */
    private boolean nullify(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag, long now) {
        if (!pay(instance, player, FictionRules.NULLIFY)) return false;
        tag.putLong(NULLIFY_UNTIL, now + FictionRules.nullifyTicks());
        instance.markDirty();
        SkillUtil.actionbar(player, SkillMsg.t("fiction.nullify", "The next blow will never have happened.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 10, 0.4, 0.8, 0.4, 0.04);
        return true;
    }

    /** Unmake: hostile projectiles within ten blocks never existed (at most 32). */
    private boolean unmake(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        List<Projectile> found = hostileProjectiles(player, level, player.position(), FictionRules.unmakeRadius(), FictionRules.unmakeMax());
        if (found.isEmpty()) { SkillUtil.fail(player, SkillMsg.t("fiction.unmake_none", "Nothing near you to unmake.")); return false; }
        if (!pay(instance, player, FictionRules.UNMAKE)) return false;
        int n = unmakeAll(level, found);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.unmake", "%s projectile(s) made fiction.", n).withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.ring(level, player.position(), FictionRules.unmakeRadius() * 0.6, 0x5A2A7A);
        return true;
    }

    /** Share the Fiction: the allies around you (you included, at most eight) lose their harmful effects and are healed a quarter of their health. */
    private boolean share(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        if (!pay(instance, player, FictionRules.SHARE)) return false;
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
        if (!pay(instance, player, FictionRules.WORLD)) return false;
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


    // ---------------------------------------------------------------- god class: the six appended modes
    /** Deny Skill: the foe you look at loses every effect and its magic is silenced (balance-law control time: a boss one second, a player three), then it is hit. */
    private boolean deny(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        LivingEntity target = SkillTargets.lookFoe(player, FictionRules.denyRange());
        if (target == null) { SkillUtil.fail(player, SkillMsg.t("fiction.no_target", "No target.")); return false; }
        if (!pay(instance, player, FictionRules.DENY)) return false;
        target.removeAllEffects();
        EnergyBridge.effect(target, "silence", BalanceLaw.controlTicks(target, FictionRules.denySilenceTicks()), 0);
        SkillDamage.hit(player, target, FictionRules.denyDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC, instance);
        SkillFx.spray(level, true, body(target), 14, 0.4, 0.8, 0.4, 0.05);
        SkillAnimeFx.screwShot(level, player.getEyePosition(), body(target));
        SkillUtil.actionbar(player, SkillMsg.t("fiction.deny", "Its power never was.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** Overwrite Rule: a rule is written into the ground where you stand for twelve seconds (first pulse now, the rest once a second from the tick). */
    private boolean rule(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag, long now) {
        if (!pay(instance, player, FictionRules.RULE)) return false;
        tag.putLong(RULE_UNTIL, now + FictionRules.ruleTicks());
        tag.putDouble("RuleX", player.getX());
        tag.putDouble("RuleY", player.getY());
        tag.putDouble("RuleZ", player.getZ());
        tag.putString("RuleDim", level.dimension().location().toString());
        instance.markDirty();
        SkillUtil.actionbar(player, SkillMsg.t("fiction.rule", "A new rule is written here.").withStyle(ChatFormatting.DARK_PURPLE));
        pulseRule(player, tag);
        return true;
    }

    /** One pulse of the rule: foes in it (at most 24) are weakened and silenced, allies (at most 8) cleansed, hostile projectiles (at most 16) erased. */
    private static void pulseRule(ServerPlayer p, CompoundTag tag) {
        ServerLevel level = p.serverLevel();
        Vec3 centre = new Vec3(tag.getDouble("RuleX"), tag.getDouble("RuleY"), tag.getDouble("RuleZ"));
        double r = FictionRules.ruleRadius();
        for (LivingEntity f : SkillTargets.foes(p, centre, r)) {
            f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, FictionRules.ruleWeakTicks(), 1));
            EnergyBridge.effect(f, "silence", BalanceLaw.controlTicks(f, FictionRules.ruleSilenceTicks()), 0);
        }
        int n = 0;
        for (LivingEntity a : SkillTargets.alliesAround(p, centre, r)) {
            if (n++ >= FictionRules.shareMax()) break;
            SkillUtil.cleanse(a);
        }
        unmakeAll(level, hostileProjectiles(p, level, centre, r, FictionRules.rulePulseMaxProjectiles()));
        SkillFx.ring(level, centre, r, 0x7A2AB0);
    }

    /** Reversal: for eight seconds the next three hits you take are made not to have happened and are written back at who struck (see {@link #onTakenDamage}). */
    private boolean reversal(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag, long now) {
        if (!pay(instance, player, FictionRules.REVERSAL)) return false;
        tag.putLong(REVERSAL_UNTIL, now + FictionRules.reversalTicks());
        tag.putInt(REVERSAL_LEFT, FictionRules.reversalCharges());
        instance.markDirty();
        SkillUtil.actionbar(player, SkillMsg.t("fiction.reversal", "Harm is written back to its maker.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 14, 0.5, 0.8, 0.5, 0.04);
        return true;
    }

    /** Mass Fiction (mastered): every foe within fourteen blocks (at most 24): ordinary creatures are erased; a boss or a player loses its effects, is silenced and takes a hit. */
    private boolean mass(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        if (!FictionRules.massUnlocked(instance.isMastered(player))) {
            SkillUtil.fail(player, SkillMsg.t("fiction.mass_locked", "Master All Fiction to unlock Mass Fiction."));
            return false;
        }
        List<LivingEntity> foes = SkillTargets.foes(player, player.position(), FictionRules.massRadius());
        if (foes.isEmpty()) { SkillUtil.fail(player, SkillMsg.t("fiction.mass_none", "Nothing near you to make fiction.")); return false; }
        if (!pay(instance, player, FictionRules.MASS)) return false;
        int erased = 0;
        for (LivingEntity f : foes) {
            SkillFx.spray(level, true, body(f), 8, 0.4, 0.8, 0.4, 0.05);
            if (FictionRules.massErases(f instanceof Player, GodTier.fullyAffected(f)) && !(canon() && isMinus(f))) {
                f.discard();                                // erased: no body, no drops
                erased++;
            } else {
                f.removeAllEffects();
                EnergyBridge.effect(f, "silence", BalanceLaw.controlTicks(f, FictionRules.massSilenceTicks()), 0);
                SkillDamage.hit(player, f, FictionRules.massDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC, instance);
            }
        }
        SkillFx.ring(level, player.position(), FictionRules.massRadius() * 0.7, 0x7A2AB0);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.mass", "%s made fiction.", erased).withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** Nothing Sticks: for twenty seconds your harmful effects are cleansed once a second (and now). */
    private boolean skin(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, CompoundTag tag, long now) {
        if (!pay(instance, player, FictionRules.SKIN)) return false;
        tag.putLong(SKIN_UNTIL, now + FictionRules.skinTicks());
        instance.markDirty();
        SkillUtil.cleanse(player);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.skin", "Nothing sticks to you.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 12, 0.4, 0.8, 0.4, 0.04);
        return true;
    }

    /** Rewrite Time: the time spent on the lesser modes never was, so their cooldowns are gone. The big modes keep theirs. */
    private boolean rewriteTime(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        if (!pay(instance, player, FictionRules.REWRITE_TIME)) return false;
        for (int m = 0; m < FictionRules.GOD_MODES; m++) if (FictionRules.resettable(m)) instance.setCoolDown(0, m);
        SkillUtil.actionbar(player, SkillMsg.t("fiction.rewritetime", "The time you spent never was.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 16, 0.5, 0.9, 0.5, 0.05);
        return true;
    }

    // ---------------------------------------------------------------- canon pass: the two appended modes
    /** Erase Presence (canon: Kumagawa erases his own aura): invisible for 20 s; creatures hunting you (not bosses, at most 24) lose you. */
    private boolean presence(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        if (!pay(instance, player, FictionRules.PRESENCE)) return false;
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, FictionRules.presenceTicks(), 0, false, false));
        int n = 0;
        for (LivingEntity f : SkillTargets.foes(player, player.position(), FictionRules.presenceRadius())) {
            if (f instanceof Mob m && !BalanceLaw.isBoss(m) && m.getTarget() == player) { m.setTarget(null); n++; }
        }
        SkillUtil.actionbar(player, SkillMsg.t("fiction.presence", "Nobody feels your presence.").withStyle(ChatFormatting.DARK_PURPLE));
        SkillFx.spray(level, true, body(player), 12, 0.4, 0.8, 0.4, 0.04);
        return true;
    }

    /** April Fiction (canon: All Fiction mixed with Unskilled, the erase lasts three minutes and then returns): the foe you look at loses its boons and its strength is erased (Weakness) for a time that ends by itself (creature 3 min, player 30 s, boss 20 s). It cannot erase a Minus. */
    private boolean april(ManasSkillInstance instance, ServerPlayer player, ServerLevel level) {
        LivingEntity target = SkillTargets.lookFoe(player, FictionRules.denyRange());
        if (target == null) { SkillUtil.fail(player, SkillMsg.t("fiction.no_target", "No target.")); return false; }
        if (isMinus(target)) { SkillUtil.fail(player, SkillMsg.t("fiction.minus", "A Minus cannot be erased. Only what was unwanted is gone.")); return false; }
        if (!pay(instance, player, FictionRules.APRIL)) return false;
        target.removeAllEffects();
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.min(FictionRules.aprilMaxTicks(), FictionRules.aprilTicks(BalanceLaw.isBoss(target), target instanceof Player)), FictionRules.aprilWeakAmp()));
        SkillFx.spray(level, true, body(target), 14, 0.4, 0.8, 0.4, 0.05);
        SkillAnimeFx.screwShot(level, player.getEyePosition(), body(target));
        SkillUtil.actionbar(player, SkillMsg.t("fiction.april", "Its strength is gone, for a while.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /** The passive Retcon, once a second: the shortest harmful effect of the owner is made not to have happened (one effect at most). */
    private static void retcon(LivingEntity owner) {
        MobEffectInstance pick = null;
        for (MobEffectInstance e : owner.getActiveEffects()) {
            if (e.getEffect().value().isBeneficial()) continue;
            if (pick == null || e.getDuration() < pick.getDuration()) pick = e;
        }
        if (pick != null) owner.removeEffect(pick.getEffect());
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
        if (god() && owner instanceof ServerPlayer rp && FictionRules.reversalOpen(tag.getLong(REVERSAL_UNTIL), now, tag.getInt(REVERSAL_LEFT))) {
            float hit = amount.get();
            int left = tag.getInt(REVERSAL_LEFT) - 1;
            tag.putInt(REVERSAL_LEFT, left);
            if (left <= 0) tag.putLong(REVERSAL_UNTIL, 0L);
            amount.set(0.0F);
            instance.markDirty();
            if (source.getEntity() instanceof LivingEntity striker && striker != owner) {
                SkillDamage.hit(rp, striker, FictionRules.reversalRaw(hit), masteryFrac(instance), SkillDamage.Kind.MAGIC, instance);
                SkillFx.spray(rp.serverLevel(), true, body(striker), 8, 0.3, 0.6, 0.3, 0.04);
            }
            SkillUtil.actionbar(rp, SkillMsg.t("fiction.reversed", "That blow was written back.").withStyle(ChatFormatting.DARK_PURPLE));
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
        if (god() && amount.get() > 0 && FictionRules.windowOpen(tag.getLong(RULE_UNTIL), now)                  // Overwrite Rule: the owner takes half inside the zone
                && owner.level().dimension().location().toString().equals(tag.getString("RuleDim"))
                && FictionRules.inZone(owner.distanceToSqr(tag.getDouble("RuleX"), tag.getDouble("RuleY"), tag.getDouble("RuleZ")), FictionRules.ruleRadius())) {
            amount.set((float) (amount.get() * FictionRules.ruleOwnerTakenMultiplier()));
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
