package com.newuniverse.nusmp.entity.riven;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.newuniverse.nusmp.skill.codex.AnimeSkillCodex;
import com.newuniverse.nusmp.skill.codex.SkillForge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /nusmp riven summon | research <anime> <ability> | accept <file> | codex  (ops only). */
public final class RivenCommands {
    private RivenCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp").then(Commands.literal("riven").requires(s -> s.hasPermission(2))
                .then(Commands.literal("summon").executes(ctx -> {
                    var r = RivenBossEntity.summon(ctx.getSource().getLevel(), ctx.getSource().getPosition());
                    ctx.getSource().sendSuccess(() -> Component.literal(r == null ? "Riven Remake could not be summoned here." : "Riven Remake takes the stage."), true);
                    return r == null ? 0 : 1;
                }))
                .then(Commands.literal("codex").executes(ctx -> {
                    int n = AnimeSkillCodex.all().size();
                    ctx.getSource().sendSuccess(() -> Component.literal("Anime Skill Codex: " + n + " skills"), false);
                    return n;
                }))
                .then(Commands.literal("research").then(Commands.argument("anime", StringArgumentType.word())
                        .then(Commands.argument("ability", StringArgumentType.greedyString()).executes(ctx -> {
                            SkillForge.research(ctx.getSource(), StringArgumentType.getString(ctx, "anime"), StringArgumentType.getString(ctx, "ability"));
                            return 1;
                        }))))
                .then(Commands.literal("accept").then(Commands.argument("file", StringArgumentType.word()).executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    if (!RivenConfig.ALLOW_INBOX.get()) { src.sendFailure(Component.literal("allowCodexInbox is off in nusmp-riven.toml.")); return 0; }
                    String err = AnimeSkillCodex.accept(StringArgumentType.getString(ctx, "file"));
                    if (err != null) { src.sendFailure(Component.literal("Not accepted: " + err)); return 0; }
                    src.sendSuccess(() -> Component.literal("Skill accepted into the live codex."), true);
                    return 1;
                })))));
    }
}
