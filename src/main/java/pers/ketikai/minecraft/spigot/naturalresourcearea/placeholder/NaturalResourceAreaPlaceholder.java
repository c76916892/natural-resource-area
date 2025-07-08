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

package pers.ketikai.minecraft.spigot.naturalresourcearea.placeholder;

import pers.ketikai.minecraft.spigot.naturalresourcearea.api.NaturalResourceAreaService;
import team.idealstate.minecraft.next.spigot.api.placeholder.Placeholder;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.command.CommandContext;
import team.idealstate.sugar.next.command.CommandResult;
import team.idealstate.sugar.next.command.annotation.CommandArgument;
import team.idealstate.sugar.next.command.annotation.CommandHandler;
import team.idealstate.sugar.next.context.annotation.component.Controller;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.validate.annotation.NotNull;

@Controller(name = "nra")
public class NaturalResourceAreaPlaceholder implements Placeholder {

    private static final String NULL = "null";

    @CommandHandler(value = "refresh countdown {area}", open = true)
    @NotNull
    public CommandResult refreshCountdown(@NotNull CommandContext context, @NotNull @CommandArgument String area) {
        try {
            return CommandResult.success(String.valueOf(service.getRefreshCountdown(area)));
        } catch (Throwable e) {
            Log.error(e);
        }
        return CommandResult.failure();
    }

    private volatile NaturalResourceAreaService service;

    @Autowired
    public void setService(@NotNull NaturalResourceAreaService service) {
        this.service = service;
    }
}
