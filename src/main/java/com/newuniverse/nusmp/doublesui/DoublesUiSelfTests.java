package com.newuniverse.nusmp.doublesui;

import com.newuniverse.nusmp.entity.MirrorDoubleEntity;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the Real Double stack (docs/doubles_ui.md) cannot be watched in a game here, so the CI server smoke test checks the parts that live on the server (prefix {@code doublesui}):
 * {@code doublesui.math} (the cycle wraps and has its "none" stop, the stack layout never overlaps for every count and every row limit, the overflow folds into "+N more"),
 * {@code doublesui.roster} (three real double entities of one owner: next / previous / pick by id, an id of a stranger, of another owner and of a far double are all refused, a double
 * that leaves clears the selection, and the selection answers {@code chosen}) and {@code doublesui.codec} (both packets survive the wire; a forged 40-row packet is cut to 16 and a
 * NaN health reads as 0). Server safe: no client class; the entities are never added to the world and everything is forgotten at the end.
 */
public final class DoublesUiSelfTests {
    private DoublesUiSelfTests() {}

    public static void register() {
        SelfTests.register("doublesui.math", DoublesUiSelfTests::math);
        SelfTests.register("doublesui.roster", DoublesUiSelfTests::roster);
        SelfTests.register("doublesui.codec", DoublesUiSelfTests::codec);
    }

    private static Result math(MinecraftServer server) {
        if (DoublesUiMath.step(DoublesUiMath.AUTO, 1, 3) != 0 || DoublesUiMath.step(2, 1, 3) != DoublesUiMath.AUTO || DoublesUiMath.step(DoublesUiMath.AUTO, -1, 3) != 2
                || DoublesUiMath.step(0, -1, 3) != DoublesUiMath.AUTO || DoublesUiMath.step(5, 1, 0) != DoublesUiMath.AUTO)
            return Result.fail("the cycle does not wrap through AUTO");
        int checked = 0;
        for (int n = 0; n <= DoublesUiMath.MAX_ROWS; n++)
            for (int maxShown = 1; maxShown <= DoublesUiMath.MAX_ROWS; maxShown++)
                for (int active = -1; active < n; active++) {
                    boolean has = active >= 0;
                    int s = DoublesUiMath.shown(n, maxShown);
                    if (s + DoublesUiMath.hidden(n, maxShown) != n) return Result.fail("shown + hidden != n for n=" + n + " max=" + maxShown);
                    boolean[] seen = new boolean[Math.max(1, n)];
                    int prevBottom = DoublesUiMath.HEADER_H;
                    for (int slot = 0; slot < s; slot++) {
                        int src = DoublesUiMath.source(slot, active, n);
                        if (src < 0 || src >= n || seen[src]) return Result.fail("slot " + slot + " shows a double twice or none (n=" + n + " active=" + active + ")");
                        seen[src] = true;
                        int top = DoublesUiMath.cardTop(slot, has);
                        if (top <= prevBottom) return Result.fail("cards overlap at slot " + slot + " (n=" + n + ")");
                        prevBottom = top + DoublesUiMath.cardHeight(slot, has);
                    }
                    if (s > 0 && has && DoublesUiMath.source(0, active, n) != active) return Result.fail("the selected double is not on top");
                    if (s > 0 && DoublesUiMath.totalHeight(n, maxShown, has) < prevBottom) return Result.fail("the stack is shorter than its cards");
                    checked++;
                }
        return Result.ok(checked + " (count, limit, selection) layouts: no overlap, selected on top, the cycle wraps through AUTO");
    }

    private static MirrorDoubleEntity make(ServerLevel level, UUID owner) {
        MirrorDoubleEntity d = NUEntities.MIRROR_DOUBLE.get().create(level);
        if (d == null) throw new IllegalStateException("no Real Double entity");
        d.setOwnerUUID(owner);
        return d;
    }

