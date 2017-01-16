# Consensus

Consensus is a Sponge plugin that allows players to vote on various things. 
Players can start a vote with `/poll <mode> <args>`. A message will pop up in chat saying that this player has started the poll.
If a player clicks the vote button, they will vote 'Yes' on the poll.
Once the poll has passed, its effects will occur.

#### Note on the arguments
Wherever a duration is requested, the format is `[nD][T[nH][nM][nS]]`.
So two days, one hour, and four seconds would be represented `2DT1H4S`.
This is applicable both in commands and in the config.

### Poll types

`mute <player> [duration] <reason>`: Votes to mute the player for the specified amount of time for the specified reason.
If no duration is specified, the configured maximum duration is used.

`kick <player> <reason>`: Votes to kick the player for the specified reason.

`ban <player> [duration] <reason>`: Votes to kick and ban the player for the amount of time for the specified reason.
If no duration is specified, the configured maximum duration is used.

`time <time> [world]`: Votes to set the time of day in the specified world. 
If no world is specified, the poll creator's world is used.

`command <command>`: Votes to run the specified command from the console's perspective.
Note that any command must be whitelisted before it can be run.

`dummy <text> [majority] [duration]`: Makes an empty vote; i.e. one that occurs and executes but doesn't actually do anything. 
Useful for personal polls, admin surveys, etc. 

As the only command absent from configuration, its majority and duration are specified in the command; 
if either is not present, they default to 0.5 and T1M respectively. See the configuration section for details.

To include spaces in `text`, surround it in double quotes (e.g. `/poll dummy "add a new map"`).

### Configuration

`enabled-modes[]`: This is a list of the modes (AKA subcommands of `/poll`) that are enabled, sans `dummy`.

Each mode has its own settings, described in a compound node of the same name.

All mode settings blocks have three common fields: `majority`, the fraction of players needed to pass the vote;
`min-players`, the number of players required to be online for a vote of this type to start;
and `duration`, how long a vote of this type lasts before resolving.

`ban{}`: The `ban` mode has three settings. The first, `max-duration`, is the maximum duration that someone can be banned for.
The second, `exempt`, is the permission for a player to be safe from being vote-banned. 
And the third, `override`, is the permission for a player to override the previous two settings.

`kick{}`: The `kick` mode has two settings, `exempt` and `override`. See `ban{}` for details.

`mute{}`: The `mute` mode has three settings, `max-duration`, `exempt`, and `override`. See `ban{}` for details.

`time{}`: The `time` mode doesn't have any settings of its own, but still has the aforementioned three common settings.

`command{}`: The `command` mode has one unique setting, `allowed-commands[]`. This is a list of commands
that are allowed to be used. It supports subcommands, such as `warp set`. `command` also has
`override`, described in `ban{}`.

### Changelog

1.0.0: By majority vote, these details won't be released.