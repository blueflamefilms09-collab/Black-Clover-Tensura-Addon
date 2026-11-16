package com.newuniverse.nusmp.skill;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.FaithGodRules;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.God;
import com.newuniverse.nusmp.skill.rules.FaithRules;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The eight Faith gods (docs/faith_gods.md): registration of the 16 skills (one Unique "Faith of X" and one god form per line, {@link FaithGodSkill}), the evolution check, the ritual that makes a Faith holder a god
 * ({@code /faith ascend <god>}) and the commands. The whole feature sits behind the SERVER setting {@code faithGods.enabled} (default true): off, the god skills do nothing, the ritual refuses and Faith is exactly the
 * 4.3.0 skill. Server-authoritative: the ritual reads the player's own skills, inventory and the block under their feet on the server and checks every argument again; nothing here ticks except the one
 * look a second of a god skill that is switched on. The only state kept is one small rate-limit map keyed by player UUID (at most {@link #MAX_CLOCKS}, cleared on logout and on server stop).
 */
public final class FaithGods {
    private FaithGods() {}

    public static final Map<String, DeferredHolder<ManasSkill, FaithGodSkill>> UNIQUE = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<ManasSkill, FaithGodSkill>> ULTIMATE = new LinkedHashMap<>();
    static final int MAX_CLOCKS = 256;
    private static final Map<UUID, Long> ASCEND_LAST = new ConcurrentHashMap<>();

    /** Registers the 16 skills; called once from {@link NUSkills}. Returns how many. */
    static int registerAll(DeferredRegister<ManasSkill> reg) {
        for (God g : FaithGodRules.GODS) {
            UNIQUE.put(g.id(), reg.register(g.uniqueId(), () -> new FaithGodSkill(g, false)));
            ULTIMATE.put(g.id(), reg.register(g.ultimateId(), () -> new FaithGodSkill(g, true)));
        }
        return UNIQUE.size() + ULTIMATE.size();
    }

    /** The master switch (SERVER setting faithGods.enabled). */
    public static boolean active() { return NUConfig.faithGodsEnabled(); }

    // ------------------------------------------------------------------ evolution (every 5 seconds, from SkillEvolution)
    /** A mastered "Faith of X" becomes its god form once the player's EP reaches the class's gate (a percent of the Nightmares-derived table, 0 = no gate); at most one per call. */
    static void evolve(ServerPlayer player, Skills skills) {
        if (!active()) return;
        for (God g : FaithGodRules.GODS) {
            ManasSkill base = UNIQUE.get(g.id()).get(), ult = ULTIMATE.get(g.id()).get();
            Optional<ManasSkillInstance> owned = skills.getSkill(base.getRegistryName());
            if (owned.isEmpty() || !owned.get().isMastered(player) || skills.getSkill(ult.getRegistryName()).isPresent()) continue;
            IExistence ex = TensuraStorages.getExistenceFrom(player);
            double ep = ex == null ? 0.0 : ex.getEP();
            if (!FaithGodRules.epReady(g, ep, NUConfig.faithGodEpGatePercent())) continue;
            SkillEvolution.evolve(player, skills, owned.get(), ult);
            return;
        }
    }

    /** How many god lines the player holds (either form). */
    static int linesHeld(Skills skills) {
        int n = 0;
        for (God g : FaithGodRules.GODS) {
            if (skills.getSkill(UNIQUE.get(g.id()).getId()).isPresent() || skills.getSkill(ULTIMATE.get(g.id()).getId()).isPresent()) n++;
        }
        return n;
    }

    private static boolean ultimateHeld(Skills skills) {
        for (ManasSkillInstance i : skills.getLearnedSkills()) if (i.getSkill() instanceof Skill s && s.getType() == Skill.SkillType.ULTIMATE) return true;
        return false;
    }

    // ------------------------------------------------------------------ the ritual
    private static Item itemOf(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
    }

    private static int count(Inventory inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static void take(Inventory inv, Item item, int amount) {
        int left = amount;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item)) continue;
            int d = Math.min(left, s.getCount());
            s.shrink(d);
            left -= d;
        }
    }

    private static boolean onShrine(ServerPlayer p) { return p.level().getBlockState(p.blockPosition().below()).is(Blocks.GOLD_BLOCK); }

    /** {@code /faith ascend <god>}: the survival way to become a god. Everything is read here on the server; returns true when the player was made a god. */
    public static boolean ascend(ServerPlayer p, String godId) {
        God g = FaithGodRules.pick(FaithGodRules.god(godId), NUConfig.faithGodCanon());      // the canon row names the offering and the ritual's worshippers
        if (g == null) { SkillUtil.fail(p, SkillMsg.t("gods.unknown", "There is no such god. /faith gods lists them.")); return false; }
        if (!p.isAlive()) return false;
        Skills skills = SkillAPI.getSkillsFrom(p);
        ManasSkillInstance faith = FaithWorship.faithOf(p);
        Item item = itemOf(g.offering());
        long now = p.level().getGameTime();
        Long last = ASCEND_LAST.get(p.getUUID());
        int worshippers = faith == null ? 0 : FaithWorship.worshippers(faith).size();
        int offered = faith == null ? 0 : FaithWorshipRules.totalWorship(FaithWorship.worshippers(faith));
        int proofs = faith == null ? 0 : FaithGodRules.proofs(ultimateHeld(skills), worshippers, offered, faith.isMastered(p), NUConfig.faithGodMinWorshippers(), NUConfig.faithGodMinOffered());
        FaithGodRules.Ritual r = FaithGodRules.ritual(active(), faith != null,
                skills.getSkill(UNIQUE.get(g.id()).getId()).isPresent() || skills.getSkill(ULTIMATE.get(g.id()).getId()).isPresent(),
                linesHeld(skills), NUConfig.faithGodMaxLines(), FaithGodRules.ritualReady(last == null ? -1L : last, now, NUConfig.faithGodAscendCooldown()),
                onShrine(p), item != null && count(p.getInventory(), item) >= g.offeringCount(), proofs,
                faith == null ? 0 : FaithSkill.getFaith(faith), NUConfig.faithGodAscendFaith());
        if (r != FaithGodRules.Ritual.OK) {
            SkillUtil.fail(p, refusal(r, g, proofs));
            return false;
        }
        if (!FaithGodRules.ritualWorshippersOk(g, worshippers)) {      // canon: Super Saiyan God needs the six righteous Saiyans (five beside the target)
            SkillUtil.fail(p, SkillMsg.t("gods.need_ritual_worshippers", "The ritual of %s needs at least %s worshippers (you have %s).", g.name(), g.canonRules().ritualWorshippers(), worshippers));
            return false;
        }
        if (ASCEND_LAST.size() >= MAX_CLOCKS) ASCEND_LAST.clear();      // only a rate limit: losing it is harmless
        ASCEND_LAST.put(p.getUUID(), now);
        take(p.getInventory(), item, g.offeringCount());
        FaithSkill.setFaith(faith, FaithRules.spend(FaithSkill.getFaith(faith), NUConfig.faithGodAscendFaith()));
        boolean learned = skills.learnSkill(UNIQUE.get(g.id()).get().createDefaultInstance(),
                SkillMsg.t("gods.ascended", "You have become the god %s.", g.name()).withStyle(ChatFormatting.GOLD));
        if (!learned) { SkillUtil.fail(p, SkillMsg.t("gods.failed", "The ritual failed.")); return false; }
        var level = p.serverLevel();
        SkillAnimeFx.sigil(level, g.sigil(), p.position(), 6.0, 0xFF000000 | g.rgb(), 40);
        SkillFx.ring(level, p.position(), 6.0, g.rgb());
        SkillFx.sound(level, p.position(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
        var server = p.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(SkillMsg.t("voice", "[Voice of the World] ").withStyle(ChatFormatting.GOLD)
                    .append(SkillMsg.t("gods.broadcast", "%s has become %s.", p.getName(), g.name()).withStyle(ChatFormatting.WHITE)), false);
        }
        return true;
    }

    private static Component refusal(FaithGodRules.Ritual r, God g, int proofs) {
        return switch (r) {
            case DISABLED -> SkillMsg.t("gods.off", "The Faith gods are switched off on this server.");
            case NOT_A_FAITH_HOLDER -> SkillMsg.t("gods.need_faith", "Only a holder of Faith or Goddess of Faith can become a god.");
            case ALREADY -> SkillMsg.t("gods.already", "You already walk the path of %s.", g.name());
            case TOO_MANY_LINES -> SkillMsg.t("gods.too_many", "You may follow only %s god line(s) at a time.", NUConfig.faithGodMaxLines());
            case COOLDOWN -> SkillMsg.t("gods.wait", "The ritual cannot be repeated yet.");
            case NO_SHRINE -> SkillMsg.t("gods.shrine", "Stand on a block of gold: it is the shrine.");
            case NO_OFFERING -> SkillMsg.t("gods.offering", "The offering is %s x%s (carried).", itemName(g.offering()), g.offeringCount());
            case FEW_PROOFS -> SkillMsg.t("gods.proofs", "Three proofs are needed (you have %s): an Ultimate skill, %s worshippers, %s Faith offered by them, Faith mastered.",
                    proofs, NUConfig.faithGodMinWorshippers(), NUConfig.faithGodMinOffered());
            default -> SkillMsg.t("gods.faith_low", "The ritual costs %s Faith.", NUConfig.faithGodAscendFaith());
        };
    }

    private static String itemName(String id) {
        Item it = itemOf(id);
        return it == null ? id : it.getDescription().getString();
    }

    /** {@code /faith gods}: one line per god (name, franchise, class, what the ritual burns). */
    public static void list(ServerPlayer p) {
        p.sendSystemMessage(SkillMsg.t("gods.list_title", "The Faith gods (/faith ascend <id>): stand on a gold block, carry the offering, prove yourself.").withStyle(ChatFormatting.GOLD));
        for (God g0 : FaithGodRules.GODS) {
            God g = FaithGodRules.pick(g0, NUConfig.faithGodCanon());
            p.sendSystemMessage(Component.literal(" " + g.id() + ": " + g.name() + " (" + g.franchise() + ", " + g.cls().key() + " class, " + g.offeringCount() + "x " + g.offering() + ")")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    // ------------------------------------------------------------------ commands
    public static void register(RegisterCommandsEvent event) {
        var ids = (com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>) (c, b) -> SharedSuggestionProvider.suggest(FaithGodRules.ids(), b);
        event.getDispatcher().register(Commands.literal("faith")
                .then(Commands.literal("gods").executes(c -> { list(c.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("ascend").then(Commands.argument("god", StringArgumentType.word()).suggests(ids)
                        .executes(c -> ascend(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "god")) ? 1 : 0))));
        event.getDispatcher().register(Commands.literal("anime_multiverse").requires(s -> s.hasPermission(2))
                .then(Commands.literal("faithgod")
                        .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("god", StringArgumentType.word()).suggests(ids)
                                        .executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "god"), false))
                                        .then(Commands.literal("ultimate").executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "god"), true))))))));
    }

    private static int give(CommandSourceStack src, ServerPlayer p, String id, boolean ultimate) {
        God g = FaithGodRules.god(id);
        if (g == null) { src.sendFailure(Component.literal("Unknown god '" + id + "'. Ids: " + String.join(", ", FaithGodRules.ids()))); return 0; }
        DeferredHolder<ManasSkill, FaithGodSkill> h = (ultimate ? ULTIMATE : UNIQUE).get(g.id());
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(h.getId()).isPresent()) { src.sendFailure(Component.literal(p.getName().getString() + " already has it.")); return 0; }
        boolean ok = skills.learnSkill(h.get().createDefaultInstance(), Component.literal("You are granted " + h.get().getName().getString() + ".").withStyle(ChatFormatting.GOLD));
        if (!ok) { src.sendFailure(Component.literal("Could not give it.")); return 0; }
        src.sendSuccess(() -> Component.literal("Gave " + h.get().getName().getString() + " to " + p.getName().getString() + "."), true);
        return 1;
    }

    // ------------------------------------------------------------------ cleanup
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { ASCEND_LAST.remove(e.getEntity().getUUID()); }

    public static void onServerStopping(ServerStoppingEvent e) { ASCEND_LAST.clear(); }

    /** For the self-test: the number of rate-limit clocks held. */
    static int openState() { return ASCEND_LAST.size(); }
}
