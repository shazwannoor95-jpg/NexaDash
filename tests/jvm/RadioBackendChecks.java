package com.shazwan.sagadash;

/** Boundary cases of the exact selector recovered from the supplied OEM VDEX. */
public final class RadioBackendChecks {
    public static void main(String[] args){
        String[][] cases={
            {null,null},{"",null},{"abc",null},{"0x2b",null},{" 1",null},
            {"2147483648",null},{"-2147483649",null},
            {"-2147483648",DashRadioBackend.INTERNAL},{"-1",DashRadioBackend.INTERNAL},
            {"0",DashRadioBackend.INTERNAL},{"1",DashRadioBackend.EXTERNAL},
            {"42",DashRadioBackend.EXTERNAL},{"43",DashRadioBackend.INTERNAL},
            {"44",DashRadioBackend.EXTERNAL},{"254",DashRadioBackend.EXTERNAL},
            {"255",DashRadioBackend.INTERNAL},{"256",DashRadioBackend.INTERNAL},
            {"2147483647",DashRadioBackend.INTERNAL}
        };
        for(String[] entry:cases){String actual=DashRadioBackend.component(entry[0]);if(!java.util.Objects.equals(actual,entry[1]))throw new AssertionError("Selector "+entry[0]+": "+actual);}
        System.out.println("PASS: "+cases.length+" recovered Radio selector boundary checks (no firmware executed)");
    }
}
