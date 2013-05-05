package com.showmehills.showmehillsuk;


import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "dGZ5b0dtQl9WRDNBMF9DXzJ3UGNZT2c6MQ") 
public class ShowMeApplicationuk extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
