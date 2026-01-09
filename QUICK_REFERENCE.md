# Quick Reference - Reusable Components & Utilities

## 📦 Available Utilities

### 1. DateUtils
**File:** `com.example.smartpos.utils.DateUtils`

```kotlin
// Format timestamp to readable date-time
DateUtils.formatTimestamp(timestamp: Long): String
// Example: "08/01/2026 10:30:45"

// Get current date in YYMMDD format for EMV
DateUtils.getCurrentDate(): String
// Example: "260108"

// Get current time in HHMMSS format for EMV
DateUtils.getCurrentTime(): String
// Example: "103045"

// Get current date-time for receipts
DateUtils.getCurrentDateTime(): String
```

---

### 2. Transaction Extensions
**File:** `com.example.smartpos.extensions.TransactionExtensions`

```kotlin
// Single transaction operations
transaction.getAmountAsDouble(): Double
transaction.canBeVoided(): Boolean
transaction.canBeRefunded(): Boolean
transaction.getStatusText(): String
transaction.getShortId(): String

// List operations
listOfTransactions.filterByType(TransactionType.SALE): List<Transaction>
listOfTransactions.calculateTotal(): Double
listOfTransactions.getActive(): List<Transaction>
```

---

## 🎨 UI Components

### 3. ScreenHeader
**File:** `com.example.smartpos.ui.theme.components.ScreenComponents`

```kotlin
ScreenHeader(
    title = "Screen Title",
    onBack = { navController.popBackStack() }
)
```

**Usage:** Every screen with back button

---

### 4. EmptyState
```kotlin
EmptyState(
    message = "No transactions available"
)
```

**Usage:** When list is empty

---

### 5. SectionHeader
```kotlin
SectionHeader(
    text = "Select a transaction:"
)
```

**Usage:** Instruction text above lists

---

### 6. ActionableTransactionCard
```kotlin
ActionableTransactionCard(
    transaction = transaction,
    actionLabel = "Void", // or "Refund", "Settle", etc.
    actionColor = Color(0xFFFF6B6B), // Red for void, Orange for refund
    onAction = { viewModel.voidTransaction(transaction) }
)
```

**Usage:** Transaction lists with action buttons

---

### 7. PrimaryActionButton
```kotlin
PrimaryActionButton(
    text = "Continue",
    onClick = { /* action */ },
    enabled = true
)
```

**Usage:** Bottom confirm/submit buttons

---

## 📐 Layout Templates

### 8. TransactionListScreenLayout
**File:** `com.example.smartpos.ui.theme.layouts.ScreenLayouts`

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactions.collectAsState()

    TransactionListScreenLayout(
        title = "My Screen Title",
        onBack = onBack,
        emptyMessage = "No items available",
        isEmpty = transactions.isEmpty(),
        instructionText = "Select an item:" // Optional
    ) {
        // LazyColumn items scope
        items(transactions) { transaction ->
            ActionableTransactionCard(
                transaction = transaction,
                actionLabel = "Action",
                actionColor = Color.Red,
                onAction = { viewModel.doAction(transaction) }
            )
        }
    }
}
```

**Perfect for:** Void, Refund, Settlement screens

---

### 9. StandardScreenLayout
```kotlin
StandardScreenLayout {
    // Your screen content with automatic:
    // - fillMaxSize()
    // - background(Color(0xFF121212))
    // - padding(24.dp)
    
    ScreenHeader("Title", onBack)
    Spacer(modifier = Modifier.height(24.dp))
    // Your content...
}
```

---

## 🔧 Common Patterns

### Pattern 1: Simple List Screen
```kotlin
@Composable
fun MyListScreen(viewModel: MyViewModel, onBack: () -> Unit) {
    val items by viewModel.items.collectAsState()

    TransactionListScreenLayout(
        title = "My List",
        onBack = onBack,
        emptyMessage = "No items",
        isEmpty = items.isEmpty()
    ) {
        items(items) { item ->
            // Your item UI
        }
    }
}
```

**Lines of code:** ~15-20 (vs 80-100 before)

---

### Pattern 2: Action List Screen (Void/Refund)
```kotlin
@Composable
fun ActionScreen(viewModel: MyViewModel, onBack: () -> Unit) {
    val transactions by viewModel.saleTransactions.collectAsState()

    TransactionListScreenLayout(
        title = "Void Transaction",
        onBack = onBack,
        emptyMessage = "No transactions",
        isEmpty = transactions.isEmpty(),
        instructionText = "Select to void:"
    ) {
        items(transactions) { transaction ->
            ActionableTransactionCard(
                transaction = transaction,
                actionLabel = "Void",
                actionColor = Color(0xFFFF6B6B),
                onAction = { viewModel.voidTransaction(transaction) }
            )
        }
    }
}
```

**Lines of code:** ~20-25 (vs 90-120 before)

---

### Pattern 3: Amount Calculation
```kotlin
// OLD WAY (10+ lines)
val totalSum: StateFlow<Double> = _transactionHistory
    .map { list -> 
        list.filter { !it.isVoided && (it.type == TransactionType.SALE || it.type == TransactionType.QR) }
            .sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 } 
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

