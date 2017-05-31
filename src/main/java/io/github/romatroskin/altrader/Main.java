package io.github.romatroskin.altrader;

import io.github.romatroskin.altrader.bot.Bot;
import io.github.romatroskin.altrader.bot.BotAnalyzeFactory;
import io.github.romatroskin.altrader.bot.BotStrategiesFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.knowm.xchange.currency.CurrencyPair;
import org.fusesource.jansi.AnsiConsole;

/**
 * Created by romatroskin on 5/25/17.
 */
public class Main {
    public static void main(String ... aArgs) throws Exception {
        AnsiConsole.systemInstall();

        final Options options = new Options();
        options.addRequiredOption("b", "base", true, "The base currency is what you're wanting to buy/sell (please refer to your trade system api help)");
        options.addRequiredOption("c", "counter", true, "The counter currency is what currency you want to use to pay/receive for your purchase/sale. (please refer to your trade system api help)");
        options.addRequiredOption("s", "strategy", true, "The strategy which will be used to trade/analyze (cci_correction|global_extrema|moving_momentum|rsi2)");
        options.addOption("a", "analyze", true, "Analyze/Workflow specified strategy live (strategy|workflow)");
        options.addOption(null, "apiKey", true, "API Key provided by your trading system (please refer to your trade system api help)");
        options.addOption(null, "secretKey", true, "Secret Key provided by your trading system (please refer to your trade system api help)");

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, aArgs);


        final CurrencyPair currencyPair = new CurrencyPair(cmd.getOptionValue("b"), cmd.getOptionValue("c"));
        final String apiKey = cmd.getOptionValue("apiKey");
        final String secretKey = cmd.getOptionValue("secretKey");
        final BotStrategiesFactory strategy = BotStrategiesFactory.valueOf(cmd.getOptionValue("s"));

        final Bot bot = new Bot(currencyPair);
        bot.setApiKeys(apiKey, secretKey);
        if(cmd.hasOption("a")) {
            final BotAnalyzeFactory analyze = BotAnalyzeFactory.valueOf(cmd.getOptionValue("a"));
            analyze.setStrategie(strategy);
            bot.run(analyze);
        } else {
            bot.run(strategy);
        }

        AnsiConsole.systemUninstall();
    }
}
