package com.github.thebiologist13;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.v1_4_R1.AxisAlignedBB;
import net.minecraft.server.v1_4_R1.EntityEnderPearl;
import net.minecraft.server.v1_4_R1.EntityLiving;
import net.minecraft.server.v1_4_R1.EntityPotion;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_4_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_4_R1.entity.CraftLivingEntity;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Golem;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Spider;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import com.github.thebiologist13.listeners.DamageController;
import com.github.thebiologist13.serialization.SBlock;
import com.github.thebiologist13.serialization.SInventory;
import com.github.thebiologist13.serialization.SLocation;
import com.github.thebiologist13.serialization.SPotionEffect;

/**
 * Represents a spawner managed by CustomSpawners.
 * 
 * @author thebiologist13
 */
public class Spawner implements Serializable {
	
	private static final long serialVersionUID = -153105685313343476L;
	//Main Data
	private Map<String, Object> data = new HashMap<String, Object>();
	//Integer is mob ID. This holds the entities that have been spawned so when one dies, it can be removed from maxMobs.
	private ConcurrentHashMap<Integer, SpawnableEntity> mobs = new ConcurrentHashMap<Integer, SpawnableEntity>();
	//If the block was powered before
	private boolean poweredBefore = false;
	//Secondary Mobs (passenger, projectiles, etc.). Key is mobId, value is associated mob from "mobs".
	private ConcurrentHashMap<Integer, Integer> secondaryMobs = new ConcurrentHashMap<Integer, Integer>();
	//Additional data
	private Map<String, Object> tag = new HashMap<String, Object>();

	//Ticks left before next spawn
	private int ticksLeft = -1; 
	//Spawnable Mobs
	private List<Integer> typeData = new ArrayList<Integer>();
	public Spawner(SpawnableEntity type, Location loc, int id) {
		this(type, loc, "", id);
	}
	
	public Spawner(SpawnableEntity type, Location loc, String name, int id) {
		SLocation[] areaPoints = new SLocation[2];
		
		areaPoints[0] = new SLocation(loc);
		areaPoints[1] = new SLocation(loc);
		
		data.put("id", id);
		data.put("loc", new SLocation(loc));
		data.put("block", new SBlock(loc.getBlock()));
		data.put("areaPoints", areaPoints);
		typeData.add(type.getId());
		data.put("converted", false);
		data.put("useSpawnArea", false);
		data.put("name", name);
	}
	
	public void addMob(int mobId, SpawnableEntity entity) {
		mobs.put(mobId, entity);
	}
	
	public void addSecondaryMob(int mobId, int boundTo) {
		secondaryMobs.put(mobId, boundTo);
	}

	public void addTypeData(SpawnableEntity data) {
		typeData.add(data.getId());
	}
	
	public void changeAreaPoint(int index, Location value) {
		if(index != 0 || index != 1) {
			return;
		}
		
		SLocation[] ap1 = (SLocation[]) this.data.get("areaPoints");
		ap1[index] = new SLocation(value);
		
		this.data.put("areaPoints", ap1);
	}

	//Spawn the mobs
	public void forceSpawn() {

		SpawnableEntity spawnType = randType();
		mainSpawn(spawnType);
		
	}
	
	//Spawn a mob on the spawner.
	public Entity forceSpawnOnLoc(SpawnableEntity entity, Location loc) {
		
		Entity e = spawnTheEntity(entity, loc);
		assignMobProps(e, entity);
		return e;
		
	}
	
	//Spawn the mobs
	public void forceSpawnType(SpawnableEntity entity) {

		mainSpawn(entity);
		
	}
	
	public Location[] getAreaPoints() {
		
		Location[] ap1 = new Location[2];
		SLocation[] ap = ((SLocation[]) this.data.get("areaPoints"));
		
		for(int i = 0; i < ap.length; i++) {
			ap1[i] = ap[i].toLocation();
		}
		
		return ap1;
	}
	
	public Block getBlock() {
		return ((SBlock) this.data.get("block")).toBlock();
	}

	public byte getBlockData() {
		return ((SBlock) this.data.get("block")).getData();
	}
	
	public int getBlockId() {
		return ((SBlock) this.data.get("block")).getId();
	}
	