// NEW WAY (3 lines)
val totalSum: StateFlow<Double> = _transactionHistory
    .map { list -> list.calculateTotal() }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
```

---

### Pattern 4: Date Formatting
```kotlin
// OLD WAY (scattered everywhere)
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
Text(formatTimestamp(transaction.timestamp))

// NEW WAY (one import, one call)
import com.example.smartpos.utils.DateUtils
Text(DateUtils.formatTimestamp(transaction.timestamp))
```

---

## 📋 Checklist for New Screens

Creating a new transaction list screen? Follow this:

- [ ] Import layout: `import com.example.smartpos.ui.theme.layouts.*`
- [ ] Import components: `import com.example.smartpos.ui.theme.components.*`
- [ ] Use `TransactionListScreenLayout`
- [ ] Use `ActionableTransactionCard` for items
- [ ] Import `DateUtils` if showing dates
- [ ] Use transaction extensions for calculations
- [ ] Follow existing color scheme:
  - Background: `Color(0xFF121212)`
  - Void button: `Color(0xFFFF6B6B)`
  - Refund button: `Color(0xFFFF9500)`
  - Primary button: `Color(0xFFC4FB6D)`
  - Card background: `Color(0xFF2D2D2D)`
  - Amount color: `Color(0xFFC4FB6D)`

---

## 🎯 When to Use What

| Scenario | Use This |
|----------|----------|
| Show list of transactions with actions | `TransactionListScreenLayout` + `ActionableTransactionCard` |
| Show empty list | `EmptyState` |
| Screen with back button | `ScreenHeader` |
| Instruction text above list | `SectionHeader` |
| Primary action button | `PrimaryActionButton` |
| Format date/time | `DateUtils.formatTimestamp()` |
| EMV date/time | `DateUtils.getCurrentDate/Time()` |
| Parse transaction amount | `transaction.getAmountAsDouble()` |
| Calculate list total | `list.calculateTotal()` |
| Check if can void | `transaction.canBeVoided()` |
| Short ID display | `transaction.getShortId()` |

---

## ⚠️ Don't Repeat These Anymore

### ❌ DON'T:
```kotlin
// Don't create formatTimestamp in every file
private fun formatTimestamp(timestamp: Long): String { ... }

// Don't manually parse amounts
val amount = transaction.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0

// Don't create header UI from scratch
Box(modifier = Modifier.fillMaxWidth()) {
    IconButton(...) { Icon(...) }
    Text("Title", ...)
}

// Don't create empty state UI manually
Box(modifier = Modifier.fillMaxSize()) {
    Text("No items", ...)
}

// Don't manually calculate totals
list.filter { !it.isVoided && (it.type == SALE || it.type == QR) }
    .sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 }
```

### ✅ DO:
```kotlin
// Use DateUtils
DateUtils.formatTimestamp(timestamp)

// Use extension
transaction.getAmountAsDouble()

// Use ScreenHeader
ScreenHeader(title = "Title", onBack = onBack)

// Use EmptyState
EmptyState(message = "No items")

// Use extension
list.calculateTotal()
```

---

## 💡 Tips

1. **Always check utilities first** before writing new code
2. **Use TransactionListScreenLayout** for any screen with transaction list
3. **Import extensions** when working with transactions
4. **Consistent colors** - use the predefined color scheme
5. **Keep screens simple** - business logic in ViewModel, UI in components

---

## 📞 Need Help?

Nếu không chắc chắn component/utility nào dùng:

1. Check `ScreenComponents.kt` for UI components
2. Check `ScreenLayouts.kt` for screen templates
3. Check `DateUtils.kt` for date/time operations
4. Check `TransactionExtensions.kt` for transaction operations
5. Look at `VoidScreen.kt` or `RefundScreen.kt` as reference examples

---

## 🚀 Quick Start Example

**Creating a new "Settlement Screen" in 5 minutes:**

```kotlin
package com.example.smartpos.ui.theme.screens

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.smartpos.ui.theme.components.ActionableTransactionCard
import com.example.smartpos.ui.theme.layouts.TransactionListScreenLayout
import com.example.smartpos.viewmodel.PosViewModel

@Composable
fun SettlementScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactionHistory.collectAsState()

    TransactionListScreenLayout(
        title = "Settlement",
        onBack = onBack,
        emptyMessage = "No transactions to settle",
        isEmpty = transactions.isEmpty(),
        instructionText = "Select transactions to settle:"
    ) {
        items(transactions) { transaction ->
            ActionableTransactionCard(
                transaction = transaction,
                actionLabel = "Settle",
                actionColor = Color(0xFF00C853),
                onAction = { viewModel.settleTransaction(transaction) }
            )
        }
    }
}
```

**Done! ~20 lines vs 100+ lines old way.**

---

**Last Updated:** January 8, 2026
**Version:** 1.0
**Author:** SmartPos Team
