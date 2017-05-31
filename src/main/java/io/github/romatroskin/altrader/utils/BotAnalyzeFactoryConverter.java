package io.github.romatroskin.altrader.utils;

import com.beust.jcommander.IStringConverter;
import io.github.romatroskin.altrader.bot.BotAnalyzeFactory;

/**
 * Created by romatroskin on 5/31/17.
 */
public class BotAnalyzeFactoryConverter implements IStringConverter<BotAnalyzeFactory> {
    @Override
    public BotAnalyzeFactory convert(String s) {
        return BotAnalyzeFactory.valueOf(s);
    }
}
