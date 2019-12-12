package com.loftschool.ozaharenko.loftcoin19.ui.converter;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.loftschool.ozaharenko.loftcoin19.data.Coin;
import com.loftschool.ozaharenko.loftcoin19.data.CoinsRepo;
import com.loftschool.ozaharenko.loftcoin19.data.CurrencyRepo;
import com.loftschool.ozaharenko.loftcoin19.util.PriceFormatter;
import com.loftschool.ozaharenko.loftcoin19.util.PriceParser;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

//from: [amount, coin]
//to:   [amount, coin]

class ConverterViewModel extends ViewModel {

    private final Subject<Integer> fromPosition = BehaviorSubject.createDefault(0); // selected POSITION in adapter

    private final Subject<Integer> toPosition = BehaviorSubject.createDefault(1);   // selected POSITION in adapter

    private final Subject<String> fromValue = BehaviorSubject.create(); // text in FROM field

    private final Subject<String> toValue = BehaviorSubject.create();   // text in TO field

    private final Observable<List<Coin>> topCoins;

    private final Observable<Coin> fromCoin;

    private final Observable<Coin> toCoin;

    private final Observable<Double> factor;

    private final PriceFormatter priceFormatter;

    private final PriceParser priceParser;

    @Inject
    ConverterViewModel(CoinsRepo coinsRepo, CurrencyRepo currencyRepo,
                       PriceFormatter priceFormatter, PriceParser priceParser) {
        this.priceFormatter = priceFormatter;
        this.priceParser = priceParser;

        topCoins = currencyRepo.currency()
                .switchMap(currency -> coinsRepo.top(currency, 3))
                .<List<Coin>>map(Collections::unmodifiableList)
                .distinctUntilChanged()
                .replay(1)
                .autoConnect()
                .subscribeOn(Schedulers.io());

        fromCoin = topCoins
                .switchMap(coins -> fromPosition
                        .observeOn(Schedulers.computation())
                        .distinctUntilChanged()
                        .map(coins::get))
                .replay(1)
                .autoConnect()
                .subscribeOn(Schedulers.computation());

        toCoin = topCoins
                .switchMap(coins -> toPosition
                        .observeOn(Schedulers.computation())
                        .distinctUntilChanged()
                        .map(coins::get))
                .replay(1)
                .autoConnect()
                .subscribeOn(Schedulers.computation());

        factor = fromCoin
                .observeOn(Schedulers.computation())
                .switchMap(fc -> toCoin
                        .observeOn(Schedulers.computation())
                        .map(tc -> fc.price() / tc.price())
                )
                .replay(1)
                .autoConnect()
                .subscribeOn(Schedulers.computation());
    }

    @NonNull
    Observable<List<Coin>> topCoins() {

        return topCoins.observeOn(AndroidSchedulers.mainThread());
    }

    @NonNull
    Observable<Coin> fromCoin() {

        return fromCoin.observeOn(AndroidSchedulers.mainThread());
    }

    @NonNull
    Observable<Coin> toCoin() {

        return toCoin.observeOn(AndroidSchedulers.mainThread());
    }

    @NonNull
    Observable<String> fromValue() {
        return toValue
                .compose(parseValue())
                .switchMap(value -> factor.map(f -> value / f))
                .compose(formatValue())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @NonNull
    Observable<String> toValue() {
        return fromValue
                .compose(parseValue())
                .switchMap(value -> factor.map(f -> value * f))
                .compose(formatValue())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    final void fromCoin(int position) {
        fromPosition.onNext(position);
    }

    final void toCoin(int position) {
        toPosition.onNext(position);
    }

    final void fromValue(String text) {
        fromValue.onNext(text);
    }

    final void toValue(String text) {
        toValue.onNext(text);
    }

    @NonNull
    private ObservableTransformer<String, Double> parseValue() {
        return upstream -> upstream
                .distinctUntilChanged()
                .map(priceParser::parse);
    }

    @NonNull
    private ObservableTransformer<Double, String> formatValue() {
        return upstream -> upstream.map(value -> {
            if (value > 0) {
                return priceFormatter.formatWithoutCurrencySymbol(value);
            } else {
                return "";
            }
        });
    }


}
