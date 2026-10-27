package dev.scripted.forgotten.logic.bio;

/**
 * The instruction set of a Bio-Node: a mob whose behaviour has been replaced by compiled code.
 * Source code is written by laying items out in a Compiler Chest, one instruction per row; the
 * first item in a row is the opcode, the items after it are its arguments.
 */
public enum Opcode {
    /** compass + target: walk to the nearest target of that kind. */
    MOVE(true),
    /** any hoe or shears: harvest a mature crop next to the node and replant it. */
    HARVEST(true),
    /** hopper [+ item]: empty the node's buffer into an adjacent container, optionally only one item. */
    DEPOSIT(true),
    /** hopper minecart: pick up dropped items nearby into the buffer. */
    PICKUP(true),
    /** any sword: strike the nearest hostile in reach. */
    ATTACK(true),
    /** bow or crossbow: fire at the nearest hostile within sensor range. */
    SHOOT(true),
    /** clock: wait one second per clock in the stack. */
    WAIT(true),
    /** repeater: jump to the line given by the repeater stack size. */
    JUMP(false),
    /** comparator + condition [+ repeater]: if the condition holds, jump to the repeater's line. */
    IF(false),
    /** lead: follow the owner for a while. */
    FOLLOW(true),
    /** note block: make a sound. Debug output. */
    EMIT(false),
    /** barrier: stop. */
    HALT(false);

    private final boolean takesTime;

    Opcode(boolean takesTime) {
        this.takesTime = takesTime;
    }

    /** Whether executing this instruction lets game time pass. Loops without one are tight. */
    public boolean takesTime() {
        return takesTime;
    }

    public static Opcode forItem(String item) {
        if (item.endsWith("_hoe") || item.equals("minecraft:shears")) {
            return HARVEST;
        }
        if (item.endsWith("_sword")) {
            return ATTACK;
        }
        return switch (item) {
            case "minecraft:compass", "minecraft:recovery_compass" -> MOVE;
            case "minecraft:hopper" -> DEPOSIT;
            case "minecraft:hopper_minecart" -> PICKUP;
            case "minecraft:bow", "minecraft:crossbow" -> SHOOT;
            case "minecraft:clock" -> WAIT;
            case "minecraft:repeater" -> JUMP;
            case "minecraft:comparator" -> IF;
            case "minecraft:lead" -> FOLLOW;
            case "minecraft:note_block" -> EMIT;
            case "minecraft:barrier" -> HALT;
            default -> null;
        };
    }
}
