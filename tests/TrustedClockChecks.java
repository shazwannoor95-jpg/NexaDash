package com.shazwan.sagadash;

/** Pure clock/deadline checks. Never changes Android settings, preferences, or real device time. */
final class TrustedClockChecks {
    private static final long DAY=86_400_000L;
    private static final long UTC=TrustedClock.MIN_UTC+200*DAY;
    private static final long ELAPSED=1_000_000L;
    private int assertions;

    private void check(boolean condition,String message){
        if(!condition)throw new AssertionError("Trusted clock: "+message);
        assertions++;
    }
    private void unknown(TrustedClock.Engine engine,long wall,long elapsed,String message){
        TrustedClock.Reading reading=engine.read(wall,elapsed);
        check(!reading.verified&&reading.source==TrustedClock.UNKNOWN&&reading.utc==wall,message);
    }
    private TrustedClock.Engine gpsClock(){
        TrustedClock.Engine engine=new TrustedClock.Engine();
        check(engine.gps("gps",UTC,ELAPSED,ELAPSED,false),"first fresh satellite fix verifies app-local time");
        return engine;
    }

    int run(){
        int before=assertions;
        wrongDeviceClocksAndSleep();
        rejectedFixes();
        restoration();
        manualConfirmation();
        corrections();
        notices();
        alarmPolicy();
        return assertions-before;
    }

    private void wrongDeviceClocksAndSleep(){
        long[] badWalls={0,1_640_995_200_000L,TrustedClock.MAX_UTC+DAY}; // 1970, 2022, wrong future.
        for(long badWall:badWalls){
            TrustedClock.Engine engine=new TrustedClock.Engine();
            unknown(engine,badWall,ELAPSED,"device wall time is not trusted merely because it exists");
            check(engine.gps("gps",UTC,ELAPSED,ELAPSED+5000,false),"satellite fix corrects wrong device clock offline");
            TrustedClock.Reading now=engine.read(badWall,ELAPSED+5000);
            check(now.verified&&now.source==TrustedClock.GPS&&now.utc==UTC+5000,"GPS UTC includes delivery delay");
            TrustedClock.Reading slept=engine.read(badWall,ELAPSED+6*60*60_000L);
            check(slept.verified&&slept.utc==UTC+6*60*60_000L,"six hours of elapsed/deep-sleep time advance the GPS clock");
        }
        TrustedClock.Engine engine=gpsClock();
        TrustedClock.Reading fresh=engine.read(0,ELAPSED+TrustedClock.MAX_AGE);
        check(fresh.verified&&fresh.utc==UTC+TrustedClock.MAX_AGE,"anchor remains usable for its specified lifetime");
        unknown(engine,0,ELAPSED+TrustedClock.MAX_AGE+1,"expired anchor cannot drive verified prayer time");
        unknown(engine,0,ELAPSED-1,"elapsed time moving backwards invalidates an anchor");
        engine.clear();unknown(engine,0,ELAPSED,"forgetting time removes verification");
        check(engine.gps("gps",UTC,ELAPSED,ELAPSED,false),"fresh GPS can verify again after forgetting time");
        TrustedClock.Engine upper=new TrustedClock.Engine();
        check(upper.gps("gps",TrustedClock.MAX_UTC-500,ELAPSED,ELAPSED,false),"last supported instant may be anchored");
        unknown(upper,0,ELAPSED+1000,"projected UTC outside supported years fails closed");
    }

