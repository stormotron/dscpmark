package com.netstorm.dscpmark;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private List<AppItem> appList;
    private List<AppItem> fullList;

    public AppAdapter(List<AppItem> appList) {
        this.appList = appList;
        this.fullList = new ArrayList<>(appList);
    }

    public void filter(String query) {
        appList.clear();
        if (query.isEmpty()) {
            appList.addAll(fullList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (AppItem item : fullList) {
                if (item.appName.toLowerCase().contains(lowerCaseQuery) ||
                    item.packageName.toLowerCase().contains(lowerCaseQuery)) {
                    appList.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem item = appList.get(position);
        
        holder.cbApp.setOnCheckedChangeListener(null);
        holder.tvAppName.setText(item.appName);
        holder.ivIcon.setImageDrawable(item.icon);
        holder.cbApp.setChecked(item.isSelected);
        
        if (item.isSelected) {
            holder.tvDscpMark.setText(holder.itemView.getContext().getString(R.string.dscp_mark_label, item.dscpMark));
            holder.tvDscpMark.setVisibility(View.VISIBLE);
        } else {
            holder.tvDscpMark.setVisibility(View.GONE);
        }

        holder.cbApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showDscpDialog(holder.itemView, item, holder);
            } else {
                item.isSelected = false;
                item.dscpMark = 0;
                holder.tvDscpMark.setVisibility(View.GONE);
            }
        });
    }

    private void showDscpDialog(View view, AppItem item, AppViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle(R.string.specify_dscp_label);

        final EditText input = new EditText(view.getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, (dialog, which) -> {
            String val = input.getText().toString();
            try {
                int mark = Integer.parseInt(val);
                if (mark >= 0 && mark <= 63) {
                    item.isSelected = true;
                    item.dscpMark = mark;
                    holder.tvDscpMark.setText(view.getContext().getString(R.string.dscp_mark_label, mark));
                    holder.tvDscpMark.setVisibility(View.VISIBLE);
                } else {
                    holder.cbApp.setChecked(false);
                    Toast.makeText(view.getContext(), R.string.value_must_be_between_0_63, Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                holder.cbApp.setChecked(false);
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.cancel();
            holder.cbApp.setChecked(false);
        });
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbApp;
        ImageView ivIcon;
        TextView tvAppName;
        TextView tvDscpMark;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            cbApp = itemView.findViewById(R.id.cbApp);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvDscpMark = itemView.findViewById(R.id.tvDscpMark);
        }
    }
}