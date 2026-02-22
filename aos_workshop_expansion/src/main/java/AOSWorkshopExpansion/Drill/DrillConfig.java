package AOSWorkshopExpansion.Drill;

import AOSWorkshopExpansion.ConfigUtils;
import AOSWorkshopExpansion.Main;
import ARLib.network.SimpleNetworkPacket;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class DrillConfig implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static DrillConfig INSTANCE = new DrillConfig();

    public static String packetConfigSyncID = Main.MODID + "packet_drill_config";

    public float baseResistance;
    public float miningResistance;
    public float inertia;
    public float maxStress;

    public static void load() {
        INSTANCE = ConfigUtils.loadConfig(DrillConfig.class, "drill.json");
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(packetConfigSyncID, new Gson().toJson(DrillConfig.INSTANCE)));
        }
    }

    public void readClient(String data) {
        DrillConfig.INSTANCE = new Gson().fromJson(data, DrillConfig.class);
        System.out.println("client loaded drill config:" + data);
    }
}