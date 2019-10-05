package nl.jpelgrm.movienotifier.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.databinding.ViewDoublepreferenceBinding;

public class DoubleRowIconPreferenceView extends RelativeLayout {
    ViewDoublepreferenceBinding binding;

    public DoubleRowIconPreferenceView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public DoubleRowIconPreferenceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public DoubleRowIconPreferenceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        binding = ViewDoublepreferenceBinding.inflate(LayoutInflater.from(context), this, true);

        if(attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DoubleRowIconPreferenceView, defStyleAttr, 0);

            int detailIcon = 0;
            String detailTitle = "";
            String detailValue = "";
            try {
                detailIcon = array.getResourceId(R.styleable.DoubleRowIconPreferenceView_detailIcon, 0);
                detailTitle = array.getString(R.styleable.DoubleRowIconPreferenceView_detailTitle);
                detailValue = array.getString(R.styleable.DoubleRowIconPreferenceView_detailValue);
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                array.recycle();
            }

            if(detailIcon != 0) {
               binding.detailIcon.setVisibility(VISIBLE);
               setIcon(detailIcon);
            } else {
                binding.detailIcon.setVisibility(INVISIBLE);
            }
            setTitle(detailTitle);
            setValue(detailValue);
        }
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

    public void setValue(int value) {
        binding.detailValue.setText(value);
    }

    public void setValue(String value) {
        binding.detailValue.setText(value);
    }
}
