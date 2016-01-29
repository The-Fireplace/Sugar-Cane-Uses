package psc_team.psc.entity.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import psc_team.psc.items.SCUItems;
import psc_team.psc.recipes.DehydratorRecipes;
import psc_team.psc.tools.Tools;

import java.util.Iterator;
import java.util.Map;

/**
 * @author The_Fireplace
 */
public class TileEntityDehydrator extends TileEntity implements ISidedInventory {
	private ItemStack[] inventory;
	public static final String PROP_NAME = "TileEntityDehydrator";
	int storedFuel = 0;
	int depletedCounter = 0;
	/** The number of ticks that the furnace will keep burning */
	private int furnaceBurnTime;
	/** The number of ticks that a fresh copy of the currently-burning item would keep the furnace burning for */
	private int currentItemBurnTime;
	private int cookTime;
	private int totalCookTime;

	public TileEntityDehydrator(){
		inventory = new ItemStack[12];
	}

	@Override
	public Packet getDescriptionPacket(){
		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		writeToNBT(nbtTagCompound);
		int metadata = getBlockMetadata();
		return new S35PacketUpdateTileEntity(this.pos, metadata, nbtTagCompound);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt){
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public String getName() {
		return StatCollector.translateToLocal("tile.dehydrator.name");
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public IChatComponent getDisplayName() {
		return null;
	}

	@Override
	public int getSizeInventory() {
		return inventory.length;
	}

	@Override
	public ItemStack getStackInSlot(int index) {
		return inventory[index];
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		ItemStack is = getStackInSlot(index);
		if(is != null){
			if(is.stackSize <= count){
				setInventorySlotContents(index, null);
			}else{
				is = is.splitStack(count);
				markDirty();
			}
		}
		return is;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack is = getStackInSlot(index);
		setInventorySlotContents(index, null);
		return is;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		boolean flag = stack != null && stack.isItemEqual(this.inventory[index]) && ItemStack.areItemStackTagsEqual(stack, this.inventory[index]);
		inventory[index] = stack;

		if(stack != null && stack.stackSize > getInventoryStackLimit()){
			stack.stackSize = getInventoryStackLimit();
		}

		if (index == 0 && !flag)
		{
			this.totalCookTime = this.getCookTime(stack);
			this.cookTime = 0;
			this.markDirty();
		}
	}

	public int getCookTime(ItemStack stack)
	{
		return 800;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
		for(int i = 0; i < inventory.length; ++i){
			inventory[i]=null;
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound compound){
		super.writeToNBT(compound);
		compound.setShort("BurnTime", (short)this.furnaceBurnTime);
		compound.setShort("CookTime", (short)this.cookTime);
		compound.setShort("CookTimeTotal", (short)this.totalCookTime);
		NBTTagList list = new NBTTagList();
		for(int i = 0; i<getSizeInventory();i++){
			ItemStack is = getStackInSlot(i);
			if(is != null){
				NBTTagCompound item = new NBTTagCompound();

				item.setByte("SlotDehydrator", (byte)i);
				is.writeToNBT(item);

				list.appendTag(item);
			}
		}
		compound.setInteger("StoredFuel", storedFuel);
		compound.setInteger("DepletedFuel", depletedCounter);
		compound.setTag("ItemsDehydrator", list);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound){
		super.readFromNBT(compound);
		NBTTagList list = (NBTTagList) compound.getTag("ItemsDehydrator");
		if(list != null){
			for(int i = 0; i<list.tagCount();i++){
				NBTTagCompound item = (NBTTagCompound) list.get(i);
				int slot = item.getByte("SlotDehydrator");
				if(slot >= 0 && slot < getSizeInventory()){
					setInventorySlotContents(slot, ItemStack.loadItemStackFromNBT(item));
				}
			}
		}else{
			System.out.println("List was null when reading TileEntityDehydrator NBTTagCompound");
		}
		this.furnaceBurnTime = compound.getShort("BurnTime");
		this.cookTime = compound.getShort("CookTime");
		this.totalCookTime = compound.getShort("CookTimeTotal");
		this.currentItemBurnTime = TileEntityFurnace.getItemBurnTime(this.inventory[1]);//TODO make sure this is correct.
		this.storedFuel = compound.getInteger("StoredFuel");
		this.depletedCounter = compound.getInteger("DepletedFuel");
	}

	public boolean isBurning()
	{
		return this.furnaceBurnTime > 0;
	}

	@SideOnly(Side.CLIENT)
	public static boolean isBurning(IInventory p_174903_0_)
	{
		return p_174903_0_.getField(0) > 0;
	}

	public void addToFuel(int amount){
		storedFuel += amount;
		this.worldObj.markBlockForUpdate(getPos());
	}

	public void removeFromFuel(int amount){
		storedFuel -= amount;
		depletedCounter += amount;
		if(storedFuel < 0)
			storedFuel = 0;
		if(depletedCounter >= 6400){
			if(this.getStackInSlot(11) != null) {
				if (this.getStackInSlot(11).stackSize < 64) {
					depletedCounter -= 6400;
					this.setInventorySlotContents(11, new ItemStack(SCUItems.depletedFuelBar, this.getStackInSlot(11).stackSize + 1));
				}
			}else{
				depletedCounter -= 6400;
				this.setInventorySlotContents(11, new ItemStack(SCUItems.depletedFuelBar));
			}
		}
		this.worldObj.markBlockForUpdate(getPos());
	}

	public int getStoredFuel(){
		return storedFuel;
	}

	public void dehydrateItems(){
		if(canDehydrate()){
				if(storedFuel > 0){
					removeFromFuel(1);
				}else
					return;
			dehydrate();
		}
	}

	private boolean canDehydrate(){return dehydrate(false);}
	private void dehydrate(){dehydrate(true);}

	private boolean dehydrate(boolean performAction){
		Integer firstSuitableInputSlot = null;
		Integer firstSuitableOutputSlot = null;
		ItemStack result = null;

		for(int inputSlot = 0; inputSlot < 5; inputSlot++){
			if(inventory[inputSlot] != null && DehydratorRecipes.instance().getResult(inventory[inputSlot]) != null){
				result = new ItemStack(DehydratorRecipes.instance().getResult(inventory[inputSlot]).getItem(), DehydratorRecipes.instance().getResultCount(inventory[inputSlot]), DehydratorRecipes.instance().getResult(inventory[inputSlot]).getMetadata());
				if(result != null){
					for(int outputSlot = 5;outputSlot<10;outputSlot++){
						ItemStack outputStack = inventory[outputSlot];
						if(outputStack == null){
							firstSuitableInputSlot = inputSlot;
							firstSuitableOutputSlot = outputSlot;
							break;
						}

						if(outputStack.getItem() == result.getItem() && (!outputStack.getHasSubtypes() || outputStack.getMetadata() == outputStack.getMetadata()) && ItemStack.areItemStackTagsEqual(outputStack, result)){
							int combinedSize = inventory[outputSlot].stackSize + result.stackSize;
							if(combinedSize <= getInventoryStackLimit() && combinedSize <= inventory[outputSlot].getMaxStackSize()){
								firstSuitableInputSlot = inputSlot;
								firstSuitableOutputSlot = outputSlot;
								break;
							}
						}
					}
					if(firstSuitableInputSlot != null) break;
				}
			}
		}

		if(firstSuitableInputSlot == null) return false;
		if(this.storedFuel <= 0) return false;
		if(!performAction) return true;

		inventory[firstSuitableInputSlot].stackSize--;
		if(inventory[firstSuitableInputSlot].stackSize <= 0) inventory[firstSuitableInputSlot] = null;
		if(inventory[firstSuitableOutputSlot] == null){
			inventory[firstSuitableOutputSlot] = result.copy();
		}else{
			inventory[firstSuitableOutputSlot].stackSize += result.stackSize;
		}
		markDirty();
		return true;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		if(side == EnumFacing.UP){
			return new int[]{0,1,2,3,4};//Items to dehydrate
		}
		if(side == EnumFacing.EAST || side == EnumFacing.WEST || side == EnumFacing.NORTH || side == EnumFacing.SOUTH){
			return new int[]{10};//Fuel
		}
		if(side == EnumFacing.DOWN){
			return new int[]{5,6,7,8,9,11};//Depleted fuel, dehydrated items
		}
		return null;
	}

	@Override
	public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
		if(stack != null){
			if(index >= 0 && index < 5){
				Iterator iterator = DehydratorRecipes.instance().getDehydratingList().entrySet().iterator();
				Map.Entry entry;
				do{
					if(!iterator.hasNext()){
						return false;
					}
					entry = (Map.Entry)iterator.next();
				}while(!Tools.areItemStacksEqual(new ItemStack(stack.getItem(), stack.getMetadata()), (ItemStack)entry.getKey()));
				return true;
			}
			if(index == 10){
				return DehydratorRecipes.instance().isFuel(stack);
			}}
		return false;
	}

	@Override
	public boolean canExtractItem(int index, ItemStack stack,
								  EnumFacing direction) {
		if(stack != null)
			if(index >= 5 && index < 10){
				return true;
			}
		return false;
	}
}