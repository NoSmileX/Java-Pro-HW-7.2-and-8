package org.example;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import lombok.NoArgsConstructor;
import org.example.entities.Account;
import org.example.entities.CurrencyRates;
import org.example.entities.Transaction;
import org.example.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

@NoArgsConstructor
public class Application {
    private final String NUMBER_VALIDATOR = "380\\d{9}";
    private Scanner sc = new Scanner(System.in);
    private EntityManager em;

    public void init() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("Bank");
        em = emf.createEntityManager();
        loadRates();
        try {
            while (true) {
                showMenu();
                var choice = sc.nextLine();
                switch (choice) {
                    case "1" -> addUser();
                    case "2" -> replenishAccount();
                    case "3" -> newTransfer();
                    case "4" -> totalBalance();
                    case "5" -> showTransactions();
                    case "6" -> addFewNewUsers();
                    case "7" -> {
                        return;
                    }
                    default -> System.out.println("Incorrect command. Try again");
                }
            }
        } finally {
            sc.close();
            em.close();
            emf.close();
        }
    }

    private void addFewNewUsers() {
        for (int i = 0; i < 5; i++) {
            User user = new User("User" + i, "38050111111" + i);
            if (commit(user)) {
                openAccounts(user);
            }
        }
    }

    private void showTransactions() {
        showAccount();
        System.out.println("Enter account ID for list of transactions: ");
        Long account = Long.parseLong(sc.nextLine());
        List<Transaction> transactions = em.getReference(Account.class, account).getList();
        System.out.println("*".repeat(25) + " Transactions " + "*".repeat(25));
        if (!transactions.isEmpty()) {
            for (Transaction transaction : transactions) {
                System.out.println(transaction);
            }
        } else {
            System.out.println("Not found any transactions.");
        }
        System.out.println("*".repeat(59));
    }

    private void totalBalance() {
        showUsers();
        System.out.println("Enter user ID: ");
        Long userId = Long.parseLong(sc.nextLine());
        List<Account> accounts = em.getReference(User.class, userId).getList();
        TypedQuery<CurrencyRates> query = em.createQuery("from CurrencyRates", CurrencyRates.class);
        List<CurrencyRates> listRates = query.getResultList();
        System.out.println("*".repeat(25) + " Balance " + "*".repeat(25));
        if (!accounts.isEmpty()) {
            for (Account account : accounts) {
                System.out.println(account + totalInUah(account, listRates));
            }
        } else {
            System.out.println("Not found any accounts.");
        }
        System.out.println("*".repeat(59));
    }

    private String totalInUah(Account account, List<CurrencyRates> listRates) {

        for (CurrencyRates currencyRates : listRates) {
            if (currencyRates.getCurrency().equals(account.getCurrency())) {
                return "Total in UAH: " + (account.getAmount()*currencyRates.getRateBuy());
            }
        }
        return null;
    }

    private void showUsers() {
        TypedQuery<User> query = em.createQuery("from User", User.class);
        List<User> list = query.getResultList();
        System.out.println("*".repeat(25) + " Users " + "*".repeat(25));
        for (User user : list) {
            System.out.println(user);
        }
        System.out.println("*".repeat(59));
    }

    private void newTransfer() {
        showAccount();
        System.out.println("Enter account ID FROM which transfer will be made: ");
        Long fromId = Long.parseLong(sc.nextLine());
        System.out.println("Enter account ID TO which transfer will be made: ");
        Long toId = Long.parseLong(sc.nextLine());
        System.out.println("Enter amount for transfer: ");
        double amount = Double.parseDouble(sc.nextLine());

        Account from = em.getReference(Account.class, fromId);
        Account to = em.getReference(Account.class, toId);
        if (from.getAmount() < amount) {
            System.out.println("Not enough money.");
        } else {
            from.setAmount(from.getAmount() - amount);
            from.addTrans(new Transaction(to.getCurrency(), -amount));

            if (!from.getCurrency().equals(to.getCurrency())) {
                TypedQuery<CurrencyRates> query = em.createQuery("from CurrencyRates", CurrencyRates.class);
                List<CurrencyRates> listRates = query.getResultList();
                for (CurrencyRates rates : listRates) {
                    if (rates.getCurrency().equals(from.getCurrency())) {
                        amount = amount * rates.getRateBuy();
                    }
                    if (rates.getCurrency().equals(to.getCurrency())) {
                        amount = amount / rates.getRateSell();
                    }
                }
            }
            to.setAmount(to.getAmount() + amount);
            to.addTrans(new Transaction(to.getCurrency(), amount));
        }
        commit(from, to);
    }

    private void showAccount() {
        TypedQuery<Account> query = em.createQuery("from Account", Account.class);
        List<Account> list = query.getResultList();
        System.out.println("*".repeat(25) + " Accounts " + "*".repeat(25));
        for (Account account : list) {
            System.out.println(account);
        }
        System.out.println("*".repeat(59));
    }

    private void replenishAccount() {
        showUsers();
        System.out.println("Enter user ID: ");
        Long userId = Long.parseLong(sc.nextLine());
        System.out.println("Enter currency to replenish (UAH, USD or EUR): ");
        Currency currency = Currency.valueOf(sc.nextLine().toUpperCase());
        System.out.println("Enter amount: ");
        double amount = Double.parseDouble(sc.nextLine());

        User user = em.getReference(User.class, userId);
        List<Account> account = user.getList();
        for (Account a : account) {
            if (a.getCurrency().equals(currency)) {
                a.setAmount(a.getAmount() + amount);
                a.addTrans(new Transaction(currency, amount));
            }
        }
        commit(user);
    }

    private void loadRates() {
        try {
            URL url = new URL("https://api.monobank.ua/bank/currency");
            Gson gson = new Gson();
            JsonReader jr = new JsonReader(new InputStreamReader(url.openStream()));
            CurrencyRates[] currencyRates = gson.fromJson(jr, CurrencyRates[].class);
            CurrencyRates UAH = new CurrencyRates(Currency.UAH, 1, 1);
            commit(UAH);
            for (var i : currencyRates) {
                if (i.getCurrency() != null) {
                    commit(i);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addUser() {
        System.out.println("Enter first name: ");
        String name = sc.nextLine();
        System.out.println("Enter phone number. Format 380********* (12 digits): ");
        String phone = sc.nextLine();

        if (phone.matches(NUMBER_VALIDATOR)) {
            User user = new User(name, phone);
            if (commit(user)) {
                System.out.println("User added to DB");
                openAccounts(user);
            } else {
                System.out.println("Error. Incorrect data or phone number already exist in DB.");
            }
        } else {
            System.out.println("Number format must be like 380********* (12 digits total)");
        }
    }

    private void openAccounts(User user) {
        user.addAccount(new Account(Currency.UAH, 0));
        user.addAccount(new Account(Currency.USD, 0));
        user.addAccount(new Account(Currency.EUR, 0));
        commit(user);
    }

    private void showMenu() {
        System.out.println("""
                Enter your choice:
                1. Add new user
                2. Replenish Account
                3. Transfer money
                4. Show balance
                5. Show transactions
                6. Add few new users(for testing)
                7. Exit
                ->\s""");
    }

    private boolean commit(Object o) {
        em.getTransaction().begin();
        try {
            em.persist(o);
            em.getTransaction().commit();
        } catch (Exception ex) {
            em.getTransaction().rollback();
            return false;
        }
        return true;
    }

    private boolean commit(Object o1, Object o2) {
        em.getTransaction().begin();
        try {
            em.persist(o1);
            em.persist(o2);
            em.getTransaction().commit();
        } catch (Exception ex) {
            em.getTransaction().rollback();
            return false;
        }
        return true;
    }
}
