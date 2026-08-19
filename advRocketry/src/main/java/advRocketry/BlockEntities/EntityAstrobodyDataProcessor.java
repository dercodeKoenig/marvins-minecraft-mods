package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.blockentities.EntityEnergyInputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.BlockMultiblockMaster;
import advRocketry.Config;
import advRocketry.Data.DataStack;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Items.ItemGalaxyDatabase;
import advRocketry.Registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

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

    public ItemStackHandler inventory;
    public GuiHandlerBlockEntity guiHandler;
    public guiModuleItemHandlerSlot storageDiskSlot;
    public guiModuleVerticalProgressBar energyBar;
    public guiModuleButton deleteLeftoverDataBtn;
    public List<WorkingData> workingData = new ArrayList<>();
    int deleteLeftoverDataBtnId = 1123452374;
    boolean deleteLeftoverData = false;

    public EntityAstrobodyDataProcessor(BlockPos pos, BlockState state) {
        super(BlockEntities.ENTITY_ASTROBODY_DATA_PROCESSOR.get(), pos, state);
        //super.forwardInteractionToMaster = true;

        inventory = new ItemStackHandler(1) {
            @Override
            public void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };


        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        guiHandler = new GuiHandlerBlockEntity(this);

        guiModuleText title = new guiModuleText(-1, "Astrobody Data Processor", guiHandler, 5, 5, 0xff000000, false);
        guiHandler.modules.add(title);
        storageDiskSlot = new guiModuleItemHandlerSlot(0, inventory, 0, 0, 1, guiHandler, 150, 78);
        guiHandler.modules.add(storageDiskSlot);

        BlockPos dataHatchPos1 = getBlockPos().below().relative(facing.getOpposite()).relative(facing.getClockWise());
        BlockPos dataHatchPos2 = getBlockPos().below().relative(facing.getOpposite());
        BlockPos dataHatchPos3 = getBlockPos().below().relative(facing.getOpposite()).relative(facing.getCounterClockWise());
        workingData.add(new WorkingData(0, guiHandler, dataHatchPos1));
        workingData.add(new WorkingData(1, guiHandler, dataHatchPos2));
        workingData.add(new WorkingData(2, guiHandler, dataHatchPos3));

        energyBar = new guiModuleVerticalProgressBar(8897964, guiHandler, 155, 20);
        guiHandler.modules.add(energyBar);

        deleteLeftoverDataBtn = new guiModuleButton(deleteLeftoverDataBtnId, "clear leftover data", guiHandler, 10, 80, 110, 15, BTN_RED, BTN_W, BTN_H);
        guiHandler.modules.add(deleteLeftoverDataBtn);

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

    void processData(WorkingData data, List<EntityEnergyInputBlock> energyInputBlocks) {
        if (level.getBlockEntity(data.dataHatchPos) instanceof EntityDataStorageBlock dataStorageBlock) {
            DataStack dataStack = dataStorageBlock.dataStorage.getDataStack();
            int amount = 0;

            if (dataStack != null) {
                amount = dataStack.amount;
                data.lastType = dataStack.type;
                data.keepTypeTimeout = 20 * 30;
            } else {
                data.keepTypeTimeout--;
                if (data.keepTypeTimeout <= 0) {
                    data.lastType = null;
                }
            }

            if (!guiHandler.playersTrackingGui.isEmpty()) {
                data.dataBar.setProgressAndSync((double) amount / dataStorageBlock.dataStorage.getDataCapacity());
                if (data.lastType != null)
                    data.dataBar.setHoverInfoAndSync(data.lastType + ": " + amount + " / " + dataStorageBlock.dataStorage.getDataCapacity());
                else
                    data.dataBar.setHoverInfoAndSync("");
            }


            ItemStack database = inventory.getStackInSlot(0);

            if (database.getItem() instanceof ItemGalaxyDatabase && data.lastType != null) {
                ResourceLocation targetId = DataStack.split(data.lastType).getSecond();
                PlanetDimension targetPlanet = (PlanetDimension) DimensionManager.INSTANCE_SERVER.get(targetId);
                String baseType = DataStack.split(data.lastType).getFirst();
                int requiredData = ItemGalaxyDatabase.POINTS_UNLOCKED(targetPlanet);
                ItemGalaxyDatabase.PlanetInfo info = ItemGalaxyDatabase.getPlanetInfo(database, targetPlanet);
                int dataOnDisk = 0;
                if (info != null) {
                    dataOnDisk = info.get(baseType);
                } else {
                    info = new ItemGalaxyDatabase.PlanetInfo();
                }

                if (!guiHandler.playersTrackingGui.isEmpty()) {
                    data.dataBarDatabase.setProgressAndSync((double) dataOnDisk / requiredData);
                    data.dataBarDatabase.setHoverInfoAndSync(data.lastType + ": " + dataOnDisk + " / " + requiredData);
                }

                if (dataOnDisk < requiredData &&
                        dataStack != null &&
                        dataStack.amount > 0 &&
                        targetPlanet != null &&
                        super.getTotalEnergyStored(energyInputBlocks) >= Config.INSTANCE.astrobody_Data_Processor_Energy_Per_Tick
                ) {
                    super.consumeEnergy(Config.INSTANCE.astrobody_Data_Processor_Energy_Per_Tick, energyInputBlocks);
                    if (Math.random() < 0.1) {
                        dataStorageBlock.dataStorage.extractData(1, false);
                        info.put(baseType, dataOnDisk + 1);
                        ItemGalaxyDatabase.setPlanetInfo(database, targetPlanet, info);
                    }
                    setChanged();
                } else if (
                        (dataOnDisk >= requiredData || targetPlanet == null) &&
                                deleteLeftoverData
                ) {
                    // clear leftover data
                    // database has to be inserted to check if dataOnDisk >= requiredData
                    dataStorageBlock.dataStorage.extractData(1, false);
                }
            } else {
                if (!guiHandler.playersTrackingGui.isEmpty()) {
                    data.dataBarDatabase.setProgressAndSync(0);
                    data.dataBarDatabase.setHoverInfoAndSync("");
                }
            }
        }
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();
            List<EntityEnergyInputBlock> energyInputBlocks = super.getEnergyInputTiles();
            if (getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
                for (WorkingData workingDatum : workingData) {
                    processData(workingDatum, energyInputBlocks);
                }
            }
            if (!guiHandler.playersTrackingGui.isEmpty()) {
                int energy = super.getTotalEnergyStored(energyInputBlocks);
                int maxEnergy = super.getMaxEnergyStored(energyInputBlocks);
                energyBar.setProgressAndSync((double) energy / maxEnergy);
                energyBar.setHoverInfoAndSync("rf: " + energy + " / " + maxEnergy);
                if (deleteLeftoverData)
                    deleteLeftoverDataBtn.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                else
                    deleteLeftoverDataBtn.setBackgroundAndSync(BTN_RED, BTN_W, BTN_H);
            }
        }
    }

    @Override
    public void readServer(CompoundTag tag, ServerPlayer player) {
        guiHandler.readServer(tag);
        if (tag.contains("guiButtonClick")) {
            int btn = tag.getInt("guiButtonClick");
            if (btn == deleteLeftoverDataBtnId) {
                deleteLeftoverData = !deleteLeftoverData;
                setChanged();
            }
        }
    }

    @Override
    public void readClient(CompoundTag tag) {
        guiHandler.readClient(tag);
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putBoolean("deleteLeftoverData", deleteLeftoverData);
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        deleteLeftoverData = tag.getBoolean("deleteLeftoverData");
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

    public static class WorkingData {
        public guiModuleVerticalProgressBar dataBar;
        public guiModuleVerticalProgressBar dataBarDatabase;
        String lastType = null;
        int keepTypeTimeout = 0;
        BlockPos dataHatchPos;

        public WorkingData(int id, GuiHandlerBlockEntity guiHandler, BlockPos hatchPos) {
            this.dataHatchPos = hatchPos;
            dataBar = new guiModuleVerticalProgressBar(id + 1001 + 100 * id, guiHandler, 10 + 50 * id, 20);
            dataBar.bar = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_vertical_progress_bar_green.png");
            dataBarDatabase = new guiModuleVerticalProgressBar(id + 1002 + 100 * id, guiHandler, 25 + 50 * id, 20);
            dataBarDatabase.bar = ResourceLocation.fromNamespaceAndPath(ARLib.ARLib.MODID, "textures/gui/gui_vertical_progress_bar_green.png");
            guiHandler.modules.add(dataBar);
            guiHandler.modules.add(dataBarDatabase);
        }
    }
}
