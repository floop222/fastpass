package fastpass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.text.View;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

@SuppressWarnings("unused")
public class Fastpass extends JavaPlugin implements Listener{

	public static Economy econ = null;
	
	ArrayList <Player> cooldown = new ArrayList <Player>();
	ArrayList <Player> cooldown2 = new ArrayList <Player>();

	public void onEnable(){
		Bukkit.getPluginManager().registerEvents(this, this);
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		if (!setupEconomy()) {
			getLogger()
					.severe(String
							.format("[%s] - Disabled due to no Vault dependency found!",
									getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
	}

		private boolean setupEconomy() {
			if (getServer().getPluginManager().getPlugin("Vault") == null) {
				return false;
			}
			RegisteredServiceProvider<Economy> rsp = getServer()
					.getServicesManager().getRegistration(Economy.class);
			if (rsp == null) {
				return false;
			}
			econ = rsp.getProvider();
			return econ != null;
		}
		
	public void onDisable(){}

	@EventHandler
	public void onCreate(SignChangeEvent e){
		Player p = e.getPlayer();
		String[] lines = e.getLines();
		if(p.hasPermission("fastpass.create")){
		 if(lines[0].equalsIgnoreCase("[fp]") && lines[1].equalsIgnoreCase("machine") && !lines[2].isEmpty() && !lines[3].isEmpty()){
				e.setLine(0, ChatColor.AQUA + "[fp]");
				e.setLine(1, ChatColor.BLUE + "machine");
				e.setLine(2, e.getLine(2));
				e.setLine(3, e.getLine(3));
			}else if(lines[0].equalsIgnoreCase("[fp]") && lines[1].equalsIgnoreCase("return") && !lines[2].isEmpty() && !lines[3].isEmpty()){
				e.setLine(0, ChatColor.AQUA + "[fp]");
				e.setLine(1, ChatColor.BLUE + "return");
				e.setLine(2, e.getLine(2));
				e.setLine(3, e.getLine(3));
			}else if (lines[0].equalsIgnoreCase("[fp]") || lines[0].equalsIgnoreCase("[fp]") && lines[1].equalsIgnoreCase("return") || lines[0].equalsIgnoreCase("[fp]") && lines[1].equalsIgnoreCase("machine")){
				p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.invalidSign"));
			}
		}else{
			p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.noCreatePermission"));
		}
	}

	@EventHandler
	public void onSignClick(final PlayerInteractEvent e){

		
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
			if(e.getClickedBlock().getState() instanceof Sign){
				 Sign s = (Sign) e.getClickedBlock().getState();
					final Player p = (Player) e.getPlayer();
					if(p.hasPermission("fastpass.use")){
					ItemStack special = new ItemStack(Material.PAPER, 1);
					ItemMeta name = special.getItemMeta();
					name.setDisplayName(ChatColor.GRAY + getConfig().getString("ticketName"));
					name.setLore(Arrays.asList(s.getLine(2)));
					special.setItemMeta(name);
					if(s.getLine(0).equalsIgnoreCase(ChatColor.AQUA + "[fp]")){
						if(s.getLine(1).equalsIgnoreCase(ChatColor.BLUE + "machine")){
						int sign = Integer.parseInt(s.getLine(3));
							if(econ.getBalance(p.getName()) < sign){
							p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.noMoney"));
							}else{
								if(p.getInventory().contains(special)){
								p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.hasTicket"));
								}else{
									if(cooldown.contains(p)){
										p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.hasCooldown"));
										return;
									}
									cooldown.add(p);
									Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
										public void run(){
											cooldown.remove(p);
										}
									}, getConfig().getInt("machineCooldown")*20);
								String value1 = s.getLine(3);
								int value = Integer.parseInt(value1);
								p.getInventory().addItem(special);
								econ.withdrawPlayer(p.getName(), value);
								p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.BLUE + value + " " + getConfig().getString("messages.getMoney"));
								p.updateInventory();
									}}}else if(s.getLine(1).equalsIgnoreCase(ChatColor.BLUE + "return")){
										if (p.getInventory().getItemInHand().hasItemMeta()
										&& p.getInventory().getItemInHand().getType().equals(Material.PAPER)
										&& p.getInventory().getItemInHand().getItemMeta().hasLore()
										&& p.getInventory().getItemInHand().getItemMeta().hasDisplayName()
										&& p.getInventory().getItemInHand().getItemMeta().getLore().contains(s.getLine(2))
										&& p.getInventory().getItemInHand().getItemMeta().getDisplayName().contains(getConfig().getString("ticketName"))) {


									if(cooldown2.contains(p)){
										p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.hasCooldown"));
									}
									cooldown2.add(p);
									Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
										public void run(){
											cooldown2.remove(p);
										}
									}, getConfig().getInt("returnCooldown")*20);
								    try {
								    	float pitch = e.getPlayer().getLocation().getPitch();
								    	float yaw = e.getPlayer().getLocation().getYaw();
										World world = p.getWorld();
										System.out.println(yaw);
										System.out.println(pitch);
										String[] signInformation = s.getLine(3).split(",");
										int x = Integer.parseInt(signInformation[0]); 
										int y = Integer.parseInt(signInformation[1]); 
										int z = Integer.parseInt(signInformation[2]); 
										p.teleport(new Location(world, x, y, z, yaw, pitch));
										p.getInventory().remove(p.getItemInHand());
										p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.BLUE + getConfig().getString("messages.rideText"));
								    } catch (NumberFormatException ex) {
										p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + "Invalid coordinates!");
								    } catch (ArrayIndexOutOfBoundsException ex){
										p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + "Invalid coordinates!");
								    }
								    
								p.updateInventory();
								}else{
									p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.wrongTicket"));
								}
						}
				}
			}else{
				p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("messages.noUsePermission"));
			}
			}
		}
	}
	

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		Player p = (Player) sender;

		if (label.equalsIgnoreCase("fp")) {
			if (p.hasPermission("fastpass.create")) {
				if (args.length == 0) {
					p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.BLUE + "Try /fp ? or /fp help");
				} else {

					switch (args[0]) {

					case "?": case "help":
						p.sendMessage(ChatColor.BLUE + "" + ChatColor.BOLD + ""+ ChatColor.ITALIC+ "            > Fastpass Help Menu >");
						p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.BLUE + "/fp machine");
						p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.BLUE + "/fp return");
						break;
						
					case "machine":
						p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + ""+ ChatColor.ITALIC+ "            > Fastpass Machine Menu >");
						p.sendMessage(ChatColor.BLUE + "Line 1: [fp]");
						p.sendMessage(ChatColor.BLUE + "Line 2: machine");
						p.sendMessage(ChatColor.BLUE + "Line 3: a name that you want");
						p.sendMessage(ChatColor.BLUE + "Line 4: the price for the ticket");
						break;
						
					case "return":
						p.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + ""+ ChatColor.ITALIC+ "            > Fastpass Return Menu >");
						p.sendMessage(ChatColor.BLUE + "Line 1: [fp]");
						p.sendMessage(ChatColor.BLUE + "Line 2: return");
						p.sendMessage(ChatColor.BLUE + "Line 3: the name of the ticket that it can receive");
						p.sendMessage(ChatColor.BLUE + "Line 4: coordinates");
						p.sendMessage(ChatColor.BLUE + "coordinates example: -90,64,10");
						break;
					}}}}else{
						p.sendMessage(ChatColor.AQUA + "[fp] " + ChatColor.RED + getConfig().getString("message.noCommandPermission"));
					}
		return false;}
	
}