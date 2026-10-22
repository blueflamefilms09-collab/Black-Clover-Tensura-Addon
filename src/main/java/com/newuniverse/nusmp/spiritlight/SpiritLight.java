package com.newuniverse.nusmp.spiritlight;

import com.newuniverse.nusmp.book.LightSpiritBridge;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the server half of the Spirit of Light (docs/magic/spirit_light.md), a holy spirit that a Light Magic caster links. The server decides everything; the clients only draw the props it spawns and
 * play the sounds it plays. All state is per player UUID in {@link #STATES} and is dropped on logout, death, respawn, dimension change and server stop ({@link #forget}). Nothing here scans the world
 * on a tick: the only entity searches are one box search per beam ({@link #fireBeam}) and one per pulse ({@link #pulse}), each behind its own cooldown and read through the caps of
 * {@link SpiritLightRules}.
 *
 * <p>While linked: a lion cub floats beside the owner's shoulder, the owner has Swiftness III (a short effect the upkeep refreshes and every cleanup removes), the owner can fire a piercing light beam
 * (cooldown, a few foes, raw damage inside the Light Magic caps through {@link LightSpiritBridge}) and a Glowing pulse (cooldown, a radius, a few foes, a short glow) that marks hostile mobs through
 * walls, and may call the powered form, a miniature seraph (one per player, a few on the server, a time limit and a cooldown). No hunger or exhaustion is touched, no damage is a share of a target's health,
 * nothing kills outright.</p>
 */
public final class SpiritLight {
    private SpiritLight() {}

    /** The state of one player's spirit. */
    static final class State {
        long lastPacket = Long.MIN_VALUE / 4, lastMessage = Long.MIN_VALUE / 4, nextBeam, nextPulse, powerReadyAt, poweredUntil;
        final long[] lastSound = new long[SpiritLightRules.SOUND_KINDS];
        UUID cub, seraph;
        /** What the spirit does with its body, face and voice (pose, cues, cooldowns): pure rules, one small object per player. */
        final LightMoodRules.Mood mood = new LightMoodRules.Mood();
        /** Where the owner was at the last upkeep and for how many ticks they have not moved (the quiet clock of the mood). */
        double lx, ly, lz;
        int still;
        boolean hasPos;

        State() { Arrays.fill(lastSound, Long.MIN_VALUE / 4); }
    }

    static final Map<UUID, State> STATES = new HashMap<>();
    /** The players that have a spirit linked: only these are visited by the once-every-four-ticks upkeep. */
    static final Set<UUID> LIVE = new HashSet<>();
    static final SpiritLightLedger.Counted SPIRITS = new SpiritLightLedger.Counted(), POWERED = new SpiritLightLedger.Counted();
    static final SpiritLightLedger.Timed EFFECTS = new SpiritLightLedger.Timed();

    private static final ThemeKey LIGHT = ThemeKey.of("magic.LIGHT");
    private static final PerfProbe.Section BEAM_SECTION = PerfProbe.section("s.spiritlight.beam"), PULSE_SECTION = PerfProbe.section("s.spiritlight.pulse"),
            UPKEEP_SECTION = PerfProbe.section("s.spiritlight.upkeep");
    static final int GOLD = 0xFFFFE08A;

    private static State state(UUID id) { return STATES.computeIfAbsent(id, k -> new State()); }

    // ================================================================================================ who may link
    /** May this player have the Spirit of Light now: the switch is on and the player has Light Magic. */
    public static boolean eligible(ServerPlayer p) { return SpiritLightConfig.enabled() && LightSpiritBridge.hasLightMagic(p); }

    private static void say(ServerPlayer p, State s, String key, String text, ChatFormatting color) {
        long now = p.level().getGameTime();
        if (!SpiritLightRules.gapOk(now, s.lastMessage, SpiritLightRules.MESSAGE_GAP)) return;
        s.lastMessage = now;
        p.displayClientMessage(Component.translatableWithFallback(key, text).withStyle(color), true);
    }

    private static void sound(ServerPlayer p, State s, Vec3 at, int kind, float volume, float pitch) {
        SpiritLightSounds.play(p.serverLevel(), at, kind, s.lastSound, volume, pitch);
    }

    // ================================================================================================ the key
    /** The one entry of the key packet. Validated here: the action is in range, at most one ask per {@link SpiritLightRules#PACKET_GAP} ticks, the spirit must be linked for everything but the link. */
    public static void onKey(ServerPlayer p, int action) {
        if (!SpiritLightRules.validAction(action)) return;
        long now = p.level().getGameTime();
        State s = action == SpiritLightRules.ACT_LINK ? state(p.getUUID()) : STATES.get(p.getUUID());
        if (s == null || !SpiritLightRules.gapOk(now, s.lastPacket, SpiritLightRules.PACKET_GAP)) return;
        s.lastPacket = now;
        switch (action) {
            case SpiritLightRules.ACT_LINK -> toggleLink(p, s);
            case SpiritLightRules.ACT_BEAM -> fireBeam(p, s);
            case SpiritLightRules.ACT_PULSE -> pulse(p, s);
            default -> togglePower(p, s);
        }
    }

    static void toggleLink(ServerPlayer p, State s) {
        if (s.cub != null) { unlink(p, s, true); return; }
        link(p, s, true);
    }

    // ================================================================================================ the link
    /** Links the cub. {@code check} false skips the Light Magic check (the self-test's stand-in player has none). */
    static boolean link(ServerPlayer p, State s, boolean check) {
        if (s.cub != null) return true;
        if (check ? !eligible(p) : !SpiritLightConfig.enabled()) {
            say(p, s, "nusmp.spiritlight.need", "The Spirit of Light answers only a Light Magic caster.", ChatFormatting.GRAY);
            return false;
        }
        UUID id = p.getUUID();
        if (!SPIRITS.acquire(id, SpiritLightRules.SPIRITS_PER_PLAYER, SpiritLightConfig.maxSpirits())) {
            say(p, s, "nusmp.spiritlight.full", "The Spirit of Light cannot answer: too many spirits are out.", ChatFormatting.RED);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        double[] a = SpiritLightRules.companion(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SPIRIT_LIGHT_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, SpiritLightRules.FORM_LIFE,
                SpiritLightRules.formParam(SpiritLightRules.V_CUB, false, 0, 0), p);
        if (e == null) { SPIRITS.release(id); return false; }
        e.setTarget(p);
        s.cub = e.getUUID();
        LIVE.add(id);
        applySwiftness(p);
        Vec3 at = new Vec3(a[0], a[1] + 0.3, a[2]);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, at, at, GOLD, 14, 0.5f);
        sound(p, s, at, SpiritLightRules.S_LINK, 0.8f, 1.0f);
        say(p, s, "nusmp.spiritlight.linked", "The Spirit of Light takes your shoulder.", ChatFormatting.GOLD);
        react(p, s, LightMoodRules.Situation.LINK, false, false);
        return true;
    }

    static void unlink(ServerPlayer p, State s, boolean say) {
        ServerLevel sl = p.serverLevel();
        if (say) react(p, s, LightMoodRules.Situation.UNLINK, false, s.seraph != null);
        endPower(p, s, false, false);
        MagicPropEntity e = prop(sl, s.cub);
        if (e != null) e.discard();
        if (s.cub != null) SPIRITS.release(p.getUUID());
        s.cub = null;
        removeSwiftness(p);
        if (say) {
            sound(p, s, p.position().add(0, 1.4, 0), SpiritLightRules.S_UNLINK, 0.7f, 1.0f);
            say(p, s, "nusmp.spiritlight.unlinked", "The Spirit of Light fades into the light.", ChatFormatting.GRAY);
        }
        if (s.seraph == null) LIVE.remove(p.getUUID());
    }

    // ================================================================================================ Swiftness III
    /** Swiftness III while linked: a short effect, ambient and without particles, refreshed once a second by the upkeep. */
    static void applySwiftness(ServerPlayer p) {
        MobEffectInstance cur = p.getEffect(MobEffects.MOVEMENT_SPEED);
        boolean has = cur != null && cur.getAmplifier() >= SpiritLightRules.SWIFT_AMPLIFIER;
        if (SpiritLightRules.swiftnessDue(has, cur == null ? 0 : cur.getDuration()))
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SpiritLightRules.SWIFT_TICKS, SpiritLightRules.SWIFT_AMPLIFIER, true, false, false));
    }

    /** Removes the spirit's own speed effect (and only that one: a potion's or a beacon's is not touched). */
    static void removeSwiftness(ServerPlayer p) {
        MobEffectInstance cur = p.getEffect(MobEffects.MOVEMENT_SPEED);
        if (cur != null && SpiritLightRules.ownSwiftness(cur.isAmbient(), cur.isVisible(), cur.getAmplifier(), cur.getDuration())) p.removeEffect(MobEffects.MOVEMENT_SPEED);
    }

    // ================================================================================================ the powered form
    static void togglePower(ServerPlayer p, State s) {
        if (s.cub == null) return;
        if (s.seraph != null) { endPower(p, s, true, true); return; }
        long now = p.level().getGameTime();
        if (!SpiritLightRules.powerReady(now, s.powerReadyAt)) {
            say(p, s, "nusmp.spiritlight.recharge", "The seraph is not ready to rise again.", ChatFormatting.GRAY);
            return;
        }
        UUID id = p.getUUID();
        if (!POWERED.acquire(id, SpiritLightRules.POWERED_PER_PLAYER, SpiritLightConfig.maxPowered())) {
            say(p, s, "nusmp.spiritlight.powered_full", "Too many seraphs are risen: the Spirit of Light stays a cub.", ChatFormatting.RED);
            return;
        }
        ServerLevel sl = p.serverLevel();
        double[] a = SpiritLightRules.seraph(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
        MagicPropEntity e = MagicProps.spawn(sl, PropKind.SPIRIT_LIGHT_1, new Vec3(a[0], a[1], a[2]), p.yBodyRot, 1.0f, SpiritLightRules.SERAPH_LIFE,
                SpiritLightRules.formParam(SpiritLightRules.V_SERAPH, false, 0, 0), p);
        if (e == null) { POWERED.release(id); return; }
        e.setTarget(p);
        s.seraph = e.getUUID();
        s.poweredUntil = now + SpiritLightRules.POWER_TICKS;
        setCubHidden(sl, s, true);
        Vec3 at = new Vec3(a[0], a[1] + 1.2, a[2]);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, at, at, GOLD, 18, 1.1f);
        sound(p, s, at, SpiritLightRules.S_POWER, 1.0f, 1.0f);
        say(p, s, "nusmp.spiritlight.risen", "The Spirit of Light rises as a seraph.", ChatFormatting.GOLD);
        react(p, s, LightMoodRules.Situation.POWER_UP, true, true);
    }

    /** Ends the seraph: the cub comes back. {@code cooldown} starts the wait before it may rise again; {@code say} plays the sound and the message. */
    static void endPower(ServerPlayer p, State s, boolean cooldown, boolean say) {
        if (s.seraph == null) return;
        ServerLevel sl = p.serverLevel();
        MagicPropEntity e = prop(sl, s.seraph);
        if (e != null) e.discard();
        s.seraph = null;
        POWERED.release(p.getUUID());
        if (cooldown) s.powerReadyAt = sl.getGameTime() + SpiritLightRules.POWER_COOLDOWN;
        setCubHidden(sl, s, false);
        if (say) {
            sound(p, s, p.position().add(0, 1.6, 0), SpiritLightRules.S_UNLINK, 0.8f, 0.85f);
            say(p, s, "nusmp.spiritlight.fallen", "The seraph settles back into the cub.", ChatFormatting.GRAY);
            react(p, s, LightMoodRules.Situation.POWER_DOWN, false, true);
        }
    }

    private static void setCubHidden(ServerLevel sl, State s, boolean hidden) {
        MagicPropEntity cub = prop(sl, s.cub);
        if (cub != null) cub.setParam(SpiritLightRules.formParam(SpiritLightRules.V_CUB, hidden, 0, 0));
    }

    // ================================================================================================ the piercing light beam
    /** The beam: from the form's mouth (the seraph's core) toward where the owner aims, stopped by a wall, through the nearest few foes. One box search, behind the cooldown. */
    static void fireBeam(ServerPlayer p, State s) {
        if (s.cub == null) return;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (now < s.nextBeam) return;
        boolean pw = s.seraph != null;
        if (!EFFECTS.acquire(p.getUUID(), now, SpiritLightRules.BEAM_LIFE, SpiritLightRules.EFFECT_PROPS_PER_PLAYER, SpiritLightConfig.maxEffectProps())) return;
        long t0 = PerfProbe.begin();
        try {
            s.nextBeam = now + SpiritLightRules.beamCooldown(pw);
            double range = SpiritLightRules.beamRange(pw);
            Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
            Vec3 far = eye.add(look.scale(range));
            HitResult wall = sl.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 aim = wall.getType() == HitResult.Type.MISS ? far : wall.getLocation();
            double[] m = pw ? SpiritLightRules.seraphCore(p.getX(), p.getY(), p.getZ(), p.yBodyRot) : SpiritLightRules.companionMouth(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
            double[] em = SpiritLightRules.clampEmitter(eye.x, eye.y, eye.z, m[0], m[1], m[2]);
            double[] dir = SpiritLightRules.beamDirection(em[0], em[1], em[2], aim.x, aim.y, aim.z, look.x, look.y, look.z);
            Vec3 from = new Vec3(em[0], em[1], em[2]);
            Vec3 end = from.add(dir[0] * range, dir[1] * range, dir[2] * range);
            HitResult stopHit = sl.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (stopHit.getType() != HitResult.Type.MISS && stopHit.getLocation().distanceTo(from) < 1.0) {      // the spirit sits behind a wall: the beam leaves the owner's eye instead
                from = eye.add(0, -0.25, 0);
                dir = new double[]{look.x, look.y, look.z};
                end = from.add(look.scale(range));
                stopHit = sl.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            }
            Vec3 stop = stopHit.getType() == HitResult.Type.MISS ? end : stopHit.getLocation();
            int hits = hitAlong(p, sl, from, stop, pw);
            double len = from.distanceTo(stop);
            double yaw = SpiritLightRules.yawOf(dir[0], dir[2]), pitch = SpiritLightRules.pitchOf(dir[0], dir[1], dir[2]);
            MagicPropEntity b = MagicProps.spawn(sl, PropKind.SPIRIT_LIGHT_2, from, (float) yaw, 1.0f, SpiritLightRules.BEAM_LIFE, SpiritLightRules.beamParam(pw, hits, pitch, len), p);
            if (b != null) b.setTarget(p);
            react(p, s, LightMoodRules.Situation.CAST_BEAM, pw, pw);
            if (stopHit.getType() != HitResult.Type.MISS) VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, stop, stop, GOLD, 8, pw ? 0.5f : 0.35f);
            sound(p, s, from, SpiritLightRules.S_BEAM, pw ? 0.9f : 0.7f, pw ? 0.92f : 1.08f);
        } finally {
            PerfProbe.end(BEAM_SECTION, t0);
        }
    }

    /** The one box search of a beam: the living things within the beam's width of the segment, the nearest few hurt as a Light Magic hit. Returns how many were hit. */
    private static int hitAlong(ServerPlayer p, ServerLevel sl, Vec3 from, Vec3 stop, boolean pw) {
        AABB box = new AABB(from, stop).inflate(SpiritLightRules.BEAM_WIDTH + 1.5);
        List<LivingEntity> found = sl.getEntitiesOfClass(LivingEntity.class, box, e -> e != p && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(p));
        int n = Math.min(found.size(), SpiritLightRules.MAX_BEAM_CANDIDATES);
        double[] along = new double[n];
        for (int i = 0; i < n; i++) {
            LivingEntity e = found.get(i);
            Vec3 c = e.getBoundingBox().getCenter();
            along[i] = SpiritLightRules.alongHit(from.x, from.y, from.z, stop.x, stop.y, stop.z, c.x, c.y, c.z, SpiritLightRules.BEAM_WIDTH + e.getBbWidth() * 0.5);
        }
        int[] pick = SpiritLightRules.pierce(along, n, SpiritLightRules.beamPierce(pw));
        float raw = SpiritLightRules.beamRaw(pw, SpiritLightConfig.beamDamageScale());
        int hits = 0;
        for (int idx : pick) if (LightSpiritBridge.hit(p, found.get(idx), raw)) hits++;
        return hits;
    }

    // ================================================================================================ the Glowing pulse
    /** The pulse: hostile mobs near the owner glow for a few seconds, through walls (foes the owner cannot see come first). One box search and at most a shortlist of line of sight tests, behind the cooldown. */
    static void pulse(ServerPlayer p, State s) {
        if (s.cub == null) return;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        if (now < s.nextPulse) return;
        boolean pw = s.seraph != null;
        if (!EFFECTS.acquire(p.getUUID(), now, SpiritLightRules.PULSE_LIFE, SpiritLightRules.EFFECT_PROPS_PER_PLAYER, SpiritLightConfig.maxEffectProps())) return;
        long t0 = PerfProbe.begin();
        try {
            s.nextPulse = now + SpiritLightRules.pulseCooldown(pw);
            double radius = SpiritLightRules.pulseRadius(pw);
            AABB box = p.getBoundingBox().inflate(radius);
            List<LivingEntity> found = sl.getEntitiesOfClass(LivingEntity.class, box, e -> hostile(p, e));
            int n = Math.min(found.size(), SpiritLightRules.MAX_PULSE_SCAN);
            double[] dsq = new double[n];
            for (int i = 0; i < n; i++) dsq[i] = found.get(i).distanceToSqr(p);
            int[] short_ = SpiritLightRules.pulseShortlist(dsq, n, radius);
            double[] sd = new double[short_.length];
            boolean[] hidden = new boolean[short_.length];
            for (int i = 0; i < short_.length; i++) {
                LivingEntity e = found.get(short_[i]);
                sd[i] = dsq[short_[i]];
                hidden[i] = !p.hasLineOfSight(e);
            }
            int[] pick = SpiritLightRules.pulsePick(sd, hidden, short_.length, SpiritLightRules.pulseTargets(pw), radius);
            int ticks = SpiritLightRules.pulseTicks(pw);
            for (int idx : pick) found.get(short_[idx]).addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, true, false, false));
            Vec3 at = p.position().add(0, 0.1, 0);
            MagicPropEntity ring = MagicProps.spawn(sl, PropKind.SPIRIT_LIGHT_2, at, p.yBodyRot, 1.0f, SpiritLightRules.PULSE_LIFE, SpiritLightRules.pulseParam(pw, radius), p);
            if (ring != null) ring.setTarget(p);
            react(p, s, LightMoodRules.Situation.CAST_PULSE, pw, pw);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, at.add(0, 1.0, 0), at.add(0, 1.0, 0), GOLD, 12, pw ? 0.9f : 0.6f);
            sound(p, s, at.add(0, 1.0, 0), SpiritLightRules.S_PULSE, 1.0f, pw ? 0.9f : 1.0f);
        } finally {
            PerfProbe.end(PULSE_SECTION, t0);
        }
    }

    /** A hostile mob for the pulse: a monster of the game's own kinds, or any mob that is hunting the owner. Never the owner, an ally, a spectator or another player. */
    private static boolean hostile(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof net.minecraft.world.entity.player.Player || e.isAlliedTo(p)) return false;
        return e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER || (e instanceof Mob m && m.getTarget() == p);
    }

    // ================================================================================================ mood: poses, faces, creature sounds and spoken lines
    /**
     * Something happened to the owner or the spirit: the pure rules ({@link LightMoodRules.Mood#react}) decide the pose (it goes into the param of the form that is out, the clients play the clip and so
     * show its face), the cub makes a creature sound, the seraph says a line (one cue per situation, behind its cooldown). {@code poseSeraph} is the form that wears the pose, {@code voiceSeraph} whether
     * the seraph speaks (it may speak while the pose is already the cub's, as it settles back).
     */
    static void react(ServerPlayer p, State s, LightMoodRules.Situation sit, boolean poseSeraph, boolean voiceSeraph) {
        if (s.cub == null) return;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        int pose = s.mood.react(poseSeraph, sit, now);
        if (pose >= 0) setPose(sl, s, poseSeraph, pose, now);
        if (voiceSeraph) {
            speak(p, s, sit, now);
        } else if (pose >= 0 && !poseSeraph) {
            int kind = LightMoodRules.cubSound(sit, pose);
            if (kind >= 0) sound(p, s, p.position().add(0, 1.4, 0), kind, 0.7f, 1.0f);
        }
    }

    private static void setPose(ServerLevel sl, State s, boolean seraph, int pose, long now) {
        MagicPropEntity e = prop(sl, seraph ? s.seraph : s.cub);
        if (e != null) e.setParam(SpiritLightRules.formParam(seraph ? SpiritLightRules.V_SERAPH : SpiritLightRules.V_CUB, !seraph && s.seraph != null, pose, now));
    }

    /** The seraph says the line of this situation: one box search of players behind {@link SpiritLightVoice}'s limits, the text on the owner's action bar; a cue without a line plays a synthesised one. */
    private static void speak(ServerPlayer p, State s, LightMoodRules.Situation sit, long now) {
        LightMoodRules.Cue cue = LightMoodRules.cueFor(true, sit, s.mood.counter());
        if (cue == null || !s.mood.cueReady(cue, now)) return;
        String line = LightMoodRules.line(cue, s.mood.counter(), SpiritLightVoiceLines.AVAILABLE, s.mood.lastLine());
        if (line == null) {
            int kind = LightMoodRules.fallbackSound(cue);
            if (kind >= 0) sound(p, s, p.position().add(0, 1.6, 0), kind, 0.6f, 1.15f);
            return;
        }
        s.mood.said(line);
        SpiritLightVoice.say(p, line, LightMoodRules.cueCooldown(cue));
        p.displayClientMessage(Component.translatableWithFallback("nusmp.spiritlight.say." + line, SpiritLightVoiceLines.text(line)).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
    }

    /** The owner took damage (the damage event of a player with a linked spirit): a flinch, or at low health the spirit's alarm. */
    public static void onOwnerHurt(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.cub == null) return;
        boolean sera = s.seraph != null;
        react(p, s, LightMoodRules.lowHealth(p.getHealth(), p.getMaxHealth()) ? LightMoodRules.Situation.OWNER_LOW : LightMoodRules.Situation.HURT, sera, sera);
    }

    /** The owner killed something: a victory strut, a word of praise. */
    public static void onOwnerKill(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.cub == null) return;
        boolean sera = s.seraph != null;
        react(p, s, LightMoodRules.Situation.OWNER_KILL, sera, sera);
    }

    /** The owner died (before everything of the spirit is cleaned up): the last word, nothing else. */
    public static void onOwnerDied(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null || s.cub == null) return;
        boolean sera = s.seraph != null;
        react(p, s, LightMoodRules.Situation.OWNER_DIED, sera, sera);
    }

    // ================================================================================================ upkeep and cleanup
    /** Every fourth tick for a player in {@link #LIVE}: drops what is no longer valid, refreshes Swiftness once a second, ends the seraph when its time is up, and breathes a few motes of light. */
    public static void upkeep(ServerPlayer p) {
        State s = STATES.get(p.getUUID());
        if (s == null) { LIVE.remove(p.getUUID()); return; }
        long t0 = PerfProbe.begin();
        try {
            ServerLevel sl = p.serverLevel();
            long now = sl.getGameTime();
            UUID id = p.getUUID();
            if (s.seraph != null && prop(sl, s.seraph) == null) { s.seraph = null; POWERED.release(id); s.powerReadyAt = now + SpiritLightRules.POWER_COOLDOWN; setCubHidden(sl, s, false); }
            if (s.cub != null && prop(sl, s.cub) == null) { endPower(p, s, true, false); s.cub = null; SPIRITS.release(id); removeSwiftness(p); }
            if (s.cub != null) {
                if (p.tickCount % 20 == 0) {
                    if (!eligible(p)) { unlink(p, s, false); return; }
                    applySwiftness(p);
                }
                if (s.seraph != null && now >= s.poweredUntil) endPower(p, s, true, true);
                quiet(p, s, now);
                if (p.tickCount % 40 == 0) {
                    double[] a = s.seraph != null ? SpiritLightRules.seraph(p.getX(), p.getY(), p.getZ(), p.yBodyRot) : SpiritLightRules.companion(p.getX(), p.getY(), p.getZ(), p.yBodyRot);
                    Fx.send(sl, LIGHT, a[0], a[1] + (s.seraph != null ? 1.4 : 0.3), a[2], s.seraph != null ? 4 : 2, s.seraph != null ? 0.6 : 0.25);
                    if (s.seraph == null) sound(p, s, new Vec3(a[0], a[1], a[2]), SpiritLightRules.S_PURR, 0.35f, 1.0f);
                }
            }
            if (s.cub == null && s.seraph == null) LIVE.remove(id);
        } finally {
            PerfProbe.end(UPKEEP_SECTION, t0);
        }
    }

    /** The quiet clock (every fourth tick): how long the owner has stood still, so the spirit can fidget, settle to rest, and wake when they move. Primitive math only. */
    private static void quiet(ServerPlayer p, State s, long now) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double dx = x - s.lx, dy = y - s.ly, dz = z - s.lz;
        boolean still = s.hasPos && dx * dx + dy * dy + dz * dz < LightMoodRules.STILL_DIST * LightMoodRules.STILL_DIST;
        s.still = still ? Math.min(s.still + 4, 1_000_000) : 0;
        s.lx = x; s.ly = y; s.lz = z;
        s.hasPos = true;
        s.mood.settle(now);
        LightMoodRules.Situation q = s.mood.quiet(now, s.still);
        if (q != null) {
            boolean sera = s.seraph != null;
            react(p, s, q, sera, sera);
        }
    }

    /** Everything of this player goes: logout, death, respawn, dimension change. */
    public static void forget(ServerPlayer p) {
        UUID id = p.getUUID();
        State s = STATES.remove(id);
        LIVE.remove(id);
        SPIRITS.forget(id);
        POWERED.forget(id);
        EFFECTS.forget(id);
        if (s == null) return;
        removeSwiftness(p);
        if (!(p.level() instanceof ServerLevel sl)) return;
        for (UUID u : new UUID[]{s.cub, s.seraph}) if (u != null && sl.getEntity(u) instanceof MagicPropEntity e) e.discard();
    }

    /** The server stops: nothing may survive into the next world. */
    public static void clearAll() {
        STATES.clear();
        LIVE.clear();
        SPIRITS.clear();
        POWERED.clear();
        EFFECTS.clear();
    }

    /** A prop ended on its own (its life ran out, or it was an orphan): if it is the one the player's state names, give its ledger slot back. */
    public static void propEnded(UUID owner, int variant, UUID entity) {
        State s = owner == null ? null : STATES.get(owner);
        if (s == null || entity == null) return;
        if (variant == SpiritLightRules.V_CUB && entity.equals(s.cub)) { s.cub = null; SPIRITS.release(owner); }
        else if (variant == SpiritLightRules.V_SERAPH && entity.equals(s.seraph)) { s.seraph = null; POWERED.release(owner); }
    }

    // ================================================================================================ shared with the props and the self-test (same package)
    static State stateOf(ServerPlayer p) { return state(p.getUUID()); }

    /** The prop entity with this id in this level, or null. */
    static MagicPropEntity prop(ServerLevel sl, UUID id) { return id != null && sl.getEntity(id) instanceof MagicPropEntity e && !e.isRemoved() ? e : null; }

    /** For the self-test: how many slots the form ledgers count. */
    static int liveCount() { return SPIRITS.total() + POWERED.total(); }
}
