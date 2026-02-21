package AOSWorkshopExpansion.Conveyor;

import AOSWorkshopExpansion.ConfigUtils;
import AOSWorkshopExpansion.Main;
import ARLib.network.SimpleNetworkPacket;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConveyorConfig implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static ConveyorConfig INSTANCE = new ConveyorConfig();

    public static String packetConfigSyncID = Main.MODID + "packet_conveyor_config";

    public float conveyorResistance = 1;
    public float conveyorInertia = 1;
    public float conveyorMaxStress = 600;

    public float conveyorEngineResistance = 1;
    public float conveyorEngineInertia = 1;
    public float conveyorEngineMaxStress = 600;

    public static void load() {
        INSTANCE = ConfigUtils.loadConfig(ConveyorConfig.class, "conveyors.json");
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(packetConfigSyncID, new Gson().toJson(ConveyorConfig.INSTANCE)));
        }
    }

    public void readClient(String data) {
        ConveyorConfig.INSTANCE = new Gson().fromJson(data, ConveyorConfig.class);
        System.out.println("client loaded conveyor config:" + data);
    }
}