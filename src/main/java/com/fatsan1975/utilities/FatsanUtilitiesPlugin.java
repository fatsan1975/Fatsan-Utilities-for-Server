package com.fatsan1975.utilities;

import com.fatsan1975.utilities.admin.AdminInventoryService;
import com.fatsan1975.utilities.admin.InvSeeListener;
import com.fatsan1975.utilities.command.BalanceCommand;
import com.fatsan1975.utilities.command.BalanceTopCommand;
import com.fatsan1975.utilities.command.ChatShowcaseCommand;
import com.fatsan1975.utilities.command.DebugCommand;
import com.fatsan1975.utilities.command.HelpCommand;
import com.fatsan1975.utilities.command.InvSeeCommand;
import com.fatsan1975.utilities.command.ModuleCommand;
import com.fatsan1975.utilities.command.PayCommand;
import com.fatsan1975.utilities.command.ReloadCommand;
import com.fatsan1975.utilities.command.SpawnCommand;
import com.fatsan1975.utilities.command.TpaCommand;
import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.PluginConfigValidator;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.economy.BalanceTopCacheService;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.economy.PayLimitService;
import com.fatsan1975.utilities.logging.AuditLogger;
import com.fatsan1975.utilities.teleport.TeleportService;
import com.fatsan1975.utilities.util.CooldownService;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

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

  @Override
  public void onEnable() {
    this.pluginConfiguration = new PluginConfiguration(this);
    this.pluginConfiguration.loadAll();

    this.moduleManager = new ModuleManager(pluginConfiguration);
    this.rateLimitService = new RateLimitService();
    this.configValidator = new PluginConfigValidator(this, pluginConfiguration);
    this.configValidator.validateAndLog();

    this.economyService = new EconomyService(this);
    if (!this.economyService.setup()) {
      getLogger().severe("Vault ekonomisi bulunamadı. Plugin kapatılıyor.");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    this.cooldownService = new CooldownService();
    this.teleportService = new TeleportService(this, pluginConfiguration);
    this.adminInventoryService = new AdminInventoryService();
    this.balanceTopCacheService = new BalanceTopCacheService(economyService, pluginConfiguration);
    this.payLimitService = new PayLimitService();
    this.auditLogger = new AuditLogger(this);

    getServer().getPluginManager().registerEvents(new InvSeeListener(), this);
    registerCommands();
    getLogger().info("FatsanUtilities etkinleştirildi.");
  }

  private void registerCommands() {
    register("balance", new BalanceCommand(economyService, pluginConfiguration, moduleManager, rateLimitService));
    register("balancetop", new BalanceTopCommand(economyService, balanceTopCacheService, pluginConfiguration, moduleManager, rateLimitService));
    register("pay", new PayCommand(this, economyService, payLimitService, auditLogger, pluginConfiguration, cooldownService, moduleManager, rateLimitService));

    TpaCommand tpaCommand = new TpaCommand(this, pluginConfiguration, cooldownService, moduleManager, rateLimitService);
    register("tpa", tpaCommand);
    register("tpaccept", tpaCommand);
    register("tpdeny", tpaCommand);

    register("rtp", new SpawnCommand(this, pluginConfiguration, teleportService, cooldownService, SpawnCommand.Mode.RTP, moduleManager, rateLimitService));
    register("spawn", new SpawnCommand(this, pluginConfiguration, teleportService, cooldownService, SpawnCommand.Mode.SPAWN, moduleManager, rateLimitService));

    register("itemchat", new ChatShowcaseCommand(pluginConfiguration, ChatShowcaseCommand.Mode.ITEM, moduleManager, rateLimitService));
    register("invchat", new ChatShowcaseCommand(pluginConfiguration, ChatShowcaseCommand.Mode.INVENTORY, moduleManager, rateLimitService));
    register("invsee", new InvSeeCommand(pluginConfiguration, adminInventoryService, moduleManager, rateLimitService, auditLogger));
    register("fudebug", new DebugCommand(pluginConfiguration, moduleManager, rateLimitService));
    register("fumodule", new ModuleCommand(pluginConfiguration, moduleManager, rateLimitService, auditLogger));

    register("futilitiesreload", new ReloadCommand(pluginConfiguration, configValidator, moduleManager, rateLimitService, auditLogger));
    register("fuhelp", new HelpCommand(pluginConfiguration, rateLimitService));
  }

  private void register(String commandName, org.bukkit.command.CommandExecutor executor) {
    PluginCommand command = getCommand(commandName);
    if (command == null) {
      getLogger().warning("Komut plugin.yml içinde bulunamadı: " + commandName);
      return;
    }
    command.setExecutor(executor);
    if (executor instanceof TabCompleter completer) {
      command.setTabCompleter(completer);
    }
  }
}
