package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class guiModuleButton extends GuiModuleBase {
    public int w, h;
    public int textureW, textureH;
    public ResourceLocation image;
    public String text;
    public int color;
    public boolean makeShadow = false;

    public void onButtonClicked(){
        CompoundTag tag = new CompoundTag();
        tag.putInt("guiButtonClick", id);
        guiHandler.sendToServer(tag);
    }

    @Override
    public void client_onMouseCLick(double x, double y, int button) {
        if (isEnabled) {
            if (isMouseOver(x, y, onGuiX, onGuiY, w, h) && button == 0) {
                onButtonClicked();
            }
        }
    }

    public void setTextAndSync(String text) {
        boolean needsUpdate = !Objects.equals(this.text, text);
        this.text = text;
        if (needsUpdate) {
            broadcastModuleUpdate();
        }
    }

    public void setColorAndSync(int color) {
        boolean needsUpdate = this.color != color;
        this.color = color;
        if (needsUpdate) {
            broadcastModuleUpdate();
        }
    }

    @Override
    public void server_writeDataToSyncToClient(CompoundTag tag) {
        CompoundTag myTag = new CompoundTag();
        myTag.putString("text", this.text);
        myTag.putInt("color", this.color);
        tag.put(getMyTagKey(), myTag);

        super.server_writeDataToSyncToClient(tag);
    }

    @Override
    public void client_handleDataSyncedToClient(CompoundTag tag) {
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());
            if (myTag.contains("text")) {
                this.text = myTag.getString("text");
            }
            if (myTag.contains("color")) {
                this.color = myTag.getInt("color");
            }
        }
        super.client_handleDataSyncedToClient(tag);
    }


    public guiModuleButton(int id, String text, IGuiHandler guiHandler, int x, int y, int w, int h, ResourceLocation image, int textureW, int textureH) {
        super(id, guiHandler, x, y);
        this.w = w;
        this.h = h;
        this.image = image;
        this.textureW = textureW;
        this.textureH = textureH;
        this.text = text;
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (isEnabled) {
            guiGraphics.blit(image, onGuiX, onGuiY, w, h, 0f, 0f, textureW, textureH, textureW, textureH);
            guiGraphics.drawString(Minecraft.getInstance().font, text, onGuiX + w / 2 - Minecraft.getInstance().font.width(text) / 2, onGuiY + h / 2 - Minecraft.getInstance().font.lineHeight / 2, color, makeShadow);
        }
    }
}
