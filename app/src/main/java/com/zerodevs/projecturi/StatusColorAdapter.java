package com.zerodevs.projecturi;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class StatusColorAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<String> statuses;

    public StatusColorAdapter(Context context, List<String> statuses) {
        super(context, android.R.layout.simple_spinner_item, statuses);
        this.context = context;
        this.statuses = statuses;
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private int getColorForStatus(String status) {
        switch (status) {
            case "Pending":
                return Color.parseColor("#FFA000"); // Amber
            case "Approved":
                return Color.parseColor("#388E3D"); // Green
            case "Delivered":
                return Color.parseColor("#388E3C"); // Green
            case "Canceled":
                return Color.parseColor("#D32F2F"); // Red
            case "Pending Payment":
                return Color.parseColor("#F57C00"); // Orange
            default:
                return Color.BLACK;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        ((TextView) view).setTextColor(getColorForStatus(getItem(position)));
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        ((TextView) view).setTextColor(getColorForStatus(getItem(position)));
        return view;
    }
}

