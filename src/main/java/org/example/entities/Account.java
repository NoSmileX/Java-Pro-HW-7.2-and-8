package org.example.entities;

import lombok.*;
import org.example.Currency;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;
    private Currency currency;
    private double amount;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Transaction> list = new ArrayList<>();

    public Account(Currency currency, double amount) {
        this.currency = currency;
        this.amount = amount;
    }

    public void addTrans(Transaction transaction) {
        list.add(transaction);
        transaction.setAccount(this);
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", currency=" + currency +
                ", amount=" + amount +
                '}';
    }
}
