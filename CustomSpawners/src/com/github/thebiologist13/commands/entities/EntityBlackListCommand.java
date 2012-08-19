package com.github.thebiologist13.commands.entities;

import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.github.thebiologist13.CustomSpawners;
import com.github.thebiologist13.SpawnableEntity;
import com.github.thebiologist13.commands.SpawnerCommand;

public class EntityBlackListCommand extends SpawnerCommand {

	public EntityBlackListCommand(CustomSpawners plugin) {
		super(plugin);
	}

	@Override
	public void run(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		//Player
		Player p = null;
		//SpawnableEntity
		SpawnableEntity s = null;
		//Damage type
		String type = "";
		//Perms
		String blacklistPerm = "customspawners.entities.blacklist";
		
		if(!(arg0 instanceof Player)) {
			log.info(NO_CONSOLE);
			return;
		}
		
		p = (Player) arg0;
		
		if(p.hasPermission(blacklistPerm) && arg3[0].equalsIgnoreCase("addblacklistitem")) {
			
			if(CustomSpawners.entitySelection.containsKey(p) && arg3.length == 2) {

				s = plugin.getEntity(CustomSpawners.entitySelection.get(p).toString());
				
				String value = arg3[1];
				
				type = parseCause(value);
				
				if(type.equals("")) {
					p.sendMessage(INVALID_CAUSE);
					return;
				}
				
			} else if(arg3.length == 2) {
				p.sendMessage(NEEDS_SELECTION);
				return;
			} else if(arg3.length == 3) {

				s = plugin.getEntity(arg3[1]);

				if(s == null) {
					p.sendMessage(NO_ID);
					return;
				}
				
				String value = arg3[2];
				
				type = parseCause(value);
				
				if(type.equals("")) {
					p.sendMessage(INVALID_CAUSE);
					return;
				}
				
			} else {
				p.sendMessage(GENERAL_ERROR);
				return;
			}
			
			//Set
			s.addDamageBlacklist(type);
			
			//Success
			p.sendMessage(ChatColor.GREEN + "Added damage type " + ChatColor.GOLD + type + ChatColor.GREEN + " to entity " +
					ChatColor.GOLD + plugin.getFriendlyName(s) + ChatColor.GREEN + "\'s damage blacklist!");
			
		} else if(p.hasPermission(blacklistPerm) && arg3[0].equalsIgnoreCase("clearblacklist")) {
			
			if(CustomSpawners.entitySelection.containsKey(p) && arg3.length == 1) {

				s = plugin.getEntity(CustomSpawners.entitySelection.get(p).toString());
				
			} else if(arg3.length == 1) {
				p.sendMessage(NEEDS_SELECTION);
				return;
			} else if(arg3.length == 2) {

				s = plugin.getEntity(arg3[1]);

				if(s == null) {
					p.sendMessage(NO_ID);
					return;
				}
				
			} else {
				p.sendMessage(GENERAL_ERROR);
				return;
			}
			
			//Set
			s.setDamageBlacklist(new ArrayList<String>());
			
			//Success
			p.sendMessage(ChatColor.GREEN + "Cleared damage blacklist for entity " +
					ChatColor.GOLD + plugin.getFriendlyName(s) + ChatColor.GREEN + "!");
			
		} else if(p.hasPermission(blacklistPerm) && arg3[0].equalsIgnoreCase("setblacklist")) {
			
			boolean useBlacklist = false;
			
			if(CustomSpawners.entitySelection.containsKey(p) && arg3.length == 2) {

				s = plugin.getEntity(CustomSpawners.entitySelection.get(p).toString());
				
				String value = arg3[1];
				
				if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
					if(value.equals("true")) {
						useBlacklist = true;
					}
				} else {
					p.sendMessage(MUST_BE_BOOLEAN);
					return;
				}
				
			} else if(arg3.length == 2) {
				p.sendMessage(NEEDS_SELECTION);
				return;
			} else if(arg3.length == 3) {

				s = plugin.getEntity(arg3[1]);

				if(s == null) {
					p.sendMessage(NO_ID);
					return;
				}
				
				String value = arg3[2];
				
				if(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
					if(value.equals("true")) {
						useBlacklist = true;
					}
				} else {
					p.sendMessage(MUST_BE_BOOLEAN);
					return;
				}
				
			} else {
				p.sendMessage(GENERAL_ERROR);
				return;
			}
			
			//Set
			s.setUseBlacklist(useBlacklist);
			
			//Success
			p.sendMessage(ChatColor.GREEN + "Set entity " + ChatColor.GOLD + plugin.getFriendlyName(s) + ChatColor.GREEN + 
					"\'s useBlacklist value to " + ChatColor.GOLD + String.valueOf(useBlacklist) + ChatColor.GREEN + "!");
			
		} else {
			p.sendMessage(NO_PERMISSION);
		}
		
	}

	private String parseCause(String value) {
		
		String type = "";
		value = value.toUpperCase();
		
		if(value.equalsIgnoreCase("blockexplosion")) {
			type = "BLOCK_EXPLOSION";
		} else if(value.equalsIgnoreCase("entityexplosion") || value.equalsIgnoreCase("creeper")) {
			type = "ENTITY_EXPLOSION";
		} else if(value.equalsIgnoreCase("firetick") || value.equalsIgnoreCase("burning")) {
			type = "FIRE_TICK";
		} else if(value.equalsIgnoreCase("attack") || value.equalsIgnoreCase("entityattack")) {
			type = "ENTITY_ATTACK";
		} else if(value.equalsIgnoreCase("item") || value.equalsIgnoreCase("itemdamage") || value.equalsIgnoreCase("item_damage")) {
			type = "ITEM";
		} else if(value.equalsIgnoreCase("spawnerfire") || value.equalsIgnoreCase("spawner_fire") 
				|| value.equalsIgnoreCase("spawnerfireticks")|| value.equalsIgnoreCase("spawner_fire_ticks")) {
			type = "SPAWNER_FIRE_TICKS";
		} else {
			DamageCause[] causes = DamageCause.values();
			boolean match = false;
			
			for(DamageCause c : causes) {
				if(c.name().equalsIgnoreCase(value)) {
					match = true;
					type = value;
					break;
				}
			}
			
			if(!match) {
				return "";
			}
		}
		
		return type;
		
	}
 	
}
