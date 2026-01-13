package me.ppgome.critterGuard.utility;

import org.bukkit.entity.*;

public class CritterUtils {

    /**
     * Checks if an entity is mountable in the eyes of the plugin.
     *
     * @param entity The entity being checked
     * @return True if it can be mounted, false if not
     */
    public static boolean isMountableEntity(Entity entity) {
        return entity instanceof AbstractHorse || entity instanceof HappyGhast || entity instanceof Strider;
    }

    /**
     * Checks if an entity is considered a pet.
     *
     * @param entity The entity being checked
     * @return True if it is considered a pet, false if not
     */
     static boolean isPetEntity(Entity entity) {
        return entity instanceof Wolf || entity instanceof Cat || entity instanceof Parrot;
    }

    /**
     * Checks if an entity can be tamed by the plugin.
     *
     * @param entity The entity being checked
     * @return True if it can be tamed, false if not
     */
    public static boolean canHandleTaming(Entity entity) {
        return isMountableEntity(entity) || isPetEntity(entity);
    }

    /**
     * Checks if an entity is a camel.
     *
     * @param entity The entity being checked.
     * @return True if it is, false if not.
     */
    public static boolean isCamel(Entity entity) {
        return entity instanceof Camel;
    }

    /**
     * Checks if an entity is a happy ghast.
     *
     * @param entity The entity being checked.
     * @return True if it is, false if not.
     */
    public static boolean isHappyGhast(Entity entity) {
        return entity instanceof HappyGhast;
    }

    /**
     * Checks if an entity is a multi-seat mount.
     *
     * @param entity The entity being checked.
     * @return True if it is, false if not.
     */
    public static boolean isMultiSeatMount(Entity entity) {
        return isCamel(entity) || isHappyGhast(entity);
    }

}
