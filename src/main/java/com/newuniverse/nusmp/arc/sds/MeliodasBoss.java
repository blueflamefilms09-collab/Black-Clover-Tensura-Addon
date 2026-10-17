package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.SpellSeal;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.quests.NusmpEvents;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Meliodas, the Dragon's Sin of Wrath, is the deepest of the three bosses of the first Sins group and his fight is a lesson in what to throw at a
 * boss. His rules (docs/arc/meliodas.md): <b>Full Counter</b> throws magic that hits his front back at its caster (never a blade, an arrow or anti-magic;
 * never from behind unless his images guard him; at most 30% of the caster's health per answer; and enough magic breaks it, see {@link FullCounter}); the
 * <b>Demon Mark</b> ladder gives each power step a meaning ({@link DemonKingMath}): at 80% he is in <b>Assault Mode</b>, which throws away his guard (no
 * Full Counter, +12% damage taken, the window the party was promised), and at the Gates he is the Demon King, whose <b>Wrath</b> is a nova that eats the
 * magic thrown at it and is broken by steel; and the black flame of <b>Hellblaze</b> burns on after you leave it. The class holds those rules and the move
 * list; the strikes are in {@link MeliodasMoves}, the numbers in {@link MeliodasTable} and {@link DemonKingMath}, all of it testable without the game.
 * Server side only; nothing here has run in a game yet.
 */
public class MeliodasBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = ArcRoster.register(SdsCharacters.MELIODAS);
    /** How the client draws him: the numbers of {@code tools/arc/manifest.json} for the model {@code meliodas}. */
    public static final ArcLook LOOK = new ArcLook("meliodas", 1.84f, 1.06f, 1.88f, 0.40f, 1.08f, 0.71f, true);
    public static final DeferredHolder<EntityType<?>, EntityType<MeliodasBoss>> TYPE =
            ArcEntities.boss(CHARACTER.id(), MeliodasBoss::new, 0.62f, 1.84f, LOOK, MeliodasBoss::createAttributes);

    private static final int RGB = CHARACTER.color(), ACCENT = CHARACTER.accent(), HELL = MeliodasMoves.HELL, WARN = MeliodasMoves.WARN;
    private static final ResourceLocation WRATH_SPEED = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "sds_meliodas_wrath");
    private static final double SLASH_REACH = 4.6, SLASH_HALF = Math.toRadians(58), KAMI_REACH = 6.4, KAMI_HALF = Math.toRadians(36),
            LUNGE_HALF_WIDTH = 1.3, IMAGE_RING = 5.0, IMAGE_HALF_WIDTH = 1.3, RUSH_HALF_WIDTH = 1.2, STEP_RADIUS = 3.2, PURGATORY_RADIUS = 3.4;

    // ---- the fight's state (all server side; a reload in the middle of a fight starts these afresh)
    private final FullCounter counter = new FullCounter();
    private Vec3 aim = new Vec3(0, 0, 1);
    private double lockedLength;
    private List<Vec3> marks = List.of();
    private String lockedFor = "";
    private long staggerUntil, lastQuip, lastReflectNote;
    private final java.util.Set<String> hinted = new java.util.HashSet<>();
    private int novaPhase, novaSerial;
    private double novaFed, novaPhysical, novaRadius = DemonKingMath.NOVA_BASE_RADIUS;
    private double appliedSpeed = Double.NaN;

    public MeliodasBoss(EntityType<MeliodasBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 8.0);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    /** The Demon Mark: his own damage factor on top of the power curve (1.00 at the Captain up to 1.30 as the Demon King). */
    @Override protected float damageScale() { return (float) DemonKingMath.damageScale(power()); }

    /** The plain blow comes every second or so; the telegraphed cuts are the heavy ones. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }

    /** A swordsman: he wants to be within a few blocks whatever the ranged moves ask for. */
    @Override protected double preferredRange(double suggested) { return Math.max(2.2, Math.min(4.5, suggested)); }

    /** The Demon King wears the second model; it is the same size. */
    @Override protected int gatesForm() { return FORM_HYBRID; }

    @Override protected float formScale(int form) { return 1.0f; }

    private void applySpeed() {
        double bonus = DemonKingMath.speedBonus(power());
        if (bonus == appliedSpeed) return;
        appliedSpeed = bonus;
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrReplacePermanentModifier(new AttributeModifier(WRATH_SPEED, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    // ================================================================ the bar and the lines
    @Override
    public String barTitle() {
        String mark = switch (DemonKingMath.mark(power())) {
            case 0 -> "Captain";
            case 1 -> "Demon Mark";
            case 2 -> "Mark spreading";
            case 3 -> "Assault Mode";
            default -> "Demon King";
        };
        return CHARACTER.name() + " - " + mark + " " + power() + "%";
    }

    @Override
    protected String powerLine(int old, int now) {
        int before = DemonKingMath.mark(old), after = DemonKingMath.mark(now);
        if (after >= 3 && before < 3) return "That's enough holding back. Assault Mode: no more tricks, only this!";
        if (after >= 2 && before < 2) return "The mark's spreading... Lostvayne is getting restless too.";
        if (after >= 1 && before < 1) return "Heh. The mark's waking up. Try to keep up.";
        return "I'm getting serious.";
    }

    /** The framework announces the Gates in the devil's voice; the party reads his own. */
    @Override
    public void announce(String text) {
        super.announce(SdsaStrikes.QLIPHOTH_GATES.equals(text) ? "The Demon Mark spreads over all of Meliodas: the Demon King wakes!" : text);
    }

    /** A teaching line about a move, said the first time it comes in a fight and not again (the chat is not a wall of text). */
    private void hint(String id, String text) {
        if (hinted.add(id)) announce(text);
    }

    /** One of 'lines' now and then (at most every seven seconds), so the chat is not a wall of shouting. */
    private void quip(String... lines) {
        if (tickCount - lastQuip < 140) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        counter.reset();
        resetNova();
        staggerUntil = 0;
        hinted.clear();
        announce("Full Counter: magic that hits Meliodas from the front while the stance is up comes back at its caster. Steel, arrows, anti-magic and anything from behind go straight through, and enough magic shatters the stance.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        counter.reset();
        resetNova();
        staggerUntil = 0;
        marks = List.of();
        lockedFor = "";
        super.onFightEnd(won);
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        applySpeed();
        int before = DemonKingMath.mark(old), after = DemonKingMath.mark(now);
        if (after >= 3 && before < 3 && !DemonKingMath.king(now)) {
            counter.drop(level().getGameTime());
            announce("Assault Mode: Meliodas gives up his guard for speed and fury. He cannot raise Full Counter and takes 12% more from every blow until the Gates open. Hit him now.");
        }
    }

    @Override
    protected void onGatesOpened() {
        applySpeed();
        announce("The Demon King: Meliodas guards again, Full Counter is back (longer, harder to break), and his Wrath can swallow the room. Do not feed it magic.");
    }

    @Override
    protected void interruptCast() {
        if (novaPhase == 1) {
            novaPhase = 0;
            novaSerial++;
        }
        super.interruptCast();
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            if (tickCount % 20 == 0) applySpeed();
            long now = sl.getGameTime();
            if (novaPhase == 1) {
                Move cast = castingMoveOrNull();
                if (cast == null || !MeliodasTable.WRATH.equals(cast.id())) {          // the wind-up was cancelled (no target): the nova is gone with it
                    novaPhase = 0;
                    novaSerial++;
                }
            }
            if (counter.up(now) && tickCount % 16 == 0) showStance(sl, now);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Meliodas's tick failed: {}", e.toString());
        }
    }

    /** The glowing pane in front of him while the stance is up (and one on every side while his images guard him). */
    private void showStance(ServerLevel sl, long now) {
        Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
        Vec3 look = SdsaStrikes.unitFlat(getViewVector(1f), new Vec3(0, 0, 1));
        int sides = counter.omni(now) ? 4 : 1;
        for (int i = 0; i < sides; i++) {
            Vec3 d = SdsaStrikes.rotateY(look, i * Math.PI / 2.0);
            SdsaStrikes.fx(sl, VfxShape.MIRROR_PANE, chest.add(d.scale(1.4)), chest.add(d.scale(2.4)), ACCENT, 20, 1.6f, false);
        }
    }

    // ================================================================ the damage that reaches him
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            Entity src = source.getEntity();
            boolean magic = !DanteMoves.antiMagicHit(source) && SpellSeal.isSpellDamage(source);
            if (novaPhase != 0) return nova(sl, src, amount, magic);
            if (magic) counter.notePressure(now);
            if (magic && counter.up(now)) {
                Vec3 look = SdsaStrikes.flat(getViewVector(1f)), toSrc = src == null ? Vec3.ZERO : SdsaStrikes.flat(src.position().subtract(position()));
                double dot = look.lengthSqr() < 1e-6 || toSrc.lengthSqr() < 1e-6 ? 1.0 : look.normalize().dot(toSrc.normalize());
                LivingEntity caster = src instanceof LivingEntity le ? le : null;
                FullCounter.Result r = counter.onHit(now, true, DemonKingMath.frontal(dot), amount, caster == null ? -1 : caster.getId(), caster == null ? 0f : caster.getMaxHealth());
                switch (r.verdict()) {
                    case REFLECT -> {
                        reflect(sl, caster, r.reflected());
                        return 0f;
                    }
                    case SHATTER -> shatter(sl, src);
                    default -> { }
                }
            }
            float a = (float) (amount * DemonKingMath.takenFactor(power()));
            if (tickCount < staggerUntil && !exposed()) a *= EXPOSED_BONUS;
            return a;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Meliodas's guard failed: {}", e.toString());
            return amount;
        }
    }

    /** The spell is swallowed and the caster is hit with their own power, capped by {@link FullCounter}. */
    private void reflect(ServerLevel sl, LivingEntity caster, float back) {
        Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
        SdsaStrikes.fx(sl, VfxShape.MIRROR_SHATTER, chest, chest.add(0, 1, 0), ACCENT, 14, 1.2f, true);
        SdsaStrikes.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 0.5f);
        if (caster == null || back <= 0f || !caster.isAlive() || !isFoe(caster)) return;
        caster.hurt(SdsaStrikes.source(this, TensuraDamageTypes.REFLECTED), back);
        if (caster instanceof ServerPlayer p && tickCount - lastReflectNote > 20) {
            lastReflectNote = tickCount;
            p.displayClientMessage(Component.literal("Full Counter: your spell comes back at you. Strike with steel, from behind, or overload it.").withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        quip("Go on, throw more at me.", "Is that all your magic has?");
    }

    /** The stance broke: the hit that did it lands, he is open for a moment, and the caster is told they did it. */
    private void shatter(ServerLevel sl, Entity breaker) {
        staggerUntil = tickCount + 30;
        Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
        SdsaStrikes.fx(sl, VfxShape.SPACE_SHATTER, chest, chest.add(0, 1, 0), ACCENT, 20, 2.0f, true);
        SdsaStrikes.sound(sl, position(), SoundEvents.SHIELD_BREAK, 1.8f, 0.6f);
        announce("Full Counter shatters under the weight of the magic! Meliodas is open.");
        credit(breaker, "sds.full_counter_broken");
        quip("Tch. That was a lot of magic.");
    }

    private static void credit(Entity who, String key) {
        try {
            if (who instanceof ServerPlayer p) NusmpEvents.fire(p, key);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] the quest hook {} failed: {}", key, e.toString());
        }
    }

    // ================================================================ the Demon King's Wrath: the nova eats magic and is broken by steel
    private void resetNova() {
        novaPhase = 0;
        novaSerial++;
        novaFed = 0;
        novaPhysical = 0;
        novaRadius = DemonKingMath.NOVA_BASE_RADIUS;
    }

    /**
     * While the nova is being channelled, magic is swallowed (it feeds the ring) and everything else counts toward breaking it; after the channel
     * (the ring stands still) every blow lands, and still counts toward the break.
     */
    private float nova(ServerLevel sl, Entity src, float amount, boolean magic) {
        if (magic && novaPhase == 1) {
            novaFed += amount;
            return 0f;
        }
        novaPhysical += amount;
        if (DemonKingMath.broken(novaPhysical / getMaxHealth())) breakNova(sl, src);
        return amount;
    }

    private void breakNova(ServerLevel sl, Entity breaker) {
        boolean channelling = novaPhase == 1;
        novaPhase = 0;
        novaSerial++;
        staggerUntil = tickCount + 80;
        if (channelling) interruptCast();
        Vec3 c = position();
        SdsaStrikes.fx(sl, VfxShape.SPACE_SHATTER, c.add(0, 1, 0), c.add(0, 2, 0), HELL, 20, 3.0f, true);
        SdsaStrikes.sound(sl, c, SoundEvents.SHIELD_BREAK, 2.2f, 0.4f);
        announce("The Wrath is broken! Meliodas staggers, and the ring dies with it.");
        credit(breaker, "sds.nova_broken");
        quip("Argh... you actually cut through it.");
    }

    // ================================================================ choosing the next move
    /**
     * The scheduler's draw, with three of his own rules: nothing starts while he is staggered; Full Counter is only on offer when the stance is ready, the
     * server allows it and he is not in Assault Mode, and it comes first when the party starts casting at him; and the images wait for a free stance.
     */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (tickCount < staggerUntil || novaPhase == 2) return -1;
        long now = level().getGameTime();
        int fc = moveIndex(MeliodasTable.FULL_COUNTER), im = moveIndex(MeliodasTable.IMAGES);
        boolean canCounter = SdsaConfig.fullCounter() && DemonKingMath.canCounter(power()) && counter.ready(now);
        if (canCounter && fc >= 0 && counter.pressed(now) && moveEligible(MeliodasTable.byId(MeliodasTable.FULL_COUNTER), s)) return fc;
        List<Move> view = new ArrayList<>(bk);
        if (fc >= 0 && !canCounter) view.set(fc, blocked(bk.get(fc)));
        if (im >= 0 && counter.up(now)) view.set(im, blocked(bk.get(im)));
        return super.chooseMove(view, s, t);
    }

    /** A copy of a move that can never be chosen (its power is out of reach), so a boss can take a move off the table for now without moving the indices. */
    private static Move blocked(Move m) {
        return Move.builder(m.id(), m.label()).minPower(1000).range(m.minRange(), m.maxRange()).run((b, t) -> { });
    }

    // ================================================================ the moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MeliodasTable.Spec s : MeliodasTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            final String id = s.id();
            final Move.MoveRun run = runOf(id);
            b.tell((bs, t, left, total) -> {
                try {
                    tellOf(id, t, left, total);
                } catch (RuntimeException e) {
                    LogUtils.getLogger().warn("[nusmp] Meliodas's tell of {} failed: {}", id, e.toString());
                }
            });
            out.add(b.run((bs, t) -> {
                if (!(level() instanceof ServerLevel)) return;
                run.run(bs, t);
            }));
        }
        return out;
    }

    // ---------------------------------------------------------------- locking what the wind-up shows
    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    private Vec3 faceOf(LivingEntity target) {
        return SdsaStrikes.unitFlat(target.position().subtract(position()), SdsaStrikes.unitFlat(getViewVector(1f), new Vec3(0, 0, 1)));
    }

    private void lockAim(String id, LivingEntity target) {
        aim = faceOf(target);
        lockedFor = id;
    }

    private Vec3 takeAim(String id, LivingEntity target) {
        Vec3 a = lockedFor.equals(id) ? aim : faceOf(target);
        lockedFor = "";
        return a;
    }

    private void lockMarks(String id, List<Vec3> list) {
        marks = list;
        lockedFor = id;
    }

    private List<Vec3> takeMarks(String id, LivingEntity target) {
        List<Vec3> m = lockedFor.equals(id) && !marks.isEmpty() ? marks : List.of(ZogratisBoss.ground(level(), target.position()));
        marks = List.of();
        lockedFor = "";
        return m;
    }

    /** The centres of the Hellblaze fields: on the target, and with the Gates open on another foe (or opposite the boss when the target is alone). */
    private List<Vec3> fieldMarks(LivingEntity target) {
        List<Vec3> out = new ArrayList<>();
        out.add(ZogratisBoss.ground(level(), target.position()));
        if (!gatesOpen()) return out;
        for (LivingEntity e : foes(40)) {
            if (e != target && e.distanceToSqr(target) > 36) { out.add(ZogratisBoss.ground(level(), e.position())); return out; }
        }
        Vec3 off = target.position().subtract(position()).multiply(1, 0, 1);
        out.add(ZogratisBoss.ground(level(), target.position().add(SdsaStrikes.unitFlat(off, new Vec3(1, 0, 0)).scale(7))));
        return out;
    }

    /** Where the rush goes: the target first, then the other foes, then spots round the target when there are not enough foes. */
    private List<Vec3> rushMarks(LivingEntity target, int n) {
        List<Vec3> out = new ArrayList<>();
        List<LivingEntity> foes = foes(40);
        out.add(ZogratisBoss.ground(level(), target.position()));
        for (LivingEntity e : foes) { if (out.size() >= n) break; if (e != target) out.add(ZogratisBoss.ground(level(), e.position())); }
        int k = 0;
        while (out.size() < n) {
            double a = (k++ + 1) * Math.PI * 2 / 3.0 + getRandom().nextDouble();
            out.add(ZogratisBoss.ground(level(), target.position().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5)));
        }
        return out;
    }

    /** The columns of the Purgatory: on the target and the other foes, then round the target; more of them as his power rises. */
    private List<Vec3> purgatoryMarks(LivingEntity target) { return SdsaStrikes.spread(this, target, 3 + tuning().castSlots(), PURGATORY_RADIUS); }

    // ---------------------------------------------------------------- the telegraphs
    private void tellOf(String id, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean f = first(left, total);
        Vec3 chest = position().add(0, getBbHeight() * 0.6, 0);
        switch (id) {
            case MeliodasTable.SLASH -> {
                if (f) lockAim(id, target);
                SdsaStrikes.tellCone(sl, position(), aim, SLASH_REACH, SLASH_HALF, RGB);
                Tells.gather(sl, chest.add(aim.scale(0.9)), 0.4, ACCENT, 3);
            }
            case MeliodasTable.KAMICHIGIRI -> {
                if (f) {
                    lockAim(id, target);
                    hint(id, "Kamichigiri: a long, heavy cut. Leave the cone or get behind him.");
                }
                SdsaStrikes.tellCone(sl, position(), aim, KAMI_REACH, KAMI_HALF, WARN);
                Tells.gather(sl, chest.add(aim.scale(0.9)), 0.6, ACCENT, 5);
            }
            case MeliodasTable.LUNGE -> {
                if (f) {
                    lockAim(id, target);
                    lockedLength = Math.max(6.0, Math.min(14.0, distanceTo(target) + 2.0));
                }
                SdsaStrikes.tellStrip(sl, position(), aim, lockedLength, LUNGE_HALF_WIDTH, RGB);
                Tells.gather(sl, chest, 0.5, ACCENT, 3);
            }
            case MeliodasTable.WAVE -> {
                if (f) lockAim(id, target);
                for (double a : waveAngles()) {
                    Vec3 d = SdsaStrikes.rotateY(aim, a);
                    Tells.line(sl, chest, chest.add(d.scale(16)), RGB);
                }
                Tells.gather(sl, chest, 0.5, ACCENT, 3);
            }
            case MeliodasTable.FULL_COUNTER -> {
                if (f) SdsaStrikes.fx(sl, VfxShape.MIRROR_PANE, chest.add(SdsaStrikes.unitFlat(getViewVector(1f), new Vec3(0, 0, 1)).scale(1.4)),
                        chest.add(SdsaStrikes.unitFlat(getViewVector(1f), new Vec3(0, 0, 1)).scale(2.4)), ACCENT, total, 1.6f, false);
                Tells.ring(sl, position(), 2.0 + 0.6 * (total - left) / Math.max(1, total), ACCENT, 1.2f, 32);
                Tells.gather(sl, chest, 0.8, RGB, 4);
            }
            case MeliodasTable.HELLBLAZE -> {
                if (f) {
                    lockMarks(id, fieldMarks(target));
                    for (Vec3 m : marks) SdsaStrikes.fx(sl, VfxShape.MAGIC_CIRCLE, m.add(0, 0.05, 0), m.add(0, 1, 0), HELL, total, (float) (DemonKingMath.fieldRadius(power()) / 1.6), false);
                }
                double r = DemonKingMath.fieldRadius(power());
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, r, HELL, 1.4f, (int) Math.min(80, 14 + r * 5));
                    Tells.ring(sl, m, Math.max(0.6, r * left / Math.max(1, total)), ACCENT, 1.1f, 28);
                    Tells.column(sl, m, 3, ACCENT);
                }
            }
            case MeliodasTable.DEMON_STEP -> {
                if (f) lockMarks(id, List.of(behind(target)));
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, STEP_RADIUS, WARN, 1.3f, 40);
                    Tells.column(sl, m, 4, ACCENT);
                    Tells.line(sl, chest, m.add(0, 1, 0), RGB);
                }
            }
            case MeliodasTable.BURST -> {
                double r = burstRadius();
                Tells.ring(sl, position(), r * (total - left) / Math.max(1, total), HELL, 1.3f, 48);
                if (left % 6 == 0) Tells.ring(sl, position(), r, WARN, 1.2f, (int) Math.min(80, 14 + r * 5));
                Tells.gather(sl, position().add(0, 0.4, 0), 1.2, ACCENT, 5);
            }
            case MeliodasTable.IMAGES -> {
                if (f) {
                    lockMarks(id, imageSpots(target));
                    hint(id, "Lostvayne: Real Image. Each image will cut across the whole ring in turn, and while they stand he guards every side. Stand between the lines, away from the middle.");
                }
                if (marks.size() > 1) {
                    Vec3 centre = marks.get(marks.size() - 1);
                    for (int i = 0; i < marks.size() - 1; i++) {
                        Vec3 from = marks.get(i);
                        Vec3 dir = SdsaStrikes.unitFlat(centre.subtract(from), new Vec3(1, 0, 0));
                        SdsaStrikes.tellStrip(sl, from, dir, from.distanceTo(centre) * 2.0, IMAGE_HALF_WIDTH, RGB);
                        Tells.ring(sl, from, 1.2, ACCENT, 1.1f, 20);
                    }
                }
            }
            case MeliodasTable.RUSH -> {
                if (f) {
                    lockMarks(id, rushMarks(target, DemonKingMath.rushDashes(power())));
                    hint(id, "Assault Rush: Meliodas will dash through each marked spot in turn. Keep off the lines.");
                }
                Vec3 prev = position().add(0, 0.2, 0);
                for (Vec3 m : marks) {
                    Tells.line(sl, prev, m.add(0, 0.2, 0), RGB);
                    Tells.ring(sl, m, 1.8, WARN, 1.2f, 28);
                    prev = m.add(0, 0.2, 0);
                }
            }
            case MeliodasTable.PURGATORY -> {
                if (f) {
                    lockMarks(id, purgatoryMarks(target));
                    announce("Hellblaze: Purgatory! Columns of black flame will rise on every marked ring. Leave them, or guard.");
                    sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, net.minecraft.sounds.SoundSource.HOSTILE, 3f, 0.5f);
                }
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, PURGATORY_RADIUS, WARN, 1.4f, (int) Math.min(80, 14 + PURGATORY_RADIUS * 5));
                    Tells.ring(sl, m, Math.max(0.8, PURGATORY_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 28);
                    Tells.column(sl, m, 5, HELL);
                }
                Tells.gather(sl, chest, 2.0, ACCENT, 8);
            }
            case MeliodasTable.WRATH -> tellWrath(sl, target, f, left, total, chest);
            default -> { }
        }
    }

    private void tellWrath(ServerLevel sl, LivingEntity target, boolean f, int left, int total, Vec3 chest) {
        if (f) {
            novaPhase = 1;
            novaSerial++;
            novaFed = 0;
            novaPhysical = 0;
            announce("Demon King's Wrath! Do not feed it magic: it swallows spells and grows (up to 11 blocks). Break it with steel, arrows or anti-magic ("
                    + Math.round(DemonKingMath.NOVA_BREAK * 100) + "% of his health), or be outside the ring when it locks.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, net.minecraft.sounds.SoundSource.HOSTILE, 3f, 0.4f);
            SdsaStrikes.fx(sl, VfxShape.NIHILITY_ZONE, position(), position().add(0, 1, 0), HELL, total + DemonKingMath.NOVA_LOCK_TICKS, (float) DemonKingMath.NOVA_BASE_RADIUS, false);
        }
        double r = DemonKingMath.novaRadius(novaFed / getMaxHealth());
        Tells.ring(sl, position(), DemonKingMath.NOVA_BASE_RADIUS, WARN, 1.3f, 80);
        if (r > DemonKingMath.NOVA_BASE_RADIUS + 0.2) Tells.ring(sl, position(), r, HELL, 1.5f, 96);
        Tells.ring(sl, position(), Math.max(1.0, r * left / Math.max(1, total)), ACCENT, 1.1f, 40);
        Tells.gather(sl, chest, 2.2, ACCENT, 10);
        if (left % 12 == 0) {
            long broken = Math.round(100.0 * (novaPhysical / getMaxHealth()) / DemonKingMath.NOVA_BREAK);
            SdsaStrikes.bar(sl, position(), 40, Component.literal("Demon King's Wrath: ring " + String.format(java.util.Locale.ROOT, "%.1f", r) + " blocks, broken "
                    + Math.min(100, broken) + "%").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }

    private double[] waveAngles() {
        int n = Math.min(3, tuning().castSlots());
        return n <= 1 ? new double[]{0.0} : n == 2 ? new double[]{-0.14, 0.14} : new double[]{-0.26, 0.0, 0.26};
    }

    private double burstRadius() { return 5.0 + 0.3 * DemonKingMath.mark(power()); }

    /** Behind the target: opposite the way it looks, two blocks away (the spot he steps to). */
    private Vec3 behind(LivingEntity target) {
        Vec3 look = SdsaStrikes.unitFlat(target.getViewVector(1f), faceOf(target));
        Vec3 spot = ZogratisBoss.groundOrNull(level(), target.position().subtract(look.scale(2.2)));
        return spot == null ? ZogratisBoss.ground(level(), target.position()) : spot;
    }

    /** The images' spots on a ring round the target, and the centre as the last entry. */
    private List<Vec3> imageSpots(LivingEntity target) {
        int n = DemonKingMath.imageCount(power());
        Vec3 c = ZogratisBoss.ground(level(), target.position());
        List<Vec3> out = new ArrayList<>();
        for (double[] s : DemonKingMath.imageSpots(n, c.x, c.z, IMAGE_RING, getRandom().nextDouble() * Math.PI * 2))
            out.add(ZogratisBoss.ground(level(), new Vec3(s[0], c.y, s[1])));
        out.add(c);
        return out;
    }

    // ---------------------------------------------------------------- what each move does
    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case MeliodasTable.SLASH -> (b, t) -> {
                MeliodasMoves.cone(this, (ServerLevel) level(), takeAim(id, t), SLASH_REACH, SLASH_HALF, MeliodasTable.SLASH_DAMAGE, 0.5, VfxShape.SLASH_WAVE, 2.4f);
                quip("Heh!", "Too slow.");
            };
            case MeliodasTable.KAMICHIGIRI -> (b, t) -> {
                MeliodasMoves.cone(this, (ServerLevel) level(), takeAim(id, t), KAMI_REACH, KAMI_HALF, MeliodasTable.KAMICHIGIRI_DAMAGE, 1.0, VfxShape.SLASH_SCYTHE, 3.2f);
                quip("Cut.");
            };
            case MeliodasTable.LUNGE -> (b, t) -> {
                Vec3 a = takeAim(id, t);
                MeliodasMoves.dash(this, (ServerLevel) level(), a, lockedLength <= 0 ? 10 : lockedLength, LUNGE_HALF_WIDTH, MeliodasTable.LUNGE_DAMAGE);
                quip("Here I come!");
            };
            case MeliodasTable.WAVE -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                Vec3 a = takeAim(id, t), chest = position().add(0, getBbHeight() * 0.5, 0);
                for (double ang : waveAngles()) MeliodasMoves.crescent(this, sl, chest, SdsaStrikes.rotateY(a, ang), MeliodasTable.WAVE_DAMAGE);
            };
            case MeliodasTable.FULL_COUNTER -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                if (raiseCounter(sl, false, false, 0)) {
                    SdsaStrikes.bar(sl, position(), 48, Component.literal("Full Counter is up: magic from the front comes back. Use steel, arrows or anti-magic, or go behind him.")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                    quip("Go on. Throw your magic at me.");
                }
            };
            case MeliodasTable.HELLBLAZE -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                for (Vec3 m : takeMarks(id, t))
                    MeliodasMoves.field(this, sl, m, DemonKingMath.fieldRadius(power()), DemonKingMath.fieldTicks(power()), DemonKingMath.burnDamage(power()), DemonKingMath.lingerTicks(power()));
                quip("Burn.");
            };
            case MeliodasTable.DEMON_STEP -> (b, t) -> {
                MeliodasMoves.step(this, (ServerLevel) level(), takeMarks(id, t).get(0), STEP_RADIUS, MeliodasTable.STEP_DAMAGE);
                quip("Behind you.");
            };
            case MeliodasTable.BURST -> (b, t) -> MeliodasMoves.burst(this, (ServerLevel) level(), burstRadius(), MeliodasTable.BURST_DAMAGE);
            case MeliodasTable.IMAGES -> (b, t) -> {
                ServerLevel sl = (ServerLevel) level();
                List<Vec3> all = takeMarks(id, t);
                List<Vec3> spots = all.size() > 1 ? all.subList(0, all.size() - 1) : all;
                Vec3 centre = all.get(all.size() - 1);
                if (DemonKingMath.canCounter(power()) && SdsaConfig.fullCounter()) {
                    counter.drop(sl.getGameTime());
                    raiseCounter(sl, true, true, spots.size() * DemonKingMath.IMAGE_GAP + 30);
                }
                MeliodasMoves.images(this, sl, spots, centre, IMAGE_HALF_WIDTH, MeliodasTable.IMAGE_DAMAGE);
                quip("Which one is real?");
            };
            case MeliodasTable.RUSH -> (b, t) -> {
                MeliodasMoves.rush(this, (ServerLevel) level(), takeMarks(id, t), RUSH_HALF_WIDTH, MeliodasTable.RUSH_DAMAGE);
                quip("Try to keep up!");
            };
            case MeliodasTable.PURGATORY -> (b, t) -> {
                float burn = DemonKingMath.burnDamage(power());
                MeliodasMoves.purgatory(this, (ServerLevel) level(), takeMarks(id, t), PURGATORY_RADIUS, MeliodasTable.PURGATORY_DAMAGE, burn, DemonKingMath.lingerTicks(power()));
                say("Hellblaze: Purgatory!");
            };
            case MeliodasTable.WRATH -> (b, t) -> lockNova((ServerLevel) level());
            default -> (b, t) -> { };
        };
    }

    /** The stance of Full Counter: false when it is up already or still cooling down. */
    private boolean raiseCounter(ServerLevel sl, boolean omni, boolean force, int ticks) {
        int p = power();
        float breakAt = (float) (getMaxHealth() * DemonKingMath.breakFraction(p));
        return counter.raise(sl.getGameTime(), ticks > 0 ? ticks : DemonKingMath.stanceTicks(p), DemonKingMath.stanceCooldown(p), breakAt, omni, force);
    }

    /** The channel is over: the ring stands still and red, and goes off in {@link DemonKingMath#NOVA_LOCK_TICKS} ticks unless it is broken first. */
    private void lockNova(ServerLevel sl) {
        if (novaPhase != 1) return;                                     // it was broken (or cancelled) during the channel
        novaPhase = 2;
        final int serial = novaSerial;
        final Vec3 centre = position();
        final double radius = DemonKingMath.novaRadius(novaFed / getMaxHealth());
        final float damage = DemonKingMath.novaDamage(novaFed / getMaxHealth());
        novaRadius = radius;
        announce("The ring is locked at " + String.format(java.util.Locale.ROOT, "%.1f", radius) + " blocks. Walk out of it, or break it now: Meliodas is open.");
        SpellRuntime.zone(sl, DemonKingMath.NOVA_LOCK_TICKS, 5, SdsaStrikes.safe("nova ring", age -> {
            if (serial != novaSerial || novaPhase != 2) return;
            Tells.ring(sl, centre, radius, WARN, 1.6f, (int) Math.min(96, 20 + radius * 6));
            Tells.ring(sl, centre, Math.max(0.5, radius * (DemonKingMath.NOVA_LOCK_TICKS - age) / DemonKingMath.NOVA_LOCK_TICKS), ACCENT, 1.2f, 40);
        }));
        later(DemonKingMath.NOVA_LOCK_TICKS, SdsaStrikes.safe("nova", () -> {
            if (serial != novaSerial || novaPhase != 2) return;
            novaPhase = 0;
            novaSerial++;
            MeliodasMoves.nova(this, sl, centre, radius, damage);
            say("This is what Wrath looks like.");
        }));
    }
}
