package io.github.vitech.pinkkicore.forge.niumo;

import io.github.vitech.pinkkicore.forge.workbench.SideMode;

import java.util.EnumMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public final class NiumoTableEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT = 0, OUTPUT = 1;
    private final EnumMap<Direction, SideMode> sides = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, LazyOptional<IItemHandler>> handlers = new EnumMap<>(Direction.class);
    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT;
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public final ContainerData data = new ContainerData() {
        @Override public int getCount() { return Direction.values().length; }
        @Override public int get(int index) { return sideMode(Direction.values()[index]).ordinal(); }
        @Override public void set(int index, int value) {}
    };

    public NiumoTableEntity(BlockPos pos, BlockState state) {
        super(NiumoTable.ENTITY.get(), pos, state);
        for (Direction side : Direction.values()) sides.put(side, side == Direction.DOWN ? SideMode.OUTPUT : SideMode.INPUT);
        createHandlers();
    }
    public SideMode sideMode(Direction side) {
        return sides.get(side);
    }

    public boolean handleButton(int button) {
        if (!(level instanceof ServerLevel serverLevel) || isRemoved()) return false;
        if (button == NiumoTableMenu.CRAFT) return craft(serverLevel);
        int index = button - NiumoTableMenu.FIRST_SIDE;
        if (index < 0 || index >= Direction.values().length) return false;
        Direction side = Direction.values()[index];
        sides.put(side, sideMode(side) == SideMode.BOTH ? SideMode.DISABLED : sideMode(side).next());
        setChanged();
        return true;
    }

    private boolean craft(ServerLevel level) {
        ItemStack input = inventory.getStackInSlot(INPUT);
        ItemStack output = inventory.getStackInSlot(OUTPUT);
        var container = new SimpleContainer(input);
        var recipe = level.getRecipeManager().getRecipeFor(NiumoTable.RECIPE.get(), container, level).orElse(null);
        if (recipe == null) return false;

        ItemStack produced = recipe.assemble(container, level.registryAccess());
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(output, produced)) return false;
            produced.grow(output.getCount());
        }
        if (produced.getCount() > Math.min(produced.getMaxStackSize(), inventory.getSlotLimit(OUTPUT))) {
            return false;
        }
        inventory.extractItem(INPUT, recipe.inputCount(), false);
        inventory.setStackInSlot(OUTPUT, produced);
        NiumoSounds.play(level, Vec3.atCenterOf(worldPosition), SoundSource.BLOCKS);
        return true;
    }

    @Override public Component getDisplayName() { return Component.translatable("block.pinkkicore.niumo_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new NiumoTableMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        for (Direction side : Direction.values()) tag.putInt("Side_"+side.getName(), sideMode(side).ordinal());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag items = tag.getCompound("Inventory").copy();
        items.putInt("Size", 2);
        inventory.deserializeNBT(items);
        for (Direction side : Direction.values()) {
            if (tag.contains("Side_"+side.getName())) {
                int mode = tag.getInt("Side_"+side.getName());
                sides.put(side, mode >= 0 && mode < 4 ? SideMode.values()[mode] : SideMode.DISABLED);
            }
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
        @Override public boolean isItemValid(int slot, ItemStack stack) { return mode().input && slot == INPUT && inventory.isItemValid(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return mode().input && slot == INPUT ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return mode().output && slot == OUTPUT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    }
}
