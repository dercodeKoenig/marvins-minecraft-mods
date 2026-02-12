package advRocketry.Render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class MapSlider extends AbstractWidget {
    private double value; // 0.0 to 1.0
    private final SliderCallback callback;

    public interface SliderCallback {
        void onValueChanged(double newValue);
    }

    public MapSlider(int x, int y, int width, int height, Component message, double initialValue, SliderCallback callback) {
        super(x, y, width, height, message);
        this.value = initialValue;
        this.callback = callback;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Draw the background track
        guiGraphics.fill(getX(), getY() + height / 2 - 2, getX() + width, getY() + height / 2 + 2, 0xFF555555);

        // Draw the "thumb" (the part you grab)
        int thumbPos = (int) (getX() + (value * (width - 8)));
        int color = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.fill(thumbPos, getY(), thumbPos + 8, getY() + height, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {}

    @Override
    public void onClick(double mouseX, double mouseY) {
        updateValueFromMouse(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        updateValueFromMouse(mouseX);
    }

    private void updateValueFromMouse(double mouseX) {
        this.value = (mouseX - getX()) / (double) width;
        this.value = Math.max(0, Math.min(1, this.value)); // Clamp 0-1
        callback.onValueChanged(this.value);
    }

    public double getValue() { return value; }
}
