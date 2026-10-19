package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Area;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Journal;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.MechanicsData;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.PuzzleBridge;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Seeds;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Why: one puzzle that is ARMED somewhere: the running copy of a {@link PuzzleRule} in a real room. It owns the stage (where each cell stands), the journal of every block it changed (so
 * ending it, for whatever reason, puts the room back block for block), the party (each player gets a small actor number the rule knows them by), and the carrying-out of the rule's effects:
 * notes, sparks of Light and Dark Rukh, lines of text, the penalty (always capped so it can never kill, always lighter than the mechanics' hazards), the question with its chat buttons,
 * the carried tokens, the pit under a wrong tile, the walk back to the start, the mimic's brood and the reward. It sends the rule only what players really did (a click on a cell, a step on or off
 * a floor tile, a chosen answer) and draws only what the rule shows, so all decisions stay in the pure rule. SAFETY: it never closes a door or a way; if nobody beats it within the bypass time
 * the mechanism simply winds down (no reward); the lectern lets a party give up at once; and when the party leaves, everything is restored. Server thread only.
 */
public final class PuzzleInstance {
    public enum State { ARMED, SOLVED, ENDED }

    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();
    private static int seq = 1;

    public final int id = seq++;
    public final String key, type;
    public final ServerLevel level;
    public final Sector sector;
    public final Room room;
    public final Area area;
    public final BlockPos anchor;
    public final int floor;
    public final String theme, flavour;
    public final long seed;
    public boolean admin;
    public State state = State.ARMED;
    public boolean dismissed, bypassed, rewardPaid;
    public int errors;
    public final List<ServerPlayer> inside = new ArrayList<>();

    PuzzleRule rule;
    PuzzleStage stage;
    private final Journal journal;
    private int[][] drawn;
    private final Map<UUID, Integer> actors = new LinkedHashMap<>();
    private final Map<UUID, BlockPos> entry = new HashMap<>();
    private final Map<Integer, Integer> standing = new HashMap<>();
    private final Set<UUID> told = new HashSet<>();
    private final Map<UUID, Long> dismissAt = new HashMap<>();
    private final List<UUID> mobs = new ArrayList<>();
    private final Map<String, ItemStack> lastIssued = new HashMap<>();
    private final SplittableRandom rng;
    private long now, born, lastUpdate = Long.MIN_VALUE, lastActAt, lastNudgeAt, solvedAt, insideTicks;
    private int acts, issued, askSeq, lastPityFails, tonesThisTick;
    private boolean warned30, warned10;
    private long toneTick = Long.MIN_VALUE;
    private final String mobTag;

    public PuzzleInstance(String key, String type, ServerLevel level, Sector sector, Room room, Area area, BlockPos anchor, int floor, String theme, String flavour, long seed) {
        this.key = key;
        this.type = type;
        this.level = level;
        this.sector = sector;
        this.room = room;
        this.area = area;
        this.anchor = anchor;
        this.floor = floor;
        this.theme = theme == null ? "" : theme;
        this.flavour = flavour == null ? "" : flavour;
        this.seed = seed;
        this.rng = Seeds.rng(seed ^ 0x1234ABCDL);
        this.journal = new Journal(level);
        this.mobTag = "nusmp_puzzle_" + Integer.toHexString(key.hashCode());
    }

    public Journal journal() { return journal; }

    public PuzzleRule rule() { return rule; }

    public long clock() { return now; }

    public boolean active() { return state != State.ENDED; }