	public Map<String, Object> getData() {
		return data;
	}
	
	public int getId() {
		return (Integer) this.data.get("id");
	}
	
	public Location getLoc() {
		return ((SLocation) this.data.get("loc")).toLocation();
	}
	
	public SpawnableEntity getMainEntity() {
		return CustomSpawners.getEntity(this.typeData.get(0).toString());
	}

	public byte getMaxLightLevel() {
		return (this.data.containsKey("maxLight")) ? (Byte) this.data.get("maxLight") : 0;
	}

	public int getMaxMobs() {
		return (this.data.containsKey("maxMobs")) ? (Integer) this.data.get("maxMobs") : 0;
	}

	public double getMaxPlayerDistance() {
		return (this.data.containsKey("maxDistance")) ? Double.parseDouble(this.data.get("maxDistance").toString()) : 0;
	}
	
	public byte getMinLightLevel() {
		return (this.data.containsKey("minLight")) ? (Byte) this.data.get("minLight") : 0;
	}
	
	public double getMinPlayerDistance() {
		return (this.data.containsKey("minDistance")) ? Double.parseDouble(this.data.get("minDistance").toString()) : 0;
	}
	
	public Map<Integer, SpawnableEntity> getMobs() {
		return this.mobs;
	}
	
	public int getMobsPerSpawn() {
		return (this.data.containsKey("mobsPerSpawn")) ? (Integer) this.data.get("mobsPerSpawn") : 0;
	}

	public String getName() {
		return (this.data.containsKey("name")) ? (String) this.data.get("name") : "";
	}

	public Object getProp(String key) {
		return (this.data.containsKey(key)) ? this.data.get(key) : null;
	}

	public double getRadius() {
		return (this.data.containsKey("radius")) ? (Double) this.data.get("radius") : 0d;
	}

	public int getRate() {
		return (this.data.containsKey("rate")) ? (Integer) this.data.get("rate") : 60;
	}

	public Map<Integer, Integer> getSecondaryMobs() {
		return secondaryMobs;
	}

	public Map<String, Object> getTag() {
		return this.tag;
	}

	public List<Integer> getTypeData() {
		return this.typeData;
	}

	public boolean hasProp(String key) {
		return this.data.containsKey(key);
	}

	public boolean hasTag(String key) {
		return this.tag.containsKey(key);
	}

	public boolean isActive() {
		return (this.data.containsKey("active")) ? (Boolean) this.data.get("active") : false;
	}

	public boolean isConverted() {
		return (Boolean) this.data.get("converted");
	}

	public boolean isHidden() {
		return (this.data.containsKey("hidden")) ? (Boolean) this.data.get("hidden") : false;
	}

	public boolean isPoweredBefore() {
		return poweredBefore;
	}

	public boolean isRedstoneTriggered() {
		return (this.data.containsKey("redstone")) ? (Boolean) this.data.get("redstone") : false;
	}

	public boolean isSpawnOnRedstone() {
		return (this.data.containsKey("spawnOnRedstone")) ? (Boolean) this.data.get("spawnOnRedstone") : false;
	}

	public boolean isUsingSpawnArea() {
		return (this.data.containsKey("useSpawnArea")) ? (Boolean) this.data.get("useSpawnArea") : false;
	}

	public void putTag(String key, Object value) {
		this.tag.put(key, value);
	}

	//Remove the spawner
	public void remove() {
		/*
		 * If the ID is -1, it should be removed next tick by not 
		 * calling these functions and removing it's file.
		 * 
		 * Won't happen initially because when the spawner is initialized, it is given an ID
		 */
		data.put("id", -1);
		data.put("active", false);
	}
	
	public void removeMob(int mobId) {
		if(mobs.containsKey(mobId))
			mobs.remove(mobId);
	}
	
	public void removePrimaryOrSecondaryMob(int mobId) {
		removeMob(mobId);
		removeSecondaryMob(mobId);
	}
	
	public void removeSecondaryMob(int mobId) {
		if(secondaryMobs.containsKey(mobId))
			secondaryMobs.remove(mobId);
	}

	public void removeTag(String key) {
		this.tag.remove(key);
	}

