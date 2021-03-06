package com.astro73.controllerblock4k;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;

/**
 * This handles actually interfacing with the storage system.
 * 
 * @author astronouth7303
 * 
 */
public class CBlockStore {
	private Connection conn;
	@SuppressWarnings("unused")
	private ControllerBlock parent;

	private PreparedStatement update_lord, insert_lord, update_serf,
			insert_serf;

	/**
	 * Connects to the database
	 * 
	 * @param config
	 *            The configuration object
	 * @throws SQLException
	 *             From JDBC
	 */
	public CBlockStore(ControllerBlock cb, Configuration config) throws SQLException {
		parent = cb;
		conn = DriverManager.getConnection((String)config.getString("SqlConnection"));

		update_lord = conn
				.prepareStatement("UPDATE ControllerBlock_Lord SET world = ?, x = ?, y = ?, z = ?, owner = ?, protection = ? WHERE id = ?;");
		insert_lord = conn
				.prepareStatement(
						"INSERT INTO ControllerBlock_Lord SET world = ?, x = ?, y = ?, z = ?, owner = ?, protection = ?;",
						Statement.RETURN_GENERATED_KEYS);
		update_serf = conn
				.prepareStatement("UPDATE ControllerBlock_Serf SET world = ?, x = ?, y = ?, z = ?, material = ?,  meta = ? WHERE lord = ? AND id = ?;");
		insert_serf = conn
				.prepareStatement(
						"INSERT INTO ControllerBlock_Serf SET world = ?, x = ?, y = ?, z = ?, material = ?,  meta = ?, lord = ?;",
						Statement.RETURN_GENERATED_KEYS);
	}

