package com.newuniverse.nusmp.entity.avatar;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.dungeon.DungeonMaw;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Devil Avatar, the floor boss of the Underworld Gate: a high devil of the Spade Kingdom's Underworld that bends the gravity of the room
 * and throws fast curses. One entity, two variants (synched): the <b>Avatar of Lucifero</b> (violet, leans on gravity) and the <b>Avatar of
 * Megicula</b> (crimson, leans on curses); both have the same moves, only their weights differ ({@link AvatarTable}). It is a
 * {@link ZogratisBoss}, so the Percentage Power System, the Gates of the Tree of Qliphoth (at 25% health it grows, 1.15 times, and brightens),
 * the Dark Disciples of its devil, the boss bar, the arena and the leash all come from the framework.
 * <ul>
 *   <li><b>Gravity</b> ({@link AvatarMoves}): Ceiling Pull lifts everyone in 14 blocks to the roof, where hex bolts rain on them; Floor Crush
 *       marks the floor under the foes and slams whoever is on the marks, the lifted ones hardest; Inversion Field is a ring that pulls up
 *       and presses down in turn for 8 s. Velocity and effects only, never a block.</li>
 *   <li><b>Curses</b>: Hex Volley (5 to 9 homing bolts, each a stack of Megicula's Curse of Decay), Dread Brand (the foe farthest from it is
 *       branded: leave the sigil), Curse Nova (a wave to jump), Shadow Step (a visible blink behind the target, then a strike).</li>
 *   <li><b>Void Claws</b> and <b>Call the Disciples</b> round out the list; <b>Abyssal Collapse</b> (60% power) chains a wider pull, the crush
 *       on marks drawn under the lifted foes, and a curse wave, and leaves the Avatar exposed for 3 s.</li>
 * </ul>
 * It carries the scoreboard tag {@code nusmp_floor_boss}. When it falls it posts the {@link DungeonCollapseEvent} of the Underworld Gate once
 * (the Treasury Collapse begins), tells the server in its own words and gives experience only: the Magic Stones come from the sentient
 * Dungeon's own death handler. The Triad's bond ({@code TriadBond}) counts every Lucifero or Megicula boss as a sibling, so
 * {@link #enrage} is ignored here: a fallen Zogratis does not make an Avatar rage.
 */
public class DevilAvatarBoss extends ZogratisBoss {
    /** The scoreboard tag of the floor bosses of the Sentient Dungeon (the stone drops treat a mob that carries it as a boss). */
    public static final String FLOOR_BOSS_TAG = "nusmp_floor_boss";

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(DevilAvatarBoss.class, EntityDataSerializers.INT);

    // the marks the telegraphs lock (package-private: AvatarMoves writes them in the wind-up and the run code reads them)
    List<Vec3> marks = List.of();
    Vec3 stepExit, brandMark;
    int brandTarget = -1;
    /** The angle of the first spoke the Brimstone Eruption's wind-up drew (NaN when none). */
    double eruptBase = Double.NaN;

    private final Set<String> hinted = new HashSet<>();
    private long lastQuip;
    /** The middle of the Nexus this Avatar guards (the centre of its collapse), or null: then its arena. */
    private BlockPos centre;
    private boolean collapsePosted;

    public DevilAvatarBoss(EntityType<DevilAvatarBoss> type, Level level) {
        super(type, level);
        addTag(FLOOR_BOSS_TAG);
        if (!level.isClientSide) entityData.set(VARIANT, getRandom().nextBoolean() ? AvatarTable.MEGICULA : AvatarTable.LUCIFERO);
    }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(AvatarTable.HEALTH, AvatarTable.ATTACK, 0.34, AvatarTable.ARMOR, AvatarTable.TOUGHNESS); }

    // ================================================================ which Avatar
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VARIANT, AvatarTable.LUCIFERO);
    }

    /** {@link AvatarTable#LUCIFERO} or {@link AvatarTable#MEGICULA}. */
    public int variant() { return AvatarTable.variantOf(entityData.get(VARIANT)); }

    public void setVariant(int variant) { entityData.set(VARIANT, AvatarTable.variantOf(variant)); }

    public boolean megicula() { return variant() == AvatarTable.MEGICULA; }

    @Override public String bossName() { return AvatarTable.nameOf(variant()); }
    @Override public Devil devil() { return megicula() ? Devil.MEGICULA : Devil.LUCIFERO; }

    /** "Avatar of Lucifero - 60%": the devil is in the name already. */
    @Override public String barTitle() { return bossName() + " - " + power() + "%" + (gatesOpen() ? " - Gates open" : ""); }

    @Override protected Component getTypeName() { return Component.literal(bossName()); }

    /** The middle of the Nexus whose Gate this Avatar guards; its collapse is centred there. */
    public void setCentre(BlockPos p) { centre = p == null ? null : p.immutable(); }

    // ================================================================ tuning
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.15f : 1.0f; }
    @Override protected float damageScale() { return 1.0f; }
    /** The plain blow comes every half to full second, so it is lighter than the telegraphed Void Claws. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }
    /** A caster that bends the room: five to ten blocks from its foes, close enough to claw and step, far enough to pull and curse. */
    @Override protected double preferredRange(double suggested) { return Math.max(5, Math.min(10, suggested)); }

    /** Not a sibling of the Dark Triad (see the class comment): a fallen Zogratis does not enrage it. */
    @Override public void enrage(String fallenName) { }

    // ================================================================ lines
    @Override protected String openingLine() { return AvatarText.opening(variant()); }
    @Override protected String powerLine(int old, int now) { return AvatarText.power(variant(), old, now); }
    @Override protected String gatesLine() { return AvatarText.gates(variant()); }
    @Override protected String deathLine() { return AvatarText.death(variant()); }

    /** One of the move's lines now and then (at most every eight seconds), so the chat is not a wall of speech. */
    private void quip(String move) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        String[] lines = AvatarText.quips(variant(), move);
        say(lines[getRandom().nextInt(lines.length)]);
    }

    /** A warning for the chat, once per fight and key (the rings and lines on the ground repeat it every time). */
    void hint(String key, String text) {
        if (hinted.add(key)) announce(text);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        hinted.clear();
        clearMarks();
        announce(AvatarTable.nameOf(variant()) + " bends the gravity of the room: it pulls to the ceiling, presses to the floor and flips the pull of a field.");
        announce("A Mana Zone or anti-magic halves every push of it. Leave the rings in the wind-up, steer out of the marks, and jump its waves.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        clearMarks();
        if (won) postCollapse();
    }

    private void clearMarks() {
        marks = List.of();
        stepExit = null;
        brandMark = null;
        brandTarget = -1;
        eruptBase = Double.NaN;
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        int before = PowerCurve.stage(old), after = PowerCurve.stage(now);
        if (after > before && after < 4 && summonDisciples(after) > 0) announce("Dark Disciples of " + devil().displayName + " answer the Avatar's call.");
    }

    @Override
    protected void onGatesOpened() {
        AvatarVfx.gates(this);
        announce("The Avatar swells with " + devil().displayName + "'s power. Its pulls reach further, its bolts multiply and its Disciples come in waves.");
        for (int delay : AvatarTable.GATE_WAVES) {
            later(delay, AvatarFx.safe("disciple wave", () -> {
                if (fightActive() && summonDisciples(2) > 0) announce("A wave of Dark Disciples steps out of the Avatar's shadow.");
            }));
        }
    }

    /**
     * The Avatar fell: the Treasury Collapse of its Gate begins (the event is posted once, centred on the middle of the Nexus), after the
     * server is told, in the Dungeon only. It gives experience and nothing else.
     */
    private void postCollapse() {
        if (collapsePosted || !(level() instanceof ServerLevel sl)) return;
        collapsePosted = true;
        if (!LivingDungeon.in(sl)) return;                                      // an Avatar called up anywhere else just dies
        ServerPlayer killer = killer(sl);
        BlockPos at = centre != null ? centre : arena();
        try {
            DungeonMaw.broadcast(sl.getServer(), Component.literal(AvatarText.fell(bossName(), killer == null ? null : killer.getName().getString()))
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        } catch (RuntimeException | LinkageError ex) {
            LogUtils.getLogger().warn("[nusmp] The fall of the Devil Avatar could not be announced: {}", ex.toString());
        }
        try {
            NeoForge.EVENT_BUS.post(new DungeonCollapseEvent(sl, at, NexusZone.UNDERWORLD_GATE, killer, true));
        } catch (RuntimeException | LinkageError ex) {
            LogUtils.getLogger().warn("[nusmp] The collapse of the Underworld Gate could not be started: {}", ex.toString());
        }
    }

    /** Whoever dealt the killing blow, else a player who was fighting it, else nobody. */
    private ServerPlayer killer(ServerLevel sl) {
        if (getKillCredit() instanceof ServerPlayer sp) return sp;
        for (UUID id : fighters()) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null && p.isAlive()) return p;
        }
        return null;
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (AvatarTable.Spec s : AvatarTable.specs(variant())) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            Move.MoveTell tell = tellOf(s.id());
            if (tell != null) b.tell(tell);
            out.add(b.run(runOf(s.id())));
        }
        return out;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case AvatarTable.VOID_CLAWS -> (b, t) -> {
                AvatarMoves.voidClaws(this, AvatarTable.CLAWS);
                quip(id);
            };
            case AvatarTable.HEX_VOLLEY -> (b, t) -> {
                AvatarMoves.hexVolley(this, t);
                quip(id);
            };
            case AvatarTable.SHADOW_STEP -> (b, t) -> {
                Vec3 locked = stepExit;
                stepExit = null;
                AvatarMoves.shadowStep(this, t, locked);
                quip(id);
            };
            case AvatarTable.CEILING_PULL -> (b, t) -> {
                List<LivingEntity> lifted = AvatarMoves.ceilingPull(this, AvatarMath.PULL_RADIUS);
                if (!lifted.isEmpty()) for (int at : AvatarMath.RAIN_AT) later(at, AvatarFx.safe("hex rain", () -> AvatarMoves.rain(this, lifted)));
                quip(id);
            };
            case AvatarTable.FLOOR_CRUSH -> (b, t) -> {
                List<Vec3> locked = marks.isEmpty() ? AvatarMoves.crushMarks(this, t, AvatarMath.markCount(power())) : marks;
                marks = List.of();
                AvatarMoves.floorCrush(this, locked, AvatarTable.CRUSH);
                quip(id);
            };
            case AvatarTable.DREAD_BRAND -> (b, t) -> {
                LivingEntity foe = level() instanceof ServerLevel sl && brandTarget >= 0 && sl.getEntity(brandTarget) instanceof LivingEntity le
                        ? le : AvatarMoves.farthestFoe(this, 40);
                Vec3 mark = brandMark;
                brandMark = null;
                brandTarget = -1;
                AvatarMoves.dreadBrand(this, foe, mark);
                quip(id);
            };
            case AvatarTable.INVERSION_FIELD -> (b, t) -> {
                Vec3 mark = marks.isEmpty() ? t.position() : marks.get(0);
                marks = List.of();
                AvatarMoves.inversionField(this, mark);
                quip(id);
            };
            case AvatarTable.CURSE_NOVA -> (b, t) -> {
                AvatarMoves.cursePulse(this, position(), AvatarMath.novaReach(gatesOpen()), AvatarTable.NOVA, gatesOpen() ? 1 : 0);
                quip(id);
            };
            case AvatarTable.CALL_DISCIPLES -> (b, t) -> {
                int made = summonDisciples(2 + (gatesOpen() ? 1 : 0));
                AvatarVfx.callStrike(this);
                if (made > 0) quip(id);
            };
            case AvatarTable.BRIMSTONE_ERUPTION -> (b, t) -> {
                double base = Double.isNaN(eruptBase) ? Math.atan2(t.getZ() - getZ(), t.getX() - getX()) : eruptBase;
                eruptBase = Double.NaN;
                AvatarMoves.eruption(this, base);
                quip(id);
            };
            case AvatarTable.ABYSSAL_COLLAPSE -> (b, t) -> {
                AvatarMoves.abyssalCollapse(this);
                String[] lines = AvatarText.quips(variant(), id);
                say(lines[getRandom().nextInt(lines.length)]);
            };
            default -> (b, t) -> { };
        };
    }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case AvatarTable.VOID_CLAWS -> (b, t, left, total) -> AvatarMoves.tellClaws(this, left, total);
            case AvatarTable.HEX_VOLLEY -> (b, t, left, total) -> AvatarMoves.tellVolley(this, t, left, total);
            case AvatarTable.SHADOW_STEP -> (b, t, left, total) -> AvatarMoves.tellStep(this, t, left, total);
            case AvatarTable.CEILING_PULL -> (b, t, left, total) -> {
                if (AvatarMoves.first(left, total))
                    hint(id, "The Avatar lets go of the gravity of the ring: everyone in it will be pulled to the ceiling. Leave the ring, or take a Mana Zone: it halves the pull.");
                AvatarMoves.tellPull(this, AvatarMath.PULL_RADIUS, left, total);
            };
            case AvatarTable.FLOOR_CRUSH -> (b, t, left, total) -> AvatarMoves.tellCrush(this, t, left, total);
            case AvatarTable.DREAD_BRAND -> (b, t, left, total) -> AvatarMoves.tellBrand(this, left, total);
            case AvatarTable.INVERSION_FIELD -> (b, t, left, total) -> AvatarMoves.tellField(this, t, left, total);
            case AvatarTable.CURSE_NOVA -> (b, t, left, total) -> AvatarMoves.tellNova(this, AvatarMath.novaReach(gatesOpen()), left, total);
            case AvatarTable.CALL_DISCIPLES -> (b, t, left, total) -> {
                if (AvatarMoves.first(left, total)) AvatarVfx.callTell(this, total);
                if (level() instanceof ServerLevel sl) {
                    Tells.ring(sl, position(), 5, AvatarFx.rgb(this));
                    Tells.gather(sl, position().add(0, 1.2, 0), 1.0, AvatarFx.accent(this), 6);
                }
            };
            case AvatarTable.BRIMSTONE_ERUPTION -> (b, t, left, total) -> AvatarMoves.tellEruption(this, t, left, total);
            case AvatarTable.ABYSSAL_COLLAPSE -> (b, t, left, total) -> AvatarMoves.tellCollapse(this, left, total);
            default -> null;
        };
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("AVariant", variant());
        if (centre != null) tag.put("ACentre", NbtUtils.writeBlockPos(centre));
        tag.putBoolean("AFell", collapsePosted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AVariant")) entityData.set(VARIANT, AvatarTable.variantOf(tag.getInt("AVariant")));
        NbtUtils.readBlockPos(tag, "ACentre").ifPresent(pos -> centre = pos);
        collapsePosted = tag.getBoolean("AFell");
        addTag(FLOOR_BOSS_TAG);
    }
}
