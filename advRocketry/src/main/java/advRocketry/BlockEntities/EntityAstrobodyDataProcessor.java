package advRocketry.BlockEntities;

import ARLib.ARLibRegistry;
import ARLib.blockentities.EntityEnergyInputBlock;
import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.network.PacketBlockEntity;
import advRocketry.Config;
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
    guiModuleItemHandlerSlot storageDiskSlot1;

    public EntityAstrobodyDataProcessor(BlockPos pos, BlockState state) {
        super(BlockEntities.ENTITY_ASTROBODY_DATA_PROCESSOR.get(), pos, state);
        //super.forwardInteractionToMaster = true;

        guiHandler = new GuiHandlerBlockEntity(this);
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
    }

    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    public void popInventory() {

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
        guiHandler.signalOpenGui(player, 176, 200, true);
    }
}
