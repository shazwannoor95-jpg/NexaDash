package dev.nexadash.radiofixture;
public class TrapActivity extends android.app.Activity {
    public void onCreate(android.os.Bundle state){super.onCreate(state);FixtureService.record(this,getIntent(),"activity");finish();}
}