    private void rejectedFixes(){
        TrustedClock.Engine engine=new TrustedClock.Engine();
        check(!engine.gps("network",UTC,ELAPSED,ELAPSED,false),"network provider cannot assert satellite time");
        check(!engine.gps("passive",UTC,ELAPSED,ELAPSED,false),"passive provider cannot assert satellite time");
        check(!engine.gps(null,UTC,ELAPSED,ELAPSED,false),"missing provider is rejected");
        check(!engine.gps("gps",UTC,ELAPSED,ELAPSED,true),"mock-provider time is rejected");
        check(!engine.gps("gps",UTC,ELAPSED,ELAPSED+TrustedClock.MAX_FIX_AGE+1,false),"stale fix is rejected");
        check(!engine.gps("gps",UTC,ELAPSED+1,ELAPSED,false),"fix from a future elapsed instant is rejected");
        check(!engine.gps("gps",UTC,-1,ELAPSED,false),"negative fix timestamp is rejected");
        long[] oldDates={0,315_964_800_000L,1_239_494_400_000L,1_554_595_200_000L,1_640_995_200_000L};
        for(long oldDate:oldDates)check(!engine.gps("gps",oldDate,ELAPSED,ELAPSED,false),"historical/week-rollover GPS date rejected: "+oldDate);
        check(!engine.gps("gps",TrustedClock.MAX_UTC,ELAPSED,ELAPSED,false),"unsupported future GPS year rejected");
        unknown(engine,0,ELAPSED,"rejected fixes never verify a clock");
        check(engine.gps("gps",UTC,ELAPSED,ELAPSED+TrustedClock.MAX_FIX_AGE,false),"freshness limit is inclusive");
        long anchor=engine.anchorUtc();
        check(!engine.gps("gps",UTC+60_000,ELAPSED,ELAPSED+TrustedClock.MAX_FIX_AGE,false),"duplicate fix cannot replace anchor");
        check(engine.anchorUtc()==anchor,"duplicate fix leaves trusted UTC unchanged");
        check(!engine.gps("gps",UTC+60_000,ELAPSED-1,ELAPSED,false),"out-of-order fix is rejected");
        check(engine.anchorUtc()==anchor,"out-of-order fix leaves trusted UTC unchanged");
    }

    private void restoration(){
        TrustedClock.Engine previous=gpsClock();long later=ELAPSED+3*60*60_000L;
        TrustedClock.Engine restored=new TrustedClock.Engine();
        check(restored.restore(previous.anchorUtc(),previous.anchorElapsed(),previous.source(),42,42,later),"same-boot process restoration accepted");
        TrustedClock.Reading reading=restored.read(0,later);
        check(reading.verified&&reading.source==TrustedClock.GPS&&reading.utc==UTC+3*60*60_000L,"restored process uses elapsed offset rather than wrong wall time");
        rejectedRestore(UTC,ELAPSED,TrustedClock.GPS,42,43,later,"anchor from an earlier boot rejected");
        rejectedRestore(UTC,ELAPSED,TrustedClock.GPS,-1,-1,later,"unknown boot identity fails closed");
        rejectedRestore(UTC,ELAPSED,TrustedClock.GPS,42,-1,later,"unavailable current boot identity fails closed");
        rejectedRestore(UTC,ELAPSED,TrustedClock.UNKNOWN,42,42,later,"unknown cached source rejected");
        rejectedRestore(UTC,ELAPSED,99,42,42,later,"unsupported cached source rejected");
        rejectedRestore(UTC,-1,TrustedClock.GPS,42,42,later,"negative cached elapsed rejected");
        rejectedRestore(UTC,later+1,TrustedClock.GPS,42,42,later,"cached future elapsed rejected");
        rejectedRestore(UTC,ELAPSED,TrustedClock.GPS,42,42,ELAPSED+TrustedClock.MAX_AGE+1,"expired cached anchor rejected");
        rejectedRestore(0,ELAPSED,TrustedClock.GPS,42,42,later,"historical cached UTC rejected");
        rejectedRestore(TrustedClock.MAX_UTC,ELAPSED,TrustedClock.GPS,42,42,later,"unsupported future cached UTC rejected");
        rejectedRestore(TrustedClock.MAX_UTC-500,ELAPSED,TrustedClock.GPS,42,42,ELAPSED+1000,"restored projected UTC beyond supported range rejected");
        TrustedClock.Engine confirmed=new TrustedClock.Engine();
        check(confirmed.restore(UTC,ELAPSED,TrustedClock.CONFIRMED_DEVICE,42,42,later),"same-boot user-confirmed clock can be restored");
        check(confirmed.read(0,later).source==TrustedClock.CONFIRMED_DEVICE,"restore preserves the confirmation source");
    }
    private void rejectedRestore(long utc,long anchorElapsed,int source,int savedBoot,int currentBoot,long now,String message){
        TrustedClock.Engine engine=new TrustedClock.Engine();
        check(!engine.restore(utc,anchorElapsed,source,savedBoot,currentBoot,now),message);
        unknown(engine,0,now,"failed restoration cannot leave a verified anchor");
    }

