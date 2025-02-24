package WorkSites.Quarry;

import ARLib.gui.ModularScreen;
import ARLib.gui.modules.*;
import ARLib.utils.InventoryUtils;
import WorkSites.Config;
import WorkSites.EntityWorkSiteBase;
import WorkSites.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

import static WorkSites.Registry.ENTITY_QUARRY;


public class EntityQuarry extends EntityWorkSiteBase {

    public static Set<BlockPos> knownQuarries = new HashSet<>();

    public int energy_try_quarry = Config.INSTANCE.energy_try_quarry;
    int yTarget = 0;
    int structureW = 3;

    Set<BlockPos> structureBlocks = new HashSet<>();

    public TreeSet<BlockPos> blocksToMine = new TreeSet<>(new Comparator<BlockPos>() {
        @Override
        // this stupid thing needs always a +/- value or it will just skip adding the entry
        // so first compare y, and if equal compare the others to get some order
        public int compare(BlockPos o1, BlockPos o2) {
            int res = -(o1.getY() - o2.getY());
            if(res == 0)
                res = (o1.getX() - o2.getX());
            if(res == 0)
                res = (o1.getZ() - o2.getZ());
            return res;
        }
    });

    public ItemStackHandler mainInventory = new ItemStackHandler(18) {
        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    public ItemStackHandler inputsInventory = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof PickaxeItem;
        }

        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    public ItemStackHandler specialResourcesInventory = new ItemStackHandler(6) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (stack.getItem() instanceof PickaxeItem)
                return true;
            else return false;
        }

        @Override
        public void onContentsChanged(int i) {
            setChanged();
        }
    };

    int currentBlockToScanIndex = 0;

    public EntityQuarry(BlockPos pos, BlockState blockState) {
        super(ENTITY_QUARRY.get(), pos, blockState);

        maxSize = 32;
        minSize = 16;
        w = 16;
        h = 16;

        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 210, 500, 0, 1, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }
        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 150, 600, 0, 1, guiHandlerMain)) {
            guiHandlerMain.getModules().add(m);
        }


        guiModuleText t1 = new guiModuleText(11001, "Output", guiHandlerMain, 10, 30, 0xff000000, false);
        guiHandlerMain.getModules().add(t1);
        guiModuleText t2 = new guiModuleText(11002, "Resources", guiHandlerMain, 10, 78, 0xff000000, false);
        guiHandlerMain.getModules().add(t2);
        guiModuleText t3 = new guiModuleText(11003, "Special Resources", guiHandlerMain, 10, 110, 0xff000000, false);
        guiHandlerMain.getModules().add(t3);

        for (int i = 0; i < inputsInventory.getSlots(); i++) {
            int x = i * 18 + 10;
            int y = 89;
            guiModuleItemHandlerSlot s = new guiModuleItemHandlerSlot(900 + i, inputsInventory, i, 1, 0, guiHandlerMain, x, y);
            guiHandlerMain.getModules().add(s);
        }

        for (int i = 0; i < specialResourcesInventory.getSlots(); i++) {
            int x = i * 18 + 10;
            int y = 120;
            guiModuleItemHandlerSlot s = new guiModuleItemHandlerSlot(1000 + i, specialResourcesInventory, i, 1, 0, guiHandlerMain, x, y);
            guiHandlerMain.getModules().add(s);
        }

        int w = 9;
        for (int i = 0; i < mainInventory.getSlots(); i++) {
            int x = i % w * 18 + 10;
            int y = i / w * 18 + 40;
            guiModuleItemHandlerSlot s = new guiModuleItemHandlerSlot(800 + i, mainInventory, i, 1, 0, guiHandlerMain, x, y);
            guiHandlerMain.getModules().add(s);
        }
    }
    @Override
    public void onLoad(){
        super.onLoad();
        knownQuarries.add(this.getBlockPos());
        controllerOffsetH = 0; // this does not use controller offset h
    }
    @Override
    public void setRemoved(){
        knownQuarries.remove(this.getBlockPos());
        super.setRemoved();
    }

    @Override
    public void updateBoundsBp() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos p1 = getBlockPos().relative(facing, - 1);
        p1 = p1.relative(facing.getClockWise(), controllerOffsetW);
        p1 = new BlockPos(p1.getX(), yTarget, p1.getZ());
        BlockPos p2 = p1.relative(facing.getCounterClockWise(), w - 1).relative(facing.getOpposite(), h - 1).relative(Direction.UP, getBlockPos().getY()-yTarget);

        pmin = new BlockPos(Math.min(p1.getX(), p2.getX()), Math.min(p1.getY(), p2.getY()), Math.min(p1.getZ(), p2.getZ()));
        pmax = new BlockPos(Math.max(p1.getX(), p2.getX()), Math.max(p1.getY(), p2.getY()), Math.max(p1.getZ(), p2.getZ()));

        // this farm does not use blacklist
        updateAllowedBlocksList();
    }

    public boolean isInBounds(BlockPos p){
        return  p.getX()<=pmax.getX()&&
                p.getY()<=pmax.getY()&&
                p.getZ()<=pmax.getZ()&&
                p.getX()>=pmin.getX()&&
                p.getY()>=pmin.getY()&&
                p.getZ()>=pmin.getZ();
    }
    @Override
    public void updateAllowedBlocksList() {

        // this thing computes a stair pattern
        // dont ask me how it works, i did this months ago and already forgot about it
        
        blocksToMine.clear();
        structureBlocks.clear();
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean useCounterClockWise = controllerOffsetW < w / 2;
        Direction currentDirection;
        if (useCounterClockWise)
            currentDirection = facing.getCounterClockWise();
        else
            currentDirection = facing.getClockWise();
        BlockPos p1 = getBlockPos().relative(facing, -1).relative(Direction.DOWN);
        // fill all blocks up to the end with structure blocks
        BlockPos next = p1;
        BlockPos next2 = null;
        while (true) {
            if (isInBounds(next)) {
                for (int i = 0; i < structureW; i++) {
                    BlockPos p;
                    if (useCounterClockWise)
                        p = next.relative(currentDirection.getCounterClockWise(), i);
                    else
                        p = next.relative(currentDirection.getClockWise(), i);
                    for (int j = yTarget; j <= p.getY(); j++) {
                        structureBlocks.add(new BlockPos(p.getX(), j, p.getZ()));
                    }
                }
            } else {
                break;
            }
            next2 = next;
            next = next.relative(currentDirection.getOpposite());
        }
        boolean wentDown = false;
        while (true) {
            next2 = next2.relative(currentDirection);
            if (!isInBounds(next2.relative(currentDirection, structureW))) {
                for (int o = 0; o < structureW; o++) {
                    for (int i = 0; i < structureW; i++) {
                        BlockPos p;
                        if (useCounterClockWise)
                            p = next2.relative(currentDirection.getCounterClockWise(), i).relative(currentDirection, o);
                        else
                            p = next2.relative(currentDirection.getClockWise(), i).relative(currentDirection, o);
                        for (int j = yTarget; j <= p.getY(); j++) {
                            structureBlocks.add(new BlockPos(p.getX(), j, p.getZ()));
                        }
                    }
                }
                next2 = next2.relative(currentDirection, structureW - 1);
                if (useCounterClockWise)
                    currentDirection = currentDirection.getCounterClockWise();
                else
                    currentDirection = currentDirection.getClockWise();
                if (!wentDown)
                    break;
                wentDown = false;
            } else if (!structureBlocks.contains(next2)) {
                for (int i = 0; i < structureW; i++) {
                    BlockPos p;
                    if (useCounterClockWise)
                        p = next2.relative(currentDirection.getCounterClockWise(), i);
                    else
                        p = next2.relative(currentDirection.getClockWise(), i);
                    for (int j = yTarget; j <= next2.getY(); j++) {
                        structureBlocks.add(new BlockPos(p.getX(), j, p.getZ()));
                    }
                }
                next2 = next2.relative(Direction.DOWN);
                if (next2.getY() == yTarget)
                    break;
                wentDown = true;
            }
        }

        int ymin = next2.getY() + 1;

        // compute allowed blockpos
        allowedBlocks.clear();
        allowedBlocksList.clear();
        for (int y = pmax.getY(); y >= ymin; y--) {
            for (int z = pmin.getZ(); z <= pmax.getZ(); z++) {
                for (int x = pmin.getX(); x <= pmax.getX(); x++) {
                    BlockPos target = new BlockPos(x, y, z);
                    if (!structureBlocks.contains(target)) {
                        allowedBlocks.add(target);
                        allowedBlocksList.add(target);
                    }
                }
            }
        }
    }

    // modified to not show y+ button
    public void updateGuiModules() {
        guiHandlerBounds.getModules().clear();

        ResourceLocation red = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/gui/red.png");
        ResourceLocation blue = ResourceLocation.fromNamespaceAndPath(Main.MODID, "textures/gui/blue.png");

        int baseOffsetX = 30;
        int baseOffsetY = 30;

        int pxPerBlock = 140 / maxSize;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int _x = x;
                int _y = h - y - 1; // because highest y is directly next to the controller
                guiModuleButton b = new guiModuleButton(-1, "", guiHandlerBounds, x * pxPerBlock + baseOffsetX, y * pxPerBlock + baseOffsetY, pxPerBlock, pxPerBlock, red, 1, 1) {
                    @Override
                    public void onButtonClicked() {
                    }
                };
                guiHandlerBounds.getModules().add(b);
            }
        }

        int controllerX = baseOffsetX + controllerOffsetW * pxPerBlock;
        int controllerY = baseOffsetY + h * pxPerBlock-controllerOffsetH*pxPerBlock;
        guiModuleImage controllerPos = new guiModuleImage(guiHandlerBounds, controllerX, controllerY, pxPerBlock, pxPerBlock, blue, 1, 1);
        guiHandlerBounds.getModules().add(controllerPos);

        guiModuleDefaultButton hinc = new guiModuleDefaultButton(101, "h+", guiHandlerBounds, 5, 10, 15, 15);
        guiModuleDefaultButton hdec = new guiModuleDefaultButton(102, "h-", guiHandlerBounds, 30, 10, 15, 15);
        guiHandlerBounds.getModules().add(hinc);
        guiHandlerBounds.getModules().add(hdec);

        guiModuleDefaultButton winc = new guiModuleDefaultButton(103, "w+", guiHandlerBounds, 55, 10, 15, 15);
        guiModuleDefaultButton wdec = new guiModuleDefaultButton(104, "w-", guiHandlerBounds, 80, 10, 15, 15);
        guiHandlerBounds.getModules().add(winc);
        guiHandlerBounds.getModules().add(wdec);

        guiModuleDefaultButton xinc = new guiModuleDefaultButton(105, "x+", guiHandlerBounds, 105, 10, 15, 15);
        guiModuleDefaultButton xdec = new guiModuleDefaultButton(106, "x-", guiHandlerBounds, 130, 10, 15, 15);
        guiHandlerBounds.getModules().add(xinc);
        guiHandlerBounds.getModules().add(xdec);

        if (guiHandlerBounds.screen instanceof ModularScreen ms) {
            ms.calculateGuiOffsetAndNotifyModules();
        }
    }

    public boolean tryQuarry() {
        if (!blocksToMine.isEmpty()) {
            BlockPos target = blocksToMine.getFirst();
            BlockState s = level.getBlockState(target);
            LootParams.Builder b = new LootParams.Builder((ServerLevel) level)
                    .withParameter(LootContextParams.TOOL, new ItemStack(Items.DIAMOND_PICKAXE))
                    .withParameter(LootContextParams.ORIGIN, getBlockPos().getCenter());
            List<ItemStack> drops = s.getDrops(b);

            if (InventoryUtils.canInsertAllItems(List.of(mainInventory), drops)) {
                level.destroyBlock(target, false);
                for (ItemStack i : drops) {
                    for (int j = 0; j < mainInventory.getSlots(); j++) {
                        i = mainInventory.insertItem(j, i, false);
                    }
                }
                blocksToMine.remove(target);
                return true;
            }
        }
        return false;
    }

    public void scanStep() {
        if (!allowedBlocksList.isEmpty()) {
            if (currentBlockToScanIndex >= allowedBlocksList.size()) {
                currentBlockToScanIndex = 0;
            }
            BlockPos nextPosToScan = allowedBlocksList.get(currentBlockToScanIndex);
            currentBlockToScanIndex += 1;

            if (blackListAsBlockPos.contains(nextPosToScan))
                return;

            BlockState s = level.getBlockState(nextPosToScan);
            if (!s.isAir() && s.getFluidState().isEmpty()) {
                blocksToMine.add(nextPosToScan);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            /*
            for (int i = 0; i < 100; i++) {
                scanStep();
            }

            if (battery.getEnergyStored() > 1) {
                for (int i = 0; i < 1; i++) {
                    if (tryQuarry()) {
                        battery.extractEnergy(1, false);
                    }
                }
                mainInventory.setStackInSlot(0,ItemStack.EMPTY);
            }
             */

            scanStep();
            if (battery.getEnergyStored() > energy_try_quarry) {
                if (tryQuarry()) {
                    battery.extractEnergy(energy_try_quarry, false);
                }
            }
        }
    }

    @Override
    public void openMainGui() {
        if (level.isClientSide) {
            guiHandlerMain.openGui(180, 240, true);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv1", mainInventory.serializeNBT(registries));
        tag.put("inv2", inputsInventory.serializeNBT(registries));
        tag.put("inv3", specialResourcesInventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mainInventory.deserializeNBT(registries, tag.getCompound("inv1"));
        inputsInventory.deserializeNBT(registries, tag.getCompound("inv2"));
        specialResourcesInventory.deserializeNBT(registries, tag.getCompound("inv3"));
    }
}
