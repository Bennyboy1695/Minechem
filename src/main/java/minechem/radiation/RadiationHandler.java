package minechem.radiation;

import java.util.ArrayList;
import java.util.List;
import minechem.MinechemItemsRegistration;
import minechem.Settings;
import minechem.api.INoDecay;
import minechem.api.IRadiationShield;
import minechem.item.element.ElementItem;
import minechem.item.molecule.MoleculeItem;
import minechem.utils.Constants;
import minechem.utils.MinechemUtil;
import minechem.utils.TimeHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class RadiationHandler
{

    private static RadiationHandler instace = new RadiationHandler();

    public static RadiationHandler getInstance()
    {
        return instace == null ? new RadiationHandler() : instace;
    }

    public class DecayEvent
    {
        public ItemStack before;
        public ItemStack after;
        public int damage;
        public long time;
    }

    public void update(EntityPlayer player)
    {
        Container openContainer = player.openContainer;
        if (openContainer != null)
        {
            if (openContainer instanceof INoDecay)
            {
                updateContainerNoDecay(player, openContainer, player.inventory);
            } else
            {
                updateContainer(player, openContainer, player.inventory);
            }
        } else
        {
            updateContainer(player, player.inventoryContainer, player.inventory);
        }
    }

    private void updateContainerNoDecay(EntityPlayer player, Container openContainer, IInventory inventory)
    {
        INoDecay container = (INoDecay)openContainer;
        List<ItemStack> itemstacks = container.getStorageInventory();
        if (itemstacks != null)
        {
            for (ItemStack itemstack : itemstacks)
            {
                if (itemstack != null && itemstack.getItem() == MinechemItemsRegistration.element && ElementItem.getRadioactivity(itemstack) != RadiationEnum.stable)
                {
                    RadiationInfo radiationInfo = ElementItem.getRadiationInfo(itemstack, player.worldObj);
                    radiationInfo.decayStarted++; // up the start with a tick to pause decay
                    ElementItem.setRadiationInfo(radiationInfo, itemstack);
                }
            }
        }
        List<ItemStack> playerStacks = container.getPlayerInventory();
        if (playerStacks != null)
        {
            updateRadiationOnItems(player.worldObj, player, openContainer, inventory, playerStacks);
        }
    }

    private void updateContainer(EntityPlayer player, Container container, IInventory inventory )
    {
        List<ItemStack> itemstacks = container.getInventory();
        updateRadiationOnItems(player.worldObj, player,container, inventory, itemstacks);
    }

    private List<DecayEvent> updateRadiationOnItems(World world, EntityPlayer player,Container container, IInventory inventory, List<ItemStack> itemstacks)
    {
        List<DecayEvent> events = new ArrayList<DecayEvent>();
        for (ItemStack itemstack : itemstacks)
        {
        	if (itemstack != null){
        		RadiationEnum radiation=null;
        		Item item=itemstack.getItem();
        		if (item == MinechemItemsRegistration.element)
                {
        			radiation = ElementItem.getRadioactivity(itemstack);
        		}
                else if (item == MinechemItemsRegistration.molecule){
        			radiation = MoleculeItem.getMolecule(itemstack).radioactivity();
        		}
        		
	            if (radiation != null && radiation != RadiationEnum.stable)
	            {
	                DecayEvent decayEvent = new DecayEvent();
                    decayEvent.time = world.getTotalWorldTime() - ElementItem.getRadiationInfo(itemstack, world).decayStarted;
	                decayEvent.before = itemstack.copy();
	                decayEvent.damage = updateRadiation(world, itemstack, inventory,player.posX,player.posY,player.posZ);
	                decayEvent.after = itemstack.copy();
	                if (decayEvent.damage > 0)
	                {
	                    events.add(decayEvent);
	                }
	                if (decayEvent.damage > 0 && container != null)
	                {
	                    applyRadiationDamage(player, container, decayEvent.damage);
	                    printRadiationDamageToChat(player, decayEvent);
	                }
	            }
        	}
        }
        return events;
    }

    private void applyRadiationDamage(EntityPlayer player, Container container, int damage)
    {
        List<Float> reductions = new ArrayList<Float>();
        if (container instanceof IRadiationShield)
        {
            float reduction = ((IRadiationShield) container).getRadiationReductionFactor(damage, null, player);
            reductions.add(reduction);
        }
        for (ItemStack armour : player.inventory.armorInventory)
        {
            if (armour != null && armour.getItem() instanceof IRadiationShield)
            {
                float reduction = ((IRadiationShield) armour.getItem()).getRadiationReductionFactor(damage, armour, player);
                reductions.add(reduction);
            }
        }
        float totalReductionFactor = 1;
        for (float reduction : reductions)
        {
            totalReductionFactor -= reduction;
        }
        if (totalReductionFactor < 0)
        {
            totalReductionFactor = 0;
        }
        damage = Math.round(damage * totalReductionFactor);
        player.attackEntityFrom(DamageSource.generic, damage);
    }

    private void printRadiationDamageToChat(EntityPlayer player, DecayEvent decayEvent)
    {
        String nameBeforeDecay = getLongName(decayEvent.before);
        String nameAfterDecay = getLongName(decayEvent.after);
        String time = TimeHelper.getTimeFromTicks(decayEvent.time);
        String message = String.format("Radiation Warning: Element %s decayed into %s after %s.", nameBeforeDecay, nameAfterDecay, time);
        player.addChatMessage(new ChatComponentText(message));
    }

    private String getLongName(ItemStack stack){
    	Item item=stack.getItem();
    	if (item==MinechemItemsRegistration.element){
    		return ElementItem.getLongName(stack);
    	}else if (item==MinechemItemsRegistration.molecule){
    		return MoleculeItem.getMolecule(stack).descriptiveName();
    	}
    	return "null";
    }
    
    private int updateRadiation(World world, ItemStack element,IInventory inventory,double x,double y,double z)
    {
        RadiationInfo radiationInfo = ElementItem.getRadiationInfo(element, world);
        int dimensionID = world.provider.dimensionId;
        if (dimensionID != radiationInfo.dimensionID && radiationInfo.isRadioactive())
        {
            radiationInfo.dimensionID = dimensionID;
            ElementItem.setRadiationInfo(radiationInfo, element);
            return 0;
        } else
        {
            long currentTime = world.getTotalWorldTime();
            return decayElement(element, radiationInfo, currentTime, world, inventory,x,y,z);
        }
    }

    private int decayElement(ItemStack element, RadiationInfo radiationInfo, long currentTime, World world,IInventory inventory,double x,double y,double z)
    {
        //try to decay every second
        if (radiationInfo.lastDecayUpdate < currentTime - Constants.TICKS_PER_SECOND)
        {
            radiationInfo.lastDecayUpdate = currentTime;
            long lifeTime = currentTime - radiationInfo.decayStarted;
            int minsAlive = (int)(lifeTime / Constants.TICKS_PER_MINUTE); // minutes for precision
            float chance = radiationInfo.radioactivity.getDecayChance() * MinechemUtil.ran.nextInt(minsAlive);
            // the safe zone check
            float safeZone = 0.5F * (100F / Settings.halfLifeMultiplier);
            if (chance/60 > safeZone)
            {
                int damage = radiationInfo.radioactivity.getDamage();
                Item item = element.getItem();
                if (item == MinechemItemsRegistration.element)
                {
                    radiationInfo = ElementItem.decay(element, world);
                }
                else if (item == MinechemItemsRegistration.molecule)
                {
                    radiationInfo = RadiationMoleculeHandler.getInstance().handleRadiationMolecule(world, element, inventory, x, y, z);
                }
                ElementItem.setRadiationInfo(radiationInfo, element);
                return damage;
            }
        }
        ElementItem.setRadiationInfo(radiationInfo, element);
        return 0;
    }

}
