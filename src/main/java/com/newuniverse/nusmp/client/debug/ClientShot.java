package com.newuniverse.nusmp.client.debug;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.relics.worn.WornDebugClient;
import com.newuniverse.nusmp.corpse.NamedCorpseDebug;
import com.newuniverse.nusmp.corpse.NamedCorpseEntity;
import com.newuniverse.nusmp.client.relics.worn.WornTrace;
import com.newuniverse.nusmp.client.relics.worn.lib.ArmorSlotClient;
import com.newuniverse.nusmp.client.relics.worn.lib.WornLibrary;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

/**
 * CI only (3.9.2). Started with {@code -Dnusmp.clientshot=1} (gradle {@code -Pclientshot}, see .github/workflows/clientshot.yml): in a real dev client under a software GL driver it makes a flat
 * world, puts the Mask of Mind Melting and the Ruby Boots of Planestriding on the player, takes screenshots of the body (third person front and back, the inventory paper doll),
 * logs what the worn layer found and decided, and quits. It exists so the worn armour relics can be SEEN by the build, not only reasoned about. Without the property nothing is registered.
 * Client only; never touches a server.
 */
public final class ClientShot {
    private ClientShot() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final boolean ON = System.getProperty("nusmp.clientshot") != null;
    private static final long LIMIT_NANOS = 40L * 60L * 1_000_000_000L;
    private static final String[] ARMOUR = {"armor.head mask_of_mind_melting", "armor.feet ruby_boots_of_planestriding"};

    private static long started;
    private static int ticks, stage, wait, joinedTicks;
    private static boolean created;
    private static int undeadI;
    private static volatile String summonResult = "";

    public static void init() {
        if (!ON) return;
        started = System.nanoTime();
        NeoForge.EVENT_BUS.addListener(ClientShot::tick);
        NeoForge.EVENT_BUS.addListener(ClientShot::equipmentChanged);
        LOG.info("[clientshot] driver on");
    }

    /** Every change of a player's head or feet piece, on the integrated server and the client: when a slot is emptied, the stack of the code that did it is written to the log. */
    private static void equipmentChanged(LivingEquipmentChangeEvent e) {
        EquipmentSlot slot = e.getSlot();
        if (!(e.getEntity() instanceof Player) || (slot != EquipmentSlot.HEAD && slot != EquipmentSlot.FEET)) return;
        String side = e.getEntity().level().isClientSide ? "client" : "server";
        say(side + " " + slot + ": " + e.getFrom().getItem() + " -> " + e.getTo().getItem());
        if (e.getTo().isEmpty() && !e.getFrom().isEmpty()) {
            StringBuilder sb = new StringBuilder("   emptied by:");
            StackTraceElement[] st = new Throwable().getStackTrace();
            for (int i = 0; i < Math.min(st.length, 30); i++) sb.append("\n      ").append(st[i]);
            say(sb.toString());
        }
    }

