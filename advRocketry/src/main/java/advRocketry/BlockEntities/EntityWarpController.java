package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModuleSlider;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.PacketBlockEntity;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.Items.ItemGalaxyStorageDisk;
import advRocketry.Registry;
import advRocketry.Render.starmap.GuiModulePlanetView;
import advRocketry.Render.starmap.SpaceMapScreen;
import advRocketry.utils.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_STATION_CONTROLLER;
import static advRocketry.Registry.ENTITY_WARP_CONTROLLER;

public class EntityWarpController extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    GuiHandlerBlockEntity guiHandler;
    public ItemStackHandler galaxyStorage;
    public guiModuleItemHandlerSlot galaxyStorageGuiSlot;
    public GuiModulePlanetView planetView;

    public EntityWarpController(BlockPos pos, BlockState blockState) {
        super(ENTITY_WARP_CONTROLLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        galaxyStorage = new ItemStackHandler(1) {
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem().equals(Registry.ITEM_GALAXY_STORAGE_DISK.get());
            }

            public void onContentsChanged(int slot) {
                EntityWarpController.this.setChanged();
            }
        };

        galaxyStorageGuiSlot = new guiModuleItemHandlerSlot(0, galaxyStorage, 0, 0, 1, guiHandler, 90, 9);
        guiHandler.modules.add(galaxyStorageGuiSlot);

        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            guiHandler.modules.add(
                    new ARLib.gui.modules.guiModuleButton(100, "open galaxy", guiHandler, 10, 10, 70, 15, BTN_BLACK, BTN_W, BTN_H) {
                        public void onButtonClicked() {
                            Minecraft.getInstance().setScreen(
                                    new SpaceMapScreen() {
                                        @Override
                                        public void tick() {
                                            super.tick();
                                            // make sure the main gui stays in sync
                                            EntityWarpController.this.guiHandler.onGuiClientTick(ClientUtils.getSinglePlayer());
                                        }

                                        @Override
                                        public void onClose() {
                                            super.onClose();
                                            // open the main gui again
                                            openGui();
                                        }

                                        public void interact(ResourceLocation dimensionId) {
                                            CompoundTag info = new CompoundTag();
                                            info.putString("interact", dimensionId.toString());
                                            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityWarpController.this, info));
                                            openGui();
                                        }

                                        public String getInteractText(ResourceLocation dimensionId) {
                                            PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
                                            if (planet == null) return "";
                                            if (planet.isKnown() || clientGetDiscoverStatusFromCurrentStorageItem(dimensionId) == ItemGalaxyStorageDisk.POINTS_UNLOCKED()) {
                                                return "select";
                                            }
                                            return "";
                                        }

                                        public String getPlanetInfoText(ResourceLocation dimensionId) {
                                            PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
                                            if (planet == null) return "";

                                            if (!planet.isKnown() && clientGetDiscoverStatusFromCurrentStorageItem(dimensionId) != ItemGalaxyStorageDisk.POINTS_UNLOCKED()) {
                                                return "We require more information about this planet.";
                                            }

                                            return super.getPlanetInfoText(dimensionId);
                                        }

                                        public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
                                            Dimension d = DimensionManager.INSTANCE_CLIENT.get(dimensionId);
                                            if (d == null) return false;

                                            if (((PlanetDimension) (d)).isKnown()) {
                                                return true;
                                            }

                                            int discoverStatus = clientGetDiscoverStatusFromCurrentStorageItem(dimensionId);
                                            if (discoverStatus != -1)
                                                return true;

                                            return false;
                                        }
                                    }
                            );
                        }
                    }
            );
        }

        planetView = new GuiModulePlanetView(22,guiHandler, 10,30,120,120);
        planetView.setTargetAndSync(ResourceLocation.fromNamespaceAndPath("minecraft", "overworld"));
        guiHandler.modules.add(planetView);


        guiHandler.modules.addAll(ARLib.gui.modules.guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 175, 10000, 1, 0, guiHandler));
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWarpController) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if(compoundTag.contains("interact")){
            String dimId =compoundTag.getString("interact");
            planetView.setTargetAndSync(ResourceLocation.tryParse(dimId));
        }
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
            if (myDim instanceof SpaceStationDimension spaceStationDimension) {

            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(200, 200, true);
    }

        // helper methods for gui rendering
        public int clientGetDiscoverStatusFromCurrentStorageItem(ResourceLocation dimensionId) {
            return ItemGalaxyStorageDisk.getUnlockPoints(galaxyStorageGuiSlot.client_getItemStackToRender(), dimensionId.toString());
        }
}
