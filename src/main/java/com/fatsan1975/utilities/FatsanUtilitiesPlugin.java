package com.fatsan1975.utilities;

import com.fatsan1975.utilities.admin.AdminInventoryService;
import com.fatsan1975.utilities.admin.InvSeeListener;
import com.fatsan1975.utilities.chat.ChatFormatListener;
import com.fatsan1975.utilities.command.BalanceCommand;
import com.fatsan1975.utilities.command.BalanceTopCommand;
import com.fatsan1975.utilities.command.BrigadierCommands;
import com.fatsan1975.utilities.command.ChatShowcaseCommand;
import com.fatsan1975.utilities.command.DebugCommand;
import com.fatsan1975.utilities.command.EcoCommand;
import com.fatsan1975.utilities.command.HelpCommand;
import com.fatsan1975.utilities.command.HomeCommand;
import com.fatsan1975.utilities.command.InvSeeCommand;
import com.fatsan1975.utilities.command.ModuleCommand;
import com.fatsan1975.utilities.command.PayCommand;
import com.fatsan1975.utilities.command.ReloadCommand;
import com.fatsan1975.utilities.command.SetSpawnCommand;
import com.fatsan1975.utilities.command.SpawnCommand;
import com.fatsan1975.utilities.command.TpaCommand;
import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.PlayerLifecycleListener;
import com.fatsan1975.utilities.core.PluginConfigValidator;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.core.scheduler.FoliaScheduler;
import com.fatsan1975.utilities.economy.BalanceTopCacheService;
import com.fatsan1975.utilities.economy.EconomyHookListener;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.economy.PayLimitService;
import com.fatsan1975.utilities.logging.AuditLogger;
import com.fatsan1975.utilities.teleport.HomeService;
import com.fatsan1975.utilities.teleport.SpawnService;
import com.fatsan1975.utilities.teleport.TeleportService;
import com.fatsan1975.utilities.util.CommandPermissions;
import com.fatsan1975.utilities.util.CooldownService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public final class FatsanUtilitiesPlugin extends JavaPlugin {
  private PluginConfiguration pluginConfiguration;
  private EconomyService economyService;
  private TeleportService teleportService;
  private CooldownService cooldownService;
  private AdminInventoryService adminInventoryService;
  private ModuleManager moduleManager;
  private RateLimitService rateLimitService;
  private PluginConfigValidator configValidator;
  private BalanceTopCacheService balanceTopCacheService;
  private PayLimitService payLimitService;
  private AuditLogger auditLogger;
  private TpaCommand tpaCommand;
  private SpawnService spawnService;
  private HomeService homeService;

  private final Map<String, CommandExecutor> executorRegistry = new HashMap<>();
  private final Map<String, String> permissionRegistry = new HashMap<>();

  public FatsanUtilitiesPlugin() {
    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
      BrigadierCommands.register(this, event.registrar()));
  }

  @Override
  public void onEnable() {
    this.pluginConfiguration = new PluginConfiguration(this);
    this.pluginConfiguration.loadAll();

    this.moduleManager = new ModuleManager(pluginConfiguration);
    this.rateLimitService = new RateLimitService();
    this.configValidator = new PluginConfigValidator(this, pluginConfiguration);
    this.configValidator.validateAndLog();

    this.auditLogger = new AuditLogger(this);

    this.economyService = new EconomyService(this, pluginConfiguration);
    if (!this.economyService.setup()) {
      getLogger().warning("Ekonomi servisi ilk denemede hazir olmadi. Runtime'da tekrar denenecek.");
    }

    this.cooldownService = new CooldownService();
    this.teleportService = new TeleportService(this, pluginConfiguration);
    this.adminInventoryService = new AdminInventoryService(pluginConfiguration);
    this.spawnService = new SpawnService(this);
    this.homeService = new HomeService(this);
    this.balanceTopCacheService = new BalanceTopCacheService(economyService, pluginConfiguration);
    this.payLimitService = new PayLimitService();

    getLogger().info("FatsanUtilities acildi. Folia: " + FoliaScheduler.isFolia()
      + ", Economy: " + economyService.mode());

    getServer().getPluginManager().registerEvents(new InvSeeListener(), this);
    getServer().getPluginManager().registerEvents(new EconomyHookListener(this, economyService), this);
    getServer().getPluginManager().registerEvents(new ChatFormatListener(pluginConfiguration, moduleManager), this);

    registerCommands();

    getServer().getPluginManager().registerEvents(
      new PlayerLifecycleListener(economyService, cooldownService, rateLimitService, tpaCommand), this);
  }

  @Override
  public void onDisable() {
    if (economyService != null) {
      economyService.shutdown();
    }
    if (auditLogger != null) {
      auditLogger.shutdown();
    }
  }

  private void registerCommands() {
    register("balance", new BalanceCommand(economyService, pluginConfiguration, moduleManager, rateLimitService));
    register("balancetop", new BalanceTopCommand(economyService, balanceTopCacheService, pluginConfiguration, moduleManager, rateLimitService));
    register("pay", new PayCommand(this, economyService, payLimitService, auditLogger, pluginConfiguration, cooldownService, moduleManager, rateLimitService));
    register("eco", new EcoCommand(economyService, pluginConfiguration, moduleManager, rateLimitService, auditLogger));

    this.tpaCommand = new TpaCommand(this, pluginConfiguration, cooldownService, moduleManager, rateLimitService, economyService);
    register("tpa", tpaCommand);
    register("tpaccept", tpaCommand);
    register("tpdeny", tpaCommand);

    register("rtp", new SpawnCommand(this, pluginConfiguration, teleportService, cooldownService, SpawnCommand.Mode.RTP, moduleManager, rateLimitService, spawnService, economyService));
    register("spawn", new SpawnCommand(this, pluginConfiguration, teleportService, cooldownService, SpawnCommand.Mode.SPAWN, moduleManager, rateLimitService, spawnService, economyService));
    register("setspawn", new SetSpawnCommand(pluginConfiguration, spawnService, moduleManager, rateLimitService));

    register("home", new HomeCommand(this, pluginConfiguration, homeService, cooldownService, moduleManager, rateLimitService, economyService, HomeCommand.Mode.HOME));
    register("sethome", new HomeCommand(this, pluginConfiguration, homeService, cooldownService, moduleManager, rateLimitService, economyService, HomeCommand.Mode.SETHOME));
    register("delhome", new HomeCommand(this, pluginConfiguration, homeService, cooldownService, moduleManager, rateLimitService, economyService, HomeCommand.Mode.DELHOME));
    register("homes", new HomeCommand(this, pluginConfiguration, homeService, cooldownService, moduleManager, rateLimitService, economyService, HomeCommand.Mode.HOMES));

    register("itemchat", new ChatShowcaseCommand(pluginConfiguration, ChatShowcaseCommand.Mode.ITEM, moduleManager, rateLimitService));
    register("invchat", new ChatShowcaseCommand(pluginConfiguration, ChatShowcaseCommand.Mode.INVENTORY, moduleManager, rateLimitService));
    register("invsee", new InvSeeCommand(pluginConfiguration, adminInventoryService, moduleManager, rateLimitService, auditLogger));
    register("fudebug", new DebugCommand(pluginConfiguration, moduleManager, rateLimitService, economyService));
    register("fumodule", new ModuleCommand(pluginConfiguration, moduleManager, rateLimitService, auditLogger));

    register("futilitiesreload", new ReloadCommand(pluginConfiguration, configValidator, moduleManager, rateLimitService, auditLogger));
    register("fuhelp", new HelpCommand(pluginConfiguration, rateLimitService));
  }

  private void register(String commandName, CommandExecutor executor) {
    executorRegistry.put(commandName, executor);

    String permission = CommandPermissions.permissionFor(commandName);
    permissionRegistry.put(commandName, permission);

    PluginCommand command = getCommand(commandName);
    if (command == null) {
      return;
    }

    command.setPermission(null);
    command.setPermissionMessage(null);
    command.setExecutor((sender, ignoredCommand, label, args) ->
      executor.onCommand(sender, new PermissionProxy(commandName, permission), label, args));
    if (executor instanceof TabCompleter completer) {
      command.setTabCompleter(completer);
    }
  }

  public void invokeExecutor(String cmdName, org.bukkit.command.CommandSender sender, String[] args) {
    CommandExecutor exec = executorRegistry.get(cmdName);
    if (exec == null) {
      return;
    }
    String permission = permissionRegistry.get(cmdName);
    exec.onCommand(sender, new PermissionProxy(cmdName, permission), cmdName, args);
  }

  private static final class PermissionProxy extends Command {
    PermissionProxy(String name, String permission) {
      super(name);
      if (permission != null) {
        setPermission(permission);
      }
    }

    @Override
    public boolean execute(org.bukkit.command.CommandSender sender, String label, String[] args) {
      return false;
    }
  }

  public PluginConfiguration pluginConfiguration() {
    return pluginConfiguration;
  }

  public EconomyService economyService() {
    return economyService;
  }

  public HomeService homeService() {
    return homeService;
  }
}
