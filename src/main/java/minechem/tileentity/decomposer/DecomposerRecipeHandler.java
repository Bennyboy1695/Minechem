package minechem.tileentity.decomposer;

import java.util.ArrayList;

import minechem.utils.MinechemHelper;
import minechem.utils.Recipe;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class DecomposerRecipeHandler
{
    public static DecomposerRecipeHandler instance = new DecomposerRecipeHandler();
    
    
    private DecomposerRecipeHandler()
    {

    }
    
    public static void recursiveRecipes()
    {

        for (String key:Recipe.recipes.keySet()){
        	if (!DecomposerRecipe.recipes.containsKey(key)&&(!((String)key).contains("compressed_cobblestone"))){
        		Recipe recipe = Recipe.get(key);
        		DecomposerRecipe.add(new DecomposerRecipeSuper(recipe.output,recipe.inStacks));
        	}
//        	String output =recipes.get(key).output.toString()+": ";
//        	for (ItemStack component:recipes.get(key).inStacks)if (component!=null)output+=("["+component.toString()+"]");
//        	Minechem.LOGGER.info(output);
        }
        
        for (String key: DecomposerRecipe.recipes.keySet())
        {
        	if (DecomposerRecipe.get(key).isNull()) DecomposerRecipe.remove(key);
        }
    }
    
    public DecomposerRecipe getRecipe(ItemStack input)
    {
        return DecomposerRecipe.get(input);
    }
    
	public DecomposerRecipe getRecipe(FluidStack fluidStack) {
		return DecomposerRecipe.get(fluidStack);
	}


    public ArrayList<ItemStack> getRecipeOutputForInput(ItemStack input)
    {
        DecomposerRecipe recipe = getRecipe(input);
        if (recipe != null)
        {
            ArrayList<ItemStack> stacks = MinechemHelper.convertChemicalsIntoItemStacks(recipe.getOutput());
            return stacks;
        }
        return null;
    }

    public ArrayList<ItemStack> getRecipeOutputForFluidInput(FluidStack input)
    {
    	DecomposerFluidRecipe fluidRecipe = (DecomposerFluidRecipe)DecomposerRecipe.get(input);
        if (fluidRecipe != null)
        {
        	
            ArrayList<ItemStack> stacks = MinechemHelper.convertChemicalsIntoItemStacks(fluidRecipe.getOutput());
            return stacks;
        }
        return null;
    }


}
