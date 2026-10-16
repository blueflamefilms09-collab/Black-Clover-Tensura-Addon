package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.menu.BuilderState;
import com.newuniverse.nusmp.avatar.net.C2S_NameAvatarPayload;
import com.newuniverse.nusmp.avatar.net.S2C_OpenActionMenuPayload;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Why: the small menu that opens when the creator right-clicks their own Avatar with an empty hand: Info (who it is) and Name (bestow a formal name,
 * the Tensura naming that binds it to you). The appearance cannot be changed here on purpose: a created Avatar keeps its look. It only sends the
 * chosen name; the server checks that the Avatar is yours and near, applies the naming and answers with its own words (shown in the screen).
 */
public class AvatarActionScreen extends Screen {
    private static final int W = 230, H = 190;
    private final S2C_OpenActionMenuPayload data;
    private boolean naming;
    private EditBox box;
    private int seenResult;
    private Component note = Component.empty();
    private boolean noteOk;

    public AvatarActionScreen(S2C_OpenActionMenuPayload data) {
        super(Component.translatable("nusmp.avatar.action.title"));
        this.data = data;
        this.seenResult = AvatarClientData.resultVersion;
    }

    @Override
    protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        box = null;
        if (naming) {
            box = new EditBox(font, x + 14, y + 66, W - 28, 16, Component.translatable("nusmp.avatar.action.name_hint"));
            box.setMaxLength(BuilderState.NAME_MAX);
            box.setHint(Component.translatable("nusmp.avatar.action.name_hint"));
            addRenderableWidget(box);
            setInitialFocus(box);
            addRenderableWidget(new ThemedButton(x + 14, y + H - 28, 100, 16, Component.translatable("nusmp.avatar.action.bestow"), b -> {
                String n = box.getValue().strip();
                if (!BuilderState.nameOk(n) || n.isEmpty()) { note = Component.translatable("nusmp.avatar.gui.confirm.name_bad"); noteOk = false; return; }
                PacketDistributor.sendToServer(new C2S_NameAvatarPayload(data.avatarId(), n));
                note = Component.translatable("nusmp.avatar.action.sent");
                noteOk = true;
            }));
            addRenderableWidget(new ThemedButton(x + W - 114, y + H - 28, 100, 16, Component.translatable("nusmp.avatar.gui.back"), b -> { naming = false; rebuildWidgets(); }));
        } else {
            ThemedButton name = new ThemedButton(x + 14, y + H - 28, 100, 16, Component.translatable("nusmp.avatar.action.name"), b -> { naming = true; rebuildWidgets(); });
            name.active = !data.named();
            addRenderableWidget(name);
            addRenderableWidget(new ThemedButton(x + W - 114, y + H - 28, 100, 16, Component.translatable("nusmp.avatar.action.close"), b -> onClose()));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (seenResult != AvatarClientData.resultVersion) {
            seenResult = AvatarClientData.resultVersion;
            var res = AvatarClientData.result;
            if (res != null && res.kind().equals("name")) {
                note = Component.translatableWithFallback(res.message(), res.message());
                noteOk = res.ok();
                if (res.ok()) onClose();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (box != null && box.isFocused() && keyCode != 256) { box.keyPressed(keyCode, scanCode, modifiers); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        float now = UiTheme.now();
        int x = (width - W) / 2, y = (height - H) / 2;
        UiPanel.panelScan(g, x, y, W, H, UiTheme.CYAN, 0.8f, 1f, now);
        g.enableScissor(x + 1, y + 1, x + W - 1, y + H - 1);
        MagicCircle.draw(g, x + W - 40, y + 50, 38f, now, UiTheme.VIOLET, 0.35f, MagicCircle.CALM, 1f);
        g.disableScissor();
        String nm = data.name().isEmpty() ? "?" : data.name();
        g.drawString(font, Component.literal(nm), x + 14, y + 12, UiTheme.GOLD);
        if (naming) {
            g.drawString(font, Component.translatable("nusmp.avatar.action.name_title"), x + 14, y + 30, UiTheme.TEXT);
            g.drawString(font, Component.translatable("nusmp.avatar.action.name_warn"), x + 14, y + 44, UiTheme.TEXT_DIM);
            g.drawString(font, Component.translatable("nusmp.avatar.action.name_warn2"), x + 14, y + 55, UiTheme.TEXT_DIM);
        } else {
            int ly = y + 30;
            g.drawString(font, Component.translatable(data.named() ? "nusmp.avatar.action.named" : "nusmp.avatar.action.unnamed"), x + 14, ly, data.named() ? UiTheme.GREEN : UiTheme.TEXT_DIM); ly += 13;
            g.drawString(font, Component.translatable("nusmp.avatar.action.race", data.race().isEmpty() ? "?" : data.race()), x + 14, ly, UiTheme.TEXT); ly += 12;
            AvatarPersonality p = AvatarPersonality.byName(data.primary()), s = AvatarPersonality.byName(data.secondary());
            g.drawString(font, Component.translatable("nusmp.avatar.action.persona", p == null ? "?" : p.displayName, s == null ? "-" : s.displayName), x + 14, ly, UiTheme.TEXT); ly += 12;
            g.drawString(font, Component.translatable("nusmp.avatar.action.luck", String.format(Locale.ROOT, "%.1f", data.luck())), x + 14, ly, UiTheme.VIOLET); ly += 12;
            g.drawString(font, Component.translatable("nusmp.avatar.action.size", String.format(Locale.ROOT, "%.2f", data.scale())), x + 14, ly, UiTheme.TEXT); ly += 12;
            // the fixed lines above already show the name, race, personality and luck: only the extra lines are listed, and never past the buttons
            String nameLine = data.name();
            for (String line : data.info()) {
                if (line.startsWith(nameLine) || line.startsWith("Race:") || line.startsWith("Creator luck:")) continue;
                if (ly > y + H - 56) break;
                g.drawString(font, font.plainSubstrByWidth(line, W - 28), x + 14, ly, UiTheme.TEXT_DIM);
                ly += 11;
            }
        }
        if (!note.getString().isEmpty()) g.drawString(font, note, x + 14, y + H - 42, noteOk ? UiTheme.GREEN : UiTheme.RED);
        super.render(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
