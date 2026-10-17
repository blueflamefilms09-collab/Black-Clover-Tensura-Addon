package com.newuniverse.nusmp.multiverse;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.GrimoireAcceptance;
import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.NightmareSouls;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpiritBond;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Every player-facing command lives under /multiverse. Admin commands need permission level 2; squad, rank info, stars info,
 * spirit info and locate are open to players (about themselves). The old /nusmp commands stay registered for old habits.
 */
public final class MultiverseCommands {
    private MultiverseCommands() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    private static int ok(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    /** 0.48: an event prize - the Black Magic grimoire with Anti-Magic (Liebe), bound at once. */
    private static int eventAntiMagic(CommandContext<CommandSourceStack> ctx, boolean replace) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        if (!replace && GrimoirePages.grimoireOf(p).isPresent())
            return fail(ctx, p.getName().getString() + " already has a grimoire. Add 'replace' to swap it for Anti-Magic.");
        GrimoireAcceptance.grantExact(p, GrimoireCover.BLACK_MAGIC, MagicType.ANTI_MAGIC, com.newuniverse.nusmp.blackclover.Devil.LIEBE);
        if (AntiMagic.book(p).isEmpty()) return fail(ctx, "The grant did not go through.");
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal("A black grimoire has chosen " + p.getName().getString() + ". Anti-Magic.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD), false);
        MultiverseSync.markDirty(p);
        return ok(ctx, "Granted Anti-Magic to " + p.getName().getString() + " (event).");
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    /** The named player if given and the source is an admin, else the source player. */
    private static ServerPlayer targetOrSelf(CommandContext<CommandSourceStack> ctx, boolean hasArg) throws CommandSyntaxException {
        return hasArg ? EntityArgument.getPlayer(ctx, "player") : ctx.getSource().getPlayerOrException();
    }

    /** /multiverse grimoire canon <player> <book> [copy]: binds the canon book (player without a grimoire) or gives an unbound copy. */
    private static int giveCanon(CommandContext<CommandSourceStack> ctx, boolean copy) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        CanonBook book = CanonBook.byId(StringArgumentType.getString(ctx, "book"));
        if (book == null) return fail(ctx, "Unknown canon grimoire. Try one of the suggestions.");
        if (copy) {
            p.getInventory().placeItemBackInInventory(GrimoireItem.createCanon(book));
            return ok(ctx, "Gave " + p.getName().getString() + " an unbound copy of " + book.owner + "'s grimoire.");
        }
        if (GrimoirePages.grimoireOf(p).isPresent()) return fail(ctx, p.getName().getString() + " already has a grimoire. /multiverse ceremony reset first, or add 'copy'.");
        MagicType magic = MagicType.byName(book.magic);
        GrimoireCover cover = GrimoireCover.byName(book.cover, 3);
        GrimoireAcceptance.grantExact(p, cover, magic, cover.isForbidden()
                ? (magic == MagicType.ANTI_MAGIC ? com.newuniverse.nusmp.blackclover.Devil.LIEBE : com.newuniverse.nusmp.blackclover.Devil.MEGICULA) : null);
        if (GrimoirePages.grimoireOf(p).isEmpty()) return fail(ctx, "The grant did not go through.");
        GrimoireItem.applyCanon(p, magic, book.id());
        MultiverseProfile.setAccepted(p, true, book.owner + "'s grimoire");
        MultiverseSync.markDirty(p);
        return ok(ctx, book.owner + "'s grimoire chose " + p.getName().getString() + ".");
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("multiverse");

        // ---------------------------------------------------------------- grimoire
        root.then(Commands.literal("grimoire").requires(MultiverseCommands::admin)
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    if (GrimoirePages.grimoireOf(p).isPresent()) return fail(ctx, p.getName().getString() + " already has a grimoire. /multiverse ceremony reset first.");
                    return Ceremony.choose(p, "the hands of the Wizard King") ? ok(ctx, "A grimoire chose " + p.getName().getString() + ".") : fail(ctx, "The grant did not go through.");
                })))
                .then(Commands.literal("page").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("page", StringArgumentType.greedyString()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            String arg = StringArgumentType.getString(ctx, "page").trim();
                            var g = GrimoirePages.grimoireOf(p);
                            if (g.isEmpty() || !(g.get().getSkill() instanceof GrimoireBook book)) return fail(ctx, "No grimoire.");
                            int mode = -1;
                            try { mode = Integer.parseInt(arg) - 1; } catch (NumberFormatException ignored) {
                                for (int m = 0; m < book.familyCount(); m++) {
                                    var pg = book.page(m);
                                    if (pg.id().equalsIgnoreCase(arg) || pg.name().equalsIgnoreCase(arg)) mode = m;
                                }
                            }
                            if (mode < 0 || mode >= book.familyCount()) {
                                StringBuilder list = new StringBuilder("Pages: ");
                                for (int m = 0; m < book.familyCount(); m++) list.append(m + 1).append('=').append(book.page(m).id()).append(m + 1 < book.familyCount() ? ", " : "");
                                return fail(ctx, "No page '" + arg + "'. " + list);
                            }
                            GrimoireBook.unlock(g.get(), mode);
                            MultiverseSync.markDirty(p);
                            return ok(ctx, "Unlocked page " + book.page(mode).name() + " for " + p.getName().getString() + " (admin; ignores mastery and gates).");
                        }))))
                .then(Commands.literal("awaken_anti").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");          // 0.48: the Lord awakens inside the Anti-Magic grimoire
                    if (AntiMagic.book(p).isEmpty()) return fail(ctx, p.getName().getString() + " has no Anti-Magic grimoire (/multiverse event antimagic).");
                    boolean done = AntiMagic.awaken(p);
                    MultiverseSync.markDirty(p);
                    return done ? ok(ctx, "Awakened " + p.getName().getString() + ".") : fail(ctx, "Already awakened.");
                })))
                // a named canon grimoire: bound to the player if they have none yet, else handed over as an unbound copy
                .then(Commands.literal("canon").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("book", StringArgumentType.word())
                                .suggests((c, b) -> { for (CanonBook k : CanonBook.values()) b.suggest(k.id()); return b.buildFuture(); })
                                .executes(ctx -> giveCanon(ctx, false))
                                .then(Commands.literal("copy").executes(ctx -> giveCanon(ctx, true)))))));

        // ---------------------------------------------------------------- grimoire slot (players)
        root.then(Commands.literal("slot").executes(ctx -> {
            com.newuniverse.nusmp.blackclover.GrimoireSlot.open(ctx.getSource().getPlayerOrException());
            return 1;
        }));

        // ---------------------------------------------------------------- ceremony
        root.then(Commands.literal("ceremony").requires(MultiverseCommands::admin)
                .then(Commands.literal("start").executes(ctx -> { Ceremony.start(ctx.getSource().getServer()); return 1; }))
                .then(Commands.literal("stop").executes(ctx -> { Ceremony.stop(ctx.getSource().getServer()); return 1; }))
                .then(Commands.literal("accept").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    if (GrimoirePages.grimoireOf(p).isPresent()) return fail(ctx, p.getName().getString() + " already has a grimoire.");
                    return Ceremony.choose(p, "the Acceptance Ceremony") ? ok(ctx, p.getName().getString() + " was chosen.") : fail(ctx, "The grant did not go through.");
                })))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    Ceremony.reset(p);
                    return ok(ctx, "Reset " + p.getName().getString() + ": no grimoire, eligible for the ceremony again.");
                })))
                .then(Commands.literal("status").executes(ctx -> ok(ctx, "Ceremony " + (Ceremony.isActive(ctx.getSource().getServer()) ? "ACTIVE" : "inactive")
                        + " (month " + MultiverseConfig.get(MultiverseConfig.CEREMONY_MONTH) + ").")))
                .then(Commands.literal("legacy_roll").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    int leaves = GrimoireAcceptance.roll(p, NightmareSouls.soulTypeOf(p));
                    return ok(ctx, "Old-style roll: " + (leaves == 0 ? "no grimoire" : leaves + " leaves"));
                }))));

        // ---------------------------------------------------------------- spirit
        var spirit = Commands.literal("spirit");
        spirit.then(Commands.literal("bond").requires(MultiverseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> { for (String t : SpiritLordSkill.TYPES) b.suggest(t.toLowerCase()); return b.buildFuture(); })
                        .executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            String why = SpiritLordSkill.bond(p, StringArgumentType.getString(ctx, "type"));
                            return why == null ? ok(ctx, p.getName().getString() + " is bound to " + SpiritLordSkill.bondedType(p) + ".") : fail(ctx, why);
                        }))));
        spirit.then(Commands.literal("incarnate").requires(MultiverseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    boolean done = SpiritBond.forceIncarnate(p, StringArgumentType.getString(ctx, "name"));
                    MultiverseSync.markDirty(p);
                    return done ? ok(ctx, "Incarnated.") : fail(ctx, p.getName().getString() + " has no spirit bond.");
                }))));
        spirit.then(Commands.literal("info").executes(ctx -> spiritInfo(ctx, false))
                .then(Commands.argument("player", EntityArgument.player()).requires(MultiverseCommands::admin).executes(ctx -> spiritInfo(ctx, true))));
        root.then(spirit);

        // ---------------------------------------------------------------- anti-magic
        // 0.48: event grants. Anti-Magic is event-only: it is never rolled, an admin hands it out (replace = over an existing grimoire)
        root.then(Commands.literal("event").requires(MultiverseCommands::admin)
                .then(Commands.literal("antimagic").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> eventAntiMagic(ctx, false))
                        .then(Commands.literal("replace").executes(ctx -> eventAntiMagic(ctx, true))))));

        // 0.47: the Zagred boss (server config zagredBossEnabled, off by default)
        var bossNode = Commands.literal("boss").requires(MultiverseCommands::admin)
                .then(Commands.literal("zagred").executes(ctx -> {
                    if (!com.newuniverse.nusmp.NUGameRules.zagredBoss(ctx.getSource().getLevel()))
                        return fail(ctx, "The Zagred boss is off. Turn it on with /gamerule nusmpZagredBoss true (or zagredBossEnabled in the server config).");
                    var z = com.newuniverse.nusmp.entity.ZagredBossEntity.summon(ctx.getSource().getLevel(), ctx.getSource().getPosition());
                    return z == null ? fail(ctx, "Zagred could not be summoned here.") : ok(ctx, "Zagred descends. The arena is here.");
                }))
                // 0.53: Sylph, the Wind Spirit Lord (no game rule: an admin starts it where they stand)
                .then(Commands.literal("sylph").executes(ctx -> {
                    var s = com.newuniverse.nusmp.entity.WindSpiritLordEntity.summon(ctx.getSource().getLevel(), ctx.getSource().getPosition());
                    return s == null ? fail(ctx, "Sylph could not be summoned here.") : ok(ctx, "Sylph, the Wind Spirit Lord, rides in on the gale.");
                }));
        com.newuniverse.nusmp.dungeon.floor.goliath.GoliathCommand.addTo(bossNode);                                       // 1.23.0: goliath [rise]
        com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisCommand.addTo(bossNode);                                    // 1.23.0
        com.newuniverse.nusmp.entity.avatar.AvatarCommand.addTo(bossNode);                           // 1.22.0: avatar [lucifero|megicula]
        com.newuniverse.nusmp.entity.zogratis.ZogratisCommand.addTo(bossNode);                       // 1.20.0: dante | vanica | zenon | lucius | triad
        com.newuniverse.nusmp.relics.RelicCommands.addTo(root);
        com.newuniverse.nusmp.relics.tier.TierCommands.addTo(root);                                                       // 1.30.0: relic tier                                                       // 1.24.0: relic list|info|give|attune|unattune|grade
        com.newuniverse.nusmp.relics.order.RelicOrderCommand.addTo(root);                                              // relic order census|enforce|status|release|summon
        com.newuniverse.nusmp.relics.djinn.DjinnCommand.addTo(root);                                                   // djinn status|release|share|grant|phase
        root.then(com.newuniverse.nusmp.compat.CompatCommand.node());                                    // 1.33.0: compat book|status
        root.then(com.newuniverse.nusmp.core.magic.merge.MergeCommand.node());                                          // 1.27.0: magic merge|list|info|remove|grant|revoke|audit
        com.newuniverse.nusmp.charter.CharterCommand.addTo(root);                                              // 1.25.0: charter status|give|reset
        root.then(bossNode);
        root.then(com.newuniverse.nusmp.labyrinth.mega.MegaCommands.node());                                           // 1.54.0: mega status|sector|tp|expand|regen|audit|plan|unseal|rooms|pools + dm|faction|rest|core|floors|system|flow|gear|selftest

        root.then(Commands.literal("antimode").requires(MultiverseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("mode", IntegerArgumentType.integer(0, 3)).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    var lord = AntiMagic.lord(p);
                    if (lord.isEmpty()) return fail(ctx, p.getName().getString() + " has no Anti-Magic.");
                    int mode = IntegerArgumentType.getInteger(ctx, "mode");
                    MultiverseProfile.setAntiMode(p, mode);
                    boolean form = mode >= 2;            // Black Form for modes 2 and 3
                    if (lord.get().getSkill() instanceof com.newuniverse.nusmp.book.AntiMagicBook) {          // 0.48: the merged Lord
                        if (com.newuniverse.nusmp.book.AntiMagicBook.inBlackForm(lord.get()) != form) com.newuniverse.nusmp.book.AntiMagicBook.setForm(p, lord.get(), form);
                    } else if (lord.get().isToggled() != form) {
                        lord.get().setToggled(form);
                        if (form) lord.get().getSkill().onToggleOn(lord.get(), p); else lord.get().getSkill().onToggleOff(lord.get(), p);
                        lord.get().markDirty();
                    }
                    return ok(ctx, p.getName().getString() + " anti-magic mode: " + MultiverseProfile.ANTI_MODES[mode]);
                }))));

        // ---------------------------------------------------------------- rank
        root.then(Commands.literal("rank")
                .then(Commands.literal("set").requires(MultiverseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .suggests((c, b) -> { for (KnightRank r : KnightRank.values()) b.suggest(r.name().toLowerCase()); return b.buildFuture(); })
                                .executes(ctx -> setRank(ctx, 5))
                                .then(Commands.argument("class", IntegerArgumentType.integer(1, 5)).executes(ctx -> setRank(ctx, IntegerArgumentType.getInteger(ctx, "class")))))))
                .then(Commands.literal("info").executes(ctx -> rankInfo(ctx, false))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> rankInfo(ctx, true)))));

        // ---------------------------------------------------------------- stars
        root.then(Commands.literal("stars")
                .then(Commands.literal("add").requires(MultiverseCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000)).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            int n = IntegerArgumentType.getInteger(ctx, "amount");
                            MultiverseProfile.addStars(p, n);
                            return ok(ctx, (n >= 0 ? "+" + n + " gold" : -n + " black") + " stars for " + p.getName().getString()
                                    + " (net " + MultiverseProfile.netStars(p) + ").");
                        }))))
                .then(Commands.literal("info").executes(ctx -> starsInfo(ctx, false))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> starsInfo(ctx, true)))));

        // ---------------------------------------------------------------- profile (social class & race)
        root.then(Commands.literal("profile").requires(MultiverseCommands::admin)
                .then(Commands.literal("class").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", StringArgumentType.word())
                                .suggests((c, b) -> { for (SocialClass s : SocialClass.values()) b.suggest(s.name().toLowerCase()); return b.buildFuture(); })
                                .executes(ctx -> {
                                    SocialClass c = SocialClass.byName(StringArgumentType.getString(ctx, "value"));
                                    if (c == null) return fail(ctx, "Royalty, Noble, Commoner or Peasant.");
                                    MultiverseProfile.setSocialClass(EntityArgument.getPlayer(ctx, "player"), c);
                                    return ok(ctx, "Social class: " + c.displayName);
                                }))))
                // races are the player's Tensura race (with this mod's buff on top); only Devil is set here, by command
                .then(Commands.literal("race").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", StringArgumentType.word())
                                .suggests((c, b) -> { b.suggest("devil"); b.suggest("none"); return b.buildFuture(); })
                                .executes(ctx -> {
                                    String v = StringArgumentType.getString(ctx, "value").toLowerCase();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    if (!v.equals("devil") && !v.equals("none"))
                                        return fail(ctx, "Races come from Tensura; only 'devil' (or 'none' to undo it) can be set here.");
                                    MultiverseProfile.setDevil(target, v.equals("devil"));
                                    MultiverseSync.markDirty(target);
                                    return ok(ctx, target.getName().getString() + " is now: " + RaceBuffs.displayName(target));
                                }))))
                .then(Commands.literal("eligible").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(ctx -> {
                            boolean v = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value");
                            MultiverseProfile.setEligible(EntityArgument.getPlayer(ctx, "player"), v);
                            return ok(ctx, "Ceremony eligibility flag: " + v);
                        })))));

        // ---------------------------------------------------------------- squads
        root.then(Commands.literal("squad")
                .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.greedyString()).executes(ctx -> {
                    Squads.create(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"));
                    return 1;
                })))
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    Squads.invite(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"));
                    return 1;
                })))
                .then(Commands.literal("leave").executes(ctx -> { Squads.leave(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("info").executes(ctx -> {
                    for (Component c : Squads.info(ctx.getSource().getPlayerOrException())) ctx.getSource().sendSuccess(() -> c, false);
                    return 1;
                }))
                .then(Commands.literal("standings").executes(ctx -> {
                    if (!Squads.available()) { ctx.getSource().sendFailure(Squads.REQUIRES_FTB); return 0; }
                    Squads.prune(ctx.getSource().getServer());
                    var all = Squads.standings(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Star Awards - squad standings").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                    if (all.isEmpty()) ctx.getSource().sendSuccess(() -> Component.literal(" No squads yet.").withStyle(ChatFormatting.GRAY), false);
                    int place = 1;
                    for (var st : all) {
                        int pl = place++;
                        ctx.getSource().sendSuccess(() -> Component.literal(" #" + pl + " " + st.name() + "  " + st.score() + " stars  (" + st.members() + " knights)")
                                .withStyle(pl == 1 ? ChatFormatting.GOLD : ChatFormatting.WHITE), false);
                    }
                    return all.size();
                }))
                .then(Commands.literal("setcaptain").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    Squads.setCaptain(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"));
                    return 1;
                })))
                .then(Commands.literal("setvice").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    Squads.setVice(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"));
                    return 1;
                }))));

        // ---------------------------------------------------------------- locate
        root.then(Commands.literal("locate")
                .then(Commands.literal("tower").executes(ctx -> locate(ctx, WorldSites.Kind.TOWER)))
                .then(Commands.literal("ruins").executes(ctx -> locate(ctx, WorldSites.Kind.RUINS))));

        root.then(com.newuniverse.nusmp.labyrinth.gen.LabyrinthCommand.build());                              // 1.35.0: locate / enter / leave / here (palette went with the Spire, 1.58.0)
        var dungeonNode = Commands.literal("dungeon");                                                       // 1.58.0: all that is left of /multiverse dungeon ... is the Treasury relics (they keep their word)
        com.newuniverse.nusmp.dungeon.nexus.relic.RelicCommand.addTo(dungeonNode);                           // /multiverse dungeon relic give | attune | release | status
        root.then(dungeonNode);
        root.then(Commands.literal("wizard").requires(MultiverseCommands::admin)                              // 1.6.0: the Tower Wizard
                .then(Commands.literal("summon").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    com.newuniverse.nusmp.wizard.WizardSystem.summon(p);
                    return ok(ctx, "A summons for " + p.getName().getString() + ".");
                })))
                .then(Commands.literal("spawn").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var w = com.newuniverse.nusmp.entity.NUEntities.TOWER_WIZARD.get().create(p.serverLevel());
                    if (w == null) return fail(ctx, "Could not create the wizard.");
                    w.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0f);
                    p.serverLevel().addFreshEntity(w);
                    return ok(ctx, "The Tower Wizard appears.");
                })));
        root.then(Commands.literal("wizardking")                                                           // 1.6.0: the one-holder title
                .then(Commands.literal("info").executes(ctx -> {
                    var s = ctx.getSource().getServer();
                    return ok(ctx, WizardKing.holder(s).isPresent() ? "The Wizard King is " + WizardKing.holderName(s) + "." : "The crown is vacant.");
                }))
                .then(Commands.literal("claim").executes(ctx -> {
                    String why = WizardKing.claim(ctx.getSource().getPlayerOrException());
                    return why == null ? 1 : fail(ctx, why);
                }))
                .then(Commands.literal("appoint").requires(MultiverseCommands::admin).then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    WizardKing.appoint(ctx.getSource().getServer(), p);
                    return 1;
                })))
                .then(Commands.literal("dethrone").requires(MultiverseCommands::admin).executes(ctx -> { WizardKing.dethrone(ctx.getSource().getServer()); return 1; })));
        root.then(com.newuniverse.nusmp.mana.ManaCommands.build());                                           // 1.5.0: the mana system
        root.then(Commands.literal("choose").then(Commands.argument("magic", StringArgumentType.word()).executes(ctx -> {    // 1.5.0: the ceremony's choice
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            return Ceremony.pick(p, StringArgumentType.getString(ctx, "magic")) ? 1 : fail(ctx, "There is no such choice waiting for you.");
        })));
        d.register(root);
    }

    // ---------------------------------------------------------------- handlers
    private static int setRank(CommandContext<CommandSourceStack> ctx, int cls) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        KnightRank r = KnightRank.byName(StringArgumentType.getString(ctx, "rank"));
        if (r == null) return fail(ctx, "Ranks: junior, intermediate, senior, grand, wizard_king.");
        MultiverseProfile.setRank(p, r, cls);
        return ok(ctx, p.getName().getString() + " is now " + KnightRank.describe(r, cls) + ".");
    }

    private static int rankInfo(CommandContext<CommandSourceStack> ctx, boolean hasArg) throws CommandSyntaxException {
        ServerPlayer p = targetOrSelf(ctx, hasArg);
        ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + ": " + MultiverseProfile.describeRank(p)
                + " | " + MultiverseProfile.socialClass(p).displayName + " | " + MultiverseProfile.race(p).displayName).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int starsInfo(CommandContext<CommandSourceStack> ctx, boolean hasArg) throws CommandSyntaxException {
        ServerPlayer p = targetOrSelf(ctx, hasArg);
        ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + ": " + MultiverseProfile.goldStars(p) + " gold, "
                + MultiverseProfile.blackStars(p) + " black, net " + MultiverseProfile.netStars(p)).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int spiritInfo(CommandContext<CommandSourceStack> ctx, boolean hasArg) throws CommandSyntaxException {
        ServerPlayer p = targetOrSelf(ctx, hasArg);
        boolean lord = SpiritLordSkill.instance(p).isPresent();
        if (!lord && !SpiritBond.bonded(p)) return fail(ctx, p.getName().getString() + " has no spirit.");
        String msg = p.getName().getString() + ": " + SpiritLordSkill.bondedType(p) + (lord ? " (Spirit Lord)" : "")
                + ", trust " + SpiritBond.trust(p) + "/100, energy " + SpiritLordSkill.energy(p) + "%"
                + (SpiritBond.incarnate(p) ? ", incarnated as " + SpiritBond.trueName(p) : ", not incarnated");
        ctx.getSource().sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> ctx, WorldSites.Kind kind) {
        var src = ctx.getSource();
        var site = WorldSites.get(src.getServer()).nearest(kind, src.getLevel().dimension(), net.minecraft.core.BlockPos.containing(src.getPosition()));
        String what = kind == WorldSites.Kind.TOWER ? "Grimoire Tower" : "Library Ruins";
        if (site.isEmpty()) return fail(ctx, "No " + what + " has been found in this dimension yet. They are rare; explore new land and they record themselves.");
        var pos = site.get().pos();
        int dist = (int) Math.sqrt(pos.distSqr(net.minecraft.core.BlockPos.containing(src.getPosition())));
        src.sendSuccess(() -> Component.literal("Nearest " + what + ": " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " (" + dist + " blocks away)")
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }
}
