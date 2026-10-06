package com.newuniverse.nusmp.blackclover;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.newuniverse.nusmp.skill.GrimoireMagicSkill;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.ChatFormatting;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /nusmp grimoire info <player>
 * /nusmp grimoire roll <player>          (rolls again, normal odds)
 * /nusmp grimoire give <player> <3|4|5>  (guaranteed grimoire)
 * /nusmp grimoire reset <player>         (removes it; they roll again automatically)
 */
public final class GrimoireCommand {
    private GrimoireCommand() {}

    public static void register(RegisterCommandsEvent event) {
        // Players: accept a devil contract (only while holding a Devil Contract).
        event.getDispatcher().register(Commands.literal("devilpact").then(Commands.literal("accept")
                .then(Commands.argument("price", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> { b.suggest("maxhp"); b.suggest("seal"); b.suggest("tax"); return b.buildFuture(); })
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            var contract = com.newuniverse.nusmp.item.NUItems.DEVIL_CONTRACT.get();
                            net.minecraft.world.item.ItemStack held = p.getMainHandItem().is(contract) ? p.getMainHandItem() : p.getOffhandItem();
                            if (!held.is(contract)) { ctx.getSource().sendFailure(Component.literal("Hold a Devil Contract.")); return 0; }
                            String price = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "price");
                            if (!com.newuniverse.nusmp.book.ForbiddenMagic.bind(p, price)) return 0;
                            held.shrink(1);
                            return 1;
                        }))));
        event.getDispatcher().register(Commands.literal("nusmp").requires(s -> s.hasPermission(2))
                .then(Commands.literal("grimoire")
                        .then(Commands.literal("info").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            var skills = SkillAPI.getSkillsFrom(p);
                            var inst = NUSkills.allGrimoireSkillIds().stream().map(skills::getSkill)
                                    .filter(java.util.Optional::isPresent).map(java.util.Optional::get).findFirst();
                            String soul = NightmareSouls.soulTypeOf(p);
                            String text = p.getName().getString() + " | soul type: " + (soul == null ? "not assigned" : soul)
                                    + " | rolled: " + GrimoireAcceptance.hasRolled(p) + " | kills: " + GrimoirePages.kills(p) + " | "
                                    + inst.map(i -> GrimoireMagicSkill.describe(p, i)).orElse("no grimoire");
                            ctx.getSource().sendSuccess(() -> Component.literal(text), false);
                            return 1;
                        })))
                        .then(Commands.literal("roll").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            int leaves = GrimoireAcceptance.roll(p, NightmareSouls.soulTypeOf(p));
                            ctx.getSource().sendSuccess(() -> Component.literal("Rolled: " + (leaves == 0 ? "no grimoire" : leaves + " leaves")), true);
                            return leaves;
                        })))
                        .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("leaves", IntegerArgumentType.integer(3, 5)).executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    GrimoireAcceptance.grant(p, IntegerArgumentType.getInteger(ctx, "leaves"), NightmareSouls.soulTypeOf(p));
                                    return 1;
                                }))))
                        .then(Commands.literal("setcover").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("cover", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((c, b) -> { for (GrimoireCover g : GrimoireCover.values()) b.suggest(g.name().toLowerCase()); return b.buildFuture(); })
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            GrimoireCover cover = GrimoireCover.byName(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "cover").toUpperCase(), 3);
                                            var g = GrimoirePages.grimoireOf(p);
                                            if (g.isEmpty()) { ctx.getSource().sendFailure(Component.literal("No grimoire.")); return 0; }
                                            Devil devil = Devil.byName(g.get().getOrCreateTag().getString("Devil"));
                                            if (cover.isForbidden() && devil == null) devil = GrimoireAcceptance.randomDevil(p.getRandom());
                                            GrimoireAcceptance.grantExact(p, cover, GrimoirePages.magicOf(g.get()), cover.isForbidden() ? devil : null);
                                            return 1;
                                        }))))
                        .then(Commands.literal("addkills").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("kills", IntegerArgumentType.integer(1, 100000)).executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    GrimoirePages.addKills(p, IntegerArgumentType.getInteger(ctx, "kills"));
                                    ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " grimoire kills: " + GrimoirePages.kills(p)), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("page").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("page", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            String arg = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "page").trim();
                                            var g = GrimoirePages.grimoireOf(p);
                                            if (g.isPresent() && g.get().getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook book) {
                                                int mode = -1;
                                                try { mode = Integer.parseInt(arg) - 1; } catch (NumberFormatException ignored) {
                                                    for (int m = 0; m < book.familyCount(); m++) {
                                                        var pg = book.page(m);
                                                        if (pg.id().equalsIgnoreCase(arg) || pg.name().equalsIgnoreCase(arg)) mode = m;
                                                    }
                                                }
                                                if (mode >= 0 && mode < book.familyCount()) {
                                                    com.newuniverse.nusmp.book.GrimoireBook.unlock(g.get(), mode);
                                                    String name = book.page(mode).name();
                                                    ctx.getSource().sendSuccess(() -> Component.literal("Unlocked page: " + name), true);
                                                    return 1;
                                                }
                                                StringBuilder list = new StringBuilder("Pages: ");
                                                for (int m = 0; m < book.familyCount(); m++) list.append(m + 1).append("=").append(book.page(m).id()).append(m + 1 < book.familyCount() ? ", " : "");
                                                ctx.getSource().sendFailure(Component.literal("No page '" + arg + "'. " + list));
                                                return 0;
                                            }
                                            var rl = net.minecraft.resources.ResourceLocation.tryParse(arg);
                                            var skill = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
                                            if (skill == null) { ctx.getSource().sendFailure(Component.literal("Unknown page: " + arg)); return 0; }
                                            GrimoirePages.grant(p, skill);
                                            return 1;
                                        }))))
                        .then(Commands.literal("spirit").then(Commands.literal("release")
                                .then(Commands.argument("spirit", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((c, b) -> { for (String n : new String[]{"Salamander", "Undine", "Sylph", "Gnome"}) b.suggest(n); return b.buildFuture(); })
                                        .executes(ctx -> {
                                            String sp = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "spirit");
                                            boolean ok = com.newuniverse.nusmp.book.SpiritSlots.get(ctx.getSource().getServer()).release(sp);
                                            ctx.getSource().sendSuccess(() -> Component.literal(ok ? sp + " is free to choose a new mage." : sp + " had no mage."), true);
                                            return ok ? 1 : 0;
                                        }))))
                        .then(Commands.literal("awaken_anti").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            boolean ok = SkillAPI.getSkillsFrom(p).learnSkill(NUSkills.ANTI_MAGIC_LORD.get().createDefaultInstance(),
                                    Component.literal("You have become an Anti-Magic Spirit Lord.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
                            ctx.getSource().sendSuccess(() -> Component.literal(ok ? "Awakened " + p.getName().getString() : "Already awakened"), true);
                            return ok ? 1 : 0;
                        })))
                        .then(Commands.literal("spirit_info").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            var tr = com.newuniverse.nusmp.book.SpiritBond.trait(com.newuniverse.nusmp.book.ForbiddenMagic.data(p).getString("nusmp_spirit_trait"));
                            String msg = !com.newuniverse.nusmp.book.SpiritBond.bonded(p) ? p.getName().getString() + " has no spirit bond."
                                    : p.getName().getString() + ": trait " + (tr == null ? "?" : tr.name()) + ", trust " + com.newuniverse.nusmp.book.SpiritBond.trust(p)
                                    + "/100, fatigue stage " + com.newuniverse.nusmp.book.SpiritBond.fatigue(p)
                                    + (com.newuniverse.nusmp.book.SpiritBond.incarnate(p) ? ", incarnated as " + com.newuniverse.nusmp.book.SpiritBond.trueName(p) : "");
                            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                            return 1;
                        })))
                        .then(Commands.literal("spirit_trust").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100)).executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    com.newuniverse.nusmp.book.SpiritBond.addTrust(p, IntegerArgumentType.getInteger(ctx, "amount"), "admin");
                                    ctx.getSource().sendSuccess(() -> Component.literal("Trust now " + com.newuniverse.nusmp.book.SpiritBond.trust(p)), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("spirit_lord").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("kind", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((c, b) -> { for (var k : com.newuniverse.nusmp.entity.SpiritLordEntity.Kind.values()) b.suggest(k.name().toLowerCase()); return b.buildFuture(); })
                                        .executes(ctx -> {
                                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                            var k = com.newuniverse.nusmp.entity.SpiritLordEntity.Kind.byName(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "kind").toUpperCase());
                                            com.newuniverse.nusmp.entity.SpiritLordEntity.toggle(p, k);
                                            return 1;
                                        }))))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            for (var id : NUSkills.allGrimoireSkillIds()) SkillAPI.getSkillsFrom(p).forgetSkill(id);
                            GrimoireAcceptance.markRolled(p, false);
                            ctx.getSource().sendSuccess(() -> Component.literal("Reset " + p.getName().getString() + "'s grimoire."), true);
                            return 1;
                        })))));
    }
}
