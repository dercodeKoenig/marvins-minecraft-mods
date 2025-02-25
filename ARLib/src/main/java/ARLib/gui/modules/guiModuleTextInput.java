package ARLib.gui.modules;


import ARLib.gui.IGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.lwjgl.glfw.GLFW;

public class guiModuleTextInput extends GuiModuleBase {
    public boolean isSelected = false;
    public int w, h;
    public String text = "";
    public int textInt = 0;
    public boolean isInt;

    public guiModuleTextInput(int id, IGuiHandler guiHandler, int x, int y, int w, int h, boolean isInt) {
        super(id, guiHandler, x, y);
        this.w = w;
        this.h = h;
        this.isInt = isInt;
    }

    public void client_onMouseCLick(double x, double y, int button) {
        if (client_isMouseOver(x, y, onGuiX, onGuiY, w, h)) {
            isSelected = true;
        } else {
            isSelected = false;
        }
    }

    public void server_writeDataToSyncToClient(CompoundTag tag) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            CompoundTag myTag = new CompoundTag();
            myTag.putString("text", text);
            tag.put(this.getMyTagKey(), myTag);
        }
        super.server_writeDataToSyncToClient(tag);
    }

    public void server_readNetworkData(CompoundTag tag) {
        if (tag.contains(this.getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(this.getMyTagKey());
            if (myTag.contains("text")) {
                if (!isInt) {
                    text = myTag.getString("text");
                } else {
                    textInt = 0;
                    String newText = myTag.getString("text");
                    try {
                        while (!newText.isEmpty() && newText.charAt(0) == '0')
                            newText = newText.substring(1);

                        textInt = Integer.parseInt(newText);
                        text = newText;
                    } catch (NumberFormatException e) {
                        if (newText.isEmpty())
                            text = "0";
                    }
                }
                broadcastModuleUpdate();
            }
        }
        super.server_readNetworkData(tag);
    }

    public void client_handleDataSyncedToClient(CompoundTag tag) {
        if (tag.contains(this.getMyTagKey())) {
            CompoundTag myTag = tag.getCompound(this.getMyTagKey());
            if (myTag.contains("text")) {
                text = myTag.getString("text");
            }
        }
        super.client_handleDataSyncedToClient(tag);
    }

    @Override
    public void client_charTyped(char codePoint, int modifiers) {

        text += codePoint;

        CompoundTag info = new CompoundTag();
        CompoundTag myTag = new CompoundTag();
        myTag.putString("text", text);
        info.put(this.getMyTagKey(), myTag);
        guiHandler.sendToServer(info);
    }

    @Override
    public void client_onKeyClick(int keyCode, int scanCode, int modifiers) {
        if (!isSelected) return;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            // Handle backspace for deleting characters
            if (!text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
            }
        }

        CompoundTag info = new CompoundTag();
        CompoundTag myTag = new CompoundTag();
        myTag.putString("text", text);
        info.put(this.getMyTagKey(), myTag);
        guiHandler.sendToServer(info);
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.isEnabled) {
            guiGraphics.fill(onGuiX - 1, onGuiY - 1, onGuiX + w + 1, onGuiY + h + 1, isSelected ? 0xffffffff : 0xff000000);
            guiGraphics.fill(onGuiX, onGuiY, onGuiX + w, onGuiY + h, 0xff000000);
            guiGraphics.drawString(Minecraft.getInstance().font, text, onGuiX + 1, onGuiY + h / 2 - Minecraft.getInstance().font.lineHeight / 2, 0xffffffff, false);
        }
    }
}
