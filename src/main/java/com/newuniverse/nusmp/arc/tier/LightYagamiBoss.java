package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.perf.PerfBudgets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: Light Yagami is the gag boss of rank F (docs/bosses/light_yagami.md): not a fight, a scripted scene. When he has a survival player in sight he runs ONE scene at them
 * ({@link LightScene}): the potato chip (his over-serious monologue while he takes and eats a chip, with his voiced line), then the pen (he raises his hand and writes the player's name in the Death
 * Note). If the writing finishes the player dies a real death ({@link LightDeathNote}, setting {@code lightDeathNoteLethal}); killing Light, knocking him away or staggering him with a hard hit
 * before it finishes cancels the scene and he is laughed at (and drops a funny reward). The SERVER decides everything here; the client only draws the cinematic from a tiny packet
 * ({@link LightNet}) sent to the players near Light.
 *
 * <p>He stands on the tier-boss framework ({@link TierBoss}): boss bar, story credit, magic stones, the five token moves of his row between scenes, no regeneration. Four things differ: he opens at
 * 100% power so no Gates ever open and no phase invulnerability can shield him (a gag boss that cannot be killed is not funny), he has no refill, his knockback resistance is zero, and while a scene
 * runs he casts nothing and stands still.
 *
 * <p>Cost: when idle, one comparison per tick and one scene check every 10 ticks; during a scene a handful of distance checks per tick; a scene sends two small packets to each player within 32
 * blocks (at most {@link #MAX_VIEWERS}) and plays at most about ten sounds; at most {@link LightScenes#MAX_ACTIVE} scenes run on the whole server.
 */
public class LightYagamiBoss extends TierBoss {
    /** Players within this many blocks of Light see the cinematic (the target always does, up to the scene's own range). */
    public static final double VIEW_RADIUS = 32.0;
    public static final int MAX_VIEWERS = 12;
    /** The target must be at least this close and at most this far, and in sight, for the scene to begin. */
    public static final double START_MIN = 3.0, START_MAX = 18.0;
    /** Ticks after the fight starts (and after a scene ends) before the next scene may begin. */
    public static final int FIRST_DELAY = 50;

    private LightScene scene;
    private Vec3 anchor = Vec3.ZERO;
    private int targetEntityId;
    private final List<UUID> viewers = new ArrayList<>();
    private long nextScene;
    private boolean diedMidScene;
    /** Test seams (package-private, never set by the game): a stand-in player that is not in the player list, and a forced value of the lethal setting. */
    ServerPlayer testTarget;
    static volatile Boolean lethalOverride;

    public LightYagamiBoss(EntityType<? extends TierBoss> type, Level level, TierRow row) {
        super(type, level, row);
    }

    /** Light's attributes: the tier framework's, with NO knockback resistance (knocking him away is the counterplay). */
    public static AttributeSupplier.Builder attributes(com.newuniverse.nusmp.arc.ArcCharacter c) {
        return TierBosses.attributes(c).add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    // ================================================================ what makes him a gag boss
    @Override protected int openingPower() { return PowerCurve.FULL; }

    @Override protected int gatesForm() { return FORM_BASE; }

    @Override protected boolean refillOnGates() { return false; }

    @Override public String barTitle() { return bossName(); }

    @Override protected void onFightStart() {
        super.onFightStart();
        nextScene = tickCount + FIRST_DELAY;
    }

    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        return scene != null ? -1 : super.chooseMove(bk, s, t);        // he casts nothing while the page is open
    }

    /** Is a scene of his running right now (server side)? */
    public boolean sceneRunning() { return scene != null && scene.running(); }

    public LightScene scene() { return scene; }

    // ================================================================ the scene, every tick
    @Override
    protected void bossTick() {
        super.bossTick();
        if (level().isClientSide || !(level() instanceof ServerLevel sl) || isDeadOrDying()) return;
        if (scene != null) {
            stepScene(sl);
            return;
        }
        if (tickCount % 10 != 0 || tickCount < nextScene || state() != STATE_COMBAT) return;
        if (!(getTarget() instanceof ServerPlayer p) || !LightDeathNote.eligible(p) || p.level() != sl) return;
        double d = distanceTo(p);
        if (d < START_MIN || d > START_MAX || !hasLineOfSight(p)) return;
        if (!startSceneFor(sl, p)) nextScene = tickCount + 40;                     // full, or the player is busy or cooling down: look again in two seconds
    }

    /** Asks the registry for a scene at this player and begins it. False when the caps or the cooldowns say no. (Package-private: the self-test drives it too.) */
    boolean startSceneFor(ServerLevel sl, ServerPlayer p) {
        LightScene[] out = new LightScene[1];
        if (LightScenes.start(getUUID(), p.getUUID(), sl.getGameTime(), out) != LightScenes.Start.STARTED) return false;
        begin(sl, out[0], p);
        return true;
    }

    private void begin(ServerLevel sl, LightScene s, ServerPlayer target) {
        scene = s;
        anchor = position();
        targetEntityId = target.getId();
        diedMidScene = false;
        viewers.clear();
        for (ServerPlayer v : sl.players()) {
            if (viewers.size() >= MAX_VIEWERS) break;
            if (v == target || (v.distanceToSqr(this) <= VIEW_RADIUS * VIEW_RADIUS && !v.isSpectator())) viewers.add(v.getUUID());
        }
        getNavigation().stop();
        if (PerfBudgets.effectPacket(sl.getGameTime())) send(sl, LightNet.OP_START);
        sl.playSound(null, getX(), getY(), getZ(), LightSounds.CHIP.get(), SoundSource.VOICE, 2.0f, 1.0f);
    }

    /** One tick of the running scene (package-private: the self-test drives it without waiting for a real tick). */
    void stepScene(ServerLevel sl) {
        LightScene s = scene;
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(s.target());
        if (p == null && testTarget != null && testTarget.getUUID().equals(s.target())) p = testTarget;
        LightScene.Step step;
        if (!s.running()) step = s.phase() == LightScene.Phase.CANCELLED ? LightScene.Step.CANCELLED : LightScene.Step.CONTINUE;      // cancelled from outside (logout, death, dimension change)
        else step = s.tick(observe(sl, p));
        switch (step) {
            case CANCELLED -> cancelled(sl, s);
            case FINISH_KILL -> finish(sl, s, p, true);
            case FINISH_JOKE -> finish(sl, s, p, false);
            case PHASE_CHANGED -> {
                phaseSound(sl, s);
                hold(p);
            }
            default -> {
                if (s.running()) {
                    cues(sl, s);
                    hold(p);
                } else {
                    clear(sl, s);                                                  // DONE without a finish step: nothing may linger
                }
            }
        }
    }

    private LightScene.Obs observe(ServerLevel sl, ServerPlayer p) {
        boolean present = p != null && p.isAlive() && p.level() == sl;
        return new LightScene.Obs(isAlive() && !isDeadOrDying(), position().distanceTo(anchor), present, present && LightDeathNote.eligible(p),
                present ? distanceTo(p) : 0.0, lethalOverride != null ? lethalOverride : ArcConfig.lightDeathNoteLethal());
    }

    /** He stands still, facing the player, for the whole scene. */
    private void hold(ServerPlayer p) {
        getNavigation().stop();
        if (p != null) getLookControl().setLookAt(p, 40f, 40f);
    }

    /** The little sounds of the beats (all ordinary level sounds, heard within a few blocks). */
    private void cues(ServerLevel sl, LightScene s) {
        int age = s.age();
        if (age == 44) {                                                            // the chip is eaten
            swing(InteractionHand.MAIN_HAND);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 1.2f, 1.0f);
        }
        if (s.phase() == LightScene.Phase.WRITE && age % 10 == 0) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BRUSH_GENERIC, SoundSource.HOSTILE, 0.7f, 1.6f);
    }

    private void phaseSound(ServerLevel sl, LightScene s) {
        swing(InteractionHand.MAIN_HAND);
        if (s.phase() == LightScene.Phase.RAISE) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1.0f, 0.8f);
        else if (s.phase() == LightScene.Phase.WRITE) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    /** The scene was cancelled (the reason is in the scene): tell the viewers, taunt, and rest before another try. */
    private void cancelled(ServerLevel sl, LightScene s) {
        LightScene.Cancel why = s.cancelReason();
        if (why == LightScene.Cancel.KNOCKED_AWAY) say("My page! You will pay for this.");
        else if (why == LightScene.Cancel.TARGET_ESCAPED) say("Running will not hide your name.");
        if (why == LightScene.Cancel.LIGHT_DIED) diedMidScene = true;
        clear(sl, s);
        send(sl, LightNet.OP_CANCEL);
        nextScene = tickCount + LightScenes.LIGHT_COOLDOWN;
        scene = null;
    }

    private void finish(ServerLevel sl, LightScene s, ServerPlayer p, boolean lethal) {
        int quote = getRandom().nextInt(LightSounds.DEATH_QUOTES.length);
        say(quote == 0 ? "Just as planned." : "I am justice!");
        if (p != null) {
            SoundEvent e = LightSounds.quote(quote);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), e, SoundSource.VOICE, 2.0f, 1.0f);
            if (lethal) {
                LightDeathNote.Result r = LightDeathNote.strike(p);
                if (r == LightDeathNote.Result.SAVED_BY_TOTEM) announce(p.getName().getString() + "'s Totem of Undying shattered the Death Note.");
                else if (r == LightDeathNote.Result.RESISTED) announce("The Death Note refused to work on " + p.getName().getString() + ".");
            } else {
                p.displayClientMessage(Component.translatable("nusmp.light_yagami.joke"), true);
            }
        }
        clear(sl, s);
        send(sl, LightNet.OP_END);
        nextScene = tickCount + LightScenes.LIGHT_COOLDOWN;
        scene = null;
    }

    /** Takes the scene out of the registry (and starts the target's cooldown). */
    private void clear(ServerLevel sl, LightScene s) { LightScenes.end(s, sl.getGameTime()); }

    /** Tells the viewers (those still online and in this level) what happened. A few tiny packets at most. */
    private void send(ServerLevel sl, byte op) {
        for (UUID id : viewers) {
            ServerPlayer v = sl.getServer().getPlayerList().getPlayer(id);
            if (v == null || v.level() != sl) continue;
            byte role = v.getId() == targetEntityId ? LightNet.ROLE_TARGET : LightNet.ROLE_BYSTANDER;
            PacketDistributor.sendToPlayer(v, new LightNet.Payload(op, getId(), targetEntityId, role));
        }
        if (op != LightNet.OP_START) viewers.clear();
    }

    // ================================================================ hits, death, removal
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && scene != null && scene.running() && LightScene.staggers(amount, getMaxHealth())) scene.interrupt(LightScene.Cancel.KNOCKED_AWAY);      // a hard hit ruins the page like a shove does
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (scene != null && scene.running()) diedMidScene = true;          // set before the loot is dropped (the super call drops it)
        super.die(source);
        endForGood(LightScene.Cancel.LIGHT_DIED);
    }

    @Override
    public void remove(RemovalReason reason) {
        endForGood(LightScene.Cancel.LIGHT_DIED);
        super.remove(reason);
    }

    /** He is dead or gone: his scene (if any) is cancelled, the viewers are told, the registry forgets it. Safe to call twice. */
    private void endForGood(LightScene.Cancel why) {
        if (level().isClientSide || scene == null) return;
        LightScene s = scene;
        scene = null;
        s.interrupt(why);
        if (level() instanceof ServerLevel sl) {
            if (why == LightScene.Cancel.LIGHT_DIED) diedMidScene = true;
            LightScenes.end(s, sl.getGameTime());
            send(sl, LightNet.OP_CANCEL);
        } else {
            LightScenes.lightGone(getUUID(), why);
        }
    }

    // ================================================================ the funny reward
    /** His reward: a few potato chips (baked potatoes with a name), and Ryuk's apple if he fell with the pen in his hand. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ItemStack chips = new ItemStack(Items.BAKED_POTATO, 2 + getRandom().nextInt(3));
        chips.set(DataComponents.CUSTOM_NAME, Component.translatable("item.nusmp.light_potato_chip").withStyle(ChatFormatting.YELLOW));
        spawnAtLocation(chips);
        if (diedMidScene) {
            ItemStack apple = new ItemStack(Items.APPLE);
            apple.set(DataComponents.CUSTOM_NAME, Component.translatable("item.nusmp.light_ryuk_apple").withStyle(ChatFormatting.DARK_RED));
            spawnAtLocation(apple);
        }
    }
}
