package io.github.vitech.pinkkicore.neoforge.workbench;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
public record MachineInput(ItemStack... stacks) implements RecipeInput {
    @Override public ItemStack getItem(int slot) { return stacks[slot]; }
    @Override public int size() { return stacks.length; }
}
