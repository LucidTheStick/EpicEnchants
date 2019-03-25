package com.songoda.epicenchants.effect;

import com.songoda.epicenchants.enums.EventType;
import com.songoda.epicenchants.enums.TriggerType;
import com.songoda.epicenchants.objects.Condition;
import com.songoda.epicenchants.objects.LeveledModifier;
import com.songoda.epicenchants.utils.single.GeneralUtils;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static com.songoda.epicenchants.effect.EffectExecutor.Who.OPPONENT;
import static com.songoda.epicenchants.effect.EffectExecutor.Who.WEARER;

public abstract class EffectExecutor {
    @Getter private final ConfigurationSection section;
    @Getter private final TriggerType triggerType;
    private final Condition condition;

    public EffectExecutor(ConfigurationSection section) {
        this.section = section;
        this.triggerType = TriggerType.valueOf(section.getString("trigger"));
        this.condition = Condition.of(section.getString("condition"));
    }

    public void testAndRun(@NotNull Player wearer, @Nullable LivingEntity opponent, int level, TriggerType type, Event event, EventType eventType) {
        if (type != triggerType) {
            return;
        }

        if (section.isString("chance") && !GeneralUtils.chance(LeveledModifier.of(section.getString("chance")).get(level, 100))) {
            return;
        }

        if (!condition.get(wearer, opponent, level, false)) {
            return;
        }

        if (this instanceof EffectEventExecutor) {
            ((EffectEventExecutor) this).execute(wearer, opponent, level, event, eventType);
            return;
        }

        execute(wearer, opponent, level, eventType);
    }

    public abstract void execute(@NotNull Player wearer, @Nullable LivingEntity opponent, int level, EventType eventType);

    public Who who() {
        if (section.isString("who")) {
            if (section.getString("who").equalsIgnoreCase("wearer")) return WEARER;
            else if (section.getString("who").equalsIgnoreCase("opponent")) return OPPONENT;
        }
        return WEARER;
    }

    public LeveledModifier getAmount() {
        return LeveledModifier.of(section.getString("amount"));
    }

    public void consume(Consumer<LivingEntity> playerConsumer, Player wearer, @Nullable LivingEntity opponent) {
        if (triggerType == TriggerType.HELD_ITEM || triggerType == TriggerType.STATIC_EFFECT) {
            playerConsumer.accept(wearer);
            return;
        }

        switch (who()) {
            case WEARER:
                playerConsumer.accept(wearer);
                break;
            case OPPONENT:
                if (opponent != null)
                    playerConsumer.accept(opponent);
        }
    }

    public enum Who {
        WEARER, OPPONENT
    }
}
