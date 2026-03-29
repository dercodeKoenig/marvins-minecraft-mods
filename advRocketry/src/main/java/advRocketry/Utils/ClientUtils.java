package advRocketry.Utils;

import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Because the server shits itself when i use client classes in my code, i wrap the calls in another class.
 * This way i can make sure the classes never load on server side.
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

    public static Player getSinglePlayer() {
        return INSTANCE.clientOnly.getSinglePlayer();
    }

    public static Level getPlayerLevel() {
        return INSTANCE.clientOnly.getSinglePlayerLevel();
    }

    public static Dimension getPlayerDimension() {
        return DimensionManager.INSTANCE_CLIENT.get(getPlayerLevel().dimension().location());
    }

    static class ClientOnly {
        public Player getSinglePlayer() {
            return Minecraft.getInstance().player;
        }

        public Level getSinglePlayerLevel() {
            return Minecraft.getInstance().level;
        }
    }
}
