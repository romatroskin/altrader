package io.github.romatroskin.altrader.bot;

import io.github.romatroskin.altrader.utils.PoloniexUtils;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by romatroskin on 5/25/17.
 */
public class Bot {
    private final String apiKey;
    private final String secretKey;

    private final List<CurrencyPair> pairs;

    public Bot(String apiKey, String secretKey, CurrencyPair... currencyPairs) {
        this(apiKey, secretKey, Arrays.asList(currencyPairs));
    }

    public Bot(String apiKey, String secretKey, List<CurrencyPair> currencyPairs) {
        assert currencyPairs.isEmpty();

        this.pairs = new ArrayList<>();
        this.pairs.addAll(currencyPairs);

        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public void run(BotTaskRunner runner) throws Exception {
        for(CurrencyPair pair : pairs) {
            runner.start(pair, PoloniexUtils.getSpec(apiKey, secretKey));
        }
    }
}
