package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class guiModuleDefaultButton extends guiModuleButton {
    ResourceLocation button = ResourceLocation.fromNamespaceAndPath("arlib","textures/gui/button.png");
    ResourceLocation button_disabled = ResourceLocation.fromNamespaceAndPath("arlib","textures/gui/button_disabled.png");
    ResourceLocation button_highlight = ResourceLocation.fromNamespaceAndPath("arlib","textures/gui/button_highlighted.png");

    public guiModuleDefaultButton(int id, String text, IGuiHandler guiHandler, int x, int y, int w, int h) {
        super(id, text, guiHandler, x, y, w, h, ResourceLocation.fromNamespaceAndPath("", ""), 200, 20);
        color = 0xFFFFFFFF;
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (isEnabled) {
            if(this.isMouseOver(mouseX,mouseY,onGuiX,onGuiY,w,h)){
                guiGraphics.blit(button_highlight, onGuiX, onGuiY, w, h, 0f, 0f, textureW, textureH, textureW, textureH);
            }else {
                guiGraphics.blit(button, onGuiX, onGuiY, w, h, 0f, 0f, textureW, textureH, textureW, textureH);
            }
        }else{
            guiGraphics.blit(button_disabled, onGuiX, onGuiY, w, h, 0f, 0f, textureW, textureH, textureW, textureH);
        }


        guiGraphics.drawString(Minecraft.getInstance().font, text, onGuiX + w / 2 - Minecraft.getInstance().font.width(text) / 2, onGuiY + h / 2 - Minecraft.getInstance().font.lineHeight / 2, color, false);
    }
}
