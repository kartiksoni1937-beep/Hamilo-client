package dev.hamilo.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class HamiloClickGuiScreen extends Screen {
    public HamiloClickGuiScreen() {
        super(Text.literal("Hamilo Client"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = 240;
        int panelHeight = 120;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xE91A1D24);
        context.fill(left, top, left + panelWidth, top + 2, 0xFF7C5CFF);
        context.drawText(this.textRenderer, Text.literal("Hamilo Client"), left + 12, top + 12, 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("Java-agent injection build"), left + 12, top + 32, 0xFFB9BBC6, false);
        context.drawText(this.textRenderer, Text.literal("Client GUI is running."), left + 12, top + 52, 0xFFE2E3EA, false);
        context.drawText(this.textRenderer, Text.literal("Press ESC to close"), left + 12, top + 92, 0xFFB9BBC6, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
