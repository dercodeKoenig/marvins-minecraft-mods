package ARLib.utils;


import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

public class ClientUtils {
    public static ClientUtils INSTANCE = new ClientUtils();

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

    static class ClientOnly {
        public Player getSinglePlayer() {
            return Minecraft.getInstance().player;
        }

        public Level getSinglePlayerLevel() {
            return Minecraft.getInstance().level;
        }
    }
}
