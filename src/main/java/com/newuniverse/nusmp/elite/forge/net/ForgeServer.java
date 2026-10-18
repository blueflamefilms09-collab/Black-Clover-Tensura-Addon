package com.newuniverse.nusmp.elite.forge.net;

import com.newuniverse.nusmp.elite.forge.ForgeComponents;
import com.newuniverse.nusmp.elite.forge.ForgeConfig;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlock;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlockEntity;
import com.newuniverse.nusmp.elite.forge.craft.EngravingHandler;
import com.newuniverse.nusmp.elite.forge.craft.Inventories;
import com.newuniverse.nusmp.elite.forge.craft.SkillProbe;
import com.newuniverse.nusmp.elite.forge.data.ForgeRecipes;
import com.newuniverse.nusmp.elite.forge.rules.Category;
import com.newuniverse.nusmp.elite.forge.rules.ForgeProgress;
import com.newuniverse.nusmp.elite.forge.rules.ForgeRecipeDef;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import com.newuniverse.nusmp.elite.forge.rules.QualityRoller;
import com.newuniverse.nusmp.elite.forge.rules.SessionCheck;
import com.newuniverse.nusmp.elite.forge.rules.Specialization;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import com.newuniverse.nusmp.elite.forge.session.ForgeFinisher;
import com.newuniverse.nusmp.elite.forge.session.ForgeSessions;
import com.newuniverse.nusmp.elite.forge.store.ForgeStore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: the server side of every forge packet, in one class, so that each packet is answered by the same few checks: the system is enabled; the player is next to a
 * real station (in range, in the same level); the thing asked for exists and is allowed. The server never trusts a number from the client: {@link #onBegin} re-reads the
 * recipe, the materials and the station tier itself; {@link #onResult} takes the session out of the book first (so a report counts once) and lets
 * {@code ForgeFinisher} judge it; {@link #onSpec} and {@link #onEngrave} charge their experience levels on the server. After every change the player's screen is
 * refreshed with a new {@link ForgeNet.OpenStation} so the "have" counts and the progress are the server's. A fake player (a self-test) has no connection and gets no packets.
 */
public final class ForgeServer {
    private ForgeServer() {}

    private static boolean canSend(ServerPlayer p) { return !(p instanceof FakePlayer) && p.connection != null; }

    private static void send(ServerPlayer p, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (canSend(p)) PacketDistributor.sendToPlayer(p, payload);
    }

    private static void say(ServerPlayer p, String key, Object... args) { p.displayClientMessage(Component.translatable(key, args), true); }

    // ================================================================ views
    public static ForgeNet.Progress progressView(ServerPlayer p) {
        ForgeProgress pr = ForgeStore.of(p);
        List<Integer> by = new ArrayList<>();
        for (StationTier t : StationTier.values()) by.add(pr.craftedAt(t));
        return new ForgeNet.Progress(pr.crafted(), pr.best(), pr.topStation(), pr.specializationId(), pr.changeCosts(), ForgeConfig.specChangeLevels(), ForgeConfig.engraveLevels(),
                SkillProbe.bonus(p), List.copyOf(by));
    }

    public static ForgeNet.OpenStation openView(ServerPlayer p, BlockPos pos, StationTier tier, boolean refresh) {
        ForgeProgress pr = ForgeStore.of(p);
        List<ForgeRecipes.Loaded> list = ForgeRecipes.list();
        list.sort(Comparator.<ForgeRecipes.Loaded>comparingInt(l -> l.def().tier()).thenComparing(l -> l.def().category().ordinal()).thenComparing(l -> l.def().id()));
        List<ForgeNet.RecipeView> views = new ArrayList<>();
        for (ForgeRecipes.Loaded l : list) {
            ForgeRecipeDef d = l.def();
            List<ForgeNet.StackView> in = new ArrayList<>(), prem = new ArrayList<>();
            List<Integer> bonus = new ArrayList<>();
            for (ForgeRecipeDef.Stack s : d.inputs()) in.add(new ForgeNet.StackView(s.item(), s.count(), have(p, s.item())));
            for (ForgeRecipeDef.Premium s : d.premium()) {
                prem.add(new ForgeNet.StackView(s.item(), s.count(), have(p, s.item())));
                bonus.add(s.bonus());
            }
            List<ForgeNet.StepView> steps = new ArrayList<>();
            for (SessionCheck.Step s : d.steps()) steps.add(new ForgeNet.StepView(s.type().id(), s.difficulty()));
            boolean unlocked = !ForgeConfig.enforceTier() || tier.canForge(d.tier());
            views.add(new ForgeNet.RecipeView(d.id(), d.tier(), d.category().id(), d.output().item(), d.output().count(), List.copyOf(in), List.copyOf(prem), List.copyOf(bonus),
                    List.copyOf(steps), d.baseQuality(), d.engrave(), unlocked, l.available(), pr.knows(d.id())));
        }
        int here = p.level().getBlockEntity(pos) instanceof ForgeStationBlockEntity be ? be.forged() : 0;
        return new ForgeNet.OpenStation(pos, tier.index(), here, refresh, progressView(p), List.copyOf(views));
    }

    private static int have(ServerPlayer p, String item) {
        Item it = Inventories.item(item);
        return it == null ? 0 : Inventories.count(p, it);
    }

    /** The tier of the station the player is allowed to use at {@code pos}, or null (and a message) when there is none close enough. */
    private static StationTier stationFor(ServerPlayer p, BlockPos pos) {
        if (!ForgeConfig.enabled()) {
            say(p, "nusmp.eforge.msg.disabled");
            return null;
        }
        StationTier t = ForgeStationBlock.tierAt(p.level(), pos);
        double r = ForgeConfig.stationRange() + 1.5;
        if (t == null || p.distanceToSqr(pos.getCenter()) > r * r) {
            say(p, "nusmp.eforge.msg.too_far");
            return null;
        }
        return t;
    }

    // ================================================================ open
    /** Opens the station screen for the player (a block click). */
    public static void open(ServerPlayer p, BlockPos pos) {
        StationTier t = stationFor(p, pos);
        if (t == null) return;
        send(p, openView(p, pos, t, false));
    }

    public static void sendSync(ServerPlayer p) {
        send(p, new ForgeNet.Sync(progressView(p)));
    }

    // ================================================================ begin
    /** Result of trying to start: the session or a translation key. */
    public record Begun(ForgeSessions.Session session, String error) {}

    /** The whole start-of-session logic, callable without a packet (the self-test uses it). */
    public static Begun begin(ServerPlayer p, BlockPos pos, String recipeId, boolean premium) {
        if (!ForgeConfig.enabled()) return new Begun(null, "nusmp.eforge.msg.disabled");
        StationTier t = ForgeStationBlock.tierAt(p.level(), pos);
        double r = ForgeConfig.stationRange() + 1.5;
        if (t == null || p.distanceToSqr(pos.getCenter()) > r * r) return new Begun(null, "nusmp.eforge.msg.too_far");
        ForgeRecipes.Loaded l = ForgeRecipes.get(recipeId);
        if (l == null || !l.available()) return new Begun(null, "nusmp.eforge.msg.unknown_recipe");
        ForgeRecipeDef d = l.def();
        if (ForgeConfig.enforceTier() && !t.canForge(d.tier())) return new Begun(null, "nusmp.eforge.msg.tier");
        UUID user = ForgeSessions.userAt(p.level(), pos);
        if (user != null && !user.equals(p.getUUID())) return new Begun(null, "nusmp.eforge.msg.in_use");
        List<Inventories.Row> rows = new ArrayList<>();
        for (ForgeRecipeDef.Stack s : d.inputs()) {
            Item it = Inventories.item(s.item());
            if (it == null) return new Begun(null, "nusmp.eforge.msg.unknown_recipe");
            rows.add(new Inventories.Row(it, s.count()));
        }
        if (!Inventories.hasAll(p, rows)) return new Begun(null, "nusmp.eforge.msg.missing");
        ForgeSessions.Session old = ForgeSessions.drop(p.getUUID());
        if (old != null && p.getServer() != null) unlight(p.getServer().getLevel(old.dimension()), old.pos());
        ForgeSessions.Session s = ForgeSessions.open(p.getUUID(), p.level(), pos, t, d, premium, p.getRandom().nextLong());
        if (p.level() instanceof ServerLevel sl) ForgeStationBlock.setLit(sl, pos, true);
        return new Begun(s, null);
    }

    public static void onBegin(ServerPlayer p, ForgeNet.Begin b) {
        Begun r = begin(p, b.pos(), b.recipe(), b.premium());
        if (r.session() == null) {
            send(p, ForgeNet.Done.fail(r.error()));
            return;
        }
        ForgeSessions.Session s = r.session();
        List<ForgeNet.StepView> steps = new ArrayList<>();
        for (SessionCheck.Step st : s.steps()) steps.add(new ForgeNet.StepView(st.type().id(), st.difficulty()));
        ForgeRecipes.Loaded rec = ForgeRecipes.get(s.recipeId());
        send(p, new ForgeNet.Start(s.id(), s.pos(), s.recipeId(), rec == null ? "minecraft:air" : rec.def().output().item(), s.tier().index(), List.copyOf(steps), s.seeds()));
    }

    // ================================================================ result
    public static void onResult(ServerPlayer p, ForgeNet.Result r) {
        ForgeSessions.Session s = ForgeSessions.take(p.getUUID(), r.session());
        if (s == null) {
            send(p, ForgeNet.Done.fail("nusmp.eforge.msg.no_session"));
            return;
        }
        ForgeFinisher.Outcome o = ForgeFinisher.finish(p, s, r.inputs());
        if (p.getServer() != null) unlight(p.getServer().getLevel(s.dimension()), s.pos());
        reply(p, s.pos(), o);
    }

    /** Answers a finished attempt: the Done packet, a refreshed screen, and a chat line on a refusal. */
    public static void reply(ServerPlayer p, BlockPos pos, ForgeFinisher.Outcome o) {
        if (!o.ok()) {
            if (!o.reason().isEmpty()) p.sendSystemMessage(Component.translatable("nusmp.eforge.msg.rejected_why", o.reason()));
            send(p, ForgeNet.Done.fail(o.message()));
        } else {
            Quality q = o.roll().quality();
            List<String> eng = new ArrayList<>();
            for (EngravingHandler.Applied a : o.engravings()) eng.add(a.id() + " " + a.level());
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.stack().getItem()).toString();
            send(p, new ForgeNet.Done(true, o.message(), q.level(), o.score(), id, o.stack().getCount(), (int) Math.round(o.roll().points() * 10), o.roll().cappedByStation(), List.copyOf(eng)));
            p.sendSystemMessage(Component.translatable("nusmp.eforge.msg.forged", o.stack().getHoverName(), Component.translatable("nusmp.eforge.quality." + q.id()).withStyle(style -> style.withColor(q.rgb()))));
        }
        StationTier t = ForgeStationBlock.tierAt(p.level(), pos);
        if (t != null) send(p, openView(p, pos, t, true));
        sendSync(p);
    }

    static void unlight(ServerLevel level, BlockPos pos) {
        if (level != null && !ForgeSessions.activeAt(level, pos)) ForgeStationBlock.setLit(level, pos, false);
    }

    // ================================================================ abort
    public static void onAbort(ServerPlayer p, ForgeNet.Abort a) {
        ForgeSessions.Session s = ForgeSessions.take(p.getUUID(), a.session());
        if (s != null && p.getServer() != null) unlight(p.getServer().getLevel(s.dimension()), s.pos());
    }

    // ================================================================ specialization
    /** Why a specialization cannot be chosen now, or null; a change costs levels. */
    public static String specRefusal(ServerPlayer p, Specialization s) {
        ForgeProgress pr = ForgeStore.of(p);
        if (s == null) return "nusmp.eforge.spec.unknown";
        if (pr.specialization() == s) return "nusmp.eforge.spec.same";
        if (pr.changeCosts() && !p.getAbilities().instabuild && p.experienceLevel < ForgeConfig.specChangeLevels()) return "nusmp.eforge.spec.cost";
        return null;
    }

    /** Chooses the specialization for the player, charging the change cost. Returns null on success, else a translation key. */
    public static String chooseSpecialization(ServerPlayer p, Specialization s) {
        String why = specRefusal(p, s);
        if (why != null) return why;
        ForgeProgress pr = ForgeStore.of(p);
        boolean change = pr.changeCosts();
        if (change && !p.getAbilities().instabuild) p.giveExperienceLevels(-ForgeConfig.specChangeLevels());
        pr.setSpecialization(s);
        ForgeStore.mirror(p);
        return null;
    }

    public static void onSpec(ServerPlayer p, ForgeNet.Spec s) {
        StationTier t = stationFor(p, s.pos());
        if (t == null) return;
        Specialization spec = Specialization.byId(s.spec());
        String why = chooseSpecialization(p, spec);
        if (why != null) {
            say(p, why, ForgeConfig.specChangeLevels());
            return;
        }
        say(p, "nusmp.eforge.spec.chosen", Component.translatable("nusmp.eforge.spec." + spec.id()));
        send(p, openView(p, s.pos(), t, true));
        sendSync(p);
    }

    // ================================================================ engrave
    /** The category a forged stack was made in (defaults to weapon for an item without the component). */
    public static Category kindOf(ItemStack stack) {
        String k = stack.get(ForgeComponents.KIND.get());
        Category c = k == null ? null : Category.byId(k);
        return c == null ? Category.WEAPON : c;
    }

    /** Engraves one more engraving on the forged item in the main hand; returns null on success or a translation key. */
    public static String engraveHeld(ServerPlayer p) {
        ItemStack stack = p.getMainHandItem();
        Quality q = ForgeComponents.qualityOf(stack);
        if (q == null) return "nusmp.eforge.engrave.not_forged";
        ForgeProgress pr = ForgeStore.of(p);
        Specialization spec = pr.specialization();
        Category c = kindOf(stack);
        if (EngravingHandler.freeSlots(stack, q, spec, c) <= 0) return "nusmp.eforge.engrave.no_slot";
        int cost = ForgeConfig.engraveLevels();
        if (!p.getAbilities().instabuild && p.experienceLevel < cost) return "nusmp.eforge.engrave.cost";
        List<EngravingHandler.Applied> done = EngravingHandler.engrave(p.registryAccess(), stack, q, spec, c, 1, p.getRandom().nextLong());
        if (done.isEmpty()) return "nusmp.eforge.engrave.none_fit";
        if (!p.getAbilities().instabuild) p.giveExperienceLevels(-cost);
        p.getInventory().setChanged();
        return null;
    }

    public static void onEngrave(ServerPlayer p, ForgeNet.Engrave e) {
        StationTier t = stationFor(p, e.pos());
        if (t == null) return;
        String why = engraveHeld(p);
        if (why != null) {
            say(p, why, ForgeConfig.engraveLevels());
            return;
        }
        say(p, "nusmp.eforge.engrave.done");
        send(p, openView(p, e.pos(), t, true));
    }

    /** Used by the info command: a one-line description of the roll a perfect play would give. */
    public static String preview(ServerPlayer p, StationTier tier, ForgeRecipeDef d) {
        Specialization spec = ForgeStore.of(p).specialization();
        QualityRoller.Result r = QualityRoller.roll(new QualityRoller.Inputs(1.0, tier, d.baseQuality(), 0, SkillProbe.bonus(p), spec == null ? 0 : spec.categoryPoints(d.category()), 0.0));
        return r.quality().id() + " (" + Math.round(r.points()) + ")";
    }
}
