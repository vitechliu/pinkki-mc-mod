package io.github.vitech.pinkkicore.forge.niumo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class NiumoTableMenu extends AbstractContainerMenu {
    public static final int CRAFT = 0;
    public static final int FIRST_SIDE = 10;

    private final NiumoTableEntity entity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public final BlockPos pos;

    public NiumoTableMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), null, new ItemStackHandler(2), new SimpleContainerData(Direction.values().length));
    }
    public NiumoTableMenu(int id, Inventory inventory, NiumoTableEntity entity) {
        this(id, inventory, entity.getBlockPos(), entity, entity.inventory, entity.data);
    }
    private NiumoTableMenu(int id, Inventory playerInventory, BlockPos pos, NiumoTableEntity entity, ItemStackHandler inventory, ContainerData data) {
        super(NiumoTable.MENU.get(), id);
        this.pos = pos; this.entity = entity; this.data = data;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        checkContainerDataCount(data, Direction.values().length);
        addSlot(new MachineSlot(inventory, 0, 44, 43));
        addSlot(new MachineSlot(inventory, 1, 116, 43) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col+row*9+9, 8+col*18, 101+row*18));
        for (int col = 0; col < 9; col++) addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 8+col*18, 159));
        addDataSlots(data);
    }
    private class MachineSlot extends SlotItemHandler {
        MachineSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override public void setChanged() {
            super.setChanged();
            if (entity != null) entity.setChanged();
        }
    }

    public int sideMode(Direction side) { return data.get(side.ordinal()); }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, NiumoTable.BLOCK.get())
            && (entity == null || !entity.isRemoved() && player.level().getBlockEntity(pos) == entity);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (entity == null || !stillValid(player) || player.isSpectator()) return false;
        return entity.handleButton(button);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
