package com.example.tomato.skymusic.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;


import com.example.tomato.skymusic.R;
import com.example.tomato.skymusic.activities.PlayMusicActivity;
import com.example.tomato.skymusic.adapter.SongListAdapter;
import com.example.tomato.skymusic.interfaces.SongOnCallBack;
import com.example.tomato.skymusic.models.Song;
import com.example.tomato.skymusic.utils.DataCenter;

import java.util.ArrayList;


public class FragmentSongList extends Fragment implements SongOnCallBack {

    public static final String SONG_PATH = "song_path";
    public static final String LIST_SONG = "list_song";
    public static final String SONG_POS = "position";

    View view;
    RecyclerView rvListSong;
    ArrayList<Song> listSong;
    SongListAdapter songAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_song_list, container, false);
        initViews();
        showListSong();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void initViews() {
        rvListSong = (RecyclerView) view.findViewById(R.id.rv_song_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rvListSong.setLayoutManager(layoutManager);
        rvListSong.setHasFixedSize(true);
    }

    private void showListSong() {
        listSong = DataCenter.instance.getListSong();
        songAdapter = new SongListAdapter(getActivity(), listSong, this);
        rvListSong.setAdapter(songAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search_detail, menu);
        MenuItem item = menu.findItem(R.id.action_search_detail);

        MenuItemCompat.setOnActionExpandListener(item, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                songAdapter.setFilter(listSong);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });
    }

    @Override
    public void onItemClicked(int position, boolean isLongClick) {

        Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
        intent.putExtra(SONG_PATH, listSong.get(position).getPath());
        intent.putExtra(SONG_POS, position);
        intent.putExtra(LIST_SONG, listSong);
        intent.putExtra(PlayMusicActivity.IS_PlAYING, false);
        getActivity().startActivity(intent);
    }

}
