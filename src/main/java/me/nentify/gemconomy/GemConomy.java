package me.nentify.gemconomy;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;

@Plugin(id = GemConomy.PLUGIN_ID, name = GemConomy.PLUGIN_NAME, version = GemConomy.PLUGIN_VERSION)
public class GemConomy {

    public static final String PLUGIN_ID = "gemconomy";
    public static final String PLUGIN_NAME = "GemConomy";
    public static final String PLUGIN_VERSION = "0.0.1";

    public static GemConomy instance;

    @Inject
    public Logger logger;

    private EconomyService economyService;

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {
        logger.info("Starting " + PLUGIN_NAME + " v" + PLUGIN_VERSION);

        instance = this;
    }

    @Listener
    public void onPlayerInteract(InteractBlockEvent.Secondary.MainHand event, @Root Player player) {
        if (isInShop(player.getWorld(), player.getLocation().getPosition())) {
            if (player.getItemInHand(HandTypes.MAIN_HAND).isPresent()
                    && isGem(player.getItemInHand(HandTypes.MAIN_HAND).get())) {

                Optional<UniqueAccount> account = economyService.getOrCreateAccount(player.getUniqueId());
                if (account.isPresent()) {
                    TransactionResult result = account.get().deposit(economyService.getDefaultCurrency(),
                            BigDecimal.valueOf(100),
                            Cause.source(this).build());

                    if (result.getResult() == ResultType.SUCCESS) {
                        ItemStack gemStack = player.getItemInHand(HandTypes.MAIN_HAND).get();

                        player.sendMessage(Text.of("Current stack quantity: " + gemStack.getQuantity()));

                        gemStack.setQuantity(gemStack.getQuantity() - 1);

                        player.sendMessage(Text.of("Gem stack quantity - 1: " + gemStack.getQuantity()));

                        player.setItemInHand(HandTypes.MAIN_HAND, gemStack);

                        player.sendMessage(Text.of("Gem stack quantity after setting: " + gemStack.getQuantity()));

                        player.sendMessage(Text.of("Quantity after getting gemStack again: " + player.getItemInHand(HandTypes.MAIN_HAND).get().getQuantity()));

                        player.sendMessage(Text.of(TextColors.GREEN, "You have received $100 for your gem!"));

                        player.playSound(
                                SoundTypes.ENTITY_EXPERIENCE_ORB_PICKUP,
                                player.getLocation().getPosition(),
                                1
                        );

                        ParticleEffect effect = ParticleEffect.builder()
                                .type(ParticleTypes.VILLAGER_HAPPY)
                                .build();

                        for (int i = 0; i < 25; i++) {
                            Random random = new Random();

                            Vector3d position = player.getLocation().getPosition();

                            player.spawnParticles(effect, new Vector3d(
                                    position.getX() + ((1.5 * random.nextDouble()) - 0.75),
                                    position.getY() + ((2 * random.nextDouble()) + 0.5),
                                    position.getZ() + ((1.5 * random.nextDouble()) - 0.75)
                            ));
                        }
                    } else if (result.getResult() == ResultType.ACCOUNT_NO_SPACE) {
                        player.sendMessage(Text.of(TextColors.RED, "You have no space left in your account."));
                    } else {
                        player.sendMessage(Text.of(TextColors.RED, "Error selling gem, contact an owner"));
                    }
                }
            }
        } else {
            player.sendMessage(Text.of(TextColors.RED, "You must be in the gem shop to sell your gems!"));
        }
    }

    public static boolean isGem(ItemStack stack) {
        return stack.getItem().getId().equals("biomesoplenty:gem")
                && stack.toContainer().getInt(DataQuery.of("UnsafeDamage")).get() > 0;
    }

    public static boolean isInShop(World world, Vector3d position) {
        String worldName = "world";

        double lesserX = 1574.0;
        double greaterX = 1594.0;

        double lesserY = 64.0;
        double greaterY = 74.0;

        double lesserZ = 1366.0;
        double greaterZ = 1386.0;

        return world.getName().equals(worldName)
                && position.getX() > lesserX && position.getX() < greaterX
                && position.getY() > lesserY && position.getY() < greaterY
                && position.getZ() > lesserZ && position.getZ() < greaterZ;
    }

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getService().equals(EconomyService.class))
            economyService = (EconomyService) event.getNewProviderRegistration().getProvider();
    }
}
