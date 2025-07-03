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

package pers.ketikai.minecraft.spigot.naturalresourcearea.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NonNull;
import pers.ketikai.minecraft.spigot.naturalresourcearea.api.PlayerSelector;
import pers.ketikai.minecraft.spigot.naturalresourcearea.api.RefreshMode;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.component.Configuration;
import team.idealstate.sugar.next.context.annotation.feature.Scope;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Data
@Configuration(uri = "config.yml", release = Context.RESOURCE_EMBEDDED + "config.yml")
@Scope(Scope.PROTOTYPE)
public class NaturalResourceAreaConfiguration {

    public interface Holder {

        void setConfiguration(@NotNull NaturalResourceAreaConfiguration configuration);
    }

    @NonNull
    @JsonDeserialize(keyAs = String.class, contentAs = Area.class)
    private final Map<String, Area> areas;

    @Data
    public static class Area {

        @NonNull
        private final String name;

        @NonNull
        private final String world;

        @NonNull
        private final Location spawn;

        @NonNull
        private final Region region;

        @NonNull
        private final Refresh refresh;

        @NonNull
        @JsonDeserialize(contentAs = Block.class)
        private final Set<Block> whitelist;

        @NonNull
        @JsonDeserialize(keyUsing = BlockKeyDeserializer.class, contentAs = Double.class)
        private final Map<Block, Double> resources;
    }

    @Data
    @JsonDeserialize(using = BlockDeserializer.class)
    public static class Block {

        @NonNull
        private final String namespace;

        @NonNull
        private final String name;

        @NonNull
        private final Byte data;
    }

    public static class BlockKeyDeserializer extends KeyDeserializer {
        @Override
        public Block deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            String[] split = key.split(":", 2);
            String namespace = split[0];
            split = split[1].split("\\$", 2);
            return new Block(namespace.trim(), split[0].trim(), split.length < 2 ? 0 : Byte.parseByte(split[1].trim()));
        }
    }

    public static class BlockDeserializer extends JsonDeserializer<Block> {
        private final BlockKeyDeserializer deserializer = new BlockKeyDeserializer();

        @Override
        public Block deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            return deserializer.deserializeKey(p.getValueAsString(), ctxt);
        }
    }

    @Data
    public static class Refresh {

        @NonNull
        private final RefreshMode mode;

        @NonNull
        private final Integer interval;

        @NonNull
        @JsonDeserialize(contentAs = Action.class)
        private final List<Action> actions;
    }

    @Data
    @JsonDeserialize(using = ActionDeserializer.class)
    public static class Action {

        @NonNull
        private final PlayerSelector selector;

        @NonNull
        private final String command;
    }

    public static class ActionDeserializer extends JsonDeserializer<Action> {
        @Override
        public Action deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String text = p.getValueAsString();
            if (text.startsWith("@")) {
                String[] split = text.split(" ", 2);
                return new Action(
                        PlayerSelector.valueOf(split[0].substring(1).trim().toUpperCase()), split[1]);
            }
            return new Action(PlayerSelector.NONE, text);
        }
    }

    @Data
    public static class Region {

        @NonNull
        private final Location first;

        @NonNull
        private final Location second;

        @NonNull
        @JsonIgnore
        private final Integer minX;

        @NonNull
        @JsonIgnore
        private final Integer maxX;

        @NonNull
        @JsonIgnore
        private final Integer minY;

        @NonNull
        @JsonIgnore
        private final Integer maxY;

        @NonNull
        @JsonIgnore
        private final Integer minZ;

        @NonNull
        @JsonIgnore
        private final Integer maxZ;

        @JsonCreator
        public Region(@NonNull Location first, @NonNull Location second) {
            Validation.notNull(first, "first must not be null");
            Validation.notNull(second, "second must not be null");
            this.first = first;
            this.second = second;
            this.minX = Math.min(first.getX(), second.getX());
            this.maxX = Math.max(first.getX(), second.getX());
            this.minY = Math.min(first.getY(), second.getY());
            this.maxY = Math.max(first.getY(), second.getY());
            this.minZ = Math.min(first.getZ(), second.getZ());
            this.maxZ = Math.max(first.getZ(), second.getZ());
        }
    }

    @Data
    @JsonDeserialize(using = LocationDeserializer.class)
    public static class Location {

        @NonNull
        private final Integer x;

        @NonNull
        private final Integer y;

        @NonNull
        private final Integer z;
    }

    public static class LocationDeserializer extends JsonDeserializer<Location> {
        @Override
        public Location deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String[] split = p.getValueAsString().split(",");
            return new Location(
                    Integer.parseInt(split[0].trim()),
                    Integer.parseInt(split[1].trim()),
                    Integer.parseInt(split[2].trim()));
        }
    }
}
