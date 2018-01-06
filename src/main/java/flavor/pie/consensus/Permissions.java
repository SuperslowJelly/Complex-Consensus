package flavor.pie.consensus;

public class Permissions {
    private Permissions() { }

    public final static String BAN_EXEMPT = "consensus.exempt.ban";
    public final static String KICK_EXEMPT = "consensus.exempt.kick";
    public final static String MUTE_EXEMPT = "consensus.exempt.mute";

    public final static String BAN_OVERRIDE = "consensus.override.ban";
    public final static String KICK_OVERRIDE = "consensus.override.kick";
    public final static String MUTE_OVERRIDE = "consensus.override.mute";
    public final static String COMMAND_OVERRIDE = "consensus.override.command";
}
