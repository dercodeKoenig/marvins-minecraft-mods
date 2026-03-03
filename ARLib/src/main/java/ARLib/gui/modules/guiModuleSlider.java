package ARLib.gui.modules;


import ARLib.gui.IGuiHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

public class guiModuleSlider extends GuiModuleBase {
    int w;
    int h;
    double value = 0;

    public guiModuleSlider(int id, IGuiHandler guiHandler, int x, int y, int w, int h) {
        super(id, guiHandler, x, y);
        this.w = w;
        this.h = h;
    }

    public void onValueChanged(double value){

    };

    public void setValueAndSync(double value) {
        if (this.value != value) {
            this.value = value;
            broadcastModuleUpdate();
        }
    }

    @Override
    public void client_handleDataSyncedToClient(CompoundTag tag) {
        super.client_handleDataSyncedToClient(tag);
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());
            if (myTag.contains("value")) {
                value = myTag.getDouble("value");
            }
        }
    }

    @Override
    public void server_writeDataToSyncToClient(CompoundTag tag) {
        CompoundTag myTag = new CompoundTag();
        myTag.putDouble("value", this.value);
        tag.put(getMyTagKey(), myTag);
        super.server_writeDataToSyncToClient(tag);
    }

    @Override
    public void client_onMouseDragged(double mouseX, double mouseY, double dragX, double dragY) {
        if (isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h))
            updateValueFromMouse(mouseX);
    }

    @Override
    public void client_onMouseCLick(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h))
            updateValueFromMouse(mouseX);
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // Draw the background track
        guiGraphics.fill(onGuiX, onGuiY + h / 2 - 2, onGuiX + w, onGuiY + h / 2 + 2, 0xFF555555);

        // Draw the "thumb" (the part you grab)
        int thumbPos = (int) (onGuiX + (value * (w - 8)));
        int color = this.isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h) ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.fill(thumbPos, onGuiY, thumbPos + 8, onGuiY + h, color);
    }

    private void updateValueFromMouse(double mouseX) {
        this.value = (mouseX - onGuiX) / (double) w;
        this.value = Math.max(0, Math.min(1, this.value)); // Clamp 0-1
        onValueChanged(this.value);
    }
}
