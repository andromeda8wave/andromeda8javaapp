package com.andromeda8finance;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Andromeda8FinanceApp extends Application {

    // -------------------------
    // Models
    // -------------------------
    public static class Article {
        private String name;            // e.g. "Food"
        private String type;            // "Income" or "Expense"
        private List<String> subArticles = new ArrayList<>();

        public Article() {}
        public Article(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public List<String> getSubArticles() { return subArticles; }
        public void setSubArticles(List<String> subArticles) { this.subArticles = subArticles; }
    }

    public static class Wallet {
        private String name;
        private double initialBalance;

        public Wallet() {}
        public Wallet(String name, double initialBalance) {
            this.name = name;
            this.initialBalance = initialBalance;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getInitialBalance() { return initialBalance; }
        public void setInitialBalance(double initialBalance) { this.initialBalance = initialBalance; }
    }

    public static class Transaction {
        private LocalDate date;
        private String type;      // "Income" or "Expense" (from article)
        private String article;
        private String subArticle;
        private String wallet;
        private double amount;
        private String comment;

        public Transaction() {}

        public Transaction(LocalDate date, String article, String subArticle,
                           String wallet, double amount, String comment) {
            this.date = date;
            this.article = article;
            this.subArticle = subArticle;
            this.wallet = wallet;
            this.amount = amount;
            this.comment = comment;
        }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getArticle() { return article; }
        public void setArticle(String article) { this.article = article; }

        public String getSubArticle() { return subArticle; }
        public void setSubArticle(String subArticle) { this.subArticle = subArticle; }

        public String getWallet() { return wallet; }
        public void setWallet(String wallet) { this.wallet = wallet; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    // -------------------------
    // In-memory Data
    // -------------------------
    private static final ObservableList<Article> articles = FXCollections.observableArrayList();
    private static final ObservableList<Wallet> wallets = FXCollections.observableArrayList();
    private static final ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    // Main UI references
    private TableView<Transaction> transactionsTable;
    private DatePicker dpStartDate;
    private DatePicker dpEndDate;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Load from JSON
        loadData();

        BorderPane root = new BorderPane();

        // Top bar: Period Filters + Buttons
        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));

        dpStartDate = new DatePicker();
        dpEndDate = new DatePicker();
        dpStartDate.setConverter(createDateConverter());
        dpEndDate.setConverter(createDateConverter());

        // Refresh table when date filters change
        dpStartDate.valueProperty().addListener((obs, oldVal, newVal) -> updateFilteredTransactions());
        dpEndDate.valueProperty().addListener((obs, oldVal, newVal) -> updateFilteredTransactions());

        Label lblFrom = new Label("From:");
        Label lblTo = new Label("To:");

        // Button: Articles Directory
        Button btnArticles = new Button("Financial Accounting Articles");
        btnArticles.setOnAction(e -> openArticlesDirectoryWindow());

        // Button: Wallets
        Button btnWallets = new Button("Wallets");
        btnWallets.setOnAction(e -> openWalletsWindow());

        // (1) NEW "Add Transaction" Button
        Button btnAddTransaction = new Button("Add Transaction");
        btnAddTransaction.setOnAction(e -> openTransactionEditor(null));

        topBox.getChildren().addAll(lblFrom, dpStartDate, lblTo, dpEndDate, btnArticles, btnWallets, btnAddTransaction);
        root.setTop(topBox);

        // Center: Transactions Table
        transactionsTable = new TableView<>();
        transactionsTable.setEditable(true);

        TableColumn<Transaction, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setMinWidth(100);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setMinWidth(80);

        TableColumn<Transaction, String> articleCol = new TableColumn<>("Article");
        articleCol.setCellValueFactory(new PropertyValueFactory<>("article"));
        articleCol.setMinWidth(100);

        TableColumn<Transaction, String> subArticleCol = new TableColumn<>("Sub-article");
        subArticleCol.setCellValueFactory(new PropertyValueFactory<>("subArticle"));
        subArticleCol.setMinWidth(100);

        TableColumn<Transaction, String> walletCol = new TableColumn<>("Wallet");
        walletCol.setCellValueFactory(new PropertyValueFactory<>("wallet"));
        walletCol.setMinWidth(100);

        TableColumn<Transaction, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setMinWidth(80);

        TableColumn<Transaction, String> commentCol = new TableColumn<>("Comment");
        commentCol.setCellValueFactory(new PropertyValueFactory<>("comment"));
        commentCol.setMinWidth(120);

        transactionsTable.getColumns().addAll(dateCol, typeCol, articleCol, subArticleCol, walletCol, amountCol, commentCol);

        // Double-click on empty space => add new transaction
        transactionsTable.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && row.isEmpty()) {
                    openTransactionEditor(null);
                }
            });
            return row;
        });

        // Right-click menu => Edit / Delete
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Transaction");
        editItem.setOnAction(e -> {
            Transaction selected = transactionsTable.getSelectionModel().getSelectedItem();
            if (selected != null) openTransactionEditor(selected);
        });
        MenuItem deleteItem = new MenuItem("Delete Transaction");
        deleteItem.setOnAction(e -> {
            Transaction selected = transactionsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                transactions.remove(selected);
                saveData();
                updateFilteredTransactions();
            }
        });
        contextMenu.getItems().addAll(editItem, deleteItem);
        transactionsTable.setContextMenu(contextMenu);

        root.setCenter(transactionsTable);
        updateFilteredTransactions(); // initial load

        Scene scene = new Scene(root, 900, 500);
        primaryStage.setTitle("Andromeda8Finance - Transaction History");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Save on exit
        saveData();
    }

    // -------------------------------------------------
    // (2) REVISED Articles Window: two panels
    // -------------------------------------------------
    private void openArticlesDirectoryWindow() {
        Stage stage = new Stage();
        stage.setTitle("Financial Articles Directory");

        // Split into two: articles on the left, sub-articles on the right
        HBox mainBox = new HBox(10);
        mainBox.setPadding(new Insets(10));

        // Left side: Articles List + fields for name, type + add/delete
        VBox leftBox = new VBox(10);
        ListView<Article> lvArticles = new ListView<>(articles);
        lvArticles.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Article item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // e.g., "Food (Expense)"
                    setText(item.getName() + " (" + item.getType() + ")");
                }
            }
        });

        TextField tfArticleName = new TextField();
        ComboBox<String> cbArticleType = new ComboBox<>();
        cbArticleType.getItems().addAll("Income", "Expense");

        // Sub-articles on the right side
        VBox rightBox = new VBox(10);
        rightBox.getChildren().add(new Label("Sub-articles:"));
        ListView<String> lvSubArticles = new ListView<>();

        // Sync article selection => refresh name, type, subArticles
        lvArticles.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                tfArticleName.setText(newVal.getName());
                cbArticleType.setValue(newVal.getType());
            } else {
                tfArticleName.clear();
                cbArticleType.setValue(null);
            }
            // Refresh sub-articles
            refreshSubArticlesView(newVal, lvSubArticles);
        });

        // Buttons for Articles
        Button btnAddArticle = new Button("Add Article");
        btnAddArticle.setOnAction(e -> {
            Article a = new Article("NewArticle", "Expense");
            articles.add(a);
            saveData();
            lvArticles.getSelectionModel().select(a);
        });

        Button btnDeleteArticle = new Button("Delete Article");
        btnDeleteArticle.setOnAction(e -> {
            Article selected = lvArticles.getSelectionModel().getSelectedItem();
            if (selected != null) {
                articles.remove(selected);
                saveData();
            }
        });

        Button btnUpdateArticle = new Button("Update Article");
        btnUpdateArticle.setOnAction(e -> {
            Article selected = lvArticles.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String newName = tfArticleName.getText().trim();
                if (newName.isEmpty()) {
                    showAlert("Article name cannot be empty.");
                    return;
                }
                selected.setName(newName);
                selected.setType(cbArticleType.getValue());
                lvArticles.refresh();
                saveData();
            }
        });

        leftBox.getChildren().addAll(
                new Label("Articles:"),
                lvArticles,
                new Label("Article Name:"),
                tfArticleName,
                new Label("Type:"),
                cbArticleType,
                btnUpdateArticle,
                new HBox(10, btnAddArticle, btnDeleteArticle)
        );

        // Right side: sub-article list, plus add/delete sub-articles
        TextField tfNewSubArticle = new TextField();
        tfNewSubArticle.setPromptText("Sub-article name");
        Button btnAddSub = new Button("Add Sub-article");
        btnAddSub.setOnAction(e -> {
            Article selected = lvArticles.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String sub = tfNewSubArticle.getText().trim();
                if (!sub.isEmpty()) {
                    selected.getSubArticles().add(sub);
                    refreshSubArticlesView(selected, lvSubArticles);
                    tfNewSubArticle.clear();
                    saveData();
                }
            }
        });

        Button btnDeleteSub = new Button("Delete Sub-article");
        btnDeleteSub.setOnAction(e -> {
            Article selected = lvArticles.getSelectionModel().getSelectedItem();
            String chosenSub = lvSubArticles.getSelectionModel().getSelectedItem();
            if (selected != null && chosenSub != null) {
                selected.getSubArticles().remove(chosenSub);
                refreshSubArticlesView(selected, lvSubArticles);
                saveData();
            }
        });

        rightBox.getChildren().addAll(
                lvSubArticles,
                tfNewSubArticle,
                new HBox(10, btnAddSub, btnDeleteSub)
        );

        mainBox.getChildren().addAll(leftBox, rightBox);

        stage.setScene(new Scene(mainBox, 700, 400));
        stage.show();
    }

    /**
     * Refreshes the sub-articles list for the given article.
     */
    private void refreshSubArticlesView(Article article, ListView<String> lvSubArticles) {
        lvSubArticles.getItems().clear();
        if (article != null) {
            lvSubArticles.getItems().addAll(article.getSubArticles());
        }
    }

    // -------------------------
    // Wallets Window
    // -------------------------
    private void openWalletsWindow() {
        Stage stage = new Stage();
        stage.setTitle("Wallets");

        ListView<Wallet> listView = new ListView<>(wallets);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Wallet item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " [Initial: " + item.getInitialBalance() + "]");
                }
            }
        });

        Button btnAddWallet = new Button("Add Wallet");
        btnAddWallet.setOnAction(e -> {
            openWalletEditor(null);
            listView.refresh();
        });

        Button btnEditWallet = new Button("Edit Selected");
        btnEditWallet.setOnAction(e -> {
            Wallet selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openWalletEditor(selected);
                listView.refresh();
            }
        });

        Button btnDelete = new Button("Delete Selected");
        btnDelete.setOnAction(e -> {
            Wallet selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                wallets.remove(selected);
                saveData();
                listView.refresh();
            }
        });

        HBox buttonBox = new HBox(10, btnAddWallet, btnEditWallet, btnDelete);
        VBox vbox = new VBox(10, listView, buttonBox);
        vbox.setPadding(new Insets(10));

        stage.setScene(new Scene(vbox, 400, 300));
        stage.show();
    }

    private void openWalletEditor(Wallet wallet) {
        Stage stage = new Stage();
        stage.setTitle(wallet == null ? "Add Wallet" : "Edit Wallet");

        Wallet temp = (wallet == null) ? new Wallet() : cloneWallet(wallet);

        TextField tfName = new TextField(temp.getName() == null ? "" : temp.getName());
        TextField tfBalance = new TextField(String.valueOf(temp.getInitialBalance()));

        GridPaneEx grid = new GridPaneEx(10, 10);
        grid.addRow("Wallet Name:", tfName);
        grid.addRow("Initial Balance:", tfBalance);

        Button btnSave = new Button("Save");
        btnSave.setOnAction(e -> {
            if (tfName.getText().trim().isEmpty()) {
                showAlert("Wallet name is required.");
                return;
            }
            double val;
            try {
                val = Double.parseDouble(tfBalance.getText());
            } catch (NumberFormatException ex) {
                showAlert("Initial Balance must be a valid number.");
                return;
            }
            temp.setName(tfName.getText().trim());
            temp.setInitialBalance(val);

            if (wallet == null) {
                wallets.add(temp);
            } else {
                wallet.setName(temp.getName());
                wallet.setInitialBalance(temp.getInitialBalance());
            }
            saveData();
            stage.close();
        });

        VBox vbox = new VBox(10, grid, btnSave);
        vbox.setPadding(new Insets(10));
        stage.setScene(new Scene(vbox));
        stage.show();
    }

    // -------------------------
    // Transactions Editor
    // -------------------------
    private void openTransactionEditor(Transaction transaction) {
        Stage stage = new Stage();
        stage.setTitle(transaction == null ? "Add Transaction" : "Edit Transaction");

        Transaction temp = transaction == null ? new Transaction() : cloneTransaction(transaction);

        DatePicker dpDate = new DatePicker(temp.getDate());
        dpDate.setConverter(createDateConverter());

        ComboBox<Article> cbArticle = new ComboBox<>(articles);
        cbArticle.setConverter(new StringConverter<>() {
            @Override
            public String toString(Article a) {
                return (a == null) ? "" : a.getName() + " (" + a.getType() + ")";
            }
            @Override
            public Article fromString(String s) { return null; }
        });
        cbArticle.setValue(findArticleByName(temp.getArticle()));

        // Sub-articles
        ComboBox<String> cbSubArticle = new ComboBox<>();
        cbArticle.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cbSubArticle.setItems(FXCollections.observableArrayList(newVal.getSubArticles()));
            } else {
                cbSubArticle.setItems(FXCollections.emptyObservableList());
            }
        });
        if (cbArticle.getValue() != null) {
            cbSubArticle.setItems(FXCollections.observableArrayList(cbArticle.getValue().getSubArticles()));
            cbSubArticle.setValue(temp.getSubArticle());
        }

        // Wallet
        ComboBox<Wallet> cbWallet = new ComboBox<>(wallets);
        cbWallet.setConverter(new StringConverter<>() {
            @Override
            public String toString(Wallet w) { return (w == null) ? "" : w.getName(); }
            @Override
            public Wallet fromString(String s) { return null; }
        });
        cbWallet.setValue(findWalletByName(temp.getWallet()));

        TextField tfAmount = new TextField(String.valueOf(temp.getAmount()));
        TextField tfComment = new TextField(temp.getComment() == null ? "" : temp.getComment());

        GridPaneEx grid = new GridPaneEx(10, 10);
        grid.addRow("Date:", dpDate);
        grid.addRow("Article:", cbArticle);
        grid.addRow("Sub-article:", cbSubArticle);
        grid.addRow("Wallet:", cbWallet);
        grid.addRow("Amount:", tfAmount);
        grid.addRow("Comment:", tfComment);

        Button btnSave = new Button("Save");
        btnSave.setOnAction(e -> {
            // Validate required fields
            if (dpDate.getValue() == null) {
                showAlert("Date is required.");
                return;
            }
            if (cbArticle.getValue() == null) {
                showAlert("Article is required.");
                return;
            }
            double amountVal;
            try {
                amountVal = Double.parseDouble(tfAmount.getText());
            } catch (NumberFormatException ex) {
                showAlert("Amount must be a valid number.");
                return;
            }

            temp.setDate(dpDate.getValue());
            temp.setArticle(cbArticle.getValue().getName());
            temp.setSubArticle(cbSubArticle.getValue());
            temp.setWallet(cbWallet.getValue() == null ? null : cbWallet.getValue().getName());
            temp.setAmount(amountVal);
            temp.setComment(tfComment.getText());

            // Determine type from the article
            temp.setType(cbArticle.getValue().getType());

            if (transaction == null) {
                transactions.add(temp);
            } else {
                transaction.setDate(temp.getDate());
                transaction.setArticle(temp.getArticle());
                transaction.setSubArticle(temp.getSubArticle());
                transaction.setWallet(temp.getWallet());
                transaction.setAmount(temp.getAmount());
                transaction.setComment(temp.getComment());
                transaction.setType(temp.getType());
            }

            saveData();
            updateFilteredTransactions();
            stage.close();
        });

        VBox vbox = new VBox(10, grid, btnSave);
        vbox.setPadding(new Insets(10));
        stage.setScene(new Scene(vbox));
        stage.show();
    }

    // -------------------------
    // Filter & Refresh
    // -------------------------
    private void updateFilteredTransactions() {
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();

        List<Transaction> filtered = transactions.stream()
                .filter(t -> {
                    if (start != null && t.getDate() != null && t.getDate().isBefore(start)) {
                        return false;
                    }
                    if (end != null && t.getDate() != null && t.getDate().isAfter(end)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        transactionsTable.setItems(FXCollections.observableArrayList(filtered));
        transactionsTable.refresh();
    }

    // -------------------------
    // Data Persistence (JSON)
    // -------------------------
    private void loadData() {
        File file = new File("andromeda8finance_data.json");
        if (!file.exists()) {
            articles.clear();
            wallets.clear();
            transactions.clear();
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonBuilder.append(line);
            }
            String json = jsonBuilder.toString();

            // minimal JSON parse
            Map<String, List<Map<String, Object>>> allData = SimpleJsonParser.parseRoot(json);

            // articles
            if (allData.containsKey("articles")) {
                articles.clear();
                for (Map<String, Object> map : allData.get("articles")) {
                    Article a = new Article();
                    a.setName((String) map.get("name"));
                    a.setType((String) map.get("type"));
                    if (map.get("subArticles") instanceof List) {
                        List<String> subs = new ArrayList<>();
                        for (Object o : (List<?>) map.get("subArticles")) {
                            subs.add((String) o);
                        }
                        a.setSubArticles(subs);
                    }
                    articles.add(a);
                }
            }

            // wallets
            if (allData.containsKey("wallets")) {
                wallets.clear();
                for (Map<String, Object> map : allData.get("wallets")) {
                    Wallet w = new Wallet();
                    w.setName((String) map.get("name"));
                    Number n = (Number) map.get("initialBalance");
                    if (n != null) {
                        w.setInitialBalance(n.doubleValue());
                    }
                    wallets.add(w);
                }
            }

            // transactions
            if (allData.containsKey("transactions")) {
                transactions.clear();
                for (Map<String, Object> map : allData.get("transactions")) {
                    Transaction t = new Transaction();
                    String dateStr = (String) map.get("date");
                    if (dateStr != null && !dateStr.isBlank()) {
                        t.setDate(LocalDate.parse(dateStr));
                    }
                    t.setType((String) map.get("type"));
                    t.setArticle((String) map.get("article"));
                    t.setSubArticle((String) map.get("subArticle"));
                    t.setWallet((String) map.get("wallet"));
                    Number amt = (Number) map.get("amount");
                    if (amt != null) t.setAmount(amt.doubleValue());
                    t.setComment((String) map.get("comment"));
                    transactions.add(t);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void saveData() {
        File file = new File("andromeda8finance_data.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // articles
        sb.append("  \"articles\": [\n");
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escape(a.getName())).append("\",\n");
            sb.append("      \"type\": \"").append(escape(a.getType())).append("\",\n");
            sb.append("      \"subArticles\": [");
            for (int j = 0; j < a.getSubArticles().size(); j++) {
                sb.append("\"").append(escape(a.getSubArticles().get(j))).append("\"");
                if (j < a.getSubArticles().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]\n");
            sb.append("    }");
            if (i < articles.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // wallets
        sb.append("  \"wallets\": [\n");
        for (int i = 0; i < wallets.size(); i++) {
            Wallet w = wallets.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escape(w.getName())).append("\",\n");
            sb.append("      \"initialBalance\": ").append(w.getInitialBalance()).append("\n");
            sb.append("    }");
            if (i < wallets.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // transactions
        sb.append("  \"transactions\": [\n");
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            sb.append("    {\n");
            sb.append("      \"date\": \"").append(t.getDate() == null ? "" : t.getDate().toString()).append("\",\n");
            sb.append("      \"type\": \"").append(escape(t.getType())).append("\",\n");
            sb.append("      \"article\": \"").append(escape(t.getArticle())).append("\",\n");
            sb.append("      \"subArticle\": \"").append(escape(t.getSubArticle())).append("\",\n");
            sb.append("      \"wallet\": \"").append(escape(t.getWallet())).append("\",\n");
            sb.append("      \"amount\": ").append(t.getAmount()).append(",\n");
            sb.append("      \"comment\": \"").append(escape(t.getComment())).append("\"\n");
            sb.append("    }");
            if (i < transactions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------------
    // Utility & Helpers
    // -------------------------
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }

    private Wallet cloneWallet(Wallet w) {
        Wallet clone = new Wallet();
        clone.setName(w.getName());
        clone.setInitialBalance(w.getInitialBalance());
        return clone;
    }

    private Transaction cloneTransaction(Transaction t) {
        Transaction clone = new Transaction();
        clone.setDate(t.getDate());
        clone.setType(t.getType());
        clone.setArticle(t.getArticle());
        clone.setSubArticle(t.getSubArticle());
        clone.setWallet(t.getWallet());
        clone.setAmount(t.getAmount());
        clone.setComment(t.getComment());
        return clone;
    }

    private Article findArticleByName(String name) {
        for (Article a : articles) {
            if (Objects.equals(a.getName(), name)) {
                return a;
            }
        }
        return null;
    }

    private Wallet findWalletByName(String name) {
        for (Wallet w : wallets) {
            if (Objects.equals(w.getName(), name)) {
                return w;
            }
        }
        return null;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private StringConverter<LocalDate> createDateConverter() {
        return new StringConverter<>() {
            private final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            @Override
            public String toString(LocalDate date) {
                return date != null ? df.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.isBlank()) return null;
                return LocalDate.parse(string, df);
            }
        };
    }

    // A small helper for building forms
    static class GridPaneEx extends GridPane {
        private int row = 0;
        public GridPaneEx(int hgap, int vgap) {
            setHgap(hgap);
            setVgap(vgap);
        }
        public void addRow(String label, javafx.scene.Node field) {
            add(new Label(label), 0, row);
            add(field, 1, row);
            row++;
        }
    }

    // Minimal JSON Parser used in loadData
    static class SimpleJsonParser {
        public static Map<String, List<Map<String, Object>>> parseRoot(String json) {
            Map<String, List<Map<String, Object>>> result = new HashMap<>();
            parseArray("articles", json, result);
            parseArray("wallets", json, result);
            parseArray("transactions", json, result);
            return result;
        }

        private static void parseArray(String key, String json, Map<String, List<Map<String, Object>>> result) {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) {
                result.put(key, Collections.emptyList());
                return;
            }
            int start = json.indexOf("[", idx);
            int end = findMatchingBracket(json, start);
            if (start < 0 || end < 0) {
                result.put(key, Collections.emptyList());
                return;
            }
            String arrayContent = json.substring(start + 1, end);
            List<String> objects = splitObjects(arrayContent);
            List<Map<String, Object>> list = new ArrayList<>();
            for (String objStr : objects) {
                list.add(parseObject(objStr));
            }
            result.put(key, list);
        }

        private static int findMatchingBracket(String str, int startIndex) {
            int depth = 0;
            for (int i = startIndex; i < str.length(); i++) {
                if (str.charAt(i) == '[') depth++;
                else if (str.charAt(i) == ']') depth--;
                if (depth == 0) return i;
            }
            return -1;
        }

        private static List<String> splitObjects(String arrayContent) {
            List<String> result = new ArrayList<>();
            int start = -1;
            int depth = 0;
            for (int i = 0; i < arrayContent.length(); i++) {
                char c = arrayContent.charAt(i);
                if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        result.add(arrayContent.substring(start, i + 1).trim());
                        start = -1;
                    }
                }
            }
            return result;
        }

        private static Map<String, Object> parseObject(String objStr) {
            Map<String, Object> map = new HashMap<>();
            if (objStr.startsWith("{")) objStr = objStr.substring(1);
            if (objStr.endsWith("}")) objStr = objStr.substring(0, objStr.length() - 1);
            List<String> pairs = splitByTopLevelCommas(objStr);
            for (String pair : pairs) {
                int colonIndex = pair.indexOf(':');
                if (colonIndex < 0) continue;
                String key = pair.substring(0, colonIndex).trim().replace("\"", "");
                String val = pair.substring(colonIndex + 1).trim();

                if (val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1).replace("\\\"", "\"");
                    map.put(key, val);
                } else if (val.startsWith("[") && val.endsWith("]")) {
                    String body = val.substring(1, val.length() - 1).trim();
                    List<String> arrayItems = splitByTopLevelCommas(body);
                    List<String> realItems = new ArrayList<>();
                    for (String s : arrayItems) {
                        s = s.trim();
                        if (s.startsWith("\"") && s.endsWith("\"")) {
                            s = s.substring(1, s.length() - 1).replace("\\\"", "\"");
                        }
                        if (!s.isBlank()) realItems.add(s);
                    }
                    map.put(key, realItems);
                } else {
                    // attempt numeric
                    try {
                        double d = Double.parseDouble(val);
                        map.put(key, d);
                    } catch (NumberFormatException e) {
                        map.put(key, val);
                    }
                }
            }
            return map;
        }

        private static List<String> splitByTopLevelCommas(String str) {
            List<String> result = new ArrayList<>();
            int braceDepth = 0, bracketDepth = 0;
            boolean inQuotes = false;
            int lastSplit = 0;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '"') inQuotes = !inQuotes;
                if (!inQuotes) {
                    if (c == '{') braceDepth++;
                    else if (c == '}') braceDepth--;
                    else if (c == '[') bracketDepth++;
                    else if (c == ']') bracketDepth--;
                    if (c == ',' && braceDepth == 0 && bracketDepth == 0) {
                        result.add(str.substring(lastSplit, i).trim());
                        lastSplit = i + 1;
                    }
                }
            }
            if (lastSplit < str.length()) {
                result.add(str.substring(lastSplit).trim());
            }
            return result;
        }
    }
}