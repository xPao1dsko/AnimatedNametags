package me.animatednametags;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gestiona un TextDisplay (multilinea, MiniMessage) montado como pasajero de cada jugador.
 */
public final class NametagManager {

    static final String ENTITY_TAG = "animatednametags";

    private final AnimatedNametagsPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Tag> tags = new HashMap<>();
    private final Set<UUID> disabled = new HashSet<>();
    private final VanillaHider vanilla = new VanillaHider();
    private final TabHook tab = new TabHook();

    private List<NametagFormat> formats = List.of();
    private BukkitTask task;
    private long tick;
    private boolean papi;

    // settings
    private int interval;
    private boolean hideVanilla;
    private boolean hideTab;
    private boolean showOwn;
    private boolean hideWhenInvisible;
    private boolean seeThrough;
    private String sneakMode;
    private float yOffset;
    private float scale;
    private int lineWidth;
    private boolean shadow;
    private String background;
    private float viewRange;
    private boolean legacyColors;

    private static final class Tag {
        final TextDisplay display;
        Component last;
        boolean sneakApplied;

        Tag(TextDisplay display) {
            this.display = display;
        }
    }

    public NametagManager(AnimatedNametagsPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ ciclo de vida

    public void start() {
        loadSettings();
        cleanupOrphans();

        if (hideVanilla) {
            vanilla.enable();
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::run, 1L, interval);

        // TAB se inicializa un tick despues para que ya este cargado.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (hideTab) {
                tab.init(plugin.getLogger());
            }
            applyHidingToAll();
        });
        Bukkit.getScheduler().runTaskLater(plugin, this::applyHidingToAll, 60L);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            remove(player);
            vanilla.show(player);
            if (tab.isActive()) {
                tab.show(player);
            }
        }
        tags.clear();
        vanilla.disable();
    }

    public void reload() {
        plugin.reloadConfig();
        shutdown();
        start();
    }

    private void loadSettings() {
        FileConfiguration c = plugin.getConfig();
        interval = Math.max(1, c.getInt("settings.update-interval-ticks", 2));
        hideVanilla = c.getBoolean("settings.hide-vanilla-nametags", true);
        hideTab = c.getBoolean("settings.hide-tab-nametags", true);
        showOwn = c.getBoolean("settings.show-own-nametag", false);
        hideWhenInvisible = c.getBoolean("settings.hide-when-invisible", true);
        seeThrough = c.getBoolean("settings.see-through", true);
        sneakMode = c.getString("settings.sneak-mode", "dim").toLowerCase(Locale.ROOT);
        yOffset = (float) c.getDouble("settings.y-offset", 0.25);
        scale = (float) c.getDouble("settings.scale", 1.0);
        lineWidth = c.getInt("settings.line-width", 400);
        shadow = c.getBoolean("settings.shadow", false);
        background = c.getString("settings.background", "40000000");
        viewRange = (float) c.getDouble("settings.view-range", 0.6);
        legacyColors = c.getBoolean("settings.legacy-colors", true);
        papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        List<NametagFormat> list = new ArrayList<>();
        ConfigurationSection section = c.getConfigurationSection("formats");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection f = section.getConfigurationSection(id);
                if (f == null) {
                    continue;
                }
                list.add(new NametagFormat(
                        id,
                        f.getString("permission", ""),
                        f.getInt("priority", 0),
                        List.copyOf(f.getStringList("lines")),
                        f.getDouble("animation-speed", 0.02)));
            }
        }
        list.sort(Comparator.comparingInt(NametagFormat::priority).reversed());
        formats = list;
    }

    private void cleanupOrphans() {
        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                if (display.getScoreboardTags().contains(ENTITY_TAG)) {
                    display.remove();
                }
            }
        }
    }

    // ------------------------------------------------------------------ eventos

    public void onJoin(Player player) {
        for (long delay : new long[]{1L, 40L, 100L}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    applyHiding(player);
                }
            }, delay);
        }
    }

    public void onQuit(Player player) {
        remove(player);
        vanilla.show(player);
    }

    public void scheduleRefresh(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                remove(player);
                tickPlayer(player);
            }
        }, 2L);
    }

    // ------------------------------------------------------------------ ocultar vanilla / TAB

    private void applyHidingToAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyHiding(player);
        }
    }

    private void applyHiding(Player player) {
        if (disabled.contains(player.getUniqueId())) {
            vanilla.show(player);
            if (tab.isActive()) {
                tab.show(player);
            }
            return;
        }
        if (hideVanilla) {
            vanilla.hide(player);
        }
        if (hideTab && tab.isActive()) {
            tab.hide(player);
        }
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            remove(player);
        }
        applyHidingToAll();
    }

    /** @return true si el nametag queda activado. */
    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        boolean nowEnabled;
        if (disabled.remove(id)) {
            nowEnabled = true;
        } else {
            disabled.add(id);
            nowEnabled = false;
            remove(player);
        }
        applyHiding(player);
        return nowEnabled;
    }

    // ------------------------------------------------------------------ bucle de animacion

    private void run() {
        tick += interval;
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayer(player);
        }
    }

    private void tickPlayer(Player player) {
        UUID id = player.getUniqueId();

        if (disabled.contains(id)
                || player.isDead()
                || player.getGameMode() == GameMode.SPECTATOR) {
            remove(player);
            return;
        }

        NametagFormat format = pickFormat(player);
        if (format == null || format.lines().isEmpty()) {
            remove(player);
            return;
        }

        Tag tag = tags.get(id);
        if (tag == null
                || !tag.display.isValid()
                || !player.getPassengers().contains(tag.display)) {
            remove(player);
            tag = spawn(player);
            if (tag == null) {
                return;
            }
            tags.put(id, tag);
        }

        boolean sneaking = player.isSneaking();
        boolean invisible = hideWhenInvisible && player.hasPotionEffect(PotionEffectType.INVISIBILITY);
        boolean hideNow = invisible || (sneaking && sneakMode.equals("hide"));

        Component text = hideNow ? Component.empty() : render(player, format);
        if (!text.equals(tag.last)) {
            tag.display.text(text);
            tag.last = text;
        }

        if (sneaking != tag.sneakApplied) {
            applySneak(tag.display, sneaking);
            tag.sneakApplied = sneaking;
        }
    }

    private NametagFormat pickFormat(Player player) {
        for (NametagFormat format : formats) {
            String perm = format.permission();
            if (perm == null || perm.isEmpty() || player.hasPermission(perm)) {
                return format;
            }
        }
        return null;
    }

    private Component render(Player player, NametagFormat format) {
        double phaseValue = ((tick * format.speed()) % 2.0) - 1.0;
        String phase = fmt(phaseValue);
        String phaseRev = fmt(-phaseValue);
        String tickStr = Long.toString(tick);

        TagResolver resolver = TagResolver.resolver(
                Placeholder.unparsed("player", player.getName()),
                Placeholder.component("displayname", player.displayName()),
                Placeholder.unparsed("health", Integer.toString((int) Math.ceil(player.getHealth()))),
                Placeholder.unparsed("world", player.getWorld().getName()),
                Placeholder.unparsed("ping", Integer.toString(player.getPing()))
        );

        List<Component> lines = new ArrayList<>(format.lines().size());
        for (String raw : format.lines()) {
            String line = raw
                    .replace("{phase}", phase)
                    .replace("{phase_rev}", phaseRev)
                    .replace("{tick}", tickStr);
            if (papi) {
                line = PapiHook.apply(player, line);
            }
            if (legacyColors) {
                line = LegacyConverter.convert(line);
            }
            lines.add(mm.deserialize(line, resolver));
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    // ------------------------------------------------------------------ entidades

    private Tag spawn(Player player) {
        TextDisplay display = player.getWorld().spawn(player.getLocation(), TextDisplay.class, e -> {
            e.setPersistent(false);
            e.addScoreboardTag(ENTITY_TAG);
            e.setBillboard(Display.Billboard.CENTER);
            e.setAlignment(TextDisplay.TextAlignment.CENTER);
            e.setLineWidth(lineWidth);
            e.setShadowed(shadow);
            e.setSeeThrough(seeThrough);
            e.setViewRange(viewRange);
            e.setTransformation(new Transformation(
                    new Vector3f(0f, yOffset, 0f),
                    new AxisAngle4f(),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f()));
            applyBackground(e);
            e.text(Component.empty());
        });

        if (!player.addPassenger(display)) {
            display.remove();
            return null;
        }
        if (!showOwn) {
            player.hideEntity(plugin, display);
        }
        return new Tag(display);
    }

    private void applyBackground(TextDisplay display) {
        if (background.equalsIgnoreCase("default")) {
            display.setDefaultBackground(true);
            return;
        }
        display.setDefaultBackground(false);
        if (background.equalsIgnoreCase("none")) {
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            return;
        }
        try {
            long v = Long.parseLong(background.replace("#", ""), 16);
            display.setBackgroundColor(Color.fromARGB(
                    (int) ((v >> 24) & 0xFF),
                    (int) ((v >> 16) & 0xFF),
                    (int) ((v >> 8) & 0xFF),
                    (int) (v & 0xFF)));
        } catch (NumberFormatException ex) {
            display.setDefaultBackground(true);
        }
    }

    private void applySneak(TextDisplay display, boolean sneaking) {
        if (sneakMode.equals("dim")) {
            display.setSeeThrough(seeThrough && !sneaking);
            display.setTextOpacity(sneaking ? (byte) 110 : (byte) -1);
        }
    }

    public void remove(Player player) {
        Tag tag = tags.remove(player.getUniqueId());
        if (tag != null && tag.display.isValid()) {
            tag.display.remove();
        }
    }
}
