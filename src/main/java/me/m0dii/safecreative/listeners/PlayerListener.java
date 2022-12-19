package me.m0dii.safecreative.listeners;

import me.m0dii.pllib.utils.InventoryUtils;
import me.m0dii.pllib.utils.TextUtils;
import me.m0dii.safecreative.SafeCreativePlugin;
import me.m0dii.safecreative.utils.Utils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PlayerListener implements Listener {
    private static final String META_BLOCK_CREATIVE = "creative_block";

    private final Set<Material> containerTypes = EnumSet.of(
            Material.CHEST,
            Material.DROPPER,
            Material.HOPPER,
            Material.DISPENSER,
            Material.TRAPPED_CHEST,
            Material.BREWING_STAND,
            Material.FURNACE,
            Material.SHULKER_BOX,
            Material.BLACK_SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX
    );
    private final SafeCreativePlugin plugin;
    private final FileConfiguration cfg;

    private static boolean enabled = true;

    public static void setEnabled(boolean enabled) {
        PlayerListener.enabled = enabled;
    }

    public static void toggleEnabled() {
        PlayerListener.enabled = !PlayerListener.enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public PlayerListener(SafeCreativePlugin plugin) {
        this.plugin = plugin;

        this.cfg = plugin.getCfg();
    }

    @EventHandler
    public void onChangeGameMode(final PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();

        Inventory inv = p.getInventory();

        GameMode mode = e.getNewGameMode();

        if (mode.equals(GameMode.SURVIVAL)) {
            removeCreativeItems(inv);
        }

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.gamemode-switch-empty")) {
            return;
        }

        if (cfg.getBoolean("clear-effects-on-switch")) {
            p.getActivePotionEffects().clear();
        }

        if (mode.equals(GameMode.CREATIVE) && cfg.getBoolean("prevent.switch-creative-with-items")) {
            if (!InventoryUtils.isEmpty(inv)) {
                p.sendMessage(Utils.format(cfg.getString("messages.inventory-not-empty")));

                e.setCancelled(true);
            }
        }

        if (mode.equals(GameMode.SURVIVAL)) {
            if (cfg.getBoolean("prevent.switch-survival-with-items")) {
                if (!InventoryUtils.isEmpty(inv)) {
                    p.sendMessage(Utils.format(cfg.getString("messages.inventory-not-empty")));

                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void worldChange(final PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();

        if (hasBypass(p)) {
            return;
        }

        removeCreativeItems(p.getInventory());
    }

    private void removeCreativeItems(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);

            if (item == null
            || item.getType().isAir()
            || item.getItemMeta() == null) {
                continue;
            }

            List<Component> lore = item.lore();

            if (lore == null) {
                continue;
            }

            for (Component comp : lore) {
                String text = TextUtils.stripColor(comp);

                if (text.equalsIgnoreCase("CREATIVE")) {
                    inv.setItem(i, new ItemStack(Material.AIR));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        HumanEntity p = e.getWhoClicked();

        if (hasBypass(p)) {
            return;
        }

        if (isCreative(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreakContainers(final BlockBreakEvent e) {
        if (!cfg.getBoolean("prevent.breaking-containers.enabled")) {
            return;
        }

        Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.container-break")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.breaking-containers.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        Material m = e.getBlock().getType();

        if (!containerTypes.contains(m)) {
            return;
        }

        if (!(e.getBlock().getState() instanceof Container cont)) {
            return;
        }

        if (cfg.getBoolean("prevent.breaking-containers.survival")) {
            if (isSurvival(p)) {
                if(cfg.getBoolean("prevent.breaking-containers.only-not-empty")) {
                    if (InventoryUtils.isEmpty(cont.getInventory())) {
                        return;
                    } else {
                        e.setCancelled(true);
                        p.sendMessage(Utils.format(cfg.getString("prevent.breaking-containers.message-not-empty")));
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }

        if (cfg.getBoolean("prevent.breaking-containers.creative")) {
            if (isCreative(p)) {
                if(cfg.getBoolean("prevent.breaking-containers.only-not-empty")) {
                    if (InventoryUtils.isEmpty(cont.getInventory())) {
                        return;
                    } else {
                        e.setCancelled(true);
                        p.sendMessage(Utils.format(cfg.getString("prevent.breaking-containers.message-not-empty")));
                    }
                } else {
                    e.setCancelled(true);
                }
            }
        }

        if (cfg.getBoolean("prevent.breaking-containers.clear-contents-on-break")) {
            Inventory inv = cont.getInventory();

            inv.clear();
        }

        if (cfg.getBoolean("prevent.breaking-containers.set-to-air")) {
            e.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreakTagged(final BlockBreakEvent e) {
        if (!cfg.getBoolean("prevent.survival-break-creative-placed.enabled")) {
            return;
        }

        Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.survival-break-creative-placed")) {
            return;
        }

        Block source = e.getBlock();

        if (source.getType().isAir()) {
            return;
        }

        if (isCreative(p)) {
            return;
        }

        if (source.hasMetadata(META_BLOCK_CREATIVE)) {
            e.setCancelled(true);

            if(cfg.getBoolean("prevent.survival-break-creative-placed.set-to-air")) {
                source.setType(Material.AIR);
            }

            p.sendMessage(Utils.format(cfg.getString("prevent.survival-break-creative-placed.message")));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlaceTagged(final BlockPlaceEvent e) {
        if (!cfg.getBoolean("prevent.survival-break-creative-placed.enabled")) {
            return;
        }

        Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.survival-break-creative-placed")) {
            return;
        }

        final Block source = e.getBlock();

        if (source.getType().isAir()) {
            return;
        }

        if (isCreative(p)) {
            source.setMetadata(META_BLOCK_CREATIVE, new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler
    public void cancelPickup(final PlayerAttemptPickupItemEvent e) {
        if (!cfg.getBoolean("prevent.item-pickup.enabled")) {
            return;
        }

        Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.item-pickup")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.item-pickup.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (isCreative(p)) {
            if (cfg.getBoolean("prevent.item-pickup.creative")) {
                e.setCancelled(true);
            }

            if (cfg.getBoolean("prevent.item-pickup.destroy")) {
                e.getItem().remove();
            }
        }

        if (p.getGameMode().equals(GameMode.ADVENTURE)) {
            if (cfg.getBoolean("prevent.item-pickup.adventure")) {
                e.setCancelled(true);
            }

            if (cfg.getBoolean("prevent.item-pickup.destroy")) {
                e.getItem().remove();
            }
        }

        if (!isSurvival(p)) {
            return;
        }

        if (!cfg.getBoolean("prevent.item-pickup.survival")) {
            return;
        }

        ItemStack item = e.getItem().getItemStack();

        List<Component> lore = item.lore();

        if (lore == null) {
            return;
        }

        for (Component comp : lore) {
            String text = TextUtils.stripColor(comp);

            if (cfg.getBoolean("prevent.item-pickup.tagged-items-only")) {
                if (text.equalsIgnoreCase("CREATIVE")) {
                    if (cfg.getBoolean("prevent.item-pickup.survival")) {
                        e.setCancelled(true);
                    }
                }
            } else {
                if (cfg.getBoolean("prevent.item-pickup.survival")) {
                    e.setCancelled(true);
                }
            }

            if (cfg.getBoolean("prevent.item-pickup.destroy")) {
                e.getItem().remove();
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(final ProjectileLaunchEvent e) {
        ProjectileSource player = e.getEntity().getShooter();

        if (!(player instanceof Player p)) {
            return;
        }

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.potion-throw")) {
            return;
        }

        World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.potion-throw.worlds");
        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        Projectile proj = e.getEntity();

        if (!(proj instanceof ThrownPotion)) {
            return;
        }

        if (!cfg.getBoolean("prevent.potion-throw.enabled")) {
            return;
        }

        if (p.getActiveItem() == null) {
            return;
        }

        if (cfg.getBoolean("prevent.potion-throw.survival")) {
            if (isSurvival(p)) {
                e.setCancelled(true);

                p.getActiveItem().setType(Material.AIR);

                p.sendMessage(Utils.format(cfg.getString("prevent.potion-throw.message")));
            }
        }

        if (cfg.getBoolean("prevent.potion-throw.creative")) {
            if (isCreative(p)) {
                e.setCancelled(true);

                p.getActiveItem().setType(Material.AIR);

                p.sendMessage(Utils.format(cfg.getString("prevent.potion-throw.message")));
            }
        }
    }

    @EventHandler
    public void onDropItem(final PlayerDropItemEvent e) {
        if (!cfg.getBoolean("prevent.item-drop.enabled")) {
            return;
        }

        HumanEntity p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.item-drop")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.item-drop.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.item-drop.survival") && isSurvival(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.item-drop.message")));
        }

        if (cfg.getBoolean("prevent.item-drop.creative") && isCreative(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.item-drop.message")));
        }
    }

    @EventHandler
    public void destroyEntity(final EntitySpawnEvent e) {
        if (!cfg.getBoolean("prevent.entity-spawn.enabled")) {
            return;
        }

        final World w = e.getEntity().getWorld();

        final List<String> worlds = cfg.getStringList("prevent.entity-spawn.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.entity-spawn.entities.all")) {
            e.setCancelled(true);

            e.getEntity().remove();

            return;
        }

        final List<String> entityTypeList = cfg.getStringList("prevent.entity-spawn.entities.list");

        if (cfg.getBoolean("prevent.entity-spawn.entities.blacklist")) {
            if (entityTypeList.stream()
                    .anyMatch(entityName -> entityName.equalsIgnoreCase(e.getEntityType().name()))) {
                e.setCancelled(true);

                e.getEntity().remove();
            }
        } else {
            if (entityTypeList.stream()
                    .noneMatch(entityName -> entityName.equalsIgnoreCase(e.getEntityType().name()))) {
                e.setCancelled(true);

                e.getEntity().remove();
            }
        }
    }

    @EventHandler
    public void restrictItems(final InventoryCreativeEvent e) {
        if (!cfg.getBoolean("restricted-items.enabled")) {
            return;
        }

        final HumanEntity p = e.getView().getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.item-restriction")) {
            return;
        }

        final ItemStack cursor = e.getCursor();

        if (cursor == null || cursor.getType().isAir()) {
            return;
        }

        for (String blacklisted : cfg.getStringList("restricted-items.list")) {
            Material m = Material.matchMaterial(blacklisted);

            if (!cursor.getType().equals(m)) {
                continue;
            }

            p.sendMessage(Utils.format(cfg.getString("restricted-items.message")));

            e.setCursor(new ItemStack(Material.AIR));

            e.setCancelled(true);
        }
    }

    @EventHandler
    public void cancelContainerOpen(final InventoryOpenEvent e) {
        if (!cfg.getBoolean("prevent.opening-containers.enabled")) {
            return;
        }

        final HumanEntity p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.container-open")) {
            return;
        }

        if (isSurvival(p)) {
            removeCreativeItems(p.getInventory());
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.opening-containers.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        final InventoryType type = e.getInventory().getType();

        if (isCreative(p)) {
            if (!type.equals(InventoryType.CREATIVE)) {
                if (cfg.getBoolean("prevent.opening-containers.creative")) {
                    p.sendMessage(Utils.format(cfg.getString("prevent.opening-containers.message")));

                    e.setCancelled(true);
                }
            }
        }

        if (isSurvival(p)) {
            if (cfg.getBoolean("prevent.opening-containers.survival")) {
                p.sendMessage(Utils.format(cfg.getString("prevent.opening-containers.message")));

                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void portalCreate(final PortalCreateEvent e) {
        if (!cfg.getBoolean("prevent.portal-creation.enabled")) {
            return;
        }

        if (!(e.getEntity() instanceof Player p)) {
            return;
        }

        final World w = e.getWorld();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.portal-create")) {
            return;
        }

        final List<String> worlds = cfg.getStringList("prevent.portal-creation.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.portal-creation.survival") && isSurvival(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.portal-creation.message")));
        }

        if (cfg.getBoolean("prevent.portal-creation.creative") && isCreative(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.portal-creation.message")));
        }
    }

    @EventHandler
    public void addCreativeTag(final InventoryCreativeEvent e) {
        final HumanEntity p = e.getView().getPlayer();

        if (hasBypass(p)) {
            return;
        }

        final ItemStack cursor = e.getCursor();

        if (cursor.getType().isAir()) {
            return;
        }

        ItemMeta cursorMeta = cursor.getItemMeta();

        List<Component> lore = List.of(TextUtils.colorize("&4CREATIVE"));

        cursorMeta.lore(lore);

        cursor.setItemMeta(cursorMeta);

        cursorMeta.getPersistentDataContainer().set(
                new NamespacedKey(this.plugin, "Type"),
                PersistentDataType.STRING, "CREATIVE"
        );

        e.setCursor(cursor);
    }

    @EventHandler
    public void onBowShoot(final EntityShootBowEvent e) {
        if (!cfg.getBoolean("prevent.bow-shoot.enabled")) {
            return;
        }

        if (!(e.getEntity() instanceof Player p)) {
            return;
        }

        final World w = p.getWorld();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.bow-shoot")) {
            return;
        }

        final List<String> worlds = cfg.getStringList("prevent.bow-shoot.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.bow-shoot.survival") && isSurvival(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.bow-shoot.message")));
        }

        if (cfg.getBoolean("prevent.bow-shoot.creative") && isCreative(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.bow-shoot.message")));
        }
    }

    @EventHandler
    public void onPotionDrink(final PlayerItemConsumeEvent e) {
        if (!cfg.getBoolean("prevent.potion-drink.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.potion-drink")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.potion-drink.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.potion-drink.survival") && isSurvival(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.potion-drink.message")));
        }

        if (cfg.getBoolean("prevent.potion-drink.creative") && isCreative(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.potion-drink.message")));
        }
    }

    @EventHandler
    public void onCommand(final PlayerCommandPreprocessEvent e) {
        if (!cfg.getBoolean("prevent.command-execution.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.command-execution")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.command-execution.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.command-execution.commands.all")) {
            if (cfg.getBoolean("prevent.command-execution.survival") && isSurvival(p)) {
                e.setCancelled(true);

                p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
            }

            if (cfg.getBoolean("prevent.command-execution.creative") && isCreative(p)) {
                e.setCancelled(true);

                p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
            }
        } else {
            final List<String> commands = cfg.getStringList("prevent.command-execution.commands.list");

            if (cfg.getBoolean("prevent.command-execution.commands.blacklist")) {
                if (cfg.getBoolean("prevent.command-execution.survival") && isSurvival(p)) {
                    if (commands.stream().anyMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }

                if (cfg.getBoolean("prevent.command-execution.creative") && isCreative(p)) {
                    if (commands.stream().anyMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }
            } else {
                if (cfg.getBoolean("prevent.command-execution.survival") && isSurvival(p)) {
                    if (commands.stream().noneMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }

                if (cfg.getBoolean("prevent.command-execution.creative") && isCreative(p)) {
                    if (commands.stream().noneMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFly(final PlayerToggleFlightEvent e) {
        if (!cfg.getBoolean("prevent.fly.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.fly")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.fly.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (isCreative(p)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.fly.message")));
        }
    }

    @EventHandler
    public void onFlyMove(final PlayerMoveEvent e) {
        if (!cfg.getBoolean("prevent.fly.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.fly")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.fly.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (isCreative(p)) {
            if (p.isFlying()) {
                p.setFlying(false);
            }

            if (p.getAllowFlight()) {
                p.setAllowFlight(false);
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEntityEvent e) {
        if (!cfg.getBoolean("prevent.interact.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.interact-frame-armor-stand")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.interact-frame-armor-stand.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.interact-frame-armor-stand.survival") && isSurvival(p)) {
            if (e.getRightClicked() instanceof ArmorStand || e.getRightClicked() instanceof ItemFrame) {
                e.setCancelled(true);

                p.sendMessage(Utils.format(cfg.getString("prevent.interact.message")));
            }
        }

        if (cfg.getBoolean("prevent.interact-frame-armor-stand.creative") && isCreative(p)) {
            if (e.getRightClicked() instanceof ArmorStand || e.getRightClicked() instanceof ItemFrame) {
                e.setCancelled(true);

                p.sendMessage(Utils.format(cfg.getString("prevent.interact-frame-armor-stand.message")));
            }
        }
    }

    @EventHandler
    public void onExpPickup(final PlayerExpChangeEvent e) {
        if (!cfg.getBoolean("prevent.exp-pickup.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.exp-pickup")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.exp-pickup.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.exp-pickup.survival") && isSurvival(p)) {
            e.setAmount(0);
        }

        if (cfg.getBoolean("prevent.exp-pickup.creative") && isCreative(p)) {
            e.setAmount(0);
        }
    }

    @EventHandler
    public void onFish(final PlayerFishEvent e) {
        if(!cfg.getBoolean("prevent.fishing.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.fishing")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.fishing.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.fishing.survival") && isSurvival(p)) {
            if(cfg.getBoolean("prevent.fishing.only-players")) {
                if(e.getCaught() instanceof Player) {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        }

        if (cfg.getBoolean("prevent.fishing.creative") && isCreative(p)) {
            if(cfg.getBoolean("prevent.fishing.only-players")) {
                if(e.getCaught() instanceof Player) {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent e) {
        if(!cfg.getBoolean("prevent.damage.enabled")) {
            return;
        }

        if (!(e.getDamager() instanceof final Player p)) {
            return;
        }

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.damage")) {
            return;
        }

        final World w = p.getWorld();

        final List<String> worlds = cfg.getStringList("prevent.damage.worlds");

        if (!worlds.isEmpty() && worlds.stream()
                .noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if(cfg.getBoolean("prevent.damage.survival") && isSurvival(p)) {
            checkDamage(e, p);
        }

        if(cfg.getBoolean("prevent.damage.creative") && isCreative(p)) {
            checkDamage(e, p);
        }
    }

    private void checkDamage(EntityDamageByEntityEvent e, Player p) {
        if(cfg.getBoolean("prevent.damage.animals") && e.getEntity() instanceof Animals) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.damage.message")));
        }

        if(cfg.getBoolean("prevent.damage.mobs") && e.getEntity() instanceof Monster) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.damage.message")));
        }

        if(cfg.getBoolean("prevent.damage.players") && e.getEntity() instanceof Player) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.damage.message")));
        }
    }

    private boolean hasBypass(HumanEntity p) {
        return !enabled || p.hasPermission("safecreative.bypass");
    }

    private boolean isCreative(HumanEntity p) {
        return p.getGameMode().equals(GameMode.CREATIVE);
    }
    
    private boolean isSurvival(HumanEntity p) {
        return p.getGameMode().equals(GameMode.SURVIVAL);
    }
}
