package com.shazwan.sagadash;

import android.app.*;
import android.content.*;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

/** Actual native dashboard FM UI; the installed OEM service retains tuner/audio access. */
final class DashFmPanel extends LinearLayout {
    final DashFmController controller;
    final TextView frequency,status,power,stations,source;
    private final TextView[] presets=new TextView[6];
    private final Activity owner;
    private final float unit;
    private final Runnable diagnostics;
    private final LinearLayout heading,tuning,transport,favourites;
    private AlertDialog dialog;
    private boolean active,closed,compact;
    DashFmPanel(Activity owner,float scale,Runnable diagnostics){
        super(owner);this.owner=owner;this.unit=Math.max(.1f,scale);this.diagnostics=diagnostics;setOrientation(VERTICAL);setPadding(p(6),p(2),p(6),p(3));
        controller=new DashFmController(owner,this::refresh);
        heading=new LinearLayout(owner);TextView name=label("FM RADIO",12,DashboardActivity.BLUE);heading.addView(name,new LayoutParams(0,-1,1));
        source=control("Auto ▾","Choose FM source",this::chooseSource);heading.addView(source,new LayoutParams(p(115),-1));
        heading.addView(control("⋯","FM options",this::options),new LayoutParams(p(40),-1));addView(heading,new LayoutParams(-1,p(29)));
        status=label("",13,DashboardActivity.MUTED);status.setMaxLines(2);addView(status,new LayoutParams(-1,p(38)));
        frequency=label("",54,DashboardActivity.TEXT);frequency.setIncludeFontPadding(false);frequency.setGravity(Gravity.CENTER);frequency.setOnClickListener(v->{if(active&&!closed)editFrequency();});frequency.setContentDescription("FM frequency; tap to tune");addView(frequency,new LayoutParams(-1,0,1));
        tuning=new LinearLayout(owner);add(tuning,control("−0.1","Tune down 0.1 MHz",()->controller.frequency(DashFmBand.step(controller.frequency,-1))));add(tuning,control("Tune","Enter FM frequency",this::editFrequency));add(tuning,control("+0.1","Tune up 0.1 MHz",()->controller.frequency(DashFmBand.step(controller.frequency,1))));addView(tuning,new LayoutParams(-1,p(44)));
        transport=new LinearLayout(owner);power=control("Start FM","Start or stop FM inside this panel",()->{if(!active||closed)return;if(controller.owned)controller.stop();else controller.start();});add(transport,power);
        stations=control("Stations","Choose a station returned by the FM service",this::chooseStation);add(transport,stations);addView(transport,new LayoutParams(-1,p(44)));
        favourites=new LinearLayout(owner);
        for(int index=0;index<presets.length;index++){
            final int slot=index;TextView preset=control("", "FM preset "+(slot+1)+"; hold to save",()->{
                int value=preset(slot);if(DashFmBand.valid(value))controller.frequency(value);else Toast.makeText(owner,"Hold to save the selected frequency",Toast.LENGTH_SHORT).show();
            });
            preset.setOnLongClickListener(v->{owner.getSharedPreferences(DashFmController.PREFS,Context.MODE_PRIVATE).edit().putInt("preset_"+slot,controller.frequency).apply();refresh();Toast.makeText(owner,"Preset "+(slot+1)+" saved",Toast.LENGTH_SHORT).show();return true;});
            presets[index]=preset;add(favourites,preset);
        }
        addView(favourites,new LayoutParams(-1,p(40)));refresh();
    }
    private int preset(int index){try{return owner.getSharedPreferences(DashFmController.PREFS,Context.MODE_PRIVATE).getInt("preset_"+index,0);}catch(ClassCastException invalid){return 0;}}
    private int p(float n){return Math.max(1,Math.round(n*unit));}
    private TextView label(String value,int size,int color){TextView t=new TextView(owner);t.setText(value);t.setTextColor(color);t.setTextSize(TypedValue.COMPLEX_UNIT_PX,p(size));t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private TextView control(String value,String description,Runnable action){
        TextView t=label(value,14,DashboardActivity.TEXT);t.setGravity(Gravity.CENTER);t.setContentDescription(description);
        GradientDrawable b=new GradientDrawable();b.setColor(DashboardActivity.PANEL);b.setCornerRadius(p(14));b.setStroke(p(1),DashboardActivity.LINE);t.setBackground(b);t.setOnClickListener(v->{if(!closed&&active)action.run();});return t;
    }
    private void add(LinearLayout row,View child){LayoutParams lp=new LayoutParams(0,-1,1);lp.setMargins(p(3),p(2),p(3),p(2));row.addView(child,lp);}
    private void refresh(){
        // Controller does not notify during construction; all views exist when refresh is first called.
        if(closed)return;
        frequency.setText(DashFmBand.text(controller.frequency)+" MHz");status.setText(controller.message);power.setText(controller.owned?"Stop FM":"Start FM");source.setText(controller.sourceLabel()+" ▾");
        stations.setEnabled(controller.stations.length>0&&!controller.initializing);stations.setAlpha(stations.isEnabled()?1:.4f);
        for(int i=0;i<presets.length;i++){int value=preset(i);presets[i].setText(DashFmBand.valid(value)?DashFmBand.text(value):"P"+(i+1)+" +");}
    }
    private void show(AlertDialog shown){
        if(dialog!=null)dialog.dismiss();dialog=shown;shown.setOnDismissListener(d->{if(dialog==shown)dialog=null;});shown.show();
    }
    private void chooseSource(){
        String[] modes={"auto","internal","external"};String[] names={"Auto · use head-unit FM selection","Built-in FM · MediaTek","External FM · GalaRadio"};
        int selected="internal".equals(controller.sourceMode)?1:"external".equals(controller.sourceMode)?2:0;
        show(new AlertDialog.Builder(owner).setTitle("FM source").setSingleChoiceItems(names,selected,(d,index)->{controller.source(modes[index]);d.dismiss();}).setNegativeButton("Cancel",null).create());
    }
    private void editFrequency(){
        EditText input=new EditText(owner);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);input.setText(DashFmBand.text(controller.frequency));input.selectAll();
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(8)});
        AlertDialog editor=new AlertDialog.Builder(owner).setTitle("FM frequency · MHz").setView(input).setPositiveButton("Tune",null).setNegativeButton("Cancel",null).create();
        show(editor);editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{controller.frequency(DashFmBand.parse(input.getText().toString()));editor.dismiss();}catch(IllegalArgumentException invalid){input.setError(invalid.getMessage());}});
    }
    private void chooseStation(){
        int[] values=controller.stations.clone();if(values.length==0)return;String[] names=new String[values.length];for(int i=0;i<values.length;i++)names[i]=DashFmBand.text(values[i])+" MHz";
        show(new AlertDialog.Builder(owner).setTitle("Stations reported by FM").setItems(names,(d,index)->controller.frequency(values[index])).setNegativeButton("Cancel",null).create());
    }
    private void options(){show(new AlertDialog.Builder(owner).setTitle("FM options").setItems(new String[]{"Presets","Save preset","Radio report"},(d,index)->{if(index<2)presetList(index==1);else diagnostics.run();}).setNegativeButton("Close",null).create());}
    private void presetList(boolean save){
        String[] names=new String[6];for(int i=0;i<names.length;i++){int value=preset(i);names[i]="P"+(i+1)+" · "+(save?"Save "+DashFmBand.text(controller.frequency):DashFmBand.valid(value)?DashFmBand.text(value)+" MHz":"Empty · save "+DashFmBand.text(controller.frequency));}
        show(new AlertDialog.Builder(owner).setTitle(save?"Save preset":"FM presets").setItems(names,(d,index)->{
            int value=preset(index);if(save||!DashFmBand.valid(value)){owner.getSharedPreferences(DashFmController.PREFS,Context.MODE_PRIVATE).edit().putInt("preset_"+index,controller.frequency).apply();refresh();}else controller.frequency(value);
        }).setNegativeButton("Cancel",null).create());
    }
    void resume(){if(!closed)active=true;}
    void pause(){active=false;if(dialog!=null){dialog.dismiss();dialog=null;}controller.stop();}
    void close(){if(closed)return;pause();controller.close();closed=true;}
    @Override protected void onMeasure(int w,int h){
        boolean small=MeasureSpec.getMode(h)!=MeasureSpec.UNSPECIFIED&&MeasureSpec.getSize(h)<p(230);
        if(small!=compact){compact=small;heading.getLayoutParams().height=p(small?26:29);status.getLayoutParams().height=p(small?20:38);status.setMaxLines(small?1:2);status.setEllipsize(android.text.TextUtils.TruncateAt.END);tuning.getLayoutParams().height=p(small?36:44);transport.getLayoutParams().height=p(small?36:44);favourites.setVisibility(small?GONE:VISIBLE);}
        frequency.setTextSize(TypedValue.COMPLEX_UNIT_PX,p(small?28:54));super.onMeasure(w,h);
    }
}
