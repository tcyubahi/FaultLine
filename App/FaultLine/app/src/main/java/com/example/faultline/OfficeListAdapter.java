package com.example.faultline;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Map;

public class OfficeListAdapter extends RecyclerView.Adapter<OfficeListAdapter.MyViewHolder> {
    private ArrayList<Map<String, Object>> officesList;
    private Context context;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView unitView, nameView, levelView;
        public Button registerButton;
        public MyViewHolder(View v) {
            super(v);
            unitView = v.findViewById(R.id.unitUD);
            nameView = v.findViewById(R.id.personName);
            levelView = v.findViewById(R.id.level);
            registerButton = v.findViewById(R.id.registerButton);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public OfficeListAdapter(ArrayList<Map<String, Object>> offices, Context context) {
        officesList = offices;
        this.context = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public OfficeListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {

        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.officeview, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.unitView.setText(officesList.get(position).get("name").toString());
        holder.registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity) context;
                mainActivity.registerUser(officesList.get(position));
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return officesList.size();
    }
}
