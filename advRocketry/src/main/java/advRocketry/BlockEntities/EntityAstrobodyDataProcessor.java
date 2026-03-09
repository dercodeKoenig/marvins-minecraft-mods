package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.blockentities.EntityEnergyInputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.network.PacketBlockEntity;
import advRocketry.Config;
import advRocketry.Data.DataStack;
import advRocketry.Data.DataTypes;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Items.ItemGalaxyDatabase;
import advRocketry.Items.ItemPlanetIdChip;
import advRocketry.Registry.BlockEntities;
import advRocketry.Registry.Items;
import advRocketry.Render.starmap.SpaceMapScreen;
import advRocketry.Utils.AxisDirections;
import advRocketry.Utils.ClientUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;

public class EntityAstrobodyDataProcessor extends EntityMultiblockMachineMasterWithData {

    public static Object[][][] structure =
            new Object[][][]{
                    {{'s', 'c', 's'},
                            {'s', 's', 's'}},

                    {{'e', 'S', 'e'},
                            {'d', 'd', 'd'}}
            };
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        charMapping.put('c', List.of(advRocketry.Registry.Blocks.ASTROBODY_DATA_PROCESSOR.get()));
        charMapping.put('S', List.of(ARLibRegistry.BLOCK_STRUCTURE.get()));
        charMapping.put('d', List.of(advRocketry.Registry.Blocks.DATA_STORAGE_BLOCK.get()));
        charMapping.put('e', List.of(ARLibRegistry.BLOCK_ENERGY_INPUT_BLOCK.get()));
        charMapping.put('s', List.of(Blocks.STONE_SLAB));
    }

    public GuiHandlerBlockEntity guiHandler;
    public guiModuleItemHandlerSlot storageDiskSlot;
    public ItemStackHandler inventory;
    public guiModuleVerticalProgressBar dataBar1;
    public guiModuleVerticalProgressBar dataBar2;
    public guiModuleVerticalProgressBar dataBar3;
    public guiModuleVerticalProgressBar dataBarDatabase1;
    public guiModuleVerticalProgressBar dataBarDatabase2;
    public guiModuleVerticalProgressBar dataBarDatabase3;

    public EntityAstrobodyDataProcessor(BlockPos pos, BlockState state) {
        super(BlockEntities.ENTITY_ASTROBODY_DATA_PROCESSOR.get(), pos, state);
        //super.forwardInteractionToMaster = true;

        inventory = new ItemStackHandler(1) {
            @Override
            public void onContentsChanged(int slot) {
                setChanged();
            }
        };

        guiHandler = new GuiHandlerBlockEntity(this);
        int id = 0;
        guiModuleText title = new guiModuleText(id++, "Astrobody Data Processor", guiHandler, 5, 5, 0xff000000, false);
        guiHandler.modules.add(title);
        storageDiskSlot = new guiModuleItemHandlerSlot(id++, inventory, 0, 0, 1, guiHandler, 70, 80);
        guiHandler.modules.add(storageDiskSlot);

        dataBar1 = new guiModuleVerticalProgressBar(id++, guiHandler, 10, 20);
        dataBar1.bar = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_vertical_progress_bar_green.png");
        dataBarDatabase1 = new guiModuleVerticalProgressBar(id++, guiHandler, 40, 20);
        dataBarDatabase1.bar = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_vertical_progress_bar_green.png");
        guiHandler.modules.add(dataBar1);
        guiHandler.modules.add(dataBarDatabase1);

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 160, 100, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 100, 200, 1, 0, guiHandler));
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityAstrobodyDataProcessor) t).tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();

            if (getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
                Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

                BlockPos dataHatchPos1 = getBlockPos().below().relative(facing.getOpposite()).relative(facing.getClockWise());
                BlockPos dataHatchPos2 = getBlockPos().below().relative(facing.getOpposite());
                BlockPos dataHatchPos3 = getBlockPos().below().relative(facing.getOpposite()).relative(facing.getCounterClockWise());


                if (level.getBlockEntity(dataHatchPos1) instanceof EntityDataStorageBlock dataStorageBlock) {
                    DataStack dataStack1 = dataStorageBlock.dataStorage.getDataStack();

                    if (!guiHandler.playersTrackingGui.isEmpty()) {
                        if (dataStack1 != null)
                            dataBar1.setProgressAndSync((double) dataStack1.amount / dataStorageBlock.dataStorage.getDataCapacity());
                        else
                            dataBar1.setProgressAndSync(0);
                    }

                    ItemStack database = inventory.getStackInSlot(0);
                    boolean hasDatabase = database.getItem() instanceof ItemGalaxyDatabase;

                    if (hasDatabase && dataStack1 != null) {
                        ResourceLocation target = DataStack.split(dataStack1.type).getSecond();
                        String baseType = DataStack.split(dataStack1.type).getFirst();
                        ItemGalaxyDatabase.PlanetInfo info = ItemGalaxyDatabase.getPlanetInfo(database, target);
                        int data1OnDisk = 0;
                        if (info != null) {
                            if (baseType.equals(DataTypes.mass)) {
                                data1OnDisk = info.mass;
                            }
                            if (baseType.equals(DataTypes.distance)) {
                                data1OnDisk = info.distance;
                            }
                            if (baseType.equals(DataTypes.composition)) {
                                data1OnDisk = info.composition;
                            }
                        } else {
                            info = new ItemGalaxyDatabase.PlanetInfo();
                        }

                        dataBarDatabase1.setProgressAndSync((double) data1OnDisk / ItemGalaxyDatabase.POINTS_UNLOCKED());

                        if (data1OnDisk < ItemGalaxyDatabase.POINTS_UNLOCKED()) {
                            dataStorageBlock.dataStorage.extractData(1, false);
                            if (baseType.equals(DataTypes.mass)) {
                                info.mass += 1;
                            }
                            if (baseType.equals(DataTypes.distance)) {
                                info.distance += 1;
                            }
                            if (baseType.equals(DataTypes.composition)) {
                                info.composition += 1;
                            }
                            ItemGalaxyDatabase.setPlanetInfo(database, target, info);
                            setChanged();
                        }
                    }else{
                        dataBarDatabase1.setProgressAndSync(0);
                    }
                }
            }
        }
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        guiHandler.readServer(tag);
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    public void popInventory() {
        Block.popResource(level, getBlockPos(), inventory.getStackInSlot(0));
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }

    @Override
    public boolean shouldHideBlock(int y, int z, int x, BlockState stateInWorld) {
        return true;
    }

    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!world.isClientSide) {
            openGui((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    public void openGui(ServerPlayer player) {
        guiHandler.signalOpenGui(player, 176, 188, true);
    }
}
