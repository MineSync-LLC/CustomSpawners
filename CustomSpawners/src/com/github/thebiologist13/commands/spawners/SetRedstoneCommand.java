package com.github.thebiologist13.commands.spawners;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.thebiologist13.CustomSpawners;
import com.github.thebiologist13.Spawner;
import com.github.thebiologist13.commands.SpawnerCommand;

public class SetRedstoneCommand extends SpawnerCommand {

	private CustomSpawners plugin;
	
	public SetRedstoneCommand(CustomSpawners plugin) {
		this.plugin = plugin;
	}
	
	public void run(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		//Player
		Player p = null;
		
		//Value to set
		boolean redstone = false;
		
		//Spawner
		Spawner s = null;
		
		if(!(arg0 instanceof Player)) {
			plugin.log.info(NO_CONSOLE);
			return;
		}
		
		p = (Player) arg0;
		
		if(p.hasPermission("customspawners.spawners.setredstone")) {
			//Set redstone powered of selection
			if(CustomSpawners.spawnerSelection.containsKey(p) && arg3.length == 2) {
				
				if(arg3[1].equalsIgnoreCase("true") || arg3[1].equalsIgnoreCase("false")) {
					if(arg3[1].equals("true")) {
						redstone = true;
					}
					//Doesn't need one for false, already set above
				} else {
					p.sendMessage(MUST_BE_BOOLEAN);
					return;
				}
				
				s = plugin.getSpawnerById(CustomSpawners.spawnerSelection.get(p));
				
			//Arguments entered for selection, but there is none
			} else if(arg3.length == 2) {
				
				p.sendMessage(NEEDS_SELECTION);
				return;
				
			//Set redstone powered of specific spawner
			} else if(arg3.length == 3) {
				
				if(arg3[2].equalsIgnoreCase("true") || arg3[2].equalsIgnoreCase("false")) {
					if(arg3[2].equals("true")) {
						redstone = true;
					}
					//Doesn't need one for false, already set above
				} else {
					p.sendMessage(MUST_BE_BOOLEAN);
					return;
				}
				
				if(!plugin.isInteger(arg3[1])) {
					p.sendMessage(ID_NOT_NUMBER);
					return;
				}
				
				if(!plugin.isValidSpawner(Integer.parseInt(arg3[1]))) {
					p.sendMessage(NO_ID);
					return;
				}
				
				s = plugin.getSpawnerById(Integer.parseInt(arg3[1]));
				
			//General error
			} else {
				
				p.sendMessage(GENERAL_ERROR);
				return;
				
			}
			
			//Set the value
			s.setRedstoneTriggered(redstone);
			
			//Success message
			p.sendMessage(ChatColor.GREEN + "Successfully set the redstone triggered value of spawner with ID " + 
					ChatColor.GOLD + String.valueOf(s.getId()) + ChatColor.GREEN + " to " + ChatColor.GOLD +
					String.valueOf(redstone) + ChatColor.GREEN + "!");
		} else {
			p.sendMessage(NO_PERMISSION);
			return;
		}
	}
}