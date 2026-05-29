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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SheathingManager {
    public static final int NORMAL_TRANSITION_TICKS = 10;
    public static final int GLOBAL_COOLDOWN_TICKS = 80;
    public static final int SLOT_COOLDOWN_TICKS = 35;
    public static final float DRAW_ATTACK_STAMINA_COST = 10.0f;

    private static final Map<UUID, PlayerSheathData> PLAYER_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_SYNC = new ConcurrentHashMap<>();

    public static PlayerSheathData get(Player player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), id -> new PlayerSheathData());
    }

    public static void cleanupPlayer(Player player) {
        PLAYER_DATA.remove(player.getUUID());
        LAST_SYNC.remove(player.getUUID());
    }

    public static void clearAll() {
        PLAYER_DATA.clear();
        LAST_SYNC.clear();
    }

    public static CompoundTag savePlayerData(Player player) {
        CompoundTag tag = new CompoundTag();
        PlayerSheathData data = get(player);
        tag.putInt("GlobalCooldown", data.getGlobalCooldownTicks());
        if (data.getChargingSlot() != null) {
            tag.putString("ArmedSlot", data.getChargingSlot().name());
        }

        CompoundTag slotsTag = new CompoundTag();
        for (SheathSlotData slot : data.getSlots()) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putBoolean("Enabled", slot.isEnabled());
            slotTag.putInt("HotbarSlot", slot.getLinkedHotbarSlot());
            slotTag.putInt("Priority", slot.getPriority());
            slotTag.putString("State", slot.getState().name());
            slotTag.putString("TapAttack", slot.getTapAttack().name());
            slotTag.putInt("CooldownTicks", slot.getCooldownTicks());
            slotTag.putBoolean("Visible", slot.isVisible());
            if (slot.hasStoredSword()) {
                slotTag.put("StoredSword", slot.getStoredSword().save(new CompoundTag()));
            }
            slotsTag.put(slot.getPosition().name(), slotTag);
        }
        tag.put("Slots", slotsTag);
        return tag;
    }

    public static void loadPlayerData(ServerPlayer player, CompoundTag tag) {
        PlayerSheathData data = get(player);
        data.setGlobalCooldownTicks(tag.getInt("GlobalCooldown"));
        data.setChargingSlot(null);
        if (tag.contains("ArmedSlot")) {
            try {
                data.setChargingSlot(SheathPosition.valueOf(tag.getString("ArmedSlot")));
            } catch (IllegalArgumentException ignored) {
                data.setChargingSlot(null);
            }
        }

        CompoundTag slotsTag = tag.getCompound("Slots");
        for (SheathPosition position : SheathPosition.values()) {
            if (!slotsTag.contains(position.name())) continue;
            SheathSlotData slot = data.getSlot(position);
            CompoundTag slotTag = slotsTag.getCompound(position.name());
            slot.setEnabled(slotTag.getBoolean("Enabled"));
            slot.setLinkedHotbarSlot(slotTag.getInt("HotbarSlot"));
            slot.setPriority(slotTag.getInt("Priority"));
            slot.setCooldownTicks(slotTag.getInt("CooldownTicks"));
            slot.setVisible(!slotTag.contains("Visible") || slotTag.getBoolean("Visible"));
            if (slotTag.contains("TapAttack")) {
                try {
                    slot.setTapAttack(UnsheatheAttackType.valueOf(slotTag.getString("TapAttack")));
                } catch (IllegalArgumentException ignored) {
                    slot.setTapAttack(UnsheatheAttackType.QUICKDRAW_SLASH);
                }
            }
            if (slotTag.contains("StoredSword")) {
                slot.setStoredSword(ItemStack.of(slotTag.getCompound("StoredSword")));
            } else {
                slot.setStoredSword(ItemStack.EMPTY);
            }

            if (slot.hasStoredSword()) {
                slot.setState(SheathState.SHEATHED);
                slot.setTransitionTicks(0);
            } else if (slotTag.contains("State")) {
                try {
                    slot.setState(SheathState.valueOf(slotTag.getString("State")));
                } catch (IllegalArgumentException ignored) {
                    slot.setState(SheathState.EMPTY);
                }
            }
        }
    }

    public static void syncPlayer(ServerPlayer player) {
        PlayerSheathData data = get(player);
        LAST_SYNC.remove(player.getUUID());
        NichirinPacketRegistry.sendSheathSync(player, data);
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
                continue;
            }

            if (slot.getCooldownTicks() > 0) {
                slot.setCooldownTicks(slot.getCooldownTicks() - 1);
                if (slot.getCooldownTicks() == 0 && slot.getState() == SheathState.COOLDOWN) {
                    slot.setState(SheathState.DRAWN);
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

    public static void handleInput(ServerPlayer player, SheathInputAction action, boolean shiftDown, int heldTicks) {
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
                startSheathe(player, data, drawn);
                return;
            }

            SheathSlotData sheathed = getSelectedLinkedSlot(player, data);
            if (sheathed != null && sheathed.getState() != SheathState.SHEATHED) {
                sheathed = null;
            }
            if (sheathed != null) {
                data.setChargingSlot(sheathed.getPosition());
            } else if (data.getGlobalCooldownTicks() > 0) {
                feedback(player, "Quickdraw cooldown (" + formatSeconds(data.getGlobalCooldownTicks()) + "s)", 0xFF5555, false);
            } else {
                feedback(player, "Select the original empty hotbar slot to quickdraw", 0xFF5555, false);
            }
            return;
        }

        SheathPosition charging = data.getChargingSlot();
        if (charging != null) {
            SheathSlotData slot = data.getSlot(charging);
            data.setChargingSlot(null);
            startUnsheathe(player, data, slot);
            return;
        }

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
        slot.setVisible(visible);
    }

    private static void startSheathe(ServerPlayer player, PlayerSheathData data, SheathSlotData slot) {
        ItemStack sword = player.getInventory().getItem(slot.getLinkedHotbarSlot());
        if (!(sword.getItem() instanceof SimpleKatana)) {
            feedback(player, "No katana in Hotbar " + (slot.getLinkedHotbarSlot() + 1), 0xFF5555, false);
            return;
        }
        if (slot.hasStoredSword()) {
            feedback(player, slot.getPosition().getDisplayName() + " already stores a katana", 0xFF5555, false);
            return;
        }

        slot.setStoredSword(sword.copy());
        player.getInventory().setItem(slot.getLinkedHotbarSlot(), ItemStack.EMPTY);
        slot.setState(SheathState.SHEATHING);
        slot.setTransitionTicks(NORMAL_TRANSITION_TICKS);
        NichirinPacketRegistry.broadcastPlayerAnimation(player, new PlayerAnimationPacket(player.getId(), "sheathing.sheathe"));
        feedback(player, "Sheathing: " + slot.getPosition().getDisplayName(), 0xFFE0A6, false);
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

        player.getInventory().selected = slot.getLinkedHotbarSlot();
        finishUnsheathe(player, data, slot);
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

        executeConditionalOrDefault(player, slot);
    }

    private static final String COND_SPRINTING = "Sprinting Draw Dash";
    private static final String COND_CROUCHING = "Crouching Low Draw";
    private static final String COND_AERIAL = "Aerial Draw Slash";

    private static void executeConditionalOrDefault(Player player, SheathSlotData slot) {
        Object attack;
        String name;
        String anim;

        if (player.isSprinting()) {
            attack = KatanaThrustAttack.createDefault();
            name = COND_SPRINTING;
            anim = "sword.thrust";
        } else if (player.isShiftKeyDown()) {
            attack = DrawSlashAttack.lowDraw();
            name = COND_CROUCHING;
            anim = "sword.check";
        } else if (!player.onGround()) {
            attack = DrawSlashAttack.aerialDraw();
            name = COND_AERIAL;
            anim = "sword.vertical";
        } else {
            UnsheatheAttackType type = slot.getTapAttack();
            attack = switch (type) {
                case OVERHEAD_BACKDRAW -> KatanaOverheadAttack.createDefault();
                case REVERSE_DRAW_SLASH -> DrawSlashAttack.reverseDraw();
                case DIAGONAL_BACKDRAW -> DrawSlashAttack.diagonalDraw();
                case DUAL_CROSS_SLASH -> DrawSlashAttack.dualCross();
                case QUICKDRAW_SLASH -> DrawSlashAttack.quickdraw();
            };
            name = type.getDisplayName();
            anim = type == UnsheatheAttackType.OVERHEAD_BACKDRAW ? "sword.vertical" : "sheathing.quick_draw";
        }

        feedback(player, "Quickdraw: " + name, 0x55FF55, true);
        MoveExecutor.executeAttackWithInfo(player, attack, name, SLOT_COOLDOWN_TICKS);
        if (player instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer, new PlayerAnimationPacket(player.getId(), anim));
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

    private static void feedback(Player player, String text, int color, boolean bold) {
        player.displayClientMessage(Component.literal(text)
                .withStyle(style -> {
                    var styled = style.withColor(color);
                    return bold ? styled.withBold(true) : styled;
                }), true);
    }

    private static String formatSeconds(int ticks) {
        return String.format("%.1f", ticks / 20.0f);
    }
}
