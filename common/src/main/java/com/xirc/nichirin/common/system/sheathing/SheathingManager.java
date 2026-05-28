package com.xirc.nichirin.common.system.sheathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.DrawSlashAttack;
import com.xirc.nichirin.common.attack.moves.KatanaCheckAttack;
import com.xirc.nichirin.common.attack.moves.KatanaOverheadAttack;
import com.xirc.nichirin.common.attack.moves.KatanaThrustAttack;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SheathingManager {
    public static final int NORMAL_TRANSITION_TICKS = 10;
    public static final int QUICK_SHEATHE_TICKS = 1;
    public static final int GLOBAL_COOLDOWN_TICKS = 80;
    public static final int SLOT_COOLDOWN_TICKS = 35;
    public static final float QUICK_SHEATHE_STAMINA_COST = 12.0f;
    public static final float DRAW_ATTACK_STAMINA_COST = 10.0f;

    private static final Map<UUID, PlayerSheathData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_SYNC = new ConcurrentHashMap<>();

    public static PlayerSheathData get(Player player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), id -> new PlayerSheathData());
    }

    public static void cleanupPlayer(Player player) {
        restoreStoredSwords(player);
        PLAYER_DATA.remove(player.getUUID());
        LAST_SYNC.remove(player.getUUID());
    }

    public static void clearAll() {
        PLAYER_DATA.clear();
        LAST_SYNC.clear();
    }

    public static void tick(Player player) {
        if (player.level().isClientSide) return;
        PlayerSheathData data = get(player);
        if (data.getGlobalCooldownTicks() > 0) {
            data.setGlobalCooldownTicks(data.getGlobalCooldownTicks() - 1);
        }

        for (SheathSlotData slot : data.getSlots()) {
            boolean hasHotbarSword = hasHotbarSword(player, slot);
            boolean hasStoredSword = slot.hasStoredSword();
            if (!slot.isEnabled() || (!hasHotbarSword && !hasStoredSword)) {
                if (!slot.hasStoredSword()) {
                    slot.setState(SheathState.EMPTY);
                }
                slot.setChargeTicks(0);
                continue;
            }

            if (slot.getCooldownTicks() > 0) {
                slot.setCooldownTicks(slot.getCooldownTicks() - 1);
                if (slot.getCooldownTicks() == 0 && slot.getState() == SheathState.COOLDOWN) {
                    slot.setState(SheathState.DRAWN);
                }
            }

            if (data.getChargingSlot() == slot.getPosition()) {
                slot.setChargeTicks(slot.getChargeTicks() + 1);
                if (slot.getChargeTicks() % 10 == 0) {
                    feedback(player, chargeMessage(slot), 0xFFE0A6, true);
                }
            }

            if (slot.getState() == SheathState.EMPTY) {
                slot.setState(SheathState.DRAWN);
            }

            if (slot.getState() == SheathState.DRAWN && slot.hasStoredSword()) {
                slot.setState(SheathState.SHEATHED);
            }

            if (slot.getState() == SheathState.SHEATHING || slot.getState() == SheathState.UNSHEATHING) {
                if (player.hasEffect(NichirinEffectRegistry.STUNNED.get()) || player.hurtTime > 0) {
                    slot.setState(slot.getState() == SheathState.UNSHEATHING ? SheathState.SHEATHED : SheathState.DRAWN);
                    slot.setTransitionTicks(0);
                    data.setChargingSlot(null);
                    continue;
                }

                slot.setTransitionTicks(slot.getTransitionTicks() - 1);
                if (slot.getTransitionTicks() <= 0) {
                    if (slot.getState() == SheathState.SHEATHING) {
                        slot.setState(SheathState.SHEATHED);
                    } else {
                        finishUnsheathe(player, data, slot);
                    }
                }
            }
        }
        syncIfChanged(player, data);
    }

    public static void handleInput(ServerPlayer player, SheathInputAction action, boolean shiftDown) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            feedback(player, "Cannot sheathe while stunned!", 0xFF5555, false);
            return;
        }
        if (player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
            feedback(player, "Cannot sheathe while blocking!", 0xFF5555, false);
            return;
        }

        PlayerSheathData data = get(player);
        if (action == SheathInputAction.PRESS) {
            SheathSlotData drawn = getSelectedLinkedSlot(player, data);
            if (drawn != null && drawn.getState() == SheathState.DRAWN) {
                startSheathe(player, data, drawn, shiftDown);
                return;
            }

            SheathSlotData sheathed = firstReadySheathedSlot(player, data);
            if (sheathed != null) {
                data.setChargingSlot(sheathed.getPosition());
                sheathed.setChargeTicks(0);
                feedback(player, "Quickdraw ready: " + sheathed.getPosition().getDisplayName(), 0xFFE0A6, true);
            } else if (data.getGlobalCooldownTicks() > 0) {
                feedback(player, "Quickdraw cooldown (" + formatSeconds(data.getGlobalCooldownTicks()) + "s)", 0xFF5555, false);
            } else {
                feedback(player, "No sheathed katana ready", 0xFF5555, false);
            }
            return;
        }

        SheathPosition charging = data.getChargingSlot();
        if (charging == null) return;
        SheathSlotData slot = data.getSlot(charging);
        data.setChargingSlot(null);
        startUnsheathe(player, data, slot);
    }

    public static boolean isSelectedKatanaSheathed(Player player) {
        PlayerSheathData data = get(player);
        SheathSlotData slot = getSelectedLinkedSlot(player, data);
        return slot != null
                && (slot.getState() == SheathState.SHEATHED
                || slot.getState() == SheathState.SHEATHING
                || slot.getState() == SheathState.UNSHEATHING);
    }

    public static void updateSlot(ServerPlayer player, SheathPosition position, boolean enabled, int hotbarSlot, int priority,
                                  UnsheatheAttackType tapAttack, UnsheatheAttackType holdAttack, boolean visible) {
        PlayerSheathData data = get(player);
        SheathSlotData slot = data.getSlot(position);
        for (SheathSlotData other : data.getSlots()) {
            if (other != slot && other.getLinkedHotbarSlot() == hotbarSlot) {
                other.setEnabled(false);
            }
        }
        slot.setEnabled(enabled);
        slot.setLinkedHotbarSlot(hotbarSlot);
        slot.setPriority(priority);
        slot.setTapAttack(tapAttack);
        slot.setHoldAttack(holdAttack);
        slot.setVisible(visible);
    }

    private static void startSheathe(ServerPlayer player, PlayerSheathData data, SheathSlotData slot, boolean quick) {
        ItemStack sword = player.getInventory().getItem(slot.getLinkedHotbarSlot());
        if (!(sword.getItem() instanceof SimpleKatana)) {
            feedback(player, "No katana in Hotbar " + (slot.getLinkedHotbarSlot() + 1), 0xFF5555, false);
            return;
        }
        if (slot.hasStoredSword()) {
            feedback(player, slot.getPosition().getDisplayName() + " already stores a katana", 0xFF5555, false);
            return;
        }
        if (quick && !StaminaManager.consume(player, QUICK_SHEATHE_STAMINA_COST)) {
            feedback(player, "Not enough stamina to quick sheathe!", 0xFF5555, false);
            return;
        }

        slot.setStoredSword(sword.copy());
        player.getInventory().setItem(slot.getLinkedHotbarSlot(), ItemStack.EMPTY);
        slot.setState(SheathState.SHEATHING);
        slot.setTransitionTicks(quick ? QUICK_SHEATHE_TICKS : NORMAL_TRANSITION_TICKS);
        NichirinPacketRegistry.broadcastPlayerAnimation(player, new PlayerAnimationPacket(player.getId(), quick ? "sheathing.quick_sheathe" : "sheathing.sheathe"));
        feedback(player, quick ? "Quick Sheathe: " + slot.getPosition().getDisplayName() : "Sheathing: " + slot.getPosition().getDisplayName(),
                quick ? 0x55FFFF : 0xFFE0A6, quick);
        if (quick) {
            data.setGlobalCooldownTicks(GLOBAL_COOLDOWN_TICKS);
            MoveExecutor.sendCooldownDisplay(player, "Unsheathe", GLOBAL_COOLDOWN_TICKS);
        }
    }

    private static void startUnsheathe(ServerPlayer player, PlayerSheathData data, SheathSlotData slot) {
        if (slot == null || slot.getState() != SheathState.SHEATHED) return;
        if (data.getGlobalCooldownTicks() > 0) {
            feedback(player, "Quickdraw cooldown (" + formatSeconds(data.getGlobalCooldownTicks()) + "s)", 0xFF5555, false);
            return;
        }
        if (slot.getCooldownTicks() > 0) {
            feedback(player, slot.getPosition().getDisplayName() + " cooldown (" + formatSeconds(slot.getCooldownTicks()) + "s)", 0xFF5555, false);
            return;
        }
        if (!slot.hasStoredSword()) {
            feedback(player, "No katana stored in " + slot.getPosition().getDisplayName(), 0xFF5555, false);
            return;
        }
        if (!player.getInventory().getItem(slot.getLinkedHotbarSlot()).isEmpty()) {
            feedback(player, "Hotbar " + (slot.getLinkedHotbarSlot() + 1) + " must be empty to draw!", 0xFF5555, false);
            return;
        }
        if (!StaminaManager.consume(player, DRAW_ATTACK_STAMINA_COST)) {
            feedback(player, "Not enough stamina to quickdraw!", 0xFF5555, false);
            return;
        }

        slot.setState(SheathState.UNSHEATHING);
        slot.setTransitionTicks(NORMAL_TRANSITION_TICKS);
        player.getInventory().selected = slot.getLinkedHotbarSlot();
        feedback(player, chargeMessage(slot), 0xFFE0A6, true);
        NichirinPacketRegistry.broadcastPlayerAnimation(player, new PlayerAnimationPacket(player.getId(),
                slot.getChargeTicks() >= 12 ? "sheathing.quick_draw_charged" : "sheathing.quick_draw"));
    }

    private static void finishUnsheathe(Player player, PlayerSheathData data, SheathSlotData slot) {
        if (slot.hasStoredSword() && player.getInventory().getItem(slot.getLinkedHotbarSlot()).isEmpty()) {
            player.getInventory().setItem(slot.getLinkedHotbarSlot(), slot.getStoredSword());
            slot.setStoredSword(ItemStack.EMPTY);
            player.getInventory().selected = slot.getLinkedHotbarSlot();
        }
        slot.setState(SheathState.DRAWN);
        data.setGlobalCooldownTicks(GLOBAL_COOLDOWN_TICKS);
        slot.setCooldownTicks(SLOT_COOLDOWN_TICKS);
        MoveExecutor.sendCooldownDisplay(player, "Unsheathe", GLOBAL_COOLDOWN_TICKS);

        UnsheatheAttackType attackType = chooseAttack(player, slot);
        feedback(player, "Quickdraw: " + attackType.getDisplayName(), 0x55FF55, true);
        executeDrawAttack(player, attackType, slot.getChargeTicks());
        slot.setChargeTicks(0);
    }

    private static UnsheatheAttackType chooseAttack(Player player, SheathSlotData slot) {
        if (slot.getChargeTicks() >= 12) return slot.getHoldAttack();
        if (player.isSprinting()) return UnsheatheAttackType.SPRINTING_DRAW_DASH;
        if (player.isShiftKeyDown()) return UnsheatheAttackType.CROUCHING_LOW_DRAW;
        if (!player.onGround()) return UnsheatheAttackType.AERIAL_DRAW_SLASH;
        return slot.getTapAttack();
    }

    private static void executeDrawAttack(Player player, UnsheatheAttackType type, int chargeTicks) {
        Object attack = switch (type) {
            case SPRINTING_DRAW_DASH -> KatanaThrustAttack.createDefault();
            case CROUCHING_LOW_DRAW -> KatanaCheckAttack.createDefault();
            case OVERHEAD_BACKDRAW, AERIAL_DRAW_SLASH -> KatanaOverheadAttack.createDefault();
            case CHARGED_DRAW_SLASH -> DrawSlashAttack.create(8.0f + Math.min(chargeTicks, 40) * 0.08f, 3.2f, 0.9f, 14, SLOT_COOLDOWN_TICKS, false);
            case DUAL_CROSS_SLASH -> DrawSlashAttack.create(9.0f, 3.0f, 1.0f, 16, SLOT_COOLDOWN_TICKS, false);
            default -> DrawSlashAttack.create(6.0f, 2.8f, 0.55f, 10, SLOT_COOLDOWN_TICKS, false);
        };
        MoveExecutor.executeAttackWithInfo(player, attack, type.getDisplayName(), SLOT_COOLDOWN_TICKS);
        if (player instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer, new PlayerAnimationPacket(player.getId(), "sheathing.draw_slash"));
        }
    }

    public static void applyClientSync(Player player, SheathPosition position, boolean enabled, int hotbarSlot,
                                       int priority, SheathState state, int cooldownTicks, boolean visible) {
        SheathSlotData slot = get(player).getSlot(position);
        slot.setEnabled(enabled);
        slot.setLinkedHotbarSlot(hotbarSlot);
        slot.setPriority(priority);
        slot.setState(state);
        slot.setCooldownTicks(cooldownTicks);
        slot.setVisible(visible);
    }

    private static void syncIfChanged(Player player, PlayerSheathData data) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        String snapshot = snapshot(data);
        if (snapshot.equals(LAST_SYNC.get(player.getUUID()))) return;
        LAST_SYNC.put(player.getUUID(), snapshot);
        NichirinPacketRegistry.sendSheathSync(serverPlayer, data);
    }

    private static String snapshot(PlayerSheathData data) {
        StringBuilder builder = new StringBuilder();
        builder.append(data.getGlobalCooldownTicks()).append('|');
        for (SheathSlotData slot : data.getSlots()) {
            builder.append(slot.isEnabled()).append(',')
                    .append(slot.getLinkedHotbarSlot()).append(',')
                    .append(slot.getPriority()).append(',')
                    .append(slot.getState()).append(',')
                    .append(slot.getCooldownTicks()).append(',')
                    .append(slot.isVisible()).append(',')
                    .append(slot.getStoredSword().getDescriptionId()).append(',')
                    .append(slot.getStoredSword().getCount()).append(';');
        }
        return builder.toString();
    }

    private static SheathSlotData firstReadySheathedSlot(Player player, PlayerSheathData data) {
        for (SheathSlotData slot : data.getSlotsByPriority()) {
            if (slot.hasStoredSword() && slot.getState() == SheathState.SHEATHED && slot.getCooldownTicks() == 0) {
                return slot;
            }
        }
        return null;
    }

    private static SheathSlotData getSelectedLinkedSlot(Player player, PlayerSheathData data) {
        int selected = player.getInventory().selected;
        for (SheathSlotData slot : data.getSlots()) {
            if (slot.isEnabled() && slot.getLinkedHotbarSlot() == selected
                    && (hasHotbarSword(player, slot) || slot.hasStoredSword())) {
                return slot;
            }
        }
        return null;
    }

    private static boolean hasHotbarSword(Player player, SheathSlotData slot) {
        ItemStack stack = player.getInventory().getItem(slot.getLinkedHotbarSlot());
        return stack.getItem() instanceof SimpleKatana;
    }

    private static void restoreStoredSwords(Player player) {
        PlayerSheathData data = PLAYER_DATA.get(player.getUUID());
        if (data == null) return;
        for (SheathSlotData slot : data.getSlots()) {
            if (!slot.hasStoredSword()) continue;
            if (player.getInventory().getItem(slot.getLinkedHotbarSlot()).isEmpty()) {
                player.getInventory().setItem(slot.getLinkedHotbarSlot(), slot.getStoredSword());
            } else {
                player.getInventory().placeItemBackInInventory(slot.getStoredSword());
            }
            slot.setStoredSword(ItemStack.EMPTY);
            slot.setState(SheathState.DRAWN);
        }
    }

    private static void feedback(Player player, String text, int color, boolean bold) {
        player.displayClientMessage(Component.literal(text)
                .withStyle(style -> {
                    var styled = style.withColor(color);
                    return bold ? styled.withBold(true) : styled;
                }), true);
    }

    private static String chargeMessage(SheathSlotData slot) {
        int percent = Math.min(100, Math.round(slot.getChargeTicks() / 40.0f * 100.0f));
        return percent >= 30
                ? "Quickdraw charge: " + percent + "% (" + slot.getHoldAttack().getDisplayName() + ")"
                : "Quickdraw: " + slot.getTapAttack().getDisplayName();
    }

    private static String formatSeconds(int ticks) {
        return String.format("%.1f", ticks / 20.0f);
    }
}
