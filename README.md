![CritterGuard](https://i.ibb.co/Dfcydww6/critterguard.png)

**A comprehensive pet protection plugin for Minecraft servers**

CritterGuard protects your beloved pets from death and theft by introducing an advanced taming system that extends beyond vanilla Minecraft limitations. Now you can tame and protect creatures like camels and happy ghasts!

> [!TIP]
> **Quick Command:** Use `/critter` or the shorter `/cg` for all commands.

> [!IMPORTANT]
> All releases are published on the [Nerd.Nu repository](https://github.com/NerdNu/CritterGuard)

---

## Features

### Entity Protection
CritterGuard protects these creatures from theft and harm:

**Mountable Creatures:**
- Horses, Mules, Donkeys
- Zombie Horses, Skeleton Horses
- Llamas, Camels
- Happy Ghasts
- Striders

**Companion Creatures:**
- Wolves, Cats, Parrots

**Protection Rules:**
- **Companions** (wolves, cats, parrots) can only be killed by their owners
- **Mounts** can be killed by anything, but only while being ridden

### Access Control System
Control who can use your mounts with two permission levels:

#### Passenger Access
- **Applies to:** Multi-seat mounts only (camels, happy ghasts)
- **Allows:** Riding as a passenger when owner/full-access user is driving
- **Cannot:** Take control or board without driver present

#### Full Access
- **Applies to:** All mounts
- **Allows:** Complete control of the mount
- **Includes:** Access to mount storage (chests on donkeys/llamas)

### Notifications
Stay informed about your mounts! You'll be notified when someone with full access:
- Mounts your creature
- Dismounts your creature
- Lets your mount die while riding

*Toggle with: `/critter notifications [on/off]`*

### Smart Seat Swapping
For multi-seat mounts, when the driver dismounts:
- Next person with full access automatically becomes driver
- If no one else has full access, everyone is dismounted
- Ensures uninterrupted journeys!

### Disguise Saddles *(LibsDisguises Required)*
Transform your mount's appearance with special saddles!

**Setup:** Add lore to any saddle starting with `Disguise: ` followed by the mob type
**Example:** `Disguise: WOLF`

> [!NOTE]  
> Multi-seat disguised mounts will stack players on top of each other due to technical limitations.

---

## Taming Guide

| Creature | Taming Method |
|----------|---------------|
| **Horses, Mules, Donkeys** | Ride until they accept you (vanilla method) |
| **Llamas** | Click with a lead |
| **Camels, Happy Ghasts** | Simply ride them once |
| **Wolves, Cats, Parrots** | Use their respective taming items (vanilla method) |

---

## Commands

### Player Commands

#### `/critter list [entityType] [player] [page]`
View your tamed creatures with optional filters.

**Parameters:** (all optional, use in order)
- `entityType`: Filter by type (`all`, `camel`, `cat`, `donkey`, `happy_ghast`, `horse`, `llama`, `mule`, `parrot`, `wolf`)
- `player`: View another player's creatures
- `page`: Navigate multiple pages (number > 0)

#### `/critter gps <identifier>`
Get coordinates and direction to your creature.

#### `/critter access <add/remove> <passenger/full> <player>`
Manage access permissions for your mounts.
*After running, click the mount to apply changes.*

#### `/critter untame`
Remove taming from a creature you own.
*Click the creature after running the command.*

#### `/critter notifications [on/off]`
Toggle mount notifications on or off.

#### `/critter showdisguise [on/off]`
Toggle viewing the disguise of the mount you're controlling.
>[!IMPORTANT]
> With this toggled on, you won't be able to move your mount. Toggle it off to regain control.

### Staff Commands

#### `/critter tame <player>`
Tame any untamed creature to a specific player.
*Click the creature after running the command.*

#### `/critter tp <player> <identifier>`
Teleport to another player's creature.

#### `/critter tphere <player> <identifier>`
Teleport another player's creature to you.

#### `/critter reload`
Reload the plugin configuration.

---

## Understanding Identifiers

The `<identifier>` parameter accepts:
1. **Creature's name** (from nametags)
2. **Creature's UUID**
3. **Index number** (from `/critter list`)

**Smart Matching:** Partial matches work! For a dog named "Fido", `/critter gps fi` works fine.

> [!WARNING]
> If multiple creatures match your partial input, the one with the lowest index number is selected.

---

## Complete Command Reference

| Command                                                  | Description                                           | Permission                                                |
|----------------------------------------------------------|-------------------------------------------------------|-----------------------------------------------------------|
| `/critter access <add/remove> <full/passenger> <player>` | Grant or remove mount access                          | `critterguard.access`                                     |
| `/critter list [entityType] [player] [page]`             | List creatures by criteria                            | `critterguard.list`                                       |
| `/critter gps <identifier>`                              | Locate and point to your creature                     | `critterguard.gps`                                        |
| `/critter notifications [on/off]`                        | Toggle mount notifications                            | `critterguard.notifications`                              |
| `/critter showdisguise [on/off]`                         | Toggles the disguise of the entity you're controlling | `critterguard.showdisguise`                                |
| `/critter untame`                                        | Untame a creature                                     | `critterguard.untame.own`<br>`critterguard.untame.others` |
| `/critter tame <player>`                                 | Tame creature to specified player                     | `critterguard.tame`                                       |
| `/critter tp <player> <identifier>`                      | Teleport to another's creature                        | `critterguard.tp`                                         |
| `/critter tphere <player> <identifier>`                  | Teleport creature to you                              | `critterguard.tphere`                                     |
| `/critter reload`                                        | Reload configuration                                  | `critterguard.reload`                                     |

> [!IMPORTANT]
> **For `/critter tame` permissions:** Also grant `critterguard.tame` for auto-complete functionality.
