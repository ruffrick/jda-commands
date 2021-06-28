[1]: https://github.com/DV8FromTheWorld/JDA/

# JDA-Commands

JDA-Commands is an easy to use, annotation-based slash command framework for the popular Discord API wrapper [JDA][1].

## Examples

### Commands

To create a command, simply create a class extending `SlashCommand` and annotate it as `@Command`.

```kotlin
@Command
class PingCommand : SlashCommand() {

    @BaseCommand
    fun ping(event: SlashCommandEvent) { // This is registered as `/ping`
        event.reply("Pong!").queue()
    }

}
```

If no name is specified in the `@Command` annotation, the command name is parsed from the class name by removing the
`Command` suffix. The `@BaseCommand` is used to create a single, top-level command. No additional name is required.

To create more complex commands with subcommands and -groups, use the `@SubCommand` annotation instead of
`@BaseCommand`. As with the command name, subcommand names are parsed from the function name the annotation is attached
to, unless a name is specified in the annotation. To group multiple subcommands in a group, specify the `group`
parameter in the annotation.

```kotlin
@Command
class HelloCommand : SlashCommand() {

    @SubCommand
    fun world(event: SlashCommandEvent) { // This is registered as `/hello world`
        event.reply("Hello world!").queue()
    }

    @SubCommand
    fun user(event: SlashCommandEvent) { // This is registered as `/hello user`
        event.reply("Hello ${event.user.name}!").queue()
    }

}
```

Command options can be added by adding parameters to the function and annotating them as `@CommandOption`. Nullable
parameters will be registered as optional arguments, non-nullable parameters as required arguments. Allowed types are
`String`, `Long`, `Boolean`, `User`, `GuildChannel` and `Role`.

```kotlin
@Command
class GreetCommand : SlashCommand() {

    @BaseCommand
    fun greet(
        event: SlashCommandEvent,
        @CommandOption user: User,
        @CommandOption message: String?
    ) { // This is registered as `/hello <user: User> <optional message: String>`
        if (message == null) {
            event.reply("Greetings, ${user.name}!")
        } else {
            event.reply("Greetings, ${user.name}! $message!")
        }.queue()
    }

}
```

To register the commands, call `registerCommands()` on your JDA or ShardManager instance. This function takes either a
list of commands, or a package name and an optional ClassLoader, in which case the package is scanned for subtypes of
SlashCommand, which are then instantiated. **This only works for classes which don't require any constructor
parameters!**

After registering the commands, this calls `updateCommands()` on the JDA or ShardManager instance it was called on. The
function returns a CommandRegistry instance containing the registered commands, which can be used to register the
commands in a guild for development purposes.

```kotlin
fun main() {
    val jda = JDABuilder.createLight("token").build()
    val commandRegistry = jda.registerCommands(
        listOf(
            PingCommand(),
            HelloCommand(),
            GreetCommand()
        )
    )
    // ...
    commandRegistry.updateCommands(developmentGuild)
}
```

The descriptions for commands and their options are read from a JSON-File located at `resources/lang/descriptions.json`.
Entries are simple key-value pairs structured as `command.<subcommandGroup>.<subcommand>.<option>`. If this file does
not exist or does not contain an entry for every command, subcommand, -group and option specified, the CommandRegistry
will throw an exception while registering the commands.

```json
{
  "ping": "Pong!",
  "hello": "Hello!",
  "hello.world": "Hello world!",
  "hello.user": "Hello user!",
  "greet": "Greet someone",
  "greet.user": "The user to greet",
  "greet.message": "The message to greet the user with"
}
```

### Buttons

This library also adds functionality to handle buttons using annotated functions. For this to work, all button IDs must
be structured as `commandName.buttonName.userId`.

```kotlin
class MoodCommand : SlashCommand() {

    @BaseCommand
    fun mood(event: SlashCommandEvent) {
        event.reply("How are you?").addActionRow(
            Button.success("${commandData.name}.good.${event.user.id}", "Good"),
            Button.danger("${commandData.name}.bad.${event.user.id}", "Bad")
        ).queue()
    }

    @CommandButton(private = true)
    fun good(event: ButtonClickEvent, userId: Long) {
        event.editMessage("That's good to hear!").setActionRows().queue()
    }

    @CommandButton(private = true)
    fun bad(event: ButtonClickEvent, userId: Long) {
        event.editMessage("Oh no! Here, have some \uD83C\uDF68!").setActionRows().queue()
    }

}
```

The `private` field in the annotation restricts the button to only be usable by the original author of the command if
set to true (defaults to false). The supplied `userId` is the ID of the original author. Registering buttons doesn't
require any additional setup, other than registering the command they belong to as described above.

## Download

### Gradle

```gradle
repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io/")
}

dependencies {
    implementation("net.dv8tion:JDA:${JDA_VERSION}")
    implementation("com.github.ruffrick:jda-commands:${COMMIT}")
}
```

### Maven

```maven
<repository>
    <id>dv8tion</id>
    <name>m2-dv8tion</name>
    <url>https://m2.dv8tion.net/releases</url>
</repository>
<repository>
    <id>jitpack</id>
    <name>jitpack</name>
    <url>https://jitpack.io/</url>
</repository>
```

```maven
<dependency>
  <groupId>net.dv8tion</groupId>
  <artifactId>JDA</artifactId>
  <version>$JDA_VERSION</version>
</dependency>
<dependency>
  <groupId>com.github.ruffrick</groupId>
  <artifactId>jda-commands</artifactId>
  <version>$COMMIT</version>
</dependency>
```
