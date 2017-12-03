package nl.jpelgrm.movienotifier.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;

public class SettingsLicensesFragment extends Fragment {
    @BindView(R.id.webview) WebView webview;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_licenses, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webview.getSettings().setJavaScriptEnabled(false);
        webview.setWebViewClient(new WebViewClient());

        if(getContext() == null) {
            return;
        }

        String html = null;
        try {
            InputStream inputStream = getContext().getAssets().open("licenses.html");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            html = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean isNight;
        SharedPreferences settings = getContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
        switch(settings.getInt("prefDayNight", AppCompatDelegate.MODE_NIGHT_AUTO)) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                isNight = false;
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                isNight = true;
                break;
            case AppCompatDelegate.MODE_NIGHT_AUTO:
            default:
                int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                isNight = (currentNightMode == Configuration.UI_MODE_NIGHT_YES);
                break;
        }

        if(html != null && isNight) {
            html = html.replaceFirst("<body>", "<body class='night'>");
        }

        webview.setVerticalScrollBarEnabled(true);
        webview.loadData(html, "text/html", "UTF-8");
    }
}
