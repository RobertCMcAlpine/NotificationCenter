package com.project.level4.watchnotificationtray;

/**
 * Created by vader001 on 03/10/2017.
 */

import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * Created by Rob on 9/27/17.
 */

public class NotificationAdapter extends WearableRecyclerView.Adapter<WearableRecyclerView.ViewHolder>{

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_TEXT = 1;
    private String notificationTitle;
    private String notificationInfo;

    public class HeaderViewHolder extends WearableRecyclerView.ViewHolder {
        public TextView headerTitle;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            headerTitle = (TextView) itemView.findViewById(R.id.recycler_view_header);

        }

    }

    public class ItemHolder extends WearableRecyclerView.ViewHolder {
        public TextView info;

        public ItemHolder(View view) {
            super(view);
            info = (TextView) view.findViewById(R.id.textViewNotificationText);
        }

    }

    public NotificationAdapter(String title, String info) {
        this.notificationTitle = title;
        this.notificationInfo = info;
    }

    @Override
    public WearableRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == TYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header_layout, parent, false);
            HeaderViewHolder vh = new HeaderViewHolder(v);
            return vh;
        } else if (viewType == TYPE_TEXT){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_info_layout, parent, false);
            ItemHolder vh = new ItemHolder(v);
            return vh;
        }

        throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(WearableRecyclerView.ViewHolder holder, int position) {

        try {
            if (holder instanceof ItemHolder) {
                ItemHolder vh = (ItemHolder) holder;
                vh.info.setText(notificationInfo);
            } else if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder vh = (HeaderViewHolder) holder;
                vh.headerTitle.setText(notificationTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        // Add extra view to show the header view
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        return TYPE_TEXT;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

}

