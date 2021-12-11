[1]: https://github.com/DV8FromTheWorld/JDA/

[2]: https://github.com/ruffrick/jda-kotlinx

# JDA-Commands

JDA-Commands is an easy to use, annotation-based slash command framework for the popular Discord API wrapper [JDA][1].
This bundles [jda-kotlinx][2] to add coroutine support for suspending event listeners.

## Examples

### Commands

To create a command, simply create a class extending `SlashCommand` and annotate it as `@Command`.

```kotlin
@Command
class PingCommand : SlashCommand() {
    @Command
    fun ping(event: SlashCommandEvent) { // This is registered as `/ping`
        event.reply("Pong!").queue()
    }
}
```

If no name is specified in the `@Command` annotation, the command name is parsed from the class name by removing the
`Command` suffix. To create a single top-level command, annotate any function in the class as `@Command`. No additional
arguments are required in the annotation.

To create more complex commands with subcommands and subcommand groups, use the `@Subcommand` annotation. To group
multiple subcommands, specify the `group` parameter in the annotation and add a `groupDescription` to any one of the
annotations belonging to the same group.

```kotlin
@Command
class HelloCommand : SlashCommand() {
    @Subcommand
    fun world(event: SlashCommandEvent) { // This is registered as `/hello world`
        event.reply("Hello world!").queue()
    }

    @Subcommand
    fun user(event: SlashCommandEvent) { // This is registered as `/hello user`
        event.reply("Hello ${event.user.name}!").queue()
    }
}
```

Command options can be added by adding parameters to the function and annotating them as `@Option`. Nullable parameters
will be registered as optional arguments, non-nullable parameters as required arguments. Allowed types are `String`,
`Long`, `Boolean`, `User`, `GuildChannel`, `Role` and `Double`.

```kotlin
@Command
class GreetCommand : SlashCommand() {
    @Command
    fun greet(
        event: SlashCommandEvent,
        @Option user: User,
        @Option message: String?
    ) { // This is registered as `/greet <user: User> <optional message: String>`
        if (message == null) {
            event.reply("Greetings, ${user.name}!")
        } else {
            event.reply("Greetings, ${user.name}! $message!")
        }.queue()
    }
}
```

To register the commands, use `CommandRegistryBuilder` to build a command registry and then call `updateCommands()` on
that instance to register the commands for a `ShardManager`, `JDA` or `Guild` instance.

```kotlin
fun main() {
    val jda = JDABuilder.createLight("token")
        .useSuspendEventManager()
        .build()

    val commandRegistry = CommandRegistryBuilder()
        .addCommands(PingCommand(), HelloCommand(), GreetCommand())
        .build()

    commandRegistry.updateCommands(jda)
}
```

### Buttons

This library also adds functionality to handle buttons using annotated functions. Simply add a button to a message using
the functions provided by `SlashCommand` and create a `@Button` function for each ID used.

```kotlin
@Command
class MoodCommand : SlashCommand() {
    @Command
    fun mood(event: SlashCommandEvent) {
        event.reply("How are you?").addActionRow(
            success("good", "Good"),
            danger("bad", "Bad")
        ).queue()
    }

    @Button
    fun good(event: ButtonClickEvent) {
        event.editMessage("That's good to hear!").setActionRows().queue()
    }

    @Button
    fun bad(event: ButtonClickEvent) {
        event.editMessage("Oh no! Here, have some \uD83C\uDF68!").setActionRows().queue()
    }
}
```

### Type Mapping

You can add support for custom types for both commands and buttons.*

```kotlin
class DurationMapper : Mapper<String, Duration> {
    private val pattern = Regex("^(\\d+)([dhms])\$").toPattern()

    override suspend fun transform(value: String): Duration {
        val matcher = pattern.matcher(value.lowercase())
        require(matcher.matches()) { "Please enter a duration, e. g. `1h` or `3d`!" }
        val count = matcher.group(1).toInt()
        require(count > 0) { "Your duration can't be less than one!" }

        return when (matcher.group(2)) {
            "d" -> Duration.days(count)
            "h" -> Duration.hours(count)
            "m" -> Duration.minutes(count)
            "s" -> Duration.seconds(count)
            else -> throw IllegalStateException("How did we get here?")
        }
    }
}
```

If an `IllegalArgumentException` is thrown while mapping, it's message will be replied to the event and the
command/button handler function will not be executed.

Allowed input types are `String`, `Long`, `Boolean`, `User`, `GuildChannel`, `Role` and `Double` as well as
`SlashCommandEvent` and `ButtonClickEvent`. When using a non-standard type for an option within a command or button
function, the option type will be the input type of the first mapper found to produce that type. 

```kotlin
@Command
fun command(
    context: CommandContext, // Requires a Mapper<SlashCommandEvent, CommandContext>
    @Option duration: Duration // Requires a Mapper<?, Duration>
) {
    // ...
}

@Button
fun button(
    context: ButtonContext, // Requires a Mapper<ButtonClickEvent, ButtonContext>
    long: Long // Requires a Mapper<ButtonClickEvent, Long>
) {
    // ...
}
```

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
