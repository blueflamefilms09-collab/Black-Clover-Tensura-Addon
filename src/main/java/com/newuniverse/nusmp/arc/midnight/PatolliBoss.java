package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Patolli is the owner's reference fight for light speed against foresight ("Julius Novachrono against Patolli"): an elf who leads the Eye of the Midnight
 * Sun behind the face of Licht and fights with Light Magic, whose defining trait in the story is that it is faster than the eye. The fight therefore makes
 * him fast and the party's answer READING: every strike is drawn before it lands, the lanes freeze a few ticks before the blow so a sidestep is the answer, and
 * what the party cannot outrun it can predict. Three mechanics belong only to him.
 * <ul>
 *   <li><b>Overreach</b> ({@link Overreach}): his close-in light-speed strikes (Light Step, the whip, the Dance of Cuts) either land or find only air; three
 *       that find air in a row leave him open for a moment. Julius did not outrun him, he stood where the strike was not.</li>
 *   <li><b>The Light Shaft can be turned back</b> ({@link ReflectRules}): a Full Reflection mirror or a live mirror array (Gauche's Mirror Magic in the story)
 *       sends the beam back and he takes a share of his health and is left open; anti-magic and a raised shield cut it.</li>
 *   <li><b>Heaven's Verdict can be broken</b> ({@link BreakWatch}): he hangs a sword over every foe (as he hung them over the citizens of the capital); the
 *       party breaks the incantation by taking a share of his health during the three seconds of the wind-up, or takes cover, a shield or anti-magic.</li>
 * </ul>
 * At the Gates (100%) he is the Dark Elf: the light turns black (Demon Light Magic: the dark effects of the mod's Demon Light layer replace the white ones),
 * and one move is added, the Dance of Cuts. Nothing here changes a block, heals him, or holds a foe for more than the balance law allows.
 */
public class PatolliBoss extends MidnightBoss {
    private static final int GOLD = 0xFFE8A0, WHITE = 0xFFFFFF, DARK = 0xC060FF, DARK_ACCENT = 0xFF8AE0;

    public static final ArcCharacter CHARACTER = ArcCharacter.boss("patolli", "Patolli")
            .title("Leader of the Eye of the Midnight Sun").source("Black Clover").saga(ArcSaga.ELF_REINCARNATION).tier(2)
            .colors(0xFFE3A0, 0xFFFFFF).magic("light").stats(1900, 14, 0.36, 5)
            .floors(Polarity.HOLY, 50, 70)
            .themes("radiant", "astral", "holy_church", "celestial_court", "sky_citadel")
            .after("fana", "rhya", "rades_spirito")
            .charter(0.25, "solar_beam", "solar_rain", "solar_flare", "solar_wave", "light_transform", "instant_move", "thought_acceleration",
                    "light_attack_resistance", "holy_attack_resistance")
            .lines("A thousand years I waited behind a borrowed face. Your kingdom owes my people a debt, and you stand in its place.",
                    "You wanted to see the elf under the man? Then see how dark the light becomes.",
                    "Light that is only borrowed... fades. Tell them I only wanted my people to wake.")
            .build();

    public static final DeferredHolder<EntityType<?>, EntityType<PatolliBoss>> TYPE = ArcEntities.boss("patolli", PatolliBoss::new, 0.55f, 2.03f,
            new ArcLook("patolli", 2.03f, 0.92f, 1.7f, 0.44f, 1.12f, 1.2f, true), () -> attributesOf(CHARACTER, 3.0));

    private final Overreach overreach = new Overreach(PatolliMath.OVERREACH_LIMIT);
    private final BreakWatch verdictWatch = new BreakWatch();
    private List<Vec3> orbs = List.of();
    private boolean shaftHinted;

    public PatolliBoss(EntityType<PatolliBoss> type, Level level) { super(type, level); }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ looks and numbers
    @Override protected String word() { return "Light"; }

    @Override protected String gatesWord() { return "Dark Elf"; }

    @Override protected String gatesAnnouncement() { return "Patolli's light turns black: the Dark Elf has come out from under the borrowed face! Demon Light Magic."; }

    @Override protected ChatFormatting chatColor() { return gatesOpen() ? ChatFormatting.DARK_PURPLE : ChatFormatting.YELLOW; }

    @Override public int styleColor() { return rgb(); }

    @Override public int styleAccent() { return accent(); }

    private int rgb() { return gatesOpen() ? DARK : GOLD; }

    private int accent() { return gatesOpen() ? DARK_ACCENT : WHITE; }

    /** A fighter of the middle distance who keeps coming back to it: he wants the blades to have room and the whip to reach. */
    @Override protected double preferredRange(double suggested) { return Math.max(4.0, Math.min(10.0, suggested)); }

    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.08f : 1.0f; }

    @Override protected String[] tensuraKeywords() { return new String[]{"solar", "light"}; }

    @Override protected int tensuraKitSize() { return 3; }

    // ================================================================ words
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "You can still see me. That will not be true for long.";
        if (now >= 60 && old < 60) return "Light does not wait for the eye. Neither will I.";
        if (now >= 40 && old < 40) return "Good, you can dodge. Let us see you do it twice.";
        return "I draw " + now + "% of my light.";
    }

    @Override
    protected void fightBegins() {
        overreach.reset();
        verdictWatch.disarm();
        orbs = List.of();
        shaftHinted = false;
        announce("Patolli moves at the speed of light. Every strike is drawn before it lands: step out of the lines. Three strikes that find nothing leave him open.");
    }

    @Override
    protected void fightEnds(boolean won) {
        overreach.reset();
        verdictWatch.disarm();
        orbs = List.of();
    }

    @Override
    protected void onGatesOpened() {
        announce("Demon Light Magic: the swords are black now, and he has a new dance.");
        if (level() instanceof ServerLevel sl) {
            fx(VfxShape.DEMON_LIGHT_FX2, position(), position().add(0, 1, 0), DARK, 70, 7f);
            MidnightFx.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5f, 0.4f);
        }
    }

    // ================================================================ every tick
    @Override
    protected void tickMidnight() {
        if (!(level() instanceof ServerLevel sl)) return;
        Move m = castingMoveOrNull();
        boolean verdict = m != null && PatolliTable.VERDICT.equals(m.id());
        if (verdictWatch.armed()) {
            if (!verdict) {
                verdictWatch.disarm();
            } else {
                double need = MidnightConfig.verdictBreak();
                if (tickCount % 4 == 0) {
                    float p = verdictWatch.progress(getHealth(), getMaxHealth(), need);
                    MidnightFx.bar(sl, position(), 56, Component.literal("Heaven's Verdict " + MidnightText.gauge(p, 12) + "  break it!").withStyle(ChatFormatting.YELLOW));
                }
                if (verdictWatch.broken(getHealth(), getMaxHealth(), need)) breakVerdict();
            }
        }
        if (tickCount % 8 == 0 && isAlive()) Tells.gather(sl, position().add(0, 1.0, 0), 0.9, rgb(), 2);          // a body of light sheds some
    }

    private void breakVerdict() {
        verdictWatch.disarm();
        fx(VfxShape.MIRROR_SHATTER, hand(), hand(), GOLD, 24, 2.0f);
        MidnightFx.sound(level(), position(), SoundEvents.GLASS_BREAK, 2f, 0.7f);
        openUp(PatolliMath.VERDICT_BROKEN_OPEN_TICKS, "The incantation breaks! The swords over you scatter and Patolli reels. He is open!");
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MidnightSpec s : PatolliTable.specs()) out.add(build(s, tellOf(s.id()), runOf(s.id())));
        return out;
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case PatolliTable.LIGHT_SWORD -> (b, t, left, total) -> tellSwords(t, left);
            case PatolliTable.JUDGMENT_WHIP -> (b, t, left, total) -> tellWhip(t, left);
            case PatolliTable.LIGHT_STEP -> (b, t, left, total) -> tellStep(t, left, total);
            case PatolliTable.RADIANT_BURST -> (b, t, left, total) -> tellBurst(left, total);
            case PatolliTable.BLINDING_FLASH -> (b, t, left, total) -> tellFlash(left, total);
            case PatolliTable.SOLAR_MAGIC -> (b, t, left, total) -> tellGather();
            case PatolliTable.HEAVEN_SPLITTING -> (b, t, left, total) -> tellOrbs(t, left, total);
            case PatolliTable.LIGHT_SHAFT -> (b, t, left, total) -> tellShaft(t, left);
            case PatolliTable.ARROWS -> (b, t, left, total) -> tellArrows(left, total);
            case PatolliTable.LAMP -> (b, t, left, total) -> tellLamp(left, total);
            case PatolliTable.VERDICT -> (b, t, left, total) -> tellVerdict(left, total);
            case PatolliTable.DEMON_DANCE -> (b, t, left, total) -> tellDance(t, left, total);
            default -> null;
        };
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case PatolliTable.LIGHT_SWORD -> (b, t) -> volley(t, PatolliMath.bladeCount(power()));
            case PatolliTable.JUDGMENT_WHIP -> (b, t) -> runWhip(t);
            case PatolliTable.LIGHT_STEP -> (b, t) -> runStep(t);
            case PatolliTable.RADIANT_BURST -> (b, t) -> runBurst();
            case PatolliTable.BLINDING_FLASH -> (b, t) -> runFlash();
            case PatolliTable.SOLAR_MAGIC -> (b, t) -> {
                if (!castTensura(t)) {
                    aim = Vec3.ZERO;
                    volley(t, 2);
                }
            };
            case PatolliTable.HEAVEN_SPLITTING -> (b, t) -> runOrbs(t);
            case PatolliTable.LIGHT_SHAFT -> (b, t) -> runShaft(t);
            case PatolliTable.ARROWS -> (b, t) -> runArrows(t);
            case PatolliTable.LAMP -> (b, t) -> runLamp();
            case PatolliTable.VERDICT -> (b, t) -> runVerdict();
            case PatolliTable.DEMON_DANCE -> (b, t) -> runDance(t);
            default -> (b, t) -> { };
        };
    }

    // ---------------------------------------------------------------- drawn effects, white by day and black at the Gates
    private void swordFx(Vec3 from, Vec3 to, float size) {
        if (gatesOpen()) fx(VfxShape.DEMON_LIGHT_FX1, from, to, DARK, 12, size);
        else fx(VfxShape.LIGHT_BLADE, from, to, GOLD, 10, size);
    }

    private void flareFx(Vec3 at, float size) {
        if (gatesOpen()) fx(VfxShape.DEMON_LIGHT_FX3, at, at.add(0, 1, 0), DARK, 24, size);
        else fx(VfxShape.LIGHT_FLARE, at, at, GOLD, 14, size);
    }

    // ---------------------------------------------------------------- Light Sword of Judgment
    private void tellSwords(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = hand();
        if (left > PatolliMath.VOLLEY_LOCK || aim.lengthSqr() < 1e-4) aim = t.getBoundingBox().getCenter().subtract(from);          // follows until the lock
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 dir = aim.normalize();
        int n = PatolliMath.bladeCount(power());
        int color = left <= PatolliMath.VOLLEY_LOCK ? MidnightFx.WARN : rgb();
        for (double a : MidnightMath.fan(n, PatolliMath.bladeSpread(n))) {
            Vec3 d = MidnightFx.rotateY(dir, a);
            Tells.line(sl, from, MidnightFx.clipWall(sl, this, from, from.add(d.scale(32))), color);
        }
        Tells.gather(sl, from, 0.6, accent(), 4);
    }

    private void volley(LivingEntity t, int n) {
        if (!(level() instanceof ServerLevel sl)) return;
        final Vec3 from = hand();
        Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        final VolleyBudget budget = new VolleyBudget(PatolliMath.BLADE_HITS_PER_FOE);
        double[] fan = MidnightMath.fan(n, PatolliMath.bladeSpread(n));
        for (int i = 0; i < n; i++) {
            final Vec3 d = MidnightFx.rotateY(dir, fan[i]);
            Runnable fire = MidnightFx.safe("light sword", () -> {
                swordFx(from, MidnightFx.clipWall(sl, this, from, from.add(d.scale(48))), 1.0f);
                MidnightBolt.launch(this, from, d.scale(PatolliMath.BLADE_SPEED), PatolliMath.BLADE_LIFE, PatolliMath.BLADE_RADIUS, rgb(), false, budget,
                        (bolt, v) -> {
                            if (MidnightFx.hit(this, v, PatolliTable.SWORD, true)) flareFx(v.position().add(0, 1, 0), 0.8f);
                        }, null, null);
            });
            if (i == 0) fire.run();
            else later(i, fire);
        }
        MidnightFx.sound(sl, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.9f);
        quip("Too slow.", "Did you see that one?", "Light does not ask permission.");
    }

    // ---------------------------------------------------------------- Bright Judgment Whip
    private Vec3 flatAimAt(LivingEntity t) { return MidnightFx.flatDir(position(), t.position(), getViewVector(1f)); }

    private void tellWhip(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (left > 8 || aim.lengthSqr() < 1e-4) aim = flatAimAt(t);
        MidnightFx.coneTell(sl, position().add(0, 0.2, 0), aim, PatolliMath.WHIP_REACH, PatolliMath.WHIP_HALF_ANGLE, left <= 8 ? MidnightFx.WARN : rgb());
    }

    private void runWhip(LivingEntity t) {
        final Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.multiply(1, 0, 1).normalize() : flatAimAt(t);
        final boolean first = lash(dir);
        double turn = (getRandom().nextBoolean() ? 1 : -1) * (0.9 + getRandom().nextDouble() * 0.4);
        final Vec3 second = MidnightFx.rotateY(dir, turn);
        if (level() instanceof ServerLevel sl) {                                  // the second lash is shown the moment the first lands, and again before it comes
            MidnightFx.coneTell(sl, position().add(0, 0.2, 0), second, PatolliMath.WHIP_REACH, PatolliMath.WHIP_HALF_ANGLE, MidnightFx.WARN);
            later(PatolliMath.WHIP_GAP / 2, MidnightFx.safe("whip tell", () -> MidnightFx.coneTell(sl, position().add(0, 0.2, 0), second, PatolliMath.WHIP_REACH,
                    PatolliMath.WHIP_HALF_ANGLE, MidnightFx.WARN)));
        }
        later(PatolliMath.WHIP_GAP, MidnightFx.safe("second lash", () -> noteStrike(first | lash(second))));
    }

    /** One lash of the whip: a cone of light in front of him. Returns whether it struck someone. */
    private boolean lash(Vec3 dir) {
        Vec3 tip = position();
        Vec3 chest = tip.add(0, 1.2, 0);
        swordFx(chest, chest.add(dir.scale(PatolliMath.WHIP_REACH)), 1.5f);
        MidnightFx.sound(level(), tip, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f, 1.6f);
        boolean landed = false;
        for (LivingEntity e : MidnightFx.inCone(this, tip, dir, PatolliMath.WHIP_REACH, PatolliMath.WHIP_HALF_ANGLE)) {
            if (MidnightFx.hit(this, e, PatolliTable.WHIP, true)) {
                landed = true;
                MidnightFx.shove(e, tip, 0.9, 0.1);
            }
        }
        return landed;
    }

    // ---------------------------------------------------------------- Light Step
    private void tellStep(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 face = t.getViewVector(1f).multiply(1, 0, 1);
            double[] b = MidnightMath.behind(t.getX(), t.getZ(), face.x, face.z, PatolliMath.STEP_BEHIND);
            Vec3 spot = ZogratisBoss.groundOrNull(level(), new Vec3(b[0], t.getY(), b[1]));
            marks = List.of(spot != null ? spot : ground(t.position()));
        }
        if (marks.isEmpty()) return;
        Vec3 end = marks.get(0);
        Tells.line(sl, position().add(0, 1, 0), end.add(0, 1, 0), rgb());
        Tells.ring(sl, end, PatolliMath.STEP_STRIKE_RADIUS, MidnightFx.WARN, 1.3f, 40);
        Tells.column(sl, end, 3, accent());
    }

    private void runStep(LivingEntity t) {
        Vec3 from = position();
        Vec3 end = marks.isEmpty() ? ground(t.position()) : marks.get(0);
        marks = List.of();
        boolean landed = false;
        for (LivingEntity e : MidnightFx.inLane(this, from, end, PatolliMath.STEP_HALF_WIDTH)) {            // the lane he crosses
            if (MidnightFx.hit(this, e, PatolliTable.STEP_LANE, true)) {
                landed = true;
                MidnightFx.shoveAside(e, from, end, 1.0);
            }
        }
        swordFx(from.add(0, 1, 0), end.add(0, 1, 0), 1.7f);
        Vec3 at = blink(end) ? end : position();
        for (LivingEntity e : MidnightFx.inRing(this, at, PatolliMath.STEP_STRIKE_RADIUS)) {                // and the place he arrives
            if (MidnightFx.hit(this, e, PatolliTable.STEP_STRIKE, true)) {
                landed = true;
                MidnightFx.shove(e, at, 0.8, 0.15);
            }
        }
        flareFx(at.add(0, 1, 0), 1.4f);
        MidnightFx.sound(level(), at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 2f);
        noteStrike(landed);
        quip("Behind you.", "You looked the wrong way.", "Too late.");
    }

    // ---------------------------------------------------------------- Radiant Burst
    private void tellBurst(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.ring(sl, position(), PatolliMath.BURST_RADIUS, rgb(), 1.3f, 48);
        Tells.ring(sl, position(), Math.max(1.0, PatolliMath.BURST_RADIUS * (total - left) / Math.max(1, total)), accent(), 1.1f, 28);
        Tells.gather(sl, position().add(0, 1, 0), 0.9, accent(), 5);
    }

    private void runBurst() {
        for (LivingEntity e : MidnightFx.inRing(this, position(), PatolliMath.BURST_RADIUS)) {
            if (MidnightFx.hit(this, e, PatolliTable.BURST, true)) MidnightFx.shove(e, position(), 1.4, 0.3);
        }
        flareFx(position().add(0, 1, 0), 3f);
        if (level() instanceof ServerLevel sl) ZogratisVfx.divineSigil(sl, position(), PatolliMath.BURST_RADIUS, 12, rgb(), false);
        MidnightFx.sound(level(), position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.5f);
    }

    // ---------------------------------------------------------------- Blinding Flash
    private void tellFlash(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (left >= total - 3) {
            MidnightFx.bar(sl, position(), PatolliMath.FLASH_RANGE + 6, Component.literal("Patolli gathers a blinding light: LOOK AWAY or raise a shield!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        }
        Tells.ring(sl, position(), Math.min(PatolliMath.FLASH_RANGE, 3 + (total - left) * 0.8), WHITE, 1.2f, 56);
        Tells.gather(sl, hand(), 0.5, WHITE, 8);
    }

    private void runFlash() {
        if (!(level() instanceof ServerLevel sl)) return;
        fx(VfxShape.LIGHT_FLARE, hand(), hand(), WHITE, 18, 4.5f);
        MidnightFx.sound(sl, position(), SoundEvents.BEACON_ACTIVATE, 2.5f, 2f);
        boolean gate = MidnightFx.controlAllowed(this);
        for (LivingEntity e : foes(PatolliMath.FLASH_RANGE)) {
            if (!hasLineOfSight(e)) continue;
            Vec3 view = e.getViewVector(1f), to = position().subtract(e.position());
            boolean looking = MidnightMath.looksToward(view.x, view.z, to.x, to.z, PatolliMath.FLASH_VIEW);
            if (looking && !e.isBlocking()) {
                MidnightFx.hit(this, e, PatolliTable.FLASH, true);
                if (gate) e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(e, PatolliMath.FLASH_BLIND_TICKS), 0));
            } else if (e instanceof ServerPlayer p) {
                p.displayClientMessage(Component.literal(looking ? "Your shield took the flash." : "You looked away from the flash."), true);
            }
        }
    }

    private void tellGather() {
        if (level() instanceof ServerLevel sl) {
            Tells.gather(sl, hand(), 0.8, accent(), 6);
            Tells.ring(sl, position(), 2.2, rgb());
        }
    }

    // ---------------------------------------------------------------- Heaven-Splitting Flash
    private void tellOrbs(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 centre = ground(t.position());
            int n = PatolliMath.orbCount(power());
            double[][] ring = MidnightMath.ring(n, PatolliMath.ORB_RING, getRandom().nextDouble() * Math.PI * 2);
            List<Vec3> list = new ArrayList<>();
            for (double[] p : ring) list.add(centre.add(p[0], PatolliMath.ORB_HEIGHT, p[1]));
            orbs = list;
        }
        for (Vec3 o : orbs) {
            Tells.gather(sl, o, 0.35, accent(), 3);
            Tells.column(sl, o.subtract(0, PatolliMath.ORB_HEIGHT, 0), PatolliMath.ORB_HEIGHT, rgb());
        }
    }

    private void runOrbs(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        final List<Vec3> spots = orbs.isEmpty() ? List.of(hand().add(0, 2, 0)) : List.copyOf(orbs);
        orbs = List.of();
        for (Vec3 o : spots) fx(VfxShape.LIGHT_FLARE, o, o, rgb(), 50, 1.3f);
        MidnightFx.sound(sl, position(), SoundEvents.BEACON_POWER_SELECT, 2f, 1.7f);
        for (int i = 0; i < spots.size(); i++) {
            final Vec3 o = spots.get(i);
            final Vec3[] end = new Vec3[1];
            later(i * PatolliMath.ORB_GAP, MidnightFx.safe("orb aim", () -> {                // the beam's line is drawn first, aimed where the foe is now
                LivingEntity tg = t.isAlive() ? t : otherFoe(null, 40);
                if (tg == null) return;
                Vec3 dir = tg.getBoundingBox().getCenter().subtract(o).normalize();
                end[0] = MidnightFx.clipWall(sl, this, o, o.add(dir.scale(40)));
                Tells.line(sl, o, end[0], MidnightFx.WARN);
            }));
            later(i * PatolliMath.ORB_GAP + PatolliMath.ORB_WARN, MidnightFx.safe("orb beam", () -> {
                if (end[0] == null) return;
                ZogratisVfx.ray(sl, o, end[0], 1.2, 8, rgb(), false);
                for (LivingEntity e : MidnightFx.inLane(this, o, end[0], PatolliMath.ORB_BEAM_HALF_WIDTH)) MidnightFx.hit(this, e, PatolliTable.ORB, true);
                MidnightFx.sound(sl, o, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 2f);
            }));
        }
    }

    // ---------------------------------------------------------------- Light Shaft of Divine Punishment
    private void tellShaft(LivingEntity t, int left) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = hand();
        if (left > PatolliMath.SHAFT_LOCK || aim.lengthSqr() < 1e-4) aim = t.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(aim.normalize().scale(PatolliMath.SHAFT_LENGTH)));
        boolean locked = left <= PatolliMath.SHAFT_LOCK;
        ZogratisVfx.ray(sl, from, end, locked ? 1.0 : 0.5, 8, locked ? 0xFF4A4A : rgb(), true);
        Tells.line(sl, from, end, locked ? MidnightFx.WARN : rgb());
        if (locked) Tells.ring(sl, position(), 1.8, MidnightFx.WARN, 1.2f, 24);
        if (!shaftHinted && left >= 20) {
            shaftHinted = true;
            announce("Patolli aims the Light Shaft of Divine Punishment. A mirror, a Full Reflection or a raised shield answers it.");
        }
    }

    private void runShaft(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = hand();
        Vec3 dir = aim.lengthSqr() > 1e-4 ? aim.normalize() : t.getBoundingBox().getCenter().subtract(from).normalize();
        Vec3 end = MidnightFx.clipWall(sl, this, from, from.add(dir.scale(PatolliMath.SHAFT_LENGTH)));
        Vec3 stop = end;
        ServerPlayer turnedBy = null;
        ReflectRules.Kind turned = ReflectRules.Kind.NONE;
        for (LivingEntity e : MidnightFx.inLane(this, from, end, PatolliMath.SHAFT_HALF_WIDTH)) {             // the nearest first
            ReflectRules.Kind k = MidnightFx.reflectKind(e, false);
            if (k != ReflectRules.Kind.NONE && e instanceof ServerPlayer p) {
                turned = k;
                turnedBy = p;
                stop = e.getBoundingBox().getCenter();
                break;
            }
            MidnightFx.hit(this, e, PatolliTable.SHAFT, true);
        }
        ZogratisVfx.ray(sl, from, stop, 2.4, 14, rgb(), false);
        MidnightFx.sound(sl, from, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9f, 2f);
        if (turnedBy != null) {
            MidnightFx.markTurned(turnedBy);
            ZogratisVfx.ray(sl, stop, hand(), 2.0, 12, 0xE8DAFF, false);                      // the beam comes back
            float amount = ReflectRules.backfireShare(turned) * getMaxHealth();
            hurt(damageSources().indirectMagic(turnedBy, turnedBy), amount);
            turnedBy.displayClientMessage(Component.literal("You turn the light back on Patolli!"), true);
            openUp(ReflectRules.openTicks(turned), "His own light strikes him down, as it did in the story. Patolli is open!");
        }
    }

    // ---------------------------------------------------------------- Arrows of Judgment
    private void tellArrows(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double r = 4 + (total - left) * 0.25;
        Tells.ring(sl, position().add(0, 3.5, 0), Math.min(r, 12), rgb(), 1.2f, 40);
        Tells.gather(sl, position().add(0, 3.5, 0), 1.6, accent(), 6);
    }

    private void runArrows(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        int salvos = PatolliMath.salvoCount(power());
        for (int k = 0; k < salvos; k++) {
            later(k * PatolliMath.ARROW_GAP, MidnightFx.safe("arrows", () -> {
                List<Vec3> spots = spotsAround(t.isAlive() ? t : otherFoe(null, 40), Math.max(1, Math.min(6, foes(30).size())), 2.0);
                markedStrike(spots, PatolliMath.ARROW_RADIUS, PatolliMath.ARROW_WARN, rgb(), c -> {
                    fx(VfxShape.LIGHT_BLADE, c.add(0, 14, 0), c, rgb(), 8, 1.2f);
                    flareFx(c.add(0, 0.3, 0), 1.0f);
                    for (LivingEntity e : MidnightFx.inRing(this, c, PatolliMath.ARROW_RADIUS)) MidnightFx.hit(this, e, PatolliTable.ARROW, true);
                });
            }));
        }
        MidnightFx.sound(sl, position(), SoundEvents.BEACON_ACTIVATE, 2f, 1.6f);
    }

    // ---------------------------------------------------------------- Lamp of Avior Gloria
    private void tellLamp(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 lamp = position().add(0, 4.5, 0);
        Tells.gather(sl, lamp, 0.8, accent(), 6);
        Tells.line(sl, position().add(0, 2, 0), lamp, rgb());
        Tells.ring(sl, position(), 3 + (total - left) * 0.3, rgb(), 1.2f, 36);
    }

    private void runLamp() {
        if (!(level() instanceof ServerLevel sl)) return;
        final Vec3 lamp = position().add(0, 4.5, 0);
        fx(VfxShape.LIGHT_FLARE, lamp, lamp, rgb(), PatolliMath.LAMP_TICKS, 2.4f);
        MidnightFx.sound(sl, position(), SoundEvents.BEACON_ACTIVATE, 2.2f, 1.4f);
        final VolleyBudget budget = new VolleyBudget(PatolliMath.LAMP_HITS_PER_FOE);
        SpellRuntime.zone(sl, PatolliMath.LAMP_TICKS, PatolliMath.LAMP_EVERY, MidnightFx.safe("lamp", age -> {
            if (!isAlive()) return;
            List<LivingEntity> foes = foes(PatolliMath.LAMP_RANGE);
            if (foes.isEmpty()) return;
            for (int k = 0; k < Math.min(PatolliMath.lampBlades(power()), foes.size()); k++) {
                final LivingEntity pick = foes.get(getRandom().nextInt(foes.size()));
                final Vec3 dir = pick.getBoundingBox().getCenter().subtract(lamp).normalize();
                Tells.line(sl, lamp, MidnightFx.clipWall(sl, this, lamp, lamp.add(dir.scale(30))), MidnightFx.WARN);       // the line is drawn, then the sword follows it
                later(PatolliMath.LAMP_WARN, MidnightFx.safe("lamp sword", () -> {
                    swordFx(lamp, MidnightFx.clipWall(sl, this, lamp, lamp.add(dir.scale(40))), 0.9f);
                    MidnightBolt.launch(this, lamp, dir.scale(3.5), 14, 0.6, rgb(), false, budget,
                            (bolt, v) -> {
                                if (MidnightFx.hit(this, v, PatolliTable.LAMP_BLADE, true)) flareFx(v.position().add(0, 1, 0), 0.7f);
                            }, null, null);
                }));
            }
        }));
    }

    // ---------------------------------------------------------------- Heaven's Verdict (breakable)
    private void tellVerdict(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        List<LivingEntity> foes = foes(PatolliMath.VERDICT_RANGE);
        if (first(left, total)) {
            verdictWatch.arm(getHealth());
            announce("Patolli raises his hand: HEAVEN'S VERDICT. A sword of light hangs over every one of you. Take " + Math.round(MidnightConfig.verdictBreak() * 100)
                    + "% of his health before it falls to break it, or find a roof, a shield or anti-magic.");
            for (LivingEntity e : foes.subList(0, Math.min(8, foes.size()))) {
                Vec3 above = e.position().add(0, PatolliMath.VERDICT_HANG, 0);
                fx(VfxShape.LIGHT_BLADE, above.add(0, 2, 0), above, WHITE, total + 6, 1.6f);
            }
        }
        for (LivingEntity e : foes.subList(0, Math.min(8, foes.size()))) {
            Tells.ring(sl, ground(e.position()), PatolliMath.VERDICT_MARK, rgb(), 1.3f, 24);
            Tells.column(sl, e.position(), PatolliMath.VERDICT_HANG, accent());
        }
        Tells.gather(sl, hand().add(0, 1, 0), 1.0, accent(), 6);
    }

    private void runVerdict() {
        if (!(level() instanceof ServerLevel sl)) return;
        verdictWatch.disarm();
        List<LivingEntity> foes = foes(PatolliMath.VERDICT_RANGE);
        for (LivingEntity e : foes.subList(0, Math.min(10, foes.size()))) {
            boolean covered = MidnightFx.covered(sl, e, PatolliMath.COVER_HEIGHT);
            float factor = PatolliMath.verdictFactor(covered, e.isBlocking(), MidnightFx.antiMagic(e));
            Vec3 above = e.position().add(0, PatolliMath.VERDICT_HANG + 4, 0);
            swordFx(above, e.position().add(0, 0.5, 0), 1.8f);
            flareFx(e.position().add(0, 0.4, 0), 1.6f);
            if (MidnightFx.hitRaw(this, e, PatolliTable.VERDICT_HIT * factor, true) && covered && e instanceof ServerPlayer p) {
                p.displayClientMessage(Component.literal("A roof near you stopped most of the swords."), true);
            }
        }
        MidnightFx.sound(sl, position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 1.6f);
        MidnightFx.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 3f, 0.5f);
    }

    // ---------------------------------------------------------------- Demon Light: Dance of Cuts (the Gates)
    private void tellDance(LivingEntity t, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            Vec3 centre = ground(t.position());
            int n = PatolliMath.danceCuts(power());
            List<Vec3> list = new ArrayList<>();
            for (double[] p : MidnightMath.ring(n, PatolliMath.DANCE_RING, getRandom().nextDouble() * Math.PI * 2)) list.add(ground(centre.add(p[0], 0, p[1])));
            marks = list;
        }
        Vec3 prev = position().add(0, 1, 0);
        for (Vec3 m : marks) {
            Tells.ring(sl, m, PatolliMath.DANCE_CUT_RADIUS, rgb(), 1.2f, 20);
            Tells.line(sl, prev, m.add(0, 1, 0), accent());
            prev = m.add(0, 1, 0);
        }
    }

    private void runDance(LivingEntity t) {
        final List<Vec3> cuts = marks.isEmpty() ? List.of(ground(t.position())) : List.copyOf(marks);
        marks = List.of();
        final boolean[] landed = {false};
        for (int i = 0; i < cuts.size(); i++) {
            final Vec3 at = cuts.get(i);
            final boolean last = i == cuts.size() - 1;
            later(i * PatolliMath.DANCE_GAP, MidnightFx.safe("dance cut", () -> {
                Vec3 from = position().add(0, 1, 0);
                blink(at);
                swordFx(from, at.add(0, 1, 0), 1.4f);
                flareFx(at.add(0, 1, 0), 1.0f);
                for (LivingEntity e : MidnightFx.inRing(this, at, PatolliMath.DANCE_CUT_RADIUS)) {
                    if (MidnightFx.hit(this, e, PatolliTable.DANCE_CUT, true)) {
                        landed[0] = true;
                        MidnightFx.shove(e, at, 0.6, 0.1);
                    }
                }
                MidnightFx.sound(level(), at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 1.8f);
                if (last) noteStrike(landed[0]);
            }));
        }
    }

    // ---------------------------------------------------------------- overreach
    /** A close-in strike resolved: it landed on someone or found only air. Three empty ones in a row open him. */
    private void noteStrike(boolean landed) {
        if (overreach.record(landed)) {
            fx(VfxShape.LIGHT_FLARE, position().add(0, 1, 0), position().add(0, 1, 0), WHITE, 16, 2.0f);
            openUp(PatolliMath.OVERREACH_OPEN_TICKS, "Patolli has struck at air three times and overreached. He is open!");
        }
    }
}
