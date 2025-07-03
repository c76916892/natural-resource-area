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

package pers.ketikai.minecraft.spigot.naturalresourcearea.controller;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pers.ketikai.minecraft.spigot.naturalresourcearea.api.NaturalResourceAreaService;
import pers.ketikai.minecraft.spigot.naturalresourcearea.configuration.NaturalResourceAreaConfiguration;
import team.idealstate.sugar.logging.Log;
import team.idealstate.sugar.next.command.Command;
import team.idealstate.sugar.next.command.CommandContext;
import team.idealstate.sugar.next.command.CommandResult;
import team.idealstate.sugar.next.command.CommandSender;
import team.idealstate.sugar.next.command.annotation.CommandArgument;
import team.idealstate.sugar.next.command.annotation.CommandHandler;
import team.idealstate.sugar.next.context.Bean;
import team.idealstate.sugar.next.context.Context;
import team.idealstate.sugar.next.context.annotation.component.Controller;
import team.idealstate.sugar.next.context.annotation.feature.Autowired;
import team.idealstate.sugar.next.context.aware.ContextAware;
import team.idealstate.sugar.validate.Validation;
import team.idealstate.sugar.validate.annotation.NotNull;

@Controller(name = "natural-resource-area")
public class NaturalResourceAreaController implements Command, ContextAware {

    private volatile Context context;

    @CommandHandler
    @NotNull
    public CommandResult reload() {
        try {
            Bean<NaturalResourceAreaConfiguration> bean = context.getBean(NaturalResourceAreaConfiguration.class);
            Validation.notNull(bean, "未能获取到配置 Bean。");
            assert bean != null;
            List<Bean<NaturalResourceAreaConfiguration.Holder>> beans =
                    context.getBeans(NaturalResourceAreaConfiguration.Holder.class);
            if (!beans.isEmpty()) {
                NaturalResourceAreaConfiguration configuration = bean.getInstance();
                for (Bean<NaturalResourceAreaConfiguration.Holder> holderBean : beans) {
                    holderBean.getInstance().setConfiguration(configuration);
                }
            }
        } catch (Throwable e) {
            Log.error(e);
            return CommandResult.failure("未能完成配置重载，错误信息请查看日志输出。");
        }
        return CommandResult.success("已完成配置重载");
    }

    @CommandHandler(open = true)
    @NotNull
    public CommandResult send(@NotNull CommandContext context) {
        try {
            CommandSender sender = context.getSender();
            org.bukkit.command.CommandSender bukkit;
            if (sender.isConsole()) {
                bukkit = Bukkit.getConsoleSender();
            } else {
                bukkit = Bukkit.getPlayer(sender.getUniqueId());
            }
            if (bukkit != null) {
                List<String> arguments = context.getArguments();
                StringBuilder message = new StringBuilder();
                for (int i = 1; i < arguments.size(); i++) {
                    message.append(arguments.get(i)).append(' ');
                }
                if (message.length() != 0) {
                    bukkit.sendMessage(message.toString());
                }
            }
        } catch (Throwable e) {
            Log.error(e);
            return CommandResult.failure("未能发送消息，错误信息请查看日志输出。");
        }
        return CommandResult.success();
    }

    @CommandHandler(value = "refresh {area}")
    @NotNull
    public CommandResult refresh(
            @NotNull CommandContext context, @NotNull @CommandArgument(completer = "completeArea") String area) {
        try {
            service.refresh(area);
        } catch (Throwable e) {
            Log.error(e);
            return CommandResult.failure("未能刷新区域，错误信息请查看日志输出。");
        }
        return CommandResult.success("已完成区域刷新");
    }

    @CommandHandler(value = "teleport {area}", open = true)
    @NotNull
    public CommandResult teleport(
            @NotNull CommandContext context, @NotNull @CommandArgument(completer = "completeArea") String area) {
        try {
            CommandSender sender = context.getSender();
            if (!sender.isConsole()) {
                Player player = Bukkit.getPlayer(sender.getUniqueId());
                if (player != null) {
                    service.teleport(player, area);
                }
            }
        } catch (Throwable e) {
            Log.error(e);
            return CommandResult.failure("未能传送玩家，错误信息请查看日志输出。");
        }
        return CommandResult.success();
    }

    @NotNull
    public List<String> completeArea(@NotNull CommandContext context, @NotNull String argument) {
        return argument.isEmpty()
                ? service.getAreas()
                : service.getAreas().stream()
                        .filter((s) -> s.toLowerCase().startsWith(argument.toLowerCase()))
                        .collect(Collectors.toList());
    }

    @Override
    public void setContext(@NotNull Context context) {
        this.context = context;
    }

    private volatile NaturalResourceAreaService service;

    @Autowired
    public void setService(@NotNull NaturalResourceAreaService service) {
        this.service = service;
    }
}