    private void manualConfirmation(){
        TrustedClock.Engine engine=new TrustedClock.Engine();
        check(!engine.confirmDevice(0,ELAPSED),"1970 device time cannot be confirmed");
        check(!engine.confirmDevice(1_640_995_200_000L,ELAPSED),"2022 device time cannot be confirmed");
        check(!engine.confirmDevice(TrustedClock.MAX_UTC,ELAPSED),"unsupported future device time cannot be confirmed");
        unknown(engine,0,ELAPSED,"invalid user confirmation does not verify a clock");
        check(engine.confirmDevice(UTC,ELAPSED),"user can explicitly confirm a plausible device time");
        TrustedClock.Reading confirmed=engine.read(0,ELAPSED+60_000);
        check(confirmed.verified&&confirmed.source==TrustedClock.CONFIRMED_DEVICE&&confirmed.utc==UTC+60_000,"confirmed time advances monotonically independent of current wall reading");
        engine.wallChanged();unknown(engine,UTC+60_000,ELAPSED+60_000,"a device-time change revokes manual confirmation");
        check(engine.confirmDevice(UTC,ELAPSED),"device time can be explicitly confirmed again");
        check(engine.gps("gps",UTC+10*60_000,ELAPSED+1000,ELAPSED+1000,false),"fresh GPS replaces an inaccurate user-confirmed clock");
        check(engine.read(0,ELAPSED+1000).source==TrustedClock.GPS,"satellite replacement is labelled GPS");
        engine.wallChanged();
        TrustedClock.Reading after=engine.read(0,ELAPSED+2000);
        check(after.verified&&after.source==TrustedClock.GPS&&after.utc==UTC+10*60_000+1000,"GPS anchor ignores device wall-time changes");
    }

    private void corrections(){
        TrustedClock.Engine engine=gpsClock();long at=ELAPSED+2000;
        long candidate=UTC+2000+5*60_000;
        check(!engine.gps("gps",candidate,at,at,false),"one large GPS time jump cannot replace a verified anchor");
        check(engine.read(0,at).utc==UTC+2000,"pending jump does not alter displayed trusted UTC");
        check(engine.gps("gps",candidate+1500,at+1500,at+1500,false),"second consistent fix confirms a large correction after one second");
        check(engine.read(0,at+1500).utc==candidate+1500,"confirmed correction drives app-local time");
        engine=gpsClock();
        check(!engine.gps("gps",candidate,at,at,false),"high-rate correction first fix stays pending");
        check(!engine.gps("gps",candidate+500,at+500,at+500,false),"subsecond correction fix cannot confirm early");
        check(engine.gps("gps",candidate+1000,at+1000,at+1000,false),"frequent consistent fixes do not continually postpone one-second confirmation");
        engine=gpsClock();
        check(!engine.gps("gps",candidate,at,at,false),"inconsistent correction begins pending");
        long second=candidate+120_000;
        check(!engine.gps("gps",second,at+1500,at+1500,false),"inconsistent second correction restarts confirmation");
        check(engine.gps("gps",second+1500,at+3000,at+3000,false),"new correction needs its own consistent confirmation");
        engine=gpsClock();
        check(!engine.gps("gps",candidate,at,at,false),"unconfirmed correction expires");
        long gap=TrustedClock.MAX_FIX_AGE+1;
        check(!engine.gps("gps",candidate+gap,at+gap,at+gap,false),"old pending evidence cannot confirm a correction");
        check(engine.read(0,at+gap).utc==UTC+2000+gap,"expired pending evidence leaves original clock running");
        engine=gpsClock();long backwards=UTC+2000-5*60_000;
        check(!engine.gps("gps",backwards,at,at,false),"one backwards GPS jump stays pending");
        check(engine.gps("gps",backwards+1500,at+1500,at+1500,false),"consistent backwards correction can be confirmed");
    }

