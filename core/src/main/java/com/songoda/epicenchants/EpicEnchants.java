package com.songoda.epicenchants;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.InvalidCommandArgument;
import com.songoda.epicenchants.commands.EnchantCommand;
import com.songoda.epicenchants.listeners.ArmorListener;
import com.songoda.epicenchants.listeners.BookListener;
import com.songoda.epicenchants.listeners.PlayerListener;
import com.songoda.epicenchants.managers.EnchantManager;
import com.songoda.epicenchants.managers.FileManager;
import com.songoda.epicenchants.menus.BookMenu;
import com.songoda.epicenchants.objects.Enchant;
import com.songoda.epicenchants.utils.EnchantUtils;
import com.songoda.epicenchants.utils.FastInv;
import com.songoda.epicenchants.utils.VersionDependent;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.stream.Collectors;

import static com.songoda.epicenchants.utils.Chat.color;
import static org.bukkit.Bukkit.getConsoleSender;

@Getter
public class EpicEnchants extends JavaPlugin {

    private FastInv bookInventory;
    private BukkitCommandManager commandManager;
    private Economy economy;
    private EnchantManager enchantManager;
    private EnchantUtils enchantUtils;
    private FileManager fileManager;
    private Locale locale;

    @Override
    public void onEnable() {
        getConsoleSender().sendMessage(color("&a============================="));
        getConsoleSender().sendMessage(color("&7" + getDescription().getName() + " " + getDescription().getVersion() + " by &5Songoda <3&7!"));
        getConsoleSender().sendMessage(color("&7Action: &aEnabling&7..."));

        Locale.init(this);
        FastInv.init(this);

        this.locale = Locale.getLocale(getConfig().getString("language"));
        this.fileManager = new FileManager(this);
        this.enchantManager = new EnchantManager();
        this.economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
        this.enchantUtils = new EnchantUtils(this);

        fileManager.createFiles();
        fileManager.loadEnchants();

        setupCommands();
        setupListeners();
        setupVersion();

        if (!enchantManager.getEnchants().isEmpty()) {
            getLogger().info("Successfully loaded enchants: " + enchantManager.getEnchants().stream().map(Enchant::getIdentifier).collect(Collectors.joining(",")));
        }

        this.bookInventory = new BookMenu(this, fileManager.getConfiguration("bookMenu"));

        getConsoleSender().sendMessage(color("&a============================="));
    }

    @Override
    public void onDisable() {
        getConsoleSender().sendMessage(color("&a============================="));
        getConsoleSender().sendMessage(color("&7" + getDescription().getName() + " " + getDescription().getVersion() + " by &5Songoda <3&7!"));
        getConsoleSender().sendMessage(color("&7Action: &cDisabling&7..."));
        getConsoleSender().sendMessage(color("&a============================="));
    }

    private void setupCommands() {
        this.commandManager = new BukkitCommandManager(this);

        commandManager.registerDependency(EpicEnchants.class, "instance", this);
        commandManager.getCommandCompletions().registerCompletion("enchants", c -> enchantManager.getEnchants().stream().map(Enchant::getIdentifier).collect(Collectors.toList()));
        commandManager.getCommandContexts().registerContext(Enchant.class, c -> enchantManager.getEnchant(c.popFirstArg()).orElseThrow(() -> new InvalidCommandArgument("No echant exists by that name")));

        commandManager.registerCommand(new EnchantCommand());
    }

    private void setupListeners() {
        EpicEnchants instance = this;
        new HashSet<Listener>() {{
            add(new BookListener(instance));
            add(new ArmorListener());
            add(new PlayerListener(instance));
        }}.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }

    private void setupVersion() {
        int currentVersion = Integer.parseInt(getServer().getClass().getPackage().getName().split("\\.")[3].split("_")[1]);

        if (currentVersion >= 13) {
            VersionDependent.initDefault(currentVersion);
        } else {
            VersionDependent.initLegacy(currentVersion);
        }
    }

}
