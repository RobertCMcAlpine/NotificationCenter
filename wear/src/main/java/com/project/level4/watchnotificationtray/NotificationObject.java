package com.project.level4.watchnotificationtray;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Rob on 08/01/2016.
 */

/**
 * Class used to store each Notification metadata from DataMap to ease readability and handling issueså
 */
public class NotificationObject implements Serializable{

    private static final long serialVersionUID = 1L;
    private String pack;
    private String title;
    private String text;
    private Bitmap icon;
    private boolean read;


    public NotificationObject() {
        this.pack = null;
        this.title = null;
        this.text = null;
        this.icon = null;
        this.read = false;


    }

    public String getPackageName() {
        return this.pack;
    }

    public String getTitle() {
        return this.title;
    }

    public String getText() {
        return this.text;
    }

    public Bitmap getIcon() {
        return this.icon;
    }

    public boolean readReceipt(){ return this.read; }

    public void setPack(String pack) {
        this.pack = pack;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public void readNotification(){ this.read = true;}


    protected class BitmapDataObject implements Serializable {
        private static final long serialVersionUID = 111696345129311948L;
        public byte[] imageByteArray;
    }

    /**
     * Included for serialization - write this layer to the output stream.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(pack);
        out.writeObject(title);
        out.writeObject(text);
        if (icon != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
            BitmapDataObject bitmapDataObject = new BitmapDataObject();
            bitmapDataObject.imageByteArray = stream.toByteArray();

            out.writeObject(bitmapDataObject);
        }
    }

    /**
     * Included for serialization - read this object from the supplied input stream.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        pack = (String) in.readObject();
        title = (String) in.readObject();
        text = (String) in.readObject();

        BitmapDataObject bitmapDataObject = (BitmapDataObject) in.readObject();
        if (bitmapDataObject != null) {
            icon = BitmapFactory.decodeByteArray(bitmapDataObject.imageByteArray, 0, bitmapDataObject.imageByteArray.length);
        } else {
            icon = null;
        }
    }
}

