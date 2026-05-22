package advRocketry.Items;

import ARLib.gui.GuiHandlerMainHandItem;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleScrollContainer;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketPlayerMainHand;
import advRocketry.Dimension.TerraformingSystem;
import advRocketry.Satellites.Satellite;
import advRocketry.Satellites.SatelliteBiomeChanger;
import advRocketry.Satellites.SatelliteManager;
import advRocketry.Utils.ClientUtils;
import advRocketry.Utils.ItemUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.checkerframework.checker.units.qual.C;

import java.util.*;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;

public class ItemBiomeChangerRemote extends ItemSatelliteIdChip implements INetworkTagReceiver {

    public GuiHandlerMainHandItem guiHandler = new GuiHandlerMainHandItem();

    public static void setScannedBiomes(ItemStack stack, List<ResourceLocation> biomes) {
        ListTag list = new ListTag();
        for (ResourceLocation id : biomes) {
            list.add(StringTag.valueOf(id.toString()));
        }
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        tag.put("scannedBiomes", list);
        ItemUtils.setTag(stack, tag);
    }

    public static List<ResourceLocation> getScannedBiomes(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains("scannedBiomes")) {
            ListTag list = tag.getList("scannedBiomes", Tag.TAG_STRING);
            return new ArrayList<>(list.stream().map(x -> ResourceLocation.parse(x.getAsString())).toList());
        }
        return new ArrayList<>();
    }

    public static void setSelected(ItemStack stack, ResourceLocation target) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        tag.putString("selected", target.toString());
        ItemUtils.setTag(stack, tag);
    }

    public static ResourceLocation getSelected(ItemStack stack) {
        CompoundTag tag = ItemUtils.getStacktagOrEmpty(stack);
        if (tag.contains("selected"))
            return ResourceLocation.parse(tag.getString("selected"));
        return null;
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(
                Component.literal(
                        "target: " + getTarget(stack)
                ).withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
                Component.literal(
                        "scanned biomes: " + getScannedBiomes(stack).size()
                ).withStyle(ChatFormatting.GRAY)
        );
        tooltipComponents.add(
                Component.literal(
                        "selected: " + getSelected(stack)
                ).withStyle(ChatFormatting.GRAY)
        );
    }

    public void makeGui(ItemStack stack) {
        List<GuiModuleBase> modules = new ArrayList<>();
        List<ResourceLocation> biomes = getScannedBiomes(stack);
        guiModuleButton scanButton = new guiModuleButton(-1, "scan biome", guiHandler, 50, 10, 100, 15, BTN_BLACK, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.put("scan", new CompoundTag());
                PacketDistributor.sendToServer(new PacketPlayerMainHand(info));
                if (guiHandler.getScreen() instanceof Screen screen)
                    screen.onClose();
                ClientUtils.getSinglePlayer().swing(InteractionHand.MAIN_HAND);
            }
        };
        scanButton.color = 0xffffffff;

        int id = 0;
        for (ResourceLocation biomeId : biomes) {
            guiModuleButton button = new guiModuleButton(id, biomeId.toString(), guiHandler, 0, id * 20 + 5, 180, 15, BTN_BLACK, BTN_W, BTN_H) {
                @Override
                public void onButtonClicked() {
                    CompoundTag info = new CompoundTag();
                    info.putString("select", biomeId.toString());
                    PacketDistributor.sendToServer(new PacketPlayerMainHand(info));
                    if (guiHandler.getScreen() instanceof Screen screen)
                        screen.onClose();
                    ClientUtils.getSinglePlayer().swing(InteractionHand.MAIN_HAND);
                }
            };
            button.color = 0xffffffff;
            modules.add(button);
            id+=1;
        }
        guiModuleScrollContainer container = new guiModuleScrollContainer(modules, 0xffa0a0a0, guiHandler, 10, 30, 180, 160);
        guiHandler.getModules().clear();
        guiHandler.getModules().add(container);
        guiHandler.getModules().add(scanButton);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        // scanning biomes is done on the remote and not with the satellite because the satellite will orbit another planet (moon) while you scan earth biomes to place them on moon
        ItemStack stack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.pass(stack);
        if (level instanceof ServerLevel serverLevel) {
            if (player.isShiftKeyDown()) {
                Satellite sat = SatelliteManager.getSatellite(getTarget(stack));
                if (sat instanceof SatelliteBiomeChanger biomeChanger) {
                    int r = 32;
                    ResourceLocation targetBiome = getSelected(stack);
                    List<Pair<Integer, Integer>> positions = new LinkedList<>();
                    for (int x = -r; x <= r; x++) {
                        for (int z = -r; z <= r; z++) {
                            if (Math.sqrt(x * x + z * z) <= r) {
                                positions.add(Pair.of(x, z));
                            }
                        }
                    }
                    Collections.shuffle(positions);
                    for (Pair<Integer,Integer> position : positions) {
                        int x = position.getFirst();
                        int z = position.getSecond();
                        biomeChanger.submitWork(serverLevel.dimension().location(), player.getBlockX() + x, player.getBlockZ() + z, targetBiome);
                    }
                    player.sendSystemMessage(Component.literal("request for biome change sent to satellite"));
                }
            }
        }
        if (level.isClientSide) {
            if (!player.isShiftKeyDown()) {
                makeGui(stack);
                guiHandler.openGui(200, 200, true);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        if (compoundTag.contains("select")) {
            String id = compoundTag.getString("select");
            setSelected(serverPlayer.getItemInHand(InteractionHand.MAIN_HAND), ResourceLocation.parse(id));
            serverPlayer.sendSystemMessage(Component.literal("selected " + id));
        }
        if (compoundTag.contains("scan")) {
            ResourceLocation biome = TerraformingSystem.getCurrentSurfaceBiome(serverPlayer.serverLevel(), serverPlayer.getBlockX(), serverPlayer.getBlockZ());
            ItemStack stack = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
            List<ResourceLocation> scannedBiomes = getScannedBiomes(stack);
            if (!scannedBiomes.contains(biome)) {
                scannedBiomes.add(biome);
                setScannedBiomes(stack, scannedBiomes);
                serverPlayer.sendSystemMessage(Component.literal("scanned " + biome));
            } else {
                serverPlayer.sendSystemMessage(Component.literal(biome + " was already scanned"));
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {

    }
}
