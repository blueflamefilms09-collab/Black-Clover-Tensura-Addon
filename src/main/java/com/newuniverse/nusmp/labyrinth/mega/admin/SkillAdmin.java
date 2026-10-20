package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import com.newuniverse.nusmp.skilltree.unique.UniqueSkills;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the admin tree's skill commands must reach the same place a Skill Book reaches, and nowhere else. Since 1.96.0 a Skill Book teaches a real Tensura UNIQUE skill (one per
 * book, {@code nusmp:line_<id>}, registered at start-up in {@link UniqueSkills}) through {@link UniqueBooks#grant}, and the player picks and slots it in Tensura's own Ability
 * Selection screen; the Skill Tree, its Skill Points and its panels were removed at the owner's request and are NOT brought back here. So {@code unlock <id>} is one call to the
 * book's own grant (the book and the command can never disagree), {@code unlock all} is that call for every registered line, and {@code reset skills} is the book's own
 * {@link UniqueBooks#forget} for the lines the player owns: it touches only the skills the books teach, never Tensura's own skills or anything vanilla. Skill Points no longer exist,
 * so {@code give_sp} gives the thing that replaced them: MASTERY. A line skill's mastery decides which of its arts have awakened and the level of its passives
 * ({@code MasteryLevels}), exactly the progression Skill Points used to buy, so the command adds mastery points to every Unique skill line the player owns and says so plainly.
 *
 * <p>Teaching 250-odd skills in one tick would send 250-odd skill-list updates in one tick, so {@code all} and {@code reset skills} are JOBS: a small clock hands a few skills per
 * tick to the same call, then tells the admin the totals. One job per player at a time. Every call is guarded: a skill list that vetoes a change is a counted failure, never an
 * exception out of the tick.
 */
public final class SkillAdmin {
    private SkillAdmin() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Skills handled per tick by a job. */
    static final int BATCH = 12;

    private enum Mode { LEARN, FORGET }

    private static final class Job {
        final UUID player;
        final Mode mode;
        final boolean thenAll;
        final ArrayDeque<String> todo;
        final int total;
        int done, already, failed;

        Job(UUID player, Mode mode, List<String> ids, boolean thenAll) {
            this.player = player;
            this.mode = mode;
            this.thenAll = thenAll;
            this.todo = new ArrayDeque<>(ids);
            this.total = ids.size();
        }
    }

    private static final Map<UUID, Job> JOBS = new LinkedHashMap<>();

    // ================================================================ the ids
    /** The ids the commands accept for one skill: the bare ids of the registered Unique skill lines (what a book is named after), in the registry's order. */
    public static List<String> ids() { return UniqueSkills.bares(); }

    /** The ids offered for tab completion: {@code all} first, then every line. */
    public static List<String> suggestions() {
        List<String> out = new ArrayList<>(UniqueSkills.count() + 1);
        out.add("all");
        out.addAll(UniqueSkills.bares());
        return out;
    }

    public static boolean busy(UUID id) { return JOBS.containsKey(id); }

    // ================================================================ unlock
    /** {@code unlock <id|all>}. Returns 1 when something was taught (or the job started), 0 otherwise; the player is told either way. */
    public static int unlock(ServerPlayer p, String id) {
        if ("all".equalsIgnoreCase(id)) return unlockAll(p);
        LineIndex.Line line = UniqueSkills.line(id.toLowerCase(Locale.ROOT));
        if (line == null) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.SKILL_UNKNOWN, id));
            return 0;
        }
        UniqueBooks.Grant g = UniqueBooks.grant(p, line.bare(), false);                 // the call a Skill Book makes (the book tells the player itself; so does this command)
        switch (g) {
            case GRANTED -> {
                AdminMsg.tell(p, AdminMsg.green(AdminText.SKILL_LEARNED, line.name()));
                return 1;
            }
            case ALREADY -> {
                AdminMsg.tell(p, AdminMsg.yellow(AdminText.SKILL_ALREADY, line.name()));
                return 0;
            }
            default -> {
                AdminMsg.tell(p, AdminMsg.red(AdminText.SKILL_FAIL, line.name(), g.name().toLowerCase(Locale.ROOT)));
                return 0;
            }
        }
    }

    private static int unlockAll(ServerPlayer p) {
        if (busy(p.getUUID())) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.SKILL_BUSY));
            return 0;
        }
        List<String> ids = ids();
        JOBS.put(p.getUUID(), new Job(p.getUUID(), Mode.LEARN, ids, false));
        AdminMsg.tell(p, AdminMsg.green(AdminText.SKILL_ALL_START, ids.size()));
        return 1;
    }

    // ================================================================ reset skills
    /**
     * {@code reset skills}: forgets the book-taught Unique skills the player owns. {@code thenAll} makes the end of the job also say the "everything cleared" line (for
     * {@code reset all}). Returns 1 when a job started, 0 when there was nothing to forget or a job is running.
     */
    public static int forgetAll(ServerPlayer p, boolean thenAll) {
        if (busy(p.getUUID())) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.SKILL_BUSY));
            return 0;
        }
        List<String> owned = new ArrayList<>();
        for (UniqueBooks.Owned o : UniqueBooks.ownedLines(p)) owned.add(o.skill().bare());
        if (owned.isEmpty()) {
            AdminMsg.tell(p, AdminMsg.gray(AdminText.RESET_SKILLS_NONE));
            if (thenAll) AdminMsg.tell(p, AdminMsg.green(AdminText.RESET_ALL));
            return 0;
        }
        JOBS.put(p.getUUID(), new Job(p.getUUID(), Mode.FORGET, owned, thenAll));
        AdminMsg.tell(p, AdminMsg.yellow(AdminText.RESET_SKILLS_START, owned.size()));
        return 1;
    }

    // ================================================================ give_sp (mastery)
    /**
     * {@code give_sp <amount>}: Skill Points were removed in 1.96.0, so this adds {@code amount} MASTERY points to every Unique skill line the player owns (mastery is what opens the
     * later arts of a skill and raises its passives now). Says so, and says plainly when there was nobody to give them to. Returns the number of skills that got them.
     */
    public static int giveMastery(ServerPlayer p, int amount) {
        List<UniqueBooks.Owned> owned = UniqueBooks.ownedLines(p);
        if (owned.isEmpty()) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.SP_NONE));
            return 0;
        }
        int given = 0;
        for (UniqueBooks.Owned o : owned) {
            try {
                o.instance().addMasteryPoint(p, (double) amount);
                o.instance().markDirty();
                given++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("[nusmp] /dungeon skill give_sp: mastery of {} could not be raised for {}: {}", o.skill().bare(), p.getGameProfile().getName(), e.toString());
            }
        }
        AdminMsg.tell(p, AdminMsg.yellow(AdminText.SP_REMOVED));
        if (given == 0) {
            AdminMsg.tell(p, AdminMsg.red(AdminText.FAILED, "mastery"));
            return 0;
        }
        AdminMsg.tell(p, AdminMsg.green(AdminText.SP_OK, amount, given));
        return given;
    }

    // ================================================================ the clock
    /** Called once per server tick: hands {@link #BATCH} skills of each running job to the book's own call, and tells the admin when a job is done. */
    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        for (Job j : new ArrayList<>(JOBS.values())) {
            ServerPlayer p = server.getPlayerList().getPlayer(j.player);
            if (p == null) JOBS.remove(j.player);
            else stepJob(j, p);
        }
    }

    /** Runs the job of a player to its end at once, {@link #BATCH} skills per round (a self-test's way to drive a job for a stand-in, which is not in the player list). */
    static void drain(ServerPlayer p) {
        Job j = JOBS.get(p.getUUID());
        for (int guard = 0; j != null && JOBS.containsKey(p.getUUID()) && guard < 10_000; guard++) stepJob(j, p);
    }

    /** One round of a job: up to {@link #BATCH} skills, then the summary when none are left. */
    private static void stepJob(Job j, ServerPlayer p) {
        for (int n = 0; n < BATCH && !j.todo.isEmpty(); n++) {
            String bare = j.todo.poll();
            try {
                if (j.mode == Mode.LEARN) {
                    UniqueBooks.Grant g = UniqueBooks.grant(p, bare, false);
                    if (g == UniqueBooks.Grant.GRANTED) j.done++;
                    else if (g == UniqueBooks.Grant.ALREADY) j.already++;
                    else j.failed++;
                } else if (UniqueBooks.forget(p, bare)) j.done++;
                else j.failed++;
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("[nusmp] /dungeon skill job ({}) failed on {} for {}: {}", j.mode, bare, p.getGameProfile().getName(), e.toString());
                j.failed++;
            }
        }
        if (!j.todo.isEmpty()) return;
        JOBS.remove(j.player);
        if (j.mode == Mode.LEARN) {
            AdminMsg.tell(p, j.failed == 0 ? AdminMsg.green(AdminText.SKILL_ALL_DONE, j.done, j.already, j.failed, j.total) : AdminMsg.yellow(AdminText.SKILL_ALL_DONE, j.done, j.already, j.failed, j.total));
        } else {
            AdminMsg.tell(p, j.failed == 0 ? AdminMsg.green(AdminText.RESET_SKILLS_DONE, j.done, j.failed) : AdminMsg.yellow(AdminText.RESET_SKILLS_DONE, j.done, j.failed));
            if (j.thenAll) AdminMsg.tell(p, AdminMsg.green(AdminText.RESET_ALL));
        }
    }

    public static void forget(UUID id) { JOBS.remove(id); }

    public static void clear() { JOBS.clear(); }
}
