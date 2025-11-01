package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import ARLib.gui.ModularScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class guiModuleItemPreview extends GuiModuleBase {
    public ItemStack itemStack;


    public guiModuleItemPreview(IGuiHandler guiHandler, int x, int y, ItemStack itemStack) {
        super(-1, guiHandler, x, y);
        this.itemStack = itemStack;
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if(isEnabled) {
            ModularScreen.renderItemStack(guiGraphics,onGuiX,onGuiY,itemStack);
            if(isMouseOver(mouseX,mouseY,onGuiX,onGuiY,18,18)){
                guiGraphics.renderTooltip(Minecraft.getInstance().font,itemStack,mouseX,mouseY);
            }
        }
    }
}