	public void removeTypeData(SpawnableEntity type) {
		if(typeData.contains(type.getId())) {
			int index = typeData.indexOf(type.getId());
			typeData.remove(index);
		}
		
		if(typeData.size() == 0) {
			typeData.add(CustomSpawners.defaultEntity.getId());
		}
		
	}

	public void setActive(boolean active) {
		this.data.put("active", active);
	}

	public void setAreaPoints(Location[] areaPoints) {
		
		if(areaPoints.length != 2)
			return;
		
		SLocation[] ap1 = new SLocation[2];
		
		for(int i = 0; i < areaPoints.length; i++) {
			ap1[i] = new SLocation(areaPoints[i]);
		}
		
		this.data.put("areaPoints", ap1);
	}

	public void setBlock(Block block) {
		this.data.put("block", new SBlock(block));
	}

	public void setConverted(boolean converted) {
		this.data.put("converted", converted);
	}
	
	public void setData(Map<String, Object> data) {
		
		if(data == null)
			return;
		
		for(String s : data.keySet()) {
			
			if(s.equals("id") || s.equals("name"))
				continue;
			
			this.data.put(s, data.get(s));
		}
		
	}

	public void setHidden(boolean hidden) {
		this.data.put("hidden", hidden);
	}

	public void setLoc(Location loc) {
		this.data.put("loc", new SLocation(loc));
		setBlock(loc.getBlock());
	}

	public void setMaxLightLevel(byte maxLightLevel) {
		this.data.put("maxLight", maxLightLevel);
	}
	
	public void setMaxMobs(int maxMobs) {
		this.data.put("maxMobs", maxMobs);
	}
	
	public void setMaxPlayerDistance(double maxPlayerDistance) {
		this.data.put("maxDistance", maxPlayerDistance);
	}
	
	public void setMinLightLevel(byte minLightLevel) {
		this.data.put("minLight", minLightLevel);
	}
	
	public void setMinPlayerDistance(double minPlayerDistance) {
		this.data.put("minDistance", minPlayerDistance);
	}

	public void setMobs(Map<Integer, SpawnableEntity> mobParam) {
		this.mobs = (ConcurrentHashMap<Integer, SpawnableEntity>) mobParam;
	}

	public void setMobsPerSpawn(int mobsPerSpawn) {
		this.data.put("mobsPerSpawn", mobsPerSpawn);
	}
	
	public void setName(String name) {
		this.data.put("name", name);
	}
	
	public void setPoweredBefore(boolean poweredBefore) {
		this.poweredBefore = poweredBefore;
	}
	
	public void setProp(String key, Object value) {
		if(key == null || value == null) {
			return;
		}
		
		this.data.put(key, value);
	}
	
	public void setRadius(double radius) {
		this.data.put("radius", radius);
	}

	public void setRate(int rate) {
		this.data.put("rate", rate);
		this.ticksLeft = rate;
	}

	public void setRedstoneTriggered(boolean redstoneTriggered) {
		this.data.put("redstone", redstoneTriggered);
	}
	
	public void setSecondaryMobs(Map<Integer, Integer> secondaryMobs) {
		this.secondaryMobs = (ConcurrentHashMap<Integer, Integer>) secondaryMobs;
	}
	
	public void setSpawnOnRedstone(boolean value) {
		this.data.put("spawnOnRedstone", value);
	}
	
	public void setTag(Map<String, Object> tag) {
		this.tag = tag;
	}
	
	/*
	 * Methods for spawning, timing, etc.
	 */

	public void setTypeData(List<Integer> typeDataParam) {
		
		if(typeDataParam == null) {
			return;
		}
		
		this.typeData = typeDataParam;
	}
	
	public void setUseSpawnArea(boolean useSpawnArea) {
		this.data.put("useSpawnArea", useSpawnArea);
	}
	
	//Spawn the mobs
	public void spawn() {

		//If the spawner is not active, return
		if(!isActive()) {
			return;
		}

		boolean hasPower = getBlock().isBlockPowered() || getBlock().isBlockIndirectlyPowered();
		
		/*
		 * This block checks if the conditions are met to spawn mobs
		 */
		if(isRedstoneTriggered() && !hasPower) {
			return;
		} else if(!isPlayerNearby()) {
			return;
		} else if(mobs.size() > getMaxMobs()) {
			return;
		}
			
		SpawnableEntity spawnType = randType();
		mainSpawn(spawnType);
		
	}
	
