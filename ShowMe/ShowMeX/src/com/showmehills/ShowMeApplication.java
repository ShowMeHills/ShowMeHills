package com.showmehills;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;


@ReportsCrashes(formKey = "dFJVM2VXaG9STEsxYTUxd0ZremVyY1E6MQ") 
public class ShowMeApplication extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
