package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class guiModuleProgressBarHorizontal6px extends GuiModuleBase {
    public int color;
    public double progress;
    public String info = "";
    public guiModuleProgressBarHorizontal6px(int id, int barColor, IGuiHandler guiHandler, int x, int y) {
        super(id, guiHandler, x, y);
        this.color = barColor;
    }

    public ResourceLocation background = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_horizontal_progress_bar_background.png");

    public void setHoverInfoAndSync(String s){
        boolean needsUpdate = !Objects.equals(s, this.info);
        this.info = s;
        if(needsUpdate) {
            broadcastModuleUpdate();
        }
    }

    public void setProgressAndSync(double progress) {
        progress = Math.max(0,Math.min(1,progress));
        boolean needsUpdate =progress != this.progress;
        this.progress = progress;
        if(needsUpdate) {
            broadcastModuleUpdate();
        }
    }

    public void server_writeDataToSyncToClient(CompoundTag tag) {
        CompoundTag myTag = new CompoundTag();
        myTag.putDouble("progress", this.progress);
        if(this.info.isEmpty()){
            myTag.putBoolean("noInfo", true);
        }else {
            myTag.putString("info", this.info);
        }
        tag.put(this.getMyTagKey(), myTag);

        super.server_writeDataToSyncToClient(tag);
    }

    public void client_handleDataSyncedToClient(CompoundTag tag) {
        if (tag.contains(this.getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(this.getMyTagKey());
            if (myTag.contains("progress")) {
                this.progress = myTag.getDouble("progress");
            }
            if (myTag.contains("info")) {
                this.info = myTag.getString("info");
            }
            if (myTag.contains("noInfo")) {
                this.info = "";
            }
        }

        super.client_handleDataSyncedToClient(tag);
    }



    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if(isEnabled) {
            guiGraphics.blit(background, onGuiX, onGuiY, 0, 0, 54, 6, 54, 6);
            guiGraphics.fill(onGuiX + 1, onGuiY + 1, onGuiX + (int) (52 * progress) + 1, onGuiY + 4 + 1, color);
            if (this.isMouseOver((double)mouseX, (double)mouseY, this.onGuiX, this.onGuiY, 54, 6)) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, Component.literal(info), mouseX, mouseY);
            }
        }
    }
}
