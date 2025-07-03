/*
 *    natural-resource-area
 *    Copyright (C) 2025  ketikai
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pers.ketikai.minecraft.spigot.naturalresourcearea.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import pers.ketikai.minecraft.spigot.naturalresourcearea.api.NaturalResourceAreaService;
import pers.ketikai.minecraft.spigot.naturalresourcearea.api.RefreshMode;
import pers.ketikai.minecraft.spigot.naturalresourcearea.configuration.NaturalResourceAreaConfiguration;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.context.ContextHolder;
import team.idealstate.sugar.next.context.annotation.component.Service;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.aware.ContextHolderAware;
import team.idealstate.sugar.next.context.lifecycle.Initializable;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Service
public class NaturalResourceAreaServiceImpl
        implements NaturalResourceAreaService,
                NaturalResourceAreaConfiguration.Holder,
                ContextHolderAware,
                Initializable {

    private final Map<String, Long> refreshRecords = new ConcurrentHashMap<>();
    private volatile ContextHolder contextHolder;
    private volatile NaturalResourceAreaConfiguration configuration;

    @Override
    public void initialize() {
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimer(
                (Plugin) contextHolder,
                () -> {
                    configuration.getAreas().keySet().forEach(a -> refresh(a, false));
                },
                20L,
                20L);
    }

    @Override
    public boolean refresh(@NotNull String area) {
        Validation.notNullOrBlank(area, "area must not be null or blank.");
        return refresh(area, true);
    }

    private boolean refresh(@NotNull String area, boolean force) {
        Validation.notNullOrBlank(area, "area must not be null or blank.");
        Map<String, NaturalResourceAreaConfiguration.Area> areas = configuration.getAreas();
        NaturalResourceAreaConfiguration.Area area1 = areas.get(area);
        if (area1 == null) {
            return false;
        }
        Map<NaturalResourceAreaConfiguration.Block, Double> resources = new LinkedHashMap<>(area1.getResources());
        if (resources.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        NaturalResourceAreaConfiguration.Refresh refresh = area1.getRefresh();
        if (!force) {
            Long last = refreshRecords.get(area);
            if (last != null) {
                long next = last + refresh.getInterval() * 60L * 1000L;
                if (now < next) {
                    return false;
                }
            }
        }
        String world = area1.getWorld();
        World world1 = Bukkit.getWorld(world);
        List<Player> worldPlayers = world1.getPlayers();
        if (!worldPlayers.isEmpty()) {
            NaturalResourceAreaConfiguration.Region region = area1.getRegion();
            int minX = region.getMinX();
            int maxX = region.getMaxX();
            int minY = region.getMinY();
            int maxY = region.getMaxY();
            int minZ = region.getMinZ();
            int maxZ = region.getMaxZ();

            List<Player> areaPlayers = new ArrayList<>();
            for (Player player : worldPlayers) {
                Location loc = player.getLocation();
                if (loc.getWorld().getName().equals(world)
                        && loc.getX() >= minX
                        && loc.getX() <= maxX
                        && loc.getY() >= minY
                        && loc.getY() <= maxY
                        && loc.getZ() >= minZ
                        && loc.getZ() <= maxZ) {
                    areaPlayers.add(player);
                }
            }

            List<Block> areaBlocks = new ArrayList<>();
            boolean loaded = false;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!loaded) {
                        loaded = world1.isChunkLoaded(x, z);
                    }
                    A:
                    for (int y = minY; y <= maxY; y++) {
                        Block block = world1.getBlockAt(x, y, z);
                        if (RefreshMode.FILL.equals(refresh.getMode())) {
                            if (!block.isEmpty()) {
                                continue;
                            }
                        }
                        Material material = block.getType();
                        String name = material.name();
                        for (NaturalResourceAreaConfiguration.Block block1 : area1.getWhitelist()) {
                            if (!"minecraft:".equalsIgnoreCase(block1.getNamespace())) {
                                continue;
                            }
                            if (!block1.getName().equalsIgnoreCase(name)) {
                                continue;
                            }
                            if (block1.getData() == block.getData()) {
                                continue A;
                            }
                        }
                        areaBlocks.add(block);
                    }
                }
            }
            if (loaded && !areaBlocks.isEmpty()) {
                double totalWeight = 0.0;
                for (Map.Entry<NaturalResourceAreaConfiguration.Block, Double> entry : resources.entrySet()) {
                    totalWeight += entry.getValue();
                }

                Random random = new Random();
                for (Block block : areaBlocks) {
                    double randomValue = random.nextDouble() * totalWeight;
                    double currentWeight = 0.0;

                    NaturalResourceAreaConfiguration.Block selected = null;
                    for (Map.Entry<NaturalResourceAreaConfiguration.Block, Double> entry : resources.entrySet()) {
                        currentWeight += entry.getValue();
                        if (randomValue <= currentWeight) {
                            selected = entry.getKey();
                            break;
                        }
                    }

                    if (selected != null) {
                        Material material = Material.matchMaterial(selected.getName());
                        if (material != null) {
                            BlockState state = block.getState();
                            state.setType(material);
                            state.setRawData(selected.getData());
                            state.update(true);
                        }
                    }
                }

                Map<String, String> variables = new HashMap<>();
                variables.put("{world}", world);
                variables.put("{area}", area1.getName());
                for (NaturalResourceAreaConfiguration.Action action : refresh.getActions()) {
                    Collection<? extends Player> players = null;
                    switch (action.getSelector()) {
                        case SERVER:
                            players = Bukkit.getOnlinePlayers();
                            break;
                        case WORLD:
                            players = worldPlayers;
                            break;
                        case AREA:
                            players = areaPlayers;
                            break;
                    }
                    String command = action.getCommand();
                    for (Map.Entry<String, String> entry : variables.entrySet()) {
                        command = command.replace(entry.getKey(), entry.getValue());
                    }
                    try {
                        if (players == null) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        } else {
                            for (Player player : players) {
                                Bukkit.dispatchCommand(player, command.replace("{player}", player.getName()));
                            }
                        }
                    } catch (CommandException e) {
                        Log.error(e);
                    }
                }
            }
        }
        refreshRecords.put(area, now);
        return true;
    }

    @Override
    public boolean teleport(@NotNull Player player, @NotNull String area) {
        Validation.notNull(player, "player must not be null.");
        Validation.notNullOrBlank(area, "area must not be null or blank.");
        Map<String, NaturalResourceAreaConfiguration.Area> areas = configuration.getAreas();
        NaturalResourceAreaConfiguration.Area area1 = areas.get(area);
        if (area1 == null) {
            return false;
        }
        String world = area1.getWorld();
        World world1 = Bukkit.getWorld(world);
        if (world1 == null) {
            return false;
        }
        NaturalResourceAreaConfiguration.Location spawn = area1.getSpawn();
        Location location = new Location(world1, spawn.getX(), spawn.getY(), spawn.getZ());
        return player.teleport(
                world1.getHighestBlockAt(location).getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @Override
    @NotNull
    public List<String> getAreas() {
        return configuration.getAreas().isEmpty()
                ? Collections.emptyList()
                : new ArrayList<>(configuration.getAreas().keySet());
    }

    @Override
    public void setContextHolder(@NotNull ContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Autowired
    @Override
    public void setConfiguration(@NotNull NaturalResourceAreaConfiguration configuration) {
        this.configuration = configuration;
    }
}
