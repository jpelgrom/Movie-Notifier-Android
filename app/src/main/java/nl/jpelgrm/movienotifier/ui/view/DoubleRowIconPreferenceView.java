package nl.jpelgrm.movienotifier.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;

public class DoubleRowIconPreferenceView extends RelativeLayout {
    @BindView(R.id.detailIcon) AppCompatImageView icon;
    @BindView(R.id.detailTitle) TextView title;
    @BindView(R.id.detailValue) TextView value;

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
        View view = View.inflate(context, R.layout.view_doublepreference, this);
        ButterKnife.bind(this, view);

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
               icon.setVisibility(VISIBLE);
               setIcon(detailIcon);
            } else {
                icon.setVisibility(INVISIBLE);
            }
            setTitle(detailTitle);
            setValue(detailValue);
        }
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

    public void setValue(int value) {
        this.value.setText(value);
    }

    public void setValue(String value) {
        this.value.setText(value);
    }
}
