package com.project.adapters;

import android.content.Context;
import com.project.models.Product;
import java.util.ArrayList;
import java.util.List;

public class HorizontalProductAdapter extends ProductAdapter {
    public HorizontalProductAdapter(List<Product> products) {
        super(products, TYPE_HORIZONTAL);
    }

    public HorizontalProductAdapter(Context context, OnProductClickListener listener) {
        super(new ArrayList<>(), TYPE_HORIZONTAL, listener);
    }
}
