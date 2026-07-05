package com.project.adapters;

import android.content.Context;
import com.project.models.Product;
import java.util.ArrayList;
import java.util.List;

public class VerticalProductAdapter extends ProductAdapter {
    public VerticalProductAdapter(List<Product> products) {
        super(products, TYPE_VERTICAL);
    }

    public VerticalProductAdapter(Context context, OnProductClickListener listener) {
        super(new ArrayList<>(), TYPE_VERTICAL, listener);
    }
}
