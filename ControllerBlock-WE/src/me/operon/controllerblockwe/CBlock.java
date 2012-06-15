package me.operon.controllerblockwe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.RedstoneWire;

public class CBlock {
	private Location blockLocation = null;
	private Material blockType = null;
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<BlockDesc> placedBlocks = new ArrayList();
	private String owner = null;

	private ControllerBlock parent = null;
	private boolean on = false;
	private boolean edit = false;
	public byte protectedLevel = 0;

	public CBlock(ControllerBlock p, Location l, String o, byte pl) {
		parent = p;
		blockLocation = l;
		owner = o;
		protectedLevel = pl;
	}

	public ControllerBlock getParent() {
		return parent;
	}

	public String getOwner() {
		return owner;
	}

	public Material getType() {
		return blockType;
	}

	public void setType(Material m) {
		blockType = m;
	}

	public Location getLoc() {
		return blockLocation;
	}

	public Iterator<BlockDesc> getBlocks() {
		return placedBlocks.iterator();
	}

	public boolean addBlock(Block b) {
		if (b.getType().equals(blockType)) {
			Location bloc = b.getLocation();
			if (placedBlocks.isEmpty()) {
				placedBlocks
				.add(new BlockDesc(bloc, Byte.valueOf(b.getData())));
				return true;
			}
			ListIterator<BlockDesc> i = placedBlocks.listIterator();
			while (i.hasNext()) {
				BlockDesc loc = i.next();
				if (bloc.getBlockY() > loc.blockLoc.getBlockY()) {
					i.previous();
					i.add(new BlockDesc(bloc, Byte.valueOf(b.getData())));
					return true;
				}
			}
			placedBlocks.add(new BlockDesc(bloc, Byte.valueOf(b.getData())));
			return true;
		}

		return false;
	}

	public boolean delBlock(Block b) {
		Location u = b.getLocation();
		for (Iterator<BlockDesc> i = placedBlocks.iterator(); i.hasNext();) {
			Location t = i.next().blockLoc;
			if (t.equals(u)) {
				i.remove();
				CBlock check = parent.getControllerBlockFor(this, u, null,
						Boolean.valueOf(true));
				if (check != null) {
					b.setType(check.blockType);
					b.setData(check.getBlock(u).blockData);
				}
				return true;
			}
		}
		return false;
	}

	public int numBlocks() {
		return placedBlocks.size();
	}

	public BlockDesc getBlock(Location l) {
		Iterator<BlockDesc> i = placedBlocks.iterator();
		while (i.hasNext()) {
			BlockDesc d = i.next();
			if (d.blockLoc.equals(l)) {
				return d;
			}
		}
		return null;
	}

	public boolean hasBlock(Location l) {
		return getBlock(l) != null;
	}

	public void updateBlock(Block b) {
		Iterator<BlockDesc> i = placedBlocks.iterator();
		while (i.hasNext()) {
			BlockDesc d = i.next();
			if (d.blockLoc.equals(b.getLocation())) {
				d.blockData = b.getState().getData().getData();
				return;
			}
		}
	}

	public boolean isBeingEdited() {
		return edit;
	}

	public void editBlock(boolean b) {
		edit = b;
		if (edit) {
			turnOn();
		} else {
			parent.saveData(this);
			doRedstoneCheck();
		}
	}

	public void destroyWithOutDrops() {
		turnOff();
	}

	public void destroy() {
		turnOff();
		int i = placedBlocks.size();
		int j = 0;
		while (i > 0) {
			if (i > 64) {
				j = 64;
				i -= 64;
			} else {
				j = i;
				i -= i;
			}
			blockLocation.getWorld().dropItemNaturally(blockLocation,
					new ItemStack(blockType, j));
		}
	}

	public boolean isOn() {
		return on;
	}

	public void doRedstoneCheck() {
		Block check = Util.getBlockAtLocation(blockLocation).getRelative(
				BlockFace.UP);
		doRedstoneCheck(check.getState());
	}

	public void doRedstoneCheck(BlockState s) {
		if (isBeingEdited()) {
			return;
		}
		if (s.getType().equals(Material.REDSTONE_TORCH_ON)) {
			turnOff();
		} else if (s.getType().equals(Material.REDSTONE_TORCH_OFF)) {
			turnOn();
		} else if (s.getType().equals(Material.REDSTONE_WIRE)) {
			if (((RedstoneWire) s.getData()).isPowered()) {
				turnOff();
			} else {
				turnOn();
			}
		} else if (s.getType().equals(Material.AIR)) {
			turnOn();
		}
	}

