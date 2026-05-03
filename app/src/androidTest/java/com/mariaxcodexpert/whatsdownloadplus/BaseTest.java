package com.mariaxcodexpert.whatsdownloadplus;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

public class BaseTest {
    public void allowPermissionsIfNeeded() throws Exception {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Thread.sleep(2000);
        UiObject allowButton = device.findObject(new UiSelector()
                .clickable(true)
                .textMatches("(?i)ALLOW|WHILE USING THE APP|ONLY THIS TIME|GRANT"));
        if (allowButton.exists()) {
            allowButton.click();
        }
    }
}