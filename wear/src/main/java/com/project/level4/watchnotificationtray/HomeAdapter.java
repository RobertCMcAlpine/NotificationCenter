package com.project.level4.watchnotificationtray;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Rob on 9/27/17.
 */

public class HomeAdapter extends WearableRecyclerView.Adapter<WearableRecyclerView.ViewHolder>{

    private static final int PENDING_REMOVAL_TIMEOUT = 3000; // 3sec

    private List<NotificationObject> itemsPendingRemoval = new LinkedList<NotificationObject>();
    private boolean undoOn; // is undo on, you can turn it on from the toolbar menu
    private Handler handler = new Handler(); // hanlder for running delayed runnables
    private HashMap<NotificationObject, Runnable> pendingRunnables = new HashMap<>(); // map of items to pending runnables, so we can cancel a removal if need be

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private Context context;
    private LinkedList<NotificationObject> menuItemsLL;

    public class HeaderViewHolder extends WearableRecyclerView.ViewHolder {
        public TextView headerTitle;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            headerTitle = (TextView) itemView.findViewById(R.id.recycler_view_header);
        }

    }

    public class ItemHolder extends WearableRecyclerView.ViewHolder {
        public TextView title;
        public CircledImageView icon;
        public View notificationView;

        public ItemHolder(View view) {
            super(view);
            notificationView = view;
            title = (TextView) view.findViewById(R.id.name);
            icon = (CircledImageView) view.findViewById(R.id.circle);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    menuItemsLL.get((getAdapterPosition())-1).readNotification();
                    String title = menuItemsLL.get(getAdapterPosition()-1).getTitle();
                    String text = menuItemsLL.get(getAdapterPosition()-1).getText();
                    ((WearMainActivity) context).setReadReceipt(getAdapterPosition()-1);
                    Intent notificationIntent = new Intent(context, WearNotificationActivity.class);
                    if (title != null) {
                        notificationIntent.putExtra("title", title);
                    }
                    if (text != null) {
                        notificationIntent.putExtra("text", text);
                    }
                    context.startActivity(notificationIntent);
                }
            });
        }

        public void setUnreadColor(){
            notificationView.setBackgroundColor(context.getResources().getColor(R.color.unread_color, null));
        }

        public void setReadColor(){
            notificationView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent, null));
        }
    }


    public HomeAdapter(Context context, LinkedList<NotificationObject> menuItemsLinkedList) {
        this.context = context;
        this.menuItemsLL = menuItemsLinkedList;
    }

    @Override
    public WearableRecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v;

        if (viewType == TYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_header_layout, parent, false);
            HeaderViewHolder vh = new HeaderViewHolder(v);
            return vh;
        } else if (viewType == TYPE_ITEM){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_item, parent, false);
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
                NotificationObject item = menuItemsLL.get(position-1);
                vh.title.setText(item.getTitle());
                Drawable drawableIcon = new BitmapDrawable(context.getResources(), item.getIcon());
                vh.icon.setImageDrawable(drawableIcon);
                if (!item.readReceipt()){
                    ((ItemHolder) holder).setUnreadColor();
                }
                else if (item.readReceipt()){
                    ((ItemHolder) holder).setReadColor();
                    ((WearMainActivity) context.getApplicationContext()).setReadReceipt(position-1);
                }
            } else if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder vh = (HeaderViewHolder) holder;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        // Add extra view to show the header view
        return menuItemsLL.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    public void setUndoOn(boolean undoOn) {
        this.undoOn = undoOn;
    }

    public boolean isUndoOn() {
        return undoOn;
    }

    public void pendingRemoval(int position) {
        final NotificationObject item = menuItemsLL.get(position);
        if (!itemsPendingRemoval.contains(item)) {
            itemsPendingRemoval.add(item);
            // this will redraw row in "undo" state
            notifyItemChanged(position-1);
            // let's create, store and post a runnable to remove the item
            Runnable pendingRemovalRunnable = new Runnable() {
                @Override
                public void run() {
                    remove(menuItemsLL.indexOf(item));
                }
            };
            handler.postDelayed(pendingRemovalRunnable, PENDING_REMOVAL_TIMEOUT);
            pendingRunnables.put(item, pendingRemovalRunnable);
        }
    }

    public void remove(int position) {
        NotificationObject item = menuItemsLL.get(position-1);
        if (itemsPendingRemoval.contains(item)) {
            itemsPendingRemoval.remove(item);
        }
        if (menuItemsLL.contains(item)) {
            menuItemsLL.remove(position-1);
            notifyItemRemoved(position);
        }
    }

    public boolean isPendingRemoval(int position) {
        NotificationObject item = menuItemsLL.get(position);
        return itemsPendingRemoval.contains(item);
    }

    public interface ReadReceiptInterface {
        void setReadReceipt(int position);
    }

}


