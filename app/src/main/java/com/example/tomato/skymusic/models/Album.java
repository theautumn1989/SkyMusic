package com.example.tomato.skymusic.models;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by iceman on 10/11/2016.
 */

public class Album implements Parcelable {

    private int id;
    private String title;
    private String artist;
    ArrayList<Song> lstSong;
    Bitmap albumArt;
    String albumArtPath;

    public Album(int id, String title, String artist, String albumArtPath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.albumArtPath = albumArtPath;
    }

    public Album(int id, String title, String artist, ArrayList<Song> lstSong, Bitmap albumArt) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.lstSong = lstSong;
        this.albumArt = albumArt;
    }

    protected Album(Parcel in) {
        id = in.readInt();
        title = in.readString();
        artist = in.readString();
        albumArt = in.readParcelable(Bitmap.class.getClassLoader());
        albumArtPath = in.readString();
    }

    public static final Creator<Album> CREATOR = new Creator<Album>() {
        @Override
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        @Override
        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    public String getAlbumArtPath() {
        return albumArtPath;
    }

    public void setAlbumArtPath(String albumArtPath) {
        this.albumArtPath = albumArtPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public ArrayList<Song> getLstSong() {
        return lstSong;
    }

    public void setLstSong(ArrayList<Song> lstSong) {
        this.lstSong = lstSong;
    }

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(artist);
        parcel.writeParcelable(albumArt, i);
        parcel.writeString(albumArtPath);
    }
}