    private static void say(String s) { LOG.info("[clientshot] {}", s); }

    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (System.nanoTime() - started > LIMIT_NANOS) {
            say("TIMEOUT at stage " + stage);
            System.exit(3);
        }
        if (mc.level == null || mc.player == null) {
            if (!created && mc.screen instanceof TitleScreen && ticks > 60) {
                created = true;
                say("creating the world");
                try {
                    mc.createWorldOpenFlows().createFreshLevel("clientshot",
                            new LevelSettings("clientshot", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(20261025L, false, false),
                            reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            mc.screen);
                } catch (RuntimeException | LinkageError ex) {
                    LOG.error("[clientshot] the world could not be created", ex);
                    System.exit(4);
                }
            } else if (ticks % 400 == 0) {
                say("waiting: screen " + (mc.screen == null ? "none" : mc.screen.getClass().getName()) + ", level " + (mc.level != null));
            }
            return;
        }
        joinedTicks++;
        Player p = mc.player;
        if (wait > 0) {
            wait--;
            keepClear(mc);
            return;
        }
        switch (stage) {
            case 0 -> {                                                                      // let the world settle
                keepClear(mc);
                if (joinedTicks > 240) stage = 1;
            }
            case 1 -> {                                                                      // dress the player, as the owner's screenshots show
                for (String a : ARMOUR) {
                    String[] x = a.split(" ");
                    mc.player.connection.sendCommand("item replace entity @s " + x[0] + " with nusmp:" + x[1]);
                }
                mc.player.connection.sendCommand("time set day");
                say("equipment commands sent");
                stage = 2;
                wait = 80;
            }
            case 2 -> {
                for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET}) report(p, s);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.setScreen(null);
                stage = 3;
                wait = 60;
            }
            case 3 -> {
                shot(mc, "third_front");
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                stage = 4;
                wait = 40;
            }
            case 4 -> {
                shot(mc, "third_back");
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.setScreen(new InventoryScreen(p));
                stage = 5;
                wait = 50;
            }
            case 5 -> {
                shot(mc, "inventory");
                mc.setScreen(null);
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                stage = 6;
                wait = 20;
            }
            case 6 -> {
                try {
                    WornDebugClient.report(p.getUUID(), List.of());
                } catch (RuntimeException ex) {
                    LOG.error("[clientshot] the debug report threw", ex);
                }
                WornTrace.Seen seen = WornTrace.of(p.getUUID());
                say("trace: " + (seen == null ? "LAYER NEVER REACHED" : "frames " + seen.frames + ", found " + seen.found + ", planned " + seen.planned + ", drawn " + seen.drawn + ", ensembles " + seen.ensembles));
                stage = 7;
                wait = 20;
            }
            case 7 -> {                                                                      // the numbered undead: cast on the integrated server, asked about on the real client
                String[] ids = NamedCorpseDebug.ids();
                if (undeadI >= ids.length) {
                    stage = 9;
                    break;
                }
                var server = mc.getSingleplayerServer();
                if (server == null) {
                    say("undead: no integrated server");
                    stage = 9;
                    break;
                }
                String id = ids[undeadI];
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                summonResult = "(pending)";
                server.execute(() -> {
                    var sp = server.getPlayerList().getPlayer(p.getUUID());
                    summonResult = sp == null ? "no server player" : NamedCorpseDebug.summon(sp, id);
                });
                stage = 8;
                wait = 60;
            }
            case 8 -> {
                String id = NamedCorpseDebug.ids()[undeadI];
                int seen = 0;
                for (Entity en : mc.level.entitiesForRendering()) {
                    if (!(en instanceof NamedCorpseEntity n) || !n.kind().id.equals(id)) continue;
                    seen++;
                    String r = mc.getEntityRenderDispatcher().getRenderer(en).getClass().getName();
                    say("undead " + id + ": server says " + summonResult + "; client sees entity " + en.getId() + " alive " + en.isAlive() + " invisible " + en.isInvisible() + " dist "
                            + Math.round(en.distanceTo(p) * 10) / 10.0 + " renderer " + r + " onFire " + en.isOnFire());
                }
                if (seen == 0) say("undead " + id + ": server says " + summonResult + "; the CLIENT SEES NOTHING");
                shot(mc, "undead_" + id);
                undeadI++;
                stage = 7;
                wait = 10;
            }
            default -> {
                say("done");
                mc.stop();
                new Thread(() -> {
                    try {
                        Thread.sleep(20_000L);
                    } catch (InterruptedException ignored) {
                        // leaving anyway
                    }
                    System.exit(0);
                }, "clientshot-exit").start();
                stage = 99;
                wait = 1_000_000;
            }
        }
    }

    /** Any screen the mod opens on joining (a questionnaire, a notice) is closed so the camera can see the body. */
    private static void keepClear(Minecraft mc) {
        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) mc.setScreen(null);
    }

    private static void report(Player p, EquipmentSlot slot) {
        ItemStack s = p.getItemBySlot(slot);
        if (s.isEmpty()) {
            say(slot + ": empty");
            return;
        }
        RelicDef d = Relics.defOf(s);
        say(slot + ": " + s.getItem() + ", def " + (d == null ? "NONE" : d.id() + " slot " + d.slot()));
        if (d != null) {
            WornLibrary.Ensemble en = WornLibrary.of(d);
            say("   library: " + (en == null ? "NO MODEL" : en.pieces.size() + " pieces, " + en.baseVerts + " vertices"));
            say("   verdict: " + ArmorSlotClient.verdict(p, s, slot));
        }
        for (Relics.Located l : Relics.locatedList(p)) say("   located: " + l.def().id() + " in " + l.where());
    }

    private static void shot(Minecraft mc, String name) {
        try {
            File dir = new File(mc.gameDirectory, "clientshot");
            dir.mkdirs();
            var image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            File f = new File(dir, name + ".png");
            image.writeToFile(f);
            image.close();
            say("saved " + f.getName());
        } catch (Exception ex) {
            LOG.error("[clientshot] screenshot {} failed", name, ex);
        }
    }
}
