package me.ppgome.critterGuard.commands;

import me.ppgome.critterGuard.*;
import me.ppgome.critterGuard.database.SavedAnimal;
import me.ppgome.critterGuard.database.SavedMount;
import me.ppgome.critterGuard.database.SavedPet;
import me.ppgome.critterGuard.utility.CritterTamingHandler;
import me.ppgome.critterGuard.utility.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents the command used to show a player their (or another player, if specified)'s list of critters.
 */
public class ListSubCommand implements SubCommandHandler {

    /**
     * The instance of the plugin.
     */
    private final CritterGuard plugin;
    /**
     * The instance of the configuration class.
     */
    private CGConfig config;
    /**
     * The instance of the plugin's cache.
     */
    private CritterCache critterCache;
    /**
     * The list of entity types players can filter by.
     */
    private List<String> entityTypes;
    /**
     * The instance of the plugin's CritterTamingHandler.
     */
    private CritterTamingHandler tamingHandler;

    /**
     * Constructor for ListSubCommand.
     * Initializes the command with the plugin instance.
     *
     * @param plugin The instance of the CritterGuard plugin.
     */
    public ListSubCommand(CritterGuard plugin) {
        this.plugin = plugin;
        this.config = plugin.getCGConfig();
        this.critterCache = plugin.getCritterCache();
        this.tamingHandler = new CritterTamingHandler(plugin);
        entityTypes = new ArrayList<>(List.of("all", "horse", "mule", "donkey", "camel", "llama",
                "happy_ghast", "strider", "wolf", "cat", "parrot"));
    }

    // /critter list [entityType] [playerName] [pageNumber]
    @Override
    public void execute(CommandSender sender, String[] args) {

        if(!(sender instanceof Player player)) return; // Ensure the command sender is a player
        boolean isEntityType;
        boolean isPageNumber;

        switch(args.length) {
            // No arguments provided. Return all critters for the player.
            case 0:
                getData(player, "all", critterCache.getPlayerMeta(player.getUniqueId()), 1);
                break;
            // One argument provided. Could be an entity type, page, or a player name.
            case 1:
                if(entityTypes.contains(args[0].toLowerCase())) {
                    getData(player, args[0].toLowerCase(), critterCache.getPlayerMeta(player.getUniqueId()), 1);
                } else if(args[0].matches("\\d+")) {
                    getData(player, "all", critterCache.getPlayerMeta(player.getUniqueId()),
                            Integer.parseInt(args[0]));
                } else {
                    getDataAsync(player, "all", args[0], 1);
                }
                break;
                // Two arguments provided. Could be entity type and player name, entity type and page, or player name and page.
            case 2:
                isEntityType = entityTypes.contains(args[0].toLowerCase());
                isPageNumber = args[1].matches("\\d+");
                if(isEntityType && isPageNumber) {
                    getData(player, args[0].toLowerCase(), critterCache.getPlayerMeta(player.getUniqueId()),
                            Integer.parseInt(args[1]));
                } else if(isEntityType) {
                    getDataAsync(player, args[0].toLowerCase(), args[1], 1);
                } else if(isPageNumber) {
                    getDataAsync(player, "all", args[0], Integer.parseInt(args[1]));
                } else {
                    player.sendMessage(getUsage());
                }
                break;
                // Three arguments provided. Should be entity type, player name, and page number.
            case 3:
                isPageNumber = args[2].matches("\\d+");
                isEntityType = entityTypes.contains(args[0].toLowerCase());
                if(isEntityType && isPageNumber) {
                    getDataAsync(player, args[0].toLowerCase(), args[1], Integer.parseInt(args[2]));
                } else {
                    player.sendMessage(getUsage());
                }
                break;
            default:
                player.sendMessage(getUsage());
        }
    }