	/**
	 * Stores the controlling block
	 * 
	 * @param id
	 *            The block's ID persistent across moves, or 0 if it has NONE
	 *            yet
	 * @param loc
	 *            The block's location
	 * @param owner
	 *            The block's owner player
	 * @param pl
	 *            The block's protection level
	 * @return The block's ID in the store.
	 */
	public long storeLordBlock(long id, Location loc, String owner,
			CBlock.Protection pl) {
		PreparedStatement stmt = id == 0 ? insert_lord : update_lord;
		try {
			stmt.setString(1, loc.getWorld().getName());
			stmt.setInt   (2, loc.getBlockX());
			stmt.setInt   (3, loc.getBlockY());
			stmt.setInt   (4, loc.getBlockZ());
			stmt.setString(5, owner);
			stmt.setString(6, pl.name());
			if (id == 0) {
				// INSERT new block
			} else {
				// UPDATE existing block
				stmt.setLong(7, id);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		try {
			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (id == 0) {
			try {
				ResultSet rs = stmt.getGeneratedKeys();
				rs.next();
				return rs.getLong(1);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
		} else {
			return id;
		}
	}

	/**
	 * Stores a controlled block
	 * 
	 * @param lord
	 *            The ID of the ControllerBlock controlling it
	 * @param bd
	 *            The data on the block
	 * @return
	 */
	public long storeSerfBlock(long lord, BlockDesc bd) {
		PreparedStatement stmt = bd.id == 0 ? insert_serf : update_serf;
		try {
			stmt.setString(1, bd.loc.getWorld().getName());
			stmt.setInt   (2, bd.loc.getBlockX());
			stmt.setInt   (3, bd.loc.getBlockY());
			stmt.setInt   (4, bd.loc.getBlockZ());
			stmt.setInt   (5, bd.mat.getId());
			stmt.setByte  (6, bd.data);
			stmt.setLong  (7, lord);
			if (bd.id == 0) {
				// INSERT new block
			} else {
				// UPDATE existing block
				stmt.setLong(8, bd.id);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		try {
			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (bd.id == 0) {
			try {
				ResultSet rs = stmt.getGeneratedKeys();
				rs.next();
				return rs.getLong(1);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return 0;
			}
		} else {
			return bd.id;
		}
	}

	public Iterable<CBlock> loadAllLords(ControllerBlock parent) {
		try {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT * FROM ControllerBlock_Lord;");
			ResultSet rs = stmt.executeQuery();
			return new LordIterable(this, rs, parent);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	class LordIterable implements Iterable<CBlock> {
		private ResultSet rs;
		private CBlockStore store;
		ControllerBlock parent;

		public LordIterable(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			parent = p;
		}

		@Override
		public Iterator<CBlock> iterator() {
			return new LordIterator(store, rs, parent);
		}
	}

	class LordIterator implements Iterator<CBlock> {
		CBlockStore store;
		ResultSet rs;
		boolean checked, hasnext;
		ControllerBlock parent;

		public LordIterator(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			checked = hasnext = false;
			parent = p;
		}

		@Override
		public boolean hasNext() {
			if (!checked) {
				try {
					hasnext = rs.next();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				checked = true;
			}
			return hasnext;
		}

		@Override
		public CBlock next() {
			while (true) {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				try {
					long id = rs.getLong("id");
					String ws = rs.getString("world");
					int x = rs.getInt("x");
					int y = rs.getInt("y");
					int z = rs.getInt("z");
					String owner = rs.getString("owner");
					String ps = rs.getString("protection");
					CBlock.Protection pl = CBlock.Protection.valueOf(ps);
					World world = parent.getServer().getWorld(ws);
					if (world == null) {
						parent.getLogger().severe(String.format("Unable to load lord block %d: World %s not found.", id, ws));
						continue;
					}
					Location loc = new Location(world, x, y, z);
					CBlock cb = new CBlock(parent, id, loc, owner, pl);
					cb.loadSerfs(store);
					return cb;
				} catch (SQLException e) {
					parent.getLogger().throwing(this.getClass().getCanonicalName(), "next", e);
					continue;
				} finally {
					checked = false;
				}
			}
		}

		@Override
		public void remove() {
			try {
				rs.deleteRow();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new IllegalStateException();
			}
		}
	}

	public Iterable<BlockDesc> loadAllSerfs(ControllerBlock parent, long lord) {
		try {
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT * FROM ControllerBlock_Serf WHERE lord = ?;");
			stmt.setLong(1, lord);
			ResultSet rs = stmt.executeQuery();
			return new SerfIterable(this, rs, parent);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	class SerfIterable implements Iterable<BlockDesc> {
		private ResultSet rs;
		private CBlockStore store;
		ControllerBlock parent;

		public SerfIterable(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			parent = p;
		}

		@Override
		public Iterator<BlockDesc> iterator() {
			return new SerfIterator(store, rs, parent);
		}
	}

	class SerfIterator implements Iterator<BlockDesc> {
		CBlockStore store;
		ResultSet rs;
		boolean checked, hasnext;
		ControllerBlock parent;

		public SerfIterator(CBlockStore store, ResultSet rs, ControllerBlock p) {
			this.store = store;
			this.rs = rs;
			checked = hasnext = false;
			parent = p;
		}

		@Override
		public boolean hasNext() {
			if (!checked) {
				try {
					hasnext = rs.next();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				checked = true;
			}
			return hasnext;
		}

		@Override
		public BlockDesc next() {
			while(true) {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				try {
					long id = rs.getLong("id");
					String ws = rs.getString("world");
					int x = rs.getInt("x");
					int y = rs.getInt("y");
					int z = rs.getInt("z");
					int mid = rs.getInt("material");
					byte data = rs.getByte("meta");
					World world = parent.getServer().getWorld(ws);
					if (world == null) {
						parent.getLogger().severe(String.format("Unable to load serf block %d: World %s not found.", id, ws));
						continue;
					}
					Location loc = new Location(world, x, y, z);
					Material mat = Material.getMaterial(mid);
					BlockDesc cb = new BlockDesc(id, loc, mat, data);
					return cb;
				} catch (SQLException e) {
					parent.getLogger().throwing(this.getClass().getCanonicalName(), "next", e);
					continue;
				} finally {
					checked = false;
				}
			}
		}

		@Override
		public void remove() {
			try {
				rs.deleteRow();
			} catch (SQLException e) {
				parent.getLogger().throwing(this.getClass().getCanonicalName(), "next", e);
				throw new IllegalStateException();
			}
		}
	}

	public boolean removeLord(Location loc) {
		return false;
	}

	public void removeLord(long id) {
		try {
			PreparedStatement stmt = conn.prepareStatement(
					"DELETE FROM ControllerBlock_Lord WHERE id = ?;");
			stmt.setLong(1, id);
			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
