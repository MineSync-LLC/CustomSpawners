package com.github.thebiologist13.commands.entities;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Villager.Profession;

import com.github.thebiologist13.CustomSpawners;
import com.github.thebiologist13.SpawnableEntity;

public class EntityProfessionCommand extends EntityCommand {

	public EntityProfessionCommand(CustomSpawners plugin) {
		super(plugin);
	}

	public EntityProfessionCommand(CustomSpawners plugin, String mainPerm) {
		super(plugin, mainPerm);
	}

	@Override
	public void run(SpawnableEntity entity, CommandSender sender, String subCommand, String[] args) {
		
		String in = getValue(args, 0, "farmer");
		
		final String INVALID = ChatColor.RED + in + " is not a valid villager profession.";
		
		Profession prof = null;
		
		try {
			prof = Profession.valueOf(in.toUpperCase());
		} catch(Exception e) {
			PLUGIN.sendMessage(sender, INVALID);
			return;
		}
		
		if(prof == null) {
			PLUGIN.sendMessage(sender, INVALID);
			return;
		}
		
		entity.setProfession(prof);
		
		PLUGIN.sendMessage(sender, getSuccessMessage(entity, "profession", in));
		
	}

}
