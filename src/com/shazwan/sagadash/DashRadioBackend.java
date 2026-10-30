package com.shazwan.sagadash;

/** The branch recovered from the owner's GalaRadio_integrate VDEX. No hardware calls. */
final class DashRadioBackend {
    static final String PROPERTY="persist.sys.xy.proinfo.exfm";
    static final String INTERNAL="com.android.fmradio/com.android.fmradio.FmMainActivity";
    static final String EXTERNAL="com.acloud.stub.extradio/com.acloud.stub.extradio.QtActivity";

    private DashRadioBackend(){}

    /** Unknown/malformed property values must not be reported as a verified backend. */
    static String component(String value){
        if(value==null)return null;
        final int chip;
        try{chip=Integer.parseInt(value);}catch(NumberFormatException invalid){return null;}
        return chip>0&&chip<255&&chip!=43?EXTERNAL:INTERNAL;
    }
}
