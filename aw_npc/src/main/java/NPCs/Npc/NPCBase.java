package NPCs.Npc;

import ARLib.gui.GuiHandlerEntity;
import ARLib.gui.ModularScreen;
import ARLib.gui.modules.*;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketEntity;
import NPCs.Items.ItemFoodOrder;
import NPCs.Blocks.TownHall.TownHallNames;
import NPCs.Blocks.TownHall.TownHallOwners;
import NPCs.Utils;
import NPCs.Npc.programs.SlowMobNavigation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static NPCs.Registry.ITEM_SET_HOME_TOOL;

public abstract class NPCBase extends PathfinderMob implements INetworkTagReceiver {

    public int slowNavigationMaxDistance = 12 * 16;
    public int slowNavigationMaxNodes = 4096 * 16;
    public int slowNavigationStepPerTick = 512;
    public SlowMobNavigation slowMobNavigation;

    public int regenerateOneAfterTicks = 20 * 10;
    public double hunger = 5;
    public double maxHunger = 20;
    public BlockPos homePosition;
    public BlockPos townHall;
    public String owner;

    public ItemStackHandler inventory = new ItemStackHandler(8){
        @Override
        public void onContentsChanged(int slot){
            onInventoryChange();
        }
    };

    public static EntityDataAccessor<String> DATA_TEXTURE = SynchedEntityData.defineId(NPCBase.class, EntityDataSerializers.STRING);

