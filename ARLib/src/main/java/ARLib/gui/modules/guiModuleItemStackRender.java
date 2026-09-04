package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import ARLib.gui.ModularScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Objects;

public class guiModuleItemStackRender extends GuiModuleBase {

    public ItemStack stack;
    public float scale = 1;


    public guiModuleItemStackRender(int id, ItemStack stack, float scale, IGuiHandler guiHandler, int x, int y) {
        super(id, guiHandler, x, y);
        this.stack = stack;
        this.scale = scale;
    }

    public void setStackAndSync(ItemStack stack) {
        if (!ItemStack.isSameItemSameComponents(stack, this.stack) || stack.getCount() != this.stack.getCount()) {
            this.stack = stack;
            broadcastModuleUpdate();
        }
    }

    @Override
    public void server_writeDataToSyncToClient(CompoundTag tag) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            CompoundTag myTag = new CompoundTag();
            if (stack.isEmpty()) {
                myTag.putBoolean("isEmpty", true);
            } else {
                CompoundTag stackTag = new CompoundTag();
                myTag.put("stack", new ItemStack(stack.getItem(), 1).save(server.registryAccess(), stackTag));
                myTag.putInt("count", stack.getCount());
            }
            tag.put(getMyTagKey(), myTag);
        }
        super.server_writeDataToSyncToClient(tag);
    }

    @Override
    public void client_handleDataSyncedToClient(CompoundTag tag) {
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());
            if (myTag.contains("isEmpty")) {
                this.stack = ItemStack.EMPTY;
            }
            if (myTag.contains("stack") && myTag.contains("count")) {
                this.stack = ItemStack.parse(Minecraft.getInstance().level.registryAccess(), myTag.getCompound("stack")).orElse(ItemStack.EMPTY);
                stack.setCount(myTag.getInt("count"));
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
        if (isEnabled) {
            if (!stack.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(onGuiX, onGuiY, 0);
                guiGraphics.pose().scale(scale, scale, scale);
                ModularScreen.renderItemStack(guiGraphics, 0, 0, this.stack);
                guiGraphics.pose().popPose();
            }
        }
    }
}
