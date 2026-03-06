package AgeOfSteam;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientUtils {
    // wrapped in player because local player is not existing on server
    public static Player getLocalPlayer(){
        return Minecraft.getInstance().player;
    }
}
