package com.xirc.nichirin.common.attack.moveset;

/**
 * Thunder Breathing moveset implementation
 * Currently empty - moves will be added later
 */
public class ThunderBreathingMoveset extends AbstractMoveset {

    public ThunderBreathingMoveset() {
        super("thunder_breathing", "Thunder Breathing", createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withSpeedMultiplier(1.3f); // Emphasizes speed

        // No moves added yet - will be implemented later
        // Example of how moves will be added:
        // .withMove(MoveInputType.BASIC,
        //     new MoveBuilder(() -> new ThunderFirstForm())
        //         .withAnimation("nichirin:thunder_first_form")
        //         .withIcon("nichirin:textures/gui/moves/thunder_first_form.png")
        //         .withDamage(8.0f)
        //         .withTiming(30, 5, 10)
        // )
    }
}