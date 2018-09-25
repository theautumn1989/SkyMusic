package com.example.tomato.skymusic.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by IceMan on 11/8/2016.
 */

public class Song implements Parcelable {

    private String id;
    private String title;
    private String album;
    private String artist;
    private String albumImagePath;
    private int duration;
    private String path;

    public Song(String id, String title, String album, String artist, String albumImagePath, int duration, String path) {
        this.id = id;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.albumImagePath = albumImagePath;
        this.duration = duration;
        this.path = path;
    }

    public Song(String id, String title, String album, String artist, int duration, String path) {
        this.id = id;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.duration = duration;
        this.path = path;
    }


    protected Song(Parcel in) {
        id = in.readString();
        title = in.readString();
        album = in.readString();
        artist = in.readString();
        albumImagePath = in.readString();
        duration = in.readInt();
        path = in.readString();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbumImagePath() {
        return albumImagePath;
    }

    public void setAlbumImagePath(String albumImagePath) {
        this.albumImagePath = albumImagePath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(title);
        parcel.writeString(album);
        parcel.writeString(artist);
        parcel.writeString(albumImagePath);
        parcel.writeInt(duration);
        parcel.writeString(path);
    }
}
