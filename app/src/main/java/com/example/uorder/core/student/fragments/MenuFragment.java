package com.example.uorder.core.student.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.uorder.R;
import com.example.uorder.core.models.Product;
import com.example.uorder.core.models.Stall;
import com.example.uorder.core.student.activities.CheckoutActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import com.google.android.material.textfield.TextInputEditText;

public class MenuFragment extends Fragment {

    RecyclerView rv;
    View bottomButtons, cardSearch;
    Button btnCheckoutNav, btnClear, btnApplySearch;
    ImageButton btnSearchToggle;
    TextInputEditText etSearchQuery;
    Spinner spinnerType;

    List<Stall> stallList = new ArrayList<>();
    List<Stall> filteredStallList = new ArrayList<>();
    List<Product> selectedList = new ArrayList<>();
    FirebaseFirestore db;

    String currentQuery = "";
    String currentType = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_menu, container, false);

        rv = v.findViewById(R.id.rv_menu_list);
        bottomButtons = v.findViewById(R.id.layout_checkout_buttons);
        btnCheckoutNav = v.findViewById(R.id.btn_checkout_nav);
        btnClear = v.findViewById(R.id.btn_clear);
        
        cardSearch = v.findViewById(R.id.card_search);
        btnSearchToggle = v.findViewById(R.id.btn_search_toggle);
        btnApplySearch = v.findViewById(R.id.btn_apply_search);
        etSearchQuery = v.findViewById(R.id.et_search_query);
        spinnerType = v.findViewById(R.id.spinner_type);

        String[] types = {"All", "Meal", "Snack", "Beverage", "Dessert", "Side Dish"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new StallAdapter(filteredStallList));

        btnSearchToggle.setOnClickListener(view -> {
            if (cardSearch.getVisibility() == View.VISIBLE) {
                cardSearch.setVisibility(View.GONE);
                currentQuery = "";
                currentType = "All";
                etSearchQuery.setText("");
                spinnerType.setSelection(0);
                applyFilter();
            } else {
                cardSearch.setVisibility(View.VISIBLE);
            }
        });

        btnApplySearch.setOnClickListener(view -> {
            currentQuery = etSearchQuery.getText().toString().trim().toLowerCase();
            currentType = spinnerType.getSelectedItem().toString();
            applyFilter();
        });

        btnCheckoutNav.setOnClickListener(view -> {
            if (selectedList.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.error_select_item), Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(getActivity(), CheckoutActivity.class);
            intent.putParcelableArrayListExtra("selectedFoods", new ArrayList<>(selectedList));
            startActivity(intent);
        });

        btnClear.setOnClickListener(view -> {
            for (Stall stall : stallList) {
                for (Product item : stall.getItems()) {
                    item.setSelected(false);
                    item.setQuantity(1);
                }
            }
            selectedList.clear();
            if (rv.getAdapter() != null) {
                rv.getAdapter().notifyDataSetChanged();
            }
            updateBottomButtons();
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshMenuData();
    }

    private void refreshMenuData() {
        if (getContext() == null) return;

        db.collection("stalls").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                stallList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Stall stall = document.toObject(Stall.class);
                    stall.setOwnerId(document.getId());
                    stall.setItems(new ArrayList<>()); // Initialize empty items list
                    stallList.add(stall);
                    fetchItemsForStall(stall);
                }
            }
        });
    }

    private void fetchItemsForStall(Stall stall) {
        db.collection("products")
                .whereEqualTo("stallId", stall.getOwnerId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Product> items = new ArrayList<>();
                        List<Product> cart = com.example.uorder.core.models.Order.getCart();
                        
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product item = document.toObject(Product.class);
                            item.setId(document.getId());
                            // Sync with cart
                            for (Product cartItem : cart) {
                                if (cartItem.getProductName().equals(item.getProductName()) && cartItem.getStallId().equals(item.getStallId())) {
                                    item.setSelected(true);
                                    item.setQuantity(cartItem.getQuantity());
                                    if (!selectedList.contains(item)) selectedList.add(item);
                                    break;
                                }
                            }
                            items.add(item);
                        }
                        stall.setItems(items);
                        applyFilter();
                        updateBottomButtons();
                    }
                });
    }

    private void applyFilter() {
        filteredStallList.clear();
        for (Stall stall : stallList) {
            boolean stallMatches = stall.getStallName().toLowerCase().contains(currentQuery);
            List<Product> filteredItems = new ArrayList<>();
            
            for (Product item : stall.getItems()) {
                boolean isAvailable = item.isAvailable();
                boolean itemMatchesName = item.getProductName().toLowerCase().contains(currentQuery);
                boolean itemMatchesType = currentType.equals("All") || currentType.equalsIgnoreCase(item.getProductType());
                
                if (isAvailable && (itemMatchesName || stallMatches) && itemMatchesType) {
                    filteredItems.add(item);
                }
            }
            
            if (!filteredItems.isEmpty()) {
                Stall filteredStall = new Stall();
                filteredStall.setStallName(stall.getStallName());
                filteredStall.setStallLogoUrl(stall.getStallLogoUrl());
                filteredStall.setOwnerId(stall.getOwnerId());
                filteredStall.setItems(filteredItems);
                filteredStallList.add(filteredStall);
            }
        }
        if (rv != null && rv.getAdapter() != null) {
            rv.getAdapter().notifyDataSetChanged();
        }
    }

    private void updateBottomButtons() {
        if (bottomButtons != null) {
            bottomButtons.setVisibility(selectedList.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private int parseQty(String value) {
        try { return Integer.parseInt(value); }
        catch (Exception e) { return 1; }
    }

    private class StallAdapter extends RecyclerView.Adapter<StallAdapter.StallVH> {
        private java.util.Set<Integer> expandedPositions = new java.util.HashSet<>();
        private List<Stall> displayList;

        StallAdapter(List<Stall> displayList) {
            this.displayList = displayList;
        }

        class StallVH extends RecyclerView.ViewHolder {
            TextView stallName, viewProfile;
            ImageView stallLogo, expandArrow;
            RecyclerView rvItems;
            View header;

            StallVH(View v) {
                super(v);
                stallName = v.findViewById(R.id.tv_stall_name);
                viewProfile = v.findViewById(R.id.tv_view_profile);
                stallLogo = v.findViewById(R.id.iv_stall_logo_small);
                expandArrow = v.findViewById(R.id.iv_expand_arrow);
                rvItems = v.findViewById(R.id.rv_stall_items);
                header = v.findViewById(R.id.card_stall_header);
            }
        }

        @NonNull
        @Override
        public StallVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_stall_section, parent, false);
            return new StallVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull StallVH holder, int position) {
            Stall stall = displayList.get(position);
            holder.stallName.setText(stall.getStallName());
            
            if (stall.getStallLogoUrl() != null && !stall.getStallLogoUrl().isEmpty()) {
                Glide.with(holder.stallLogo.getContext())
                        .load(stall.getStallLogoUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(holder.stallLogo);
            } else {
                holder.stallLogo.setImageResource(R.drawable.ic_launcher_background);
            }

            boolean isExpanded = expandedPositions.contains(position);
            holder.rvItems.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.expandArrow.setRotation(isExpanded ? 180f : 0f);

            holder.header.setOnClickListener(v -> {
                if (isExpanded) {
                    expandedPositions.remove(position);
                } else {
                    expandedPositions.add(position);
                }
                notifyItemChanged(position);
            });

            View.OnClickListener toDetail = v -> {
                Intent intent = new Intent(getActivity(), com.example.uorder.core.student.activities.StallDetailActivity.class);
                intent.putExtra("stallId", stall.getOwnerId());
                startActivity(intent);
            };

            holder.stallName.setOnClickListener(toDetail);
            holder.viewProfile.setOnClickListener(toDetail);
            holder.stallLogo.setOnClickListener(toDetail);

            holder.rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
            holder.rvItems.setAdapter(new MenuAdapter(stall.getItems()));
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }
    }

    private class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.VH> {
        List<Product> items;
        MenuAdapter(List<Product> items) { this.items = items; }
        
        class VH extends RecyclerView.ViewHolder {
            MaterialCardView card;
            TextView name, price, desc;
            ImageView img;
            Button minus, plus, add;
            EditText qty;

            VH(View v) {
                super(v);
                card = v.findViewById(R.id.card_food);
                name = v.findViewById(R.id.tv_food_name);
                price = v.findViewById(R.id.tv_food_price);
                desc = v.findViewById(R.id.tv_description);
                img = v.findViewById(R.id.iv_food_image);
                minus = v.findViewById(R.id.btn_minus);
                plus = v.findViewById(R.id.btn_plus);
                add = v.findViewById(R.id.btn_add_toggle);
                qty = v.findViewById(R.id.tv_quantity);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.item_food_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product item = items.get(position);
            h.name.setText(item.getProductName());
            h.price.setText(getString(R.string.price_format, item.getProductPrice()));
            h.desc.setText(item.getProductDescription());
            h.qty.setText(String.valueOf(item.getQuantity()));
            
            if (item.getProductImageUrl() != null && !item.getProductImageUrl().isEmpty()) {
                Glide.with(h.img.getContext())
                        .load(item.getProductImageUrl())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .into(h.img);
            } else {
                Glide.with(h.img.getContext())
                        .load(R.drawable.ic_launcher_background)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(h.img);
            }

            h.card.setStrokeColor(item.isSelected() ? 
                getResources().getColor(R.color.success) : Color.TRANSPARENT);
            h.add.setText(item.isSelected() ? getString(R.string.remove_caps) : getString(R.string.add));

            h.add.setOnClickListener(v -> {
                item.setSelected(!item.isSelected());
                if (item.isSelected()) {
                    selectedList.add(item);
                } else {
                    selectedList.remove(item);
                }
                com.example.uorder.core.models.Order.setCart(selectedList);
                updateBottomButtons();
                notifyItemChanged(position);
            });

            h.plus.setOnClickListener(v -> {
                int val = parseQty(h.qty.getText().toString()) + 1;
                item.setQuantity(val);
                h.qty.setText(String.valueOf(val));
                if (item.isSelected()) {
                    com.example.uorder.core.models.Order.setCart(selectedList);
                }
            });

            h.minus.setOnClickListener(v -> {
                int val = parseQty(h.qty.getText().toString());
                if (val > 1) {
                    val--;
                    item.setQuantity(val);
                    h.qty.setText(String.valueOf(val));
                    if (item.isSelected()) {
                        com.example.uorder.core.models.Order.setCart(selectedList);
                    }
                }
            });
        }

        @Override
        public int getItemCount() { return items.size(); }
    }
}