	//Tick the spawn rate down and spawn mobs if it is time to spawn. Return the ticks left.
	public int tick() {
		if(!(getRate() <= 0)) {
			ticksLeft--;
			if(ticksLeft == 0) {
				ticksLeft = getRate();
				spawn();
				return 0;
			}
		}
		
		return ticksLeft;
	}
	
	private boolean areaEmpty(Location loc, boolean spawnInWater, float height, float width, float length) {
		
		
		height = (float) Math.floor(height) + 1.0f;
		width = (float) Math.floor(width) + 1.0f;
		length = (float) Math.floor(length) + 1.0f;
		
		for(int y = 0; y <= height; y++) {
			double testY = loc.getY() + y;
			
			for(int x = 0; x <= width; x++) {
				double testX = loc.getX() + x;
				
				for(int z = 0; z <= length; z++) {
					double testZ = loc.getZ() + z;
					Location testLoc = new Location(loc.getWorld(), testX, testY, testZ);
					
					if(!isEmpty(testLoc, spawnInWater))
						return false;
				}
				
			}
			
		}
		
		return true;
		
	}
	
	/*
	 * Methods for choosing locations, checking things, etc.
	 */
	
	//Assigns properties to a LivingEntity
	private void assignMobProps(Entity baseEntity, SpawnableEntity data) {
		
		baseEntity.setVelocity(data.getVelocity().clone());
		baseEntity.setFireTicks(data.getFireTicks());
		
		if(baseEntity instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) baseEntity;
			data.setMaxHealth(entity.getMaxHealth());
			data.setMaxAir(entity.getMaximumAir());
			setBasicProps(entity, data);
			
			if(entity instanceof Ageable) {
				Ageable a = (Ageable) entity;
				setAgeProps(a, data);
			}
			
			if(entity instanceof Animals) {
				Animals animal = (Animals) entity;
				
				//Setting animal specific properties
				if(animal instanceof Pig) {
					Pig p = (Pig) animal;
					p.setSaddle(data.isSaddled());
				} else if(animal instanceof Sheep) {
					Sheep s = (Sheep) animal;
					DyeColor color = DyeColor.valueOf(data.getColor());
					s.setColor(color);
				} else if(animal instanceof Wolf) {
					Wolf w = (Wolf) animal;
					w.setAngry(data.isAngry());
					w.setTamed(data.isTamed());
					if(data.isTamed()) {
						
						ArrayList<Player> nearPlayers = getNearbyPlayers(w.getLocation(), 16);
						int index = (int) Math.round(Math.rint(nearPlayers.size() - 1));
						if(nearPlayers != null) {
							w.setOwner(nearPlayers.get(index));
						}
						
						w.setSitting(data.isSitting());
						
					}
				} else if(animal instanceof Ocelot) {
					Ocelot o = (Ocelot) animal;
					o.setTamed(data.isTamed());
					if(data.isTamed()) {
						Ocelot.Type catType = Ocelot.Type.valueOf(data.getCatType());
						o.setCatType(catType);
						
						ArrayList<Player> nearPlayers = getNearbyPlayers(o.getLocation(), 16);
						int index = (int) Math.round(Math.rint(nearPlayers.size() - 1));
						if(nearPlayers != null) {
							o.setOwner(nearPlayers.get(index));
						}
						
						o.setSitting(data.isSitting());
						
					}
				}
			} else if(entity instanceof Villager) {
				Villager v = (Villager) entity;
				v.setAge(data.getAge());
				v.setProfession(data.getProfession());
			} else if(entity instanceof Monster) {
				Monster monster = (Monster) entity;
				
				//Setting monster specific properties.
				if(monster instanceof Enderman) {
					Enderman e = (Enderman) monster;
					e.setCarriedMaterial(data.getEndermanBlock());
				} else if(monster instanceof Creeper) {
					Creeper c = (Creeper) monster;
					c.setPowered(data.isCharged());
				} else if(monster instanceof PigZombie) {
					PigZombie p = (PigZombie) monster;
					if(data.isAngry()) {
						p.setAngry(true);
					}
				} else if(monster instanceof Spider) {
					Spider s = (Spider) monster;
					if(data.isJockey()) {
						makeJockey(s);
					}
				} else if(monster instanceof Zombie) {
					Zombie z = (Zombie) monster;
					boolean isVillager = false;
					if(data.hasProp("zombie"))
						isVillager = (Boolean) (data.getProp("zombie"));
					z.setBaby((data.getAge() < -1) ? true : false);
					z.setVillager(isVillager);
				} else if(monster instanceof Skeleton) {
					Skeleton sk = (Skeleton) monster;
					SkeletonType skType = SkeletonType.NORMAL;
					
					if(data.hasProp("wither"))
						skType = ((Boolean) (data.getProp("wither")) == true) ? SkeletonType.WITHER : SkeletonType.NORMAL;
					
					sk.setSkeletonType(skType);
					
				}
			} else if(entity instanceof Golem) {
				Golem golem = (Golem) entity;
				
				if(golem instanceof IronGolem) {
					IronGolem i = (IronGolem) golem;
					if(data.isAngry()) {
						ArrayList<Player> nearPlayers = getNearbyPlayers(i.getLocation(), 16);
						int index = (int) Math.round(Math.rint(nearPlayers.size() - 1));
						if(nearPlayers != null) {
							i.setPlayerCreated(false);
							i.damage(0, nearPlayers.get(index));
							i.setTarget(nearPlayers.get(index));
						}
					}
				}
				//Some are not classified as animals or monsters
			} else if(entity instanceof Slime) {
				Slime s = (Slime) entity;
				s.setSize(data.getSlimeSize());
			} else if(entity instanceof MagmaCube) {
				MagmaCube m = (MagmaCube) entity;
				m.setSize(data.getSlimeSize());
			}
			
		} else if(baseEntity instanceof Projectile) {
			Projectile pro = (Projectile) baseEntity;
			
			//Eventually add explosive arrows and such :D
			
			if(pro instanceof EnderPearl) {
				EnderPearl e = (EnderPearl) pro;
				ArrayList<Player> players = getNearbyPlayers(e.getLocation(), getMaxPlayerDistance() + 1);
				int index = (int) Math.round(randomGenRange(0, players.size()));
				e.setShooter(players.get(index));
			} else if(pro instanceof Fireball) {
				Fireball f = (Fireball) pro;
				setExplosiveProps(f, data);
				f.setVelocity(new Vector(0, 0, 0));
				f.setDirection(data.getVelocity());
			} else if(pro instanceof SmallFireball) {
				SmallFireball f = (SmallFireball) pro;
				setExplosiveProps(f, data);
				f.setVelocity(new Vector(0, 0, 0));
				f.setDirection(data.getVelocity());
			}
			
		} else if(baseEntity instanceof Explosive) {
			
			Explosive ex = (Explosive) baseEntity;
			
			if(ex instanceof TNTPrimed) {
				TNTPrimed tnt = (TNTPrimed) ex;
				setExplosiveProps(tnt, data);
				tnt.setFuseTicks(data.getFuseTicks());
			}
			
		}
		
	}
	
	private boolean getBlockBelowFromEntity(Entity e) {
		
		boolean reqsBlockBelow = true;
		
		switch(e.getType()) {
		case ENDER_DRAGON:
			reqsBlockBelow = false;
			break;
		case WITHER:
			reqsBlockBelow = false;
			break;
		case BLAZE:
			reqsBlockBelow = false;
			break;
		case BAT:
			reqsBlockBelow = false;
			break;
		default:
			reqsBlockBelow = true;
			if(e instanceof Flying)
				reqsBlockBelow = false;
			if(e instanceof WaterMob)
				reqsBlockBelow = false;
			if(!(e instanceof LivingEntity))
				reqsBlockBelow = false;
			break;
		}
		
		return reqsBlockBelow;
		
	}
	
	//Check if players are nearby
	private ArrayList<Player> getNearbyPlayers(Location source, double max) {
		ArrayList<Player> players = new ArrayList<Player>();
		for(Player p : Bukkit.getOnlinePlayers()) {
			//Finds distance between spawner and player is 3D space.
			double distance = p.getLocation().distance(getLoc());
			
			if(distance <= max) {
				players.add(p);
			}
		}
		return players;
	}
	
	//Determines a valid spawn location
	private Location getSpawningLocation(SpawnableEntity type, boolean reqsBlockBelow, float height, float width, float length) {
		//Random locations
		double randX = 0;
		double randY = 0;
		double randZ = 0;
		
		//Can it spawn in liquids?
		boolean spawnInWater = true;
		
		//Actual location
		Location spawnLoc = getLoc();
		
		//Amount of times tried
		int tries = 0;
		
		//As long as the mob has not been spawned, keep trying
		while(tries < 128) {

			tries++;
			Location loc = getLoc();
			Location[] areaPoints = getAreaPoints();
			
			//Getting a random location.
			if(!isUsingSpawnArea()) {
				//Generates a random location using randomGenRad(), a location for the block above, and a location for the block below.
				randX = randomGenRad()  + loc.getBlockX();
				randY = Math.round(randomGenRad()) + loc.getBlockY();
				randZ = randomGenRad() + loc.getBlockZ();

			} else {
				//Generates a random location in the spawn area using randomGenRange().
				randX = randomGenRange(areaPoints[0].getX(), areaPoints[1].getX());
				randY = randomGenRange(areaPoints[0].getY(), areaPoints[1].getY());
				randZ = randomGenRange(areaPoints[0].getZ(), areaPoints[1].getZ());
			}

			spawnLoc = new Location(loc.getWorld(), randX, randY + 1, randZ, randRot(), 0);
			
			//loc is the location of the spawner block
			//spawnLoc is the location being tested to spawn in
			if(!isEmpty(spawnLoc, spawnInWater)) {
				continue;
			}
			
			if(!areaEmpty(spawnLoc, spawnInWater, height, width, length))
				continue;
			
			//Block below
			if(reqsBlockBelow) {
				Location blockBelow = new Location(loc.getWorld(), randX, randY - 1, randZ);
				
				if(isEmpty(blockBelow, false)) {
					continue;
				}
			}
			
			//Light leveling
			if(!((getBlock().getLightLevel() <= getMaxLightLevel()) && (getBlock().getLightLevel() >= getMinLightLevel())))
				continue;
			
			if(!((spawnLoc.getBlock().getLightLevel() <= getMaxLightLevel()) && (spawnLoc.getBlock().getLightLevel() >= getMinLightLevel())))
				continue;
			
			return spawnLoc;
		}
		
		return null;
	}
	
	//Checks if a block is "empty"
	private boolean isEmpty(Location loc1, boolean liquidsNotSolid) {
		if(loc1.getBlock().isLiquid() && liquidsNotSolid) {
			return true;
		} else if(loc1.getBlock().isEmpty()){
			return true;
		} else {
			return false;
		}
		
	}
	
	//Check if players are nearby
	private boolean isPlayerNearby() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			
			World pWorld = p.getLocation().getWorld();
			World sWorld = getLoc().getWorld();
			
			if(!pWorld.equals(sWorld)) {
				continue;
			}
			
			//Finds distance between spawner and player in 3D space.
			double distance = p.getLocation().distance(getLoc());
			if(distance <= getMaxPlayerDistance() && distance >= getMinPlayerDistance()) {
				return true;
			}
		}
		return false;
	}
	
	private void mainSpawn(SpawnableEntity spawnType) {
		
		//Loop to spawn until the mobs per spawn is reached
		for(int i = 0; i < getMobsPerSpawn(); i++) {
			
			if((mobs.size() + 1) > getMaxMobs()) {
				return;
			}
			
			Location spLoc = getLoc();
			
			Entity e;
			
			if(spawnType.hasAllDimensions()) {
				Location spawnLocation = getSpawningLocation(spawnType, spawnType.requiresBlockBelow(), 
						spawnType.getHeight(), spawnType.getWidth(), spawnType.getLength());
				
				if(spawnLocation == null)
					continue;
				
				if(!spawnLocation.getChunk().isLoaded())
					continue;
				
				e = spawnTheEntity(spawnType, spawnLocation);

				net.minecraft.server.v1_4_R1.Entity nmEntity = ((CraftEntity) e).getHandle();
				
				AxisAlignedBB bb = nmEntity.boundingBox;
				
				spawnType.setHeight((float) (bb.d - bb.a));
				spawnType.setWidth((float) (bb.e - bb.b));
				spawnType.setLength((float) (bb.f - bb.c));
				spawnType.setBlockBelow(getBlockBelowFromEntity(e));
			} else {
				
				if(!spLoc.getChunk().isLoaded())
					continue;
				
				e = spawnTheEntity(spawnType, spLoc);
				
				net.minecraft.server.v1_4_R1.Entity nmEntity = ((CraftEntity) e).getHandle();
				
				spawnType.setHeight(nmEntity.height);
				spawnType.setWidth(nmEntity.width);
				spawnType.setLength(nmEntity.length);
				spawnType.setBlockBelow(getBlockBelowFromEntity(e));
				
				Location spawnLocation = getSpawningLocation(spawnType, getBlockBelowFromEntity(e), nmEntity.height, nmEntity.width, nmEntity.length);
				
				if(spawnLocation == null)
					continue;
				
				e.teleport(spawnLocation);
			}
			
			if(e != null) {
				
				assignMobProps(e, spawnType);
				
				mobs.put(e.getEntityId(), spawnType);
				
			}
			
		}
		
	}
	
	//Makes a spider jockey
	private Skeleton makeJockey(Spider spider) {
		Location spiderLoc = spider.getLocation();
		LivingEntity skele = (LivingEntity) spiderLoc.getWorld().spawn(spiderLoc, EntityType.SKELETON.getEntityClass());
		skele.getEquipment().setItemInHand(new ItemStack(Material.BOW));
		spider.setPassenger(skele);
		addSecondaryMob(skele.getEntityId(), spider.getEntityId());
		return (Skeleton) skele;
	}
	
	//Generate random double for location's parts within radius
	private double randomGenRad() {
		Random rand = new Random();
		if(rand.nextFloat() < 0.5) {
			return Math.floor((rand.nextDouble() * ((Double) data.get("radius"))) * -1) - 0.5;
		} else {
			return Math.floor((rand.nextDouble() * ((Double) data.get("radius"))) * 1) + 0.5;
		}
	}
	
	private double randomGenRange(double arg0, double arg1) {
		Random rand = new Random();
		double difference = Math.abs(arg0 - arg1);
		if(arg0 < arg1) {
			return Math.floor(arg0 + (difference * rand.nextDouble())) - 0.5;
		} else if(arg1 < arg0) {
			return Math.floor(arg1 + (difference * rand.nextDouble())) + 0.5;
		} else {
			return arg0;
		}
	}
	
	private float randRot() {
		Random rand = new Random();
		return (Math.round(rand.nextFloat() * 3)) * 90; //Snaps to 90 degree angles
	}
	
	private SpawnableEntity randType() {
		Random rand = new Random();
		int index = rand.nextInt(typeData.size());
		SpawnableEntity e = CustomSpawners.getEntity(typeData.get(index).toString());
		if(e == null) {
			return CustomSpawners.defaultEntity;
		}
		return e;
	}
	
	//Age properties
	private void setAgeProps(Ageable a, SpawnableEntity data) {
		if(data.getAge() == -2) {
			a.setBaby();
		} else if(data.getAge() == -1) {
			a.setAdult();
		} else {
			a.setAge(data.getAge());
		}
		
	}
	
	//Assigns some basic props
	private void setBasicProps(LivingEntity entity, SpawnableEntity data) {
		
		setInventory(entity, data.getInventory()); 
		
		for(SPotionEffect p : data.getEffects()) {
			PotionEffect ef = new PotionEffect(p.getType(), p.getDuration(), p.getAmplifier());
			entity.addPotionEffect(ef, true);
		}
		
		//Health handling
		if(data.getHealth() == -2) {
			entity.setHealth(1);
		} else if(data.getHealth() == -1) {
			entity.setHealth(entity.getMaxHealth());
		} else {
			if(data.getHealth() > entity.getMaxHealth()) {
				entity.setHealth(entity.getMaxHealth());
				DamageController.extraHealthEntities.put(entity.getEntityId(), data.getHealth() - entity.getMaxHealth());
			} else if(data.getHealth() < 0) {
				entity.setHealth(0);
			} else {
				entity.setHealth(data.getHealth());
			}
		}
		
		//Air handling
		if(data.getAir() == -2) {
			entity.setRemainingAir(0);
		} else if(data.getAir() == -1) {
			entity.setRemainingAir(entity.getMaximumAir());
		} else {
			entity.setRemainingAir(data.getAir());
		}
		
	}
	
	//Explosive Properties
	private void setExplosiveProps(Explosive e, SpawnableEntity data) {
		e.setYield(data.getYield());
		e.setIsIncendiary(data.isIncendiary());
	}
	
	//Inventory stuff
	private void setInventory(LivingEntity entity, SInventory data) {
		
		EntityEquipment ee = entity.getEquipment();
		
		if(ee == null) {
			return;
		}
		
		if(data.isEmpty()) {
			switch(entity.getType()) {
			case SKELETON:
				if(((Skeleton) entity).getSkeletonType().equals(SkeletonType.NORMAL)) {
					ee.setItemInHand(new ItemStack(Material.BOW));
				} else {
					ee.setItemInHand(new ItemStack(Material.STONE_SWORD));
				}
				break;
			case PIG_ZOMBIE:
				ee.setItemInHand(new ItemStack(Material.GOLD_SWORD));
				break;
			default:
				break;
			} 
			
		} else {
			ee.setItemInHand(data.getHand());
			ee.setArmorContents(data.getArmor());  
		}
		
	}
	
	//Spawns the actual entity
	private Entity spawnTheEntity(SpawnableEntity spawnType, Location spawnLocation) {
		if(spawnType.getType().equals(EntityType.DROPPED_ITEM)) {
			return getLoc().getWorld().dropItemNaturally(spawnLocation, spawnType.getItemType());
		} else if(spawnType.getType().equals(EntityType.FALLING_BLOCK)) {
			return getLoc().getWorld().spawnFallingBlock(spawnLocation, spawnType.getItemType().getType(), (byte) spawnType.getItemType().getDurability());
		} else if(spawnType.getType().equals(EntityType.SPLASH_POTION)) { //TODO Explicitly set NBT to make custom potions
			World world = getLoc().getWorld();
			SPotionEffect effect = spawnType.getPotionEffect();
			PotionType type = PotionType.getByEffect(effect.getType());
			Potion p = new Potion(type);
			try {
				if(effect.getDuration() >= 9600) {
					if(!type.isInstant()) {
						p.setHasExtendedDuration(true);
					}
				} else {
					if(effect.getAmplifier() >= 2) {
						p.setLevel(2);
					}
				}
			} catch(IllegalArgumentException e) {}
			p.setSplash(true);
			int data = p.toDamageValue();
			 
			net.minecraft.server.v1_4_R1.World nmsWorld = ((CraftWorld) world).getHandle();
			EntityPotion ent = new EntityPotion(nmsWorld, spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), new net.minecraft.server.v1_4_R1.ItemStack(373, 1, data));
			nmsWorld.addEntity(ent);
			return ent.getBukkitEntity();
		} else if(spawnType.getType().equals(EntityType.ENDER_PEARL)) {
			World world = getLoc().getWorld();
			EntityLiving nearPlayer = ((CraftLivingEntity) getNearbyPlayers(getLoc(), getMaxPlayerDistance() + 1).get(0)).getHandle();
			 
			net.minecraft.server.v1_4_R1.World nmsWorld = ((CraftWorld) world).getHandle();
			EntityEnderPearl ent = new EntityEnderPearl(nmsWorld, nearPlayer);
			ent.setLocation(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ(), 0, 0);
			nmsWorld.addEntity(ent);
			return ent.getBukkitEntity();
		} else if(spawnType.getType().equals(EntityType.LIGHTNING)) {
			return getLoc().getWorld().strikeLightningEffect(spawnLocation);
		} else {
			return getLoc().getWorld().spawn(spawnLocation, spawnType.getType().getEntityClass());
		}
		
	}
	
}
