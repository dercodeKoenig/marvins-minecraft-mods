package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.gui.modules.guiModuleText;
import ARLib.network.PacketBlockEntity;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Dimension.SpaceStationDimension;
import advRocketry.Items.ItemGalaxyDatabase;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_WARP_CONTROLLER;

public class EntityWarpController extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler;
    public ItemStackHandler galaxyStorage;
    public guiModuleItemHandlerSlot galaxyStorageGuiSlot;
    public GuiModulePlanetView targetView;
    public GuiModulePlanetView currentView;
    public guiModuleText inOrbitText;
    public guiModuleText targetText;
    public guiModuleText statusText;

    public EntityWarpController(BlockPos pos, BlockState blockState) {
        super(ENTITY_WARP_CONTROLLER.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        galaxyStorage = new ItemStackHandler(1) {
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem().equals(Registry.ITEM_GALAXY_DATABASE.get());
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
                                            if (planet.isKnown() || client_IsDistanceUnlocked(dimensionId)) {
                                                return "select";
                                            }
                                            return "";
                                        }

                                        public String getPlanetInfoText(ResourceLocation dimensionId) {
                                            PlanetDimension planet = ((PlanetDimension) DimensionManager.INSTANCE_CLIENT.get(dimensionId));
                                            if (planet == null) return "";

                                            if (!planet.isKnown() && !client_IsDistanceUnlocked(dimensionId)) {
                                                return "We require more information about this planet.";
                                            }

                                            return super.getPlanetInfoText(dimensionId, client_getPlanetInfo(dimensionId));
                                        }

                                        public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
                                            Dimension d = DimensionManager.INSTANCE_CLIENT.get(dimensionId);
                                            if (d == null) return false;

                                            if (((PlanetDimension) (d)).isKnown()) {
                                                return true;
                                            }

                                            if (client_IsDimensionKnown(dimensionId))
                                                return true;

                                            return false;
                                        }
                                    }
                            );
                        }
                    }
            );
        }


        inOrbitText = new guiModuleText(32, "In Orbit:", guiHandler, 10, 30, 0xff000000, false);
        guiHandler.modules.add(inOrbitText);

        targetText = new guiModuleText(33, "Target:", guiHandler, 130, 30, 0xff000000, false);
        guiHandler.modules.add(targetText);

        currentView = new GuiModulePlanetView(22, guiHandler, 10, 50, 110, 110);
        guiHandler.modules.add(currentView);

        targetView = new GuiModulePlanetView(23, guiHandler, 130, 50, 110, 110);
        guiHandler.modules.add(targetView);

        guiModuleButton warpBtn = new guiModuleButton(339, "travel", guiHandler, 130, 165, 50, 15, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(warpBtn);

        guiModuleButton clearBtn = new guiModuleButton(340, "clear", guiHandler, 190, 165, 50, 15, BTN_BLACK, BTN_W, BTN_H);
        guiHandler.modules.add(clearBtn);

        statusText = new guiModuleText(87, "status", guiHandler, 10, 165, 0xff000000, false);
        guiHandler.modules.add(statusText);

        guiHandler.modules.addAll(ARLib.gui.modules.guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 195, 10000, 1, 0, guiHandler));
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWarpController) t).tick();
    }


    public void popInventory() {
        for (int i = 0; i < galaxyStorage.getSlots(); i++) {
            Block.popResource(level, getBlockPos(), galaxyStorage.getStackInSlot(i));
            galaxyStorage.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("interact")) {
            String dimId = compoundTag.getString("interact");
            // just one additional check to make sure the client did not cheat...
            Dimension dim = DimensionManager.INSTANCE_SERVER.get(ResourceLocation.parse(dimId));
            if (dim instanceof PlanetDimension planetDimension) {
                if (planetDimension.isKnown() || ItemGalaxyDatabase.isDistanceUnlocked(galaxyStorage.getStackInSlot(0), dimId)) {
                    targetView.setTargetAndSync(ResourceLocation.tryParse(dimId));
                    setChanged();
                }
            }
        }
        if (compoundTag.contains("guiButtonClick")) {
            int btn = compoundTag.getInt("guiButtonClick");
            Dimension myDim = DimensionManager.INSTANCE_SERVER.get(level.dimension().location());
            if (myDim instanceof SpaceStationDimension spaceStationDimension) {
                if (btn == 339) {
                    // warp!
                    spaceStationDimension.setTargetPlanet(targetView.dimensionId);
                    //guiHandler.signalCloseGui(serverPlayer);
                }
            }
            if (btn == 340) {
                // clear target
                targetView.setTargetAndSync(null);
                setChanged();
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
        if (targetView.dimensionId != null)
            tag.putString("targetView", targetView.dimensionId.toString());
        tag.put("galaxyStorage", galaxyStorage.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("targetView"))
            targetView.setTargetAndSync(ResourceLocation.tryParse(tag.getString("targetView")));
        galaxyStorage.deserializeNBT(registries, tag.getCompound("galaxyStorage"));
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();

            if (DimensionManager.INSTANCE_SERVER.get(level.dimension().location()) instanceof SpaceStationDimension spaceStation) {
                if (spaceStation.isInOrbit()) {
                    currentView.setTargetAndSync(spaceStation.getParentDimensionId());
                } else {
                    currentView.setTargetAndSync(null);
                }

                Dimension currentOrbitedPlanet = currentView.dimensionId == null ? null : DimensionManager.INSTANCE_SERVER.get(currentView.dimensionId);
                Dimension targetPlanet = targetView.dimensionId == null ? null : DimensionManager.INSTANCE_SERVER.get(targetView.dimensionId);

                String inOrbitString = "Space";
                if (currentOrbitedPlanet != null && spaceStation.isInOrbit())
                    inOrbitString = currentOrbitedPlanet.getName();
                inOrbitText.setTextAndSync("In Orbit:\n" + inOrbitString);

                String targetString = "Space";
                if (targetPlanet != null)
                    targetString = targetPlanet.getName();
                targetText.setTextAndSync("Target:\n" + targetString);

                if (targetPlanet != null && spaceStation.isInSpaceTravel() && targetPlanet.getDimensionId().equals(spaceStation.getParentDimensionId())) {
                    String text = "In Space Travel\n";
                    double distance = spaceStation.getPosition(0).distanceTo(targetPlanet.getPosition(0));
                    distance = (double) Math.round(distance * 100) / 100;
                    text += "Distance: " + distance + " AU";
                    statusText.setTextAndSync(text);
                } else if (targetPlanet != null && targetPlanet != currentOrbitedPlanet) {
                    double distance = spaceStation.getPosition(0).distanceTo(targetPlanet.getPosition(0));
                    distance = (double) Math.round(distance * 100) / 100;
                    String text = "Distance to target:\n" + distance + " AU";
                    statusText.setTextAndSync(text);
                } else {
                    statusText.setTextAndSync("");
                }
            }
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(250, 220, true);
    }

    // helper methods for gui rendering
    public boolean client_IsDimensionKnown(ResourceLocation dimensionId) {
        return ItemGalaxyDatabase.isDimensionKnown(galaxyStorageGuiSlot.client_getItemStackToRender(), dimensionId.toString());
    }

    public boolean client_IsDistanceUnlocked(ResourceLocation dimensionId) {
        return ItemGalaxyDatabase.isDistanceUnlocked(galaxyStorageGuiSlot.client_getItemStackToRender(), dimensionId.toString());
    }

    public ItemGalaxyDatabase.PlanetInfo client_getPlanetInfo(ResourceLocation dimensionId) {
        return ItemGalaxyDatabase.getPlanetInfo(galaxyStorageGuiSlot.client_getItemStackToRender(), dimensionId.toString());
    }
}
