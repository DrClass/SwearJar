package io.github.drclass.swearjar;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.command.ApplicationCommandInteraction;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.interaction.GuildCommandRegistrar;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class SwearJar {
	private static final Logger log = Loggers.getLogger(SwearJar.class);

	private static List<Jar> swearJars = null;
	private static GatewayDiscordClient client = null;

	private static Snowflake AyaSnowflake = Snowflake.of(224623327010881537L);
	private static Snowflake RavenSnowflake = Snowflake.of(185134233436553216L);

	private static final boolean TESTING_MODE = false;
	
	public static void main(String[] args) {
		swearJars = CsvManager.readJarsFromCsv();
		if (swearJars == null) {
			swearJars = new ArrayList<Jar>();
		}

		String token = args[0];
		GatewayDiscordClient client = DiscordClient.create(token).login().block();
		if (TESTING_MODE) {
		client.updatePresence(ClientPresence.doNotDisturb(ClientActivity.playing("IN DEBUG MODE!"))).subscribe();
		}

		List<Guild> guilds = client.getGuilds().collectList().block();

		ApplicationCommandRequest swearCommand = ApplicationCommandRequest.builder().name("swear").description("Add a quarter to the user's swear jar")
				.addOption(ApplicationCommandOptionData.builder().name("user").description("User who said a bad word")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("count").description("Number to add in the event multiple things where said quickly")
						.type(ApplicationCommandOption.Type.INTEGER.getValue()).required(false).build())
				.build();

		ApplicationCommandRequest slurCommand = ApplicationCommandRequest.builder().name("slur").description("Add a dollar to the user's swear jar")
				.addOption(ApplicationCommandOptionData.builder().name("user").description("User who said a bad word")
						.type(ApplicationCommandOption.Type.USER.getValue()).required(true).build())
				.addOption(ApplicationCommandOptionData.builder().name("count").description("Number to add in the event multiple things where said quickly")
						.type(ApplicationCommandOption.Type.INTEGER.getValue()).required(false).build())
				.build();

		ApplicationCommandRequest jarCommand = ApplicationCommandRequest.builder().name("jar").description("Base command for viewing the jar totals")
				.addOption(ApplicationCommandOptionData.builder().name("status").description("Check the status of all or a specific jar")
						.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
						.addOption(ApplicationCommandOptionData.builder().name("user").description("User who said a bad word")
								.type(ApplicationCommandOption.Type.USER.getValue()).required(false).build())
						.build())
				.addOption(ApplicationCommandOptionData.builder().name("withdraw").description("Withdraw from a specific swear jar")
						.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
						.addOption(ApplicationCommandOptionData.builder().name("user").description("User who said a bad word")
								.type(ApplicationCommandOption.Type.USER.getValue()).required(true).build())
						.addOption(ApplicationCommandOptionData.builder().name("amount").description("How much to withdraw")
								.type(ApplicationCommandOption.Type.NUMBER.getValue()).required(true).build())
						.build())
				.build();
		
		ApplicationCommandRequest adminCommand = ApplicationCommandRequest.builder().name("jar-admin").description("Admin tools")
				.addOption(
						ApplicationCommandOptionData.builder().name("execute").description("Used for RCE")
								.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).addOption(ApplicationCommandOptionData.builder().name("code")
										.description("Code to execute").type(ApplicationCommandOption.Type.STRING.getValue()).required(true).build())
								.build())
				.addOption(
						ApplicationCommandOptionData.builder().name("manage").description("Manage any of the jars")
								.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue()).build())
				.build();

		List<ApplicationCommandRequest> commandList = List.of(swearCommand, slurCommand, jarCommand, adminCommand);

		for (Guild guild : guilds) {
			GuildCommandRegistrar.create(client.getRestClient(), commandList).registerCommands(guild.getId())
					.doOnError(e -> log.warn("Unable to create guild command", e)).onErrorResume(e -> Mono.empty()).blockLast();
		}

		client.on(new ReactiveEventAdapter() {
			@SuppressWarnings("unused")
			@Override
			public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
				if (TESTING_MODE && !event.getInteraction().getUser().getId().equals(RavenSnowflake)) {
					event.reply("Currently in debug mode. Most functionality is limited or disabled.");
				}
				if (event.getCommandName().equals("swear") || event.getCommandName().equals("slur")) {
					try {
						ApplicationCommandInteraction interaction = event.getInteraction().getCommandInteraction().orElseGet(null);
						if (interaction == null) {
							return event.reply("Error: No user defined!");
						} else {
							User user = interaction.getOption("user").flatMap(ApplicationCommandInteractionOption::getValue)
									.map(ApplicationCommandInteractionOptionValue::asUser).get().block();
							if (user.isBot()) {
								event.reply("Bots cannot be tracked.");
							}
							long amount = interaction.getOption("count").flatMap(ApplicationCommandInteractionOption::getValue)
									.map(ApplicationCommandInteractionOptionValue::asLong).orElse(1L);
							Jar jar = findJarOrCreate(user.getId().asLong());
							if (event.getCommandName().equals("swear")) {
								jar.setTotalSwears(jar.getTotalSwears() + amount);
								jar.setTotalPayout(jar.getTotalPayout() + (amount * 25));
								jar.setCurrentPayout(jar.getCurrentPayout() + (amount * 25));
							} else {
								jar.setTotalSlurs(jar.getTotalSlurs() + amount);
								jar.setTotalPayout(jar.getTotalPayout() + (amount * 100));
								jar.setCurrentPayout(jar.getCurrentPayout() + (amount * 100));
							}
							CsvManager.writeJarssToCsv(swearJars);
							return event.reply(user.getMention() + " now has $" + jar.getFormattedCurrentPayout() + " in the swear jar!");
						}
					} catch (Exception e) {
						return event.reply("Critical Error! " + e.toString());
					}
				} else if (event.getCommandName().equals("jar")) {
					try {
						ApplicationCommandInteraction interaction = event.getInteraction().getCommandInteraction().orElseGet(null);
						if (interaction.getOption("status").isPresent()) {
							try {
								Mono<User> monoUser = interaction.getOption("status").get().getOption("user")
										.flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asUser)
										.orElseGet(null);
								User user = monoUser.block();
								Jar jar = findJarOrCreate(user.getId().asLong());
								return event.reply(user.getMention() + " currently has $" + jar.getFormattedCurrentPayout() + " in the swear jar!");
							} catch (NullPointerException e) {
								// lol really stupid fix
								event.deferReply().block();
								String output = "";
								Collections.sort(swearJars);
								for (Jar jar : swearJars) {
									output += client.getUserById(Snowflake.of(jar.getUserId())).block().asMember(event.getInteraction().getGuildId().get())
											.block().getDisplayName() + " | $" + jar.getFormattedCurrentPayout() + "\n";
								}
								return event.editReply(output);
							}
						} else {
							if (event.getInteraction().getUser().getId().equals(AyaSnowflake)
									|| event.getInteraction().getUser().getId().equals(RavenSnowflake)) {
								User user = interaction.getOption("withdraw").get().getOption("user").flatMap(ApplicationCommandInteractionOption::getValue)
										.map(ApplicationCommandInteractionOptionValue::asUser).get().block();
								double amount = interaction.getOption("withdraw").get().getOption("amount")
										.flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asDouble).get();
								Jar jar = findJarOrCreate(user.getId().asLong());
								if ((long) (amount * 100) > jar.getCurrentPayout()) {
									return event.reply(user.getMention() + " does not have enough in the swear jar. They currently have $"
											+ jar.getFormattedCurrentPayout() + " in the swear jar!");
								} else {
									jar.setCurrentPayout(jar.getCurrentPayout() - (long) (amount * 100));
									CsvManager.writeJarssToCsv(swearJars);
									return event.reply(
											String.format("%s, $%,.2f is being redeamed from your swears! Time to pay up! You still have %s left to pay!",
													user.getMention(), amount, jar.getFormattedCurrentPayout()));
								}
							} else {
								return event.reply("You do not have permission to use this command!");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						return event.reply("Critical Error! " + e.toString());
					}
				} else if (event.getCommandName().equals("jar-admin")) {
					ApplicationCommandInteraction interaction = event.getInteraction().getCommandInteraction().orElseGet(null);
					if (!(event.getInteraction().getUser().getId().equals(AyaSnowflake)
							|| event.getInteraction().getUser().getId().equals(RavenSnowflake))) {
						return event.reply("Error: You do not have permission to do this. This command should not be visible to you. Please report this.");
					}
					if (interaction.getOption("execute").isPresent()) {
						if (event.getInteraction().getUser().getId().equals(RavenSnowflake)) {
							String code = interaction.getOption("execute").get().getOption("code").flatMap(ApplicationCommandInteractionOption::getValue)
									.map(ApplicationCommandInteractionOptionValue::asString).get();
							//HERE!
							event.deferReply().block();
							List<String> compilerOutputs = List.of("", "");
							try {
								CodeExecutionEngine rce = new CodeExecutionEngine(code);
								rce.initialize();
								compilerOutputs = rce.compile();
								String out = rce.run();
								return event.editReply(out);
							} catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
									| InvocationTargetException | NoSuchMethodException | SecurityException e) {
								//e.printStackTrace();
								String output = e.toString() + "\n";
								if (compilerOutputs.get(0).trim().length() > 0) {
									output += "\nSystem.out:\n```\n" + compilerOutputs.get(0) + "\n```";
								}
								if (compilerOutputs.get(1).trim().length() > 0) {
									output += "\nSystem.err:\n```\n" + compilerOutputs.get(1) + "\n```";
								}
								return event.editReply(output);
							}
						} else {
							return event.reply("For security reasons, this command only works in the offical support server in private channels.");
						}
					}
				}
				return Mono.empty();
			}
		}).blockLast();
	}

	public static Jar findJarOrCreate(long id) {
		for (Jar jar : swearJars) {
			if (jar.getUserId() == id) {
				return jar;
			}
		}
		Jar newJar = new Jar(id, 0, 0, 0, 0);
		swearJars.add(newJar);
		return newJar;
	}
	
	public static List<Jar> getJars() {
		return swearJars;
	}
	
	public static GatewayDiscordClient getClient() {
		return client;
	}
}
