/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.os;

import android.os.BatteryConsumer;
import android.os.BatteryStats;
import android.os.BatteryUsageStats;
import android.os.BatteryUsageStatsQuery;

/**
 * Estimates the battery discharge amounts.
 */
public class BatteryChargeCalculator extends PowerCalculator {

    @Override
    public boolean isPowerComponentSupported(@BatteryConsumer.PowerComponent int powerComponent) {
        // Always apply this power calculator, no matter what power components were requested
        return true;
    }

    @Override
    public void calculate(BatteryUsageStats.Builder builder, BatteryStats batteryStats,
            long rawRealtimeUs, long rawUptimeUs, BatteryUsageStatsQuery query) {
        builder.setDischargePercentage(
                batteryStats.getDischargeAmount(BatteryStats.STATS_SINCE_CHARGED));

        int batteryCapacityMah = batteryStats.getLearnedBatteryCapacity() / 1000;
        if (batteryCapacityMah <= 0) {
            batteryCapacityMah = batteryStats.getMinLearnedBatteryCapacity() / 1000;
            if (batteryCapacityMah <= 0) {
                batteryCapacityMah = batteryStats.getEstimatedBatteryCapacity();
            }
        }
        builder.setBatteryCapacity(batteryCapacityMah);

        final double dischargedPowerLowerBoundMah =
                batteryStats.getLowDischargeAmountSinceCharge() * batteryCapacityMah / 100.0;
        final double dischargedPowerUpperBoundMah =
                batteryStats.getHighDischargeAmountSinceCharge() * batteryCapacityMah / 100.0;
        builder.setDischargePercentage(
                batteryStats.getDischargeAmount(BatteryStats.STATS_SINCE_CHARGED))
                .setDischargedPowerRange(dischargedPowerLowerBoundMah,
                        dischargedPowerUpperBoundMah)
                .setDischargeDurationMs(batteryStats.getBatteryRealtime(rawRealtimeUs) / 1000);

        final long batteryTimeRemainingMs = batteryStats.computeBatteryTimeRemaining(rawRealtimeUs);
        if (batteryTimeRemainingMs != -1) {
            builder.setBatteryTimeRemainingMs(batteryTimeRemainingMs / 1000);
        }

        final long chargeTimeRemainingMs = batteryStats.computeChargeTimeRemaining(rawRealtimeUs);
        if (chargeTimeRemainingMs != -1) {
            builder.setChargeTimeRemainingMs(chargeTimeRemainingMs / 1000);
        }

        builder.getAggregateBatteryConsumerBuilder(
                BatteryUsageStats.AGGREGATE_BATTERY_CONSUMER_SCOPE_DEVICE)
                .setConsumedPower(
                        (dischargedPowerLowerBoundMah + dischargedPowerUpperBoundMah) / 2);
    }
}