    private static Result roster(MinecraftServer server) {
        ServerLevel level = server.overworld();
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
        List<MirrorDoubleEntity> all = new ArrayList<>();
        try {
            MirrorDoubleEntity a = make(level, owner), b = make(level, owner), c = make(level, owner), stranger = make(level, other);
            all.add(a); all.add(b); all.add(c); all.add(stranger);
            for (MirrorDoubleEntity d : all) DoublesRoster.register(d);
            Vec3 here = Vec3.ZERO, far = new Vec3(500, 0, 500);
            if (DoublesRoster.count(owner) != 3 || DoublesRoster.count(other) != 1) return Result.fail("the roster holds " + DoublesRoster.count(owner) + " / " + DoublesRoster.count(other));
            if (DoublesRoster.activeOf(owner) != null) return Result.fail("a fresh roster has a selection");
            UUID[] want = {a.getUUID(), b.getUUID(), c.getUUID(), null};
            for (UUID w : want) {
                if (!DoublesRoster.apply(owner, level, here, C2S_DoubleSelectPayload.NEXT, UUID.randomUUID())) return Result.fail("next did not move the selection");
                if (!java.util.Objects.equals(DoublesRoster.activeOf(owner), w)) return Result.fail("next landed on " + DoublesRoster.activeOf(owner) + ", wanted " + w);
            }
            if (!DoublesRoster.apply(owner, level, here, C2S_DoubleSelectPayload.PREV, null) || !c.getUUID().equals(DoublesRoster.activeOf(owner))) return Result.fail("previous from none did not reach the last double");
            if (!DoublesRoster.apply(owner, level, here, C2S_DoubleSelectPayload.SET, b.getUUID()) || !b.getUUID().equals(DoublesRoster.activeOf(owner))) return Result.fail("pick by id failed");
            if (DoublesRoster.apply(owner, level, here, C2S_DoubleSelectPayload.SET, UUID.randomUUID())) return Result.fail("an unknown id was accepted");
            if (DoublesRoster.apply(owner, level, here, C2S_DoubleSelectPayload.SET, stranger.getUUID())) return Result.fail("another owner's double was accepted");
            if (DoublesRoster.apply(owner, level, far, C2S_DoubleSelectPayload.SET, a.getUUID())) return Result.fail("a double out of reach was accepted");
            if (DoublesRoster.apply(owner, level, here, (byte) 9, a.getUUID())) return Result.fail("an unknown op was accepted");
            if (!b.getUUID().equals(DoublesRoster.activeOf(owner))) return Result.fail("a refused request changed the selection");
            boolean trades = DoublesUiConfig.selectedDoubleTrades();
            if ((DoublesRoster.chosen(owner, level) == b) != trades) return Result.fail("chosen() does not follow the selectedDoubleTrades setting");
            if (DoublesRoster.chosen(other, level) != null) return Result.fail("an owner with no selection got a chosen double");
            DoublesRoster.unregister(b);
            if (DoublesRoster.activeOf(owner) != null || DoublesRoster.count(owner) != 2) return Result.fail("a double that left did not clear the selection");
            DoublesRoster.resetActive(owner);
            return Result.ok("3 doubles + 1 stranger: next/previous/pick, 4 kinds of bad request refused, leave clears the selection, selectedDoubleTrades=" + trades);
        } finally {
            for (MirrorDoubleEntity d : all) DoublesRoster.unregister(d);
            DoublesRoster.forget(owner);
            DoublesRoster.forget(other);
        }
    }

    private static Result codec(MinecraftServer server) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        UUID id = UUID.randomUUID(), sel = UUID.randomUUID();
        C2S_DoubleSelectPayload.CODEC.encode(buf, new C2S_DoubleSelectPayload(C2S_DoubleSelectPayload.SET, id));
        C2S_DoubleSelectPayload up = C2S_DoubleSelectPayload.CODEC.decode(buf);
        if (up.op() != C2S_DoubleSelectPayload.SET || !up.id().equals(id)) return Result.fail("the select packet did not survive the wire");
        List<DoubleRow> rows = new ArrayList<>();
        for (int i = 0; i < 40; i++) rows.add(new DoubleRow(i == 3 ? sel : UUID.randomUUID(), i + 1, i == 0 ? Float.NaN : 10f + i, 20f, 100 + i, 1200));
        S2C_DoublesPayload.CODEC.encode(buf, new S2C_DoublesPayload(sel, rows));
        S2C_DoublesPayload down = S2C_DoublesPayload.CODEC.decode(buf);
        if (down.rows().size() != DoublesUiMath.MAX_ROWS) return Result.fail("a 40-row packet was not cut to " + DoublesUiMath.MAX_ROWS + ": " + down.rows().size());
        if (down.rows().get(0).health() != 0f) return Result.fail("a NaN health was not cleaned");
        if (!down.active().equals(sel) || !down.rows().get(3).id().equals(sel) || down.rows().get(5).lifeLeft() != 105) return Result.fail("the roster packet changed on the wire");
        if (buf.readableBytes() != 0) {
            // the writer stops at 16 rows, so the rest of the 40 was never written: nothing may be left over
            return Result.fail(buf.readableBytes() + " bytes left over");
        }
        return Result.ok("both packets round trip; 40 rows cut to " + DoublesUiMath.MAX_ROWS + ", NaN cleaned");
    }
}
