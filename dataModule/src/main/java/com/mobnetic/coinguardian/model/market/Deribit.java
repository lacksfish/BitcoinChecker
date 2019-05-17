package com.mobnetic.coinguardian.model.market;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import com.mobnetic.coinguardian.model.CheckerInfo;
import com.mobnetic.coinguardian.model.CurrencyPairInfo;
import com.mobnetic.coinguardian.model.Market;
import com.mobnetic.coinguardian.model.Ticker;

public class Deribit extends Market {

    private final static String NAME = "Deribit";
    private final static String TTS_NAME = NAME;
    private final static String URL = "https://www.deribit.com/api/v1/public/getsummary?instrument=%1$s";
    private final static String URL_CURRENCY_PAIRS = "https://www.deribit.com/api/v1/public/getinstruments";

    public Deribit() {
        super(NAME, TTS_NAME, null);
    }

    @Override
    public String getUrl(int requestId, CheckerInfo checkerInfo) {
        return String.format(URL, checkerInfo.getCurrencyPairId());
    }

    @Override
    protected void parseTicker(int requestId, String responseString, Ticker ticker, CheckerInfo checkerInfo) throws Exception {
        this.parseTickerFromJsonObject(requestId, new JSONObject(responseString), ticker, checkerInfo);
    }

    @Override
    protected void parseTickerFromJsonObject(int requestId, JSONObject jsonObject, Ticker ticker, CheckerInfo checkerInfo) throws Exception {
        JSONObject result = (JSONObject) jsonObject.get("result");
        String instrumentName = result.getString("instrumentName");
        ticker.bid = result.getDouble("bidPrice");
        ticker.ask = result.getDouble("askPrice");

        String base = instrumentName.substring(0, 3).toLowerString();
        String captalisedBase = base.substring(0, 1).toUpperCase() + base.substring(1);
        ticker.vol = result.getDouble("volume" + captalisedBase);
        if (!result.isNull("high")) {
          ticker.high = result.getDouble("high");
        }
        if (!result.isNull("low")) {
          ticker.low = result.getDouble("low");
        }
        ticker.last = result.getDouble("last");
        // This is an ISO timestamp representing UTC time
        ticker.timestamp = jsonObject.getLong("usOut")/TimeUtils.NANOS_IN_MILLIS;
    }

    // ====================
    // Get currency pairs
    // ====================
    @Override
    public String getCurrencyPairsUrl(int requestId) {
        return URL_CURRENCY_PAIRS;
    }

    @Override
    protected void parseCurrencyPairs(int requestId, String responseString, List<CurrencyPairInfo> pairs) throws Exception {
        JSONObject response = new JSONObject(responseString);
        JSONArray instruments = (JSONArray) response.get("result");
        for (int i = 0; i < instruments.length(); i++) {
          this.parseCurrencyPairsFromJsonObject(requestId, instruments.getJSONObject(i), pairs);
        }
    }

    @Override
    protected void parseCurrencyPairsFromJsonObject(int requestId, JSONObject jsonObject, List<CurrencyPairInfo> pairs) throws Exception {
        String base = jsonObject.getString("baseCurrency");
        String id = jsonObject.getString("instrumentName");
        String quote;
        if (jsonObject.getString("kind").equals("option")) {
          base = base + "-Options";
          quote = id.substring(id.indexOf(base) + base.length());
        } else {
          quote = jsonObject.getString("currency");
        }

        pairs.add(new CurrencyPairInfo(base, quote, id));
    }
}
