package nl.jpelgrm.movienotifier.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.databinding.ViewLicensetextviewBinding;

public class LicenseTextView extends LinearLayout {
    ViewLicensetextviewBinding binding;

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
        binding = ViewLicensetextviewBinding.inflate(LayoutInflater.from(context), this, true);

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

            binding.licenseTitle.setText(licenseTitle);
            binding.licenseNotice.setText(licenseNotice);
            binding.licenseLicense.setText(licenseLicense);
        }
    }
}
