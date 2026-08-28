package ARLib.holoProjector;

import ARLib.gui.GuiHandlerMainHandItem;
import ARLib.gui.modules.GuiModuleBase;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleScrollContainer;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketPlayerMainHand;
import ARLib.utils.ClientUtils;
import ARLib.utils.DimensionUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static ARLib.ARLibRegistry.BLOCK_STRUCTURE_PREVIEW;


public class itemHoloProjector extends Item implements INetworkTagReceiver {
    public static Map<String, List<BlockInfo>> structureBlocks = new HashMap<>();
    public static Map<Integer, String> buttonIdToMachineName = new HashMap<>();
    static GuiHandlerMainHandItem guiHandler;
    static int id = 0;

    public itemHoloProjector(Properties properties) {
        super(properties);

        // Ensure the event listener is registered only on the client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(this::handleScroll);
        }
    }

    public static synchronized void registerMultiblock(String name, Object[][][] structure, HashMap<Character, List<Block>> charMapping) {
        List<BlockInfo> blockInfoList = new ArrayList<>();
        for (int y = 0; y < structure.length; y++) {
            for (int z = 0; z < structure[y].length; z++) {
                for (int x = 0; x < structure[y][z].length; x++) {
                    if (structure[y][z][x] != null) {
                        List<Block> allowed_blocks = itemHoloProjector.getAllowableBlocks(structure[y][z][x], charMapping);
                        BlockInfo info = new BlockInfo();
                        info.pos = new BlockPos(x, y, z);
                        info.allowedBlocks = allowed_blocks;
                        blockInfoList.add(info);
                    }
                }
            }
        }
        System.out.println("register multiblock in holo projector:" + id + ":" + name);
        structureBlocks.put(name, blockInfoList);
        buttonIdToMachineName.put(id, name);
        id += 1;
    }

    public static List<Block> getAllowableBlocks(Object input, HashMap<Character, List<Block>> charMapping) {
        if (input instanceof Character && charMapping.containsKey(input)) {
            return charMapping.get(input);
        } else if (input instanceof String) { //OreDict entry

        } else if (input instanceof Block) {
            List<Block> list = new ArrayList<>();
            list.add((Block) input);
            return list;
        } else if (input instanceof List) {
            return (List<Block>) input;
        }
        return new ArrayList<>();
    }

    private void handleScroll(InputEvent.MouseScrollingEvent event) {
        Player player = ClientUtils.getSinglePlayer();

        if (player != null &&
                player.getMainHandItem().is(this) &&
                player.isShiftKeyDown()) {

            double scrollDelta = event.getScrollDeltaY();

            CompoundTag tag = new CompoundTag();
            if (scrollDelta > 0) {
                // Scrolled up
                tag.putBoolean("scroll", true);
            } else if (scrollDelta < 0) {
                // Scrolled down
                tag.putBoolean("scroll", false);
            }

            PacketDistributor.sendToServer(new PacketPlayerMainHand(tag));
            event.setCanceled(true);
        }
    }

    public void initGui() {
        guiHandler = new GuiHandlerMainHandItem();
        List<GuiModuleBase> containerModules = new ArrayList<>();

        for (int id : buttonIdToMachineName.keySet()) {
            String name = buttonIdToMachineName.get(id);
            guiModuleButton button = new guiModuleButton(id, name, guiHandler, 5, 25 * id + 15, 230, 20, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_red.png"), 64, 20) {
                public void onButtonClicked() {
                    super.onButtonClicked();
                    Minecraft.getInstance().popGuiLayer();
                }
            };
            button.color = 0xffffffff;
            containerModules.add(button);
        }
        guiModuleScrollContainer container = new guiModuleScrollContainer(containerModules, 0xFFA0A0A0, guiHandler, 5, 5, 240, 190);
        guiHandler.getModules().add(container);
    }

    public void placeLayer(CompoundTag itemTag) {
        removeLastPreview(itemTag);
        if (itemTag.contains("dimension") &&
                itemTag.contains("y") &&
                itemTag.contains("posX") &&
                itemTag.contains("posZ") &&
                itemTag.contains("posY") &&
                itemTag.contains("stepX") &&
                itemTag.contains("stepZ") &&
                itemTag.contains("selectedMachine")) {
            Level world = DimensionUtils.getDimensionLevelServer(itemTag.getString("dimension"));
            List<BlockInfo> machineBlocks = structureBlocks.get(itemTag.getString("selectedMachine"));
            BlockPos clickedPos = new BlockPos(itemTag.getInt("posX"), itemTag.getInt("posY"), itemTag.getInt("posZ"));
            int stepX = itemTag.getInt("stepX");
            int stepZ = itemTag.getInt("stepZ");
            for (BlockInfo i : machineBlocks) {
                int x = i.pos.getX();
                int y = i.pos.getY();
                int z = i.pos.getZ();
                if (y == itemTag.getInt("y")) {
                    int globalX = clickedPos.getX() + (x) * stepZ - (z) * stepX;
                    int globalY = clickedPos.getY() + machineHeight(itemTag.getString("selectedMachine")) - y;
                    int globalZ = clickedPos.getZ() - (x) * stepX - (z) * stepZ;
                    BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);
                    if (world.getBlockState(globalPos).canBeReplaced()) {
                        if (!i.allowedBlocks.contains(Blocks.AIR)) {
                            world.setBlock(globalPos, BLOCK_STRUCTURE_PREVIEW.get().defaultBlockState(), 3);
                            EntityStructurePreviewBlock structureEntity = (EntityStructurePreviewBlock) (world.getBlockEntity(globalPos));
                            structureEntity.setValidBlocks(i.allowedBlocks);
                            // when we delete a preview block and place a new preview block with a different state,
                            // it will not send the change to client because the block effectively did not change from the perspective of minecraft
                            // when the valid blocks are changed, it has to sync to client because client will not call onload since the block was never removed on client
                        }
                    }
                }
            }
        }
    }

    public void removeLastPreview(CompoundTag itemTag) {
        if (itemTag.contains("dimension") &&
                itemTag.contains("posX") &&
                itemTag.contains("posZ") &&
                itemTag.contains("posY") &&
                itemTag.contains("stepX") &&
                itemTag.contains("stepZ") &&
                itemTag.contains("selectedMachine")) {
            Level world = DimensionUtils.getDimensionLevelServer(itemTag.getString("dimension"));
            List<BlockInfo> machineBlocks = structureBlocks.get(itemTag.getString("selectedMachine"));
            BlockPos clickedPos = new BlockPos(itemTag.getInt("posX"), itemTag.getInt("posY"), itemTag.getInt("posZ"));
            int stepX = itemTag.getInt("stepX");
            int stepZ = itemTag.getInt("stepZ");
            for (BlockInfo i : machineBlocks) {
                int x = i.pos.getX();
                int y = i.pos.getY();
                int z = i.pos.getZ();
                int globalX = clickedPos.getX() + (x) * stepZ - (z) * stepX;
                int globalY = clickedPos.getY() + machineHeight(itemTag.getString("selectedMachine")) - y;
                int globalZ = clickedPos.getZ() - (x) * stepX - (z) * stepZ;
                BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);
                if (world.getBlockState(globalPos).getBlock().equals(BLOCK_STRUCTURE_PREVIEW.get())) {
                    world.setBlock(globalPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && !context.getPlayer().isShiftKeyDown()) {
            ItemStack stack = context.getItemInHand();
            Direction facing = context.getHorizontalDirection().getOpposite();
            BlockPos clickedPos = context.getClickedPos().above();
            CompoundTag tag = getStacktagOrEmpty(stack);
            if (tag.contains("selectedMachine")) {
                removeLastPreview(tag);
                tag.putInt("y", machineHeight(tag.getString("selectedMachine")));
                tag.putInt("posX", clickedPos.getX());
                tag.putInt("posY", clickedPos.getY());
                tag.putInt("posZ", clickedPos.getZ());
                tag.putInt("stepX", facing.getStepX());
                tag.putInt("stepZ", facing.getStepZ());
                tag.putString("dimension", DimensionUtils.getLevelId(context.getLevel()));
                setTag(stack, tag);
                placeLayer(tag);
            }
        }
        if (!context.getLevel().isClientSide && context.getPlayer().isShiftKeyDown()) {
            use(context.getLevel(), context.getPlayer(), context.getHand());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                initGui();
                guiHandler.openGui(250, 200, true);
            }
            return InteractionResultHolder.success(itemstack);
        }
        return InteractionResultHolder.pass(itemstack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = getStacktagOrEmpty(stack);
        if (tag.contains("selectedMachine")) {
            tooltipComponents.add(Component.literal(tag.getString("selectedMachine")));
        }
    }

    CompoundTag getStacktagOrEmpty(ItemStack stack) {
        try {
            return stack.get(DataComponents.CUSTOM_DATA).copyTag();
        } catch (Exception e) {
            return new CompoundTag();
        }
    }

    void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    int machineHeight(String name) {
        List<BlockInfo> blocks = structureBlocks.get(name);
        int maxY = 0;
        for (BlockInfo i : blocks) {
            maxY = Math.max(maxY, i.pos.getY());
        }
        return maxY;
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {

        ItemStack stack = p.getInventory().getSelected();
        CompoundTag itemTag = getStacktagOrEmpty(stack);

        if (compoundTag.contains("guiButtonClick")) {
            int buttonId = compoundTag.getInt("guiButtonClick");
            String name = buttonIdToMachineName.get(buttonId);
            itemTag.putString("selectedMachine", name);
            setTag(stack, itemTag);
        }

        if (compoundTag.contains("scroll") && itemTag.contains("selectedMachine")) {
            boolean was_upScroll = compoundTag.getBoolean("scroll");
            int y = 0;
            if (itemTag.contains("y")) {
                y = itemTag.getInt("y");
            }
            if (was_upScroll && y > 0) {
                y -= 1;
            }
            if (!was_upScroll && y < machineHeight(itemTag.getString("selectedMachine"))) {
                y += 1;
            }
            itemTag.putInt("y", y);
            setTag(stack, itemTag);
            placeLayer(itemTag);
            //System.out.println(getStacktagOrEmpty(stack));
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {

    }

    static class BlockInfo {
        List<Block> allowedBlocks;
        BlockPos pos;
        BlockInfo() {
        }
    }
}