    /**
     * Retrieves player metadata and outputs the list of animals for the specified page.
     *
     * @param player      The player who executed the command.
     * @param entityType  The type of entity to filter by (e.g., "horse", "cat").
     * @param playerMeta  The PlayerMeta data for the specified player.
     * @param page        The page number to retrieve.
     */
    private void getData(Player player, String entityType, PlayerMeta playerMeta, int page) {
        if(playerMeta != null && !playerMeta.getOwnedList().isEmpty()){
            List<SavedAnimal> filteredList = getFilteredList(playerMeta.getOwnedList(), entityType);
            int totalPages = 0;
            if(!filteredList.isEmpty()) totalPages = (int) Math.ceil((double) filteredList.size() / 5); // Calculate total pages based on 5 items per page
            outputList(getAnimalsPerPage(filteredList, page, totalPages), player, player.getName(), entityType, page,
                    totalPages);
        } else {
            player.sendMessage(config.LIST_DOES_NOT_EXIST_OR_OWN);
        }
    }

    /**
     * Asynchronously retrieves player metadata and outputs the list of animals for the specified page.
     *
     * @param player        The player who executed the command.
     * @param entityType    The type of entity to filter by (e.g., "horse", "cat").
     * @param searchedName  The name of the player being searched for.
     * @param page          The page number to retrieve.
     */
    private void getDataAsync(Player player, String entityType, String searchedName, int page) {
        getPlayerMeta(searchedName).thenAccept(meta -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (meta != null && !meta.getOwnedList().isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                            List<SavedAnimal> filteredList = getFilteredList(meta.getOwnedList(), entityType);
                            int totalPages = 0;
                            if(!filteredList.isEmpty()) totalPages = (int) Math.ceil((double) filteredList.size() / 5); // Calculate total pages based on 5 items per page
                            outputList(getAnimalsPerPage(filteredList, page, totalPages), player, searchedName,
                                    entityType, page, totalPages);
                        });
            } else {
                player.sendMessage(config.LIST_DOES_NOT_EXIST_OR_OWN);
            }
        }));
    }

    /**
     * Applies the entity filter so that only the specified entity type appears in the list.
     *
     * @param animalList The unfiltered list of all critters
     * @param entityType The entity type to filter by
     * @return The filtered list of critters
     */
    private List<SavedAnimal> getFilteredList(List<SavedAnimal> animalList, String entityType) {
        List<SavedAnimal> filteredList = new ArrayList<>();

        if (entityType.equals("all")) {
            filteredList.addAll(animalList); // If "all", return the entire list
        } else {
            for (SavedAnimal animal : animalList) {
                if (animal.getEntityType().equalsIgnoreCase(entityType)) {
                    filteredList.add(animal); // Filter by entity type
                }
            }
        }

        return filteredList;

    }

    /**
     * Retrieves a sublist of animals for the specified page.
     * Each page contains a maximum of 5 animals.
     *
     * @param filteredList The list of filtered animals.
     * @param page         The page number to retrieve.
     * @return A sublist of animals for the specified page.
     */
    private List<SavedAnimal> getAnimalsPerPage(List<SavedAnimal> filteredList, int page, int totalPages) {

        if(filteredList.isEmpty()) return filteredList;

        if(page < 1) page = 1; // Ensure page is at least 1
        if (page > totalPages) page = totalPages; // Ensure page does not exceed total pages

        int startIndex = (page - 1) * 5; // Calculate start index for the page
        int endIndex = Math.min(startIndex + 5, filteredList.size()); // Calculate end index, ensuring it does not exceed the list size
        return filteredList.subList(startIndex, endIndex); // Return the sublist for the current page
    }

    /**
     * Outputs the list of animals to the player.
     *
     * @param animalList The list of animals to output.
     * @param player     The player who executed the command.
     */
    private void outputList(List<SavedAnimal> animalList, Player player, String searchedName, String entityType, int page, int totalPages) {
        if (animalList.isEmpty()) {
            player.sendMessage(config.LIST_NO_MATCH);
            return;
        }
        Component message = Component.text("----==== ", NamedTextColor.GRAY)
                .append(Component.text("Critter List", NamedTextColor.GOLD))
                .append(Component.text(" ====----", NamedTextColor.GRAY))
                .appendNewline();
        for (SavedAnimal animal : animalList) {
            String Uuid = animal.getEntityUuid().toString().substring(0, 8);
            String name = animal.getEntityName() != null ? animal.getEntityName() : "No name";
            String type = animal.getEntityType();
            String color = animal.getColor() != null ? animal.getColor() : "N/A";
            Location location;
            Entity entity = Bukkit.getEntity(animal.getEntityUuid());
            if(entity != null) {
                location = entity.getLocation();
            } else {
                location = animal.getLastLocation();
            }

            // Despite doing the check earlier, a NPE is still triggered on the name variable in some instances.
            if(name == null || location == null) continue;
            name = validateName(animal, name);

            message = message.appendNewline()
                    .append(Component.text("[" + animal.getIndex() + "] ", NamedTextColor.GOLD))
                    .append(Component.text(Uuid + "... ", NamedTextColor.GRAY))
                    .append(Component.text(name, NamedTextColor.GREEN)).appendNewline()
                    .append(Component.text("     Type: " + type, NamedTextColor.BLUE))
                    .append(Component.text(" Color: " + MessageUtils.capitalizeFirstLetter(color),
                            NamedTextColor.YELLOW)).appendNewline()
                    .append(Component.text("     "))
                    .append(MessageUtils.locationBuilder(location, NamedTextColor.RED));
        }
        if(page > totalPages) page = totalPages;
        message = message.appendNewline()
                .append(Component.text("----==== ", NamedTextColor.GRAY));

        // If the player is on any page but the first, display the back arrow.
        if(page > 1) {
            message = message.append(Component.text(" ◀ ", NamedTextColor.YELLOW)
                    .hoverEvent(HoverEvent.showText(Component.text("Last page")))
                    .clickEvent(ClickEvent.runCommand("cg list " + entityType + " " + searchedName + " " + (page - 1))));
        }

        message = message.append(Component.text("Page " + page + "/" + totalPages, NamedTextColor.GOLD));

        // If the player is on any page but the last, display the next arrow.
        if(page < totalPages) {
            message = message.append(Component.text(" ▶ ", NamedTextColor.YELLOW)
                    .hoverEvent(HoverEvent.showText(Component.text("Next page")))
                    .clickEvent(ClickEvent.runCommand("cg list " + entityType + " " + searchedName + " " + (page + 1))));
        }

        message = message.append(Component.text(" ====----", NamedTextColor.GRAY));
        player.sendMessage(message);
    }

    /**
     * Asynchronously fetches a player's playermeta by converting username to UUID using Bukkit's OfflinePlayer object.
     *
     * @param playerName The name of the player whose playermeta is being fetched
     * @return The playermeta instance if the player has played before
     */
    private CompletableFuture<PlayerMeta> getPlayerMeta(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            if (!player.hasPlayedBefore()) {
                return null; // Player does not exist
            }
            return critterCache.getPlayerMeta(player.getUniqueId());
        });
    }

    /*
     Due to a bug early on in the life of CritterGuard, some names have the stringified version of a
     TextComponent object. This method converts them back.
     */
    private String validateName(SavedAnimal savedAnimal, String name) {
        Pattern pattern = Pattern.compile("content=\"(.*?)\"");
        Matcher matcher = pattern.matcher(name);

        if(matcher.find()) {
            String fixedName = matcher.group(1);
            savedAnimal.setEntityName(fixedName);
            if(savedAnimal instanceof SavedMount savedMount) {
                plugin.getSavedMountTable().save(savedMount);
            } else {
                plugin.getSavedPetTable().save((SavedPet) savedAnimal);
            }
            return fixedName;
        }
        return name;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> entityTypes;
            case 2 -> null;
            default -> List.of();
        };
    }

    @Override
    public String getCommandName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Display a list of critters for a specific entity type and player.";
    }

    @Override
    public Component getUsage() {
        return MessageUtils.miniMessageDeserialize(config.PREFIX + " " + getStringUsage());
    }

    @Override
    public String getStringUsage() {
        return "<red>Usage: /critter list [entityType] [playerName] [pageNumber]</red>";
    }

    @Override
    public String getPermission() {
        return "critterguard.list";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }
}
