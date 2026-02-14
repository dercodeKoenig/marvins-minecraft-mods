package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleItemHandlerSlot;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import ARLib.network.PacketBlockEntity;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Registry;
import advRocketry.Render.starmap.SpaceMapScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;

public class EntityObservatory extends EntityMultiblockMachineMaster {

    public MeshData meshAxle;
    public MeshData meshScope;
    public MeshData meshCasingXPlus;
    public MeshData meshCasingXMinus;
    public MeshData meshBase;

    public VertexBuffer axle;
    public VertexBuffer scope;
    public VertexBuffer casingXPlus;
    public VertexBuffer casingXMinus;
    public VertexBuffer base;

    public int lastLight;


    public ItemStackHandler itemStackHandler;


    public GuiHandlerBlockEntity guiHandler;
    ARLib.gui.modules.guiModuleItemHandlerSlot storageDiskSlot1;
    ARLib.gui.modules.guiModuleItemHandlerSlot storageDiskSlot2;
    ARLib.gui.modules.guiModuleItemHandlerSlot planetIdChipSlot;


    public EntityObservatory(BlockPos pos, BlockState state) {
        super(Registry.ENTITY_OBSERVATORY.get(), pos, state);
        super.forwardInteractionToMaster = true;

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                axle = new VertexBuffer(VertexBuffer.Usage.STATIC);
                scope = new VertexBuffer(VertexBuffer.Usage.STATIC);
                casingXMinus = new VertexBuffer(VertexBuffer.Usage.STATIC);
                casingXPlus = new VertexBuffer(VertexBuffer.Usage.STATIC);
                base = new VertexBuffer(VertexBuffer.Usage.STATIC);
            });
        }

        guiHandler = new GuiHandlerBlockEntity(this);
        itemStackHandler = new ItemStackHandler(3) {
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == 0 || slot == 1)
                    return stack.getItem().equals(Registry.ITEM_GALAXY_STORAGE_DISK.get());
                if (slot == 2)
                    return stack.getItem().equals(Registry.ITEM_PLANET_ID_CHIP.get());
                return false;
            }
        };
        storageDiskSlot1 = new guiModuleItemHandlerSlot(0, itemStackHandler, 0, 1, 0, guiHandler, 130, 160);
        guiHandler.modules.add(storageDiskSlot1);
        storageDiskSlot2 = new guiModuleItemHandlerSlot(1, itemStackHandler, 1, 1, 0, guiHandler, 150, 160);
        guiHandler.modules.add(storageDiskSlot2);
        guiHandler.modules.add(
                new ARLib.gui.modules.guiModuleText(3, "galaxy data storage:", guiHandler, 10, 163, 0xff000000, false)
        );

        planetIdChipSlot = new guiModuleItemHandlerSlot(4, itemStackHandler, 2, 1, 0, guiHandler, 150, 140);
        guiHandler.modules.add(planetIdChipSlot);
        guiHandler.modules.add(
                new ARLib.gui.modules.guiModuleText(5, "planet id chip:", guiHandler, 10, 143, 0xff000000, false)
        );

        guiHandler.modules.add(
                new ARLib.gui.modules.guiModuleButton(100, "open galaxy", guiHandler, 10, 10, 70, 15, ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_button_black.png"), 64, 20) {
                    public void onButtonClicked() {
                        Minecraft.getInstance().setScreen(
                                new SpaceMapScreen() {
                                    @Override
                                    public void tick() {
                                        super.tick();
                                        // make sure the main gui stays in sync
                                        EntityObservatory.this.guiHandler.sendPing();
                                    }

                                    @Override
                                    public void onClose() {
                                        super.onClose();
                                        // open the main gui again
                                        openGui();
                                    }

                                    public void interact(ResourceLocation dimensionId) {
                                        CompoundTag info = new CompoundTag();
                                        info.putString("writeToChip", dimensionId.toString());
                                        PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityObservatory.this, info));
                                    }

                                    public String getInteractText(ResourceLocation dimensionId) {
                                        return "interact with " + dimensionId;
                                    }

                                    public String getPlanetInfoText(ResourceLocation dimensionId) {
                                        return "can visit:" + DimensionManager.INSTANCE_CLIENT.get(dimensionId).canVisit();
                                    }

                                    public boolean shouldRenderPlanet(ResourceLocation dimensionId) {
                                        Dimension d = DimensionManager.INSTANCE_CLIENT.get(dimensionId);
                                        return ((PlanetDimension) (d)).isKnown();
                                    }
                                }
                        );
                    }
                }
        );

        guiHandler.modules.addAll(ARLib.gui.modules.guiModulePlayerInventorySlot.makePlayerHotbarModules(15, 185, 10000, 0, 1, guiHandler));
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                axle.close();
                scope.close();
                casingXPlus.close();
                casingXMinus.close();
                base.close();
            });
        }
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityObservatory) t).tick();
    }

    public void openGui() {
        guiHandler.openGui(200, 210, true);
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        guiHandler.readServer(tag);
        if (tag.contains("writeToChip")) {
            if (itemStackHandler.getStackInSlot(2).getItem() instanceof ItemPlanetIdChip planetIdChip) {
                ResourceLocation target = ResourceLocation.parse(tag.getString("writeToChip"));
                ItemPlanetIdChip.setSelectedDimension(target, itemStackHandler.getStackInSlot(2));
            }
        }
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
        if (tag.contains("openGui")) {
            openGui();
        }
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("storageItemStackHandler", itemStackHandler.serializeNBT(registries));
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemStackHandler.deserializeNBT(registries, tag.getCompound("storageItemStackHandler"));
    }

    public void popInventory() {
        for (int i = 0; i < itemStackHandler.getSlots(); i++) {
            Block.popResource(level, getBlockPos(), itemStackHandler.getStackInSlot(i));
        }
    }


    public static Object[][][] structure =
            new Object[][][]{
                    {{null, null, null, null, null},
                            {null, 's', 'g', 's', null},
                            {null, 's', 's', 's', null},
                            {null, 's', 's', 's', null},
                            {null, null, null, null, null}},

                    {{null, null, null, null, null},
                            {null, 's', 's', 's', null},
                            {null, 's', 'g', 's', null},
                            {null, 's', 's', 's', null},
                            {null, null, null, null, null}},

                    {{null, 's', 's', 's', null},
                            {'s', 'a', 'a', 'a', 's'},
                            {'s', 'a', 'a', 'a', 's'},
                            {'s', 'a', 'g', 'a', 's'},
                            {null, 's', 's', 's', null}},

                    {{null, '*', 'c', '*', null},
                            {'*', 's', 's', 's', '*'},
                            {'*', 's', 's', 's', '*'},
                            {'*', 's', 's', 's', '*'},
                            {null, '*', '*', '*', null}},

                    {{null, '*', '*', '*', null},
                            {'*', 't', 't', 't', '*'},
                            {'*', 't', 'm', 't', '*'},
                            {'*', 't', 't', 't', '*'},
                            {null, '*', '*', '*', null}}};

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(Registry.OBSERVATORY.get()));
        charMapping.put('s', List.of(ARLibRegistry.BLOCK_STRUCTURE.get()));
        charMapping.put('t', List.of(Registry.STRUCTURE_TOWER.get()));
        charMapping.put('g', List.of(Blocks.GLASS));
        charMapping.put('a', List.of(Blocks.AIR));
        charMapping.put('m', List.of(ARLibRegistry.BLOCK_MOTOR.get()));
        charMapping.put('*', List.of(
                ARLibRegistry.BLOCK_STRUCTURE.get(),
                ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get(),
                ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()
        ));
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }


    @Override
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld) {
        Block block = stateInWorld.getBlock();
        if (block.equals(ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()))
            return false;
        if (block.equals(ARLibRegistry.BLOCK_ITEM_INPUT_BLOCK.get()))
            return false;
        if (block.equals(ARLibRegistry.BLOCK_ITEM_OUTPUT_BLOCK.get()))
            return false;

        return true;
    }


    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!world.isClientSide) {
            CompoundTag info = new CompoundTag();
            info.put("openGui", new CompoundTag());
            PacketDistributor.sendToPlayer((ServerPlayer) player, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        return InteractionResult.SUCCESS;
    }

}
