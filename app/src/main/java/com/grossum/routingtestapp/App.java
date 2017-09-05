package com.grossum.routingtestapp;

import android.support.multidex.MultiDexApplication;

/**
 * @author Severyn Parkhomenko <sparkhomenko@grossum.com>
 * @copyright (c) Grossum. (http://www.grossum.com)
 * @project RoutingTestApp
 */
public class App extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
/*        Consumer<? super Throwable> defaultErrorHandler = RxJavaPlugins.getErrorHandler();
        RxJavaPlugins.setErrorHandler(error -> {
            if (!(error instanceof UndeliverableException)) {
                defaultErrorHandler.accept(error);
            }
        });*/
    }
}
