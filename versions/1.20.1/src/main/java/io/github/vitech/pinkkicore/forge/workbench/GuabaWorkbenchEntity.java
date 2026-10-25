package io.github.vitech.pinkkicore.forge.workbench;

import io.github.vitech.pinkkicore.forge.item.ModItems;
import java.util.EnumMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class GuabaWorkbenchEntity extends BlockEntity implements MenuProvider {
    public static final int CATALYST = 0, INPUT = 1, OUTPUT = 2;
    private int progress, status;
    private ResourceLocation activeRecipe;
    private RedstoneMode redstone = RedstoneMode.IGNORE;
    private final EnumMap<Direction, SideMode> sides = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IItemHandler>> handlers = new EnumMap<>(Direction.class);
    public final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT || slot == CATALYST && stack.is(ModItems.GUABA.get());
        }
        @Override protected void onContentsChanged(int slot) { inventoryChanged(slot); }
    };
    public final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 10; }
        @Override public int get(int index) {
            if (index == 0) return progress;
            if (index == 1) return redstone.ordinal();
            if (index >= 2 && index < 8) return sideMode(Direction.values()[index-2]).ordinal();
            if (index == 8) return powered() ? 1 : 0;
            if (index == 9) return status;
            return 0;
        }
        // Server-owned data; the client uses a separate SimpleContainerData.
        @Override public void set(int index, int value) {}
    };
    public GuabaWorkbenchEntity(BlockPos pos, BlockState state) {
        super(WorkbenchContent.ENTITY.get(), pos, state);
        for (Direction side : Direction.values()) sides.put(side, side == Direction.DOWN ? SideMode.OUTPUT : SideMode.INPUT);
        createHandlers();
    }
    public void inventoryChanged(int slot) {
        if (slot != OUTPUT) { progress = 0; activeRecipe = null; }
        setChanged();
    }
    public SideMode sideMode(Direction side) { return sides.get(side); }
    public RedstoneMode redstoneMode() { return redstone; }
    public boolean powered() { return level != null && level.hasNeighborSignal(worldPosition); }
    public int progress() { return progress; }
    public boolean handleButton(int button) {
        if (button == 0) redstone = RedstoneMode.values()[(redstone.ordinal()+1)%4];
        else if (button >= 10 && button < 16) {
            Direction side = Direction.values()[button-10];
            sides.put(side, sideMode(side).next());
        } else return false;
        setChanged();
        return true;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, GuabaWorkbenchEntity machine) { machine.process(); }
    private void process() {
        if (level == null || level.isClientSide) return;
        var container = new SimpleContainer(inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2));
        var recipe = level.getRecipeManager().getRecipeFor(WorkbenchContent.RECIPE.get(), container, level).orElse(null);
        ItemStack guaba = inventory.getStackInSlot(CATALYST);
        if (recipe == null || !guaba.is(ModItems.GUABA.get()) || guaba.getCount() < Math.max(1, recipe.guabaCost())) {
            if (progress != 0 || activeRecipe != null) { progress = 0; activeRecipe = null; setChanged(); }
            status = 0;
            return;
        }
        if (!recipe.getId().equals(activeRecipe)) { progress = 0; activeRecipe = recipe.getId(); }
        if (!redstone.allows(powered())) { status = 3; return; }
        ItemStack result = recipe.getResultItem(level.registryAccess()), output = inventory.getStackInSlot(OUTPUT);
        if (!output.isEmpty() && (!ItemStack.isSameItemSameTags(output, result) || output.getCount()+result.getCount() > Math.min(output.getMaxStackSize(), inventory.getSlotLimit(OUTPUT)))) {
            status = 2;
            return;
        }
        status = 1;
        progress++;
        setChanged();
        if (progress < GuabaConversionRecipe.DURATION) return;
        // Commit all consumption and output together, exclusively on the server thread.
        ItemStack produced = result.copy();
        if (!output.isEmpty()) { produced = output.copy(); produced.grow(result.getCount()); }
        inventory.extractItem(INPUT, recipe.inputCount(), false);
        if (recipe.guabaCost() > 0) inventory.extractItem(CATALYST, recipe.guabaCost(), false);
        inventory.setStackInSlot(OUTPUT, produced);
        progress = 0;
        activeRecipe = null;
        setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.pinkkicore.guaba_workbench"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GuabaWorkbenchMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        if (activeRecipe != null) tag.putString("ActiveRecipe", activeRecipe.toString());
        tag.putInt("Redstone", redstone.ordinal());
        for (Direction side : Direction.values()) tag.putInt("Side_"+side.getName(), sideMode(side).ordinal());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag items = tag.getCompound("Inventory").copy();
        items.putInt("Size", 3);
        inventory.deserializeNBT(items);
        progress = Math.max(0, Math.min(GuabaConversionRecipe.DURATION-1, tag.getInt("Progress")));
        activeRecipe = ResourceLocation.tryParse(tag.getString("ActiveRecipe"));
        if (activeRecipe == null) progress = 0;
        redstone = RedstoneMode.fromId(tag.getInt("Redstone"));
        for (Direction side : Direction.values()) {
            if (tag.contains("Side_"+side.getName())) sides.put(side, SideMode.fromId(tag.getInt("Side_"+side.getName())));
        }
    }
    private void createHandlers() {
        for (Direction side : Direction.values()) handlers.put(side, LazyOptional.of(() -> new Port(side)));
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved() && side != null && capability == ForgeCapabilities.ITEM_HANDLER) return handlers.get(side).cast();
        return super.getCapability(capability, side);
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); handlers.values().forEach(LazyOptional::invalidate); }
    @Override public void reviveCaps() { super.reviveCaps(); createHandlers(); }
    private final class Port implements IItemHandler {
        private final Direction side;
        private Port(Direction side) { this.side = side; }
        private SideMode mode() { return isRemoved() ? SideMode.DISABLED : sideMode(side); }
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return mode().canInsertSlot(slot) && inventory.isItemValid(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return mode().canInsertSlot(slot) ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return mode().output && slot == OUTPUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    }
}
