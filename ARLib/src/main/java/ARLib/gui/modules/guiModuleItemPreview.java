package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import ARLib.gui.ModularScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class guiModuleItemPreview extends guiModuleItemStackRender {
    public guiModuleItemPreview(IGuiHandler guiHandler, int x, int y, ItemStack itemStack) {
        super(-1, itemStack,1, guiHandler, x, y);
    }
}
