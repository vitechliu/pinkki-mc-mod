package io.github.vitech.pinkkicore.forge.charging;

import io.github.vitech.pinkkicore.forge.item.ModItems;
import net.minecraft.core.BlockPos;
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

public final class GuabaChargingMenu extends AbstractContainerMenu {
    private final GuabaChargingEntity entity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    public final BlockPos pos;

    public GuabaChargingMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), null, new ItemStackHandler(2), new SimpleContainerData(10));
    }
    public GuabaChargingMenu(int id, Inventory inventory, GuabaChargingEntity entity) {
        this(id, inventory, entity.getBlockPos(), entity, entity.inventory, entity.data);
    }
    private GuabaChargingMenu(int id, Inventory playerInventory, BlockPos pos, GuabaChargingEntity entity, ItemStackHandler inventory, ContainerData data) {
        super(ChargingContent.MENU.get(), id);
        this.pos = pos; this.entity = entity; this.data = data;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        checkContainerDataCount(data, 10);
        addSlot(new MachineSlot(inventory, 0, 44, 53));
        addSlot(new MachineSlot(inventory, 1, 116, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col+row*9+9, 8+col*18, 91+row*18));
        for (int col = 0; col < 9; col++) addSlot(new net.minecraft.world.inventory.Slot(playerInventory, col, 8+col*18, 149));
        addDataSlots(data);
    }
    private class MachineSlot extends SlotItemHandler {
        private final int machineIndex;
        MachineSlot(ItemStackHandler handler, int slot, int x, int y) { super(handler, slot, x, y); machineIndex = slot; }
        @Override public void setChanged() {
            super.setChanged();
            if (entity != null) entity.inventoryChanged(machineIndex);
        }
    }
    public int progress() { return data.get(0); }
    public int redstoneMode() { return data.get(1); }
    public int sideMode(int side) { return data.get(2+side); }
    public boolean powered() { return data.get(8) != 0; }
    public int status() { return data.get(9); }
    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, ChargingContent.BLOCK.get())
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
