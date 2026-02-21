package AOSWorkshopExpansion.Piston;

import AOSWorkshopExpansion.ConfigUtils;
import AOSWorkshopExpansion.Main;
import ARLib.network.SimpleNetworkPacket;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class PistonConfig implements SimpleNetworkPacket.SimpleNetworkDataReceiver {

    public static PistonConfig INSTANCE = new PistonConfig();

    public static String packetConfigSyncID = Main.MODID + "packet_piston_config";

    public float baseResistance;
    public float perBlockResistance;
    public float inertia;
    public float maxStress;

    public static void load(){
        INSTANCE = ConfigUtils.loadConfig(PistonConfig.class, "piston.json");
    }

    public static void SyncConfig(ServerPlayer p) {
        if (p != null) {
            PacketDistributor.sendToPlayer(p, new SimpleNetworkPacket(packetConfigSyncID, new Gson().toJson(PistonConfig.INSTANCE)));
        }
    }
    public void readClient(String data) {
        PistonConfig.INSTANCE = new Gson().fromJson(data, PistonConfig.class);
        System.out.println("client loaded piston config:" + data);
    }
}