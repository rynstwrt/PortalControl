package art.ryanstew.portalcontrol;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {

    String prefix = ChatColor.GREEN.toString() + ChatColor.BOLD + "[PortalControl]: " + ChatColor.RESET;
    FileConfiguration config = getConfig();

    @Override
    public void onEnable()
    {
        config.addDefault("worlds.exampleworld.netherportaldest", "world_nether");
        config.addDefault("worlds.exampleworld.endportaldest", "world_the_end");
        config.options().copyDefaults(true);
        saveConfig();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable()
    {
    }

    public void sendHelpMessage(CommandSender sender)
    {
        String message = ChatColor.GREEN.toString() + ChatColor.BOLD +
                "\nPortalControl Help:\n" +
                ChatColor.GOLD + "/portalcontrol help\n" +
                "/portalcontrol reload\n" +
                "/portalcontrol link <nether/end> <fromWorld> <toWorld>\n" +
                "/portalcontrol unlink <nether/end> <world>\n" +
                "/portalcontrol showlinks\n";
        sender.sendMessage(message);

        if (sender instanceof Player)
        {
            Player p = (Player) sender;
            Location l = p.getEyeLocation();
            p.playSound(l, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 5, 1);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
        {
            sendHelpMessage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help"))
        {
            sendHelpMessage(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            reloadConfig();
            config = getConfig();
            sender.sendMessage(prefix + ChatColor.GREEN.toString() + ChatColor.BOLD + "Successfully reloaded config file.");
            return true;
        }

        // link (command format: /portalcontrol link <nether/end> <fromWorld> <toWorld>)
        if (args[0].equalsIgnoreCase("link"))
        {
            if (args.length != 4)
            {
                sendHelpMessage(sender);
                return true;
            }

            String type = args[1];
            if (!type.equalsIgnoreCase("nether") && !type.equalsIgnoreCase("end"))
            {
                sendHelpMessage(sender);
                return true;
            }

            String fromWorldString = args[2];
            World fromWorld = getServer().getWorld(fromWorldString);
            if (fromWorld == null)
            {
                sendHelpMessage(sender);
                return true;
            }

            String toWorldString = args[3];
            World toWorld = getServer().getWorld(toWorldString);
            if (toWorld == null)
            {
                sendHelpMessage(sender);
                return true;
            }

            String configPath = (type.equalsIgnoreCase("nether")) ? ".netherportaldest" : ".endportaldest";
            config.set("worlds." + fromWorld.getName() + configPath, toWorld.getName());
            saveConfig();

            sender.sendMessage(prefix + ChatColor.GREEN.toString() + ChatColor.BOLD + "Successfully linked " + type.toLowerCase() + " portals from " + fromWorld.getName() + " to " + toWorld.getName() + ".");

            return true;
        }

        // unlink (command format: /portalcontrol unlink <nether/end> <fromWorld>
        if (args[0].equalsIgnoreCase("unlink"))
        {
            if (args.length != 3)
            {
                sendHelpMessage(sender);
                return true;
            }

            String type = args[1];
            if (!type.equalsIgnoreCase("nether") && !type.equalsIgnoreCase("end"))
            {
                sendHelpMessage(sender);
                return true;
            }

            String fromWorldString = args[2];
            World fromWorld = getServer().getWorld(fromWorldString);
            if (fromWorld == null)
            {
                sendHelpMessage(sender);
                return true;
            }

            String configPath = (type.equalsIgnoreCase("nether")) ? ".netherportaldest" : ".endportaldest";
            config.set("worlds." + fromWorld.getName() + configPath, null);
            saveConfig();

            sender.sendMessage(prefix + ChatColor.GREEN.toString() + ChatColor.BOLD + "Successfully unlinked " + type.toLowerCase() + " portals from " + fromWorld.getName() + ".");

            return true;
        }

        // show links
        if (args[0].equalsIgnoreCase("showlinks"))
        {
            ArrayList<String> worlds = new ArrayList<>(config.getConfigurationSection("worlds").getKeys(false));

            Bukkit.getConsoleSender().sendMessage(worlds.toString());
            String message = ChatColor.GREEN.toString() + ChatColor.BOLD + "\nPortal Control\n\n";

            for (int i = 0; i < worlds.size(); ++i)
            {
                String worldName = worlds.get(i);
                message += ChatColor.AQUA.toString() + ChatColor.BOLD + worldName + ":";

                String netherPath = "worlds." + worldName + ".netherportaldest";
                String endPath = "worlds." + worldName + ".endportaldest";

                if (config.isSet(netherPath))
                {
                    message += "\n" + ChatColor.GREEN + "Nether portals => " + config.getString(netherPath);
                }

                if (config.isSet(endPath))
                {
                    message += "\n" + ChatColor.GREEN + "End portals => " + config.getString(endPath);
                }

                message += "\n";
            }
            sender.sendMessage(message);
            return true;
        }

        return false;
    }

    @EventHandler
    public void onPortalTeleport(PlayerPortalEvent e)
    {
        World fromWorld = e.getFrom().getWorld();
        World toWorld = e.getTo().getWorld();

        // if fromWorld is in config
        if (config.getStringList("worlds." + fromWorld.getName()) != null)
        {
            boolean isNether = toWorld.getEnvironment().equals(World.Environment.NETHER);

            String configPath = (isNether) ? ".netherportaldest" : ".endportaldest";
            String newToWorldString = config.getString("worlds." + fromWorld.getName() + configPath);

            // if that realm isn't in the config
            if (newToWorldString == null)
                return;

            World newToWorld = getServer().getWorld(newToWorldString);

            if (newToWorld == null)
            {
                e.setCancelled(true);
                Bukkit.getConsoleSender().sendMessage(prefix + ChatColor.RED.toString() + ChatColor.BOLD + "A player just tried to use a PortalControl portal that was configured incorrectly.");
                e.getPlayer().sendMessage(prefix + ChatColor.RED.toString() + ChatColor.BOLD + "This portal is configured incorrectly. Please contact a system administrator.");
            }
            else
            {
                e.setCancelled(true);
                Location newSpawnLocation = newToWorld.getSpawnLocation();
                Location newLocation = new Location(newToWorld, newSpawnLocation.getX(), newSpawnLocation.getY(), newSpawnLocation.getZ(), newSpawnLocation.getYaw(), newSpawnLocation.getPitch());
                e.getPlayer().teleport(newLocation);
            }
        }
    }
}
