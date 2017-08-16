package nl.jpelgrm.movienotifier.ui.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;

public class AccountActivity extends AppCompatActivity {
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.frame) FrameLayout frame;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in_immediately, R.anim.exit_to_left).add(R.id.frame, new AccountStartFragment()).commit();
        }
    }

    public void showLogin() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.frame, new AccountLoginFragment())
                .addToBackStack(null)
                .commit();
    }

    public void showSignup() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.frame, new AccountAddFragment())
                .addToBackStack(null)
                .commit();
    }
}

