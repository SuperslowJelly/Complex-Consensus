package io.github.superslowjelly.consensus;

import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class TextBuilder {

    public static final String PREFIX = "&8&l[&6&lConsensus&7&l-&e&lVote&8&l]&r";

    private final Text.Builder OUTPUT;

    private TextBuilder(final String INITIAL_INPUT) {
        OUTPUT = Text.builder();
        this.append(INITIAL_INPUT);
    }

    private TextBuilder() {
        OUTPUT = Text.builder();
    }

    public static TextBuilder create(final String INITIAL_INPUT) {
        return new TextBuilder(INITIAL_INPUT);
    }

    public static TextBuilder create() {
        return new TextBuilder();
    }

    public TextBuilder append(final String INPUT) {
        OUTPUT.append(TextSerializers.FORMATTING_CODE.deserialize(INPUT));
        return this;
    }

    public TextBuilder append(final Text INPUT) {
        OUTPUT.append(INPUT);
        return this;
    }

    public Text build() {
        return OUTPUT.build();
    }
}
