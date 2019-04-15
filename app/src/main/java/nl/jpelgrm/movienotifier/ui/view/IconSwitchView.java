package nl.jpelgrm.movienotifier.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;

public class IconSwitchView extends RelativeLayout {
    @BindView(R.id.detailIcon) ImageView icon;
    @BindView(R.id.detailTitle) TextView title;
    @BindView(R.id.detailSwitch) SwitchCompat onoff;

    public IconSwitchView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public IconSwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public IconSwitchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View view = View.inflate(context, R.layout.view_iconswitch, this);
        ButterKnife.bind(this, view);

        if(attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconSwitchView, defStyleAttr, 0);

            int switchIcon = 0;
            String switchTitle = "";
            try {
                switchIcon = array.getResourceId(R.styleable.IconSwitchView_switchIcon, 0);
                switchTitle = array.getString(R.styleable.IconSwitchView_switchTitle);
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                array.recycle();
            }

            if(switchIcon != 0) {
                icon.setVisibility(VISIBLE);
                setIcon(switchIcon);
            } else {
                icon.setVisibility(INVISIBLE);
            }
            setTitle(switchTitle);
        }

        this.setOnClickListener(v -> onoff.performClick());
    }

    public void setIcon(int drawable) {
        icon.setImageResource(drawable);
    }

    public void setTitle(int title) {
        this.title.setText(title);
    }

    public void setTitle(String title) {
        this.title.setText(title);
        icon.setContentDescription(title);
    }

    public boolean isChecked() {
        return onoff.isChecked();
    }

    public void setChecked(boolean checked) {
        onoff.setChecked(checked);
    }

    public void setOnSwitchClickListener(View.OnClickListener listener) {
        onoff.setOnClickListener(listener);
    }

    public void setClickable(boolean enabled) {
        super.setClickable(enabled);
        onoff.setEnabled(enabled);
    }
}
