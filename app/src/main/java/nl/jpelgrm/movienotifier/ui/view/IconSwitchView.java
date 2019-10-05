package nl.jpelgrm.movienotifier.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.databinding.ViewIconswitchBinding;

public class IconSwitchView extends RelativeLayout {
    ViewIconswitchBinding binding;

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
        binding = ViewIconswitchBinding.inflate(LayoutInflater.from(context), this, true);

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
                binding.detailIcon.setVisibility(VISIBLE);
                setIcon(switchIcon);
            } else {
                binding.detailIcon.setVisibility(INVISIBLE);
            }
            setTitle(switchTitle);
        }

        this.setOnClickListener(v -> binding.detailSwitch.performClick());
    }

    public void setIcon(int drawable) {
        binding.detailIcon.setImageResource(drawable);
    }

    public void setTitle(int title) {
        binding.detailTitle.setText(title);
    }

    public void setTitle(String title) {
        binding.detailTitle.setText(title);
        binding.detailIcon.setContentDescription(title);
    }

    public boolean isChecked() {
        return binding.detailSwitch.isChecked();
    }

    public void setChecked(boolean checked) {
        binding.detailSwitch.setChecked(checked);
    }

    public void setOnSwitchClickListener(View.OnClickListener listener) {
        binding.detailSwitch.setOnClickListener(listener);
    }

    public void setClickable(boolean enabled) {
        super.setClickable(enabled);
        binding.detailSwitch.setEnabled(enabled);
    }
}
