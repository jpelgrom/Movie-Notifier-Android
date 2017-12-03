package nl.jpelgrm.movienotifier.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;

public class LicenseTextView extends LinearLayout {
    @BindView(R.id.licenseTitle) TextView title;
    @BindView(R.id.licenseNotice) TextView notice;
    @BindView(R.id.licenseLicense) TextView license;

    public LicenseTextView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public LicenseTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public LicenseTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        View view = View.inflate(context, R.layout.view_licensetextview, this);
        ButterKnife.bind(this, view);

        if(attrs != null) {
            TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LicenseTextView, defStyleAttr, 0);

            String licenseTitle = "";
            String licenseNotice = "";
            String licenseLicense = "";
            try {
                licenseTitle = array.getString(R.styleable.LicenseTextView_licenseTitle);
                licenseNotice = array.getString(R.styleable.LicenseTextView_licenseNotice);
                licenseLicense = array.getString(R.styleable.LicenseTextView_licenseLicense);
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                array.recycle();
            }

            title.setText(licenseTitle);
            notice.setText(licenseNotice);
            license.setText(licenseLicense);
        }
    }
}
