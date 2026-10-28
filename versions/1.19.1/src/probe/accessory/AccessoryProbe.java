package pinkkicore.test;

import com.mojang.authlib.GameProfile;
import io.github.vitech.pinkkicore.forge.effect.*;
import io.github.vitech.pinkkicore.forge.item.ModItems;
import java.util.UUID;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod("pinkkicore_accessory_probe")
public final class AccessoryProbe {
    public AccessoryProbe() { MinecraftForge.EVENT_BUS.addListener(this::started); }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void started(ServerStartedEvent event) {
        try {
            var level = event.getServer().overworld();
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "AccessoryProbe"));
            for (String id : new String[]{"guaba_hood", "pink_blessing"}) {
                var recipe = level.getRecipeManager().byKey(new ResourceLocation("pinkkicore", id)).orElseThrow();
                check(recipe.getResultItem().getCount() == 1, "recipe count");
                var menu = new AbstractContainerMenu(null, -1) {
                    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
                    public boolean stillValid(Player p) { return true; }
                };
                var grid = new CraftingContainer(menu, 3, 3);
                var ingot = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("pinkkicore", "pink_ingot"));
                for (int i : id.equals("guaba_hood") ? new int[]{0,2,3,5} : new int[]{1,3,5,7}) grid.setItem(i, new ItemStack(ingot));
                if (id.equals("guaba_hood")) grid.setItem(1,new ItemStack(ModItems.GUABA.get()));
                check(((net.minecraft.world.item.crafting.CraftingRecipe)recipe).matches(grid,level), "recipe layout " + id);
                grid.setItem(4,new ItemStack(ingot));
                check(!((net.minecraft.world.item.crafting.CraftingRecipe)recipe).matches(grid,level), "extra ingredient accepted");
            }
            check(ModItems.GUABA_HOOD.get().getItemCategory() == net.minecraft.world.item.CreativeModeTab.TAB_COMBAT
                    && ModItems.PINK_BLESSING.get().getItemCategory() == net.minecraft.world.item.CreativeModeTab.TAB_COMBAT, "creative combat tab");
            if (ModList.get().isLoaded("curios")) CuriosChecks.run(player);
            else {
                check(new ItemStack(ModItems.GUABA_HOOD.get()).getMaxStackSize()==1,"hood size");
                check(player.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),200)),"no-Curios potion behavior");
                System.out.println("ACCESSORY optional Curios absent + recipes + items PASS");
            }
            System.out.println("ACCESSORY_PROBE PASS");
        } catch (Throwable error) { error.printStackTrace(); System.out.println("ACCESSORY_PROBE FAIL " + error); }
    }
    // Nested class is never loaded on the absent-Curios path.
    static final class CuriosChecks {
        static void tick(Player player) { MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(player)); }
        static void run(Player player) {
            var inv = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve().orElseThrow();
            var head = inv.getStacksHandler("head").orElseThrow().getStacks();
            var charm = inv.getStacksHandler("charm").orElseThrow().getStacks();
            check(head.getSlots()==1 && charm.getSlots()==1,"slot sizes");
            var hood = new ItemStack(ModItems.GUABA_HOOD.get());
            var blessing = new ItemStack(ModItems.PINK_BLESSING.get());
            // Curios 5.1's backing DynamicStackHandler is unrestricted; the real
            // CurioSlot validates through ICuriosHelper rather than isItemValid.
            var helper = CuriosApi.getCuriosHelper();
            var headContext = new top.theillusivec4.curios.api.SlotContext("head",player,0,false,true);
            var charmContext = new top.theillusivec4.curios.api.SlotContext("charm",player,0,false,true);
            check(helper.isStackValid(headContext,hood) && !helper.isStackValid(headContext,blessing),"head tag validation");
            check(helper.isStackValid(charmContext,blessing) && !helper.isStackValid(charmContext,hood),"charm tag validation");
            player.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),200));
            player.addEffect(new MobEffectInstance(GuabaPotions.THUNDER_GUABA.get(),200));
            head.setStackInSlot(0,hood); tick(player);
            check(!player.hasEffect(GuabaPotions.GUABA.get()) && !player.hasEffect(GuabaPotions.THUNDER_GUABA.get()),"old effects survived");
            for (var effect : new MobEffect[]{GuabaPotions.GUABA.get(),GuabaPotions.THUNDER_GUABA.get()}) {
                for (int amp=0;amp<4;amp++) check(!player.addEffect(new MobEffectInstance(effect,200,amp)),"immunity failed");
            }
            check(player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200)),"unrelated effect blocked");
            head.setStackInSlot(0,ItemStack.EMPTY); tick(player);
            check(player.addEffect(new MobEffectInstance(GuabaPotions.GUABA.get(),200)),"hood removal retained immunity");
            player.removeAllEffects();
            double armor=player.getAttributeValue(Attributes.ARMOR), hp=player.getMaxHealth();
            for(int round=0;round<3;round++) {
                charm.setStackInSlot(0,blessing.copy()); tick(player); tick(player);
                check(player.getAttributeValue(Attributes.ARMOR)==armor+2 && player.getMaxHealth()==hp+2,"passive attributes");
                charm.setStackInSlot(0,ItemStack.EMPTY); tick(player);
                check(player.getAttributeValue(Attributes.ARMOR)==armor && player.getMaxHealth()==hp,"attribute residue");
            }
            System.out.println("ACCESSORY slots + recipes + existing effects + immunity I-IV + unrelated effect + unequip + repeated attributes PASS");
        }
    }
}
