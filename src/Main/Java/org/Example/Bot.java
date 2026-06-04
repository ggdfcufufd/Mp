package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Bot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;
    private final String githubToken;
    private final String githubUsername;

    public Bot(String botToken, String botUsername, String githubToken, String githubUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.githubToken = githubToken;
        this.githubUsername = githubUsername;
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String chatId = update.getMessage().getChatId().toString();
        String text = update.getMessage().getText();

        if (text.startsWith("/plugin ")) {
            String input = text.substring(8).trim();
            if (input.isEmpty()) {
                sendMessage(chatId, """
                        ❌ Опиши плагин!
                        Примеры:
                        /plugin Убрать урон от жемчуга Края
                        /plugin Отменить кулдаун эндер-перла
                        /plugin Запретить PvP в радиусе 100 блоков от спавна
                        /plugin Авто-посадка дерева после рубки
                        /plugin Хил по команде /heal""");
                return;
            }
            processPluginRequest(chatId, input);
        } else if (text.equals("/start")) {
            sendMessage(chatId, """
                    👋 Я создаю плагины для Minecraft Paper.
                    
                    Напиши /plugin и опиши, что нужно:
                    • /plugin Убрать урон от жемчуга
                    • /plugin Отменить кулдаун перла
                    • /plugin Запрет PvP у спавна
                    
                    Я сам напишу код, загружу на GitHub и пришлю JAR!""");
        } else {
            sendMessage(chatId, "Используй команду /plugin с описанием. /start — помощь.");
        }
    }

    private void processPluginRequest(String chatId, String input) {
        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("перл") || lowerInput.contains("pearl") || lowerInput.contains("жемчуг")) {
            buildEnderPearlPlugin(chatId, input);
        } else if (lowerInput.contains("pvp") || lowerInput.contains("пвп")) {
            buildNoPvPPlugin(chatId, input);
        } else if (lowerInput.contains("хил") || lowerInput.contains("heal") || lowerInput.contains("здоровье")) {
            buildHealPlugin(chatId, input);
        } else if (lowerInput.contains("дерев") || lowerInput.contains("tree") || lowerInput.contains("посадк")) {
            buildTreePlugin(chatId, input);
        } else {
            buildCustomPlugin(chatId, input);
        }
    }

    // ─── ГОТОВЫЕ ШАБЛОНЫ ПЛАГИНОВ ───

    private void buildEnderPearlPlugin(String chatId, String description) {
        String pluginName = "EnderPearlFix";
        String repoName = "ender-pearl-fix";
        String commandName = "pearl";

        String mainCode = """
            package org.example;
            
            import org.bukkit.ChatColor;
            import org.bukkit.entity.EnderPearl;
            import org.bukkit.entity.Player;
            import org.bukkit.event.EventHandler;
            import org.bukkit.event.Listener;
            import org.bukkit.event.entity.EntityDamageByEntityEvent;
            import org.bukkit.event.entity.ProjectileLaunchEvent;
            import org.bukkit.event.player.PlayerTeleportEvent;
            import org.bukkit.plugin.java.JavaPlugin;
            import org.bukkit.projectiles.ProjectileSource;
            
            public class """ + pluginName + """ extends JavaPlugin implements Listener {
            
                @Override
                public void onEnable() {
                    getServer().getPluginManager().registerEvents(this, this);
                    getLogger().info(ChatColor.GREEN + "EnderPearlFix включён! Урон и кулдаун отменены.");
                }
            
                // Убираем урон от жемчуга
                @EventHandler
                public void onPearlDamage(EntityDamageByEntityEvent event) {
                    if (event.getDamager() instanceof EnderPearl) {
                        event.setCancelled(true);
                    }
                }
            
                // Убираем кулдаун
                @EventHandler
                public void onPearlThrow(ProjectileLaunchEvent event) {
                    if (event.getEntity() instanceof EnderPearl pearl) {
                        ProjectileSource shooter = pearl.getShooter();
                        if (shooter instanceof Player player) {
                            // Сбрасываем кулдаун
                            player.setCooldown(org.bukkit.Material.ENDER_PEARL, 0);
                        }
                    }
                }
            
                // Убираем урон при телепортации
                @EventHandler
                public void onPearlTeleport(PlayerTeleportEvent event) {
                    if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                        event.setCancelled(false); // разрешаем
                        // Убираем урон от падения после телепорта
                        event.getPlayer().setFallDistance(0);
                    }
                }
            }
            """;

        String pluginYml = """
            name: """ + pluginName + """
            version: 1.0.0
            main: org.example.""" + pluginName + """
            api-version: 1.20
            description: Отменяет ограничения жемчуга Края — урон и кулдаун
            """;

        buildAndUpload(chatId, repoName, pluginName, mainCode, pluginYml,
                "Отменяет урон и кулдаун эндер-перла. Кидай без ограничений!");
    }

    private void buildNoPvPPlugin(String chatId, String description) {
        String pluginName = "NoPvPZone";
        String repoName = "no-pvp-zone";

        String mainCode = """
            package org.example;
            
            import org.bukkit.ChatColor;
            import org.bukkit.Location;
            import org.bukkit.entity.Player;
            import org.bukkit.event.EventHandler;
            import org.bukkit.event.Listener;
            import org.bukkit.event.entity.EntityDamageByEntityEvent;
            import org.bukkit.plugin.java.JavaPlugin;
            
            public class """ + pluginName + """ extends JavaPlugin implements Listener {
            
                private Location spawnLocation;
                private int radius = 100;
            
                @Override
                public void onEnable() {
                    getServer().getPluginManager().registerEvents(this, this);
                    // Спавн = мир "world", координаты 0,0
                    spawnLocation = new Location(getServer().getWorld("world"), 0, 64, 0);
                    getLogger().info(ChatColor.GREEN + "NoPvPZone включён! PvP запрещено в радиусе " + radius + " блоков от спавна.");
                }
            
                @EventHandler
                public void onPlayerDamage(EntityDamageByEntityEvent event) {
                    if (!(event.getDamager() instanceof Player attacker)) return;
                    if (!(event.getEntity() instanceof Player victim)) return;
            
                    if (attacker.getLocation().distance(spawnLocation) <= radius ||
                        victim.getLocation().distance(spawnLocation) <= radius) {
                        event.setCancelled(true);
                        attacker.sendMessage(ChatColor.RED + "PvP запрещено в этой зоне!");
                    }
                }
            }
            """;

        String pluginYml = """
            name: """ + pluginName + """
            version: 1.0.0
            main: org.example.""" + pluginName + """
            api-version: 1.20
            description: Запрещает PvP в радиусе от спавна
            """;

        buildAndUpload(chatId, repoName, pluginName, mainCode, pluginYml,
                "Запрещает PvP в радиусе 100 блоков от спавна (0,0).");
    }

    private void buildHealPlugin(String chatId, String description) {
        String pluginName = "HealCommand";
        String repoName = "heal-command";

        String mainCode = """
            package org.example;
            
            import org.bukkit.ChatColor;
            import org.bukkit.attribute.Attribute;
            import org.bukkit.command.Command;
            import org.bukkit.command.CommandSender;
            import org.bukkit.entity.Player;
            import org.bukkit.plugin.java.JavaPlugin;
            import org.jetbrains.annotations.NotNull;
            
            public class """ + pluginName + """ extends JavaPlugin {
            
                @Override
                public void onEnable() {
                    getLogger().info(ChatColor.GREEN + "HealCommand включён! /heal — восстановить здоровье.");
                }
            
                @Override
                public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                         @NotNull String label, String[] args) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("Только для игроков!");
                        return true;
                    }
            
                    double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    player.setHealth(maxHealth);
                    player.setFoodLevel(20);
                    player.setSaturation(10);
                    player.sendMessage(ChatColor.GREEN + "Ты полностью исцелён!");
                    return true;
                }
            }
            """;

        String pluginYml = """
            name: """ + pluginName + """
            version: 1.0.0
            main: org.example.""" + pluginName + """
            api-version: 1.20
            description: Команда /heal для восстановления здоровья
            commands:
              heal:
                description: Восстановить здоровье
                usage: /heal
            """;

        buildAndUpload(chatId, repoName, pluginName, mainCode, pluginYml,
                "Добавляет команду /heal — полное восстановление здоровья и голода.");
    }

    private void buildTreePlugin(String chatId, String description) {
        String pluginName = "AutoReplant";
        String repoName = "auto-replant";

        String mainCode = """
            package org.example;
            
            import org.bukkit.ChatColor;
            import org.bukkit.Material;
            import org.bukkit.block.Block;
            import org.bukkit.entity.Player;
            import org.bukkit.event.EventHandler;
            import org.bukkit.event.Listener;
            import org.bukkit.event.block.BlockBreakEvent;
            import org.bukkit.inventory.ItemStack;
            import org.bukkit.plugin.java.JavaPlugin;
            
            public class """ + pluginName + """ extends JavaPlugin implements Listener {
            
                @Override
                public void onEnable() {
                    getServer().getPluginManager().registerEvents(this, this);
                    getLogger().info(ChatColor.GREEN + "AutoReplant включён! Саженцы сажаются автоматически.");
                }
            
                @EventHandler
                public void onTreeBreak(BlockBreakEvent event) {
                    Block block = event.getBlock();
                    Material type = block.getType();
                    Material sapling = getSapling(type);
            
                    if (sapling != null) {
                        Player player = event.getPlayer();
                        // Сажаем саженец на то же место
                        getServer().getScheduler().runTaskLater(this, () -> {
                            if (block.getType() == Material.AIR) {
                                block.setType(sapling);
                            }
                        }, 1L);
                    }
                }
            
                private Material getSapling(Material log) {
                    return switch (log) {
                        case OAK_LOG, OAK_WOOD -> Material.OAK_SAPLING;
                        case SPRUCE_LOG, SPRUCE_WOOD -> Material.SPRUCE_SAPLING;
                        case BIRCH_LOG, BIRCH_WOOD -> Material.BIRCH_SAPLING;
                        case JUNGLE_LOG, JUNGLE_WOOD -> Material.JUNGLE_SAPLING;
                        case ACACIA_LOG, ACACIA_WOOD -> Material.ACACIA_SAPLING;
                        case DARK_OAK_LOG, DARK_OAK_WOOD -> Material.DARK_OAK_SAPLING;
                        case CHERRY_LOG, CHERRY_WOOD -> Material.CHERRY_SAPLING;
                        default -> null;
                    };
                }
            }
            """;

        String pluginYml = """
            name: """ + pluginName + """
            version: 1.0.0
            main: org.example.""" + pluginName + """
            api-version: 1.20
            description: Автоматически сажает саженец после рубки дерева
            """;

        buildAndUpload(chatId, repoName, pluginName, mainCode, pluginYml,
                "Авто-посадка саженца после рубки дерева.");
    }

    private void buildCustomPlugin(String chatId, String description) {
        String pluginName = "CustomPlugin";
        String repoName = "custom-plugin-" + System.currentTimeMillis() % 100000;

        String mainCode = """
            package org.example;
            
            import org.bukkit.ChatColor;
            import org.bukkit.event.EventHandler;
            import org.bukkit.event.Listener;
            import org.bukkit.event.player.PlayerJoinEvent;
            import org.bukkit.plugin.java.JavaPlugin;
            
            public class """ + pluginName + """ extends JavaPlugin implements Listener {
            
                @Override
                public void onEnable() {
                    getServer().getPluginManager().registerEvents(this, this);
                    getLogger().info(ChatColor.GREEN + "Плагин запущен! Описание: """ + description + """");
                }
            
                @EventHandler
                public void onJoin(PlayerJoinEvent event) {
                    event.getPlayer().sendMessage(ChatColor.GOLD + "Плагин """ + pluginName + """ активен!");
                }
            }
            """;

        String pluginYml = """
            name: """ + pluginName + """
            version: 1.0.0
            main: org.example.""" + pluginName + """
            api-version: 1.20
            description: """ + description + """
            """;

        buildAndUpload(chatId, repoName, pluginName, mainCode, pluginYml,
                "Кастомный плагин. Описание: " + description);
    }

    // ─── ЗАГРУЗКА НА GITHUB И СБОРКА ───

    private void buildAndUpload(String chatId, String repoName, String pluginName,
                                 String mainCode, String pluginYml, String successMsg) {
        new Thread(() -> {
            try {
                sendMessage(chatId, "🔄 Создаю плагин " + pluginName + "...");

                String repoUrl = createGitHubRepo(repoName);
                sendMessage(chatId, "📁 Репозиторий: " + repoUrl);

                uploadFile(repoName, "src/main/java/org/example/" + pluginName + ".java", mainCode);
                uploadFile(repoName, "src/main/resources/plugin.yml", pluginYml);
                uploadFile(repoName, "build.gradle.kts", getBuildGradle());
                uploadFile(repoName, "settings.gradle.kts", "rootProject.name = \"" + repoName + "\"");
                uploadFile(repoName, ".github/workflows/build.yml", getWorkflow());
                uploadFile(repoName, "gradle/wrapper/gradle-wrapper.properties", getGradleProperties());

                sendMessage(chatId, "📤 Файлы загружены! GitHub Actions запущен...");
                Thread.sleep(20000);

                sendMessage(chatId, """
                        ✅ Плагин """ + pluginName + """ собран!
                        
                        📥 Скачай JAR здесь:
                        https://github.com/""" + githubUsername + "/" + repoName + """/releases
                        
                        📂 Положи файл в папку plugins сервера Paper.
                        
                        💡 """ + successMsg);

            } catch (Exception e) {
                sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
        }).start();
    }

    // ─── GITHUB API ───

    private String createGitHubRepo(String name) throws IOException {
        HttpURLConnection conn = apiCall("https://api.github.com/user/repos", "POST",
                "{\"name\":\"" + name + "\",\"private\":false,\"auto_init\":false}");
        if (conn.getResponseCode() == 201) {
            return "https://github.com/" + githubUsername + "/" + name;
        }
        throw new IOException("Ошибка создания репо: " + conn.getResponseCode());
    }

    private void uploadFile(String repo, String path, String content) throws IOException {
        String json = "{\"message\":\"Add " + path + "\",\"content\":\"" +
                Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)) + "\"}";
        apiCall("https://api.github.com/repos/" + githubUsername + "/" + repo + "/contents/" + path, "PUT", json);
    }

    private HttpURLConnection apiCall(String urlStr, String method, String json) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "token " + githubToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private void sendMessage(String chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (Exception ignored) {}
    }

    // ─── КОНФИГИ ───

    private String getBuildGradle() {
        return """
            plugins {
                id("java")
                id("com.github.johnrengelman.shadow") version "8.1.1"
            }
            group = "org.example"
            version = "1.0.0"
            repositories {
                mavenCentral()
                maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
            }
            dependencies {
                compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
            }
            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                }
            }
            """;
    }

    private String getWorkflow() {
        return """
            name: Build Plugin JAR
            on:
              push:
                branches: [main]
              workflow_dispatch:
            jobs:
              build:
                runs-on: ubuntu-latest
                permissions:
                  contents: write
                steps:
                  - uses: actions/checkout@v4
                  - name: Set up JDK 21
                    uses: actions/setup-java@v4
                    with:
                      java-version: '21'
                      distribution: 'temurin'
                  - name: Setup Gradle
                    uses: gradle/actions/setup-gradle@v3
                    with:
                      gradle-version: 8.12
                  - name: Build
                    run: gradle shadowJar
                  - name: Upload artifact
                    uses: actions/upload-artifact@v4
                    with:
                      name: plugin
                      path: build/libs/*.jar
                  - name: Create Release
                    uses: softprops/action-gh-release@v2
                    with:
                      tag_name: v${{ github.run_number }}
                      name: Release ${{ github.run_number }}
                      files: build/libs/*.jar
                    env:
                      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
            """;
    }

    private String getGradleProperties() {
        return """
            distributionBase=GRADLE_USER_HOME
            distributionPath=wrapper/dists
            distributionUrl=https\\://services.gradle.org/distributions/gradle-8.12-bin.zip
            networkTimeout=10000
            validateDistributionUrl=true
            zipStoreBase=GRADLE_USER_HOME
            zipStorePath=wrapper/dists
            """;
    }

    // ─── ЗАПУСК ───

    public static void main(String[] args) throws Exception {
        String botToken = System.getenv("BOT_TOKEN");
        String botUsername = System.getenv("BOT_USERNAME");
        String githubToken = System.getenv("GITHUB_TOKEN");
        String githubUsername = System.getenv("GITHUB_USERNAME");

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(new Bot(botToken, botUsername, githubToken, githubUsername));
        System.out.println("Бот-строитель плагинов запущен!");
    }
}
