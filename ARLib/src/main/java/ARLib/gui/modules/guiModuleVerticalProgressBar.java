package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class guiModuleVerticalProgressBar extends GuiModuleBase {
    public ResourceLocation background = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_vertical_progress_bar_background.png");
    public ResourceLocation bar = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_vertical_progress_bar.png");


    public double progress;
    public String info = "";

    public guiModuleVerticalProgressBar(int id, IGuiHandler guiHandler, int x, int y) {
        super(id, guiHandler, x, y);
    }

    public void setHoverInfoAndSync(String s) {
        boolean needsUpdate = !Objects.equals(s, this.info);
        this.info = s;
        if (needsUpdate) {
            broadcastModuleUpdate();
        }
    }

    public void setProgressAndSync(double progress) {
        progress = Math.max(0, Math.min(1, progress));
        boolean needsUpdate = progress != this.progress;
        this.progress = progress;
        if (needsUpdate) {
            broadcastModuleUpdate();
        }
    }

    public void server_writeDataToSyncToClient(CompoundTag tag) {
        CompoundTag myTag = new CompoundTag();
        myTag.putDouble("progress", this.progress);
        if (this.info.isEmpty()) {
            myTag.putBoolean("noInfo", true);
        } else {
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


    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isEnabled) {
            int v_offset = (int) (((double) 1.0F - progress) * (double) 52.0F);
            int v_offset_tex = (int) (((double) 1.0F - progress) * (double) 44.0F);
            guiGraphics.blit(this.background, this.onGuiX, this.onGuiY, 0.0F, 0.0F, 14, 54, 14, 54);
            guiGraphics.blit(this.bar, this.onGuiX + 1, this.onGuiY + v_offset + 1, 12, 52 - v_offset, 0.0F, 0.0F + (float) v_offset_tex, 6, 44 - v_offset_tex, 6, 44);
            if (this.isMouseOver((double) mouseX, (double) mouseY, this.onGuiX, this.onGuiY, 14, 54)) {
                guiGraphics.renderTooltip(Minecraft.getInstance().font, Component.literal(info), mouseX, mouseY);
            }
        }

    }
}
