package com.BugBazaar.ui.payment;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.BugBazaar.ui.addresses.AddressDatabaseHelper;
import com.BugBazaar.ui.addresses.AddressItem;
import com.BugBazaar.ui.cart.NotificationHelper;
import com.BugBazaar.R;
import com.BugBazaar.ui.cart.CartDatabaseHelper;
import com.BugBazaar.ui.cart.CartItem;
import com.BugBazaar.ui.cart.CartItemDBModel;
import com.BugBazaar.ui.myorders.OrderHistoryActivity;
import com.BugBazaar.ui.myorders.OrderHistoryDatabaseHelper;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import org.json.JSONObject;

import java.util.List;

public class OrderSummary extends AppCompatActivity {
    private List<CartItem> cartItems;
    private TextView txtProdQuantityOS;
    private TextView txtTotalCostOS;
    private TextView txtFinalCostOS;
    private RadioGroup rbGroupPaymentOptions;
    private RadioButton rbPayViaWallet;
    private RadioButton rbPayViaRazorpay;
    Button btnProceedPaymentOS;
    private List<AddressItem> addressList;
    private AlertDialog alertDialog;
    private AddressDatabaseHelper addressDBHelper;
    private int finalCost;  // Define finalCost as a class member
    private String newOrderID; // Declare newOrderID as a class member
    StringBuilder productnames = new StringBuilder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);
        // Toolbar title set
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Order Summary");
        txtProdQuantityOS = findViewById(R.id.txtProdQuantityOS);
        txtTotalCostOS = findViewById(R.id.txtTotalCostOS);
        txtFinalCostOS = findViewById(R.id.txtFinalCostOS);
        btnProceedPaymentOS = findViewById(R.id.btnProceedPaymentOS);


        // Retrieve all addresses from the AddressDB database
        addressDBHelper = new AddressDatabaseHelper(this);
        addressList = addressDBHelper.getAllAddresses();
        // Find the default address named "Home"
        String defaultAddress = findDefaultAddress(addressList);
        // Set the default address in the TextView
        TextView txtAddressBox = findViewById(R.id.txtAddressBox);
        txtAddressBox.setText(defaultAddress);

        TextView editAddressBtn = findViewById(R.id.editAddressBtn); // Make sure to use the correct ID

        // Get a reference to the txtWalletBalance TextView
        TextView txtWalletBalance = findViewById(R.id.txtWalletBalance);
        // Update the balance dynamically (you need to retrieve the actual balance)
        int walletBalance = getWalletBalance(); // Replace with your logic to retrieve the wallet balance
        String formattedBalance=formatPrice(walletBalance);
        formattedBalance= "Wallet Balance: \n" + formattedBalance; // Format the balance string

        txtWalletBalance.setText(formattedBalance);

        // Initialize the RadioGroup and RadioButton elements
        rbGroupPaymentOptions = findViewById(R.id.rbGroupPaymentOptions);
        rbPayViaWallet = findViewById(R.id.rbPayViaWallet);
        rbPayViaRazorpay = findViewById(R.id.rbPayViaRazorpay);

        // Initialize Razorpay with your API key
        Checkout.preload(getApplicationContext());
        Checkout checkout = new Checkout();
        checkout.setKeyID("rzp_test_YEExgm42Uvy0u1");

        newOrderID = generateNewOrderID();

        // Initialize your CartDatabaseHelper
        CartDatabaseHelper cartDBHelper = new CartDatabaseHelper(this, "cart.db", null, 1);
        // Initialize your cartItems list and populate it with all items from the database
        cartItems = cartDBHelper.getAllRecords();
        // Initialize prodQuantity to store the product quantity
        int prodQuantity = 0;
        // Iterate through the list of cart items and retrieve the product_quantity from the database
        for (CartItem cartItem : cartItems) {
            // Retrieve the product name for each item
            String productName = cartItem.getProductName();

            // Retrieve the product quantity from the database using a query
            int quantity = getProductQuantityFromDatabase(cartDBHelper, productName);

        // Add the quantity to prodQuantity
        prodQuantity += quantity;
        //Product Names String for Notification
        productnames.append(productName+",\n");
    }
        // Set the product quantity
        txtProdQuantityOS.setText(String.valueOf(prodQuantity));


        Intent intent = getIntent();
        int totalCost = intent.getIntExtra("totalPrice", 0);
        // Format the totalCost and set it in the TextView
        String formattedTotalCost = formatPrice(totalCost);
        txtTotalCostOS.setText(formattedTotalCost);

        int deliveryCharges = 536;

        // Final Cost to be sent to Razorpay or wallet
        finalCost = totalCost + deliveryCharges;
        String formattedFinalCost = formatPrice(finalCost);
        txtFinalCostOS.setText(formattedFinalCost);

            // Set an OnClickListener to the "Proceed to Payment" button
            btnProceedPaymentOS.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Check which radio button is selected
                    int selectedRadioButtonId = rbGroupPaymentOptions.getCheckedRadioButtonId();

                    if (selectedRadioButtonId == R.id.rbPayViaWallet) {

                        int walletBalance = getWalletBalance(); // Replace with your logic to retrieve the wallet balance
                        if (walletBalance >= finalCost) {
                            // Deduct the payment amount from the wallet balance
                            int newWalletBalance = walletBalance - finalCost;
                            // Update the wallet balance (you need to implement this method)
                            updateWalletBalance(newWalletBalance);

                            // Proceed with the order (you may want to move this code to a separate method)
                            onPaymentSuccess("Wallet Payment Successful");
                        }else{
                            Toast.makeText(getApplicationContext(),"Insufficient balance in wallet",Toast.LENGTH_SHORT).show();
                        }
                    }else if (selectedRadioButtonId == R.id.rbPayViaRazorpay) {
                        int amountInPaise=finalCost*100;

                        try {
                            // You need to pass a JSONObject with payment details to Razorpay
                            JSONObject options = new JSONObject();
                            options.put("name", "BugBazaar Private Limited"); // Replace with your company name
                            options.put("description", "Order Payment");
                            options.put("currency", "INR"); // Replace with the appropriate currency code
                            options.put("amount", amountInPaise); // Amount should be in paise
                            options.put("prefill.email", "customer@example.com");
                            options.put("prefill.contact", "1234567890");

                            // Callback URL (optional, can be used for handling payment success or failure)
                            // options.put("callback_url", "your_callback_url");
                            checkout.open(OrderSummary.this,options);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        Toast.makeText(getApplicationContext(),"Please select payment method",Toast.LENGTH_SHORT).show();
                    }
                }
            });

        editAddressBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddressSelectionPopup();
            }
        });
    }
    private void showAddressSelectionPopup() {
        // Inflate the dialog layout
        View popupView = getLayoutInflater().inflate(R.layout.address_popup, null);

        // Create the AlertDialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(popupView);

        // Get the ListView from the layout
        ListView addressListView = popupView.findViewById(R.id.addressListView);

        // Retrieve the list of addresses from your database
        List<AddressItem> addressList = addressDBHelper.getAllAddresses(); // Implement this method

        // Create an adapter to display the addresses in the ListView
        ArrayAdapter<AddressItem> addressAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, addressList);

        // Set the adapter for the ListView
        addressListView.setAdapter(addressAdapter);

        // Set a click listener for the items in the ListView
        addressListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Handle the address selection here
                AddressItem selectedAddress = addressList.get(position);

                // Update the UI with the selected address
                updateAddressOnUI(selectedAddress);

                // Dismiss the popup
                alertDialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private void updateAddressOnUI(AddressItem selectedAddress) {
        // Assuming you have a TextView to display the selected address
        TextView addressTextView = findViewById(R.id.txtAddressBox); // Change to the actual ID of your TextView

        // Update the TextView with the selected address
        String addressText = selectedAddress.getNickname() + ": " + selectedAddress.getAddress();
        addressTextView.setText(addressText);
    }


    public void onPaymentSuccess(String s) {
        Toast.makeText(this,"Payment Successful",Toast.LENGTH_SHORT).show();
        CartDatabaseHelper cartDBHelper = new CartDatabaseHelper(this, "cart.db", null, 1);
        String message = String.valueOf(productnames);
        NotificationHelper.showNotification(this, new StringBuilder(message));
        // Generate a new order ID
        String newOrderID = generateNewOrderID();
        // Store the new order ID in the database
        storeOrderIDInDatabase(newOrderID);
        // Move to the Order History Activity
        moveToOrderHistoryActivity();



        // Save the order details to the OrderHistory database
        OrderHistoryDatabaseHelper dbHelper = new OrderHistoryDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Loop through your cart items and save the details for each item
        for (CartItem cartItem : cartItems) {
            String productName = cartItem.getProductName();
            int quantity = getProductQuantityFromDatabase(cartDBHelper, productName);
            //int amountInPaise = finalCost * 100; // Modify this as needed

            ContentValues values = new ContentValues();
            values.put(OrderHistoryDatabaseHelper.OrderHistoryEntry.COLUMN_ORDER_ID, newOrderID);  // Add order ID
            values.put(OrderHistoryDatabaseHelper.OrderHistoryEntry.COLUMN_PRODUCT_NAME, productName);
            values.put(OrderHistoryDatabaseHelper.OrderHistoryEntry.COLUMN_PRODUCT_QUANTITY, quantity);
            values.put(OrderHistoryDatabaseHelper.OrderHistoryEntry.COLUMN_FINAL_COST, finalCost);

            long newRowId = db.insert(OrderHistoryDatabaseHelper.OrderHistoryEntry.TABLE_NAME, null, values);
            if (newRowId == -1) {
                // Handle insertion error
            }
        }

        // Move to the Order History Activity
        Intent orderHistoryIntent = new Intent(this, OrderHistoryActivity.class);
        // Start OrderHistoryActivity
        startActivity(orderHistoryIntent);
        // Clear All Cart Items
        clearCartItems();
    }



    public void onPaymentError(int code, String response) {
        // This method is called when there is a payment error
        String message = "Your payment has been failed. Please try again!";
        NotificationHelper.showNotification(this, new StringBuilder(message));
        // Log the error code and response for debugging
        Log.e("Razorpay Error", "Error Code: " + code);
        Log.e("Razorpay Error", "Error Response: " + response);

        // You can display an error message to the user or take appropriate action based on the error code and response
        // For example, you can show a Toast message with the error details:
        Toast.makeText(this, "Payment Error: " + response, Toast.LENGTH_SHORT).show();

        // You can also perform additional error handling based on the error code if needed
        switch (code) {
            case Checkout.NETWORK_ERROR:
                Toast.makeText(this, "Network Error: Please check the internet connection.", Toast.LENGTH_LONG).show();
                break;
            case Checkout.INVALID_OPTIONS:
                // Handle invalid payment options
                break;
            case Checkout.PAYMENT_CANCELED:
                Toast.makeText(this, "Payment Error: Payment Canceled by user.", Toast.LENGTH_LONG).show();
                break;
            // Add more cases for specific error codes as needed
        }
    }

    // Define a method to retrieve product_quantity from the database based on product name
    private int getProductQuantityFromDatabase(CartDatabaseHelper dbHelper, String productName) {
        int quantity = 0;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                CartItemDBModel.CartItemEntry.TABLE_NAME, // Table name
                new String[]{CartItemDBModel.CartItemEntry.COLUMN_QUANTITY}, // Columns to retrieve
                CartItemDBModel.CartItemEntry.COLUMN_PRODUCT_NAME + "=?", // Selection criteria
                new String[]{productName}, // Selection arguments
                null, // Group by
                null, // Having
                null // Order by
        );

        if (cursor != null && cursor.moveToFirst()) {
            quantity = cursor.getInt(cursor.getColumnIndexOrThrow(CartItemDBModel.CartItemEntry.COLUMN_QUANTITY));
            cursor.close();
        }

        return quantity;
    }
    private String generateNewOrderID() {
        // Get the last used order ID from the database
        OrderHistoryDatabaseHelper orderHistoryDBHelper = new OrderHistoryDatabaseHelper(this);
        SQLiteDatabase db = orderHistoryDBHelper.getWritableDatabase();
        String lastOrderID = orderHistoryDBHelper.findLastOrderID(db);
        db.close();

        // Increment the last order ID to generate a new one
        return orderHistoryDBHelper.incrementOrderID(lastOrderID);
    }

    private void storeOrderIDInDatabase(String newOrderID) {
        OrderHistoryDatabaseHelper orderHistoryDBHelper = new OrderHistoryDatabaseHelper(this);
        SQLiteDatabase db = orderHistoryDBHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(OrderHistoryDatabaseHelper.OrderHistoryEntry.COLUMN_PRODUCT_NAME, newOrderID);
        values.put(OrderHistoryDatabaseHelper.OrderHistoryEntry.COLUMN_PRODUCT_QUANTITY, 0); // You can set the quantity as needed
        values.put(OrderHistoryDatabaseHelper.OrderHistoryEntry.COLUMN_FINAL_COST, 0); // You can set the final cost as needed

        long newRowId = db.insert(OrderHistoryDatabaseHelper.OrderHistoryEntry.TABLE_NAME, null, values);
        db.close();
    }

    private void moveToOrderHistoryActivity() {
        // Create an Intent to start the OrderHistoryActivity
        Intent intent = new Intent(this, OrderHistoryActivity.class);

        // Pass any necessary data to the OrderHistoryActivity
        // For example, you can pass the new order ID:
        intent.putExtra("order_id", newOrderID);

        // Start the OrderHistoryActivity
        startActivity(intent);
    }

    private void clearCartItems() {
        // Implement the logic to clear all cart items here
        // You can use the CartDatabaseHelper to delete all records from the cart database
        CartDatabaseHelper cartDBHelper = new CartDatabaseHelper(this, "cart.db", null, 1);
        SQLiteDatabase db = cartDBHelper.getWritableDatabase();
        db.delete(CartItemDBModel.CartItemEntry.TABLE_NAME, null, null);
        db.close();
    }

    private String formatPrice(int price) {
        return String.format("₹%,d", price);
    }

    private int getWalletBalance() {
        // Retrieve the wallet balance from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyWalletPrefs", Context.MODE_PRIVATE);
        int walletBalance = sharedPreferences.getInt("wallet_balance", 12000); // Replace "wallet_balance" with your actual key
        return walletBalance;
    }
    private void updateWalletBalance(int changeInBalance) {
        // Retrieve the current wallet balance from where it's stored (e.g., SharedPreferences or a local database)
        int currentBalance = getCurrentWalletBalance();

        // Calculate the new wallet balance after the change
        int updatedBalance = currentBalance - finalCost; // Add or deduct the change in balance

        // Store the updated balance back in the appropriate storage (e.g., SharedPreferences or a local database)
        storeWalletBalance(updatedBalance);
    }

    private int getCurrentWalletBalance() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyWalletPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getInt("wallet_balance", 12000);
    }
    public void storeWalletBalance(int newBalance) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyWalletPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("wallet_balance", newBalance);
        editor.apply();
    }
    private String findDefaultAddress(List<AddressItem> addressList) {
        for (AddressItem addressItem : addressList) {
            if ("Home".equals(addressItem.getNickname())) {
                return (addressItem.getNickname()+": "+addressItem.getAddress());
            }
        }
        // If the default address is not found, you can return a default message or handle it as needed.
        return "Default address not found";
    }

    // Code to handle back button
    public void onBackButtonClick(View view) {
        onBackPressed(); // Navigate back to the previous activity
    }
}
