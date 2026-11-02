package pinkkicore.test;
import static pinkkicore.test.PortProbe.*;
import io.github.vitech.pinkkicore.neoforge.entity.*;
import io.github.vitech.pinkkicore.neoforge.ore.PinkOreContent;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import java.util.*;
final class WorldChecks {
    static void run(ServerLevel level) {
        var enchants=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silk=new ItemStack(Items.DIAMOND_PICKAXE);silk.enchant(enchants.getOrThrow(Enchantments.SILK_TOUCH),1);
        var fortune=new ItemStack(Items.DIAMOND_PICKAXE);fortune.enchant(enchants.getOrThrow(Enchantments.FORTUNE),3);
        for(var block:List.of(PinkOreContent.ORE.get(),PinkOreContent.DEEPSLATE_ORE.get())) {
            var state=block.defaultBlockState();var pos=new BlockPos(0,150,0);
            check(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state)&&!new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),"ore tool tier");
            var normal=Block.getDrops(state,level,pos,null,null,new ItemStack(Items.IRON_PICKAXE));
            check(normal.size()==1&&normal.getFirst().is(PinkOreContent.RAW_PINK.get())&&normal.getFirst().getCount()==1,"ore raw drop");
            var precise=Block.getDrops(state,level,pos,null,null,silk);
            check(precise.size()==1&&precise.getFirst().is(block.asItem()),"ore silk drop");
            int total=0;for(int i=0;i<128;i++) {
                var items=Block.getDrops(state,level,pos,null,null,fortune);
                check(items.size()==1&&items.getFirst().is(PinkOreContent.RAW_PINK.get())&&items.getFirst().getCount()<=4,"fortune drop");total+=items.getFirst().getCount();
            }
            check(total>128,"fortune no bonus");
        }
        for(String input:List.of("raw_pink","pink_ore","deepslate_pink_ore"))for(String method:List.of("smelting","blasting")) {
            var recipe=(AbstractCookingRecipe)level.getRecipeManager().byKey(id("pink_ingot_from_"+method+"_"+input)).orElseThrow().value();
            check(recipe.matches(new SingleRecipeInput(new ItemStack(BuiltInRegistries.ITEM.get(id(input)))),level),"smelting input");
            check(recipe.getResultItem(level.registryAccess()).is(PinkOreContent.INGOT.get())&&recipe.getCookingTime()==(method.equals("smelting")?200:100)&&recipe.getExperience()==1,"smelting output/time/XP");
        }
        for(String[] pair:new String[][]{{"pink_block","pink_ingot"},{"raw_pink_block","raw_pink"},{"pink_ingot","pink_nugget"}}) {
            var small=BuiltInRegistries.ITEM.get(id(pair[1]));var large=BuiltInRegistries.ITEM.get(id(pair[0]));
            var recipe=(CraftingRecipe)level.getRecipeManager().byKey(id(pair[0]+"_from_"+pair[1])).orElseThrow().value();
            var grid=CraftingInput.of(3,3,java.util.stream.IntStream.range(0,9).mapToObj(i->new ItemStack(small)).toList());
            check(recipe.matches(grid,level)&&recipe.assemble(grid,level.registryAccess()).is(large),"compression");
            var reverse=(CraftingRecipe)level.getRecipeManager().byKey(id(pair[1]+"_from_"+pair[0])).orElseThrow().value();
            var input=CraftingInput.of(1,1,List.of(new ItemStack(large)));
            check(reverse.matches(input,level)&&reverse.assemble(input,level.registryAccess()).getCount()==9,"decompression");
        }
        var configured=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for(String suffix:List.of("","_large","_buried")) {
            var ore=(OreConfiguration)configured.get(id("ore_pink"+suffix)).config();
            var diamond=(OreConfiguration)configured.get(id("minecraft:ore_diamond"+(suffix.isEmpty()?"_small":suffix))).config();
            check(ore.size==diamond.size&&ore.discardChanceOnAirExposure==diamond.discardChanceOnAirExposure,"ore diamond shape");
            var key=ResourceKey.create(Registries.PLACED_FEATURE,id("ore_pink"+suffix));
            check(level.getBiome(new BlockPos(0,-32,0)).value().getGenerationSettings().features().stream().flatMap(h->h.stream()).anyMatch(h->h.is(key)),"overworld ore missing");
            check(level.getServer().getLevel(Level.NETHER).getBiome(new BlockPos(0,32,0)).value().getGenerationSettings().features().stream().flatMap(h->h.stream()).noneMatch(h->h.is(key)),"ore in Nether");
        }
        var biomes=level.registryAccess().registryOrThrow(Registries.BIOME);
        var types=List.of(ModEntities.PINKKI.get(),ModEntities.GUABA_YINGRONG.get(),ModEntities.BINGWA.get());
        for(var biome:List.of(Biomes.PLAINS,Biomes.FOREST,Biomes.TAIGA,Biomes.SWAMP))for(var type:types) {
            var spawns=biomes.getHolderOrThrow(biome).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream().filter(s->s.type==type).toList();
            check(spawns.size()==1&&spawns.getFirst().getWeight().asInt()==1&&spawns.getFirst().minCount==1&&spawns.getFirst().maxCount==1,"wildlife biome/group");
        }
        for(var biome:List.of(Biomes.NETHER_WASTES,Biomes.THE_END))check(biomes.getHolderOrThrow(biome).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream().noneMatch(s->types.contains(s.type)),"wildlife dimension leak");
        int ores=0,deep=0,lower=0,upper=0;var cursor=new BlockPos.MutableBlockPos();
        for(int cx=8;cx<12;cx++)for(int cz=8;cz<12;cz++) {
            var chunk=level.getChunk(cx,cz);
            for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=-64;y<32;y++) {
                var state=chunk.getBlockState(cursor.set(cx*16+x,y,cz*16+z));
                if(state.is(PinkOreContent.ORE.get())||state.is(PinkOreContent.DEEPSLATE_ORE.get())) { ores++;if(state.is(PinkOreContent.DEEPSLATE_ORE.get()))deep++;if(y< -24)lower++;else upper++; }
            }
        }
        check(ores>0&&deep>0&&lower>upper,"new chunk ore distribution");
        pass("iron tier, raw/silk/fortune drops, 12 processing recipes, biome scope/wildlife groups, 16 real chunks ore="+ores+" deep="+deep+" lower="+lower+" upper="+upper);
        tongue(level);
    }
    static void tongue(ServerLevel level) {
        for(int scenario=0;scenario<3;scenario++) {
            var frog=ModEntities.BINGWA.get().create(level);frog.setPos(40,100,40);
            var cow=EntityType.COW.create(level);cow.setNoAi(true);cow.setPos(41.5,100,40);level.addFreshEntity(frog);level.addFreshEntity(cow);frog.setTarget(cow);
            float hp=cow.getHealth();frog.doHurtTarget(cow);
            if(scenario==1)cow.setPos(50,100,50);
            if(scenario==2)for(int y=100;y<103;y++)level.setBlockAndUpdate(new BlockPos(41,y,40),Blocks.STONE.defaultBlockState());
            for(int i=0;i<5;i++)frog.aiStep();check(cow.getHealth()==hp,"tongue early damage");frog.aiStep();
            check((cow.getHealth()<hp)==(scenario==0),"tongue delay/range/LOS "+scenario);
            check(!frog.canDrownInFluidType(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()),"frog drowning");
            frog.discard();cow.discard();
        }
        pass("tongue tick-6 damage, wind-up escape, wall rejection, amphibian drowning immunity");
    }
}
