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

`time <time> [world]`: Votes to set the time of day in the specified world. 
If no world is specified, the poll creator's world is used.

`weather <weather> [world]`: Votes to set the weather in the specified world.
If no world is specified, the poll creator's world is used.

`dummy <text> [majority] [duration]`: Makes an empty vote; i.e. one that occurs and executes but doesn't actually do anything. 
Useful for personal polls, admin surveys, etc. 

To include spaces in `text`, surround it in double quotes (e.g. `/poll dummy "add a new map"`).

### Configuration

Each mode has its own settings, described in a compound node of the same name.

All mode settings blocks have three common fields: `majority`, the fraction of players needed to pass the vote;
`min-players`, the number of players required to be online for a vote of this type to start;
and `duration`, how long a vote of this type lasts before resolving.

`time{}`: The `time` mode doesn't have any settings of its own, but still has the aforementioned three common settings.

`weather{}`: The `weather` mode doesn't have any settings of its own, but still has the aforementioned three common settings.

### Permissions

Current permissions can be found in [this class.](https://github.com/SuperslowJelly/Complex-Consensus/blob/master/src/main/java/io/github/superslowjelly/consensus/Permissions.java)