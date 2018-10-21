package nl.jpelgrm.movienotifier.ui.view;

import android.content.Context;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import nl.jpelgrm.movienotifier.R;
import nl.jpelgrm.movienotifier.models.NotificationType;

public class NotificationTypeSettingView extends RelativeLayout {
    @BindView(R.id.typeTitle) TextView title;
    @BindView(R.id.typeDescription) TextView description;
    @BindView(R.id.typeCheckBox) AppCompatCheckBox value;

    private NotificationType notificationType;

    private Runnable checkedChangedListener = null;

    public NotificationTypeSettingView(Context context) {
        super(context);
        init(context, false, null);
    }

    public NotificationTypeSettingView(Context context, boolean checked) {
        super(context);
        init(context, checked, null);
    }

    public NotificationTypeSettingView(Context context, boolean checked, NotificationType notificationType) {
        super(context);
        init(context, checked, notificationType);
    }

    private void init(Context context, boolean checked, NotificationType notificationType) {
        View view = View.inflate(context, R.layout.view_notificationtypesetting, this);
        ButterKnife.bind(this, view);

        this.notificationType = notificationType;

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                value.setChecked(!value.isChecked());
            }
        });

        value.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(checkedChangedListener != null) {
                    checkedChangedListener.run();
                }
            }
        });

        updateViews(checked);
    }

    private void updateViews(boolean checked) {
        if(notificationType != null) {
            title.setText(notificationType.getName());
            description.setText(notificationType.getDescription());
            value.setChecked(checked);
        } else {
            title.setText("");
            description.setText("");
            value.setChecked(false);
        }
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
        updateViews(false);
    }

    public void setNotificationType(NotificationType notificationType, boolean checked) {
        this.notificationType = notificationType;
        updateViews(checked);
    }

    public boolean getValue() {
        return value.isChecked();
    }

    public void setValue(boolean value) {
        this.value.setChecked(value);
    }

    public Runnable getCheckedChangedListener() {
        return checkedChangedListener;
    }

    public void setCheckedChangedListener(Runnable checkedChangedListener) {
        this.checkedChangedListener = checkedChangedListener;
    }

    public void setEnabledUser(boolean enabled) {
        this.value.setEnabled(enabled);
        this.setClickable(enabled);
    }
}