    private void alarmPolicy(){
        int ledger=0;
        check(!PrayerAlarm.alreadyDelivered(ledger,0),"first prayer can be delivered");
        ledger=PrayerAlarm.markDelivered(ledger,0);ledger=PrayerAlarm.markDelivered(ledger,1);
        check(PrayerAlarm.alreadyDelivered(ledger,0),"backwards correction cannot replay prayer A after prayer B");
        check(PrayerAlarm.alreadyDelivered(ledger,1),"other delivered prayer remains recorded");
        check(!PrayerAlarm.alreadyDelivered(ledger,2),"unannounced prayer remains eligible");
        check(PrayerAlarm.alreadyDelivered(ledger,-1)&&PrayerAlarm.alreadyDelivered(ledger,5),"invalid prayer indices fail closed");
        check(TrustedClock.manualClockChanged(new TrustedClock.Reading(UTC,TrustedClock.CONFIRMED_DEVICE,true),UTC+6000),"device-clock edit missed while process absent revokes manual trust");
        check(!TrustedClock.manualClockChanged(new TrustedClock.Reading(UTC,TrustedClock.GPS,true),0),"device edits never invalidate a satellite clock");
        check(!TrustedClock.manualClockChanged(new TrustedClock.Reading(UTC,TrustedClock.CONFIRMED_DEVICE,true),UTC+100),"small read/scheduling jitter does not revoke manual trust");
        check(!PrayerAlarm.shouldDeliver(false,UTC,UTC),"unverified clock cannot trigger prayer alert");
        check(!PrayerAlarm.shouldDeliver(true,UTC,UTC-1),"future prayer is never announced early");
        check(PrayerAlarm.shouldDeliver(true,UTC,UTC),"verified alert is due exactly at prayer time");
        check(PrayerAlarm.shouldDeliver(true,UTC,UTC+PrayerAlarm.LATE_MS),"late catch-up limit is inclusive");
        check(!PrayerAlarm.shouldDeliver(true,UTC,UTC+PrayerAlarm.LATE_MS+1),"stale alert is dropped after sleep");
        check(!PrayerAlarm.shouldDeliver(true,0,UTC),"missing prayer timestamp cannot notify");
        check(PrayerAlarm.elapsedDeadline(5000,UTC,UTC+60000)==65000,"alarm deadline uses elapsed time despite incorrect RTC");
        check(PrayerAlarm.elapsedDeadline(5000,UTC,UTC)==-1,"past/due prayer cannot schedule a new future alarm");
        check(PrayerAlarm.elapsedDeadline(5000,UTC,UTC-1)==-1,"past prayer is not scheduled backwards");
    }
    private void notices(){
        long opened=ELAPSED,deadline=opened+4000;
        check(Notice.remaining(deadline,opened)==4000,"notice starts with its intended lifetime");
        check(Notice.remaining(deadline,opened+1500)==2500,"notice lifetime decreases using elapsed time");
        check(Notice.remaining(deadline,deadline)==0,"notice expires exactly at its deadline");
        check(Notice.remaining(deadline,opened+6*60*60_000L)==0,"deep sleep cannot renew an old notice lifetime");
        check(Notice.remaining(deadline,deadline+1)==0,"expired notice remainder clamps to zero");
    }
}
