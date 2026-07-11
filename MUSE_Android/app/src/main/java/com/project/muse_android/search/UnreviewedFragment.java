package com.project.muse_android.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.models.Order;
import com.project.muse_android.R;
import com.project.muse_android.databinding.FragmentUnreviewedBinding;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UnreviewedFragment extends Fragment {

    private FragmentUnreviewedBinding binding;
    private UnreviewedOrderAdapter adapter;
    private SessionManager sessionManager;
    private final List<Order> unreviewedOrders = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUnreviewedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());

        setupRecyclerView();
        fetchUnreviewedOrders();
    }

    private void setupRecyclerView() {
        adapter = new UnreviewedOrderAdapter(requireContext(), unreviewedOrders, new UnreviewedOrderAdapter.OnUnreviewedClickListener() {
            @Override
            public void onReviewClick(Order order) {
                if (getActivity() instanceof MyReviewsActivity) {
                    ((MyReviewsActivity) getActivity()).startReview(order);
                }
            }

            @Override
            public void onItemClick(Order order) {
                android.content.Intent intent = new android.content.Intent(getActivity(), com.project.muse_android.order.OrderDetailActivity.class);
                intent.putExtra("order", order);
                startActivity(intent);
            }
        });
        binding.rvUnreviewed.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvUnreviewed.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchUnreviewedOrders();
    }

    private void fetchUnreviewedOrders() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        ApiClient.INSTANCE.getInstance().getMyOrders(userId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    unreviewedOrders.clear();
                    for (Order order : response.body()) {
                        String status = order.getStatus();
                        if (("DELIVERED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "Đã giao".equalsIgnoreCase(status))
                            && !order.isReviewed()) {
                            unreviewedOrders.add(order);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    binding.emptyState.setVisibility(unreviewedOrders.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) {
                if (isAdded()) binding.emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private static class UnreviewedOrderAdapter extends RecyclerView.Adapter<UnreviewedOrderAdapter.ViewHolder> {
        private final Context context;
        private final List<Order> orders;
        private final OnUnreviewedClickListener listener;

        interface OnUnreviewedClickListener {
            void onReviewClick(Order order);
            void onItemClick(Order order);
        }

        UnreviewedOrderAdapter(Context context, List<Order> orders, OnUnreviewedClickListener listener) {
            this.context = context;
            this.orders = orders;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_unreviewed, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Order order = orders.get(position);
            
            // Show info of the first item in the order
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                Order.OrderItem firstItem = order.getItems().get(0);
                String name = firstItem.getName();
                if (order.getItems().size() > 1) {
                    name += " (và " + (order.getItems().size() - 1) + " sản phẩm khác)";
                }
                holder.tvName.setText(name);
                
                String url = firstItem.getImage();
                if (url != null) {
                    if (!url.startsWith("http")) url = "https://server-testing-ymn9.onrender.com" + (url.startsWith("/") ? "" : "/") + url;
                    Glide.with(context).load(url).placeholder(R.drawable.demo_product).into(holder.ivProduct);
                }
            }
            
            holder.btnReview.setOnClickListener(v -> listener.onReviewClick(order));
            holder.itemView.setOnClickListener(v -> listener.onItemClick(order));
        }

        @Override
        public int getItemCount() { return orders.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProduct;
            TextView tvName;
            View btnReview;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivProduct = itemView.findViewById(R.id.ivProductImage);
                tvName = itemView.findViewById(R.id.tvProductName);
                btnReview = itemView.findViewById(R.id.btnWriteReview);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
