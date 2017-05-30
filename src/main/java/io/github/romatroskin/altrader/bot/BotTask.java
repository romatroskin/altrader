package io.github.romatroskin.altrader.bot;

import org.knowm.xchange.currency.CurrencyPair;

import java.util.function.Function;

/**
 * Created by romatroskin on 5/25/17.
 */
public class BotTask {
    private final CurrencyPair currencyPair;
    private final BotTaskRunner taskRunner;

    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    public BotTaskRunner getTaskRunner() {
        return taskRunner;
    }

    public interface ChooseCurrencyPairStep {
        ChooseTaskRunnerStep with(final BotTaskRunner runner);
    }

    public interface ChooseTaskRunnerStep {
        BotTask create();
    }

    private static class Builder implements ChooseCurrencyPairStep, ChooseTaskRunnerStep {
        final CurrencyPair currencyPair;
        BotTaskRunner taskRunner;

        Builder(CurrencyPair pair) {
            this.currencyPair = pair;
        }

        @Override
        public ChooseTaskRunnerStep with(BotTaskRunner runner) {
            this.taskRunner = runner;
            return this;
        }

        @Override
        public BotTask create() {
            return new BotTask(this);
        }
    }

    private BotTask(Builder builder) {
        this.currencyPair = builder.currencyPair;
        this.taskRunner = builder.taskRunner;
    }

    public static BotTask make(CurrencyPair pair, Function<ChooseCurrencyPairStep, ChooseTaskRunnerStep> configuration) {
        return configuration.andThen(ChooseTaskRunnerStep::create).apply(new Builder(pair));
    }
}
