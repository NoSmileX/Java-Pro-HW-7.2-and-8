package org.example.entities;

import lombok.*;
import org.example.Currency;

import javax.persistence.*;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "user_transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Currency currency;
    private double amount;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    public Transaction(Currency currency, double amount) {
        this.currency = currency;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", currency=" + currency +
                ", amount=" + amount +
                '}';
    }
}
