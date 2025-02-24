package WorkSites.WarehouseCrafter;

import ResearchSystem.EngineeringStation.MenuEngineeringStation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static ResearchSystem.Registry.ITEM_RESEARCH_BOOK;

public class ScreenWarehouseCrafter extends AbstractContainerScreen<MenuWarehouseCrafter> {
    public ScreenWarehouseCrafter(MenuWarehouseCrafter menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = 180;
        this.imageHeight=205;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        guiGraphics.blit(
                ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_item_slot_background.png"),
                slot.x-1,slot.y-1,
                0, 0,
                18, 18,
                18,18
        );

        super.renderSlot(guiGraphics,slot);
    }



       @Override
   protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {

        /*
         * Renders the background texture to the screen. 'leftPos' and
         * 'topPos' should already represent the top left corner of where
         * the texture should be rendered as it was precomputed from the
         * 'imageWidth' and 'imageHeight'. The two zeros represent the
         * integer u/v coordinates inside the PNG file, whose size is
         * represented by the last two integers (typically 256 x 256).
         */
           guiGraphics.blit(
                   ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/simple_gui_background.png"),
                   this.leftPos, this.topPos,
                   this.imageWidth, this.imageHeight,
                   0, 0,
                   176, 171,
                   176, 171
           );

           guiGraphics.blit(
                   ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"),
                   this.leftPos+125, this.topPos+36,
                   16, 12,
                   0, 0,
                   16, 12,
                   16, 12
           );

        guiGraphics.renderItem(new ItemStack(ITEM_RESEARCH_BOOK.get()),leftPos+10,topPos+18);

    }
}
