package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public class guiModuleScrollContainer extends GuiModuleBase {

    public double top_extra_offset = 0;
    public List<GuiModuleBase> modules;
    protected int w;
    protected int h;
    int left, top;
    int backgroundColor;

    public guiModuleScrollContainer(List<GuiModuleBase> modules, int backgroundColor, IGuiHandler guiHandler, int x, int y, int w, int h) {
        super(-1, guiHandler, x, y);
        this.w = w;
        this.h = h;
        this.modules = modules;
        this.backgroundColor = backgroundColor;
    }

    public List<GuiModuleBase> getAllModulesAndSubModules() {
        List<GuiModuleBase> allModules = new ArrayList<>();
        for (GuiModuleBase i : modules) {
            allModules.add(i);
            if (i instanceof guiModuleScrollContainer container) {
                List<GuiModuleBase> sublist = container.getAllModulesAndSubModules();
                allModules.addAll(sublist);
            }
        }
        return allModules;
    }

    @Override
    public void client_onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOver(mouseX, mouseY, onGuiX, onGuiY, w, h)) {
            this.top_extra_offset += scrollY * 10;
        }
        this.top_extra_offset = Math.min(0, this.top_extra_offset);
        int maxY = 0;
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            maxY = Math.max(maxY, i.y);
        }
        this.top_extra_offset = Math.max(-maxY, this.top_extra_offset);
        setGuiOffset(left, top);
    }

    public void setGuiOffset(int left, int top) {
        this.left = left;
        this.top = top;
        onGuiX = x + left;
        onGuiY = y + top;
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.setGuiOffset(left + x, (int) (top + y + top_extra_offset));
        }
    }

    public void client_onKeyClick(int keyCode, int scanCode, int modifiers) {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.client_onKeyClick(keyCode, scanCode, modifiers);
        }
    }

    public void client_charTyped(char codePoint, int modifiers) {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.client_charTyped(codePoint, modifiers);
        }
    }

    public void client_onMouseClick(double x, double y, int button) {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.client_onMouseClick(x, y, button);
        }
    }

    public void client_onMouseDragged(double mouseX, double mouseY, double dragX, double dragY) {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.client_onMouseDragged(mouseX, mouseY, dragX, dragY);
        }
    }

    public void serverTick() {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.serverTick();
        }
    }

    public void server_readNetworkData(CompoundTag tag) {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.server_readNetworkData(tag);
        }
        super.server_readNetworkData(tag);
    }

    public void client_handleDataSyncedToClient(CompoundTag tag) {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.client_handleDataSyncedToClient(tag);
        }
        super.client_handleDataSyncedToClient(tag);
    }

    public void server_writeDataToSyncToClient(CompoundTag tag) {
        for (int n = 0; n < modules.size(); n++) {
            if (!(n < modules.size())) break;
            GuiModuleBase i = modules.get(n);
            i.server_writeDataToSyncToClient(tag);
        }
        super.server_writeDataToSyncToClient(tag);
    }

    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (isEnabled) {
            guiGraphics.fill(onGuiX, onGuiY, onGuiX + w, onGuiY + h, backgroundColor);
            guiGraphics.enableScissor(onGuiX, onGuiY, onGuiX + w, onGuiY + h);
            for (int n = 0; n < modules.size(); n++) {
                if (!(n < modules.size())) break;
                GuiModuleBase i = modules.get(n);
                i.render(guiGraphics, mouseX, mouseY, partialTick);
                i.checkMouseInScissor(guiGraphics, mouseX, mouseY, partialTick);
            }
            guiGraphics.disableScissor();
        }
    }

    public String getMyTagKey() {
        return "moduleTag" + this.id;
    }
}
