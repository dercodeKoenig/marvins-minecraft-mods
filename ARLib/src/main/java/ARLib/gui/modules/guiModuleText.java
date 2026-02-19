package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.Objects;

public class guiModuleText extends GuiModuleBase {

    public String text;
    public int color;
    public boolean makeShadow;

    public guiModuleText(int id, String text, IGuiHandler guiHandler, int x, int y, int color, boolean makeShadow) {
        super(id, guiHandler, x, y);
        this.text = text;
        this.color = color;
        this.makeShadow = makeShadow;
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


    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (isEnabled && text != null) {
            Font font = Minecraft.getInstance().font;

            // Split the text into an array of lines wherever there is a newline
            String[] lines = text.split("\n");
            int currentY = onGuiY;

            // Loop through each line and draw it
            for (String line : lines) {
                guiGraphics.drawString(font, line, onGuiX, currentY, color, makeShadow);

                // Increase the Y position by the font's standard line height
                currentY += font.lineHeight;
            }
        }
    }
}
