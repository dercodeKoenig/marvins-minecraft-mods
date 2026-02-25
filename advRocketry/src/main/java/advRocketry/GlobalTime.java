package advRocketry;

import ARLib.network.SimpleNetworkPacket;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * this class provides a universal timer that counts the total ticks since a world is created
 * it will automatically sync the universal time to client
 * it will interpolate smoothly between target time and current time on client
 */

public class GlobalTime implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static final String saveFile = "universalTime.txt";
    public static final int syncTimeAfterTicks = 20 * 60; // sync time once per minute in case ticks have been skipped
    public static String PACKET_ID_SYNCTIME = "adv_rocketry_globaltime";

    public static GlobalTime INSTANCE = new GlobalTime();

    public long universalTimeServer = 0; // time on server
    public long universalTimeClient = 0;
    public float universalTimeClientCorrection = 0;
    public long universalTimeClientTarget = 0;

    public static long getGlobalTime() {
        if (FMLLoader.getDist().isClient()) {
            return INSTANCE.universalTimeClient; // client or integrated server use this
        } else {
            return INSTANCE.universalTimeServer; // dedicated server use this
        }
    }

    public static float getGlobalTimeClientCorrection() {
        if (FMLLoader.getDist().isClient()) {
            return INSTANCE.universalTimeClientCorrection;
        } else {
            return 0; // dedicated server does not have this
        }
    }

    public static void tickServer() {
        INSTANCE.universalTimeServer+=1;
        if (INSTANCE.universalTimeServer % syncTimeAfterTicks == 0) {
            PacketDistributor.sendToAllPlayers(
                    new SimpleNetworkPacket(PACKET_ID_SYNCTIME, String.valueOf(INSTANCE.universalTimeServer))
            );
        }
    }

    public static void tickClient() {
        INSTANCE.universalTimeClient+=1;
        INSTANCE.universalTimeClientTarget+=1;

        // to smoothly interpolate between the time difference
        INSTANCE.universalTimeClient += (int) INSTANCE.universalTimeClientCorrection;
        INSTANCE.universalTimeClientCorrection = INSTANCE.universalTimeClientCorrection % 1;
        long targetDiff_L = INSTANCE.universalTimeClientTarget - INSTANCE.universalTimeClient;
        float targetDiff_G = targetDiff_L - INSTANCE.universalTimeClientCorrection;
        INSTANCE.universalTimeClientCorrection += (targetDiff_G / 100);

    }

    @Override
    public void readClient(String data) {
        universalTimeClientTarget = Long.parseLong(data);
        if (Math.abs(universalTimeClientTarget- universalTimeClient) > 20){
            System.out.println("client universal time significantly off target by "+(universalTimeClientTarget- universalTimeClient));
        }

    }

    public static void save() {
        Path saveFile = Path.of(String.valueOf(Main.worldPath), GlobalTime.saveFile);
        System.out.println("saving universal time...");
        try {
            Files.writeString(saveFile, String.valueOf(INSTANCE.universalTimeServer));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("saved universal time: " + INSTANCE.universalTimeServer);
    }

    public static void load() {
        Path saveFile = Path.of(String.valueOf(Main.worldPath), GlobalTime.saveFile);
        System.out.println("reading saved universal time...");
        // reset in case file does not exist
        INSTANCE.universalTimeServer = 0;
        // read
        try {
            String content = Files.readString(saveFile);
            INSTANCE.universalTimeServer = Long.parseLong(content);
        } catch (IOException e) {
            System.out.println("could not read universal time from file");
        }
        //sync
        //INSTANCE.universalTimeClient = INSTANCE.universalTimeServer;
        //INSTANCE.universalTimeClientTarget = INSTANCE.universalTimeServer;

        System.out.println("universal time: " + INSTANCE.universalTimeServer);
    }
}