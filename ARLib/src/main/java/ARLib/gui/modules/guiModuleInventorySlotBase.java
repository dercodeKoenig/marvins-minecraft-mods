package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import ARLib.gui.ModularScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

public abstract class guiModuleInventorySlotBase extends GuiModuleBase {

    public ResourceLocation slot_background = ResourceLocation.fromNamespaceAndPath("arlib","textures/gui/gui_item_slot_background.png");
    public int slot_bg_w = 18;
    public int slot_bg_h = 18;
    public void setSlotBackground(ResourceLocation bg, int textureWidth, int textureHeight){
        slot_background = bg;
        slot_bg_h = textureHeight;
        slot_bg_w = textureWidth;
    }

    public int w = 18;
    public int h = 18;

    public int invGroup;
    public int instantTransferTarget;

    // returns the ItemStack for the slot to render
    public abstract ItemStack client_getItemStackToRender();

    // called when the slot is clicked from server
    public abstract void server_handleInventoryClick(Player player, int button, boolean isShift);

    @Override
    public void client_onMouseCLick(double mx, double my, int button) {
        if (isEnabled) {
            boolean isShiftDown =
                    InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LSHIFT) ||
                            InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_RSHIFT);

            if (isMouseOver(mx, my, onGuiX, onGuiY, w, h)) {
                CompoundTag tag = new CompoundTag();
                CompoundTag myTag = new CompoundTag();

                // add client id to the tag
                UUID myId = Minecraft.getInstance().player.getUUID();
                myTag.putUUID("uuid_from", myId);
                myTag.putInt("mouseButtonClicked", button);
                myTag.putBoolean("isShift", isShiftDown);

                tag.put(getMyTagKey(), myTag);
                guiHandler.sendToServer(tag);

                //server_handleInventoryClick(Minecraft.getInstance().player,button,isShiftDown);
            }
        }
    }

    @Override
    public void server_readNetworkData(CompoundTag tag) {
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());

            if (myTag.contains("uuid_from") && myTag.contains("mouseButtonClicked") && myTag.contains("isShift")) {
                UUID from_uuid = myTag.getUUID("uuid_from");
                int button = myTag.getInt("mouseButtonClicked");
                boolean isShift = myTag.getBoolean("isShift");
                Player player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(from_uuid);
                if (player != null)
                    server_handleInventoryClick(player, button, isShift);
            }
        }
    }


    public guiModuleInventorySlotBase(int id, IGuiHandler guiHandler, int inventoryGroup, int instantTransferTargetGroup, int x, int y) {
        super(id,guiHandler,x, y);
        this. invGroup = inventoryGroup;
        this. instantTransferTarget = instantTransferTargetGroup;
    }

    @Override
    public  void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (isEnabled) {

            guiGraphics.blit(slot_background, onGuiX, onGuiY, 0f, 0f, w, h, slot_bg_w, slot_bg_h);
            ModularScreen.renderItemStack(guiGraphics, onGuiX, onGuiY, client_getItemStackToRender());

            if (!client_getItemStackToRender().isEmpty() && isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h)) {
                guiGraphics.fill(onGuiX, onGuiY, w + onGuiX, h + onGuiY, 0x30FFFFFF); // Semi-transparent white
                guiGraphics.renderTooltip(Minecraft.getInstance().font, client_getItemStackToRender(), mouseX, mouseY);
            }

        }
    }
}
