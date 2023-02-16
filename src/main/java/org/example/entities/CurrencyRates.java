package org.example.entities;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import org.example.Currency;

import javax.persistence.*;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@ToString
@Entity
public class CurrencyRates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @SerializedName("currencyCodeA")
    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
    private Currency currency;
    private double rateSell;
    private double rateBuy;

    public CurrencyRates(Currency currency, double rateSell, double rateBuy) {
        this.currency = currency;
        this.rateSell = rateSell;
        this.rateBuy = rateBuy;
    }
}
