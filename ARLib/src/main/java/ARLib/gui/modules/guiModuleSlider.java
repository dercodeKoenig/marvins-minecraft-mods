package ARLib.gui.modules;


import ARLib.gui.IGuiHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

public class guiModuleSlider extends GuiModuleBase {
    public double value = 0;
    int w;
    int h;
    boolean isChanged = false;

    public guiModuleSlider(int id, IGuiHandler guiHandler, int x, int y, int w, int h) {
        super(id, guiHandler, x, y);
        this.w = w;
        this.h = h;
    }

    public void onValueChangedClient(double value) {

    }

    public void onValueChangeReceivedOnServer(double value) {

    }

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
    public void server_readNetworkData(CompoundTag tag) {
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());
            if (myTag.contains("value")) {
                setValueAndSync(myTag.getDouble("value"));
                onValueChangeReceivedOnServer(value);
            }
        }
    }

    @Override
    public void client_onMouseDragged(double mouseX, double mouseY, double dragX, double dragY) {
        if (isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h) || isChanged)
            // if we are already changing and the user keeps dragging, updat the value to max even if
            // the mouse it out of bounds
            updateValueFromMouse(mouseX);
    }

    @Override
    public void client_onMouseClick(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h))
            updateValueFromMouse(mouseX);
    }

    @Override
    public void client_onMouseReleased(double x, double y, int btn) {
        if (isChanged) {
            isChanged = false;
            // internal update
            CompoundTag tag = new CompoundTag();
            CompoundTag info = new CompoundTag();
            info.putDouble("value", value);
            tag.put(getMyTagKey(), info);
            guiHandler.sendToServer(tag);
        }
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        if (!isEnabled)
            return;

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
        isChanged = true;
        onValueChangedClient(this.value);
    }
}
