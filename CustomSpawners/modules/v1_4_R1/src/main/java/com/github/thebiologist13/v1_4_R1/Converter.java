package com.github.thebiologist13.v1_4_R1;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.server.v1_4_R1.NBTTagCompound;
import net.minecraft.server.v1_4_R1.NBTTagDouble;
import net.minecraft.server.v1_4_R1.NBTTagList;
import net.minecraft.server.v1_4_R1.TileEntity;
import net.minecraft.server.v1_4_R1.TileEntityMobSpawner;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_4_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.github.thebiologist13.api.IConverter;
import com.github.thebiologist13.api.ISpawnableEntity;
import com.github.thebiologist13.api.ISpawner;

public class Converter implements IConverter { //Might need an ISpawner interface?

	@Override
	public void convert(ISpawner spawner) {
		Block block = spawner.getBlock();

		if (spawner.isConverted()) {
			block.setTypeIdAndData(spawner.getBlockId(),
					spawner.getBlockData(), false);
		} else {
			if (!isTileEntity(block)) {
				throw new IllegalArgumentException(
						"Parameter block is not a TileEntity.");
			}

			spawner.setActive(false);

			NBTTagCompound compound = getSpawnerNBT(spawner);

			if (compound == null || compound.isEmpty()) {
				return;
			}

			setMobSpawnerNBT(block, compound);

		}

		spawner.setConverted(!spawner.isConverted());

	}

	public <T extends Entity> NBTTagCompound getEntityNBT(T entity) {
		NBTTagCompound compound = new NBTTagCompound();

		Class<? extends Object> clazz = entity.getClass();
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if ((method.getName() == "d")
					&& (method.getParameterTypes().length == 1)
					&& (method.getParameterTypes()[0] == NBTTagCompound.class)) {
				try {
					method.setAccessible(true);
					method.invoke(entity, compound);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return compound;
	}

	public NBTTagCompound getSpawnerNBT(ISpawner s) {
		NBTTagCompound sData = new NBTTagCompound(); // Spawner NBT
		NBTTagCompound eData = new NBTTagCompound(); // Entity NBT
		ISpawnableEntity mainEntity = s.getMainEntity(); // The primary entity of the spawner.

		Location spawnLocation = null;

		if (s.isUsingSpawnArea()) {
			spawnLocation = s.getAreaPoints()[0]; 
			// TODO This can be changed. Really just needs a single point to spawn to. Should add option to disable.
		}

		if (s.getTypeData().size() == 1) {
			Location pos = (spawnLocation == null) ? s.getLoc() : spawnLocation;
			Entity e = s.forceSpawnOnLoc(mainEntity, pos);
			eData = getEntityNBT(e);

			if (eData.isEmpty()) { // If empty
				return null;
			}

			EntityType type = e.getType();
			String id = type.getName();

			if (id == null || id.isEmpty()) {
				switch (type) {
				case SPLASH_POTION:
					id = "ThrownPotion";
					break;
				case EGG:
					id = "Egg";
					break;
				default:
					return null;
				}
			}

			eData.setString("id", id);

			if (eData.hasKey("Pos") && spawnLocation == null) {
				eData.remove("Pos");
			}

			eData.set(
					"Motion",
					makeDoubleList(new double[] { mainEntity.getXVelocity(),
							mainEntity.getYVelocity(),
							mainEntity.getZVelocity() }));

			sData.set("SpawnData", eData);
			e.remove();
		} else {
			List<ISpawnableEntity> typeData = s.getTypesEntities();
			NBTTagCompound[] potentials = new NBTTagCompound[typeData.size()];

			for (int i = 0; i < typeData.size(); i++) {
				NBTTagCompound potentialData = new NBTTagCompound();
				ISpawnableEntity se = s.getTypesEntities().get(i);
				Location pos = (spawnLocation == null) ? s.getLoc()
						: spawnLocation;
				Entity e = s.forceSpawnOnLoc(se, pos);
				NBTTagCompound eData2 = getEntityNBT(e);
				if (eData2.isEmpty()) { // If empty
					return null;
				}

				EntityType type = e.getType();
				String id = type.getName();

				if (id == null || id.isEmpty()) {
					switch (type) {
					case SPLASH_POTION:
						id = "ThrownPotion";
						break;
					case EGG:
						id = "Egg";
						break;
					default:
						return null;
					}
				}

				eData2.setString("id", id);

				if (eData2.hasKey("Pos") && spawnLocation == null) {
					eData2.remove("Pos");
				}

				eData2.set(
						"Motion",
						makeDoubleList(new double[] { se.getXVelocity(),
								se.getYVelocity(), se.getZVelocity() }));

				if (i == 0) {
					eData = eData2;
					sData.set("SpawnData", eData2);
				}

				potentialData.setCompound("Properties", eData2);
				potentialData.setInt("Weight", i + 1);
				potentialData.setString("Type", eData2.getString("id"));
				potentials[i] = potentialData;

				e.remove();
			}

			sData.set("SpawnPotentials", makeCompoundList(potentials));
		}

		// Spawner Data

		sData.setString("id", "MobSpawner");
		sData.setInt("x", s.getLoc().getBlockX());
		sData.setInt("y", s.getLoc().getBlockY());
		sData.setInt("z", s.getLoc().getBlockZ());
		sData.setString("EntityId", eData.getString("id"));
		sData.setShort("SpawnCount", (short) s.getMobsPerSpawn());
		sData.setShort("SpawnRange", (short) s.getRadius());
		sData.setShort("Delay", (short) s.getRate());
		sData.setShort("MinSpawnDelay", (short) s.getRate());
		sData.setShort("MaxSpawnDelay", (short) (s.getRate() + 1));
		sData.setShort("MaxNearbyEntities", (short) s.getMaxMobs());
		sData.setShort("RequiredPlayerRange", (short) s.getMaxPlayerDistance());

		return sData;
	}

	public boolean isTileEntity(Block b) {
		CraftWorld w = (CraftWorld) b.getWorld();
		TileEntity e = w.getTileEntityAt(b.getX(), b.getY(), b.getZ());

		if (e != null)
			return true;

		return false;
	}

	public void setEntityNBT(Entity e, NBTTagCompound n) {
		net.minecraft.server.v1_4_R1.Entity nms = ((CraftEntity) e).getHandle();
		Class<?> entityClass = nms.getClass();
		Method[] methods = entityClass.getMethods();
		for (Method method : methods) {
			if ((method.getName() == "a")
					&& (method.getParameterTypes().length == 1)
					&& (method.getParameterTypes()[0] == NBTTagCompound.class)) {
				try {
					method.setAccessible(true);
					method.invoke(nms, n);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

	}

	private NBTTagList makeCompoundList(NBTTagCompound[] m0) {
		NBTTagList list = new NBTTagList();
		int i = m0.length;

		for (int j = 0; j < i; j++) {
			list.add(m0[j]);
		}

		return list;

	}
	
	private NBTTagList makeDoubleList(double[] d0) {
		NBTTagList list = new NBTTagList();
		int i = d0.length;

		for (int j = 0; j < i; j++) {
			double d = d0[j];

			list.add(new NBTTagDouble((String) null, d));
		}

		return list;

	}

	private void setMobSpawnerNBT(Block b, NBTTagCompound n) {
		if (!isTileEntity(b)) {
			throw new IllegalArgumentException(
					"Parameter block is not a TileEntity.");
		}

		CraftWorld cw = (CraftWorld) b.getWorld();
		TileEntityMobSpawner te = (TileEntityMobSpawner) cw.getTileEntityAt(
				b.getX(), b.getY(), b.getZ());

		te.a(n);
	}

}
