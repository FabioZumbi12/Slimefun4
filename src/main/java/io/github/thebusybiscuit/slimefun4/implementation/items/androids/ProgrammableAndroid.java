package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.cscorelib2.chat.ChatColors;
import io.github.thebusybiscuit.cscorelib2.inventory.ItemUtils;
import io.github.thebusybiscuit.cscorelib2.item.CustomItem;
import io.github.thebusybiscuit.cscorelib2.skull.SkullBlock;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import io.github.thebusybiscuit.slimefun4.utils.PatternUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.AdvancedMenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Lists.SlimefunItems;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunBlockHandler;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.UnregisterReason;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

public abstract class ProgrammableAndroid extends Android implements InventoryBlock, RecipeDisplayItem {

    private static final int[] border = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 18, 24, 25, 26, 27, 33, 35, 36, 42, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53 };
    private static final int[] border_out = { 10, 11, 12, 13, 14, 19, 23, 28, 32, 37, 38, 39, 40, 41 };

    protected final List<BlockFace> directions = Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
    protected final Set<MachineFuel> recipes = new HashSet<>();
    protected final String texture;

    public ProgrammableAndroid(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        this.texture = item.getSkullTexture().orElse(null);
        registerDefaultFuelTypes();

        new BlockMenuPreset(getID(), "Programmable Android") {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                boolean open = BlockStorage.getLocationInfo(b.getLocation(), "owner").equals(p.getUniqueId().toString()) || p.hasPermission("slimefun.android.bypass");

                if (!open) {
                    SlimefunPlugin.getLocal().sendMessage(p, "inventory.no-access", true);
                }

                return open;
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                menu.replaceExistingItem(15, new CustomItem(SlimefunUtils.getCustomHead("e01c7b5726178974b3b3a01b42a590e54366026fd43808f2a787648843a7f5a"), "&aStart/Continue"));
                menu.addMenuClickHandler(15, (p, slot, item, action) -> {
                    SlimefunPlugin.getLocal().sendMessage(p, "android.started", true);
                    BlockStorage.addBlockInfo(b, "paused", "false");
                    p.closeInventory();
                    return false;
                });

                menu.replaceExistingItem(17, new CustomItem(SlimefunUtils.getCustomHead("16139fd1c5654e56e9e4e2c8be7eb2bd5b499d633616663feee99b74352ad64"), "&4Pause"));
                menu.addMenuClickHandler(17, (p, slot, item, action) -> {
                    BlockStorage.addBlockInfo(b, "paused", "true");
                    SlimefunPlugin.getLocal().sendMessage(p, "android.stopped", true);
                    return false;
                });

                menu.replaceExistingItem(16, new CustomItem(SlimefunUtils.getCustomHead("d78f2b7e5e75639ea7fb796c35d364c4df28b4243e66b76277aadcd6261337"), "&bMemory Core", "", "&8\u21E8 &7Click to open the Script Editor"));
                menu.addMenuClickHandler(16, (p, slot, item, action) -> {
                    BlockStorage.addBlockInfo(b, "paused", "true");
                    SlimefunPlugin.getLocal().sendMessage(p, "android.stopped", true);
                    openScriptEditor(p, b);
                    return false;
                });
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };

        registerBlockHandler(getID(), new SlimefunBlockHandler() {

            @Override
            public void onPlace(Player p, Block b, SlimefunItem item) {
                BlockStorage.addBlockInfo(b, "owner", p.getUniqueId().toString());
                BlockStorage.addBlockInfo(b, "script", "START-TURN_LEFT-REPEAT");
                BlockStorage.addBlockInfo(b, "index", "0");
                BlockStorage.addBlockInfo(b, "fuel", "0");
                BlockStorage.addBlockInfo(b, "rotation", p.getFacing().getOppositeFace().toString());
                BlockStorage.addBlockInfo(b, "paused", "true");
                b.setType(Material.PLAYER_HEAD);

                Rotatable blockData = (Rotatable) b.getBlockData();
                blockData.setRotation(p.getFacing());
                b.setBlockData(blockData);
            }

            @Override
            public boolean onBreak(Player p, Block b, SlimefunItem item, UnregisterReason reason) {
                boolean allow = reason == UnregisterReason.PLAYER_BREAK && (BlockStorage.getLocationInfo(b.getLocation(), "owner").equals(p.getUniqueId().toString()) || p.hasPermission("slimefun.android.bypass"));

                if (allow) {
                    BlockMenu inv = BlockStorage.getInventory(b);

                    if (inv != null) {
                        if (inv.getItemInSlot(43) != null) {
                            b.getWorld().dropItemNaturally(b.getLocation(), inv.getItemInSlot(43));
                            inv.replaceExistingItem(43, null);
                        }

                        for (int slot : getOutputSlots()) {
                            if (inv.getItemInSlot(slot) != null) {
                                b.getWorld().dropItemNaturally(b.getLocation(), inv.getItemInSlot(slot));
                                inv.replaceExistingItem(slot, null);
                            }
                        }
                    }
                }

                return allow;
            }
        });
    }

    private void registerDefaultFuelTypes() {
        if (getTier() == 1) {
            registerFuel(new MachineFuel(800, new ItemStack(Material.COAL_BLOCK)));
            registerFuel(new MachineFuel(45, new ItemStack(Material.BLAZE_ROD)));

            // Coal & Charcoal
            registerFuel(new MachineFuel(8, new ItemStack(Material.COAL)));
            registerFuel(new MachineFuel(8, new ItemStack(Material.CHARCOAL)));

            // Logs
            for (Material mat : Tag.LOGS.getValues()) {
                registerFuel(new MachineFuel(2, new ItemStack(mat)));
            }

            // Wooden Planks
            for (Material mat : Tag.PLANKS.getValues()) {
                registerFuel(new MachineFuel(1, new ItemStack(mat)));
            }
        }
        else if (getTier() == 2) {
            registerFuel(new MachineFuel(100, new ItemStack(Material.LAVA_BUCKET)));
            registerFuel(new MachineFuel(200, SlimefunItems.BUCKET_OF_OIL));
            registerFuel(new MachineFuel(500, SlimefunItems.BUCKET_OF_FUEL));
        }
        else {
            registerFuel(new MachineFuel(2500, SlimefunItems.URANIUM));
            registerFuel(new MachineFuel(1200, SlimefunItems.NEPTUNIUM));
            registerFuel(new MachineFuel(3000, SlimefunItems.BOOSTED_URANIUM));
        }
    }

    @Override
    public String getLabelLocalPath() {
        return "guide.tooltips.recipes.generator";
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> list = new ArrayList<>();

        for (MachineFuel fuel : recipes) {
            ItemStack item = fuel.getInput().clone();
            ItemMeta im = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColors.color("&8\u21E8 &7Lasts " + NumberUtils.getTimeLeft(fuel.getTicks() / 2)));
            im.setLore(lore);
            item.setItemMeta(im);
            list.add(item);
        }

        return list;
    }

    @Override
    public int[] getInputSlots() {
        return new int[0];
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] { 20, 21, 22, 29, 30, 31 };
    }

    public abstract AndroidType getAndroidType();

    public abstract float getFuelEfficiency();

    public abstract int getTier();

    protected void tick(Block b) {
        if (b.getType() != Material.PLAYER_HEAD) {
            // The Android was destroyed or moved.
            return;
        }

        if ("false".equals(BlockStorage.getLocationInfo(b.getLocation(), "paused"))) {
            BlockMenu menu = BlockStorage.getInventory(b);
            float fuel = Float.parseFloat(BlockStorage.getLocationInfo(b.getLocation(), "fuel"));

            if (fuel < 0.001) {
                consumeFuel(b, menu);
            }
            else {
                String[] script = PatternUtils.DASH.split(BlockStorage.getLocationInfo(b.getLocation(), "script"));

                int index = Integer.parseInt(BlockStorage.getLocationInfo(b.getLocation(), "index")) + 1;
                if (index >= script.length) index = 0;

                boolean refresh = true;
                BlockStorage.addBlockInfo(b, "fuel", String.valueOf(fuel - 1));
                ScriptAction part = ScriptAction.valueOf(script[index]);

                if (getAndroidType().isType(part.getRequiredType())) {
                    BlockFace face = BlockFace.valueOf(BlockStorage.getLocationInfo(b.getLocation(), "rotation"));
                    double damage = getTier() < 2 ? 20D : 4D * getTier();

                    switch (part) {
                    case GO_DOWN:
                        move(b, face, b.getRelative(BlockFace.DOWN));
                        break;
                    case GO_FORWARD:
                        move(b, face, b.getRelative(face));
                        break;
                    case GO_UP:
                        move(b, face, b.getRelative(BlockFace.UP));
                        break;
                    case REPEAT:
                        BlockStorage.addBlockInfo(b, "index", String.valueOf(0));
                        break;
                    case TURN_LEFT:
                        int indexLeft = directions.indexOf(BlockFace.valueOf(BlockStorage.getLocationInfo(b.getLocation(), "rotation"))) - 1;
                        if (indexLeft < 0) indexLeft = directions.size() - 1;

                        Rotatable rotatableLeft = (Rotatable) b.getBlockData();
                        rotatableLeft.setRotation(directions.get(indexLeft));
                        b.setBlockData(rotatableLeft);
                        BlockStorage.addBlockInfo(b, "rotation", directions.get(indexLeft).toString());

                        break;
                    case TURN_RIGHT:
                        int indexRight = directions.indexOf(BlockFace.valueOf(BlockStorage.getLocationInfo(b.getLocation(), "rotation"))) + 1;
                        if (indexRight == directions.size()) indexRight = 0;

                        Rotatable rotatableRight = (Rotatable) b.getBlockData();
                        rotatableRight.setRotation(directions.get(indexRight));
                        b.setBlockData(rotatableRight);
                        BlockStorage.addBlockInfo(b, "rotation", directions.get(indexRight).toString());

                        break;
                    case DIG_FORWARD:
                        mine(b, menu, b.getRelative(face));
                        break;
                    case DIG_UP:
                        mine(b, menu, b.getRelative(BlockFace.UP));
                        break;
                    case DIG_DOWN:
                        mine(b, menu, b.getRelative(BlockFace.DOWN));
                        break;
                    case CATCH_FISH:
                        fish(b, menu);
                        break;
                    case MOVE_AND_DIG_FORWARD:
                        movedig(b, menu, face, b.getRelative(face));
                        break;
                    case MOVE_AND_DIG_UP:
                        movedig(b, menu, face, b.getRelative(BlockFace.UP));
                        break;
                    case MOVE_AND_DIG_DOWN:
                        movedig(b, menu, face, b.getRelative(BlockFace.DOWN));
                        break;
                    case INTERFACE_ITEMS:
                        depositItems(menu, b.getRelative(face));
                        break;
                    case INTERFACE_FUEL:
                        refuel(menu, b.getRelative(face));
                        break;
                    case FARM_FORWARD:
                        farm(menu, b.getRelative(face));
                        break;
                    case FARM_DOWN:
                        farm(menu, b.getRelative(BlockFace.DOWN));
                        break;
                    case FARM_EXOTIC_FORWARD:
                        exoticFarm(menu, b.getRelative(face));
                        break;
                    case FARM_EXOTIC_DOWN:
                        exoticFarm(menu, b.getRelative(BlockFace.DOWN));
                        break;
                    case CHOP_TREE:
                        refresh = chopTree(b, menu, face);
                        break;
                    case ATTACK_MOBS_ANIMALS:
                        killEntities(b, damage, e -> true);
                        break;
                    case ATTACK_MOBS:
                        killEntities(b, damage, e -> e instanceof Monster);
                        break;
                    case ATTACK_ANIMALS:
                        killEntities(b, damage, e -> e instanceof Animals);
                        break;
                    case ATTACK_ANIMALS_ADULT:
                        killEntities(b, damage, e -> e instanceof Animals && e instanceof org.bukkit.entity.Ageable && ((org.bukkit.entity.Ageable) e).isAdult());
                        break;
                    default:
                        break;
                    }
                }
                if (refresh) {
                    BlockStorage.addBlockInfo(b, "index", String.valueOf(index));
                }
            }
        }
    }

    private void depositItems(BlockMenu menu, Block facedBlock) {
        if (facedBlock.getType() == Material.DISPENSER && BlockStorage.check(facedBlock, "ANDROID_INTERFACE_ITEMS")) {
            Dispenser d = (Dispenser) facedBlock.getState();

            for (int slot : getOutputSlots()) {
                ItemStack stack = menu.getItemInSlot(slot);

                if (stack != null) {
                    Optional<ItemStack> optional = d.getInventory().addItem(stack).values().stream().findFirst();

                    if (optional.isPresent()) {
                        menu.replaceExistingItem(slot, optional.get());
                    }
                    else {
                        menu.replaceExistingItem(slot, null);
                    }
                }
            }
        }
    }

    private void consumeFuel(Block b, BlockMenu menu) {
        ItemStack item = menu.getItemInSlot(43);

        if (item != null) {
            for (MachineFuel recipe : recipes) {
                if (SlimefunUtils.isItemSimilar(item, recipe.getInput(), true)) {
                    menu.consumeItem(43);

                    if (getTier() == 2) {
                        menu.pushItem(new ItemStack(Material.BUCKET), getOutputSlots());
                    }

                    BlockStorage.addBlockInfo(b, "fuel", String.valueOf((int) (recipe.getTicks() * this.getFuelEfficiency())));
                    break;
                }
            }
        }
    }

    private void refuel(BlockMenu menu, Block facedBlock) {
        if (facedBlock.getType() == Material.DISPENSER && BlockStorage.check(facedBlock, "ANDROID_INTERFACE_FUEL")) {
            Dispenser d = (Dispenser) facedBlock.getState();

            for (int slot = 0; slot < 9; slot++) {
                ItemStack item = d.getInventory().getItem(slot);

                if (item != null) {
                    insertFuel(menu, d.getInventory(), slot, menu.getItemInSlot(43), item);
                }
            }
        }
    }

    private boolean insertFuel(BlockMenu menu, Inventory dispenser, int slot, ItemStack currentFuel, ItemStack newFuel) {
        if (currentFuel == null) {
            menu.replaceExistingItem(43, newFuel);
            dispenser.setItem(slot, null);
            return true;
        }
        else if (SlimefunUtils.isItemSimilar(newFuel, currentFuel, true)) {
            int rest = newFuel.getType().getMaxStackSize() - currentFuel.getAmount();

            if (rest > 0) {
                int amount = newFuel.getAmount() > rest ? rest : newFuel.getAmount();
                menu.replaceExistingItem(43, new CustomItem(newFuel, currentFuel.getAmount() + amount));
                ItemUtils.consumeItem(newFuel, amount, false);
            }

            return true;
        }

        return false;
    }

    protected void move(Block b, BlockFace face, Block block) {
        if (block.getY() > 0 && block.getY() < block.getWorld().getMaxHeight() && (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR)) {
            block.setType(Material.PLAYER_HEAD);
            Rotatable blockData = (Rotatable) block.getBlockData();
            blockData.setRotation(face.getOppositeFace());
            block.setBlockData(blockData);

            SkullBlock.setFromBase64(block, texture);

            b.setType(Material.AIR);
            BlockStorage.moveBlockInfo(b.getLocation(), block.getLocation());
        }
    }

    private void constructMenu(BlockMenuPreset preset) {
        for (int i : border) {
            preset.addItem(i, new CustomItem(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), " "), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : border_out) {
            preset.addItem(i, new CustomItem(new ItemStack(Material.ORANGE_STAINED_GLASS_PANE), " "), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : getOutputSlots()) {
            preset.addMenuClickHandler(i, new AdvancedMenuClickHandler() {

                @Override
                public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                    return false;
                }

                @Override
                public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                    return cursor == null || cursor.getType() == null || cursor.getType() == Material.AIR;
                }
            });
        }

        ItemStack generator = SlimefunUtils.getCustomHead("9343ce58da54c79924a2c9331cfc417fe8ccbbea9be45a7ac85860a6c730");

        if (getTier() == 1) {
            preset.addItem(34, new CustomItem(generator, "&8\u21E9 &cFuel Input &8\u21E9", "", "&rThis Android runs on solid Fuel", "&re.g. Coal, Wood, etc..."), ChestMenuUtils.getEmptyClickHandler());
        }
        else if (getTier() == 2) {
            preset.addItem(34, new CustomItem(generator, "&8\u21E9 &cFuel Input &8\u21E9", "", "&rThis Android runs on liquid Fuel", "&re.g. Lava, Oil, Fuel, etc..."), ChestMenuUtils.getEmptyClickHandler());
        }
        else {
            preset.addItem(34, new CustomItem(generator, "&8\u21E9 &cFuel Input &8\u21E9", "", "&rThis Android runs on radioactive Fuel", "&re.g. Uranium, Neptunium or Boosted Uranium"), ChestMenuUtils.getEmptyClickHandler());
        }
    }

    public void addItems(Block b, ItemStack... items) {
        BlockMenu inv = BlockStorage.getInventory(b);

        for (ItemStack item : items) {
            inv.pushItem(item, getOutputSlots());
        }
    }

    public void registerFuel(MachineFuel fuel) {
        this.recipes.add(fuel);
    }

}
