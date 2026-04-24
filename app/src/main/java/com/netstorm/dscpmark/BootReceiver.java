package com.netstorm.dscpmark;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            IptablesHelper.deleteMainRule();
            IptablesHelper.applyRules(context);
            IptablesHelper.createMainRule();
        }
    }
}