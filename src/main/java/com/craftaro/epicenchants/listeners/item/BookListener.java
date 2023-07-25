package com.craftaro.epicenchants.listeners.item;

import com.craftaro.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.craftaro.epicenchants.events.EnchantApplyEvent;
import com.craftaro.epicenchants.objects.Enchant;
import com.craftaro.epicenchants.objects.Group;
import com.craftaro.epicenchants.EpicEnchants;
import com.craftaro.epicenchants.enums.EnchantResult;
import com.craftaro.epicenchants.utils.Tuple;
import com.craftaro.epicenchants.utils.single.GeneralUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

import static com.craftaro.epicenchants.enums.EnchantResult.ALREADY_APPLIED;
import static com.craftaro.epicenchants.enums.EnchantResult.BROKEN_FAILURE;
import static com.craftaro.epicenchants.enums.EnchantResult.CONFLICT;
import static com.craftaro.epicenchants.enums.EnchantResult.MAXED_OUT;
import static java.util.concurrent.ThreadLocalRandom.current;

public class BookListener extends ItemListener {
    public BookListener(EpicEnchants instance) {
        super(instance);
    }

    @Override
    void onApply(InventoryClickEvent event, NBTItem cursor, NBTItem current) {
        if (!cursor.hasTag("book-item") || !cursor.getBoolean("book-item")) {
            return;
        }

        event.setCancelled(true);

        ItemStack toApply = event.getCurrentItem();
        Enchant enchant = this.instance.getEnchantManager().getValue(cursor.getString("enchant")).orElseThrow(() -> new IllegalStateException("Book without enchant!"));

        if (!enchant.getItemWhitelist().contains(XMaterial.matchXMaterial(current.getItem().getType()))) {
            return;
        }
        // get total amount of enchantments on item
        int currentEnchantmentTotal = this.instance.getEnchantUtils().getEnchants(toApply).size();
        int maxAllowedOverride = this.instance.getEnchantUtils().getMaximumEnchantsCanApply((Player) event.getWhoClicked());
        int maxAllowedApply = this.instance.getEnchantUtils().getMaximumEnchantsCanApplyItem(toApply, (Player) event.getWhoClicked());
        maxAllowedApply = Math.min(maxAllowedApply, maxAllowedOverride);
        // item is at max enchantments
        if (currentEnchantmentTotal >= maxAllowedApply) {
            this.instance.getLocale().getMessage("enchants.maxallowed").processPlaceholder("max_enchants", maxAllowedApply).sendPrefixedMessage(event.getWhoClicked());
            return;
        }

        int level = cursor.getInteger("level");
        int successRate = cursor.getInteger("success-rate");
        int destroyRate = cursor.getInteger("destroy-rate");

        EnchantApplyEvent enchantEvent = new EnchantApplyEvent(toApply, enchant, level, successRate, destroyRate);
        Bukkit.getPluginManager().callEvent(enchantEvent);

        if (enchantEvent.isCancelled()) {
            return;
        }

        Tuple<ItemStack, EnchantResult> result = this.instance.getEnchantUtils().apply(toApply, enchant, enchantEvent.getLevel(), enchantEvent.getSuccessRate(), enchantEvent.getDestroyRate());

        this.instance.getLocale().getMessage(GeneralUtils.getMessageFromResult(result.getRight()))
                .processPlaceholder("enchant", enchant.getIdentifier())
                .sendPrefixedMessage(event.getWhoClicked());

        if (result.getRight() == BROKEN_FAILURE) {
            event.getClickedInventory().clear(event.getSlot());
        }

        if (result.getRight() != CONFLICT && result.getRight() != MAXED_OUT && result.getRight() != ALREADY_APPLIED) {
            useItem(event);
        }

        event.getClickedInventory().setItem(event.getSlot(), result.getLeft());
    }

    @Override
    void onClick(PlayerInteractEvent event, NBTItem clicked) {
        if (!clicked.hasTag("mystery-book") || !clicked.getBoolean("mystery-book")) {
            return;
        }

        event.setCancelled(true);

        if (event.getItem().getAmount() != 1 && event.getPlayer().getInventory().firstEmpty() == -1) {
            return;
        }

        Group group = this.instance.getGroupManager().getValue(clicked.getString("group")).orElseThrow(() -> new IllegalStateException("Book without group!"));

        Optional<Enchant> enchant = this.instance.getEnchantManager().getRandomEnchant(group);

        if (!enchant.isPresent()) {
            throw new IllegalStateException("The " + group.getName() + " group does not have any enchants.");
        }

        int level = current().nextInt(enchant.get().getMaxLevel()) + 1;

        useItem(event);
        event.getPlayer().getInventory().addItem(enchant.get().getBook().get(enchant.get(), level));

        event.getPlayer().sendMessage(this.instance.getLocale().getMessage("book.discover")
                .processPlaceholder("group_name", group.getName())
                .processPlaceholder("group_color", group.getColor())
                .processPlaceholder("enchant_format", enchant.get().getFormat())
                .processPlaceholder("level", level)
                .getPrefixedMessage());
    }
}
