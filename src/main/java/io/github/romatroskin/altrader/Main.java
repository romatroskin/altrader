package io.github.romatroskin.altrader;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import io.github.romatroskin.altrader.bot.Bot;
import io.github.romatroskin.altrader.bot.BotAnalyzeFactory;
import io.github.romatroskin.altrader.bot.BotStrategiesFactory;
import io.github.romatroskin.altrader.utils.BotAnalyzeFactoryConverter;
import io.github.romatroskin.altrader.utils.BotStrategiesFactoryConverter;
import io.github.romatroskin.altrader.utils.CurrencyPairListConverter;
import org.fusesource.jansi.AnsiConsole;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.ArrayList;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Created by romatroskin on 5/25/17.
 */
@Parameters(separators = "=")
public class Main {
    @Parameter(names={"--currency-pairs"}, description = "The currency pairs on which bot will trade. (please refer to your trade system api help)",
            listConverter = CurrencyPairListConverter.class, required = true)
    private List<CurrencyPair> currencyPairs = new ArrayList<>();

    @Parameter(names={"--apiKey"}, required = true, description = "API Key provided by your trading system (please refer to your trade system api help)")
    private String apiKey;

    @Parameter(names={"--secretKey"}, required = true, description = "Secret Key provided by your trading system (please refer to your trade system api help)")
    private String secretKey;

    @Parameter(names={"-s", "--strategy"}, required = true, description = "The strategy which will be used to trade/analyze (cci_correction|global_extrema|moving_momentum|rsi2|ichemoku)",
            converter = BotStrategiesFactoryConverter.class)
    private BotStrategiesFactory strategy;

    @Parameter(names={"-a", "--analyze"}, description = "Analyze/Workflow specified strategy live (strategy|workflow)",
            converter = BotAnalyzeFactoryConverter.class)
    private BotAnalyzeFactory analyze;


    public static void main(String ... argv) throws Exception {
        AnsiConsole.systemInstall();

        Main main = new Main();
        JCommander.newBuilder().addObject(main).build().parse(argv);
        System.out.print(ansi().eraseScreen());
        main.run();

        AnsiConsole.systemUninstall();
    }

    private void run() throws Exception {
        final Bot bot = new Bot(apiKey, secretKey, currencyPairs);
        if(analyze != null) {
            analyze.setStrategie(strategy);
            bot.run(analyze);
        } else {
            bot.run(strategy);
        }
    }
}