	public void turnOff() {
		Iterator<BlockDesc> i = placedBlocks.iterator();
		while (i.hasNext()) {
			BlockDesc d = i.next();
			Location loc = d.blockLoc;
			CBlock check = parent.getControllerBlockFor(this, loc, null,
					Boolean.valueOf(true));
			Block cur = loc.getWorld().getBlockAt(loc.getBlockX(),
					loc.getBlockY(), loc.getBlockZ());
			boolean applyPhysics = true;

			if (check != null) {
				cur.setTypeIdAndData(check.blockType.getId(),
						check.getBlock(loc).blockData, applyPhysics);
			} else if (protectedLevel == 0) {
				cur.setType(Material.AIR);
			}
		}
		on = false;
	}

	public void turnOn() {
		for (Iterator<BlockDesc> i = placedBlocks.iterator(); i.hasNext();) {
			BlockDesc b = i.next();
			Location loc = b.blockLoc;
			Block cur = loc.getWorld().getBlockAt(loc.getBlockX(),
					loc.getBlockY(), loc.getBlockZ());
			boolean applyPhysics = true;
			if (protectedLevel == 0) {
				if ((cur.getType().equals(Material.SAND))
						|| (cur.getType().equals(Material.GRAVEL))
						|| (cur.getType().equals(Material.TORCH))
						|| (cur.getType().equals(Material.REDSTONE_TORCH_OFF))
						|| (cur.getType().equals(Material.REDSTONE_TORCH_ON))
						|| (cur.getType().equals(Material.RAILS))
						|| (cur.getType().equals(Material.LADDER))
						|| (cur.getType().equals(Material.GRAVEL))
						|| (cur.getType().equals(Material.POWERED_RAIL))
						|| (cur.getType().equals(Material.DETECTOR_RAIL))) {
					applyPhysics = false;
				}
			}
			cur.setTypeIdAndData(blockType.getId(), b.blockData, applyPhysics);
		}
		on = true;
	}

	public void turnOn(Location l) {
		Iterator<BlockDesc> i = placedBlocks.iterator();
		while (i.hasNext()) {
			BlockDesc b = i.next();
			if (l.equals(b.blockLoc)) {
				Block cur = Util.getBlockAtLocation(l);
				boolean applyPhysics = true;
				if (protectedLevel == 0) {
					applyPhysics = false;
				}
				cur.setTypeIdAndData(blockType.getId(), b.blockData,
						applyPhysics);
			}
		}
	}

	public CBlock(ControllerBlock p, int version, String s) {
		parent = p;
		String[] args = s.split(",");

		if (((version < 3) && (args.length < 4))
				|| ((version >= 3) && (args.length < 5))) {
			parent.log
			.severe("ERROR: Invalid ControllerBlock description in data file, skipping");
			return;
		}

		if (version >= 4) {
			blockLocation = parseLocation(p.getServer(), args[0], args[1],
					args[2], args[3]);
			parent.log.debug("CB Location: "
					+ Util.formatLocation(blockLocation));
		}

		blockType = Material.getMaterial(args[3]);
		int i;
		if (version >= 3) {
			owner = args[5];
			i = 6;
		} else {
			owner = null;
			i = 7;
		}

		protectedLevel = 0;
		if (i < args.length) {
			if (args[i].equals("protected")) {
				protectedLevel = 0;
				i++;
			}
			if (args[i].equals("semi-protected")) {
				protectedLevel = 1;
				i++;
			} else if (args[i].equals("unprotected")) {
				protectedLevel = 2;
				i++;
			}
		}

		while (i < args.length) {
			if (version >= 4) {
				if (args.length - i >= 6) {
					placedBlocks.add(new BlockDesc(
							parseLocation(p.getServer(), args[(i++)],
									args[(i++)], args[(i++)], args[(i++)]),
									Byte.valueOf(Byte.parseByte(args[(i++)]))));
				} else {
					parent.log
					.severe("ERROR: Block description in save file is corrupt");
					return;
				}
			}
		}
	}

	public String serialize() {
		String result = loc2str(blockLocation);
		result = result + "," + blockType;
		result = result + "," + owner;
		if (protectedLevel == 1) {
			result = result + ",semi-protected";
		} else if (protectedLevel == 2) {
			result = result + ",unprotected";
		}
		Iterator<BlockDesc> i = placedBlocks.iterator();
		while (i.hasNext()) {
			BlockDesc b = i.next();
			result = result + "," + loc2str(b.blockLoc);
			result = result + "," + Byte.toString(b.blockData);
		}
		return result;
	}

	public Location parseLocation(Server server, String worldName, String X,
			String Y, String Z) {
		return new Location(server.getWorld(worldName), Integer.parseInt(X),
				Integer.parseInt(Y), Integer.parseInt(Z));
	}

	public String loc2str(Location l) {
		if (l == null) {
			parent.log
			.severe("ERROR: null location while trying to save CBlock at "
					+ loc2str(blockLocation));
		}
		if (l.getWorld() == null) {
			parent.log
			.severe("ERROR: null world in location while trying to save CBlock");
		}
		return l.getWorld().getName() + "," + l.getBlockX() + ","
		+ l.getBlockY() + "," + l.getBlockZ();
	}
}