    public ItemStackHandler foodOrderStackHandler = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                if (stack.getItem() instanceof ItemFoodOrder) {
                    return true;
                }
            }
            return false;
        }
    };

    public ItemStackHandler combinedInventory = new ItemStackHandler(0) {
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot == 0)
                setItemInHand(InteractionHand.MAIN_HAND, stack);
            else if (slot == 1)
                setItemInHand(InteractionHand.OFF_HAND, stack);
            else {
                inventory.setStackInSlot(slot - 2, stack);
            }
        }

        public int getSlots() {
            return 2 + inventory.getSlots();
        }

        public ItemStack getStackInSlot(int slot) {
            if (slot == 0)
                return getMainHandItem();
            else if (slot == 1)
                return getOffhandItem();
            else {
                return inventory.getStackInSlot(slot - 2);
            }
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot >= 2) return inventory.insertItem(slot - 2, stack, simulate);

            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                ItemStack existing = getStackInSlot(slot);
                int limit = this.getStackLimit(slot, stack);
                if (!existing.isEmpty()) {
                    if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                        return stack;
                    }
                    limit -= existing.getCount();
                }

                if (limit <= 0) {
                    return stack;
                } else {
                    boolean reachedLimit = stack.getCount() > limit;
                    if (!simulate) {
                        if (existing.isEmpty()) {
                            setStackInSlot(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                        } else {
                            existing.grow(reachedLimit ? limit : stack.getCount());
                        }

                        this.onContentsChanged(slot);
                    }

                    return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
                }
            }
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot >= 2) return inventory.extractItem(slot - 2, amount, simulate);

            if (amount == 0) {
                return ItemStack.EMPTY;
            } else {
                this.validateSlotIndex(slot);
                ItemStack existing = getStackInSlot(slot);
                if (existing.isEmpty()) {
                    return ItemStack.EMPTY;
                } else {
                    int toExtract = Math.min(amount, existing.getMaxStackSize());
                    if (existing.getCount() <= toExtract) {
                        if (!simulate) {
                            setStackInSlot(slot, ItemStack.EMPTY);
                            this.onContentsChanged(slot);
                            return existing;
                        } else {
                            return existing.copy();
                        }
                    } else {
                        if (!simulate) {
                            setStackInSlot(slot, existing.copyWithCount(existing.getCount() - toExtract));
                            this.onContentsChanged(slot);
                        }

                        return existing.copyWithCount(toExtract);
                    }
                }
            }
        }

        protected void validateSlotIndex(int slot) {
        }

        public void onContentsChanged(int slot) {
            onInventoryChange();
        }
    };
    public void onInventoryChange(){

    }

    public ItemStackHandler armorInventory;

    public GuiHandlerEntity guiHandler;
    public guiModuleProgressBarHorizontal6px lifeBar;
    public guiModuleProgressBarHorizontal6px hungerBar;
    public guiModuleText ownerText;
    public guiModuleText townHallText;
    public guiModuleTextInput nameTextInput;

    int ticksSinceLastRegen = 0;
    public UUID followOwner = null;


    protected NPCBase(EntityType<?> entityType, Level level) {
        super((EntityType<? extends PathfinderMob>) entityType, level);
        this.setPersistenceRequired();
        this.noCulling = true;
        setGuaranteedDrop(EquipmentSlot.MAINHAND);
        setGuaranteedDrop(EquipmentSlot.OFFHAND);
        setGuaranteedDrop(EquipmentSlot.CHEST);
        setGuaranteedDrop(EquipmentSlot.HEAD);
        setGuaranteedDrop(EquipmentSlot.LEGS);
        setGuaranteedDrop(EquipmentSlot.FEET);

        setCanPickUpLoot(true);

        super.getNavigation().getNodeEvaluator().setCanOpenDoors(true);
        super.getNavigation().getNodeEvaluator().setCanPassDoors(true);
        super.getNavigation().getNodeEvaluator().setCanFloat(true);

        slowMobNavigation = new SlowMobNavigation(this);

        armorInventory = new ItemStackHandler((NonNullList<ItemStack>) super.getArmorSlots()) {
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot == EquipmentSlot.HEAD.getIndex()) {
                    return getEquipmentSlotForItem(stack).equals(EquipmentSlot.HEAD);
                }
                if (slot == EquipmentSlot.CHEST.getIndex()) {
                    return getEquipmentSlotForItem(stack).equals(EquipmentSlot.CHEST);
                }
                if (slot == EquipmentSlot.LEGS.getIndex()) {
                    return getEquipmentSlotForItem(stack).equals(EquipmentSlot.LEGS);
                }
                if (slot == EquipmentSlot.FEET.getIndex()) {
                    return getEquipmentSlotForItem(stack).equals(EquipmentSlot.FEET);
                }
                return false;
            }
        };


        guiHandler = new GuiHandlerEntity(this);
        guiModuleItemHandlerSlot head = new guiModuleItemHandlerSlot(0, armorInventory, EquipmentSlot.HEAD.getIndex(), 1, 0, guiHandler, 10, 50) {
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.slot_background, this.onGuiX, this.onGuiY, 0.0F, 0.0F, this.w, this.h, this.slot_bg_w, this.slot_bg_h);
                if (this.client_getItemStackToRender().isEmpty())
                    guiGraphics.blit(ResourceLocation.withDefaultNamespace("textures/item/empty_armor_slot_helmet.png"), this.onGuiX + 1, this.onGuiY + 1, 0.0F, 0.0F, 16, 16, 16, 16);
                ModularScreen.renderItemStack(guiGraphics, this.onGuiX, this.onGuiY, this.client_getItemStackToRender());
                if (!this.client_getItemStackToRender().isEmpty() && this.client_isMouseOver((double) mouseX, (double) mouseY, this.onGuiX, this.onGuiY, this.w, this.h)) {
                    guiGraphics.fill(this.onGuiX, this.onGuiY, this.w + this.onGuiX, this.h + this.onGuiY, 822083583);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, this.client_getItemStackToRender(), mouseX, mouseY);
                }
            }
        };
        guiModuleItemHandlerSlot chest = new guiModuleItemHandlerSlot(1, armorInventory, EquipmentSlot.CHEST.getIndex(), 1, 0, guiHandler, 10, 70) {
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.slot_background, this.onGuiX, this.onGuiY, 0.0F, 0.0F, this.w, this.h, this.slot_bg_w, this.slot_bg_h);
                if (this.client_getItemStackToRender().isEmpty())
                    guiGraphics.blit(ResourceLocation.withDefaultNamespace("textures/item/empty_armor_slot_chestplate.png"), this.onGuiX + 1, this.onGuiY + 1, 0.0F, 0.0F, 16, 16, 16, 16);
                ModularScreen.renderItemStack(guiGraphics, this.onGuiX, this.onGuiY, this.client_getItemStackToRender());
                if (!this.client_getItemStackToRender().isEmpty() && this.client_isMouseOver((double) mouseX, (double) mouseY, this.onGuiX, this.onGuiY, this.w, this.h)) {
                    guiGraphics.fill(this.onGuiX, this.onGuiY, this.w + this.onGuiX, this.h + this.onGuiY, 822083583);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, this.client_getItemStackToRender(), mouseX, mouseY);
                }
            }
        };
        guiModuleItemHandlerSlot leg = new guiModuleItemHandlerSlot(2, armorInventory, EquipmentSlot.LEGS.getIndex(), 1, 0, guiHandler, 10, 90) {
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.slot_background, this.onGuiX, this.onGuiY, 0.0F, 0.0F, this.w, this.h, this.slot_bg_w, this.slot_bg_h);
                if (this.client_getItemStackToRender().isEmpty())
                    guiGraphics.blit(ResourceLocation.withDefaultNamespace("textures/item/empty_armor_slot_leggings.png"), this.onGuiX + 1, this.onGuiY + 1, 0.0F, 0.0F, 16, 16, 16, 16);
                ModularScreen.renderItemStack(guiGraphics, this.onGuiX, this.onGuiY, this.client_getItemStackToRender());
                if (!this.client_getItemStackToRender().isEmpty() && this.client_isMouseOver((double) mouseX, (double) mouseY, this.onGuiX, this.onGuiY, this.w, this.h)) {
                    guiGraphics.fill(this.onGuiX, this.onGuiY, this.w + this.onGuiX, this.h + this.onGuiY, 822083583);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, this.client_getItemStackToRender(), mouseX, mouseY);
                }
            }
        };
        guiModuleItemHandlerSlot feet = new guiModuleItemHandlerSlot(3, armorInventory, EquipmentSlot.FEET.getIndex(), 1, 0, guiHandler, 10, 110) {
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                guiGraphics.blit(this.slot_background, this.onGuiX, this.onGuiY, 0.0F, 0.0F, this.w, this.h, this.slot_bg_w, this.slot_bg_h);
                if (this.client_getItemStackToRender().isEmpty())
                    guiGraphics.blit(ResourceLocation.withDefaultNamespace("textures/item/empty_armor_slot_boots.png"), this.onGuiX + 1, this.onGuiY + 1, 0.0F, 0.0F, 16, 16, 16, 16);
                ModularScreen.renderItemStack(guiGraphics, this.onGuiX, this.onGuiY, this.client_getItemStackToRender());
                if (!this.client_getItemStackToRender().isEmpty() && this.client_isMouseOver((double) mouseX, (double) mouseY, this.onGuiX, this.onGuiY, this.w, this.h)) {
                    guiGraphics.fill(this.onGuiX, this.onGuiY, this.w + this.onGuiX, this.h + this.onGuiY, 822083583);
                    guiGraphics.renderTooltip(Minecraft.getInstance().font, this.client_getItemStackToRender(), mouseX, mouseY);
                }
            }
        };
        guiHandler.getModules().add(head);
        guiHandler.getModules().add(chest);
        guiHandler.getModules().add(leg);
        guiHandler.getModules().add(feet);

        guiModuleItemHandlerSlot foodOrderSlot = new guiModuleItemHandlerSlot(13001, foodOrderStackHandler, 0, 1, 0, guiHandler, 140, 50);
        guiHandler.getModules().addFirst(foodOrderSlot);

        int w = 5;
        for (int i = 0; i < combinedInventory.getSlots(); i++) {
            int x = i % w * 18 + 40;
            int y = i / w * 18 + 50;
            guiModuleItemHandlerSlot m = new guiModuleItemHandlerSlot(i + 100, combinedInventory, i, 1, 0, guiHandler, x, y);
            guiHandler.getModules().add(m);
        }

        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerHotbarModules(10, 190, 200, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }
        for (GuiModuleBase m : guiModulePlayerInventorySlot.makePlayerInventoryModules(10, 130, 300, 0, 1, guiHandler)) {
            guiHandler.getModules().add(m);
        }

        lifeBar = new guiModuleProgressBarHorizontal6px(1000, 0xffBE0204, guiHandler, 40, 90);
        hungerBar = new guiModuleProgressBarHorizontal6px(1001, 0xff563225, guiHandler, 40, 100);

        guiHandler.getModules().add(lifeBar);
        guiHandler.getModules().add(hungerBar);

        ownerText = new guiModuleText(2001, "owner", guiHandler, 10, 9, 0xff000000, false);
        townHallText = new guiModuleText(2002, "townhallpos", guiHandler, 10, 21, 0xff000000, false);
        guiModuleText nameText = new guiModuleText(2003, "Name: ", guiHandler, 10, 33, 0xff000000, false);
        nameTextInput = new guiModuleTextInput(2004, guiHandler, 40, 33, 100, 10) {
            @Override
            public void server_readNetworkData(CompoundTag tag) {
                super.server_readNetworkData(tag);
                setCustomName(Component.literal(text));
            }
        };
        guiHandler.getModules().add(ownerText);
        guiHandler.getModules().add(townHallText);
        guiHandler.getModules().add(nameText);
        guiHandler.getModules().add(nameTextInput);

        guiModuleButton resetTownHallButton = new guiModuleButton(2999, "reset Townhall", guiHandler, 90, 110, 80, 15, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20);
        resetTownHallButton.color = 0xffffffff;
        resetTownHallButton.makeShadow = true;
        guiHandler.getModules().add(resetTownHallButton);

        guiModuleButton setHomeButton = new guiModuleButton(3000, "set home", guiHandler, 110, 90, 60, 15, ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/gui_button_black.png"), 64, 20);
        setHomeButton.color = 0xffffffff;
        setHomeButton.makeShadow = true;
        guiHandler.getModules().add(setHomeButton);

        setCustomName(Component.literal("NPC"));
    }


    public static void updateAllTownHalls() {
        for (ServerLevel l : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            for (Entity e : l.getEntities().getAll()) {
                if (e instanceof NPCBase npc) {
                    npc.updateTownHall();
                }
            }
        }
    }

    public void updateTownHall() {
        // assign to townhall
        if (townHall == null) {
            // scan for townhall, use anyone where owner is registered as an owner of the townhall
            for (BlockPos p : Utils.sortBlockPosByDistanceToNPC(TownHallOwners.getEntries(level()).keySet(), this)) {
                if (Utils.distanceManhattan(this, p.getCenter()) > 256)
                    break;

                if (TownHallOwners.getOwners(level(), p).contains(owner)) {
                    townHall = p;
                    //System.out.println("npc " + getUUID() + " now belongs to townhall" + p);
                    break;
                }
            }
        } else {
            if (!TownHallOwners.getOwners(level(), townHall).contains(owner)) {
                //System.out.println("townhall " + townHall + "is no longer valid");
                townHall = null;
                updateTownHall();
            }
        }
        if (townHall != null) {
            townHallText.setTextAndSync("Town: " + TownHallNames.getName(level(), townHall));
        } else {
            townHallText.setTextAndSync("Town: none");
        }
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TEXTURE, "");
    }


    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        setCustomNameVisible(true);
        if (!level().isClientSide) {

            if (owner == null) {
                Player closestPlayer = null;
                double closestDistance = 999;
                for (Player p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                    if (getPosition(0).distanceTo(p.getPosition(0)) < closestDistance) {
                        closestDistance = getPosition(0).distanceTo(p.getPosition(0));
                        closestPlayer = p;
                    }
                }
                if (closestPlayer != null) {
                    owner = closestPlayer.getName().getString();
                }
            }
            if (owner != null) {
                ownerText.setTextAndSync("Owner: " + owner);
            }
            updateTownHall();
            nameTextInput.text = getCustomName().getString();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            Set<String> owners = TownHallOwners.getOwners(level(), townHall);
            if ((owners != null && owners.contains(player.getName().getString())) || player.getName().getString().equals(owner)) {
                if (!player.isShiftKeyDown()) {
                    if (!guiHandler.playersTrackingGui.containsKey(player.getUUID())) {
                        CompoundTag tag = new CompoundTag();
                        tag.put("openGui", new CompoundTag());
                        PacketDistributor.sendToPlayer((ServerPlayer) player, PacketEntity.getEntityPacket(this, tag));
                    }
                } else {
                    if (followOwner == null) {
                        followOwner = player.getUUID();
                    } else {
                        followOwner = null;
                    }
                }
            }
        }
        return InteractionResult.SUCCESS_NO_ITEM_USED;
    }


    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {

            if (super.isDeadOrDying()) return;


            guiHandler.serverTick();

            if (ticksSinceLastRegen < regenerateOneAfterTicks) {
                ticksSinceLastRegen++;
            } else {
                if (getHealth() + 0.1 < getAttributeValue(Attributes.MAX_HEALTH)) {
                    if (hunger > 5) {
                        setHealth((float) Math.min(getHealth() + 0.5f, getAttributeValue(Attributes.MAX_HEALTH)));
                        ticksSinceLastRegen = 0;
                        hunger -= 0.5;
                    }
                }
            }
            lifeBar.setProgressAndSync(super.getHealth() / super.getAttributeValue(Attributes.MAX_HEALTH));
            hunger -= 0.0005;
            hunger = Math.max(0, hunger);
            if (hunger < 0.1) {
                setHealth(Math.max(5, getHealth() - 0.01f));
            }
            hungerBar.setProgressAndSync(hunger / maxHunger);
        }
        this.updateSwingTime(); //wtf do i need to call this myself??
    }

    @Override
    public boolean canTakeItem(ItemStack itemstack) {
        return Utils.countEmptySlots(this) > 0;
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack itemstack = itemEntity.getItem();
        if (Utils.countEmptySlots(this) > 0) {
            ItemStack stackCopy = itemstack.copy();
            for (int i = 0; i < combinedInventory.getSlots(); i++) {
                stackCopy = combinedInventory.insertItem(i, stackCopy, false);
            }
            this.onItemPickup(itemEntity);
            this.take(itemEntity, itemstack.getCount());
            itemEntity.discard();
        }
    }

    @Override
    protected void hurtArmor(DamageSource damageSource, float damage) {
        this.doHurtEquipment(damageSource, damage, new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD});
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            if (source.getEntity() instanceof LivingEntity) {
                List<CombatNPC> fighters = level().getEntitiesOfClass(CombatNPC.class, new AABB(blockPosition()).inflate(64), (e) -> true);
                for (CombatNPC fighter : fighters) {
                    if (fighter.runForHelpProgram != null) {
                        fighter.runForHelpProgram.requestHelp(this, source.getEntity());
                    }
                }
                HostileEntities.addTemporaryHostile(source.getEntity(), this, 20 * 15);
            }
        }
        return super.hurt(source, amount);
    }


    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            level.addFreshEntity(new ItemEntity(level, getPosition(0).x, getPosition(0).y, getPosition(0).z, inventory.getStackInSlot(i)));
        }
        for (int i = 0; i < foodOrderStackHandler.getSlots(); i++) {
            level.addFreshEntity(new ItemEntity(level, getPosition(0).x, getPosition(0).y, getPosition(0).z, foodOrderStackHandler.getStackInSlot(i)));
        }
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
    }

    @Override
    public ItemStack eat(Level level, ItemStack food, FoodProperties foodProperties) {
        hunger += foodProperties.nutrition();
        return super.eat(level, food, foodProperties);
    }
    @Override
    public int getMaxFallDistance() { // makes fighters not jump from >3 block tall walls down to attack a target
        return 3;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("inventory1", inventory.serializeNBT(this.registryAccess()));

        if (homePosition != null) {
            compound.putInt("homePositionX", homePosition.getX());
            compound.putInt("homePositionY", homePosition.getY());
            compound.putInt("homePositionZ", homePosition.getZ());
        }

        if (townHall != null) {
            compound.putInt("townHallX", townHall.getX());
            compound.putInt("townHallY", townHall.getY());
            compound.putInt("townHallZ", townHall.getZ());
        }

        if (owner != null) {
            compound.putString("owner", owner);
        }

        compound.putDouble("hunger", hunger);

        compound.putString("texture", getEntityData().get(DATA_TEXTURE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        inventory.deserializeNBT(this.registryAccess(), compound.getCompound("inventory1"));

        if (compound.contains("homePositionX") && compound.contains("homePositionY") && compound.contains("homePositionZ")) {
            homePosition = new BlockPos(compound.getInt("homePositionX"), compound.getInt("homePositionY"), compound.getInt("homePositionZ"));
        }

        if (compound.contains("townHallX") && compound.contains("townHallY") && compound.contains("townHallZ")) {
            townHall = new BlockPos(compound.getInt("townHallX"), compound.getInt("townHallY"), compound.getInt("townHallZ"));
        }

        if (compound.contains("owner")) {
            owner = compound.getString("owner");
        }

        hunger = compound.getDouble("hunger");

        if(compound.contains("texture")) {
            getEntityData().set(DATA_TEXTURE, compound.getString("texture"));
        }
    }


    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer p) {
        // verify server side that the player is friend or owner before allow anything to go to the gui
        Set<String> owners = TownHallOwners.getOwners(level(), townHall);
        if ((owners != null && owners.contains(p.getName().getString())) || p.getName().getString().equals(owner)) {
            guiHandler.readServer(compoundTag);

            if (compoundTag.contains("guiButtonClick")) {
                if (compoundTag.getInt("guiButtonClick") == 2999) {
                    townHall = null;
                    updateTownHall();
                }
                if (compoundTag.getInt("guiButtonClick") == 3000) {
                    ItemStack setHomeTool = new ItemStack(ITEM_SET_HOME_TOOL.get());
                    CompoundTag info = new CompoundTag();
                    info.putUUID("uuid", getUUID());
                    Utils.setStackTag(setHomeTool, info);
                    level().addFreshEntity(new ItemEntity(level(), p.position().x, p.position().y, p.position().z, setHomeTool));

                    CompoundTag response = new CompoundTag();
                    response.put("closeGui", new CompoundTag());
                    PacketDistributor.sendToPlayer(p, PacketEntity.getEntityPacket(this, response));
                    p.sendSystemMessage(Component.literal("Shift click the Set-Home-Tool on a bed!"));
                }
            }
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);

        if (compoundTag.contains("openGui")) {
            guiHandler.openGui(180, 220, true);
        }
        if (compoundTag.contains("closeGui")) {
            if (NPCBase.this.guiHandler.screen instanceof ModularScreen m) {
                m.onClose();
            }
        }
    }
}
