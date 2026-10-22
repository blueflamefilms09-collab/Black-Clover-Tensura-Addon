package com.newuniverse.nusmp.spiritdark;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Decision;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Event;
import com.newuniverse.nusmp.spiritdark.DarkMoodRules.Pose;
import com.newuniverse.nusmp.spiritdark.SpiritDarkSoundRules.Cue;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the Spirit of Darkness (docs/magic/spirit_dark.md): a shadow-wisp companion that a Dark Magic caster links with a key, and a powered miniature demon the caster may call for fifteen seconds.
 * While the spirit is linked: a melee strike gives back a small capped share of the damage as health ({@link SpiritDarkRules#stealOfHit}, per strike and per second), marks the struck foe with Wither
 * or Blindness for a few seconds (once per foe per five seconds, {@link SpiritDarkRules.TargetLedger}) and the Shadow Step key moves the owner up to 14 blocks to a validated spot, preferring dark ones
 * ({@link ShadowStep}). Nothing here touches hunger, takes a percentage of anyone's maximum health or kills outright. The spirit also has moods (poses, faces, spoken lines: {@link DarkMoodRules}).
 *
 * <p>The server decides everything; the clients draw the props (client.prop.SpiritDarkPropPainter) from the prop word alone. All state is per player UUID in {@link #STATES} (only players who linked
 * ever get an entry) and is dropped on logout, death, respawn, dimension change and server stop. Nothing here scans the world on a tick; the one search (the Shadow Step destination) runs once per
 * step, at most every nine seconds per player. The server-wide caps are {@link SpiritDarkConfig} values.</p>
 */
public final class SpiritDark {
    private SpiritDark() {}

    /** The state of one player's spirit. */
    static final class State {
        long lastPacket = Long.MIN_VALUE / 4, lastMessage = Long.MIN_VALUE / 4, stepReadyAt, stepRetryAt, powerReadyAt, nextWhisper, nextFlutter;
        UUID companion, powered;
        final SpiritDarkRules.StealWindow steal = new SpiritDarkRules.StealWindow();
        final SpiritDarkRules.TargetLedger marks = new SpiritDarkRules.TargetLedger();
        final DarkMoodRules.Director mood = new DarkMoodRules.Director();
        final Map<String, String> lastLine = new HashMap<>();
        int lastMark = -1;
    }

    private static final ThemeKey DARK = ThemeKey.of("magic.DARK");
    private static final int COLOR = 0xFF5B3A8A;
    /** A prop lives practically forever: it ends when the link does (unlink, logout, death, dimension change). */
    private static final int LIFE = 1_200_000;

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players that have a spirit out: only these are visited by the once-a-few-ticks upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final DarkCapLedger SPIRITS = new DarkCapLedger(), POWERED = new DarkCapLedger();

    private static State state(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    // ================================================================================================ who may
    /** May this player have the spirit: the switch is on and they hold Dark Magic (the Dark grimoire's skill). */
    public static boolean eligible(ServerPlayer p) {
        if (!SpiritDarkConfig.enabled()) return false;
        return SkillAPI.getSkillsFrom(p).getSkill(NUSkills.BOOK_DARK.getId()).isPresent();
    }

    private static void say(ServerPlayer p, State s, String key, String text, ChatFormatting color) {
        long now = p.level().getGameTime();
        if (!SpiritDarkRules.gapOk(now, s.lastMessage, SpiritDarkRules.MESSAGE_GAP)) return;
        s.lastMessage = now;
        p.displayClientMessage(Component.translatableWithFallback(key, text).withStyle(color), true);
    }

    // ================================================================================================ the key
    /** The server entry of the spirit's three actions (until 3.0.0 the key packet's work; now called by the spirit control plane, {@code SpiritDarkControl}): validated, rate limited per player. */
    public static void onKey(ServerPlayer p, int action) {
        if (!SpiritDarkRules.validAction(action)) return;
        State s = state(p.getUUID());
        long now = p.level().getGameTime();
        if (!SpiritDarkRules.gapOk(now, s.lastPacket, SpiritDarkRules.PACKET_GAP)) return;
        s.lastPacket = now;
        if (!eligible(p)) {
            if (s.companion == null && s.powered == null) STATES.remove(p.getUUID(), s);
            else forget(p);
            return;
        }
        switch (action) {
            case SpiritDarkRules.ACT_LINK -> { if (s.companion != null) unlink(p, s, true); else link(p, s); }
            case SpiritDarkRules.ACT_STEP -> step(p, s);
            default -> power(p, s);
        }
    }

    // ================================================================================================ link and unlink
    static boolean link(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        if (!SPIRITS.acquire(p.getUUID(), SpiritDarkRules.SPIRITS_PER_PLAYER, SpiritDarkConfig.maxSpirits())) {
            say(p, s, "nusmp.spirit_dark.full", "The Spirit of Darkness cannot answer: too many spirits are out.", ChatFormatting.RED);
            return false;
        }
        long now = sl.getGameTime();
        double[] a = SpiritDarkRules.companion(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SPIRITDARK_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, LIFE, SpiritDarkRules.plain(SpiritDarkRules.V_COMPANION, false), p);
        if (e == null) { SPIRITS.release(p.getUUID()); return false; }
        e.setTarget(p);
        s.companion = e.getUUID();
        LIVE.add(p.getUUID());
        s.nextWhisper = now + 200 + p.getRandom().nextInt(200);
        s.mood.armIdle(now, p.getRandom().nextInt(DarkMoodRules.IDLE_EVERY));
        Vec3 at = new Vec3(a[0], a[1], a[2]);
        VfxSpawn.send(sl, VfxShape.DARKFX_BURST, at, at.add(0, 0.3, 0), COLOR, 16, 0.5f);
        Fx.send(sl, DARK, at.x, at.y, at.z, 6, 0.4);
        SpiritDarkSounds.play(p, Cue.CHIME, at);
        say(p, s, "nusmp.spirit_dark.linked", "The Spirit of Darkness settles on your shoulder.", ChatFormatting.DARK_PURPLE);
        mood(p, s, Event.LINK);
        return true;
    }

    static void unlink(ServerPlayer p, State s, boolean speak) {
        ServerLevel sl = p.serverLevel();
        if (speak) mood(p, s, Event.UNLINK);
        long now = sl.getGameTime();
        dissolve(sl, prop(sl, s.powered), now);
        dissolve(sl, prop(sl, s.companion), now);
        if (s.powered != null) POWERED.release(p.getUUID());
        if (s.companion != null) SPIRITS.release(p.getUUID());
        s.powered = null;
        s.companion = null;
        LIVE.remove(p.getUUID());
        if (speak) say(p, s, "nusmp.spirit_dark.unlinked", "The Spirit of Darkness melts back into the shadows.", ChatFormatting.GRAY);
    }

    /** A prop that ends with a farewell: it plays its bow and dissolve ({@link Pose#FAREWELL}) and ends itself a moment later ({@link SpiritDarkProps}). */
    private static void dissolve(ServerLevel sl, MagicPropEntity e, long now) {
        if (e == null) return;
        int param = e.param();
        e.setParam(SpiritDarkRules.param(SpiritDarkRules.variant(param), false, Pose.FAREWELL, SpiritDarkRules.face(param), now));
    }

    // ================================================================================================ the powered form
    private static void power(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (s.companion == null) { say(p, s, "nusmp.spirit_dark.needlink", "Link the Spirit of Darkness first.", ChatFormatting.GRAY); return; }
        if (s.powered != null) { endPowered(p, s, true); return; }
        if (now < s.powerReadyAt) { say(p, s, "nusmp.spirit_dark.recharging", "The demon is not ready to come again yet.", ChatFormatting.GRAY); return; }
        if (!POWERED.acquire(p.getUUID(), SpiritDarkRules.POWERED_PER_PLAYER, SpiritDarkConfig.maxPowered())) {
            say(p, s, "nusmp.spirit_dark.poweredfull", "Too many demons are out.", ChatFormatting.RED);
            return;
        }
        double[] a = SpiritDarkRules.powered(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SPIRITDARK_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, SpiritDarkRules.POWER_TICKS,
                SpiritDarkRules.param(SpiritDarkRules.V_POWERED, false, Pose.APPEAR, DarkMoodRules.Face.ANGRY, now), p);
        if (e == null) { POWERED.release(p.getUUID()); return; }
        e.setTarget(p);
        s.powered = e.getUUID();
        s.powerReadyAt = now + SpiritDarkRules.POWER_COOLDOWN;
        s.nextFlutter = now + 40;
        Vec3 at = new Vec3(a[0], a[1], a[2]);
        VfxSpawn.send(sl, VfxShape.DARKFX_BURST, at.add(0, 1, 0), at.add(0, 1.4, 0), COLOR, 24, 1.1f);
        Fx.send(sl, DARK, at.x, at.y + 1.0, at.z, 10, 0.8);
        SpiritDarkSounds.play(p, Cue.POWERED, at);
        hideCompanion(sl, s, true);
        mood(p, s, Event.POWER_CALL);
    }

    private static void endPowered(ServerPlayer p, State s, boolean speak) {
        ServerLevel sl = p.serverLevel();
        if (speak) mood(p, s, Event.POWER_END);
        dissolve(sl, prop(sl, s.powered), sl.getGameTime());
        if (s.powered != null) POWERED.release(p.getUUID());
        s.powered = null;
        hideCompanion(sl, s, false);
    }

    private static void hideCompanion(ServerLevel sl, State s, boolean hidden) {
        MagicPropEntity c = prop(sl, s.companion);
        if (c != null && SpiritDarkRules.hidden(c.param()) != hidden) c.setParam(SpiritDarkRules.withHidden(c.param(), hidden));
    }

    // ================================================================================================ Shadow Step
    private static void step(ServerPlayer p, State s) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (s.companion == null) { say(p, s, "nusmp.spirit_dark.needlink", "Link the Spirit of Darkness first.", ChatFormatting.GRAY); return; }
        if (now < s.stepReadyAt) { say(p, s, "nusmp.spirit_dark.steprecharging", "The shadows are still settling.", ChatFormatting.GRAY); return; }
        if (now < s.stepRetryAt || !p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping()) return;
        Vec3 look = p.getLookAngle();
        ShadowStep.Spot spot = ShadowStep.pick(new ShadowProbe(sl, p), p.getX(), p.getY(), p.getZ(), p.getEyeHeight(), p.getYRot(), look.y, SpiritDarkRules.STEP_RANGE_MAX);
        if (spot == null || !lastLook(sl, p, spot)) {
            s.stepRetryAt = now + SpiritDarkRules.STEP_RETRY_GAP;
            say(p, s, "nusmp.spirit_dark.nostep", "No shadow to step into that way.", ChatFormatting.GRAY);
            return;
        }
        boolean powered = s.powered != null;
        s.stepReadyAt = now + SpiritDarkRules.stepCooldown(powered);
        Vec3 from = p.position(), to = new Vec3(spot.x(), spot.y(), spot.z());
        VfxSpawn.send(sl, VfxShape.DARKFX_BURST, from.add(0, 1, 0), from.add(0, 1.2, 0), COLOR, 18, 0.9f);
        Fx.send(sl, DARK, from.x, from.y + 1.0, from.z, 8, 0.6);
        SpiritDarkSounds.play(p, Cue.STEP, from);
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0f;                                                          // the spot has a floor within a block or two: no fall to pay for
        VfxSpawn.send(sl, VfxShape.DARKFX_BURST, to.add(0, 1, 0), to.add(0, 1.2, 0), COLOR, 18, 0.9f);
        Fx.send(sl, DARK, to.x, to.y + 1.0, to.z, 8, 0.6);
        SpiritDarkSounds.play(p, Cue.ARRIVE, to);
        mood(p, s, Event.STEP);
    }

    /** The server's last look before moving: the player's real hit box fits there and the spot is inside the world border. */
    private static boolean lastLook(ServerLevel sl, ServerPlayer p, ShadowStep.Spot spot) {
        net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(spot.x(), spot.y(), spot.z());
        if (!sl.getWorldBorder().isWithinBounds(bp)) return false;
        return sl.noCollision(p, p.getBoundingBox().move(spot.x() - p.getX(), spot.y() - p.getY(), spot.z() - p.getZ()));
    }

    // ================================================================================================ the strike (life steal and the mark)
    /** A melee strike of a player landed on a living thing for {@code dealt} damage (the damage event). */
    public static void onStrike(ServerPlayer p, LivingEntity target, float dealt) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.companion == null || target == p || !target.isAlive() || target.isAlliedTo(p) || !(target instanceof Mob || target instanceof ServerPlayer)) return;
        if (!SpiritDarkConfig.enabled()) return;
        long now = p.level().getGameTime();
        boolean powered = s.powered != null;
        float give = s.steal.grant(now, SpiritDarkRules.stealOfHit(dealt, powered), powered);
        if (give > 0f && p.getHealth() < p.getMaxHealth()) {
            p.heal(give);
            Vec3 at = p.position().add(0, 1.2, 0);
            SpiritDarkSounds.play(p, Cue.STRIKE, at);
            mood(p, s, Event.STRIKE);
        }
        int mark = s.marks.tryMark(target.getUUID(), now);
        if (mark >= 0) {
            if (SpiritDarkRules.effectOf(mark) == SpiritDarkRules.FX_WITHER) target.addEffect(new MobEffectInstance(MobEffects.WITHER, powered ? SpiritDarkRules.WITHER_TICKS_POWERED : SpiritDarkRules.WITHER_TICKS, 0, false, true));
            else target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, SpiritDarkRules.BLIND_TICKS, 0, false, true));
            Fx.send(p.serverLevel(), DARK, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 3, 0.3);
        }
    }

    /** The owner took damage (the damage event). */
    public static void onOwnerHurt(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.companion == null) return;
        float frac = p.getHealth() / Math.max(1f, p.getMaxHealth());
        if (frac <= DarkMoodRules.LOW_HEALTH_FRACTION) mood(p, s, Event.LOW_HEALTH, frac);
        else mood(p, s, Event.DAMAGED, frac);
    }

    /** The owner killed something (the death event). */
    public static void onOwnerKill(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s != null && s.companion != null) mood(p, s, Event.KILL);
    }

    /** The owner cast a spell (the grimoire's cast path): a Dark or Shadow cast makes the spirit react. */
    public static void onCast(ServerPlayer p, MagicType magic) {
        if (STATES.isEmpty() || (magic != MagicType.DARK && magic != MagicType.SHADOW)) return;
        State s = STATES.get(p.getUUID());
        if (s != null && s.companion != null) mood(p, s, Event.CAST);
    }

    /** The owner died: the spirit has a last word, then everything goes. */
    public static void onOwnerDeath(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s != null && s.companion != null) mood(p, s, Event.OWNER_DIED);
        forget(p);
    }

    // ================================================================================================ moods: pose, face, voice
    private static void mood(ServerPlayer p, State s, Event e) { mood(p, s, e, 1f); }

    private static void mood(ServerPlayer p, State s, Event e, float ownerHealth) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        boolean powered = s.powered != null;
        Decision d = s.mood.decide(e, powered, now, p.getRandom().nextInt(100), ownerHealth);
        if (d == null) return;
        applyMood(sl, s, d, now);
        if (d.voiceGroup() != null) speak(p, s, d.voiceGroup());
    }

    private static void applyMood(ServerLevel sl, State s, Decision d, long now) {
        for (UUID id : new UUID[]{s.powered, s.companion}) {
            MagicPropEntity e = prop(sl, id);
            if (e == null) continue;
            int cur = e.param(), variant = SpiritDarkRules.variant(cur);
            boolean hidden = SpiritDarkRules.hidden(cur);
            int next;
            if (d.pose() != null) next = SpiritDarkRules.param(variant, hidden, d.pose(), d.face(), now);
            else {
                Pose curPose = SpiritDarkRules.pose(cur);
                boolean playing = SpiritDarkRules.stamp(cur) != 0 && SpiritDarkRules.since(cur, now) < curPose.ticks;
                next = playing ? SpiritDarkRules.withFace(cur, d.face()) : SpiritDarkRules.param(variant, hidden, Pose.LOUNGE, d.face(), now);
            }
            if (next != cur) e.setParam(next);
        }
    }

    /** A spoken line from the group (a recorded one if there is any, else the group's synthesised cue), to players within 32 blocks, plus its text on the owner's action bar. */
    private static void speak(ServerPlayer p, State s, String group) {
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = prop(sl, s.powered != null ? s.powered : s.companion);
        Vec3 at = e != null ? e.position().add(0, e.getBbHeight() * 0.5 + 0.9, 0) : p.position().add(0, 1.8, 0);
        String line = SpiritDarkConfig.voice() ? SpiritDarkVoiceCatalog.pick(group, p.getRandom().nextFloat(), s.lastLine.get(group)) : null;
        if (line != null) {
            s.lastLine.put(group, line);
            SpiritDarkSounds.say(sl, at, line);
            p.displayClientMessage(Component.literal("Spirit of Darkness: " + SpiritDarkVoiceCatalog.text(line)).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        } else {
            SpiritDarkSounds.play(p, SpiritDarkVoiceCatalog.fallback(group), at);
        }
    }

    // ================================================================================================ upkeep and cleanup
    /** Every few ticks for the players in {@link #LIVE}: drops what is no longer valid, the ambient sounds, the idle mood. */
    public static void upkeep(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) { LIVE.remove(p.getUUID()); return; }
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (s.powered != null && prop(sl, s.powered) == null) {                                     // the demon's fifteen seconds ran out (or the prop was lost)
            POWERED.release(p.getUUID());
            s.powered = null;
            hideCompanion(sl, s, false);
            mood(p, s, Event.POWER_END);
        }
        MagicPropEntity pe = prop(sl, s.powered);
        if (pe != null && pe.maxLife() - pe.life() <= DarkMoodRules.Pose.FAREWELL.ticks) endPowered(p, s, true);       // its last moments: it takes its leave with the farewell clip instead of just vanishing
        if (s.companion != null && prop(sl, s.companion) == null) { s.companion = null; SPIRITS.release(p.getUUID()); }
        if (s.companion == null) { unlink(p, s, false); return; }
        if (p.tickCount % 20 == 0 && !eligible(p)) { unlink(p, s, false); return; }
        MagicPropEntity shown = prop(sl, s.powered != null ? s.powered : s.companion);
        if (shown != null) {
            Vec3 at = shown.position().add(0, 1.0, 0);
            if (s.powered == null && now >= s.nextWhisper) { SpiritDarkSounds.play(p, Cue.WHISPER, at); s.nextWhisper = now + 360 + p.getRandom().nextInt(240); }
            if (s.powered != null && now >= s.nextFlutter) { SpiritDarkSounds.play(p, Cue.FLUTTER, at); s.nextFlutter = now + 70 + p.getRandom().nextInt(60); }
        }
        if (s.mood.idleDue(now, p.getRandom().nextInt(DarkMoodRules.IDLE_EVERY))) mood(p, s, Event.IDLE);
    }

    /** Everything of this player goes at once: logout, death, respawn, dimension change. */
    public static void forget(ServerPlayer p) {
        State s = STATES.remove(p.getUUID());
        LIVE.remove(p.getUUID());
        SPIRITS.forget(p.getUUID());
        POWERED.forget(p.getUUID());
        SpiritDarkSounds.forget(p.getUUID());
        if (s == null || !(p.level() instanceof ServerLevel sl)) return;
        for (UUID id : new UUID[]{s.companion, s.powered}) if (id != null && sl.getEntity(id) instanceof MagicPropEntity e) e.discard();
    }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() {
        STATES.clear();
        LIVE.clear();
        SPIRITS.clear();
        POWERED.clear();
        SpiritDarkSounds.clear();
    }

    // ================================================================================================ shared with the props (same package)
    static State stateOf(ServerPlayer p) { return state(p.getUUID()); }

    /** A prop ended on its own (its life ran out, or it was an orphan): if it is the one the player's state names, give its ledger slot back. */
    static void propEnded(UUID owner, int variant, UUID entity) {
        State s = owner == null ? null : STATES.get(owner);
        if (s == null || entity == null) return;
        if (variant == SpiritDarkRules.V_POWERED && entity.equals(s.powered)) { s.powered = null; POWERED.release(owner); }
        else if (variant == SpiritDarkRules.V_COMPANION && entity.equals(s.companion)) { s.companion = null; SPIRITS.release(owner); }
    }

    /** The prop entity with this id in this level, or null. */
    static MagicPropEntity prop(ServerLevel sl, UUID id) { return id != null && sl.getEntity(id) instanceof MagicPropEntity e && !e.isRemoved() ? e : null; }

    /** For the self-test: how many live things the ledgers count. */
    public static int liveCount() { return SPIRITS.total() + POWERED.total(); }
}
