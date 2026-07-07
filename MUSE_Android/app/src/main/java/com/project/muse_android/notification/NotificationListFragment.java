package com.project.muse_android.notification;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.models.Notification;
import com.project.muse_android.databinding.FragmentNotificationListBinding;
import com.project.muse_android.main.MainActivity;
import com.project.network.ApiClient;
import com.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationListFragment extends Fragment {

    private FragmentNotificationListBinding binding;
    private NotificationAdapter adapter;
    private String typeFilter; // "promotion", "system", "order"

    public static NotificationListFragment newInstance(String type) {
        NotificationListFragment fragment = new NotificationListFragment();
        Bundle args = new Bundle();
        args.putString("type_filter", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            typeFilter = getArguments().getString("type_filter");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        
        // Show local notifications immediately for better UX
        processAndDisplayNotifications(new ArrayList<>());
        
        fetchNotifications();
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(requireContext());
        adapter.setOnNotificationClickListener((n, position) -> {
            if ("unread".equals(n.getStatus())) {
                markNotificationAsRead(n, position);
            }
        });
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNotifications.setAdapter(adapter);
    }

    private void markNotificationAsRead(Notification n, int position) {
        if (n.getId() == null) {
            // Local notification
            n.setStatus("read");
            adapter.notifyItemChanged(position);
            
            // Save updated status locally in SessionManager
            SessionManager sessionManager = new SessionManager(requireContext());
            String userId = sessionManager.getUserId();
            List<Notification> locals = sessionManager.getLocalNotifications();
            for (Notification local : locals) {
                if (local.getTitle() != null && local.getTitle().equals(n.getTitle()) 
                        && local.getMessage() != null && local.getMessage().equals(n.getMessage())) {
                    local.setStatus("read");
                    break;
                }
            }
            String json = new com.google.gson.Gson().toJson(locals);
            requireContext().getSharedPreferences("MUSE_PREFS", Context.MODE_PRIVATE)
                    .edit().putString("local_notifications_" + userId, json).apply(); // Use user-specific key
            
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateNotificationBadge();
            }
            return;
        }

        // Server notification
        String token = new SessionManager(requireContext()).getToken();
        if (token == null) return;
        
        ApiClient.INSTANCE.getInstance().markAsRead("Bearer " + token, n.getId()).enqueue(new Callback<Notification>() {
            @Override
            public void onResponse(Call<Notification> call, Response<Notification> response) {
                if (response.isSuccessful()) {
                    n.setStatus("read");
                    adapter.notifyItemChanged(position);
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateNotificationBadge();
                    }
                }
            }

            @Override
            public void onFailure(Call<Notification> call, Throwable t) {
                // Fail silently
            }
        });
    }

    private void fetchNotifications() {
        SessionManager sessionManager = new SessionManager(requireContext());
        String userId = sessionManager.getUserId();
        String token = sessionManager.getToken();
        
        if (userId == null || token == null) {
            // Already showing local ones from onViewCreated
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        ApiClient.INSTANCE.getInstance().getNotifications("Bearer " + token, userId).enqueue(new Callback<com.project.models.NotificationResponse>() {
            @Override
            public void onResponse(Call<com.project.models.NotificationResponse> call, Response<com.project.models.NotificationResponse> response) {
                if (!isAdded()) return;
                binding.progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    processAndDisplayNotifications(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<com.project.models.NotificationResponse> call, Throwable t) {
                if (!isAdded()) return;
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void processAndDisplayNotifications(List<Notification> serverNotifications) {
        List<Notification> allNotifications = new ArrayList<>(serverNotifications);
        
        // Add local mock notifications
        List<Notification> localList = new SessionManager(requireContext()).getLocalNotifications();
        allNotifications.addAll(localList);
        
        // Sort by date (descending)
        if (allNotifications.size() > 1) {
            try {
                java.util.Collections.sort(allNotifications, (n1, n2) -> {
                    if (n1.getCreatedAt() == null) return 1;
                    if (n2.getCreatedAt() == null) return -1;
                    return n2.getCreatedAt().compareTo(n1.getCreatedAt());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<Notification> filteredList = new ArrayList<>();
        String filter = (typeFilter != null) ? typeFilter.toLowerCase().trim() : "";

        for (Notification n : allNotifications) {
            String nType = (n.getType() != null) ? n.getType().toLowerCase().trim() : "";
            
            if (filter.equals("promotion")) {
                if (nType.equals("promotion") || nType.equals("stock")) {
                    filteredList.add(n);
                }
            } else if (nType.equals(filter)) {
                filteredList.add(n);
            }
        }
        
        if (filteredList.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            adapter.setData(new ArrayList<>());
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            adapter.setData(filteredList);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