    // ---------------------------------------------------------------- arming
    /** Builds the rule for this party and finds a place for it; false when the room cannot hold it (nothing was changed). */
    public boolean arm(List<ServerPlayer> players, long tick) {
        now = tick;
        born = tick;
        lastActAt = tick;
        int party = 0;
        for (ServerPlayer p : players) if (PuzzleBridge.affects(p)) party++;
        PuzzleRule r = PuzzleCatalog.create(type, new Params(seed, Math.max(1, party), PuzzleBridge.tier(floor)));
        if (r == null) return false;
        PuzzleStage st = PuzzleStage.plan(level, r, anchor, room, Seeds.rng(seed));
        if (st == null) return false;
        rule = r;
        stage = st;
        drawn = fresh();
        inside.clear();
        inside.addAll(players);
        for (ServerPlayer p : players) actorOf(p);
        redraw(true);
        placeLectern();
        apply(rule.begin(tick), null);
        redraw(false);
        return true;
    }

    private int[][] fresh() {
        int[][] d = new int[stage.cells.size()][];
        for (PuzzleStage.Cell c : stage.cells) {
            d[c.index()] = new int[c.tile() ? 1 : c.height()];
            java.util.Arrays.fill(d[c.index()], -1);
        }
        return d;
    }

    private void placeLectern() {
        if (stage.lectern == null) return;
        journal.placeInAir(stage.lectern, Blocks.LECTERN.defaultBlockState());
    }

    /** A new party size before anybody has done anything: build the puzzle again for it (the same seed, a board sized for the party). */
    private void maybeRescale() {
        if (acts > 0 || state != State.ARMED) return;
        int party = 0;
        for (ServerPlayer p : inside) if (PuzzleBridge.affects(p)) party++;
        if (party < 1 || party == rule.params().party()) return;
        PuzzleRule r = PuzzleCatalog.create(type, rule.params().withParty(party));
        if (r == null) return;
        journal.restore();
        PuzzleStage st = PuzzleStage.plan(level, r, anchor, room, Seeds.rng(seed));
        if (st == null) {                                                          // keep the old board (restore it)
            redrawAll();
            placeLectern();
            return;
        }
        rule = r;
        stage = st;
        drawn = fresh();
        standing.clear();
        redraw(true);
        placeLectern();
        apply(rule.begin(now), null);
    }

    private void redrawAll() {
        drawn = fresh();
        redraw(true);
    }

    // ---------------------------------------------------------------- the party
    int actorOf(ServerPlayer p) {
        Integer a = actors.get(p.getUUID());
        if (a == null) {
            a = Math.min(7, actors.size());
            actors.put(p.getUUID(), a);
        }
        return a;
    }

