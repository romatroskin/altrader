package io.github.romatroskin.altrader.bot;

import io.github.romatroskin.altrader.utils.PoloniexUtils;
import org.knowm.xchange.currency.CurrencyPair;

/**
 * Created by romatroskin on 5/25/17.
 */
public class Bot {
    private final CurrencyPair pair;
    private String apiKey;
    private String secretKey;

    public Bot(CurrencyPair pair) {
        this.pair = pair;
    }

    public void run(BotTaskRunner runner) throws Exception {
        runner.start(pair, PoloniexUtils.getSpec(apiKey, secretKey));
    }

    public void setApiKeys(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }
}
