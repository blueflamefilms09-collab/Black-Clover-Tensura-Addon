package com.newuniverse.nusmp.arc.zagred;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.DaemonMaster;
import com.newuniverse.nusmp.entity.GrimoireDaemonEntity;
import com.newuniverse.nusmp.entity.ZagredFx;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.item.BossRelics;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.quests.QuestHooks;
import com.newuniverse.nusmp.sound.NUSounds;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the owner asked for Zagred to be remade ("remake Zagred over since he sucks, but leave his magic"), and the remake is a raid boss in the sense
 * that the party must READ what he says and ANSWER it. This is the boss itself: it stands on the shared boss framework (power steps at 20, 40, 60, 80 and
 * the Gates at 100, the per-second damage budget, the second health bar) under the SAME entity id as the old one ({@code nusmp:zagred}), and it adds only
 * what is Zagred's own: his Words as telegraphed commands with counters ({@link ZagredTable}, {@link ZagredDecrees}), the ward he writes round himself
 * in the first four acts ({@link ZagredWard}), the heart he hides in the fifth ({@link ZagredHeart}), the verdicts that put a word of their own over
 * several players ({@link ZagredVerdict}), and Overwrite, the ultimate that writes a rule over the arena. The book magic of Kotodama (the players' words,
 * counters and skills) is NOT touched by any of this; the old cubiform boss ({@code entity/ZagredBossEntity}) stays in the tree as {@code nusmp:zagred_legacy}.
 *
 * <p>The five acts are the framework's power steps: I the Reader (20%), II the Speaker (40%), III the Judge (60%), IV the Author (80%), V the Heart (the
 * Gates, 100%, the devil form). Every rule that keeps the fight fair is stated where it is enforced: no healing in combat (only the Gates' second bar,
 * which is the canon "the heart rebuilds the body"), one hard control per twelve seconds through {@link BalanceLaw#beginControl}, two cuts of a Word never
 * chain ({@link ZagredComposure}), reflected damage goes through {@link BalanceLaw#cap} and never kills, and no block is ever changed (the Mega-Dungeon
 * guard owns every block there). It also implements {@link DaemonMaster}, so the Grimoire Daemons (the pages, the turrets, the arch daemon, the rule stones)
 * serve the remade boss as they served the old one.
 */
public class ZagredBoss extends ArcBoss implements DaemonMaster {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final int VIOLET = 0x8A4CFF, RED = 0xF2194D;

    /** Who he is: read by the roster, the floor-boss picker, the story unlock and the Charter table. Registered by the lead's one line in {@code ArcCharacters}. */
    public static final ArcCharacter CHARACTER = ArcCharacter.boss("zagred", "Zagred")
            .title("the Word Soul Devil").source("Black Clover").saga(ArcSaga.ELF_REINCARNATION).tier(2)
            .colors(0x8A4CFF, 0xF2194D).devil(Devil.ZAGRED).magic("kotodama").stats(1900, 16, 0.30, 10)
            .floors(Polarity.ABYSSAL, 40, 80).themes("cursed_archive", "shadow", "abyss")
            .after("patolli", "rades_spirito")
            .charter(0.30, "chant_annulment", "sound_domination", "voice_cannon", "magic_jamming", "thought_acceleration", "multilayer_barrier",
                    "spiritual_attack_resistance", "law_manipulation")
            .lines(ZagredLines.OPENING, ZagredLines.GATES, ZagredLines.DEATH)
            .build();

    /** The entity type {@code nusmp:zagred}: the hitbox and how it is drawn (the numbers come from the art kit's manifest, the bigger of the two forms). */
    public static final DeferredHolder<EntityType<?>, EntityType<ZagredBoss>> TYPE = ArcEntities.boss("zagred", ZagredBoss::new, 0.55f, 2.31f,
            new ArcLook("zagred", 2.31f, 1.05f, 1.95f, 1.07f, 2.05f, 2.27f, true), ZagredBoss::createAttributes);

    /** What the wind-up of the move being cast decided, shared by its telegraph and its decree (cleared when the next wind-up begins). */
    static final class Plan {
        Vec3 from, to;
        UUID victim;
        final List<List<Vec3>> sentences = new ArrayList<>();
        final List<Vec3> marks = new ArrayList<>();

        void clear() {
            from = null;
            to = null;
            victim = null;
            sentences.clear();
            marks.clear();
        }
    }

    /** What the rule and the reflection remember about one fighter. */
    static final class Track {
        Vec3 last;
        int still, air, bad;
        float health = -1f;
        long reflectAt, punishAt;
    }

    // ---- state read and written by ZagredDecrees (package-private on purpose: the decrees are this fight's other half)
    final Plan plan = new Plan();
    final List<ZagredMark> marks = new ArrayList<>();
    long verdictUntil, shatterUntil, reverseUntil, ruleUntil, staggerUntil;
    ZagredRule rule, pendingRule, lastRule;
    final List<UUID> stones = new ArrayList<>();
    int stoneKills;
    final Map<UUID, Track> tracks = new HashMap<>();
    int lesserAlive, greaterAlive, archAlive;

    // ---- his own rules
    private final ZagredWard ward = new ZagredWard();
    private final ZagredComposure composure = new ZagredComposure();
    private final ZagredHeart heart = new ZagredHeart();
    private final ZagredAttention attention = new ZagredAttention();
    private long lastSpeech, lastClosedHint, lastCutHint, nextSwap, lockTargetUntil, nextWordAt;
    private boolean heartWasOpen, heartSpoken;
    private String lastMoveId = "";

    public ZagredBoss(EntityType<ZagredBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(1900, 16, 0.30, 10, 6); }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ spawning (the command /multiverse boss zagred)
    /**
     * Summons him at {@code at} with his arena there: the entry point of the owner's command, which is switched by the Zagred config
     * ({@code zagredBossEnabled} / the game rule), not by the arcs layer, so it keeps working whatever the arcs are set to.
     */
    public static ZagredBoss summon(ServerLevel sl, Vec3 at) {
        ZagredBoss z = TYPE.get().create(sl);
        if (z == null) return null;
        z.moveTo(at.x, at.y, at.z, 0f, 0f);
        z.setArena(BlockPos.containing(at));
        if (!sl.addFreshEntity(z)) return null;
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, at.add(0, 0.05, 0), at.add(0, 1, 0), 0xFF000000 | VIOLET, 60, 3f);
        sl.playSound(null, z.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.5f);
        return z;
    }

    // ================================================================ small facts
    /** Every hook the framework or another class calls without a guard of its own: a mistake here is logged once per call and never takes the server tick down. */
    private void guarded(String what, Runnable body) {
        try {
            body.run();
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Zagred's {} failed: {}", what, e.toString());
        }
    }

    long now() { return level().getGameTime(); }

    /** The act as the framework's power step, 0 to 4. */
    int stage() { return PowerCurve.stage(power()); }

    /** Players (and only players) fighting him within reach; at least one, so the packs never scale to nothing. */
    int fighterCount() {
        int n = 0;
        for (LivingEntity e : foes(48)) if (e instanceof Player) n++;
        return Math.max(1, n);
    }

    private boolean staggered(long t) { return t < staggerUntil; }

    /** Unwritten or staggered: he cannot speak, strike or walk. */
    private boolean helpless(long t) { return staggered(t) || ward.reforming(t); }

    private static String roman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "V"; };
    }

    private static int secs(long ticks) { return (int) ((ticks + 19) / 20); }

    private static MutableComponent bold(String text, int rgb) { return Component.literal(text).withStyle(Style.EMPTY.withColor(rgb).withBold(true)); }

    private boolean isMine(Projectile p) {
        Entity o = p.getOwner();
        return o == this || (o != null && (o.isAlliedTo(this) || isFamily(o)));
    }

    /** The living thing a blow is credited to: the owner of a summon, else whoever struck. */
    private static LivingEntity creditOf(Entity e) {
        if (e instanceof OwnableEntity o && o.getOwner() instanceof LivingEntity owner) return owner;
        return e instanceof LivingEntity le ? le : null;
    }

    // ================================================================ looks and numbers
    @Override protected ChatFormatting chatColor() { return ChatFormatting.DARK_PURPLE; }

    @Override protected int gatesForm() { return FORM_HYBRID; }

    /** The devil under the coat is a little larger than the man (wings and horns are the model's own; this is the body). */
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.15f : 1.0f; }

    @Override protected String[] tensuraKeywords() { return new String[]{"dark", "death", "curse", "hell", "shadow"}; }

    @Override protected int tensuraKitSize() { return 3; }

    /** The plain blow is lighter than the telegraphed claw, and nothing at all while he is unwritten or staggered. */
    @Override
    protected float meleeDamage() {
        return helpless(level().getGameTime()) ? 0f : (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f;
    }

    /** The man keeps his distance and speaks; the devil comes to you. */
    @Override
    protected double preferredRange(double suggested) {
        return gatesOpen() ? Math.max(3.0, Math.min(8.0, suggested)) : Math.max(6.0, Math.min(13.0, suggested));
    }

    @Override
    public String barTitle() {
        int act = ZagredMath.act(power());
        return bossName() + " - Act " + roman(act) + ": " + ZagredMath.actName(act) + (gatesOpen() ? " - Gates open" : "");
    }

    @Override
    protected String powerLine(int old, int now) {
        int i = ZagredMath.act(now) - 2;                                   // act II is index 0
        if (i >= 0 && i < ZagredLines.ACT.length) {
            String[] pool = ZagredLines.ACT[i];
            return pool[getRandom().nextInt(pool.length)];
        }
        return super.powerLine(old, now);
    }

    /** His daemons are his own: his Words never hurt them and they never count as foes. */
    @Override public boolean isAlliedTo(Entity other) { return other instanceof GrimoireDaemonEntity || super.isAlliedTo(other); }

    /**
     * Once a verdict or a threat swap has chosen whom he looks at, the next hurt does not drag his eyes elsewhere for five seconds; without this the
     * framework's "turn on whoever hit me last" would undo the swap in the same second.
     */
    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity cur = getTarget();
        if (target != null && cur != null && target != cur && cur.isAlive() && level().getGameTime() < lockTargetUntil && distanceToSqr(cur) < 64.0 * 64.0) return;
        super.setTarget(target);
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (ZagredTable.Spec s : ZagredTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            final ZagredTable.Spec spec = s;
            b.tell((boss, target, left, total) -> tellOf(spec, target, left, total));
            out.add(b.run((boss, target) -> runOf(spec, target)));
        }
        return out;
    }

    /** The wind-up: the Word on every screen with its counter, the floor and the air drawn, and one line of speech. */
    private void tellOf(ZagredTable.Spec s, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean first = left >= total - 3;                                    // the first call of a wind-up (calls come every third tick)
        if (first) {
            plan.clear();
            ZagredWord w = s.word();
            if (w != null) {
                if (w != ZagredWord.OVERWRITE) ZagredScreen.word(sl, position(), w, total);        // the ultimate shows its rule instead
                speak(sl, w);
            }
        }
        ZagredDecrees.tell(this, sl, s.id(), target, left, total, first);
    }

    /** The decree, then the heart: in the devil form every finished Word opens it for a moment. */
    private void runOf(ZagredTable.Spec s, LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            ZagredDecrees.run(this, sl, s.id(), target);
        } finally {
            lastMoveId = s.id();
        }
        if (!s.isWord()) return;
        long t = sl.getGameTime();
        nextWordAt = t + ZagredMath.wordGap(stage());                          // he speaks, then he fights: one title at a time
        if (gatesOpen()) heart.open(t, ZagredHeart.windowAfterWord(archAlive > 0));
    }

    private void speak(ServerLevel sl, ZagredWord w) {
        sl.playSound(null, blockPosition(), voiceOf(w), SoundSource.HOSTILE, 1.6f, 0.85f + getRandom().nextFloat() * 0.3f);
        if (tickCount - lastSpeech < 100) return;                              // a line in chat at most every five seconds: the title is the information
        lastSpeech = tickCount;
        String[] pool = ZagredLines.forWord(w);
        say(pool[getRandom().nextInt(pool.length)]);
    }

    private static SoundEvent voiceOf(ZagredWord w) {
        return switch (w) {
            case SEAL -> NUSounds.ZAGRED_SEAL.get();
            case FALL -> NUSounds.ZAGRED_FALL.get();
            case SHATTER -> NUSounds.ZAGRED_SHATTER.get();
            case OVERWRITE -> NUSounds.ZAGRED_OVERWRITE.get();
            default -> NUSounds.ZAGRED_WORD.get();
        };
    }

    /** He answers what the party does (see {@link ZagredAi}); while unwritten, staggered or with nothing to say he chooses nothing and walks or claws. */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (!(level() instanceof ServerLevel sl)) return super.chooseMove(bk, s, t);
        if (helpless(t)) return -1;
        ZagredAi.Read read = situationRead(sl, s, t);
        double[] w = new double[bk.size()];
        for (int i = 0; i < w.length; i++) {
            Move m = bk.get(i);
            ZagredTable.Spec spec = ZagredTable.byId(m.id());
            w[i] = spec != null && moveEligible(m, s) ? ZagredAi.weight(spec, read) : 0;
        }
        return ZagredAi.choose(w, getRandom().nextDouble());
    }

    private ZagredAi.Read situationRead(ServerLevel sl, MoveScheduler.Situation s, long t) {
        List<LivingEntity> foes = foes(32);
        int close = 0, fliers = 0;
        double crowd = ZagredMath.crowdRadius();
        for (LivingEntity e : foes) {
            if (distanceTo(e) <= crowd) close++;
            if (!e.onGround() && !e.isInWater() && ZagredDecrees.floorHeight(sl, e) > 2.0) fliers++;
        }
        int stage = stage();
        boolean ruleOpen = (rule != null && t < ruleUntil) || !stones.isEmpty();
        return new ZagredAi.Read(s.distance(), foes.size(), close, fliers, countShots(sl), attention.casters(t), attention.burst(t, getMaxHealth()), BalanceLaw.controlCooldownLeft(this) == 0, stage,
                gatesOpen(), lastMoveId, lesserAlive, ZagredMath.lesserCap(stage, fighterCount()), composure.silenced(t) || t < nextWordAt, !marks.isEmpty(), ruleOpen);
    }

    /** Enemy projectiles in flight towards him (an arrow lying in the ground does not count). Spell bolts are not entities and are not counted. */
    private int countShots(ServerLevel sl) {
        int n = 0;
        Vec3 me = getBoundingBox().getCenter();
        for (Projectile p : sl.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(28), pr -> pr.isAlive() && !isMine(pr))) {
            Vec3 toMe = me.subtract(p.position());
            Vec3 v = p.getDeltaMovement();
            if (toMe.lengthSqr() < 1e-4 || v.lengthSqr() < 0.25) continue;
            if (toMe.normalize().dot(v.normalize()) > 0.5) n++;
        }
        return n;
    }

    // ================================================================ taking damage
    /** Before anything else: the rule's forbidden elements, the bookkeeping, a cut Word, and Reverse. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            return judge(sl, source, amount);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Zagred's damage filter failed: {}", e.toString());
            return amount;
        }
    }

    /** What of this blow will really count: the invulnerability frames swallow a blow that is not stronger than the last one. */
    private float counted(DamageSource source, float amount) {
        if (invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) return amount <= lastHurt ? 0f : amount - lastHurt;
        return amount;
    }

    private float judge(ServerLevel sl, DamageSource source, float amount) {
        long t = sl.getGameTime();
        float real = counted(source, amount);
        if (real <= 0f) return amount;                                        // swallowed by vanilla anyway: no side effects
        boolean anti = DanteMoves.antiMagicHit(source);
        Entity direct = source.getEntity();
        LivingEntity striker = direct instanceof LivingEntity le ? le : null;
        LivingEntity credit = creditOf(direct);
        if (rule != null && t < ruleUntil && rule.element() != null && rule.element().equals(ZagredDecrees.elementOf(source))) {
            if (striker != null) punishOnce(sl, striker, t);                  // the rule forbids it: the blow does not land, and the one who broke it pays
            return 0f;
        }
        attention.hit(t, real);
        if (credit instanceof ServerPlayer sp) {
            attention.threatAdd(sp.getUUID(), real);
            if (anti || AntiMagic.isMagic(source)) attention.magicHit(sp.getUUID(), t);
        }
        if (anti) tryCut(sl, source, t);
        if (t < reverseUntil && !anti && striker != null) return reverse(sl, striker, amount, t);
        return amount;
    }

    /** An anti-magic blow while a Word is being spoken cuts it: he is silenced for three seconds and cannot be cut again for five more. */
    private void tryCut(ServerLevel sl, DamageSource source, long t) {
        Move m = castingMoveOrNull();
        if (state() != STATE_CASTING || m == null) return;
        ZagredTable.Spec spec = ZagredTable.byId(m.id());
        if (spec == null || !spec.isWord() || spec.ultimate()) return;          // the ultimate is answered by the stones, not by one blow
        Entity cutter = source.getEntity();
        if (!composure.cut(t)) {
            if (cutter instanceof ServerPlayer sp && t - lastCutHint > 40) {
                lastCutHint = t;
                sp.displayClientMessage(ZagredScreen.text("message.nusmp.zagred.composed", "He is composed: he cannot be cut again yet.", 0xB8B0C8, true), true);
            }
            return;
        }
        interruptCast();
        plan.clear();
        if (gatesOpen()) heart.open(t, ZagredHeart.AFTER_CUT);
        Vec3 mouth = getEyePosition();
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, mouth, mouth.add(0, 0.3, 0), 0xFFFFFFFF, 14, 1.8f);
        sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6f, 1.3f);
        ZagredScreen.title(sl, position(), ZagredScreen.text("message.nusmp.zagred.cut", "CUT", 0xFFFFFF, true),
                ZagredScreen.text("message.nusmp.zagred.cut.sub", "He cannot speak for three seconds", 0xE8E0F8, false), 2, 24, 8);
        say(ZagredLines.CUT[getRandom().nextInt(ZagredLines.CUT.length)]);
        if (cutter instanceof ServerPlayer sp) NusmpEvents.fire(sp, "zagred.cut", spec.word().id());
    }

    /** Reverse: half of a blow is turned back on the one who struck (never lethal, capped by the balance law, at most once every half second each). */
    private float reverse(ServerLevel sl, LivingEntity striker, float amount, long t) {
        float back = amount * ZagredMath.reflectShare();
        Track tr = tracks.computeIfAbsent(striker.getUUID(), k -> new Track());
        if (t >= tr.reflectAt && striker.isAlive()) {
            tr.reflectAt = t + 10;
            float dmg = Math.min(BalanceLaw.cap(striker, back), striker.getMaxHealth() * 0.15f);
            dmg = Math.min(dmg, Math.max(0f, striker.getHealth() - 1f));
            if (dmg > 0.01f) {
                striker.hurt(damageSources().indirectMagic(this, this), dmg);
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), striker.getBoundingBox().getCenter(), ZagredWord.REVERSE.argb(), 12, 0.8f);
            }
        }
        return amount - back;                                                 // the blow is turned, not doubled: he takes the other half
    }

    private void punishOnce(ServerLevel sl, LivingEntity who, long t) {
        Track tr = tracks.computeIfAbsent(who.getUUID(), k -> new Track());
        if (t < tr.punishAt) return;
        tr.punishAt = t + 20;
        ZagredDecrees.punish(this, sl, who);
        if (who instanceof Player p) p.displayClientMessage(ZagredScreen.text("message.nusmp.zagred.forbidden", "The rule forbids it.", RED, true), true);
    }

    /** After the filter, and not while he is exposed: the ward (acts I to IV) or the heart (act V). */
    @Override
    protected float mitigate(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            return shell(sl, source, amount);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Zagred's shell failed: {}", e.toString());
            return amount;
        }
    }

    private float shell(ServerLevel sl, DamageSource source, float amount) {
        long t = sl.getGameTime();
        boolean anti = DanteMoves.antiMagicHit(source);
        float a = staggered(t) ? amount * 1.25f : amount;
        if (gatesOpen()) {
            float k = heart.factor(t, anti);
            if (k < 1f && source.getEntity() instanceof ServerPlayer sp && t - lastClosedHint > 60) {
                lastClosedHint = t;
                sp.displayClientMessage(ZagredScreen.text("message.nusmp.zagred.heart.closed", "His heart is closed: strike when he finishes a Word.", 0xB8B0C8, true), true);
            }
            return a * k;
        }
        if (!ward.present()) return a;
        ZagredWard.Grain grain = anti || AntiMagic.isMagic(source) ? ZagredWard.Grain.ARCANE : ZagredWard.Grain.KINETIC;
        float through = ward.absorb(a, grain, anti, t);
        afterWardHit(sl, t);
        return through;
    }

    /** The effects of whatever the last blow did to the ward: layers breaking, the ward falling. */
    private void afterWardHit(ServerLevel sl, long t) {
        int broken = ward.lastBroken();
        if (broken > 0) {
            Vec3 c = getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, c.add(0, 0.3, 0), c, VIOLET | 0xFF000000, 16, 1.6f);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.4f, 0.8f);
        }
        if (ward.lastReformStarted()) unwritten(sl, t);
    }

    /** The last layer fell: for three seconds he is unwritten, he takes half again, and the Word he was speaking falls apart. */
    private void unwritten(ServerLevel sl, long t) {
        composure.unwrite(t, ZagredWard.REFORM_TICKS);
        Move m = castingMoveOrNull();
        if (state() == STATE_CASTING && m != null && !m.ultimate()) {
            interruptCast();
            plan.clear();
        }
        Vec3 c = getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, c, c.add(0, 1, 0), 0xFFFFFFFF, 20, 2.6f);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        ZagredScreen.title(sl, position(), ZagredScreen.text("message.nusmp.zagred.unwritten", "UNWRITTEN", 0xFFD060, true),
                ZagredScreen.text("message.nusmp.zagred.unwritten.sub", "He cannot act and takes half again: strike now", 0xE8E0F8, false), 2, 50, 10);
        say(ZagredLines.UNWRITTEN[getRandom().nextInt(ZagredLines.UNWRITTEN.length)]);
    }

    /** The Last Word's third mark, or an Arch daemon falling, breaks the outermost layer of the ward at once (in the devil form it opens the heart). */
    public void breakWardLayer() { guarded("ward break", this::wardLayerBroken); }

    private void wardLayerBroken() {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        long t = sl.getGameTime();
        if (gatesOpen()) {
            heart.open(t, ZagredHeart.AFTER_CUT);
            return;
        }
        if (ward.present() && ward.breakOne(t)) afterWardHit(sl, t);
    }

    // ================================================================ the fight, every tick
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl) || isDeadOrDying()) return;
        try {
            long t = sl.getGameTime();
            if (fightActive()) {
                tickWard(sl, t);
                tickHeartCue(sl, t);
                tickShatter(sl, t);
                ZagredDecrees.tickVerdict(this, sl, t);
                ZagredDecrees.tickRule(this, sl, t);
                if (helpless(t)) {                                           // unwritten or staggered: he stands still
                    getNavigation().stop();
                    setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
                }
            }
            if (tickCount % 20 == 0) everySecond(sl, t);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] Zagred's tick failed: {}", e.toString());
        }
    }

    private void tickWard(ServerLevel sl, long t) {
        if (!ward.present()) return;
        Move m = castingMoveOrNull();
        boolean channeling = state() == STATE_CASTING && m != null && m.ultimate();
        if (ward.tick(t, channeling, archAlive > 0)) {
            announce("The ward is written again, one layer short for twenty seconds.");
            Vec3 c = position();
            ZagredFx.ring(sl, c, 4f, 24, ZagredFx.PALE);
            sl.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.6f, 0.7f);
        }
        if (t % 10 == 0 && !ward.reforming(t)) {
            int n = ward.effective(t);
            Vec3 c = position();
            for (int i = 0; i < n; i++) {
                if (!ward.standing(i)) continue;
                int rgb = ZagredWard.grainOfLayer(i) == ZagredWard.Grain.ARCANE ? VIOLET : RED;
                Tells.ring(sl, c.add(0, 0.35 + 0.42 * i, 0), 1.1 + 0.18 * i, rgb, 0.9f, 16);
            }
        }
    }

    /** The heart opening is a thing to see and hear: a flash on his chest and a chime, and a glow while it stays open. */
    private void tickHeartCue(ServerLevel sl, long t) {
        if (!gatesOpen() || transforming()) {
            heartWasOpen = false;
            return;
        }
        boolean open = heart.isOpen(t);
        Vec3 chest = position().add(0, getBbHeight() * 0.62, 0);
        if (open && !heartWasOpen) {
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, chest, chest.add(0, 0.4, 0), 0xFF000000 | RED, 14, 1.2f);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.4f, 1.6f);
            if (!heartSpoken) {
                heartSpoken = true;
                say(ZagredLines.HEART);
            }
        }
        if (open && t % 6 == 0) Tells.gather(sl, chest, 0.6, RED, 3);
        heartWasOpen = open;
    }

    /** Shatter: while the dome stands every projectile that is not his is erased, and the spell bolts of the players come apart. */
    private void tickShatter(ServerLevel sl, long t) {
        if (t >= shatterUntil) return;
        double r = ZagredMath.shatterRadius();
        int erased = SpellRuntime.dissolveBolts(sl, position(), r, this);
        int shown = 0;
        for (Projectile p : sl.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(r), pr -> pr.isAlive() && !isMine(pr))) {
            if (p.distanceToSqr(this) > r * r) continue;
            if (shown++ < 6) VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, p.position(), p.position().add(0, 0.3, 0), ZagredWord.SHATTER.argb(), 8, 0.5f);
            p.discard();
            erased++;
        }
        if (erased > 0 && t % 3 == 0) sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0f, 1.7f);
    }

    private void everySecond(ServerLevel sl, long t) {
        upkeepDaemons(sl);
        attention.prune(t);
        if (fightActive() && !transforming()) {
            swapTarget(sl, t);
            statusLine(sl, t);
        }
    }

    /** From the Judge on he turns on whoever has hurt him most, so no one can hold his eyes forever and no one can hide behind a tank. */
    private void swapTarget(ServerLevel sl, long t) {
        if (stage() < 2 || t < nextSwap) return;
        nextSwap = t + ZagredMath.threatSwapTicks();
        UUID top = attention.topThreat(id -> sl.getPlayerByUUID(id) instanceof ServerPlayer p && isFoe(p) && p.distanceToSqr(this) <= 48.0 * 48.0);
        attention.decayThreat();
        if (top == null || state() == STATE_CASTING || helpless(t) || fighterCount() < 2) return;
        if (!(sl.getPlayerByUUID(top) instanceof ServerPlayer p)) return;
        LivingEntity cur = getTarget();
        if (cur != null && cur.getUUID().equals(top)) return;
        lockTargetUntil = 0;
        setTarget(p);
        lockTargetUntil = t + 100;
        p.displayClientMessage(ZagredScreen.text("message.nusmp.zagred.turns", "He turns on you.", RED, true), true);
        Tells.ring(sl, p.position(), 1.4, RED, 1.2f, 20);
    }

    /** One line a second on the action bar of everyone near: the ward's layers and their grain, or the heart, and whatever else is in force. */
    private void statusLine(ServerLevel sl, long t) {
        MutableComponent line = Component.empty();
        if (gatesOpen()) {
            if (heart.isOpen(t)) line.append(bold("HEART OPEN " + secs(heart.openLeft(t)) + "s", RED));
            else line.append(bold("HEART CLOSED", 0x8C8498));
        } else if (ward.present()) {
            if (ward.reforming(t)) {
                line.append(bold("UNWRITTEN", 0xFFD060));
            } else {
                line.append(bold("WARD ", 0xE8E0F8));
                for (int i = ward.effective(t) - 1; i >= 0; i--) {                           // the outermost layer breaks first
                    if (!ward.standing(i)) continue;
                    boolean arcane = ZagredWard.grainOfLayer(i) == ZagredWard.Grain.ARCANE;
                    line.append(bold("[" + (arcane ? "SPELL " : "BLOW ") + Math.round(ward.fraction(i) * 100) + "%] ", arcane ? VIOLET : RED));
                }
            }
        }
        if (composure.silenced(t) && !ward.reforming(t)) line.append(bold(" SILENCED", 0x7FD8FF));
        if (staggered(t)) line.append(bold(" STAGGERED", 0xFFD060));
        if (rule != null && t < ruleUntil) line.append(bold(" RULE: " + rule.text().toUpperCase(Locale.ROOT) + " " + secs(ruleUntil - t) + "s", RED));
        if (line.getSiblings().isEmpty()) return;
        ZagredScreen.bar(sl, position(), line);
    }

    // ================================================================ the daemons
    /** Counts his pages, turrets and anchors (the caps of the acts are kept against these numbers) and removes rule stones nobody is tracking. */
    void upkeepDaemons(ServerLevel sl) {
        int l = 0, g = 0, a = 0;
        for (GrimoireDaemonEntity d : GrimoireDaemonEntity.of(sl, getUUID(), arenaCenter(), 80)) {
            switch (d.tier()) {
                case GrimoireDaemonEntity.LESSER -> l++;
                case GrimoireDaemonEntity.GREATER -> g++;
                case GrimoireDaemonEntity.ARCH -> a++;
                case GrimoireDaemonEntity.STONE -> { if (!stones.contains(d.getUUID())) d.discard(); }      // a stone left by a reloaded fight
                default -> { }
            }
        }
        lesserAlive = l;
        greaterAlive = g;
        archAlive = a;
        stones.removeIf(id -> !(sl.getEntity(id) instanceof GrimoireDaemonEntity s && s.isAlive()));
    }

    void discardStones(ServerLevel sl) {
        for (UUID id : new ArrayList<>(stones)) {
            if (sl.getEntity(id) instanceof GrimoireDaemonEntity s && s.isAlive()) {
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, s.position().add(0, 1, 0), s.position(), 0xFFFFFFFF, 12, 0.8f);
                s.discard();
            }
        }
        stones.clear();
    }

    /** Ends the rule (it ran out, or three stones cut the sentence): the stones go, the counters reset. */
    void endRule(ServerLevel sl) {
        rule = null;
        pendingRule = null;
        ruleUntil = 0;
        stoneKills = 0;
        tracks.clear();
        discardStones(sl);
    }

    @Override public Mob daemonMaster() { return this; }

    @Override public int daemonAct() { return ZagredMath.act(power()); }

    @Override public List<LivingEntity> daemonFoes(Vec3 at, double radius) { return foesAround(at, radius); }

    @Override public void daemonBlast(LivingEntity victim, float raw) { strike(victim, raw, true); }

    /** An Arch daemon fell: in the human form its anchor on the ward breaks a layer; in the devil form it opens the heart. */
    @Override
    public void archFell() { guarded("anchor fall", this::anchorFell); }

    private void anchorFell() {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        long t = sl.getGameTime();
        upkeepDaemons(sl);
        if (gatesOpen()) {
            heart.open(t, ZagredHeart.AFTER_ANCHOR);
            return;
        }
        if (ward.present() && ward.breakOne(t)) afterWardHit(sl, t);
    }

    /** A rule stone broke. The third one cuts the sentence: the rule is erased, the Overwrite being spoken falls apart, and he is staggered. */
    @Override
    public void stoneBroken() { guarded("stone break", this::stoneFell); }

    private void stoneFell() {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        stoneKills++;
        int need = ZagredMath.stonesNeeded();
        if (stoneKills < need) {
            announce("A stone of the sentence breaks (" + stoneKills + " of " + need + ").");
            return;
        }
        Move m = castingMoveOrNull();
        if (state() == STATE_CASTING && m != null && ZagredTable.OVERWRITE.equals(m.id())) interruptCast();
        endRule(sl);
        long t = sl.getGameTime();
        staggerUntil = t + ZagredMath.staggerAfterStones();
        if (gatesOpen()) heart.open(t, ZagredHeart.AFTER_CUT);
        Vec3 c = getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, c, c.add(0, 1, 0), 0xFFFFFFFF, 24, 3f);
        sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        ZagredScreen.title(sl, position(), ZagredScreen.text("message.nusmp.zagred.stones", "THE SENTENCE BREAKS", 0xFFFFFF, true),
                ZagredScreen.text("message.nusmp.zagred.stones.sub", "He is staggered: strike now", 0xE8E0F8, false), 2, 40, 10);
        say(ZagredLines.STONES);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() { guarded("fight start", this::fightStarted); }

    /** The fight begins: a fresh ward, the opening title and the two lessons. */
    private void fightStarted() {
        if (!(level() instanceof ServerLevel sl)) return;
        resetEncounters(sl);
        long t = sl.getGameTime();
        ward.set(ZagredMath.wardLayers(stage()), getMaxHealth(), ZagredMath.wardLayerShare(), t);
        nextWordAt = t + 60;                                                   // three seconds to read the opening lines before the first Word
        ZagredScreen.notice(sl, position(), "message.nusmp.zagred.act.1", "ACT I", VIOLET, "message.nusmp.zagred.act.1.name", "The Reader");
        announce("Zagred speaks his Words aloud. Read the title on your screen and answer it: keep moving, jump, raise a shield, break his line of sight, or hold anti-magic.");
        announce("His ward has layers. Violet ones wear down to spells, red ones to swords and arrows, and anti-magic breaks through both. When the last one falls he is unwritten.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        guarded("fight end", () -> fightEnded(won));
    }

    /** The fight is over, won or abandoned: everything he held is let go, and a victory pays out. */
    private void fightEnded(boolean won) {
        if (!(level() instanceof ServerLevel sl)) return;
        long t = sl.getGameTime();
        resetEncounters(sl);
        ward.set(0, getMaxHealth(), 0f, t);
        for (GrimoireDaemonEntity d : GrimoireDaemonEntity.of(sl, getUUID(), arenaCenter(), 128)) d.discard();
        if (!won) return;
        sl.playSound(null, blockPosition(), NUSounds.ZAGRED_PHASE.get(), SoundSource.HOSTILE, 2f, 0.6f);
        for (UUID id : new ArrayList<>(fighters())) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p == null) continue;
            KotodamaWords.reward(p);                                          // the same reward rule as before the remake (the config decides it)
            BossRelics.give(p);
        }
    }

    /** Forgets everything an encounter held: verdict marks, the rule and its stones, the cut and unwritten timers, the heart, the threat list. */
    private void resetEncounters(ServerLevel sl) {
        marks.clear();
        verdictUntil = 0;
        shatterUntil = 0;
        reverseUntil = 0;
        staggerUntil = 0;
        plan.clear();
        lastRule = null;
        endRule(sl);
        composure.reset();
        heart.close();
        heartWasOpen = false;
        heartSpoken = false;
        attention.clear();
        lockTargetUntil = 0;
        lastMoveId = "";
    }

    @Override
    protected void onPowerChanged(int old, int now) { guarded("act change", () -> actChanged(old, now)); }

    /** A step of the power curve: the act changes, he writes a fresh ward (or gives it up for the heart), and the daemons of the act come. */
    private void actChanged(int old, int now) {
        if (!(level() instanceof ServerLevel sl)) return;
        int before = PowerCurve.stage(old), after = PowerCurve.stage(now);
        if (after <= before) return;
        long t = sl.getGameTime();
        int act = after + 1;
        QuestHooks.bossPhase(this, "zagred", act);                             // "zagred:2" .. "zagred:5": the act that STARTS (the old boss reported 2 to 4)
        sl.playSound(null, blockPosition(), NUSounds.ZAGRED_PHASE.get(), SoundSource.HOSTILE, 2f, 0.8f);
        if (after < 4) {
            ward.set(ZagredMath.wardLayers(after), getMaxHealth(), ZagredMath.wardLayerShare(), t);            // each act he writes a fresh ward
            String name = ZagredMath.actName(act);
            ZagredScreen.notice(sl, position(), "message.nusmp.zagred.act." + act, "ACT " + roman(act), after >= 3 ? RED : VIOLET,
                    "message.nusmp.zagred.act." + act + ".name", Character.toUpperCase(name.charAt(0)) + name.substring(1));
        } else {
            ward.set(0, getMaxHealth(), 0f, t);
        }
        ZagredFx.corrupt(sl, position(), 14f, 60, after >= 3 ? ZagredFx.RED : 0xFF000000 | VIOLET);
        ZagredFx.ring(sl, position(), 8f, 30, after >= 3 ? ZagredFx.RED : 0xFF000000 | VIOLET);
        if (after >= 2 && after < 4) ZagredDecrees.anchors(this, sl);
    }

    @Override
    protected void onGatesOpened() { guarded("gates", this::gatesOpened); }

    /** The Gates: the ward goes, the heart takes its place, every encounter in force is cancelled. */
    private void gatesOpened() {
        if (!(level() instanceof ServerLevel sl)) return;
        long t = sl.getGameTime();
        marks.clear();
        verdictUntil = 0;
        shatterUntil = 0;
        reverseUntil = 0;
        staggerUntil = 0;
        plan.clear();
        endRule(sl);
        composure.reset();
        ward.set(0, getMaxHealth(), 0f, t);
        heart.close();
        heart.open(t + PowerCurve.TRANSFORM_TICKS, ZagredHeart.AFTER_GATES);       // the first window opens as the transformation ends
        announce("The Gates open. Zagred gives up the borrowed body: under it is the Word Soul, and its heart is closed.");
        announce("A closed heart takes less than half of a blow. It opens for a few seconds whenever he finishes a Word, when a Word is cut and when an Arch page falls. Anti-magic always reaches it.");
        ZagredFx.corrupt(sl, position(), 20f, 90, ZagredFx.RED);
        ZagredFx.ring(sl, position(), 10f, 60, ZagredFx.RED);
        later(PowerCurve.TRANSFORM_TICKS, ZagredDecrees.safe("gates anchors", () -> { if (fightActive()) ZagredDecrees.anchors(this, sl); }));
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ZgLayers", ward.count());
        tag.putFloat("ZgCap", ward.cap());
        float[] hp = ward.hitPoints();
        for (int i = 0; i < hp.length; i++) tag.putFloat("ZgHp" + i, hp[i]);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("ZgLayers")) return;
        float[] hp = new float[ZagredWard.MAX];
        for (int i = 0; i < hp.length; i++) hp[i] = tag.getFloat("ZgHp" + i);
        ward.load(tag.getInt("ZgLayers"), tag.getFloat("ZgCap"), hp, level().getGameTime());
    }
}