    private List<ServerPlayer> playersOf(int actor) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : inside) {
            Integer a = actors.get(p.getUUID());
            if (actor < 0 || (a != null && a == actor)) out.add(p);
        }
        return out;
    }

    // ---------------------------------------------------------------- the tick
    /** Every tick with the players of the area (already filtered: alive, in this level and inside the area). */
    public void update(List<ServerPlayer> players, long tick) {
        if (state == State.ENDED) return;
        now = tick;
        long dt = lastUpdate == Long.MIN_VALUE ? 1 : Math.max(0, Math.min(40, tick - lastUpdate));
        lastUpdate = tick;
        inside.clear();
        inside.addAll(players);
        for (ServerPlayer p : players) {
            actorOf(p);
            entry.putIfAbsent(p.getUUID(), p.blockPosition());
            if (told.add(p.getUUID())) announce(p);
        }
        try {
            if (!players.isEmpty()) insideTicks += dt;
            if (state == State.ARMED) {
                if (tick % 40 == 0) maybeRescale();
                syncSteps();
                apply(rule.tick(tick), null);
                if (state == State.ARMED) {
                    pityAndNudge();
                    bypassClock();
                }
            }
            if ((tick & 1) == 0) redraw(false);
            if (tick % 10 == 0) drawLinks();
        } catch (RuntimeException e) {
            errors++;
            if (WARNED.add(type + ":update")) LOG.warn("[nusmp] puzzle {} failed in update: {}", type, e.toString());
        }
    }

    private int tileUnder(ServerPlayer p) {
        int cell = stage.tileAt(Mth.floor(p.getX()), Mth.floor(p.getZ()));
        if (cell < 0 || p.isSpectator()) return -1;
        double top = stage.cells.get(cell).base().getY() + 1;
        return p.getY() >= top - 0.2 && p.getY() <= top + 0.6 ? cell : -1;
    }

    private void syncSteps() {
        Set<Integer> present = new HashSet<>();
        for (ServerPlayer p : inside) {
            int actor = actorOf(p);
            present.add(actor);
            int cell = tileUnder(p);
            int prev = standing.getOrDefault(actor, -1);
            if (cell == prev) continue;
            standing.put(actor, cell);
            if (prev >= 0) apply(rule.act(Act.stepOff(actor, prev, now)), p);
            if (cell >= 0) {
                lastActAt = now;
                acts++;
                apply(rule.act(Act.stepOn(actor, cell, now, 0)), p);
            }
        }
        for (Integer actor : new ArrayList<>(standing.keySet())) {
            if (present.contains(actor)) continue;
            int prev = standing.remove(actor);
            if (prev >= 0) apply(rule.act(Act.stepOff(actor, prev, now)), null);
        }
    }

    private void pityAndNudge() {
        int fails = rule.fails();
        if (fails >= 3 && fails > lastPityFails) {
            lastPityFails = fails;
            Act s = rule.suggest(now);
            if (s != null && s.cell() >= 0 && s.cell() < stage.cells.size()) {
                spark(s.cell(), true);
                tellAll(common("pity"), true);
            }
        }
        if (now - lastActAt > rule.nudgeTicks() && now - lastNudgeAt > rule.nudgeTicks()) {
            lastNudgeAt = now;
            tellAll(common("nudge"), true);
        }
    }

    private void bypassClock() {
        long limit = (long) PuzzleWeights.get().bypassSeconds * 20L;
        long left = Math.max(0, (limit - insideTicks) / 20);
        if (left <= 30 && !warned30) {
            warned30 = true;
            tellAll(common("bypassWarn", Long.toString(left)), true);
        } else if (left <= 10 && !warned10) {
            warned10 = true;
            tellAll(common("bypassWarn", Long.toString(left)), true);
        }
        if (insideTicks >= limit) bypass();
    }

    /** The mechanism winds down by itself after the bypass time: nothing was locked, nothing is paid. */
    public void bypass() {
        if (state != State.ARMED) return;
        bypassed = true;
        tellAll(common("bypass"), false);
        end();
    }

    // ---------------------------------------------------------------- drawing
    private void redraw(boolean force) {
        for (PuzzleStage.Cell c : stage.cells) {
            int layers = c.tile() ? 1 : c.height();
            for (int l = 0; l < layers; l++) {
                int face = rule.face(c.index(), l);
                if (!force && drawn[c.index()][l] == face) continue;
                drawn[c.index()][l] = face;
                BlockPos pos = c.tile() ? c.base() : c.base().above(l);
                journal.set(pos, PuzzlePalette.state(c.palette(), face, flavour));
            }
        }
    }

    private void drawLinks() {
        List<int[]> links = rule.links();
        if (links.isEmpty()) return;
        for (int[] l : links) {
            Vec3 a = Vec3.atCenterOf(stage.standing(l[0])).add(0, 0.8, 0), b = Vec3.atCenterOf(stage.standing(l[1])).add(0, 0.8, 0);
            RukhFx.stream(level, a, b, RukhFx.Alignment.LIGHT, 3);
        }
    }

    // ---------------------------------------------------------------- what players do
    /** A right click on a block of this puzzle: true when it was ours (the caller cancels the vanilla action). */
    public boolean onClick(ServerPlayer p, BlockPos pos, ItemStack held, boolean sneak, long tick) {
        if (state == State.ENDED || stage == null) return false;
        now = Math.max(now, tick);
        if (stage.lectern != null && pos.equals(stage.lectern)) {
            plinth(p, sneak);
            return true;
        }
        int cell = stage.cellAt(pos);
        if (cell < 0) return false;
        if (state != State.ARMED) return true;
        inside.removeIf(x -> x.getUUID().equals(p.getUUID()));
        inside.add(p);
        int actor = actorOf(p);
        lastActAt = now;
        acts++;
        apply(rule.act(new Act(Act.Kind.CLICK, actor, cell, sneak ? 1 : 0, PuzzleTokens.tokenOf(held, key), now, 0)), p);
        redraw(false);
        return true;
    }

    /** A chosen answer of a question; null when accepted, else the lang suffix of the reason. */
    public String choose(ServerPlayer p, int question, int answer, long tick) {
        if (state != State.ARMED) return "pickNone";
        if (question != askSeq) return "stale";
        now = Math.max(now, tick);
        lastActAt = now;
        acts++;
        apply(rule.act(Act.choose(actorOf(p), answer, now)), p);
        redraw(false);
        return null;
    }

    private void plinth(ServerPlayer p, boolean sneak) {
        if (sneak) {
            Long t = dismissAt.get(p.getUUID());
            if (t != null && now - t <= 120) {
                dismissed = true;
                tellAll(common("dismissed"), false);
                end();
                return;
            }
            dismissAt.put(p.getUUID(), now);
            send(p, common("dismissAsk"), false);
            return;
        }
        send(p, name().withStyle(ChatFormatting.GOLD), false);
        send(p, hint().withStyle(ChatFormatting.GRAY), false);
        send(p, common("progress", Integer.toString(rule.fails())), true);
        apply(rule.question(), p);
    }

    /** Plays an action the way the world would: a self-test drives the puzzle through this, so the real click and step paths are exercised. */
    public void perform(ServerPlayer p, Act a, long tick) {
        if (state == State.ENDED) return;
        now = Math.max(now, tick);
        switch (a.kind()) {
            case CLICK -> {
                BlockPos at = stage.cells.get(a.cell()).base();
                ItemStack held = a.item() == null ? ItemStack.EMPTY : tokenStack(a.item());
                p.setItemInHand(InteractionHand.MAIN_HAND, held);
                onClick(p, at, held, a.sneaking(), tick);
            }
            case STEP -> {
                BlockPos at = stage.standing(a.cell());
                if (a.stepsOn()) p.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                else p.setPos(stage.minX - 1 + 0.5, stage.floorY + 1, stage.minZ - 1 + 0.5);
                update(List.of(p), tick);
            }
            case CHOOSE -> choose(p, askSeq, a.arg(), tick);
        }
    }

    /** The token item last issued for a token word (or a fresh one), for the self-tests. */
    ItemStack tokenStack(String token) {
        ItemStack s = lastIssued.get(token);
        return s != null ? s.copy() : PuzzleTokens.make(token, key);
    }

    // ---------------------------------------------------------------- the rule's effects
    private void apply(List<Fx> fx, ServerPlayer source) {
        for (Fx f : fx) {
            try {
                switch (f.kind()) {
                    case TONE -> tone(f);
                    case SPARK -> spark(f.cell(), f.n() == 1);
                    case SAY -> say(f);
                    case PENALTY -> penalty(f);
                    case SOLVED -> onSolved();
                    case RESET -> onReset();
                    case GIVE -> give(f);
                    case CONSUME -> consume(f);
                    case ASK -> ask(f);
                    case PIT -> pit(f);
                    case RETURN -> back(f);
                    case MIMIC -> mimic(f);
                }
            } catch (RuntimeException e) {
                errors++;
                if (WARNED.add(type + ":" + f.kind())) LOG.warn("[nusmp] puzzle {} could not carry out {}: {}", type, f.kind(), e.toString());
            }
        }
    }

    private BlockPos posOf(int cell) { return cell >= 0 && cell < stage.cells.size() ? stage.standing(cell) : anchor; }

    private SoundEvent instrument(String name) {
        return switch (name == null ? "" : name) {
            case "bell" -> SoundEvents.NOTE_BLOCK_BELL.value();
            case "chime" -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case "bass" -> SoundEvents.NOTE_BLOCK_BASS.value();
            default -> SoundEvents.NOTE_BLOCK_HARP.value();
        };
    }

    private void tone(Fx f) {
        if (toneTick != now) { toneTick = now; tonesThisTick = 0; }
        if (++tonesThisTick > 10) return;
        float pitch = (float) Math.max(0.5, Math.min(2.0, Math.pow(2.0, (f.n() - 12) / 12.0)));
        level.playSound(null, posOf(f.cell()), instrument(f.key()), SoundSource.BLOCKS, 1.0f, pitch);
    }

    private void spark(int cell, boolean good) {
        Vec3 c = Vec3.atCenterOf(posOf(cell)).add(0, 0.6, 0);
        RukhFx.burst(level, c, good ? RukhFx.Alignment.LIGHT : RukhFx.Alignment.DARK, good ? 5 : 3);
    }

    private void pit(Fx f) {
        level.playSound(null, posOf(f.cell()), SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0f, 0.6f);
        spark(f.cell(), false);
    }

    private void say(Fx f) {
        Component line = line(f.key(), f.more());
        boolean chat = f.key().startsWith("clue") || f.key().startsWith("rule") || f.key().equals("recite") || f.key().startsWith("inscr") || f.key().startsWith("item.") || f.key().equals("wants")
                || f.key().equals("close") || f.key().equals("digitsRight");
        for (ServerPlayer p : playersOf(f.actor())) send(p, line, !chat);
    }

    // ---------------------------------------------------------------- penalties
    private void penalty(Fx f) {
        for (ServerPlayer p : playersOf(f.actor())) {
            if (!PuzzleBridge.affects(p)) continue;
            applyPenalty(p, f.key(), f.n());
            send(p, common("penalty." + f.key()), true);
        }
    }

    /** The costs, all scaled by depth and none able to kill: damage never takes the last three health points. */
    private void applyPenalty(ServerPlayer p, String kind, int sev) {
        int tier = PuzzleBridge.tier(floor);
        switch (kind) {
            case "dark" -> PuzzleBridge.effect(p, "minecraft:darkness", 40 * sev, 0);
            case "drain" -> {
                int n = Math.min(Math.max(0, p.totalExperience), 6 * sev * tier);
                if (n > 0) p.giveExperiencePoints(-n);
                PuzzleBridge.effect(p, "minecraft:weakness", 60 * sev, 0);
            }
            case "slow" -> PuzzleBridge.effect(p, "minecraft:slowness", 60 * sev, 1);
            case "hunger" -> PuzzleBridge.effect(p, "minecraft:hunger", 80 * sev, 0);
            case "ambush" -> {
                int alive = 0;
                for (UUID id : mobs) if (level.getEntity(id) != null) alive++;
                for (int i = 0; i < 1 + sev && alive < 4; i++, alive++) {
                    Mob m = PuzzleMobs.spawn(level, "undead", floor, p.blockPosition(), rng, mobTag, 1.0);
                    if (m != null) { mobs.add(m.getUUID()); m.setTarget(p); }
                }
            }
            default -> {
                double amount = sev * 1.5 * (1.0 + 0.5 * (tier - 1));
                double safe = p.getHealth() - 3.0;
                if (safe > 0.0) PuzzleBridge.hurt(level, p, Math.min(amount, safe), "magic");
            }
        }
    }

    // ---------------------------------------------------------------- tokens
    private void give(Fx f) {
        List<ServerPlayer> who = playersOf(f.actor());
        if (who.isEmpty() || issued >= 64) return;
        ServerPlayer p = who.get(0);
        ItemStack s = PuzzleTokens.make(f.key(), key);
        lastIssued.put(f.key(), s.copy());
        issued++;
        if (!p.getInventory().add(s)) p.drop(s, false);
        level.playSound(null, p.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private void consume(Fx f) {
        for (ServerPlayer p : playersOf(f.actor())) {
            if (p.isCreative()) return;
            ItemStack held = p.getMainHandItem();
            if (f.key().equals(PuzzleTokens.tokenOf(held, key))) { held.shrink(1); return; }
            ItemStack off = p.getOffhandItem();
            if (f.key().equals(PuzzleTokens.tokenOf(off, key))) { off.shrink(1); return; }
        }
    }

    // ---------------------------------------------------------------- questions
    private void ask(Fx f) {
        askSeq++;
        MutableComponent q = Component.empty().append(line("ask", List.of(Integer.toString(f.n())))).append(Component.literal(" ")).append(
                Component.translatableWithFallback(PuzzleText.langKey(f.key()), PuzzleText.en(f.key()))).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : inside) send(p, q, false);
        for (int i = 0; i < f.more().size(); i++) {
            MutableComponent a = Component.literal("  [" + (char) ('A' + i) + "] ").append(Component.translatableWithFallback(PuzzleText.langKey(f.more().get(i)), PuzzleText.en(f.more().get(i))));
            a.setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nusmp_puzzle pick " + id + " " + askSeq + " " + i))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, common("pickHint"))));
            for (ServerPlayer p : inside) send(p, a, false);
        }
    }

    // ---------------------------------------------------------------- the walk back, the mimic, the reset
    private void back(Fx f) {
        for (ServerPlayer p : playersOf(f.actor())) {
            BlockPos e = entry.get(p.getUUID());
            BlockPos to = e != null && area.contains(e) ? e : anchor;
            PuzzleBridge.teleport(p, to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            standing.remove(actors.getOrDefault(p.getUUID(), 0));
        }
    }

    private void mimic(Fx f) {
        BlockPos at = posOf(f.cell());
        Mob m = PuzzleMobs.spawn(level, "beast", floor, at, rng, mobTag, 1.8);
        if (m == null) m = PuzzleMobs.spawn(level, "swarm", floor, at, rng, mobTag, 1.0);
        if (m != null) {
            mobs.add(m.getUUID());
            m.setGlowingTag(true);
            m.setCustomName(Component.translatableWithFallback(PuzzleText.langKey("mimic_chests.mimic.name"), "Mimic"));
        }
        level.playSound(null, at, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0f, 0.5f);
        spark(f.cell(), false);
    }

    private void onReset() {
        standing.clear();
        redrawAll();
        lastActAt = now;
    }

    // ---------------------------------------------------------------- the reward
    private void onSolved() {
        if (state != State.ARMED) return;
        state = State.SOLVED;
        solvedAt = now;
        redraw(false);
        List<ServerPlayer> who = new ArrayList<>();
        for (ServerPlayer p : inside) if (PuzzleBridge.affects(p)) who.add(p);
        boolean flawless = rule.fails() == 0;
        int rolls = flawless ? 2 : 1;
        long s = seed;
        for (ServerPlayer p : who) PuzzleBridge.loot(level, anchor, p, s += 7919L, rolls);
        if (who.isEmpty()) PuzzleBridge.loot(level, anchor, null, s, rolls);
        PuzzleBridge.xp(level, anchor, (int) (14 + 9 * PuzzleBridge.depth(floor)) * Math.max(1, who.size()) * (flawless ? 2 : 1));
        rewardPaid = true;
        level.playSound(null, anchor, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.2f);
        for (PuzzleStage.Cell c : stage.cells) if (c.index() % 2 == 0) spark(c.index(), true);
        for (ServerPlayer p : inside) {
            if (p.connection == null) continue;
            try {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 10));
                p.connection.send(new ClientboundSetTitleTextPacket(common("solvedTitle").withStyle(ChatFormatting.GREEN)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(name().withStyle(ChatFormatting.GRAY)));
            } catch (RuntimeException ignored) {
                // a title is never worth an exception
            }
        }
        tellAll(common("solved"), false);
        if (flawless) tellAll(common("flawless"), true);
        if (!admin) MechanicsData.get(level.getServer()).markSolved("puzzle:" + key);
    }

    /** True once a solved puzzle has been on show long enough. */
    public boolean solvedLongEnough(long tick) { return state == State.SOLVED && tick - solvedAt > 160; }

    // ---------------------------------------------------------------- ending
    /** The one cleanup: every block back, every monster and token gone. Safe to call twice. */
    public void end() {
        if (state == State.ENDED) return;
        state = State.ENDED;
        try {
            journal.restore();
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] puzzle {} could not restore its blocks: {}", type, e.toString());
        }
        for (UUID id : new ArrayList<>(mobs)) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) e.discard();
        }
        mobs.clear();
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) PuzzleTokens.purge(p, key);
        for (ServerPlayer p : inside) PuzzleTokens.purge(p, key);
        inside.clear();
        PuzzleData.get(level.getServer()).forget(this);
    }

    // ---------------------------------------------------------------- text
    private MutableComponent name() { return Component.translatableWithFallback(PuzzleText.langKey(type + ".name"), PuzzleText.en(type + ".name")); }

    private MutableComponent hint() { return Component.translatableWithFallback(PuzzleText.langKey(type + ".hint"), PuzzleText.en(type + ".hint")); }

    private MutableComponent common(String key, String... args) {
        String rel = "common." + key;
        return Component.translatableWithFallback(PuzzleText.langKey(rel), PuzzleText.en(rel), (Object[]) args);
    }

    private MutableComponent flavourLine() {
        String rel = PuzzleText.en("theme." + theme) != null ? "theme." + theme : "theme.default";
        return Component.translatableWithFallback(PuzzleText.langKey(rel), PuzzleText.en(rel), name());
    }

    private Component line(String key, List<String> more) {
        String rel = PuzzleText.resolve(rule == null ? type : rule.id(), key);
        if (rel == null) return Component.literal(key);
        Object[] args = new Object[more.size()];
        for (int i = 0; i < args.length; i++) {
            String m = more.get(i);
            args[i] = m.startsWith("$") ? Component.translatableWithFallback(PuzzleText.langKey(m.substring(1)), PuzzleText.en(m.substring(1))) : m;
        }
        return Component.translatableWithFallback(PuzzleText.langKey(rel), PuzzleText.en(rel), args);
    }

    private void announce(ServerPlayer p) {
        send(p, Component.translatableWithFallback(PuzzleText.langKey("common.enter"), PuzzleText.en("common.enter"), name()).withStyle(ChatFormatting.GOLD), true);
        send(p, flavourLine().withStyle(ChatFormatting.DARK_AQUA), false);
        send(p, hint().withStyle(ChatFormatting.GRAY), false);
    }

    private void tellAll(Component c, boolean bar) {
        for (ServerPlayer p : inside) send(p, c, bar);
    }

    private static void send(ServerPlayer p, Component c, boolean actionBar) {
        if (p == null || p.connection == null) return;
        try {
            if (actionBar) p.displayClientMessage(c, true);
            else p.sendSystemMessage(c);
        } catch (RuntimeException ignored) {
            // a message is never worth an exception
        }
    }

    public String describe() {
        return type + " #" + id + " key " + key + " floor " + floor + " theme " + theme + " " + state + " cells " + (stage == null ? 0 : stage.cells.size()) + " fails " + (rule == null ? 0 : rule.fails()) + " party "
                + (rule == null ? 0 : rule.params().party()) + " blocks changed " + journal.size();
    }

    /** For the self-tests: the block the world shows for a cell layer right now. */
    BlockState shown(int cell, int layer) {
        PuzzleStage.Cell c = stage.cells.get(cell);
        return level.getBlockState(c.tile() ? c.base() : c.base().above(layer));
    }
}
