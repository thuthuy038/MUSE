package com.project.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.models.User;
import com.project.muse_android.databinding.ItemShippingAddressBinding;

import java.util.ArrayList;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    public interface OnAddressActionListener {
        void onAddressSelected(User.Address address);
        void onAddressEdit(User.Address address);
    }

    private List<User.Address> addresses = new ArrayList<>();
    private OnAddressActionListener listener;
    private int selectedPosition = -1;

    public AddressAdapter(OnAddressActionListener listener) {
        this.listener = listener;
    }

    public void setAddresses(List<User.Address> addresses) {
        this.addresses = addresses;
        // Find default address
        for (int i = 0; i < addresses.size(); i++) {
            if (addresses.get(i).isDefault()) {
                selectedPosition = i;
                break;
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemShippingAddressBinding binding = ItemShippingAddressBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new AddressViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        User.Address address = addresses.get(position);
        holder.bind(address, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    class AddressViewHolder extends RecyclerView.ViewHolder {
        private final ItemShippingAddressBinding binding;

        public AddressViewHolder(ItemShippingAddressBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(User.Address address, boolean isSelected) {
            binding.txtUserName.setText(address.getFullName() != null ? address.getFullName() : "");
            binding.txtUserPhone.setText(address.getPhone() != null ? address.getPhone() : "");
            
            StringBuilder sb = new StringBuilder();
            if (address.getStreet() != null && !address.getStreet().isEmpty()) {
                sb.append(address.getStreet());
            }
            if (address.getWard() != null && !address.getWard().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getWard());
            }
            if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getDistrict());
            }
            if (address.getProvince() != null && !address.getProvince().isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.getProvince());
            }
            binding.txtAddress.setText(sb.toString());
            
            binding.txtDefaultBadge.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);
            binding.rbSelect.setChecked(isSelected);

            binding.rbSelect.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = getAdapterPosition();
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                if (listener != null) {
                    listener.onAddressSelected(address);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = getAdapterPosition();
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
                if (listener != null) {
                    listener.onAddressSelected(address);
                }
            });

            binding.tvEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddressEdit(address);
                }
            });
        }
    }
}
