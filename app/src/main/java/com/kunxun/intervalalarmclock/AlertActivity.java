package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

public class AlertActivity extends Activity implements SlideBar.OnTriggerListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        SlideBar slideToUnLock = findViewById(R.id.slideBar);
        slideToUnLock.setOnTriggerListener(this);
    }

    @Override
    public void onTrigger() {
        Toast.makeText(this, "closed", Toast.LENGTH_SHORT).show();

    }
}
