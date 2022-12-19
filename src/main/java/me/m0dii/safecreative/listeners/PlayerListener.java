package me.m0dii.safecreative.listeners;

import me.m0dii.pllib.utils.InventoryUtils;
import me.m0dii.pllib.utils.TextUtils;
import me.m0dii.safecreative.SafeCreative;
import me.m0dii.safecreative.utils.Utils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PlayerListener implements Listener {
    private final SafeCreative plugin;
    private final FileConfiguration cfg;

    public PlayerListener(SafeCreative plugin) {
        this.plugin = plugin;

        this.cfg = plugin.getCfg();
    }

    @EventHandler
    public void onChangeGameMode(final PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();

        if (hasBypass(p)) {
            return;
        }

        GameMode mode = e.getNewGameMode();

        Inventory inv = p.getInventory();

        if (mode.equals(GameMode.CREATIVE) && cfg.getBoolean("prevent.switch-creative-with-items")) {
            if (!InventoryUtils.isEmpty(inv)) {
                p.sendMessage(Utils.format(cfg.getString("messages.inventory-not-empty")));

                e.setCancelled(true);
            }
        }

        if(cfg.getBoolean("clear-effects-on-switch")) {
            e.getPlayer().getActivePotionEffects().clear();
        }

        if (mode.equals(GameMode.SURVIVAL)) {
            if(cfg.getBoolean("prevent.switch-survival-with-items")) {
                if (!InventoryUtils.isEmpty(inv)) {
                    p.sendMessage(Utils.format(cfg.getString("messages.inventory-not-empty")));

                    e.setCancelled(true);
                }
            }

            removeCreativeItems(inv);
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

            if(lore == null) {
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

        if(hasBypass(p)) {
            return;
        }

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(final BlockBreakEvent e) {
        if(!cfg.getBoolean("prevent.breaking-containers.enabled")) {
            return;
        }

        Player p = e.getPlayer();

        if (hasBypass(p) || p.hasPermission("safecreative.bypass.container-break")) {
            return;
        }

        Material m = e.getBlock().getType();

        if (!containerTypes.contains(m)) {
            return;
        }

        if(cfg.getBoolean("prevent.breaking-containers.survival")) {
            if (p.getGameMode().equals(GameMode.SURVIVAL)) {
                e.setCancelled(true);
            }
        }

        if(cfg.getBoolean("prevent.breaking-containers.creative")) {
            if (p.getGameMode().equals(GameMode.CREATIVE)) {
                e.setCancelled(true);
            }
        }

        if(!(e.getBlock().getState() instanceof Container cont)) {
            return;
        }

        if(cfg.getBoolean("prevent.breaking-containers.clear-contents-on-break")) {
            Inventory inv = cont.getInventory();

            inv.clear();
        }

        if(cfg.getBoolean("prevent.breaking-containers.set-to-air")) {
            e.getBlock().setType(Material.AIR);
        }
    }

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

    @EventHandler
    public void cancelPickup(final PlayerAttemptPickupItemEvent e) {
        if(!cfg.getBoolean("prevent.item-pickup.enabled")) {
            return;
        }

        Player p = e.getPlayer();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.item-pickup")) {
            return;
        }

        World w = p.getWorld();

        if(cfg.getStringList("prevent.item-pickup.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if(cfg.getBoolean("prevent.item-pickup.creative")) {
                e.setCancelled(true);
            }

            if(cfg.getBoolean("prevent.item-pickup.destroy")) {
                e.getItem().remove();
            }
        }

        if (!p.getGameMode().equals(GameMode.SURVIVAL)) {
            return;
        }

        if(!cfg.getBoolean("prevent.item-pickup.survival")) {
            return;
        }

        ItemStack item = e.getItem().getItemStack();

        List<Component> lore = item.lore();

        if(lore == null) {
            return;
        }

        for (Component comp : lore) {
            String text = TextUtils.stripColor(comp);

            if(cfg.getBoolean("prevent.item-pickup.tagged-items-only")) {
                if (text.equalsIgnoreCase("CREATIVE")) {
                    if(cfg.getBoolean("prevent.item-pickup.survival")) {
                        e.setCancelled(true);
                    }
                }
            } else {
                if(cfg.getBoolean("prevent.item-pickup.survival")) {
                    e.setCancelled(true);
                }
            }

            if(cfg.getBoolean("prevent.item-pickup.destroy")) {
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

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.potion-throw")) {
            return;
        }

        World w = p.getWorld();

        if (cfg.getStringList("prevent.potion-throw.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        Projectile proj = e.getEntity();

        if (!(proj instanceof ThrownPotion)) {
            return;
        }

        if(!cfg.getBoolean("prevent.potion-throw.enabled")) {
            return;
        }

        if(cfg.getBoolean("prevent.potion-throw.survival")) {
            if (p.getGameMode().equals(GameMode.SURVIVAL)) {
                if (p.getActiveItem() != null) {
                    e.setCancelled(true);

                    p.getActiveItem().setType(Material.AIR);

                    p.sendMessage(Utils.format(cfg.getString("prevent.potion-throw.message")));
                }
            }
        }

        if(cfg.getBoolean("prevent.potion-throw.creative")) {
            if (p.getGameMode().equals(GameMode.CREATIVE)) {
                if (p.getActiveItem() != null) {
                    e.setCancelled(true);

                    p.getActiveItem().setType(Material.AIR);

                    p.sendMessage(Utils.format(cfg.getString("prevent.potion-throw.message")));
                }
            }
        }
    }

    @EventHandler
    public void onDropItem(final PlayerDropItemEvent e) {
        if(!cfg.getBoolean("prevent.item-drop.enabled")) {
            return;
        }

        HumanEntity p = e.getPlayer();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.item-drop")) {
            return;
        }

        World w = p.getWorld();

        if (cfg.getStringList("prevent.item-drop.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if(cfg.getBoolean("prevent.item-drop.survival") && p.getGameMode().equals(GameMode.SURVIVAL)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.item-drop.message")));
        }

        if(cfg.getBoolean("prevent.item-drop.creative") && p.getGameMode().equals(GameMode.CREATIVE)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.item-drop.message")));
        }
    }

    @EventHandler
    public void destroyEntity(final EntitySpawnEvent e) {
        if(!cfg.getBoolean("prevent.entity-spawn.enabled")) {
            return;
        }

        final World w = e.getEntity().getWorld();

        if (cfg.getStringList("prevent.entity-spawn.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if(cfg.getBoolean("prevent.entity-spawn.entities.all")) {
            e.setCancelled(true);

            e.getEntity().remove();

            return;
        }

        List<String> entityTypeList = cfg.getStringList("prevent.entity-spawn.entities.list");

        if(cfg.getBoolean("prevent.entity-spawn.entities.blacklist")) {
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
        if(!cfg.getBoolean("restricted-items.enabled")) {
            return;
        }

        final HumanEntity p = e.getView().getPlayer();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.item-restriction")) {
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
        if(!cfg.getBoolean("prevent.opening-containers.enabled")) {
            return;
        }

        final HumanEntity p = e.getPlayer();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.container-open")) {
            return;
        }

        if (p.getGameMode().equals(GameMode.SURVIVAL)) {
            removeCreativeItems(p.getInventory());
        }

        final World w = p.getWorld();

        if(cfg.getStringList("prevent.opening-containers.worlds")
            .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        final InventoryType type = e.getInventory().getType();

        if (p.getGameMode().equals(GameMode.CREATIVE)) {
            if (!type.equals(InventoryType.CREATIVE)) {
                if(cfg.getBoolean("prevent.opening-containers.creative")) {
                    p.sendMessage(Utils.format(cfg.getString("prevent.opening-containers.message")));

                    e.setCancelled(true);
                }
            }
        }

        if (p.getGameMode().equals(GameMode.SURVIVAL)) {
            if(cfg.getBoolean("prevent.opening-containers.survival")) {
                p.sendMessage(Utils.format(cfg.getString("prevent.opening-containers.message")));

                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void portalCreate(final PortalCreateEvent e) {
        if(!cfg.getBoolean("prevent.portal-creation.enabled")) {
            return;
        }

        if(!(e.getEntity() instanceof Player p)) {
            return;
        }

        final World w = e.getWorld();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.portal-create")) {
            return;
        }

        if (cfg.getStringList("prevent.portal-creation.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if(cfg.getBoolean("prevent.portal-creation.survival") && p.getGameMode().equals(GameMode.SURVIVAL)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.portal-creation.message")));
        }

        if(cfg.getBoolean("prevent.portal-creation.creative") && p.getGameMode().equals(GameMode.CREATIVE)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.portal-creation.message")));
        }
    }

    @EventHandler
    public void addCreativeTag(final InventoryCreativeEvent e) {
        final HumanEntity p = e.getView().getPlayer();

        if(hasBypass(p)) {
            return;
        }

        final ItemStack cursor = e.getCursor();

        if (!cursor.getType().equals(Material.AIR)) {
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
    }

    @EventHandler
    public void onBowShoot(final EntityShootBowEvent e) {
        if(!cfg.getBoolean("prevent.bow-shoot.enabled")) {
            return;
        }

        if(!(e.getEntity() instanceof Player p)) {
            return;
        }

        final World w = p.getWorld();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.bow-shoot")) {
            return;
        }

        if (cfg.getStringList("prevent.bow-shoot.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if(cfg.getBoolean("prevent.bow-shoot.survival") && p.getGameMode().equals(GameMode.SURVIVAL)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.bow-shoot.message")));
        }

        if(cfg.getBoolean("prevent.bow-shoot.creative") && p.getGameMode().equals(GameMode.CREATIVE)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.bow-shoot.message")));
        }
    }

    @EventHandler
    public void onPotionDrink(final PlayerItemConsumeEvent e) {
        if(!cfg.getBoolean("prevent.potion-drink.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.potion-drink")) {
            return;
        }

        final World w = p.getWorld();

        if (cfg.getStringList("prevent.potion-drink.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if(cfg.getBoolean("prevent.potion-drink.survival") && p.getGameMode().equals(GameMode.SURVIVAL)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.potion-drink.message")));
        }

        if(cfg.getBoolean("prevent.potion-drink.creative") && p.getGameMode().equals(GameMode.CREATIVE)) {
            e.setCancelled(true);

            p.sendMessage(Utils.format(cfg.getString("prevent.potion-drink.message")));
        }
    }

    @EventHandler
    public void onCommand(final PlayerCommandPreprocessEvent e) {
        if(!cfg.getBoolean("prevent.command-execution.enabled")) {
            return;
        }

        final Player p = e.getPlayer();

        if(hasBypass(p) || p.hasPermission("safecreative.bypass.command-execution")) {
            return;
        }

        final  World w = p.getWorld();

        if (cfg.getStringList("prevent.command-execution.worlds")
                .stream().noneMatch(worldName -> worldName.equalsIgnoreCase(w.getName()))) {
            return;
        }

        if (cfg.getBoolean("prevent.command-execution.commands.all")) {
            if(cfg.getBoolean("prevent.command-execution.survival") && p.getGameMode().equals(GameMode.SURVIVAL)) {
                e.setCancelled(true);

                p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
            }

            if(cfg.getBoolean("prevent.command-execution.creative") && p.getGameMode().equals(GameMode.CREATIVE)) {
                e.setCancelled(true);

                p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
            }
        } else {
            final List<String> commands = cfg.getStringList("prevent.command-execution.commands.list");

            if(cfg.getBoolean("prevent.command-execution.commands.blacklist")) {
                if (cfg.getBoolean("prevent.command-execution.survival") && p.getGameMode().equals(GameMode.SURVIVAL)) {
                    if (commands.stream().anyMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }

                if(cfg.getBoolean("prevent.command-execution.creative") && p.getGameMode().equals(GameMode.CREATIVE)) {
                    if (commands.stream().anyMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }
            }
            else {
                if (cfg.getBoolean("prevent.command-execution.survival") && p.getGameMode().equals(GameMode.SURVIVAL)) {
                    if (commands.stream().noneMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }

                if(cfg.getBoolean("prevent.command-execution.creative") && p.getGameMode().equals(GameMode.CREATIVE)) {
                    if (commands.stream().noneMatch(command -> StringUtils.startsWithIgnoreCase(e.getMessage(), command))) {
                        e.setCancelled(true);

                        p.sendMessage(Utils.format(cfg.getString("prevent.command-execution.message")));
                    }
                }
            }
        }
    }

    private boolean hasBypass(HumanEntity p) {
        return p.hasPermission("safecreative.bypass");
    }
}
