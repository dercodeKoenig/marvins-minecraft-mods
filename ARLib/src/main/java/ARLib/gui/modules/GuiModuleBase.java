package ARLib.gui.modules;

import ARLib.gui.IGuiHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;

public class GuiModuleBase {

    public int x;
    public int y;
    public int id;

    public boolean isEnabled = true;

    public boolean isMouseInScissor;

    protected int onGuiX;
    protected int onGuiY;

    protected IGuiHandler guiHandler;
    public GuiModuleBase(int id, IGuiHandler guiHandler, int x, int y) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.guiHandler = guiHandler;
        this.onGuiX = 0;
        this.onGuiY = 0;
    }

    public void broadcastModuleUpdate(){
        CompoundTag tag = new CompoundTag();
        server_writeDataToSyncToClient(tag);
        this.guiHandler.broadcastUpdate(tag);
    }

    public void setIsEnabledAndBroadcastUpdate(boolean isEnabled) {
        boolean needsUpdate = isEnabled != this.isEnabled;
        this.isEnabled = isEnabled;
        if (needsUpdate) {
            broadcastModuleUpdate();
        }
    }

    public void setGuiOffset(int left, int top){
        onGuiX = x+left;
        onGuiY = y+top;
    }

    public boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        if(!isEnabled)return false;
        if(!isMouseInScissor) return false;
        return mouseX >= x &&
                mouseX <= x + w &&
                mouseY >= y &&
                mouseY <= y + h;
    }

    public void client_onMouseScrolled(double mouseX,double  mouseY, double scrollX,double scrollY){

    }

    public void client_onMouseDragged(double mouseX,double  mouseY, double dragX,double dragY){

    }

    public void client_onMouseReleased(double x, double y, int button) {

    }

    public void client_onMouseClick(double x, double y, int button) {

    }

    public void client_onKeyClick(int keyCode, int scanCode, int modifiers) {

    }
    public void client_charTyped(char codePoint, int modifiers) {

    }

    public void server_readNetworkData(CompoundTag tag) {

    }

    public void client_handleDataSyncedToClient(CompoundTag tag) {
        if(tag.contains(getMySuperTagKey())) {
            CompoundTag myTag = tag.getCompound(getMySuperTagKey());
            if(myTag.contains("updateIsEnabled")){
                isEnabled = (myTag.getBoolean("updateIsEnabled"));
            }
        }
    }

    public void serverTick() {

    }

    public void server_writeDataToSyncToClient(CompoundTag tag){
        CompoundTag myTag = new CompoundTag();
        myTag.putBoolean("updateIsEnabled", isEnabled);
        tag.put(getMySuperTagKey(),myTag);
    }

    public  void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

    }
    public void checkMouseInScissor(GuiGraphics guiGraphics,
                                    int mouseX,
                                    int mouseY,
                                    float partialTick){
        isMouseInScissor = guiGraphics.containsPointInScissor(mouseX, mouseY);
    }

    public String getMyTagKey(){
        return "moduleTag"+this.id;
    }
    public String getMySuperTagKey(){
        return "moduleTag_s_"+this.id;
    }
}
