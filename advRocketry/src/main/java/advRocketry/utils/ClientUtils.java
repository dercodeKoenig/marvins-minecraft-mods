package advRocketry.utils;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Because the server shits itself when i use client classes in my code, i wrap the calls in another class.
 * This way i can make sure the classes never load on server side.
 * I think there was something similar called sided proxies in older version
 * <p>
 * Even if i try to shield calls with ``FMLEnvironment.dist.isClient()`` and use Minecraft.getInstance() this will still crash the server, so wrap it.
 * </p>
 * <p>
 *     i am not sure if this is the correct way to do it but it appears to be working
 * </p>
 */

public class ClientUtils {
    public static ClientUtils INSTANCE = new ClientUtils();

    // this should not trigger loading of client side classes
    ClientOnly clientOnly = null;

    public ClientUtils() {
        // if i only create the instance on client it should not trigger the class loading on server
        if (FMLEnvironment.dist.isClient()) {
            clientOnly = new ClientOnly();
        }
    }

    public static Player getSinglePlayer(){
        return INSTANCE.clientOnly.getSinglePlayer();
    }
    public static Level getPlayerLevel(){
        return INSTANCE.clientOnly.getSinglePlayerLevel();
    }
    public static Dimension getPlayerDimension(){
        return DimensionManager.INSTANCE_CLIENT.get(getPlayerLevel().dimension().location());
    }

    static class ClientOnly {
        // Player, not LocalPlayer
        public Player getSinglePlayer() {
            return Minecraft.getInstance().player;
        }

        // Level, not ClientLevel
        public Level getSinglePlayerLevel() {
            return Minecraft.getInstance().level;
        }
    }
}
