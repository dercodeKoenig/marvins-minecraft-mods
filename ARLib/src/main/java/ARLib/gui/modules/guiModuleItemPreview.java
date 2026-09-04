package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import ARLib.gui.ModularScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class guiModuleItemPreview extends guiModuleItemStackRender {
    // simplified client side version

    public guiModuleItemPreview(IGuiHandler guiHandler, int x, int y, ItemStack itemStack) {
        super(-1, itemStack,1, guiHandler, x, y);
    }

    @Override
    public void server_writeDataToSyncToClient(CompoundTag tag) {

    }

    @Override
    public void client_handleDataSyncedToClient(CompoundTag tag) {

    }
}
