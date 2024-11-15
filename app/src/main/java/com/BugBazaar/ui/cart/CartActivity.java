package com.BugBazaar.ui.cart;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.BugBazaar.R;
import com.BugBazaar.ui.BaseActivity;
import com.BugBazaar.ui.payment.OrderSummary;

import java.util.List;
import java.util.Locale;

public class CartActivity extends BaseActivity implements CartAdapter.UpdateTotalCostListener {
    private int totalCostq=2;
    public void updateTotalCost() {

    }

    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private CartDatabaseHelper cartDBHelper;
    TextView txtTotalCostNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maincart);

        // Initialize your CartDatabaseHelper
        CartDatabaseHelper cartDBHelper = new CartDatabaseHelper(this, "cart.db", null, 1);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("My Cart");

        // Find the RecyclerView and the empty cart TextView in the layout
        cartRecyclerView = findViewById(R.id.cartRecyclerView);
        TextView cartEmptyTextView = findViewById(R.id.cartEmptyTextView);
        ImageView cartEmptyImage=findViewById(R.id.cartEmptyImage);
        LinearLayout emptyLinear=findViewById(R.id.emptyLinear);
        Button checkoutButton=findViewById(R.id.checkoutButton);

        //TotalCost
        TextView txtTotalCostText=findViewById(R.id.txtTotalCostText);
        txtTotalCostNumber=findViewById(R.id.txtTotalCostNumber);

        // Find the RecyclerView in the layout
        cartRecyclerView = findViewById(R.id.cartRecyclerView);

        // Initialize your cartItems list and populate it with all items from the database
        cartItems = cartDBHelper.getAllRecords();

        // Create and set up the adapter
        cartAdapter = new CartAdapter(this, cartItems, cartDBHelper,this);

        cartRecyclerView.setAdapter(cartAdapter);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Check if the cart is empty and show/hide the empty cart message
        if (cartItems.isEmpty()) {
            //Empty msg and image visible
            emptyLinear.setVisibility(View.VISIBLE);
            cartEmptyTextView.setVisibility(View.VISIBLE);
            //Checkout and total Gone
            txtTotalCostText.setVisibility(View.GONE);
            txtTotalCostNumber.setVisibility(View.GONE);
            checkoutButton.setVisibility(View.GONE);
            cartEmptyImage.setVisibility(View.VISIBLE);

            // Hide the RecyclerView
            cartRecyclerView.setVisibility(View.GONE);

        } else {
            // Hide the "Cart is Empty" message
            emptyLinear.setVisibility(View.GONE);
            cartEmptyTextView.setVisibility(View.GONE);
            cartEmptyImage.setVisibility(View.GONE);

            //Checkout and total Visible
            checkoutButton.setVisibility(View.VISIBLE);
            txtTotalCostText.setVisibility(View.VISIBLE);
            txtTotalCostNumber.setVisibility(View.VISIBLE);

            // Show the RecyclerView
            cartRecyclerView.setVisibility(View.VISIBLE);
        }

        // Calculate the total cart value and update the txtTotalCostNumber TextView
        //This does not refresh price real time on UI
        int totalCost = calculateTotalCost(cartItems);
        String formattedTotalCost = formatTotalCost(totalCost);
        txtTotalCostNumber.setText(formattedTotalCost);

        checkoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalCostq = calculateTotalCost(cartItems);
                String formattedTotalCost = formatTotalCost(totalCostq);
                txtTotalCostNumber.setText(formattedTotalCost);
                // Perform the checkout action here
                Intent intentz=new Intent(CartActivity.this, OrderSummary.class);
                intentz.putExtra("totalPrice",totalCostq);
                startActivity(intentz);
            }
        });
    }

    // Calculate the total cart value
    private int calculateTotalCost(List<CartItem> cartItems) {
        int totalCost = 0;
        for (CartItem cartItem : cartItems) {
            totalCost =totalCost+ cartItem.getPrice() * cartItem.getQuantity();
        }
        return totalCost;
    }
    // Format the total cart value with commas
    private String formatTotalCost(int totalCost) {
        return String.format("₹%,d.00", totalCost);
    }

    // Code to handle back button
    public void onBackButtonClick(View view) {
        onBackPressed(); // Navigate back to the previous activity
    }
}
