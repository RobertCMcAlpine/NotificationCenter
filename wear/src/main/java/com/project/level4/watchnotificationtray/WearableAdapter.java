package com.project.level4.watchnotificationtray;

/**
 * Created by Rob on 20/12/2015.
 */

/**
 * Adapter used to display each individual notification icon, title and text in Notification History
 * List
 */

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.LinkedList;

public class WearableAdapter extends WearableListView.Adapter {
    private Context context;
    private final LayoutInflater mInflater;
    private LinkedList<NotificationObject> notificationLL;


    public WearableAdapter(Context context, LinkedList<NotificationObject> notificationLinkedList) {
        this.context = context;
        this.mInflater = LayoutInflater.from(context);
        this.notificationLL = notificationLinkedList;
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(
            ViewGroup viewGroup, int i) {
        return new ItemViewHolder(mInflater.inflate(R.layout.listview_item, null));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int position) {
        if (position < notificationLL.size()) {
            if (!notificationLL.isEmpty() && notificationLL != null) {
                NotificationObject notification = notificationLL.get(position);
                ItemViewHolder itemViewHolder = (ItemViewHolder) viewHolder;
                CircledImageView circledView = itemViewHolder.mCircledImageView;

                // check if notification has icon. If not, assign it one.
                if (notification.getIcon() != null){
                    Drawable icon = new BitmapDrawable(context.getResources(), notification.getIcon());
                    circledView.setImageDrawable(icon);
                } else{
                    circledView.setImageResource(R.drawable.ic_settings);
                }

                // set title for notification
                TextView textView = itemViewHolder.mItemTextView;
                if (notification.getTitle() != null){
                    if (notification.getTitle().length() > 15) {
                        textView.setText(notification.getTitle().substring(0, Math.min(notification.getTitle().length(), 15)) + "...");
                    } else {
                        textView.setText(notification.getTitle());
                    }
                } else if (notification.getText() != null) {
                    if (notification.getText().length() > 15) {
                        textView.setText(notification.getText().substring(0, Math.min(notification.getText().length(), 15)) + "...");
                    } else {
                        textView.setText(notification.getText());
                    }
                }

                itemViewHolder.itemView.setTag(position);
//                Log.i("WearableAdapter","Successfuly set onBindViewHolder");
            }
        }
    }

    @Override
    public int getItemCount() {
        return notificationLL.size();
    }

    private static class ItemViewHolder extends WearableListView.ViewHolder {
        private CircledImageView mCircledImageView;
        private TextView mItemTextView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mCircledImageView = (CircledImageView)
                    itemView.findViewById(R.id.circle);
            mItemTextView = (TextView) itemView.findViewById(R.id.name);
        }
    }
}
