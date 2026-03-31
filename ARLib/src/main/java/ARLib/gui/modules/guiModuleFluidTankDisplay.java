package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import ARLib.gui.ModularScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class guiModuleFluidTankDisplay extends GuiModuleBase {

    public ResourceLocation fluid_bar_background = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_vertical_progress_bar_background.png");
    public ResourceLocation fluid_bar_grading = ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_vertical_progress_bar_4p.png");

    IFluidHandler fluidHandler;
    int targetSlot;
    public FluidStack client_myFluidStack;
    FluidStack lastFluidStack;
    public int maxCapacity;
    int last_maxCapacity;

    // textures width and height
    int fluid_bar_background_tw = 14;
    int fluid_bar_background_th = 54;
    int borderpx = 1;

    // full size of the bar when rendering (background)
    int w = 14;
    int h = 54;

    // size of the bar
    int bar_size_w = w - borderpx * 2;
    int bar_size_h = h - borderpx * 2;

    int fluid_bar_offset_x = borderpx;
    int fluid_bar_offset_y = borderpx;


    public guiModuleFluidTankDisplay(int id, IFluidHandler fluidHandler, int targetSlot, IGuiHandler guiHandler, int x, int y) {
        super(id, guiHandler, x, y);
        this.fluidHandler = fluidHandler;
        this.targetSlot = targetSlot;
        this.client_myFluidStack = FluidStack.EMPTY;
        this.lastFluidStack = FluidStack.EMPTY;
    }


    @Override
    public void server_writeDataToSyncToClient(CompoundTag tag) {
        CompoundTag myTag = new CompoundTag();
        RegistryAccess registryAccess = ServerLifecycleHooks.getCurrentServer().registryAccess();
        FluidStack f = fluidHandler.getFluidInTank(targetSlot);
        if (f.isEmpty()){
            myTag.putBoolean("hasFluid",false);
        }else{
            myTag.putBoolean("hasFluid",true);
            Tag fluid = fluidHandler.getFluidInTank(targetSlot).save(registryAccess);
            myTag.put("fluid", fluid);
        }
        myTag.putInt("maxCapacity", fluidHandler.getTankCapacity(targetSlot));
        myTag.putLong("time",System.currentTimeMillis());

        tag.put(getMyTagKey(), myTag);

        super.server_writeDataToSyncToClient(tag);

    }

    long last_packet_time = 0; // sometimes older packets can come in after newer ones. so this will make sure only the most recent data will be used
    @Override
    public void client_handleDataSyncedToClient(CompoundTag tag) {
        if (tag.contains(getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(getMyTagKey());
            if (myTag.contains("maxCapacity"))
                this.maxCapacity = myTag.getInt("maxCapacity");
            if (myTag.contains("time")) {
                long update_time = myTag.getLong("time");
                if (update_time > last_packet_time) {
                    last_packet_time = update_time;
                    if (myTag.contains("hasFluid")) {
                        if (myTag.getBoolean("hasFluid")) {
                            Tag fluid = myTag.get("fluid");
                            RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
                            client_myFluidStack = FluidStack.parse(registryAccess, fluid).get();
                        } else {
                            client_myFluidStack = FluidStack.EMPTY;
                        }
                    }
                }
            }
        }
        super.client_handleDataSyncedToClient(tag);
    }

    int last_update = 0;
    @Override
    public void serverTick() {
        last_update += 1;
        // update every x ticks
        if (
                (!fluidHandler.getFluidInTank(targetSlot).equals(lastFluidStack) ||
                        fluidHandler.getFluidInTank(targetSlot).getAmount() != lastFluidStack.getAmount() ||
                        fluidHandler.getTankCapacity(targetSlot) != last_maxCapacity)
                        && last_update > 2) {
            last_maxCapacity = fluidHandler.getTankCapacity(targetSlot);
            last_update = 0;
            lastFluidStack = fluidHandler.getFluidInTank(targetSlot).copy();

            broadcastModuleUpdate();
        }
    }


    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        if(isEnabled) {

            guiGraphics.blit(fluid_bar_background, onGuiX, onGuiY, 0, 0, w, h, fluid_bar_background_tw, fluid_bar_background_th);

            if (!client_myFluidStack.isEmpty()) {
                double relative_fluid_level = (double) client_myFluidStack.getAmount() / maxCapacity;
                int y_offset = (int) ((1 - relative_fluid_level) * bar_size_h);
                IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(client_myFluidStack.getFluid());
                int color = extensions.getTintColor();
                int alpha = (color >> 24) & 0xFF; // Extract alpha (bits 24-31)
                int red = (color >> 16) & 0xFF;   // Extract red (bits 16-23)
                int green = (color >> 8) & 0xFF;  // Extract green (bits 8-15)
                int blue = color & 0xFF;          // Extract blue (bits 0-7)
                float af = (float) alpha / 255f;
                float rf = (float) red / 255f;
                float gf = (float) green / 255f;
                float bf = (float) blue / 255f;

                ResourceLocation fluidtexture = extensions.getStillTexture();
                TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(fluidtexture);
                guiGraphics.blit(onGuiX + fluid_bar_offset_x, onGuiY + fluid_bar_offset_y + y_offset, 0, bar_size_w, bar_size_h - y_offset, sprite, rf, gf, bf, af);
            }

            guiGraphics.blit(fluid_bar_grading, onGuiX, onGuiY, 0, 0, w, h, fluid_bar_background_tw, fluid_bar_background_th);

            if (isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h)) {
                String info = "0/" + maxCapacity + "mb)";
                if (!client_myFluidStack.isEmpty()) {
                    info = client_myFluidStack.getHoverName().getString() + ":" + client_myFluidStack.getAmount() + "/" + maxCapacity + "mb";
                }
                ModularScreen.addToolTip(new ModularScreen.ToolTip(mouseX, mouseY, Component.literal(info)));
            }
        }
    }
}
